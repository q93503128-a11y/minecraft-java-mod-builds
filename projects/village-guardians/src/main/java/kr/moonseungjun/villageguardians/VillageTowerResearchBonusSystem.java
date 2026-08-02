package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Extra researched volleys layer on top of the normal tower attacks without replacing them. */
public final class VillageTowerResearchBonusSystem {
    private static int ticks;

    private VillageTowerResearchBonusSystem() {}

    public static void reset() { ticks = 0; }

    public static void tick(MinecraftServer server) {
        int research = VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.TOWER);
        if (research <= 0 || !VillageRaidSystem.isActive()) return;
        int interval = Math.max(20, 56 - research * 10);
        if (++ticks < interval) return;
        ticks = 0;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        int radius = VillageWorldSystem.FORTRESS_RADIUS - 4;
        int index = 0;
        for (VillageTowerSpecializationSystem.TowerKind kind : VillageTowerSpecializationSystem.TowerKind.values()) {
            if (!VillageTowerSpecializationSystem.installed(kind)
                    || VillageTowerSpecializationSystem.disabled(kind)) continue;
            BlockPos tower = switch (kind) {
                case BALLISTA -> center.offset(radius, 18, -radius);
                case FLAME -> center.offset(-radius, 18, -radius);
                case FROST -> center.offset(radius, 18, radius);
                case ARCANE -> center.offset(-radius, 18, radius);
            };
            Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 120.0);
            if (target == null) continue;
            float damage = 1.6f + research * 1.25f + VillageProgressionSystem.wallLevel() * 0.35f;
            Vec3 start = Vec3.atCenterOf(tower);
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
            for (int step = 0; step <= 8; step++) {
                Vec3 point = start.lerp(end, step / 8.0);
                level.sendParticles(index % 2 == 0 ? ParticleTypes.ENCHANT : ParticleTypes.CRIT,
                        point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
            target.hurtServer(level, level.damageSources().magic(), damage);
            index++;
        }
    }
}
