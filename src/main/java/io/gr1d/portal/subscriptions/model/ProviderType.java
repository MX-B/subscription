package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.model.BaseEnum;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "provider_type")
public class ProviderType extends BaseEnum {

    private static final Map<String, ProviderType> map = new HashMap<>(2);

    public static final ProviderType FREE = new ProviderType(1L, "FREE");
    public static final ProviderType PAID = new ProviderType(2L, "PAID");

    public ProviderType() {
        super();
    }

    private ProviderType(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
        map.put(name, this);
    }

    public static ProviderType valueOf(final String name) {
        return map.get(name);
    }

}
