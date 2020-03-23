package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.ApiGatewayTenant;
import io.gr1d.portal.subscriptions.model.Tenant;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantSimpleResponse;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApiGatewayTenantRepository extends CrudRepository<ApiGatewayTenant, Long>, JpaSpecificationExecutor<ApiGatewayTenant> {

    ApiGatewayTenant findByUuid(final String uuid);

    @Query("SELECT COUNT(api) FROM ApiGatewayTenant agt " +
            "JOIN agt.apiGateway.api api " +
            "JOIN agt.tenant tenant " +
            "WHERE agt.removedAt IS NULL " +
            "  AND api.id = :apiId " +
            "  AND tenant.id = :tenantId")
    long countByApiAndTenant(@Param("apiId") Long apiId, @Param("tenantId") Long tenantId);

    @Query("SELECT agt FROM ApiGatewayTenant agt " +
           "WHERE agt.removedAt IS NULL " +
           "  AND agt.apiGateway.removedAt IS NULL" +
           "  AND agt.apiGateway.api.removedAt IS NULL " +
           "  AND agt.apiGateway.gateway.removedAt IS NULL " +
           "  AND (agt.apiGateway.externalId = :apiExternalId OR agt.apiGateway.productionExternalId = :apiExternalId)" +
           "  AND agt.tenant.realm = :tenantRealm")
    ApiGatewayTenant findByApiAndTenantRealm(@Param("apiExternalId") String apiExternalId,
                                             @Param("tenantRealm") String tenantRealm);

    ApiGatewayTenant findByUuidAndRemovedAtIsNull(final String uuid);

    @Query("SELECT new io.gr1d.portal.subscriptions.response.ApiGatewayTenantSimpleResponse(" +
                "agt.uuid, " +
                "agt.tenant.realm, " +
                "agt.apiGateway.gateway, " +
                "agt.apiGateway.api, " +
                "agt.apiGateway.externalId, " +
                "agt.apiGateway.productionExternalId, " +
                "agt.type.name) " +
           "FROM ApiGatewayTenant agt " +
           "WHERE agt.removedAt IS NULL " +
           "  AND agt.apiGateway.removedAt IS NULL" +
           "  AND agt.apiGateway.api.removedAt IS NULL " +
           "  AND agt.apiGateway.gateway.removedAt IS NULL " +
           "  AND agt.tenant.realm = :tenantRealm")
    List<ApiGatewayTenantSimpleResponse> findAllByTenant(@Param("tenantRealm") String tenantRealm);

    List<ApiGatewayTenant> findByTenant(Tenant tenant);
    List<ApiGatewayTenant> findByApiGateway(ApiGateway apiGateway);

}
