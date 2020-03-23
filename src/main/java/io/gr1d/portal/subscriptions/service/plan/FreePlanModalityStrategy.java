package io.gr1d.portal.subscriptions.service.plan;

import io.gr1d.portal.subscriptions.service.PlanModalityStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("PlanModalityStrategy.FREE")
public class FreePlanModalityStrategy implements PlanModalityStrategy {

    @Override
    public Map<String, String> validateAndProcessMetadata(final Map<String, String> metadata) {
        final Map<String, String> result = new HashMap<>(1);
        result.put("limit", validateNumeric(metadata, "limit"));
        return result;
    }

}
