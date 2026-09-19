package io.github.q93503128.turnbound.world;

/**
 * Field-contact presentation policy for the first Drehmal route.
 *
 * <p>The user-provided R_PG capture established the ordering: see the enemy in-world, get one concise awareness
 * beat, then immediately chase/contact into battle without a preparation menu. The route archetype only changes
 * how long that readable beat lasts; combat composition remains owned by the encounter catalog.</p>
 */
final class DrehmalFieldEncounterPolicy {
    enum Archetype { ROADSIDE_THREAT, OPTIONAL_DANGER, PATROL }

    record Policy(Archetype archetype, int alertPreludeTicks) {}

    private DrehmalFieldEncounterPolicy() {}

    static Policy forEncounter(
            DrehmalFirstRouteCatalog.EncounterSlot encounter,
            DrehmalFirstRouteCatalog.Site site
    ) {
        if (encounter == null || site == null) {
            return new Policy(Archetype.ROADSIDE_THREAT, 12);
        }
        if ("ELITE_ZONE".equals(site.kind()) || "ELITE".equals(encounter.tier())) {
            return new Policy(Archetype.OPTIONAL_DANGER, 18);
        }
        if ("PATROL_ZONE".equals(site.kind()) || !encounter.patrolLocator().isBlank()) {
            return new Policy(Archetype.PATROL, 10);
        }
        return new Policy(Archetype.ROADSIDE_THREAT, 12);
    }
}
