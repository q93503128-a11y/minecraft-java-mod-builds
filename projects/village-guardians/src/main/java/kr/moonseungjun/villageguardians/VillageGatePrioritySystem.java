package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class VillageGatePrioritySystem {
    private static final int ATTACK_INTERVAL_TICKS = 18;
    private static int attackTicks;

    private VillageGatePrioritySystem() {}

    public static void reset() {
        attackTicks = 0;
    }

    public static void tick(MinecraftServer server) {
        if (!VillageRaidSystem.isActive()
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)) {
            attackTicks = 0;
            return;
        }
        ServerLevel level = server.overworld();
        if (VillageWorldSystem.isNorthGatePassable(level)) {
            attackTicks = 0;
            return;
        }
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return;
        }

        boolean attackTick = ++attackTicks >= ATTACK_INTERVAL_TICKS;
        if (attackTick) {
            attackTicks = 0;
        }
        AABB battlefield = new AABB(center).inflate(
                VillageWorldSystem.BATTLEFIELD_RADIUS, 96, VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, battlefield)) {
            if (!mob.isAlive() || !VillageRaidSystem.isActiveEnemy(mob.getUUID())) {
                continue;
            }
            if (VillageAttackPlanSystem.frontOf(mob.getUUID()) != VillageAttackPlanSystem.Front.NORTH
                    || VillageAttackPlanSystem.isInsideFortress(mob.blockPosition())) {
                continue;
            }
            boolean raidLogicHadChosenPlayer = mob.getTarget() instanceof ServerPlayer;
            mob.setTarget(null);
            BlockPos target = VillageFortressBuildings.attackPoint(
                    center, VillageProgressionSystem.Building.WALLS, mob.blockPosition());
            mob.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.16);

            if (raidLogicHadChosenPlayer && attackTick
                    && VillageFortressBuildings.isTouchingStructure(
                    center, VillageProgressionSystem.Building.WALLS, mob.blockPosition())) {
                mob.swing(InteractionHand.MAIN_HAND);
                int damage = 8 + Math.min(22, VillageCouncilState.currentDay() * 2);
                VillageProgressionSystem.damageBuilding(
                        server, VillageProgressionSystem.Building.WALLS, damage);
            }
        }
    }
}
