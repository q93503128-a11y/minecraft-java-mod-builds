package io.github.q93503128.turnbound.world;

/** Minecraft-free save-migration predicates for the first Drehmal route. */
final class DrehmalStartMigrationRules {
    private DrehmalStartMigrationRules() {}

    static boolean shouldMigrateLegacyHubArrival(
            boolean hasCurrentArrival,
            boolean hasLegacyHubEvidence,
            boolean insideHub
    ) {
        return !hasCurrentArrival && hasLegacyHubEvidence && insideHub;
    }
}
