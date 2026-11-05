package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.GachaPresentationTimeline;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Client-only camera rig for the private in-world summon stage. */
public final class SummonCameraController {
    private static final float MIN_DISTANCE = 2.8F;
    private static final float MAX_DISTANCE = 6.5F;
    private static final float VIEW_LERP = 0.34F;
    private static final float DISTANCE_LERP = 0.30F;
    private static final float FOV_LERP = 0.28F;

    private static boolean active;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static Entity previousCameraEntity;
    private static ArmorStand anchor;
    private static float previousYaw;
    private static float previousPitch;

    private static double pivotX;
    private static double pivotY;
    private static double pivotZ;
    private static float baseYaw;
    private static float currentYaw;
    private static float targetYaw;
    private static float currentPitch;
    private static float targetPitch;
    private static float currentDistance;
    private static float targetDistance;
    private static float currentFov;
    private static float targetFov;

    private SummonCameraController() {}

    public static void enter(double x, double y, double z, float cameraYaw) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(cameraYaw)) return;
        if (active) exit();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        previousCameraType = minecraft.options.getCameraType();
        previousCameraEntity = minecraft.getCameraEntity();
        if (minecraft.player != null) {
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
        }

        pivotX = x;
        pivotY = y + 1.45D;
        pivotZ = z;
        baseYaw = Mth.wrapDegrees(cameraYaw);
        currentYaw = targetYaw = Mth.wrapDegrees(baseYaw + 12.0F);
        currentPitch = targetPitch = 8.0F;
        currentDistance = targetDistance = 5.1F;
        currentFov = targetFov = 43.0F;

        anchor = new ArmorStand(minecraft.level, pivotX, pivotY, pivotZ);
        anchor.setInvisible(true);
        anchor.setNoGravity(true);
        minecraft.setCameraEntity(anchor);
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
        apply();
    }

    public static void update(int slotTick, GachaPresentationTimeline.Phase phase, int stars) {
        if (!active || phase == null) return;
        float intensity = GachaPresentationTimeline.intensity(stars);
        float rarity = intensity / 4.0F;
        float drift = (float)Math.sin(Math.max(0, slotTick) * 0.105F) * (1.4F + rarity * 1.6F);
        switch (phase) {
            case SIGNAL -> {
                targetYaw = Mth.wrapDegrees(baseYaw + 13.0F + intensity * 0.8F + drift);
                targetPitch = 11.0F - rarity * 0.8F;
                targetDistance = 5.4F + rarity * 0.18F;
                targetFov = 44.0F + rarity * 0.5F;
            }
            case SILHOUETTE -> {
                targetYaw = Mth.wrapDegrees(baseYaw + 8.0F + drift * 0.65F);
                targetPitch = 9.5F - rarity * 0.9F;
                targetDistance = 4.5F - intensity * 0.10F;
                targetFov = 41.0F - intensity * 0.30F;
            }
            case REVEAL -> {
                targetYaw = Mth.wrapDegrees(baseYaw - 1.5F - intensity * 1.25F + drift * 0.30F);
                targetPitch = 7.5F - rarity * 1.0F;
                targetDistance = 3.6F - intensity * 0.12F;
                targetFov = 38.0F - intensity * 0.55F;
            }
            case NAME -> {
                targetYaw = Mth.wrapDegrees(baseYaw - 4.0F - intensity * 0.55F + drift * 0.20F);
                targetPitch = 10.5F;
                targetDistance = 3.9F - intensity * 0.08F;
                targetFov = 39.0F - intensity * 0.20F;
            }
            case COMPLETE -> { }
        }
    }

    public static void exit() {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        Entity restore = previousCameraEntity;
        if (restore == null || restore.level() != minecraft.level) restore = minecraft.player;
        minecraft.setCameraEntity(restore);
        minecraft.options.setCameraType(previousCameraType);
        if (minecraft.player != null) {
            minecraft.player.setYRot(previousYaw);
            minecraft.player.setYHeadRot(previousYaw);
            minecraft.player.setXRot(previousPitch);
        }
        active = false;
        anchor = null;
        previousCameraEntity = null;
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        currentYaw = Mth.wrapDegrees(currentYaw + Mth.wrapDegrees(targetYaw - currentYaw) * VIEW_LERP);
        currentPitch += (targetPitch - currentPitch) * VIEW_LERP;
        currentDistance += (targetDistance - currentDistance) * DISTANCE_LERP;
        currentFov += (targetFov - currentFov) * FOV_LERP;
        apply();
        event.setYaw(currentYaw);
        event.setPitch(currentPitch);
        event.setRoll(0.0F);
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active) event.setFOV(currentFov);
    }

    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (active && !event.isCameraFlipped()) {
            event.setDistance(Mth.clamp(currentDistance, MIN_DISTANCE, MAX_DISTANCE));
        }
    }

    private static void apply() {
        if (anchor == null) return;
        anchor.setPos(pivotX, pivotY, pivotZ);
        anchor.setYRot(currentYaw);
        anchor.setYHeadRot(currentYaw);
        anchor.setYBodyRot(currentYaw);
        anchor.setXRot(currentPitch);
        anchor.setOldPosAndRot();
    }

    public static boolean active() { return active; }
}
