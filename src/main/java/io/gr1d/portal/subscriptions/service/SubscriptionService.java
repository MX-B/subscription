package io.gr1d.portal.subscriptions.service;

import static java.util.Optional.ofNullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.service.Gr1dClock;
import io.gr1d.portal.subscriptions.exception.PlanNotFoundException;
import io.gr1d.portal.subscriptions.exception.SubscriptionNotFoundException;
import io.gr1d.portal.subscriptions.model.Plan;
import io.gr1d.portal.subscriptions.model.PlanModality;
import io.gr1d.portal.subscriptions.model.Subscription;
import io.gr1d.portal.subscriptions.repository.PlanRepository;
import io.gr1d.portal.subscriptions.repository.SubscriptionRepository;
import io.gr1d.portal.subscriptions.request.SubscriptionRequest;
import io.gr1d.portal.subscriptions.request.UnsubscriptionRequest;
import io.gr1d.portal.subscriptions.response.SubscriptionResponse;
import io.gr1d.portal.subscriptions.response.UserResponse;

@Service
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final Gr1dClock clock;

    @Autowired
    public SubscriptionService(final PlanRepository planRepository,
                               final SubscriptionRepository subscriptionRepository,
                               final Gr1dClock clock) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    public Subscription subscribe(final SubscriptionRequest request) throws PlanNotFoundException {
        final Plan plan = ofNullable(planRepository.findPlan(request.getPlan(), request.getTenantRealm(),
                request.getApiExternalId(), request.getGatewayExternalId()))
                .orElseThrow(PlanNotFoundException::new);

        final LocalDate now = LocalDate.now(clock);
        final LocalDate nextMonth = now.withDayOfMonth(1).plusMonths(1);
        final Optional<Subscription> currentSubscription = subscriptionRepository
                .findSubscription(request.getUserId(), plan.getApiGatewayTenant(), now);

        if (currentSubscription.isPresent()) {
            final Subscription current = currentSubscription.get();
            if (current.getPlan().getUuid().equals(request.getPlan())) {
                if (current.getNextSubscription() != null) {
                    reactivateCurrentPlan(current);
                }
                current.setPaymentMethod(request.getPaymentMethod());
                return current;
            }
        }

        // if we already have an active subscription, we have to schedule this to the next month
        final LocalDate start = currentSubscription.isPresent() ? nextMonth : now.withDayOfMonth(1);
        final Subscription subscription = new Subscription();
        subscription.setSubscriptionStart(start);
        subscription.setPlan(plan);
        subscription.setUserId(request.getUserId());
        subscription.setPaymentMethod(request.getPaymentMethod());
        final Subscription createdSubscription = subscriptionRepository.save(subscription);

        currentSubscription.ifPresent(current -> {
            if (current.getNextSubscription() != null) {
                // if subscription has an end, then we already have an upcoming subscription
                // we have to remove it because this subscription never started
                current.getNextSubscription().setRemovedAt(LocalDateTime.now(clock));
                subscriptionRepository.save(current.getNextSubscription());
            } else {
                current.setSubscriptionEnd(nextMonth.minusDays(1));
            }
            current.setNextSubscription(createdSubscription);
            subscriptionRepository.save(current);
        });

        return createdSubscription;
    }

    private void reactivateCurrentPlan(final Subscription current) {
        current.getNextSubscription().setRemovedAt(LocalDateTime.now(clock));
        subscriptionRepository.save(current.getNextSubscription());

        current.setNextSubscription(null);
        current.setSubscriptionEnd(null);
        subscriptionRepository.save(current);
    }

    public void unsubscribe(final UnsubscriptionRequest request) throws PlanNotFoundException, SubscriptionNotFoundException {
        final Plan plan = request.getPlan() == null
                ? null
                : planRepository.findByUuid(request.getPlan()).orElseThrow(PlanNotFoundException::new);
        final LocalDate now = LocalDate.now(clock);
        final Optional<Subscription> sub = subscriptionRepository
                .findByUserAndApiExternalId(request.getTenantRealm(), request.getGatewayExternalId(),
                        request.getApiExternalId(), request.getUserId(), now);
        if (sub.isPresent()) {
            final Subscription subscription = sub.get();
            if (plan != null) {
                if (subscription.getNextSubscription() != null && plan.equals(subscription.getNextSubscription().getPlan())) {
                    // trying to cancel the next subscription then
                    reactivateCurrentPlan(subscription);
                } else if (plan.equals(subscription.getPlan())) {
                    // trying to cancel the current subscription
                    subscription.setSubscriptionEnd(now.withDayOfMonth(1).plusMonths(1).minusDays(1));
                    subscriptionRepository.save(subscription);
                } else {
                    // the plan did not match any subscription
                    throw new SubscriptionNotFoundException();
                }
            } else {
                // no plan provided, so the user wants to cancel any subscriptions for the API
                if (subscription.getNextSubscription() != null) {
                    subscription.getNextSubscription().setRemovedAt(LocalDateTime.now(clock));
                    subscriptionRepository.save(subscription.getNextSubscription());
                }
                subscription.setNextSubscription(null);
                subscription.setSubscriptionEnd(now.withDayOfMonth(1).plusMonths(1).minusDays(1));
                subscriptionRepository.save(subscription);
            }
        } else {
            throw new SubscriptionNotFoundException();
        }
    }

    public SubscriptionResponse findSubscription(final String tenantRealm, final String gatewayExternalId,
            final String apiExternalId, final String userId, final LocalDate date) throws SubscriptionNotFoundException {
        final LocalDate now = Optional.ofNullable(date).orElse(LocalDate.now(clock));
        final Optional<Subscription> subscription = subscriptionRepository.findByUserAndApiExternalId(tenantRealm, gatewayExternalId, apiExternalId, userId, now);
        return subscription
                .map(SubscriptionResponse::new)
                .orElseThrow(SubscriptionNotFoundException::new);
    }

    public PageResult<SubscriptionResponse> listSubscriptionsByUser(final String tenantRealm,
            final String userId, final Pageable pageable) {
        final LocalDate now = LocalDate.now(clock);
        final Page<Subscription> page = subscriptionRepository.listSubscriptions(tenantRealm,
                userId, now, pageable);
        final List<SubscriptionResponse> list = page.getContent().stream()
                .map(SubscriptionResponse::new)
                .collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    @Transactional(readOnly = true)
    public Iterable<SubscriptionResponse> listSubscriptionsByUserAndModalitiesIn(final String tenantRealm,
            final String userId, String...modalities) {

        Collection<PlanModality> values = PlanModality.values();
        String[] codes = modalities == null ? values.toArray(new String[values.size()]) : modalities;
        LocalDate now = LocalDate.now(clock);
        Stream<Subscription> subscriptions = subscriptionRepository.listSubscriptions(tenantRealm, userId, now, Arrays.asList(codes));
        return subscriptions.map(SubscriptionResponse::new).collect(Collectors.toList());
    }

    public List<UserResponse> listUsersSubscriptions(String tenantRealm) {
      return subscriptionRepository.listUsersSubscriptions(tenantRealm);
    }

}
