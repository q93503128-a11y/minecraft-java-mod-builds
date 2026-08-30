package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class P0Scenario {
    private P0Scenario() {}

    public static BattleState create() {
        List<CombatantState> units = baseAllies();
        units.add(new CombatantState("enemy_sword_a", PrototypeRoster.swordEnemy("E_SWORD_A", "훈련 검병 A"), CombatantSide.ENEMY, 4));
        units.add(new CombatantState("enemy_sword_b", PrototypeRoster.swordEnemy("E_SWORD_B", "훈련 검병 B"), CombatantSide.ENEMY, 5));
        units.add(new CombatantState("enemy_archer", PrototypeRoster.archerEnemy(), CombatantSide.ENEMY, 6));
        units.add(new CombatantState("enemy_shield", PrototypeRoster.shieldEnemy(), CombatantSide.ENEMY, 7));
        units.add(new CombatantState("enemy_shaman", PrototypeRoster.shamanEnemy(), CombatantSide.ENEMY, 8));
        return new BattleState(units);
    }

    /** Compatibility regression fixture; production field sessions use SouthgateEncounterCatalog. */
    public static BattleState createFieldPatrol() {
        List<CombatantState> units = baseAllies();
        units.add(new CombatantState("enemy_e001", PrototypeRoster.corruptedWalker(), CombatantSide.ENEMY, 4));
        units.add(new CombatantState("enemy_e002", PrototypeRoster.boneArcher(), CombatantSide.ENEMY, 5));
        units.add(new CombatantState("enemy_e005", PrototypeRoster.fieldMedic(), CombatantSide.ENEMY, 6));
        return new BattleState(units);
    }

    static List<CombatantState> baseAllies() {
        List<CombatantState> units = new ArrayList<>();
        units.add(new CombatantState("ally_kyren", PrototypeRoster.kyren(), CombatantSide.ALLY, 0));
        units.add(new CombatantState("ally_lumea", PrototypeRoster.lumea(), CombatantSide.ALLY, 1));
        units.add(new CombatantState("ally_bram", PrototypeRoster.bram(), CombatantSide.ALLY, 2));
        units.add(new CombatantState("ally_elysia", PrototypeRoster.elysia(), CombatantSide.ALLY, 3));
        return units;
    }

    public static String runAutoDiagnostic(int maxActions) {
        BattleState state = create();
        BattleEngine engine = new BattleEngine(state);
        int actions = 0;
        while (state.outcome() == BattleOutcome.RUNNING && actions < maxActions) {
            CombatantState actor = engine.nextReady();
            chooseAutoAction(engine, state, actor);
            actions++;
        }
        String timeline = state.events().stream().filter(event -> event.type().equals("TURN_READY")).limit(12)
                .map(BattleEvent::sourceId).reduce((left, right) -> left + " > " + right).orElse("none");
        return "outcome=" + state.outcome() + ", actions=" + actions + ", pulses=" + state.logicalPulse()
                + ", allies=" + state.living(CombatantSide.ALLY).size() + ", enemies=" + state.living(CombatantSide.ENEMY).size()
                + ", timeline=" + timeline;
    }

    public static void chooseAutoAction(BattleEngine engine, BattleState state, CombatantState actor) {
        if (actor.side() == CombatantSide.ENEMY) { chooseEnemy(engine, state, actor); return; }
        switch (actor.definition().id()) {
            case "P01" -> {
                CombatantState target = priorityEnemy(state.living(CombatantSide.ENEMY));
                String skill = actor.cooldown("p01_shatter") == 0 && actor.counter("focus") >= 1 ? "p01_shatter" : "p01_basic";
                engine.useSkill(actor.instanceId(), skill, target.instanceId());
            }
            case "P02" -> {
                CombatantState other = state.living(CombatantSide.ALLY).stream().filter(unit -> unit != actor)
                        .min(Comparator.comparing((CombatantState unit) -> !unit.definition().id().equals("P01")).thenComparingLong(CombatantState::gauge)).orElse(null);
                if (other != null && actor.cooldown("p02_time_leap") == 0) engine.useSkill(actor.instanceId(), "p02_time_leap", other.instanceId());
                else if (other != null) engine.useSkill(actor.instanceId(), "p02_basic", other.instanceId());
                else engine.useSkill(actor.instanceId(), "p02_delay_field");
            }
            case "P03" -> {
                List<CombatantState> enemies = state.living(CombatantSide.ENEMY);
                if (actor.cooldown("p03_press") == 0 && !enemies.isEmpty()) engine.useSkill(actor.instanceId(), "p03_press", priorityEnemy(enemies).instanceId());
                else engine.useSkill(actor.instanceId(), "p03_basic");
            }
            case "P04" -> {
                if (!state.downed(CombatantSide.ALLY).isEmpty() && actor.cooldown("p04_revive") == 0) {
                    engine.useSkill(actor.instanceId(), "p04_revive", state.downed(CombatantSide.ALLY).getFirst().instanceId());
                } else {
                    CombatantState target = weakest(state.living(CombatantSide.ALLY));
                    engine.useSkill(actor.instanceId(), "p04_basic", target.instanceId());
                }
            }
            case "F03" -> {
                CombatantState target = priorityEnemy(state.living(CombatantSide.ENEMY));
                String skill = actor.cooldown("f03_focus_shot") == 0 ? "f03_focus_shot" : "f03_shot";
                engine.useSkill(actor.instanceId(), skill, target.instanceId());
            }
            default -> throw new IllegalStateException("Unknown prototype actor " + actor.definition().id());
        }
    }

    private static void chooseEnemy(BattleEngine engine, BattleState state, CombatantState actor) {
        List<CombatantState> allies = state.living(CombatantSide.ALLY);
        List<CombatantState> own = state.living(CombatantSide.ENEMY);
        switch (actor.definition().id()) {
            case "E002" -> engine.useSkill(actor.instanceId(), actor.cooldown("e002_aimed") == 0 ? "e002_aimed" : "e002_basic", weakest(allies).instanceId());
            case "E003" -> {
                if (actor.status("e003_armed") != null) engine.useSkill(actor.instanceId(), "e003_explode");
                else if (actor.cooldown("e003_arm") == 0) engine.useSkill(actor.instanceId(), "e003_arm");
                else engine.useSkill(actor.instanceId(), "e003_basic", distributedTarget(allies, actor).instanceId());
            }
            case "E004" -> {
                CombatantState target = weakest(allies);
                boolean execute = target.hp() * 2 <= target.maxHp() && actor.cooldown("e004_stab") == 0;
                engine.useSkill(actor.instanceId(), execute ? "e004_stab" : "e004_basic", target.instanceId());
            }
            case "E005" -> {
                if (actor.cooldown("e005_reform") == 0) engine.useSkill(actor.instanceId(), "e005_reform");
                else engine.useSkill(actor.instanceId(), "e005_basic", weakest(own).instanceId());
            }
            case "B01" -> {
                if (actor.flag("b01_charge_ready") && actor.cooldown("b01_charge") == 0) engine.useSkill(actor.instanceId(), "b01_charge");
                else if (actor.flag("b01_phase3") && actor.cooldown("b01_charge") == 0) engine.useSkill(actor.instanceId(), "b01_warn");
                else if (actor.cooldown("b01_scratch") == 0) engine.useSkill(actor.instanceId(), "b01_scratch");
                else engine.useSkill(actor.instanceId(), "b01_basic", weakest(allies).instanceId());
            }
            case "E_ARCHER" -> engine.useSkill(actor.instanceId(), actor.cooldown("e_archer_active") == 0 ? "e_archer_active" : "e_archer_basic", weakest(allies).instanceId());
            case "E_SHIELD" -> {
                if (actor.cooldown("e_shield_active") == 0) engine.useSkill(actor.instanceId(), "e_shield_active", weakest(own).instanceId());
                else engine.useSkill(actor.instanceId(), "e_shield_basic", distributedTarget(allies, actor).instanceId());
            }
            case "E_SHAMAN" -> {
                if (actor.cooldown("e_shaman_active") == 0) {
                    engine.useSkill(actor.instanceId(), "e_shaman_active");
                    for (CombatantState unit : state.living(CombatantSide.ENEMY)) unit.putStatus(new StatusInstance("attack_multiplier", actor.instanceId(), 2, 0.15));
                } else engine.useSkill(actor.instanceId(), "e_shaman_basic", weakest(own).instanceId());
            }
            default -> engine.useSkill(actor.instanceId(), actor.definition().basicSkillId(), distributedTarget(allies, actor).instanceId());
        }
    }

    private static CombatantState weakest(List<CombatantState> units) {
        return units.stream().min(Comparator.comparingDouble((CombatantState unit) -> unit.hp() / (double) unit.maxHp())
                .thenComparingInt(CombatantState::initiativeSeed)).orElseThrow();
    }

    private static CombatantState priorityEnemy(List<CombatantState> enemies) {
        return enemies.stream().min(Comparator.comparingInt((CombatantState unit) -> enemyPriority(unit.definition().id()))
                .thenComparingDouble(unit -> unit.hp() / (double) unit.maxHp()).thenComparingInt(CombatantState::initiativeSeed)).orElseThrow();
    }

    private static int enemyPriority(String id) {
        if (id.equals("E005") || id.equals("E_SHAMAN")) return 0;
        if (id.equals("E002") || id.equals("E003") || id.equals("E_ARCHER")) return 1;
        if (id.equals("E001") || id.equals("E004") || id.startsWith("E_SWORD")) return 2;
        if (id.equals("B01")) return 4;
        return 5;
    }

    private static CombatantState distributedTarget(List<CombatantState> allies, CombatantState actor) {
        if (allies.isEmpty()) throw new IllegalStateException("Enemy has no living target");
        return allies.get(Math.floorMod(actor.initiativeSeed(), allies.size()));
    }
}
