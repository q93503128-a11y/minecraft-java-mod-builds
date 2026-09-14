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
    private static final int REQUIRED_SALVAGE = 3;
    private static final int READY_MARKER_OFFSET_X = 2;

    private ExpeditionFieldExtractionRelay() {}

    /**
     * Materializes the technical relay after the player begins interacting with the deployed field cell.
     * This deliberately does not run a world tick scan and does not alter authoritative expedition state.
     * The approach uses only built-in Minecraft blocks: a dark deepslate frame and the vanilla geode
     * smooth-basalt/calcite layering make the relay readable without introducing a final Riftfrontier art asset.
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
        if (overworld.getBlockState(relay).is(Blocks.LODESTONE)) {
            refreshPresentation(player);
            return;
        }
        if (!overworld.getBlockState(relay).isAir()) return;

        decorateApproach(overworld, relay);
        overworld.setBlockAndUpdate(relay, Blocks.LODESTONE.defaultBlockState());
        refreshPresentation(player);
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Extraction relay online at the far edge of the field cell. "
                + "Complete the salvage objective, then right-click the lodestone to extract."
        ));
        ExpeditionPlayerFeedback.extractionRelayOnline(player);
    }

    /**
     * Projects the existing authoritative salvage count into world-space readability. No readiness flag is stored:
     * two vanilla end-rod markers are present only while the current run's recovered salvage satisfies the gate.
     */
    public static void refreshPresentation(ServerPlayer player) {
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
        if (!overworld.getBlockState(relay).is(Blocks.LODESTONE)) return;

        decorateApproach(overworld, relay);
        int recovered = active.get().recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        boolean ready = FieldRelayPresentationPolicy.extractionReady(recovered, REQUIRED_SALVAGE);
        setReadyMarker(overworld, relay.offset(-READY_MARKER_OFFSET_X, 0, 0), ready);
        setReadyMarker(overworld, relay.offset(READY_MARKER_OFFSET_X, 0, 0), ready);
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
            clearReadyMarkers(level, clickedPos);
            ExpeditionHubTerminal.ensurePresent(player);
            ExpeditionPlayerFeedback.extractionComplete(player);
            player.sendSystemMessage(Component.literal(
                "[Riftfrontier] Hub stations online: smithing table = provision, lodestone = deploy."
            ));
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

    private static void decorateApproach(ServerLevel level, BlockPos relay) {
        for (int dx = -2; dx <= 2; dx++) {
            setTechnicalFloor(level, relay.offset(dx, -1, -2), Blocks.SMOOTH_BASALT.defaultBlockState());
            setTechnicalFloor(level, relay.offset(dx, -1, -1), Blocks.CALCITE.defaultBlockState());
            setTechnicalFloor(level, relay.offset(dx, -1, 0), Blocks.DEEPSLATE_TILES.defaultBlockState());
        }
        setTechnicalFloor(level, relay.below(), Blocks.CHISELED_DEEPSLATE.defaultBlockState());
    }

    private static void setTechnicalFloor(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState replacement) {
        var state = level.getBlockState(pos);
        if (state.is(Blocks.SMOOTH_STONE)
            || state.is(Blocks.SMOOTH_BASALT)
            || state.is(Blocks.CALCITE)
            || state.is(Blocks.DEEPSLATE_TILES)
            || state.is(Blocks.CHISELED_DEEPSLATE)) {
            level.setBlockAndUpdate(pos, replacement);
        }
    }

    private static void setReadyMarker(ServerLevel level, BlockPos pos, boolean ready) {
        var state = level.getBlockState(pos);
        if (ready) {
            if (state.isAir()) level.setBlockAndUpdate(pos, Blocks.END_ROD.defaultBlockState());
        } else if (state.is(Blocks.END_ROD)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void clearReadyMarkers(ServerLevel level, BlockPos relay) {
        setReadyMarker(level, relay.offset(-READY_MARKER_OFFSET_X, 0, 0), false);
        setReadyMarker(level, relay.offset(READY_MARKER_OFFSET_X, 0, 0), false);
    }
}
