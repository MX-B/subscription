package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class ApiNotFoundException extends Gr1dNotFoundException {

    public ApiNotFoundException() {
        super("io.gr1d.subscriptions.APINotFound");
    }

}
