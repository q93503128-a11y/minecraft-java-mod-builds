package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Keeps the first TURNBOUND hub safe without deleting Drehmal's ambient residents.
 *
 * <p>Villagers, iron golems and other authored ambience remain. Hostile vanilla/source mobs are rejected only
 * inside the New Drabyel safety radius, while TURNBOUND battle actors are never touched.</p>
 */
public final class DrehmalHubEntityPolicy {
    private DrehmalHubEntityPolicy() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event == null || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || !DrehmalWorldBinding.isBound(level.getServer())) return;
        if (event.getEntity() instanceof BattleActorEntity) return;
        if (!(event.getEntity() instanceof Monster hostile)) return;
        if (!DrehmalFirstRouteRuntime.insideHubCoordinates(hostile.getX(), hostile.getZ())) return;
        event.setCanceled(true);
    }
}
