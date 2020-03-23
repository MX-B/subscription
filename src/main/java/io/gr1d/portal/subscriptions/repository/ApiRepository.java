package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.response.ApiListResponse;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiRepository extends CrudRepository<Api, Long>, JpaSpecificationExecutor<Api> {

    Api findByUuidAndRemovedAtIsNull(final String uuid);

    Api findBySlugAndRemovedAtIsNull(final String slug);

    @Modifying
    @Query("UPDATE Api api SET api.removedAt = :removedAt WHERE api.provider = :provider AND api.removedAt IS NULL")
    void removeApiByProvider(@Param("provider") Provider provider, @Param("removedAt") LocalDateTime removedAt);

    List<Api> findByRemovedAtIsNull();

    @Query("SELECT DISTINCT new io.gr1d.portal.subscriptions.response.ApiListResponse(a) " +
           "FROM Api a " +
           "WHERE a.removedAt IS NULL " +
           "AND a.id NOT IN (SELECT api.id FROM Plan p JOIN p.apiGatewayTenant.apiGateway.api api)")
    List<ApiListResponse> listAllWithoutPlan();

}
