package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class P0Scenario {
    private P0Scenario() {}

    public static BattleState create() {
        List<CombatantState> units = new ArrayList<>();
        units.add(new CombatantState("ally_kyren", PrototypeRoster.kyren(), CombatantSide.ALLY, 0));
        units.add(new CombatantState("ally_lumea", PrototypeRoster.lumea(), CombatantSide.ALLY, 1));
        units.add(new CombatantState("ally_bram", PrototypeRoster.bram(), CombatantSide.ALLY, 2));
        units.add(new CombatantState("ally_elysia", PrototypeRoster.elysia(), CombatantSide.ALLY, 3));
        units.add(new CombatantState("enemy_sword_a", PrototypeRoster.swordEnemy("E_SWORD_A", "훈련 검병 A"), CombatantSide.ENEMY, 4));
        units.add(new CombatantState("enemy_sword_b", PrototypeRoster.swordEnemy("E_SWORD_B", "훈련 검병 B"), CombatantSide.ENEMY, 5));
        units.add(new CombatantState("enemy_archer", PrototypeRoster.archerEnemy(), CombatantSide.ENEMY, 6));
        units.add(new CombatantState("enemy_shield", PrototypeRoster.shieldEnemy(), CombatantSide.ENEMY, 7));
        units.add(new CombatantState("enemy_shaman", PrototypeRoster.shamanEnemy(), CombatantSide.ENEMY, 8));
        return new BattleState(units);
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
        String timeline = state.events().stream()
                .filter(event -> event.type().equals("TURN_READY"))
                .limit(12)
                .map(BattleEvent::sourceId)
                .reduce((left, right) -> left + " > " + right)
                .orElse("none");
        return "outcome=" + state.outcome() + ", actions=" + actions + ", pulses=" + state.logicalPulse() + ", timeline=" + timeline;
    }

    public static void chooseAutoAction(BattleEngine engine, BattleState state, CombatantState actor) {
        if (actor.side() == CombatantSide.ENEMY) {
            chooseEnemy(engine, state, actor);
            return;
        }

        switch (actor.definition().id()) {
            case "P01" -> {
                CombatantState target = state.living(CombatantSide.ENEMY).getFirst();
                String skill = actor.cooldown("p01_shatter") == 0 && actor.counter("focus") >= 1
                        ? "p01_shatter"
                        : "p01_basic";
                engine.useSkill(actor.instanceId(), skill, target.instanceId());
            }
            case "P02" -> {
                CombatantState kyren = state.living(CombatantSide.ALLY).stream()
                        .filter(unit -> unit.definition().id().equals("P01"))
                        .findFirst()
                        .orElseGet(() -> state.living(CombatantSide.ALLY).getFirst());
                String skill = actor.cooldown("p02_time_leap") == 0 ? "p02_time_leap" : "p02_basic";
                engine.useSkill(actor.instanceId(), skill, kyren.instanceId());
            }
            case "P03" -> {
                List<CombatantState> enemies = state.living(CombatantSide.ENEMY);
                if (actor.cooldown("p03_press") == 0 && !enemies.isEmpty()) {
                    engine.useSkill(actor.instanceId(), "p03_press", enemies.getFirst().instanceId());
                } else {
                    engine.useSkill(actor.instanceId(), "p03_basic");
                }
            }
            case "P04" -> {
                if (!state.downed(CombatantSide.ALLY).isEmpty() && actor.cooldown("p04_revive") == 0) {
                    engine.useSkill(actor.instanceId(), "p04_revive", state.downed(CombatantSide.ALLY).getFirst().instanceId());
                } else {
                    CombatantState target = state.living(CombatantSide.ALLY).stream()
                            .min(Comparator.comparingDouble(unit -> unit.hp() / (double) unit.maxHp()))
                            .orElseThrow();
                    engine.useSkill(actor.instanceId(), "p04_basic", target.instanceId());
                }
            }
            default -> throw new IllegalStateException("Unknown prototype actor " + actor.definition().id());
        }
    }

    private static void chooseEnemy(BattleEngine engine, BattleState state, CombatantState actor) {
        List<CombatantState> allies = state.living(CombatantSide.ALLY);
        List<CombatantState> own = state.living(CombatantSide.ENEMY);
        switch (actor.definition().id()) {
            case "E_ARCHER" -> {
                CombatantState target = allies.stream()
                        .min(Comparator.comparingDouble(unit -> unit.hp() / (double) unit.maxHp()))
                        .orElseThrow();
                String skill = actor.cooldown("e_archer_active") == 0 ? "e_archer_active" : "e_archer_basic";
                engine.useSkill(actor.instanceId(), skill, target.instanceId());
            }
            case "E_SHIELD" -> {
                if (actor.cooldown("e_shield_active") == 0) {
                    CombatantState target = own.stream()
                            .min(Comparator.comparingInt(CombatantState::barrier).thenComparingInt(CombatantState::hp))
                            .orElseThrow();
                    engine.useSkill(actor.instanceId(), "e_shield_active", target.instanceId());
                } else {
                    engine.useSkill(actor.instanceId(), "e_shield_basic", allies.getFirst().instanceId());
                }
            }
            case "E_SHAMAN" -> {
                if (actor.cooldown("e_shaman_active") == 0) {
                    engine.useSkill(actor.instanceId(), "e_shaman_active");
                    for (CombatantState unit : state.living(CombatantSide.ENEMY)) {
                        unit.putStatus(new StatusInstance("attack_multiplier", actor.instanceId(), 2, 0.15));
                    }
                } else {
                    CombatantState target = own.stream()
                            .min(Comparator.comparingDouble(unit -> unit.hp() / (double) unit.maxHp()))
                            .orElseThrow();
                    engine.useSkill(actor.instanceId(), "e_shaman_basic", target.instanceId());
                }
            }
            default -> engine.useSkill(actor.instanceId(), actor.definition().basicSkillId(), allies.getFirst().instanceId());
        }
    }
}
