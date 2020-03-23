package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Subscription;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NextSubscriptionResponse {

    private String uuid;
    private PlanResponse plan;

    private LocalDate subscriptionStart;

    public NextSubscriptionResponse(final Subscription subscription) {
        this.uuid = subscription.getUuid();
        this.subscriptionStart = subscription.getSubscriptionStart();

        this.plan = new PlanResponse(subscription.getPlan());
    }

}
