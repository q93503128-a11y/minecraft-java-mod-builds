package dev.moonseungjun.openworldrpg.progression.r01;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server RNG boundary for Earthloong first-clear boss loot.
 *
 * <p>Once first-clear state exists, the six-family normal source roll and 15% Mythic roll are
 * persisted exactly once. Final item materialization stays gated on unresolved generic equipment
 * generation canon instead of silently inventing armor slot or Superior/Exalted distribution.</p>
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
            return current.firstClearPlan();
        }

        int normalIndex = player.getRandom().nextInt(
                R01EarthloongBossLootRules.NORMAL_BASE_POOL.size()
        );
        int signatureRoll = player.getRandom().nextInt(100);
        int mythicIndex = player.getRandom().nextInt(
                R01EarthloongBossLootRules.MYTHIC_POOL.size()
        );

        R01EarthloongBossLootPlanState.FirstClearPlan plan =
                R01EarthloongBossLootRules.createPlan(
                        normalIndex,
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
