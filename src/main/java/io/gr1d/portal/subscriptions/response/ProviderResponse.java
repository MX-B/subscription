package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.api.recipients.Recipient;
import io.gr1d.portal.subscriptions.model.Provider;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProviderResponse {

    private String uuid;
    private String name;
    private String phone;
    private String email;
    private String walletId;
    private String type;

    private Recipient wallet;

    private LocalDateTime createdAt;
    private LocalDateTime removedAt;

    public ProviderResponse(final Provider provider, final Recipient wallet) {
        this(provider);
        this.wallet = wallet;
    }

    public ProviderResponse(final Provider provider) {
        this.uuid = provider.getUuid();
        this.name = provider.getName();
        this.phone = provider.getPhone();
        this.email = provider.getEmail();
        this.walletId = provider.getWalletId();
        this.type = provider.getType().getName();

        this.createdAt = provider.getCreatedAt();
        this.removedAt = provider.getRemovedAt();
    }

}
