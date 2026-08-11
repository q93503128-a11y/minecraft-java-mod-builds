package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Scripted raid destruction uses real block updates, so unsupported decorations can pop as item entities.
 * Village Guardians has no block-looting loop: block-item debris created inside the fortress during a raid is discarded.
 * Equipment, monster loot, food, arrows and every non-block item are untouched.
 */
@EventBusSubscriber(modid = VillageGuardians.MOD_ID)
public final class VillageRaidDebrisDropGuard {
    private VillageRaidDebrisDropGuard() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getLevel() instanceof ServerLevel)
                || !(event.getEntity() instanceof ItemEntity itemEntity)
                || !(itemEntity.getItem().getItem() instanceof BlockItem)
                || !VillageRaidSystem.isActive()
                || !VillageWorldSystem.isInsideVillageArea(itemEntity.blockPosition())) {
            return;
        }
        event.setCanceled(true);
        itemEntity.discard();
    }
}
