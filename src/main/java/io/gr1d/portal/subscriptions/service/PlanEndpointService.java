package io.gr1d.portal.subscriptions.service;

import io.gr1d.portal.subscriptions.model.*;
import io.gr1d.portal.subscriptions.repository.ApiEndpointRepository;
import io.gr1d.portal.subscriptions.repository.PlanEndpointRepository;
import io.gr1d.portal.subscriptions.request.PlanEndpointRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
public class PlanEndpointService {

    private final PlanEndpointRepository planEndpointRepository;
    private final ApiEndpointRepository apiEndpointRepository;

    @Autowired
    public PlanEndpointService(final PlanEndpointRepository planEndpointRepository, final ApiEndpointRepository apiEndpointRepository) {
        this.planEndpointRepository = planEndpointRepository;
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Transactional(rollbackFor = Throwable.class)
    public PlanEndpoint createOrUpdate(final Plan plan, final PlanEndpointRequest planEndpointRequest, final PlanModalityStrategy strategy) {
        final Optional<PlanEndpoint> previousPlanEnpoint = ofNullable(plan.getUuid()).map(planUuid ->
            planEndpointRepository.findByApiEndpointPathAndPlanUuidAndRemovedAtIsNull(planEndpointRequest.getEndpoint(), planUuid));
        final PlanEndpoint planEndpoint = previousPlanEnpoint.map(PlanEndpoint::clonePlanEndpoint).orElseGet(PlanEndpoint::new);
        final Api api = plan.getApi() != null ? plan.getApi() : plan.getApiGatewayTenant().getApiGateway().getApi();
        final MethodType methodType = MethodType.valueOf(planEndpointRequest.getMethodType());
        final String endpoint = planEndpointRequest.getEndpoint();
        final ApiEndpoint apiEndpoint = apiEndpointRepository.findByApiAndPathAndMethodType(api,
                endpoint, methodType)
                .orElseGet(() -> createApiEndpoint(planEndpointRequest, api, methodType));

        apiEndpoint.setName(planEndpointRequest.getName());
        apiEndpointRepository.save(apiEndpoint);

        planEndpoint.setApiEndpoint(apiEndpoint);
        planEndpoint.setExternalId(planEndpointRequest.getExternalId());
        planEndpoint.setMetadata(strategy.validateAndProcessMetadata(planEndpointRequest.getMetadata()));

        if (plan.getModality().equals(PlanModality.UNLIMITED_RANGE)) {
            final List<PlanEndpointRange> ranges = planEndpointRequest.getRanges().stream()
                    .map(rangeRequest -> new PlanEndpointRange(planEndpoint, rangeRequest))
                    .collect(Collectors.toList());
            planEndpoint.setPlanEndpointRanges(ranges);
        } else {
            planEndpoint.setPlanEndpointRanges(null);
        }

        if (!previousPlanEnpoint.isPresent()) {
            planEndpoint.setPlan(plan);

            return planEndpointRepository.save(planEndpoint);
        } else {
            final PlanEndpoint previous = previousPlanEnpoint.get();

            if (planEndpoint.hasChangedConfig(previousPlanEnpoint.get())) {
                previous.setRemovedAt(LocalDateTime.now());
                planEndpointRepository.save(previous);
                planEndpoint.setPlan(plan);
                return planEndpointRepository.save(planEndpoint);
            }

            final PlanEndpoint prev = previousPlanEnpoint.get();
            prev.setExternalId(planEndpoint.getExternalId());
            prev.setApiEndpoint(planEndpoint.getApiEndpoint());
            prev.setUpdatedAt(LocalDateTime.now());
            return planEndpointRepository.save(prev);
        }
    }

    private ApiEndpoint createApiEndpoint(final PlanEndpointRequest planEndpointRequest, final Api api, final MethodType methodType) {
        return ApiEndpoint.builder()
                .methodType(methodType)
                .path(planEndpointRequest.getEndpoint())
                .api(api)
                .build();
    }

}
