package com.ocp.eia.modules.asset;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentSchemaSeedIntegrityTest {

    private static final String SEED_PREFIX = "seed/equipment-schemas/";

    private record SeedEntry(
            UUID schemaId,
            UUID equipmentId,
            String equipmentCode,
            String fileName,
            String expectedConstructeur
    ) {}

    private static final List<SeedEntry> MANIFEST = List.of(
            entry("c0010001-0001-0001-0001-000000000001", "b0020001-0001-0001-0001-000000000001", "VAR-VEI-SI23", "veichi-p04-dimension.png", "VEICHI"),
            entry("c0010002-0002-0002-0002-000000000002", "b0020001-0001-0001-0001-000000000001", "VAR-VEI-SI23", "veichi-p06-wiring-power.png", "VEICHI"),
            entry("c0010003-0003-0003-0003-000000000003", "b0020001-0001-0001-0001-000000000001", "VAR-VEI-SI23", "veichi-p07-wiring-control.png", "VEICHI"),
            entry("c0010018-0018-0018-0018-000000000018", "b0020001-0001-0001-0001-000000000001", "VAR-VEI-SI23", "veichi-p05-terminals.png", "VEICHI"),
            entry("c0010004-0004-0004-0004-000000000004", "b0020006-0006-0006-0006-000000000006", "SEN-EAU", "veichi-p07-sonde-eau.png", null),
            entry("c0010005-0005-0005-0005-000000000005", "b0020002-0002-0002-0002-000000000002", "VAR-GD-100PV", "goodrive-p12-install.png", "INVT"),
            entry("c0010006-0006-0006-0006-000000000006", "b0020002-0002-0002-0002-000000000002", "VAR-GD-100PV", "goodrive-p13-system-pv.png", "INVT"),
            entry("c0010008-0008-0008-0008-000000000008", "b0020002-0002-0002-0002-000000000002", "VAR-GD-100PV", "goodrive-p14-terminals.png", "INVT"),
            entry("c0010020-0020-0020-0020-000000000020", "b0020002-0002-0002-0002-000000000002", "VAR-GD-100PV", "goodrive-p16-digital-io.png", "INVT"),
            entry("c0010007-0007-0007-0007-000000000007", "b0020004-0004-0004-0004-000000000004", "POM-PV", "goodrive-p13-pompe-pv.png", null),
            entry("c0010009-0009-0009-0009-000000000009", "b0020003-0003-0003-0003-000000000003", "MOT-PV", "goodrive-p15-motor-terminals.png", null),
            entry("c0010010-0010-0010-0010-000000000010", "b0020005-0005-0005-0005-000000000005", "CAP-PV", "goodrive-p13-cap-pv.png", null),
            entry("c0010011-0011-0011-0011-000000000011", "b0010008-0008-0008-0008-000000000008", "VAR-HIT-SJ200", "hitachi-p2-13-dimension.png", "Hitachi"),
            entry("c0010012-0012-0012-0012-000000000012", "b0010008-0008-0008-0008-000000000008", "VAR-HIT-SJ200", "hitachi-p2-20-terminals-input.png", "Hitachi"),
            entry("c0010019-0019-0019-0019-000000000019", "b0010008-0008-0008-0008-000000000008", "VAR-HIT-SJ200", "hitachi-p2-23-motor-output.png", "Hitachi"),
            entry("c0010013-0013-0013-0013-000000000013", "b0010007-0007-0007-0007-000000000007", "VAR-ABB-11", "acs880-p128-zcu12.png", "ABB"),
            entry("c0010014-0014-0014-0014-000000000014", "b0010007-0007-0007-0007-000000000007", "VAR-ABB-11", "acs880-p218-sto.png", "ABB"),
            entry("c0010015-0015-0015-0015-000000000015", "b0010001-0001-0001-0001-000000000001", "VAR-ACS-SPIN", "acs880-spin-p30-control.png", "ABB"),
            entry("c0010016-0016-0016-0016-000000000016", "b0010005-0005-0005-0005-000000000005", "ENC-FEN", "acs880-spin-p30-encoder.png", "ABB"),
            entry("c0010017-0017-0017-0017-000000000017", "b0010006-0006-0006-0006-000000000006", "FREIN-MEC", "acs880-spin-p108-brake.png", null)
    );

    @Test
    void manifest_hasTwentyUniqueSchemas() {
        assertEquals(20, MANIFEST.size());
        assertEquals(20, MANIFEST.stream().map(SeedEntry::schemaId).distinct().count());
        assertEquals(20, MANIFEST.stream().map(SeedEntry::fileName).distinct().count());
    }

    @Test
    void eachSchemaFileExistsOnClasspath() {
        for (SeedEntry entry : MANIFEST) {
            String resource = SEED_PREFIX + entry.fileName();
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(stream, "Missing seed PNG: " + resource);
            } catch (Exception e) {
                throw new AssertionError("Unable to read seed PNG: " + resource, e);
            }
        }
    }

    @Test
    void filePathsAreUniquePerEquipment() {
        Set<String> globalPaths = new HashSet<>();
        Set<String> equipmentFilePairs = new HashSet<>();

        for (SeedEntry entry : MANIFEST) {
            String filePath = "equipment/" + entry.equipmentId() + "/" + entry.fileName();
            assertTrue(globalPaths.add(filePath), "Duplicate file path: " + filePath);
            assertTrue(
                    equipmentFilePairs.add(entry.equipmentId() + "|" + entry.fileName()),
                    "Duplicate equipment/file pair: " + entry.equipmentCode() + " / " + entry.fileName()
            );
        }
    }

    @Test
    void seedFolderContainsExactlyManifestFiles() throws Exception {
        Set<String> expected = MANIFEST.stream().map(SeedEntry::fileName).collect(Collectors.toSet());
        assertEquals(20, expected.size());

        URL url = getClass().getClassLoader().getResource(SEED_PREFIX);
        assertNotNull(url, "Missing seed directory: " + SEED_PREFIX);

        if (!"file".equals(url.getProtocol())) {
            return;
        }

        Set<String> onClasspath;
        try (var stream = Files.list(Path.of(url.toURI()))) {
            onClasspath = stream
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }

        assertEquals(expected, onClasspath, "Seed folder must contain exactly manifest PNGs (no orphans)");
    }

    @Test
    void equipmentCodesMatchExpectedAssignments() {
        for (SeedEntry entry : MANIFEST) {
            String expectedSuffix = entry.equipmentCode().toLowerCase().replace("-", "");
            if (entry.equipmentCode().startsWith("VAR-VEI")) {
                assertTrue(entry.fileName().startsWith("veichi-"), entry.fileName());
            } else if (entry.equipmentCode().equals("SEN-EAU")) {
                assertTrue(entry.fileName().contains("sonde-eau"), entry.fileName());
            } else if (entry.equipmentCode().startsWith("VAR-GD")) {
                assertTrue(entry.fileName().startsWith("goodrive-"), entry.fileName());
            } else if (entry.equipmentCode().equals("POM-PV")) {
                assertTrue(entry.fileName().contains("pompe-pv"), entry.fileName());
            } else if (entry.equipmentCode().equals("MOT-PV")) {
                assertTrue(entry.fileName().contains("motor"), entry.fileName());
            } else if (entry.equipmentCode().equals("CAP-PV")) {
                assertTrue(entry.fileName().contains("cap-pv"), entry.fileName());
            } else if (entry.equipmentCode().startsWith("VAR-HIT")) {
                assertTrue(entry.fileName().startsWith("hitachi-"), entry.fileName());
            } else if (entry.equipmentCode().startsWith("VAR-ABB")) {
                assertTrue(entry.fileName().startsWith("acs880-"), entry.fileName());
            } else if (entry.equipmentCode().startsWith("VAR-ACS") || entry.equipmentCode().equals("ENC-FEN")) {
                assertTrue(entry.fileName().startsWith("acs880-spin-"), entry.fileName());
            } else if (entry.equipmentCode().equals("FREIN-MEC")) {
                assertTrue(entry.fileName().contains("brake"), entry.fileName());
            }
            assertNotNull(expectedSuffix);
        }
    }

    private static SeedEntry entry(
            String schemaId, String equipmentId, String equipmentCode, String fileName, String constructeur
    ) {
        return new SeedEntry(
                UUID.fromString(schemaId),
                UUID.fromString(equipmentId),
                equipmentCode,
                fileName,
                constructeur
        );
    }
}
