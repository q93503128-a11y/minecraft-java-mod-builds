package kr.moonseungjun.turnboundre.network;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class M6WorldEncounterAnchorPayloadTest {
    private static final UUID ANCHOR = UUID.fromString("00000000-0000-0000-0000-000000006001");

    @Test
    void previewRoundTripPreservesServerAuthoredFacts() {
        var view = new WorldEncounterAnchorPayloads.PreviewView(
                ANCHOR,
                "turnbound_re:region_01/overworld_patrol",
                "turnbound_re:debug_overworld_patrol",
                2,
                List.of("minecraft:zombie", "minecraft:skeleton"),
                List.of("COIN", "ESSENCE", "CHARACTER_SHARD"),
                true,
                4,
                "iron_reinforcement",
                "",
                "");
        assertEquals(view, WorldEncounterAnchorPayloads.AnchorPreviewS2C.from(view).decode());
    }

    @Test
    void startRequestRoundTripBindsEntityLocatorEncounterAndPreviewedPreparation() {
        var decoded = WorldEncounterAnchorPayloads.StartAnchorEncounterC2S.of(
                ANCHOR,
                "turnbound_re:region_01/overworld_patrol",
                "turnbound_re:debug_overworld_patrol",
                "cooked_salmon_ration").decode();
        assertEquals(ANCHOR, decoded.anchorEntityId());
        assertEquals("turnbound_re:region_01/overworld_patrol", decoded.locator());
        assertEquals("turnbound_re:debug_overworld_patrol", decoded.encounterId());
        assertEquals("cooked_salmon_ration", decoded.expectedPreparationId());
    }

    @Test
    void emptyPreparationIsAValidExplicitNoPreparationSelection() {
        var view = new WorldEncounterAnchorPayloads.PreviewView(
                ANCHOR, "turnbound_re:a", "turnbound_re:e", 1,
                List.of("minecraft:zombie"), List.of("COIN"), true, 1,
                null, "", "");
        assertEquals("", view.preparationId());
        assertEquals("", WorldEncounterAnchorPayloads.AnchorPreviewS2C.from(view).decode().preparationId());

        var start = WorldEncounterAnchorPayloads.StartAnchorEncounterC2S.of(
                ANCHOR, "turnbound_re:a", "turnbound_re:e", null).decode();
        assertEquals("", start.expectedPreparationId());
    }

    @Test
    void rejectionCarriesOnlyCodeAndDetail() {
        var payload = WorldEncounterAnchorPayloads.AnchorRejectedS2C.of("TOO_FAR", "turnbound_re:region_01/overworld_patrol");
        assertEquals("TOO_FAR", payload.code());
        assertEquals("turnbound_re:region_01/overworld_patrol", payload.detail());
    }

    @Test
    void impossiblePreviewFactsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WorldEncounterAnchorPayloads.PreviewView(
                ANCHOR, "turnbound_re:a", "turnbound_re:e", 1,
                List.of(), List.of("COIN"), true, 1, "", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new WorldEncounterAnchorPayloads.PreviewView(
                ANCHOR, "turnbound_re:a", "turnbound_re:e", 1,
                List.of("minecraft:zombie"), List.of("COIN"), true, 5, "", "", ""));
    }
}
