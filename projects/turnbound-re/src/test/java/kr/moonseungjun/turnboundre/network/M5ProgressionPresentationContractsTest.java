package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.client.PartyFormationDraft;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M5ProgressionPresentationContractsTest {
    @Test
    void setPartyWireRoundTripsExpectedAndRequestedComposition() {
        ProgressionNetworkPayloads.SetPartyC2S payload = ProgressionNetworkPayloads.SetPartyC2S.of(
                List.of("turnbound_re:zombie", "turnbound_re:skeleton"),
                List.of("turnbound_re:witch", "turnbound_re:zombie", "turnbound_re:skeleton"));
        ProgressionNetworkPayloads.DecodedSetParty decoded = payload.decode();
        assertEquals(List.of("turnbound_re:zombie", "turnbound_re:skeleton"), decoded.expectedParty());
        assertEquals(List.of("turnbound_re:witch", "turnbound_re:zombie", "turnbound_re:skeleton"), decoded.requestedParty());
    }

    @Test
    void growthWireCarriesStaleWriteGuard() {
        ProgressionNetworkPayloads.DecodedGrowth decoded = ProgressionNetworkPayloads.GrowthC2S
                .of("ASCEND", "turnbound_re:zombie", 3, 40).decode();
        assertEquals("ASCEND", decoded.operation());
        assertEquals("turnbound_re:zombie", decoded.characterId());
        assertEquals(3, decoded.expectedStar());
        assertEquals(40, decoded.expectedLevel());
    }

    @Test
    void progressionSnapshotRoundTripsSkillsGrowthAndPresentationFacts() {
        ProgressionNetworkPayloads.ActionView action = new ProgressionNetworkPayloads.ActionView(
                "turnbound_re:zombie_gravebreaker", "SKILL", -35, 65, 70, "MELEE",
                "ENEMY", "SINGLE", 1,
                List.of(new ProgressionNetworkPayloads.EffectView("DAMAGE", "", 1.0, 0, 1.0),
                        new ProgressionNetworkPayloads.EffectView("APPLY_STATUS", "turnbound_re:def_down", 0.0, 2, 0.7)));
        ProgressionNetworkPayloads.GrowthView growth = new ProgressionNetworkPayloads.GrowthView(
                true, 18, 31, new ProgressionNetworkPayloads.StatsView(388, 99, 86, 29, 121),
                new ProgressionNetworkPayloads.CostView(420, 21, 0), "",
                4, 50, new ProgressionNetworkPayloads.StatsView(405, 104, 91, 30, 126),
                new ProgressionNetworkPayloads.CostView(1200, 75, 20), "NOT_AT_LEVEL_CAP");
        ProgressionNetworkPayloads.CharacterView zombie = new ProgressionNetworkPayloads.CharacterView(
                "turnbound_re:zombie", true, 2, 3, 30, 40, 2,
                List.of("VANGUARD", "BREAKER"), 380, 97, 84, 29, 119,
                List.of("FIRE=WEAK", "MELEE=RESIST"),
                "turnbound_re:zombie_rotten_swing",
                List.of("turnbound_re:zombie_undead_grit", "turnbound_re:zombie_gravebreaker"),
                "turnbound_re:zombie_relentless_horde",
                List.of("turnbound_re:zombie_undead_endurance"),
                List.of(action), growth);
        ProgressionNetworkPayloads.Snapshot snapshot = new ProgressionNetworkPayloads.Snapshot(
                1200, 85, 12, List.of(zombie.id()), List.of(zombie), "GROWTH_ACCEPTED", "level=30");

        ProgressionNetworkPayloads.Snapshot decoded = ProgressionNetworkPayloads.ProgressSnapshotS2C.of(snapshot).decode();
        assertEquals(snapshot, decoded);
        assertEquals("turnbound_re:def_down", decoded.character(zombie.id()).orElseThrow()
                .action(action.id()).orElseThrow().effects().get(1).status());
        assertEquals(420, decoded.character(zombie.id()).orElseThrow().growth().levelCost().coin());
        assertFalse(decoded.character(zombie.id()).orElseThrow().growth().canAscend());
    }

    @Test
    void partyDraftSwapsExistingMembersInsteadOfCreatingDuplicates() {
        List<String> current = List.of("a", "b", "c");
        assertEquals(List.of("c", "b", "a"), PartyFormationDraft.assign(current, 0, "c"));
        assertEquals(List.of("a", "c"), PartyFormationDraft.remove(List.of("a", "b", "c"), 1));
    }

    @Test
    void clientPreviewUsesOnlyServerPublishedCostsAndStillBlocksOverCapacityDraft() {
        ProgressionNetworkPayloads.CharacterView a = view("a", true, 5);
        ProgressionNetworkPayloads.CharacterView b = view("b", true, 5);
        ProgressionNetworkPayloads.CharacterView c = view("c", true, 4);
        ProgressionNetworkPayloads.Snapshot snapshot = new ProgressionNetworkPayloads.Snapshot(
                0, 0, 12, List.of("a"), List.of(a, b, c), "", "");

        assertEquals(10, PartyFormationDraft.cost(List.of("a", "b"), snapshot));
        assertTrue(PartyFormationDraft.isSubmittable(List.of("a", "b"), snapshot));
        assertEquals(14, PartyFormationDraft.cost(List.of("a", "b", "c"), snapshot));
        assertFalse(PartyFormationDraft.isSubmittable(List.of("a", "b", "c"), snapshot));
    }

    @Test
    void unownedCharacterCannotBecomeAClientSubmittableParty() {
        ProgressionNetworkPayloads.CharacterView locked = view("locked", false, 2);
        ProgressionNetworkPayloads.Snapshot snapshot = new ProgressionNetworkPayloads.Snapshot(
                0, 0, 12, List.of(), List.of(locked), "", "");
        assertFalse(PartyFormationDraft.isSubmittable(List.of("locked"), snapshot));
        assertFalse(locked.growth().canLevelUp());
        assertFalse(locked.growth().canAscend());
    }

    private static ProgressionNetworkPayloads.CharacterView view(String id, boolean owned, int cost) {
        ProgressionNetworkPayloads.StatsView stats = new ProgressionNetworkPayloads.StatsView(100, 20, 20, 20, 60);
        ProgressionNetworkPayloads.GrowthView growth = new ProgressionNetworkPayloads.GrowthView(
                owned, 0, owned ? 2 : 0, stats, ProgressionNetworkPayloads.CostView.ZERO,
                owned ? "" : "NOT_OWNED", 2, 30, stats, ProgressionNetworkPayloads.CostView.ZERO,
                owned ? "NOT_AT_LEVEL_CAP" : "NOT_OWNED");
        return new ProgressionNetworkPayloads.CharacterView(
                id, owned, 2, 2, owned ? 1 : 0, 30, cost,
                List.of("STRIKER"), 100, 20, 20, 20, 60,
                List.of("FIRE=NORMAL"), "basic", List.of("skill"), "burst", List.of("passive"),
                List.of(new ProgressionNetworkPayloads.ActionView("basic", "BASIC", 10, 10, 5,
                        "FIRE", "ENEMY", "SINGLE", 1,
                        List.of(new ProgressionNetworkPayloads.EffectView("DAMAGE", "", 1.0, 0, 1.0)))), growth);
    }
}
