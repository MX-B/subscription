package io.gr1d.portal.subscriptions.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.exception.PlanNotFoundException;
import io.gr1d.portal.subscriptions.exception.SubscriptionNotFoundException;
import io.gr1d.portal.subscriptions.model.Subscription;
import io.gr1d.portal.subscriptions.request.SubscriptionRequest;
import io.gr1d.portal.subscriptions.request.UnsubscriptionRequest;
import io.gr1d.portal.subscriptions.response.SubscriptionResponse;
import io.gr1d.portal.subscriptions.response.UserResponse;
import io.gr1d.portal.subscriptions.service.SubscriptionService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/subscription")
public class SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionController(final SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @ApiOperation(value = "Subscribe", notes = "Performs a subscription to a API plan", tags = "Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/gateway/{gatewayExternalId}/api/{apiExternalId}/subscribe", method = POST, consumes = JSON)
    public CreatedResponse subscribe(@PathVariable final String tenantRealm,
                                     @PathVariable final String gatewayExternalId,
                                     @PathVariable final String apiExternalId,
                                     @Valid @RequestBody final SubscriptionRequest request) throws PlanNotFoundException {
        request.setTenantRealm(tenantRealm);
        request.setGatewayExternalId(gatewayExternalId);
        request.setApiExternalId(apiExternalId);
        log.info("Creating a new Subscription with request {}", request);
        final Subscription subscription = subscriptionService.subscribe(request);
        return new CreatedResponse(subscription.getUuid());
    }

    @ApiOperation(value = "Unsubscribe", notes = " Performs an unsubscription to a subscription", tags = "Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/gateway/{gatewayExternalId}/api/{apiExternalId}/unsubscribe", method = POST, consumes = JSON)
    public void unsubscribe(@PathVariable final String tenantRealm,
                            @PathVariable final String gatewayExternalId,
                            @PathVariable final String apiExternalId,
                            @Valid @RequestBody final UnsubscriptionRequest request) throws PlanNotFoundException, SubscriptionNotFoundException {
        request.setTenantRealm(tenantRealm);
        request.setGatewayExternalId(gatewayExternalId);
        request.setApiExternalId(apiExternalId);
        log.info("Unsubscribing with request {}", request);
        subscriptionService.unsubscribe(request);
    }

    @ApiOperation(value = "Get Subscription", notes = "Returns an active subscription for the API", tags = "Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/gateway/{gatewayExternalId}/api/{apiExternalId}/user/{userId}", method = GET, produces = JSON)
    public SubscriptionResponse getSubscription(@PathVariable final String tenantRealm,
                                                @PathVariable final String gatewayExternalId,
                                                @PathVariable final String apiExternalId,
                                                @PathVariable final String userId,
                                                @RequestHeader(value = "X-Date", required = false) final String dateStr)
            throws SubscriptionNotFoundException {

        log.info("Requesting a Subscription for Tenant(realm_id:{}) , Gateway(external_id:{}) , API(production_external_id:{})  and User {}, date {}",
                tenantRealm, gatewayExternalId, apiExternalId, userId, dateStr);

        final LocalDate date = Optional.ofNullable(dateStr)
                .map(dt -> LocalDate.parse(dt, DateTimeFormatter.ISO_DATE))
                .orElse(null);

        return subscriptionService.findSubscription(tenantRealm, gatewayExternalId, apiExternalId, userId, date);
    }

    @ApiOperation(value = "List Subscriptions", notes = "Lists all subscriptions for the given user", tags = "Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/user/{userId}", method = GET, produces = JSON)
    public PageResult<SubscriptionResponse> listSubscriptionsForUser(@PathVariable final String tenantRealm,
                                                                     @PathVariable final String userId,
                                                                     final Gr1dPageable page) {
        log.info("Requesting list of Subscriptions for User {}/{} {}", tenantRealm, userId, page);
        return subscriptionService.listSubscriptionsByUser(tenantRealm, userId, page.toPageable());
    }

    @ApiOperation(value = "List Subscriptions", notes = "Lists all subscriptions for the given user", tags = "Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/user/{userId}/plans", method = GET, produces = JSON)
    public Iterable<SubscriptionResponse> listSubscriptionsForUserAndModalities(@PathVariable final String tenantRealm,
                                                                     @PathVariable final String userId,
                                                                     @RequestParam(name = "modalities", required = false) String[] modalities) {
        log.info("Requesting list of Subscriptions for User {}/{} {}", tenantRealm, userId, modalities);
        return subscriptionService.listSubscriptionsByUserAndModalitiesIn(tenantRealm, userId, modalities);
    }
    
    @ApiOperation(value = "List the users that have subscriptions", notes = "Lists all users that have subscriptions", tags = "Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/users", method = GET, produces = JSON)
    public List<UserResponse> listUsersSubscriptions(@PathVariable final String tenantRealm) {
        log.info("Requesting list users that have subscriptions {}", tenantRealm);
        return subscriptionService.listUsersSubscriptions(tenantRealm);
    }

}
