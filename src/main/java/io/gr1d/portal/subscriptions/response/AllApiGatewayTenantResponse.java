package io.gr1d.portal.subscriptions.response;

import lombok.Data;

@Data
public class AllApiGatewayTenantResponse {

    private ApiGatewayTenantResponse production;

    private ApiGatewayTenantResponse sandbox;
}
