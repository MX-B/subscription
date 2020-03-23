package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.validation.Value;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import javax.validation.constraints.*;

@ToString
@Getter@Setter
public class ProviderRequest {

    @NotEmpty
    private String name;

    private String walletId;

    @NotEmpty
    @Size(max = 24)
    @Pattern(regexp = "\\+\\d{3,23}")
    private String phone;

    @NotEmpty
    @Email
    @Size(max = 64)
    private String email;

    @NotEmpty
    @Value(values = { "FREE", "PAID" })
    @ApiModelProperty("Type of the provider, `FREE` or `PAID`, defaults to `PAID`")
    private String type = "PAID";

    @AssertTrue(message = "{javax.validation.constraints.NotEmpty.message}")
    private boolean isWalletId() {
        return !"PAID".equals(type) || (!StringUtils.isEmpty(walletId) && "PAID".equals(type));
    }

}
