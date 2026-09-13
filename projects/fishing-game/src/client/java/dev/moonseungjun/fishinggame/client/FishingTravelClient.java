package dev.moonseungjun.fishinggame.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.moonseungjun.fishinggame.FishingGameMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class FishingTravelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(FishingGameMod.id("travel_controls"));
        KeyMapping travelKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.fishinggame.travel",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (travelKey.consumeClick()) {
                if (client.player.fishing != null) continue;
                if (client.gui.screen() instanceof TravelScreen) client.gui.setScreen(null);
                else client.gui.setScreen(new TravelScreen());
            }
        });
    }
}
