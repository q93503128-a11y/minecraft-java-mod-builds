package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
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

        Vec3 arena = session.battleAnchor();
        out.append("A|").append(number(arena.x)).append('|').append(number(arena.y)).append('|')
                .append(number(arena.z)).append('|').append(number(session.battleYaw())).append('\n');

        for (CombatantState combatant : state.combatants()) {
            Vec3 pos = session.combatantPosition(combatant.instanceId());
            if (pos == null) pos = arena;
            out.append("U|").append(combatant.instanceId()).append('|').append(combatant.definition().id()).append('|')
                    .append(combatant.side()).append('|').append(combatant.definition().name()).append('|')
                    .append(combatant.hp()).append('|').append(combatant.maxHp()).append('|')
                    .append(combatant.barrier()).append('|').append(combatant.gauge()).append('|')
                    .append(combatant.downed() ? 1 : 0).append('|')
                    .append(number(pos.x)).append('|').append(number(pos.y)).append('|').append(number(pos.z)).append('\n');
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
        return out.toString();
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}