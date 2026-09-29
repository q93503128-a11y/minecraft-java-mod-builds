package io.github.q93503128.turnbound.world;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DrehmalMapPlacementCatalog {
    public static final int SCHEMA_VERSION = 1;
    private static final String RESOURCE = "/data/turnbound/world/capital_valley_map_placement_v1.json";
    private static final Plan PLAN = load();

    public record Seed(int x, int z) {}
    public record ArenaSeed(int x, int z, float yaw) {}
    public record Source(String repository, String commit, String paths, String locations, String towers) {}
    public record Zone(String id, String label, String role, List<Seed> corridor) {
        public Zone { corridor = List.copyOf(corridor); }
    }
    public record Placement(String siteLocator, String zoneId, boolean strictSite, int searchRadius,
                            List<Seed> siteSeeds, List<ArenaSeed> arenaSeeds, List<Seed> patrolSeeds) {
        public Placement {
            siteSeeds = List.copyOf(siteSeeds);
            arenaSeeds = List.copyOf(arenaSeeds);
            patrolSeeds = List.copyOf(patrolSeeds);
        }
    }
    public record Plan(String routeId, Source source, List<Zone> zones, List<Placement> placements) {
        public Plan { zones = List.copyOf(zones); placements = List.copyOf(placements); }
    }

    private DrehmalMapPlacementCatalog() {}
    public static Plan plan() { return PLAN; }

    public static Placement placement(String siteLocator) {
        if (siteLocator == null) return null;
        for (Placement p : PLAN.placements()) if (siteLocator.equals(p.siteLocator())) return p;
        return null;
    }

    public static Zone zone(String zoneId) {
        if (zoneId == null) return null;
        for (Zone zone : PLAN.zones()) if (zoneId.equals(zone.id())) return zone;
        return null;
    }

    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!DrehmalFirstRouteCatalog.ROUTE_ID.equals(PLAN.routeId())) errors.add("map placement route mismatch");
        if (PLAN.source().repository().isBlank() || PLAN.source().commit().length() < 7) errors.add("map placement source incomplete");
        Map<String, Zone> zones = new LinkedHashMap<>();
        for (Zone z : PLAN.zones()) {
            if (z.id().isBlank() || zones.put(z.id(), z) != null) errors.add("duplicate/blank placement zone " + z.id());
            if (z.label().isBlank() || z.role().isBlank() || z.corridor().size() < 2) errors.add("incomplete placement zone " + z.id());
        }
        Set<String> sites = new HashSet<>();
        for (Placement p : PLAN.placements()) {
            if (!sites.add(p.siteLocator())) errors.add("duplicate map placement " + p.siteLocator());
            if (DrehmalFirstRouteCatalog.site(p.siteLocator()) == null) errors.add("unknown placement site " + p.siteLocator());
            Zone zone = zones.get(p.zoneId());
            if (zone == null) errors.add("unknown placement zone " + p.siteLocator());
            if (p.siteSeeds().isEmpty()) errors.add("no placement seeds " + p.siteLocator());
            if (p.searchRadius() < 0 || p.searchRadius() > 24) errors.add("invalid search radius " + p.siteLocator());
            if (zone != null && p.siteSeeds().stream().noneMatch(seed -> near(seed, zone.corridor(), 120))) {
                errors.add("placement escaped zone " + p.siteLocator());
            }
            var encounter = encounterAtSite(p.siteLocator());
            if (encounter != null && p.arenaSeeds().size() < 2) errors.add("encounter needs arena seeds " + p.siteLocator());
            if (encounter != null && !encounter.patrolLocator().isBlank() && p.patrolSeeds().size() < 2) {
                errors.add("patrol needs points " + p.siteLocator());
            }
        }
        return List.copyOf(errors);
    }

    private static DrehmalFirstRouteCatalog.EncounterSlot encounterAtSite(String site) {
        for (var e : DrehmalFirstRouteCatalog.route().encounters()) if (site.equals(e.siteLocator())) return e;
        return null;
    }
    private static boolean near(Seed seed, List<Seed> corridor, double max) {
        double m = max * max;
        for (Seed p : corridor) {
            double dx=seed.x()-p.x(), dz=seed.z()-p.z();
            if (dx*dx+dz*dz<=m) return true;
        }
        return false;
    }

    private static Plan load() {
        try (InputStream stream = DrehmalMapPlacementCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing map placement catalog");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != SCHEMA_VERSION) throw new IllegalStateException("Unsupported map placement schema");
            JsonObject s = root.getAsJsonObject("source");
            Source source = new Source(str(s,"repository"),str(s,"commit"),str(s,"paths"),str(s,"locations"),str(s,"towers"));
            List<Zone> zones = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("zones")) {
                JsonObject z=e.getAsJsonObject();
                zones.add(new Zone(str(z,"id"),str(z,"label"),str(z,"role"),seeds(z.getAsJsonArray("corridor"))));
            }
            List<Placement> placements = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("placements")) {
                JsonObject p=e.getAsJsonObject();
                List<ArenaSeed> arenas=new ArrayList<>();
                for (JsonElement a : p.getAsJsonArray("arenaSeeds")) {
                    JsonObject x=a.getAsJsonObject();
                    arenas.add(new ArenaSeed(x.get("x").getAsInt(),x.get("z").getAsInt(),x.get("yaw").getAsFloat()));
                }
                placements.add(new Placement(str(p,"siteLocator"),str(p,"zoneId"),
                        p.get("strictSite").getAsBoolean(),p.get("searchRadius").getAsInt(),
                        seeds(p.getAsJsonArray("siteSeeds")),arenas,seeds(p.getAsJsonArray("patrolSeeds"))));
            }
            return new Plan(str(root,"routeId"),source,zones,placements);
        } catch (Exception ex) {
            if (ex instanceof RuntimeException r) throw r;
            throw new IllegalStateException("Failed loading map placement catalog", ex);
        }
    }
    private static List<Seed> seeds(JsonArray raw) {
        List<Seed> out=new ArrayList<>();
        for (JsonElement e:raw) { JsonObject p=e.getAsJsonObject(); out.add(new Seed(p.get("x").getAsInt(),p.get("z").getAsInt())); }
        return List.copyOf(out);
    }
    private static String str(JsonObject o,String k) {
        String v=o.get(k).getAsString().trim();
        if(v.isBlank()) throw new IllegalStateException("Blank map placement field "+k);
        return v;
    }
}
