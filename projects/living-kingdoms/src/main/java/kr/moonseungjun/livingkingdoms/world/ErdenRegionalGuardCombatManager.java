package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/** Loaded-chunk defensive combat for the public village guard roster. */
public final class ErdenRegionalGuardCombatManager {
    private static final int COMBAT_INTERVAL = 20;
    private static final double ACQUIRE_RADIUS = 18.0D;
    private static final double DEFEND_WHILE_OFF_DUTY_RADIUS = 8.0D;
    private static final double MELEE_RADIUS = 3.1D;
    private static final float MELEE_DAMAGE = 4.0F;

    private ErdenRegionalGuardCombatManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (level == null
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")
                || level.getGameTime() % COMBAT_INTERVAL != 0L) return;
        ErdenRegionalGovernanceSavedData governance = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalGovernanceSavedData.TYPE);
        if (!governance.hasGovernance(
                ErdenRegionalGovernanceManager.GOVERNANCE_REVISION,
                ErdenRegionalGovernanceManager.EXPECTED_COUNCILS,
                ErdenRegionalGovernanceManager.EXPECTED_GUARDS)) return;

        Map<String, ErdenRegionalGovernanceSavedData.GuardPost> names = guardNames(governance);
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            AABB villageBounds = new AABB(
                    settlement.x() - ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS - 40,
                    level.getMinY(),
                    settlement.z() - ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS - 40,
                    settlement.x() + ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40,
                    level.getMaxY(),
                    settlement.z() + ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40);
            for (Villager guard : level.getEntitiesOfClass(
                    Villager.class, villageBounds,
                    candidate -> names.containsKey(candidate.getName().getString()))) {
                ErdenRegionalGovernanceSavedData.GuardPost post = names.get(guard.getName().getString());
                if (post == null || !post.alive()) continue;
                Monster threat = nearestThreat(level, guard);
                if (threat == null) continue;
                boolean onDuty = post.slot() == 0 ? dayTime < 12_000L : dayTime >= 12_000L;
                double distanceSquared = guard.distanceToSqr(threat);
                if (!onDuty
                        && distanceSquared > DEFEND_WHILE_OFF_DUTY_RADIUS * DEFEND_WHILE_OFF_DUTY_RADIUS) {
                    continue;
                }
                if (!level.hasChunkAt(threat.blockPosition())) continue;
                if (distanceSquared <= MELEE_RADIUS * MELEE_RADIUS) {
                    guard.swing(InteractionHand.MAIN_HAND);
                    threat.hurtServer(level, level.damageSources().mobAttack(guard), MELEE_DAMAGE);
                } else {
                    guard.getNavigation().moveTo(
                            threat.getX(), threat.getY(), threat.getZ(), onDuty ? 0.72D : 0.64D);
                }
            }
        }
    }

    private static Monster nearestThreat(ServerLevel level, Villager guard) {
        AABB bounds = guard.getBoundingBox().inflate(ACQUIRE_RADIUS, 8.0D, ACQUIRE_RADIUS);
        Monster nearest = null;
        double best = ACQUIRE_RADIUS * ACQUIRE_RADIUS;
        for (Monster candidate : level.getEntitiesOfClass(
                Monster.class, bounds,
                monster -> monster.isAlive() && !monster.isRemoved())) {
            double distance = guard.distanceToSqr(candidate);
            if (distance >= best) continue;
            best = distance;
            nearest = candidate;
        }
        return nearest;
    }

    private static Map<String, ErdenRegionalGovernanceSavedData.GuardPost> guardNames(
            ErdenRegionalGovernanceSavedData governance) {
        Map<String, ErdenRegionalGovernanceSavedData.GuardPost> result = new HashMap<>();
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : governance.guardPosts()) {
            if (guard.alive()) result.put(guardName(guard), guard);
        }
        return result;
    }

    private static String guardName(ErdenRegionalGovernanceSavedData.GuardPost guard) {
        return settlementName(guard.settlementId()) + " 경비대 " + (guard.slot() + 1)
                + "조 " + guard.generation() + "기";
    }

    private static String settlementName(String id) {
        return switch (id) {
            case "harvest_crossing" -> "수확나루";
            case "silvermead" -> "은초원";
            case "sunfield" -> "해들판";
            case "pinewatch" -> "솔망루";
            case "blackstone" -> "흑석";
            case "ironvale" -> "철골짜기";
            default -> id;
        };
    }
}
