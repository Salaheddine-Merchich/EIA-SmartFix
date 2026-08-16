package com.ocp.eia.modules.knowledge.application;

import com.ocp.eia.modules.knowledge.domain.model.QuerySignals;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuerySignalExtractorTest {

    @Test
    void extract_e21InLongPhrase_findsCode() {
        QuerySignals signals = QuerySignalExtractor.extract("E21 surchauffe variateur Hitachi SJ200");

        assertTrue(signals.faultCodes().contains("E21"));
        assertEquals("Hitachi", signals.manufacturer().orElseThrow());
    }

    @Test
    void extract_2310AbbFilature_findsCodeAndManufacturer() {
        QuerySignals signals = QuerySignalExtractor.extract("2310 surintensité ABB filature");

        assertTrue(signals.faultCodes().contains("2310"));
        assertEquals("ABB", signals.manufacturer().orElseThrow());
        assertEquals("filature", signals.equipmentHint().orElseThrow());
        assertEquals("Filature", signals.equipmentZone().orElseThrow());
    }

    @Test
    void extract_out1Goodrive_normalizesCase() {
        QuerySignals signals = QuerySignalExtractor.extract("OUt1 protection phase U Goodrive");

        assertEquals("OUt1", signals.faultCodes().get(0));
        assertEquals("Goodrive", signals.manufacturer().orElseThrow());
    }

    @Test
    void extract_f001Convoyeur_findsCode() {
        QuerySignals signals = QuerySignalExtractor.extract("F001 surchauffe convoyeur");

        assertTrue(signals.faultCodes().contains("F001"));
        assertEquals("convoyage", signals.equipmentHint().orElseThrow());
        assertEquals("Convoyeur", signals.equipmentFamily().orElseThrow());
        assertEquals("Zone Convoyage", signals.equipmentZone().orElseThrow());
    }

    @Test
    void extract_suffixCode_trvPreferred() {
        QuerySignals signals = QuerySignalExtractor.extract("2310-TRV surintensité traverse");

        assertTrue(signals.faultCodes().contains("2310-TRV"));
    }

    @Test
    void extract_calendarYear_notTreatedAsFaultCode() {
        QuerySignals signals = QuerySignalExtractor.extract("Pompe PV ne démarre plus depuis 2024");

        assertFalse(signals.hasFaultCodes());
        assertFalse(signals.faultCodes().contains("2024"));
    }

    @Test
    void extract_yearBeforeRealCode_keepsOnlyRealCode() {
        QuerySignals signals = QuerySignalExtractor.extract("depuis 2024 le variateur affiche E21");

        assertEquals(List.of("E21"), signals.faultCodes());
        assertFalse(signals.faultCodes().contains("2024"));
    }

    @Test
    void extract_2310StillRecognizedAsFaultCode() {
        QuerySignals signals = QuerySignalExtractor.extract("2310 surintensité ABB filature");

        assertTrue(signals.faultCodes().contains("2310"));
    }

    @Test
    void isLikelyCalendarYear_distinguishesYearFromIndustrialCode() {
        assertTrue(QuerySignalExtractor.isLikelyCalendarYear("2024"));
        assertTrue(QuerySignalExtractor.isLikelyCalendarYear("1999"));
        assertFalse(QuerySignalExtractor.isLikelyCalendarYear("2310"));
        assertFalse(QuerySignalExtractor.isLikelyCalendarYear("E21"));
        assertFalse(QuerySignalExtractor.isLikelyCalendarYear("1500"));
    }

    @Test
    void extract_blankQuery_returnsEmpty() {
        QuerySignals signals = QuerySignalExtractor.extract("   ");

        assertFalse(signals.hasFaultCodes());
        assertTrue(signals.manufacturer().isEmpty());
    }

    @Test
    void normalizeCode_outLowerCase() {
        assertEquals("OUt1", QuerySignalExtractor.normalizeCode("out1"));
    }

    @Test
    void extract_pompePvNoStart_resolvesZoneFamilyAndSymptoms() {
        QuerySignals signals = QuerySignalExtractor.extract("Pompe PV ne démarre plus station solaire");

        assertTrue(signals.equipmentFamily().isEmpty(),
                "Pompe PV = zone Station PV sans verrouillage famille (variateur inclus)");
        assertEquals("Station PV", signals.equipmentZone().orElseThrow());
        assertTrue(signals.hasSymptomCategory("no_start"));
        assertTrue(signals.symptomKeywords().contains("veille"));
        assertTrue(signals.symptomKeywords().contains("sommeil"));
        assertTrue(signals.hasSemanticContext());
    }
}
