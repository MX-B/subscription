package io.gr1d.portal.subscriptions.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.ObjectUtils;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import io.gr1d.portal.subscriptions.request.PlanRequest;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "plan")
@EntityListeners(AuditListener.class)
public class Plan extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_gateway_tenant_id")
    private ApiGatewayTenant apiGatewayTenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id")
    private Api api;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private PlanType type;

    @ManyToOne
    @JoinColumn(name = "modality_id")
    private PlanModality modality;

    @Column
    private String name;

    @Column
    private String description;

    private int priority;

    @Column
    private Long value;

    @Column(name = "pending_sync", nullable = false)
    private boolean pendingSync;

    @ManyToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<PlanEndpoint> plansEndpoint;

    @Override
    protected String uuidBase() {
        return "PLAN";
    }

    public Plan clonePlan(final PlanRequest planRequest) {
        final Plan clone = new Plan();
        clone.setName(name);
        clone.setDescription(description);
        clone.setApiGatewayTenant(apiGatewayTenant);
        clone.setModality(modality);
        clone.setType(type);

        final List<PlanEndpoint> endpoints = planRequest.getPlanEndpoints().stream().map(end -> {
            final PlanEndpoint planEndpoint = new PlanEndpoint();
            planEndpoint.getApiEndpoint().setPath(end.getEndpoint());
            planEndpoint.getApiEndpoint().setName(end.getName());
            planEndpoint.getApiEndpoint().setMethodType(MethodType.valueOf(end.getMethodType()));
            planEndpoint.setExternalId(end.getExternalId());
            planEndpoint.setPlan(clone);
            planEndpoint.setMetadata(end.getMetadata());
            final List<PlanEndpointRange> ranges = end.getRanges().stream()
                    .map(rangeRequest -> new PlanEndpointRange(planEndpoint, rangeRequest))
                    .collect(Collectors.toList());
            planEndpoint.setPlanEndpointRanges(ranges);
            return planEndpoint;
        }).collect(Collectors.toList());

        clone.setPlansEndpoint(endpoints);
        return clone;
    }

    public boolean hasChangedConfig(final Plan another) {
        return !type.equals(another.type)
                || ObjectUtils.notEqual(value, another.value)
                || (PlanType.SELL.equals(type) && !apiGatewayTenant.equals(another.apiGatewayTenant))
                || !another.modality.equals(modality)
                || hasChangedAnyEndpointConfig(another);
    }

    private boolean hasChangedAnyEndpointConfig(final Plan another) {
        if (getPlansEndpoint().size() != another.getPlansEndpoint().size()) {
            return true;
        }
        final Map<String, PlanEndpoint> endpoints = another.getPlansEndpoint().stream()
                .collect(Collectors.toMap(PlanEndpoint::getEndpoint, item -> item));

        for (final PlanEndpoint thisPlanEndpoint : getPlansEndpoint()) {
            final PlanEndpoint anotherPlanEndpoint = endpoints.remove(thisPlanEndpoint.getEndpoint());
            if (anotherPlanEndpoint == null || thisPlanEndpoint.hasChangedConfig(anotherPlanEndpoint)) {
                return true;
            }
        }

        return !endpoints.isEmpty();
    }

}
