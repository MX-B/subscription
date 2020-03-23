package io.gr1d.portal.subscriptions.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Table;

import io.gr1d.core.datasource.model.BaseEnum;

@Entity
@Table(name = "payment_method")
public class PaymentMethod extends BaseEnum {

    private static final Map<String, PaymentMethod> modes = new HashMap<>();

    public static final PaymentMethod CREDIT_CARD = new PaymentMethod(1L, "CREDIT_CARD");
    public static final PaymentMethod INVOICE = new PaymentMethod(2L, "INVOICE");

    public PaymentMethod() {
        super();
    }

    private PaymentMethod(final Long id, final String name) {
        this();
        setId(id);
        setName(name);
        modes.put(name, this);
    }

    public static PaymentMethod valueOf(String code) {
        return modes.get(code);
    }

}