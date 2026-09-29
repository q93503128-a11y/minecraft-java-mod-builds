package io.github.q93503128.turnbound.world;

import com.google.gson.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Source-backed, data-only New Drabyel service placement plan.
 *
 * <p>The source map data is used as a reference only. No external code, map tile or resource-pack asset is bundled.
 * Runtime placement still validates the migrated world before materializing a TURNBOUND service actor.</p>
 */
public final class DrabyelMapPlacementCatalog {
    public static final int SCHEMA_VERSION = 1;
    private static final String RESOURCE = "/data/turnbound/world/new_drabyel_map_placement_v1.json";
    private static final Plan PLAN = load();

    public record Seed(int x, int z) {}
    public record Exclusion(int x, int z, double radius, String reason) {}
    public record Source(String repository, String commit, String paths, String usage) {}
    public record Placement(String serviceLocator, int searchRadius, int expectedY, double preferredRoadDistance,
                            Seed faceTarget, List<Seed> seeds, List<Exclusion> exclusions) {
        public Placement {
            seeds = List.copyOf(seeds);
            exclusions = List.copyOf(exclusions);
        }
    }
    public record Plan(String hubLocator, Source source, List<Placement> placements) {
        public Plan { placements = List.copyOf(placements); }
    }

    private DrabyelMapPlacementCatalog() {}

    public static Plan plan() { return PLAN; }

    public static Placement placement(String serviceLocator) {
        if (serviceLocator == null) return null;
        for (Placement placement : PLAN.placements()) {
            if (serviceLocator.equals(placement.serviceLocator())) return placement;
        }
        return null;
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!DrabyelHubServiceCatalog.HUB_LOCATOR.equals(PLAN.hubLocator())) {
            errors.add("Drabyel placement hub mismatch " + PLAN.hubLocator());
        }
        if (PLAN.source().repository().isBlank() || PLAN.source().commit().length() < 7
                || PLAN.source().paths().isBlank() || PLAN.source().usage().isBlank()) {
            errors.add("Drabyel placement source metadata incomplete");
        }

        Set<String> locators = new HashSet<>();
        for (Placement placement : PLAN.placements()) {
            if (!locators.add(placement.serviceLocator())) {
                errors.add("duplicate Drabyel placement " + placement.serviceLocator());
            }
            if (DrabyelHubServiceCatalog.service(placement.serviceLocator()) == null) {
                errors.add("unknown Drabyel service placement " + placement.serviceLocator());
            }
            if (placement.searchRadius() < 0 || placement.searchRadius() > 8) {
                errors.add("invalid Drabyel placement radius " + placement.serviceLocator());
            }
            if (placement.expectedY() < 1 || placement.expectedY() > 320) {
                errors.add("invalid Drabyel placement expected Y " + placement.serviceLocator());
            }
            if (placement.preferredRoadDistance() < 2.0D || placement.preferredRoadDistance() > 12.0D) {
                errors.add("invalid Drabyel placement road distance " + placement.serviceLocator());
            }
            if (placement.faceTarget() == null) {
                errors.add("missing Drabyel placement face target " + placement.serviceLocator());
            }
            if (placement.seeds().isEmpty()) {
                errors.add("missing Drabyel placement seeds " + placement.serviceLocator());
            }
            for (Exclusion exclusion : placement.exclusions()) {
                if (exclusion.radius() <= 0.0D || exclusion.radius() > 12.0D || exclusion.reason().isBlank()) {
                    errors.add("invalid Drabyel placement exclusion " + placement.serviceLocator());
                }
            }
        }

        for (var service : DrabyelHubServiceCatalog.hub().services()) {
            if (!locators.contains(service.locator())) {
                errors.add("missing Drabyel auto-placement " + service.locator());
            }
        }
        return List.copyOf(errors);
    }

    public static boolean excluded(Placement placement, int x, int z) {
        if (placement == null) return true;
        for (Exclusion exclusion : placement.exclusions()) {
            double dx = x + 0.5D - (exclusion.x() + 0.5D);
            double dz = z + 0.5D - (exclusion.z() + 0.5D);
            if (dx * dx + dz * dz <= exclusion.radius() * exclusion.radius()) return true;
        }
        return false;
    }

    private static Plan load() {
        try (InputStream stream = DrabyelMapPlacementCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing New Drabyel map placement catalog");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != SCHEMA_VERSION) {
                throw new IllegalStateException("Unsupported New Drabyel placement schema");
            }
            JsonObject sourceRaw = root.getAsJsonObject("source");
            Source source = new Source(
                    string(sourceRaw, "repository"),
                    string(sourceRaw, "commit"),
                    string(sourceRaw, "paths"),
                    string(sourceRaw, "usage"));

            List<Placement> placements = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("placements")) {
                JsonObject raw = element.getAsJsonObject();
                Seed target = seed(raw.getAsJsonObject("faceTarget"));
                List<Seed> seeds = new ArrayList<>();
                for (JsonElement seed : raw.getAsJsonArray("seeds")) {
                    seeds.add(seed(seed.getAsJsonObject()));
                }
                List<Exclusion> exclusions = new ArrayList<>();
                if (raw.has("exclusions")) {
                    for (JsonElement exclusionElement : raw.getAsJsonArray("exclusions")) {
                        JsonObject exclusion = exclusionElement.getAsJsonObject();
                        exclusions.add(new Exclusion(
                                exclusion.get("x").getAsInt(),
                                exclusion.get("z").getAsInt(),
                                exclusion.get("radius").getAsDouble(),
                                string(exclusion, "reason")));
                    }
                }
                placements.add(new Placement(
                        string(raw, "serviceLocator"),
                        raw.get("searchRadius").getAsInt(),
                        raw.get("expectedY").getAsInt(),
                        raw.get("preferredRoadDistance").getAsDouble(),
                        target,
                        seeds,
                        exclusions));
            }

            return new Plan(string(root, "hubLocator"), source, placements);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading New Drabyel map placement catalog", exception);
        }
    }

    private static Seed seed(JsonObject raw) {
        if (raw == null || !raw.has("x") || !raw.has("z")) {
            throw new IllegalStateException("Incomplete New Drabyel placement seed");
        }
        return new Seed(raw.get("x").getAsInt(), raw.get("z").getAsInt());
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing New Drabyel placement field " + key);
        }
        String value = object.get(key).getAsString().trim();
        if (value.isBlank()) throw new IllegalStateException("Blank New Drabyel placement field " + key);
        return value;
    }
}
