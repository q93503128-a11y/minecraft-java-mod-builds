package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/** Removes only pathological natural-vegetation debris left by a completed capital build. */
public final class ConstructionDebrisCleaner {
    private static final int CLEANUP_RADIUS = 272;
    private static final int PATHOLOGICAL_DEBRIS_THRESHOLD = 24;
    private static final Set<Item> NATURAL_DEBRIS = Set.of(
            Items.DEAD_BUSH,
            Items.WHEAT_SEEDS,
            Items.STICK,
            Items.OAK_SAPLING,
            Items.SPRUCE_SAPLING,
            Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING,
            Items.ACACIA_SAPLING,
            Items.DARK_OAK_SAPLING,
            Items.MANGROVE_PROPAGULE,
            Items.AZALEA,
            Items.FLOWERING_AZALEA
    );

    private ConstructionDebrisCleaner() {
    }

    public static int cleanIfPathological(ServerLevel level, String homelandId,
                                          RealmSiteLayoutSavedData.RealmSite site) {
        AABB bounds = new AABB(
                site.centerX() - CLEANUP_RADIUS, level.getMinY(), site.centerZ() - CLEANUP_RADIUS,
                site.centerX() + CLEANUP_RADIUS + 1, level.getMaxY(), site.centerZ() + CLEANUP_RADIUS + 1
        );
        List<ItemEntity> debris = level.getEntitiesOfClass(ItemEntity.class, bounds,
                entity -> NATURAL_DEBRIS.contains(entity.getItem().getItem()));
        if (debris.size() < PATHOLOGICAL_DEBRIS_THRESHOLD) return 0;
        debris.forEach(ItemEntity::discard);
        LivingKingdoms.LOGGER.warn(
                "Removed {} construction vegetation debris entities around {} after terrain integration",
                debris.size(), homelandId
        );
        return debris.size();
    }
}
