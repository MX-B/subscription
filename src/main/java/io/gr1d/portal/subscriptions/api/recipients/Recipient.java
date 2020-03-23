package io.gr1d.portal.subscriptions.api.recipients;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Recipient {

    private String name;
    private String bankCode;
    private String bankName;
    private String documentNumber;
    private String documentType;
    private String bankAccount;
    private String bankAccountVerification;
    private String agency;
    private String agencyVerification;
    private String bankAccountType;

    private Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(final Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
