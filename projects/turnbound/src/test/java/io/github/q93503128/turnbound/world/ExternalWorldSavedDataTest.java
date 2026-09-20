package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalWorldSavedDataTest {
    @Test
    void onboardingFlagRulesArePlayerScopedAndIdempotentWithoutMinecraftRuntime() {
        Set<String> entries = new LinkedHashSet<>();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertFalse(DrehmalOnboardingFlags.contains(entries, first, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertTrue(DrehmalOnboardingFlags.add(entries, first, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertFalse(DrehmalOnboardingFlags.add(entries, first, DrehmalContextualOnboarding.HUB_MENU_VIEWED));

        assertTrue(DrehmalOnboardingFlags.contains(entries, first, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertFalse(DrehmalOnboardingFlags.contains(entries, second, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertTrue(DrehmalOnboardingFlags.forPlayer(entries, first)
                .contains(DrehmalContextualOnboarding.HUB_MENU_VIEWED));
    }

    @Test
    void malformedPersistedFlagsAreIgnored() {
        UUID player = UUID.randomUUID();
        Set<String> decoded = DrehmalOnboardingFlags.decode(List.of(
                "",
                "not-a-uuid|HUB_MENU_VIEWED",
                player + "|bad|flag",
                player + "|" + DrehmalContextualOnboarding.HUB_MENU_VIEWED));

        assertTrue(DrehmalOnboardingFlags.contains(
                decoded, player, DrehmalContextualOnboarding.HUB_MENU_VIEWED));
        assertFalse(DrehmalOnboardingFlags.contains(decoded, player, "bad|flag"));
    }
}
