package io.gr1d.portal.subscriptions.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import io.gr1d.portal.subscriptions.model.ApiGatewayTenant;
import io.gr1d.portal.subscriptions.model.Subscription;
import io.gr1d.portal.subscriptions.response.UserResponse;

public interface SubscriptionRepository extends CrudRepository<Subscription, Long> {

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.userId = :userId " +
            "  AND s.plan.apiGatewayTenant.tenant.realm = :tenantRealm " +
            "  AND s.plan.apiGatewayTenant.apiGateway.gateway.removedAt IS NULL " +
            "  AND s.plan.apiGatewayTenant.tenant.removedAt IS NULL " +
            "  AND s.plan.apiGatewayTenant.removedAt IS NULL " +
            "  AND s.plan.apiGatewayTenant.apiGateway.removedAt IS NULL" +
            "  AND s.subscriptionStart <= :date " +
            "  AND (s.subscriptionEnd IS NULL OR s.subscriptionEnd >= :date)" +
            "  AND s.removedAt IS NULL")
    Page<Subscription> listSubscriptions(
            @Param("tenantRealm") final String tenantRealm,
            @Param("userId") final String userId,
            @Param("date") LocalDate date,
            Pageable pageable);

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.userId = :userId " +
            "  AND s.plan.modality.code in(:modalities) " +
            "  AND s.plan.apiGatewayTenant.tenant.realm = :tenantRealm " +
            "  AND s.plan.apiGatewayTenant.apiGateway.gateway.removedAt IS NULL " +
            "  AND s.plan.apiGatewayTenant.tenant.removedAt IS NULL " +
            "  AND s.plan.apiGatewayTenant.removedAt IS NULL " +
            "  AND s.plan.apiGatewayTenant.apiGateway.removedAt IS NULL" +
            "  AND s.subscriptionStart <= :date " +
            "  AND (s.subscriptionEnd IS NULL OR s.subscriptionEnd >= :date)" +
            "  AND s.removedAt IS NULL")
    Stream<Subscription> listSubscriptions(
            @Param("tenantRealm") final String tenantRealm,
            @Param("userId") final String userId,
            @Param("date") LocalDate date,
            @Param("modalities") Collection<String> modalities);

    @Query("SELECT s FROM Subscription s " +
           "WHERE s.userId = :userId " +
           "  AND s.plan.apiGatewayTenant.tenant.realm = :tenantRealm " +
           "  AND s.plan.apiGatewayTenant.apiGateway.gateway.externalId = :gatewayExternalId " +
           "  AND (s.plan.apiGatewayTenant.apiGateway.productionExternalId = :apiExternalId OR s.plan.apiGatewayTenant.apiGateway.externalId = :apiExternalId) " +
           "  AND s.plan.apiGatewayTenant.apiGateway.gateway.removedAt IS NULL " +
           "  AND s.plan.apiGatewayTenant.tenant.removedAt IS NULL " +
           "  AND s.plan.apiGatewayTenant.removedAt IS NULL " +
           "  AND s.plan.apiGatewayTenant.apiGateway.removedAt IS NULL" +
           "  AND s.subscriptionStart <= :date " +
           "  AND (s.subscriptionEnd IS NULL OR s.subscriptionEnd >= :date)" +
           "  AND s.removedAt IS NULL")
    Optional<Subscription> findByUserAndApiExternalId(@Param("tenantRealm") final String tenantRealm,
                                                                @Param("gatewayExternalId") final String gatewayExternalId,
                                                                @Param("apiExternalId") final String apiExternalId,
                                                                @Param("userId") final String userId,
                                                                @Param("date") LocalDate date);
    @Query("SELECT s FROM Subscription s " +
           "WHERE s.removedAt IS NULL " +
           "  AND s.userId = :userId " +
           "  AND s.plan.apiGatewayTenant = :apiGatewayTenant " +
           "  AND s.subscriptionStart <= :date " +
           "  AND (s.subscriptionEnd IS NULL OR s.subscriptionEnd >= :date)"
    )
    Optional<Subscription> findSubscription(@Param("userId") String userId,
                                            @Param("apiGatewayTenant") ApiGatewayTenant apiGatewayTenant,
                                            @Param("date") LocalDate date);
    
    
    @Query("SELECT DISTINCT new io.gr1d.portal.subscriptions.response.UserResponse("
        + "s.userId, s.plan.apiGatewayTenant.tenant.realm) FROM Subscription s " +
        "WHERE s.plan.apiGatewayTenant.tenant.realm = :tenantRealm " +
        "  AND s.plan.apiGatewayTenant.apiGateway.gateway.removedAt IS NULL " +
        "  AND s.plan.apiGatewayTenant.tenant.removedAt IS NULL " +
        "  AND s.plan.apiGatewayTenant.removedAt IS NULL " +
        "  AND s.plan.apiGatewayTenant.apiGateway.removedAt IS NULL" +
        "  AND s.removedAt IS NULL")
    List<UserResponse> listUsersSubscriptions(@Param("tenantRealm") final String tenantRealm);

}
