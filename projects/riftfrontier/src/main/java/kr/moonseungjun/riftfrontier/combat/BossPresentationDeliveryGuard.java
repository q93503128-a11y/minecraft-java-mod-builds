package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

/**
 * Final server-side admission fence before a validated boss presentation sample can leave the
 * authoritative runtime boundary.
 *
 * <p>A {@link MinecraftBossCombatAdapter.ValidatedTickResult} is immutable and may outlive the
 * actor/world context that produced it. Delivery therefore revalidates the exact live entity
 * instance, server level, current server tick and semantic identity immediately before network
 * fan-out. This does not author presentation timing or assets; it only prevents a retained sample
 * from being applied after its authoritative Minecraft context has changed.</p>
 */
public final class BossPresentationDeliveryGuard {
    private BossPresentationDeliveryGuard() {}

    public static void requireCurrent(
        ServerLevel level,
        LivingEntity boss,
        long serverGameTick,
        MinecraftBossCombatAdapter.ValidatedTickResult result
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(boss, "boss");
        Objects.requireNonNull(result, "result");

        if (!MinecraftCombatAuthority.isEligibleBossActor(level, boss)
            || level.getEntity(boss.getUUID()) != boss) {
            throw new IllegalStateException(
                "validated boss presentation owner is no longer the exact authoritative server entity"
            );
        }
        if (level.getGameTime() != serverGameTick) {
            throw new IllegalStateException(
                "validated boss presentation sample is stale for the current authoritative server tick"
            );
        }

        BossPresentationSemanticState state = result.presentationState();
        if (state.entityId() != boss.getId()
            || !state.entityUuid().equals(boss.getUUID())
            || state.serverGameTick() != serverGameTick) {
            throw new IllegalArgumentException(
                "validated boss presentation state does not belong to this exact entity/tick"
            );
        }
    }
}
