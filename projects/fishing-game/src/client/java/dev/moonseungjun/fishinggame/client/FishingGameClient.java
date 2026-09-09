package dev.moonseungjun.fishinggame.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.network.ProfileSnapshotPayload;
import dev.moonseungjun.fishinggame.network.ReelInputPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class FishingGameClient implements ClientModInitializer {
    private static final int HEARTBEAT_TICKS = 5;
    private boolean lastHeld;
    private int heartbeatTicks;

    @Override
    public void onInitializeClient() {
        FishingHud.initialize();

        ClientPlayNetworking.registerGlobalReceiver(ProfileSnapshotPayload.TYPE, (payload, context) -> ClientFishingState.apply(payload));
        ClientPlayNetworking.registerGlobalReceiver(FishingStatePayload.TYPE, (payload, context) -> ClientFishingState.apply(payload));

        KeyMapping.Category category = KeyMapping.Category.register(FishingGameMod.id("controls"));
        KeyMapping bagKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishinggame.catch_bag",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (bagKey.consumeClick()) {
                if (client.gui.screen() instanceof CatchBagScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new CatchBagScreen());
                }
            }

            boolean fishing = client.player.fishing != null;
            if (!fishing) {
                lastHeld = false;
                heartbeatTicks = 0;
                return;
            }

            boolean held = client.options.keyUse.isDown();
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
}
