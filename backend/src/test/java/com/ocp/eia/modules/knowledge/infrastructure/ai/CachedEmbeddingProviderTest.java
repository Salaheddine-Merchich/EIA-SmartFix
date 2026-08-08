package com.ocp.eia.modules.knowledge.infrastructure.ai;

import com.ocp.eia.modules.knowledge.domain.port.EmbeddingProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedEmbeddingProviderTest {

    @Mock
    private OllamaEmbeddingAdapter delegate;

    private CachedEmbeddingProvider cachedProvider;

    @BeforeEach
    void setUp() {
        cachedProvider = new CachedEmbeddingProvider(delegate);
    }

    @Test
    void embed_firstCall_callsDelegate() {
        // Given
        String text = "Moteur en surchauffe";
        float[] expectedEmbedding = {0.1f, 0.2f, 0.3f};
        when(delegate.embed(anyString())).thenReturn(expectedEmbedding);

        // When
        float[] result = cachedProvider.embed(text);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(delegate, times(1)).embed(text);
    }

    @Test
    void embed_secondCallSameText_usesCacheWithoutCallingDelegate() {
        // Given
        String text = "Moteur en surchauffe";
        float[] expectedEmbedding = {0.1f, 0.2f, 0.3f};
        when(delegate.embed(anyString())).thenReturn(expectedEmbedding);

        // When
        float[] firstCall = cachedProvider.embed(text);
        float[] secondCall = cachedProvider.embed(text);

        // Then
        assertThat(firstCall).isEqualTo(expectedEmbedding);
        assertThat(secondCall).isEqualTo(expectedEmbedding);
        verify(delegate, times(1)).embed(text); // delegate appelé une seule fois
    }

    @Test
    void embed_differentCasing_usesCacheForNormalizedText() {
        // Given
        String text1 = "MOTEUR EN SURCHAUFFE";
        String text2 = "moteur en surchauffe";
        String text3 = "  Moteur En Surchauffe  "; // avec espaces
        float[] expectedEmbedding = {0.1f, 0.2f, 0.3f};
        when(delegate.embed(anyString())).thenReturn(expectedEmbedding);

        // When
        float[] result1 = cachedProvider.embed(text1);
        float[] result2 = cachedProvider.embed(text2);
        float[] result3 = cachedProvider.embed(text3);

        // Then
        assertThat(result1).isEqualTo(expectedEmbedding);
        assertThat(result2).isEqualTo(expectedEmbedding);
        assertThat(result3).isEqualTo(expectedEmbedding);
        // Tous normalisés vers la même clé de cache → delegate appelé une seule fois
        verify(delegate, times(1)).embed(anyString());
    }

    @Test
    void embed_differentTexts_callsDelegateForEach() {
        // Given
        String text1 = "Moteur en surchauffe";
        String text2 = "Fuite hydraulique";
        float[] embedding1 = {0.1f, 0.2f, 0.3f};
        float[] embedding2 = {0.4f, 0.5f, 0.6f};
        when(delegate.embed(text1)).thenReturn(embedding1);
        when(delegate.embed(text2)).thenReturn(embedding2);

        // When
        float[] result1 = cachedProvider.embed(text1);
        float[] result2 = cachedProvider.embed(text2);

        // Then
        assertThat(result1).isEqualTo(embedding1);
        assertThat(result2).isEqualTo(embedding2);
        verify(delegate, times(1)).embed(text1);
        verify(delegate, times(1)).embed(text2);
    }

    @Test
    void getCache_returnsCache() {
        // When/Then
        assertThat(cachedProvider.getCache()).isNotNull();
    }
}