package io.gr1d.portal.subscriptions.controller;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.integrations.whitelabel.model.response.V1TenantAssetResponse;
import io.gr1d.integrations.whitelabel.model.response.V1TenantFieldResponse;
import io.gr1d.integrations.whitelabel.model.response.V1TenantResponse;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.TestUtils;
import io.gr1d.portal.subscriptions.request.TenantCreateRequest;
import io.gr1d.portal.subscriptions.request.TenantRequest;
import io.gr1d.portal.subscriptions.response.TenantResponse;
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
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gr1d.portal.subscriptions.TestUtils.*;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.MockitoAnnotations.initMocks;
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
public class TenantControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private Flyway flyway;

    @Before
    public void init() {
        initMocks(this);
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
    }

    private void createStubToTenantCreation(final String realm) {
        TestUtils.createStubAuthentication();
        stubFor(WireMock.post(urlEqualTo("/v1/tenant"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"uuid\": \"1b6f7bf2-3052-40f9-8561-8a576ccc84b2\" }")));
        stubFor(WireMock.get(urlEqualTo(String.format("/v1/tenant/by-realm/%s", realm)))
                .willReturn(aResponse().withStatus(404)));
    }

    private void createStubToUpdateTenant(final String realm) {
        WireMock.reset();
        TestUtils.createStubAuthentication();
        stubFor(WireMock.put(urlEqualTo(String.format("/v1/tenant/by-realm/%s", realm)))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"uuid\": \"1b6f7bf2-3052-40f9-8561-8a576ccc84b2\" }")));
    }

    private void createStubToRealm(final TenantCreateRequest request) {
        WireMock.reset();
        TestUtils.createStubAuthentication();

        final V1TenantResponse response = new V1TenantResponse();
        response.setEmail(request.getEmail());
        response.setName(request.getName());
        response.setUuid("1b6f7bf2-3052-40f9-8561-8a576ccc84b2");
        response.setRealm(request.getRealm());
        response.setUrl(request.getUrl());
        response.setAssets(new V1TenantAssetResponse());
        response.getAssets().put("email", singletonMap("logo_header", "https://storage.gr1d.io/logo_header.png"));

        final Map<String, Object> map = new HashMap<>();
        map.put("support_email", request.getSupportEmail());
        map.put("phone", request.getPhone());

        response.setFields(new V1TenantFieldResponse());
        response.getFields().put("payments", map);

        stubFor(WireMock.get(urlEqualTo(String.format("/v1/tenant/by-realm/%s", request.getRealm())))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(json(response))));
    }

    @Test
    @FlywayTest
    public void testTenantCreation() throws Exception {
        final TenantCreateRequest request = Fixture.from(TenantCreateRequest.class).gimme("valid");
        final String uri = "/tenant";

        createStubToTenantCreation(request.getRealm());

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", uri, uuid);

        createStubToRealm(request);

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].name").value(request.getName()))
                .andExpect(jsonPath("content[0].logo").value("https://storage.gr1d.io/logo_header.png"))
                .andExpect(jsonPath("content[0].wallet_id").value(request.getWalletId()))
                .andExpect(jsonPath("content[0].phone").value(request.getPhone()))
                .andExpect(jsonPath("content[0].email").value(request.getEmail()))
                .andExpect(jsonPath("content[0].realm").value(request.getRealm()))
                .andExpect(jsonPath("content[0].url").value(request.getUrl()))
                .andExpect(jsonPath("content[0].uuid").value(uuid));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").value(request.getName()))
                .andExpect(jsonPath("logo").value("https://storage.gr1d.io/logo_header.png"))
                .andExpect(jsonPath("wallet_id").value(request.getWalletId()))
                .andExpect(jsonPath("phone").value(request.getPhone()))
                .andExpect(jsonPath("email").value(request.getEmail()))
                .andExpect(jsonPath("realm").value(request.getRealm()))
                .andExpect(jsonPath("url").value(request.getUrl()))
                .andExpect(jsonPath("uuid").value(uuid));

        final TenantCreateRequest anotherRequest = Fixture.from(TenantCreateRequest.class).gimme("valid");
        anotherRequest.setRealm(request.getRealm());
        createStubToUpdateTenant(request.getRealm());

        checkXss(mockMvc.perform(put(uriSingle)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isOk()));

        createStubToRealm(anotherRequest);

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].name").value(anotherRequest.getName()))
                .andExpect(jsonPath("content[0].wallet_id").value(anotherRequest.getWalletId()))
                .andExpect(jsonPath("content[0].phone").value(anotherRequest.getPhone()))
                .andExpect(jsonPath("content[0].email").value(anotherRequest.getEmail()))
                .andExpect(jsonPath("content[0].realm").value(request.getRealm())) // realm should not change
                .andExpect(jsonPath("content[0].support_email").value(anotherRequest.getSupportEmail()))
                .andExpect(jsonPath("content[0].url").value(anotherRequest.getUrl()))
                .andExpect(jsonPath("content[0].uuid").value(uuid));

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").value(anotherRequest.getName()))
                .andExpect(jsonPath("wallet_id").value(anotherRequest.getWalletId()))
                .andExpect(jsonPath("phone").value(anotherRequest.getPhone()))
                .andExpect(jsonPath("email").value(anotherRequest.getEmail()))
                .andExpect(jsonPath("realm").value(request.getRealm())) // realm should not change
                .andExpect(jsonPath("support_email").value(anotherRequest.getSupportEmail()))
                .andExpect(jsonPath("url").value(anotherRequest.getUrl()))
                .andExpect(jsonPath("uuid").value(uuid));

        mockMvc.perform(get(String.format("%s/by-realm/%s", uri, request.getRealm())).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").value(anotherRequest.getName()))
                .andExpect(jsonPath("wallet_id").value(anotherRequest.getWalletId()))
                .andExpect(jsonPath("phone").value(anotherRequest.getPhone()))
                .andExpect(jsonPath("email").value(anotherRequest.getEmail()))
                .andExpect(jsonPath("realm").value(request.getRealm())) // realm should not change
                .andExpect(jsonPath("support_email").value(anotherRequest.getSupportEmail()))
                .andExpect(jsonPath("url").value(anotherRequest.getUrl()))
                .andExpect(jsonPath("uuid").value(uuid));
    }

    @Test
    @FlywayTest
    public void testRealmUnique() throws Exception {
        final TenantCreateRequest request = Fixture.from(TenantCreateRequest.class).gimme("valid");
        final TenantCreateRequest anotherRequest = Fixture.from(TenantCreateRequest.class).gimme("valid");
        final String uri = "/tenant";

        createStubToTenantCreation(request.getRealm());

        anotherRequest.setRealm(request.getRealm());

        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        final String uuid = response.getUuid();
        final String uriSingle = String.format("%s/%s", uri, uuid);

        createStubToRealm(request);

        mockMvc.perform(get(uriSingle).accept(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("realm").value(request.getRealm()))
                .andExpect(jsonPath("realm").value(anotherRequest.getRealm()))
                .andExpect(jsonPath("uuid").value(uuid));

        mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(anotherRequest))).andDo(print()).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @FlywayTest
    public void testTenantInactivation() throws Exception {
        final TenantCreateRequest request = Fixture.from(TenantCreateRequest.class).gimme("valid");
        final String uri = "/tenant";

        createStubToTenantCreation(request.getRealm());

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

        createStubToRealm(request);

        mockMvc.perform(get(uri)).andDo(print())
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
        mockMvc.perform(get(uri)).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
        mockMvc.perform(delete(uriSingle)).andDo(print()).andExpect(status().isNotFound());

//        // and then all APIs are removed
//        mockMvc.perform(get(uriApi)).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("content").isArray())
//                .andExpect(jsonPath("content").value(hasSize(0)));
    }

    @Test
    @FlywayTest
    public void testNotFound() throws Exception {
        final String uri = "/tenant/8927349872";
        mockMvc.perform(get(uri)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        final TenantCreateRequest request = Fixture.from(TenantCreateRequest.class).gimme("valid");

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
        final TenantCreateRequest invalidName = Fixture.from(TenantCreateRequest.class).gimme("invalidName");
        final TenantCreateRequest invalidWalletId = Fixture.from(TenantCreateRequest.class).gimme("invalidWallet");
        final TenantCreateRequest invalidPhone = Fixture.from(TenantCreateRequest.class).gimme("invalidPhone");
        final TenantCreateRequest invalidEmail = Fixture.from(TenantCreateRequest.class).gimme("invalidEmail");
        final TenantCreateRequest invalidRealm = Fixture.from(TenantCreateRequest.class).gimme("invalidRealm");
        final TenantCreateRequest invalidSupportEmail = Fixture.from(TenantCreateRequest.class).gimme("invalidSupportEmail");

        createStubToTenantCreation(invalidName.getRealm());
        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidName)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidWalletId)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidPhone)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidEmail)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidSupportEmail)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));

        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(invalidRealm)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()));
    }

    @Test
    @FlywayTest
    public void testValidateRealmExistance() throws Exception {
        final TenantCreateRequest request = Fixture.from(TenantCreateRequest.class).gimme("valid");

        TestUtils.createStubAuthentication();
        stubFor(WireMock.post(urlEqualTo("/v1/tenant"))
                .willReturn(aResponse().withStatus(422).withHeader("Content-Type", "application/json")
                        .withBody("[\n" +
                                "  {\n" +
                                "    \"error\": \"ObjectError\",\n" +
                                "    \"message\": \"Nenhum realm encontrado com esse valor\",\n" +
                                "    \"meta\": \"realm\",\n" +
                                "    \"timestamp\": \"2019-02-13T10:02:39.445\"\n" +
                                "  }\n" +
                                "]")));

        checkXss(mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andDo(print()).andExpect(status().isUnprocessableEntity()))
                .andExpect(jsonPath("$[0].message").value("Nenhum realm encontrado com esse valor"))
                .andExpect(jsonPath("$[0].meta").value("realm"));
    }

}
