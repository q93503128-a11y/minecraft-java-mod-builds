package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContentKernelTest {
    @Test
    void fixtureContentResolvesWithoutErrors() {
        var report = CoreContentBootstrap.bootstrapAndValidate();
        assertFalse(report.hasErrors(), report::format);
        assertEquals(7, report.definitionCount());
    }

    @Test
    void duplicateStableIdsAreRejected() {
        ContentRegistry registry = new ContentRegistry();
        ContentId id = ContentId.rift("archetype/test");
        registry.register(new CoreDefinition.CombatArchetype(id, Set.of("pursue")));
        assertThrows(IllegalStateException.class, () -> registry.register(new CoreDefinition.CombatArchetype(id, Set.of("retreat"))));
    }

    @Test
    void missingReferencesFailValidationBeforeRuntimeUse() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.Creature(ContentId.rift("creature/broken_fixture"), ContentId.rift("region/missing"), ContentId.rift("archetype/missing"), ContentId.rift("loot/missing"), Set.of("pursue")));
        var report = new ContentValidator().validate(registry);
        assertTrue(report.hasErrors());
        assertEquals(3, report.issues().stream().filter(i -> i.severity() == ContentValidator.Severity.ERROR).count());
    }

    @Test
    void encounterWithoutConsequenceWarnsButDoesNotHardFail() {
        ContentRegistry registry = new ContentRegistry();
        ContentId region = ContentId.rift("region/test");
        ContentId archetype = ContentId.rift("archetype/test");
        ContentId loot = ContentId.rift("loot/test");
        ContentId creature = ContentId.rift("creature/test");
        registry.register(new CoreDefinition.CombatArchetype(archetype, Set.of("pursue")));
        registry.register(new CoreDefinition.Region(region, "test rule", Set.of(archetype)));
        registry.register(new CoreDefinition.LootProfile(loot, List.of("test")));
        registry.register(new CoreDefinition.Creature(creature, region, archetype, loot, Set.of("pursue")));
        registry.register(new CoreDefinition.Encounter(ContentId.rift("encounter/test"), region, List.of(creature), "survive", ""));
        var report = new ContentValidator().validate(registry);
        assertFalse(report.hasErrors(), report::format);
        assertEquals(1, report.warnings().size());
    }

    @Test
    void contentIdParsingRejectsUnstableSyntax() {
        assertEquals("riftfrontier:region/test", ContentId.parse("riftfrontier:region/test").toString());
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("RiftFrontier:Bad Path"));
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("missing_namespace"));
    }
}
