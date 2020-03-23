package io.gr1d.portal.subscriptions.service.plan;

import io.gr1d.portal.subscriptions.service.PlanModalityStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("PlanModalityStrategy.SANDBOX")
public class SandboxPlanModalityStrategy implements PlanModalityStrategy {
	
	@Override
	public Map<String, String> validateAndProcessMetadata(final Map<String, String> metadata) {
		final Map<String, String> result = new HashMap<>(2);
		result.put("hits", validateNumeric(metadata, "hits"));
		result.put("hit_value", validateNumeric(metadata, "hit_value"));
		return result;
	}
	
}
