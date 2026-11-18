package kr.moonseungjun.anomalousclasses.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;

/** Identity and tentative balance data. Higher-tier entries are NOT enabled until altar work. */
public final class PactCatalog {
    public record Material(ResourceLocation itemId, int count) {
        public Material {
            if (itemId == null || count < 1) throw new IllegalArgumentException("Invalid offering");
        }
        public Item item() {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || BuiltInRegistries.ITEM.getKey(item).equals(ResourceLocation.withDefaultNamespace("air")))
                throw new IllegalStateException("Unregistered offering: " + itemId);
            return item;
        }
    }
    public record Ability(String id, int cooldownSeconds) {}
    public record Patron(String id, String name, int tier, boolean accessible, boolean enabled, double health,
            double attack, double speed, double mining, List<Material> offering,
            List<Material> release, double hpSacrifice, List<Ability> abilities) {
        public Patron {
            offering = List.copyOf(offering);
            release = List.copyOf(release);
            abilities = List.copyOf(abilities);
        }
    }
    private static final Map<String, Patron> ALL = load();
    private PactCatalog() {}
    public static Map<String, Patron> all() { return ALL; }
    public static Patron get(String id) { return ALL.get(id); }
    public static List<Patron> introductory() {
        return ALL.values().stream().filter(Patron::accessible).toList();
    }
    public static long multiplier(int existingContracts) {
        if (existingContracts < 0 || existingContracts >= 63) throw new IllegalArgumentException("Contract count overflow");
        return 1L << existingContracts;
    }
    public static long required(Material material, int existingContracts) {
        return Math.multiplyExact((long) material.count(), multiplier(existingContracts));
    }
    public static double aggregate(List<Double> values) {
        double highest = 0, total = 0;
        for (Double value : values) {
            if (value != null && Double.isFinite(value) && value > 0) {
                total += value;
                highest = Math.max(highest, value);
            }
        }
        return highest + .5 * (total - highest);
    }
    private static Map<String, Patron> load() {
        try (var stream = PactCatalog.class.getResourceAsStream(
                "/data/anomalousclasses/pact_catalog_v1.json")) {
            if (stream == null) throw new IllegalStateException("Missing pact catalog");
            var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            var result = new LinkedHashMap<String, Patron>();
            for (JsonElement value : root.getAsJsonArray("entries")) {
                JsonObject object = value.getAsJsonObject();
                String id = object.get("id").getAsString();
                int tier = object.get("tier").getAsInt();
                JsonObject graces = object.getAsJsonObject("graces");
                boolean accessible = object.get("open_without_altar").getAsBoolean();
                boolean enabled = object.get("enabled").getAsBoolean();
                List<Ability> abilities = new ArrayList<>();
                for (JsonElement ability : object.getAsJsonArray("abilities")) {
                    JsonObject v = ability.getAsJsonObject();
                    abilities.add(new Ability(v.get("id").getAsString(), v.get("cooldown").getAsInt()));
                }
                Patron patron = new Patron(id, object.get("name").getAsString(), tier, accessible, enabled,
                        graces.get("max_health").getAsDouble(), graces.get("attack_damage").getAsDouble(),
                        graces.get("movement_speed").getAsDouble(), graces.get("mining_speed").getAsDouble(),
                        materials(object.getAsJsonArray("offering")),
                        materials(object.getAsJsonArray("release_offering")),
                        object.get("hp_sacrifice").getAsDouble(), abilities);
                if (id.isBlank() || tier < 1 || tier > 4 || (accessible && (tier != 1
                        || patron.offering().isEmpty() || patron.release().isEmpty()))
                        || (enabled && (patron.offering().isEmpty() || patron.release().isEmpty()))
                        || result.putIfAbsent(id, patron) != null)
                    throw new IllegalStateException("Bad pact catalog entry: " + id);
            }
            if (result.size() != 16 || result.values().stream().filter(Patron::accessible).count() != 4
                    || result.values().stream().filter(Patron::enabled).count() != 12)
                throw new IllegalStateException("Expected 16 patrons, 4 accessible introductions, 12 enabled combat identities");
            return Collections.unmodifiableMap(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid pact catalog", failure);
        }
    }
    private static List<Material> materials(JsonArray array) {
        List<Material> result = new ArrayList<>();
        for (JsonElement entry : array) {
            JsonObject material = entry.getAsJsonObject();
            ResourceLocation id = ResourceLocation.parse(material.get("item").getAsString());
            result.add(new Material(id, material.get("count").getAsInt()));
        }
        return result;
    }
}
