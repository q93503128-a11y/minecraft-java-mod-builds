package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes item entities created by authored terrain replacement without deleting live player loot.
 */
public final class ConstructionDebrisCleaner {
    private static final Map<String, Integer> CLEANUP_RADII = Map.of(
            "erden_kingdom", 330,
            "silvana_forest", 260,
            "kardum_league", 270
    );
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

    private ConstructionDebrisCleaner() {
    }

    /**
     * Called exactly when an authored capital finishes, before waiting players enter it. Every item
     * entity inside the construction district is therefore a build by-product and can be discarded.
     */
    public static int cleanConstructionCompletion(ServerLevel level, String homelandId,
                                                  RealmSiteLayoutSavedData.RealmSite site) {
        AABB bounds = bounds(level, homelandId, site);
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class, bounds);
        debris.forEach(ItemEntity::discard);
        if (!debris.isEmpty()) {
            LivingKingdoms.LOGGER.info(
                    "Removed {} total construction item entities around {} before player placement",
                    debris.size(), homelandId
            );
        }
        return debris.size();
    }

    /** Safe during play: removes only the known natural drops caused by delayed block updates. */
    public static void schedule(ServerLevel level, String homelandId,
                                RealmSiteLayoutSavedData.RealmSite site) {
        int removed = cleanNaturalDebris(level, homelandId, site);
        if (removed > 0) {
            LivingKingdoms.LOGGER.info(
                    "Removed {} delayed construction vegetation drops around {}",
                    removed, homelandId
            );
        }
    }

    public static int cleanIfPathological(ServerLevel level, String homelandId,
                                          RealmSiteLayoutSavedData.RealmSite site) {
        return cleanNaturalDebris(level, homelandId, site);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        ServerLevel level = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (level == null) return;
        for (String homelandId : CLEANUP_RADII.keySet()) {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
            if (site != null && site.built()) cleanNaturalDebris(level, homelandId, site);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || !NATURAL_DEBRIS.contains(item.getItem().getItem())) return;
        if (insideAnyBuiltSettlement(level, item.getX(), item.getZ())) event.setCanceled(true);
    }

    private static boolean insideAnyBuiltSettlement(ServerLevel level, double x, double z) {
        for (Map.Entry<String, Integer> entry : CLEANUP_RADII.entrySet()) {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, entry.getKey());
            if (site == null || !site.built()) continue;
            int radius = entry.getValue();
            double dx = x - site.centerX();
            double dz = z - site.centerZ();
            if (dx * dx + dz * dz <= (double) radius * radius) return true;
        }
        return false;
    }

    private static int cleanNaturalDebris(ServerLevel level, String homelandId,
                                          RealmSiteLayoutSavedData.RealmSite site) {
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class,
                bounds(level, homelandId, site),
                entity -> NATURAL_DEBRIS.contains(entity.getItem().getItem()));
        debris.forEach(ItemEntity::discard);
        return debris.size();
    }

    private static AABB bounds(ServerLevel level, String homelandId,
                               RealmSiteLayoutSavedData.RealmSite site) {
        int radius = CLEANUP_RADII.getOrDefault(homelandId, 280);
        return new AABB(
                site.centerX() - radius, level.getMinY(), site.centerZ() - radius,
                site.centerX() + radius + 1, level.getMaxY(), site.centerZ() + radius + 1
        );
    }
}
