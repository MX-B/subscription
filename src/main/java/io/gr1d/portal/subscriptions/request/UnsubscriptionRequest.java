package io.gr1d.portal.subscriptions.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

@ToString
@Getter@Setter
public class UnsubscriptionRequest {

    @NotEmpty
    private String userId;

    private String plan;

    @ApiModelProperty(hidden = true)
    private String tenantRealm;

    @ApiModelProperty(hidden = true)
    private String gatewayExternalId;

    @ApiModelProperty(hidden = true)
    private String apiExternalId;

}
