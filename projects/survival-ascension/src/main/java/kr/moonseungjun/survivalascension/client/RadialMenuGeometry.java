package kr.moonseungjun.survivalascension.client;

import net.minecraft.client.Minecraft;

/** Shared geometry for all Survival Ascension radial menus. */
final class RadialMenuGeometry {
    private static final double FIRST_CENTER_DEGREES = 90.0D;

    private RadialMenuGeometry() {}

    static int selectedIndex(int itemCount) {
        if (itemCount <= 1) return 0;
        Minecraft minecraft = Minecraft.getInstance();
        double centerX = minecraft.getWindow().getScreenWidth() * 0.5D;
        double centerY = minecraft.getWindow().getScreenHeight() * 0.5D;
        double dx = minecraft.mouseHandler.xpos() - centerX;
        double dy = minecraft.mouseHandler.ypos() - centerY;
        double pointer = normalize(Math.toDegrees(Math.atan2(dy, dx)));
        double step = 360.0D / itemCount;
        double clockwiseFromFirst = normalize(FIRST_CENTER_DEGREES - pointer);
        return Math.floorMod((int) Math.floor((clockwiseFromFirst + step * 0.5D) / step), itemCount);
    }

    static double iconRadians(int index, int itemCount) {
        return Math.toRadians(normalize(centerDegrees(index, itemCount)));
    }

    static double sectorStartRadians(int index, int itemCount) {
        double step = 360.0D / itemCount;
        return Math.toRadians(normalize(centerDegrees(index, itemCount) - step * 0.5D));
    }

    static double sectorEndRadians(int index, int itemCount) {
        double step = 360.0D / itemCount;
        return Math.toRadians(normalize(centerDegrees(index, itemCount) + step * 0.5D));
    }

    private static double centerDegrees(int index, int itemCount) {
        return FIRST_CENTER_DEGREES - (360.0D / itemCount) * index;
    }

    private static double normalize(double degrees) {
        double value = degrees % 360.0D;
        return value < 0.0D ? value + 360.0D : value;
    }
}
