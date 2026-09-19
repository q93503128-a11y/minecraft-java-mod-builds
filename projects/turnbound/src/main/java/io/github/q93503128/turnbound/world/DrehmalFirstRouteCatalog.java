package io.github.q93503128.turnbound.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data contract for the first Drehmal production route.
 *
 * <p>Source-backed Drehmal coordinates are survey seeds only. Player-facing placement may become production-enabled
 * only after the exact Minecraft 26.2 position is recorded and {@code verifiedIn26_2} is true.</p>
 */
public final class DrehmalFirstRouteCatalog {
    public static final int SCHEMA_VERSION = 1;
    public static final String ROUTE_ID = "turnbound:route/capital_valley_to_drabyel";
    private static final String RESOURCE = "/data/turnbound/world/capital_valley_route_v1.json";
    private static final Route ROUTE = load();

    public record Position(int x, int y, int z) {}

    public record Site(
            String locator,
            String kind,
            String surveySeedAnchor,
            String playerLabel,
            Position runtimePosition,
            int safetyRadius,
            int encounterRadius,
            boolean verifiedIn26_2,
            boolean productionEnabled
    ) {}

    public record ArenaCandidate(Position center, float yaw) {}

    public record Footprint(
            String locator,
            String siteLocator,
            int radius,
            int allySlots,
            int enemySlots,
            List<ArenaCandidate> candidates,
            boolean verifiedIn26_2,
            boolean productionEnabled
    ) {
        public Footprint {
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
        }
    }

    public record Patrol(
            String locator,
            String surveySeedSite,
            String mode,
            int dwellMinTicks,
            int dwellMaxTicks,
            List<Position> points,
            boolean verifiedIn26_2,
            boolean productionEnabled
    ) {
        public Patrol {
            points = List.copyOf(points);
        }

        /** Compatibility constructor for authored/tests that predate roaming metadata. */
        public Patrol(String locator, String surveySeedSite, List<Position> points,
                      boolean verifiedIn26_2, boolean productionEnabled) {
            this(locator, surveySeedSite, "LOOP", 0, 0, points, verifiedIn26_2, productionEnabled);
        }
    }

    public record EncounterSlot(
            String locator,
            String siteLocator,
            String tier,
            String footprintLocator,
            String patrolLocator,
            String combatEncounterId,
            String playerLabel,
            int fieldVisibleCount,
            boolean verifiedIn26_2,
            boolean productionEnabled
    ) {
        /** Compatibility constructor: one field representative was the historical implicit default. */
        public EncounterSlot(String locator, String siteLocator, String tier, String footprintLocator,
                             String patrolLocator, String combatEncounterId, String playerLabel,
                             boolean verifiedIn26_2, boolean productionEnabled) {
            this(locator, siteLocator, tier, footprintLocator, patrolLocator, combatEncounterId,
                    playerLabel, 1, verifiedIn26_2, productionEnabled);
        }
    }

    public record Route(
            String id,
            String profileId,
            List<Site> sites,
            List<Footprint> footprints,
            List<Patrol> patrols,
            List<EncounterSlot> encounters
    ) {
        public Route {
            sites = List.copyOf(sites);
            footprints = List.copyOf(footprints);
            patrols = List.copyOf(patrols);
            encounters = List.copyOf(encounters);
        }
    }

    private DrehmalFirstRouteCatalog() {}

    public static Route route() {
        return ROUTE;
    }

    public static Site site(String locator) {
        if (locator == null) return null;
        for (Site site : ROUTE.sites()) {
            if (locator.equals(site.locator())) return site;
        }
        return null;
    }

    public static Footprint footprint(String locator) {
        if (locator == null) return null;
        for (Footprint footprint : ROUTE.footprints()) {
            if (locator.equals(footprint.locator())) return footprint;
        }
        return null;
    }

    public static EncounterSlot encounterByCombatId(String encounterId) {
        if (encounterId == null || encounterId.isBlank()) return null;
        for (EncounterSlot encounter : ROUTE.encounters()) {
            if (encounterId.equals(encounter.combatEncounterId())) return encounter;
        }
        return null;
    }

    public static Patrol patrol(String locator) {
        if (locator == null || locator.isBlank()) return null;
        for (Patrol patrol : ROUTE.patrols()) {
            if (locator.equals(patrol.locator())) return patrol;
        }
        return null;
    }

    public static List<Site> productionSites() {
        return ROUTE.sites().stream()
                .filter(Site::productionEnabled)
                .filter(Site::verifiedIn26_2)
                .filter(site -> site.runtimePosition() != null)
                .toList();
    }

    public static List<EncounterSlot> productionEncounters() {
        return ROUTE.encounters().stream()
                .filter(EncounterSlot::productionEnabled)
                .filter(EncounterSlot::verifiedIn26_2)
                .toList();
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!ROUTE_ID.equals(ROUTE.id())) errors.add("unexpected first-route id " + ROUTE.id());
        if (!DrehmalWorldProfile.PROFILE_ID.equals(ROUTE.profileId())) {
            errors.add("first-route profile mismatch " + ROUTE.profileId());
        }

        Map<String, Site> sites = indexSites(errors);
        Map<String, Footprint> footprints = indexFootprints(errors);
        Map<String, Patrol> patrols = indexPatrols(errors);
        Set<String> encounterIds = new HashSet<>();

        for (Site site : ROUTE.sites()) {
            if (site.surveySeedAnchor().isBlank()
                    || DrehmalWorldProfile.enabled(site.surveySeedAnchor()) == null) {
                errors.add("unknown survey seed anchor for site " + site.locator());
            }
            if (site.playerLabel().isBlank()) errors.add("blank player label for site " + site.locator());
            if (site.safetyRadius() < 0 || site.safetyRadius() > 160) {
                errors.add("invalid safety radius for site " + site.locator());
            }
            if (site.encounterRadius() < 0 || site.encounterRadius() > 96) {
                errors.add("invalid encounter radius for site " + site.locator());
            }
            if (site.productionEnabled() && (!site.verifiedIn26_2() || site.runtimePosition() == null)) {
                errors.add("unverified production site " + site.locator());
            }
        }

        for (Footprint footprint : ROUTE.footprints()) {
            if (!sites.containsKey(footprint.siteLocator())) {
                errors.add("footprint references unknown site " + footprint.locator());
            }
            if (footprint.radius() < 6 || footprint.radius() > 48) {
                errors.add("invalid footprint radius " + footprint.locator());
            }
            if (footprint.allySlots() < 1 || footprint.allySlots() > 4) {
                errors.add("invalid ally slots " + footprint.locator());
            }
            if (footprint.enemySlots() < 1 || footprint.enemySlots() > 5) {
                errors.add("invalid enemy slots " + footprint.locator());
            }
            if (footprint.productionEnabled() && !footprint.verifiedIn26_2()) {
                errors.add("unverified production footprint " + footprint.locator());
            }
            if (footprint.productionEnabled() && footprint.candidates().size() < 2) {
                errors.add("production footprint needs two camera-safe arena candidates " + footprint.locator());
            }
        }

        for (Patrol patrol : ROUTE.patrols()) {
            if (!sites.containsKey(patrol.surveySeedSite())) {
                errors.add("patrol references unknown survey site " + patrol.locator());
            }
            if (!Set.of("LOOP", "ROAM").contains(patrol.mode())) {
                errors.add("unknown patrol mode " + patrol.locator() + " -> " + patrol.mode());
            }
            if (patrol.dwellMinTicks() < 0 || patrol.dwellMaxTicks() < patrol.dwellMinTicks()
                    || patrol.dwellMaxTicks() > 200) {
                errors.add("invalid patrol dwell range " + patrol.locator());
            }
            if (patrol.productionEnabled()
                    && (!patrol.verifiedIn26_2() || patrol.points().size() < 2)) {
                errors.add("unverified production patrol " + patrol.locator());
            }
            if (patrol.productionEnabled() && patrol.verifiedIn26_2()) {
                for (Position point : patrol.points()) {
                    if (DrehmalRouteZoneRules.insideSafetyZone(
                            ROUTE.sites(), point.x() + 0.5D, point.z() + 0.5D)) {
                        errors.add("production patrol enters a safety zone " + patrol.locator());
                        break;
                    }
                }
            }
        }

        for (EncounterSlot encounter : ROUTE.encounters()) {
            if (!encounterIds.add(encounter.locator())) {
                errors.add("duplicate encounter locator " + encounter.locator());
            }
            Site site = sites.get(encounter.siteLocator());
            Footprint footprint = footprints.get(encounter.footprintLocator());
            Patrol patrol = encounter.patrolLocator().isBlank() ? null : patrols.get(encounter.patrolLocator());
            if (site == null) errors.add("encounter references unknown site " + encounter.locator());
            if (footprint == null) errors.add("encounter references unknown footprint " + encounter.locator());
            if (!encounter.patrolLocator().isBlank() && patrol == null) {
                errors.add("encounter references unknown patrol " + encounter.locator());
            }
            if (encounter.playerLabel().isBlank()) errors.add("blank encounter label " + encounter.locator());
            if (encounter.fieldVisibleCount() < 1 || encounter.fieldVisibleCount() > 3) {
                errors.add("invalid field visible count " + encounter.locator());
            }
            if (encounter.productionEnabled() && encounter.combatEncounterId().isBlank()) {
                errors.add("production encounter has no combat binding " + encounter.locator());
            }
            if (encounter.productionEnabled()) {
                if (!encounter.verifiedIn26_2()) errors.add("unverified production encounter " + encounter.locator());
                if (site == null || !site.productionEnabled()) {
                    errors.add("production encounter has inactive site " + encounter.locator());
                }
                if (footprint == null || !footprint.productionEnabled()) {
                    errors.add("production encounter has inactive footprint " + encounter.locator());
                }
                if (patrol != null && !patrol.productionEnabled()) {
                    errors.add("production encounter has inactive patrol " + encounter.locator());
                }
            }
        }

        return List.copyOf(errors);
    }

    private static Map<String, Site> indexSites(List<String> errors) {
        Map<String, Site> out = new LinkedHashMap<>();
        for (Site site : ROUTE.sites()) {
            if (site.locator().isBlank()) {
                errors.add("blank first-route site locator");
            } else if (out.put(site.locator(), site) != null) {
                errors.add("duplicate first-route site " + site.locator());
            }
        }
        return out;
    }

    private static Map<String, Footprint> indexFootprints(List<String> errors) {
        Map<String, Footprint> out = new LinkedHashMap<>();
        for (Footprint footprint : ROUTE.footprints()) {
            if (footprint.locator().isBlank()) {
                errors.add("blank footprint locator");
            } else if (out.put(footprint.locator(), footprint) != null) {
                errors.add("duplicate footprint " + footprint.locator());
            }
        }
        return out;
    }

    private static Map<String, Patrol> indexPatrols(List<String> errors) {
        Map<String, Patrol> out = new LinkedHashMap<>();
        for (Patrol patrol : ROUTE.patrols()) {
            if (patrol.locator().isBlank()) {
                errors.add("blank patrol locator");
            } else if (out.put(patrol.locator(), patrol) != null) {
                errors.add("duplicate patrol " + patrol.locator());
            }
        }
        return out;
    }

    private static Route load() {
        try (InputStream stream = DrehmalFirstRouteCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND first-route catalog " + RESOURCE);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            int schema = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : -1;
            if (schema != SCHEMA_VERSION) {
                throw new IllegalStateException("Unsupported first-route schema " + schema);
            }

            List<Site> sites = new ArrayList<>();
            for (JsonElement element : array(root, "sites")) {
                JsonObject raw = element.getAsJsonObject();
                sites.add(new Site(
                        string(raw, "locator"),
                        string(raw, "kind"),
                        string(raw, "surveySeedAnchor"),
                        string(raw, "playerLabel"),
                        position(raw),
                        integer(raw, "safetyRadius", 0),
                        integer(raw, "encounterRadius", 0),
                        bool(raw, "verifiedIn26_2", false),
                        bool(raw, "productionEnabled", false)));
            }

            List<Footprint> footprints = new ArrayList<>();
            for (JsonElement element : array(root, "footprints")) {
                JsonObject raw = element.getAsJsonObject();
                List<ArenaCandidate> candidates = new ArrayList<>();
                if (raw.has("candidates") && raw.get("candidates").isJsonArray()) {
                    for (JsonElement candidateElement : raw.getAsJsonArray("candidates")) {
                        JsonObject candidate = candidateElement.getAsJsonObject();
                        candidates.add(new ArenaCandidate(
                                position(candidate, true),
                                decimal(candidate, "yaw", 0.0F)));
                    }
                }
                footprints.add(new Footprint(
                        string(raw, "locator"),
                        string(raw, "siteLocator"),
                        integer(raw, "radius", -1),
                        integer(raw, "allySlots", -1),
                        integer(raw, "enemySlots", -1),
                        candidates,
                        bool(raw, "verifiedIn26_2", false),
                        bool(raw, "productionEnabled", false)));
            }

            List<Patrol> patrols = new ArrayList<>();
            for (JsonElement element : array(root, "patrols")) {
                JsonObject raw = element.getAsJsonObject();
                List<Position> points = new ArrayList<>();
                if (raw.has("points")) {
                    for (JsonElement point : raw.getAsJsonArray("points")) {
                        points.add(position(point.getAsJsonObject(), true));
                    }
                }
                patrols.add(new Patrol(
                        string(raw, "locator"),
                        string(raw, "surveySeedSite"),
                        stringOr(raw, "mode", "LOOP"),
                        integer(raw, "dwellMinTicks", 0),
                        integer(raw, "dwellMaxTicks", 0),
                        points,
                        bool(raw, "verifiedIn26_2", false),
                        bool(raw, "productionEnabled", false)));
            }

            List<EncounterSlot> encounters = new ArrayList<>();
            for (JsonElement element : array(root, "encounters")) {
                JsonObject raw = element.getAsJsonObject();
                encounters.add(new EncounterSlot(
                        string(raw, "locator"),
                        string(raw, "siteLocator"),
                        string(raw, "tier"),
                        string(raw, "footprintLocator"),
                        optionalString(raw, "patrolLocator"),
                        optionalString(raw, "combatEncounterId"),
                        string(raw, "playerLabel"),
                        integer(raw, "fieldVisibleCount", 1),
                        bool(raw, "verifiedIn26_2", false),
                        bool(raw, "productionEnabled", false)));
            }

            return new Route(
                    string(root, "routeId"),
                    string(root, "profileId"),
                    sites,
                    footprints,
                    patrols,
                    encounters);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND first-route catalog", exception);
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing first-route array " + key);
        }
        return object.getAsJsonArray(key);
    }

    private static Position position(JsonObject raw) {
        if (raw == null || !raw.has("position") || !raw.get("position").isJsonObject()) return null;
        return position(raw.getAsJsonObject("position"), true);
    }

    private static Position position(JsonObject raw, boolean required) {
        if (raw == null) {
            if (required) throw new IllegalStateException("Missing first-route position");
            return null;
        }
        if (!raw.has("x") || !raw.has("y") || !raw.has("z")) {
            if (required) throw new IllegalStateException("Incomplete first-route position");
            return null;
        }
        return new Position(raw.get("x").getAsInt(), raw.get("y").getAsInt(), raw.get("z").getAsInt());
    }

    private static String string(JsonObject object, String key) {
        String value = optionalString(object, key);
        if (value.isBlank()) throw new IllegalStateException("Missing first-route field " + key);
        return value;
    }

    private static String optionalString(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) return "";
        return object.get(key).getAsString().trim();
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        String value = optionalString(object, key);
        return value.isBlank() ? fallback : value;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsInt()
                : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsBoolean()
                : fallback;
    }

    private static float decimal(JsonObject object, String key, float fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsFloat()
                : fallback;
    }
}
