package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleResultSummary;

import java.util.UUID;

/** Canonical one-time campaign rewards that sit beside the generic Gold/XP/Essence settlement. */
public final class CampaignSupplementalRewardService {
    private CampaignSupplementalRewardService() {}

    public static void apply(UUID playerId, String encounterId, BattleResultSummary result) {
        if (playerId == null || encounterId == null || result == null || !result.firstClear()) return;

        int crystal = switch (encounterId) {
            // B01's existing special bundle already contains its 1,200 boss Crystal + 1,800 tutorial Crystal.
            case "BATTLE_B02", "BATTLE_B03", "BATTLE_B04", "BATTLE_B05" -> 1_200;
            default -> 0;
        };
        String choiceTier = switch (encounterId) {
            // T3 drops are explicitly unlocked by B03; B05 is the authored T4 first-clear source.
            case "BATTLE_B03", "BATTLE_B04" -> "T3";
            case "BATTLE_B05" -> "T4";
            default -> "";
        };
        if (crystal == 0 && choiceTier.isBlank()) return;

        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        PlayerProfile profile = PlayerProfile.restore(snapshot.profile());
        EquipmentInventory equipment = EquipmentInventory.restore(snapshot.equipment());
        if (crystal > 0) profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, crystal);
        if (!choiceTier.isBlank()) equipment.grantChoiceToken(choiceTier, 1);

        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                profile.snapshot(), snapshot.characters(), snapshot.growth(), equipment.snapshot(), snapshot.quests(),
                snapshot.activeParty(), snapshot.clearedEncounters(), snapshot.orphanedCharacterIds(), snapshot.orphanedEquipmentIds()));
        CampaignProgressStore.markDirty(playerId);
    }
}
