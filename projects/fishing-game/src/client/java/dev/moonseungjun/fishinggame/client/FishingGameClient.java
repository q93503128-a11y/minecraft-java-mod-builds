package dev.moonseungjun.fishinggame.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.client.fish.AnglerEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.BreamEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.CatfishEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.CyprinidEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.EncounterFishModelLayers;
import dev.moonseungjun.fishinggame.client.fish.EncounterFishRenderer;
import dev.moonseungjun.fishinggame.client.fish.FatEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.LongEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.PelagicEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.SmallEncounterFishModel;
import dev.moonseungjun.fishinggame.client.fish.TallEncounterFishModel;
import dev.moonseungjun.fishinggame.entity.FishingEntities;
import dev.moonseungjun.fishinggame.fishing.CastChargeMath;
import dev.moonseungjun.fishinggame.network.CastReleasePayload;
import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.network.ProfileSnapshotPayload;
import dev.moonseungjun.fishinggame.network.ReelInputPayload;
import dev.moonseungjun.fishinggame.profile.PlayerFishingProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public final class FishingGameClient implements ClientModInitializer {
    private static final int HEARTBEAT_TICKS = 5;
    private static boolean castCharging;
    private static int castChargeTicks;

    private boolean lastHeld;
    private boolean previousUseDown;
    private int heartbeatTicks;

    @Override
    public void onInitializeClient() {
        registerEncounterFishRendering();
        FishingHud.initialize();

        ClientPlayNetworking.registerGlobalReceiver(
                ProfileSnapshotPayload.TYPE,
                (payload, context) -> ClientFishingState.apply(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(
                FishingStatePayload.TYPE,
                (payload, context) -> ClientFishingState.apply(payload)
        );

        KeyMapping.Category category = KeyMapping.Category.register(FishingGameMod.id("controls"));
        KeyMapping bagKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishinggame.catch_bag",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));
        KeyMapping bestiaryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishinggame.bestiary",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                resetCastCharge();
                previousUseDown = false;
                lastHeld = false;
                heartbeatTicks = 0;
                return;
            }

            while (bagKey.consumeClick()) {
                if (client.gui.screen() instanceof CatchBagScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new CatchBagScreen());
                }
            }

            while (bestiaryKey.consumeClick()) {
                if (client.player.fishing != null) continue;
                if (client.gui.screen() instanceof BestiaryScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new BestiaryScreen());
                }
            }

            boolean useDown = client.options.keyUse.isDown();
            boolean fishing = client.player.fishing != null;
            if (!fishing) {
                boolean holdingRod = client.player.getMainHandItem().is(Items.FISHING_ROD)
                        || client.player.getOffhandItem().is(Items.FISHING_ROD);
                boolean bagHasSpace = ClientFishingState.catches().size() < PlayerFishingProfile.BAG_CAPACITY;
                boolean canCharge = holdingRod && bagHasSpace && client.gui.screen() == null;

                if (!castCharging && useDown && !previousUseDown && canCharge) {
                    castCharging = true;
                    castChargeTicks = 0;
                }

                if (castCharging) {
                    if (useDown && canCharge) {
                        castChargeTicks++;
                        if (castChargeTicks == CastChargeMath.FULL_CHARGE_TICKS) {
                            FishingClientAudio.onCastFullyCharged();
                        }
                    } else {
                        boolean cancelled = !holdingRod || client.gui.screen() != null || !bagHasSpace;
                        sendCastRelease(cancelled);
                        resetCastCharge();
                    }
                }

                previousUseDown = useDown;
                lastHeld = false;
                heartbeatTicks = 0;
                return;
            }

            if (castCharging) {
                sendCastRelease(true);
                resetCastCharge();
            }
            previousUseDown = useDown;

            boolean held = useDown;
            heartbeatTicks++;
            if (held != lastHeld || heartbeatTicks >= HEARTBEAT_TICKS) {
                if (ClientPlayNetworking.canSend(ReelInputPayload.TYPE)) {
                    ClientPlayNetworking.send(new ReelInputPayload(held));
                }
                lastHeld = held;
                heartbeatTicks = 0;
            }
        });
    }

    public static boolean isCastCharging() {
        return castCharging;
    }

    public static float castChargeProgress() {
        return CastChargeMath.normalizedCharge(castChargeTicks);
    }

    private static void sendCastRelease(boolean cancelled) {
        if (ClientPlayNetworking.canSend(CastReleasePayload.TYPE)) {
            ClientPlayNetworking.send(new CastReleasePayload(cancelled));
        }
    }

    private static void resetCastCharge() {
        castCharging = false;
        castChargeTicks = 0;
    }

    @SuppressWarnings("deprecation")
    private static void registerEncounterFishRendering() {
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.SMALL, SmallEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.TALL, TallEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.FAT, FatEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.LONG, LongEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.ANGLER, AnglerEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.CYPRINID, CyprinidEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.PELAGIC, PelagicEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.BREAM, BreamEncounterFishModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(EncounterFishModelLayers.CATFISH, CatfishEncounterFishModel::createBodyLayer);

        EntityRendererRegistry.register(
                FishingEntities.SMALL_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.SMALL, SmallEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.TALL_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.TALL, TallEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.FAT_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.FAT, FatEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.LONG_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.LONG, LongEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.ANGLER_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.ANGLER, AnglerEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.CYPRINID_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.CYPRINID, CyprinidEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.PELAGIC_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.PELAGIC, PelagicEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.BREAM_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.BREAM, BreamEncounterFishModel::new)
        );
        EntityRendererRegistry.register(
                FishingEntities.CATFISH_FISH,
                context -> new EncounterFishRenderer(context, EncounterFishModelLayers.CATFISH, CatfishEncounterFishModel::new)
        );
    }
}
