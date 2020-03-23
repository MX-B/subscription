package io.gr1d.portal.subscriptions.service.plan;

import io.gr1d.portal.subscriptions.service.PlanModalityStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("PlanModalityStrategy.UNLIMITED_RANGE")
public class UnlimitedRangePlanModalityStrategy implements PlanModalityStrategy {

	@Override
	public Map<String, String> validateAndProcessMetadata(Map<String, String> metadata) {
		return null;
	}
}
