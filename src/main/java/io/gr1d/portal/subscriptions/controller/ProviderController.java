package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.core.datasource.model.Gr1dPageable;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.exception.ProviderNotFoundException;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.request.ProviderRequest;
import io.gr1d.portal.subscriptions.response.ProviderResponse;
import io.gr1d.portal.subscriptions.service.ProviderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping(path = "/provider")
public class ProviderController extends BaseController {

    private final ProviderService providerService;

    @Autowired
    public ProviderController(final ProviderService providerService) {
        this.providerService = providerService;
    }

    @And({
            @Spec(path = "name", params = "name", spec = Like.class),
            @Spec(path = "removedAt", params = "removed", spec = NotNull.class, constVal = "false")
    })
    private interface ProviderSpec extends Specification<Provider> {
    }

    @ApiImplicitParams({ @ApiImplicitParam(name = "name ", dataType = "string", paramType = "query") })
    @ApiOperation(nickname = "listProviders", value = "List Providers", notes = "Returns a list with all registered Providers", tags = "Provider")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public Gr1dPage<ProviderResponse> list(final ProviderSpec specification, final Gr1dPageable page) {
        log.info("Listing Providers {}", page);
        return providerService.list(specification, page.toPageable());
    }

    @ApiOperation(nickname = "createProvider", value = "Create Provider", notes = "Creates a new Provider", tags = "Provider")
    @RequestMapping(path = "", method = POST, consumes = JSON)
    public ResponseEntity<CreatedResponse> create(@Valid @RequestBody final ProviderRequest request) {
        log.info("Creating Provider with request {}", request);
        final Provider provider = providerService.save(request);
        final URI uri = URI.create(String.format("/provider/%s", provider.getUuid()));
        return ResponseEntity.created(uri).body(new CreatedResponse(provider.getUuid()));
    }

    @ApiOperation(nickname = "updateProvider", value = "Update Provider", notes = "Updates an existing Provider", tags = "Provider")
    @RequestMapping(path = "/{uuid}", method = PUT, consumes = JSON)
    public void update(@PathVariable final String uuid,
                       @Valid @RequestBody final ProviderRequest request) throws ProviderNotFoundException {
        log.info("Updating Provider {} with request {}", uuid, request);
        providerService.update(uuid, request);
    }

    @ApiOperation(nickname = "removeProvider", value = "Remove Provider", notes = "Removes an existing Provider", tags = "Provider")
    @RequestMapping(path = "/{uuid}", method = DELETE)
    public void remove(@PathVariable final String uuid) throws ProviderNotFoundException {
        log.info("Removing Provider {}", uuid);
        providerService.remove(uuid);
    }

    @ApiOperation(nickname = "getProviderDetails", value = "Provider details", notes = "Returns information about a single Provider with given uuid", tags = "Provider")
    @RequestMapping(path = "/{uuid}", method = GET, produces = JSON)
    public ProviderResponse get(@PathVariable final String uuid) throws ProviderNotFoundException {
        log.info("Requesting Provider {}", uuid);
        return providerService.find(uuid);
    }

    @ApiOperation(nickname = "getProviderUUIDByProviderEmail", value = "Provider UUID By Email", notes = "Returns the Provider UUID with given email", tags = "Provider")
    @RequestMapping(path = "/getByEmail/{email}", method = GET, produces = JSON)
    public String getProviderUUIDByProviderEmail(@PathVariable final String email) throws ProviderNotFoundException {
        log.info("Requesting Provider UUID By Email {}", email);
        return providerService.findByEmail(email);
    }
}
