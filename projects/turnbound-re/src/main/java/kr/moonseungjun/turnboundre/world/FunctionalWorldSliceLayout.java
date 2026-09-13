package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.RegionDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinate-free gameplay contract for the first HUB_01 -> REGION_01 functional world slice.
 * Offsets are only a disposable implementation harness; authored definitions keep stable locator ids.
 */
public final class FunctionalWorldSliceLayout {
    public static final String DIMENSION = "minecraft:overworld";
    public static final String HUB_ID = "turnbound_re:hub_01";
    public static final String REGION_ID = "turnbound_re:region_01";

    public enum SiteKind {
        FORGE,
        MINING,
        FARMING,
        FISHING,
        ENCOUNTER
    }

    public record Site(String id, SiteKind kind, String locator, int offsetX, int offsetZ) {
        public Site {
            if (id == null || id.isBlank() || kind == null) throw new IllegalArgumentException("site id/kind required");
            locator = locator == null ? "" : locator;
        }

        public boolean authoredLocator() { return !locator.isBlank(); }
    }

    public static final Site HUB_FORGE = new Site("hub_forge", SiteKind.FORGE, "", 0, 0);
    public static final Site ORE_OUTCROP = new Site(
            "ore_outcrop", SiteKind.MINING, "turnbound_re:region_01/ore_outcrop", 42, -12);
    public static final Site RIVERSIDE_PLOT = new Site(
            "riverside_plot", SiteKind.FARMING, "turnbound_re:region_01/riverside_plot", 42, 12);
    public static final Site RIVER_POOL = new Site(
            "river_pool", SiteKind.FISHING, "turnbound_re:region_01/river_pool", 60, 15);
    public static final Site OVERWORLD_PATROL = new Site(
            "overworld_patrol", SiteKind.ENCOUNTER, "turnbound_re:region_01/overworld_patrol", 52, 0);
    public static final Site RIFT_ELITE = new Site(
            "rift_elite", SiteKind.ENCOUNTER, "turnbound_re:region_01/rift_elite", 72, 0);

    public static final List<Site> SITES = List.of(
            HUB_FORGE,
            ORE_OUTCROP,
            RIVERSIDE_PLOT,
            RIVER_POOL,
            OVERWORLD_PATROL,
            RIFT_ELITE);

    private FunctionalWorldSliceLayout() {}

    /** Validates that every physical stop in the slice still resolves to the current authored data contract. */
    public static List<String> validate(DefinitionRegistry definitions, String dimension) {
        List<String> errors = new ArrayList<>();
        if (definitions == null) return List.of("definitions missing");
        if (!DIMENSION.equals(dimension)) errors.add("functional slice requires " + DIMENSION + ", got " + dimension);

        RegionDefinition hub = definitions.regions().get(HUB_ID);
        RegionDefinition region = definitions.regions().get(REGION_ID);
        if (hub == null) errors.add("missing " + HUB_ID);
        if (region == null) errors.add("missing " + REGION_ID);
        if (hub != null) {
            if (!DIMENSION.equals(hub.dimension())) errors.add(HUB_ID + " dimension mismatch");
            if (!hub.exits().contains(REGION_ID)) errors.add(HUB_ID + " must exit to " + REGION_ID);
        }
        if (region != null) {
            if (!DIMENSION.equals(region.dimension())) errors.add(REGION_ID + " dimension mismatch");
            if (!region.exits().contains(HUB_ID)) errors.add(REGION_ID + " must exit to " + HUB_ID);
        }

        Set<String> locators = new HashSet<>();
        Set<String> positions = new HashSet<>();
        for (Site site : SITES) {
            String position = site.offsetX() + "," + site.offsetZ();
            if (!positions.add(position)) errors.add("duplicate physical site offset " + position);
            if (!site.authoredLocator()) continue;
            if (!locators.add(site.locator())) errors.add("duplicate functional locator " + site.locator());

            if (site.kind() == SiteKind.ENCOUNTER) {
                if (WorldEncounterAnchorResolver.resolve(definitions, site.locator(), DIMENSION).isEmpty()) {
                    errors.add("unresolved encounter locator " + site.locator());
                }
            } else {
                WorldResourceAnchorResolver.Resolved resolved = WorldResourceAnchorResolver
                        .resolve(definitions, site.locator(), DIMENSION).orElse(null);
                if (resolved == null) {
                    errors.add("unresolved resource locator " + site.locator());
                } else if (!site.kind().name().equals(resolved.anchor().activity())) {
                    errors.add("resource activity mismatch " + site.locator()
                            + " expected=" + site.kind().name() + " actual=" + resolved.anchor().activity());
                }
            }
        }
        return List.copyOf(errors);
    }
}
