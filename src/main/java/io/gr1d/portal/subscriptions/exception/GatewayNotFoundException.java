package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class GatewayNotFoundException extends Gr1dNotFoundException {

    public GatewayNotFoundException() {
        super("io.gr1d.subscriptions.gatewayNotFound");
    }

}
