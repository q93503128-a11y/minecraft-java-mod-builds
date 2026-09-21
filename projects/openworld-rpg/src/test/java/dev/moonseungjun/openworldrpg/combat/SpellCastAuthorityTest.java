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
        SpellCastAuthority authority = authorityWithArcBolt(new PlayerCombatStateStore());
        UUID player = UUID.randomUUID();
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        authority = authorityWithArcBolt(states);

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
    void timedCastContinuationSurvivesPastShortReentryWindowWithoutDoubleSpend() {
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        SpellCastAuthority authority = authorityWithArcBolt(states);
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        UUID player = UUID.randomUUID();

        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.preflightAttempt(player, spec.id(), 400));
        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.commitAcceptedCast(player, spec.id(), 400));
        assertEquals(88.0, states.getOrCreate(player, 400).mana(400), 0.0001);

        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.preflightAttempt(player, spec.id(), 420, true)
        );
        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.commitAcceptedCast(player, spec.id(), 420, true)
        );
        assertEquals(88.0, states.getOrCreate(player, 420).mana(420), 0.0001);

        authority.onEngineCostConsumed(player, spec.id(), 420);
        authority.onEngineCastCompleted(player, spec.id(), 420, "RELEASE", 1.0F);
    }

    @Test
    void staleTransactionCannotBypassCooldownWithoutEngineContinuationProof() {
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        SpellCastAuthority authority = authorityWithArcBolt(states);
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        UUID player = UUID.randomUUID();

        assertEquals(SpellCastAuthority.AttemptDecision.ALLOW, authority.commitAcceptedCast(player, spec.id(), 500));
        assertEquals(
                SpellCastAuthority.AttemptDecision.BLOCK,
                authority.preflightAttempt(player, spec.id(), 510, false)
        );
        assertEquals(
                SpellCastAuthority.AttemptDecision.ALLOW,
                authority.preflightAttempt(player, spec.id(), 510, true)
        );
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
                authority.onImpact(
                        player,
                        spec.id(),
                        5,
                        42,
                        7.5,
                        1.0,
                        sourceSnapshot(),
                        targetSnapshot()
                )
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
                authority.onImpact(
                        player,
                        spec.id(),
                        208,
                        77,
                        9.0,
                        1.0,
                        sourceSnapshot(),
                        targetSnapshot()
                )
        );
        assertEquals(1, impacts.get());
        assertEquals(88.0, states.getOrCreate(player, 208).mana(208), 0.0001);
    }

    @Test
    void directMagicImpactUsesProjectSnapshotsAndIgnoresDonorPower() {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        PlayerCombatStateStore states = new PlayerCombatStateStore();
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        states,
                        ProjectSpellTransactionPolicy.SpellImpactPort.directMagic()
                )
        );

        var result = authority.onImpact(
                UUID.randomUUID(),
                spec.id(),
                1,
                99,
                9999.0,
                17.0,
                sourceSnapshot(),
                targetSnapshot()
        );

        assertEquals(SpellCastAuthority.ImpactDecision.accepted(false, 32.0, 5.0), result);
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

    private static dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction.DamageSourceSnapshot sourceSnapshot() {
        return new dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction.DamageSourceSnapshot(
                8, 30.0, 20.0, 0.0, 1.0
        );
    }

    private static dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction.DamageTargetSnapshot targetSnapshot() {
        return new dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction.DamageTargetSnapshot(
                45.0, 35.0, 1.0, 0.0, 190.0
        );
    }

    private static SpellCastAuthority authorityWithArcBolt(PlayerCombatStateStore states) {
        SpellCastAuthority authority = new SpellCastAuthority("openworld_rpg");
        ProjectSpellSpec spec = ProjectSpellSpec.arcBolt();
        authority.registerPolicy(
                spec.id(),
                new ProjectSpellTransactionPolicy(
                        spec,
                        states,
                        ProjectSpellTransactionPolicy.SpellImpactPort.failClosed()
                )
        );
        return authority;
    }
}
