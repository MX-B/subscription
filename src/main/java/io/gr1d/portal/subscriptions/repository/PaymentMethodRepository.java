package io.gr1d.portal.subscriptions.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import io.gr1d.portal.subscriptions.model.PaymentMethod;

public interface PaymentMethodRepository extends CrudRepository<PaymentMethod, Long> {
	
	Optional<PaymentMethod> findByName(final String name);

}
