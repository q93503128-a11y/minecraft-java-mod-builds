package dev.moonseungjun.augmentationsurvival.network;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveChargePresentationService;
import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.DominationWorldState;
import dev.moonseungjun.augmentationsurvival.augment.companion.ReviveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Common networking for active use requests, presentation and the server-owned ten-slot loadout. */
public final class ActiveAbilityNetworking {
    private static final String INSTANT_MINING_ID = UtilityAugmentService.INSTANT_MINING_ID;
    private static final String DOMINATION_ID = AugmentationSurvival.MOD_ID + ":domination";
    private static final String THREAT_SUPPRESSION_ID = AugmentationSurvival.MOD_ID + ":threat_suppression";

    private ActiveAbilityNetworking() { }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(ActiveAbilityRequestPayload.TYPE, ActiveAbilityRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActiveLoadoutEditPayload.TYPE, ActiveLoadoutEditPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DominationManageRequestPayload.TYPE, DominationManageRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DominationRosterPayload.TYPE, DominationRosterPayload.CODEC);
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
        ServerPlayNetworking.registerGlobalReceiver(
                DominationManageRequestPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer owner = context.player();
                    if (AugmentationPlayerStateService.state(owner).level(DOMINATION_ID) <= 0) return;
                    switch (payload.action()) {
                        case DominationManageRequestPayload.RELEASE -> {
                            try {
                                CompanionAugmentService.remoteRelease(owner, UUID.fromString(payload.entityId()));
                            } catch (IllegalArgumentException ignored) {
                                // Invalid UUIDs are not trusted from clients.
                            }
                        }
                        case DominationManageRequestPayload.RELEASE_ALL -> CompanionAugmentService.remoteReleaseAll(owner);
                        default -> { }
                    }
                    syncDomination(owner);
                }
        );
        ServerPlayerEvents.JOIN.register(ActiveAbilityNetworking::syncNow);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> syncNow(newPlayer));
        AugmentProgressNetworking.initialize();
        ProtectionHudNetworking.initialize();
    }

    public static void syncDomination(ServerPlayer owner) {
        if (!ServerPlayNetworking.canSend(owner, DominationRosterPayload.TYPE)) return;
        int level = AugmentationPlayerStateService.state(owner).level(DOMINATION_ID);
        if (level <= 0) return;
        var server = owner.level().getServer();
        DominationWorldState world = CompanionAugmentService.roster(server);
        List<DominationRosterPayload.Member> members = new ArrayList<>();
        for (var row : world.membersOf(owner.getUUID())) {
            UUID id;
            try { id = UUID.fromString(row.getKey()); }
            catch (IllegalArgumentException invalid) { continue; }
            DominationWorldState.Member value = row.getValue();
            members.add(new DominationRosterPayload.Member(
                    row.getKey(), value.type(), value.dimension(), value.reserved(),
                    CompanionAugmentService.isLoadedCompanion(id, owner.getUUID()),
                    value.reserved() ? ReviveAugmentService.remainingSeconds(server, id) : 0
            ));
        }
        ServerPlayNetworking.send(owner, new DominationRosterPayload(
                world.occupied(owner.getUUID()), AdvancedWorldTuning.current().dominationCap(level), members
        ));
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

        syncDomination(player);

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
