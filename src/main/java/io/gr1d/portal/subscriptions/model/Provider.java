package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;

@Entity
@Getter @Setter
@Table(name = "provider")
@EntityListeners(AuditListener.class)
public class Provider extends BaseModel {

    @NotEmpty
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "wallet_id", length = 64)
    private String walletId;

    private String phone;

    private String email;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private ProviderType type;

    @Override
    protected String uuidBase() {
        return "PRV";
    }

}
