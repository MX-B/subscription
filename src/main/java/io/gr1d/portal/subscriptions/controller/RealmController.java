package io.gr1d.portal.subscriptions.controller;

import io.gr1d.core.controller.BaseController;
import io.gr1d.portal.subscriptions.response.RealmResponse;
import io.gr1d.portal.subscriptions.service.KeycloakRealmService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping(path = "/realm")
public class RealmController extends BaseController {

    private final KeycloakRealmService realmService;

    @Autowired
    public RealmController(final KeycloakRealmService realmService) {
        this.realmService = realmService;
    }

    @ApiOperation(nickname = "listRealms", value = "List Realms", notes = "Returns a list with all registered Realms", tags = "Realm")
    @RequestMapping(path = "", method = GET, produces = JSON)
    public List<RealmResponse> list() {
        log.info("Listing all realms");
        return realmService.listRealms();
    }

}
