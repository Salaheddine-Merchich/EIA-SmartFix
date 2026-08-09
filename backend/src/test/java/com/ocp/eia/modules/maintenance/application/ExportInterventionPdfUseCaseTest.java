package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.*;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.infrastructure.pdf.InterventionPdfGenerator;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportInterventionPdfUseCaseTest {

    @Mock
    private InterventionRepository interventionRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private InterventionPdfGenerator pdfGenerator;

    @InjectMocks
    private ExportInterventionPdfUseCase useCase;

    private UUID interventionId;
    private Intervention intervention;
    private Equipment equipment;
    private Failure failure;
    private User technicien;

    @BeforeEach
    void setUp() {
        interventionId = UUID.randomUUID();
        
        equipment = Equipment.builder()
                .id(UUID.randomUUID())
                .code("EQ001")
                .designation("Test Equipment")
                .famille("Test Family")
                .zone("Test Zone")
                .build();

        technicien = User.builder()
                .id(UUID.randomUUID())
                .email("tech@test.com")
                .nomPrenom("Jean Dupont")
                .role(Role.TECHNICIEN)
                .build();

        failure = Failure.builder()
                .id(UUID.randomUUID())
                .equipment(equipment)
                .dateHeure(Instant.now())
                .criticite(Criticite.MOYENNE)
                .statut(StatutPanne.OUVERTE)
                .descriptionInitiale("Test failure")
                .build();

        intervention = Intervention.builder()
                .id(interventionId)
                .failure(failure)
                .technicien(technicien)
                .description("Test intervention")
                .symptomes("Test symptoms")
                .causeRacine("Test root cause")
                .analyseTechnique("Test analysis")
                .actionsCorrectives("Test actions")
                .statutValidation(StatutValidation.VALIDEE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void execute_shouldReturnPdfExportResult_whenInterventionExists() {
        // Given
        byte[] expectedPdfContent = "test pdf content".getBytes();
        when(interventionRepository.findByIdWithDetails(interventionId))
                .thenReturn(Optional.of(intervention));
        when(equipmentRepository.findById(equipment.getId()))
                .thenReturn(Optional.of(equipment));
        when(pdfGenerator.generateInterventionReport(any(Intervention.class), any(Equipment.class)))
                .thenReturn(expectedPdfContent);

        // When
        var result = useCase.execute(interventionId);

        // Then
        assertThat(result.content()).isEqualTo(expectedPdfContent);
        assertThat(result.filename()).contains("intervention-EQ001-");
        assertThat(result.filename()).contains(LocalDate.now().toString());
        assertThat(result.filename()).endsWith(".pdf");
    }

    @Test
    void execute_shouldThrowResourceNotFoundException_whenInterventionNotFound() {
        // Given
        when(interventionRepository.findByIdWithDetails(interventionId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.execute(interventionId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Intervention introuvable");
    }

    @Test
    void execute_shouldHandleNullEquipment_whenEquipmentNotFound() {
        // Given
        byte[] expectedPdfContent = "test pdf content".getBytes();
        when(interventionRepository.findByIdWithDetails(interventionId))
                .thenReturn(Optional.of(intervention));
        when(equipmentRepository.findById(equipment.getId()))
                .thenReturn(Optional.empty());
        when(pdfGenerator.generateInterventionReport(any(Intervention.class), any()))
                .thenReturn(expectedPdfContent);

        // When
        var result = useCase.execute(interventionId);

        // Then
        assertThat(result.content()).isEqualTo(expectedPdfContent);
        assertThat(result.filename()).contains("intervention-unknown-");
        assertThat(result.filename()).endsWith(".pdf");
    }
}