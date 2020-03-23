package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Subscription;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import static java.util.Optional.ofNullable;

@Data
public class SubscriptionResponse {

    private String uuid;
    private ApiGatewayResponse api;
    private String userId;
    private PlanResponse plan;
    private TenantResponse tenant;
    private BigDecimal percentageTenantSplit;

    private LocalDate subscriptionStart;
    private LocalDate subscriptionEnd;

    private NextSubscriptionResponse nextSubscription;

    public SubscriptionResponse(final Subscription subscription) {
        this.uuid = subscription.getUuid();
        this.userId = subscription.getUserId();
        this.subscriptionStart = subscription.getSubscriptionStart();
        this.subscriptionEnd = subscription.getSubscriptionEnd();

        this.plan = new PlanResponse(subscription.getPlan());
        this.tenant = new TenantResponse(subscription.getPlan().getApiGatewayTenant().getTenant());
        this.api = new ApiGatewayResponse(subscription.getPlan().getApiGatewayTenant().getApiGateway());

        this.percentageTenantSplit = subscription.getPlan().getApiGatewayTenant().getPercentageSplit();

        ofNullable(subscription.getNextSubscription()).ifPresent(this::setNextSubscription);
    }

    private void setNextSubscription(final Subscription subscription) {
        this.nextSubscription = new NextSubscriptionResponse(subscription);
    }

}
