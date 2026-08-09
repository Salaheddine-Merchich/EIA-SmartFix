package com.ocp.eia.modules.maintenance.application;

import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.Intervention;
import com.ocp.eia.domain.repository.EquipmentRepository;
import com.ocp.eia.domain.repository.InterventionRepository;
import com.ocp.eia.modules.maintenance.infrastructure.pdf.InterventionPdfGenerator;
import com.ocp.eia.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportInterventionPdfUseCase {

    private final InterventionRepository interventionRepository;
    private final EquipmentRepository equipmentRepository;
    private final InterventionPdfGenerator pdfGenerator;

    public PdfExportResult execute(UUID interventionId) {
        // Charger intervention avec toutes les relations
        Intervention intervention = interventionRepository.findByIdWithDetails(interventionId)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention introuvable"));
        
        // Charger équipement complet
        Equipment equipment = equipmentRepository.findById(intervention.getFailure().getEquipment().getId())
                .orElse(null);
        
        // Générer PDF
        byte[] pdfContent = pdfGenerator.generateInterventionReport(intervention, equipment);
        
        String filename = String.format("intervention-%s-%s.pdf", 
            equipment != null ? equipment.getCode() : "unknown",
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
        return new PdfExportResult(pdfContent, filename);
    }
    
    public record PdfExportResult(byte[] content, String filename) {}
}