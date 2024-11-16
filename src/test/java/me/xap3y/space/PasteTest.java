package me.xap3y.space;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "SPRING_DATASOURCE_USERNAME=space",
        "SPRING_DATASOURCE_PASSWORD=test",
        "SPRING_DATASOURCE_HOST=internal.2.db.xap3y.eu",
        "SPRING_DATASOURCE_SCHEMA=space_test",
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PasteTest {

    private static final String TEST_TEXT = "Hello, World!";
    private static final String PASTE_CREATE_PATH = "/v1/paste/create";
    private static final String PASTE_GET_PATH = "/v1/paste/get";
    private static final String TEST_API_KEY = "test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPasteCreationAndValidate() throws Exception {

        String uniqueId;

        MvcResult result = this.mockMvc.perform(post(PASTE_CREATE_PATH)
                        .content("{\"text\":\"" + TEST_TEXT + "\"}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", TEST_API_KEY)
                )
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").value(false),
                        jsonPath("$.uniqueId").exists(),
                        jsonPath("$.message").exists()
                ).andReturn();

        uniqueId = JsonPath.read(result.getResponse().getContentAsString(), "$.uniqueId");

        this.mockMvc.perform(get(PASTE_GET_PATH + "/" + uniqueId))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").value(false),
                        jsonPath("$.uniqueId").value(uniqueId),
                        jsonPath("$.message").value(TEST_TEXT)
                );
    }

}