package dev.moonseungjun.augmentationsurvival.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Version-pinned, read-only World Bosses integration. Mutates only the in-memory
 * common reward entries returned by its canonical config API, not the dependency
 * JAR, end-user config files, or original World Bosses-exclusive item weights.
 *
 * World Bosses 2.0.2 calls RaidCacheLootConfig.get for both rollReward and
 * buildLootPool, so the same effective pool is authoritative for UI and rolls.
 */
public final class WorldBossesRaidCacheCoreIntegration {
    private static final String RESOURCE =
            "data/augmentation_survival/augmentation/worldbosses_raid_cache_core_pool.json";
    private static final String EXPECTED_MOD = "worldbosses";
    private static final String CACHE_CONFIG = "com.mki.worldbosses.config.RaidCacheLootConfig";
    private static final String ENTRY_CLASS = CACHE_CONFIG + "$LootEntry";
    private static final String RARITY_CLASS = "com.mki.worldbosses.server.CacheRarity";
    private static final int MAX_VANILLA_SLOTS = 30;

    private static final Set<String> REPORTED = new HashSet<>();
    private static final ThreadLocal<Boolean> BUILDING_FULL_PREVIEW =
            ThreadLocal.withInitial(() -> false);
    private static final Settings SETTINGS = loadSettings();
    private static boolean versionChecked;
    private static boolean supportedVersion;

    private WorldBossesRaidCacheCoreIntegration() { }

    /** Called only by the optional mixin at the third-party cache config return boundary. */
    public static void applyPool(String cacheId, Object definition) {
        if (definition == null || !SETTINGS.cacheIds().contains(cacheId) || !compatibleVersion()) return;
        synchronized (definition) {
            try {
                @SuppressWarnings("unchecked")
                Map<Object, Object> categories =
                        (Map<Object, Object>) call(definition, "categories");
                Object common = null;
                Object commonKey = null;
                int totalCategoryWeight = 0;
                for (var pair : categories.entrySet()) {
                    Object category = pair.getValue();
                    if (category == null || !getBool(category, "enabled")) continue;
                    int weight = getInt(category, "categoryWeight");
                    if (weight <= 0) continue;
                    totalCategoryWeight += weight;
                    if (((Enum<?>) pair.getKey()).name().equals("COMMON")) {
                        common = category;
                        commonKey = pair.getKey();
                    }
                }
                if (common == null || commonKey == null || totalCategoryWeight <= 0) return;
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) get(common, "rewards");
                if (raw == null || raw.isEmpty()) return;
                for (Object entry : raw) {
                    if (entry != null && SETTINGS.coreItemIds().contains(getString(entry, "item"))) {
                        // Idempotent: get() is called repeatedly during each reel animation.
                        return;
                    }
                }
                int commonCategoryWeight = getInt(common, "categoryWeight");
                long totalEntryWeight = 0;
                long fillerWeight = 0;
                List<Object> fillerEntries = new ArrayList<>();
                for (Object entry : raw) {
                    if (!isValid(entry)) continue;
                    int weight = getInt(entry, "weight");
                    totalEntryWeight += weight;
                    if (SETTINGS.fillerIds().contains(getString(entry, "item"))) {
                        fillerEntries.add(entry);
                        fillerWeight += weight;
                    }
                }
                if (totalEntryWeight <= 0 || fillerWeight <= 0) {
                    warn(cacheId, "No removable ordinary fillers in COMMON category");
                    return;
                }
                int[] inserted = new int[SETTINGS.cores().size()];
                int reserved = 0;
                for (int i = 0; i < inserted.length; i++) {
                    Core core = SETTINGS.cores().get(i);
                    // Derivation: (categoryWeight / totalCategories) *
                    //     (entryWeight / totalEntryWeight) = desired final chance.
                    double weight = (core.basisPoints() / 10000.0)
                            * totalCategoryWeight / commonCategoryWeight
                            * totalEntryWeight * SETTINGS.scale();
                    inserted[i] = Math.max(1, (int) Math.round(weight));
                    reserved += inserted[i];
                }
                long scaledFiller = fillerWeight * SETTINGS.scale();
                // Never take more than 25% of the selected ordinary outcomes.
                if (reserved >= scaledFiller / 4 || reserved >= Integer.MAX_VALUE) {
                    warn(cacheId, "Ordinary filler budget is insufficient; base rewards untouched");
                    return;
                }
                Map<Object, Integer> updatedWeights = new HashMap<>();
                long removed = 0;
                for (int i = 0; i < fillerEntries.size(); i++) {
                    Object entry = fillerEntries.get(i);
                    long original = (long) getInt(entry, "weight") * SETTINGS.scale();
                    long take = (i == fillerEntries.size() - 1)
                            ? reserved - removed
                            : Math.round((double) reserved * getInt(entry, "weight") / fillerWeight);
                    long value = original - take;
                    if (value < 1 || value >= Integer.MAX_VALUE) {
                        warn(cacheId, "Unexpected filler weight after normalization");
                        return;
                    }
                    updatedWeights.put(entry, (int) value);
                    removed += take;
                }
                if (removed != reserved) {
                    warn(cacheId, "Reallocation conservation failed");
                    return;
                }

                // Apply atomically after all validations; worldbosses:* entries are NEVER reduced.
                List<Object> revised = new ArrayList<>(raw);
                for (Object entry : revised) {
                    if (!isValid(entry)) continue;
                    int old = getInt(entry, "weight");
                    long scaled = (long) old * SETTINGS.scale();
                    if (scaled >= Integer.MAX_VALUE) {
                        warn(cacheId, "Too-large user-authored weight; fail closed");
                        return;
                    }
                }
                for (Object entry : revised) {
                    if (!isValid(entry)) continue;
                    setInt(entry, "weight", updatedWeights.getOrDefault(entry,
                            getInt(entry, "weight") * SETTINGS.scale()));
                }
                Constructor<?> entryCtor = Class.forName(ENTRY_CLASS).getConstructor(
                        String.class, String.class, int.class, int.class,
                        int.class, int.class, boolean.class);
                for (int i = 0; i < inserted.length; i++) {
                    Core core = SETTINGS.cores().get(i);
                    revised.add(entryCtor.newInstance(
                            core.item(), core.label(), 1, 1, 0, inserted[i], true));
                }
                set(common, "rewards", revised);

                long totalAfter = 0;
                for (Object entry : revised) if (isValid(entry)) totalAfter += getInt(entry, "weight");
                if (totalAfter != totalEntryWeight * SETTINGS.scale()) {
                    throw new IllegalStateException("Cache pool weight sum changed");
                }
                if (REPORTED.add(cacheId)) {
                    AugmentationSurvival.LOGGER.info(
                            "Integrated World Bosses raid cache {}: 3 visible core rewards; {} / {} / {} bp",
                            cacheId,
                            SETTINGS.cores().get(0).basisPoints(),
                            SETTINGS.cores().get(1).basisPoints(),
                            SETTINGS.cores().get(2).basisPoints());
                }
            } catch (ReflectiveOperationException | ClassCastException | IllegalStateException ex) {
                warn(cacheId, "External cache schema mismatch: " + ex);
            }
        }
    }

    /** Only the presentation rarity changes; the server already picked its item from the real pool. */
    public static Object decorateReward(Object reward) {
        if (reward == null || !compatibleVersion()) return reward;
        try {
            Item item = (Item) call(reward, "item");
            Core core = coreFor(item);
            if (core == null) return reward;
            Object oldRarity = call(reward, "rarity");
            Object newRarity = rarity(core.displayRarity());
            if (oldRarity == newRarity) return reward;
            return reward.getClass().getConstructor(
                    oldRarity.getClass(), String.class, Item.class, int.class, int.class).newInstance(
                    newRarity, call(reward, "label"), item,
                    (int) call(reward, "count"), (int) call(reward, "xpBonus"));
        } catch (ReflectiveOperationException | ClassCastException ex) {
            warn("reward", "Unable to decorate won core: " + ex);
            return reward;
        }
    }

    /**
     * World Bosses' menu has 30 slots. If original pool exceeds the cap, reserve
     * visibility for ALL boss-exclusive entries and new cores; trim only
     * ordinary candidates that already have the least UI priority.
     */
    public static List<?> decoratePreview(String cacheId, int limit, List<?> displayed) {
        if (displayed == null || !SETTINGS.cacheIds().contains(cacheId)
                || !compatibleVersion() || BUILDING_FULL_PREVIEW.get()) return displayed;
        try {
            List<?> full = displayed;
            // The original code truncates by rarity before returning, potentially hiding cores.
            BUILDING_FULL_PREVIEW.set(true);
            try {
                Method method = Class.forName(CACHE_CONFIG).getMethod(
                        "buildLootPool", String.class, int.class);
                Object all = method.invoke(null, cacheId, 0);
                if (all instanceof List<?> result) full = result;
            } finally {
                BUILDING_FULL_PREVIEW.set(false);
            }
            List<Object> retyped = new ArrayList<>(full.size());
            Class<?> poolClass = Class.forName(CACHE_CONFIG + "$CachePoolEntry");
            Class<?> rarityClass = Class.forName(RARITY_CLASS);
            Constructor<?> ctor = poolClass.getConstructor(
                    rarityClass, Item.class, String.class,
                    int.class, int.class, int.class, int.class);
            for (Object entry : full) {
                Item item = (Item) call(entry, "item");
                Core core = coreFor(item);
                if (core == null) {
                    retyped.add(entry);
                } else {
                    retyped.add(ctor.newInstance(
                            rarity(core.displayRarity()), item, call(entry, "label"),
                            (int) call(entry, "minCount"), (int) call(entry, "maxCount"),
                            (int) call(entry, "xpBonus"), (int) call(entry, "chanceBasisPoints")));
                }
            }
            Comparator<Object> comparator = Comparator
                    .comparingInt((Object entry) -> ordinal(entry)).reversed()
                    .thenComparing(Comparator.comparingInt((Object entry) -> chance(entry)).reversed())
                    .thenComparing(entry -> label(entry), String.CASE_INSENSITIVE_ORDER);
            retyped.sort(comparator);
            int maximum = Math.min(MAX_VANILLA_SLOTS, Math.min(limit > 0 ? limit : 30,
                    SETTINGS.maxDisplayEntries()));
            if (retyped.size() <= maximum) return retyped;
            List<Object> selected = new ArrayList<>(maximum);
            for (Object entry : retyped) {
                Item item = (Item) call(entry, "item");
                if (coreFor(item) != null || isProtectedForUi(item)) selected.add(entry);
            }
            if (selected.size() > maximum) {
                warn(cacheId, "More protected rewards than World Bosses preview slots");
                return retyped.subList(0, maximum);
            }
            for (Object entry : retyped) {
                if (selected.size() >= maximum) break;
                if (!selected.contains(entry)) selected.add(entry);
            }
            selected.sort(comparator);
            return List.copyOf(selected);
        } catch (ReflectiveOperationException | IllegalStateException ex) {
            BUILDING_FULL_PREVIEW.set(false);
            warn(cacheId, "Cache UI preview adapter failed: " + ex);
            return displayed;
        }
    }

    private static boolean isProtectedForUi(Item item) {
        String id = String.valueOf(BuiltInRegistries.ITEM.getKey(item));
        return id.startsWith("worldbosses:")
                || id.equals("minecraft:nether_star")
                || id.equals("minecraft:elytra")
                || id.equals("minecraft:totem_of_undying")
                || id.equals("minecraft:enchanted_golden_apple");
    }

    private static Core coreFor(Item item) {
        if (item == null) return null;
        String id = String.valueOf(BuiltInRegistries.ITEM.getKey(item));
        for (Core core : SETTINGS.cores()) if (core.item().equals(id)) return core;
        return null;
    }

    private static int ordinal(Object entry) {
        try { return ((Enum<?>) call(entry, "rarity")).ordinal(); }
        catch (ReflectiveOperationException ex) { return -1; }
    }
    private static int chance(Object entry) {
        try { return (int) call(entry, "chanceBasisPoints"); }
        catch (ReflectiveOperationException ex) { return 0; }
    }
    private static String label(Object entry) {
        try { return String.valueOf(call(entry, "label")); }
        catch (ReflectiveOperationException ex) { return ""; }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object rarity(String name) throws ReflectiveOperationException {
        Class klass = Class.forName(RARITY_CLASS);
        return Enum.valueOf(klass, name);
    }
    private static boolean compatibleVersion() {
        if (versionChecked) return supportedVersion;
        synchronized (WorldBossesRaidCacheCoreIntegration.class) {
            if (!versionChecked) {
                var optional = FabricLoader.getInstance().getModContainer(EXPECTED_MOD);
                supportedVersion = optional.isPresent()
                        && SETTINGS.version().equals(optional.get().getMetadata()
                                .getVersion().getFriendlyString());
                if (optional.isPresent() && !supportedVersion) {
                    AugmentationSurvival.LOGGER.warn(
                            "Skipping raid-cache bonus: World Bosses runtime differs from pinned {}",
                            SETTINGS.version());
                }
                versionChecked = true;
            }
        }
        return supportedVersion;
    }
    private static boolean isValid(Object entry) throws ReflectiveOperationException {
        return entry != null && getBool(entry, "enabled")
                && getInt(entry, "weight") > 0
                && !getString(entry, "item").isBlank();
    }
    private static Object call(Object o, String method) throws ReflectiveOperationException {
        return o.getClass().getMethod(method).invoke(o);
    }
    private static Object get(Object o, String field) throws ReflectiveOperationException {
        return o.getClass().getField(field).get(o);
    }
    private static void set(Object o, String field, Object value) throws ReflectiveOperationException {
        Field f = o.getClass().getField(field);
        f.set(o, value);
    }
    private static int getInt(Object o, String field) throws ReflectiveOperationException {
        return (int) get(o, field);
    }
    private static boolean getBool(Object o, String field) throws ReflectiveOperationException {
        return (boolean) get(o, field);
    }
    private static String getString(Object o, String field) throws ReflectiveOperationException {
        return String.valueOf(get(o, field));
    }
    private static void setInt(Object o, String field, int value) throws ReflectiveOperationException {
        o.getClass().getField(field).setInt(o, value);
    }
    private static synchronized void warn(String context, String message) {
        // Avoid repeating the same warning for each reel frame and each preview.
        if (REPORTED.add("warn:" + context + ":" + message)) {
            AugmentationSurvival.LOGGER.warn("World Bosses cache integration [{}]: {}", context, message);
        }
    }
    private static Settings loadSettings() {
        try (var stream = WorldBossesRaidCacheCoreIntegration.class
                .getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing " + RESOURCE);
            var root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schema_version").getAsInt() != 1) {
                throw new IllegalStateException("Unexpected raid cache schema");
            }
            List<Core> cores = new ArrayList<>();
            JsonArray definitions = root.getAsJsonArray("core_outcomes");
            for (JsonElement e : definitions) {
                JsonObject o = e.getAsJsonObject();
                cores.add(new Core(o.get("item").getAsString(), o.get("label").getAsString(),
                        o.get("chance_basis_points").getAsInt(), o.get("display_rarity").getAsString()));
            }
            Set<String> fillers = new HashSet<>();
            root.getAsJsonArray("ordinary_filler_ids").forEach(e -> fillers.add(e.getAsString()));
            Set<String> caches = new HashSet<>();
            root.getAsJsonArray("supported_caches").forEach(e -> caches.add(e.getAsString()));
            if (cores.size() != 3 || caches.size() != 11) {
                throw new IllegalStateException("Invalid raid cache cores or cache IDs");
            }
            int count = 0;
            for (Core core : cores) {
                if (core.basisPoints() <= 0 || !core.item().startsWith("augmentation_survival:")) {
                    throw new IllegalStateException("Invalid core reward");
                }
                count += core.basisPoints();
            }
            if (count != 300) throw new IllegalStateException("Expected 3% shared core pool");
            Set<String> ids = new HashSet<>();
            cores.forEach(core -> ids.add(core.item()));
            return new Settings(root.get("required_worldbosses_version").getAsString(),
                    root.get("weight_scale").getAsInt(),
                    root.get("max_display_entries").getAsInt(),
                    List.copyOf(cores), Set.copyOf(fillers), Set.copyOf(caches), Set.copyOf(ids));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid World Bosses cache core tuning", ex);
        }
    }
    private record Core(String item, String label, int basisPoints, String displayRarity) {}
    private record Settings(String version, int scale, int maxDisplayEntries,
            List<Core> cores, Set<String> fillerIds, Set<String> cacheIds, Set<String> coreItemIds) {}
}
