package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleResultSummary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Applies only rewards explicitly defined by the v0.4 Hard/Rift tables. */
public final class EndgameProgressService {
    private static final List<String> RESULT_PARTY = List.of("P01", "P03", "P04", "F03");

    private EndgameProgressService() {}

    public static BattleResultSummary previewVictory(UUID playerId, String encounterId) {
        if (!EndgameEncounterCatalog.contains(encounterId)) return BattleResultSummary.none();
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        boolean firstClear = !snapshot.clearedEncounters().contains(encounterId);
        int gold = rewardGold(encounterId);
        int crystal = firstClear ? firstClearCrystal(encounterId) : 0;
        int essence = firstClear ? firstClearEssence(encounterId) : 0;
        List<String> equipment = firstClear && EndgameEncounterCatalog.hardBoss(encounterId)
                ? List.of("T4 장비 선택권 ×1") : List.of();
        return new BattleResultSummary(0, gold, crystal, essence, equipment, firstClear, party(playerId));
    }

    public static BattleResultSummary commit(UUID playerId, String encounterId, BattleOutcome outcome) {
        if (outcome != BattleOutcome.ALLY_VICTORY || !EndgameEncounterCatalog.contains(encounterId)) {
            return BattleResultSummary.none();
        }
        BattleResultSummary preview = previewVictory(playerId, encounterId);
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        PlayerProfile profile = PlayerProfile.restore(snapshot.profile());
        EquipmentInventory equipment = EquipmentInventory.restore(snapshot.equipment());
        Set<String> clears = new LinkedHashSet<>(snapshot.clearedEncounters());
        boolean firstClear = clears.add(encounterId);

        if (preview.gold() > 0) profile.grant(PlayerProfile.Currency.GOLD, preview.gold());
        if (firstClear) {
            if (preview.crystal() > 0) profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, preview.crystal());
            if (preview.starEssence() > 0) profile.grant(PlayerProfile.Currency.STAR_ESSENCE, preview.starEssence());
            if (EndgameEncounterCatalog.hardBoss(encounterId)) equipment.grantChoiceToken("T4", 1);
            // v0.4 states that F10/F20/F30 add a T3/T4 choice reward, but does not assign an exact tier.
            // That ambiguous tier is intentionally left as a content-audit gap rather than fabricated here.
        }

        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                profile.snapshot(), snapshot.characters(), snapshot.growth(), equipment.snapshot(), snapshot.quests(),
                clears, snapshot.orphanedCharacterIds(), snapshot.orphanedEquipmentIds()));
        CampaignProgressStore.markDirty(playerId);
        return new BattleResultSummary(preview.xp(), preview.gold(), preview.crystal(), preview.starEssence(),
                preview.equipmentRewards(), firstClear, preview.party());
    }

    private static int rewardGold(String encounterId) {
        if (EndgameEncounterCatalog.rift(encounterId)) {
            return V04Catalogs.riftGold(EndgameEncounterCatalog.riftFloorNumber(encounterId));
        }
        String bossId = EndgameEncounterCatalog.bossId(encounterId);
        if (bossId.isBlank()) return 0;
        return V04Catalogs.battleGold(V04Catalogs.encounter("BATTLE_" + bossId));
    }

    private static int firstClearCrystal(String encounterId) {
        if (EndgameEncounterCatalog.hardBoss(encounterId)) return 600;
        if (EndgameEncounterCatalog.rift(encounterId)) return 60;
        return 0;
    }

    private static int firstClearEssence(String encounterId) {
        return EndgameEncounterCatalog.rift(encounterId) ? 25 : 0;
    }

    private static List<BattleResultSummary.PartyXp> party(UUID playerId) {
        ArrayList<BattleResultSummary.PartyXp> out = new ArrayList<>();
        for (String characterId : RESULT_PARTY) {
            if (!CampaignProgressStore.ownedCharacters(playerId).contains(characterId)) continue;
            CharacterProgression.State state = CampaignProgressStore.character(playerId, characterId);
            out.add(new BattleResultSummary.PartyXp(characterId, CanonicalData.definition(characterId).name(),
                    state.level(), state.xp(), state.level(), state.xp(), CharacterProgression.xpToNext(state.level())));
        }
        return List.copyOf(out);
    }
}
