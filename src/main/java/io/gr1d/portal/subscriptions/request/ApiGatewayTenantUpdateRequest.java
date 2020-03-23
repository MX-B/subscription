package io.gr1d.portal.subscriptions.request;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiGatewayTenantUpdateRequest {

    @Valid
    @NotNull
    private List<PlanRequest> plans;

    @Min(0)
    @Max(100)
    private BigDecimal percentageSplit;
    
    private List<String> paymentMethods;

}
