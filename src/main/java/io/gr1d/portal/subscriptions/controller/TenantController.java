package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.exception.TenantNotFoundException;
import io.gr1d.portal.subscriptions.model.Tenant;
import io.gr1d.portal.subscriptions.request.TenantCreateRequest;
import io.gr1d.portal.subscriptions.request.TenantRequest;
import io.gr1d.portal.subscriptions.response.TenantResponse;
import io.gr1d.portal.subscriptions.service.TenantService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.domain.Like;
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
@RequestMapping(path = "/tenant")
public class TenantController extends BaseController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(final TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @And({
        @Spec(path = "name", params = "name", spec = Like.class),
        @Spec(path = "realm", params = "realm", spec = Equal.class),
        @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface TenantSpec extends Specification<Tenant> {
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "realm", dataType = "string", paramType = "query")
    })
	@ApiOperation(nickname = "listTenants", value = "List Tenants", notes = "Returns a list with all registered Tenants", tags = "Tenant")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public Gr1dPage<TenantResponse> list(final TenantSpec specification, final Gr1dPageable page) {
        log.info("Listing Tenants {}", page);
        return tenantService.list(specification, page.toPageable());
    }

	@ApiOperation(nickname = "createTenant", value = "Create Tenant", notes = "Creates a new Tenant", tags = "Tenant")
    @RequestMapping(path = "", method = POST, consumes = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final TenantCreateRequest request) {
        log.info("Creating Tenant with request {}", request);
        final Tenant tenant = tenantService.save(request);
        final URI uri = URI.create(String.format("/tenant/%s", tenant.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(tenant.getUuid()));
    }

	@ApiOperation(nickname = "updateTenant", value = "Update tenant", notes = "Updates an existing Tenant", tags = "Tenant")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid, @Valid @RequestBody final TenantRequest request) throws TenantNotFoundException {
        log.info("Updating Tenant {} with request {}", uuid, request);
        tenantService.update(uuid, request);
    }

	@ApiOperation(nickname = "removeTenant", value = "Remove Tenant", notes = "Remove an existing Tenant", tags = "Tenant")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws TenantNotFoundException {
        log.info("Removing Tenant {}", uuid);
        tenantService.remove(uuid);
    }

	@ApiOperation(nickname = "getTenantDetails", value = "Tenant details", notes = "Returns information about a single Tenant with given uuid", tags = "Tenant")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public TenantResponse get(@PathVariable final String uuid) throws TenantNotFoundException {
        log.info("Requesting Tenant {}", uuid);
        return tenantService.find(uuid);
    }

    @ApiOperation(nickname = "getTenantDetailsByRealm", value = "Tenant details", notes = "Returns information about a single Tenant with given realm_id", tags = "Tenant")
    @RequestMapping(path = "/by-realm/{realm}", method = GET, produces = JSON)
    public TenantResponse getByRealm(@PathVariable final String realm) throws TenantNotFoundException {
        log.info("Requesting Tenant by realm {}", realm);
        return tenantService.findByRealm(realm);
    }

}
