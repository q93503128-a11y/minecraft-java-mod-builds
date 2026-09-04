from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 match, got {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Frontier: static worker routing caches must not survive an integrated/dedicated server restart.
fw = Path("projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java")
replace_once(
    fw,
    "import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;\n",
    "import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\n",
    "frontier stopping import",
)
replace_once(
    fw,
    "    public static void tick(MinecraftServer server, SettlementData data) {\n",
    """    public static void onServerStopping(ServerStoppingEvent event) {\n        RESOURCE_TARGETS.clear();\n        RESOURCE_SEARCH_RETRY_AFTER.clear();\n        BLOCKED_TARGETS.clear();\n        MOVEMENT_WATCHES.clear();\n    }\n\n    public static void tick(MinecraftServer server, SettlementData data) {\n""",
    "frontier worker cache stop cleanup",
)

fentry = Path("projects/frontier-settlement/src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java")
replace_once(
    fentry,
    "        NeoForge.EVENT_BUS.addListener(SettlementWorkerService::onLivingDrops);\n",
    "        NeoForge.EVENT_BUS.addListener(SettlementWorkerService::onLivingDrops);\n        NeoForge.EVENT_BUS.addListener(SettlementWorkerService::onServerStopping);\n",
    "frontier worker stop listener",
)

fprops = Path("projects/frontier-settlement/gradle.properties")
replace_once(fprops, "mod_version=0.1.0-alpha.91\n", "mod_version=0.1.0-alpha.92\n", "frontier version")

root = Path("projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension")

# Expedition incident runtime owns ServerLevel/bossbars/mobs in static maps. Dispose it on server stop.
incident = root / "expedition/ExpeditionIncidentSystem.java"
replace_once(
    incident,
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.tick.PlayerTickEvent;\n",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\nimport net.neoforged.neoforge.event.tick.PlayerTickEvent;\n",
    "incident stopping import",
)
replace_once(
    incident,
    "    private static void removeStaleServerIncidents(MinecraftServer server) {\n",
    """    public static void onServerStopping(ServerStoppingEvent event) {\n        List<UUID> activeOwners = new ArrayList<>();\n        for (Map.Entry<UUID, ActiveIncident> entry : ACTIVE.entrySet()) {\n            ActiveIncident active = entry.getValue();\n            if (active.level.getServer() != event.getServer()) continue;\n            cleanupMobs(active);\n            closeBossBar(active.bossBar);\n            activeOwners.add(entry.getKey());\n        }\n        for (UUID owner : activeOwners) ACTIVE.remove(owner);\n\n        List<UUID> pendingOwners = new ArrayList<>();\n        for (Map.Entry<UUID, PendingIncident> entry : PENDING.entrySet()) {\n            PendingIncident pending = entry.getValue();\n            if (pending.level.getServer() != event.getServer()) continue;\n            closeBossBar(pending.bossBar);\n            pendingOwners.add(entry.getKey());\n        }\n        for (UUID owner : pendingOwners) PENDING.remove(owner);\n    }\n\n    private static void removeStaleServerIncidents(MinecraftServer server) {\n""",
    "incident stop cleanup",
)

# Apex runtime: same static ServerLevel/entity ownership; do not wait for a later server tick to discover stale state.
apex = root / "apex/ApexHuntSystem.java"
replace_once(
    apex,
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n",
    "apex stopping import",
)
replace_once(
    apex,
    "    private static void removeStaleServerHunts(MinecraftServer server) {\n",
    """    public static void onServerStopping(ServerStoppingEvent event) {\n        List<UUID> stopped = new ArrayList<>();\n        for (Map.Entry<UUID, Hunt> entry : ACTIVE.entrySet()) {\n            Hunt hunt = entry.getValue();\n            if (hunt.level.getServer() != event.getServer()) continue;\n            cleanupMobs(hunt);\n            closeBossBar(hunt);\n            stopped.add(entry.getKey());\n        }\n        for (UUID owner : stopped) ACTIVE.remove(owner);\n        ticker = 0;\n    }\n\n    private static void removeStaleServerHunts(MinecraftServer server) {\n""",
    "apex stop cleanup",
)

# Ascension Trial stale cleanup used to close only its bar, retaining tracked mobs through the static Trial reference.
trial = root / "endgame/AscensionTrialSystem.java"
replace_once(
    trial,
    "import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n",
    "import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n",
    "trial stopping import",
)
replace_once(
    trial,
    """    private static void fail(Trial trial, String reason) {\n        for (UUID id : trial.mobIds) {\n            Entity entity = trial.level.getEntity(id);\n            if (entity != null) entity.discard();\n        }\n        trial.mobIds.clear();\n""",
    """    private static void fail(Trial trial, String reason) {\n        cleanupMobs(trial);\n""",
    "trial shared mob cleanup",
)
replace_once(
    trial,
    "    private static void removeStaleServerTrials(MinecraftServer server) {\n",
    """    public static void onServerStopping(ServerStoppingEvent event) {\n        List<UUID> stopped = new ArrayList<>();\n        for (Map.Entry<UUID, Trial> entry : ACTIVE.entrySet()) {\n            Trial trial = entry.getValue();\n            if (trial.level.getServer() != event.getServer()) continue;\n            cleanupMobs(trial);\n            closeBossBar(trial);\n            stopped.add(entry.getKey());\n        }\n        for (UUID owner : stopped) ACTIVE.remove(owner);\n        ticker = 0;\n    }\n\n    private static void removeStaleServerTrials(MinecraftServer server) {\n""",
    "trial stop cleanup",
)
replace_once(
    trial,
    """            if (trial.level.getServer() != server) {\n                closeBossBar(trial);\n                stale.add(entry.getKey());\n            }\n""",
    """            if (trial.level.getServer() != server) {\n                cleanupMobs(trial);\n                closeBossBar(trial);\n                stale.add(entry.getKey());\n            }\n""",
    "trial stale mob cleanup",
)
replace_once(
    trial,
    "    private static void closeBossBar(Trial trial) {\n",
    """    private static void cleanupMobs(Trial trial) {\n        for (UUID id : trial.mobIds) {\n            Entity entity = trial.level.getEntity(id);\n            if (entity != null) entity.discard();\n        }\n        trial.mobIds.clear();\n    }\n\n    private static void closeBossBar(Trial trial) {\n""",
    "trial cleanup helper",
)

# Siege: co-op defenders shown on the bossbar and paid co-op XP are encounter participants too.
# Also move local physical supply consumption inside the start commit boundary.
siege = root / "production/OutpostSiegeSystem.java"
replace_once(
    siege,
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n",
    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\nimport net.neoforged.neoforge.event.server.ServerStoppingEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n",
    "siege stopping import",
)
replace_once(siege, "import java.util.UUID;\n", "import java.util.UUID;\nimport java.util.function.BooleanSupplier;\n", "siege boolean supplier import")
replace_once(
    siege,
    """    public static boolean isActive(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }\n    public static void startOrStatus(ServerPlayer player) { startOrStatus(player, SiegeMode.OUTPOST); }\n    public static void startBastionOrStatus(ServerPlayer player) { startOrStatus(player, SiegeMode.BASTION); }\n\n    private static void startOrStatus(ServerPlayer player, SiegeMode mode) {\n""",
    """    public static boolean isActive(ServerPlayer player) {\n        if (ACTIVE.containsKey(player.getUUID())) return true;\n        if (!player.isAlive() || player.isSpectator()) return false;\n        for (Siege siege : ACTIVE.values()) {\n            if (player.level() == siege.level\n                    && distanceToCenterSqr(player, siege.anchor) <= DEFENSE_RADIUS * DEFENSE_RADIUS) return true;\n        }\n        return false;\n    }\n    public static void startOrStatus(ServerPlayer player) { startOrStatus(player, SiegeMode.OUTPOST, () -> true); }\n    public static void startBastionOrStatus(ServerPlayer player) { startOrStatus(player, SiegeMode.BASTION, () -> true); }\n    public static void startOrStatus(ServerPlayer player, BooleanSupplier localSupplyCommit) {\n        startOrStatus(player, SiegeMode.OUTPOST, localSupplyCommit);\n    }\n    public static void startBastionOrStatus(ServerPlayer player, BooleanSupplier localSupplyCommit) {\n        startOrStatus(player, SiegeMode.BASTION, localSupplyCommit);\n    }\n\n    private static void startOrStatus(ServerPlayer player, SiegeMode mode, BooleanSupplier localSupplyCommit) {\n""",
    "siege participant authority and callback overload",
)
replace_once(
    siege,
    """        if (!spawnWave(siege)) {\n            cleanupMobs(siege); closeBossBar(siege);\n            player.sendSystemMessage(Component.literal(\"§c[\" + label + \"] §f전초 외곽에 충분한 습격대를 배치할 열린 로딩 지형이 없습니다. §7보급권은 소비하지 않았습니다.\"));\n            return;\n        }\n        if (!production.consumeSupplyCharges(player, mode.supplyCost)) {\n""",
    """        if (!spawnWave(siege)) {\n            cleanupMobs(siege); closeBossBar(siege);\n            player.sendSystemMessage(Component.literal(\"§c[\" + label + \"] §f전초 외곽에 충분한 습격대를 배치할 열린 로딩 지형이 없습니다. §7보급권은 소비하지 않았습니다.\"));\n            return;\n        }\n        if (!localSupplyCommit.getAsBoolean()) {\n            cleanupMobs(siege); closeBossBar(siege);\n            player.sendSystemMessage(Component.literal(\"§c[\" + label + \"] §f전초의 현지 실물 보급 재고가 바뀌어 시작하지 않았습니다. §7보급권/재사용 대기시간은 소비되지 않았습니다.\"));\n            return;\n        }\n        if (!production.consumeSupplyCharges(player, mode.supplyCost)) {\n""",
    "siege local supply commit boundary",
)
replace_once(
    siege,
    "    public static void onLivingDeath(LivingDeathEvent event) {\n",
    """    public static void onServerStopping(ServerStoppingEvent event) {\n        List<UUID> stopped = new ArrayList<>();\n        for (Map.Entry<UUID, Siege> entry : ACTIVE.entrySet()) {\n            Siege siege = entry.getValue();\n            if (siege.level.getServer() != event.getServer()) continue;\n            cleanupMobs(siege);\n            closeBossBar(siege);\n            stopped.add(entry.getKey());\n        }\n        for (UUID owner : stopped) ACTIVE.remove(owner);\n        ticker = 0;\n    }\n\n    public static void onLivingDeath(LivingDeathEvent event) {\n""",
    "siege stop cleanup",
)

# Operation: local physical stock is now a pre-charge commit callback; failed stock recheck rolls SavedData back.
operation = root / "expedition/ExpeditionOperationSystem.java"
replace_once(
    operation,
    "import net.neoforged.neoforge.event.tick.PlayerTickEvent;\n\npublic final class ExpeditionOperationSystem {\n",
    "import net.neoforged.neoforge.event.tick.PlayerTickEvent;\n\nimport java.util.function.BooleanSupplier;\n\npublic final class ExpeditionOperationSystem {\n",
    "operation boolean supplier import",
)
replace_once(
    operation,
    "    public static void startOrStatus(ServerPlayer player) {\n",
    """    public static void startOrStatus(ServerPlayer player) { startOrStatus(player, () -> true); }\n\n    public static void startOrStatus(ServerPlayer player, BooleanSupplier localSupplyCommit) {\n""",
    "operation callback overload",
)
replace_once(
    operation,
    """        if (!data.start(player, operation, outpost.dimension(), outpost.pos(), deadline, complication)) { player.sendSystemMessage(Component.literal(\"§c[원정 작전] §f작전 상태가 바뀌어 출발하지 못했습니다.\")); return; }\n        if (!production.consumeSupplyCharge(player)) {\n""",
    """        if (!data.start(player, operation, outpost.dimension(), outpost.pos(), deadline, complication)) { player.sendSystemMessage(Component.literal(\"§c[원정 작전] §f작전 상태가 바뀌어 출발하지 못했습니다.\")); return; }\n        if (!localSupplyCommit.getAsBoolean()) {\n            data.fail(player);\n            player.sendSystemMessage(Component.literal(\"§c[원정 작전] §f전초의 현지 실물 보급 재고가 바뀌어 출발하지 않았습니다. §7보급권은 소비되지 않았습니다.\"));\n            return;\n        }\n        if (!production.consumeSupplyCharge(player)) {\n""",
    "operation local supply commit boundary",
)

production = root / "production/ProductionService.java"
replace_once(
    production,
    """        if (bastion) OutpostSiegeSystem.startBastionOrStatus(player);\n        else OutpostSiegeSystem.startOrStatus(player);\n        if (OutpostSiegeSystem.isActive(player) && !consumeLocalOutpostSupply(player, prepared)) {\n            player.sendSystemMessage(Component.literal(\"§c[전선 현지 보급] §f방어전 시작 직후 전초 재고가 바뀌어 시작을 취소했습니다. §7재고를 확인한 뒤 다시 시도하세요.\"));\n        }\n""",
    """        if (bastion) OutpostSiegeSystem.startBastionOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared));\n        else OutpostSiegeSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared));\n""",
    "siege local supply transaction",
)
replace_once(
    production,
    """        ExpeditionOperationSystem.startOrStatus(player);\n        if (ExpeditionOperationSystem.isActive(player) && !consumeLocalOutpostSupply(player, prepared)) {\n            player.sendSystemMessage(Component.literal(\"§c[전선 현지 보급] §f원정 출발 직후 전초 재고가 바뀌어 시작을 취소했습니다. §7재고를 확인한 뒤 다시 시도하세요.\"));\n        }\n""",
    "        ExpeditionOperationSystem.startOrStatus(player, () -> consumeLocalOutpostSupply(player, prepared));\n",
    "operation local supply transaction",
)

# Register orderly lifecycle hooks and bump the replacement JAR version.
sentry = root / "SurvivalAscension.java"
replace_once(sentry, '    public static final String VERSION = "0.61.0-alpha.1";\n', '    public static final String VERSION = "0.61.1-alpha.1";\n', "survival runtime version")
replace_once(
    sentry,
    "        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onServerStopping);\n",
    """        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onServerStopping);\n        NeoForge.EVENT_BUS.addListener(ExpeditionIncidentSystem::onServerStopping);\n        NeoForge.EVENT_BUS.addListener(ApexHuntSystem::onServerStopping);\n        NeoForge.EVENT_BUS.addListener(AscensionTrialSystem::onServerStopping);\n        NeoForge.EVENT_BUS.addListener(OutpostSiegeSystem::onServerStopping);\n""",
    "survival encounter stop listeners",
)
sprops = Path("projects/survival-ascension/gradle.properties")
replace_once(sprops, "mod_version=0.61.0-alpha.1\n", "mod_version=0.61.1-alpha.1\n", "survival version")

print("deep audit 2 fixes applied")
