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
        out.append("H|1|").append(session.auto() ? 1 : 0).append('|').append(session.speed()).append('|')
                .append(state.outcome()).append('|').append(actor).append('|').append(session.finished() ? 1 : 0).append('|')
                .append(session.autoAllowed() ? 1 : 0).append('|').append(session.speedAllowed() ? 1 : 0).append('|')
                .append(session.fleeAllowed() ? 1 : 0).append('\n');

        Vec3 arena = session.battleAnchor();
        out.append("A|").append(number(arena.x)).append('|').append(number(arena.y)).append('|')
                .append(number(arena.z)).append('|').append(number(session.battleYaw())).append('\n');

        for (CombatantState combatant : state.combatants()) {
            Vec3 pos = session.combatantPosition(combatant.instanceId());
            if (pos == null) pos = arena;
            String statuses = combatant.statusesView().keySet().stream().sorted().collect(Collectors.joining(","));
            out.append("U|").append(combatant.instanceId()).append('|').append(combatant.definition().id()).append('|')
                    .append(combatant.side()).append('|').append(safe(combatant.definition().name())).append('|')
                    .append(combatant.hp()).append('|').append(combatant.maxHp()).append('|')
                    .append(combatant.barrier()).append('|').append(combatant.gauge()).append('|')
                    .append(combatant.downed() ? 1 : 0).append('|')
                    .append(number(pos.x)).append('|').append(number(pos.y)).append('|').append(number(pos.z)).append('|')
                    .append(safe(statuses)).append('\n');
        }

        out.append("T|").append(state.timelinePreview(8).stream().map(CombatantState::instanceId)
                .collect(Collectors.joining(","))).append('\n');

        if (state.currentActorId() != null) {
            CombatantState current = state.combatant(state.currentActorId());
            for (SkillDefinition skill : current.definition().skills()) {
                String canonicalSkillId = current.definition().canonicalSkillId(skill.id());
                out.append("S|").append(canonicalSkillId).append('|').append(safe(skill.name())).append('|')
                        .append(skill.targetRule()).append('|').append(skill.cooldown()).append('|')
                        .append(current.cooldown(skill.id())).append('|').append(safe(skill.description())).append('\n');
            }
        }

        BattleResultSummary result = session.resultSummary();
        out.append("R|").append(result.xp()).append('|').append(result.gold()).append('|')
                .append(result.firstClear() ? 1 : 0).append('|').append(result.crystal()).append('|')
                .append(result.starEssence()).append('|')
                .append(safe(String.join(",", result.equipmentRewards()))).append('\n');
        for (BattleResultSummary.PartyXp member : result.party()) {
            out.append("P|").append(safe(member.characterId())).append('|').append(safe(member.name())).append('|')
                    .append(member.levelBefore()).append('|').append(member.xpBefore()).append('|')
                    .append(member.levelAfter()).append('|').append(member.xpAfter()).append('|')
                    .append(member.xpToNextAfter()).append('\n');
        }
        return out.toString();
    }

    private static String safe(String value) { return value.replace('|', '/').replace('\n', ' ').replace('\r', ' '); }
    private static String number(double value) { return String.format(Locale.ROOT, "%.3f", value); }
}
