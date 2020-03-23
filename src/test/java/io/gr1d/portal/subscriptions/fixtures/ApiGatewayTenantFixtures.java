package io.gr1d.portal.subscriptions.fixtures;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.Rule;
import br.com.six2six.fixturefactory.loader.TemplateLoader;
import io.gr1d.portal.subscriptions.model.*;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantUpdateRequest;
import io.gr1d.portal.subscriptions.request.PlanRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiGatewayTenantFixtures implements TemplateLoader {

    @Override
    public void load() {
        Fixture.of(ApiGatewayTenantCreateRequest.class).addTemplate("valid", new Rule() {
            {
                add("plans", has(1).of(PlanRequest.class, "valid_unlimited"));
                add("percentageSplit", BigDecimal.valueOf(25.55).setScale(2));
            }
        });

        Fixture.of(ApiGatewayTenantUpdateRequest.class).addTemplate("valid", new Rule() {
            {
                add("plans", has(1).of(PlanRequest.class, "valid_unlimited"));
                add("percentageSplit", BigDecimal.valueOf(25.55).setScale(2));
            }
        });

        Fixture.of(ApiGatewayTenantCreateRequest.class).addTemplate("invalidHitValue").inherits("valid", new Rule() {
            {
                add("plans", has(1).of(PlanRequest.class, "invalid_hit_valid_unlimited"));
                add("percentageSplit", BigDecimal.valueOf(25.55).setScale(2));
            }
        });

        Fixture.of(ApiGatewayTenantCreateRequest.class).addTemplate("hitValueTooLarge").inherits("valid", new Rule() {
            {
                add("plans", has(1).of(PlanRequest.class, "hitValueTooLarge_unlimited"));
                add("percentageSplit", BigDecimal.valueOf(25.55).setScale(2));
            }
        });

        Fixture.of(ApiGatewayTenantCreateRequest.class).addTemplate("invalidLimit").inherits("valid", new Rule() {
            {
                add("plans", has(1).of(PlanRequest.class, "invalidLimit_free"));
                add("percentageSplit", BigDecimal.valueOf(25.55).setScale(2));
            }
        });

        Fixture.of(PlanEndpoint.class).addTemplate("valid_free", new Rule() {
            {
                final Map<String, String> metadata = new HashMap<>();
                metadata.put("limit", "100");
                add("metadata", metadata);
                final ApiEndpoint apiEndpoint = new ApiEndpoint();
                apiEndpoint.setPath("/test/endpoint/");
                apiEndpoint.setName("Name Endpoint");
                apiEndpoint.setMethodType(MethodType.GET);
                add("apiEndpoint", apiEndpoint);
            }
        });

        Fixture.of(ApiGatewayTenantCreateRequest.class).addTemplate("valid_unlimited_range", new Rule() {
            {
                add("plans", has(1).of(PlanRequest.class, "valid_unlimited_range"));
                add("percentageSplit", BigDecimal.TEN.setScale(2));
            }
        });

        Fixture.of(ApiGatewayTenant.class).addTemplate("valid_free", new Rule() {
            {
                final Plan free = new Plan();
                free.setModality(PlanModality.FREE);
                free.setDescription("Lorem ipsum");
                free.setName("FREE");
                free.setPlansEndpoint(Collections.singletonList(Fixture.from(PlanEndpoint.class).gimme("valid_free")));
                add("plans", Collections.singletonList(free));
            }
        });
    }

    public static class ApiGatewayTenantRule extends Rule {
        public ApiGatewayTenantRule(final ApiGateway apiGateway, final Tenant tenant) {
            add("apiGatewayUuid", apiGateway.getUuid());
            add("tenantUuid", tenant.getUuid());
        }
    }

    public static class ApiGatewayTenantEntityRule extends Rule {
        public ApiGatewayTenantEntityRule(final ApiGateway apiGateway, final Tenant tenant) {
            add("apiGateway", apiGateway);
            add("tenant", tenant);
        }
    }
}
