package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContentKernelTest {
    @Test
    void fixtureContentLoadsFromJsonAndResolvesWithoutErrors() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        assertFalse(pack.validation().hasErrors(), pack.validation()::format);
        assertEquals(10, pack.validation().definitionCount());
        assertEquals(ContentPackLoader.CURRENT_CONTENT_SCHEMA, pack.schemaVersion());
        assertEquals("riftfrontier:fixture/vertical_slice_01", pack.packId());
    }

    @Test
    void catalogIsDeterministicAndCountsTypedDefinitions() {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var first = ContentCatalog.from(pack.registry());
        var second = ContentCatalog.from(pack.registry());
        assertEquals(first.fingerprint(), second.fingerprint());
        assertEquals(2, first.counts().get(CoreDefinition.Kind.COMBAT_ARCHETYPE));
        assertEquals(2, first.counts().get(CoreDefinition.Kind.CREATURE));
        assertEquals(1, first.counts().get(CoreDefinition.Kind.EXPEDITION_RESOURCE));
        assertEquals(1, first.counts().get(CoreDefinition.Kind.CONTRACT));
        assertEquals(1, first.counts().get(CoreDefinition.Kind.EXTRACTION_RESULT));
        assertEquals(10, first.entries().size());
        assertEquals(64, first.fingerprint().length());
    }

    @Test
    void m2RegionContractResourceReferencesResolveAsOneGraph() {
        var registry = CoreContentBootstrap.bootstrapAndValidate().registry();
        ContentId regionId = ContentId.rift("region/vertical_slice_01");
        ContentId resourceId = ContentId.rift("resource/rift_salvage");
        ContentId contractId = ContentId.rift("contract/salvage_recovery");

        var region = (CoreDefinition.Region) registry.find(CoreDefinition.Kind.REGION, regionId).orElseThrow();
        var resource = (CoreDefinition.ExpeditionResource) registry.find(CoreDefinition.Kind.EXPEDITION_RESOURCE, resourceId).orElseThrow();
        var contract = (CoreDefinition.Contract) registry.find(CoreDefinition.Kind.CONTRACT, contractId).orElseThrow();

        assertTrue(region.resources().contains(resourceId));
        assertTrue(region.contracts().contains(contractId));
        assertEquals(regionId, resource.region());
        assertEquals(regionId, contract.region());
        assertEquals(3, contract.requiredResources().get(resourceId));
        assertTrue(registry.contains(CoreDefinition.Kind.EXTRACTION_RESULT, contract.extractionResult()));
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
        assertEquals(3, report.byCode(ContentValidator.Code.MISSING_REFERENCE).size());
        assertTrue(report.format().contains("[MISSING_REFERENCE]"));
    }

    @Test
    void packLoaderReportsBrokenReferencesBeforeRuntimeUse() {
        String json = """
            {
              "schema_version": 1,
              "pack_id": "riftfrontier:fixture/broken",
              "definitions": [
                {
                  "kind": "creature",
                  "id": "riftfrontier:creature/broken",
                  "region": "riftfrontier:region/missing",
                  "archetype": "riftfrontier:archetype/missing",
                  "loot_profile": "riftfrontier:loot/missing",
                  "behaviours": ["pursue"]
                }
              ]
            }
            """;
        var pack = new ContentPackLoader().load(new StringReader(json));
        assertTrue(pack.validation().hasErrors());
        assertThrows(IllegalStateException.class, pack::requireValid);
    }

    @Test
    void unsupportedContentSchemaIsRejectedExplicitly() {
        String json = """
            {"schema_version": 999, "pack_id": "riftfrontier:fixture/future", "definitions": []}
            """;
        var error = assertThrows(IllegalArgumentException.class, () -> new ContentPackLoader().load(new StringReader(json)));
        assertTrue(error.getMessage().contains("Unsupported content schema"));
    }

    @Test
    void duplicateValuesInDataAreRejectedInsteadOfSilentlyCollapsed() {
        String json = """
            {
              "schema_version": 1,
              "pack_id": "riftfrontier:fixture/duplicate",
              "definitions": [
                {
                  "kind": "combat_archetype",
                  "id": "riftfrontier:archetype/duplicate",
                  "behaviours": ["pursue", "pursue"]
                }
              ]
            }
            """;
        assertThrows(IllegalArgumentException.class, () -> new ContentPackLoader().load(new StringReader(json)));
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
        assertEquals(1, report.byCode(ContentValidator.Code.NO_WORLD_CONSEQUENCE).size());
    }

    @Test
    void contentIdParsingRejectsUnstableSyntax() {
        assertEquals("riftfrontier:region/test", ContentId.parse("riftfrontier:region/test").toString());
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("RiftFrontier:Bad Path"));
        assertThrows(IllegalArgumentException.class, () -> ContentId.parse("missing_namespace"));
    }
}
