package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellTransactionPolicy;
import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.spellengine.ProjectSpellInputRules;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MendSpellAuthorityContractTest {
    @Test
    void mendOwnsCanonicalClassResourceCooldownAndTapInputContract() {
        ProjectSpellSpec spec = ProjectSpellSpec.mend();
        assertEquals(ProjectSpellSpec.MEND_ID, spec.id());
        assertEquals(22.0, spec.manaCost(), 0.0001);
        assertEquals(160, spec.cooldownTicks());
        assertEquals(0.30, ProjectSpellSpec.MEND_HEAL_COEFFICIENT, 0.0001);
        assertEquals(
                RootClass.CLERIC,
                ProjectSpellSpec.requiredRootClass(spec.id()).orElseThrow()
        );

        assertFalse(ProjectSpellInputRules.suppressHeldRepeat(spec.id(), true));
        assertTrue(ProjectSpellInputRules.suppressHeldRepeat(spec.id(), false));

        PlayerCombatStateStore states = new PlayerCombatStateStore();
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        states,
                        ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );

        UUID player = UUID.randomUUID();
        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.commitAcceptedCast(player, spec.id(), 100)
        );
        assertEquals(78.0, states.getOrCreate(player, 100).mana(100), 0.0001);
        assertEquals(160, states.getOrCreate(player, 100).cooldownRemainingTicks(spec.id(), 100));

        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.commitAcceptedCast(player, spec.id(), 100)
        );
        assertEquals(78.0, states.getOrCreate(player, 100).mana(100), 0.0001);
    }
}
