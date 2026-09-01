package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

final class BattleSkillTooltip {
    private BattleSkillTooltip() {}

    static List<String> lines(ClientBattleState.Skill skill) {
        List<String> lines = new ArrayList<>();
        lines.add(skill.name());
        lines.add(targetLabel(skill.targetRule()) + "  ·  " + cooldownLabel(skill));
        if (skill.remaining() > 0) lines.add("남은 쿨타임 " + skill.remaining() + "회");
        String description = BattlePresentationText.skillDescription(skill.description());
        if (!description.isBlank()) lines.add(description);
        return List.copyOf(lines);
    }

    static String targetLabel(String rule) {
        return switch (rule) {
            case "SELF" -> "자신";
            case "ALLY_SINGLE" -> "아군 1명";
            case "ALLY_ALL" -> "아군 전체";
            case "ENEMY_SINGLE" -> "적 1명";
            case "ENEMY_ALL" -> "적 전체";
            case "DEAD_ALLY_SINGLE" -> "전투불능 아군 1명";
            default -> "대상 지정";
        };
    }

    private static String cooldownLabel(ClientBattleState.Skill skill) {
        return BattlePresentationText.cooldownType(skill.baseCooldown());
    }
}
