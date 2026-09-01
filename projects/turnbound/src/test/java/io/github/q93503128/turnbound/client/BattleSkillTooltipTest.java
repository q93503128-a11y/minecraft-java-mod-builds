package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleSkillTooltipTest {
    @Test
    void tooltipExposesTargetCooldownAndDescription() {
        ClientBattleState.Skill skill = new ClientBattleState.Skill(
                "p01_breaker_strike", "파쇄 일격", "ENEMY_SINGLE", 2, 1,
                "적 1명에게 강한 피해를 주고 집중에 따라 위력이 증가합니다.");
        List<String> lines = BattleSkillTooltip.lines(skill);
        assertEquals("파쇄 일격", lines.getFirst());
        assertTrue(lines.stream().anyMatch(line -> line.contains("적 1명")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("쿨타임 2")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("남은 쿨타임 1")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("집중")));
    }

    @Test
    void canonicalCombatTermsAreLocalizedOnlyAtPresentationBoundary() {
        ClientBattleState.Skill basic = new ClientBattleState.Skill(
                "test_basic", "시험 행동", "SELF", 0, 0,
                "ATK 125% 피해 · Turn Gauge +80 · 대상 MaxHP 20% 보호막 · DEF +15% · SPD +10%.");
        List<String> lines = BattleSkillTooltip.lines(basic);
        String joined = String.join(" ", lines);

        assertTrue(joined.contains("기본 행동 · 쿨타임 없음"));
        assertTrue(joined.contains("공격력 125%"));
        assertTrue(joined.contains("행동 게이지 +80"));
        assertTrue(joined.contains("최대 HP 20%"));
        assertTrue(joined.contains("방어력 +15%"));
        assertTrue(joined.contains("속도 +10%"));
        assertFalse(joined.contains("ATK"));
        assertFalse(joined.contains("Turn Gauge"));
        assertFalse(joined.contains("MaxHP"));
        assertFalse(joined.contains("DEF"));
        assertFalse(joined.contains("SPD"));
        assertFalse(joined.contains("Basic"));
    }
}
