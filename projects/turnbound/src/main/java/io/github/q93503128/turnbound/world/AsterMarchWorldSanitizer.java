package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

/** Removes vanilla block-decay drops from the authored RPG world; TURNBOUND rewards never use loose item entities. */
public final class AsterMarchWorldSanitizer {
    private AsterMarchWorldSanitizer() {}

    public static void tick(ServerPlayer player) {
        if (player == null || player.tickCount % 10 != 0 || !WorldSessionRouter.active(player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        var area = player.getBoundingBox().inflate(112.0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();
    }
}
