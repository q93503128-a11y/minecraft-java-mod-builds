package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;

public final class WorldBossesRaidCacheGameTests implements CustomTestMethodInvoker {
    private static final List<String> CACHES = List.of(
            "verdant_raid_cache", "storm_raid_cache", "dune_raid_cache",
            "infernal_raid_cache", "soul_raid_cache", "void_raid_cache",
            "umbral_raid_cache", "dragon_raid_cache", "griffin_raid_cache",
            "weaver_raid_cache", "primordial_raid_cache");
    private static final Map<String, Integer> CORE_BPS = Map.of(
            "augmentation_survival:epic_augment_core", 200,
            "augmentation_survival:legendary_augment_core", 80,
            "augmentation_survival:mythic_augment_core", 20);
    private static final int SCALE = 10000;

    @GameTest(maxTicks = 80)
    public void allRaidCachesDisplayTheSameEffectiveCoreProbabilitiesAsTheirServerRoll(GameTestHelper helper)
            throws ReflectiveOperationException {
        require(FabricLoader.getInstance().isModLoaded("worldbosses"), "Pinned World Bosses runtime not loaded");
        Class<?> config = Class.forName("com.mki.worldbosses.config.RaidCacheLootConfig");
        Method get = config.getMethod("get", String.class);
        Method preview = config.getMethod("buildLootPool", String.class, int.class);
        Method roll = config.getMethod("rollReward", String.class, Random.class);
        for (String id : CACHES) {
            Object definition = get.invoke(null, id);
            @SuppressWarnings("unchecked")
            Map<Object, Object> categories = (Map<Object, Object>) definition.getClass()
                    .getMethod("categories").invoke(definition);
            int categoryTotal = 0;
            Object common = null;
            for (var cat : categories.entrySet()) {
                if (cat.getValue() == null || !boolField(cat.getValue(), "enabled")) continue;
                int weight = intField(cat.getValue(), "categoryWeight");
                if (weight <= 0) continue;
                categoryTotal += weight;
                if (((Enum<?>) cat.getKey()).name().equals("COMMON")) common = cat.getValue();
            }
            require(common != null, "Missing common category for " + id);
            int commonWeight = intField(common, "categoryWeight");
            @SuppressWarnings("unchecked")
            List<Object> entries = (List<Object>) common.getClass().getField("rewards").get(common);
            long totalWeight = 0;
            Map<String, Long> selected = new HashMap<>();
            for (Object entry : entries) {
                if (!boolField(entry, "enabled") || intField(entry, "weight") <= 0) continue;
                String item = String.valueOf(entry.getClass().getField("item").get(entry));
                int weight = intField(entry, "weight");
                totalWeight += weight;
                if (CORE_BPS.containsKey(item)) selected.merge(item, (long) weight, Long::sum);
                if (item.startsWith("worldbosses:")) {
                    require(weight % SCALE == 0,
                            "Protected external reward weight modified: " + id + " / " + item);
                }
            }
            require(selected.size() == 3, "Not exactly three core candidates in " + id);
            for (var core : CORE_BPS.entrySet()) {
                double effectiveBasisPoints = 10000.0 * commonWeight / categoryTotal
                        * selected.get(core.getKey()) / totalWeight;
                require(Math.abs(effectiveBasisPoints - core.getValue()) < 0.02,
                        "Core probability mismatch in " + id + " for " + core.getKey()
                                + ": expected=" + core.getValue() + "bp actual=" + effectiveBasisPoints);
            }
            // Server config is cached: querying twice must never multiply the core entries.
            get.invoke(null, id);
            require(entries.size() >= 3 && selected.size() == 3, "Idempotence broken: " + id);
            @SuppressWarnings("unchecked")
            List<Object> display = (List<Object>) preview.invoke(null, id, 30);
            require(display.size() <= 30, "Overfilled World Bosses menu: " + id);
            for (var core : CORE_BPS.entrySet()) {
                boolean visible = false;
                for (Object view : display) {
                    Item displayedItem = (Item) view.getClass().getMethod("item").invoke(view);
                    String key = String.valueOf(BuiltInRegistries.ITEM.getKey(displayedItem));
                    if (key.equals(core.getKey())) {
                        int bp = (int) view.getClass().getMethod("chanceBasisPoints").invoke(view);
                        require(Math.abs(bp - core.getValue()) <= 1,
                                "UI chance differs from actual roll for " + id + " / " + key);
                        visible = true;
                        break;
                    }
                }
                require(visible, "Actual core cannot be seen in cache UI: " + id + " / " + core.getKey());
            }
            // Exercise the real roll path and ensure it produces a valid registered item.
            Object reward = roll.invoke(null, id, new Random(0xA600L));
            Item rewarded = (Item) reward.getClass().getMethod("item").invoke(reward);
            require(rewarded != null && rewarded != net.minecraft.world.item.Items.AIR,
                    "Server cache returned invalid reward: " + id);
        }
        GameTestPlayers.succeed(helper);
    }
    private static int intField(Object o, String field) throws ReflectiveOperationException {
        return o.getClass().getField(field).getInt(o);
    }
    private static boolean boolField(Object o, String field) throws ReflectiveOperationException {
        return o.getClass().getField(field).getBoolean(o);
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
