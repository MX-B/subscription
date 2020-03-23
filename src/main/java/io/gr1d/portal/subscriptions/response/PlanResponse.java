package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Plan;
import io.gr1d.portal.subscriptions.request.PlanRequest;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class PlanResponse {

    private String uuid;
    private String modality;
    private String name;
    private String description;
    private Long value;
    private List<PlanEndpointResponse> planEndpoints;
    private Map<String, String> metadata;

    public PlanResponse(final Plan plan) {
        this.uuid = plan.getUuid();
        this.modality = plan.getModality().getCode();
        this.name = plan.getName();
        this.description = plan.getDescription();
        this.value = plan.getValue();
        this.planEndpoints = plan.getPlansEndpoint().stream().map(PlanEndpointResponse::new).collect(Collectors.toList());

        // TODO temporariamente enquanto não há mudanças no IC
        if (!this.planEndpoints.isEmpty()) {
            metadata = new HashMap<>(this.planEndpoints.get(0).getMetadata());
        } else {
            metadata = new HashMap<>();
        }
        if (value != null) {
            metadata.put("value", String.valueOf(value));
        }
    }

    public PlanRequest toRequest() {
        return PlanRequest.fromPlanResponse(this);
    }

}
