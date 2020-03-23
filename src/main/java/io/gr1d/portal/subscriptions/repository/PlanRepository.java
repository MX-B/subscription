package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.ApiGatewayTenant;
import io.gr1d.portal.subscriptions.model.Plan;
import io.gr1d.portal.subscriptions.model.PlanModality;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanRepository extends CrudRepository<Plan, Long> {

    Optional<Plan> findByUuid(final String uuid);

    Plan findByUuidAndRemovedAtIsNull(final String uuid);

    @Query("SELECT plan FROM Plan plan " +
            "WHERE plan.pendingSync = true " +
            " AND plan.removedAt IS NULL " +
            " AND plan.apiGatewayTenant IS NOT NULL " +
            " AND plan.apiGatewayTenant.removedAt IS NULL " +
            " AND plan.type.id = 2")
    List<Plan> findPending();

    @Query("SELECT p FROM Plan p " +
            "WHERE p.removedAt IS NULL " +
            " AND p.uuid = :uuid " +
            "  AND p.apiGatewayTenant.tenant.realm = :tenantRealm" +
            "  AND ((p.apiGatewayTenant.apiGateway.externalId = :apiExternalId) OR (p.apiGatewayTenant.apiGateway.productionExternalId = :apiExternalId))" +
            "  AND p.apiGatewayTenant.apiGateway.gateway.externalId = :gatewayExternalId")
    Plan findPlan(@Param("uuid") final String uuid,
                  @Param("tenantRealm") final String tenantRealm,
                  @Param("apiExternalId") final String apiExternalId,
                  @Param("gatewayExternalId") final String gatewayExternalId);

    List<Plan> findByApiGatewayTenantIdAndModalityInAndRemovedAtIsNullOrderByPriority(Long apiGatewayTenantId, PlanModality... modalities);

    List<Plan> findByApiGatewayTenantIdAndRemovedAtIsNullOrderByPriority(Long apiGatewayTenantId);

    @Modifying
    @Query("UPDATE Plan p SET p.removedAt = :removedAt WHERE p.removedAt IS NULL AND p.apiGatewayTenant = :apiGatewayTenant AND p.uuid NOT IN :uuids")
    void removePlans(@Param("apiGatewayTenant") ApiGatewayTenant apiGatewayTenant,
                     @Param("uuids") List<String> uuids,
                     @Param("removedAt") LocalDateTime removedAt);

    @Modifying
    @Query("UPDATE Plan p SET p.removedAt = :removedAt WHERE p.removedAt IS NULL AND p.apiGatewayTenant = :apiGatewayTenant")
    void removePlans(@Param("apiGatewayTenant") ApiGatewayTenant apiGatewayTenant,
                     @Param("removedAt") LocalDateTime removedAt);

}
