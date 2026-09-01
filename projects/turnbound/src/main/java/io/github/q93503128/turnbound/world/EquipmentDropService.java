package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.session.BattleResultSummary;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Battle equipment drop authority for rates fully specified by v0.4 §99.
 *
 * Canon fixes T3 Elite=20% and repeat regional Boss=15%, with T3 drops unlocked after B03.
 * It does not define per-item weights inside a tier, so this implementation bridge selects uniformly
 * from the six authored T3 items. The roll is derived from the durable reward transaction id so a
 * persistence retry cannot reroll the reward.
 *
 * T4 Rift 21-30 / Hard Boss repeat distribution is intentionally not rolled here because v0.4 names
 * those sources but does not author a rate or T3/T4 split for Hard repeat rewards.
 */
public final class EquipmentDropService {
    public record Drop(String itemId, String tier, String name, boolean queued) {
        public static Drop none() { return new Drop("", "", "", false); }
        public boolean present() { return !itemId.isBlank(); }
    }

    private static final List<V04Catalogs.EquipmentSpec> T3_POOL = V04Catalogs.equipment().stream()
            .filter(spec -> "T3".equals(spec.tier())).toList();

    private EquipmentDropService() {}

    public static Drop preview(UUID playerId, String transactionId, String encounterId, BattleResultSummary result) {
        if (playerId == null || transactionId == null || transactionId.isBlank()
                || encounterId == null || encounterId.isBlank() || result == null) return Drop.none();
        if (!CampaignContentUnlocks.chapter3Complete(playerId) || !V04Catalogs.hasEncounter(encounterId)) return Drop.none();

        V04Catalogs.Encounter encounter = V04Catalogs.encounter(encounterId);
        double rate = 0.0;
        if (!encounter.boss() && encounter.enemies().stream().anyMatch(id -> id.startsWith("EL"))) {
            rate = 0.20;
        } else if (encounter.boss() && !result.firstClear()) {
            rate = 0.15;
        }
        if (rate <= 0.0 || unit(transactionId, encounterId, "rate") >= rate || T3_POOL.isEmpty()) return Drop.none();

        int index = (int)Math.floor(unit(transactionId, encounterId, "item") * T3_POOL.size());
        if (index >= T3_POOL.size()) index = T3_POOL.size() - 1;
        V04Catalogs.EquipmentSpec item = T3_POOL.get(Math.max(0, index));
        EquipmentInventory.Snapshot inventory = CampaignProgressStore.equipment(playerId);
        boolean queued = inventory.items().size() >= EquipmentInventory.MAX_INSTANCES || !inventory.pendingRewards().isEmpty();
        return new Drop(item.id(), item.tier(), item.name(), queued);
    }

    public static Drop commit(UUID playerId, String transactionId, String encounterId, BattleResultSummary result) {
        Drop drop = preview(playerId, transactionId, encounterId, result);
        if (!drop.present()) return drop;
        CampaignProgressStore.grantEquipment(playerId, drop.itemId());
        return drop;
    }

    private static double unit(String transactionId, String encounterId, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((transactionId + "|" + encounterId + "|" + salt).getBytes(StandardCharsets.UTF_8));
            long raw = ByteBuffer.wrap(bytes, 0, Long.BYTES).getLong() & Long.MAX_VALUE;
            return raw / (double)Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
