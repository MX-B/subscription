package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.api.bridge.BridgeApi;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.repository.PlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@ConditionalOnProperty(value = "gr1d.planSync.enabled", havingValue = "true")
public class PlanSyncScheduler {

    private final BridgeApi bridgeApi;
	private final PlanRepository planRepository;

	@Autowired
    public PlanSyncScheduler(final BridgeApi bridgeApi, final PlanRepository planRepository) {
        this.bridgeApi = bridgeApi;
        this.planRepository = planRepository;
    }

    @Transactional
    @Scheduled(cron = "${gr1d.planSync.cron}")
	public void updatePendingUsers() {
		log.info("Starting schedule job to synchronize plans with bridge");
        planRepository.findPending().forEach(plan -> {
            try {
                final ApiGateway apiGateway = plan.getApiGatewayTenant().getApiGateway();
                bridgeApi.createPlan(apiGateway.getGateway().getExternalId(), apiGateway.getExternalId(), new CreatedResponse(plan.getUuid()));
                plan.setPendingSync(false);
                planRepository.save(plan);
            } catch (final Exception e) {
                log.error("Exception while trying to synchronize plan with Bridge", e);
            }
        });
	}

}
