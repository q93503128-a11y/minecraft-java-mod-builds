package io.github.q93503128.turnbound.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Source-backed New Drabyel service survey hints.
 *
 * <p>These hints are navigation aids only. They never become runtime NPC coordinates until the migrated 26.2 world
 * is inspected in-client and the service catalog is explicitly promoted.</p>
 */
public final class DrabyelHubSurveyPlan {
    public record SourceSeed(String label, Integer x, Integer y, Integer z, String note) {
        public String coordinateText() {
            return x == null || y == null || z == null ? "no exact source coordinate" : x + " " + y + " " + z;
        }
    }

    private static final Map<String,String> SOURCE_HINTS = Map.of(
            "GREETER", "northern approach / town-entry sightline; 502 67 1801 is the town reference, not a certified doorway block",
            "TRAVEL", "stables are immediately to the right on entry; preserve horse access and doorway clearance",
            "MARKET", "Adventuring Merchant frontage is the primary TURNBOUND market candidate; central booths are secondary context",
            "BLACKSMITH", "Goibhniu's Smithy / Runic Blacksmith is the strongest authored forge anchor",
            "STORY", "use the statue/farmhouse/church-side landmark space without blocking source signs, graveyard or original loot",
            "SUMMON", "pick an existing interior only after inspection; church/artifact space and Cat Map basement are source-content conflict zones"
    );

    private static final Map<String,List<SourceSeed>> SOURCE_SEEDS = Map.of(
            "GREETER", List.of(
                    new SourceSeed("New Drabyel town reference",502,67,1801,"approach survey seed only")),
            "TRAVEL", List.of(
                    new SourceSeed("entry/stables search origin",502,67,1801,"stables are immediately right on entry; exact stable block is not documented")),
            "MARKET", List.of(
                    new SourceSeed("Adventuring Merchant",516,67,1851,"orange/red awning; preferred service context"),
                    new SourceSeed("Oak Merchant",530,67,1833,"central booth cluster reference"),
                    new SourceSeed("Coal Merchant",532,67,1838,"central booth cluster reference"),
                    new SourceSeed("Wheat Merchant",541,67,1830,"central booth cluster reference")),
            "BLACKSMITH", List.of(
                    new SourceSeed("Runic Blacksmith",526,65,1841,"Goibhniu's Smithy; blue-awning exterior smithy")),
            "STORY", List.of(
                    new SourceSeed("farmhouse / statue search zone",null,null,null,"statue is in front of the farmhouse; church is south of town")),
            "SUMMON", List.of(
                    new SourceSeed("known occupied farmhouse basement",516,65,1861,"Cat Map location; inspect as a conflict/exclusion reference, not a default summon placement"))
    );

    private DrabyelHubSurveyPlan() {}

    public static List<String> serviceSeedLines() {
        List<String> lines = new ArrayList<>();
        for (DrabyelHubServiceCatalog.Service service : DrabyelHubServiceCatalog.hub().services()) {
            String state = service.productionEnabled()
                    ? "PRODUCTION"
                    : service.verifiedIn26_2() ? "VERIFIED" : "PENDING";
            lines.add(service.role() + " · " + service.playerLabel()
                    + " · zone " + service.zone()
                    + " · " + state
                    + " · " + sourceHint(service.role()));
        }
        return List.copyOf(lines);
    }

    public static String sourceHint(String role) {
        return SOURCE_HINTS.getOrDefault(normalize(role), "no source hint");
    }

    public static List<SourceSeed> sourceSeeds(String role) {
        return SOURCE_SEEDS.getOrDefault(normalize(role), List.of());
    }

    public static List<String> sourceSeedLines(String role) {
        String normalized = normalize(role);
        List<String> out = new ArrayList<>();
        for (SourceSeed seed : sourceSeeds(normalized)) {
            out.add(normalized + " · " + seed.label() + " · " + seed.coordinateText() + " · " + seed.note());
        }
        return List.copyOf(out);
    }

    private static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
