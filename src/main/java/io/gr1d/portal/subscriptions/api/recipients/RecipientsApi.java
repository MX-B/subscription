package io.gr1d.portal.subscriptions.api.recipients;

import io.gr1d.portal.subscriptions.api.KeycloakFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "recipientsApi", url = "${gr1d.subscription.integration.recipientService}", configuration = KeycloakFeignConfiguration.class)
public interface RecipientsApi {

    @GetMapping("/recipient/{recipientUuid}")
    Recipient getRecipientData(@PathVariable("recipientUuid") String recipientUuid);

}
