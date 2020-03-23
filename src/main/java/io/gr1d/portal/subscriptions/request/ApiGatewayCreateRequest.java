package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.datasource.validation.Unique;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@ToString
@Getter@Setter
public class ApiGatewayCreateRequest {

    @NotEmpty
    @Size(max = 64)
    @Unique(message = "{io.gr1d.subscriptions.validation.Unique.apiGatewayMessage}",
            property = "externalId", entity = ApiGateway.class)
    private String externalId;

    @NotEmpty
    @Size(max = 64)
    @Unique(message = "{io.gr1d.subscriptions.validation.Unique.apiGatewayMessage}",
            property = "productionExternalId", entity = ApiGateway.class)
    private String productionExternalId;

    @NotEmpty
    @Size(max = 64)
    private String apiUuid;

    @NotEmpty
    @Size(max = 64)
    private String gatewayUuid;

}
