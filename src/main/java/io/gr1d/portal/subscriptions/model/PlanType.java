package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.model.BaseEnum;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "plan_type")
public class PlanType extends BaseEnum {

    public static final PlanType BUY = new PlanType(1L, "BUY");
    public static final PlanType SELL = new PlanType(2L, "SELL");

    public PlanType() {
        super();
    }

    private PlanType(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
    }

}