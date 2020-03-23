package io.gr1d.portal.subscriptions.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import io.gr1d.portal.subscriptions.model.PaymentMethod;
import io.gr1d.portal.subscriptions.repository.PaymentMethodRepository;

@Service
public class PaymentMethodService {

	@Autowired
    private PaymentMethodRepository repository;

	public List<PaymentMethod> listPaymentMethods() {
		return Lists.newArrayList(repository.findAll());
	}

}
