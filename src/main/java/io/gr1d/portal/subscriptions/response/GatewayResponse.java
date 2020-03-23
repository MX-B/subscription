package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Gateway;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GatewayResponse {

    private String uuid;
    private String externalId;
    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime removedAt;

    public GatewayResponse(final Gateway gateway) {
        this.uuid = gateway.getUuid();
        this.externalId = gateway.getExternalId();
        this.name = gateway.getName();

        this.createdAt = gateway.getCreatedAt();
        this.removedAt = gateway.getRemovedAt();
    }
}
