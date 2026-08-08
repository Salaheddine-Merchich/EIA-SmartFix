package com.ocp.eia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration"
})
@ActiveProfiles("test")
class EiaSmartFixApplicationTests {

    @Test
    void contextLoads() {
    }
}
