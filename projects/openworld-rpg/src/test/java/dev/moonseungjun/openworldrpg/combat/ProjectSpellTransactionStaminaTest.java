package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellTransactionPolicy;
import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectSpellTransactionStaminaTest {
    @Test
    void ironCounterCostIsCommittedThroughSharedSpellTransaction() {
        var states = new PlayerCombatStateStore();
        var spec = ProjectSpellSpec.warriorIronCounter();
        var policy = new ProjectSpellTransactionPolicy(
                spec,
                states,
                ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
        );
        UUID playerId = UUID.randomUUID();
        long tick = 20L;
        var context = new SpellCastAuthority.CastContext(
                playerId,
                spec.id(),
                tick,
                false
        );

        assertEquals(0.0, spec.manaCost(), 0.0001);
        assertEquals(18.0, spec.staminaCost(), 0.0001);
        assertTrue(policy.preflight(context));
        assertTrue(policy.commitAcceptedCast(context));
        assertEquals(
                82.0,
                states.getOrCreate(playerId, tick).stamina(tick),
                0.0001
        );
        assertEquals(
                100.0,
                states.getOrCreate(playerId, tick).mana(tick),
                0.0001
        );
    }

    @Test
    void mixedResourceFailureDoesNotPartiallySpendMana() {
        var states = new PlayerCombatStateStore();
        UUID playerId = UUID.randomUUID();
        long tick = 40L;
        var state = states.getOrCreate(playerId, tick);
        assertTrue(state.spendStamina(90.0, 0L, tick));

        var spec = new ProjectSpellSpec(
                "openworld_rpg:test_mixed_cost",
                20.0,
                18.0,
                40,
                0.0,
                0.0,
                1
        );
        var policy = new ProjectSpellTransactionPolicy(
                spec,
                states,
                ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
        );
        var context = new SpellCastAuthority.CastContext(
                playerId,
                spec.id(),
                tick,
                false
        );

        assertFalse(policy.preflight(context));
        assertFalse(policy.commitAcceptedCast(context));
        assertEquals(100.0, state.mana(tick), 0.0001);
        assertEquals(10.0, state.stamina(tick), 0.0001);
    }

    @Test
    void manaCostAdjustmentAppliesDuringPreflightAndConsumesAfterCommit() {
        var states = new PlayerCombatStateStore();
        UUID playerId = UUID.randomUUID();
        long tick = 20L;
        var state = states.getOrCreate(playerId, tick);
        assertTrue(state.spendMana(82.0, tick));

        var spec = new ProjectSpellSpec(
                "openworld_rpg:test_discounted_cost",
                20.0,
                0.0,
                40,
                0.0,
                0.0,
                1
        );
        boolean[] committed = {false};
        var policy = new ProjectSpellTransactionPolicy(
                spec,
                states,
                ProjectSpellTransactionPolicy.SpellImpactPort.failClosed(),
                new ProjectSpellTransactionPolicy.ManaCostAdjustment() {
                    @Override
                    public double previewMultiplier(
                            SpellCastAuthority.CastContext context
                    ) {
                        return 0.90;
                    }

                    @Override
                    public void commit(
                            SpellCastAuthority.CastContext context
                    ) {
                        committed[0] = true;
                    }
                }
        );
        var context = new SpellCastAuthority.CastContext(
                playerId,
                spec.id(),
                tick,
                false
        );

        assertTrue(policy.preflight(context));
        assertTrue(policy.commitAcceptedCast(context));
        assertTrue(committed[0]);
        assertEquals(0.0, state.mana(tick), 0.0001);
    }
}
