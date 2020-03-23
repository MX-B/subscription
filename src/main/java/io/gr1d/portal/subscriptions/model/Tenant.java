package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;

@Entity
@Getter@Setter
@Table(name = "tenant")
@EntityListeners(AuditListener.class)
public class Tenant extends BaseModel {

    @NotEmpty
    @Column(name = "wallet_id", nullable = false, length = 64)
    private String walletId;

    @Column(name = "realm", length = 64)
    private String realm;

    @Column(name = "name", length = 64)
    private String name;

    @Override
    protected String uuidBase() {
        return "TEN";
    }

}
