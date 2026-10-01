package dev.moonseungjun.openworldrpg.combat.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorReactionCapabilities;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorWeakPointProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MageAstralConvergenceRuntimeTest {
    @Test
    void fiveBasePulsesPreserveLockedTotals() {
        assertEquals(
                ProjectSpellSpec.ASTRAL_CONVERGENCE_ACTION_COEFFICIENT,
                MageAstralConvergenceRuntime.ACTION_COEFFICIENT_PER_PULSE
                        * MageAstralConvergenceRuntime.PULSE_COUNT,
                0.0001
        );
        assertEquals(
                ProjectSpellSpec.ASTRAL_CONVERGENCE_POISE_COEFFICIENT,
                MageAstralConvergenceRuntime.POISE_COEFFICIENT_PER_PULSE
                        * MageAstralConvergenceRuntime.PULSE_COUNT,
                0.0001
        );
        assertEquals(100L, MageAstralConvergenceRuntime.DURATION_TICKS);
        assertEquals(20L, MageAstralConvergenceRuntime.PULSE_INTERVAL_TICKS);
        assertEquals(5, MageAstralConvergenceRuntime.PULSE_COUNT);
    }

    @Test
    void weaveScalesOnlyTheFinalDetonationBonus() {
        double baseAction =
                MageAstralConvergenceRuntime.ACTION_COEFFICIENT_PER_PULSE;
        double basePoise =
                MageAstralConvergenceRuntime.POISE_COEFFICIENT_PER_PULSE;

        assertEquals(
                baseAction,
                MageAstralConvergenceRuntime.actionCoefficientForPulse(
                        false,
                        true,
                        1.20
                ),
                0.0001
        );
        assertEquals(
                baseAction * (1.0 + 0.15 * 1.20),
                MageAstralConvergenceRuntime.actionCoefficientForPulse(
                        true,
                        true,
                        1.20
                ),
                0.0001
        );
        assertEquals(
                basePoise * (1.0 + 0.25 * 1.20),
                MageAstralConvergenceRuntime.poiseCoefficientForPulse(
                        true,
                        true,
                        1.20
                ),
                0.0001
        );
    }

    @Test
    void authoredPullMultiplierCanRepresentNormalEliteAndBossRules() {
        var normal = profileWithPull(1.0);
        var elite = profileWithPull(0.5);
        var immovable = new ExternalActorCombatProfile(
                "openworld_rpg:test_boss",
                10,
                100.0F,
                0.0,
                0.0,
                10.0,
                ExternalActorCombatProfile.CombatRank.BOSS,
                ExternalActorReactionCapabilities.none(),
                ExternalActorWeakPointProfile.none(),
                true
        );

        assertEquals(
                0.40,
                MageAstralConvergenceRuntime.maximumPullBlocks(normal),
                0.0001
        );
        assertEquals(
                0.20,
                MageAstralConvergenceRuntime.maximumPullBlocks(elite),
                0.0001
        );
        assertEquals(
                0.0,
                MageAstralConvergenceRuntime.maximumPullBlocks(immovable),
                0.0001
        );
    }

    @Test
    void radiusUsesRealBoundingBoxAndVerticalEnvelope() {
        Vec3 origin = new Vec3(0.0, 64.0, 0.0);
        assertTrue(
                MageAstralConvergenceRuntime.contains(
                        origin,
                        new AABB(
                                5.8,
                                63.5,
                                -0.2,
                                6.2,
                                65.5,
                                0.2
                        )
                )
        );
        assertFalse(
                MageAstralConvergenceRuntime.contains(
                        origin,
                        new AABB(
                                6.1,
                                63.5,
                                -0.2,
                                6.5,
                                65.5,
                                0.2
                        )
                )
        );
        assertFalse(
                MageAstralConvergenceRuntime.contains(
                        origin,
                        new AABB(
                                -0.2,
                                67.1,
                                -0.2,
                                0.2,
                                68.0,
                                0.2
                        )
                )
        );
    }

    private static ExternalActorCombatProfile profileWithPull(
            double multiplier
    ) {
        return new ExternalActorCombatProfile(
                "openworld_rpg:test_actor_" + multiplier,
                10,
                100.0F,
                0.0,
                0.0,
                10.0,
                ExternalActorCombatProfile.CombatRank.NORMAL_ELITE,
                new ExternalActorReactionCapabilities(
                        true,
                        false,
                        false,
                        false,
                        multiplier
                ),
                ExternalActorWeakPointProfile.none(),
                false
        );
    }
}
