package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.exception.GatewayNotFoundException;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.request.GatewayCreateRequest;
import io.gr1d.portal.subscriptions.request.GatewayRequest;
import io.gr1d.portal.subscriptions.service.GatewayService;
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
@RequestMapping(path = "/gateway")
public class GatewayController extends BaseController {

    private final GatewayService gatewayService;

    public GatewayController(final GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "externalId", params = "external_id", spec = Equal.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface GatewaySpec extends Specification<Gateway> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "external_id", dataType = "string", paramType = "query")
    })
    @ApiOperation(nickname = "listGateways", value = "List Gateways", notes = "Returns a list with all registered Gateways", tags = "Gateway")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public PageResult<Gateway> list(final GatewaySpec specification, final Gr1dPageable page) {
        log.info("Listing Gateways {}", page);
        return gatewayService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "createGateway", value = "Create Gateway", notes = "Creates a new Gateway", tags = "Gateway")
    @RequestMapping(path = "", method = POST, consumes = JSON, produces = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final GatewayCreateRequest request) {
        log.info("Creating Gateway with request {}", request);
        final Gateway gateway = gatewayService.save(request);
        final URI uri = URI.create(String.format("/gateway/%s", gateway.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(gateway.getUuid()));
    }

    @ApiOperation(nickname = "updateGateway", value = "Update Gateway", notes = "Updates an existing Gateway", tags = "Gateway")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid,
                       @Valid @RequestBody final GatewayRequest request) throws GatewayNotFoundException {
        log.info("Updating Gateway {} with request {}", uuid, request);
        gatewayService.update(uuid, request);
    }

    @ApiOperation(nickname = "removeGateway", value = "Remove Gateway", notes = "Removes an existing Gateway", tags = "Gateway")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws GatewayNotFoundException {
        log.info("Removing Gateway {}", uuid);
        gatewayService.remove(uuid);
    }

    @ApiOperation(nickname = "getGatewayDetails", value = "Gateway details", notes = "Returns information about a single Gateway with given uuid", tags = "Gateway")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public Gateway get(@PathVariable final String uuid) throws GatewayNotFoundException {
        log.info("Requesting Gateway {}", uuid);
        return gatewayService.find(uuid);
    }

}
