package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.Currency;
import io.gr1d.portal.subscriptions.model.Provider;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Getter
public class ApiListResponse {

    private final String uuid;
    private final String slug;
    private final String name;
    private final String externalId;
    private final ProviderResponse provider;
    private final String splitMode;
    private final CurrencyResponse currency;
    private final CategoryResponse category;

    private final LocalDateTime createdAt;
    private final LocalDateTime removedAt;

    public ApiListResponse(final Api api) {
        uuid = api.getUuid();
        slug = api.getSlug();
        currency = ofNullable(api.getCurrency()).map(CurrencyResponse::new).orElse(null);
        category = ofNullable(api.getCategory()).map(CategoryResponse::new).orElse(null);
        name = api.getName();
        externalId = api.getExternalId();
        provider = new ProviderResponse(api.getProvider());
        splitMode = api.getSplitMode().getName();
        createdAt = api.getCreatedAt();
        removedAt = api.getRemovedAt();
    }

}
