package io.gr1d.portal.subscriptions.controller.v1;

import br.com.six2six.fixturefactory.Fixture;
import br.com.six2six.fixturefactory.loader.FixtureFactoryLoader;
import io.gr1d.core.model.CreatedResponse;
import io.gr1d.portal.subscriptions.SpringTestApplication;
import io.gr1d.portal.subscriptions.request.CategoryRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static io.gr1d.portal.subscriptions.TestUtils.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringTestApplication.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class V1CategoryControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private Flyway flyway;

    private String uuid;
    private CategoryRequest category;

    @Before
    public void init() {
        FixtureFactoryLoader.loadTemplates("io.gr1d.portal.subscriptions.fixtures");
    }

    @After
    public void clean() throws IllegalArgumentException {
        flyway.clean();
    }

    @Before
    public void createCategory() throws Exception {
        category = Fixture.from(CategoryRequest.class).gimme("valid");

        mockMvc.perform(get("/category")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(0)));

        final ResultActions actions = checkXss(mockMvc.perform(post("/category")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(json(category))).andDo(print()).andExpect(status().isCreated()));
        final CreatedResponse response = getResult(actions, CreatedResponse.class);
        uuid = response.getUuid();
    }

    @Test
    @FlywayTest
    public void testList() throws Exception {
        mockMvc.perform(get("/v1/category")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isArray())
                .andExpect(jsonPath("content").value(hasSize(1)))
                .andExpect(jsonPath("content[0].uuid").value(uuid))
                .andExpect(jsonPath("content[0].name").value(category.getName()))
                .andExpect(jsonPath("content[0].slug").value(category.getSlug()));
    }

    @Test
    @FlywayTest
    public void testListUnpaged() throws Exception {
        mockMvc.perform(get("/v1/category/all")).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value(uuid))
                .andExpect(jsonPath("$[0].name").value(category.getName()))
                .andExpect(jsonPath("$[0].slug").value(category.getSlug()));
    }

    @Test
    @FlywayTest
    public void testGetDetailsBySlug() throws Exception {
        mockMvc.perform(get(String.format("/v1/category/by-slug/%s", category.getSlug()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("uuid").value(uuid))
                .andExpect(jsonPath("name").value(category.getName()))
                .andExpect(jsonPath("slug").value(category.getSlug()));
    }

}
