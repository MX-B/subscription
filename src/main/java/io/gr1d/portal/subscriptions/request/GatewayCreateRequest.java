package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.datasource.validation.Unique;
import io.gr1d.portal.subscriptions.model.Gateway;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@ToString
@Getter@Setter
public class GatewayCreateRequest {

    @NotEmpty
    private String name;

    @NotEmpty
    @Size(max = 64)
    @Unique(message = "{io.gr1d.subscriptions.validation.Unique.gatewayMessage}",
            property = "externalId", entity = Gateway.class)
    private String externalId;

}
