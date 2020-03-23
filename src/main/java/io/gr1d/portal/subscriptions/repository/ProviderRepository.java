package io.gr1d.portal.subscriptions.repository;

import io.gr1d.core.datasource.repository.BaseModelRepository;
import io.gr1d.portal.subscriptions.model.Provider;
import org.springframework.data.jpa.repository.Query;

public interface ProviderRepository extends BaseModelRepository<Provider> {

    @Query("SELECT p.uuid FROM Provider p WHERE p.email = ?1")
    String findUUIDByEmail(String email);

}
