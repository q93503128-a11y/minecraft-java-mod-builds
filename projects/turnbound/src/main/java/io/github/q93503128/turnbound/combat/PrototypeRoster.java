package io.github.q93503128.turnbound.combat;

import java.util.List;
import java.util.Map;

/** Small deterministic fixtures retained for diagnostics. Production campaign combat uses CanonicalData. */
public final class PrototypeRoster {
    private PrototypeRoster() {}

    public static CombatantDefinition kyren() {
        return new CombatantDefinition("P01", "카이렌", new BattleStats(900, 120, 85, 105), "p01_chase_slash", List.of(
                new SkillDefinition("p01_chase_slash", "추적 베기", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00))),
                new SkillDefinition("p01_breaker_strike", "파쇄 일격", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(2.20))),
                new SkillDefinition("p01_duel_lock", "결투 고정", TargetRule.ENEMY_SINGLE, 3, List.of(SkillEffect.selfGaugeAdd(120)))),
                4, List.of("P01_FOCUS"), Map.of("focusMax", 3.0, "focusDamagePer", 0.15));
    }

    public static CombatantDefinition lumea() {
        return new CombatantDefinition("P02", "루메아", new BattleStats(780, 90, 75, 125), "p02_accelerate", List.of(
                new SkillDefinition("p02_accelerate", "가속", TargetRule.ALLY_SINGLE, 0, List.of(SkillEffect.gaugeAdd(180))),
                new SkillDefinition("p02_time_leap", "시간 도약", TargetRule.ALLY_SINGLE, 4, List.of(SkillEffect.gaugeAtLeast(1000)),
                        "자신을 제외한 아군의 Gauge를 최소 1000으로 만듭니다.", List.of("SELF_FORBIDDEN"), Map.of()),
                new SkillDefinition("p02_delay_field", "지연장", TargetRule.ENEMY_ALL, 3, List.of(SkillEffect.gaugeAdd(-120)))),
                5, List.of("P02_LATE_WAIT"), Map.of("slowAllyTurnGauge", 60.0));
    }

    public static CombatantDefinition bram() {
        return new CombatantDefinition("P03", "브람", new BattleStats(1250, 88, 130, 75), "p03_guard_stance", List.of(
                new SkillDefinition("p03_guard_stance", "방진", TargetRule.SELF, 0, List.of(SkillEffect.barrier(0.12))),
                new SkillDefinition("p03_guard_transfer", "보호 전환", TargetRule.ALLY_SINGLE, 3, List.of(SkillEffect.guardRedirect(0.70, 2)),
                        "다른 아군의 단일 직접 피해 70%를 대신 받습니다.", List.of("SELF_FORBIDDEN"), Map.of()),
                new SkillDefinition("p03_shield_pressure", "방패 압박", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(0.90), SkillEffect.gaugeAdd(-120)))),
                4, List.of("P03_COUNTER"), Map.of("counterPotency", 0.65));
    }

    public static CombatantDefinition elysia() {
        return new CombatantDefinition("P04", "엘리시아", new BattleStats(820, 105, 70, 95), "p04_heal", List.of(
                new SkillDefinition("p04_heal", "치유", TargetRule.ALLY_SINGLE, 0, List.of(SkillEffect.heal(0.70))),
                new SkillDefinition("p04_returned_breath", "되돌아온 숨", TargetRule.DEAD_ALLY_SINGLE, 5, List.of(SkillEffect.revive(0.30))),
                new SkillDefinition("p04_resting_light", "안식의 빛", TargetRule.ALLY_ALL, 3, List.of(SkillEffect.heal(0.90)))),
                4, List.of("P04_LAST_TOUCH"), Map.of("emergencyHeal", 0.80));
    }

    public static CombatantDefinition borderHunter() {
        return new CombatantDefinition("F03", "변경 사냥꾼", new BattleStats(650, 88, 56, 103), "f03_shot", List.of(
                new SkillDefinition("f03_shot", "사격", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(0.90))),
                new SkillDefinition("f03_focus_shot", "집중 사격", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(1.45)))));
    }

    public static CombatantDefinition corruptedWalker() {
        return new CombatantDefinition("E001", "부패 보행자", new BattleStats(720, 92, 68, 82), "e001_basic", List.of(
                new SkillDefinition("e001_basic", "썩은 주먹", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00)))),
                0, List.of("E001_TENACITY"), Map.of("tenacityThreshold", 0.30, "tenacityBarrier", 0.10));
    }

    public static CombatantDefinition boneArcher() {
        return new CombatantDefinition("E002", "뼈 사수", new BattleStats(560, 100, 54, 105), "e002_basic", List.of(
                new SkillDefinition("e002_basic", "뼈 화살", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(0.95))),
                new SkillDefinition("e002_aimed", "조준 사격", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(1.45)))));
    }

    public static CombatantDefinition unstableExploder() {
        return new CombatantDefinition("E003", "불안정 폭발체", new BattleStats(650, 125, 50, 78), "e003_basic", List.of(
                new SkillDefinition("e003_basic", "몸통 박기", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(0.70))),
                new SkillDefinition("e003_arm", "팽창", TargetRule.SELF, 3, List.of(SkillEffect.mark("e003_armed", 2)),
                        "Armed 상태가 됩니다. 다음 정규 행동에 대폭발을 사용합니다."),
                new SkillDefinition("e003_explode", "대폭발", TargetRule.ENEMY_ALL, 0, List.of(SkillEffect.damage(1.20)),
                        "적 전체에 ATK 120% 피해를 주고 자신은 전투불능이 됩니다.", List.of("NON_BASIC_ZERO_CD", "E003_EXPLODE"), Map.of())));
    }

    public static CombatantDefinition hookTracker() { return unstableExploder(); }

    public static CombatantDefinition roadsideRaider() {
        return new CombatantDefinition("E004", "길목 약탈자", new BattleStats(680, 98, 64, 100), "e004_basic", List.of(
                new SkillDefinition("e004_basic", "베기", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00))),
                new SkillDefinition("e004_stab", "비열한 찌르기", TargetRule.ENEMY_SINGLE, 2, List.of(SkillEffect.damage(1.55)),
                        "HP 50% 이하 대상을 우선해 ATK 155% 피해를 줍니다.")));
    }

    public static CombatantDefinition ironSentinel() { return roadsideRaider(); }

    public static CombatantDefinition fieldMedic() {
        return new CombatantDefinition("E005", "야전 치유사", new BattleStats(590, 82, 60, 94), "e005_basic", List.of(
                new SkillDefinition("e005_basic", "응급 봉합", TargetRule.ALLY_SINGLE, 0, List.of(SkillEffect.heal(0.55))),
                new SkillDefinition("e005_reform", "전열 정비", TargetRule.ALLY_ALL, 3, List.of(SkillEffect.defenseUp(0.15, 2)))));
    }

    public static CombatantDefinition graul() {
        return new CombatantDefinition("B01", "들이받는 왕 그라울", new BattleStats(2800, 150, 115, 92), "b01_basic", List.of(
                new SkillDefinition("b01_basic", "뿔치기", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.20))),
                new SkillDefinition("b01_scratch", "지면 긁기", TargetRule.SELF, 3, List.of(SkillEffect.attackMod(0.15, 2)),
                        "자신의 ATK +15%, 2행동 지속."),
                new SkillDefinition("b01_warn", "돌파 예고", TargetRule.SELF, 0, List.of(SkillEffect.mark("b01_charge_warning", 2)),
                        "다음 정규 행동에 왕의 돌진을 사용합니다.", List.of("NON_BASIC_ZERO_CD", "BOSS_WARNING"), Map.of()),
                new SkillDefinition("b01_charge", "왕의 돌진", TargetRule.ENEMY_ALL, 4, List.of(SkillEffect.damage(1.05)),
                        "적 전체에 ATK 105% 피해.")),
                0, List.of("BOSS", "B01_PHASES"), Map.of("level", 6.0, "phase2", 0.70, "phase3", 0.35,
                "phase2Defense", 0.15, "phase3Speed", 0.20));
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
                new SkillDefinition("e_shaman_active", "전투 주술", TargetRule.ALLY_ALL, 3, List.of(SkillEffect.attackMod(0.15, 2)))));
    }

    public static CombatantDefinition trainingEnemy(String id, String name, int hp, int attack, int defense, int speed) {
        String basic = id.toLowerCase() + "_basic";
        return new CombatantDefinition(id, name, new BattleStats(hp, attack, defense, speed), basic, List.of(
                new SkillDefinition(basic, "공격", TargetRule.ENEMY_SINGLE, 0, List.of(SkillEffect.damage(1.00)))));
    }
}
