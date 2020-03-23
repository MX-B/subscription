package io.gr1d.portal.subscriptions.model;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Getter @Setter
@Table(name = "plan_endpoint")
@EntityListeners(AuditListener.class)
public class PlanEndpoint extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "external_id")
    private String externalId;

    @ManyToOne(fetch = FetchType.EAGER,  cascade = CascadeType.ALL)
    @JoinColumn(name = "api_endpoint_id")
    private ApiEndpoint apiEndpoint = new ApiEndpoint();

    @OneToMany(mappedBy = "planEndpoint", fetch = FetchType.EAGER, orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PlanEndpointRange> planEndpointRanges;

    @Override
    protected String uuidBase() {
        return "PLE";
    }

    @Column(name = "value", length = 24)
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "property", length = 24)
    @JoinTable(name = "plan_endpoint_metadata", joinColumns = @JoinColumn(name = "plan_endpoint_id"))
    private Map<String, String> metadata;

    public PlanEndpoint clonePlanEndpoint() {
        final PlanEndpoint clone = new PlanEndpoint();
        clone.setPlan(plan);
        clone.setExternalId(externalId);
        clone.setApiEndpoint(apiEndpoint);
        clone.setMetadata(metadata);
        clone.setPlanEndpointRanges(planEndpointRanges);
        return clone;
    }

    private boolean equalMetadata(final Map<String, String> metadata) {
        if (metadata.size() != this.metadata.size()) {
            return false;
        }
        final Set<String> keys = metadata.keySet();
        for (String key : keys) {
            if (!StringUtils.equals(metadata.get(key), this.metadata.get(key))) {
                return false;
            }
        }
        return true;
    }

    private boolean equalRanges(final List<PlanEndpointRange> planEndpointRanges) {
        if (planEndpointRanges.size() != this.planEndpointRanges.size()) {
            return false;
        }

        return planEndpointRanges.containsAll(this.planEndpointRanges) && this.planEndpointRanges.containsAll(planEndpointRanges);

    }

    public boolean hasChangedConfig (final PlanEndpoint another) {

        if (!another.plan.getModality().equals(this.getPlan().getModality())) {
            return true;
        }

        if (another.plan.getModality().equals(PlanModality.UNLIMITED_RANGE)) {
            return !equalRanges(another.planEndpointRanges);
        }

        return !equalMetadata(another.metadata);
    }

    public String getEndpoint() {
        return this.apiEndpoint.getPath();
    }

    public String getName() {
        return this.apiEndpoint.getName();
    }

    void setName(final String name) {
        this.apiEndpoint.setName(name);
    }

    void setEndpoint(String apiEndpoint) {
        this.apiEndpoint.setPath(apiEndpoint);
    }
}
