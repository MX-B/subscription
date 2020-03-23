package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.Tenant;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TenantRepository extends CrudRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    Optional<Tenant> findByUuid(String uuid);

    Optional<Tenant> findByUuidAndRemovedAtIsNull(String uuid);

    Optional<Tenant> findByRealmAndRemovedAtIsNull(String realm);

}
