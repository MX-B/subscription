package io.gr1d.portal.subscriptions.request;

import io.gr1d.portal.subscriptions.response.PlanEndpointRangeResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

@ToString
@Getter@Setter
public class PlanEndpointRangeRequest {

    @NotEmpty
    private Long initRange;

    private Long finalRange;

    @NotEmpty
    private Long value;

    static PlanEndpointRangeRequest fromPlanResponse(final PlanEndpointRangeResponse response) {
        final PlanEndpointRangeRequest planEndpointRequest = new PlanEndpointRangeRequest();
        planEndpointRequest.setInitRange(response.getInitRange());
        planEndpointRequest.setFinalRange(response.getFinalRange());
        planEndpointRequest.setValue(response.getValue());
        return planEndpointRequest;
    }
}
