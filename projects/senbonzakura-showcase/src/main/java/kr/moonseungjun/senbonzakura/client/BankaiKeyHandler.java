package kr.moonseungjun.senbonzakura.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.senbonzakura.network.BankaiVisualPayload;
import kr.moonseungjun.senbonzakura.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class BankaiKeyHandler {
    public static final KeyMapping BANKAI = new KeyMapping(
            "key.senbonzakura.bankai",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KeyMapping.Category.MISC);

    private BankaiKeyHandler() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(BANKAI);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        while (BANKAI.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) continue;

            boolean holding = player.getMainHandItem().getItem() == ModItems.SENBONZAKURA.get()
                    || player.getOffhandItem().getItem() == ModItems.SENBONZAKURA.get();
            if (!holding) continue;

            ClientPacketDistributor.sendToServer(new BankaiVisualPayload("action=request"));
        }
    }
}
