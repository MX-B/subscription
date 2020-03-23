package io.gr1d.portal.subscriptions.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

@ToString
@Getter@Setter
public class ApiGatewayUpdateRequest {

    @NotEmpty
    private String externalId;

    @NotEmpty
    private String productionExternalId;

}
