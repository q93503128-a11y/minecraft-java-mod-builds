package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorReactionCapabilities;
import org.junit.jupiter.api.Test;

class ExternalActorReactionCapabilitiesTest {
    @Test
    void legacyAndBossProfilesFailClosedForForcedMovement() {
        var legacy = new ExternalActorCombatProfile(
                "example:legacy",
                8,
                100.0F,
                10.0,
                10.0,
                40.0
        );
        assertFalse(legacy.reactionCapabilities().pullToward());
        assertFalse(legacy.reactionCapabilities().knockdown());
        assertFalse(legacy.reactionCapabilities().launch());

        var earthloong =
                ExternalActorCombatProfile.r01Earthloong();
        assertFalse(
                earthloong.reactionCapabilities().pullToward()
        );
    }

    @Test
    void normalActorCapabilityMustBeExplicitlyAdmitted() {
        var capabilities =
                ExternalActorReactionCapabilities.normalBaseline();
        assertTrue(capabilities.pullToward());
        assertTrue(capabilities.knockdown());
        assertTrue(capabilities.launch());
    }
}
