package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.PlanEndpoint;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class PlanEndpointResponse {

    private String uuid;
    private String name;
    private String externalId;
    private String endpoint;
    private String methodType;
    private Map<String, String> metadata;
    private List<PlanEndpointRangeResponse> ranges = new ArrayList<>();

    public PlanEndpointResponse(final PlanEndpoint planEndpoint) {
        this.uuid = planEndpoint.getUuid();
        this.externalId = planEndpoint.getExternalId();
        this.name = planEndpoint.getName();
        this.endpoint = planEndpoint.getEndpoint();
        this.methodType = planEndpoint.getApiEndpoint().getMethodType().getName();
        this.metadata = planEndpoint.getMetadata();
        planEndpoint.getPlanEndpointRanges().forEach(planEndpointRange -> this.ranges.add(new PlanEndpointRangeResponse(planEndpointRange)));
    }

}
