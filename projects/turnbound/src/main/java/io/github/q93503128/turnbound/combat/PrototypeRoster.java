package io.github.q93503128.turnbound.combat;

import java.util.List;

public final class PrototypeRoster {
    private PrototypeRoster() {}

    public static CombatantDefinition kyren() {
        return new CombatantDefinition("P01", "카이렌", new BattleStats(900, 120, 85, 105), "p01_basic", List.of(
                new SkillDefinition("p01_basic", "추적 베기", TargetRule.ENEMY_SINGLE, 0,
                        List.of(SkillEffect.damage(1.00)),
                        "적 1명에게 ATK 100% 피해. 같은 적을 이어서 공격하면 카이렌의 집중이 쌓입니다."),
                new SkillDefinition("p01_shatter", "파쇄 일격", TargetRule.ENEMY_SINGLE, 2,
                        List.of(SkillEffect.damage(2.20)),
                        "적 1명에게 ATK 220%의 강한 피해. 보유한 집중이 많을수록 피해가 추가로 증폭됩니다."),
                new SkillDefinition("p01_duel_lock", "결투 고정", TargetRule.ENEMY_SINGLE, 3,
                        List.of(SkillEffect.selfGaugeAdd(120)),
                        "적 1명을 집중 대상으로 고정하고 집중을 쌓으며, 자신의 Turn Gauge를 120 당깁니다.")));
    }

    public static CombatantDefinition lumea() {
        return new CombatantDefinition("P02", "루메아", new BattleStats(780, 90, 75, 125), "p02_basic", List.of(
                new SkillDefinition("p02_basic", "가속", TargetRule.ALLY_SINGLE, 0,
                        List.of(SkillEffect.gaugeAdd(180)),
                        "아군 1명의 Turn Gauge를 180 증가시켜 다음 행동을 앞당깁니다."),
                new SkillDefinition("p02_time_leap", "시간 도약", TargetRule.ALLY_SINGLE, 4,
                        List.of(SkillEffect.gaugeAtLeast(1000)),
                        "자신을 제외한 아군 1명의 Turn Gauge를 최소 1000까지 올려 행동 가능 상태로 만듭니다."),
                new SkillDefinition("p02_delay_field", "지연장", TargetRule.ENEMY_ALL, 3,
                        List.of(SkillEffect.gaugeAdd(-120)),
                        "모든 적의 Turn Gauge를 120 감소시켜 적 전체의 행동을 늦춥니다.")));
    }

    public static CombatantDefinition bram() {
        return new CombatantDefinition("P03", "브람", new BattleStats(1250, 88, 130, 75), "p03_basic", List.of(
                new SkillDefinition("p03_basic", "방진", TargetRule.SELF, 0,
                        List.of(SkillEffect.barrier(0.12)),
                        "자신에게 MaxHP의 12%만큼 보호막을 생성합니다."),
                new SkillDefinition("p03_guard", "보호 전환", TargetRule.ALLY_SINGLE, 3,
                        List.of(SkillEffect.guardRedirect(0.70, 2)),
                        "아군 1명이 받는 직접 피해의 70%를 브람이 대신 받습니다. 대상의 2회 행동 동안 유지됩니다."),
                new SkillDefinition("p03_press", "방패 압박", TargetRule.ENEMY_SINGLE, 2,
                        List.of(SkillEffect.damage(0.90), SkillEffect.gaugeAdd(-120)),
                        "적 1명에게 ATK 90% 피해를 주고 Turn Gauge를 120 감소시킵니다.")));
    }

    public static CombatantDefinition elysia() {
        return new CombatantDefinition("P04", "엘리시아", new BattleStats(820, 105, 70, 95), "p04_basic", List.of(
                new SkillDefinition("p04_basic", "치유", TargetRule.ALLY_SINGLE, 0,
                        List.of(SkillEffect.heal(0.70)),
                        "살아있는 아군 1명의 HP를 엘리시아 ATK의 70%만큼 회복합니다."),
                new SkillDefinition("p04_revive", "되돌아온 숨", TargetRule.DEAD_ALLY_SINGLE, 5,
                        List.of(SkillEffect.revive(0.30)),
                        "전투불능 아군 1명을 대상 MaxHP의 30% HP로 부활시킵니다."),
                new SkillDefinition("p04_rest_light", "안식의 빛", TargetRule.ALLY_ALL, 3,
                        List.of(SkillEffect.heal(0.90)),
                        "모든 살아있는 아군을 회복합니다. 다른 아군은 ATK 90%, 엘리시아 자신은 ATK 70%만큼 회복합니다.")));
    }

    public static CombatantDefinition corruptedWalker() {
        return new CombatantDefinition("E001", "부패 보행자", new BattleStats(720, 92, 68, 82), "e001_basic", List.of(
                new SkillDefinition("e001_basic", "썩은 주먹", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00)))));
    }

    public static CombatantDefinition boneArcher() {
        return new CombatantDefinition("E002", "뼈 사수", new BattleStats(560, 100, 54, 105), "e002_basic", List.of(
                new SkillDefinition("e002_basic", "뼈 화살", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(0.95))),
                new SkillDefinition("e002_aimed", "조준 사격", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(1.45)))));
    }

    public static CombatantDefinition fieldMedic() {
        return new CombatantDefinition("E005", "야전 치유사", new BattleStats(590, 82, 60, 94), "e005_basic", List.of(
                new SkillDefinition("e005_basic", "응급 봉합", TargetRule.ALLY_SINGLE, 0, List.of(SkillEffect.heal(0.55))),
                new SkillDefinition("e005_reform", "전열 정비", TargetRule.ALLY_ALL, 3, List.of(SkillEffect.defenseUp(0.15, 2)))));
    }

    public static CombatantDefinition swordEnemy(String id, String name) {
        String basic = id.toLowerCase() + "_basic";
        return new CombatantDefinition(id, name, new BattleStats(700, 95, 70, 90), basic, List.of(
                new SkillDefinition(basic, "베기", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00)))));
    }

    public static CombatantDefinition archerEnemy() {
        return new CombatantDefinition("E_ARCHER", "훈련 궁수", new BattleStats(560, 105, 55, 105), "e_archer_basic", List.of(
                new SkillDefinition("e_archer_basic", "속사", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(0.95))),
                new SkillDefinition("e_archer_active", "약점 사격", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(1.45)))));
    }

    public static CombatantDefinition shieldEnemy() {
        return new CombatantDefinition("E_SHIELD", "훈련 방패병", new BattleStats(950, 75, 125, 70), "e_shield_basic", List.of(
                new SkillDefinition("e_shield_basic", "방패 타격", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(0.80))),
                new SkillDefinition("e_shield_active", "수비 전개", TargetRule.ALLY_SINGLE, 2, List.of(SkillEffect.barrier(0.20)))));
    }

    public static CombatantDefinition shamanEnemy() {
        return new CombatantDefinition("E_SHAMAN", "훈련 주술사", new BattleStats(620, 90, 65, 95), "e_shaman_basic", List.of(
                new SkillDefinition("e_shaman_basic", "회복 주술", TargetRule.ALLY_SINGLE, 0, List.of(SkillEffect.heal(0.50))),
                new SkillDefinition("e_shaman_active", "전투 주술", TargetRule.ALLY_ALL, 3, List.of(SkillEffect.gaugeAdd(0)))));
    }

    public static CombatantDefinition trainingEnemy(String id, String name, int hp, int attack, int defense, int speed) {
        String basic = id.toLowerCase() + "_basic";
        return new CombatantDefinition(id, name, new BattleStats(hp, attack, defense, speed), basic, List.of(
                new SkillDefinition(basic, "공격", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00)))));
    }
}
