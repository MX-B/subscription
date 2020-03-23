package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.ApiGateway;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiGatewayResponse {

    private String uuid;

    private ApiListResponse api;
    private String apiUuid;
    private PlanResponse apiPlan;

    private String name;
    private String externalId;
    private String productionExternalId;
    private String splitMode;

    private GatewayResponse gateway;
    private ProviderResponse provider;

    private LocalDateTime createdAt;
    private LocalDateTime removedAt;

    public ApiGatewayResponse(final ApiGateway apiGateway) {
        this.uuid = apiGateway.getUuid();
        this.api = new ApiListResponse(apiGateway.getApi());
        this.apiUuid = apiGateway.getApi().getUuid();
        this.name = apiGateway.getApi().getName();
        this.externalId = apiGateway.getExternalId();
        this.productionExternalId = apiGateway.getProductionExternalId();
        this.gateway = new GatewayResponse(apiGateway.getGateway());
        this.provider = new ProviderResponse(apiGateway.getApi().getProvider());
        this.splitMode = apiGateway.getApi().getSplitMode().getName();
        if (apiGateway.getApi().getPlan() != null) {
            this.apiPlan = new PlanResponse(apiGateway.getApi().getPlan());
        }

        this.createdAt = apiGateway.getCreatedAt();
        this.removedAt = apiGateway.getRemovedAt();
    }

}
