package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Clears pre-existing natural mobs that were loaded before strict join suppression. */
public final class VillageGlobalMobPurgeSystem {
    private VillageGlobalMobPurgeSystem() {}

    public static void purge(MinecraftServer server) {
        if (server == null) return;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        ServerLevel level = server.overworld();
        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 96.0;
        AABB loadedBattleWorld = new AABB(center).inflate(radius, 128, radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, loadedBattleWorld)) {
            if (VillageWorldSystem.isAllowedGameMob(mob)) continue;
            if (VillageMercenarySystem.recognize(mob)) continue;
            if (VillageDefenseSystem.recognizeDefenseMob(mob)) continue;
            if (!mob.isPersistenceRequired()) mob.discard();
        }
    }
}
