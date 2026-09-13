package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Temporary world interaction for closing the first expedition loop without a command-only extraction.
 * The lodestone is a technical validation affordance, not final Region 01 art, UI, or portal language.
 * All objective, ownership, retention, pressure and reward authority remains in ExpeditionGameplayService.
 */
public final class ExpeditionFieldExtractionRelay {
    private static final int RELAY_OFFSET_Z = 5;

    private ExpeditionFieldExtractionRelay() {}

    /**
     * Materializes the technical relay after the player begins interacting with the deployed field cell.
     * This deliberately does not run a world tick scan and does not alter authoritative expedition state.
     */
    public static void ensurePresent(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ServerLevel overworld = level.getServer().overworld();
        if (level != overworld) return;

        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        var active = ExpeditionGameplayService.activeFor(player, world);
        if (active.isEmpty()
            || active.get().status() != ExpeditionRun.Status.DEPLOYED
            || !active.get().regionId().equals(ExpeditionGameplayService.REGION_ID)) {
            return;
        }

        BlockPos relay = relayPosition();
        if (overworld.getBlockState(relay).is(Blocks.LODESTONE)) return;
        if (!overworld.getBlockState(relay).isAir()) return;

        overworld.setBlockAndUpdate(relay, Blocks.LODESTONE.defaultBlockState());
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Extraction relay online at the far edge of the field cell. "
                + "Complete the salvage objective, then right-click the lodestone to extract."
        ));
    }

    /**
     * Consumes interaction only when the authoritative player's active Region 01 run owns this relay.
     * A rejected early extraction remains an authoritative no-op and surfaces the existing gate reason.
     */
    public static boolean tryUse(ServerPlayer player, BlockPos clickedPos) {
        if (!clickedPos.equals(relayPosition())) return false;

        ServerLevel level = (ServerLevel) player.level();
        if (level != level.getServer().overworld()) return false;
        if (!level.getBlockState(clickedPos).is(Blocks.LODESTONE)) return false;

        RiftfrontierWorldData world = RiftfrontierWorldData.get(level);
        var active = ExpeditionGameplayService.activeFor(player, world);
        if (active.isEmpty()
            || active.get().status() != ExpeditionRun.Status.DEPLOYED
            || !active.get().regionId().equals(ExpeditionGameplayService.REGION_ID)) {
            return false;
        }

        try {
            ExpeditionGameplayService.extract(player);
        } catch (IllegalStateException rejected) {
            player.sendSystemMessage(Component.literal(
                "[Riftfrontier] Extraction relay locked: " + rejected.getMessage()
            ));
        }
        return true;
    }

    static BlockPos relayPosition() {
        return ExpeditionGameplayService.technicalRegionCenter().offset(0, 0, RELAY_OFFSET_Z);
    }
}
