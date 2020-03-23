package io.gr1d.portal.subscriptions.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.api.bridge.ApiBridge;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayFixtures;
import io.gr1d.portal.subscriptions.fixtures.ApiGatewayTenantFixtures;
import io.gr1d.portal.subscriptions.fixtures.ProviderFixtures.ProviderRule;
import io.gr1d.portal.subscriptions.model.*;
import io.gr1d.portal.subscriptions.repository.*;
import io.gr1d.portal.subscriptions.request.ApiCreateRequest;
import io.gr1d.portal.subscriptions.response.ApiResponse;
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

import java.util.Collections;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gr1d.portal.subscriptions.TestUtils.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProviderRepository providerRepository;
    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private GatewayRepository gatewayRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ApiGatewayRepository apiGatewayRepository;
    @Autowired
    private ApiGatewayTenantRepository apiGatewayTenantRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private Flyway flyway;

    @Before
    public void init() {
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
    }

    @Test
    @FlywayTest
    public void testApiCreationAndUpdate() throws Exception {
        final Tag oneTag = tagRepository.save(Fixture.from(Tag.class).gimme("valid"));
        final Tag anotherTag = tagRepository.save(Fixture.from(Tag.class).gimme("valid"));
        final Category oneCategory = categoryRepository.save(Fixture.from(Category.class).gimme("valid"));
        final Category anotherCategory = categoryRepository.save(Fixture.from(Category.class).gimme("valid"));
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        final String uri = "/api";

        request.setCurrency("BRL");
        request.setTags(Collections.singletonList(oneTag.getUuid()));
        request.setCategory(oneCategory.getUuid());

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", uri, uuid);

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid))
                .andExpect(jsonPath("content[0].name").value(request.getName()))
                .andExpect(jsonPath("content[0].split_mode").value("MANUAL"))
                .andExpect(jsonPath("content[0].external_id").value(request.getExternalId()))
                .andExpect(jsonPath("content[0].provider.name").value(provider.getName()))
                .andExpect(jsonPath("content[0].provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("content[0].provider.wallet_id").value(provider.getWalletId()))
                .andExpect(jsonPath("content[0].currency.symbol").value("R$"))
                .andExpect(jsonPath("content[0].currency.iso_code").value("BRL"))
                .andExpect(jsonPath("content[0].currency.name").value("Reais"))
                .andExpect(jsonPath("content[0].category.uuid").value(oneCategory.getUuid()))
                .andExpect(jsonPath("content[0].category.slug").value(oneCategory.getSlug()))
                .andExpect(jsonPath("content[0].category.name").value(oneCategory.getName()))
                .andExpect(jsonPath("content[0].category.description").value(oneCategory.getDescription()));

        final ApiBridge apiBridge = new ApiBridge();
        apiBridge.setDescription("Description");
        apiBridge.setId(request.getExternalId());
        apiBridge.setName(request.getName());

        stubFor(WireMock.get(urlEqualTo(String.format("/backoffice/apis/%s", request.getExternalId())))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(json(apiBridge))));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("name").value(request.getName()))
                .andExpect(jsonPath("split_mode").value("MANUAL"))
                .andExpect(jsonPath("external_id").value(request.getExternalId()))
                .andExpect(jsonPath("api_bridge.id").value(apiBridge.getId()))
                .andExpect(jsonPath("api_bridge.name").value(apiBridge.getName()))
                .andExpect(jsonPath("api_bridge.description").value(apiBridge.getDescription()))
                .andExpect(jsonPath("provider.name").value(provider.getName()))
                .andExpect(jsonPath("provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("provider.wallet_id").value(provider.getWalletId()))

                .andExpect(jsonPath("tags").isArray())
                .andExpect(jsonPath("tags").value(hasSize(1)))
                .andExpect(jsonPath("tags[0].uuid").value(oneTag.getUuid()))
                .andExpect(jsonPath("tags[0].slug").value(oneTag.getSlug()))
                .andExpect(jsonPath("tags[0].name").value(oneTag.getName()))
                .andExpect(jsonPath("tags[0].description").value(oneTag.getDescription()))

                .andExpect(jsonPath("category.uuid").value(oneCategory.getUuid()))
                .andExpect(jsonPath("category.slug").value(oneCategory.getSlug()))
                .andExpect(jsonPath("category.name").value(oneCategory.getName()))
                .andExpect(jsonPath("category.description").value(oneCategory.getDescription()));

        final Provider anotherProvider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest anotherRequest = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(anotherProvider));

        anotherRequest.setTags(Collections.singletonList(anotherTag.getUuid()));
        anotherRequest.setCategory(anotherCategory.getUuid());

        Assertions.assertThat(anotherRequest.getName()).isNotEqualTo(request.getName());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isOk()));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("name").value(anotherRequest.getName()))
                .andExpect(jsonPath("external_id").value(request.getExternalId()))
                .andExpect(jsonPath("external_id").value(not(anotherRequest.getExternalId())))
                .andExpect(jsonPath("provider.name").value(anotherProvider.getName()))
                .andExpect(jsonPath("provider.uuid").value(anotherProvider.getUuid()))
                .andExpect(jsonPath("provider.wallet_id").value(anotherProvider.getWalletId()))

                .andExpect(jsonPath("tags").isArray())
                .andExpect(jsonPath("tags").value(hasSize(1)))
                .andExpect(jsonPath("tags[0].uuid").value(anotherTag.getUuid()))
                .andExpect(jsonPath("tags[0].slug").value(anotherTag.getSlug()))
                .andExpect(jsonPath("tags[0].name").value(anotherTag.getName()))
                .andExpect(jsonPath("tags[0].description").value(anotherTag.getDescription()))

                .andExpect(jsonPath("category.uuid").value(anotherCategory.getUuid()))
                .andExpect(jsonPath("category.slug").value(anotherCategory.getSlug()))
                .andExpect(jsonPath("category.name").value(anotherCategory.getName()))
                .andExpect(jsonPath("category.description").value(anotherCategory.getDescription()));
    }

    @Test
    @FlywayTest
    public void testApiInactivation() throws Exception {
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        final String uri = "/api";

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", uri, uuid);

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid));
        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid));

        mockMvc.perform(delete(uriSingle)).andDo(print()).andExpect(status().isOk());
        mockMvc.perform(get(uriSingle)).andDo(print()).andExpect(status().isNotFound());
        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
        mockMvc.perform(delete(uriSingle)).andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    @FlywayTest
    public void testApiSplitAuto() throws Exception {
        apiSplitAuto();
    }

    public ApiResponse apiSplitAuto() throws Exception {
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("splitAuto", new ProviderRule(provider));
        final String uri = "/api";

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", uri, uuid);

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid));

        final ResultActions getActions = mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("split_mode").value("AUTO"))
                .andExpect(jsonPath("plan.modality").value(request.getPlan().getModality()))
                .andExpect(jsonPath("plan.name").value(request.getPlan().getName()))
                .andExpect(jsonPath("plan.description").value(request.getPlan().getDescription()))
                .andExpect(jsonPath("plan.plan_endpoints").value(hasSize(1)))
                .andExpect(jsonPath("plan.plan_endpoints[0].name").value(request.getPlan().getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plan.plan_endpoints[0].endpoint").value(request.getPlan().getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plan.plan_endpoints[0].metadata.hit_value").value(request.getPlan().getPlanEndpoints().get(0).getMetadata().get("hit_value")));

        mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isOk());

        return getResult(getActions, ApiResponse.class);
    }

    @Test
    @FlywayTest
    public void testApiSplitDefaultManual() throws Exception {
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        final String uri = "/api";

        request.setPlan(null);
        request.setSplitMode(null);

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", uri, uuid);

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("split_mode").value("MANUAL"))
                .andExpect(jsonPath("plan").doesNotExist());
    }

    @Test
    @FlywayTest
    public void testApiSplitAutoThenChangeToManual() throws Exception {
        final String uri = "/api";
        final ApiResponse api = apiSplitAuto();
        final String uriSingle = String.format("%s/%s", uri, api.getUuid());
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class)
                .gimme("valid", new ProviderRule(api.getProvider().getUuid()));

        mockMvc.perform(put(uriSingle).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isOk());

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(api.getUuid()))
                .andExpect(jsonPath("split_mode").value("MANUAL"))
                .andExpect(jsonPath("plan").doesNotExist());

        final ApiCreateRequest requestSplitAuto = Fixture.from(ApiCreateRequest.class)
                .gimme("splitAuto", new ProviderRule(api.getProvider().getUuid()));

        mockMvc.perform(put(uriSingle).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(requestSplitAuto))).andDo(print()).andExpect(status().isOk());

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(api.getUuid()))
                .andExpect(jsonPath("split_mode").value("AUTO"))
                .andExpect(jsonPath("plan.modality").value(requestSplitAuto.getPlan().getModality()))
                .andExpect(jsonPath("plan.name").value(requestSplitAuto.getPlan().getName()))
                .andExpect(jsonPath("plan.description").value(requestSplitAuto.getPlan().getDescription()))
                .andExpect(jsonPath("plan.plan_endpoints").value(hasSize(1)))
                .andExpect(jsonPath("plan.plan_endpoints[0].name").value(requestSplitAuto.getPlan().getPlanEndpoints().get(0).getName()))
                .andExpect(jsonPath("plan.plan_endpoints[0].endpoint").value(requestSplitAuto.getPlan().getPlanEndpoints().get(0).getEndpoint()))
                .andExpect(jsonPath("plan.plan_endpoints[0].metadata.hit_value").value(requestSplitAuto.getPlan().getPlanEndpoints().get(0).getMetadata().get("hit_value")));
    }

    @Test
    @FlywayTest
    public void testListApiByProvider() throws Exception {
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        final String uri = "/api";

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated());

        mockMvc.perform(get(String.format("%s?provider=%s", uri, provider.getUuid()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));

        mockMvc.perform(get(String.format("%s?provider=somerandomuid", uri))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testNotFound() throws Exception {
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final String uri = "/api/12y8e9129y8h";

        mockMvc.perform(get(uri)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid");
        request.setProvider(provider.getUuid());

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
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest invalidName = Fixture.from(ApiCreateRequest.class).gimme("invalidName", new ProviderRule(provider));
        final String uri = "/api";

        checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidName)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testNotFoundProvider() throws Exception {
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid");
        final String uri = "/api";

        checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print())
                .andExpect(status().isNotFound()));
    }

    @Test
    @FlywayTest
    public void testValidationUniqueExternalId() throws Exception {
        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        final String uri = "/api";

        checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andDo(print()).andExpect(status().isCreated()));

        checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testListBridgeApis() throws Exception {
        stubFor(WireMock.get(urlEqualTo("/backoffice/apis?name=payments"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{" +
                                "  \"content\": [" +
                                "    {" +
                                "      \"id\": \"53866343-3b89-4632-8663-433b8906327c\"," +
                                "      \"icon\": \"https://cloud.storage/icon.png\"," +
                                "      \"name\": \"API Payments\"," +
                                "      \"subtitle\": \"\"," +
                                "      \"description\": \"Api description\"" +
                                "    }" +
                                "  ]," +
                                "  \"total_elements\": 27," +
                                "  \"page\": 0," +
                                "  \"size\": 1" +
                                "}")));

        mockMvc.perform(get("/api/bridge?name=payments")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(27))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value("53866343-3b89-4632-8663-433b8906327c"))
                .andExpect(jsonPath("$.content[0].name").value("API Payments"))
                .andExpect(jsonPath("$.content[0].description").value("Api description"));
    }

    @Test
    @FlywayTest
    public void testListBridgeAPIsWithoutAssociation() throws Exception {
        stubFor(WireMock.get(urlEqualTo("/backoffice/apis?size=" + Integer.MAX_VALUE))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{" +
                                "  \"content\": [" +
                                "    {" +
                                "      \"id\": \"53866343-3b89-4632-8663-433b8906327c\"," +
                                "      \"icon\": \"https://cloud.storage/icon.png\"," +
                                "      \"name\": \"API Payments\"," +
                                "      \"subtitle\": \"\"," +
                                "      \"description\": \"Api description\"" +
                                "    }" +
                                "  ]," +
                                "  \"total_elements\": 1," +
                                "  \"page\": 0," +
                                "  \"size\": " + Integer.MAX_VALUE +
                                "}")));

        mockMvc.perform(get("/api/bridgeWithoutAssociation")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value("53866343-3b89-4632-8663-433b8906327c"))
                .andExpect(jsonPath("$.content[0].name").value("API Payments"))
                .andExpect(jsonPath("$.content[0].description").value("Api description"));


        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        request.setExternalId("53866343-3b89-4632-8663-433b8906327c");
        final String uri = "/api";

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));

        mockMvc.perform(get("/api/bridgeWithoutAssociation")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").value(hasSize(0)));

    }

    @Test
    @FlywayTest
    public void testListWithoutPlan() throws Exception {

        final Provider provider = providerRepository.save(Fixture.from(Provider.class).gimme("valid"));
        final ApiCreateRequest request = Fixture.from(ApiCreateRequest.class).gimme("valid", new ProviderRule(provider));
        final String uri = "/api";

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();

        final Api api = apiRepository.findByUuidAndRemovedAtIsNull(uuid);

        final Tenant tenant = tenantRepository.save(Fixture.from(Tenant.class).gimme("valid"));
        final Gateway gateway = gatewayRepository.save(Fixture.from(Gateway.class).gimme("valid"));
        final ApiGateway apiGateway = apiGatewayRepository.save(Fixture.from(ApiGateway.class)
                .gimme("valid", new ApiGatewayFixtures.ApiGatewayRule(api, gateway)));

        ApiGatewayTenant apiGatewayTenant = Fixture.from(ApiGatewayTenant.class)
                .gimme("valid_free", new ApiGatewayTenantFixtures.ApiGatewayTenantEntityRule(apiGateway, tenant));

        final Plan plan = apiGatewayTenant.getPlans().get(0);

        apiGatewayTenant = apiGatewayTenantRepository.save(apiGatewayTenant);

        mockMvc.perform(get("/api/listWithoutPlan")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value(uuid))
                .andExpect(jsonPath("$[0].name").value(request.getName()))
                .andExpect(jsonPath("$[0].external_id").value(request.getExternalId()))
                .andExpect(jsonPath("$[0].provider.name").value(provider.getName()))
                .andExpect(jsonPath("$[0].provider.uuid").value(provider.getUuid()))
                .andExpect(jsonPath("$[0].provider.wallet_id").value(provider.getWalletId()));

        plan.setApiGatewayTenant(apiGatewayTenant);

        planRepository.save(plan);

        mockMvc.perform(get("/api/listWithoutPlan")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testListEndpoints() throws Exception {
        final String uri = "/api";
        final String externalId = UUID.randomUUID().toString();
        final String uriSingle = String.format("%s/%s/endpoints", uri, externalId);

        stubFor(WireMock.get(urlEqualTo(String.format("/backoffice/apis/%s/endpoints", externalId)))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"endpoints\": [\"GET /\", \"POST /charge\"] }")));

        mockMvc.perform(get(uriSingle)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("endpoints").isArray())
                .andExpect(jsonPath("endpoints").value(hasSize(2)))
                .andExpect(jsonPath("endpoints[0]").value("GET /"))
                .andExpect(jsonPath("endpoints[1]").value("POST /charge"));

        stubFor(WireMock.get(urlEqualTo(String.format("/backoffice/apis/%s/endpoints", externalId)))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get(uriSingle)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("endpoints").isArray())
                .andExpect(jsonPath("endpoints").value(hasSize(0)));
    }
}
