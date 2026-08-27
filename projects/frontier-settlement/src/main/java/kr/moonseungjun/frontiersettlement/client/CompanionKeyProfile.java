package kr.moonseungjun.frontiersettlement.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Pack-safe client key normalization for known redundant defaults.
 *
 * This never rewrites a user-customized binding. It only removes the shipped Xaero quick-waypoint
 * B binding while that mapping is still on its own default. Xaero's U waypoint screen remains
 * available, while B is left free for Sophisticated Backpacks and M belongs to Frontier.
 */
public final class CompanionKeyProfile {
    private static boolean applied;

    private CompanionKeyProfile() {}

    public static void tick(ClientTickEvent.Post event) {
        if (applied) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) return;
        applied = true;

        boolean changed = false;
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (!mapping.isDefault()) continue;
            String name = mapping.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("xaero")) continue;
            if (mapping.getKey().getValue() != GLFW.GLFW_KEY_B) continue;

            minecraft.options.setKey(mapping, InputConstants.UNKNOWN);
            changed = true;
        }
        if (changed) {
            KeyMapping.resetMapping();
            minecraft.options.save();
        }
    }

    public static void resetSession() {
        applied = false;
    }
}
