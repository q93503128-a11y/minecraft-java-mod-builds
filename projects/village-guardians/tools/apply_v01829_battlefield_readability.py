#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing patch anchor: {label}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, new_block: str, label: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        raise SystemExit(f"missing patch start: {label}")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise SystemExit(f"missing patch end: {label}")
    return text[:start_index] + new_block + text[end_index:]


def patch_file(path: Path, transform) -> None:
    text = path.read_text(encoding="utf-8")
    updated = transform(text)
    if updated == text:
        raise SystemExit(f"no changes produced for {path.name}")
    path.write_text(updated, encoding="utf-8")


def patch_props(text: str) -> str:
    return replace_once(text, "mod_version=0.18.28-alpha.1", "mod_version=0.18.29-alpha.1", "version")


def patch_readme(text: str) -> str:
    text = replace_once(text, "현재 소스 버전 `0.18.28-alpha.1`", "현재 소스 버전 `0.18.29-alpha.1`", "readme version")
    text = replace_once(text, "목표 JAR `villageguardians-0.18.28-alpha.1.jar`", "목표 JAR `villageguardians-0.18.29-alpha.1.jar`", "readme jar")
    anchor = "## 0.18.28 공중 병종·대공 방어 생태계\n"
    section = """## 0.18.29 복합 전장 가독성·공중 경고 정합

- 0.18.28에서 공중 병종별 실제 경고 시간이 12/18/24틱, 플레이어 타격 반경이 2.15/2.75/3.10블록으로 갈라졌는데 월드 경고 메시는 여전히 18틱·2.35블록 고정이던 판정/연출 불일치를 제거했다. 이제 경고 지속시간과 주 위험 원이 실제 회피 판정과 같은 값을 사용한다.
- 하늘 약탈귀·파성 망령·폭풍 사냥귀의 공중 경고를 서로 다른 색·마커 밀도·회전 언어로 분리했다. 시설 폭격 표식과 플레이어 급강하 표식도 같은 메시를 억지로 공유하지 않고 구조물 여부를 함께 인코딩한다.
- 낮 정찰과 웨이브 도착 신호의 지상 주공/별동대 계산에서 성벽을 우회하는 공중 병력을 제외했다. 비행 병력 때문에 존재하지 않는 지상 별동대 방향에 성벽 경보가 뜨거나 주공 수가 부풀던 문제를 막았다.
- 최대 100개 활성 적이 전부 체력+병과 이름표를 항상 띄우던 전장 텍스트 과밀을 정리했다. 보스·공중 병력·공병·주술/지원·포탑 교란 등 전술 표적은 항상 표시하고, 일반 전열은 수호자 22블록 안에서만 체력 이름표가 보인다.
- 시야 밖의 모든 일반 적을 벽 너머 발광시키던 전역 아웃라인을 폐기했다. 보스와 전술 위협만 가려졌을 때 강조되어 성벽 너머가 수십 개 실루엣으로 도배되지 않으면서도 반드시 찾아야 할 표적은 놓치지 않는다.

"""
    return replace_once(text, anchor, section + anchor, "readme v01829 section")


def patch_enemy(text: str) -> str:
    text = replace_once(
        text,
        "        mob.setCustomNameVisible(true);",
        "        mob.setCustomNameVisible(alwaysShowNameplate(archetype, boss, isFlying(mob)));",
        "initial nameplate policy",
    )
    anchor = """    public static boolean isBoss(Archetype archetype) {
        return archetype.ordinal() >= Archetype.SIEGE_BEAST.ordinal();
    }
"""
    addition = anchor + """

    public static boolean isTacticalThreat(Archetype archetype) {
        if (archetype == null) return false;
        return switch (archetype) {
            case SAPPER, SHIELDBREAKER, HEXER, WAR_CHANTER, NECROMANCER, TOWER_HUNTER -> true;
            default -> isBoss(archetype);
        };
    }

    public static boolean alwaysShowNameplate(Archetype archetype, boolean boss, boolean flying) {
        return flying || boss || isTacticalThreat(archetype);
    }
"""
    return replace_once(text, anchor, addition, "tactical threat policy")


def patch_health(text: str) -> str:
    text = replace_once(
        text,
        "            mob.setCustomNameVisible(true);",
        "            mob.setCustomNameVisible(shouldShowEnemyNameplate(server, mob));",
        "health nameplate visibility",
    )
    anchor = "    private static void removeLegacyHealthTeams(MinecraftServer server) {\n"
    helper = """    private static boolean shouldShowEnemyNameplate(MinecraftServer server, Mob mob) {
        VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);
        if (VillageEnemyArchetypeSystem.alwaysShowNameplate(
                archetype, VillageRaidSystem.isBossEnemy(mob), VillageEnemyArchetypeSystem.isFlying(mob))) {
            return true;
        }
        double nearbySquared = 22.0 * 22.0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;
            if (player.distanceToSqr(mob) <= nearbySquared) return true;
        }
        return false;
    }

"""
    return replace_once(text, anchor, helper + anchor, "health nameplate helper")


def patch_raid(text: str) -> str:
    old_priority = """    public static int aerialThreatPriority(Mob mob) {
        VillageEnemyArchetypeSystem.AerialRole role = aerialRoleOf(mob);
        if (role == null) return 0;
        return switch (role) {
            case BOMBARDIER -> 300;
            case HARRIER -> 220;
            case RAIDER -> 140;
        };
    }
"""
    new_priority = """    public static int aerialThreatPriority(Mob mob) {
        return aerialThreatPriority(aerialRoleOf(mob));
    }

    public static int aerialThreatPriority(VillageEnemyArchetypeSystem.AerialRole role) {
        if (role == null) return 0;
        return switch (role) {
            case BOMBARDIER -> 300;
            case HARRIER -> 220;
            case RAIDER -> 140;
        };
    }
"""
    text = replace_once(text, old_priority, new_priority, "aerial role priority overload")

    warning_anchor = """    private static int aerialWarningTicks(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 24;
            case HARRIER -> 12;
            case RAIDER -> AERIAL_WARNING_TICKS;
        };
    }
"""
    warning_addition = warning_anchor + """

    private static double aerialStrikeRadius(VillageEnemyArchetypeSystem.AerialRole role) {
        return switch (role) {
            case BOMBARDIER -> 3.10;
            case HARRIER -> 2.15;
            case RAIDER -> AERIAL_PLAYER_STRIKE_RADIUS;
        };
    }
"""
    text = replace_once(text, warning_anchor, warning_addition, "aerial strike radius")

    old_begin = """        AERIAL_STRIKES.put(mob.getUUID(), strike);
        VillageDefenseEffectSystem.aerialAssaultWarning(level, point, building != null);
        Vec3 dive = point.add(0.0, building == null ? 3.0 : 4.5, 0.0);
"""
    new_begin = """        AERIAL_STRIKES.put(mob.getUUID(), strike);
        double dangerRadius = building == null ? aerialStrikeRadius(role) : 0.0;
        VillageDefenseEffectSystem.aerialAssaultWarning(
                level, point, role, building != null, aerialWarningTicks(role), dangerRadius);
        Vec3 dive = point.add(0.0, building == null ? 3.0 : 4.5, 0.0);
"""
    text = replace_once(text, old_begin, new_begin, "aerial warning truth")

    text = replace_once(
        text,
        "            VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), true);",
        "            VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), role, true, 0.0);",
        "structure aerial impact role",
    )
    old_radius = """        double radius = switch (role) {
            case BOMBARDIER -> 3.10;
            case HARRIER -> 2.15;
            case RAIDER -> AERIAL_PLAYER_STRIKE_RADIUS;
        };
"""
    text = replace_once(text, old_radius, "        double radius = aerialStrikeRadius(role);\n", "shared aerial radius")
    text = replace_once(
        text,
        "        VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), false);",
        "        VillageDefenseEffectSystem.aerialAssaultImpact(level, strike.point(), role, false, radius);",
        "player aerial impact role",
    )

    old_outline = "        mob.setGlowingTag(isBossEnemy(mob) || !visibleToAnyPlayer);"
    new_outline = """        VillageEnemyArchetypeSystem.Archetype archetype = archetypeOf(mob);
        boolean tactical = VillageEnemyArchetypeSystem.isFlying(mob)
                || VillageEnemyArchetypeSystem.isTacticalThreat(archetype);
        mob.setGlowingTag(isBossEnemy(mob) || (tactical && !visibleToAnyPlayer));"""
    return replace_once(text, old_outline, new_outline, "occluded tactical outline")


def patch_effects(text: str) -> str:
    old = """    public static void aerialAssaultWarning(ServerLevel level, Vec3 center, boolean structure) {
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
    new = """    public static void aerialAssaultWarning(
            ServerLevel level, Vec3 center, VillageEnemyArchetypeSystem.AerialRole role,
            boolean structure, int warningTicks, double radius) {
        if (level == null || center == null || role == null) return;
        VillageSkillEffectEntity.spawn(level, null, "raid_aerial_warning", center,
                new Vec3(0.0, 0.0, 1.0), Math.max(6, warningTicks), 0.0f,
                aerialSignalExtra(role, structure, radius));
    }

    public static void aerialAssaultImpact(
            ServerLevel level, Vec3 center, VillageEnemyArchetypeSystem.AerialRole role,
            boolean structure, double radius) {
        if (level == null || center == null || role == null) return;
        int duration = switch (role) {
            case BOMBARDIER -> 18;
            case HARRIER -> 12;
            case RAIDER -> 16;
        };
        VillageSkillEffectEntity.spawn(level, null, "raid_aerial_impact", center,
                new Vec3(0.0, 0.0, 1.0), duration, 0.0f,
                aerialSignalExtra(role, structure, radius));
    }

    private static String aerialSignalExtra(
            VillageEnemyArchetypeSystem.AerialRole role, boolean structure, double radius) {
        return String.format(Locale.ROOT, "%d|%d|%.2f", role.ordinal(), structure ? 1 : 0,
                Math.max(0.0, radius));
    }
"""
    return replace_once(text, old, new, "role truthful aerial effect payload")


def patch_attack_plan(text: str) -> str:
    old_preview = """        for (int index = 0; index < Math.max(0, count); index++) {
            counts.merge(frontForIndex(day, wave, index), 1, Integer::sum);
        }
"""
    new_preview = """        for (int index = 0; index < Math.max(0, count); index++) {
            if (!isGroundAssaultIndex(day, wave, count, index)) continue;
            counts.merge(frontForIndex(day, wave, index), 1, Integer::sum);
        }
"""
    text = replace_once(text, old_preview, new_preview, "ground-only attack plan preview")

    old_used = """    private static Map<Front, Boolean> usedFronts(int day, int wave, int count) {
        Map<Front, Boolean> used = new HashMap<>();
        for (int i = 0; i < Math.max(0, count); i++) used.put(frontForIndex(day, wave, i), true);
        return used;
    }
"""
    new_used = """    private static Map<Front, Boolean> usedFronts(int day, int wave, int count) {
        Map<Front, Boolean> used = new HashMap<>();
        for (int i = 0; i < Math.max(0, count); i++) {
            if (!isGroundAssaultIndex(day, wave, count, i)) continue;
            used.put(frontForIndex(day, wave, i), true);
        }
        return used;
    }

    private static boolean isGroundAssaultIndex(int day, int wave, int count, int index) {
        VillageWaveTrait trait = VillageWaveTrait.select(day, wave);
        int bosses = VillageRaidSystem.previewBossCount(
                day, wave, VillageRaidSystem.previewMaxWaves(day), Math.max(0, count));
        boolean boss = index >= 0 && index < bosses;
        return !VillageEnemyArchetypeSystem.willSpawnFlying(day, wave, index, boss, trait);
    }
"""
    return replace_once(text, old_used, new_used, "ground-only arrival fronts")


def patch_mesh(text: str) -> str:
    new_block = """    private static void renderAerialAssault(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra, boolean impact) {
        AerialSignal signal = parseAerialSignal(extra);
        int role = signal.role();
        boolean structure = signal.structure();
        double fade = Math.max(0.0, 1.0 - progress);
        double dangerRadius = structure
                ? (role == 1 ? 2.65 : role == 2 ? 1.75 : 2.15)
                : Math.max(0.6, signal.radius());
        int primary = switch (role) {
            case 1 -> rgba(139, 121, 255, (int) ((impact ? 245 : 215) * Math.max(0.28, fade)));
            case 2 -> rgba(91, 232, 255, (int) ((impact ? 250 : 225) * Math.max(0.28, fade)));
            default -> rgba(116, 207, 255, (int) ((impact ? 238 : 205) * Math.max(0.28, fade)));
        };
        int secondary = switch (role) {
            case 1 -> rgba(231, 218, 255, (int) (160 * Math.max(0.22, fade)));
            case 2 -> rgba(246, 255, 173, (int) (155 * Math.max(0.22, fade)));
            default -> rgba(225, 250, 255, (int) (150 * Math.max(0.22, fade)));
        };

        // The inner warning ring is the actual dodge radius for player-targeted strikes.
        // Animation is kept on secondary geometry so the gameplay boundary never lies.
        double primaryRadius = impact ? dangerRadius + progress * (role == 1 ? 3.4 : role == 2 ? 2.2 : 2.8) : dangerRadius;
        ring(pose, out, b, primaryRadius, 0.055, impact ? 0.15 : 0.105,
                role == 1 ? 64 : 56, primary, age * (role == 2 ? 0.09 : 0.045));
        double pulse = 0.92 + Math.sin(age * (role == 2 ? 0.48 : 0.28)) * 0.07;
        ring(pose, out, b, Math.max(0.55, dangerRadius * 0.66 * pulse), 0.085, 0.05,
                44, secondary, -age * (role == 2 ? 0.12 : 0.065));

        int markers = role == 1 ? 8 : role == 2 ? 3 : 4;
        for (int i = 0; i < markers; i++) {
            double a = i * TAU / markers + age * (role == 2 ? 0.085 : role == 1 ? -0.025 : 0.025);
            double markerRadius = Math.max(0.65, dangerRadius * (role == 1 ? 0.92 : 0.84));
            double rise = impact ? 0.18 + progress * (role == 2 ? 0.48 : 0.65) : 0.12;
            chevron(pose, out, b, a, markerRadius, rise,
                    role == 1 ? 0.42 : role == 2 ? 0.28 : 0.34, primary);
        }
        if (role == 1) {
            ring(pose, out, b, dangerRadius * 0.82, 0.16, 0.035, 40,
                    withAlpha(secondary, Math.max(28, (int) (125 * fade))), age * 0.03);
            verticalPillar(pose, out, b, impact ? 0.55 : 0.24,
                    impact ? 5.2 * fade + 0.5 : 3.6, withAlpha(primary, impact ? 195 : 120));
        } else if (role == 2) {
            verticalPillar(pose, out, b, impact ? 0.30 : 0.12,
                    impact ? 3.2 * fade + 0.35 : 2.1, withAlpha(primary, impact ? 175 : 92));
        } else {
            verticalPillar(pose, out, b, impact ? 0.42 : 0.18,
                    impact ? 4.2 * fade + 0.4 : 2.8, withAlpha(primary, impact ? 185 : 105));
        }
    }

    private static AerialSignal parseAerialSignal(String extra) {
        int role = 0;
        boolean structure = false;
        double radius = 2.75;
        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split("\\\\|", -1);
            try { role = Math.max(0, Math.min(2, Integer.parseInt(parts[0]))); }
            catch (NumberFormatException ignored) {}
            structure = parts.length > 1 && "1".equals(parts[1]);
            if (parts.length > 2) {
                try { radius = Math.max(0.0, Double.parseDouble(parts[2])); }
                catch (NumberFormatException ignored) {}
            }
        }
        return new AerialSignal(role, structure, radius);
    }

    private record AerialSignal(int role, boolean structure, double radius) {}

"""
    text = replace_between(
        text,
        "    private static void renderAerialAssault(",
        "    private static void renderRaidFrontSignal(",
        new_block,
        "aerial mesh role/radius truth",
    )
    return text


def patch_old_test(text: str) -> str:
    text = replace_once(
        text,
        '    assert "mod_version=0.18.28-alpha.1" in props\n',
        '    assert "mod_version=" in props\n',
        "v01828 historical props",
    )
    return replace_once(
        text,
        '    assert "0.18.28-alpha.1" in readme and "villageguardians-0.18.28-alpha.1.jar" in readme\n',
        '    assert "현재 소스 버전" in readme and "villageguardians-" in readme\n',
        "v01828 historical readme",
    )


def main() -> None:
    patch_file(ROOT / "gradle.properties", patch_props)
    patch_file(ROOT / "README.md", patch_readme)
    patch_file(JAVA / "VillageEnemyArchetypeSystem.java", patch_enemy)
    patch_file(JAVA / "VillageHealthDisplaySystem.java", patch_health)
    patch_file(JAVA / "VillageRaidSystem.java", patch_raid)
    patch_file(JAVA / "VillageDefenseEffectSystem.java", patch_effects)
    patch_file(JAVA / "VillageAttackPlanSystem.java", patch_attack_plan)
    patch_file(JAVA / "VillageSkillMeshLibrary.java", patch_mesh)
    patch_file(ROOT / "tools/test_v01828_air_defense_ecosystem.py", patch_old_test)
    Path(__file__).unlink()


if __name__ == "__main__":
    main()
