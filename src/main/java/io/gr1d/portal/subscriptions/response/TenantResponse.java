package io.gr1d.portal.subscriptions.response;

import io.gr1d.integrations.whitelabel.model.response.V1TenantResponse;
import io.gr1d.portal.subscriptions.api.recipients.Recipient;
import io.gr1d.portal.subscriptions.model.Tenant;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResponse {

    private String uuid;
    private String realm;
    private String name;
    private String phone;
    private String logo;
    private String url;
    private String walletId;
    private String email;
    private String supportEmail;

    private Recipient wallet;

    private LocalDateTime createdAt;
    private LocalDateTime removedAt;

    public TenantResponse(final Tenant tenant) {
        this.uuid = tenant.getUuid();
        this.realm = tenant.getRealm();
        this.walletId = tenant.getWalletId();
        this.createdAt = tenant.getCreatedAt();
        this.removedAt = tenant.getRemovedAt();
    }

    public TenantResponse(final Tenant tenant, final V1TenantResponse response, final Recipient wallet) {
        this(tenant);

        this.realm = response.getRealm();
        this.name = response.getName();
        this.email = response.getEmail();
        this.url = response.getUrl();
        this.phone = response.getField(String.class, "payments", "phone");
        this.supportEmail = response.getField(String.class, "payments", "support_email");
        this.logo = response.getAsset("email", "logo_header");

        this.wallet = wallet;
    }

}
