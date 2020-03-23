package io.gr1d.portal.subscriptions.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

@Getter@Setter
@ToString(callSuper = true)
public class ApiGatewayTenantCreateRequest extends ApiGatewayTenantUpdateRequest {

    @NotEmpty
    private String tenantUuid;

    @NotEmpty
    private String apiGatewayUuid;

}
