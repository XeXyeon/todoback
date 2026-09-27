package com.toodback;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-at-least-thirty-two-bytes-long",
        "spring.datasource.url=jdbc:h2:mem:boardtest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ToodbackApplicationTests {

    @Test
    void contextLoads() {
    }

}
