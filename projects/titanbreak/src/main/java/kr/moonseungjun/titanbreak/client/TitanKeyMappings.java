package kr.moonseungjun.titanbreak.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.network.AugmentAbilityPayload;
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

    public static final KeyMapping ANALYSIS = new KeyMapping(
            "key.titanbreak.analysis",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY);

    public static final KeyMapping HOOK = new KeyMapping(
            "key.titanbreak.hook",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

    private TitanKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(REFLEX_DRIVE);
        event.register(ANALYSIS);
        event.register(HOOK);
    }

    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.gui.screen() != null) return;

        if (REFLEX_DRIVE.matches(event.getKeyEvent())) {
            boolean nextRequested = !TitanClientState.flag("requested");
            ClientPacketDistributor.sendToServer(new DriveTogglePayload(nextRequested));
        } else if (HOOK.matches(event.getKeyEvent())) {
            int ability = mc.player.isShiftKeyDown()
                    ? AugmentAbilityPayload.PHASE_STEP
                    : AugmentAbilityPayload.HOOK;
            ClientPacketDistributor.sendToServer(new AugmentAbilityPayload(ability));
        }
    }
}
