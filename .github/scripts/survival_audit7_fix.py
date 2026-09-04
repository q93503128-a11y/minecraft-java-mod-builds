from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace(path: str, old: str, new: str):
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing expected text in {path}: {old[:120]!r}")
    text2 = text.replace(old, new, 1)
    p.write_text(text2, encoding="utf-8")


# Version bump.
replace(
    "projects/survival-ascension/gradle.properties",
    "mod_version=0.61.5-alpha.1",
    "mod_version=0.61.6-alpha.1",
)
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    'public static final String VERSION = "0.61.5-alpha.1";',
    'public static final String VERSION = "0.61.6-alpha.1";',
)

# Apex: participant membership is reward authority; arena proximity is separate encounter admission authority.
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    '''    public static boolean isActive(ServerPlayer player) {\n        UUID id = player.getUUID();\n        if (ACTIVE.containsKey(id)) return true;\n        for (Hunt hunt : ACTIVE.values()) if (hunt.participants.contains(id)) return true;\n        return false;\n    }\n\n    public static void tryStart(ServerPlayer player) {''',
    '''    public static boolean isActive(ServerPlayer player) {\n        UUID id = player.getUUID();\n        if (ACTIVE.containsKey(id)) return true;\n        for (Hunt hunt : ACTIVE.values()) if (hunt.participants.contains(id)) return true;\n        return false;\n    }\n\n    public static boolean hasActiveNear(ServerPlayer player) {\n        for (Hunt hunt : ACTIVE.values()) {\n            if (player.level() == hunt.level\n                    && distanceToCenterSqr(player, hunt.center) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) return true;\n        }\n        return false;\n    }\n\n    public static void tryStart(ServerPlayer player) {'''
)

# Trial: same split between reward participants and arena admission authority.
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    '''    public static boolean isActive(ServerPlayer player) {\n        UUID id = player.getUUID();\n        if (ACTIVE.containsKey(id)) return true;\n        for (Trial trial : ACTIVE.values()) if (trial.participants.contains(id)) return true;\n        return false;\n    }\n\n    public static void onServerTick(ServerTickEvent.Pre event) {''',
    '''    public static boolean isActive(ServerPlayer player) {\n        UUID id = player.getUUID();\n        if (ACTIVE.containsKey(id)) return true;\n        for (Trial trial : ACTIVE.values()) if (trial.participants.contains(id)) return true;\n        return false;\n    }\n\n    public static boolean hasActiveNear(ServerPlayer player) {\n        for (Trial trial : ACTIVE.values()) {\n            if (player.level() == trial.level\n                    && distanceToCenterSqr(player, trial.center) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) return true;\n        }\n        return false;\n    }\n\n    public static void onServerTick(ServerTickEvent.Pre event) {'''
)

# Incident: pending and active arenas both reserve their full anti-overlap envelope.
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    '''    public static boolean isActive(ServerPlayer player) {\n        UUID id = player.getUUID();\n        if (PENDING.containsKey(id) || ACTIVE.containsKey(id)) return true;\n        for (PendingIncident pending : PENDING.values()) if (pending.participants.contains(id)) return true;\n        for (ActiveIncident active : ACTIVE.values()) if (active.participants.contains(id)) return true;\n        return false;\n    }\n\n    public static void onPlayerTick(PlayerTickEvent.Post event) {''',
    '''    public static boolean isActive(ServerPlayer player) {\n        UUID id = player.getUUID();\n        if (PENDING.containsKey(id) || ACTIVE.containsKey(id)) return true;\n        for (PendingIncident pending : PENDING.values()) if (pending.participants.contains(id)) return true;\n        for (ActiveIncident active : ACTIVE.values()) if (active.participants.contains(id)) return true;\n        return false;\n    }\n\n    public static boolean hasActiveNear(ServerPlayer player) {\n        double clearanceSqr = INCIDENT_CENTER_CLEARANCE * INCIDENT_CENTER_CLEARANCE;\n        for (PendingIncident pending : PENDING.values()) {\n            if (player.level() == pending.level\n                    && distanceToCenterSqr(player, pending.center) < clearanceSqr) return true;\n        }\n        for (ActiveIncident active : ACTIVE.values()) {\n            if (player.level() == active.level\n                    && distanceToCenterSqr(player, active.center) < clearanceSqr) return true;\n        }\n        return false;\n    }\n\n    public static void onPlayerTick(PlayerTickEvent.Post event) {'''
)

# Siege: DEFENSE_RADIUS is participation/UI range; EXCLUSION_RADIUS is encounter admission range.
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/production/OutpostSiegeSystem.java",
    '''    public static boolean isActive(ServerPlayer player) {\n        if (ACTIVE.containsKey(player.getUUID())) return true;\n        if (!player.isAlive() || player.isSpectator()) return false;\n        for (Siege siege : ACTIVE.values()) {\n            if (player.level() == siege.level\n                    && distanceToCenterSqr(player, siege.anchor) <= DEFENSE_RADIUS * DEFENSE_RADIUS) return true;\n        }\n        return false;\n    }\n    public static void startOrStatus(ServerPlayer player) { startOrStatus(player, SiegeMode.OUTPOST, () -> true); }''',
    '''    public static boolean isActive(ServerPlayer player) {\n        if (ACTIVE.containsKey(player.getUUID())) return true;\n        if (!player.isAlive() || player.isSpectator()) return false;\n        for (Siege siege : ACTIVE.values()) {\n            if (player.level() == siege.level\n                    && distanceToCenterSqr(player, siege.anchor) <= DEFENSE_RADIUS * DEFENSE_RADIUS) return true;\n        }\n        return false;\n    }\n\n    public static boolean hasActiveNear(ServerPlayer player) {\n        for (Siege siege : ACTIVE.values()) {\n            if (player.level() == siege.level\n                    && distanceToCenterSqr(player, siege.anchor) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) return true;\n        }\n        return false;\n    }\n    public static void startOrStatus(ServerPlayer player) { startOrStatus(player, SiegeMode.OUTPOST, () -> true); }'''
)

# Final sequence: expose its 128-block arena boundary independently from owner identity.
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionSystem.java",
    '''    public static boolean isActive(ServerPlayer player) {\n        return ACTIVE.containsKey(player.getUUID());\n    }\n\n    public static boolean isFinalSequenceActive(ServerPlayer player) {\n        return isActive(player) || FinalAscensionBossSystem.isActive(player);\n    }\n\n    public static boolean hasOtherMajorActivity(ServerPlayer player) {\n        return AscensionTrialSystem.isActive(player)\n                || ApexHuntSystem.isActive(player)\n                || ExpeditionIncidentSystem.isActive(player)\n                || ExpeditionOperationSystem.isActive(player)\n                || OutpostSiegeSystem.isActive(player);\n    }''',
    '''    public static boolean isActive(ServerPlayer player) {\n        return ACTIVE.containsKey(player.getUUID());\n    }\n\n    public static boolean hasActiveNear(ServerPlayer player) {\n        for (Run run : ACTIVE.values()) {\n            if (player.level() == run.level\n                    && distanceToCenterSqr(player, run.center) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) return true;\n        }\n        return false;\n    }\n\n    public static boolean isFinalSequenceActive(ServerPlayer player) {\n        return isActive(player) || hasActiveNear(player)\n                || FinalAscensionBossSystem.isActive(player) || FinalAscensionBossSystem.hasActiveNear(player);\n    }\n\n    public static boolean hasOtherMajorActivity(ServerPlayer player) {\n        return AscensionTrialSystem.isActive(player) || AscensionTrialSystem.hasActiveNear(player)\n                || ApexHuntSystem.isActive(player) || ApexHuntSystem.hasActiveNear(player)\n                || ExpeditionIncidentSystem.isActive(player) || ExpeditionIncidentSystem.hasActiveNear(player)\n                || ExpeditionOperationSystem.isActive(player)\n                || OutpostSiegeSystem.isActive(player) || OutpostSiegeSystem.hasActiveNear(player);\n    }'''
)
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionSystem.java",
    '''    private static boolean hasConflictingActivity(ServerPlayer player) {\n        return hasOtherMajorActivity(player) || FinalAscensionBossSystem.isActive(player);\n    }''',
    '''    private static boolean hasConflictingActivity(ServerPlayer player) {\n        return hasOtherMajorActivity(player)\n                || FinalAscensionBossSystem.isActive(player) || FinalAscensionBossSystem.hasActiveNear(player);\n    }'''
)

# Final boss: nearby players shown the encounter must also be blocked from opening another major arena.
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionBossSystem.java",
    '''    private static final double PLAYER_RADIUS = 72.0D;\n    private static final double RECALL_RADIUS = 44.0D;''',
    '''    private static final double PLAYER_RADIUS = 72.0D;\n    private static final double EXCLUSION_RADIUS = 128.0D;\n    private static final double RECALL_RADIUS = 44.0D;'''
)
replace(
    "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionBossSystem.java",
    '''    public static boolean isActive(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }\n    public static boolean isInternalSpawn() { return internalSpawn; }''',
    '''    public static boolean isActive(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }\n\n    public static boolean hasActiveNear(ServerPlayer player) {\n        for (Run run : ACTIVE.values()) {\n            if (player.level() == run.level\n                    && distanceToCenterSqr(player, run.center) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) return true;\n        }\n        return false;\n    }\n\n    public static boolean isInternalSpawn() { return internalSpawn; }'''
)

print("SURVIVAL_AUDIT7_FIX_APPLIED")
