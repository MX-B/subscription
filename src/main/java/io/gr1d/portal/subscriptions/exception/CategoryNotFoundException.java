package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dHttpRuntimeException;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends Gr1dHttpRuntimeException {

    public CategoryNotFoundException() {
        super(HttpStatus.NOT_FOUND.value(), "io.gr1d.subscriptions.categoryNotFound");
    }

}
