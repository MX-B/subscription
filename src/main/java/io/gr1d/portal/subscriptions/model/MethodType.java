package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.model.BaseEnum;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "method_type")
public class MethodType extends BaseEnum {

    private static final Map<String, MethodType> map = new HashMap<>();

    public static final MethodType GET = new MethodType(1L, "GET");
    public static final MethodType POST = new MethodType(2L, "POST");
    public static final MethodType PUT = new MethodType(3L, "PUT");
    public static final MethodType DELETE = new MethodType(4L, "DELETE");
    public static final MethodType HEAD = new MethodType(5L, "HEAD");
    public static final MethodType OPTIONS = new MethodType(6L, "OPTIONS");
    public static final MethodType PATCH = new MethodType(7L, "PATCH");
    public static final MethodType TRACE = new MethodType(8L, "TRACE");
    public static final MethodType CONNECT = new MethodType(9L, "CONNECT");

    public MethodType() {
        super();
    }

    private MethodType(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
        map.put(name, this);
    }

    public static MethodType valueOf(String code) {
        return map.get(code);
    }

}