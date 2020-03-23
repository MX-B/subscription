package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.Gateway;
import lombok.Getter;

@Getter
public class ApiGatewayTenantSimpleResponse {

    private final String uuid;
    private final String tenantRealm;
    private final GatewayResponse gateway;
    private final ApiListResponse api;
    private final String externalId;
    private final String productionExternalId;
    private final String type;

    public ApiGatewayTenantSimpleResponse(final String uuid,
                                          final String tenantRealm,
                                          final Gateway gateway,
                                          final Api api,
                                          final String externalId,
                                          final String productionExternalId,
                                          final String type) {
        this.uuid = uuid;
        this.tenantRealm = tenantRealm;
        this.gateway = new GatewayResponse(gateway);
        this.api = new ApiListResponse(api);
        this.externalId = externalId;
        this.productionExternalId = productionExternalId;
        this.type = type;
    }

}
