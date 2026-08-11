package com.ocp.eia.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private Storage storage = new Storage();
    private Ai ai = new Ai();
    private Knowledge knowledge = new Knowledge();

    @Getter
    @Setter
    public static class Knowledge {
        private boolean enabled = false;
        /** Provider IA : ollama (défaut), openai, gemini */
        private String provider = "ollama";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessExpirationMs;
        private long refreshExpirationMs;
    }

    @Getter
    @Setter
    public static class Cookie {
        /** Use Secure cookies (HTTPS). Default false for local HTTP demo. */
        private boolean secure = false;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Getter
    @Setter
    public static class Storage {
        private String path;
    }

    @Getter
    @Setter
    public static class Ai {
        private Rag rag = new Rag();

        @Getter
        @Setter
        public static class Rag {
            private int topK = 5;
            private int embeddingDimension = 768;
            private double similarityThreshold = 0.70;
            private boolean fastPathEnabled = true;
            private double fastPathMinSimilarity = 0.85;
            private boolean hybridTextEnabled = true;
            private Context context = new Context();
            private Performance performance = new Performance();
            
            @Getter
            @Setter
            public static class Context {
                private double equipmentBoost = 2.0;
                private double familyBoost = 1.5;
                private double zoneBoost = 1.2;
            }
            
            @Getter
            @Setter
            public static class Performance {
                private String embeddingTimeout = "5s";
                private String llmTimeout = "180s";
                private int circuitBreakerThreshold = 5;
                private String circuitBreakerTimeout = "30s";
                private int threadPoolCoreSize = 2;
                private int threadPoolMaxSize = 8;
                private int threadPoolQueueCapacity = 100;
            }
        }
    }
}
