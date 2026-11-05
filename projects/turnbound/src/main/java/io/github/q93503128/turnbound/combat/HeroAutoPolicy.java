package io.github.q93503128.turnbound.combat;

import java.util.Comparator;
import java.util.List;
import static io.github.q93503128.turnbound.combat.AutoTargeting.*;

/** Character-specific tactical AUTO, kept separate from enemy AI. */
final class HeroAutoPolicy {
    private HeroAutoPolicy() {}

    static void chooseHero(BattleEngine engine, BattleState state, CombatantState actor) {
        List<CombatantState> enemies = state.living(CombatantSide.ENEMY);
        List<CombatantState> allies = state.living(CombatantSide.ALLY).stream().filter(unit -> !unit.definition().summon()).toList();
        switch (actor.definition().id()) {
            case "P01" -> chooseKyren(engine, state, actor, enemies);
            case "P02" -> chooseLumea(engine, state, actor, allies, enemies);
            case "P03" -> chooseBram(engine, actor, allies, enemies);
            case "P04" -> chooseElysia(engine, state, actor, allies);
            case "P05" -> chooseLynette(engine, actor, enemies);
            case "P06" -> chooseMorwen(engine, actor, enemies);
            case "P07" -> chooseMarion(engine, state, actor, enemies);
            case "P08" -> chooseRaze(engine, actor, enemies);
            case "F01" -> engine.useSkill(actor.instanceId(), "f01_wood_sword",
                    targetIds(enemies,
                            Comparator.comparingInt((CombatantState unit) -> enemyPriority(unit.definition().id()))
                                    .thenComparingDouble(unit -> unit.hp() / (double)unit.maxHp()),
                            2));
            case "F02" -> engine.useSkill(actor.instanceId(), "f02_first_aid", mostInjured(allies).instanceId());
            case "F03" -> {
                CombatantState target = priorityEnemy(enemies);
                engine.useSkill(actor.instanceId(), actor.cooldown("f03_focus_shot") == 0 ? "f03_focus_shot" : "f03_shot", target.instanceId());
            }
            case "F04" -> {
                if (actor.cooldown("f04_endure") == 0 && actor.hp() * 100 <= actor.maxHp() * 70) engine.useSkill(actor.instanceId(), "f04_endure");
                else engine.useSkill(actor.instanceId(), "f04_shield_push", priorityEnemy(enemies).instanceId());
            }
            default -> useBasic(engine, state, actor);
        }
    }

    private static void chooseKyren(BattleEngine engine, BattleState state, CombatantState actor, List<CombatantState> enemies) {
        CombatantState focus = state.find(actor.ref("focusTarget"));
        if (focus == null || focus.downed()) {
            CombatantState target = priorityEnemy(enemies);
            if (actor.cooldown("p01_duel_lock") == 0) engine.useSkill(actor.instanceId(), "p01_duel_lock", target.instanceId());
            else engine.useSkill(actor.instanceId(), "p01_chase_slash", target.instanceId());
            return;
        }

        int focusValue = actor.counter("focus");
        if (focusValue >= 2 && actor.cooldown("p01_breaker_strike") == 0) {
            engine.useSkill(actor.instanceId(), "p01_breaker_strike", focus.instanceId());
        } else if (focusValue < 2 && actor.cooldown("p01_duel_lock") == 0) {
            engine.useSkill(actor.instanceId(), "p01_duel_lock", focus.instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "p01_chase_slash", focus.instanceId());
        }
    }

    private static void chooseLumea(BattleEngine engine, BattleState state, CombatantState actor, List<CombatantState> allies, List<CombatantState> enemies) {
        List<CombatantState> projected = state.timelinePreview(6);
        List<CombatantState> future = !projected.isEmpty() && projected.getFirst() == actor
                ? projected.subList(1, projected.size()) : projected;

        CombatantState reviver = allies.stream()
                .filter(unit -> unit != actor && unit.definition().id().equals("P04"))
                .findFirst().orElse(null);
        if (!state.downed(CombatantSide.ALLY).isEmpty() && reviver != null
                && actor.cooldown("p02_time_leap") == 0 && futureRank(future, reviver) >= 2) {
            engine.useSkill(actor.instanceId(), "p02_time_leap", reviver.instanceId());
            return;
        }

        CombatantState imminentEnemy = future.stream().limit(2)
                .filter(unit -> unit.side() == CombatantSide.ENEMY && !unit.downed())
                .findFirst().orElse(null);
        if (imminentEnemy != null && actor.cooldown("p02_delay_field") == 0) {
            engine.useSkill(actor.instanceId(), "p02_delay_field",
                    targetIds(enemies, Comparator.comparingLong(CombatantState::gauge).reversed(), 2));
            return;
        }

        List<CombatantState> otherAllies = allies.stream()
                .filter(unit -> unit != actor && !unit.definition().summon()).toList();
        if (otherAllies.isEmpty()) {
            engine.useSkill(actor.instanceId(), "p02_accelerate", actor.instanceId());
            return;
        }

        CombatantState best = otherAllies.stream().max(
                Comparator.comparingInt((CombatantState unit) -> unit.speed() < actor.speed() ? 1 : 0)
                        .thenComparingInt(unit -> futureRank(future, unit))
                        .thenComparingInt(CombatantState::attack))
                .orElseThrow();
        int rank = futureRank(future, best);
        if (actor.cooldown("p02_time_leap") == 0 && rank >= 2 && best.gauge() < 700) {
            engine.useSkill(actor.instanceId(), "p02_time_leap", best.instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "p02_accelerate", best.instanceId());
        }
    }

    private static int futureRank(List<CombatantState> future, CombatantState target) {
        int index = future.indexOf(target);
        return index < 0 ? 99 : index;
    }

    private static void chooseBram(BattleEngine engine, CombatantState actor, List<CombatantState> allies, List<CombatantState> enemies) {
        CombatantState endangered = allies.stream().filter(unit -> unit != actor)
                .min(injuryComparator()).orElse(null);
        if (endangered != null && endangered.hp() * 100 <= endangered.maxHp() * 60
                && actor.cooldown("p03_guard_transfer") == 0) {
            engine.useSkill(actor.instanceId(), "p03_guard_transfer", endangered.instanceId());
        } else if (actor.counter("guard") >= 50 && actor.cooldown("p03_shield_pressure") == 0) {
            engine.useSkill(actor.instanceId(), "p03_shield_pressure",
                    targetIds(enemies, Comparator.comparingLong(CombatantState::gauge).reversed(), 2));
        } else {
            engine.useSkill(actor.instanceId(), "p03_guard_stance", priorityEnemy(enemies).instanceId());
        }
    }
    private static void chooseElysia(BattleEngine engine, BattleState state, CombatantState actor, List<CombatantState> allies) {
        List<CombatantState> downed = state.downed(CombatantSide.ALLY).stream()
                .filter(unit -> !unit.definition().summon()).toList();
        if (!downed.isEmpty() && actor.cooldown("p04_returned_breath") == 0) {
            engine.useSkill(actor.instanceId(), "p04_returned_breath", downed.getFirst().instanceId());
            return;
        }
        CombatantState injured = mostInjured(allies);
        boolean critical = injured.hp() * 100 <= injured.maxHp() * 35;
        long low = allies.stream().filter(unit -> unit.hp() * 100 <= unit.maxHp() * 65).count();
        if (!critical && low >= 2 && actor.cooldown("p04_resting_light") == 0) {
            engine.useSkill(actor.instanceId(), "p04_resting_light");
        } else {
            engine.useSkill(actor.instanceId(), "p04_heal", injured.instanceId());
        }
    }
    private static void chooseLynette(BattleEngine engine, CombatantState actor, List<CombatantState> enemies) {
        CombatantState sightline = enemies.stream()
                .filter(target -> target.instanceId().equals(actor.ref("sightline")))
                .findFirst().orElse(null);
        if (sightline == null) {
            CombatantState target = priorityEnemy(enemies);
            if (actor.cooldown("p05_hunt_signal") == 0) engine.useSkill(actor.instanceId(), "p05_hunt_signal", target.instanceId());
            else engine.useSkill(actor.instanceId(), "p05_suppressive_shot", target.instanceId());
            return;
        }
        if (actor.counter("shot") >= 2 && actor.cooldown("p05_piercing_shot") == 0) {
            engine.useSkill(actor.instanceId(), "p05_piercing_shot", sightline.instanceId());
        } else if (actor.counter("shot") == 0 && actor.cooldown("p05_hunt_signal") == 0) {
            engine.useSkill(actor.instanceId(), "p05_hunt_signal", sightline.instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "p05_suppressive_shot", sightline.instanceId());
        }
    }
    private static void chooseMorwen(BattleEngine engine, CombatantState actor, List<CombatantState> enemies) {
        CombatantState execute = enemies.stream()
                .filter(unit -> unit.hp() * 100 <= unit.maxHp() * 30)
                .min(Comparator.comparingInt(CombatantState::hp)).orElse(null);
        if (execute != null && actor.cooldown("p06_funeral_order") == 0) {
            engine.useSkill(actor.instanceId(), "p06_funeral_order", execute.instanceId());
        } else if (actor.counter("records") >= 2 && actor.cooldown("p06_condolence") == 0) {
            engine.useSkill(actor.instanceId(), "p06_condolence", priorityEnemy(enemies).instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "p06_echo",
                    targetIds(enemies,
                            Comparator.comparingInt((CombatantState unit) -> enemyPriority(unit.definition().id()))
                                    .thenComparingDouble(unit -> unit.hp() / (double)unit.maxHp()),
                            2));
        }
    }
    private static void chooseMarion(BattleEngine engine, BattleState state, CombatantState actor, List<CombatantState> enemies) {
        CombatantState partner = state.living(CombatantSide.ALLY).stream()
                .filter(unit -> unit.definition().summon() && actor.instanceId().equals(unit.ref("ownerId")))
                .findFirst().orElse(null);
        CombatantState target = priorityEnemy(enemies);
        List<CombatantState> allies = state.living(CombatantSide.ALLY).stream()
                .filter(unit -> !unit.definition().summon() && unit != actor).toList();
        CombatantState endangered = allies.stream()
                .min(Comparator.comparingDouble(unit -> unit.hp() / (double)unit.maxHp())).orElse(null);

        if (partner != null && endangered != null && endangered.hp() * 100 <= endangered.maxHp() * 55
                && actor.cooldown("p07_summon_toto") == 0) {
            engine.useSkill(actor.instanceId(), "p07_summon_toto", endangered.instanceId());
        } else if (actor.cooldown("p07_joint_attack") == 0 && (actor.counter("bond") >= 50 || partner == null)) {
            engine.useSkill(actor.instanceId(), "p07_joint_attack", target.instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "p07_command", target.instanceId());
        }
    }
    private static void chooseRaze(BattleEngine engine, CombatantState actor, List<CombatantState> enemies) {
        CombatantState target = priorityEnemy(enemies);
        if (actor.counter("fury") >= 60 && !actor.hasStatus("attack_multiplier")
                && actor.cooldown("p08_battle_mania") == 0) {
            engine.useSkill(actor.instanceId(), "p08_battle_mania");
        } else if (actor.hp() * 100 > actor.maxHp() * 45 && actor.counter("fury") < 80
                && actor.cooldown("p08_blood_charge") == 0) {
            engine.useSkill(actor.instanceId(), "p08_blood_charge");
        } else {
            engine.useSkill(actor.instanceId(), "p08_frenzy", target.instanceId());
        }
    }
}
