package io.github.q93503128.turnbound.world;

import java.util.UUID;

/** Canonical v0.4 chapter gates used by Radia facilities and server-authoritative menu actions. */
public final class CampaignContentUnlocks {
    private CampaignContentUnlocks() {}

    public static boolean prologueComplete(UUID playerId) { return completed(playerId, "MQ_P00_03_south_gate"); }
    public static boolean chapter1Complete(UUID playerId) { return completed(playerId, "MQ_C01_03_graul") || cleared(playerId, "BATTLE_B01"); }
    public static boolean chapter2Complete(UUID playerId) { return completed(playerId, "MQ_C02_03_verna") || cleared(playerId, "BATTLE_B02"); }
    public static boolean chapter3Complete(UUID playerId) { return completed(playerId, "MQ_C03_03_oro7") || cleared(playerId, "BATTLE_B03"); }
    public static boolean chapter4Complete(UUID playerId) { return completed(playerId, "MQ_C04_03_kolvak") || cleared(playerId, "BATTLE_B04"); }
    public static boolean storyComplete(UUID playerId) { return completed(playerId, "MQ_C05_03_reconnect") || flag(playerId, "ENDGAME"); }

    /** Chapter 1 clear unlocks Echo Archive summoning. */
    public static boolean archive(UUID playerId) { return chapter1Complete(playerId) || flag(playerId, "ARCHIVE"); }

    /** Chapter 1 clear unlocks Gold-only equipment enhancement. */
    public static boolean forge(UUID playerId) { return chapter1Complete(playerId); }

    /** Chapter 2 clear unlocks accessory equipment. */
    public static boolean accessory(UUID playerId) { return chapter2Complete(playerId) || flag(playerId, "ACCESSORY_SLOT"); }

    /** Chapter 2 exposes the first Character Quest layer; P08 is explicitly previewed in Chapter 4. */
    public static boolean characterQuestStageOne(UUID playerId) { return chapter2Complete(playerId); }
    public static boolean p08CharacterQuest(UUID playerId) { return chapter4Complete(playerId); }

    /** Chapter 4 exposes preview UI only; actual Signature/Awakening progression is post-B05. */
    public static boolean awakeningPreview(UUID playerId) { return chapter4Complete(playerId) || flag(playerId, "AWAKENING"); }
    public static boolean signaturePreview(UUID playerId) { return chapter4Complete(playerId) || flag(playerId, "SIGNATURE_PREVIEW"); }

    /** Chapter 5 reconnect unlocks the physical Rift Gate and actual Signature/Awakening endgame. */
    public static boolean endgame(UUID playerId) { return storyComplete(playerId) || flag(playerId, "RIFT_GATE"); }
    public static boolean signatureActual(UUID playerId) { return storyComplete(playerId) || flag(playerId, "SIGNATURE_TRIALS"); }

    private static boolean completed(UUID playerId, String questId) {
        return CampaignProgressStore.snapshot(playerId).quests().completed().contains(questId);
    }

    private static boolean cleared(UUID playerId, String encounterId) {
        return CampaignProgressStore.snapshot(playerId).clearedEncounters().contains(encounterId);
    }

    private static boolean flag(UUID playerId, String flag) {
        return CampaignProgressStore.snapshot(playerId).quests().unlockFlags().contains(flag);
    }
}
