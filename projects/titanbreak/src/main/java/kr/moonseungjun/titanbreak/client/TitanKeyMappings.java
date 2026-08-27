package kr.moonseungjun.titanbreak.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.network.DriveTogglePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class TitanKeyMappings {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "controls"));

    public static final KeyMapping REFLEX_DRIVE = new KeyMapping(
            "key.titanbreak.reflex_drive",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    private TitanKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(REFLEX_DRIVE);
    }

    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS || !REFLEX_DRIVE.matches(event.getKeyEvent())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.screen != null) return;

        boolean nextRequested = !TitanClientState.flag("requested");
        ClientPacketDistributor.sendToServer(new DriveTogglePayload(nextRequested));
    }
}
