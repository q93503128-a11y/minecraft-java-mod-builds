package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.data.DefinitionBundleParser;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6ExpeditionWorldFirstContractTest {
    @Test
    void journalProjectsOnlyAuthoredWorldAnchorsInStableOrder() throws IOException {
        DefinitionBundleParser.Parsed parsed = ProductionDefinitionFixture.load();
        DefinitionRegistry definitions = parsed.registry();

        List<ExpeditionNetworkPayloads.EncounterView> views = ExpeditionJournalProjection.encounters(definitions);

        assertEquals(List.of("turnbound_re:debug_overworld_patrol"),
                views.stream().map(ExpeditionNetworkPayloads.EncounterView::id).toList());
        assertTrue(views.stream().allMatch(view -> view.difficulty() > 0 && view.enemyCount() > 0));
        assertEquals(List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider"),
                views.getFirst().enemySourceEntities());
        assertTrue(views.getFirst().hasWorldRoute());
        assertEquals("turnbound_re:region_01/overworld_patrol", views.getFirst().locator());
        assertEquals("minecraft:overworld", views.getFirst().dimension());
        assertEquals(325, views.getFirst().x());
        assertEquals(71, views.getFirst().y());
        assertEquals(290, views.getFirst().z());
    }

    @Test
    void journalSnapshotRoundTripsServerAuthoredEnemyVisualIdentities() throws IOException {
        DefinitionRegistry definitions = ProductionDefinitionFixture.load().registry();
        ExpeditionNetworkPayloads.JournalView view = new ExpeditionNetworkPayloads.JournalView(
                List.of("turnbound_re:zombie"),
                ExpeditionJournalProjection.encounters(definitions),
                "",
                "");

        ExpeditionNetworkPayloads.JournalView decoded = ExpeditionNetworkPayloads.JournalSnapshotS2C.of(view).decode();

        assertEquals(view, decoded);
    }

    @Test
    void expeditionWireHasNoDirectEncounterStartCommand() {
        boolean directStartPayloadExists = List.of(ExpeditionNetworkPayloads.class.getDeclaredClasses()).stream()
                .anyMatch(type -> "StartEncounterC2S".equals(type.getSimpleName()));

        assertFalse(directStartPayloadExists,
                "journal must not expose an encounter-id launch command; world anchors own production entry");
    }
}
