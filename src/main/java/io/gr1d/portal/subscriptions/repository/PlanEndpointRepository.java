package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.Plan;
import io.gr1d.portal.subscriptions.model.PlanEndpoint;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PlanEndpointRepository extends CrudRepository<PlanEndpoint, Long> {

    PlanEndpoint findByApiEndpointPathAndPlanUuidAndRemovedAtIsNull(final String path, final String planUuid);

	List<PlanEndpoint> findByPlanIdAndRemovedAtIsNull(Long planId);

	@Modifying
	@Query("UPDATE PlanEndpoint p SET p.removedAt = :removedAt WHERE p.removedAt IS NULL AND p.plan.uuid NOT IN :uuids")
	void removePlanEndpointByPlanUuids(@Param("uuids") List<String> uuids, @Param("removedAt") LocalDateTime removedAt);

	@Modifying
	@Query("UPDATE PlanEndpoint p SET p.removedAt = :removedAt WHERE p.removedAt IS NULL AND p.plan = :plan")
	void removePlanEndpointByPlan(@Param("plan") Plan apiGatewayTenant, @Param("removedAt") LocalDateTime removedAt);

}
