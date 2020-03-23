package io.gr1d.portal.subscriptions.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter@Setter
@ToString(exclude = "logo")
public class TenantRequest {

    private String logo;

    @NotEmpty
    private String url;

    @NotEmpty
    private String name;

    @NotEmpty
    private String walletId;

    @NotEmpty
    @Size(max = 24)
    @Pattern(regexp = "\\+\\d{3,23}")
    private String phone;

    @NotEmpty
    @Email
    @Size(max = 64)
    private String email;

    @Email
    @NotEmpty
    @Size(max = 64)
    private String supportEmail;

}
