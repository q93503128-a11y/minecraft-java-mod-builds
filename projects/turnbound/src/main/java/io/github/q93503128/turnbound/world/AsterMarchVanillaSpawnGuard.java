package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Prevents unrelated mobs from entering the authored Aster March RPG spaces at all.
 *
 * The field sanitizers remain as a low-frequency safety net for stale/legacy entities that already exist in a
 * world, while this guard stops fresh spawns and chunk-loaded vanilla mobs before they are added to the level.
 */
public final class AsterMarchVanillaSpawnGuard {
    private AsterMarchVanillaSpawnGuard() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event == null || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || mob instanceof BattleActorEntity) return;
        if (!insideAuthoredRegion(entity.getX(), entity.getZ())) return;

        event.setCanceled(true);
    }

    static boolean insideAuthoredRegion(double x, double z) {
        for (AsterMarchRegionCatalog.Region region : AsterMarchRegionCatalog.regions()) {
            if (region.contains(x, z)) return true;
        }
        return false;
    }
}
