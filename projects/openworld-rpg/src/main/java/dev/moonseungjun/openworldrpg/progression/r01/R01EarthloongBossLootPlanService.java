package dev.moonseungjun.openworldrpg.progression.r01;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server RNG boundary for Earthloong first-clear boss loot.
 *
 * <p>The six-family normal source roll, exact guaranteed-Superior+ grade, armor slot when the
 * Ironbound Guard family is selected, and the 15% Mythic result are persisted exactly once.
 * Older persisted plans created before the armor-slot rule closed keep their original base/grade/
 * signature result and receive only the missing armor slot once. Final generic affix/item
 * materialization remains a separate equipment-generation connection.</p>
 */
public final class R01EarthloongBossLootPlanService {
    private R01EarthloongBossLootPlanService() {
    }

    public static R01EarthloongBossLootPlanState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01EarthloongBossLootPlanAttachments.EARTHLOONG_BOSS_LOOT,
                R01EarthloongBossLootPlanState.initial()
        );
    }

    public static Optional<R01EarthloongBossLootPlanState.FirstClearPlan>
    ensureFirstClearPlan(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!R01PlayerStateService.state(player).quarry().firstClear()) {
            return Optional.empty();
        }

        R01EarthloongBossLootPlanState current = state(player);
        if (current.firstClearPlan().isPresent()) {
            var plan = current.firstClearPlan().orElseThrow();
            if (plan.requiresArmorSlotResolution()) {
                int slotIndex = player.getRandom().nextInt(
                        R01EarthloongBossLootRules.ARMOR_SLOTS.size()
                );
                plan = plan.withNormalArmorSlot(
                        R01EarthloongBossLootRules.ARMOR_SLOTS.get(slotIndex)
                );
                current = new R01EarthloongBossLootPlanState(
                        current.schemaVersion(),
                        Optional.of(plan)
                );
                player.setAttached(
                        R01EarthloongBossLootPlanAttachments.EARTHLOONG_BOSS_LOOT,
                        current
                );
            }
            return Optional.of(plan);
        }

        int normalIndex = player.getRandom().nextInt(
                R01EarthloongBossLootRules.NORMAL_BASE_POOL.size()
        );
        int superiorPlusGradeRoll = player.getRandom().nextInt(
                R01EarthloongBossLootRules.SUPERIOR_PLUS_WEIGHT_TOTAL
        );
        int armorSlotIndex = player.getRandom().nextInt(
                R01EarthloongBossLootRules.ARMOR_SLOTS.size()
        );
        int signatureRoll = player.getRandom().nextInt(100);
        int mythicIndex = player.getRandom().nextInt(
                R01EarthloongBossLootRules.MYTHIC_POOL.size()
        );

        R01EarthloongBossLootPlanState.FirstClearPlan plan =
                R01EarthloongBossLootRules.createPlan(
                        normalIndex,
                        superiorPlusGradeRoll,
                        armorSlotIndex,
                        signatureRoll,
                        mythicIndex
                );
        R01EarthloongBossLootPlanState next =
                current.withFirstClearPlan(plan);
        player.setAttached(
                R01EarthloongBossLootPlanAttachments.EARTHLOONG_BOSS_LOOT,
                next
        );
        return Optional.of(plan);
    }
}
