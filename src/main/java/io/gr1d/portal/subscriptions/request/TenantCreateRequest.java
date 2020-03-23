package io.gr1d.portal.subscriptions.request;

import io.gr1d.core.datasource.validation.Unique;
import io.gr1d.portal.subscriptions.model.Tenant;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter@Setter
@ToString(callSuper = true)
public class TenantCreateRequest extends TenantRequest {

    @NotEmpty
    @NotNull
    @Override
    public String getLogo() {
        return super.getLogo();
    }

    @NotEmpty
    @Size(max = 64)
    @Unique(message = "{io.gr1d.subscriptions.validation.Unique.tenantMessage}",
            property = "realm", entity = Tenant.class)
    private String realm;

}
