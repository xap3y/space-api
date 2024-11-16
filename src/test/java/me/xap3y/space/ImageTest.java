package me.xap3y.space;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
/*@TestPropertySource(properties = {"spring.datasource.username=xapspace", "spring.datasource.password=space"})*/
public class ImageTest {

    private static final String IMAGE_ID = "JJYD7HAT";
    private static final String IMAGE_RENDER_PATH = "/v1/image/get/";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testImageEndpoint() throws Exception {
        this.mockMvc.perform(get(IMAGE_RENDER_PATH + IMAGE_ID))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.IMAGE_PNG),
                        header().doesNotExist("X-Uploader"),
                        header().doesNotExist("X-Image-Size"),
                        header().doesNotExist("X-Image-Type")
                );
    }

    @Test
    void testUploaderInfoQueryParam() throws Exception {
        this.mockMvc.perform(get(IMAGE_RENDER_PATH + IMAGE_ID + "?uploader_info=true"))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.IMAGE_PNG),
                        header().exists("X-Uploader"),
                        header().doesNotExist("X-Image-Size"),
                        header().doesNotExist("X-Image-Type")
                );
    }

    @Test
    void testImageInfoQueryParam() throws Exception {
        this.mockMvc.perform(get(IMAGE_RENDER_PATH + IMAGE_ID + "?image_info=true"))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.IMAGE_PNG),
                        header().doesNotExist("X-Uploader"),
                        header().exists("X-Image-Size"),
                        header().exists("X-Image-Type")
                );
    }

    @Test
    void testAllInfoQueryParam() throws Exception {
        this.mockMvc.perform(get(IMAGE_RENDER_PATH + IMAGE_ID + "?image_info=true&uploader_info=true"))
                .andDo(print())
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.IMAGE_PNG),
                        header().exists("X-Uploader"),
                        header().exists("X-Image-Size"),
                        header().exists("X-Image-Type")
                );
    }

    @Test
    void testBase64Json() throws Exception {
        this.mockMvc.perform(get(IMAGE_RENDER_PATH + IMAGE_ID + "?base64=true"))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.APPLICATION_JSON),
                        jsonPath("$.error").value(false),
                        jsonPath("$.message").exists(),
                        jsonPath("$.imageId").doesNotExist()
                );
    }

    @Test
    void testBase64Raw() throws Exception {
        this.mockMvc.perform(get(IMAGE_RENDER_PATH + IMAGE_ID + "?raw=true&base64=true"))
                .andExpectAll(
                        status().isOk(),
                        content().contentType(MediaType.TEXT_PLAIN)
                );
    }
}
