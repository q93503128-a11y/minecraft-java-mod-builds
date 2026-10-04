package io.github.q93503128.turnbound.client;

/** Pure Minecraft-yaw to screen-direction projection used by the objective guide. */
final class QuestGuideDirection {
    private QuestGuideDirection() {}

    static double targetDeltaDegrees(
            double playerX, double playerZ, float playerYaw, double targetX, double targetZ
    ) {
        double targetYaw = Math.toDegrees(Math.atan2(-(targetX - playerX), targetZ - playerZ));
        return wrapDegrees(targetYaw - playerYaw);
    }

    static String arrow(double delta) {
        if (delta >= -22.5 && delta < 22.5) return "↑";
        if (delta >= 22.5 && delta < 67.5) return "↗";
        if (delta >= 67.5 && delta < 112.5) return "→";
        if (delta >= 112.5 && delta < 157.5) return "↘";
        if (delta >= 157.5 || delta < -157.5) return "↓";
        if (delta >= -157.5 && delta < -112.5) return "↙";
        if (delta >= -112.5 && delta < -67.5) return "←";
        return "↖";
    }

    private static double wrapDegrees(double value) {
        value %= 360.0;
        if (value >= 180.0) value -= 360.0;
        if (value < -180.0) value += 360.0;
        return value;
    }
}
