package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class ProviderNotFoundException extends Gr1dNotFoundException {

    public ProviderNotFoundException() {
        super("io.gr1d.subscriptions.providerNotFound");
    }

}
