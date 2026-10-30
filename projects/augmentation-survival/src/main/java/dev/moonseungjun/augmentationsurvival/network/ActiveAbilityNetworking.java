package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveChargePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Common networking for active use requests, presentation and the server-owned ten-slot loadout. */
public final class ActiveAbilityNetworking {
    private static final String INSTANT_MINING_ID = UtilityAugmentService.INSTANT_MINING_ID;
    private static final String THREAT_SUPPRESSION_ID = AugmentationSurvival.MOD_ID + ":threat_suppression";

    private ActiveAbilityNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ActiveAbilityRequestPayload.TYPE, ActiveAbilityRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActiveLoadoutEditPayload.TYPE, ActiveLoadoutEditPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActiveLoadoutStatePayload.TYPE, ActiveLoadoutStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ActivePresentationPayload.TYPE, ActivePresentationPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                ActiveAbilityRequestPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
                    List<String> slots = ActiveAbilityRouter.resolvedSlots(state);
                    int slot = payload.slotIndex();
                    if (slot >= 0 && slot < slots.size() && !INSTANT_MINING_ID.equals(slots.get(slot))) {
                        ActiveAbilityRouter.tryUseSlot(player, slot);
                    }
                    syncNow(player);
                }
        );
        ServerPlayNetworking.registerGlobalReceiver(
                ActiveLoadoutEditPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    switch (payload.action()) {
                        case ActiveLoadoutEditPayload.ASSIGN -> {
                            if (!INSTANT_MINING_ID.equals(payload.augmentId())) {
                                ActiveAbilityRouter.assignSlot(player, payload.firstSlot(), payload.augmentId());
                            }
                        }
                        case ActiveLoadoutEditPayload.SWAP -> ActiveAbilityRouter.swapSlots(player, payload.firstSlot(), payload.secondSlot());
                        case ActiveLoadoutEditPayload.CLEAR -> ActiveAbilityRouter.clearSlot(player, payload.firstSlot());
                        default -> { }
                    }
                    syncNow(player);
                }
        );
        ServerPlayerEvents.JOIN.register(ActiveAbilityNetworking::syncNow);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> syncNow(newPlayer));
        AugmentProgressNetworking.initialize();
        ProtectionHudNetworking.initialize();
    }

    public static void syncNow(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ActiveLoadoutStatePayload.TYPE)) return;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        List<String> slots = new ArrayList<>(ActiveAbilityRouter.resolvedSlots(state));
        for (int i = 0; i < slots.size(); i++) {
            if (INSTANT_MINING_ID.equals(slots.get(i))) slots.set(i, "");
        }

        List<Integer> cooldowns = new ArrayList<>(10);
        List<Integer> maxCharges = new ArrayList<>(10);
        List<Integer> charge0Ticks = new ArrayList<>(10);
        List<Integer> charge1Ticks = new ArrayList<>(10);
        long now = player.level().getGameTime();
        for (String id : slots) {
            if (id.isBlank()) {
                cooldowns.add(0);
                maxCharges.add(0);
                charge0Ticks.add(0);
                charge1Ticks.add(0);
                continue;
            }
            long remaining = Math.max(0L, state.cooldownReadyTick(id) - now);
            cooldowns.add((int) Math.min(Integer.MAX_VALUE, remaining));
            ActiveChargePresentationService.Snapshot charge =
                    ActiveChargePresentationService.snapshot(state, id, now);
            maxCharges.add(charge.maxCharges());
            charge0Ticks.add(charge.charge0Ticks());
            charge1Ticks.add(charge.charge1Ticks());
        }

        List<String> owned = ActiveAbilityRouter.ownedActiveIds(state).stream()
                .filter(id -> !INSTANT_MINING_ID.equals(id))
                .toList();
        ServerPlayNetworking.send(player, new ActiveLoadoutStatePayload(
                slots, cooldowns, maxCharges, charge0Ticks, charge1Ticks, owned
        ));

        // Threat Suppression is persistent server state, so reconnect/respawn must rebuild its
        // corner status even when the player has not toggled it during this client session.
        if (ActiveAbilityRouter.isThreatSuppressionEnabled(player)
                && ServerPlayNetworking.canSend(player, ActivePresentationPayload.TYPE)) {
            ServerPlayNetworking.send(player, new ActivePresentationPayload(
                    THREAT_SUPPRESSION_ID, ActivePresentationPayload.TOGGLE_ON, 0, 24.0F
            ));
        }
    }
}
