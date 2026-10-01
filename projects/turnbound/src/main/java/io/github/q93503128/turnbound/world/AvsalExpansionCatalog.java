package io.github.q93503128.turnbound.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Source-backed route authoring for New Drabyel -> Av'Sal outskirts. */
public final class AvsalExpansionCatalog {
    public static final int SCHEMA_VERSION = 1;
    public static final String ROUTE_ID = "turnbound:route/new_drabyel_to_avsal_outskirts";
    private static final String RESOURCE = "/data/turnbound/world/avsal_route_v1.json";
    private static final Plan PLAN = load();

    public record Source(String repository, String commit, String paths, String startLandmark, String destinationLandmark) {}
    public record Point(int x, int z) {}
    public record SitePlan(
            String locator,
            String kind,
            String playerLabel,
            Point seed,
            int searchRadius,
            int safetyRadius,
            int encounterRadius,
            int detectionRadius,
            String encounterLocator,
            String combatEncounterId,
            String tier,
            List<Point> patrolSeeds
    ) {
        public SitePlan { patrolSeeds = List.copyOf(patrolSeeds); }
        public boolean encounter() { return !combatEncounterId.isBlank(); }
    }
    public record Plan(String routeId, Source source, List<Point> corridor, List<SitePlan> sites) {
        public Plan {
            corridor = List.copyOf(corridor);
            sites = List.copyOf(sites);
        }
    }

    private AvsalExpansionCatalog() {}

    public static Plan plan() { return PLAN; }

    public static SitePlan site(String locator) {
        if (locator == null) return null;
        for (SitePlan site : PLAN.sites()) if (locator.equals(site.locator())) return site;
        return null;
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!ROUTE_ID.equals(PLAN.routeId())) errors.add("unexpected Av'Sal route id " + PLAN.routeId());
        Source source = PLAN.source();
        if (source.repository().isBlank() || source.commit().length() < 7 || source.paths().isBlank()
                || source.startLandmark().isBlank() || source.destinationLandmark().isBlank()) {
            errors.add("Av'Sal route source metadata incomplete");
        }
        if (PLAN.corridor().size() < 8) errors.add("Av'Sal source corridor too short");
        if (!PLAN.corridor().isEmpty()) {
            if (distance(PLAN.corridor().getFirst(), new Point(530, 1848)) > 80.0D) {
                errors.add("Av'Sal corridor does not begin near New Drabyel");
            }
            if (distance(PLAN.corridor().getLast(), new Point(-162, 1413)) > 80.0D) {
                errors.add("Av'Sal corridor does not end near the outskirts source landmark");
            }
        }

        Set<String> locators = new HashSet<>();
        Set<String> encounterLocators = new HashSet<>();
        int common = 0;
        int elite = 0;
        int boss = 0;
        for (SitePlan site : PLAN.sites()) {
            if (site.locator().isBlank() || !locators.add(site.locator())) errors.add("duplicate/blank Av'Sal site " + site.locator());
            if (site.playerLabel().isBlank()) errors.add("blank Av'Sal site label " + site.locator());
            if (site.searchRadius() < 0 || site.searchRadius() > 24) errors.add("invalid Av'Sal search radius " + site.locator());
            if (site.safetyRadius() < 0 || site.safetyRadius() > 96) errors.add("invalid Av'Sal safety radius " + site.locator());
            if (site.encounterRadius() < 0 || site.encounterRadius() > 96) errors.add("invalid Av'Sal encounter radius " + site.locator());
            if (site.detectionRadius() < 12 || site.detectionRadius() > 96) errors.add("invalid Av'Sal detection radius " + site.locator());
            if (site.encounter()) {
                if (site.encounterLocator().isBlank() || !encounterLocators.add(site.encounterLocator())) {
                    errors.add("duplicate/blank Av'Sal encounter locator " + site.locator());
                }
                if (!CampaignEncounterCatalog.contains(site.combatEncounterId())) {
                    errors.add("unknown Av'Sal combat encounter " + site.locator() + " -> " + site.combatEncounterId());
                }
                if (!Set.of("COMMON", "ELITE", "BOSS").contains(site.tier())) errors.add("invalid Av'Sal tier " + site.locator());
                if (site.patrolSeeds().size() < 2) errors.add("Av'Sal encounter needs patrol seeds " + site.locator());
                if ("COMMON".equals(site.tier())) common++;
                if ("ELITE".equals(site.tier())) elite++;
                if ("BOSS".equals(site.tier())) boss++;
            } else if (!site.encounterLocator().isBlank() || !site.tier().isBlank()) {
                errors.add("non-combat Av'Sal site carries encounter metadata " + site.locator());
            }
        }
        if (common < 1) errors.add("Av'Sal first slice needs a common route encounter");
        if (elite < 1) errors.add("Av'Sal first slice needs an optional elite encounter");
        if (boss < 1) errors.add("Av'Sal first slice needs its first boss encounter");
        return List.copyOf(errors);
    }

    private static Plan load() {
        try (InputStream stream = AvsalExpansionCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND Av'Sal route catalog " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != SCHEMA_VERSION) throw new IllegalStateException("Unsupported Av'Sal route schema");
            JsonObject rawSource = root.getAsJsonObject("source");
            Source source = new Source(
                    required(rawSource, "repository"), required(rawSource, "commit"), required(rawSource, "paths"),
                    required(rawSource, "startLandmark"), required(rawSource, "destinationLandmark"));
            List<Point> corridor = points(root.getAsJsonArray("corridor"));
            List<SitePlan> sites = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("sites")) {
                JsonObject raw = element.getAsJsonObject();
                JsonObject seed = raw.getAsJsonObject("seed");
                sites.add(new SitePlan(
                        required(raw, "locator"), required(raw, "kind"), required(raw, "playerLabel"),
                        new Point(seed.get("x").getAsInt(), seed.get("z").getAsInt()),
                        integer(raw, "searchRadius", 12), integer(raw, "safetyRadius", 0),
                        integer(raw, "encounterRadius", 0), integer(raw, "detectionRadius", 28),
                        optional(raw, "encounterLocator"), optional(raw, "combatEncounterId"), optional(raw, "tier"),
                        raw.has("patrolSeeds") ? points(raw.getAsJsonArray("patrolSeeds")) : List.of()));
            }
            return new Plan(required(root, "routeId"), source, corridor, sites);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND Av'Sal route catalog", ex);
        }
    }

    private static List<Point> points(JsonArray raw) {
        List<Point> out = new ArrayList<>();
        if (raw == null) return List.of();
        for (JsonElement element : raw) {
            JsonObject point = element.getAsJsonObject();
            out.add(new Point(point.get("x").getAsInt(), point.get("z").getAsInt()));
        }
        return List.copyOf(out);
    }

    private static String required(JsonObject raw, String key) {
        String value = optional(raw, key);
        if (value.isBlank()) throw new IllegalStateException("Missing Av'Sal route field " + key);
        return value;
    }

    private static String optional(JsonObject raw, String key) {
        return raw != null && raw.has(key) && !raw.get(key).isJsonNull() ? raw.get(key).getAsString().trim() : "";
    }

    private static int integer(JsonObject raw, String key, int fallback) {
        return raw != null && raw.has(key) ? raw.get(key).getAsInt() : fallback;
    }

    private static double distance(Point a, Point b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }
}
