package io.gr1d.portal.subscriptions.controller.external.v1;

import io.gr1d.core.controller.BaseController;
import io.gr1d.portal.subscriptions.exception.SubscriptionNotFoundException;
import io.gr1d.portal.subscriptions.response.SubscriptionResponse;
import io.gr1d.portal.subscriptions.service.SubscriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@Api(tags = "V1 Subscription")
@RequestMapping(path = "/v1/subscription")
public class V1SubscriptionController extends BaseController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public V1SubscriptionController(final SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @ApiOperation(value = "Get Subscription", notes = "Returns an active subscription for the API", tags = "V1 Subscription")
    @RequestMapping(path = "/tenant/{tenantRealm}/gateway/{gatewayExternalId}/api/{apiExternalId}/user/{userId}", method = GET, produces = JSON)
    public SubscriptionResponse getSubscription(@PathVariable final String tenantRealm,
                                                @PathVariable final String gatewayExternalId,
                                                @PathVariable final String apiExternalId,
                                                @PathVariable final String userId) throws SubscriptionNotFoundException {

        log.info("Requesting a Subscription for Tenant(realm_id:{}) , Gateway(external_id:{}) , API(sandbox_external_id:{})  and User {}",
                tenantRealm, gatewayExternalId, apiExternalId, userId);

        return subscriptionService.findSubscription(tenantRealm, gatewayExternalId, apiExternalId, userId, null);
    }

}
