package io.github.q93503128.turnbound.combat;

import java.util.List;

/** Single location for skill eligibility; resolution and per-character effects remain in BattleEngine. */
final class BattleActionValidator {
    private BattleActionValidator() {}

    static void validateActor(CombatantState actor, SkillDefinition skill) {
        if (actor.downed() || actor.cooldown(skill.id()) > 0)
            throw new IllegalStateException("Actor unavailable or skill on cooldown");
        if (actor.hasStatus("action_disable"))
            throw new IllegalStateException("Actor is action-disabled");
        if (actor.hasStatus("silence") && !skill.isBasic())
            throw new IllegalStateException("Actor is silenced");
    }

    static void validateTargets(BattleState state, CombatantState actor, SkillDefinition skill,
                                List<CombatantState> targets) {
        if (skill.hasRule("SELF_FORBIDDEN") && targets.contains(actor))
            throw new IllegalArgumentException("Skill cannot target self");
        if (skill.hasRule("OTHER_ALLY_IF_AVAILABLE") && targets.contains(actor)
                && state.living(actor.side()).stream().anyMatch(unit -> unit != actor && !unit.definition().summon()))
            throw new IllegalArgumentException("Skill requires another ally while one is available");
        if (skill.hasRule("P08_FURY_60") && actor.counter("fury") < 60)
            throw new IllegalStateException("Not enough Fury");
        if (actor.definition().id().equals("P07") && skill.id().equals("p07_summon_toto")
                && state.living(actor.side()).stream().noneMatch(
                        unit -> unit.definition().summon() && actor.instanceId().equals(unit.ref("ownerId"))))
            throw new IllegalStateException("Contract partner is unavailable");
    }
}
