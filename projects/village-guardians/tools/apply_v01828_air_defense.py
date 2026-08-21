#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing patch anchor: {label}")
    if text.count(old) != 1:
        raise SystemExit(f"ambiguous patch anchor: {label} ({text.count(old)})")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    if start not in text or end not in text:
        raise SystemExit(f"missing patch boundary: {label}")
    left, rest = text.split(start, 1)
    middle, right = rest.split(end, 1)
    return left + replacement + end + right


def patch_enemy() -> None:
    path = JAVA / "VillageEnemyArchetypeSystem.java"
    text = read(path)
    text = replace_once(text,
        "    public record SpawnedEnemy(Mob mob, Archetype archetype, boolean boss) {}\n",
        """    public record SpawnedEnemy(Mob mob, Archetype archetype, boolean boss) {}\n\n    /** Runtime doctrine for the real flying roster. All roles keep the same Phantom silhouette,\n     * while target ownership, cadence and threat priority are authored by VillageRaidSystem. */\n    public enum AerialRole {\n        RAIDER(\"하늘 약탈귀\", \"수호자 급강하\"),\n        BOMBARDIER(\"파성 망령\", \"내부 시설 폭격\"),\n        HARRIER(\"폭풍 사냥귀\", \"고속 수호자 추격\");\n\n        private final String displayName;\n        private final String combatRole;\n        AerialRole(String displayName, String combatRole) {\n            this.displayName = displayName;\n            this.combatRole = combatRole;\n        }\n        public String displayName() { return displayName; }\n        public String combatRole() { return combatRole; }\n    }\n""", "aerial role enum")
    text = replace_once(text,
        """    /** Shared deterministic predicate used by both the real spawn path and daytime intelligence. */\n    public static boolean willSpawnFlying(\n            int day, int wave, int index, boolean boss, VillageWaveTrait trait) {\n        return !boss && shouldSpawnFlying(day, wave, index, trait);\n    }\n\n""",
        """    /** Shared deterministic predicate used by both the real spawn path and daytime intelligence. */\n    public static boolean willSpawnFlying(\n            int day, int wave, int index, boolean boss, VillageWaveTrait trait) {\n        return !boss && shouldSpawnFlying(day, wave, index, trait);\n    }\n\n    /** Deterministic flying doctrine; daytime intel and the real spawn loop call this exact selector. */\n    public static AerialRole aerialRole(int day, int wave, int index, VillageWaveTrait trait) {\n        if (day < 10) return AerialRole.RAIDER;\n        int roll = Math.floorMod(day * 31 + wave * 17 + index * 13, 12);\n        if (day >= 13 && (trait == VillageWaveTrait.HUNTERS ? roll >= 6 : roll >= 10)) {\n            return AerialRole.HARRIER;\n        }\n        if (trait == VillageWaveTrait.STORMFRONT ? roll >= 5 : roll >= 8) {\n            return AerialRole.BOMBARDIER;\n        }\n        return AerialRole.RAIDER;\n    }\n\n""", "aerial role selector")
    write(path, text)


def patch_raid() -> None:
    path = JAVA / "VillageRaidSystem.java"
    text = read(path)
    text = replace_once(text,
        "    private static final Map<UUID, Integer> ACTIVE_WAVES = new HashMap<>();\n    private static final Map<UUID, AerialStrike> AERIAL_STRIKES = new HashMap<>();\n",
        "    private static final Map<UUID, Integer> ACTIVE_WAVES = new HashMap<>();\n    private static final Map<UUID, VillageEnemyArchetypeSystem.AerialRole> ACTIVE_AERIAL_ROLES = new HashMap<>();\n    private static final Map<UUID, AerialStrike> AERIAL_STRIKES = new HashMap<>();\n",
        "raid aerial role map")
    text = replace_once(text,
        """        return ACTIVE_ENEMIES.contains(uuid)\n                || ACTIVE_ARCHETYPES.containsKey(uuid)\n                || ACTIVE_WAVES.containsKey(uuid)\n                || entity.entityTags().contains(RAID_ENEMY_TAG);\n""",
        """        return ACTIVE_ENEMIES.contains(uuid)\n                || ACTIVE_ARCHETYPES.containsKey(uuid)\n                || ACTIVE_WAVES.containsKey(uuid)\n                || ACTIVE_AERIAL_ROLES.containsKey(uuid)\n                || entity.entityTags().contains(RAID_ENEMY_TAG);\n""", "raid metadata authority")
    text = replace_once(text,
        """    public static VillageEnemyArchetypeSystem.Archetype archetypeOf(Mob mob) {\n        return mob == null ? null : ACTIVE_ARCHETYPES.get(mob.getUUID());\n    }\n\n""",
        """    public static VillageEnemyArchetypeSystem.Archetype archetypeOf(Mob mob) {\n        return mob == null ? null : ACTIVE_ARCHETYPES.get(mob.getUUID());\n    }\n\n    public static VillageEnemyArchetypeSystem.AerialRole aerialRoleOf(Mob mob) {\n        if (mob == null || !VillageEnemyArchetypeSystem.isFlying(mob)) return null;\n        return ACTIVE_AERIAL_ROLES.getOrDefault(mob.getUUID(), VillageEnemyArchetypeSystem.AerialRole.RAIDER);\n    }\n\n    /** Higher values are selected first by dedicated anti-air defenders. */\n    public static int aerialThreatPriority(Mob mob) {\n        VillageEnemyArchetypeSystem.AerialRole role = aerialRoleOf(mob);\n        if (role == null) return 0;\n        return switch (role) {\n            case BOMBARDIER -> 300;\n            case HARRIER -> 220;\n            case RAIDER -> 140;\n        };\n    }\n\n""", "raid aerial role accessors")
    text = replace_once(text,
        """            VillageEnemyArchetypeSystem.configure(\n                    level, mob, spawned.archetype(), currentTrait, day, wave, boss);\n            if (boss) VillageBossAspectSystem.configure(level, mob, day, wave, index);\n""",
        """            VillageEnemyArchetypeSystem.configure(\n                    level, mob, spawned.archetype(), currentTrait, day, wave, boss);\n            if (VillageEnemyArchetypeSystem.isFlying(mob)) {\n                VillageEnemyArchetypeSystem.AerialRole aerialRole =\n                        VillageEnemyArchetypeSystem.aerialRole(day, wave, index, currentTrait);\n                ACTIVE_AERIAL_ROLES.put(mob.getUUID(), aerialRole);\n                mob.setCustomName(Component.literal(\"§b웨이브 \" + wave + \" · \" + aerialRole.displayName()\n                        + \" §8[\" + aerialRole.combatRole() + \" · 성벽 우회]\"));\n            }\n            if (boss) VillageBossAspectSystem.configure(level, mob, day, wave, index);\n""", "spawn aerial role registration")

    start = "    private static void directFlyingEnemy(\n"
    end = "    private static void moveFlyingToward(Mob mob, Vec3 lookAt, Vec3 wanted, double speed) {\n"
    replacement = """    private static void directFlyingEnemy(\n            MinecraftServer server, ServerLevel level, Mob mob,\n            VillageEnemyArchetypeSystem.Archetype archetype, BlockPos villageCenter) {\n        if (villageCenter == null) return;\n        UUID id = mob.getUUID();\n        VillageEnemyArchetypeSystem.AerialRole role = aerialRoleOf(mob);\n        if (role == null) role = VillageEnemyArchetypeSystem.AerialRole.RAIDER;\n        // One owner only: vanilla Phantom targeting and all ground-elite navigation stay disabled.\n        mob.setTarget(null);\n\n        AerialStrike strike = AERIAL_STRIKES.get(id);\n        if (strike != null) {\n            if (abilityTicks < strike.impactTick()) {\n                Vec3 dive = strike.point().add(0.0, strike.targetsBuilding() ? 4.5 : 3.0, 0.0);\n                moveFlyingToward(mob, strike.point(), dive, aerialDiveSpeed(role));\n                return;\n            }\n            if (!strike.resolved()) {\n                resolveAerialStrike(server, level, mob, strike);\n                strike = strike.resolvedCopy();\n                AERIAL_STRIKES.put(id, strike);\n            }\n            if (abilityTicks < strike.recoveryUntilTick()) {\n                Vec3 recover = strike.point().add(0.0, strike.targetsBuilding() ? 12.5 : 11.0, 0.0);\n                moveFlyingToward(mob, strike.point(), recover, aerialRecoverySpeed(role));\n                return;\n            }\n            AERIAL_STRIKES.remove(id);\n        }\n\n        int phase = Math.floorMod(abilityTicks + id.hashCode(), aerialCadence(role));\n        // Bombardiers deliberately ignore nearby defenders while an internal facility still exists.\n        if (role == VillageEnemyArchetypeSystem.AerialRole.BOMBARDIER) {\n            VillageProgressionSystem.Building building = chooseTarget(\n                    villageCenter, mob.blockPosition(), true, archetype);\n            if (building != null && building != VillageProgressionSystem.Building.WALLS) {\n                BlockPos targetBlock = VillageWorldSystem.buildingCenter(building);\n                Vec3 target = Vec3.atCenterOf(targetBlock).add(0.0, 1.0, 0.0);\n                double angle = abilityTicks * 0.034 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;\n                Vec3 cruise = target.add(Math.cos(angle) * 9.5, 11.5, Math.sin(angle) * 9.5);\n                if (phase == 0 && mob.position().distanceToSqr(cruise) <= 24.0 * 24.0) {\n                    beginAerialStrike(level, mob, role, target, building);\n                    return;\n                }\n                moveFlyingToward(mob, target, cruise, 1.08);\n                return;\n            }\n        }\n\n        ServerPlayer player = nearestFlyingPriorityPlayer(server, mob, aerialPlayerSearchRange(role));\n        if (player != null) {\n            if (phase == 0) {\n                beginAerialStrike(level, mob, role, player.position(), null);\n                return;\n            }\n            double turn = role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 0.078 : 0.055;\n            double angle = abilityTicks * turn + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;\n            double radius = role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 6.0\n                    : 7.0 + Math.floorMod(id.hashCode(), 4);\n            double altitude = role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 6.5\n                    : 7.5 + Math.floorMod(id.hashCode() >>> 4, 4);\n            Vec3 cruise = player.position().add(Math.cos(angle) * radius, altitude, Math.sin(angle) * radius);\n            moveFlyingToward(mob, player.position().add(0.0, 1.0, 0.0), cruise,\n                    role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 1.38 : 1.20);\n            return;\n        }\n\n        VillageProgressionSystem.Building targetBuilding = chooseTarget(\n                villageCenter, mob.blockPosition(), true, archetype);\n        if (targetBuilding == null || targetBuilding == VillageProgressionSystem.Building.WALLS) return;\n        BlockPos targetBlock = VillageWorldSystem.buildingCenter(targetBuilding);\n        Vec3 target = Vec3.atCenterOf(targetBlock).add(0.0, 1.0, 0.0);\n        double angle = abilityTicks * 0.042 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;\n        Vec3 cruise = target.add(Math.cos(angle) * 8.0, 9.5, Math.sin(angle) * 8.0);\n        if (phase == 0 && mob.position().distanceToSqr(cruise) <= 22.0 * 22.0) {\n            beginAerialStrike(level, mob, role, target, targetBuilding);\n            return;\n        }\n        moveFlyingToward(mob, target, cruise, 1.16);\n    }\n\n    private static int aerialCadence(VillageEnemyArchetypeSystem.AerialRole role) {\n        return switch (role) {\n            case BOMBARDIER -> 112;\n            case HARRIER -> 62;\n            case RAIDER -> AERIAL_ASSAULT_CADENCE;\n        };\n    }\n\n    private static int aerialWarningTicks(VillageEnemyArchetypeSystem.AerialRole role) {\n        return switch (role) {\n            case BOMBARDIER -> 24;\n            case HARRIER -> 12;\n            case RAIDER -> AERIAL_WARNING_TICKS;\n        };\n    }\n\n    private static int aerialRecoveryTicks(VillageEnemyArchetypeSystem.AerialRole role) {\n        return switch (role) {\n            case BOMBARDIER -> 44;\n            case HARRIER -> 24;\n            case RAIDER -> AERIAL_RECOVERY_TICKS;\n        };\n    }\n\n    private static double aerialPlayerSearchRange(VillageEnemyArchetypeSystem.AerialRole role) {\n        return role == VillageEnemyArchetypeSystem.AerialRole.HARRIER ? 38.0 : 28.0;\n    }\n\n    private static double aerialDiveSpeed(VillageEnemyArchetypeSystem.AerialRole role) {\n        return switch (role) {\n            case BOMBARDIER -> 1.34;\n            case HARRIER -> 1.78;\n            case RAIDER -> 1.52;\n        };\n    }\n\n    private static double aerialRecoverySpeed(VillageEnemyArchetypeSystem.AerialRole role) {\n        return switch (role) {\n            case BOMBARDIER -> 1.20;\n            case HARRIER -> 1.58;\n            case RAIDER -> 1.38;\n        };\n    }\n\n    private static void beginAerialStrike(\n            ServerLevel level, Mob mob, VillageEnemyArchetypeSystem.AerialRole role,\n            Vec3 point, VillageProgressionSystem.Building building) {\n        int impactTick = abilityTicks + aerialWarningTicks(role);\n        AerialStrike strike = new AerialStrike(point, building, impactTick,\n                impactTick + aerialRecoveryTicks(role), false);\n        AERIAL_STRIKES.put(mob.getUUID(), strike);\n        VillageDefenseEffectSystem.aerialAssaultWarning(level, point, building != null);\n        Vec3 dive = point.add(0.0, building == null ? 3.0 : 4.5, 0.0);\n        moveFlyingToward(mob, point, dive, aerialDiveSpeed(role));\n    }\n\n    private static void resolveAerialStrike(\n            MinecraftServer server, ServerLevel level, Mob mob, AerialStrike strike) {\n        VillageEnemyArchetypeSystem.AerialRole role = aerialRoleOf(mob);\n        if (role == null) role = VillageEnemyArchetypeSystem.AerialRole.RAIDER;\n        if (strike.targetsBuilding()) {\n            VillageProgressionSystem.Building building = strike.building();\n            if (building != null && VillageProgressionSystem.isOperational(building)) {\n                int day = VillageCouncilState.currentDay();\n                float multiplier = currentTrait.structureDamageMultiplier()\n                        * VillageWarfrontSystem.structureDamageMultiplier(day)\n                        * VillageBossAspectSystem.structureMultiplier(mob)\n                        * VillageDifficultyTuning.earlyStructureMultiplier(day)\n                        * VillageDifficultyTuning.defenderStateStructureMultiplier(server);\n                float roleMultiplier = switch (role) {\n                    case BOMBARDIER -> 1.75f;\n                    case HARRIER -> 0.58f;\n                    case RAIDER -> 1.0f;\n                };\n                int damage = Math.max(1, Math.round((4 + wave + Math.min(16, day) * 0.45f)\n                        * multiplier * roleMultiplier));\n                VillageProgressionSystem.damageBuilding(server, building, damage);\n            }\n            VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), true);\n            return;\n        }\n\n        double radius = switch (role) {\n            case BOMBARDIER -> 3.10;\n            case HARRIER -> 2.15;\n            case RAIDER -> AERIAL_PLAYER_STRIKE_RADIUS;\n        };\n        double radiusSquared = radius * radius;\n        float roleMultiplier = switch (role) {\n            case BOMBARDIER -> 0.85f;\n            case HARRIER -> 0.82f;\n            case RAIDER -> 1.0f;\n        };\n        float damage = Math.max(2.0f, (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * roleMultiplier);\n        for (ServerPlayer player : server.getPlayerList().getPlayers()) {\n            if (player.level() != level || !player.isAlive() || player.isSpectator()\n                    || VillageRespawnSystem.isDowned(player)) continue;\n            if (player.position().distanceToSqr(strike.point()) <= radiusSquared) {\n                player.hurtServer(level, level.damageSources().mobAttack(mob), damage);\n            }\n        }\n        VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), false);\n    }\n\n"""
    text = replace_between(text, start, end, replacement, "authored aerial roles block")
    text = replace_once(text,
        """    private static ServerPlayer nearestFlyingPriorityPlayer(MinecraftServer server, Mob mob) {\n        ServerPlayer chosen = null;\n        double chosenDistance = 28.0 * 28.0;\n""",
        """    private static ServerPlayer nearestFlyingPriorityPlayer(MinecraftServer server, Mob mob, double range) {\n        ServerPlayer chosen = null;\n        double chosenDistance = range * range;\n""", "flying player range")
    text = replace_once(text,
        """        ACTIVE_ARCHETYPES.remove(uuid);\n        ACTIVE_WAVES.remove(uuid);\n        AERIAL_STRIKES.remove(uuid);\n""",
        """        ACTIVE_ARCHETYPES.remove(uuid);\n        ACTIVE_WAVES.remove(uuid);\n        ACTIVE_AERIAL_ROLES.remove(uuid);\n        AERIAL_STRIKES.remove(uuid);\n""", "release aerial role")
    text = replace_once(text,
        """        ACTIVE_ENEMIES.clear();\n        ACTIVE_ARCHETYPES.clear();\n        ACTIVE_WAVES.clear();\n        AERIAL_STRIKES.clear();\n""",
        """        ACTIVE_ENEMIES.clear();\n        ACTIVE_ARCHETYPES.clear();\n        ACTIVE_WAVES.clear();\n        ACTIVE_AERIAL_ROLES.clear();\n        AERIAL_STRIKES.clear();\n""", "clear aerial role")
    write(path, text)


def patch_elite() -> None:
    path = JAVA / "VillageEnemyEliteSystem.java"
    text = read(path)
    text = replace_once(text,
        """            VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);\n            if (archetype == null || VillageEnemyArchetypeSystem.isBoss(archetype) || ACTIVE.containsKey(mob.getUUID())) continue;\n""",
        """            VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);\n            // Flying movement has a single authoritative owner in VillageRaidSystem. Ground elite\n            // doctrines must never re-enable target/navigation AI on Phantom assault units.\n            if (VillageEnemyArchetypeSystem.isFlying(mob)) continue;\n            if (archetype == null || VillageEnemyArchetypeSystem.isBoss(archetype) || ACTIVE.containsKey(mob.getUUID())) continue;\n""", "ground elite excludes flying")
    write(path, text)


def patch_intel() -> None:
    path = JAVA / "VillageWaveIntelSystem.java"
    text = read(path)
    text = replace_once(text,
        """            Map<VillageEnemyArchetypeSystem.Archetype, Integer> roster = new LinkedHashMap<>();\n            List<String> bossLines = new ArrayList<>();\n            int flying = 0;\n""",
        """            Map<VillageEnemyArchetypeSystem.Archetype, Integer> roster = new LinkedHashMap<>();\n            Map<VillageEnemyArchetypeSystem.AerialRole, Integer> aerialRoster = new LinkedHashMap<>();\n            List<String> bossLines = new ArrayList<>();\n""", "intel aerial roster")
    text = replace_once(text,
        """                if (VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait)) {\n                    flying++;\n                    continue;\n                }\n""",
        """                if (VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait)) {\n                    VillageEnemyArchetypeSystem.AerialRole aerialRole =\n                            VillageEnemyArchetypeSystem.aerialRole(day, wave, index, trait);\n                    aerialRoster.merge(aerialRole, 1, Integer::sum);\n                    continue;\n                }\n""", "intel role count")
    text = replace_once(text,
        """            String direction = VillageAttackPlanSystem.scoutLine(day, wave, count);\n            String air = flying <= 0\n                    ? \"없음\"\n                    : \"하늘 약탈귀 ×\" + flying + \" · 성벽 우회 · 대공 발사대/성루 명사수 권장\";\n            String elite = VillageEnemyEliteSystem.scoutSummary(day, count);\n""",
        """            String direction = VillageAttackPlanSystem.scoutLine(day, wave, count);\n            List<String> airLines = new ArrayList<>();\n            aerialRoster.forEach((role, amount) -> airLines.add(\n                    role.displayName() + \" ×\" + amount + \" · \" + role.combatRole()));\n            int flyingCount = aerialRoster.values().stream().mapToInt(Integer::intValue).sum();\n            String air = airLines.isEmpty()\n                    ? \"없음\"\n                    : String.join(\" / \", airLines) + \" · 성벽 우회 · 대공 발사대/성루 명사수 권장\";\n            String elite = VillageEnemyEliteSystem.scoutSummary(day, Math.max(0, count - flyingCount));\n""", "intel role detail")
    write(path, text)


def patch_turrets() -> None:
    path = JAVA / "VillagePlacedTurretSystem.java"
    text = read(path)
    text = replace_once(text,
        "            case BOMBARD -> score += cluster * 34.0;\n",
        """            case BOMBARD -> {\n                score += cluster * 34.0;\n                score -= pendingBombardOverlapPenalty(mob.position());\n            }\n""", "bombard overlap score")
    text = replace_once(text,
        """            case ANTI_AIR -> {\n                if (flying) score += 420.0;\n                else if (archetype == VillageEnemyArchetypeSystem.Archetype.MARKSMAN\n""",
        """            case ANTI_AIR -> {\n                if (flying) score += 420.0 + VillageRaidSystem.aerialThreatPriority(mob);\n                else if (archetype == VillageEnemyArchetypeSystem.Archetype.MARKSMAN\n""", "anti air role priority")
    text = replace_once(text,
        """    private static float piercingMultiplier(Mob target) {\n""",
        """    private static double pendingBombardOverlapPenalty(Vec3 point) {\n        double penalty = 0.0;\n        for (PendingBombard shot : PENDING_BOMBARDS) {\n            double separation = shot.radius() + 2.0;\n            if (shot.impact().distanceToSqr(point) <= separation * separation) penalty += 190.0;\n        }\n        return Math.min(380.0, penalty);\n    }\n\n    private static float piercingMultiplier(Mob target) {\n""", "bombard overlap helper")
    write(path, text)


def patch_mercenary() -> None:
    path = JAVA / "VillageMercenarySystem.java"
    text = read(path)
    text = replace_once(text,
        """                .min(java.util.Comparator\n                        .comparingInt((Mob enemy) -> VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1)\n                        .thenComparingDouble(mercenary::distanceToSqr)).orElse(null);\n""",
        """                .min(java.util.Comparator\n                        .comparingInt((Mob enemy) -> VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1)\n                        .thenComparingInt(enemy -> -VillageRaidSystem.aerialThreatPriority(enemy))\n                        .thenComparingDouble(mercenary::distanceToSqr)).orElse(null);\n""", "mercenary aerial threat priority")
    write(path, text)


def patch_player_ranger() -> None:
    path = JAVA / "VillageRoleAbilitySystem.java"
    text = read(path)
    text = replace_once(text,
        """            if (event.getSource().getDirectEntity() instanceof AbstractArrow directArrow) {\n                EmpoweredArrowState rapid = RAPID_ARROWS.remove(directArrow.getUUID());\n                if (rapid != null) event.setAmount(event.getAmount() * rapid.power());\n            }\n            TimedScale rally = RALLY_SCALE.get(attacker.getUUID());\n""",
        """            if (event.getSource().getDirectEntity() instanceof AbstractArrow directArrow) {\n                EmpoweredArrowState rapid = RAPID_ARROWS.remove(directArrow.getUUID());\n                if (rapid != null) event.setAmount(event.getAmount() * rapid.power());\n                if (role == VillageRole.RANGER && event.getEntity() instanceof Mob target\n                        && VillageEnemyArchetypeSystem.isFlying(target)) {\n                    event.setAmount(event.getAmount() * 1.18f);\n                }\n            }\n            TimedScale rally = RALLY_SCALE.get(attacker.getUUID());\n""", "player ranger aerial damage")
    text = replace_once(text,
        """                    double miss = body.distanceToSqr(closest);\n                    return miss * 6.5 + to.lengthSqr() * 0.010;\n""",
        """                    double miss = body.distanceToSqr(closest);\n                    double aerialBias = VillageEnemyArchetypeSystem.isFlying(target) ? -18.0 : 0.0;\n                    return miss * 6.5 + to.lengthSqr() * 0.010 + aerialBias;\n""", "player ranger aerial aim bias")
    write(path, text)

    role_path = JAVA / "VillageRole.java"
    role = read(role_path)
    role = replace_once(role,
        '            "활 충전 시간이 짧아지고 조준이 적에게 보정되며 화살로 처치하면 사용 화살을 회수합니다.",\n',
        '            "활 충전 시간이 짧아지고 조준이 공중 위협을 우선 보정하며 공중 적에게 화살 피해가 18% 증가하고 처치 시 화살을 회수합니다.",\n',
        "ranger passive truth")
    write(role_path, role)


def patch_historical_test() -> None:
    path = ROOT / "tools/test_v01827_aerial_combat_integrity.py"
    text = read(path)
    text = replace_once(text,
        '    assert "mod_version=0.18.27-alpha.1" in props\n    assert "0.18.27-alpha.1" in readme and "villageguardians-0.18.27-alpha.1.jar" in readme\n',
        '    assert "mod_version=0.18." in props\n    assert "현재 소스 버전 `0.18." in readme and "목표 JAR `villageguardians-0.18." in readme\n',
        "v01827 version independent")
    write(path, text)


def patch_metadata() -> None:
    props = ROOT / "gradle.properties"
    text = read(props)
    text = replace_once(text, "mod_version=0.18.27-alpha.1", "mod_version=0.18.28-alpha.1", "version bump")
    write(props, text)

    readme = ROOT / "README.md"
    text = read(readme)
    text = replace_once(text,
        "- 현재 소스 버전 `0.18.27-alpha.1`\n- 목표 JAR `villageguardians-0.18.27-alpha.1.jar`\n",
        "- 현재 소스 버전 `0.18.28-alpha.1`\n- 목표 JAR `villageguardians-0.18.28-alpha.1.jar`\n",
        "readme version")
    marker = "## 0.18.27 공중 습격 전투 완성도·정찰 정합\n"
    section = """## 0.18.28 공중 병종·대공 방어 생태계\n\n- 실제 비행 적을 하늘 약탈귀·파성 망령·폭풍 사냥귀 3교리로 분화했다. 7일차 기본 급습 이후 10일차부터 시설 폭격형, 13일차부터 고속 추격형이 섞이며 실제 스폰과 낮 정찰이 같은 결정 규칙을 사용한다.\n- 파성 망령은 플레이어에게 쉽게 끌려가지 않고 내부 시설을 우선 폭격한다. 폭풍 사냥귀는 더 넓게 수호자를 탐색하고 짧은 경고·빠른 급강하·짧은 회복 주기로 압박하지만 1회 피해와 판정 반경은 작다.\n- 지상 정예 교리 시스템이 비행 적에게 갈고리/암살/기병 이동 AI를 다시 덮어쓸 수 있던 0.18.27 잔여 소유권 충돌을 차단했다. 공중 이동은 끝까지 습격 시스템 하나가 소유한다.\n- 대공 발사대와 성루 명사수 용병은 단순히 가장 가까운 비행 적을 함께 두드리는 대신 시설 폭격형 → 고속 추격형 → 일반 급습형의 위협 우선순위를 공유한다.\n- 성루사수 플레이어의 조준 보정은 시야 원뿔 안 공중 적을 우선하며, 활 피해는 공중 적에게 18% 증가한다. 플레이어가 직접 대공 역할을 맡을 수 있게 했다.\n- 광역 투석포는 이미 다른 투석포의 12틱 지연 포격이 예약된 군집에 큰 감점을 줘, 여러 포탑이 같은 빈 좌표에 연속 포격하는 지연 오버킬을 줄였다.\n- 낮 정찰은 공중 적 총수뿐 아니라 세 공중 교리의 실제 예정 수량·역할을 따로 공개하며, 지상 정예 예상 수량에서도 공중 적을 제외한다.\n\n"""
    if marker not in text:
        raise SystemExit("missing patch anchor: readme v01827 section")
    text = text.replace(marker, section + marker, 1)
    write(readme, text)


def write_test() -> None:
    path = ROOT / "tools/test_v01828_air_defense_ecosystem.py"
    path.write_text('''#!/usr/bin/env python3\nfrom pathlib import Path\n\nROOT = Path(__file__).resolve().parents[1]\nJAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"\n\ndef read(name: str) -> str:\n    return (JAVA / name).read_text(encoding="utf-8")\n\ndef main() -> None:\n    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")\n    readme = (ROOT / "README.md").read_text(encoding="utf-8")\n    enemy = read("VillageEnemyArchetypeSystem.java")\n    raid = read("VillageRaidSystem.java")\n    elite = read("VillageEnemyEliteSystem.java")\n    intel = read("VillageWaveIntelSystem.java")\n    turret = read("VillagePlacedTurretSystem.java")\n    merc = read("VillageMercenarySystem.java")\n    ability = read("VillageRoleAbilitySystem.java")\n    role = read("VillageRole.java")\n    old = (ROOT / "tools/test_v01827_aerial_combat_integrity.py").read_text(encoding="utf-8")\n\n    assert "mod_version=0.18.28-alpha.1" in props\n    assert "0.18.28-alpha.1" in readme and "villageguardians-0.18.28-alpha.1.jar" in readme\n    assert 'assert "mod_version=0.18.27-alpha.1" in props' not in old\n    assert "enum AerialRole" in enemy\n    for token in ("RAIDER", "BOMBARDIER", "HARRIER", "public static AerialRole aerialRole"):\n        assert token in enemy\n    assert "day < 10" in enemy and "day >= 13" in enemy\n\n    assert "ACTIVE_AERIAL_ROLES" in raid\n    assert "ACTIVE_AERIAL_ROLES.put(mob.getUUID(), aerialRole)" in raid\n    assert "ACTIVE_AERIAL_ROLES.remove(uuid)" in raid and "ACTIVE_AERIAL_ROLES.clear()" in raid\n    assert "public static int aerialThreatPriority" in raid\n    assert "case BOMBARDIER -> 300" in raid and "case HARRIER -> 220" in raid\n    direct = raid.split("private static void directFlyingEnemy", 1)[1].split("private static void moveFlyingToward", 1)[0]\n    assert "role == VillageEnemyArchetypeSystem.AerialRole.BOMBARDIER" in direct\n    assert "aerialCadence(role)" in direct and "aerialWarningTicks(role)" in direct\n    assert "case BOMBARDIER -> 112" in direct and "case HARRIER -> 62" in direct\n    assert "case BOMBARDIER -> 1.75f" in direct and "case HARRIER -> 0.58f" in direct\n    assert "mob.setTarget(null);" in direct\n\n    discover = elite.split("private static void discover", 1)[1].split("private static void grappler", 1)[0]\n    assert "VillageEnemyArchetypeSystem.isFlying(mob)" in discover and "continue;" in discover\n\n    assert "Map<VillageEnemyArchetypeSystem.AerialRole, Integer> aerialRoster" in intel\n    assert "VillageEnemyArchetypeSystem.aerialRole(day, wave, index, trait)" in intel\n    assert "count - flyingCount" in intel\n    for label in ("파성 망령", "폭풍 사냥귀"):\n        assert label in enemy\n\n    assert "VillageRaidSystem.aerialThreatPriority(mob)" in turret\n    assert "pendingBombardOverlapPenalty" in turret and "penalty += 190.0" in turret\n    assert "-VillageRaidSystem.aerialThreatPriority(enemy)" in merc\n\n    assert "VillageEnemyArchetypeSystem.isFlying(target)" in ability\n    assert "event.getAmount() * 1.18f" in ability\n    assert "aerialBias" in ability and "? -18.0 : 0.0" in ability\n    assert "공중 적에게 화살 피해가 18% 증가" in role\n\n    print("[PASS] deterministic three-role aerial roster is shared by runtime and daytime intel")\n    print("[PASS] ground elite AI can no longer steal flying movement ownership")\n    print("[PASS] bombardier/harrier have distinct target, cadence, telegraph and damage identities")\n    print("[PASS] anti-air turret and ranger mercenary share aerial threat priority")\n    print("[PASS] player ranger has truthful aerial aim support and damage identity")\n    print("[PASS] delayed bombard overlap receives reservation-style target penalty")\n    print("[PASS] historical v0.18.27 regression is version-independent")\n    print("[PASS] v0.18.28 air-defense ecosystem contract complete")\n\nif __name__ == "__main__":\n    main()\n''', encoding="utf-8")


def main() -> None:
    patch_enemy()
    patch_raid()
    patch_elite()
    patch_intel()
    patch_turrets()
    patch_mercenary()
    patch_player_ranger()
    patch_historical_test()
    patch_metadata()
    write_test()
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
