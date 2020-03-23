package io.gr1d.portal.subscriptions.exception;

import io.gr1d.core.exception.Gr1dNotFoundException;

public class PlanNotFoundException extends Gr1dNotFoundException {

    public PlanNotFoundException() {
        super("io.gr1d.subscriptions.planNotFound");
    }

}
