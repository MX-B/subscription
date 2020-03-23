package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@Entity
@Getter @Setter
@Table(name = "gateway")
@EntityListeners(AuditListener.class)
public class Gateway extends BaseModel {

    private String name;

    @Column(name = "external_id")
    private String externalId;

    @Override
    protected String uuidBase() {
        return "GTW";
    }

}
