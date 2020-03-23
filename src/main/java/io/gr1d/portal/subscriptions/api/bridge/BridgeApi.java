package io.gr1d.portal.subscriptions.api.bridge;

import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.model.Gr1dPage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import static io.gr1d.core.controller.BaseController.JSON;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@FeignClient(name = "bridgeApi", url = "${gr1d.subscription.integration.bridgeService}")
public interface BridgeApi {

    @RequestMapping(path = "/backoffice/apis/{externalId}/endpoints", method = GET, produces = { JSON })
    ApiEndpoints listEndpoints(@RequestHeader("X-Tenant") String tenantRealm, @PathVariable("externalId") String externalId);

    @RequestMapping(path = "/backoffice/apis?name={name}", method = GET, produces = { JSON })
    Gr1dPage<ApiBridge> listApisByName(@RequestHeader("X-Tenant") String tenantRealm, @PathVariable("name") String name);

    @RequestMapping(path = "/backoffice/apis?size=" + Integer.MAX_VALUE, method = GET, produces = { JSON })
    Gr1dPage<ApiBridge> listApis(@RequestHeader("X-Tenant") String tenantRealm);

    @RequestMapping(path = "/backoffice/apis/{apiId}", method = GET, produces = { JSON })
    ApiBridge getApiData(@RequestHeader("X-Tenant") String tenantRealm, @PathVariable("apiId") String apiId);

    @RequestMapping(path = "/hc", method = GET)
    void healthcheck();

    @RequestMapping(path = "/gateways/{gatewayExternalId}/apis/{apiExternalId}/plan", consumes = { JSON })
    void createPlan(@PathVariable("gatewayExternalId") String gatewayExternalId,
                    @PathVariable("apiExternalId") String apiExternalId,
                    @RequestBody CreatedResponse body);

}
