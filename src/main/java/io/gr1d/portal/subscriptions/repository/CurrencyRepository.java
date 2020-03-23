package io.gr1d.portal.subscriptions.repository;

import io.gr1d.portal.subscriptions.model.Currency;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends CrudRepository<Currency, Long>, JpaSpecificationExecutor<Currency> {

    Currency findByIsoCode(String isoCode);

}
