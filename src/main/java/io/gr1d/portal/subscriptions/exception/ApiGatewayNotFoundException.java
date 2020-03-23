package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class ApiGatewayNotFoundException extends Gr1dNotFoundException {

    public ApiGatewayNotFoundException() {
        super("io.gr1d.subscriptions.apiGatewayNotFound");
    }

}
