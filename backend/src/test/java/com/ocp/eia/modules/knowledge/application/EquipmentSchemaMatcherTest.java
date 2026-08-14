package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.application.dto.AiDto.EquipmentSchemaDto;
import com.ocp.eia.domain.model.Equipment;
import com.ocp.eia.domain.model.EquipmentSchema;
import com.ocp.eia.domain.repository.EquipmentSchemaRepository;
import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import com.ocp.eia.modules.knowledge.domain.model.SearchContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentSchemaMatcherTest {

    private static final UUID VEI_ID = UUID.fromString("b0020001-0001-0001-0001-000000000001");
    private static final UUID GD_ID = UUID.fromString("b0020002-0002-0002-0002-000000000002");
    private static final UUID POM_ID = UUID.fromString("b0020004-0004-0004-0004-000000000004");
    private static final UUID HIT_ID = UUID.fromString("b0010008-0008-0008-0008-000000000008");

    @Mock private EquipmentSchemaRepository schemaRepository;

    @InjectMocks private EquipmentSchemaMatcher matcher;

    private Equipment veichi;
    private Equipment goodrive;
    private Equipment pompe;
    private Equipment hitachi;

    @BeforeEach
    void setUp() {
        veichi = equipment(VEI_ID, "VAR-VEI-SI23", "Variateur VEICHI", "Variateur", "Station PV", "VEICHI");
        goodrive = equipment(GD_ID, "VAR-GD-100PV", "Variateur Goodrive PV", "Variateur", "Station PV", "INVT");
        pompe = equipment(POM_ID, "POM-PV", "Pompe solaire", "Pompe", "Station PV", null);
        hitachi = equipment(HIT_ID, "VAR-HIT-SJ200", "Variateur Hitachi SJ200", "Variateur", "Zone Convoyage", "Hitachi");
    }

    @Test
    void match_pompePvQuery_returnsDistinctEquipmentSchemas() {
        QuerySignals signals = QuerySignalExtractor.extract("Pompe PV ne démarre plus station solaire");
        SearchContext context = SearchContext.of(null, null, null, "Station PV");

        when(schemaRepository.findActiveByZoneAndFamily(eq("Station PV"), any()))
                .thenReturn(List.of(
                        schema(UUID.randomUUID(), veichi, "Cablage X1 TA-TC", "wiring",
                                new String[]{"x1", "ta-tc", "veille", "f14", "pompe", "var-vei"}),
                        schema(UUID.randomUUID(), veichi, "Bornes SI23", "terminal",
                                new String[]{"si23", "borne", "var-vei"}),
                        schema(UUID.randomUUID(), goodrive, "Schema systeme PV", "wiring",
                                new String[]{"pv", "cablage", "out1", "goodrive", "var-gd"}),
                        schema(UUID.randomUUID(), pompe, "Chaine pompe PV", "wiring",
                                new String[]{"pompe", "pv", "ne demarre plus", "pom-pv", "pompe solaire"})
                ));
        when(schemaRepository.countByEquipmentIdAndActiveTrue(VEI_ID)).thenReturn(4L);
        when(schemaRepository.countByEquipmentIdAndActiveTrue(GD_ID)).thenReturn(4L);
        when(schemaRepository.countByEquipmentIdAndActiveTrue(POM_ID)).thenReturn(1L);

        List<EquipmentSchemaDto> result = matcher.match(signals, context);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(s -> s.equipmentCode().equals("POM-PV")));
        assertEquals(result.size(), result.stream().map(EquipmentSchemaDto::equipmentId).distinct().count());
        assertTrue(result.size() <= 3);
    }

    @Test
    void match_returnsAtMostOneSchemaPerEquipment() {
        QuerySignals signals = QuerySignalExtractor.extract("Pompe PV ne démarre plus station solaire");
        SearchContext context = SearchContext.of(null, null, null, "Station PV");

        UUID schema1 = UUID.randomUUID();
        UUID schema2 = UUID.randomUUID();
        when(schemaRepository.findActiveByZoneAndFamily(eq("Station PV"), any()))
                .thenReturn(List.of(
                        schema(schema1, goodrive, "Systeme PV", "wiring", new String[]{"pv", "goodrive"}),
                        schema(schema2, goodrive, "Bornes PV", "terminal", new String[]{"pv", "borne", "goodrive"}),
                        schema(UUID.randomUUID(), pompe, "Chaine pompe", "wiring", new String[]{"pompe", "pom-pv"})
                ));
        when(schemaRepository.countByEquipmentIdAndActiveTrue(GD_ID)).thenReturn(2L);
        when(schemaRepository.countByEquipmentIdAndActiveTrue(POM_ID)).thenReturn(1L);

        List<EquipmentSchemaDto> result = matcher.match(signals, context);

        long goodriveCount = result.stream().filter(s -> s.equipmentCode().equals("VAR-GD-100PV")).count();
        assertTrue(goodriveCount <= 1);
    }

    @Test
    void match_hitachiE21_excludesNonHitachiInSameZone() {
        QuerySignals signals = QuerySignalExtractor.extract("Defaut E21 variateur convoyeur Hitachi");
        SearchContext context = SearchContext.of(null, null, "Variateur", "Zone Convoyage");

        when(schemaRepository.findActiveByZoneAndFamily(eq("Zone Convoyage"), eq("Variateur")))
                .thenReturn(List.of(
                        schema(UUID.randomUUID(), hitachi, "Cablage SJ200", "wiring",
                                new String[]{"e21", "hitachi", "sj200", "fw", "rv"}),
                        schema(UUID.randomUUID(), equipment(UUID.randomUUID(), "VAR-OTHER", "Autre variateur",
                                "Variateur", "Zone Convoyage", "Siemens"),
                                "Schema autre", "wiring", new String[]{"convoyage", "variateur"})
                ));
        when(schemaRepository.countByEquipmentIdAndActiveTrue(HIT_ID)).thenReturn(3L);

        List<EquipmentSchemaDto> result = matcher.match(signals, context);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(s -> s.equipmentCode().equals("VAR-HIT-SJ200")));
        assertEquals(3, result.getFirst().totalSchemasForEquipment());
    }

    @Test
    void match_pompePvQuery_includesTotalSchemaCounts() {
        QuerySignals signals = QuerySignalExtractor.extract("Pompe PV ne démarre plus station solaire");
        SearchContext context = SearchContext.of(null, null, null, "Station PV");

        when(schemaRepository.findActiveByZoneAndFamily(eq("Station PV"), any()))
                .thenReturn(List.of(
                        schema(UUID.randomUUID(), veichi, "Cablage X1 TA-TC", "wiring",
                                new String[]{"x1", "ta-tc", "veille", "f14", "pompe", "var-vei"}),
                        schema(UUID.randomUUID(), goodrive, "Schema systeme PV", "wiring",
                                new String[]{"pv", "cablage", "out1", "goodrive", "var-gd"}),
                        schema(UUID.randomUUID(), pompe, "Chaine pompe PV", "wiring",
                                new String[]{"pompe", "pv", "ne demarre plus", "pom-pv", "pompe solaire"})
                ));
        when(schemaRepository.countByEquipmentIdAndActiveTrue(VEI_ID)).thenReturn(4L);
        when(schemaRepository.countByEquipmentIdAndActiveTrue(GD_ID)).thenReturn(4L);
        when(schemaRepository.countByEquipmentIdAndActiveTrue(POM_ID)).thenReturn(1L);

        List<EquipmentSchemaDto> result = matcher.match(signals, context);

        assertTrue(result.size() >= 2);
        assertTrue(result.stream().anyMatch(s -> s.equipmentCode().equals("POM-PV") && s.totalSchemasForEquipment() == 1));
        assertTrue(result.stream().anyMatch(s -> s.equipmentCode().equals("VAR-VEI-SI23") && s.totalSchemasForEquipment() == 4));
    }

    private static Equipment equipment(
            UUID id, String code, String designation, String famille, String zone, String constructeur
    ) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setCode(code);
        equipment.setDesignation(designation);
        equipment.setFamille(famille);
        equipment.setZone(zone);
        equipment.setConstructeur(constructeur);
        return equipment;
    }

    private static EquipmentSchema schema(
            UUID id, Equipment equipment, String label, String type, String[] keywords
    ) {
        return EquipmentSchema.builder()
                .id(id)
                .equipment(equipment)
                .label(label)
                .schemaType(type)
                .sourcePdf("manual.pdf")
                .sourcePage(1)
                .filePath("equipment/" + equipment.getId() + "/schema-" + id + ".png")
                .mimeType("image/png")
                .caption(label)
                .triggerKeywords(keywords)
                .active(true)
                .build();
    }
}
