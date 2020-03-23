package io.gr1d.portal.subscriptions.service;

import io.gr1d.core.datasource.model.PageResult;
import io.gr1d.portal.subscriptions.exception.GatewayNotFoundException;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.repository.GatewayRepository;
import io.gr1d.portal.subscriptions.request.GatewayCreateRequest;
import io.gr1d.portal.subscriptions.request.GatewayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class GatewayService {

    private final GatewayRepository gatewayRepository;
    private final ApplicationContext applicationContext;

    @Autowired
    public GatewayService(final GatewayRepository gatewayRepository, final ApplicationContext applicationContext) {
        this.gatewayRepository = gatewayRepository;
        this.applicationContext = applicationContext;
    }

    public PageResult<Gateway> list(final Specification<Gateway> spec, final Pageable pageable) {
        return PageResult.ofPage(gatewayRepository.findAll(spec, pageable));
    }

    public Gateway find(final String uuid) throws GatewayNotFoundException {
        return gatewayRepository.findByUuid(uuid)
                .orElseThrow(GatewayNotFoundException::new);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Gateway save(final GatewayCreateRequest request) {
        final Gateway gateway = new Gateway();
        gateway.setName(request.getName());
        gateway.setExternalId(request.getExternalId());
        return gatewayRepository.save(gateway);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void update(final String uuid, final GatewayRequest request) throws GatewayNotFoundException {
        final Gateway gateway = findOrThrow(uuid);
        gateway.setName(request.getName());
        gatewayRepository.save(gateway);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void remove(final String uuid) throws GatewayNotFoundException {
        final Gateway gateway = findOrThrow(uuid);

        log.info("Removing Gateway with uuid: {}", uuid);
        gateway.setRemovedAt(LocalDateTime.now());
        gatewayRepository.save(gateway);

        final ApiGatewayService apiGatewayService = applicationContext.getBean(ApiGatewayService.class);
        apiGatewayService.removeByGateway(gateway);
    }

    public Gateway findOrThrow(final String uuid) throws GatewayNotFoundException {
        return gatewayRepository.findByUuidAndRemovedAtIsNull(uuid)
                .orElseThrow(GatewayNotFoundException::new);
    }
}
