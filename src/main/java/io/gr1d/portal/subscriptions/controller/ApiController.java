package io.gr1d.portal.subscriptions.controller;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.api.bridge.ApiBridge;
import io.gr1d.portal.subscriptions.api.bridge.ApiEndpoints;
import io.gr1d.portal.subscriptions.exception.ApiGatewayTenantNotFoundException;
import io.gr1d.portal.subscriptions.exception.ApiNotFoundException;
import io.gr1d.portal.subscriptions.exception.ProviderNotFoundException;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.request.ApiCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiRequest;
import io.gr1d.portal.subscriptions.response.AllApiGatewayTenantResponse;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantResponse;
import io.gr1d.portal.subscriptions.response.ApiListResponse;
import io.gr1d.portal.subscriptions.response.ApiResponse;
import io.gr1d.portal.subscriptions.service.ApiGatewayTenantService;
import io.gr1d.portal.subscriptions.service.ApiService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;

@Slf4j
@RestController
@RequestMapping(path = "/api")
public class ApiController extends BaseController {

    private final ApiGatewayTenantService apiGatewayTenantService;
    private final ApiService apiService;

    @Autowired
    public ApiController(final ApiGatewayTenantService apiGatewayTenantService,
                         final ApiService apiService) {
        this.apiGatewayTenantService = apiGatewayTenantService;
        this.apiService = apiService;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "provider.uuid", params = "provider", spec = Equal.class),
            @Spec(path = "externalId", params = "external_id", spec = Equal.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface ApiSpec extends Specification<Api> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "external_id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "provider", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listAPIs", value = "List API", notes = "Returns a list with all registered APIs", tags = "API")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public Gr1dPage<ApiListResponse> list(final ApiSpec specification, final Gr1dPageable page) {
        log.info("Listing APIs {}", page);
        return apiService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "listAPIEndpoints", value = "List API Endpoints", notes = "Returns a list with all endpoints for the given External Id", tags = "API")
    @RequestMapping(path = "/{externalId}/endpoints", method = GET, produces = JSON)
    public ApiEndpoints listEndpoints(@PathVariable final String externalId) {
        log.info("Requesting API endpoints with External ID {}", externalId);
        return apiService.listEndpoints(externalId);
    }

    @ApiOperation(nickname = "createAPI", value = "Create a new API", tags = "API")
    @RequestMapping(path = "", method = POST, consumes = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final ApiCreateRequest request) throws ProviderNotFoundException {
        log.info("Creating API: {}", request);
        final Api api = apiService.save(request);
        final URI uri = URI.create(String.format("/api/%s", api.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(api.getUuid()));
    }

    @ApiOperation(nickname = "updateAPI", value = "Update an existing API", tags = "API")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid, @Valid @RequestBody final ApiRequest request)
            throws ApiNotFoundException, ProviderNotFoundException {
        log.info("Updating API {} with request {}", uuid, request);
        apiService.update(uuid, request);
    }

    @ApiOperation(nickname = "removeAPI", value = "Inactivate an existing API", tags = "API")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws ApiNotFoundException {
        log.info("Removing API {}", uuid);
        apiService.remove(uuid);
    }

    @ApiOperation(nickname = "getAPIdetails", value = "API Details", notes = "Returns a more detailed information about API with given uuid", tags = "API")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public ApiResponse get(@PathVariable final String uuid) throws ApiNotFoundException {
        log.info("Requesting API {}", uuid);
        return apiService.get(uuid);
    }

    @ApiOperation(nickname = "getAPIdetails", value = "API Details per Tenant", notes = "Returns a more detailed information about API for a Tenant", tags = "API")
    @RequestMapping(path = "/{apiExternalId}/tenant/{tenantRealm}", method = GET, produces = JSON)
    public ApiGatewayTenantResponse findByApiAndGatewayAndTenantRealm(@PathVariable final String apiExternalId,
                                                                      @PathVariable final String tenantRealm) throws ApiGatewayTenantNotFoundException {
        log.info("Requesting API/Gateway/Tenant by API(externalId:{}) and Tenant(realm:{})", apiExternalId, tenantRealm);
        return apiGatewayTenantService.findByApiAndGatewayAndTenantRealm(apiExternalId, tenantRealm);
    }

    @RequestMapping(path = "/all/{apiExternalId}/tenant/{tenantRealm}", method = GET, produces = JSON)
    public AllApiGatewayTenantResponse findAllApisByExternalIdAndTenantRealm(@PathVariable final String apiExternalId,
                                                                             @PathVariable final String tenantRealm) throws ApiGatewayTenantNotFoundException {
        log.info("Requesting API/Gateway/Tenant by API(externalId:{}) and Tenant(realm:{})", apiExternalId, tenantRealm);
        return apiGatewayTenantService.findAllApisByExternalIdAndTenantRealm(apiExternalId, tenantRealm);
    }

    @ApiImplicitParams({@ApiImplicitParam(name = "name", dataType = "string", paramType = "query")})
    @ApiOperation(nickname = "listBridgeAPIs", value = "List Bridge API", notes = "Returns a list with all registered Bridge APIs", tags = "API")
    @RequestMapping(path = "/bridge", method = GET, produces = JSON)
    public Gr1dPage<ApiBridge> listBridgeApis(final String name) {
        log.info("Listing Bridge APIs by name {}", name);
        return apiService.listBridgeApis(name);
    }

    @ApiOperation(nickname = "listBridgeAPIsWithoutAssociation", value = "List Bridge API Without Association", notes = "Returns a list with Bridge APIs not yet API association", tags = "API")
    @RequestMapping(path = "/bridgeWithoutAssociation", method = GET, produces = JSON)
    public Gr1dPage<ApiBridge> listBridgeApisWithoutAssociation() {
        log.info("Listing Bridge APIs without association");
        return apiService.listBridgeApisWithoutAssociation();
    }


    @ApiOperation(nickname = "listAPIsWithoutPlan", value = "List APIs Without Plan", notes = "Returns a list with all registered APIs still without plans", tags = "API")
    @RequestMapping(path = "/listWithoutPlan", method = GET, produces = JSON)
    public List<ApiListResponse> listWithoutPlan() {
        log.info("Listing APIs without Plan");
        return apiService.listAllWithoutPlan();
    }
    
    @ApiOperation(nickname = "getAPIPaymentMethods", value = "API Payment Methods per Tenant", notes = "Returns the list of payment methods available for an API in a specific Tenant", tags = "API")
    @RequestMapping(path = "/{apiExternalId}/tenant/{tenantRealm}/paymentMethods", method = GET, produces = JSON)
    public Set<String> findPaymentMethodsByApiAndGatewayAndTenantRealm(@PathVariable final String apiExternalId,
                                                                      @PathVariable final String tenantRealm) throws ApiGatewayTenantNotFoundException {
        log.info("Requesting payment methods for API/Gateway/Tenant by API(externalId:{}) and Tenant(realm:{})", apiExternalId, tenantRealm);
        return apiGatewayTenantService.findPaymentMethodsByApiAndGatewayAndTenantRealm(apiExternalId, tenantRealm);
    }

}
