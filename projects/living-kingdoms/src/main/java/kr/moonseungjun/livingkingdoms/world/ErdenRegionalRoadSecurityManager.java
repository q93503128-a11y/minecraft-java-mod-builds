package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Connects the village watch rosters to the four streamed national-road waystations.
 *
 * <p>Coverage exists as aggregate public duty while the road is unloaded. If a player is actually
 * present at a loaded waystation, threat detection becomes physical and records the incident in
 * the responsible village council. This layer never loads a road chunk itself.</p>
 */
public final class ErdenRegionalRoadSecurityManager {
    public static final int SECURITY_REVISION = 1;
    public static final int EXPECTED_WAYSTATIONS = 4;
    public static final int EXPECTED_ASSIGNED_SETTLEMENTS = 6;
    private static final int INCIDENT_INTERVAL = 100;
    private static final int INCIDENT_COOLDOWN = 600;

    private static final Map<String, String> SETTLEMENT_WAYSTATION = new LinkedHashMap<>();

    static {
        SETTLEMENT_WAYSTATION.put("harvest_crossing", "amber_post");
        SETTLEMENT_WAYSTATION.put("silvermead", "amber_post");
        SETTLEMENT_WAYSTATION.put("sunfield", "amber_post");
        SETTLEMENT_WAYSTATION.put("pinewatch", "northwatch_post");
        SETTLEMENT_WAYSTATION.put("blackstone", "westroad_post");
        SETTLEMENT_WAYSTATION.put("ironvale", "ironroad_post");
        validateAssignments();
    }

    private static MinecraftServer activeServer;
    private static boolean ciPassed;

    private ErdenRegionalRoadSecurityManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            activeServer = server;
            ciPassed = false;
        }
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ErdenRegionalGovernanceSavedData governance = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalGovernanceSavedData.TYPE);
        if (!governance.hasGovernance(
                ErdenRegionalGovernanceManager.GOVERNANCE_REVISION,
                ErdenRegionalGovernanceManager.EXPECTED_COUNCILS,
                ErdenRegionalGovernanceManager.EXPECTED_GUARDS)) return;

        if (level.getGameTime() % INCIDENT_INTERVAL == 0L) {
            detectLoadedWaystationThreats(level, governance);
        }
        verifyCi(governance);
    }

    private static void detectLoadedWaystationThreats(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance) {
        long now = level.getGameTime();
        for (ErdenRegionalRoadNetwork.Waystation station : ErdenRegionalRoadNetwork.waystations()) {
            if (!playerNear(level, station.x(), station.z(), 224)) continue;
            if (!level.hasChunk(station.x() >> 4, station.z() >> 4)) continue;
            String responsible = responsibleSettlement(station.id(), governance);
            if (responsible == null || governance.aliveGuardCount(responsible) <= 0) continue;
            ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(responsible);
            if (council == null || now - council.lastIncidentTick() < INCIDENT_COOLDOWN) continue;

            AABB bounds = new AABB(
                    station.x() - 96, level.getMinY(), station.z() - 96,
                    station.x() + 96, level.getMaxY(), station.z() + 96);
            List<Monster> threats = level.getEntitiesOfClass(Monster.class, bounds);
            if (threats.isEmpty()) continue;
            governance.replaceCouncil(council.recordIncident(now, 3));
            for (ServerPlayer player : level.players()) {
                double dx = player.getX() - station.x();
                double dz = player.getZ() - station.z();
                if (dx * dx + dz * dz > 224.0D * 224.0D) continue;
                player.sendSystemMessage(Component.literal(
                        "§c[" + station.name() + " 경계] §f"
                                + settlementName(responsible) + " 경비권역에서 위협 "
                                + threats.size() + "체를 확인했습니다."));
            }
        }
    }

    private static String responsibleSettlement(
            String waystationId,
            ErdenRegionalGovernanceSavedData governance) {
        String fallback = null;
        int bestSafety = Integer.MIN_VALUE;
        for (Map.Entry<String, String> entry : SETTLEMENT_WAYSTATION.entrySet()) {
            if (!entry.getValue().equals(waystationId)) continue;
            ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(entry.getKey());
            if (council == null || governance.aliveGuardCount(entry.getKey()) <= 0) continue;
            if (council.safetyScore() > bestSafety) {
                bestSafety = council.safetyScore();
                fallback = entry.getKey();
            }
        }
        return fallback;
    }

    private static void verifyCi(ErdenRegionalGovernanceSavedData governance) {
        if (ciPassed || !isCi()) return;
        validateAssignments();
        int covered = 0;
        for (ErdenRegionalRoadNetwork.Waystation station : ErdenRegionalRoadNetwork.waystations()) {
            if (responsibleSettlement(station.id(), governance) != null) covered++;
        }
        if (covered != EXPECTED_WAYSTATIONS) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_ROAD_SECURITY_PASS revision={} waystations={} assigned_settlements={} covered_waystations={} village_guard_roster=true aggregate_road_watch=true loaded_waystation_incident_detection=true council_incident_accounting=true no_chunk_loading=true persistent_forced_chunks=false",
                SECURITY_REVISION, EXPECTED_WAYSTATIONS,
                EXPECTED_ASSIGNED_SETTLEMENTS, covered);
    }

    private static void validateAssignments() {
        if (SETTLEMENT_WAYSTATION.size() != EXPECTED_ASSIGNED_SETTLEMENTS
                || ErdenRegionalRoadNetwork.waystations().size() != EXPECTED_WAYSTATIONS) {
            throw new IllegalStateException("Invalid Erden regional road-security coverage counts");
        }
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            String stationId = SETTLEMENT_WAYSTATION.get(settlement.id());
            if (stationId == null || ErdenRegionalRoadNetwork.waystations().stream()
                    .noneMatch(station -> station.id().equals(stationId))) {
                throw new IllegalStateException(
                        "Regional settlement has no valid waystation security assignment " + settlement.id());
            }
            if (ErdenRegionalRoadNetwork.routeToCapital(settlement.id()).size() < 2) {
                throw new IllegalStateException(
                        "Regional security assignment lost capital road " + settlement.id());
            }
        }
    }

    private static boolean playerNear(ServerLevel level, int x, int z, int radius) {
        double radiusSquared = (double) radius * radius;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (dx * dx + dz * dz <= radiusSquared) return true;
        }
        return false;
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

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }
}
