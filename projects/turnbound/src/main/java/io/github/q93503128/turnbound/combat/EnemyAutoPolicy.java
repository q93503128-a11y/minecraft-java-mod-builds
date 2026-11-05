package io.github.q93503128.turnbound.combat;

import java.util.Comparator;
import java.util.List;
import static io.github.q93503128.turnbound.combat.AutoTargeting.*;

/** Enemy and boss AUTO policies. */
final class EnemyAutoPolicy {
    private EnemyAutoPolicy() {}

    static void chooseEnemy(BattleEngine engine, BattleState state, CombatantState actor) {
        List<CombatantState> allies = state.living(CombatantSide.ALLY);
        List<CombatantState> own = state.living(CombatantSide.ENEMY);
        switch (actor.definition().id()) {
            case "CV_A" -> engine.useSkill(actor.instanceId(),
                    actor.cooldown("cv_a_charge") == 0 ? "cv_a_charge" : "cv_a_basic",
                    distributedTarget(allies, actor).instanceId());
            case "CV_B" -> {
                CombatantState low = allies.stream()
                        .filter(unit -> unit.hp() * 100 <= unit.maxHp() * 60)
                        .min(Comparator.comparingInt(CombatantState::hp)).orElse(null);
                if (low != null && actor.cooldown("cv_b_opportunist") == 0) {
                    engine.useSkill(actor.instanceId(), "cv_b_opportunist", low.instanceId());
                } else {
                    engine.useSkill(actor.instanceId(), "cv_b_basic", distributedTarget(allies, actor).instanceId());
                }
            }
            case "CV_C" -> {
                CombatantState aimed = state.find(actor.ref("cv_c_aim_target"));
                if (actor.flag("cv_c_aim_ready") && actor.cooldown("cv_c_aimed") == 0) {
                    CombatantState target = aimed != null && !aimed.downed() ? aimed : weakest(allies);
                    engine.useSkill(actor.instanceId(), "cv_c_aimed", target.instanceId());
                } else if (actor.cooldown("cv_c_aimed") == 0) {
                    engine.useSkill(actor.instanceId(), "cv_c_take_aim", weakest(allies).instanceId());
                } else {
                    engine.useSkill(actor.instanceId(), "cv_c_basic", weakest(allies).instanceId());
                }
            }
            case "EL_CV01" -> {
                CombatantState previous = state.find(actor.ref("el_cv01_last_target"));
                CombatantState target = previous != null && !previous.downed() ? previous : weakest(allies);
                if (actor.flag("el_cv01_charge_ready") && actor.cooldown("el_cv01_charge") == 0) {
                    engine.useSkill(actor.instanceId(), "el_cv01_charge", target.instanceId());
                } else if (actor.hp() * 2 <= actor.maxHp() && actor.cooldown("el_cv01_stomp") == 0) {
                    engine.useSkill(actor.instanceId(), "el_cv01_stomp");
                } else if (actor.cooldown("el_cv01_charge") == 0) {
                    engine.useSkill(actor.instanceId(), "el_cv01_warn");
                } else {
                    engine.useSkill(actor.instanceId(), "el_cv01_gore", target.instanceId());
                }
            }
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
            case "AV_B01" -> chooseAvsalB01(engine, actor, allies);
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

    private static void chooseAvsalB01(BattleEngine engine, CombatantState actor, List<CombatantState> allies) {
        if (actor.hasStatus("av_b01_breach_warning") && actor.cooldown("av_b01_breach") == 0) {
            engine.useSkill(actor.instanceId(), "av_b01_breach");
        } else if (actor.barrier() == 0 && actor.cooldown("av_b01_barrier") == 0) {
            engine.useSkill(actor.instanceId(), "av_b01_barrier");
        } else if (actor.flag("av_b01_phase2") && actor.cooldown("av_b01_breach") == 0) {
            engine.useSkill(actor.instanceId(), "av_b01_warn");
        } else if (actor.cooldown("av_b01_lock") == 0) {
            engine.useSkill(actor.instanceId(), "av_b01_lock", highestGauge(allies).instanceId());
        } else {
            engine.useSkill(actor.instanceId(), "av_b01_basic", distributedTarget(allies, actor).instanceId());
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

}
