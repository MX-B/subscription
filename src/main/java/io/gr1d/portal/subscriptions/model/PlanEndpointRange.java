package io.gr1d.portal.subscriptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gr1d.portal.subscriptions.request.PlanEndpointRangeRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "plan_endpoint_range")
public class PlanEndpointRange {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_endpoint_id")
    private PlanEndpoint planEndpoint;

    @Column(name = "init_range")
    private Long initRange;

    @Column(name = "final_range")
    private Long finalRange;

    @Column
    private Long value;

    public PlanEndpointRange(final PlanEndpoint planEndpoint, final PlanEndpointRangeRequest request) {
        this.planEndpoint = planEndpoint;
        this.initRange = request.getInitRange();
        this.finalRange = request.getFinalRange();
        this.value = request.getValue();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PlanEndpointRange that = (PlanEndpointRange) o;
        return initRange.equals(that.initRange) &&
                Objects.equals(finalRange, that.finalRange) &&
                value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initRange, finalRange, value);
    }
}
