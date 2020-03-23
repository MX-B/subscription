package io.gr1d.portal.subscriptions.service;

import feign.FeignException;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.api.bridge.ApiBridge;
import io.gr1d.portal.subscriptions.api.bridge.ApiEndpoints;
import io.gr1d.portal.subscriptions.api.bridge.BridgeApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class BridgeService {

    private final BridgeApi bridgeApi;

    @Autowired
    public BridgeService(final BridgeApi bridgeApi) {
        this.bridgeApi = bridgeApi;
    }

    ApiEndpoints listEndpoints(final String externalId) {
        try {
            return bridgeApi.listEndpoints("master", externalId);
        } catch (FeignException e) {
            log.error("Error while trying to list api endpoints", e);
            final ApiEndpoints apiEndpoints = new ApiEndpoints();
            apiEndpoints.setEndpoints(Collections.emptyList());
            return apiEndpoints;
        }
    }

    Gr1dPage<ApiBridge> listBridgeApis(final String name) {
        return bridgeApi.listApisByName("master", name);
    }

    Gr1dPage<ApiBridge> listApis() {
        return bridgeApi.listApis("master");
    }

    ApiBridge getApiData(final String externalId) {
        try {
            return bridgeApi.getApiData("master", externalId);
        } catch (FeignException e) {
            log.error("Error while trying to retrieve API data from bridge {} ", externalId, e);
        }
        return null;
    }
}
