package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.exception.Gr1dHttpException;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.api.bridge.ApiEndpoints;
import io.gr1d.portal.subscriptions.exception.ApiGatewayNotFoundException;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.request.ApiGatewayCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayUpdateRequest;
import io.gr1d.portal.subscriptions.response.ApiGatewayResponse;
import io.gr1d.portal.subscriptions.service.ApiGatewayService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
import net.kaczmarzyk.spring.data.jpa.domain.NotNull;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
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
@RequestMapping(path = "/apiGateway")
public class ApiGatewayController extends BaseController {

    private final ApiGatewayService apiGatewayService;

    public ApiGatewayController(final ApiGatewayService apiGatewayService) {
        this.apiGatewayService = apiGatewayService;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false"),
            @Spec(path = "api.uuid", params = "api", spec = Equal.class),
            @Spec(path = "externalId", params = "external_id", spec = Equal.class),
            @Spec(path = "gateway.uuid", params = "gateway", spec = Equal.class),
            @Spec(path = "gateway.externalId", params = "gateway_external_id", spec = Equal.class),
    })
    private interface ApiGatewaySpec extends Specification<ApiGateway> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "external_id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "gateway", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "gateway_external_id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "api", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listAPIGateways", value = "List API Gateways", notes = "Returns a list with all registered Gateways paired with API", tags = "API/Gateway")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public Gr1dPage<ApiGatewayResponse> list(final ApiGatewaySpec specification, final Gr1dPageable page) {
        log.info("Listing API/Gateway {}", page);
        return apiGatewayService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "listAPIEndpoints", value = "List API Endpoints", notes = "Returns a list with all endpoints for the given API", tags = "API/Gateway")
    @RequestMapping(path = "/{uuid}/endpoints", method = GET, produces = JSON)
    public ApiEndpoints listEndpoints(@PathVariable final String uuid) throws ApiGatewayNotFoundException {
        log.info("Requesting API/Gateway endpoints {}", uuid);
        return apiGatewayService.listEndpoints(uuid);
    }

    @ApiOperation(nickname = "createAPIGateway", value = "Create API Gateway", notes = "Creates a new Gateway and API pair", tags = "API/Gateway")
    @RequestMapping(path = "", method = POST, consumes = JSON, produces = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final ApiGatewayCreateRequest request) throws Gr1dHttpException {
        log.info("Creating API/Gateway with request {}", request);
        final ApiGateway gateway = apiGatewayService.save(request);
        final URI uri = URI.create(String.format("/apiGateway/%s", gateway.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(gateway.getUuid()));
    }

    @ApiOperation(nickname = "updateGateway", value = "Update Gateway", notes = "Updates an existing Gateway and API pair", tags = "API/Gateway")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid,
                       @Valid @RequestBody final ApiGatewayUpdateRequest request) throws Gr1dHttpException {
        log.info("Updating API/Gateway {} with request {}", uuid, request);
        apiGatewayService.update(uuid, request);
    }

    @ApiOperation(nickname = "removeGateway", value = "Remove Gateway", notes = "Removes an existing API and Gateway pair", tags = "API/Gateway")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws ApiGatewayNotFoundException {
        log.info("Removing API/Gateway {}", uuid);
        apiGatewayService.remove(uuid);
    }

    @ApiOperation(nickname = "getGatewayDetails", value = "Gateway details", notes = "Returns information about a single API and Gateway pair with given uuid", tags = "API/Gateway")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public ApiGatewayResponse get(@PathVariable final String uuid) throws ApiGatewayNotFoundException {
        log.info("Requesting API/Gateway {}", uuid);
        return apiGatewayService.find(uuid);
    }

    @ApiOperation(nickname = "getGatewayDetailsByProductionExternalId", value = "API Gateway by Production External Id", notes = "Returns information about a single API and Gateway pair with given production external Id", tags = "API/Gateway")
    @RequestMapping(path = "/production/{externalId}", method = GET, produces = JSON)
    public ApiGatewayResponse getByProductionExternalId(@PathVariable final String externalId) throws ApiGatewayNotFoundException {
        log.info("Requesting API/Gateway with externalId {}", externalId);
        return apiGatewayService.findByProductionExternalId(externalId);
    }

    @ApiOperation(nickname = "getGatewayDetailsBySandboxExternalId", value = "API Gateway by Sandbox External Id", notes = "Returns information about a single API and Gateway pair with given sandbox external Id", tags = "API/Gateway")
    @RequestMapping(path = "/sandbox/{externalId}", method = GET, produces = JSON)
    public ApiGatewayResponse getBySandboxExternalId(@PathVariable final String externalId) throws ApiGatewayNotFoundException {
        log.info("Requesting API/Gateway with externalId {}", externalId);
        return apiGatewayService.findBySandboxExternalId(externalId);
    }

}
