package io.gr1d.portal.subscriptions.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.gr1d.portal.subscriptions.TestUtils.checkXss;
import static io.gr1d.portal.subscriptions.TestUtils.getResult;
import static io.gr1d.portal.subscriptions.TestUtils.json;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayFixtures.ApiGatewayRule;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayTenantFixtures.ApiGatewayTenantRule;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.ApiGateway;
import io.gr1d.portal.subscriptions.model.ApiType;
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
import io.gr1d.portal.subscriptions.request.PlanEndpointRangeRequest;
import io.gr1d.portal.subscriptions.request.PlanEndpointRequest;
import io.gr1d.portal.subscriptions.request.PlanRequest;
import io.gr1d.portal.subscriptions.request.SubscriptionRequest;
import io.gr1d.portal.subscriptions.response.ApiGatewayTenantResponse;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class ApiGatewayTenantControllerIntegrationTest {

    private static final String URI = "/apiGatewayTenant";

    @Autowired private MockMvc mockMvc;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ApiRepository apiRepository;
    @Autowired private GatewayRepository gatewayRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ApiGatewayRepository apiGatewayRepository;
    @Autowired private Flyway flyway;

    private Tenant tenant;
    private ApiGateway apiGateway;
    private ApiGateway apiGatewayFree;
    private Gateway gateway;
    private Api api;
    private Api apiFree;
    private Provider providerPaid;
    private Provider providerFree;

    @Before
    public void init() {
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");
        final Api api = Fixture.from(Api.class).gimme("valid");
        final Api apiFree = Fixture.from(Api.class).gimme("valid");

        this.gateway = gatewayRepository.save(Fixture.from(Gateway.class).gimme("valid"));
        this.providerPaid = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        this.providerFree = providerRepository.save(Fixture.from(Provider.class).gimme("validFree"));

        api.setProvider(providerPaid);
        this.api = apiRepository.save(api);

        apiFree.setProvider(providerFree);
        this.apiFree = apiRepository.save(apiFree);

        this.tenant = tenantRepository.save(Fixture.from(Tenant.class).gimme("valid"));
        this.apiGateway = apiGatewayRepository.save(Fixture.from(ApiGateway.class)
                .gimme("valid", new ApiGatewayRule(api, gateway)));

        this.apiGatewayFree = apiGatewayRepository.save(Fixture.from(ApiGateway.class)
                .gimme("valid", new ApiGatewayRule(apiFree, gateway)));

        stubFor(WireMock.post(urlEqualTo(String.format("/gateways/%s/apis/%s/plan", gateway.getExternalId(), apiGateway.getExternalId())))
                .willReturn(aResponse().withStatus(200)));
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreation() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimited = new PlanRequest();
        unlimited.setModality("UNLIMITED");
        unlimited.setDescription("Lorem ipsum");
        unlimited.setName("BASIC");
        unlimited.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited")));
        unlimited.getPlanEndpoints().get(0).setEndpoint("/first");

        final PlanRequest defaultPlan = new PlanRequest();
        defaultPlan.setModality("DEFAULT");
        defaultPlan.setDescription("Ipsum lorem");
        defaultPlan.setName("PRO");
        defaultPlan.setValue(200L);
        defaultPlan.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_default")));
        defaultPlan.getPlanEndpoints().get(0).setEndpoint("/second");

        final PlanRequest freePlan = new PlanRequest();
        freePlan.setModality("FREE");
        freePlan.setDescription("Ilopsum rem");
        freePlan.setName("FREE");
        freePlan.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_free")));
        freePlan.getPlanEndpoints().get(0).setEndpoint("/third");

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));
        unlimitedRange.getPlanEndpoints().get(0).setEndpoint("/fourth");

        request.setPlans(Arrays.asList(unlimited, defaultPlan, freePlan, unlimitedRange));

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

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid))
                .andExpect(jsonPath("content[0].type").value(ApiType.FREEMIUM.getName()))
                .andExpect(jsonPath("content[0].tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("content[0].api_gateway.name").value(api.getName()))
                .andExpect(jsonPath("content[0].api_gateway.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("content[0].api_gateway.provider.uuid").value(providerPaid.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.provider.name").value(providerPaid.getName()))
                .andExpect(jsonPath("content[0].api_gateway.provider.phone").value(providerPaid.getPhone()))
                .andExpect(jsonPath("content[0].api_gateway.provider.wallet_id").value(providerPaid.getWalletId()));

        ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("type").value(ApiType.FREEMIUM.getName()))
                .andExpect(jsonPath("tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("api_gateway.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("api_gateway.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("api_gateway.name").value(api.getName()))
                .andExpect(jsonPath("api_gateway.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("api_gateway.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("api_gateway.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("api_gateway.provider.uuid").value(providerPaid.getUuid()))
                .andExpect(jsonPath("api_gateway.provider.name").value(providerPaid.getName()))
                .andExpect(jsonPath("api_gateway.provider.phone").value(providerPaid.getPhone()))
                .andExpect(jsonPath("api_gateway.provider.wallet_id").value(providerPaid.getWalletId()))
                .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(4)))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                //TODO Bug na edição do nome
//                .andExpect(jsonPath("plans[0].plan_endpoints[0].name").value(unlimited.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].endpoint").value(unlimited.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                .andExpect(jsonPath("plans[1].modality").value("DEFAULT"))
                .andExpect(jsonPath("plans[1].name").value(defaultPlan.getName()))
                .andExpect(jsonPath("plans[1].description").value(defaultPlan.getDescription()))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].name").value(defaultPlan.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].endpoint").value(defaultPlan.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.hit_value").value(defaultPlan.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.hits").value(defaultPlan.getPlanEndpoints().get(0).getMetadata().get("hits")))
                .andExpect(jsonPath("plans[2].modality").value("FREE"))
                .andExpect(jsonPath("plans[2].name").value(freePlan.getName()))
                .andExpect(jsonPath("plans[2].description").value(freePlan.getDescription()))
                //TODO Bug na edição do nome
//                .andExpect(jsonPath("plans[2].plan_endpoints[0].name").value(freePlan.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[2].plan_endpoints[0].endpoint").value(freePlan.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[2].plan_endpoints[0].metadata.limit").value(freePlan.getPlanEndpoints().get(0).getMetadata().get("limit")))
                .andExpect(jsonPath("plans[3].modality").value("UNLIMITED_RANGE"))
                .andExpect(jsonPath("plans[3].name").value(unlimitedRange.getName()))
                .andExpect(jsonPath("plans[3].value").value(unlimitedRange.getValue()))
                .andExpect(jsonPath("plans[3].description").value(unlimitedRange.getDescription()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].name").value(unlimitedRange.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].endpoint").value(unlimitedRange.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].method_type").value(unlimitedRange.getPlanEndpoints().get(0).getMethodType()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges").value(hasSize(unlimitedRange.getPlanEndpoints().get(0).getRanges().size())))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].init_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].final_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].value").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].init_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].final_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].value").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].init_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(2).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].value").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(2).getValue()))
           , ApiGatewayTenantResponse.class);

        PlanRequest unlimitedPlan2 = response.getPlans().get(0).toRequest();
        PlanRequest defaultPlan2 = response.getPlans().get(1).toRequest();
        PlanRequest freePlan2 = response.getPlans().get(2).toRequest();
        PlanRequest unlimitedRangePlan2 = response.getPlans().get(3).toRequest();
        ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Arrays.asList(freePlan2, defaultPlan2, unlimitedPlan2, unlimitedRangePlan2), BigDecimal.valueOf(3.33), Collections.emptyList());

        final Map<String, String> metadata = defaultPlan2.getPlanEndpoints().get(0).getMetadata();
        metadata.put("hit_value", "1100");
        metadata.put("hits", "99000");

        freePlan2.setName("SUPER FREE");

        unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).setFinalRange(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getFinalRange() + 2);
        unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).setInitRange(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getInitRange() + 2);
        unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).setFinalRange(50L);
        PlanEndpointRangeRequest planEndpointRangeRequest = new PlanEndpointRangeRequest();
        planEndpointRangeRequest.setInitRange(51L);
        planEndpointRangeRequest.setValue(10L);
        unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().add(planEndpointRangeRequest);

        defaultPlan2.setDirty(true);
        freePlan2.setDirty(true);
        unlimitedRangePlan2.setDirty(true);

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.FREEMIUM.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(4)))
                .andExpect(jsonPath("plans[0].modality").value("FREE"))
                .andExpect(jsonPath("plans[0].name").value(freePlan2.getName()))
                .andExpect(jsonPath("plans[0].description").value(freePlan2.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.limit").value(freePlan2.getPlanEndpoints().get(0).getMetadata().get("limit")))
                .andExpect(jsonPath("plans[0].uuid").value(freePlan2.getUuid()))
                .andExpect(jsonPath("plans[1].modality").value("DEFAULT"))
                .andExpect(jsonPath("plans[1].name").value(defaultPlan2.getName()))
                .andExpect(jsonPath("plans[1].description").value(defaultPlan2.getDescription()))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.hit_value").value(metadata.get("hit_value")))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.hits").value(metadata.get("hits")))
                .andExpect(jsonPath("plans[1].uuid").value(not(defaultPlan2.getUuid()))) // a new plan should be created
                .andExpect(jsonPath("plans[2].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[2].name").value(unlimitedPlan2.getName()))
                .andExpect(jsonPath("plans[2].description").value(unlimitedPlan2.getDescription()))
                .andExpect(jsonPath("plans[2].plan_endpoints[0].metadata.hit_value").value(unlimitedPlan2.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                .andExpect(jsonPath("plans[2].uuid").value(unlimitedPlan2.getUuid()))
                .andExpect(jsonPath("plans[3].modality").value("UNLIMITED_RANGE"))
                .andExpect(jsonPath("plans[3].uuid").value(not(unlimitedRangePlan2.getUuid()))) // a new plan should be created
                .andExpect(jsonPath("plans[3].name").value(unlimitedRangePlan2.getName()))
                .andExpect(jsonPath("plans[3].value").value(unlimitedRangePlan2.getValue()))
                .andExpect(jsonPath("plans[3].description").value(unlimitedRangePlan2.getDescription()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].name").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].endpoint").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].method_type").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getMethodType()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges").value(hasSize(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().size())))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].final_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].final_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].final_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[3].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(3).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[3].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(3).getValue()))
                , ApiGatewayTenantResponse.class);

        unlimitedPlan2 = response.getPlans().get(2).toRequest();
        defaultPlan2 = response.getPlans().get(1).toRequest();
        freePlan2 = response.getPlans().get(0).toRequest();
        unlimitedRangePlan2 = response.getPlans().get(3).toRequest();
        unlimitedRangePlan2.getPlanEndpoints().get(0).setName("CHANGED");
        updateRequest = new ApiGatewayTenantUpdateRequest(Arrays.asList(freePlan2, defaultPlan2, unlimitedPlan2, unlimitedRangePlan2), BigDecimal.valueOf(20.54), Collections.emptyList());

        unlimitedRangePlan2.setDirty(true);

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.FREEMIUM.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(4)))
                .andExpect(jsonPath("plans[0].modality").value("FREE"))
                .andExpect(jsonPath("plans[0].name").value(freePlan2.getName()))
                .andExpect(jsonPath("plans[0].description").value(freePlan2.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.limit").value(freePlan2.getPlanEndpoints().get(0).getMetadata().get("limit")))
                .andExpect(jsonPath("plans[0].uuid").value(freePlan2.getUuid()))
                .andExpect(jsonPath("plans[1].modality").value("DEFAULT"))
                .andExpect(jsonPath("plans[1].name").value(defaultPlan2.getName()))
                .andExpect(jsonPath("plans[1].description").value(defaultPlan2.getDescription()))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.hit_value").value(metadata.get("hit_value")))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.hits").value(metadata.get("hits")))
                .andExpect(jsonPath("plans[1].uuid").value(defaultPlan2.getUuid()))
                .andExpect(jsonPath("plans[2].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[2].name").value(unlimitedPlan2.getName()))
                .andExpect(jsonPath("plans[2].description").value(unlimitedPlan2.getDescription()))
                .andExpect(jsonPath("plans[2].plan_endpoints[0].metadata.hit_value").value(unlimitedPlan2.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                .andExpect(jsonPath("plans[2].uuid").value(unlimitedPlan2.getUuid()))
                .andExpect(jsonPath("plans[3].modality").value("UNLIMITED_RANGE"))
                .andExpect(jsonPath("plans[3].name").value(unlimitedRangePlan2.getName()))
                .andExpect(jsonPath("plans[3].value").value(unlimitedRangePlan2.getValue()))
                .andExpect(jsonPath("plans[3].description").value(unlimitedRangePlan2.getDescription()))
                .andExpect(jsonPath("plans[3].uuid").value(unlimitedRangePlan2.getUuid()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].name").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].endpoint").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].method_type").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getMethodType()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges").value(hasSize(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().size())))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].final_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(0).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].final_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(1).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].final_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(2).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[3].init_range").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(3).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[3].value").value(unlimitedRangePlan2.getPlanEndpoints().get(0).getRanges().get(3).getValue()));
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationFreeApiAndUpdateFreemiumApi() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest freePlan1 = new PlanRequest();
        freePlan1.setModality("FREE");
        freePlan1.setDescription("Lorem ipsum");
        freePlan1.setName("BASIC");
        freePlan1.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_free")));
        freePlan1.getPlanEndpoints().get(0).setEndpoint("/first");

        final PlanRequest freePlan2 = new PlanRequest();
        freePlan2.setModality("FREE");
        freePlan2.setDescription("Ipsum lorem");
        freePlan2.setName("PRO");
        freePlan2.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_free")));
        freePlan2.getPlanEndpoints().get(0).setEndpoint("/second");

        final PlanRequest freePlan3 = new PlanRequest();
        freePlan3.setModality("FREE");
        freePlan3.setDescription("Ilopsum rem");
        freePlan3.setName("FREE");
        freePlan3.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_free")));
        freePlan3.getPlanEndpoints().get(0).setEndpoint("/third");

        request.setPlans(Arrays.asList(freePlan1, freePlan2, freePlan3));

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

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid))
                .andExpect(jsonPath("content[0].type").value(ApiType.FREE.getName()))
                .andExpect(jsonPath("content[0].tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("content[0].api_gateway.name").value(api.getName()))
                .andExpect(jsonPath("content[0].api_gateway.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("content[0].api_gateway.provider.uuid").value(providerPaid.getUuid()))
                .andExpect(jsonPath("content[0].api_gateway.provider.name").value(providerPaid.getName()))
                .andExpect(jsonPath("content[0].api_gateway.provider.phone").value(providerPaid.getPhone()))
                .andExpect(jsonPath("content[0].api_gateway.provider.wallet_id").value(providerPaid.getWalletId()));

        ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("uuid").value(uuid))
                        .andExpect(jsonPath("type").value(ApiType.FREE.getName()))
                        .andExpect(jsonPath("tenant.uuid").value(tenant.getUuid()))
                        .andExpect(jsonPath("api_gateway.uuid").value(apiGateway.getUuid()))
                        .andExpect(jsonPath("api_gateway.external_id").value(apiGateway.getExternalId()))
                        .andExpect(jsonPath("api_gateway.name").value(api.getName()))
                        .andExpect(jsonPath("api_gateway.api_uuid").value(api.getUuid()))
                        .andExpect(jsonPath("api_gateway.gateway.uuid").value(gateway.getUuid()))
                        .andExpect(jsonPath("api_gateway.gateway.name").value(gateway.getName()))
                        .andExpect(jsonPath("api_gateway.provider.uuid").value(providerPaid.getUuid()))
                        .andExpect(jsonPath("api_gateway.provider.name").value(providerPaid.getName()))
                        .andExpect(jsonPath("api_gateway.provider.phone").value(providerPaid.getPhone()))
                        .andExpect(jsonPath("api_gateway.provider.wallet_id").value(providerPaid.getWalletId()))
                        .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                        .andExpect(jsonPath("plans").value(hasSize(3)))
                        .andExpect(jsonPath("plans[0].modality").value("FREE"))
                        .andExpect(jsonPath("plans[0].name").value(freePlan1.getName()))
                        .andExpect(jsonPath("plans[0].description").value(freePlan1.getDescription()))
                        //TODO Bug na edição do nome
//                        .andExpect(jsonPath("plans[0].plan_endpoints[0].name").value(freePlan1.getPlanEndpoints().get(0).getName()))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].endpoint").value(freePlan1.getPlanEndpoints().get(0).getEndpoint()))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].method_type").value(freePlan1.getPlanEndpoints().get(0).getMethodType()))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.limit").value(freePlan1.getPlanEndpoints().get(0).getMetadata().get("limit")))
                        .andExpect(jsonPath("plans[1].modality").value("FREE"))
                        .andExpect(jsonPath("plans[1].name").value(freePlan2.getName()))
                        .andExpect(jsonPath("plans[1].description").value(freePlan2.getDescription()))
                        //TODO Bug na edição do nome
//                        .andExpect(jsonPath("plans[1].plan_endpoints[0].name").value(freePlan2.getPlanEndpoints().get(0).getName()))
                        .andExpect(jsonPath("plans[1].plan_endpoints[0].endpoint").value(freePlan2.getPlanEndpoints().get(0).getEndpoint()))
                        .andExpect(jsonPath("plans[1].plan_endpoints[0].method_type").value(freePlan2.getPlanEndpoints().get(0).getMethodType()))
                        .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.limit").value(freePlan2.getPlanEndpoints().get(0).getMetadata().get("limit")))
                        .andExpect(jsonPath("plans[2].modality").value("FREE"))
                        //TODO Bug na edição do nome
//                        .andExpect(jsonPath("plans[2].name").value(freePlan3.getName()))
                        .andExpect(jsonPath("plans[2].description").value(freePlan3.getDescription()))
                        .andExpect(jsonPath("plans[2].plan_endpoints[0].name").value(freePlan3.getPlanEndpoints().get(0).getName()))
                        .andExpect(jsonPath("plans[2].plan_endpoints[0].endpoint").value(freePlan3.getPlanEndpoints().get(0).getEndpoint()))
                        .andExpect(jsonPath("plans[2].plan_endpoints[0].method_type").value(freePlan3.getPlanEndpoints().get(0).getMethodType()))
                        .andExpect(jsonPath("plans[2].plan_endpoints[0].metadata.limit").value(freePlan3.getPlanEndpoints().get(0).getMetadata().get("limit")))
                , ApiGatewayTenantResponse.class);

        final PlanRequest freePlan1_2 = response.getPlans().get(0).toRequest();
        final PlanRequest freePlan2_2 = response.getPlans().get(1).toRequest();
        final PlanRequest freePlan3_2 = response.getPlans().get(2).toRequest();

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        final ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Arrays.asList(freePlan3_2, freePlan2_2, freePlan1_2, unlimitedRange), BigDecimal.valueOf(5.55), Collections.emptyList());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.FREEMIUM.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(4)))
                .andExpect(jsonPath("plans[0].modality").value("FREE"))
                .andExpect(jsonPath("plans[0].name").value(freePlan3_2.getName()))
                .andExpect(jsonPath("plans[0].description").value(freePlan3_2.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.limit").value(freePlan3_2.getPlanEndpoints().get(0).getMetadata().get("limit")))
                .andExpect(jsonPath("plans[0].uuid").value(freePlan3_2.getUuid()))
                .andExpect(jsonPath("plans[1].modality").value("FREE"))
                .andExpect(jsonPath("plans[1].name").value(freePlan2_2.getName()))
                .andExpect(jsonPath("plans[1].description").value(freePlan2_2.getDescription()))
                .andExpect(jsonPath("plans[1].plan_endpoints[0].metadata.limit").value(freePlan2_2.getPlanEndpoints().get(0).getMetadata().get("limit")))
                .andExpect(jsonPath("plans[1].uuid").value(freePlan2_2.getUuid()))
                .andExpect(jsonPath("plans[2].modality").value("FREE"))
                .andExpect(jsonPath("plans[2].name").value(freePlan1_2.getName()))
                .andExpect(jsonPath("plans[2].description").value(freePlan1_2.getDescription()))
                .andExpect(jsonPath("plans[2].plan_endpoints[0].metadata.limit").value(freePlan1_2.getPlanEndpoints().get(0).getMetadata().get("limit")))
                .andExpect(jsonPath("plans[2].uuid").value(freePlan1_2.getUuid()))
                .andExpect(jsonPath("plans[3].modality").value("UNLIMITED_RANGE"))
                .andExpect(jsonPath("plans[3].uuid").value(not(unlimitedRange.getUuid())))
                .andExpect(jsonPath("plans[3].name").value(unlimitedRange.getName()))
                .andExpect(jsonPath("plans[3].value").value(unlimitedRange.getValue()))
                .andExpect(jsonPath("plans[3].description").value(unlimitedRange.getDescription()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].name").value(unlimitedRange.getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].endpoint").value(unlimitedRange.getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].method_type").value(unlimitedRange.getPlanEndpoints().get(0).getMethodType()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges").value(hasSize(unlimitedRange.getPlanEndpoints().get(0).getRanges().size())))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].init_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].final_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[0].value").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].init_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).getInitRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].final_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).getFinalRange()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[1].value").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).getValue()))
                .andExpect(jsonPath("plans[3].plan_endpoints[0].ranges[2].init_range").value(unlimitedRange.getPlanEndpoints().get(0).getRanges().get(2).getInitRange()));
    }

    @Test
    @FlywayTest
    public void testChangePlanWithActiveSubscription() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
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
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        // subscription
        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId("f56b9b96-1905-4a48-8896-3e34b3e1ba5a");
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk()));

        final PlanRequest planRequest = response.getPlans().get(0).toRequest();
        final Map<String, String> metadata = planRequest.getPlanEndpoints().get(0).getMetadata();
        metadata.put("hit_value", "489485");
        planRequest.setDirty(true);

        final ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Collections.singletonList(planRequest), BigDecimal.valueOf(4.44), Collections.emptyList());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].uuid").value(not(response.getPlans().get(0).getUuid())))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value("489485"));
    }

    @Test
    @FlywayTest
    public void testChangePlanEndpointNameOnly() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
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
                        .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                        .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                        .andExpect(jsonPath("plans").value(hasSize(1)))
                        .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                        .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                        .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        // subscription
        final SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        subscriptionRequest.setPlan(response.getPlans().get(0).getUuid());
        subscriptionRequest.setUserId("f56b9b96-1905-4a48-8896-3e34b3e1ba5a");
        subscriptionRequest.setPaymentMethod("CREDIT_CARD");

        final String subscribeUrl = String.format("/subscription/tenant/%s/gateway/%s/api/%s/subscribe", tenant.getRealm(), gateway.getExternalId(), apiGateway.getExternalId());
        checkXss(mockMvc.perform(post(subscribeUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(subscriptionRequest))).andDo(print()).andExpect(status().isOk()));

        final PlanRequest planRequest = response.getPlans().get(0).toRequest();
        final PlanEndpointRequest endpointRequest = planRequest.getPlanEndpoints().get(0);
        endpointRequest.setName("Another name");
        planRequest.setDirty(true);

        final ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Collections.singletonList(planRequest), BigDecimal.valueOf(1.11), Collections.emptyList());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].uuid").value(response.getPlans().get(0).getUuid()))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].name").value("Another name"))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")));
    }

    @Test
    @FlywayTest
    public void testChangePlanValue() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimited = new PlanRequest();
        unlimited.setValue(15000L);
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
                        .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                        .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                        .andExpect(jsonPath("plans").value(hasSize(1)))
                        .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                        .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                        .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        final PlanRequest planRequest = response.getPlans().get(0).toRequest();
        planRequest.setValue(28000L);
        planRequest.setDirty(true);

        final ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Collections.singletonList(planRequest), BigDecimal.valueOf(1.11), Collections.emptyList());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].uuid").value(not(response.getPlans().get(0).getUuid())))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"));
    }

    @Test
    @FlywayTest
    public void testRemovePlanEndpoint() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimited = new PlanRequest();
        unlimited.setModality("UNLIMITED");
        unlimited.setDescription("Lorem ipsum");
        unlimited.setName("BASIC");
        unlimited.setPlanEndpoints(Arrays.asList(
                Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited"),
                Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_2")
        ));
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
                        .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                        .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                        .andExpect(jsonPath("plans").value(hasSize(1)))
                        .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                        .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                        .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                        .andExpect(jsonPath("plans[0].plan_endpoints").value(hasSize(2)))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                        .andExpect(jsonPath("plans[0].plan_endpoints[1].metadata.hit_value").value(unlimited.getPlanEndpoints().get(1).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        final PlanRequest planRequest = response.getPlans().get(0).toRequest();
        planRequest.getPlanEndpoints().remove(1);
        planRequest.setDirty(true);

        final ApiGatewayTenantUpdateRequest updateRequest = new ApiGatewayTenantUpdateRequest(Collections.singletonList(planRequest), BigDecimal.valueOf(2.22), Collections.emptyList());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("percentage_split").value(updateRequest.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].uuid").value(not(response.getPlans().get(0).getUuid())))
                .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[0].plan_endpoints").value(hasSize(1)))
                .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")));
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantRemoval() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid));
        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid));

        mockMvc.perform(delete(uriSingle)).andDo(print()).andExpect(status().isOk());
        mockMvc.perform(get(uriSingle)).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("removed_at").isNotEmpty());
        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
        mockMvc.perform(put(uriSingle)).andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantUniqueness() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].meta").value("api_gateway_uuid"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"))
                .andExpect(jsonPath("$[0].message").value("Já existe uma associação desta API a este Parceiro"));

        mockMvc.perform(delete(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @FlywayTest
    public void testListApiGatewayTenantByTenant() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());

        mockMvc.perform(get(String.format("%s?tenant=%s", URI, tenant.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?tenant=somerandomuid", URI))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testFindApiByExternalIdAndTenant() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());

        mockMvc.perform(get(String.format("/api/%s/tenant/%s", apiGateway.getExternalId(), tenant.getRealm()))).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format("/v1/tenant/%s/api/%s", tenant.getRealm(), apiGateway.getExternalId()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("tenant.uuid").value(tenant.getUuid()))
                .andExpect(jsonPath("api_gateway.uuid").value(apiGateway.getUuid()))
                .andExpect(jsonPath("api_gateway.external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("api_gateway.name").value(api.getName()))
                .andExpect(jsonPath("api_gateway.api_uuid").value(api.getUuid()))
                .andExpect(jsonPath("api_gateway.gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("api_gateway.gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("api_gateway.provider.uuid").value(providerPaid.getUuid()))
                .andExpect(jsonPath("api_gateway.provider.name").value(providerPaid.getName()))
                .andExpect(jsonPath("api_gateway.provider.phone").value(providerPaid.getPhone()))
                .andExpect(jsonPath("api_gateway.provider.wallet_id").value(providerPaid.getWalletId()));

        mockMvc.perform(get(String.format("/v1/tenant/%s/api", tenant.getRealm()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").isString())
                .andExpect(jsonPath("$[0].external_id").value(apiGateway.getExternalId()))
                .andExpect(jsonPath("$[0].type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("$[0].tenant_realm").value(tenant.getRealm()))
                .andExpect(jsonPath("$[0].gateway.uuid").value(gateway.getUuid()))
                .andExpect(jsonPath("$[0].gateway.name").value(gateway.getName()))
                .andExpect(jsonPath("$[0].gateway.external_id").value(gateway.getExternalId()))
                .andExpect(jsonPath("$[0].api.name").value(api.getName()))
                .andExpect(jsonPath("$[0].api.uuid").value(api.getUuid()))
                .andExpect(jsonPath("$[0].api.external_id").value(api.getExternalId()))
                .andExpect(jsonPath("$[0].api.provider.uuid").value(providerPaid.getUuid()))
                .andExpect(jsonPath("$[0].api.provider.name").value(providerPaid.getName()))
                .andExpect(jsonPath("$[0].api.provider.phone").value(providerPaid.getPhone()))
                .andExpect(jsonPath("$[0].api.provider.wallet_id").value(providerPaid.getWalletId()));
    }

    @Test
    @FlywayTest
    public void testListApiGatewayTenantByGateway() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());

        mockMvc.perform(get(String.format("%s?gateway=%s", URI, gateway.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?gateway=somerandomuid", URI))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testUpdateWithNoPlans() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                .andExpect(jsonPath("plans").value(hasSize(1)));

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(new ApiGatewayTenantUpdateRequest(Collections.emptyList(), BigDecimal.ONE, Collections.emptyList())))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.FREE.getName()))
                .andExpect(jsonPath("plans").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testListApiGatewayTenantByApiExternalId() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());

        mockMvc.perform(get(String.format("%s?api_external_id=%s", URI, apiGateway.getExternalId()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?api_external_id=%s&gateway=%s", URI, apiGateway.getExternalId(), gateway.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?api_external_id=somerandomuid", URI))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testNotFound() throws Exception {
        final ApiGatewayTenantUpdateRequest request = Fixture.from(ApiGatewayTenantUpdateRequest.class).gimme("valid");
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));
        
        final String uri = String.format("%s/12y8e9129y8h", URI);

        mockMvc.perform(get(uri)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());


        checkXss(mockMvc.perform(put(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andDo(print())
                .andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testValidations() throws Exception {
        final ApiGatewayTenantCreateRequest invalidHitValue = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("invalidHitValue", new ApiGatewayTenantRule(apiGateway, tenant));
        final ApiGatewayTenantCreateRequest invalidLimit = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("invalidLimit", new ApiGatewayTenantRule(apiGateway, tenant));
        final ApiGatewayTenantCreateRequest hitValueTooLarge = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("hitValueTooLarge", new ApiGatewayTenantRule(apiGateway, tenant));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidHitValue)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidLimit)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(hitValueTooLarge)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationPlanWithoutPlanEndpoint() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));
        
        final PlanRequest unlimited = new PlanRequest();
        unlimited.setModality("UNLIMITED");
        unlimited.setDescription("Lorem ipsum");
        unlimited.setName("BASIC");

        final PlanRequest defaultPlan = new PlanRequest();
        defaultPlan.setModality("DEFAULT");
        defaultPlan.setDescription("Ipsum lorem");
        defaultPlan.setName("PRO");
        defaultPlan.setValue(200L);

        final PlanRequest freePlan = new PlanRequest();
        freePlan.setModality("FREE");
        freePlan.setDescription("Ilopsum rem");
        freePlan.setName("FREE");

        request.setPlans(Arrays.asList(unlimited, defaultPlan, freePlan));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("type").value(ApiType.FREEMIUM.getName()))
                .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                .andExpect(jsonPath("plans").value(hasSize(3)))
                .andExpect(jsonPath("plans[0].modality").value(unlimited.getModality()))
                .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plans[1].modality").value(defaultPlan.getModality()))
                .andExpect(jsonPath("plans[1].name").value(defaultPlan.getName()))
                .andExpect(jsonPath("plans[1].description").value(defaultPlan.getDescription()))
                .andExpect(jsonPath("plans[2].modality").value(freePlan.getModality()))
                .andExpect(jsonPath("plans[2].name").value(freePlan.getName()))
                .andExpect(jsonPath("plans[2].description").value(freePlan.getDescription()))
        ;
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidInitValueUnlimitedRangePlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).setInitRange(1L);

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].plan_endpoints[0].ranges[0].init_range"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));
    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidFinalValueUnlimitedRangePlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        unlimitedRange.getPlanEndpoints().get(0).getRanges().get(0).setFinalRange(300L);

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].plan_endpoints[0].ranges[1].init_range"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidLastFinalValueUnlimitedRangePlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        unlimitedRange.getPlanEndpoints().get(0).getRanges().get(2).setFinalRange(500L);

        request.setPlans(Collections.singletonList(unlimitedRange));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].plan_endpoints[0].ranges[2].final_range"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidModality() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality(null);
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].modality"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

        unlimitedRange.setModality("INVALID");
        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].modality"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidValueUnlimitedRangePlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(null);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].value"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationUnlimitedRangePlanWithoutRanges() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(200L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        unlimitedRange.getPlanEndpoints().get(0).setRanges(null);

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].plan_endpoints[0].ranges"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidRangeUnlimitedRangePlan() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));
        
        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(50L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).setInitRange(20L);
        unlimitedRange.getPlanEndpoints().get(0).getRanges().get(1).setFinalRange(19L);

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans[0].plan_endpoints[0].ranges[1].final_range"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationInvalidPartnerFree() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGatewayFree, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimitedRange = new PlanRequest();
        unlimitedRange.setModality("UNLIMITED_RANGE");
        unlimitedRange.setDescription("Lorem ipsum");
        unlimitedRange.setName("ENTERPRISE");
        unlimitedRange.setValue(55L);
        unlimitedRange.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited_range")));

        request.setPlans(Collections.singletonList(unlimitedRange));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].meta").value("plans"))
                .andExpect(jsonPath("$[0].error").value("ObjectError"));

    }

    @Test
    @FlywayTest
    public void testApiGatewayTenantCreationValidPartnerFree() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGatewayFree, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest free = new PlanRequest();
        free.setModality("FREE");
        free.setDescription("Lorem ipsum");
        free.setName("FREE");
        free.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_free")));

        request.setPlans(Collections.singletonList(free));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
    }

    @Test
    @FlywayTest
    public void testGetPlanInfo() throws Exception {
        final ApiGatewayTenantCreateRequest request = Fixture.from(ApiGatewayTenantCreateRequest.class)
                .gimme("valid", new ApiGatewayTenantRule(apiGateway, tenant));
        request.setPaymentMethods(Arrays.asList("CREDIT_CARD"));

        final PlanRequest unlimited = new PlanRequest();
        unlimited.setValue(15000L);
        unlimited.setModality("UNLIMITED");
        unlimited.setDescription("Lorem ipsum");
        unlimited.setName("BASIC");
        unlimited.setPlanEndpoints(Collections.singletonList(Fixture.from(PlanEndpointRequest.class).gimme("valid_unlimited")));

        request.setPlans(Collections.singletonList(unlimited));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse result = getResult(actions, CreatedResponse.class);
        final String uuid = result.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);
        final ApiGatewayTenantResponse response = getResult(mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("type").value(ApiType.PAID.getName()))
                        .andExpect(jsonPath("percentage_split").value(request.getPercentageSplit()))
                        .andExpect(jsonPath("plans").value(hasSize(1)))
                        .andExpect(jsonPath("plans[0].modality").value("UNLIMITED"))
                        .andExpect(jsonPath("plans[0].name").value(unlimited.getName()))
                        .andExpect(jsonPath("plans[0].description").value(unlimited.getDescription()))
                        .andExpect(jsonPath("plans[0].plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")))
                , ApiGatewayTenantResponse.class);

        mockMvc.perform(get(String.format("/plan/%s", response.getPlans().get(0).getUuid()))
                .accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("modality").value("UNLIMITED"))
                .andExpect(jsonPath("name").value(unlimited.getName()))
                .andExpect(jsonPath("description").value(unlimited.getDescription()))
                .andExpect(jsonPath("plan_endpoints[0].metadata.hit_value").value(unlimited.getPlanEndpoints().get(0).getMetadata().get("hit_value")));
    }

}
