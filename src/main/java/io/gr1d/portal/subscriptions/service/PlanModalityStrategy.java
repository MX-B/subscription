package io.gr1d.portal.subscriptions.service;

import io.gr1d.portal.subscriptions.exception.InvalidPropertyException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public interface PlanModalityStrategy {
	
	Map<String, String> validateAndProcessMetadata(Map<String, String> metadata);
	
	default String validateNumeric(final Map<String, String> metadata, final String property) {
		final String number = Optional.ofNullable(metadata.get(property))
				.orElseThrow(() -> new InvalidPropertyException(property, "{javax.validation.constraints.NotEmpty.message}"));
		if (!StringUtils.isNumeric(number)) {
			throw new InvalidPropertyException(property, "{io.gr1d.subscriptions.validation.number.message}");
		}
		if (number.length() > 20) {
			throw new InvalidPropertyException(property, "{io.gr1d.subscriptions.validation.maxDigits.message}");
		}
		return number;
	}
	
}
