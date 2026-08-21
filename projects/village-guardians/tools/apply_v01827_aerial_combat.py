#!/usr/bin/env python3
"""Apply the v0.18.27 authored aerial-combat integration patch reproducibly."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"missing patch anchor: {label}")
    return text.replace(old, new, 1)


def main() -> None:
    props = ROOT / "gradle.properties"
    text = read(props)
    if "mod_version=0.18.27-alpha.1" not in text:
        text = replace_once(text, "mod_version=0.18.26-alpha.1", "mod_version=0.18.27-alpha.1", "version")
    write(props, text)

    readme = ROOT / "README.md"
    text = read(readme)
    if "- 현재 소스 버전 `0.18.27-alpha.1`" not in text:
        text = replace_once(text, "- 현재 소스 버전 `0.18.26-alpha.1`", "- 현재 소스 버전 `0.18.27-alpha.1`", "readme source")
    if "- 목표 JAR `villageguardians-0.18.27-alpha.1.jar`" not in text:
        text = replace_once(text, "- 목표 JAR `villageguardians-0.18.26-alpha.1.jar`", "- 목표 JAR `villageguardians-0.18.27-alpha.1.jar`", "readme jar")
    section = """## 0.18.27 공중 습격 전투 완성도·정찰 정합

- 공중 적의 플레이어 공격을 바닐라 Phantom 공격 AI에 맡기면서 모드가 동시에 이동 목표를 덮어쓰던 이중 소유권을 제거했다. 하늘 약탈귀는 모드가 직접 순항 → 경고 → 급강하 타격 → 상승 이탈을 제어한다.
- 급강하 타격은 경고 시점의 위치를 고정해 18틱 뒤 작은 반경에만 피해를 준다. 플레이어가 경고 원 밖으로 빠지면 피할 수 있고, 시설 공습도 같은 고정 지점 경고 후 피해가 들어간다.
- 공중 습격의 경고/충돌은 서버 동기화 절차 메시로 표시해 실제 판정 위치와 시각 위치를 공유한다.
- 낮 정찰은 실제 스폰 판정과 같은 `willSpawnFlying` 규칙을 사용해 웨이브별 하늘 약탈귀 수를 따로 공개한다. 지상 병과로 잘못 표시되던 정찰 오차를 제거했다.
- 성루 명사수 용병은 사거리 안에 공중 적이 있으면 지상 최근접 적보다 먼저 사격해 대공 발사대의 보조 대공 전력으로 기능한다.

"""
    if "## 0.18.27 공중 습격 전투 완성도·정찰 정합" not in text:
        text = replace_once(text, "## 0.18.26 공중 습격·포탑 역할 분화\n", section + "## 0.18.26 공중 습격·포탑 역할 분화\n", "readme section")
    write(readme, text)

    enemy_path = JAVA / "VillageEnemyArchetypeSystem.java"
    enemy = read(enemy_path)
    enemy = replace_once(enemy,
        """    public static boolean isFlying(Mob mob) {
        return mob != null && mob.getType() == EntityTypes.PHANTOM;
    }

    private static boolean shouldSpawnFlying(int day, int wave, int index, VillageWaveTrait trait) {
""",
        """    public static boolean isFlying(Mob mob) {
        return mob != null && mob.getType() == EntityTypes.PHANTOM;
    }

    /** Shared deterministic predicate used by both the real spawn path and daytime intelligence. */
    public static boolean willSpawnFlying(
            int day, int wave, int index, boolean boss, VillageWaveTrait trait) {
        return !boss && shouldSpawnFlying(day, wave, index, trait);
    }

    private static boolean shouldSpawnFlying(int day, int wave, int index, VillageWaveTrait trait) {
""", "flying preview predicate")
    enemy = replace_once(enemy,
        "        boolean flying = !boss && shouldSpawnFlying(day, wave, index, trait);",
        "        boolean flying = willSpawnFlying(day, wave, index, boss, trait);",
        "spawn uses shared predicate")
    write(enemy_path, enemy)

    intel_path = JAVA / "VillageWaveIntelSystem.java"
    intel = read(intel_path)
    intel = replace_once(intel,
        """            List<String> bossLines = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                boolean boss = index < bosses;
                VillageEnemyArchetypeSystem.Archetype archetype =
                        VillageEnemyArchetypeSystem.previewArchetype(day, wave, index, boss, trait);
""",
        """            List<String> bossLines = new ArrayList<>();
            int flying = 0;
            for (int index = 0; index < count; index++) {
                boolean boss = index < bosses;
                if (VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait)) {
                    flying++;
                    continue;
                }
                VillageEnemyArchetypeSystem.Archetype archetype =
                        VillageEnemyArchetypeSystem.previewArchetype(day, wave, index, boss, trait);
""", "intel counts real flying roster")
    intel = replace_once(intel,
        """            String direction = VillageAttackPlanSystem.scoutLine(day, wave, count);
            String elite = VillageEnemyEliteSystem.scoutSummary(day, count);
""",
        """            String direction = VillageAttackPlanSystem.scoutLine(day, wave, count);
            String air = flying <= 0
                    ? "없음"
                    : "하늘 약탈귀 ×" + flying + " · 성벽 우회 · 대공 발사대/성루 명사수 권장";
            String elite = VillageEnemyEliteSystem.scoutSummary(day, count);
""", "intel air summary")
    intel = replace_once(intel,
        """                    + "\n" + direction
                    + "\n공성 병과: "
""",
        """                    + "\n" + direction
                    + "\n공중 위협: " + air
                    + "\n공성 병과: "
""", "intel air detail")
    intel = replace_once(intel,
        """                + players + "명\n주공·별동대·전장 상황·웨이브 특성·병과·수량은 낮에 미리 공개됩니다."
""",
        """                + players + "명\n주공·별동대·공중 위협·전장 상황·웨이브 특성·병과·수량은 낮에 미리 공개됩니다."
""", "intel report")
    write(intel_path, intel)

    merc_path = JAVA / "VillageMercenarySystem.java"
    merc = read(merc_path)
    merc = replace_once(merc,
        """                .stream().filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))
                .min(java.util.Comparator.comparingDouble(mercenary::distanceToSqr)).orElse(null);
""",
        """                .stream().filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))
                .min(java.util.Comparator
                        .comparingInt((Mob enemy) -> VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1)
                        .thenComparingDouble(mercenary::distanceToSqr)).orElse(null);
""", "ranger air priority")
    merc = replace_once(merc,
        '        RANGER("ranger", "성루 명사수", "원거리에서 적을 자동 사격하며 후방을 지원합니다."),',
        '        RANGER("ranger", "성루 명사수", "원거리에서 적을 자동 사격하며 공중 위협을 우선 요격합니다."),',
        "ranger description")
    write(merc_path, merc)

    effects_path = JAVA / "VillageDefenseEffectSystem.java"
    effects = read(effects_path)
    methods = """    public static void aerialAssaultWarning(ServerLevel level, Vec3 center, boolean structure) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_aerial_warning", center,
                new Vec3(0.0, 0.0, 1.0), 18, 0.0f, structure ? "1" : "0");
    }

    public static void aerialAssaultImpact(ServerLevel level, Vec3 center, boolean structure) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_aerial_impact", center,
                new Vec3(0.0, 0.0, 1.0), 16, 0.0f, structure ? "1" : "0");
    }

"""
    if "public static void aerialAssaultWarning" not in effects:
        effects = replace_once(effects, "    public static void raidFrontWarning(ServerLevel level, Vec3 center, boolean mainFront) {\n", methods + "    public static void raidFrontWarning(ServerLevel level, Vec3 center, boolean mainFront) {\n", "aerial effects")
    write(effects_path, effects)

    mesh_path = JAVA / "VillageSkillMeshLibrary.java"
    mesh = read(mesh_path)
    mesh = replace_once(mesh,
        """            case "raid_front_warning" -> renderRaidFrontSignal(pose, out, basis, age, progress, state.extra, false);
            case "raid_front_arrival" -> renderRaidFrontSignal(pose, out, basis, age, progress, state.extra, true);
""",
        """            case "raid_front_warning" -> renderRaidFrontSignal(pose, out, basis, age, progress, state.extra, false);
            case "raid_front_arrival" -> renderRaidFrontSignal(pose, out, basis, age, progress, state.extra, true);
            case "raid_aerial_warning" -> renderAerialAssault(pose, out, basis, age, progress, state.extra, false);
            case "raid_aerial_impact" -> renderAerialAssault(pose, out, basis, age, progress, state.extra, true);
""", "aerial mesh switch")
    mesh_method = """    private static void renderAerialAssault(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra, boolean impact) {
        boolean structure = "1".equals(extra);
        double fade = Math.max(0.0, 1.0 - progress);
        double pulse = 0.92 + Math.sin(age * 0.32) * 0.08;
        int primary = structure
                ? rgba(116, 207, 255, (int) ((impact ? 235 : 205) * Math.max(0.28, fade)))
                : rgba(105, 237, 255, (int) ((impact ? 245 : 220) * Math.max(0.28, fade)));
        int secondary = rgba(225, 250, 255, (int) (150 * Math.max(0.22, fade)));
        double radius = impact ? 1.15 + progress * 3.8 : 2.35 * pulse;
        ring(pose, out, b, radius, 0.055, impact ? 0.15 : 0.095, 56, primary, age * 0.055);
        ring(pose, out, b, Math.max(0.6, radius * 0.62), 0.085, 0.05, 44, secondary, -age * 0.07);
        for (int i = 0; i < 4; i++) {
            double a = i * TAU / 4.0 + (impact ? -age * 0.03 : age * 0.025);
            chevron(pose, out, b, a, Math.max(0.7, radius * 0.84),
                    impact ? 0.18 + progress * 0.65 : 0.12, impact ? 0.42 : 0.34, primary);
        }
        verticalPillar(pose, out, b, impact ? 0.42 : 0.18,
                impact ? 4.2 * fade + 0.4 : 2.8, withAlpha(primary, impact ? 185 : 105));
    }

"""
    if "private static void renderAerialAssault" not in mesh:
        mesh = replace_once(mesh, "    private static void renderRaidFrontSignal(\n", mesh_method + "    private static void renderRaidFrontSignal(\n", "aerial mesh method")
    write(mesh_path, mesh)

    raid_path = JAVA / "VillageRaidSystem.java"
    raid = read(raid_path)
    raid = replace_once(raid, "import net.minecraft.world.entity.Mob;\n", "import net.minecraft.world.entity.Mob;\nimport net.minecraft.world.entity.ai.attributes.Attributes;\n", "raid attributes import")
    raid = replace_once(raid,
        "    private static final Map<UUID, Integer> ACTIVE_WAVES = new HashMap<>();\n",
        "    private static final Map<UUID, Integer> ACTIVE_WAVES = new HashMap<>();\n    private static final Map<UUID, AerialStrike> AERIAL_STRIKES = new HashMap<>();\n",
        "aerial strike map")
    raid = replace_once(raid,
        "    private static final int STRUCTURE_ATTACK_INTERVAL = 30;\n",
        "    private static final int STRUCTURE_ATTACK_INTERVAL = 30;\n    private static final int AERIAL_ASSAULT_CADENCE = 90;\n    private static final int AERIAL_WARNING_TICKS = 18;\n    private static final int AERIAL_RECOVERY_TICKS = 34;\n    private static final double AERIAL_PLAYER_STRIKE_RADIUS = 2.75;\n",
        "aerial constants")

    old_flying = """    private static void directFlyingEnemy(
            MinecraftServer server, ServerLevel level, Mob mob,
            VillageEnemyArchetypeSystem.Archetype archetype, BlockPos villageCenter) {
        if (villageCenter == null) return;
        ServerPlayer player = nearestFlyingPriorityPlayer(server, mob);
        if (player != null) {
            mob.setTarget(player);
            mob.getLookControl().setLookAt(player, 45.0f, 45.0f);
            mob.getMoveControl().setWantedPosition(player.getX(), player.getY() + 2.5, player.getZ(), 1.28);
            return;
        }
        mob.setTarget(null);
        VillageProgressionSystem.Building targetBuilding = chooseTarget(
                villageCenter, mob.blockPosition(), true, archetype);
        if (targetBuilding == null || targetBuilding == VillageProgressionSystem.Building.WALLS) return;
        BlockPos target = VillageWorldSystem.buildingCenter(targetBuilding);
        mob.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5);
        mob.getMoveControl().setWantedPosition(target.getX() + 0.5, target.getY() + 9.0, target.getZ() + 0.5, 1.18);
        boolean attackTick = Math.floorMod(structureAttackTicks + mob.getUUID().hashCode(), STRUCTURE_ATTACK_INTERVAL) == 0;
        if (!attackTick || mob.position().distanceToSqr(Vec3.atCenterOf(target)) > 14.0 * 14.0) return;
        int day = VillageCouncilState.currentDay();
        float multiplier = currentTrait.structureDamageMultiplier()
                * VillageWarfrontSystem.structureDamageMultiplier(day)
                * VillageBossAspectSystem.structureMultiplier(mob)
                * VillageDifficultyTuning.earlyStructureMultiplier(day)
                * VillageDifficultyTuning.defenderStateStructureMultiplier(server);
        int damage = Math.max(1, Math.round((4 + wave + Math.min(16, day) * 0.45f) * multiplier));
        VillageProgressionSystem.damageBuilding(server, targetBuilding, damage);
        VillageDefenseEffectSystem.structureImpact(level, Vec3.atCenterOf(target), false);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                target.getX() + 0.5, target.getY() + 2.0, target.getZ() + 0.5,
                9, 0.6, 0.4, 0.6, 0.035);
    }
"""
    new_flying = """    private static void directFlyingEnemy(
            MinecraftServer server, ServerLevel level, Mob mob,
            VillageEnemyArchetypeSystem.Archetype archetype, BlockPos villageCenter) {
        if (villageCenter == null) return;
        UUID id = mob.getUUID();
        // Keep vanilla Phantom's dive target disabled: this system owns aerial movement and impact timing.
        mob.setTarget(null);

        AerialStrike strike = AERIAL_STRIKES.get(id);
        if (strike != null) {
            if (abilityTicks < strike.impactTick()) {
                Vec3 dive = strike.point().add(0.0, strike.targetsBuilding() ? 4.5 : 3.0, 0.0);
                moveFlyingToward(mob, strike.point(), dive, 1.52);
                return;
            }
            if (!strike.resolved()) {
                resolveAerialStrike(server, level, mob, strike);
                strike = strike.resolvedCopy();
                AERIAL_STRIKES.put(id, strike);
            }
            if (abilityTicks < strike.recoveryUntilTick()) {
                Vec3 recover = strike.point().add(0.0, strike.targetsBuilding() ? 12.5 : 11.0, 0.0);
                moveFlyingToward(mob, strike.point(), recover, 1.38);
                return;
            }
            AERIAL_STRIKES.remove(id);
        }

        ServerPlayer player = nearestFlyingPriorityPlayer(server, mob);
        int phase = Math.floorMod(abilityTicks + id.hashCode(), AERIAL_ASSAULT_CADENCE);
        if (player != null) {
            if (phase == 0) {
                beginAerialStrike(level, mob, player.position(), null);
                return;
            }
            double angle = abilityTicks * 0.055 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;
            double radius = 7.0 + Math.floorMod(id.hashCode(), 4);
            double altitude = 7.5 + Math.floorMod(id.hashCode() >>> 4, 4);
            Vec3 cruise = player.position().add(Math.cos(angle) * radius, altitude, Math.sin(angle) * radius);
            moveFlyingToward(mob, player.position().add(0.0, 1.0, 0.0), cruise, 1.20);
            return;
        }

        VillageProgressionSystem.Building targetBuilding = chooseTarget(
                villageCenter, mob.blockPosition(), true, archetype);
        if (targetBuilding == null || targetBuilding == VillageProgressionSystem.Building.WALLS) return;
        BlockPos targetBlock = VillageWorldSystem.buildingCenter(targetBuilding);
        Vec3 target = Vec3.atCenterOf(targetBlock).add(0.0, 1.0, 0.0);
        double angle = abilityTicks * 0.042 + Math.floorMod(id.hashCode(), 360) * Math.PI / 180.0;
        Vec3 cruise = target.add(Math.cos(angle) * 8.0, 9.5, Math.sin(angle) * 8.0);
        if (phase == 0 && mob.position().distanceToSqr(cruise) <= 22.0 * 22.0) {
            beginAerialStrike(level, mob, target, targetBuilding);
            return;
        }
        moveFlyingToward(mob, target, cruise, 1.16);
    }

    private static void beginAerialStrike(
            ServerLevel level, Mob mob, Vec3 point, VillageProgressionSystem.Building building) {
        int impactTick = abilityTicks + AERIAL_WARNING_TICKS;
        AerialStrike strike = new AerialStrike(point, building, impactTick,
                impactTick + AERIAL_RECOVERY_TICKS, false);
        AERIAL_STRIKES.put(mob.getUUID(), strike);
        VillageDefenseEffectSystem.aerialAssaultWarning(level, point, building != null);
        Vec3 dive = point.add(0.0, building == null ? 3.0 : 4.5, 0.0);
        moveFlyingToward(mob, point, dive, 1.52);
    }

    private static void resolveAerialStrike(
            MinecraftServer server, ServerLevel level, Mob mob, AerialStrike strike) {
        if (strike.targetsBuilding()) {
            VillageProgressionSystem.Building building = strike.building();
            if (building != null && VillageProgressionSystem.isOperational(building)) {
                int day = VillageCouncilState.currentDay();
                float multiplier = currentTrait.structureDamageMultiplier()
                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageBossAspectSystem.structureMultiplier(mob)
                        * VillageDifficultyTuning.earlyStructureMultiplier(day)
                        * VillageDifficultyTuning.defenderStateStructureMultiplier(server);
                int damage = Math.max(1, Math.round((4 + wave + Math.min(16, day) * 0.45f) * multiplier));
                VillageProgressionSystem.damageBuilding(server, building, damage);
            }
            VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), true);
            return;
        }

        double radiusSquared = AERIAL_PLAYER_STRIKE_RADIUS * AERIAL_PLAYER_STRIKE_RADIUS;
        float damage = Math.max(2.0f, (float) mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            if (player.position().distanceToSqr(strike.point()) <= radiusSquared) {
                player.hurtServer(level, level.damageSources().mobAttack(mob), damage);
            }
        }
        VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), false);
    }

    private static void moveFlyingToward(Mob mob, Vec3 lookAt, Vec3 wanted, double speed) {
        mob.getLookControl().setLookAt(lookAt.x, lookAt.y, lookAt.z, 45.0f, 45.0f);
        mob.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, speed);
    }
"""
    if "private static void beginAerialStrike" not in raid:
        raid = replace_once(raid, old_flying, new_flying, "authored aerial combat")
    raid = replace_once(raid,
        "        ACTIVE_ARCHETYPES.remove(uuid);\n        ACTIVE_WAVES.remove(uuid);\n",
        "        ACTIVE_ARCHETYPES.remove(uuid);\n        ACTIVE_WAVES.remove(uuid);\n        AERIAL_STRIKES.remove(uuid);\n",
        "release aerial state")
    raid = replace_once(raid,
        "        ACTIVE_ENEMIES.clear();\n        ACTIVE_ARCHETYPES.clear();\n        ACTIVE_WAVES.clear();\n",
        "        ACTIVE_ENEMIES.clear();\n        ACTIVE_ARCHETYPES.clear();\n        ACTIVE_WAVES.clear();\n        AERIAL_STRIKES.clear();\n",
        "clear aerial state")
    record = """    private record AerialStrike(
            Vec3 point, VillageProgressionSystem.Building building, int impactTick,
            int recoveryUntilTick, boolean resolved) {
        boolean targetsBuilding() { return building != null; }
        AerialStrike resolvedCopy() {
            return new AerialStrike(point, building, impactTick, recoveryUntilTick, true);
        }
    }

"""
    if "private record AerialStrike" not in raid:
        raid = replace_once(raid, "    private static void clearState() {\n", record + "    private static void clearState() {\n", "aerial strike record")
    write(raid_path, raid)

    old_test = ROOT / "tools/test_v01826_flying_turret_roles.py"
    test = read(old_test)
    if 'assert "mod_version=0.18.26-alpha.1" in props' in test:
        test = test.replace(
            '    assert "mod_version=0.18.26-alpha.1" in props\n    assert "0.18.26-alpha.1" in readme and "villageguardians-0.18.26-alpha.1.jar" in readme\n',
            '    assert "mod_version=" in props\n    assert "0.18.26 공중 습격·포탑 역할 분화" in readme\n', 1)
    write(old_test, test)

    test27 = ROOT / "tools/test_v01827_aerial_combat_integrity.py"
    test27.write_text(r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    enemy = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    merc = read("VillageMercenarySystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    old = (ROOT / "tools/test_v01826_flying_turret_roles.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.27-alpha.1" in props
    assert "0.18.27-alpha.1" in readme and "villageguardians-0.18.27-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.26-alpha.1" in props' not in old
    assert "public static boolean willSpawnFlying(" in enemy
    assert "boolean flying = willSpawnFlying(day, wave, index, boss, trait);" in enemy
    assert "VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait)" in intel
    assert "공중 위협: " in intel and "하늘 약탈귀 ×" in intel
    assert "private static final Map<UUID, AerialStrike> AERIAL_STRIKES" in raid
    direct = raid.split("private static void directFlyingEnemy", 1)[1].split("private static ServerPlayer nearestFlyingPriorityPlayer", 1)[0]
    assert "mob.setTarget(null);" in direct and "mob.setTarget(player)" not in direct
    assert "beginAerialStrike" in direct and "resolveAerialStrike" in direct
    assert "AERIAL_WARNING_TICKS = 18" in raid and "AERIAL_RECOVERY_TICKS = 34" in raid
    assert "player.position().distanceToSqr(strike.point()) <= radiusSquared" in raid
    assert "level.damageSources().mobAttack(mob)" in raid
    assert "AERIAL_STRIKES.remove(uuid);" in raid and "AERIAL_STRIKES.clear();" in raid
    assert "aerialAssaultWarning" in effects and "aerialAssaultImpact" in effects
    assert '"raid_aerial_warning"' in effects and '"raid_aerial_impact"' in effects
    assert 'case "raid_aerial_warning"' in mesh and 'case "raid_aerial_impact"' in mesh
    assert "renderAerialAssault" in mesh
    ranger = merc.split("private static void rangedAttack", 1)[1].split("private static void healAllies", 1)[0]
    assert "VillageEnemyArchetypeSystem.isFlying(enemy) ? 0 : 1" in ranger
    assert "공중 위협을 우선 요격" in merc
    print("[PASS] real spawn and daytime intel share one deterministic flying predicate")
    print("[PASS] flying combat has one authored owner with fixed warning/dive/impact/recovery phases")
    print("[PASS] player and structure aerial strikes resolve from fixed telegraphed positions")
    print("[PASS] aerial warning and impact use synchronized procedural meshes")
    print("[PASS] ranger mercenaries prioritize flying threats inside line of sight")
    print("[PASS] historical v0.18.26 regression is version-independent")
    print("[PASS] v0.18.27 aerial combat integrity contract complete")

if __name__ == "__main__":
    main()
''', encoding="utf-8")


if __name__ == "__main__":
    main()
