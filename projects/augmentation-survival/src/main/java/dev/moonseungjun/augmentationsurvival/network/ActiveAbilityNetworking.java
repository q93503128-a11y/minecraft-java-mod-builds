package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Common networking for active use requests and the server-owned ten-slot loadout. */
public final class ActiveAbilityNetworking {
    private ActiveAbilityNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ActiveAbilityRequestPayload.TYPE, ActiveAbilityRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActiveLoadoutEditPayload.TYPE, ActiveLoadoutEditPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActiveLoadoutStatePayload.TYPE, ActiveLoadoutStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                ActiveAbilityRequestPayload.TYPE,
                (payload, context) -> {
                    ActiveAbilityRouter.tryUseSlot(context.player(), payload.slotIndex());
                    syncNow(context.player());
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(
                ActiveLoadoutEditPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    switch (payload.action()) {
                        case ActiveLoadoutEditPayload.ASSIGN -> ActiveAbilityRouter.assignSlot(player, payload.firstSlot(), payload.augmentId());
                        case ActiveLoadoutEditPayload.SWAP -> ActiveAbilityRouter.swapSlots(player, payload.firstSlot(), payload.secondSlot());
                        case ActiveLoadoutEditPayload.CLEAR -> ActiveAbilityRouter.clearSlot(player, payload.firstSlot());
                        default -> { }
                    }
                    syncNow(player);
                }
        );
        ServerPlayerEvents.JOIN.register(ActiveAbilityNetworking::syncNow);
    }

    public static void syncNow(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ActiveLoadoutStatePayload.TYPE)) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> slots = ActiveAbilityRouter.resolvedSlots(state);
        List<Integer> cooldowns = new ArrayList<>(10);
        long now = player.level().getGameTime();
        for (String id : slots) {
            if (id.isBlank()) {
                cooldowns.add(0);
            } else {
                long remaining = Math.max(0L, state.cooldownReadyTick(id) - now);
                cooldowns.add((int) Math.min(Integer.MAX_VALUE, remaining));
            }
        }
        ServerPlayNetworking.send(player, new ActiveLoadoutStatePayload(
                slots, cooldowns, ActiveAbilityRouter.ownedActiveIds(state)
        ));
    }
}
