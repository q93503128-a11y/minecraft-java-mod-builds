package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellTransactionPolicy;
import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpellCastAuthorityTest {
    @Test
    void nonProjectSpellPassesThrough() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");

        assertEquals(
                SpellCastAuthority.AttemptDecision.PASS_THROUGH,
                authority.preflightAttempt(UUID.randomUUID(), "external_mod:spell", 0)
        );
    }

    @Test
    void projectSpellWithoutPolicyIsBlocked() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");

        assertEquals(
                SpellCastAuthority.AttemptDecision.BLOCK,
                authority.preflightAttempt(UUID.randomUUID(), "openworld_rpg:test_spell", 0)
        );
    }

    @Test
    void arcBoltCommitsManaAndCooldownExactlyOnceAcrossSameTickReentry() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        states,
                        ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );

        UUID player = UUID.randomUUID();

        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.preflightAttempt(player, spec.id(), 100));
        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.commitAcceptedCast(player, spec.id(), 100));
        assertEquals(88.0, states.getOrCreate(player, 100).mana(100), 0.0001);
        assertEquals(60, states.getOrCreate(player, 100).cooldownRemainingTicks(spec.id(), 100));

        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.commitAcceptedCast(player, spec.id(), 100));
        assertEquals(88.0, states.getOrCreate(player, 100).mana(100), 0.0001);

        authority.onEngineCostConsumed(player, spec.id(), 100);
        authority.onEngineCastCompleted(player, spec.id(), 100, "RELEASE", 1.0F);

        assertEquals(SpellCastAuthority.AttemptDecision.BLOCK, authority.preflightAttempt(player, spec.id(), 101));
        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.preflightAttempt(player, spec.id(), 160));
    }

    @Test
    void impactIsFailClosedUntilProjectCombatImpactPortExists() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        states,
                        ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );

        UUID player = UUID.randomUUID();
        authority.commitAcceptedCast(player, spec.id(), 0);

        assertEquals(
                SpellCastAuthority.ImpactDecision.rejected(),
                authority.onImpact(player, spec.id(), 0, 42, 7.5, 1.0)
        );
    }

    @Test
    void committedProjectStageWithoutTransactionFails() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        new PlayerCombatStateStore(),
                        ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> authority.onEngineCostConsumed(UUID.randomUUID(), spec.id(), 0)
        );
    }
}
