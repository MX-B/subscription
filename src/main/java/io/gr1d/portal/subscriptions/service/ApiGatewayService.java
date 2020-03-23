package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.core.exception.Gr1dConstraintException;
import io.gr1d.core.exception.Gr1dHttpException;
import io.gr1d.core.model.Gr1dPage;
import io.gr1d.portal.subscriptions.api.bridge.ApiEndpoints;
import io.gr1d.portal.subscriptions.exception.ApiGatewayNotFoundException;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.repository.ApiGatewayRepository;
import io.gr1d.portal.subscriptions.request.ApiGatewayCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayUpdateRequest;
import io.gr1d.portal.subscriptions.response.ApiGatewayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiGatewayService {

    private final BridgeService bridgeService;
    private final ApiService apiService;
    private final GatewayService gatewayService;
    private final ApiGatewayRepository repository;
    private final ApplicationContext applicationContext;

    @Autowired
    public ApiGatewayService(final BridgeService bridgeService, final ApiService apiService,
                             final GatewayService gatewayService, final ApiGatewayRepository repository,
                             final ApplicationContext applicationContext) {
        this.bridgeService = bridgeService;
        this.apiService = apiService;
        this.gatewayService = gatewayService;
        this.repository = repository;
        this.applicationContext = applicationContext;
    }

    public Gr1dPage<ApiGatewayResponse> list(final Specification<ApiGateway> spec, final Pageable pageable) {
        final Page<ApiGateway> page = repository.findAll(spec, pageable);
        final List<ApiGatewayResponse> list = page.getContent().stream()
                .map(ApiGatewayResponse::new)
                .collect(Collectors.toList());
        return PageResult.ofPage(page, list);
    }

    public ApiEndpoints listEndpoints(final String uuid) throws ApiGatewayNotFoundException {
        final ApiGatewayResponse apiGateway = find(uuid);
        return bridgeService.listEndpoints(apiGateway.getExternalId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized ApiGateway save(final ApiGatewayCreateRequest request) throws Gr1dHttpException {
        final long count = repository.countByApiUuidAndGatewayUuidAndRemovedAtIsNull(request.getApiUuid(), request.getGatewayUuid());

        if (count > 0) {
            throw new Gr1dHttpException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "io.gr1d.subscriptions.apiGatewayAlreadyExists");
        }

        if (repository.findByGatewayUuidAndRemovedAtIsNullAndExternalId(request.getGatewayUuid(), request.getExternalId()) != null) {
            throw new Gr1dConstraintException("external_id", "io.gr1d.subscriptions.externalIdAlreadyUsed");
        }

        if (repository.findByGatewayUuidAndRemovedAtIsNullAndProductionExternalId(request.getGatewayUuid(), request.getProductionExternalId()) != null) {
            throw new Gr1dConstraintException("production_external_id", "io.gr1d.subscriptions.externalIdAlreadyUsed");
        }

        final Gateway gateway = gatewayService.findOrThrow(request.getGatewayUuid());
        final Api api = apiService.findOrThrow(request.getApiUuid());
        final ApiGateway apiGateway = new ApiGateway();

        apiGateway.setApi(api);
        apiGateway.setGateway(gateway);
        apiGateway.setExternalId(request.getExternalId());
        apiGateway.setProductionExternalId(request.getProductionExternalId());

        return repository.save(apiGateway);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public synchronized void update(final String uuid, final ApiGatewayUpdateRequest request) throws Gr1dHttpException {
        final ApiGateway gateway = findNotRemoved(uuid);
        ApiGateway found = repository.findByGatewayUuidAndRemovedAtIsNullAndExternalId(
                gateway.getGateway().getUuid(), request.getExternalId());

        if (found != null && !found.equals(gateway)) {
            throw new Gr1dConstraintException("external_id", "io.gr1d.subscriptions.externalIdAlreadyUsed");
        }

        found = repository.findByGatewayUuidAndRemovedAtIsNullAndProductionExternalId(
                gateway.getGateway().getUuid(), request.getProductionExternalId());

        if (found != null && !found.equals(gateway)) {
            throw new Gr1dConstraintException("production_external_id", "io.gr1d.subscriptions.externalIdAlreadyUsed");
        }

        gateway.setExternalId(request.getExternalId());
        gateway.setProductionExternalId(request.getProductionExternalId());
        repository.save(gateway);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void removeByGateway(final Gateway gateway) {
        repository.findByGateway(gateway).forEach(this::remove);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void removeByApi(final Api api) {
        repository.findByApi(api).forEach(this::remove);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void remove(final String uuid) throws ApiGatewayNotFoundException {
        final ApiGateway gateway = findNotRemoved(uuid);
        remove(gateway);
    }

    private void remove(final ApiGateway apiGateway) {
        log.info("Removing ApiGateway with uuid: {}", apiGateway.getUuid());
        apiGateway.setRemovedAt(LocalDateTime.now());
        repository.save(apiGateway);

        final ApiGatewayTenantService apiGatewayTenantService = applicationContext.getBean(ApiGatewayTenantService.class);
        apiGatewayTenantService.removeByApiGateway(apiGateway);
    }

    ApiGateway findNotRemoved(final String uuid) throws ApiGatewayNotFoundException {
        return repository.findByUuidAndRemovedAtIsNull(uuid)
                .orElseThrow(ApiGatewayNotFoundException::new);
    }

    public ApiGatewayResponse find(final String uuid) throws ApiGatewayNotFoundException {
        return repository.findByUuid(uuid)
                .map(ApiGatewayResponse::new)
                .orElseThrow(ApiGatewayNotFoundException::new);
    }

    public ApiGatewayResponse findByProductionExternalId(String externalId) throws ApiGatewayNotFoundException {
        return repository.findByRemovedAtIsNullAndProductionExternalId(externalId)
                .map(ApiGatewayResponse::new)
                .orElseThrow(ApiGatewayNotFoundException::new);
    }

    public ApiGatewayResponse findBySandboxExternalId(String externalId) throws ApiGatewayNotFoundException {
        return repository.findByRemovedAtIsNullAndExternalId(externalId)
                .map(ApiGatewayResponse::new)
                .orElseThrow(ApiGatewayNotFoundException::new);
    }
}
