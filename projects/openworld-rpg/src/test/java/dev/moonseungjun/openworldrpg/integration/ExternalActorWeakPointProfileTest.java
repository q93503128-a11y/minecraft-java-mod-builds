package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorWeakPointProfile;
import org.junit.jupiter.api.Test;

class ExternalActorWeakPointProfileTest {
    @Test
    void emptyProfileAlwaysFailsClosed() {
        var profile = ExternalActorWeakPointProfile.none();
        assertTrue(profile.isEmpty());
        assertFalse(profile.containsNormalized(0.0, 0.9, 0.0));
    }

    @Test
    void authoredZoneAcceptsOnlyExplicitLocalVolume() {
        var profile = ExternalActorWeakPointProfile.of(
                new ExternalActorWeakPointProfile.Zone(
                        -0.45, 0.45,
                        0.72, 1.08,
                        -0.40, 0.55
                )
        );
        assertTrue(profile.containsNormalized(0.0, 0.90, 0.1));
        assertFalse(profile.containsNormalized(0.0, 0.60, 0.1));
        assertFalse(profile.containsNormalized(0.8, 0.90, 0.1));
    }

    @Test
    void malformedAuthoredVolumesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExternalActorWeakPointProfile.Zone(
                        1.0, 1.0,
                        0.5, 1.0,
                        -0.5, 0.5
                )
        );
    }
}
