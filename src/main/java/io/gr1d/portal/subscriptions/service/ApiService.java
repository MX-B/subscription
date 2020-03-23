package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.exception.Gr1dConstraintException;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.api.bridge.ApiBridge;
import io.gr1d.portal.subscriptions.api.bridge.ApiEndpoints;
import io.gr1d.portal.subscriptions.exception.ApiNotFoundException;
import io.gr1d.portal.subscriptions.exception.ProviderNotFoundException;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.model.SplitMode;
import io.gr1d.portal.subscriptions.repository.ApiRepository;
import io.gr1d.portal.subscriptions.repository.CurrencyRepository;
import io.gr1d.portal.subscriptions.request.ApiCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiRequest;
import io.gr1d.portal.subscriptions.response.ApiListResponse;
import io.gr1d.portal.subscriptions.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class ApiService {

    private final BridgeService bridgeService;
    private final ApiRepository apiRepository;
    private final ProviderService providerService;
    private final PlanService planService;
    private final ApplicationContext applicationContext;
    private final CurrencyRepository currencyRepository;
    private final TagService tagService;
    private final CategoryService categoryService;

    @Autowired
    public ApiService(final BridgeService bridgeService, final ApiRepository apiRepository,
                      final ProviderService providerService, final PlanService planService,
                      final ApplicationContext applicationContext, final CurrencyRepository currencyRepository,
                      final TagService tagService, final CategoryService categoryService) {
        this.bridgeService = bridgeService;
        this.apiRepository = apiRepository;
        this.providerService = providerService;
        this.planService = planService;
        this.applicationContext = applicationContext;
        this.currencyRepository = currencyRepository;
        this.tagService = tagService;
        this.categoryService = categoryService;
    }

    public ApiEndpoints listEndpoints(final String externalId) {
        return bridgeService.listEndpoints(externalId);
    }

    public Gr1dPage<ApiBridge> listBridgeApis(final String name) {
        return bridgeService.listBridgeApis(name);
    }

    public Gr1dPage<ApiListResponse> list(final Specification<Api> spec, final Pageable pageable) {
        final Page<Api> page = apiRepository.findAll(spec, pageable);
        final List<ApiListResponse> list = page.getContent().stream()
                .map(ApiListResponse::new)
                .collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Api save(final ApiCreateRequest request) throws ProviderNotFoundException {
        final Provider provider = providerService.findNotRemoved(request.getProvider());
        final SplitMode splitMode = SplitMode.valueOf(request.getSplitMode());
        final Api api = new Api();
        final PlanConfigurer configurer = plan -> planService.setApi(api, plan);

        api.setProvider(provider);
        api.setSlug(request.getSlug());
        api.setName(request.getName());
        api.setExternalId(request.getExternalId());
        api.setSplitMode(splitMode);

        ofNullable(request.getCurrency())
                .map(currencyRepository::findByIsoCode)
                .ifPresent(api::setCurrency);

        checkSlugUnique(api, request.getSlug());
        assignTagsAndCategory(request, api);
        apiRepository.save(api);

        if (splitMode.equals(SplitMode.AUTO)) {
            request.getPlan().setDirty(true);
            api.setPlan(planService.createOrUpdate(request.getPlan(), configurer));
            apiRepository.save(api);
        }

        return api;
    }

    private void checkSlugUnique(final Api api, final String slug) {
        final Api returnedApi = apiRepository.findBySlugAndRemovedAtIsNull(slug);
        if (returnedApi != null && !returnedApi.equals(api)) {
            throw new Gr1dConstraintException("slug", "io.gr1d.subscriptions.APISlugUnique");
        }
    }

    private void assignTagsAndCategory(final ApiRequest request, final Api api) {
        if (request.getTags() != null) {
            api.setTags(request.getTags().stream()
                    .map(tagService::find)
                    .collect(Collectors.toList()));
        }
        if (request.getCategory() != null) {
            api.setCategory(categoryService.find(request.getCategory()));
        }
    }


    @Transactional(rollbackFor = Throwable.class)
    public void update(final String uuid, final ApiRequest request) throws ApiNotFoundException, ProviderNotFoundException {
        final Api api = findOrThrow(uuid);
        final String providerUuid = request.getProvider();
        final SplitMode splitMode = SplitMode.valueOf(request.getSplitMode());
        final PlanConfigurer configurer = plan -> planService.setApi(api, plan);

        checkSlugUnique(api, request.getSlug());

        if (!api.getProvider().getUuid().equals(providerUuid)) {
            final Provider provider = providerService.findNotRemoved(providerUuid);
            api.setProvider(provider);
        }

        api.setSlug(request.getSlug());
        api.setName(request.getName());
        api.setSplitMode(splitMode);

        ofNullable(request.getCurrency())
                .map(currencyRepository::findByIsoCode)
                .ifPresent(api::setCurrency);

        assignTagsAndCategory(request, api);

        if (splitMode.equals(SplitMode.AUTO)) {
            request.getPlan().setDirty(true);
            if (api.getPlan() != null) {
                request.getPlan().setUuid(api.getPlan().getUuid());
                api.setPlan(planService.createOrUpdate(request.getPlan(), configurer));
            } else {
                api.setPlan(planService.createOrUpdate(request.getPlan(), configurer));
            }
        } else if (api.getPlan() != null) {
            planService.remove(api.getPlan());
            api.setPlan(null);
        }

        apiRepository.save(api);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void remove(final String uuid) throws ApiNotFoundException {
        final Api api = findOrThrow(uuid);

        log.info("Removing Api with uuid: {}", uuid);
        api.setRemovedAt(LocalDateTime.now());
        apiRepository.save(api);

        final ApiGatewayService apiGatewayService = applicationContext.getBean(ApiGatewayService.class);
        apiGatewayService.removeByApi(api);
    }

    public Api findOrThrow(final String uuid) throws ApiNotFoundException {
        return ofNullable(apiRepository.findByUuidAndRemovedAtIsNull(uuid))
                .orElseThrow(ApiNotFoundException::new);
    }

    public ApiResponse get(final String uuid) throws ApiNotFoundException {
        final Api api = findOrThrow(uuid);
        final ApiBridge apiBridge = bridgeService.getApiData(api.getExternalId());
        return new ApiResponse(api, apiBridge);
    }

    public Gr1dPage<ApiBridge> listBridgeApisWithoutAssociation() {
        final List<String> apis = apiRepository.findByRemovedAtIsNull().stream()
                .map(Api::getExternalId)
                .collect(Collectors.toList());
        final Gr1dPage<ApiBridge> apiBridgeGr1dPage = bridgeService.listApis();
        final List<ApiBridge> apiBridgeList = apiBridgeGr1dPage.getContent().stream()
                .filter(apiBridge -> !apis.contains(apiBridge.getId()))
                .collect(Collectors.toList());

        apiBridgeGr1dPage.setContent(apiBridgeList);
        apiBridgeGr1dPage.setSize(apiBridgeList.size());
        apiBridgeGr1dPage.setTotalElements(apiBridgeList.size());

        return apiBridgeGr1dPage;
    }

    public List<ApiListResponse> listAllWithoutPlan() {
        return apiRepository.listAllWithoutPlan();
    }

}
