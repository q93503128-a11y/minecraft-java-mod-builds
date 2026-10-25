package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.spatial.SpatialLinkService;
import dev.moonseungjun.augmentationsurvival.augment.spatial.SpatialLinkWorldState;
import java.util.ArrayList;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned networking boundary for the spatial-link screen. */
public final class SpatialLinkNetworking {
    private SpatialLinkNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(SpatialLinkActionPayload.TYPE, SpatialLinkActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SpatialLinkSnapshotPayload.TYPE, SpatialLinkSnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SpatialLinkActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!SpatialLinkService.hasAccess(player)) {
                player.sendSystemMessage(Component.translatable("message.augmentation_survival.spatial_link.no_access"));
                return;
            }
            handle(player, payload);
            sendSnapshot(player);
        });
    }

    private static void handle(ServerPlayer player, SpatialLinkActionPayload payload) {
        switch (payload.action()) {
            case OPEN -> { }
            case REGISTER -> SpatialLinkService.registerCurrentLocation(player, payload.value());
            case TELEPORT -> SpatialLinkService.teleport(player, payload.gateId());
            case FAVORITE -> SpatialLinkService.toggleFavorite(player, payload.gateId());
            case DEFAULT -> SpatialLinkService.setDefault(player, payload.gateId());
            case RENAME -> SpatialLinkService.rename(player, payload.gateId(), payload.value());
            case DELETE -> SpatialLinkService.delete(player, payload.gateId());
            case SHARE, UNSHARE -> {
                ServerPlayer target = player.level().getServer().getPlayerList().getPlayerByName(payload.value());
                if (target != null) {
                    SpatialLinkService.share(
                            player,
                            payload.gateId(),
                            target,
                            payload.action() == SpatialLinkActionPayload.Action.SHARE
                    );
                    sendSnapshot(target);
                }
            }
            case TELEPORT_DEFAULT -> SpatialLinkService.teleportDefault(player);
        }
    }

    public static void sendSnapshot(ServerPlayer player) {
        if (!SpatialLinkService.hasAccess(player)) return;
        SpatialLinkWorldState state = SpatialLinkService.snapshot(player);
        ArrayList<SpatialLinkSnapshotPayload.GateView> views = new ArrayList<>();
        for (SpatialLinkWorldState.GateRecord gate : state.visibleGates(player.getUUID())) {
            views.add(new SpatialLinkSnapshotPayload.GateView(
                    gate.id(),
                    gate.name(),
                    gate.dimension(),
                    gate.x(), gate.y(), gate.z(),
                    gate.owner().equals(player.getUUID().toString()),
                    state.isFavorite(player.getUUID(), gate.id()),
                    state.isDefault(player.getUUID(), gate.id())
            ));
        }
        ServerPlayNetworking.send(player, new SpatialLinkSnapshotPayload(views));
    }
}
