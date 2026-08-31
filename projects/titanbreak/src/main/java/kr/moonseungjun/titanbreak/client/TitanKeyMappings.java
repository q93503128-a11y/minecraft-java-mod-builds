package kr.moonseungjun.titanbreak.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.network.AugmentAbilityPayload;
import kr.moonseungjun.titanbreak.network.DriveTogglePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public final class TitanKeyMappings {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "controls"));
    private static final Set<String> REPLACEMENT_ARMS = Set.of(
            "blade_arm", "high_frequency_blade_arm", "power_arm", "wire_hook_arm",
            "rail_projector_arm", "photon_emitter_arm", "shock_palm", "shield_projector_arm");

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

    public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || mc.gui.screen() != null) return;

        String rightArm = TitanClientState.installedIn("RIGHT_ARM_MAIN");
        String leftArm = TitanClientState.installedIn("LEFT_ARM_MAIN");
        boolean rightReplacement = REPLACEMENT_ARMS.contains(rightArm);
        boolean leftReplacement = REPLACEMENT_ARMS.contains(leftArm);
        if (!rightReplacement && !leftReplacement) return;

        InteractionHand selected = rightReplacement ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (event.getHand() != selected) return;

        event.setCanceled(true);
        event.setSwingHand(true);
        ClientPacketDistributor.sendToServer(new AugmentAbilityPayload(
                selected == InteractionHand.MAIN_HAND ? AugmentAbilityPayload.ARM_RIGHT : AugmentAbilityPayload.ARM_LEFT));
    }
}
