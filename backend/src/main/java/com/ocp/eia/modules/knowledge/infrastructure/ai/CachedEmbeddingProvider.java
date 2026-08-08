package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Component
@ConditionalOnExpression("${app.knowledge.enabled:false} == true and '${app.knowledge.provider:ollama}' == 'ollama'")
@Slf4j
public class CachedEmbeddingProvider implements EmbeddingProviderPort {

    private final OllamaEmbeddingAdapter delegate;
    private final Cache<String, float[]> embeddingCache;

    public CachedEmbeddingProvider(OllamaEmbeddingAdapter delegate) {
        this.delegate = delegate;
        this.embeddingCache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .build();
        log.info("Cache d'embeddings initialisé : max 200 entrées, TTL 5 min");
    }

    @Override
    public float[] embed(String text) {
        String cacheKey = normalizeCacheKey(text);
        
        float[] cached = embeddingCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache hit embedding pour clé: {}", cacheKey.substring(0, Math.min(16, cacheKey.length())));
            return cached;
        }

        log.debug("Cache miss embedding, appel Ollama pour: {}", text.substring(0, Math.min(50, text.length())));
        float[] embedding = delegate.embed(text);
        embeddingCache.put(cacheKey, embedding);
        
        return embedding;
    }

    /**
     * Normalise le texte pour la clé de cache : 
     * - trim spaces
     * - lowercase 
     * - hash MD5 pour éviter clés trop longues
     */
    private String normalizeCacheKey(String text) {
        String normalized = text.trim().toLowerCase();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(normalized.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback : utilisation directe du texte normalisé (tronqué)
            return normalized.length() > 100 
                ? normalized.substring(0, 100) 
                : normalized;
        }
    }

    public Cache<String, float[]> getCache() {
        return embeddingCache;
    }
}