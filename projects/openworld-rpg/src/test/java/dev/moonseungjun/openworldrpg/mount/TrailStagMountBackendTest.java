package dev.moonseungjun.openworldrpg.mount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class TrailStagMountBackendTest {
    @Test
    void firstUnlockIsPermanentSelectedAndCodecStable() {
        var unlocked = TrailStagMountState.initial().unlockTrailStag();
        assertTrue(unlocked.trailStagUnlocked());
        assertTrue(unlocked.trailStagSelected());
        assertEquals(unlocked, unlocked.unlockTrailStag());

        var encoded = TrailStagMountState.CODEC
                .encodeStart(JsonOps.INSTANCE, unlocked)
                .getOrThrow();
        assertEquals(
                unlocked,
                TrailStagMountState.CODEC
                        .parse(JsonOps.INSTANCE, encoded)
                        .getOrThrow()
        );
    }

    @Test
    void resolveBreakUsesExactTwelveSecondResummonCooldown() {
        var state = TrailStagMountState.initial()
                .unlockTrailStag()
                .recordResolveBreak(1_000L);
        assertEquals(240L, state.forcedCooldownTicksRemaining(1_000L));
        assertEquals(1L, state.forcedCooldownTicksRemaining(1_239L));
        assertEquals(0L, state.forcedCooldownTicksRemaining(1_240L));
    }

    @Test
    void freshResolveMatchesRiderMaxHpAndReportsOnlyTrueOverflow() {
        var fresh = TrailStagResolveState.fresh(100.0);
        assertEquals(100.0, fresh.maximum());
        assertEquals(100.0, fresh.current());

        var first = fresh.apply(35.0);
        assertEquals(65.0, first.state().current());
        assertEquals(35.0, first.resolveConsumed());
        assertEquals(0.0, first.overflowDamage());
        assertFalse(first.brokeResolve());

        var breaking = first.state().apply(80.0);
        assertEquals(0.0, breaking.state().current());
        assertEquals(65.0, breaking.resolveConsumed());
        assertEquals(15.0, breaking.overflowDamage());
        assertTrue(breaking.brokeResolve());
    }

    @Test
    void movementAndSummonSeedsStayOnCanonValues() {
        assertEquals(20, TrailStagMountRules.SUMMON_CHANNEL_TICKS);
        assertEquals(80, TrailStagMountRules.HOSTILE_COMBAT_QUIET_TICKS);
        assertEquals(6.4, TrailStagMountRules.CRUISE_BLOCKS_PER_SECOND);
        assertEquals(1.25, TrailStagMountRules.MAX_AUTHORED_STEP_UP_BLOCKS);
        assertEquals(1.65, TrailStagMountRules.JUMP_APEX_BLOCKS);
        assertEquals(0.85, TrailStagMountRules.JUMP_HORIZONTAL_VELOCITY_RETENTION);
        assertEquals(3, TrailStagMountRules.JUMP_LANDING_RECOVERY_TICKS);
    }
}
