package io.gr1d.integrations.whitelabel.model.response;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static java.util.Optional.ofNullable;

@Slf4j
@Getter@Setter
public class V1TenantResponse extends V1TenantListResponse {

    private V1TenantAssetResponse assets;
    private V1TenantFieldResponse fields;

    @SuppressWarnings("unchecked")
    public <T> T getField(final Class<T> clazz, final String namespace, final String property) {
        try {
            return (T) ofNullable(fields).map(f -> f.get(namespace)).map(n -> n.get(property)).orElse(null);
        } catch (ClassCastException e) {
            log.error("Error while trying to cast", e);
            return null;
        }
    }

    public String getAsset(final String namespace, final String property) {
        return ofNullable(assets).map(a -> a.get(namespace)).map(n -> n.get(property)).orElse(null);
    }

}
