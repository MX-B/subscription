package io.gr1d.portal.subscriptions.response;

import static java.util.Optional.ofNullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.gr1d.core.datasource.model.BaseEnum;
import io.gr1d.portal.subscriptions.model.ApiGatewayTenant;
import io.gr1d.portal.subscriptions.model.PaymentMethod;
import lombok.Data;

@Data
public class ApiGatewayTenantResponse {

    private String uuid;
    private TenantResponse tenant;
    private ApiGatewayResponse apiGateway;
    private List<PlanResponse> plans;
    private String type;
    private BigDecimal percentageSplit;
    private Set<String> paymentMethods;

    private LocalDateTime createdAt;
    private LocalDateTime removedAt;

    public ApiGatewayTenantResponse(final ApiGatewayTenant apiGatewayTenant) {
        this.uuid = apiGatewayTenant.getUuid();
        this.tenant = new TenantResponse(apiGatewayTenant.getTenant());
        this.apiGateway = new ApiGatewayResponse(apiGatewayTenant.getApiGateway());
        this.percentageSplit = apiGatewayTenant.getPercentageSplit();

        this.createdAt = apiGatewayTenant.getCreatedAt();
        this.removedAt = apiGatewayTenant.getRemovedAt();
        this.type = ofNullable(apiGatewayTenant.getType()).map(BaseEnum::getName).orElse(null);
        
        this.paymentMethods = Optional.ofNullable(apiGatewayTenant.getPaymentMethods())
				.map(Collection::stream).orElseGet(Stream::empty)
				.map(PaymentMethod::getName)
				.collect(Collectors.toSet());
    }
}
