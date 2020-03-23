package io.gr1d.portal.subscriptions.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "api")
@EntityListeners(AuditListener.class)
public class Api extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "split_mode_id")
    private SplitMode splitMode;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "slug")
    private String slug;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "api_tag",
            joinColumns = @JoinColumn(name = "api_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags;

    @Override
    protected String uuidBase() {
        return "API";
    }

}
