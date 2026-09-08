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
    void progressionSnapshotRoundTripsOwnedLockedStatsAndPresentationFacts() {
        ProgressionNetworkPayloads.CharacterView zombie = new ProgressionNetworkPayloads.CharacterView(
                "turnbound_re:zombie", true, 2, 3, 30, 40, 2,
                List.of("VANGUARD", "BREAKER"), 380, 97, 84, 29, 119,
                List.of("FIRE=WEAK", "MELEE=RESIST"),
                "turnbound_re:zombie_rotten_swing",
                List.of("turnbound_re:zombie_undead_grit", "turnbound_re:zombie_gravebreaker"),
                "turnbound_re:zombie_relentless_horde");
        ProgressionNetworkPayloads.CharacterView witch = new ProgressionNetworkPayloads.CharacterView(
                "turnbound_re:witch", false, 4, 4, 0, 50, 4,
                List.of("SUPPORT", "CONTROLLER"), 125, 34, 26, 24, 85,
                List.of("ARCANE=RESIST"),
                "turnbound_re:witch_splash_hex",
                List.of("turnbound_re:witch_weakening_brew"),
                "turnbound_re:witch_cauldron_overflow");
        ProgressionNetworkPayloads.Snapshot snapshot = new ProgressionNetworkPayloads.Snapshot(
                1200, 85, 12, List.of(zombie.id()), List.of(zombie, witch), "ACCEPTED", "partyCost=2");

        ProgressionNetworkPayloads.Snapshot decoded = ProgressionNetworkPayloads.ProgressSnapshotS2C.of(snapshot).decode();
        assertEquals(snapshot, decoded);
        assertTrue(decoded.character(zombie.id()).orElseThrow().owned());
        assertFalse(decoded.character(witch.id()).orElseThrow().owned());
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
    }

    private static ProgressionNetworkPayloads.CharacterView view(String id, boolean owned, int cost) {
        return new ProgressionNetworkPayloads.CharacterView(
                id, owned, 2, 2, owned ? 1 : 0, 30, cost,
                List.of("STRIKER"), 100, 20, 20, 20, 60,
                List.of("FIRE=NORMAL"), "basic", List.of("skill"), "burst");
    }
}
