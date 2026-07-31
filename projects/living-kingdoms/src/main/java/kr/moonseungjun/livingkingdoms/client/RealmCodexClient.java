package kr.moonseungjun.livingkingdoms.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.livingkingdoms.network.RequestCodexPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class RealmCodexClient {
    private static final KeyMapping MAP_KEY = new KeyMapping(
            "key.livingkingdoms.realm_map", InputConstants.KEY_M, KeyMapping.Category.MISC
    );
    private static final KeyMapping STATUS_KEY = new KeyMapping(
            "key.livingkingdoms.character_status", InputConstants.KEY_K, KeyMapping.Category.MISC
    );

    private RealmCodexClient() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MAP_KEY);
        event.register(STATUS_KEY);
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        while (MAP_KEY.consumeClick()) request("map");
        while (STATUS_KEY.consumeClick()) request("status");
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        int y = 4;
        event.addListener(Button.builder(Component.literal("지도 [M]"), ignored -> request("map"))
                .pos(4, y).size(54, 18).build());
        event.addListener(Button.builder(Component.literal("상태 [K]"), ignored -> request("status"))
                .pos(61, y).size(54, 18).build());
    }

    private static void request(String page) {
        if (Minecraft.getInstance().player != null) {
            ClientPacketDistributor.sendToServer(new RequestCodexPayload(page));
        }
    }
}
