package io.gr1d.portal.subscriptions.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ToString {
	private static final Gson GSON;
	private static final Set<String> EXCLUDED_FIELDS = Collections.unmodifiableSet(new HashSet<>(
		Arrays.asList("pass", "password", "cardNumber", "cardExpirationDate", "cardCvv", "cardHolderName")));
	
	static {
		GSON = new GsonBuilder().serializeNulls().addSerializationExclusionStrategy(new ExclusionList()).create();
	}
	
	private ToString() {
		// NOOP
	}
	
	public static String toString(final Object obj) {
		final String objectClassName = obj.getClass().getName();
		
		try {
			final String objectAsJson = asJson(obj);
			return String.format("%s %s", objectClassName, objectAsJson);
		} catch (final Throwable e) {
			return String.format("%s [error creating json] {\"json_exception\":\"%s\",\"json_exception_message\":\"%s\"}", objectClassName, e.getClass().getName(), e.getLocalizedMessage());
		}
	}
	
	private static String asJson(final Object obj) {
		return GSON.toJson(obj);
	}
	
	private static class ExclusionList implements ExclusionStrategy {
		/** Skip fields based on their names */
		@Override
		public boolean shouldSkipField(final FieldAttributes f) {
			return EXCLUDED_FIELDS.contains(f.getName());
		}
		
		/** Do not skip any classes */
		@Override
		public boolean shouldSkipClass(final Class<?> clazz) {
			return false;
		}
	}
}
