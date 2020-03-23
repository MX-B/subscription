package io.gr1d.portal.subscriptions.request;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter@Setter
public class SubscriptionRequest {

    @NotEmpty
    private String userId;

    @NotEmpty
    private String plan;
    
    @NotEmpty
    @JsonProperty("payment_method")
    private String paymentMethod;

    @ApiModelProperty(hidden = true)
    private String tenantRealm;

    @ApiModelProperty(hidden = true)
    private String gatewayExternalId;

    @ApiModelProperty(hidden = true)
    private String apiExternalId;

}
