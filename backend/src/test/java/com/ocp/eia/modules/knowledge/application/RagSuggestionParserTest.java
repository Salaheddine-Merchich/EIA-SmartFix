package com.ocp.eia.modules.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocp.eia.application.dto.AiDto.AiSuggestions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagSuggestionParserTest {

    private RagSuggestionParser parser;

    @BeforeEach
    void setUp() {
        parser = new RagSuggestionParser(new ObjectMapper());
    }

    @Test
    void parse_plainJson() throws Exception {
        String json = """
                {
                  "probableCauses": ["Cause A"],
                  "correctiveActions": ["Action 1", "Action 2"],
                  "summary": "Resume test",
                  "advice": "Conseil test"
                }
                """;

        AiSuggestions result = parser.parse(json);

        assertThat(result.probableCauses()).containsExactly("Cause A");
        assertThat(result.correctiveActions()).containsExactly("Action 1", "Action 2");
        assertThat(result.summary()).isEqualTo("Resume test");
        assertThat(result.advice()).isEqualTo("Conseil test");
    }

    @Test
    void parse_jsonWrappedInMarkdownFence() throws Exception {
        String wrapped = """
                ```json
                {
                  "probableCauses": ["Surchauffe moteur"],
                  "correctiveActions": ["Verifier ventilation", "Controler paliers"],
                  "summary": "Diagnostic variateur",
                  "advice": "Documenter intervention"
                }
                ```
                """;

        AiSuggestions result = parser.parse(wrapped);

        assertThat(result.probableCauses()).containsExactly("Surchauffe moteur");
        assertThat(result.correctiveActions()).hasSize(2);
        assertThat(result.summary()).isEqualTo("Diagnostic variateur");
    }

    @Test
    void parse_truncatedJson_throws() {
        String truncated = """
                {
                  "probableCauses": ["Cause incomplete"],
                  "correctiveActions": [
                """;

        assertThatThrownBy(() -> parser.parse(truncated))
                .isInstanceOf(Exception.class);
    }
}
