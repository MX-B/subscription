package io.gr1d.portal.subscriptions.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.gr1d.portal.subscriptions.TestUtils.checkXss;
import static io.gr1d.portal.subscriptions.TestUtils.getResult;
import static io.gr1d.portal.subscriptions.TestUtils.json;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.stream.Collectors;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.core.service.Gr1dClock;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayFixtures;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayTenantFixtures;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.model.Tenant;
import io.gr1d.portal.subscriptions.repository.ApiGatewayRepository;
import io.gr1d.portal.subscriptions.repository.ApiRepository;
import io.gr1d.portal.subscriptions.repository.GatewayRepository;
import io.gr1d.portal.subscriptions.repository.ProviderRepository;
import io.gr1d.portal.subscriptions.repository.TenantRepository;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayTenantUpdateRequest;
import io.gr1d.portal.subscriptions.request.PlanEndpointRequest;
import io.gr1d.portal.subscriptions.request.PlanRequest;
import io.gr1d.portal.subscriptions.request.SubscriptionRequest;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantResponse;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class SubscriptionControllerIntegrationTest {

    private static final String URI = "/apiGatewayTenant";
    private static final String USER_ID = "f56b9b96-1905-4a48-8896-3e34b3e1ba5a";

    @Autowired private MockMvc mockMvc;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ApiRepository apiRepository;
    @Autowired private GatewayRepository gatewayRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ApiGatewayRepository apiGatewayRepository;
    @Autowired private Flyway flyway;
    @Autowired private Gr1dClock gr1dClock;

    private Tenant tenant;
    private ApiGateway apiGateway;
    private Gateway gateway;
    private Api api;
    private Provider provider;

    @Before
    public void init() {
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");
        final Api api = Fixture.from(Api.class).gimme("valid");

        this.gateway = gatewayRepository.save(Fixture.from(Gateway.class).gimme("valid"));
        this.provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));

        api.setProvider(provider);
        this.api = apiRepository.save(api);

        this.tenant = tenantRepository.save(Fixture.from(Tenant.class).gimme("valid"));
        this.apiGateway = apiGatewayRepository.save(Fixture.from(ApiGateway.class)
                .gimme("valid", new ApiGatewayFixtures.ApiGatewayRule(api, gateway)));

        stubFor(WireMock.post(urlEqualTo(String.format("/gateways/%s/apis/%s/plan", gateway.getExternalId(), apiGateway.getProductionExternalId())))
                .willReturn(aResponse().withStatus(200)));
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
        gr1dClock.setup();
    }

    @Test
    @FlywayTest
    public void testChangePlanWithActiveSubscription() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimited = new PlanRequest();
        unlimited.setModality("UNLIMITED");
        unlimited.setDescription("Lorem ipsum");
        unlimited.setName("BASIC");
        unlimited.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited")));

        request.setPlans(Collections.singletonList(unlimited));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].endpoint").value(unlimited.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        // subscription
        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        final CreatedResponse subscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        // /tenant/{tenantUuid}/gateway/{gatewayUuid}/api/{apiExternalId}/user/{userId}
        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s",
                tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("api.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("api.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("api.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("api.production_external_id").value(apiGateway.getProductionExternalId()))
                .andExpect(jsonPath("api.name").value(api.getName()))
                .andExpect(jsonPath("api.split_mode").value(api.getSplitMode().getName()))
                .andExpect(jsonPath("api.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("api.gateway.external_id").value(gateway.getExternalId()))
                .andExpect(jsonPath("api.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("api.provider.phone").value(provider.getPhone()))
                .andExpect(jsonPath("api.provider.wallet_id").value(provider.getWalletId()))
                .andExpect(jsonPath("plan.modality").value("UNLIMITED"))
                .andExpect(jsonPath("plan.name").value(unlimited.getName()))
                .andExpect(jsonPath("plan.description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plan.plan_endpoints[0].metadata.hit_value").value("1500"));

        mockMvc.perform(get(String.format("/v1/subscription/tenant/%s/gateway/%s/api/%s/user/%s",
                tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()));

        final String subscriptionUrl = String.format("/subscription/tenant/%s/user/%s", tenant.getRealm(), USER_ID);
        mockMvc.perform(get(subscriptionUrl).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("content[0].tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("content[0].tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("content[0].api.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("content[0].api.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("content[0].api.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("content[0].api.production_external_id").value(apiGateway.getProductionExternalId()))
                .andExpect(jsonPath("content[0].api.name").value(api.getName()))
                .andExpect(jsonPath("content[0].api.split_mode").value(api.getSplitMode().getName()))
                .andExpect(jsonPath("content[0].api.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("content[0].api.gateway.external_id").value(gateway.getExternalId()))
                .andExpect(jsonPath("content[0].api.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("content[0].api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("content[0].api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("content[0].api.provider.phone").value(provider.getPhone()))
                .andExpect(jsonPath("content[0].api.provider.wallet_id").value(provider.getWalletId()))
                .andExpect(jsonPath("content[0].plan.modality").value("UNLIMITED"))
                .andExpect(jsonPath("content[0].plan.name").value(unlimited.getName()))
                .andExpect(jsonPath("content[0].plan.description").value(unlimited.getDescription()))
                .andExpect(jsonPath("content[0].plan.plan_endpoints[0].metadata.hit_value").value("1500"));

        final PlanRequest planRequest = response.getPlans().get(0).toRequest();
        planRequest.setModality("FREE");
        planRequest.setDirty(true);
        final PlanEndpointRequest planEndpointRequest = planRequest.getPlanEndpoints().get(0);
        final Map<String, String> metadata = planEndpointRequest.getMetadata();
        metadata.put("limit", "200");
        metadata.remove("hit_value");
        final ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Collections.singletonList(planRequest), BigDecimal.TEN, Collections.emptyList());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        final ApiGatewayTenantResponse anotherResponse = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].modality").value("FREE"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.limit").value("200")), ApiGatewayTenantResponse.class);

        // new generated uuid
        Assertions.assertThat(response.getPlans().get(0).getUuid()).isNotEqualTo(anotherResponse.getPlans().get(0).getUuid());

        mockMvc.perform(get(subscriptionUrl).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("content[0].tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("content[0].tenant.realm").value(tenant.getRealm()))
                .andExpect(jsonPath("content[0].api.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("content[0].api.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("content[0].api.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("content[0].api.production_external_id").value(apiGateway.getProductionExternalId()))
                .andExpect(jsonPath("content[0].api.name").value(api.getName()))
                .andExpect(jsonPath("content[0].api.split_mode").value(api.getSplitMode().getName()))
                .andExpect(jsonPath("content[0].api.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("content[0].api.gateway.external_id").value(gateway.getExternalId()))
                .andExpect(jsonPath("content[0].api.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("content[0].api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("content[0].api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("content[0].api.provider.phone").value(provider.getPhone()))
                .andExpect(jsonPath("content[0].api.provider.wallet_id").value(provider.getWalletId()))
                .andExpect(jsonPath("content[0].plan.uuid").value(response.getPlans().get(0).getUuid()))
                .andExpect(jsonPath("content[0].plan.modality").value("UNLIMITED"))
                .andExpect(jsonPath("content[0].plan.name").value(unlimited.getName()))
                .andExpect(jsonPath("content[0].plan.description").value(unlimited.getDescription()))
                .andExpect(jsonPath("content[0].plan.plan_endpoints[0].metadata.hit_value").value("1500"));
    }

    @Test
    @FlywayTest
    public void testSubscribeToRemovedPlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimited = new PlanRequest();
        unlimited.setModality("UNLIMITED");
        unlimited.setDescription("Lorem ipsum");
        unlimited.setName("BASIC");
        unlimited.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited")));

        request.setPlans(Collections.singletonList(unlimited));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].endpoint").value(unlimited.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(new ApiGatewayTenantUpdateRequest(Collections.emptyList(), BigDecimal.TEN, Collections.emptyList())))).andDo(print()).andExpect(status().isOk()));

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid()); // already removed
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testSubscriptionAndUnsubscription() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final Clock clock = Mockito.mock(Clock.class);
        gr1dClock.setClock(clock);
        when(clock.instant()).thenReturn(instant(2018, 11, 12));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        final HashMap<String, String> metadata = new HashMap<>();

        final PlanRequest basic = new PlanRequest();
        basic.setModality("DEFAULT");
        basic.setDescription("Lorem ipsum");
        basic.setName("BASIC");
        basic.setValue(2000L);
        basic.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.put("hits", "15000");
        metadata.put("hit_value", "1500");
        basic.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest premium = new PlanRequest();
        premium.setModality("DEFAULT");
        premium.setDescription("Lorem ipsum");
        premium.setName("PREMIUM");
        premium.setValue(10000L);
        premium.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "100000");
        metadata.put("hit_value", "1500");
        premium.getPlanEndpoints().get(0).setMetadata(metadata);


        final PlanRequest master = new PlanRequest();
        master.setModality("DEFAULT");
        master.setDescription("Lorem ipsum");
        master.setName("MASTER");
        master.setValue(100000L);
        master.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "1000000");
        metadata.put("hit_value", "100");
        master.getPlanEndpoints().get(0).setMetadata(metadata);


        request.setPlans(Arrays.asList(basic, premium, master));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk()), ApiGatewayTenantResponse.class);

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        final CreatedResponse subscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("percentage_tenant_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()));

        // two days after subscribe, the user chose another plan
        when(clock.instant()).thenReturn(instant(2018, 11, 14));
        subscriptionRequest.setPlan(response.getPlans().get(1).getUuid());

        final CreatedResponse premiumSubscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("percentage_tenant_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription.uuid").value(premiumSubscription.getUuid()))
                .andExpect(jsonPath("next_subscription.subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("next_subscription.subscription_end").doesNotExist())
                .andExpect(jsonPath("next_subscription.plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("next_subscription.plan.name").value(premium.getName()))
                .andExpect(jsonPath("next_subscription.plan.value").value(premium.getValue()));

        // the next day the user changed his mind and opted for a more expensive plan to fit his needs
        when(clock.instant()).thenReturn(instant(2018, 11, 15));
        subscriptionRequest.setPlan(response.getPlans().get(2).getUuid());

        final CreatedResponse masterSubscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        // the premium plan wont be activated and should be removed because the start date never came out
        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("percentage_tenant_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription.uuid").value(masterSubscription.getUuid()))
                .andExpect(jsonPath("next_subscription.subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("next_subscription.subscription_end").doesNotExist())
                .andExpect(jsonPath("next_subscription.plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("next_subscription.plan.name").value(master.getName()))
                .andExpect(jsonPath("next_subscription.plan.value").value(master.getValue()));

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID))
                .header("X-Date", "2018-12-01"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(masterSubscription.getUuid()))
                .andExpect(jsonPath("percentage_tenant_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(master.getName()))
                .andExpect(jsonPath("plan.value").value(master.getValue()));

        // the next day the user changed his mind again and decided to stay with the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(response.getPlans().get(2).getUuid());

        checkXss(mockMvc.perform(post(String.format("/subscription/tenant/%s/gateway/%s/api/%s/unsubscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("percentage_tenant_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next").doesNotExist());

        // then he unsubscribed for the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());

        checkXss(mockMvc.perform(post(String.format("/subscription/tenant/%s/gateway/%s/api/%s/unsubscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("percentage_tenant_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next").doesNotExist());
    }

    @Test
    @FlywayTest
    public void testSubscriptionAndUnsubscriptionForAPI() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final Clock clock = Mockito.mock(Clock.class);
        gr1dClock.setClock(clock);
        when(clock.instant()).thenReturn(instant(2018, 11, 12));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        final HashMap<String, String> metadata = new HashMap<>();

        final PlanRequest basic = new PlanRequest();
        basic.setModality("DEFAULT");
        basic.setDescription("Lorem ipsum");
        basic.setName("BASIC");
        basic.setValue(2000L);
        basic.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.put("hits", "15000");
        metadata.put("hit_value", "1500");
        basic.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest premium = new PlanRequest();
        premium.setModality("DEFAULT");
        premium.setDescription("Lorem ipsum");
        premium.setName("PREMIUM");
        premium.setValue(10000L);
        premium.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "100000");
        metadata.put("hit_value", "1500");
        premium.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest master = new PlanRequest();
        master.setModality("DEFAULT");
        master.setDescription("Lorem ipsum");
        master.setName("MASTER");
        master.setValue(100000L);
        master.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "1000000");
        metadata.put("hit_value", "100");
        master.getPlanEndpoints().get(0).setMetadata(metadata);

        request.setPlans(Arrays.asList(basic, premium, master));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk()), ApiGatewayTenantResponse.class);

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        final CreatedResponse subscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()));

        // two days after subscribe, the user chose another plan
        when(clock.instant()).thenReturn(instant(2018, 11, 14));
        subscriptionRequest.setPlan(response.getPlans().get(1).getUuid());

        final CreatedResponse premiumSubscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription.uuid").value(premiumSubscription.getUuid()))
                .andExpect(jsonPath("next_subscription.subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("next_subscription.subscription_end").doesNotExist())
                .andExpect(jsonPath("next_subscription.plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("next_subscription.plan.name").value(premium.getName()))
                .andExpect(jsonPath("next_subscription.plan.value").value(premium.getValue()));

        // then he unsubscribed for the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(null);

        checkXss(mockMvc.perform(post(String.format("/subscription/tenant/%s/gateway/%s/api/%s/unsubscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription").doesNotExist());
    }

    @Test
    @FlywayTest
    public void testSubscriptionChangeThenChangeBack() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final Clock clock = Mockito.mock(Clock.class);
        gr1dClock.setClock(clock);
        when(clock.instant()).thenReturn(instant(2018, 11, 12));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        final HashMap<String, String> metadata = new HashMap<>();

        final PlanRequest basic = new PlanRequest();
        basic.setModality("DEFAULT");
        basic.setDescription("Lorem ipsum");
        basic.setName("BASIC");
        basic.setValue(2000L);
        basic.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.put("hits", "15000");
        metadata.put("hit_value", "1500");
        basic.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest premium = new PlanRequest();
        premium.setModality("DEFAULT");
        premium.setDescription("Lorem ipsum");
        premium.setName("PREMIUM");
        premium.setValue(10000L);
        premium.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "100000");
        metadata.put("hit_value", "1500");
        premium.getPlanEndpoints().get(0).setMetadata(metadata);

        request.setPlans(Arrays.asList(basic, premium));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk()), ApiGatewayTenantResponse.class);

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        final CreatedResponse subscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()));

        // two days after subscribe, the user chose another plan
        when(clock.instant()).thenReturn(instant(2018, 11, 14));
        subscriptionRequest.setPlan(response.getPlans().get(1).getUuid());

        final CreatedResponse premiumSubscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription.uuid").value(premiumSubscription.getUuid()))
                .andExpect(jsonPath("next_subscription.subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("next_subscription.subscription_end").doesNotExist())
                .andExpect(jsonPath("next_subscription.plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("next_subscription.plan.name").value(premium.getName()))
                .andExpect(jsonPath("next_subscription.plan.value").value(premium.getValue()));

        // then he unsubscribed for the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());

        getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription").doesNotExist());

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID))
                .header("X-Date", "2018-12-01"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription").doesNotExist());
    }

    @Test
    @FlywayTest
    public void testUnsubscriptionWithoutSubscription() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final Clock clock = Mockito.mock(Clock.class);
        gr1dClock.setClock(clock);
        when(clock.instant()).thenReturn(instant(2018, 11, 12));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        final HashMap<String, String> metadata = new HashMap<>();

        final PlanRequest basic = new PlanRequest();
        basic.setModality("DEFAULT");
        basic.setDescription("Lorem ipsum");
        basic.setName("BASIC");
        basic.setValue(2000L);
        basic.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.put("hits", "15000");
        metadata.put("hit_value", "1500");
        basic.getPlanEndpoints().get(0).setMetadata(metadata);

        request.setPlans(Collections.singletonList(basic));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk()), ApiGatewayTenantResponse.class);

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        // then he unsubscribed for the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(null);

        checkXss(mockMvc.perform(post(String.format("/subscription/tenant/%s/gateway/%s/api/%s/unsubscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testUnsubscriptionForAnInvalidPlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final Clock clock = Mockito.mock(Clock.class);
        gr1dClock.setClock(clock);
        when(clock.instant()).thenReturn(instant(2018, 11, 12));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        final HashMap<String, String> metadata = new HashMap<>();

        final PlanRequest basic = new PlanRequest();
        basic.setModality("DEFAULT");
        basic.setDescription("Lorem ipsum");
        basic.setName("BASIC");
        basic.setValue(2000L);
        basic.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.put("hits", "15000");
        metadata.put("hit_value", "1500");
        basic.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest premium = new PlanRequest();
        premium.setModality("DEFAULT");
        premium.setDescription("Lorem ipsum");
        premium.setName("PREMIUM");
        premium.setValue(10000L);
        premium.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "100000");
        metadata.put("hit_value", "1500");
        premium.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest master = new PlanRequest();
        master.setModality("DEFAULT");
        master.setDescription("Lorem ipsum");
        master.setName("MASTER");
        master.setValue(100000L);
        master.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "1000000");
        metadata.put("hit_value", "100");
        master.getPlanEndpoints().get(0).setMetadata(metadata);

        request.setPlans(Arrays.asList(basic, premium, master));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk()), ApiGatewayTenantResponse.class);

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        final CreatedResponse subscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()));

        // two days after subscribe, the user chose another plan
        when(clock.instant()).thenReturn(instant(2018, 11, 14));
        subscriptionRequest.setPlan(response.getPlans().get(1).getUuid());

        final CreatedResponse premiumSubscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription.uuid").value(premiumSubscription.getUuid()))
                .andExpect(jsonPath("next_subscription.subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("next_subscription.subscription_end").doesNotExist())
                .andExpect(jsonPath("next_subscription.plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("next_subscription.plan.name").value(premium.getName()))
                .andExpect(jsonPath("next_subscription.plan.value").value(premium.getValue()));

        // the next day the user changed his mind again and decided to stay with the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(response.getPlans().get(2).getUuid());

        checkXss(mockMvc.perform(post(String.format("/subscription/tenant/%s/gateway/%s/api/%s/unsubscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testResubscriptionForCurrentSubscription() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantFixtures.ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final Clock clock = Mockito.mock(Clock.class);
        gr1dClock.setClock(clock);
        when(clock.instant()).thenReturn(instant(2018, 11, 12));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        final HashMap<String, String> metadata = new HashMap<>();

        final PlanRequest basic = new PlanRequest();
        basic.setModality("DEFAULT");
        basic.setDescription("Lorem ipsum");
        basic.setName("BASIC");
        basic.setValue(2000L);
        basic.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.put("hits", "15000");
        metadata.put("hit_value", "1500");
        basic.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest premium = new PlanRequest();
        premium.setModality("DEFAULT");
        premium.setDescription("Lorem ipsum");
        premium.setName("PREMIUM");
        premium.setValue(10000L);
        premium.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "100000");
        metadata.put("hit_value", "1500");
        premium.getPlanEndpoints().get(0).setMetadata(metadata);

        final PlanRequest master = new PlanRequest();
        master.setModality("DEFAULT");
        master.setDescription("Lorem ipsum");
        master.setName("MASTER");
        master.setValue(100000L);
        master.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        metadata.clear();
        metadata.put("hits", "1000000");
        metadata.put("hit_value", "100");
        master.getPlanEndpoints().get(0).setMetadata(metadata);

        request.setPlans(Arrays.asList(basic, premium, master));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk()), ApiGatewayTenantResponse.class);

        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId(USER_ID);
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        final CreatedResponse subscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()));

        // two days after subscribe, the user chose another plan
        when(clock.instant()).thenReturn(instant(2018, 11, 14));
        subscriptionRequest.setPlan(response.getPlans().get(1).getUuid());

        final CreatedResponse premiumSubscription = getResult(checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk())), CreatedResponse.class);

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").value("2018-11-30"))
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next_subscription.uuid").value(premiumSubscription.getUuid()))
                .andExpect(jsonPath("next_subscription.subscription_start").value("2018-12-01"))
                .andExpect(jsonPath("next_subscription.subscription_end").doesNotExist())
                .andExpect(jsonPath("next_subscription.plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("next_subscription.plan.name").value(premium.getName()))
                .andExpect(jsonPath("next_subscription.plan.value").value(premium.getValue()));

        // the next day the user changed his mind again and decided to stay with the current plan
        when(clock.instant()).thenReturn(instant(2018, 11, 16));
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());

        checkXss(mockMvc.perform(post(String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(String.format("/subscription/tenant/%s/gateway/%s/api/%s/user/%s", tenant.getRealm(), gateway.getExternalId(), apiGateway.getProductionExternalId(), USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(subscription.getUuid()))
                .andExpect(jsonPath("subscription_start").value("2018-11-01"))
                .andExpect(jsonPath("subscription_end").doesNotExist())
                .andExpect(jsonPath("plan.modality").value("DEFAULT"))
                .andExpect(jsonPath("plan.name").value(basic.getName()))
                .andExpect(jsonPath("plan.value").value(basic.getValue()))
                .andExpect(jsonPath("next").doesNotExist());
    }

    private Instant instant(final int year, final int month, final int day) {
        return LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

}
