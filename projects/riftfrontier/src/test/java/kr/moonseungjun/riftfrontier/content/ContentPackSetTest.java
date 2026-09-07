package kr.moonseungjun.riftfrontier.content;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentPackSetTest {
    private final ContentPackLoader loader = new ContentPackLoader();

    @Test
    void crossDocumentReferencesBecomeValidAfterMerge() {
        var base = loader.load(new StringReader("""
            {"schema_version":1,"pack_id":"riftfrontier:base","definitions":[
              {"kind":"combat_archetype","id":"riftfrontier:archetype/skirmisher","behaviours":["pursue"]},
              {"kind":"loot_profile","id":"riftfrontier:loot/basic","pools":["scrap"]},
              {"kind":"region","id":"riftfrontier:region/test","gameplay_rule":"unstable weather","archetypes":["riftfrontier:archetype/skirmisher"]}
            ]}
            """), "riftfrontier:content/base.json");
        var extension = loader.load(new StringReader("""
            {"schema_version":1,"pack_id":"riftfrontier:extension","depends_on":["riftfrontier:base"],"definitions":[
              {"kind":"creature","id":"riftfrontier:creature/scout","region":"riftfrontier:region/test","archetype":"riftfrontier:archetype/skirmisher","loot_profile":"riftfrontier:loot/basic","behaviours":["pursue"]}
            ]}
            """), "riftfrontier:content/extension.json");

        assertTrue(extension.validation().hasErrors(), "an isolated extension is expected to have unresolved references");
        var merged = ContentPackSet.merge(List.of(extension, base));
        var report = new ContentValidator().validate(merged.registry());

        assertFalse(report.hasErrors(), report.format());
        assertEquals(List.of("riftfrontier:base", "riftfrontier:extension"), merged.packIds());
        assertEquals("riftfrontier:content/base.json", merged.provenance().get("riftfrontier:base"));
        assertEquals("riftfrontier:content/extension.json", merged.provenance().get("riftfrontier:extension"));
        assertEquals(4, merged.registry().size());
    }

    @Test
    void missingPackDependencyIsRejectedBeforeRegistryPublication() {
        var extension = loader.load(new StringReader("""
            {"schema_version":1,"pack_id":"riftfrontier:extension","depends_on":["riftfrontier:missing"],"definitions":[]}
            """), "riftfrontier:content/extension.json");

        var error = assertThrows(IllegalArgumentException.class, () -> ContentPackSet.merge(List.of(extension)));
        assertTrue(error.getMessage().contains("requires missing pack"));
        assertTrue(error.getMessage().contains("riftfrontier:content/extension.json"));
    }

    @Test
    void dependencyCyclesAreRejectedDeterministically() {
        var alpha = loader.load(new StringReader("""
            {"schema_version":1,"pack_id":"riftfrontier:alpha","depends_on":["riftfrontier:beta"],"definitions":[]}
            """));
        var beta = loader.load(new StringReader("""
            {"schema_version":1,"pack_id":"riftfrontier:beta","depends_on":["riftfrontier:alpha"],"definitions":[]}
            """));

        var error = assertThrows(IllegalArgumentException.class, () -> ContentPackSet.merge(List.of(beta, alpha)));
        assertTrue(error.getMessage().contains("dependency cycle"));
        assertTrue(error.getMessage().contains("riftfrontier:alpha"));
        assertTrue(error.getMessage().contains("riftfrontier:beta"));
    }

    @Test
    void duplicatePackIdsAreRejectedWithBothSources() {
        String json = """
            {"schema_version":1,"pack_id":"riftfrontier:duplicate","definitions":[
              {"kind":"combat_archetype","id":"riftfrontier:archetype/one","behaviours":["pursue"]}
            ]}
            """;
        var first = loader.load(new StringReader(json), "riftfrontier:content/one.json");
        var second = loader.load(new StringReader(json.replace("archetype/one", "archetype/two")), "riftfrontier:content/two.json");

        var error = assertThrows(IllegalArgumentException.class, () -> ContentPackSet.merge(List.of(first, second)));
        assertTrue(error.getMessage().contains("riftfrontier:content/one.json"));
        assertTrue(error.getMessage().contains("riftfrontier:content/two.json"));
    }
}
