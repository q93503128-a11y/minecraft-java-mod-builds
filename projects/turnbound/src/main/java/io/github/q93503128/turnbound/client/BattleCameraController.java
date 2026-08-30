package io.github.q93503128.turnbound.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Battlefield-centered orbit camera.
 *
 * Minecraft's detached third-person camera computes its position from the camera entity's yaw/pitch.
 * The previous implementation changed only the rendered Viewport angles, which could leave the physical
 * third-person camera offset looking from a different direction. alpha.9 keeps the invisible player shell
 * rotation and the rendered camera rotation on the same smoothed view.
 */
public final class BattleCameraController {
    private static final float DEFAULT_DISTANCE = 8.4F;
    private static final float MIN_DISTANCE = 4.5F;
    private static final float MAX_DISTANCE = 16.0F;
    private static final float DEFAULT_PITCH = 18.0F;
    private static final float MIN_PITCH = -10.0F;
    private static final float MAX_PITCH = 60.0F;
    private static final float FOV = 70.0F;

    private static boolean active;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static float previousYaw;
    private static float previousPitch;
    private static float currentYaw;
    private static float currentPitch;
    private static float targetYaw;
    private static float targetPitch;
    private static float distance = DEFAULT_DISTANCE;

    private BattleCameraController() {}

    public record View(float yaw, float pitch, float distance, float fov) {}

    public static void enter(float arenaYaw) {
        if (active) return;
        Minecraft minecraft = Minecraft.getInstance();
        previousCameraType = minecraft.options.getCameraType();
        if (minecraft.player != null) {
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
        }
        currentYaw = arenaYaw;
        targetYaw = arenaYaw;
        currentPitch = DEFAULT_PITCH;
        targetPitch = DEFAULT_PITCH;
        distance = DEFAULT_DISTANCE;
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
        applyPlayerView(minecraft);
    }

    public static void exit() {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.setCameraType(previousCameraType);
        if (minecraft.player != null) {
            minecraft.player.setYRot(previousYaw);
            minecraft.player.setYHeadRot(previousYaw);
            minecraft.player.setXRot(previousPitch);
        }
        active = false;
        distance = DEFAULT_DISTANCE;
    }

    static void orbit(double deltaX, double deltaY) {
        if (!active) return;
        targetYaw = Mth.wrapDegrees(targetYaw - (float) deltaX * 0.92F);
        targetPitch = Mth.clamp(targetPitch + (float) deltaY * 0.66F, MIN_PITCH, MAX_PITCH);
    }

    static void zoom(double scrollY) {
        if (!active || scrollY == 0.0D) return;
        distance = Mth.clamp(distance - (float) scrollY * 0.80F, MIN_DISTANCE, MAX_DISTANCE);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        currentYaw = Mth.wrapDegrees(currentYaw + Mth.wrapDegrees(targetYaw - currentYaw) * 0.52F);
        currentPitch += (targetPitch - currentPitch) * 0.52F;
        applyPlayerView(Minecraft.getInstance());
        event.setYaw(currentYaw);
        event.setPitch(currentPitch);
        event.setRoll(0.0F);
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active) event.setFOV(FOV);
    }

    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (active && !event.isCameraFlipped()) event.setDistance(distance);
    }

    private static void applyPlayerView(Minecraft minecraft) {
        if (minecraft.player == null) return;
        minecraft.player.setYRot(currentYaw);
        minecraft.player.setYHeadRot(currentYaw);
        minecraft.player.setXRot(currentPitch);
    }

    static View view() {
        return new View(currentYaw, currentPitch, distance, FOV);
    }

    static boolean active() { return active; }
}
