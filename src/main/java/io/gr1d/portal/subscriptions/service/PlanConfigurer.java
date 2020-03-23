package io.gr1d.portal.subscriptions.service;

import io.gr1d.portal.subscriptions.model.Plan;

@FunctionalInterface
public interface PlanConfigurer {

    void configureNewPlan(Plan plan);

}
