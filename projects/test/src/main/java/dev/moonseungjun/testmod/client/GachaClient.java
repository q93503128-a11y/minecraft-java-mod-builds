package dev.moonseungjun.testmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class GachaClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("moon_test", "experiments")
    );

    private static final KeyMapping PLAY_GACHA = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.moon_test.gacha", InputConstants.KEY_G, CATEGORY)
    );

    private static int previewIndex;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PLAY_GACHA.consumeClick()) {
                GachaCinematicScreen.Rarity[] values = GachaCinematicScreen.Rarity.values();
                GachaCinematicScreen.Rarity rarity = values[Math.floorMod(previewIndex++, values.length)];
                client.gui.setScreen(new GachaCinematicScreen(rarity));
            }
        });
    }
}
