package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.validation.Value;
import io.gr1d.portal.subscriptions.response.PlanEndpointResponse;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ToString
@Getter@Setter
public class PlanEndpointRequest {

    private String externalId;

    @NotEmpty
    private String name;

    @NotEmpty
    private String endpoint;

    @Value(values = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT"})
    @ApiModelProperty("Default `GET`")
    private String methodType = "GET";

    private Map<String, String> metadata;

    private List<PlanEndpointRangeRequest> ranges = new ArrayList<>();

    static PlanEndpointRequest fromPlanResponse(final PlanEndpointResponse response) {
        final PlanEndpointRequest planEndpointRequest = new PlanEndpointRequest();
        planEndpointRequest.setName(response.getName());
        planEndpointRequest.setExternalId(response.getExternalId());
        planEndpointRequest.setEndpoint(response.getEndpoint());
        planEndpointRequest.setMetadata(response.getMetadata());

        response.getRanges().forEach(planEndpointRangeResponse ->
                planEndpointRequest.getRanges().add(PlanEndpointRangeRequest.fromPlanResponse(planEndpointRangeResponse)));

        return planEndpointRequest;
    }
}
