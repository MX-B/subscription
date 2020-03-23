package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class TenantNotFoundException extends Gr1dNotFoundException {

    public TenantNotFoundException() {
        super("io.gr1d.subscriptions.tenantNotFound");
    }
}
