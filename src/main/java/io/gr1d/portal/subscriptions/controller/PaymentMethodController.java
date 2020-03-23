package io.gr1d.portal.subscriptions.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.gr1d.core.controller.BaseController;
import io.gr1d.portal.subscriptions.model.PaymentMethod;
import io.gr1d.portal.subscriptions.service.PaymentMethodService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = "/paymentMethods")
public class PaymentMethodController extends BaseController {

    private final PaymentMethodService paymentMethodService;

    @Autowired
    public PaymentMethodController(final PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @ApiOperation(value = "Get Payment Methods", notes = "Returns a list of payments methods avaiables", tags = "Payment Methods")
    @RequestMapping(method = GET, produces = JSON)
    public List<PaymentMethod> getPaymentMethods() {
        log.info("Requesting the list of payment methods available ");
        return paymentMethodService.listPaymentMethods();
    }
}
