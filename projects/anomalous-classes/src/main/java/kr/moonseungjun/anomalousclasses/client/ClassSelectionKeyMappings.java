package kr.moonseungjun.anomalousclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ClassSelectionKeyMappings {
    public static final KeyMapping OPEN_CLASS_MENU = new KeyMapping(
            "key.anomalousclasses.open_class_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.anomalousclasses");

    public static final KeyMapping OPEN_PROFILE = new KeyMapping(
            "key.anomalousclasses.open_profile",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.anomalousclasses");

    private ClassSelectionKeyMappings() {}
}
