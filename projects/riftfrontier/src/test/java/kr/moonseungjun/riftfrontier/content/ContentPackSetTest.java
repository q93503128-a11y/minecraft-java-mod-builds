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
              {"type":"combat_archetype","id":"riftfrontier:archetype/skirmisher","behaviours":["pursue"]},
              {"type":"loot_profile","id":"riftfrontier:loot/basic","pools":["scrap"]},
              {"type":"region","id":"riftfrontier:region/test","gameplay_rule":"unstable weather","archetypes":["riftfrontier:archetype/skirmisher"]}
            ]}
            """));
        var extension = loader.load(new StringReader("""
            {"schema_version":1,"pack_id":"riftfrontier:extension","definitions":[
              {"type":"creature","id":"riftfrontier:creature/scout","region":"riftfrontier:region/test","archetype":"riftfrontier:archetype/skirmisher","loot_profile":"riftfrontier:loot/basic","behaviours":["pursue"]}
            ]}
            """));

        assertTrue(extension.validation().hasErrors(), "an isolated extension is expected to have unresolved references");
        var merged = ContentPackSet.merge(List.of(base, extension));
        var report = new ContentValidator().validate(merged.registry());

        assertFalse(report.hasErrors(), report.format());
        assertEquals(List.of("riftfrontier:base", "riftfrontier:extension"), merged.packIds());
        assertEquals(4, merged.registry().size());
    }

    @Test
    void duplicatePackIdsAreRejected() {
        String json = """
            {"schema_version":1,"pack_id":"riftfrontier:duplicate","definitions":[
              {"type":"combat_archetype","id":"riftfrontier:archetype/one","behaviours":["pursue"]}
            ]}
            """;
        var first = loader.load(new StringReader(json));
        var second = loader.load(new StringReader(json.replace("archetype/one", "archetype/two")));

        assertThrows(IllegalArgumentException.class, () -> ContentPackSet.merge(List.of(first, second)));
    }
}
