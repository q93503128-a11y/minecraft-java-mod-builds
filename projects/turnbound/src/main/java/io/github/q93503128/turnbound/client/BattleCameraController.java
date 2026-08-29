package io.github.q93503128.turnbound.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;

/**
 * P0 battle camera: third-person overview behind the commander anchor.
 * The server still owns battle state; this class only owns local presentation.
 */
public final class BattleCameraController {
    private static final float DEFAULT_DISTANCE = 11.0F;
    private static final float MIN_DISTANCE = 6.0F;
    private static final float MAX_DISTANCE = 18.0F;
    private static final float MIN_PITCH = -10.0F;
    private static final float MAX_PITCH = 58.0F;

    private static boolean active;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static float previousYaw;
    private static float previousPitch;
    private static float distance = DEFAULT_DISTANCE;

    private BattleCameraController() {
    }

    public static void enter() {
        if (active) return;
        Minecraft minecraft = Minecraft.getInstance();
        previousCameraType = minecraft.options.getCameraType();
        if (minecraft.player != null) {
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
            minecraft.player.setXRot(22.0F);
        }
        distance = DEFAULT_DISTANCE;
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
    }

    public static void exit() {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.setCameraType(previousCameraType);
        if (minecraft.player != null) {
            minecraft.player.setYRot(previousYaw);
            minecraft.player.setXRot(previousPitch);
        }
        active = false;
        distance = DEFAULT_DISTANCE;
    }

    static void orbit(double deltaX, double deltaY) {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.player.setYRot(minecraft.player.getYRot() - (float) deltaX * 0.18F);
        minecraft.player.setXRot(clamp(minecraft.player.getXRot() + (float) deltaY * 0.15F, MIN_PITCH, MAX_PITCH));
    }

    static void zoom(double scrollY) {
        if (!active || scrollY == 0.0D) return;
        distance = clamp(distance - (float) scrollY * 0.75F, MIN_DISTANCE, MAX_DISTANCE);
    }

    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (active && !event.isCameraFlipped()) {
            event.setDistance(distance);
        }
    }

    static float distanceForTest() {
        return distance;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
