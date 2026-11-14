package kr.moonseungjun.anomalousclasses.client;

import net.minecraft.client.Minecraft;

/** One-shot client-only origin tracking for a server-opened skill tree. */
final class SkillTreeNavigation {
    enum ReturnTarget { WORLD, PROFILE }

    private static final int REQUEST_TIMEOUT_TICKS = 200;
    private static ReturnTarget pendingTarget;
    private static int ticksRemaining;

    private SkillTreeNavigation() {}

    static void begin(ReturnTarget target) {
        pendingTarget = target;
        ticksRemaining = REQUEST_TIMEOUT_TICKS;
    }

    static ReturnTarget consumeForResponse(Minecraft minecraft) {
        ReturnTarget target = pendingTarget;
        clear();
        if (target == null) return null;
        boolean expectedScreen = target == ReturnTarget.PROFILE
                ? minecraft.screen instanceof ProfileScreen
                : minecraft.screen == null;
        return expectedScreen ? target : null;
    }

    static void cancel(ReturnTarget target) {
        if (pendingTarget == target) clear();
    }

    static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            clear();
        } else if (pendingTarget != null && --ticksRemaining <= 0) {
            clear();
        }
    }

    private static void clear() {
        pendingTarget = null;
        ticksRemaining = 0;
    }
}
