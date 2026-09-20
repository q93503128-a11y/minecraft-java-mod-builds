package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalWorldSavedDataTest {
    @Test
    void onboardingFlagsArePlayerScopedAndIdempotent() {
        ExternalWorldSavedData data = new ExternalWorldSavedData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertFalse(data.onboardingFlag(first, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        data.markOnboardingFlag(first, DrehmalContextualOnboarding.HUB_MENU_VIEWED);
        data.markOnboardingFlag(first, DrehmalContextualOnboarding.HUB_MENU_VIEWED);

        assertTrue(data.onboardingFlag(first, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertFalse(data.onboardingFlag(second, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertTrue(data.onboardingFlags(first).contains(DrehmalContextualOnboarding.HUB_MENU_VIEWED));
    }

    @Test
    void malformedFlagsAreRejectedAtTheWriteBoundary() {
        ExternalWorldSavedData data = new ExternalWorldSavedData();
        UUID player = UUID.randomUUID();

        data.markOnboardingFlag(player, "");
        data.markOnboardingFlag(player, "bad|flag");

        assertTrue(data.onboardingFlags(player).isEmpty());
    }
}
