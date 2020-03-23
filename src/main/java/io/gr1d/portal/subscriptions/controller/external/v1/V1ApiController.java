package io.gr1d.portal.subscriptions.controller.external.v1;

import io.gr1d.core.controller.BaseController;
import io.gr1d.portal.subscriptions.exception.ApiGatewayTenantNotFoundException;
import io.gr1d.portal.subscriptions.repository.ApiGatewayTenantRepository;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantResponse;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantSimpleResponse;
import io.gr1d.portal.subscriptions.service.ApiGatewayTenantService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@Api(tags = "V1 APIs")
@RequestMapping(path = "/v1")
public class V1ApiController extends BaseController {

    private final ApiGatewayTenantService apiGatewayTenantService;
    private final ApiGatewayTenantRepository repository;

    @Autowired
    public V1ApiController(final ApiGatewayTenantService apiGatewayTenantService, final ApiGatewayTenantRepository repository) {
        this.apiGatewayTenantService = apiGatewayTenantService;
        this.repository = repository;
    }

    @ApiOperation(nickname = "getAPIdetails", value = "API Details per Tenant", notes = "Returns a more detailed information about API for a Tenant")
    @RequestMapping(path = "/tenant/{tenantRealm}/api/{apiExternalId}", method = GET, produces = JSON)
    public ApiGatewayTenantResponse findByApiAndGatewayAndTenantRealm(@PathVariable final String apiExternalId,
                                                                      @PathVariable final String tenantRealm) throws ApiGatewayTenantNotFoundException {
        log.info("Requesting API by Tenant(realm:{}) and API(external_id:{})", tenantRealm, apiExternalId);
        return apiGatewayTenantService.findByApiAndGatewayAndTenantRealm(apiExternalId, tenantRealm);
    }

    @ApiOperation(nickname = "listApisByTenant", value = "List APIs per Tenant", notes = "Returns a complete list of all APIs for the given Tenant")
    @RequestMapping(path = "/tenant/{tenantRealm}/api", method = GET, produces = JSON)
    public List<ApiGatewayTenantSimpleResponse> listApisByTenant(@PathVariable final String tenantRealm) {
        log.info("Listing All APIs by Tenant(realm:{})", tenantRealm);
        return repository.findAllByTenant(tenantRealm);
    }

}
