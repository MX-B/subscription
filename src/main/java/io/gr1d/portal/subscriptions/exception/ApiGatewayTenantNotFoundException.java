package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class ApiGatewayTenantNotFoundException extends Gr1dNotFoundException {

    public ApiGatewayTenantNotFoundException() {
        super("io.gr1d.subscriptions.apiGatewayTenantNotFound");
    }

}
