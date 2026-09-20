package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.presentation.HeroSkillAudioStyle;

/** Pure semantic cue routing. Network transport and client playback are deliberately outside this class. */
final class BattleAudioCueRouting {
    private BattleAudioCueRouting() {}

    static String cue(BattleState state, BattleEvent event) {
        String type = event.type();
        return switch (type) {
            case "ACTION" -> actionCue(state, event);
            case "DAMAGE" -> encode(heavy(state, event) ? "hit_heavy" : "hit_light",
                    "IMPACT", heavy(state, event) ? 3 : 1, event);
            case "REACTION_DAMAGE" -> encode("reaction_hit", "REACTION", 3, event);
            case "DOT" -> encode("dot_tick", "IMPACT", 1, event);
            case "HEAL", "REACTION_HEAL" -> encode("heal", "SUPPORT",
                    "REACTION_HEAL".equals(type) ? 2 : 1, event);
            case "BARRIER" -> encode("barrier", "SUPPORT", 2, event);
            case "REVIVE", "SELF_REVIVE" -> encode("revive", "SYSTEM", 3, event);
            case "DOWN" -> encode("down", "IMPACT", 3, event);
            case "BOSS_PHASE" -> encode("boss_phase", "SYSTEM", 3, event);
            case "SPAWN" -> encode("spawn", "SYSTEM", 2, event);
            default -> null;
        };
    }

    private static String actionCue(BattleState state, BattleEvent event) {
        CombatantState source = state.find(event.sourceId());
        HeroSkillAudioStyle.Style style = source == null ? null
                : HeroSkillAudioStyle.resolve(source.definition().id(), event.detail());
        return style == null
                ? encode("skill", "SKILL", 2, event)
                : encode(style.cueId(), "SKILL", style.priority(), event);
    }

    private static boolean heavy(BattleState state, BattleEvent event) {
        CombatantState target = state.find(event.targetId());
        return target != null && event.value() >= Math.max(1, (int)Math.floor(target.maxHp() * 0.18));
    }

    private static String encode(String id, String group, int priority, BattleEvent event) {
        return safe(id) + "|" + safe(group) + "|" + priority + "|" + safe(event.sourceId()) + "|"
                + safe(event.targetId()) + "|" + safe(event.detail()) + "|" + event.value();
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace('|', '/').replace('\n', ' ');
    }
}
