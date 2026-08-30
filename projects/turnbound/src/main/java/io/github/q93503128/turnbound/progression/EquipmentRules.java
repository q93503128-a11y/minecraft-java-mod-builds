package io.github.q93503128.turnbound.progression;

import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.content.V04Catalogs;

/** Canonical v0.4 equipment economy and reference CP calculations. */
public final class EquipmentRules {
    private EquipmentRules() {}

    public static int shopPrice(String tier) {
        return switch (tier) {
            case "T1" -> 4_000;
            case "T2" -> 12_000;
            default -> throw new IllegalArgumentException("Only T1/T2 are sold in the normal v0.4 shop");
        };
    }

    public static EquipmentInventory.Item buyNormal(
            EquipmentInventory inventory,
            PlayerProfile profile,
            String itemId,
            int chapter) {
        V04Catalogs.EquipmentSpec spec = V04Catalogs.equipment(itemId);
        int unlockChapter = switch (spec.tier()) {
            case "T1" -> 1;
            case "T2" -> 2;
            default -> throw new IllegalArgumentException("T3/T4 are not normal-shop equipment");
        };
        if (chapter < unlockChapter) throw new IllegalStateException(spec.tier() + " shop inventory is not unlocked");
        if (!inventory.hasFreeSlot()) throw new IllegalStateException("Equipment inventory is full (300/300)");
        int cost = shopPrice(spec.tier());
        if (!profile.spend(PlayerProfile.Currency.GOLD, cost)) throw new IllegalStateException("Not enough Gold");
        return inventory.grant(itemId);
    }

    public static int combatPower(BattleStats finalStats) {
        return (int)Math.round(finalStats.maxHp() * 0.25
                + finalStats.attack() * 8.0
                + finalStats.defense() * 5.0
                + finalStats.speed() * 6.0);
    }
}
