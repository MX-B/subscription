package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.PlanEndpointRange;
import lombok.Data;

@Data
public class PlanEndpointRangeResponse {

    private Long initRange;
    private Long finalRange;
    private Long value;

    public PlanEndpointRangeResponse(final PlanEndpointRange planEndpointRange) {
        this.initRange = planEndpointRange.getInitRange();
        this.finalRange = planEndpointRange.getFinalRange();
        this.value = planEndpointRange.getValue();
    }

}
