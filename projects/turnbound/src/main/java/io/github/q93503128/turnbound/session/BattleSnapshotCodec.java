package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.SkillDefinition;

import java.util.stream.Collectors;

public final class BattleSnapshotCodec {
    private BattleSnapshotCodec() {}

    public static String encode(BattleSession session) {
        BattleState state = session.state();
        String actor = state.currentActorId() == null ? "" : state.currentActorId();
        StringBuilder out = new StringBuilder();
        out.append("H|").append(1).append('|').append(session.auto() ? 1 : 0).append('|')
                .append(session.speed()).append('|').append(state.outcome()).append('|')
                .append(actor).append('|').append(session.finished() ? 1 : 0).append('\n');

        for (CombatantState combatant : state.combatants()) {
            out.append("U|").append(combatant.instanceId()).append('|').append(combatant.definition().id()).append('|')
                    .append(combatant.side()).append('|').append(combatant.definition().name()).append('|')
                    .append(combatant.hp()).append('|').append(combatant.maxHp()).append('|')
                    .append(combatant.barrier()).append('|').append(combatant.gauge()).append('|')
                    .append(combatant.downed() ? 1 : 0).append('\n');
        }

        out.append("T|").append(state.timelinePreview(8).stream()
                .map(CombatantState::instanceId)
                .collect(Collectors.joining(","))).append('\n');

        if (state.currentActorId() != null) {
            CombatantState current = state.combatant(state.currentActorId());
            for (SkillDefinition skill : current.definition().skills()) {
                out.append("S|").append(skill.id()).append('|').append(skill.name()).append('|')
                        .append(skill.targetRule()).append('|').append(skill.cooldown()).append('|')
                        .append(current.cooldown(skill.id())).append('\n');
            }
        }

        // Presentation intentionally does not expose internal TURN_READY/pulse/combatant IDs as player-facing text.
        return out.toString();
    }
}
