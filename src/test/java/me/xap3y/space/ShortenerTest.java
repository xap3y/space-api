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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "SPRING_DATASOURCE_USERNAME=space",
        "SPRING_DATASOURCE_PASSWORD=test",
        "SPRING_DATASOURCE_HOST=internal.2.db.xap3y.eu",
        "SPRING_DATASOURCE_SCHEMA=space_test",
})
public class ShortenerTest {

    private static final String TEST_URL = "https://www.cloudflare.com/";
    private static final String URL_CREATE_PATH = "/v1/url/create";
    private static final String URL_GET_PATH = "/v1/url/get/";
    private static final String TEST_API_KEY = "test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUrlCreationAndValidate() throws Exception {

        String uniqueId = "";
        MvcResult result = this.mockMvc.perform(post(URL_CREATE_PATH)
                        .formField("url", TEST_URL)
                        .header("X-API-Key", TEST_API_KEY)
                )
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").value(false),
                        jsonPath("$.message.url").value(TEST_URL),
                        jsonPath("$.message.createdAt").exists(),
                        jsonPath("$.message.expiresAt").exists(),
                        jsonPath("$.message.visits").exists(),
                        jsonPath("$.message.shortCode").exists(),
                        jsonPath("$.message.uploader").exists()
                ).andReturn();
        uniqueId = JsonPath.read(result.getResponse().getContentAsString(), "$.message.shortCode");

        this.mockMvc.perform(get(URL_GET_PATH + uniqueId))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").value(false),
                        jsonPath("$.message").value(TEST_URL),
                        jsonPath("$.uniqueId").value(uniqueId),
                        header().doesNotExist("X-Uploader"),
                        header().doesNotExist("X-Url-CreatedAt"),
                        header().doesNotExist("X-Url-ExpiresAt")
                );

        this.mockMvc.perform(get(URL_GET_PATH + uniqueId + "?raw=true&uploader_info=true&url_info=true"))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType("text/plain;charset=UTF-8"),
                        content().string(TEST_URL),
                        header().exists("X-Uploader"),
                        header().exists("X-Url-CreatedAt"),
                        header().exists("X-Url-ExpiresAt")
                );
    }
}
