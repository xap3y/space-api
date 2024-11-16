package me.xap3y.space;

import me.xap3y.space.controller.BasicController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "SPRING_DATASOURCE_USERNAME=space",
        "SPRING_DATASOURCE_PASSWORD=test",
        "SPRING_DATASOURCE_HOST=internal.2.db.xap3y.eu",
        "SPRING_DATASOURCE_SCHEMA=space_test",
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/*@TestPropertySource(properties = {"spring.datasource.", "spring.datasource.username=xapspace", "spring.datasource.password=space"})*/
class SpaceApplicationTests {

    @Autowired
    private BasicController basicController;

    @Test
    void contextLoads() throws Exception {
        assertThat(basicController).isNotNull();
    }

}
