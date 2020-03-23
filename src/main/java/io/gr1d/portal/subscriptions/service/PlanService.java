package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.api.bridge.BridgeApi;
import io.gr1d.portal.subscriptions.exception.InvalidModalityException;
import io.gr1d.portal.subscriptions.exception.PlanNotFoundException;
import io.gr1d.portal.subscriptions.model.*;
import io.gr1d.portal.subscriptions.repository.PlanRepository;
import io.gr1d.portal.subscriptions.request.PlanRequest;
import io.gr1d.portal.subscriptions.response.PlanResponse;
import io.gr1d.portal.subscriptions.service.plan.PlanModalityStrategyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class PlanService {

    private final BridgeApi bridgeApi;
    private final PlanRepository planRepository;
    private final PlanModalityStrategyResolver strategyResolver;
    private final PlanEndpointService planEndpointService;
    private final Boolean planSyncEnabled;

    @Autowired
    public PlanService(final BridgeApi bridgeApi, final PlanRepository planRepository,
                       final PlanModalityStrategyResolver strategyResolver,
                       final PlanEndpointService planEndpointService,
                       @Value("${gr1d.planSync.enabled}") final Boolean planSyncEnabled) {
        this.bridgeApi = bridgeApi;
        this.planRepository = planRepository;
        this.strategyResolver = strategyResolver;
        this.planEndpointService = planEndpointService;
        this.planSyncEnabled = planSyncEnabled;
    }

    public PlanResponse find(final String uuid) throws PlanNotFoundException {
        return planRepository.findByUuid(uuid)
                .map(PlanResponse::new)
                .orElseThrow(PlanNotFoundException::new);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void remove(final Plan plan) {
        plan.setRemovedAt(LocalDateTime.now());
        planRepository.save(plan);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Plan createOrUpdate(final PlanRequest planRequest, final PlanConfigurer configurer) {
        return createOrUpdate(planRequest, configurer, 0);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Plan createOrUpdate(final PlanRequest planRequest, final PlanConfigurer configurer, final int priority) {
        final Optional<Plan> previousPlan = ofNullable(planRequest.getUuid()).map(planRepository::findByUuidAndRemovedAtIsNull);
        final Plan plan = previousPlan.map(prev -> prev.clonePlan(planRequest)).orElseGet(Plan::new);
        final PlanModality modality = PlanModality.valueOf(planRequest.getModality());
        final PlanModalityStrategy strategy = resolveStrategy(planRequest.getModality(), modality);

        if (!planRequest.isDirty() && previousPlan.isPresent()) {
            previousPlan.get().setPriority(priority);
            return planRepository.save(previousPlan.get());
        }

        plan.setDescription(planRequest.getDescription());
        plan.setName(planRequest.getName());
        plan.setModality(modality);
        plan.setPriority(priority);

        if (modality.equals(PlanModality.FREE) || modality.equals(PlanModality.SANDBOX)) {
            plan.setValue(0L);
        } else {
            plan.setValue(planRequest.getValue());
        }

        if (!previousPlan.isPresent()) {
            configurer.configureNewPlan(plan);
            planRequest.getPlanEndpoints().forEach(planEndpointRequest -> planEndpointService.createOrUpdate(plan, planEndpointRequest, strategy));
            return plan;
        } else {
            final Plan prev = previousPlan.get();

            if (plan.hasChangedConfig(previousPlan.get())) {
                prev.setRemovedAt(LocalDateTime.now());
                planRepository.save(prev);
                configurer.configureNewPlan(plan);
                planRequest.getPlanEndpoints().forEach(planEndpointRequest -> planEndpointService.createOrUpdate(plan, planEndpointRequest, strategy));
                return plan;
            }

            prev.setName(plan.getName());
            prev.setDescription(plan.getDescription());
            prev.setValue(plan.getValue());
            prev.setPriority(plan.getPriority());
            prev.setUpdatedAt(LocalDateTime.now());

            planRepository.save(prev);

            planRequest.getPlanEndpoints().forEach(planEndpointRequest -> planEndpointService.createOrUpdate(prev, planEndpointRequest, strategy));

            return prev;
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void setApiGatewayTenant(final ApiGatewayTenant apiGatewayTenant, final Plan plan) {
        plan.setApiGatewayTenant(apiGatewayTenant);
        plan.setType(PlanType.SELL);
        this.createNewPlan(apiGatewayTenant.getApiGateway(), plan);
        planRepository.save(plan);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void setApi(final Api api, final Plan plan) {
        plan.setApi(api);
        plan.setType(PlanType.BUY);
        planRepository.save(plan);
    }

    private void createNewPlan(final ApiGateway apiGateway, final Plan plan) {
        plan.setPendingSync(true);
        if (planSyncEnabled) {
            try {
                bridgeApi.createPlan(apiGateway.getGateway().getExternalId(), apiGateway.getExternalId(), new CreatedResponse(plan.getUuid()));
                plan.setPendingSync(false);
            } catch (final Exception e) {
                log.error("Exception while trying to synchronize plan with Bridge", e);
            }
        }
        planRepository.save(plan);
    }

    private PlanModalityStrategy resolveStrategy(final String modalityString, final PlanModality modality) {
        return ofNullable(modality)
                .map(strategyResolver::resolve)
                .orElseThrow(() -> new InvalidModalityException("Modality " + modalityString + " is invalid"));
    }

}
