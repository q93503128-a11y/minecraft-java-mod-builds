package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Separate management input from active use: Shift+slot never tries to cast Domination. */
public final class DominationManagementNetworking {
    private static final String ID = "augmentation_survival:domination";
    private DominationManagementNetworking() { }
    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                DominationManagementRequestPayload.TYPE, DominationManagementRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                DominationManagementSnapshotPayload.TYPE, DominationManagementSnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DominationManagementRequestPayload.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            if (AugmentationPlayerStateService.state(player).level(ID) <= 0 || player.isDeadOrDying()) return;
            if (!request.open()) {
                try {
                    CompanionAugmentService.releaseOwnedCompanion(player, UUID.fromString(request.entityUuid()));
                } catch (IllegalArgumentException ignored) {
                    // Invalid/unowned UUIDs are rejected, never acted upon.
                }
            }
            send(player, request.open());
        });
    }
    private static void send(ServerPlayer player, boolean open) {
        if (!ServerPlayNetworking.canSend(player, DominationManagementSnapshotPayload.TYPE)) return;
        var roster = CompanionAugmentService.ownerRoster(player);
        var results = new ArrayList<DominationManagementSnapshotPayload.CompanionView>();
        for (var record : roster.companions(player.getUUID())) {
            boolean loaded = false;
            String currentDimension = record.dimension();
            try {
                UUID id = UUID.fromString(record.uuid());
                for (var level : player.level().getServer().getAllLevels()) {
                    var entity = level.getEntity(id);
                    if (entity != null) {
                        loaded = true;
                        currentDimension = entity.level().dimension().identifier().toString();
                        break;
                    }
                }
            } catch (IllegalArgumentException ignored) { }
            results.add(new DominationManagementSnapshotPayload.CompanionView(
                    record.uuid(), record.name(), record.entityId(), currentDimension, loaded));
        }
        int level = Math.min(3, AugmentationPlayerStateService.state(player).level(ID));
        int capacity = AdvancedWorldTuning.current().dominationCap(level);
        ServerPlayNetworking.send(player, new DominationManagementSnapshotPayload(
                roster.occupied(player.getUUID()), capacity, results, open));
    }
}
