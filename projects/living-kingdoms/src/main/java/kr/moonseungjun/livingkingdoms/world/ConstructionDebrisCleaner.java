package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Suppresses vegetation drops for the full period in which construction neighbour updates settle. */
public final class ConstructionDebrisCleaner {
    private static final int CLEANUP_RADIUS = 288;
    private static final long CLEANUP_WINDOW_TICKS = 600L;
    private static final Set<Item> NATURAL_DEBRIS = Set.of(
            Items.DEAD_BUSH,
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD,
            Items.STICK,
            Items.APPLE,
            Items.OAK_SAPLING,
            Items.SPRUCE_SAPLING,
            Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING,
            Items.ACACIA_SAPLING,
            Items.DARK_OAK_SAPLING,
            Items.CHERRY_SAPLING,
            Items.PALE_OAK_SAPLING,
            Items.MANGROVE_PROPAGULE,
            Items.AZALEA,
            Items.FLOWERING_AZALEA,
            Items.BAMBOO,
            Items.SUGAR_CANE,
            Items.KELP,
            Items.SEAGRASS,
            Items.VINE,
            Items.GLOW_BERRIES,
            Items.SWEET_BERRIES
    );
    private static final Map<MinecraftServer, Map<String, CleanupTask>> TASKS = new WeakHashMap<>();

    private ConstructionDebrisCleaner() {
    }

    public static void schedule(ServerLevel level, String homelandId,
                                RealmSiteLayoutSavedData.RealmSite site) {
        CleanupTask task = new CleanupTask(level, homelandId, site.centerX(), site.centerZ(),
                level.getGameTime() + CLEANUP_WINDOW_TICKS);
        synchronized (TASKS) {
            TASKS.computeIfAbsent(level.getServer(), ignored -> new HashMap<>()).put(homelandId, task);
        }
        int removed = cleanNow(task);
        if (removed > 0) {
            LivingKingdoms.LOGGER.warn(
                    "Removed {} existing construction vegetation drops around {} and opened delayed cleanup window",
                    removed, homelandId
            );
        }
    }

    public static int cleanIfPathological(ServerLevel level, String homelandId,
                                          RealmSiteLayoutSavedData.RealmSite site) {
        schedule(level, homelandId, site);
        return 0;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 5 != 0) return;
        synchronized (TASKS) {
            Map<String, CleanupTask> tasks = TASKS.get(event.getServer());
            if (tasks == null || tasks.isEmpty()) return;
            Iterator<CleanupTask> iterator = tasks.values().iterator();
            while (iterator.hasNext()) {
                CleanupTask task = iterator.next();
                if (task.level().getGameTime() > task.expiresAt()) {
                    iterator.remove();
                    continue;
                }
                cleanNow(task);
            }
            if (tasks.isEmpty()) TASKS.remove(event.getServer());
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)
                || !(event.getLevel() instanceof ServerLevel level)
                || !NATURAL_DEBRIS.contains(item.getItem().getItem())) return;
        synchronized (TASKS) {
            Map<String, CleanupTask> tasks = TASKS.get(level.getServer());
            if (tasks == null) return;
            for (CleanupTask task : tasks.values()) {
                if (task.level() == level && inside(task, item.getX(), item.getZ())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    private static int cleanNow(CleanupTask task) {
        AABB bounds = new AABB(
                task.centerX() - CLEANUP_RADIUS, task.level().getMinY(), task.centerZ() - CLEANUP_RADIUS,
                task.centerX() + CLEANUP_RADIUS + 1, task.level().getMaxY(), task.centerZ() + CLEANUP_RADIUS + 1
        );
        List<ItemEntity> debris = task.level().getEntitiesOfClass(ItemEntity.class, bounds,
                entity -> NATURAL_DEBRIS.contains(entity.getItem().getItem()));
        debris.forEach(ItemEntity::discard);
        return debris.size();
    }

    private static boolean inside(CleanupTask task, double x, double z) {
        return Math.abs(x - task.centerX()) <= CLEANUP_RADIUS
                && Math.abs(z - task.centerZ()) <= CLEANUP_RADIUS;
    }

    private record CleanupTask(ServerLevel level, String homelandId,
                               int centerX, int centerZ, long expiresAt) {
    }
}
