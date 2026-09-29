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
    private static final Map<String,String> SOURCE_HINTS = Map.of(
            "GREETER", "New Drabyel entrance · hub survey seed 502 67 1801",
            "TRAVEL", "entrance stables · immediately right of the town entrance",
            "MARKET", "merchant frontage · source seed about 516.5 67 1854.5",
            "BLACKSMITH", "Runic Blacksmith / smithy · source seed about 526 65 1840",
            "STORY", "central landmark space near the statue/farmhouse area",
            "SUMMON", "existing interior candidate · no exact source coordinate is promoted"
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
        return SOURCE_HINTS.getOrDefault(role == null ? "" : role.trim().toUpperCase(Locale.ROOT), "no source hint");
    }
}
