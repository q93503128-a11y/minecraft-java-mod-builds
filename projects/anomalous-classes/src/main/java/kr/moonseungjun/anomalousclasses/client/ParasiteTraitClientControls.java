package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.network.payload.ParasiteTraitInputPayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client-side digit capture only; the server validates every cast. */
public final class ParasiteTraitClientControls {
    private static int currentClassIndex = -1;
    private static boolean mode;
    private ParasiteTraitClientControls() {}

    public static boolean isPact() {
        return currentClassIndex == ClassId.PACT_CASTER.networkIndex();
    }
    public static boolean isBarrier() {
        return currentClassIndex == ClassId.BARRIER_CASTER.networkIndex();
    }
    public static boolean isParasite() {
        return currentClassIndex == ClassId.PARASITE_MORPHER.networkIndex();
    }
    public static void updateClass(int index) {
        if (index != currentClassIndex) mode = false;
        currentClassIndex = index;
        if (!isParasite()) mode = false;
    }
    public static void confirm(boolean enabled) {
        mode = enabled && isParasite();
    }
    public static void reset() {
        currentClassIndex = -1;
        mode = false;
    }
    public static void toggle() {
        if (!isParasite()) return;
        mode = !mode;
        PacketDistributor.sendToServer(new ParasiteTraitInputPayload(-1));
    }

    /** Suppress vanilla hotbar switching only while explicit trait mode is active. */
    public static boolean interceptNumber(int key, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!mode || !isParasite() || minecraft.player == null || minecraft.level == null
                || minecraft.screen != null) return false;
        int index = key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9
                ? key - GLFW.GLFW_KEY_1
                : key == GLFW.GLFW_KEY_0 ? 9 : -1;
        if (index < 0) return false;
        if (action == GLFW.GLFW_PRESS)
            PacketDistributor.sendToServer(new ParasiteTraitInputPayload(index));
        // Consume both release and key-repeat; a held key must not spam skills.
        return true;
    }
}
