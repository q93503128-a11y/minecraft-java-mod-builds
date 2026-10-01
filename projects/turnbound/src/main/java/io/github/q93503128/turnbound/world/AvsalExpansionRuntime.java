package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Live-world binding for MQ_AV01: New Drabyel -> Av'Sal outskirts. */
final class AvsalExpansionRuntime {
    static final String ROAD_EVENT_SITE = "turnbound:site/avsal/roadside_echo";
    static final String ROAD_PATROL_SITE = "turnbound:site/avsal/road_patrol";
    static final String ROAD_ELITE_SITE = "turnbound:site/avsal/road_elite";
    static final String ROAD_HOUNDS_SITE = "turnbound:site/avsal/road_hounds";
    static final String ROAD_COURIER_SITE = "turnbound:site/avsal/road_courier";
    static final String WAYSIDE_CACHE_SITE = "turnbound:site/avsal/wayside_cache";
    static final String RELAY_SENTRIES_SITE = "turnbound:site/avsal/relay_sentries";
    static final String OUTSKIRTS_SITE = "turnbound:site/avsal/outskirts";
    static final String SCAVENGER_CLUE_SITE = "turnbound:site/avsal/scavenger_contact";
    static final String SURVIVOR_CLUE_SITE = "turnbound:site/avsal/survivor_shelter";
    static final String RECORDS_CLUE_SITE = "turnbound:site/avsal/weathered_records";
    static final String RELAY_WEST_SITE = "turnbound:site/avsal/relay_west";
    static final String RELAY_GUARD_SITE = "turnbound:site/avsal/relay_guard";
    static final String RELAY_EAST_SITE = "turnbound:site/avsal/relay_east";
    static final String FIRST_BOSS_SITE = "turnbound:site/avsal/north_gate_boss";

    private static final Map<ServerLevel, Snapshot> CACHE = new IdentityHashMap<>();

    record Snapshot(
            Map<String, DrehmalFirstRouteCatalog.Site> sites,
            Map<String, DrehmalFirstRouteCatalog.Footprint> footprints,
            Map<String, DrehmalFirstRouteCatalog.Patrol> patrols,
            List<DrehmalFirstRouteCatalog.EncounterSlot> encounters
    ) {
        Snapshot {
            sites = Map.copyOf(sites);
            footprints = Map.copyOf(footprints);
            patrols = Map.copyOf(patrols);
            encounters = List.copyOf(encounters);
        }
    }

    private AvsalExpansionRuntime() {}

    static boolean shouldGuideBriefing(ServerPlayer player) {
        if (player == null) return false;
        var server = player.level().getServer();
        if (server == null) return false;
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        return AvsalExpansionProgress.briefingReady(clears, flags);
    }

    static boolean active(ServerPlayer player) {
        if (player == null) return false;
        var server = player.level().getServer();
        return server != null && AvsalExpansionProgress.briefed(
                ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID()));
    }

    static String storyDialogue(ServerPlayer player) {
        if (player == null) return "서쪽 길 너머의 폐허에서 이상한 울림이 다시 들린다는 소문이 있습니다.";
        var server = player.level().getServer();
        if (server == null) return "서쪽 길 너머의 폐허에서 이상한 울림이 다시 들린다는 소문이 있습니다.";
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        if (!DrabyelOpeningTutorial.patrolCleared(clears)) {
            return "북쪽 길이 아직 어수선합니다. 먼저 마을 입구의 순찰 문제부터 정리하는 편이 좋겠습니다.";
        }
        if (!flags.contains(DrehmalContextualOnboarding.HUB_ROUTE_REVIEWED)) {
            return "서쪽 길로 나가기 전 지도를 확인해 두세요. 아브살로 향하는 옛 가도는 여러 번 꺾여 있습니다.";
        }
        boolean first = AvsalExpansionProgress.mark(player, AvsalExpansionProgress.BRIEFED);
        return first
                ? "아브살 쪽 폐허에서 짧은 울림이 반복된다는 이야기가 들어왔습니다. 오래된 도시의 사정과 별개로 최근 생긴 현상 같습니다. 서쪽 가도를 따라 외곽까지 가서 흔적만 확인해 주세요."
                : "아브살 외곽까지는 서쪽 가도를 따라가면 됩니다. 폐허 안쪽으로 성급히 들어가지 말고 먼저 외곽의 흔적부터 확인하세요.";
    }

    static DrehmalContextualOnboarding.Guidance guidance(ServerPlayer player) {
        if (player == null) return null;
        if (shouldGuideBriefing(player)) {
            return new DrehmalContextualOnboarding.Guidance(
                    "뉴 드라비엘 입구의 아렌에게 먼 서쪽 길의 소식을 확인하십시오.",
                    "지도와 지역 준비가 끝났다면 입구 안내원이 아브살 원정 의뢰를 이어 줍니다.");
        }
        if (!active(player)) return null;

        var server = player.level().getServer();
        if (server == null) return null;
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        return switch (AvsalExpansionProgress.stage(flags, clears)) {
            case INVESTIGATE -> new DrehmalContextualOnboarding.Guidance(
                    "아브살 외곽의 조사 지점 3곳 중 2곳을 확인하십시오. (" + AvsalExpansionProgress.investigationCount(flags) + "/2)",
                    "지도에 표시된 폐품상, 생존자, 낡은 기록 중 원하는 순서로 두 곳만 확인하면 됩니다.");
            case RELAYS -> new DrehmalContextualOnboarding.Guidance(
                    "아브살 안쪽 중계 흔적 3곳 중 2곳을 해결하십시오. (" + AvsalExpansionProgress.relayCount(flags) + "/2)",
                    "서부 배전실, 수로 중계기 파수조, 동부 우회선 중 원하는 두 곳만 해결하면 됩니다.");
            case BOSS -> new DrehmalContextualOnboarding.Guidance(
                    "북쪽 수로문으로 이동해 수로 집행기 카르논을 격파하십시오.",
                    "카르논은 Barrier가 남아 있을 때 직접 공격을 반격합니다. 장벽을 끊고 예고된 수문 충돌에 대비하십시오.");
            case CLEARED -> new DrehmalContextualOnboarding.Guidance(
                    "북쪽 수로문이 열렸습니다. 아브살 안쪽을 더 조사할 수 있습니다.",
                    "외곽 의뢰 중개소와 발견한 이동 지점을 이용해 정비할 수 있습니다.");
            case ROAD_EVENT -> new DrehmalContextualOnboarding.Guidance(
                    "뉴 드라비엘 서쪽 가도를 따라 아브살 외곽으로 향하십시오.",
                    "길에서 이상한 흔적을 발견해도 모든 것을 조사할 필요는 없습니다.");
            case COURIER -> new DrehmalContextualOnboarding.Guidance(
                    "서쪽 가도의 운송인 데른을 찾아 최근 길 상황을 확인하십시오.",
                    "데른은 가도 한가운데의 물류 지점에 있습니다. 그의 요청 중 전투 의뢰는 선택입니다.");
            case ROAD_PATROL -> new DrehmalContextualOnboarding.Guidance(
                    "끊긴 가도를 따라 폐허 방향으로 계속 이동하십시오.",
                    "앞쪽에서 움직이는 순찰이 보입니다. 먼저 보고 전투하거나 지나갈 길을 살필 수 있습니다.");
            case OUTSKIRTS -> new DrehmalContextualOnboarding.Guidance(
                    clears.contains("AV_ROAD_PATROL")
                            ? "순찰이 약해진 틈에 아브살 외곽까지 이동하십시오."
                            : "순찰대를 돌파하거나 우회해 아브살 외곽까지 이동하십시오.",
                    "길 바깥의 강한 적은 선택입니다. 메인 진행을 위해 반드시 싸울 필요는 없습니다.");
            default -> new DrehmalContextualOnboarding.Guidance(
                    "마을 중심에서 아브살로 향하는 길의 소식을 확인하십시오.",
                    "서쪽 가도에 들어가기 전 준비를 마치십시오.");
        };
    }

    static FieldUiSnapshot.Navigation navigation(ServerPlayer player) {
        if (player == null) return FieldUiSnapshot.Navigation.none();
        if (shouldGuideBriefing(player)) {
            var greeter = DrabyelHubServiceRuntime.serviceByRole(player, "GREETER");
            if (greeter != null && greeter.runtimePosition() != null) {
                var pos = greeter.runtimePosition();
                return new FieldUiSnapshot.Navigation(greeter.locator(), greeter.playerLabel(), pos.x() + 0.5D, pos.z() + 0.5D);
            }
            return FieldUiSnapshot.Navigation.none();
        }
        if (!active(player)) return FieldUiSnapshot.Navigation.none();

        var server = player.level().getServer();
        if (server == null) return FieldUiSnapshot.Navigation.none();
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        if (flags.contains(AvsalExpansionProgress.OUTSKIRTS_REACHED)) {
            if (!AvsalExpansionProgress.investigationComplete(flags)) return investigationNavigation(player, flags);
            if (!AvsalExpansionProgress.relayComplete(flags)) return relayNavigation(player, flags, clears);
            if (!clears.contains("AV_FIRST_BOSS")) {
                DrehmalFirstRouteCatalog.Site boss = site(player, FIRST_BOSS_SITE);
                if (boss != null && boss.runtimePosition() != null) {
                    var pos = boss.runtimePosition();
                    return new FieldUiSnapshot.Navigation(boss.locator(), boss.playerLabel(), pos.x() + 0.5D, pos.z() + 0.5D);
                }
            }
            return FieldUiSnapshot.Navigation.none();
        }

        String target = !flags.contains(AvsalExpansionProgress.ROADSIDE_ECHO_SEEN)
                ? ROAD_EVENT_SITE
                : !flags.contains("AVSAL_ROAD_COURIER_OFFERED")
                ? ROAD_COURIER_SITE
                : !flags.contains(AvsalExpansionProgress.ROAD_PATROL_SEEN)
                ? ROAD_PATROL_SITE
                : OUTSKIRTS_SITE;
        DrehmalFirstRouteCatalog.Site site = site(player, target);
        if (site == null || site.runtimePosition() == null) return FieldUiSnapshot.Navigation.none();
        var pos = site.runtimePosition();
        return new FieldUiSnapshot.Navigation(site.locator(), site.playerLabel(), pos.x() + 0.5D, pos.z() + 0.5D);
    }

    static void recordProgress(ServerPlayer player) {
        if (!active(player) || !(player.level() instanceof ServerLevel)) return;
        Snapshot snapshot = snapshot(player);
        var server = player.level().getServer();
        if (server == null) return;
        Set<String> flags = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());

        if (!flags.contains(AvsalExpansionProgress.ROADSIDE_ECHO_SEEN)
                && inside(player, AvsalExpansionCatalog.site(ROAD_EVENT_SITE), snapshot.sites().get(ROAD_EVENT_SITE))) {
            if (AvsalExpansionProgress.mark(player, AvsalExpansionProgress.ROADSIDE_ECHO_SEEN)) {
                FieldNetwork.showDialogue(player, "길가의 흔적",
                        "부서진 금속 파편에서 짧은 울림이 반복됩니다. 폐허 쪽에서 이어지는 신호와 닮았지만, 오래된 도시의 기록과 같은 것인지는 알 수 없습니다.");
            }
        }
        if (!flags.contains(AvsalExpansionProgress.ROAD_PATROL_SEEN)
                && inside(player, AvsalExpansionCatalog.site(ROAD_PATROL_SITE), snapshot.sites().get(ROAD_PATROL_SITE))) {
            AvsalExpansionProgress.mark(player, AvsalExpansionProgress.ROAD_PATROL_SEEN);
        }
        if (!flags.contains(AvsalExpansionProgress.WAYSIDE_CACHE_SEEN)
                && inside(player, AvsalExpansionCatalog.site(WAYSIDE_CACHE_SITE), snapshot.sites().get(WAYSIDE_CACHE_SITE))) {
            if (AvsalExpansionProgress.mark(player, AvsalExpansionProgress.WAYSIDE_CACHE_SEEN)) {
                FieldNetwork.showDialogue(player, "뒤집힌 운송수레",
                        "찢어진 포장과 부서진 금속 고리가 남아 있습니다. 오래된 폐허의 물건이라기보다 최근 운송 중 버려진 짐에 가깝습니다.");
            }
        }
        if (!flags.contains(AvsalExpansionProgress.OUTSKIRTS_REACHED)
                && inside(player, AvsalExpansionCatalog.site(OUTSKIRTS_SITE), snapshot.sites().get(OUTSKIRTS_SITE))) {
            if (AvsalExpansionProgress.mark(player, AvsalExpansionProgress.OUTSKIRTS_REACHED)) {
                FieldNetwork.showDialogue(player, "아브살 외곽",
                        "무너진 외곽과 사람이 드나든 흔적이 함께 보입니다. 지도에 표시되는 세 조사점 중 두 곳을 확인하면 충분합니다.");
            }
        }

        Set<String> refreshed = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        if (refreshed.contains(AvsalExpansionProgress.OUTSKIRTS_REACHED)
                && !refreshed.contains(AvsalExpansionProgress.REGION_DISCOVERED)) {
            AvsalExpansionProgress.mark(player, AvsalExpansionProgress.REGION_DISCOVERED);
            refreshed = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        }
        if (refreshed.contains(AvsalExpansionProgress.OUTSKIRTS_REACHED)
                && !refreshed.contains(AvsalExpansionProgress.CLUE_RECORDS)
                && inside(player, AvsalExpansionCatalog.site(RECORDS_CLUE_SITE), snapshot.sites().get(RECORDS_CLUE_SITE))) {
            if (AvsalExpansionProgress.mark(player, AvsalExpansionProgress.CLUE_RECORDS)) {
                boolean completed = AvsalExpansionProgress.reconcileInvestigation(player);
                FieldNetwork.showDialogue(player, "비에 젖은 장부",
                        "최근 날짜가 적힌 거래 흔적과 폐허 안쪽으로 옮겨진 물품 목록이 남아 있습니다. 이곳이 완전히 버려진 도시는 아닙니다."
                                + (completed ? "\n\n서로 다른 흔적 두 개가 같은 방향을 가리킵니다. 외곽 상황은 충분히 파악했습니다." : ""));
            }
        }
        AvsalExpansionProgress.reconcileInvestigation(player);

        refreshed = ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        if (AvsalExpansionProgress.investigationComplete(refreshed)) {
            if (!refreshed.contains(AvsalExpansionProgress.RELAY_WEST)
                    && inside(player, AvsalExpansionCatalog.site(RELAY_WEST_SITE), snapshot.sites().get(RELAY_WEST_SITE))) {
                if (AvsalExpansionProgress.mark(player, AvsalExpansionProgress.RELAY_WEST)) {
                    FieldNetwork.showDialogue(player, "서부 배전실",
                            "끊어진 배선이 바닥의 오래된 홈과 맞물립니다. 남은 선을 우회 연결하자 짧은 진동이 북쪽으로 이어집니다.");
                }
            }
            if (!refreshed.contains(AvsalExpansionProgress.RELAY_EAST)
                    && inside(player, AvsalExpansionCatalog.site(RELAY_EAST_SITE), snapshot.sites().get(RELAY_EAST_SITE))) {
                if (AvsalExpansionProgress.mark(player, AvsalExpansionProgress.RELAY_EAST)) {
                    FieldNetwork.showDialogue(player, "동부 중계기",
                            "무너진 중계기를 직접 살리는 대신 남은 선을 우회시켰습니다. 신호 일부가 북쪽 수로문 쪽으로 다시 모입니다.");
                }
            }
            Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
            if (clears.contains("AV_RELAY_GUARD") && !refreshed.contains(AvsalExpansionProgress.RELAY_GUARD)) {
                AvsalExpansionProgress.mark(player, AvsalExpansionProgress.RELAY_GUARD);
            }
            if (AvsalExpansionProgress.reconcileRelay(player)) {
                FieldNetwork.showDialogue(player, "끊어진 선",
                        "두 개의 중계선이 다시 이어졌습니다. 북쪽 수로문의 잠금이 풀리며 거대한 집행기가 움직이기 시작합니다.");
            }
        }
    }

    private static FieldUiSnapshot.Navigation investigationNavigation(ServerPlayer player, Set<String> flags) {
        List<String> candidates = new ArrayList<>();
        if (!flags.contains(AvsalExpansionProgress.CLUE_SCAVENGER)) candidates.add(SCAVENGER_CLUE_SITE);
        if (!flags.contains(AvsalExpansionProgress.CLUE_SURVIVOR)) candidates.add(SURVIVOR_CLUE_SITE);
        if (!flags.contains(AvsalExpansionProgress.CLUE_RECORDS)) candidates.add(RECORDS_CLUE_SITE);
        DrehmalFirstRouteCatalog.Site best = null;
        double bestDistance = Double.MAX_VALUE;
        for (String locator : candidates) {
            DrehmalFirstRouteCatalog.Site candidate = site(player, locator);
            if (candidate == null || candidate.runtimePosition() == null) continue;
            var pos = candidate.runtimePosition();
            double dx = player.getX() - (pos.x() + 0.5D);
            double dz = player.getZ() - (pos.z() + 0.5D);
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        if (best == null) return FieldUiSnapshot.Navigation.none();
        var pos = best.runtimePosition();
        return new FieldUiSnapshot.Navigation(best.locator(), best.playerLabel(), pos.x() + 0.5D, pos.z() + 0.5D);
    }

    private static FieldUiSnapshot.Navigation relayNavigation(ServerPlayer player, Set<String> flags, Set<String> clears) {
        List<String> candidates = new ArrayList<>();
        if (!flags.contains(AvsalExpansionProgress.RELAY_WEST)) candidates.add(RELAY_WEST_SITE);
        if (!flags.contains(AvsalExpansionProgress.RELAY_GUARD) && !clears.contains("AV_RELAY_GUARD")) candidates.add(RELAY_GUARD_SITE);
        if (!flags.contains(AvsalExpansionProgress.RELAY_EAST)) candidates.add(RELAY_EAST_SITE);
        DrehmalFirstRouteCatalog.Site best = null;
        double bestDistance = Double.MAX_VALUE;
        for (String locator : candidates) {
            DrehmalFirstRouteCatalog.Site candidate = site(player, locator);
            if (candidate == null || candidate.runtimePosition() == null) continue;
            var pos = candidate.runtimePosition();
            double dx = player.getX() - (pos.x() + 0.5D);
            double dz = player.getZ() - (pos.z() + 0.5D);
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) { bestDistance = distance; best = candidate; }
        }
        if (best == null) return FieldUiSnapshot.Navigation.none();
        var pos = best.runtimePosition();
        return new FieldUiSnapshot.Navigation(best.locator(), best.playerLabel(), pos.x() + 0.5D, pos.z() + 0.5D);
    }

    static DrehmalFirstRouteCatalog.Site locationSite(ServerPlayer player) {
        if (!active(player)) return null;
        return DrehmalLocationBannerRules.current(productionSites(player), player.getX(), player.getZ());
    }

    static List<DrehmalFirstRouteCatalog.Site> productionSites(ServerPlayer player) {
        return active(player) ? List.copyOf(snapshot(player).sites().values()) : List.of();
    }

    static List<DrehmalFirstRouteCatalog.EncounterSlot> productionEncounters(ServerPlayer player) {
        if (!active(player)) return List.of();
        return snapshot(player).encounters().stream()
                .filter(slot -> encounterAvailable(player, slot.combatEncounterId()))
                .toList();
    }

    static boolean encounterAvailable(ServerPlayer player, String combatEncounterId) {
        if (!active(player) || player.level().getServer() == null || combatEncounterId == null) return false;
        Set<String> flags = ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        if ("AV_RELAY_GUARD".equals(combatEncounterId)) return AvsalExpansionProgress.investigationComplete(flags);
        if ("AV_FIRST_BOSS".equals(combatEncounterId)) {
            return AvsalExpansionProgress.relayComplete(flags) && !clears.contains("AV_FIRST_BOSS");
        }
        return true;
    }

    static DrehmalFirstRouteCatalog.Site site(ServerPlayer player, String locator) {
        return active(player) ? snapshot(player).sites().get(locator) : null;
    }

    static DrehmalFirstRouteCatalog.Footprint footprint(ServerPlayer player, String locator) {
        return active(player) ? snapshot(player).footprints().get(locator) : null;
    }

    static DrehmalFirstRouteCatalog.Patrol patrol(ServerPlayer player, String locator) {
        return active(player) ? snapshot(player).patrols().get(locator) : null;
    }

    static DrehmalFirstRouteCatalog.EncounterSlot encounterByCombatId(ServerLevel level, String combatEncounterId) {
        Snapshot snapshot = CACHE.get(level);
        if (snapshot == null || combatEncounterId == null) return null;
        for (var encounter : snapshot.encounters()) if (combatEncounterId.equals(encounter.combatEncounterId())) return encounter;
        return null;
    }

    static boolean ownsSite(String locator) { return AvsalExpansionCatalog.site(locator) != null; }

    static DrehmalFirstRouteCatalog.Position visibleActorPosition(ServerLevel level, DrehmalFirstRouteCatalog.Site site) {
        if (level == null || site == null || site.runtimePosition() == null) return null;
        var home = site.runtimePosition();
        for (int[] offset : offsets(10)) {
            int x = home.x() + offset[0];
            int z = home.z() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!standing(level, feet) || !DrehmalAdaptiveRoutePlacement.fieldProxyContentClear(level, x, y, z)) continue;
            return new DrehmalFirstRouteCatalog.Position(x, y, z);
        }
        return null;
    }

    static boolean insideSafetyZone(ServerLevel level, double x, double z) {
        Snapshot snapshot = CACHE.get(level);
        return snapshot != null && DrehmalRouteZoneRules.insideSafetyZone(List.copyOf(snapshot.sites().values()), x, z);
    }

    static void clear() { CACHE.clear(); }

    private static synchronized Snapshot snapshot(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return new Snapshot(Map.of(), Map.of(), Map.of(), List.of());
        }
        return CACHE.computeIfAbsent(level, ignored -> resolve(player, level));
    }

    private static Snapshot resolve(ServerPlayer player, ServerLevel level) {
        Map<String, DrehmalFirstRouteCatalog.Site> sites = new LinkedHashMap<>();
        Map<String, DrehmalFirstRouteCatalog.Footprint> footprints = new LinkedHashMap<>();
        Map<String, DrehmalFirstRouteCatalog.Patrol> patrols = new LinkedHashMap<>();
        List<DrehmalFirstRouteCatalog.EncounterSlot> encounters = new ArrayList<>();

        for (AvsalExpansionCatalog.SitePlan plan : AvsalExpansionCatalog.plan().sites()) {
            DrehmalFirstRouteCatalog.Position position = plan.encounter()
                    ? resolveEncounterSite(level, plan)
                    : ("NPC_ZONE".equals(plan.kind()) || "CLUE_ZONE".equals(plan.kind()))
                    ? resolvePassiveSite(level, plan)
                    : ground(level, plan.seed());
            if (position == null) continue;
            sites.put(plan.locator(), new DrehmalFirstRouteCatalog.Site(
                    plan.locator(), plan.kind(), AvsalExpansionCatalog.ROUTE_ID, plan.playerLabel(),
                    position, plan.safetyRadius(), plan.encounterRadius(), true, true));
            if (!plan.encounter()) continue;

            List<DrehmalFirstRouteCatalog.ArenaCandidate> arenas = resolveArenas(player, level, position);
            List<DrehmalFirstRouteCatalog.Position> patrolPoints = resolvePatrol(level, plan.patrolSeeds());
            if (arenas.size() < 2 || patrolPoints.size() < 2) {
                Turnbound.LOGGER.warn("TURNBOUND left Av'Sal encounter {} dormant: arenas={}, patrolPoints={}",
                        plan.encounterLocator(), arenas.size(), patrolPoints.size());
                continue;
            }

            String footprintId = plan.encounterLocator().replace("turnbound:encounter/", "turnbound:footprint/");
            String patrolId = plan.encounterLocator().replace("turnbound:encounter/", "turnbound:patrol/");
            int enemySlots = CampaignEncounterCatalog.spec(plan.combatEncounterId()).enemies().size();
            var footprint = new DrehmalFirstRouteCatalog.Footprint(
                    footprintId, plan.locator(), 14, 4, enemySlots, arenas, true, true);
            var patrol = new DrehmalFirstRouteCatalog.Patrol(
                    patrolId, plan.locator(), "ROAM", 30, 90, patrolPoints, true, true);
            footprints.put(footprintId, footprint);
            patrols.put(patrolId, patrol);

            var encounter = new DrehmalFirstRouteCatalog.EncounterSlot(
                    plan.encounterLocator(), plan.locator(), plan.tier(), footprintId, patrolId,
                    plan.combatEncounterId(), plan.playerLabel(), 1, true, true);
            if (DrehmalEncounterActivationRules.ready(encounter, sites.get(plan.locator()), footprint, patrol)) encounters.add(encounter);
        }

        Turnbound.LOGGER.info("TURNBOUND resolved Av'Sal first slice: {} sites, {} encounters", sites.size(), encounters.size());
        return new Snapshot(sites, footprints, patrols, encounters);
    }

    private static boolean inside(ServerPlayer player, AvsalExpansionCatalog.SitePlan plan, DrehmalFirstRouteCatalog.Site site) {
        if (player == null || plan == null || site == null || site.runtimePosition() == null) return false;
        var pos = site.runtimePosition();
        double dx = player.getX() - (pos.x() + 0.5D);
        double dz = player.getZ() - (pos.z() + 0.5D);
        double radius = plan.detectionRadius();
        return dx * dx + dz * dz <= radius * radius;
    }

    private static DrehmalFirstRouteCatalog.Position resolveEncounterSite(ServerLevel level, AvsalExpansionCatalog.SitePlan plan) {
        for (int[] offset : offsets(plan.searchRadius())) {
            int x = plan.seed().x() + offset[0];
            int z = plan.seed().z() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!standing(level, feet) || !DrehmalAdaptiveRoutePlacement.fieldProxyContentClear(level, x, y, z)) continue;
            if (DrehmalRouteZoneRules.insideSafetyZone(
                    DrehmalAdaptiveRoutePlacement.productionSites(level), x + 0.5D, z + 0.5D)) continue;
            return new DrehmalFirstRouteCatalog.Position(x, y, z);
        }
        return null;
    }

    private static DrehmalFirstRouteCatalog.Position resolvePassiveSite(ServerLevel level, AvsalExpansionCatalog.SitePlan plan) {
        for (int[] offset : offsets(plan.searchRadius())) {
            int x = plan.seed().x() + offset[0];
            int z = plan.seed().z() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!standing(level, feet)) continue;
            if (!DrehmalAdaptiveRoutePlacement.sourceContentClear(level, x, y, z, 2.25D)) continue;
            return new DrehmalFirstRouteCatalog.Position(x, y, z);
        }
        return null;
    }

    private static DrehmalFirstRouteCatalog.Position ground(ServerLevel level, AvsalExpansionCatalog.Point point) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, point.x(), point.z());
        return new DrehmalFirstRouteCatalog.Position(point.x(), y, point.z());
    }

    private static List<DrehmalFirstRouteCatalog.ArenaCandidate> resolveArenas(
            ServerPlayer player, ServerLevel level, DrehmalFirstRouteCatalog.Position home
    ) {
        List<DrehmalFirstRouteCatalog.ArenaCandidate> out = new ArrayList<>();
        int[][] probes = {{14,0},{-14,0},{0,14},{0,-14},{10,10},{-10,10},{10,-10},{-10,-10},{20,0},{0,20}};
        float[] yaws = {0.0F, 90.0F, 180.0F, 270.0F};
        for (int i = 0; i < probes.length && out.size() < 4; i++) {
            int sx = home.x() + probes[i][0];
            int sz = home.z() + probes[i][1];
            DrehmalFirstRouteCatalog.ArenaCandidate candidate = null;
            for (int[] offset : offsets(4)) {
                int x = sx + offset[0];
                int z = sz + offset[1];
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!standing(level, feet) || localHeightSpread(level, x, z, 5, 2) > 3) continue;
                if (!DrehmalAdaptiveRoutePlacement.sourceContentClear(level, x, y, z, 4.5D)) continue;
                Vec3 center = new Vec3(x + 0.5D, y, z + 0.5D);
                float yaw = yaws[i % yaws.length];
                if (!BattleSessionManager.surveyArenaOpen(player, center, yaw, 4)) continue;
                candidate = new DrehmalFirstRouteCatalog.ArenaCandidate(new DrehmalFirstRouteCatalog.Position(x, y, z), yaw);
                break;
            }
            if (candidate == null) continue;
            var resolved = candidate;
            if (out.stream().noneMatch(existing ->
                    existing.center().x() == resolved.center().x() && existing.center().z() == resolved.center().z())) out.add(candidate);
        }
        return List.copyOf(out);
    }

    private static List<DrehmalFirstRouteCatalog.Position> resolvePatrol(ServerLevel level, List<AvsalExpansionCatalog.Point> seeds) {
        List<DrehmalFirstRouteCatalog.Position> out = new ArrayList<>();
        for (AvsalExpansionCatalog.Point seed : seeds) {
            DrehmalFirstRouteCatalog.Position found = null;
            for (int[] offset : offsets(6)) {
                int x = seed.x() + offset[0];
                int z = seed.z() + offset[1];
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!standing(level, feet) || !DrehmalAdaptiveRoutePlacement.fieldProxyContentClear(level, x, y, z)) continue;
                found = new DrehmalFirstRouteCatalog.Position(x, y, z);
                break;
            }
            if (found == null) continue;
            var resolved = found;
            if (out.stream().noneMatch(existing -> existing.x() == resolved.x() && existing.z() == resolved.z())) out.add(found);
        }
        return List.copyOf(out);
    }

    private static boolean standing(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        if (level.getBlockState(below).isAir() || level.getBlockState(below).is(BlockTags.LEAVES)
                || !level.getFluidState(below).isEmpty()) return false;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos pos = feet.above(dy);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) return false;
        }
        return localHeightSpread(level, feet.getX(), feet.getZ(), 1, 1) <= 2;
    }

    private static int localHeightSpread(ServerLevel level, int x, int z, int radius, int step) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dz = -radius; dz <= radius; dz += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min;
    }

    private static List<int[]> offsets(int radius) {
        List<int[]> out = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz <= radius * radius) out.add(new int[]{dx, dz});
            }
        }
        out.sort(Comparator.comparingInt(value -> value[0] * value[0] + value[1] * value[1]));
        return out;
    }
}
