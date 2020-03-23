package io.gr1d.portal.subscriptions.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayFixtures.ApiGatewayRequestRule;
import io.gr1d.portal.subscriptions.model.Api;
import io.gr1d.portal.subscriptions.model.Gateway;
import io.gr1d.portal.subscriptions.model.Provider;
import io.gr1d.portal.subscriptions.repository.ApiRepository;
import io.gr1d.portal.subscriptions.repository.GatewayRepository;
import io.gr1d.portal.subscriptions.repository.ProviderRepository;
import io.gr1d.portal.subscriptions.request.ApiGatewayCreateRequest;
import io.gr1d.portal.subscriptions.request.ApiGatewayUpdateRequest;
import org.assertj.core.api.Assertions;
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

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.gr1d.portal.subscriptions.TestUtils.checkXss;
import static io.gr1d.portal.subscriptions.TestUtils.getResult;
import static io.gr1d.portal.subscriptions.TestUtils.json;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class ApiGatewayControllerIntegrationTest {

    private static final String URI = "/apiGateway";

    @Autowired private MockMvc mockMvc;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ApiRepository apiRepository;
    @Autowired private GatewayRepository gatewayRepository;
    @Autowired private Flyway flyway;

    private Provider provider;
    private Api api;
    private Api anotherApi;
    private Gateway gateway;

    @Before
    public void init() {
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");

        this.provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final Api api = Fixture.from(Api.class).gimme("valid");
        api.setProvider(provider);

        final Api anotherApi = Fixture.from(Api.class).gimme("valid");
        anotherApi.setProvider(provider);
        this.anotherApi = apiRepository.save(anotherApi);

        this.api = apiRepository.save(api);
        this.gateway = gatewayRepository.save(Fixture.from(Gateway.class).gimme("valid"));
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
    }

    @Test
    @FlywayTest
    public void testApiGatewayCreationAndUpdate() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid))
                .andExpect(jsonPath("content[0].external_id").value(request.getExternalId()))
                .andExpect(jsonPath("content[0].production_external_id").value(request.getProductionExternalId()))
                .andExpect(jsonPath("content[0].api.uuid").value(api.getUuid()))
                .andExpect(jsonPath("content[0].api.name").value(api.getName()))
                .andExpect(jsonPath("content[0].api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("content[0].api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("content[0].api.provider.wallet_id").value(provider.getWalletId()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("external_id").value(request.getExternalId()))
                .andExpect(jsonPath("api.uuid").value(api.getUuid()))
                .andExpect(jsonPath("api.name").value(api.getName()))
                .andExpect(jsonPath("api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("api.provider.wallet_id").value(provider.getWalletId()));

        final ApiGatewayUpdateRequest anotherRequest = Fixture.from(ApiGatewayUpdateRequest.class).gimme("valid");
        Assertions.assertThat(anotherRequest.getExternalId()).isNotEqualTo(request.getExternalId());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("external_id").value(anotherRequest.getExternalId()))
                .andExpect(jsonPath("production_external_id").value(anotherRequest.getProductionExternalId()))
                .andExpect(jsonPath("api.uuid").value(api.getUuid()))
                .andExpect(jsonPath("api.name").value(api.getName()))
                .andExpect(jsonPath("api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("api.provider.wallet_id").value(provider.getWalletId()));
    }

    @Test
    @FlywayTest
    public void testValidateUniqueApiGateway() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));
        final ApiGatewayCreateRequest anotherRequest = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testValidateUniqueExternalId() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));
        final ApiGatewayCreateRequest anotherRequest = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(anotherApi, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        anotherRequest.setExternalId(request.getExternalId());
        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isUnprocessableEntity()));

        anotherRequest.setExternalId(UUID.randomUUID().toString());
        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isCreated()));

        final ApiGatewayUpdateRequest updateRequest = new ApiGatewayUpdateRequest();
        updateRequest.setExternalId(anotherRequest.getExternalId());
        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(updateRequest))).andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testApiGatewayRemoval() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", URI, uuid);

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("removed_at").doesNotExist())
                .andExpect(jsonPath("external_id").value(request.getExternalId()))
                .andExpect(jsonPath("production_external_id").value(request.getProductionExternalId()))
                .andExpect(jsonPath("api.uuid").value(api.getUuid()))
                .andExpect(jsonPath("api.name").value(api.getName()))
                .andExpect(jsonPath("api.provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("api.provider.name").value(provider.getName()))
                .andExpect(jsonPath("api.provider.wallet_id").value(provider.getWalletId()));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(delete(uriSingle)).andDo(print()).andExpect(status().isOk());

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("removed_at").isNotEmpty());
        mockMvc.perform(delete(uriSingle)).andDo(print()).andExpect(status().isNotFound());

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testListApiGatewayByApi() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));

        mockMvc.perform(get(String.format("%s?api=%s", URI, api.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?api=somerandomuid", URI))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testListApiGatewayByGateway() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));

        mockMvc.perform(get(String.format("%s?gateway=%s", URI, gateway.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?gateway=somerandomuid", URI))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testListApiGatewayByExternalId() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));

        mockMvc.perform(get(String.format("%s?external_id=%s", URI, request.getExternalId()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?external_id=somerandomuid", URI))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testNotFound() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));
        final String uri = String.format("%s/%s", URI, "/12y8e9129y8h");

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
        final ApiGatewayCreateRequest invalidExternalId = Fixture.from(ApiGatewayCreateRequest.class)
                .gimme("invalidExternalId", new ApiGatewayRequestRule(api, gateway));

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidExternalId)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testNotFoundGateway() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class)
                .gimme("valid", new ApiGatewayRequestRule(api, gateway));
        request.setGatewayUuid("123123");

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testNotFoundApi() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class)
                .gimme("valid", new ApiGatewayRequestRule(api, gateway));
        request.setApiUuid("123123");

        checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testListEndpoints() throws Exception {
        final ApiGatewayCreateRequest request = Fixture.from(ApiGatewayCreateRequest.class).gimme("valid", new ApiGatewayRequestRule(api, gateway));

        mockMvc.perform(get(URI)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s/endpoints", URI, uuid);

        stubFor(WireMock.get(urlEqualTo(String.format("/backoffice/apis/%s/endpoints", request.getExternalId())))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"endpoints\": [\"GET /\", \"POST /charge\"] }")));

        mockMvc.perform(get(uriSingle)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("endpoints").isArray())
                .andExpect(jsonPath("endpoints").value(hasSize(2)))
                .andExpect(jsonPath("endpoints[0]").value("GET /"))
                .andExpect(jsonPath("endpoints[1]").value("POST /charge"));

        stubFor(WireMock.get(urlEqualTo(String.format("/backoffice/apis/%s/endpoints", request.getExternalId())))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get(uriSingle)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("endpoints").isArray())
                .andExpect(jsonPath("endpoints").value(hasSize(0)));
    }

}
