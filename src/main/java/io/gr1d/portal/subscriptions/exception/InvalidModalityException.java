package io.gr1d.portal.subscriptions.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidModalityException extends RuntimeException {
	
	public InvalidModalityException(final String message) {
		super(message);
	}
	
}
