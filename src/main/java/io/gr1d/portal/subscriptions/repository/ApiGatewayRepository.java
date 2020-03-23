package io.gr1d.portal.subscriptions.repository;

import io.gr1d.core.datasource.repository.BaseModelRepository;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.Gateway;

import java.util.List;
import java.util.Optional;

public interface ApiGatewayRepository extends BaseModelRepository<ApiGateway> {

    long countByApiUuidAndGatewayUuidAndRemovedAtIsNull(final String apiUuid, final String gatewayUuid);

    ApiGateway findByGatewayUuidAndRemovedAtIsNullAndExternalId(final String gatewayUuid, final String externalId);

    ApiGateway findByGatewayUuidAndRemovedAtIsNullAndProductionExternalId(final String gatewayUuid, final String productionExternalId);

    List<ApiGateway> findByGateway(Gateway gateway);
    List<ApiGateway> findByApi(Api api);

    Optional<ApiGateway> findByRemovedAtIsNullAndProductionExternalId(final String productionExternalId);

    Optional<ApiGateway> findByRemovedAtIsNullAndExternalId(final String externalId);
}
