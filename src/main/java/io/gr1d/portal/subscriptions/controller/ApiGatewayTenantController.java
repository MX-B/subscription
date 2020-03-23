package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.exception.Gr1dHttpException;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.exception.ApiGatewayTenantNotFoundException;
import io.gr1d.portal.subscriptions.model.ApiGatewayTenant;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantUpdateRequest;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantResponse;
import io.gr1d.portal.subscriptions.service.ApiGatewayTenantService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@RestController
@RequestMapping(path = "/apiGatewayTenant")
public class ApiGatewayTenantController extends BaseController {

    private final ApiGatewayTenantService service;

    @Autowired
    public ApiGatewayTenantController(final ApiGatewayTenantService service) {
        this.service = service;
    }

    @And({
            @Spec(path = "tenant.uuid", params = "tenant", spec = Equal.class),
            @Spec(path = "tenant.realm", params = "tenant_realm", spec = Equal.class),
            @Spec(path = "apiGateway.externalId", params = "api_external_id", spec = Equal.class),
            @Spec(path = "apiGateway.uuid", params = "api_gateway", spec = Equal.class),
            @Spec(path = "apiGateway.gateway.uuid", params = "gateway", spec = Equal.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface ApiGatewayTenantSpec extends Specification<ApiGatewayTenant> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "tenant", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "tenant_realm", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "api_external_id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "api_gateway", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "gateway", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listAPIGatewayTenant", value = "List API Gateway Tenant", notes = "Returns a list with all registered Gateways paired with API", tags = "Tenant -> API")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<ApiGatewayTenantResponse> list(final ApiGatewayTenantSpec specification, final Gr1dPageable page) {
        log.info("Listing API/Gateway/Tenant {}", page);
        return service.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "createAPIGatewayTenant", value = "Create API Gateway", notes = "Creates a new Gateway and API pair", tags = "Tenant -> API")
    @RequestMapping(path = "", method = POST, consumes = JSON, produces = JSON)
    public synchronized ResponseEntity<CreatedResponse> create(@Valid @RequestBody final ApiGatewayTenantCreateRequest request) throws Gr1dHttpException {
        log.info("Creating API/Gateway/Tenant with request {}", request);
        final ApiGatewayTenant gateway = service.save(request);
        final URI uri = URI.create(String.format("/apiGatewayTenant/%s", gateway.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(gateway.getUuid()));
    }

    @ApiOperation(nickname = "updateAPIGatewayTenant", value = "Update Gateway", notes = "Updates an existing Gateway and API pair", tags = "Tenant -> API")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid,
                       @Valid @RequestBody final ApiGatewayTenantUpdateRequest request) throws ApiGatewayTenantNotFoundException {
        log.info("Updating API/Gateway/Tenant {} with request {}", uuid, request);
        service.update(uuid, request);
    }

    @ApiOperation(nickname = "removeAPIGatewayTenant", value = "Remove Gateway", notes = "Removes an existing API and Gateway pair", tags = "Tenant -> API")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws ApiGatewayTenantNotFoundException {
        log.info("Removing API/Gateway/Tenant {}", uuid);
        service.remove(uuid);
    }

    @ApiOperation(nickname = "getAPIGatewayTenantDetails", value = "Gateway details", notes = "Returns information about a single API and Gateway pair with given uuid", tags = "Tenant -> API")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public ApiGatewayTenantResponse get(@PathVariable final String uuid) throws ApiGatewayTenantNotFoundException {
        log.info("Requesting API/Gateway/Tenant: {}", uuid);
        return service.find(uuid);
    }

}
