package io.gr1d.portal.subscriptions.controller;

import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.TestUtils;
import io.gr1d.portal.subscriptions.model.Currency;
import io.gr1d.portal.subscriptions.repository.CurrencyRepository;
import io.gr1d.spring.keycloak.model.Realm;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.gr1d.portal.subscriptions.TestUtils.jsonCammelCase;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class CurrencyControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private Flyway flyway;
    @Autowired private CurrencyRepository repository;

    @Before
    public void init() {
        initMocks(this);
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");

        repository.deleteAll();

        final Currency currency = new Currency();
        currency.setIsoCode("BRL");
        currency.setName("Reais");
        currency.setSymbol("R$");

        repository.save(currency);
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
    }

    @Test
    @FlywayTest
    public void testList() throws Exception {
        mockMvc.perform(get("/currency")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].name").value("Reais"))
                .andExpect(jsonPath("content[0].iso_code").value("BRL"))
                .andExpect(jsonPath("content[0].symbol").value("R$"));

        mockMvc.perform(get("/currency?iso_code=USD")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
        mockMvc.perform(get("/currency?iso_code=BRL")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));
        mockMvc.perform(get("/currency?name=Dollar")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));
        mockMvc.perform(get("/currency?name=Re")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)));
    }

}
