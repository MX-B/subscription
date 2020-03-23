package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class SubscriptionNotFoundException extends Gr1dNotFoundException {

    public SubscriptionNotFoundException() {
        super("io.gr1d.subscriptions.subscriptionNotFound");
    }

}
