package io.github.q93503128.turnbound.combat;

import java.util.Comparator;
import java.util.List;

/** Shared deterministic targets and fallback skills used by both AUTO policies. */
final class AutoTargeting {
    private AutoTargeting() {}

    static void basicEnemy(BattleEngine engine, CombatantState actor, List<CombatantState> allies) { engine.useSkill(actor.instanceId(), actor.definition().basicSkillId(), distributedTarget(allies, actor).instanceId()); }

    static void useBasic(BattleEngine engine, BattleState state, CombatantState actor) {
        SkillDefinition basic = actor.definition().skill(actor.definition().basicSkillId());
        switch (basic.targetRule()) {
            case SELF, ALLY_ALL, ENEMY_ALL -> engine.useSkill(actor.instanceId(), basic.id());
            case ENEMY_TWO -> engine.useSkill(actor.instanceId(), basic.id(),
                    targetIds(state.living(actor.side().opposite()), Comparator.comparingInt(CombatantState::initiativeSeed), 2));
            case ENEMY_SINGLE -> engine.useSkill(actor.instanceId(), basic.id(), distributedTarget(state.living(actor.side().opposite()), actor).instanceId());
            case ALLY_SINGLE -> engine.useSkill(actor.instanceId(), basic.id(), mostInjured(state.living(actor.side())).instanceId());
            case DEAD_ALLY_SINGLE -> { List<CombatantState> downed = state.downed(actor.side()); if (!downed.isEmpty()) engine.useSkill(actor.instanceId(), basic.id(), downed.getFirst().instanceId()); else throw new IllegalStateException("No dead ally for basic revive"); }
        }
    }

    static String[] targetIds(List<CombatantState> units, Comparator<CombatantState> comparator, int max) {
        return units.stream().sorted(comparator.thenComparingInt(CombatantState::initiativeSeed))
                .limit(Math.max(1, max)).map(CombatantState::instanceId).toArray(String[]::new);
    }

    static Comparator<CombatantState> injuryComparator() {
        return Comparator.comparingDouble((CombatantState unit) -> unit.hp() / (double)unit.maxHp())
                .thenComparingInt(unit -> -(unit.maxHp() - unit.hp()))
                .thenComparingInt(CombatantState::initiativeSeed);
    }
    static CombatantState mostInjured(List<CombatantState> units) { return units.stream().min(injuryComparator()).orElseThrow(); }
    static CombatantState weakest(List<CombatantState> units) { return mostInjured(units); }
    static CombatantState strongest(List<CombatantState> units) { return units.stream().max(Comparator.comparingInt(CombatantState::attack).thenComparingInt(unit -> -unit.initiativeSeed())).orElseThrow(); }
    static CombatantState highestGauge(List<CombatantState> units) { return units.stream().max(Comparator.comparingLong(CombatantState::gauge).thenComparingInt(unit -> -unit.initiativeSeed())).orElseThrow(); }
    static CombatantState priorityEnemy(List<CombatantState> enemies) { return enemies.stream().min(Comparator.comparingInt((CombatantState unit) -> enemyPriority(unit.definition().id())).thenComparingDouble(unit -> unit.hp() / (double)unit.maxHp()).thenComparingInt(CombatantState::initiativeSeed)).orElseThrow(); }
    static int enemyPriority(String id) { if (id.equals("E005") || id.equals("E011") || id.equals("E_SHAMAN")) return 0; if (id.equals("E002") || id.equals("E003") || id.equals("E007") || id.equals("E013") || id.equals("E_ARCHER")) return 1; if (id.startsWith("EL")) return 2; if (id.startsWith("B")) return 5; return 3; }
    static CombatantState distributedTarget(List<CombatantState> targets, CombatantState actor) { if (targets.isEmpty()) throw new IllegalStateException("Actor has no living target"); return targets.get(Math.floorMod(actor.initiativeSeed(), targets.size())); }
}
