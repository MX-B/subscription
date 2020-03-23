package io.gr1d.portal.subscriptions.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPropertyException extends RuntimeException {

    private final String property;

    public InvalidPropertyException(final String property, final String message) {
        super(message);
        this.property = property;
    }

}
