package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * TURNBOUND owns hostile combat in the bound Drehmal production world.
 *
 * <p>Vanilla hostile monsters are rejected across the whole bound overworld instead of only inside New Drabyel.
 * Drehmal villagers, animals, iron golems and other non-hostile authored ambience remain. A periodic local sweep
 * removes stale hostile mobs that were already stored in chunks before this policy became active.</p>
 */
public final class DrehmalHubEntityPolicy {
    private static final double SWEEP_RADIUS = 128.0D;

    private DrehmalHubEntityPolicy() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event == null || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || !DrehmalWorldBinding.isBound(level.getServer())) return;
        if (event.getEntity() instanceof BattleActorEntity) return;
        if (event.getEntity() instanceof Monster) event.setCanceled(true);
    }

    static void sweep(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || !DrehmalWorldBinding.isBound(level.getServer())) return;
        AABB area = player.getBoundingBox().inflate(SWEEP_RADIUS, 64.0D, SWEEP_RADIUS);
        for (Monster hostile : level.getEntitiesOfClass(Monster.class, area)) {
            hostile.discard();
        }
    }
}
