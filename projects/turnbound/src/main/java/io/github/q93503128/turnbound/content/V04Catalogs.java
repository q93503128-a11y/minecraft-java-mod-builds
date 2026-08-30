package io.github.q93503128.turnbound.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Typed access to the remaining canonical v0.4 data resources. */
public final class V04Catalogs {
    private static final Map<String, Encounter> ENCOUNTERS = encounters();
    private static final Map<String, EquipmentSpec> EQUIPMENT = equipment();
    private static final Map<String, SignatureSpec> SIGNATURES = signatures();
    private static final Map<Integer, RiftFloor> RIFT = rift();
    private static final JsonObject REGIONS = load("/data/turnbound/regions/v04.json");
    private static final JsonObject QUESTS = load("/data/turnbound/quests/v04.json");
    private static final JsonObject LOOT = load("/data/turnbound/loot/v04.json");
    private static final JsonObject STATUSES = load("/data/turnbound/statuses/v04.json");
    private static final JsonObject PASSIVES = load("/data/turnbound/passives/v04.json");
    private static final JsonObject SKILLS = load("/data/turnbound/skills/v04.json");

    private V04Catalogs() {}

    public record Encounter(String id, String label, String region, int level, List<String> enemies, int respawnSeconds, boolean boss) {}
    public record Stat(String type, double value) {}
    public record EquipmentSpec(String id, String tier, String slot, String name, Stat main, Stat sub, String fixedEffect) {}
    public record SignatureSpec(String id, String owner, String name, Stat main, Stat sub, String baseRule, String milestone10, String milestone20) {}
    public record RiftFloor(int floor, int level, List<String> enemies, boolean hardBossPattern) {}
    public record Anchor(String id, double x, double y, double z, float yaw) {}
    public record Region(String id, String name, int minX, int maxX, int minZ, int maxZ, int levelMin, int levelMax, Anchor fastTravel, Anchor bossGate, List<String> facilities) {}

    public static Encounter encounter(String id) {
        Encounter value = ENCOUNTERS.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown encounter " + id);
        return value;
    }
    public static boolean hasEncounter(String id) { return ENCOUNTERS.containsKey(id); }
    public static List<Encounter> encounters() { return List.copyOf(ENCOUNTERS.values()); }
    public static List<Encounter> regionEncounters(String region) { return ENCOUNTERS.values().stream().filter(e -> e.region().equals(region)).toList(); }
    public static EquipmentSpec equipment(String id) { return required(EQUIPMENT, id, "equipment"); }
    public static List<EquipmentSpec> equipment() { return List.copyOf(EQUIPMENT.values()); }
    public static SignatureSpec signature(String id) { return required(SIGNATURES, id, "signature"); }
    public static SignatureSpec signatureFor(String characterId) { return SIGNATURES.values().stream().filter(s -> s.owner().equals(characterId)).findFirst().orElseThrow(); }
    public static RiftFloor riftFloor(int floor) { RiftFloor f = RIFT.get(floor); if (f == null) throw new IllegalArgumentException("Invalid Rift floor " + floor); return f; }
    public static List<RiftFloor> riftFloors() { return List.copyOf(RIFT.values()); }
    public static JsonObject questsRaw() { return QUESTS.deepCopy(); }
    public static JsonObject lootRaw() { return LOOT.deepCopy(); }
    public static JsonObject statusesRaw() { return STATUSES.deepCopy(); }
    public static JsonObject passivesRaw() { return PASSIVES.deepCopy(); }
    public static JsonObject skillsRaw() { return SKILLS.deepCopy(); }

    public static Region region(String id) {
        for (JsonElement e : REGIONS.getAsJsonArray("regions")) {
            JsonObject o = e.getAsJsonObject();
            if (!o.get("id").getAsString().equals(id)) continue;
            Anchor travel = anchor(o.getAsJsonObject("fastTravel"), 0.0F);
            Anchor boss = o.has("bossGate") ? anchor(o.getAsJsonObject("bossGate"), (float)o.getAsJsonObject("bossGate").get("yaw").getAsDouble()) : null;
            List<String> facilities = o.has("facilities") ? strings(o.getAsJsonArray("facilities")) : List.of();
            return new Region(id, o.get("name").getAsString(), o.get("minX").getAsInt(), o.get("maxX").getAsInt(), o.get("minZ").getAsInt(), o.get("maxZ").getAsInt(), o.get("levelMin").getAsInt(), o.get("levelMax").getAsInt(), travel, boss, facilities);
        }
        throw new IllegalArgumentException("Unknown region " + id);
    }

    public static int battleGold(Encounter encounter) {
        if (encounter.boss()) return switch (encounter.enemies().getFirst()) {
            case "B01" -> 12_000; case "B02" -> 18_000; case "B03" -> 24_000; case "B04" -> 32_000; case "B05" -> 50_000; default -> 0;
        };
        int perNormal = 70 + 18 * encounter.level();
        int result = 0;
        for (String id : encounter.enemies()) result += id.startsWith("EL") ? perNormal * 3 : perNormal;
        return result;
    }

    public static int battleXp(Encounter encounter) {
        if (encounter.boss()) return switch (encounter.enemies().getFirst()) {
            case "B01" -> 5_000; case "B02" -> 8_000; case "B03" -> 12_000; case "B04" -> 18_000; case "B05" -> 28_000; default -> 0;
        };
        int perNormal = 80 + 90 * encounter.level();
        int result = 0;
        for (String id : encounter.enemies()) result += id.startsWith("EL") ? perNormal * 3 : perNormal;
        return result;
    }

    public static int bossFirstClearEssence(String bossId) {
        return switch (bossId) { case "B01" -> 60; case "B02" -> 80; case "B03" -> 100; case "B04" -> 150; case "B05" -> 250; default -> 0; };
    }

    public static int riftGold(int floor) {
        int base = 1_000 + 300 * floor;
        return RIFT.get(floor).hardBossPattern() ? base + 5_000 + 500 * floor : base;
    }

    public static int enhanceCost(String tier, int fromLevel) {
        if (fromLevel < 0 || fromLevel >= 20) throw new IllegalArgumentException("Enhancement source level must be 0..19");
        double factor = switch (tier) { case "T1" -> 1.0; case "T2" -> 1.5; case "T3" -> 2.2; case "T4" -> 3.2; case "SIGNATURE" -> 4.0; default -> throw new IllegalArgumentException("Unknown tier " + tier); };
        return (int)Math.round(50.0 * factor * Math.pow(fromLevel + 1, 1.55));
    }

    private static Map<String, Encounter> encounters() {
        Map<String, Encounter> out = new LinkedHashMap<>();
        for (JsonElement e : load("/data/turnbound/battles/v04.json").getAsJsonArray("encounters")) {
            JsonObject o = e.getAsJsonObject();
            Encounter value = new Encounter(o.get("id").getAsString(), o.get("label").getAsString(), o.get("region").getAsString(), o.get("level").getAsInt(), strings(o.getAsJsonArray("enemies")), o.get("respawnSeconds").getAsInt(), o.get("boss").getAsBoolean());
            if (out.put(value.id(), value) != null) throw new IllegalStateException("Duplicate encounter " + value.id());
        }
        return Map.copyOf(out);
    }

    private static Map<String, EquipmentSpec> equipment() {
        Map<String, EquipmentSpec> out = new LinkedHashMap<>();
        for (JsonElement e : load("/data/turnbound/equipment/v04.json").getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            EquipmentSpec value = new EquipmentSpec(o.get("id").getAsString(), o.get("tier").getAsString(), o.get("slot").getAsString(), o.get("name").getAsString(), stat(o.getAsJsonObject("main")), stat(o.getAsJsonObject("sub")), o.get("fixedEffect").getAsString());
            out.put(value.id(), value);
        }
        return Map.copyOf(out);
    }

    private static Map<String, SignatureSpec> signatures() {
        Map<String, SignatureSpec> out = new LinkedHashMap<>();
        for (JsonElement e : load("/data/turnbound/signatures/v04.json").getAsJsonArray("signatures")) {
            JsonObject o = e.getAsJsonObject();
            SignatureSpec value = new SignatureSpec(o.get("id").getAsString(), o.get("owner").getAsString(), o.get("name").getAsString(), stat(o.getAsJsonObject("main")), stat(o.getAsJsonObject("sub")), o.get("baseRule").getAsString(), o.get("m10").getAsString(), o.get("m20").getAsString());
            out.put(value.id(), value);
        }
        return Map.copyOf(out);
    }

    private static Map<Integer, RiftFloor> rift() {
        Map<Integer, RiftFloor> out = new LinkedHashMap<>();
        for (JsonElement e : load("/data/turnbound/battles/rift_v04.json").getAsJsonArray("floors")) {
            JsonObject o = e.getAsJsonObject();
            RiftFloor value = new RiftFloor(o.get("floor").getAsInt(), o.get("level").getAsInt(), strings(o.getAsJsonArray("enemies")), o.get("hardBossPattern").getAsBoolean());
            out.put(value.floor(), value);
        }
        return Map.copyOf(out);
    }

    private static JsonObject load(String resource) {
        try (InputStream stream = V04Catalogs.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND resource " + resource);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != 4) throw new IllegalStateException("Invalid TURNBOUND schema at " + resource);
            return root;
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading " + resource, ex);
        }
    }

    private static Stat stat(JsonObject o) { return new Stat(o.get("type").getAsString(), o.get("value").getAsDouble()); }
    private static Anchor anchor(JsonObject o, float yaw) { return new Anchor(o.get("id").getAsString(), o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble(), yaw); }
    private static List<String> strings(JsonArray array) { List<String> out = new ArrayList<>(); for (JsonElement e : array) out.add(e.getAsString()); return List.copyOf(out); }
    private static <K,V> V required(Map<K,V> map, K key, String kind) { V value = map.get(key); if (value == null) throw new IllegalArgumentException("Unknown " + kind + " " + key); return value; }
}
