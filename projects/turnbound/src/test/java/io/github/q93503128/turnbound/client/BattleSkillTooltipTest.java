package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
