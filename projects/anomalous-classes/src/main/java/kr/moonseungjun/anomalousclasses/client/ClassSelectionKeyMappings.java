package kr.moonseungjun.anomalousclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ClassSelectionKeyMappings {
    public static final KeyMapping OPEN_PROFILE = new KeyMapping(
            "key.anomalousclasses.open_profile",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.anomalousclasses");


    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
            "key.anomalousclasses.open_skill_tree",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.anomalousclasses");

    public static final KeyMapping OPEN_CLASS_MANAGEMENT = new KeyMapping(
            "key.anomalousclasses.open_class_management",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            "key.categories.anomalousclasses");

    public static final KeyMapping MENTAL_INTRUSION = new KeyMapping(
            "key.anomalousclasses.mental_intrusion",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.anomalousclasses");

    public static final KeyMapping MENTAL_SHOCK = skill("key.anomalousclasses.mental_shock", GLFW.GLFW_KEY_Z);
    public static final KeyMapping MENTAL_SUGGESTION = skill("key.anomalousclasses.mental_suggestion", GLFW.GLFW_KEY_X);
    public static final KeyMapping MENTAL_OVERLOAD = skill("key.anomalousclasses.mental_overload", GLFW.GLFW_KEY_C);
    public static final KeyMapping MENTAL_CONTAINMENT = skill("key.anomalousclasses.mental_containment", GLFW.GLFW_KEY_V);

    private static KeyMapping skill(String key, int glfw) {
        return new KeyMapping(key, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
                glfw, "key.categories.anomalousclasses");
    }

    private ClassSelectionKeyMappings() {}
}
