package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentPackLoader;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ProductionRegion01ExpeditionContentTest {
    private static final String RESOURCE = "/data/riftfrontier/riftfrontier/content/region_01.json";

    @Test
    void successfulExtractionPolicyMatchesAuthoritativePressureLoop() throws Exception {
        var stream = ProductionRegion01ExpeditionContentTest.class.getResourceAsStream(RESOURCE);
        assertNotNull(stream, "production Region 01 pack must be packaged");

        ContentPackLoader.LoadedPack pack;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            pack = new ContentPackLoader().load(reader, RESOURCE);
        }
        pack.requireValid();

        ContentId extractionId = ContentId.parse("riftfrontier:extraction/region_01_secured_return");
        var extraction = (CoreDefinition.ExtractionResultProfile) pack.registry()
            .find(CoreDefinition.Kind.EXTRACTION_RESULT, extractionId)
            .orElseThrow();

        assertEquals(100, extraction.retainedPercent());
        assertEquals(1, extraction.threatDelta(),
            "successful Region 01 extraction must agree with the authoritative +1 regional pressure settlement");
        assertTrue(extraction.worldConsequence().contains("raises Region 01 pressure"),
            "content policy must not tell players or downstream systems that a successful extraction reduces pressure");
        assertFalse(extraction.worldConsequence().contains("reduces unresolved expedition pressure"));
    }
}
