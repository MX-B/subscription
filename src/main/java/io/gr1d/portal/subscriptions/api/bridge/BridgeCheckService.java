package io.gr1d.portal.subscriptions.api.bridge;

import feign.FeignException;
import io.gr1d.core.healthcheck.CheckService;
import io.gr1d.core.healthcheck.response.ServiceInfo;
import io.gr1d.core.healthcheck.response.enums.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BridgeCheckService implements CheckService {

    private static final String NAME = "Bridge API";

    private final BridgeApi bridgeApi;
    private final String host;

    @Autowired
    public BridgeCheckService(final BridgeApi bridgeApi,
                              @Value("${gr1d.subscription.integration.bridgeService}") final String host) {
        this.bridgeApi = bridgeApi;
        this.host = host;
    }

    @Override
    public ServiceInfo check() {
        try {
            bridgeApi.healthcheck();
            return new ServiceInfo(NAME, host, ServiceStatus.LIVE, null);
        } catch (final FeignException e) {
            return new ServiceInfo(NAME, host, ServiceStatus.DOWN, e.getMessage());
        }
    }

}
