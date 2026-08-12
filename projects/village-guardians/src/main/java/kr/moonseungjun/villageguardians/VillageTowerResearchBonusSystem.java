package kr.moonseungjun.villageguardians;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Research bonus layer for the v0.18.9 player-placed turret network.
 * The old fixed corner-tower coordinates are intentionally not used anymore.
 */
public final class VillageTowerResearchBonusSystem {
    private static int ticks;

    private VillageTowerResearchBonusSystem() {}

    public static void reset() { ticks = 0; }

    public static void tick(MinecraftServer server) {
        int research = VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.TOWER);
        if (server == null || research <= 0 || !VillageRaidSystem.isActive()) return;
        int interval = Math.max(24, 70 - research * 9);
        if (++ticks < interval) return;
        ticks = 0;
        ServerLevel level = server.overworld();
        for (VillagePlacedTurretSystem.TurretState turret : VillagePlacedTurretSystem.states()) {
            if (!turret.active()) continue;
            double range = Math.max(30.0, turret.type().range() + research * 3.0);
            Mob target = VillageRaidSystem.nearestActiveEnemy(level, turret.pos(), range);
            if (target == null) continue;
            float damage = 1.5f + research * 1.35f + turret.level() * 0.55f;
            Vec3 start = Vec3.atCenterOf(turret.pos().above());
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
            for (int step = 0; step <= 7; step++) {
                Vec3 point = start.lerp(end, step / 7.0);
                level.sendParticles(step % 2 == 0 ? ParticleTypes.ENCHANT : ParticleTypes.CRIT,
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
            target.hurtServer(level, level.damageSources().magic(), damage);
        }
    }
}
