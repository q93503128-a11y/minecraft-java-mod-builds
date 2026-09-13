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

        assertEquals(List.of(
                "turnbound_re:debug_overworld_patrol",
                "turnbound_re:debug_rift_elite"),
                views.stream().map(ExpeditionNetworkPayloads.EncounterView::id).toList());
        assertEquals(views.size(), views.stream().map(ExpeditionNetworkPayloads.EncounterView::id).distinct().count());
        assertTrue(views.stream().allMatch(view -> view.difficulty() > 0 && view.enemyCount() > 0));
    }

    @Test
    void expeditionWireHasNoDirectEncounterStartCommand() {
        boolean directStartPayloadExists = List.of(ExpeditionNetworkPayloads.class.getDeclaredClasses()).stream()
                .anyMatch(type -> "StartEncounterC2S".equals(type.getSimpleName()));

        assertFalse(directStartPayloadExists,
                "journal must not expose an encounter-id launch command; world anchors own production entry");
    }
}
