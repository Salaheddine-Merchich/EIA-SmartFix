package com.ocp.eia.modules.maintenance.infrastructure.pdf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfLabelMapperTest {

    @Test
    void shouldMapEnumsToFrenchLabels() {
        assertThat(PdfLabelMapper.criticite(com.ocp.eia.domain.model.Criticite.MOYENNE)).isEqualTo("Moyenne");
        assertThat(PdfLabelMapper.statutPanne(com.ocp.eia.domain.model.StatutPanne.OUVERTE)).isEqualTo("Ouverte");
        assertThat(PdfLabelMapper.statutValidation(com.ocp.eia.domain.model.StatutValidation.VALIDEE)).isEqualTo("Validée");
        assertThat(PdfLabelMapper.orDash("  ")).isEqualTo("—");
        assertThat(PdfLabelMapper.orDash("Valeur")).isEqualTo("Valeur");
    }
}
