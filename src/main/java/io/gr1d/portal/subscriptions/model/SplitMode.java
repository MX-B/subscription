package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.model.BaseEnum;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "split_mode")
public class SplitMode extends BaseEnum {

    private static final Map<String, SplitMode> modes = new HashMap<>();

    public static final SplitMode AUTO = new SplitMode(1L, "AUTO");
    public static final SplitMode MANUAL = new SplitMode(2L, "MANUAL");

    public SplitMode() {
        super();
    }

    private SplitMode(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
        modes.put(name, this);
    }

    public static SplitMode valueOf(String code) {
        return modes.get(code);
    }

}