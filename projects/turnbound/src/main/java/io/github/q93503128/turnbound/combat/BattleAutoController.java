package io.github.q93503128.turnbound.combat;

import java.util.Comparator;
import java.util.List;

/** Server-side deterministic AUTO and enemy action policy. */
public final class BattleAutoController {
    private BattleAutoController() {}

    public static void chooseAutoAction(BattleEngine engine, BattleState state, CombatantState actor) {
        if (actor.definition().summon()) { engine.useSkill(actor.instanceId(), actor.definition().basicSkillId()); return; }
        if (actor.side() == CombatantSide.ENEMY) { chooseEnemy(engine, state, actor); return; }
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
            case "F01" -> engine.useSkill(actor.instanceId(), "f01_wood_sword", priorityEnemy(enemies).instanceId());
            case "F02" -> engine.useSkill(actor.instanceId(), "f02_first_aid", weakest(allies).instanceId());
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
            engine.useSkill(actor.instanceId(), "p02_delay_field", imminentEnemy.instanceId());
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
                .min(Comparator.comparingDouble(unit -> unit.hp() / (double)unit.maxHp())).orElse(null);
        if (endangered != null && endangered.hp() * 100 <= endangered.maxHp() * 60
                && actor.cooldown("p03_guard_transfer") == 0) {
            engine.useSkill(actor.instanceId(), "p03_guard_transfer", endangered.instanceId());
        } else if (actor.counter("guard") >= 50 && actor.cooldown("p03_shield_pressure") == 0) {
            engine.useSkill(actor.instanceId(), "p03_shield_pressure", highestGauge(enemies).instanceId());
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
        long low = allies.stream().filter(unit -> unit.hp() * 100 <= unit.maxHp() * 60).count();
        if (low >= 2 && actor.cooldown("p04_resting_light") == 0) {
            engine.useSkill(actor.instanceId(), "p04_resting_light");
        } else {
            engine.useSkill(actor.instanceId(), "p04_heal", weakest(allies).instanceId());
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
            engine.useSkill(actor.instanceId(), "p06_echo", priorityEnemy(enemies).instanceId());
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
            engine.useSkill(actor.instanceId(), "p08_blood_charge", target.instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "p08_frenzy", target.instanceId());
        }
    }
    private static void chooseEnemy(BattleEngine engine, BattleState state, CombatantState actor) {
        List<CombatantState> allies = state.living(CombatantSide.ALLY);
        List<CombatantState> own = state.living(CombatantSide.ENEMY);
        switch (actor.definition().id()) {
            case "E001" -> basicEnemy(engine, actor, allies);
            case "E002" -> engine.useSkill(actor.instanceId(), actor.cooldown("e002_aimed") == 0 ? "e002_aimed" : "e002_basic", weakest(allies).instanceId());
            case "E003" -> { if (actor.hasStatus("e003_armed")) engine.useSkill(actor.instanceId(), "e003_explode"); else if (actor.cooldown("e003_arm") == 0) engine.useSkill(actor.instanceId(), "e003_arm"); else engine.useSkill(actor.instanceId(), "e003_basic", distributedTarget(allies, actor).instanceId()); }
            case "E004" -> { CombatantState low = allies.stream().filter(unit -> unit.hp() * 2 <= unit.maxHp()).min(Comparator.comparingInt(CombatantState::hp)).orElse(null); if (low != null && actor.cooldown("e004_stab") == 0) engine.useSkill(actor.instanceId(), "e004_stab", low.instanceId()); else engine.useSkill(actor.instanceId(), "e004_basic", distributedTarget(allies, actor).instanceId()); }
            case "E005" -> { if (actor.cooldown("e005_reform") == 0) engine.useSkill(actor.instanceId(), "e005_reform"); else engine.useSkill(actor.instanceId(), "e005_basic", weakest(own).instanceId()); }
            case "E006" -> engine.useSkill(actor.instanceId(), actor.cooldown("e006_charge") == 0 ? "e006_charge" : "e006_basic", distributedTarget(allies, actor).instanceId());
            case "E007" -> { if (actor.cooldown("e007_slow_spores") == 0) engine.useSkill(actor.instanceId(), "e007_slow_spores"); else basicEnemy(engine, actor, allies); }
            case "E008" -> { if (actor.cooldown("e008_barrier") == 0) engine.useSkill(actor.instanceId(), "e008_barrier", weakest(own).instanceId()); else basicEnemy(engine, actor, allies); }
            case "E009" -> engine.useSkill(actor.instanceId(), actor.cooldown("e009_delay") == 0 ? "e009_delay" : "e009_basic", (actor.cooldown("e009_delay") == 0 ? highestGauge(allies) : distributedTarget(allies, actor)).instanceId());
            case "E010" -> engine.useSkill(actor.instanceId(), actor.cooldown("e010_flood_rot") == 0 ? "e010_flood_rot" : "e010_basic", weakest(allies).instanceId());
            case "E011" -> { if (actor.cooldown("e011_support") == 0) engine.useSkill(actor.instanceId(), "e011_support", strongest(own).instanceId()); else basicEnemy(engine, actor, allies); }
            case "E012" -> { CombatantState target = allies.stream().filter(unit -> unit.gauge() < actor.gauge()).min(Comparator.comparingLong(CombatantState::gauge)).orElse(weakest(allies)); engine.useSkill(actor.instanceId(), actor.cooldown("e012_pounce") == 0 ? "e012_pounce" : "e012_basic", target.instanceId()); }
            case "E013" -> { if (actor.cooldown("e013_embers") == 0) engine.useSkill(actor.instanceId(), "e013_embers"); else basicEnemy(engine, actor, allies); }
            case "E014" -> engine.useSkill(actor.instanceId(), actor.cooldown("e014_crush") == 0 ? "e014_crush" : "e014_basic", weakest(allies).instanceId());
            case "EL01" -> { if (actor.cooldown("el01_command") == 0) engine.useSkill(actor.instanceId(), "el01_command"); else basicEnemy(engine, actor, allies); }
            case "EL02" -> { CombatantState previous = state.find(actor.ref("el02_last_target")); CombatantState target = previous != null && !previous.downed() ? previous : weakest(allies); engine.useSkill(actor.instanceId(), actor.cooldown("el02_piercing_horn") == 0 ? "el02_piercing_horn" : "el02_basic", target.instanceId()); }
            case "EL03" -> { if (actor.barrier() == 0 && actor.cooldown("el03_barrier") == 0) engine.useSkill(actor.instanceId(), "el03_barrier"); else basicEnemy(engine, actor, allies); }
            case "EL04" -> { if (actor.cooldown("el04_collapse") == 0) engine.useSkill(actor.instanceId(), "el04_collapse"); else basicEnemy(engine, actor, allies); }
            case "B01" -> chooseB01(engine, actor, allies);
            case "B02" -> chooseB02(engine, state, actor, allies);
            case "B03" -> chooseB03(engine, actor, allies);
            case "B04" -> chooseB04(engine, actor, allies);
            case "B05" -> chooseB05(engine, actor, allies);
            case "E_ARCHER" -> engine.useSkill(actor.instanceId(), actor.cooldown("e_archer_active") == 0 ? "e_archer_active" : "e_archer_basic", weakest(allies).instanceId());
            case "E_SHIELD" -> { if (actor.cooldown("e_shield_active") == 0) engine.useSkill(actor.instanceId(), "e_shield_active", weakest(own).instanceId()); else basicEnemy(engine, actor, allies); }
            case "E_SHAMAN" -> { if (actor.cooldown("e_shaman_active") == 0) engine.useSkill(actor.instanceId(), "e_shaman_active"); else engine.useSkill(actor.instanceId(), "e_shaman_basic", weakest(own).instanceId()); }
            default -> useBasic(engine, state, actor);
        }
    }

    private static void chooseB01(BattleEngine engine, CombatantState actor, List<CombatantState> allies) {
        if (actor.hasStatus("b01_charge_warning") && actor.cooldown("b01_charge") == 0) engine.useSkill(actor.instanceId(), "b01_charge");
        else if (actor.flag("b01_phase3") && actor.cooldown("b01_charge") == 0) engine.useSkill(actor.instanceId(), "b01_warn");
        else if (actor.cooldown("b01_scratch") == 0) engine.useSkill(actor.instanceId(), "b01_scratch");
        else engine.useSkill(actor.instanceId(), "b01_basic", weakest(allies).instanceId());
    }

    private static void chooseB02(BattleEngine engine, BattleState state, CombatantState actor, List<CombatantState> allies) {
        if (actor.cooldown("b02_root_prison") == 0) engine.useSkill(actor.instanceId(), "b02_root_prison", highestGauge(allies).instanceId());
        else if (actor.cooldown("b02_summon") == 0 && state.living(CombatantSide.ENEMY).size() < 5) engine.useSkill(actor.instanceId(), "b02_summon");
        else if (actor.cooldown("b02_thorn_wave") == 0) engine.useSkill(actor.instanceId(), "b02_thorn_wave");
        else engine.useSkill(actor.instanceId(), "b02_basic", distributedTarget(allies, actor).instanceId());
    }

    private static void chooseB03(BattleEngine engine, CombatantState actor, List<CombatantState> allies) {
        if (actor.flag("b03_phase3") && actor.counter("b03_overclock_count") >= 2) engine.useSkill(actor.instanceId(), "b03_overclock");
        else if (actor.barrier() == 0 && actor.cooldown("b03_barrier") == 0) engine.useSkill(actor.instanceId(), "b03_barrier");
        else if (actor.cooldown("b03_drain") == 0) engine.useSkill(actor.instanceId(), "b03_drain");
        else engine.useSkill(actor.instanceId(), "b03_basic", highestGauge(allies).instanceId());
    }

    private static void chooseB04(BattleEngine engine, CombatantState actor, List<CombatantState> allies) {
        if (actor.flag("b04_eruption_ready") && actor.cooldown("b04_eruption") == 0) engine.useSkill(actor.instanceId(), "b04_eruption");
        else if (actor.flag("b04_phase3") && actor.cooldown("b04_eruption") == 0) engine.useSkill(actor.instanceId(), "b04_warn");
        else if (actor.cooldown("b04_collapse") == 0) engine.useSkill(actor.instanceId(), "b04_collapse");
        else if (actor.cooldown("b04_fury") == 0) engine.useSkill(actor.instanceId(), "b04_fury");
        else engine.useSkill(actor.instanceId(), "b04_basic", weakest(allies).instanceId());
    }

    private static void chooseB05(BattleEngine engine, CombatantState actor, List<CombatantState> allies) {
        if (actor.flag("b05_relay_ready") && actor.cooldown("b05_relay_collapse") == 0) engine.useSkill(actor.instanceId(), "b05_relay_collapse");
        else if (actor.flag("b05_phase3") && actor.cooldown("b05_relay_collapse") == 0) engine.useSkill(actor.instanceId(), "b05_warn");
        else if (actor.flag("b05_phase2") && !actor.flag("b05_phase3") && actor.cooldown("b05_order_collapse") == 0) engine.useSkill(actor.instanceId(), "b05_order_collapse");
        else if (actor.flag("b05_phase2") && !actor.flag("b05_phase3") && actor.cooldown("b05_rift_wave") == 0) engine.useSkill(actor.instanceId(), "b05_rift_wave");
        else if (actor.cooldown("b05_time_cut") == 0) engine.useSkill(actor.instanceId(), "b05_time_cut", highestGauge(allies).instanceId());
        else if (actor.cooldown("b05_mark") == 0) engine.useSkill(actor.instanceId(), "b05_mark", weakest(allies).instanceId());
        else engine.useSkill(actor.instanceId(), "b05_basic", distributedTarget(allies, actor).instanceId());
    }

    private static void basicEnemy(BattleEngine engine, CombatantState actor, List<CombatantState> allies) { engine.useSkill(actor.instanceId(), actor.definition().basicSkillId(), distributedTarget(allies, actor).instanceId()); }

    private static void useBasic(BattleEngine engine, BattleState state, CombatantState actor) {
        SkillDefinition basic = actor.definition().skill(actor.definition().basicSkillId());
        switch (basic.targetRule()) {
            case SELF, ALLY_ALL, ENEMY_ALL -> engine.useSkill(actor.instanceId(), basic.id());
            case ENEMY_SINGLE -> engine.useSkill(actor.instanceId(), basic.id(), distributedTarget(state.living(actor.side().opposite()), actor).instanceId());
            case ALLY_SINGLE -> engine.useSkill(actor.instanceId(), basic.id(), weakest(state.living(actor.side())).instanceId());
            case DEAD_ALLY_SINGLE -> { List<CombatantState> downed = state.downed(actor.side()); if (!downed.isEmpty()) engine.useSkill(actor.instanceId(), basic.id(), downed.getFirst().instanceId()); else throw new IllegalStateException("No dead ally for basic revive"); }
        }
    }

    private static CombatantState weakest(List<CombatantState> units) { return units.stream().min(Comparator.comparingDouble((CombatantState unit) -> unit.hp() / (double)unit.maxHp()).thenComparingInt(CombatantState::initiativeSeed)).orElseThrow(); }
    private static CombatantState strongest(List<CombatantState> units) { return units.stream().max(Comparator.comparingInt(CombatantState::attack).thenComparingInt(unit -> -unit.initiativeSeed())).orElseThrow(); }
    private static CombatantState highestGauge(List<CombatantState> units) { return units.stream().max(Comparator.comparingLong(CombatantState::gauge).thenComparingInt(unit -> -unit.initiativeSeed())).orElseThrow(); }
    private static CombatantState priorityEnemy(List<CombatantState> enemies) { return enemies.stream().min(Comparator.comparingInt((CombatantState unit) -> enemyPriority(unit.definition().id())).thenComparingDouble(unit -> unit.hp() / (double)unit.maxHp()).thenComparingInt(CombatantState::initiativeSeed)).orElseThrow(); }
    private static int enemyPriority(String id) { if (id.equals("E005") || id.equals("E011") || id.equals("E_SHAMAN")) return 0; if (id.equals("E002") || id.equals("E003") || id.equals("E007") || id.equals("E013") || id.equals("E_ARCHER")) return 1; if (id.startsWith("EL")) return 2; if (id.startsWith("B")) return 5; return 3; }
    private static CombatantState distributedTarget(List<CombatantState> targets, CombatantState actor) { if (targets.isEmpty()) throw new IllegalStateException("Actor has no living target"); return targets.get(Math.floorMod(actor.initiativeSeed(), targets.size())); }
}
