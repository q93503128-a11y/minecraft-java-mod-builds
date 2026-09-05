#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / "projects/frontier-settlement"
JAVA = P / "src/main/java/kr/moonseungjun/frontiersettlement"
SETTLEMENT = JAVA / "settlement"
NETWORK = JAVA / "network"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one target, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


# Version / lock.
gradle = P / "gradle.properties"
replace_once(gradle, "mod_version=0.1.0-alpha.109", "mod_version=0.1.0-alpha.110")
with gradle.open("a", encoding="utf-8") as out:
    out.write("\n# Alpha.110 construction workforce: builder population scales with construction offices and completed outposts, building/road/outpost lanes may run in parallel within infrastructure-derived slots, and concurrent sites must stay physically separated.\n")

lock_path = P / "COMPANION_LOCK.json"
lock = json.loads(read(lock_path))
lock.setdefault("target", {})["frontier_settlement"] = "0.1.0-alpha.110"
notes = lock.setdefault("notes", [])
notes.append("Alpha.110 replaces the old single shared-project lock for building/road/outpost with up to three infrastructure-derived managed lanes while civil flattening remains exclusive. Physical builders scale from a base crew of 2 by +2 per construction office and +1 per completed outpost, capped at 12. Road and outpost keep dedicated builder slots; remaining builders form the serialized building crew. Concurrent managed sites must remain at least 24 horizontal blocks apart; no force-load, teleport, virtual resource or second material authority is added.")
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

# Central project authority: up to 3 managed lanes, civil work exclusive, 24-block separation.
authority = SETTLEMENT / "SettlementProjectAuthority.java"
write(authority, '''package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/** Server-authoritative construction lane and concurrency policy. */
public final class SettlementProjectAuthority {
    public enum ProjectLane { BUILDING, ROAD, OUTPOST }

    public static final int MAX_PARALLEL_MANAGED_PROJECTS = 3;
    public static final int MIN_PARALLEL_SEPARATION = 24;
    private static final long MIN_PARALLEL_SEPARATION_SQR = (long) MIN_PARALLEL_SEPARATION * MIN_PARALLEL_SEPARATION;

    private SettlementProjectAuthority() {}

    public static int parallelProjectLimit(SettlementData data) {
        int offices = Math.max(0, data.buildingCount(BuildingType.CONSTRUCTION_OFFICE));
        int expansion = offices + data.outposts().size() / 2;
        return Math.min(MAX_PARALLEL_MANAGED_PROJECTS, 1 + Math.min(2, expansion));
    }

    public static int activeManagedProjectCount(SettlementData data) {
        int active = 0;
        if (data.construction().active()) active++;
        if (data.roadConstruction().active()) active++;
        if (data.outpostConstruction().active()) active++;
        return active;
    }

    public static boolean laneActive(SettlementData data, ProjectLane lane) {
        return switch (lane) {
            case BUILDING -> data.construction().active();
            case ROAD -> data.roadConstruction().active();
            case OUTPOST -> data.outpostConstruction().active();
        };
    }

    public static String startBlockReason(MinecraftServer server, SettlementData data, ProjectLane lane) {
        if (SettlementCivilWorkData.get(server).project().active()) {
            return "대규모 토목 평탄화가 끝난 뒤 다른 공사를 시작해 주세요.";
        }
        if (laneActive(data, lane)) {
            return switch (lane) {
                case BUILDING -> "이미 본진 건물 공사가 진행 중입니다.";
                case ROAD -> "이미 도로 공사가 진행 중입니다.";
                case OUTPOST -> "이미 전초기지 공사가 진행 중입니다.";
            };
        }
        int limit = parallelProjectLimit(data);
        if (activeManagedProjectCount(data) >= limit) {
            return "동시 공사 슬롯 " + limit + "개가 모두 사용 중입니다. 건설소나 전초기지를 늘리면 최대 3개까지 확장됩니다.";
        }
        return null;
    }

    public static boolean separatedFromOtherActive(SettlementData data, ProjectLane lane, BlockPos point) {
        if (lane != ProjectLane.BUILDING && data.construction().active()
                && horizontalDistanceSqr(point, data.construction().origin()) < MIN_PARALLEL_SEPARATION_SQR) return false;
        if (lane != ProjectLane.ROAD && data.roadConstruction().active()) {
            for (BlockPos center : data.roadConstruction().centers()) {
                if (horizontalDistanceSqr(point, center) < MIN_PARALLEL_SEPARATION_SQR) return false;
            }
        }
        if (lane != ProjectLane.OUTPOST && data.outpostConstruction().active()
                && horizontalDistanceSqr(point, data.outpostConstruction().gate()) < MIN_PARALLEL_SEPARATION_SQR) return false;
        return true;
    }

    public static boolean routeSeparatedFromOtherActive(SettlementData data, ProjectLane lane, List<BlockPos> route) {
        for (BlockPos point : route) if (!separatedFromOtherActive(data, lane, point)) return false;
        return true;
    }

    private static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public static boolean anyActive(MinecraftServer server, SettlementData data) {
        return activeManagedProjectCount(data) > 0 || SettlementCivilWorkData.get(server).project().active();
    }
}
''')

# Scalable builder population and dedicated road/outpost reservations.
construction = SETTLEMENT / "SettlementConstructionService.java"
replace_once(construction,
'''    private static final int BUILDER_ROUTE_MARGIN = 32;
    private static final int MAX_BUILDER_CREW = 3;''',
'''    private static final int BUILDER_ROUTE_MARGIN = 32;
    private static final int BASE_BUILDER_CREW = 2;
    private static final int BUILDERS_PER_CONSTRUCTION_OFFICE = 2;
    private static final int OUTPOST_BUILDER_BONUS_CAP = 6;
    private static final int MAX_BUILDER_CREW = 12;''')
replace_once(construction,
'''        if (SettlementProjectAuthority.anyActive(server, data)) {
            return invalidPlacement("현재 공동 공사가 끝난 뒤 새 건물을 배치해 주세요.");
        }
        String locked = lockedReason(data, type);''',
'''        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.BUILDING);
        if (projectBlock != null) return invalidPlacement(projectBlock);
        if (!SettlementProjectAuthority.separatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.BUILDING, selectedCenter)) {
            return invalidPlacement("동시 공사 현장은 서로 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어뜨려 주세요.");
        }
        String locked = lockedReason(data, type);''')
replace_once(construction,
'''        if (SettlementProjectAuthority.anyActive(server, data)) {
            return new StartResult(false, "현재 공동 공사가 끝난 뒤 건물을 시작해 주세요.");
        }

        PlacementCheck check = checkPlacement(player, type, selectedCenter, rotationId);''',
'''        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.BUILDING);
        if (projectBlock != null) return new StartResult(false, projectBlock);
        if (!SettlementProjectAuthority.separatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.BUILDING, selectedCenter)) {
            return new StartResult(false, "동시 공사 현장은 서로 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어뜨려 주세요.");
        }

        PlacementCheck check = checkPlacement(player, type, selectedCenter, rotationId);''')
replace_once(construction,
'''        FrontierWorkerEntity builder = ensureProjectBuilder(level, data);
        if (builder == null) {
            data.clearConstruction();''',
'''        List<FrontierWorkerEntity> builders = buildingProjectBuilders(level, data);
        if (builders.isEmpty()) {
            data.clearConstruction();''')
replace_once(construction,
'''        List<FrontierWorkerEntity> builders = ensureProjectBuilders(level, data);
        if (builders.isEmpty()) return false;''',
'''        List<FrontierWorkerEntity> builders = buildingProjectBuilders(level, data);
        if (builders.isEmpty()) return false;''')
replace_once(construction,
'''    public static int desiredBuilderCount(SettlementData data) {
        return Math.min(MAX_BUILDER_CREW, 1 + Math.max(0, data.buildingCount(BuildingType.CONSTRUCTION_OFFICE)));
    }

    public static List<FrontierWorkerEntity> ensureProjectBuilders(ServerLevel level, SettlementData data) {''',
'''    public static int desiredBuilderCount(SettlementData data) {
        int offices = Math.max(0, data.buildingCount(BuildingType.CONSTRUCTION_OFFICE));
        int outpostBonus = Math.min(OUTPOST_BUILDER_BONUS_CAP, data.outposts().size());
        return Math.min(MAX_BUILDER_CREW, BASE_BUILDER_CREW + offices * BUILDERS_PER_CONSTRUCTION_OFFICE + outpostBonus);
    }

    public static List<FrontierWorkerEntity> buildingProjectBuilders(ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> all = ensureProjectBuilders(level, data);
        if (all.isEmpty()) return List.of();
        List<FrontierWorkerEntity> crew = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (data.roadConstruction().active() && i == 0) continue;
            if (data.outpostConstruction().active() && i == 1) continue;
            crew.add(all.get(i));
        }
        return List.copyOf(crew);
    }

    public static FrontierWorkerEntity infrastructureProjectBuilder(ServerLevel level, SettlementData data,
                                                                     SettlementProjectAuthority.ProjectLane lane) {
        List<FrontierWorkerEntity> all = ensureProjectBuilders(level, data);
        int index = switch (lane) {
            case ROAD -> 0;
            case OUTPOST -> 1;
            case BUILDING -> -1;
        };
        return index >= 0 && all.size() > index ? all.get(index) : null;
    }

    public static List<FrontierWorkerEntity> ensureProjectBuilders(ServerLevel level, SettlementData data) {''')

# Road lane: own slot, parallel gate, conservative separation.
road = SETTLEMENT / "SettlementRoadService.java"
replace_once(road,
'''        if (SettlementProjectAuthority.anyActive(server, data)) {
            return invalid("현재 공동 공사가 끝난 뒤 새 도로를 계획해 주세요.");
        }
        if (data.houseCount() < 1 || data.lumberCampCount() < 1) {''',
'''        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.ROAD);
        if (projectBlock != null) return invalid(projectBlock);
        if (data.houseCount() < 1 || data.lumberCampCount() < 1) {''')
replace_once(road,
'''        RouteCandidate chosen = chooseCandidate(level, data, startXZ, endXZ);
        if (!chosen.valid()) return invalid(chosen.message().isBlank()
                ? "두 자동 경로 모두 안전한 3칸 폭 도로를 만들 수 없습니다." : chosen.message());

        if (!chosen.supports().isEmpty()''',
'''        RouteCandidate chosen = chooseCandidate(level, data, startXZ, endXZ);
        if (!chosen.valid()) return invalid(chosen.message().isBlank()
                ? "두 자동 경로 모두 안전한 3칸 폭 도로를 만들 수 없습니다." : chosen.message());
        if (!SettlementProjectAuthority.routeSeparatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.ROAD, chosen.centers())) {
            return invalid("동시 공사 도로는 다른 활성 공사 현장에서 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어져야 합니다.");
        }

        if (!chosen.supports().isEmpty()''')
replace_once(road,
'''        data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports());
        if (SettlementConstructionService.ensureProjectBuilder(level, data) == null) {''',
'''        data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports());
        if (SettlementConstructionService.infrastructureProjectBuilder(level, data, SettlementProjectAuthority.ProjectLane.ROAD) == null) {''')
replace_once(road,
'''        ServerLevel level = server.overworld();
        FrontierWorkerEntity builder = findRoadBuilder(level, data, data.centerPos(), road, plan);
        if (builder == null) return false;''',
'''        ServerLevel level = server.overworld();
        FrontierWorkerEntity builder = SettlementConstructionService.infrastructureProjectBuilder(
                level, data, SettlementProjectAuthority.ProjectLane.ROAD);
        if (builder == null) return false;''')

# Outpost lane: own slot, parallel gate, conservative separation.
outpost = SETTLEMENT / "SettlementOutpostService.java"
replace_once(outpost,
'''        if (SettlementProjectAuthority.anyActive(server, data)) {
            return PlacementCheck.invalid("현재 공동 공사가 끝난 뒤 전초기지를 배치해 주세요.");
        }

        int roadIndex = nearestUnclaimedRoad(data, selected);''',
'''        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.OUTPOST);
        if (projectBlock != null) return PlacementCheck.invalid(projectBlock);

        int roadIndex = nearestUnclaimedRoad(data, selected);''')
replace_once(outpost,
'''        BlockPos gate = gateFor(road);
        ServerLevel level = server.overworld();
        if (!assessSite(level, data, roadIndex, gate, road.directionX(), road.directionZ())) {''',
'''        BlockPos gate = gateFor(road);
        if (!SettlementProjectAuthority.separatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.OUTPOST, gate)) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(),
                    "general", "동시 공사 전초기지는 다른 활성 공사 현장에서 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어져야 합니다.");
        }
        ServerLevel level = server.overworld();
        if (!assessSite(level, data, roadIndex, gate, road.directionX(), road.directionZ())) {''')
replace_once(outpost,
'''        if (SettlementProjectAuthority.anyActive(server, data)) {
            return new StartResult(false, "현재 공동 공사가 끝난 뒤 전초기지를 시작해 주세요.");
        }
        if (roadIndex < 0 || roadIndex >= data.roads().size() || isRoadClaimed(data, roadIndex)) {''',
'''        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.OUTPOST);
        if (projectBlock != null) return new StartResult(false, projectBlock);
        if (roadIndex < 0 || roadIndex >= data.roads().size() || isRoadClaimed(data, roadIndex)) {''')
replace_once(outpost,
'''        RoadSegment road = data.roads().get(roadIndex);
        BlockPos gate = gateFor(road);
        if (player.blockPosition().distSqr(road.end())''',
'''        RoadSegment road = data.roads().get(roadIndex);
        BlockPos gate = gateFor(road);
        if (!SettlementProjectAuthority.separatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.OUTPOST, gate)) {
            return new StartResult(false, "동시 공사 전초기지는 다른 활성 공사 현장에서 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어져야 합니다.");
        }
        if (player.blockPosition().distSqr(road.end())''')
replace_once(outpost,
'''        if (SettlementConstructionService.ensureProjectBuilder(level, data) == null) {
            data.clearOutpostConstruction();''',
'''        if (SettlementConstructionService.infrastructureProjectBuilder(level, data, SettlementProjectAuthority.ProjectLane.OUTPOST) == null) {
            data.clearOutpostConstruction();''')
replace_once(outpost,
'''        ServerLevel level = server.overworld();
        FrontierWorkerEntity builder = findOutpostBuilder(level, data, data.centerPos(), state, plan);
        if (builder == null) return false;''',
'''        ServerLevel level = server.overworld();
        FrontierWorkerEntity builder = SettlementConstructionService.infrastructureProjectBuilder(
                level, data, SettlementProjectAuthority.ProjectLane.OUTPOST);
        if (builder == null) return false;''')

# Network pre-check must use the same server authority instead of the retired global single-project lock.
network = NETWORK / "SettlementNetwork.java"
replace_once(network,
'''import kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementRoadService;''',
'''import kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementProjectAuthority;
import kr.moonseungjun.frontiersettlement.settlement.SettlementRoadService;''')
replace_once(network,
'''        if(data.construction().active()||data.roadConstruction().active()||data.outpostConstruction().active()||SettlementCivilWorkData.get(player.level().getServer()).project().active()){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),false,false,0,0,0,payload.rotation(),"현재 공사가 끝난 뒤 새 건물을 배치해 주세요."));return;}''',
'''        String projectLock=SettlementProjectAuthority.startBlockReason(player.level().getServer(),data,SettlementProjectAuthority.ProjectLane.BUILDING);if(projectLock!=null){context.reply(new PlacementPreviewPayload(payload.nonce(),type.id(),false,false,0,0,0,payload.rotation(),projectLock));return;}''')

# Update construction-office documentation to match lane-based scheduling.
office = SETTLEMENT / "SettlementConstructionOfficeService.java"
replace_once(office,
''' * authorize additional physical builders (up to the centralized crew cap), while SettlementConstructionService
 * remains the sole serialized mutation/resource scheduler so multiple bodies cannot double-spend a project step.''',
''' * authorize additional physical builders (up to the centralized crew cap). Building mutation remains serialized
 * inside SettlementConstructionService, while road/outpost lanes reserve separate physical builders so parallel jobs
 * cannot issue navigation orders to the same body or double-spend one project's persisted step.''')

# Verifier.
verifier = P / "tools/test_current_source.py"
replace_once(verifier, 'require("mod_version=0.1.0-alpha.109" in gradle, "current verifier/version drift")',
                       'require("mod_version=0.1.0-alpha.110" in gradle, "current verifier/version drift")')
replace_once(verifier, 'require("MAX_BUILDER_CREW = 3" in construction, "bounded construction crew cap missing")',
'''require("MAX_BUILDER_CREW = 12" in construction, "expanded bounded construction crew cap missing")
require("BASE_BUILDER_CREW = 2" in construction and "BUILDERS_PER_CONSTRUCTION_OFFICE = 2" in construction,
        "builder workforce no longer scales from base crew through construction offices")
require("data.outposts().size()" in construction and "OUTPOST_BUILDER_BONUS_CAP = 6" in construction,
        "completed outposts no longer expand builder workforce")
require("buildingProjectBuilders" in construction and "infrastructureProjectBuilder" in construction,
        "dedicated building/road/outpost builder routing missing")''')
replace_once(verifier,
'''require("ensureProjectBuilder" in road and "clearRoadConstruction" in road, "road start is not transactional")
require("ensureProjectBuilder" in outpost and "clearOutpostConstruction" in outpost, "outpost start is not transactional")''',
'''require("infrastructureProjectBuilder" in road and "ProjectLane.ROAD" in road and "clearRoadConstruction" in road,
        "road start is not transactional through its dedicated builder lane")
require("infrastructureProjectBuilder" in outpost and "ProjectLane.OUTPOST" in outpost and "clearOutpostConstruction" in outpost,
        "outpost start is not transactional through its dedicated builder lane")
project_authority = text(SETTLEMENT / "SettlementProjectAuthority.java")
require("MAX_PARALLEL_MANAGED_PROJECTS = 3" in project_authority and "parallelProjectLimit" in project_authority,
        "managed parallel-project capacity missing")
require("MIN_PARALLEL_SEPARATION = 24" in project_authority and "routeSeparatedFromOtherActive" in project_authority,
        "parallel project physical-separation guard missing")
require("SettlementCivilWorkData.get(server).project().active()" in project_authority,
        "civil work is no longer exclusive against managed parallel projects")
require("startBlockReason" in construction and "ProjectLane.BUILDING" in construction,
        "building path bypasses centralized lane capacity")
require("startBlockReason" in road and "ProjectLane.ROAD" in road,
        "road path bypasses centralized lane capacity")
require("startBlockReason" in outpost and "ProjectLane.OUTPOST" in outpost,
        "outpost path bypasses centralized lane capacity")''')
replace_once(verifier,
'''print("CURRENT SOURCE CHECK PASS: alpha109 settlement navigation + alpha108 tree-aware placement + prior authority invariants")''',
'''print("CURRENT SOURCE CHECK PASS: alpha110 scalable parallel construction crews + alpha109 navigation + prior authority invariants")''')

print("PATCH APPLIED: Frontier alpha110 scalable builders + managed parallel construction lanes")
