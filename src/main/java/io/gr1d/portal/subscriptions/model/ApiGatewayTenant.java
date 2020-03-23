package io.gr1d.portal.subscriptions.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "api_gateway_tenant")
@EntityListeners(AuditListener.class)
public class ApiGatewayTenant extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "api_gateway_id")
    private ApiGateway apiGateway;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private ApiType type;

    @Column(name = "percentage_split")
    private BigDecimal percentageSplit = BigDecimal.ZERO.setScale(2);

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    @ManyToMany(mappedBy = "apiGatewayTenant", fetch = FetchType.LAZY)
    private List<Plan> plans;
    
    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "api_payment_method",
            joinColumns = @JoinColumn(name = "api_gateway_tenant_id"),
            inverseJoinColumns = @JoinColumn(name = "payment_method_id"))
    private Set<PaymentMethod> paymentMethods;

    @Override
    protected String uuidBase() {
        return "AGT";
    }

}
