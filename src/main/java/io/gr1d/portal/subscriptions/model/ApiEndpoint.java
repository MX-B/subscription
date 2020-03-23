package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_endpoint")
@EntityListeners(AuditListener.class)
public class ApiEndpoint extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "api_id")
    private Api api;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "path", nullable = false)
    private String path;

    @ManyToOne
    @JoinColumn(name = "method_type_id")
    private MethodType methodType;

    @Override
    protected String uuidBase() {
        return "APE";
    }
}
