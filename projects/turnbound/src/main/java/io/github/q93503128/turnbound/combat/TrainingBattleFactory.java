package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit operator/regression battle fixture.
 *
 * <p>This is never selected by normal campaign encounter routing.</p>
 */
public final class TrainingBattleFactory {
    private TrainingBattleFactory() {}

    public static BattleState create() {
        List<CombatantState> units = baseAllies();
        units.add(new CombatantState("enemy_sword_a",
                PrototypeRoster.swordEnemy("E_SWORD_A", "훈련 검병 A"), CombatantSide.ENEMY, 4));
        units.add(new CombatantState("enemy_sword_b",
                PrototypeRoster.swordEnemy("E_SWORD_B", "훈련 검병 B"), CombatantSide.ENEMY, 5));
        units.add(new CombatantState("enemy_archer", PrototypeRoster.archerEnemy(), CombatantSide.ENEMY, 6));
        units.add(new CombatantState("enemy_shield", PrototypeRoster.shieldEnemy(), CombatantSide.ENEMY, 7));
        units.add(new CombatantState("enemy_shaman", PrototypeRoster.shamanEnemy(), CombatantSide.ENEMY, 8));
        return new BattleState(units);
    }

    public static BattleState createFieldPatrol() {
        List<CombatantState> units = baseAllies();
        units.add(new CombatantState("enemy_e001", PrototypeRoster.corruptedWalker(), CombatantSide.ENEMY, 4));
        units.add(new CombatantState("enemy_e002", PrototypeRoster.boneArcher(), CombatantSide.ENEMY, 5));
        units.add(new CombatantState("enemy_e005", PrototypeRoster.fieldMedic(), CombatantSide.ENEMY, 6));
        return new BattleState(units);
    }

    private static List<CombatantState> baseAllies() {
        List<CombatantState> units = new ArrayList<>();
        units.add(new CombatantState("ally_kyren", PrototypeRoster.kyren(), CombatantSide.ALLY, 0));
        units.add(new CombatantState("ally_lumea", PrototypeRoster.lumea(), CombatantSide.ALLY, 1));
        units.add(new CombatantState("ally_bram", PrototypeRoster.bram(), CombatantSide.ALLY, 2));
        units.add(new CombatantState("ally_elysia", PrototypeRoster.elysia(), CombatantSide.ALLY, 3));
        return units;
    }
}
