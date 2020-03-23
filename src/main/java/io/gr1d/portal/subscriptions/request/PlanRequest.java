package io.gr1d.portal.subscriptions.request;

import io.gr1d.portal.subscriptions.config.ValidPlan;
import io.gr1d.portal.subscriptions.response.PlanResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@Getter@Setter
@ValidPlan
public class PlanRequest {

    private boolean dirty;

    private String uuid;

    @NotEmpty
    private String modality;
    private String name;
    private String description;
    private Long value;

    @Valid
    private List<PlanEndpointRequest> planEndpoints;

    public static PlanRequest fromPlanResponse(final PlanResponse response) {
        final PlanRequest plan = new PlanRequest();
        plan.setUuid(response.getUuid());
        plan.setName(response.getName());
        plan.setDescription(response.getDescription());
        plan.setModality(response.getModality());
        plan.setValue(response.getValue());
        plan.setPlanEndpoints(response.getPlanEndpoints().stream().map(PlanEndpointRequest::fromPlanResponse).collect(Collectors.toList()));
        return plan;
    }
}
