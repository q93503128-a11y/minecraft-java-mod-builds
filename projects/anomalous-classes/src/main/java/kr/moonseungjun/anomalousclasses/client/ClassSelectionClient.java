package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import net.minecraft.client.Minecraft;

public final class ClassSelectionClient {
    private ClassSelectionClient() {}

    public static void receive(ClassSelectionStatePayload state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state.result() == ClassSelectionStatePayload.Result.OPEN) {
            if (minecraft.screen instanceof ClassSelectionScreen screen) {
                screen.applyServerState(state);
            } else {
                minecraft.setScreen(new ClassSelectionScreen(state));
            }
            return;
        }
        if (minecraft.screen instanceof ClassSelectionScreen screen) screen.applyServerState(state);
    }

    public static void receive(ProfileStatePayload state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ProfileScreen screen) screen.applyServerState(state);
        else minecraft.setScreen(new ProfileScreen(state));
    }

    public static void receive(SkillTreeStatePayload state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SkillTreeScreen screen) screen.applyServerState(state);
        else minecraft.setScreen(new SkillTreeScreen(state));
    }
}
