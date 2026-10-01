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
 * Server-authoritative deterministic equipment drops.
 *
 * <p>Drehmal open-world progression uses T1/T2 drops before the legacy B03 T3 gate so repeatable field content
 * can actually improve equipment while Gold is spent at the forge. Legacy T3 rules remain unchanged after B03.
 * Every roll is derived from the durable reward transaction id, so save retries cannot reroll equipment.</p>
 */
public final class EquipmentDropService {
    public record Drop(String itemId, String tier, String name, boolean queued) {
        public static Drop none() { return new Drop("", "", "", false); }
        public boolean present() { return !itemId.isBlank(); }
    }

    private record DropRule(String tier, double rate) {}

    private static final List<V04Catalogs.EquipmentSpec> T1_POOL = pool("T1");
    private static final List<V04Catalogs.EquipmentSpec> T2_POOL = pool("T2");
    private static final List<V04Catalogs.EquipmentSpec> T3_POOL = pool("T3");

    private EquipmentDropService() {}

    public static Drop preview(UUID playerId, String transactionId, String encounterId, BattleResultSummary result) {
        if (playerId == null || transactionId == null || transactionId.isBlank()
                || encounterId == null || encounterId.isBlank() || result == null) return Drop.none();
        if (!V04Catalogs.hasEncounter(encounterId)) return Drop.none();

        V04Catalogs.Encounter encounter = V04Catalogs.encounter(encounterId);
        DropRule early = openworldRule(encounterId, result.firstClear());
        if (early != null) return roll(playerId, transactionId, encounterId, early);

        if (!CampaignContentUnlocks.chapter3Complete(playerId)) return Drop.none();
        double rate = 0.0D;
        if (!encounter.boss() && encounter.enemies().stream().anyMatch(id -> id.startsWith("EL"))) {
            rate = 0.20D;
        } else if (encounter.boss() && !result.firstClear()) {
            rate = 0.15D;
        }
        return rate <= 0.0D ? Drop.none()
                : roll(playerId, transactionId, encounterId, new DropRule("T3", rate));
    }

    public static Drop commit(UUID playerId, String transactionId, String encounterId, BattleResultSummary result) {
        Drop drop = preview(playerId, transactionId, encounterId, result);
        if (!drop.present()) return drop;
        CampaignProgressStore.grantEquipment(playerId, drop.itemId());
        return drop;
    }

    private static DropRule openworldRule(String encounterId, boolean firstClear) {
        return switch (encounterId) {
            case "CV_FIRST_COMMON", "CV_TEMPLE_WILDLIFE", "CV_TOWER_ROAD", "CV_CAMP_WILDLIFE",
                    "CV_DRABYEL_NORTH", "CV_DRABYEL_ROAD", "CV_HOUND_ROAM", "CV_SPORE_GROVE" ->
                    new DropRule("T1", firstClear ? 0.45D : 0.18D);
            case "CV_WARNING_CAVE_ELITE" ->
                    new DropRule("T2", firstClear ? 0.20D : 0.20D);
            case "CV_BRIAR_STAG" ->
                    new DropRule("T2", firstClear ? 0.35D : 0.25D);
            case "AV_ROAD_HOUNDS", "AV_ROAD_PATROL", "AV_RELAY_SENTRIES" ->
                    new DropRule("T2", firstClear ? 0.55D : 0.22D);
            case "AV_ROAD_ELITE" ->
                    new DropRule("T2", firstClear ? 0.25D : 0.30D);
            default -> null;
        };
    }

    private static Drop roll(UUID playerId, String transactionId, String encounterId, DropRule rule) {
        List<V04Catalogs.EquipmentSpec> pool = switch (rule.tier()) {
            case "T1" -> T1_POOL;
            case "T2" -> T2_POOL;
            case "T3" -> T3_POOL;
            default -> List.of();
        };
        if (pool.isEmpty() || unit(transactionId, encounterId, "rate:" + rule.tier()) >= rule.rate()) {
            return Drop.none();
        }

        int index = (int)Math.floor(unit(transactionId, encounterId, "item:" + rule.tier()) * pool.size());
        if (index >= pool.size()) index = pool.size() - 1;
        V04Catalogs.EquipmentSpec item = pool.get(Math.max(0, index));
        EquipmentInventory.Snapshot inventory = CampaignProgressStore.equipment(playerId);
        boolean queued = inventory.items().size() >= EquipmentInventory.MAX_INSTANCES
                || !inventory.pendingRewards().isEmpty();
        return new Drop(item.id(), item.tier(), item.name(), queued);
    }

    private static List<V04Catalogs.EquipmentSpec> pool(String tier) {
        return V04Catalogs.equipment().stream().filter(spec -> tier.equals(spec.tier())).toList();
    }

    private static double unit(String transactionId, String encounterId, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((transactionId + "|" + encounterId + "|" + salt)
                    .getBytes(StandardCharsets.UTF_8));
            long raw = ByteBuffer.wrap(bytes, 0, Long.BYTES).getLong() & Long.MAX_VALUE;
            return raw / (double)Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
