package io.gr1d.portal.subscriptions.response;

import io.gr1d.portal.subscriptions.model.Currency;
import lombok.Getter;

@Getter
public class CurrencyResponse {
    private String name;
    private String isoCode;
    private String symbol;

    public CurrencyResponse(final Currency currency) {
        this.name = currency.getName();
        this.isoCode = currency.getIsoCode();
        this.symbol = currency.getSymbol();
    }
}
