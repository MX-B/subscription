package io.gr1d.portal.subscriptions.service;

import static java.util.Optional.ofNullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.exception.Gr1dConstraintException;
import io.gr1d.portal.subscriptions.exception.ApiGatewayNotFoundException;
import io.gr1d.portal.subscriptions.exception.ApiGatewayTenantNotFoundException;
import io.gr1d.portal.subscriptions.exception.TenantNotFoundException;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.ApiGatewayTenant;
import io.gr1d.portal.subscriptions.model.ApiType;
import io.gr1d.portal.subscriptions.model.PaymentMethod;
import io.gr1d.portal.subscriptions.model.Plan;
import io.gr1d.portal.subscriptions.model.PlanModality;
import io.gr1d.portal.subscriptions.model.ProviderType;
import io.gr1d.portal.subscriptions.model.Tenant;
import io.gr1d.portal.subscriptions.repository.ApiGatewayTenantRepository;
import io.gr1d.portal.subscriptions.repository.PaymentMethodRepository;
import io.gr1d.portal.subscriptions.repository.PlanEndpointRepository;
import io.gr1d.portal.subscriptions.repository.PlanRepository;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantUpdateRequest;
import io.gr1d.portal.subscriptions.response.AllApiGatewayTenantResponse;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantResponse;
import io.gr1d.portal.subscriptions.response.PlanResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApiGatewayTenantService {

	private final PaymentMethodRepository paymentMethodRepository;
    private final ApiGatewayTenantRepository repository;
    private final PlanRepository planRepository;
    private final PlanEndpointRepository planEndpointRepository;
    private final ApiGatewayService apiGatewayService;
    private final TenantService tenantService;
    private final PlanService planService;

    @Autowired
    public ApiGatewayTenantService(final ApiGatewayTenantRepository repository, final PlanRepository planRepository,
                                   final ApiGatewayService apiGatewayService, final TenantService tenantService,
                                   final PlanService planService, final PlanEndpointRepository planEndpointRepository, PaymentMethodRepository paymentMethodRepository) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.apiGatewayService = apiGatewayService;
        this.tenantService = tenantService;
        this.planService = planService;
        this.planEndpointRepository = planEndpointRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public PageResult<ApiGatewayTenantResponse> list(final Specification<ApiGatewayTenant> spec, final Pageable pageable) {
        final Page<ApiGatewayTenant> page = repository.findAll(spec, pageable);
        final List<ApiGatewayTenantResponse> list = page.getContent().stream()
                .map(ApiGatewayTenantResponse::new)
                .collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    private class ApiGatewayTenantPlanConfigurer implements PlanConfigurer {

        private final ApiGatewayTenant apiGatewayTenant; 

        private ApiGatewayTenantPlanConfigurer(final ApiGatewayTenant apiGatewayTenant) {
            this.apiGatewayTenant = apiGatewayTenant;
        }

        @Override
        public void configureNewPlan(final Plan plan) {
            planService.setApiGatewayTenant(apiGatewayTenant, plan);
        }
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public ApiGatewayTenant save(final ApiGatewayTenantCreateRequest request)
            throws ApiGatewayNotFoundException, TenantNotFoundException {
        final ApiGateway apiGateway = apiGatewayService.findNotRemoved(request.getApiGatewayUuid());
        final Tenant tenant = tenantService.findNotRemoved(request.getTenantUuid());
        final ApiGatewayTenant apiGatewayTenant = new ApiGatewayTenant();
        final PlanConfigurer configurer = new ApiGatewayTenantPlanConfigurer(apiGatewayTenant);

        if (repository.countByApiAndTenant(apiGateway.getApi().getId(), tenant.getId()) > 0) {
            throw new Gr1dConstraintException("api_gateway_uuid", "io.gr1d.subscriptions.apiAlreadyExistsOnTenant");
        }

        apiGatewayTenant.setApiGateway(apiGateway);
        apiGatewayTenant.setTenant(tenant);
        apiGatewayTenant.setPercentageSplit(ofNullable(request.getPercentageSplit()).orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.DOWN));   
        
        Set<PaymentMethod> paymentMethods = Optional.ofNullable(request.getPaymentMethods())
				.map(Collection::stream).orElseGet(Stream::empty)
				.map(paymentMethod -> paymentMethodRepository.findByName(paymentMethod))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
        
        apiGatewayTenant.setPaymentMethods(paymentMethods);

        getApiType(request, apiGatewayTenant);

        final ApiGatewayTenant createdApi = repository.save(apiGatewayTenant);
        final AtomicInteger priority = new AtomicInteger(0);

        request.getPlans().forEach(plan -> planService.createOrUpdate(plan, configurer, priority.getAndIncrement()));

        return createdApi;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void update(final String uuid, final ApiGatewayTenantUpdateRequest request) throws ApiGatewayTenantNotFoundException {
        final ApiGatewayTenant apiGatewayTenant = findNotRemoved(uuid);
        final PlanConfigurer configurer = new ApiGatewayTenantPlanConfigurer(apiGatewayTenant);

        apiGatewayTenant.setPercentageSplit(ofNullable(request.getPercentageSplit()).orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.DOWN));

        getApiType(request, apiGatewayTenant);

        final AtomicInteger priority = new AtomicInteger(0);
        final List<String> plans = request.getPlans().stream()
                .map(plan -> planService.createOrUpdate(plan, configurer, priority.getAndIncrement()))
                .map(Plan::getUuid)
                .collect(Collectors.toList());

        if (plans.isEmpty()) {
            planRepository.removePlans(apiGatewayTenant, LocalDateTime.now());
        } else {
            planRepository.removePlans(apiGatewayTenant, plans, LocalDateTime.now());
        }
        
        Set<PaymentMethod> paymentMethods = Optional.ofNullable(request.getPaymentMethods())
				.map(Collection::stream).orElseGet(Stream::empty)
				.map(paymentMethod -> paymentMethodRepository.findByName(paymentMethod))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
        
        apiGatewayTenant.setPaymentMethods(paymentMethods);
        

        repository.save(apiGatewayTenant);
    }

    private void getApiType(final ApiGatewayTenantUpdateRequest request, final ApiGatewayTenant apiGatewayTenant) {
        final Map<String, String> modalities = new HashMap<>(2);
        request.getPlans().forEach(planRequest -> {
            if (planRequest.getModality().equals(PlanModality.FREE.getCode()) || planRequest.getModality().equals(PlanModality.SANDBOX.getCode())) {
                modalities.put("FREE", planRequest.getModality());
            } else {
                modalities.put("PAY", planRequest.getModality());
            }
        });

        if (modalities.containsKey("FREE") && modalities.containsKey("PAY")) {
            apiGatewayTenant.setType(ApiType.FREEMIUM);
        } else if (modalities.containsKey("PAY") && !modalities.containsKey("FREE")) {
            apiGatewayTenant.setType(ApiType.PAID);
        } else {
            apiGatewayTenant.setType(ApiType.FREE);
        }

        if (apiGatewayTenant.getApiGateway().getApi().getProvider().getType().equals(ProviderType.FREE)
                && (!apiGatewayTenant.getType().equals(ApiType.FREE))) {
            throw new Gr1dConstraintException("plans", "io.gr1d.subscriptions.paidApiNotAllowed");
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void removeByTenant(final Tenant tenant) {
        repository.findByTenant(tenant).forEach(this::remove);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void removeByApiGateway(final ApiGateway apiGateway) {
        repository.findByApiGateway(apiGateway).forEach(this::remove);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void remove(final String uuid) throws ApiGatewayTenantNotFoundException {
        final ApiGatewayTenant apiGatewayTenant = findNotRemoved(uuid);
        remove(apiGatewayTenant);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void remove(final ApiGatewayTenant apiGatewayTenant) {
        log.info("Removing ApiGatewayTenant with uuid: {}", apiGatewayTenant.getUuid());
        apiGatewayTenant.setRemovedAt(LocalDateTime.now());
        repository.save(apiGatewayTenant);
    }

    private ApiGatewayTenant get(final String uuid) throws ApiGatewayTenantNotFoundException {
        return ofNullable(repository.findByUuid(uuid))
                .orElseThrow(ApiGatewayTenantNotFoundException::new);
    }


    public AllApiGatewayTenantResponse findAllApisByExternalIdAndTenantRealm(String apiExternalId, String tenantRealm) throws ApiGatewayTenantNotFoundException {
        final ApiGatewayTenant apiGatewayTenant = ofNullable(repository.findByApiAndTenantRealm(apiExternalId, tenantRealm))
                .orElseThrow(ApiGatewayTenantNotFoundException::new);

        AllApiGatewayTenantResponse allApiGatewayTenantResponse = new AllApiGatewayTenantResponse();

        final ApiGateway apiGateway = apiGatewayTenant.getApiGateway();
        final String productionExternalId = apiGateway.getProductionExternalId();
        if (productionExternalId != null) {
            allApiGatewayTenantResponse.setProduction(toResponse(productionExternalId, apiGatewayTenant));

            final String externalId = apiGateway.getExternalId();
            if (!productionExternalId.equals(externalId)) {
                allApiGatewayTenantResponse.setSandbox(toResponse(externalId, apiGatewayTenant));
            }
        }

        return allApiGatewayTenantResponse;
    }


    public ApiGatewayTenantResponse findByApiAndGatewayAndTenantRealm(final String apiExternalId,
            final String tenantRealm) throws ApiGatewayTenantNotFoundException {
        final ApiGatewayTenant apiGatewayTenant = ofNullable(repository.findByApiAndTenantRealm(apiExternalId, tenantRealm))
                .orElseThrow(ApiGatewayTenantNotFoundException::new);
        return toResponse(apiExternalId, apiGatewayTenant);
    }

    private ApiGatewayTenantResponse toResponse(final String apiExternalId, final ApiGatewayTenant apiGatewayTenant) {
        final List<Plan> plans = listPlansByApiType(apiExternalId, apiGatewayTenant);

        final ApiGatewayTenantResponse response = new ApiGatewayTenantResponse(apiGatewayTenant);

        plans.forEach(plan -> plan.setPlansEndpoint(planEndpointRepository.findByPlanIdAndRemovedAtIsNull(plan.getId())));

        response.setPlans(plans.stream().map(PlanResponse::new).collect(Collectors.toList()));
        return response;
    }

    private List<Plan> listPlansByApiType(String apiId, ApiGatewayTenant apiGatewayTenant) {
    	Long apiGatewayTenantId = apiGatewayTenant.getId();
		if (apiId == null) { // chamado pelo gateway
			Collection<PlanModality> planModalities = PlanModality.values();
			PlanModality[] modalities = planModalities.toArray(new PlanModality[planModalities.size()]);
			return planRepository.findByApiGatewayTenantIdAndModalityInAndRemovedAtIsNullOrderByPriority(
					apiGatewayTenantId, modalities);
		} else {

			if (isSandbox(apiId, apiGatewayTenant.getApiGateway())) {
				return planRepository.findByApiGatewayTenantIdAndModalityInAndRemovedAtIsNullOrderByPriority(
						apiGatewayTenantId, PlanModality.SANDBOX);
			} else {
				Collection<PlanModality> planModalities = PlanModality.values(PlanModality.SANDBOX);
				PlanModality[] modalities = planModalities.toArray(new PlanModality[planModalities.size()]);
				return planRepository.findByApiGatewayTenantIdAndModalityInAndRemovedAtIsNullOrderByPriority(
						apiGatewayTenantId, modalities);
			}
		}
    }

    private boolean isSandbox(String apiId, ApiGateway apiGateway) {
        String externalId = apiGateway.getExternalId();
        String productionExternalId = apiGateway.getProductionExternalId();
        return apiId != null && apiId.equals(externalId) && !externalId.equals(productionExternalId);
    }

    public ApiGatewayTenantResponse find(final String uuid) throws ApiGatewayTenantNotFoundException {
        return toResponse(null, get(uuid));
    }

    private ApiGatewayTenant findNotRemoved(final String uuid) throws ApiGatewayTenantNotFoundException {
        return ofNullable(repository.findByUuidAndRemovedAtIsNull(uuid))
                .orElseThrow(ApiGatewayTenantNotFoundException::new);
    }

	public Set<String> findPaymentMethodsByApiAndGatewayAndTenantRealm(String apiExternalId,
			String tenantRealm) throws ApiGatewayTenantNotFoundException {
        final ApiGatewayTenant apiGatewayTenant = ofNullable(repository.findByApiAndTenantRealm(apiExternalId, tenantRealm))
                .orElseThrow(ApiGatewayTenantNotFoundException::new);
        return toResponse(apiExternalId, apiGatewayTenant).getPaymentMethods();
	}

}
