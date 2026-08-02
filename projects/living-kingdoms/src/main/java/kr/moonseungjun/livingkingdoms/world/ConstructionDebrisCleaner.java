package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Set;

/**
 * One-shot cleanup for authored construction.
 *
 * <p>Normal gameplay loot is never deleted continuously. Construction writes already use
 * drop-suppressing update flags; this class only clears item entities inside a cell at the exact
 * moment that authored construction for that cell finishes, plus delayed natural vegetation drops
 * during the bounded capital stabilization checks.</p>
 */
public final class ConstructionDebrisCleaner {
    private static final int ERDEN_CONSTRUCTION_RADIUS = 1_300;
    private static final int STREAMED_CHUNK_MARGIN = 2;
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

    /** Called once before waiting players enter the newly built Erden district. */
    public static int cleanConstructionCompletion(ServerLevel level, String homelandId,
                                                  RealmSiteLayoutSavedData.RealmSite site) {
        requireErden(homelandId);
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class, bounds(level, site));
        debris.forEach(ItemEntity::discard);
        if (!debris.isEmpty()) {
            LivingKingdoms.LOGGER.info(
                    "Removed {} total construction item entities around Erden before player placement",
                    debris.size()
            );
        }
        return debris.size();
    }

    /**
     * Clears only the cell whose authored road, building and infrastructure writes just completed.
     * This method is never called as a continuous gameplay cleaner.
     */
    public static int cleanStreamedChunkCompletion(ServerLevel level, ChunkPos chunk) {
        int minX = chunk.getMinBlockX() - STREAMED_CHUNK_MARGIN;
        int minZ = chunk.getMinBlockZ() - STREAMED_CHUNK_MARGIN;
        int maxX = chunk.getMinBlockX() + 16 + STREAMED_CHUNK_MARGIN;
        int maxZ = chunk.getMinBlockZ() + 16 + STREAMED_CHUNK_MARGIN;
        AABB chunkBounds = new AABB(
                minX, level.getMinY(), minZ,
                maxX, level.getMaxY(), maxZ
        );
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class, chunkBounds);
        debris.forEach(ItemEntity::discard);
        if (!debris.isEmpty()) {
            LivingKingdoms.LOGGER.debug(
                    "Removed {} construction item entities after streamed Erden cell {},{}",
                    debris.size(), chunk.x(), chunk.z()
            );
        }
        return debris.size();
    }

    /** Used during the bounded stabilization pass and by diagnostics. */
    public static void schedule(ServerLevel level, String homelandId,
                                RealmSiteLayoutSavedData.RealmSite site) {
        requireErden(homelandId);
        int removed = cleanNaturalDebris(level, site);
        if (removed > 0) {
            LivingKingdoms.LOGGER.info("Removed {} delayed construction vegetation drops around Erden", removed);
        }
    }

    public static int cleanIfPathological(ServerLevel level, String homelandId,
                                          RealmSiteLayoutSavedData.RealmSite site) {
        requireErden(homelandId);
        return cleanNaturalDebris(level, site);
    }

    /** Compatibility hook. Continuous cleanup was removed because it could delete legitimate loot. */
    public static void onServerTick(ServerTickEvent.Post event) {
    }

    /** Compatibility hook. Item spawning during ordinary play is never cancelled. */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
    }

    private static int cleanNaturalDebris(ServerLevel level,
                                          RealmSiteLayoutSavedData.RealmSite site) {
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class,
                bounds(level, site),
                entity -> NATURAL_DEBRIS.contains(entity.getItem().getItem()));
        debris.forEach(ItemEntity::discard);
        return debris.size();
    }

    private static AABB bounds(ServerLevel level, RealmSiteLayoutSavedData.RealmSite site) {
        return new AABB(
                site.centerX() - ERDEN_CONSTRUCTION_RADIUS, level.getMinY(),
                site.centerZ() - ERDEN_CONSTRUCTION_RADIUS,
                site.centerX() + ERDEN_CONSTRUCTION_RADIUS + 1, level.getMaxY(),
                site.centerZ() + ERDEN_CONSTRUCTION_RADIUS + 1
        );
    }

    private static void requireErden(String homelandId) {
        if (!"erden_kingdom".equals(homelandId)) {
            throw new IllegalArgumentException("Inactive homeland cleanup request: " + homelandId);
        }
    }
}
