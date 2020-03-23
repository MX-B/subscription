package io.gr1d.portal.subscriptions.security;

import org.springframework.stereotype.Component;
import io.gr1d.auth.keycloak.ConfigSecurity;
import io.gr1d.auth.keycloak.EndpointConfiguration;
import io.gr1d.core.healthcheck.HealthCheckController;
import io.gr1d.portal.subscriptions.controller.ApiController;
import io.gr1d.portal.subscriptions.controller.ApiGatewayController;
import io.gr1d.portal.subscriptions.controller.ApiGatewayTenantController;
import io.gr1d.portal.subscriptions.controller.CategoryController;
import io.gr1d.portal.subscriptions.controller.CurrencyController;
import io.gr1d.portal.subscriptions.controller.GatewayController;
import io.gr1d.portal.subscriptions.controller.PaymentMethodController;
import io.gr1d.portal.subscriptions.controller.PlanController;
import io.gr1d.portal.subscriptions.controller.ProviderController;
import io.gr1d.portal.subscriptions.controller.RealmController;
import io.gr1d.portal.subscriptions.controller.SubscriptionController;
import io.gr1d.portal.subscriptions.controller.TagController;
import io.gr1d.portal.subscriptions.controller.TenantController;
import io.gr1d.portal.subscriptions.controller.external.v1.V1ApiController;
import io.gr1d.portal.subscriptions.controller.external.v1.V1CategoryController;
import io.gr1d.portal.subscriptions.controller.external.v1.V1SubscriptionController;
import io.gr1d.portal.subscriptions.controller.external.v1.V1TagController;

@Component
public class AccessConfig implements EndpointConfiguration {

    private static final String USER = "user";
    private static final String ADMIN = "admin";

    @Override
    public void configure(final ConfigSecurity config) throws Exception {
        config
                .allow(V1ApiController.class, "findByApiAndGatewayAndTenantRealm", USER, ADMIN)
                .allow(V1ApiController.class, "listApisByTenant", USER, ADMIN)
                .allow(V1SubscriptionController.class, "getSubscription", USER, ADMIN)

                .allow(RealmController.class, "list", ADMIN)
                .allow(CurrencyController.class, "list", USER, ADMIN)
                .allow(HealthCheckController.class, "completeHealthCheck", ADMIN)

                .allow(TagController.class, "list", USER, ADMIN)
                .allow(TagController.class, "get", USER, ADMIN)
                .allow(TagController.class, "create", ADMIN)
                .allow(TagController.class, "remove", ADMIN)
                .allow(TagController.class, "update", ADMIN)

                .allow(CategoryController.class, "list", USER, ADMIN)
                .allow(CategoryController.class, "get", USER, ADMIN)
                .allow(CategoryController.class, "create", ADMIN)
                .allow(CategoryController.class, "remove", ADMIN)
                .allow(CategoryController.class, "update", ADMIN)

                .allow(V1TagController.class, "list", USER, ADMIN)
                .allow(V1TagController.class, "listAll", USER, ADMIN)
                .allow(V1TagController.class, "get", USER, ADMIN)
                .allow(V1CategoryController.class, "list", USER, ADMIN)
                .allow(V1CategoryController.class, "listAll", USER, ADMIN)
                .allow(V1CategoryController.class, "get", USER, ADMIN)

                .allow(ApiController.class, "list", USER, ADMIN)
                .allow(ApiController.class, "listBridgeApis", USER, ADMIN)
                .allow(ApiController.class, "listEndpoints", USER, ADMIN)                
                .allow(ApiController.class, "get", USER, ADMIN)
                .allow(ApiController.class, "create", ADMIN)
                .allow(ApiController.class, "remove", ADMIN)
                .allow(ApiController.class, "update", ADMIN)
                .allow(ApiController.class, "findByApiAndGatewayAndTenantRealm", USER, ADMIN)
                .allow(ApiController.class, "findPaymentMethodsByApiAndGatewayAndTenantRealm", USER, ADMIN)
                .allow(ApiController.class, "findAllApisByExternalIdAndTenantRealm", USER, ADMIN)
                .allow(ApiController.class, "findPaymentMethodsByApiAndGatewayAndTenantRealm", USER, ADMIN)  
                .allow(ApiController.class, "listWithoutPlan", USER, ADMIN)  
                .allow(ApiController.class, "listBridgeApisWithoutAssociation", USER, ADMIN)                  
                
                .allow(ApiGatewayController.class, "list", USER, ADMIN)
                .allow(ApiGatewayController.class, "listEndpoints", USER, ADMIN)
                .allow(ApiGatewayController.class, "get", USER, ADMIN)
                .allow(ApiGatewayController.class, "create", ADMIN)
                .allow(ApiGatewayController.class, "remove", ADMIN)
                .allow(ApiGatewayController.class, "update", ADMIN)
                .allow(ApiGatewayController.class, "getByProductionExternalId", USER, ADMIN)
                .allow(ApiGatewayController.class, "getBySandboxExternalId", USER, ADMIN)

                .allow(ProviderController.class, "list", USER, ADMIN)
                .allow(ProviderController.class, "get", USER, ADMIN)
                .allow(ProviderController.class, "create", ADMIN)
                .allow(ProviderController.class, "remove", ADMIN)
                .allow(ProviderController.class, "update", ADMIN)

                .allow(SubscriptionController.class, "getSubscription", USER, ADMIN)
                .allow(SubscriptionController.class, "subscribe", ADMIN)
                .allow(SubscriptionController.class, "unsubscribe", ADMIN)
                .allow(SubscriptionController.class, "listSubscriptionsForUser", ADMIN)
                .allow(SubscriptionController.class, "listUsersSubscriptions", USER, ADMIN)

                .allow(TenantController.class, "list", USER, ADMIN)
                .allow(TenantController.class, "get", USER, ADMIN)
                .allow(TenantController.class, "create", ADMIN)
                .allow(TenantController.class, "remove", ADMIN)
                .allow(TenantController.class, "update", ADMIN)

                .allow(GatewayController.class, "list", USER, ADMIN)
                .allow(GatewayController.class, "get", USER, ADMIN)
                .allow(GatewayController.class, "create", ADMIN)
                .allow(GatewayController.class, "remove", ADMIN)
                .allow(GatewayController.class, "update", ADMIN)

                .allow(ApiGatewayTenantController.class, "list", USER, ADMIN)
                .allow(ApiGatewayTenantController.class, "get", USER, ADMIN)
                .allow(ApiGatewayTenantController.class, "create", ADMIN)
                .allow(ApiGatewayTenantController.class, "remove", ADMIN)
                .allow(ApiGatewayTenantController.class, "update", ADMIN)
                .allow(PlanController.class, "getPlan", USER, ADMIN)
                .allow(PaymentMethodController.class, "getPaymentMethods", USER, ADMIN)                
        ;
    }


}
