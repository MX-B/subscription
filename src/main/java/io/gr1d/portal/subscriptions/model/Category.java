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
@Table(name = "category")
@EntityListeners(AuditListener.class)
public class Category extends BaseModel {

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "description")
    private String description;

    @Override
    protected String uuidBase() {
        return "CTG";
    }

}
