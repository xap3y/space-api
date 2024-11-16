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

/*
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "SPRING_DATASOURCE_USERNAME=space",
        "SPRING_DATASOURCE_PASSWORD=test",
        "SPRING_DATASOURCE_HOST=internal.2.db.xap3y.eu",
        "SPRING_DATASOURCE_SCHEMA=space_test",
})
public class PasteTest {

    private static final String TEST_TEXT = "Hello, World!";
    private static final String PASTE_CREATE_PATH = "/v1/paste/create";
    private static final String PASTE_GET_PATH = "/v1/paste/get";
    private static final String TEST_API_KEY = "test";
    private static String UNIQUE_ID = "";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPasteCreation() throws Exception {
        MvcResult result = this.mockMvc.perform(post(PASTE_CREATE_PATH)
                        .param("body", "{\"text\": \"" + TEST_TEXT + "\"}")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
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
        UNIQUE_ID = JsonPath.read(result.getResponse().getContentAsString(), "$.uniqueId");
    }

    @Test
    void testPasteExists() throws Exception {
        this.mockMvc.perform(get(PASTE_GET_PATH + "/" + UNIQUE_ID)
                        .header("X-API-KEY", TEST_API_KEY)
                )
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").value(false),
                        jsonPath("$.uniqueId").value(UNIQUE_ID),
                        jsonPath("$.message").value(TEST_TEXT)
                );
    }
}
*/
public class PasteTest {}