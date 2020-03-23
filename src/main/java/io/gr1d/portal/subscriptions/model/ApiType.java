package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.model.BaseEnum;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "api_type")
public class ApiType extends BaseEnum {

    private static final Map<String, ApiType> map = new HashMap<>(3);

    public static final ApiType FREE = new ApiType(1L, "FREE");
    public static final ApiType FREEMIUM = new ApiType(2L, "FREEMIUM");
    public static final ApiType PAID = new ApiType(3L, "PAID");

    public ApiType() {
        super();
    }

    private ApiType(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
        map.put(name, this);
    }

}
