package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellTransactionPolicy;
import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
        authority.onEngineCastCompleted(player, spec.id(), 0, "RELEASE", 1.0F);

        assertEquals(
                SpellCastAuthority.ImpactDecision.rejected(),
                authority.onImpact(player, spec.id(), 5, 42, 7.5, 1.0)
        );
    }

    @Test
    void projectileImpactCanArriveAfterSpellCastCompletionWithoutReusingResourceToken() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        AtomicInteger impacts = new AtomicInteger();
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        states,
                        (registeredSpec, context) -> {
                            assertEquals(spec, registeredSpec);
                            assertEquals(1.20, registeredSpec.actionCoefficient(), 0.0001);
                            assertEquals(0.50, registeredSpec.poiseCoefficient(), 0.0001);
                            impacts.incrementAndGet();
                            return SpellCastAuthority.ImpactDecision.accepted(false);
                        }
                )
        );

        UUID player = UUID.randomUUID();
        authority.commitAcceptedCast(player, spec.id(), 200);
        authority.onEngineCastCompleted(player, spec.id(), 200, "RELEASE", 1.0F);

        assertEquals(
                SpellCastAuthority.ImpactDecision.accepted(false),
                authority.onImpact(player, spec.id(), 208, 77, 9.0, 1.0)
        );
        assertEquals(1, impacts.get());
        assertEquals(88.0, states.getOrCreate(player, 208).mana(208), 0.0001);
    }

    @Test
    void differentProjectSpellsShareOneAcceptedCastLockPerPlayer() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        ProjectSpellSpec arcBolt = ProjectSpellSpec.arcBolt();
        ProjectSpellSpec second = new ProjectSpellSpec(
                "openworld_rpg:second_test_spell", 10.0, 40, 1.0, 0.2, 1
        );
        authority.registerPolicy(
                arcBolt.id(),
                new ProjectSpellTransactionPolicy(
                        arcBolt, states, ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );
        authority.registerPolicy(
                second.id(),
                new ProjectSpellTransactionPolicy(
                        second, states, ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );

        UUID player = UUID.randomUUID();
        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.commitAcceptedCast(player, arcBolt.id(), 300)
        );
        assertEquals(
                SpellCastAuthority.AttemptDecision.BLOCK,
                authority.commitAcceptedCast(player, second.id(), 300)
        );
        assertEquals(88.0, states.getOrCreate(player, 300).mana(300), 0.0001);
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
