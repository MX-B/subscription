package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
@Table(name = "api_gateway")
@EntityListeners(AuditListener.class)
public class ApiGateway extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "api_id")
    private Api api;

    @ManyToOne
    @JoinColumn(name = "gateway_id")
    private Gateway gateway;

    private String externalId;

    private String productionExternalId;

    @Override
    protected String uuidBase() {
        return "AGW";
    }

}
