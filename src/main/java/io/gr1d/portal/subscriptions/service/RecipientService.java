package io.gr1d.portal.subscriptions.service;

import feign.FeignException;
import io.gr1d.core.util.Markers;
import io.gr1d.portal.subscriptions.api.recipients.Recipient;
import io.gr1d.portal.subscriptions.api.recipients.RecipientsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RecipientService {

    private final RecipientsApi recipientsApi;

    @Autowired
    public RecipientService(final RecipientsApi recipientsApi) {
        this.recipientsApi = recipientsApi;
    }

    public Recipient getRecipientData(String uuid) {
        try {
            return recipientsApi.getRecipientData(uuid);
        } catch (FeignException e) {
            log.error(Markers.NOTIFY_ADMIN, "Error while trying to retrieve recipient {}", uuid, e);
            return null;
        }
    }
}
