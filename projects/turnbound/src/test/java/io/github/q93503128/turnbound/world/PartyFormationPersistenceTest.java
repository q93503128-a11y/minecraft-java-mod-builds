package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartyFormationPersistenceTest {
    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void cleanup() { CampaignProgressStore.removeRuntime(playerId); }

    @Test
    void activePartyRoundTripsAndDrivesEncounterFormation() {
        unlockP08();
        CampaignProgressStore.setActiveParty(playerId, List.of("P08", "P01", "P03"));
        String json = CampaignSaveCodec.encode(CampaignProgressStore.snapshot(playerId));
        CampaignProgressStore.removeRuntime(playerId);
        CampaignProgressStore.restore(playerId, CampaignSaveCodec.decode(json));

        assertEquals(List.of("P08", "P01", "P03"), CampaignProgressStore.activeParty(playerId));
        assertEquals(List.of("P08", "P01", "P03"), CampaignEncounterCatalog.createBattle(playerId, "ENC_M01")
                .living(io.github.q93503128.turnbound.combat.CombatantSide.ALLY)
                .stream().map(unit -> unit.definition().id()).toList());
    }

    @Test
    void partyRejectsDuplicatesUnownedAndMoreThanFour() {
        CampaignProgressStore.ensureNewGame(playerId);
        assertThrows(IllegalArgumentException.class, () -> CampaignProgressStore.setActiveParty(playerId, List.of("P01", "P01")));
        assertThrows(IllegalArgumentException.class, () -> CampaignProgressStore.setActiveParty(playerId, List.of("P02")));
        unlockP08();
        assertThrows(IllegalArgumentException.class, () -> CampaignProgressStore.setActiveParty(playerId,
                List.of("P01", "P03", "P04", "F03", "P08")));
    }

    @Test
    void activePartyGetsFullXpAndReserveGetsTwentyPercent() {
        unlockP08();
        CampaignProgressStore.setActiveParty(playerId, List.of("P01", "P08"));
        CharacterProgression.State p01Before = CampaignProgressStore.character(playerId, "P01");
        CharacterProgression.State p08Before = CampaignProgressStore.character(playerId, "P08");
        CharacterProgression.State p03Before = CampaignProgressStore.character(playerId, "P03");

        CampaignProgressStore.commit(playerId, "ENC_M01", BattleOutcome.ALLY_VICTORY);

        int full = io.github.q93503128.turnbound.content.V04Catalogs.battleXp(
                io.github.q93503128.turnbound.content.V04Catalogs.encounter("ENC_M01"));
        assertEquals(CharacterProgression.gain(p01Before, full, 40).after(), CampaignProgressStore.character(playerId, "P01"));
        assertEquals(CharacterProgression.gain(p08Before, full, 30).after(), CampaignProgressStore.character(playerId, "P08"));
        assertEquals(CharacterProgression.gain(p03Before, (int)Math.floor(full * 0.20), 40).after(), CampaignProgressStore.character(playerId, "P03"));
    }

    /** Advance only the zero-reward tutorial recruitment bridge before adding the test-only P08 ownership. */
    private void unlockP08() {
        CampaignProgressStore.ensureNewGame(playerId);
        CampaignProgressStore.commit(playerId, "TUTORIAL_1", BattleOutcome.ALLY_VICTORY);
        CampaignProgressStore.commit(playerId, "TUTORIAL_2", BattleOutcome.ALLY_VICTORY);

        CampaignProgressStore.Snapshot old = CampaignProgressStore.snapshot(playerId);
        PlayerProfile profile = PlayerProfile.restore(old.profile());
        profile.acquireCharacter("P08");
        var characters = new java.util.LinkedHashMap<>(old.characters());
        var growth = new java.util.LinkedHashMap<>(old.growth());
        characters.put("P08", new CharacterProgression.State(1, 0));
        growth.put("P08", io.github.q93503128.turnbound.progression.CharacterGrowthRules.initial("P08"));
        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                profile.snapshot(), characters, growth, old.equipment(), old.quests(), old.activeParty(),
                new LinkedHashSet<>(old.clearedEncounters()), old.orphanedCharacterIds(), old.orphanedEquipmentIds()));
    }
}
