package io.github.q93503128.turnbound.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Battlefield-centered orbit camera following the v0.4 battle-camera contract.
 * Minecraft's own third-person collision ray cast runs after the detached-distance event,
 * so the requested distance is still shortened when terrain blocks the camera.
 *
 * <p>Authoritative combat never waits for the camera. Snapshot transitions only add a
 * presentation kick (damage, knock-down, multi-hit and revive) on top of the player's
 * current orbit, so combat remains readable without stealing camera control.</p>
 */
public final class BattleCameraController {
    /** alpha.16 visual calibration: closer/lower default while preserving the authored v0.4 orbit limits. */
    private static final float DEFAULT_DISTANCE = 9.25F;
    private static final float MIN_DISTANCE = 6.0F;
    private static final float MAX_DISTANCE = 18.0F;
    private static final float DEFAULT_PITCH = 16.0F;
    private static final float MIN_PITCH = -10.0F;
    private static final float MAX_PITCH = 58.0F;
    private static final float HORIZONTAL_DEGREES_PER_PIXEL = 0.18F;
    private static final float VERTICAL_DEGREES_PER_PIXEL = 0.15F;
    private static final float WHEEL_DISTANCE_STEP = 0.75F;
    private static final float FOV = 62.0F;

    private static boolean active;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static float previousYaw;
    private static float previousPitch;
    private static float currentYaw;
    private static float currentPitch;
    private static float targetYaw;
    private static float targetPitch;
    private static float distance = DEFAULT_DISTANCE;

    // Short presentation impulse. These offsets decay independently from the player's orbit target.
    private static float impactYaw;
    private static float impactPitch;
    private static float impactRoll;
    private static float impactDistance;
    private static float impactFov;
    private static int impactFrames;
    private static int impactPhase;

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
        clearImpulse();
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
        clearImpulse();
    }

    static void orbit(double deltaX, double deltaY) {
        if (!active) return;
        targetYaw = Mth.wrapDegrees(targetYaw - (float) deltaX * HORIZONTAL_DEGREES_PER_PIXEL);
        targetPitch = Mth.clamp(targetPitch + (float) deltaY * VERTICAL_DEGREES_PER_PIXEL, MIN_PITCH, MAX_PITCH);
    }

    static void zoom(double scrollY) {
        if (!active || scrollY == 0.0D) return;
        distance = Mth.clamp(distance - (float) scrollY * WHEEL_DISTANCE_STEP, MIN_DISTANCE, MAX_DISTANCE);
    }

    /**
     * Converts an authoritative snapshot delta into a single readable camera impulse.
     * AoE damage intentionally creates one combined kick instead of one kick per target.
     */
    static void onSnapshotTransition(ClientBattleState.Snapshot before, ClientBattleState.Snapshot after) {
        if (!active || before == null || after == null || !after.active()) return;

        float strongestRatio = 0.0F;
        int damagedTargets = 0;
        boolean knockDown = false;
        boolean bossImpact = false;
        boolean revived = false;

        for (ClientBattleState.Unit current : after.units()) {
            ClientBattleState.Unit previous = unit(before, current.id());
            if (previous == null) continue;

            int lost = Math.max(0, previous.hp() - current.hp());
            if (lost > 0) {
                damagedTargets++;
                strongestRatio = Math.max(strongestRatio, lost / (float) Math.max(1, previous.maxHp()));
                knockDown |= !previous.downed() && current.downed();
                bossImpact |= current.defId().startsWith("B");
            }
            revived |= previous.downed() && !current.downed();
        }

        if (damagedTargets > 0) {
            impact(strongestRatio, damagedTargets, knockDown, bossImpact);
        } else if (revived) {
            revivePulse();
        }
    }

    private static ClientBattleState.Unit unit(ClientBattleState.Snapshot snapshot, String id) {
        for (ClientBattleState.Unit unit : snapshot.units()) if (unit.id().equals(id)) return unit;
        return null;
    }

    private static void impact(float damageRatio, int targets, boolean knockDown, boolean bossImpact) {
        float strength = 0.60F + Mth.clamp(damageRatio, 0.0F, 0.60F) * 4.2F;
        if (targets > 1) strength += Math.min(0.65F, (targets - 1) * 0.18F);
        if (knockDown) strength += 0.85F;
        if (bossImpact) strength += 0.35F;
        strength = Mth.clamp(strength, 0.65F, 3.65F);

        impactYaw = Math.max(impactYaw, strength * 0.42F);
        impactPitch = Math.max(impactPitch, strength * 0.32F);
        impactRoll = Math.max(impactRoll, strength * 0.23F);
        impactDistance = Math.max(impactDistance, 0.16F + strength * 0.11F);
        impactFov = Math.max(impactFov, 0.65F + strength * 0.42F);
        impactFrames = Math.max(impactFrames, knockDown || bossImpact ? 16 : 11);
    }

    private static void revivePulse() {
        // A small outward release reads as recovery without reusing the violent hit shake.
        impactDistance = Math.min(impactDistance, -0.28F);
        impactFov = Math.min(impactFov, -1.15F);
        impactRoll = Math.max(impactRoll, 0.18F);
        impactFrames = Math.max(impactFrames, 12);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        // Short ease removes mouse stepping without making orbit feel detached from the cursor.
        currentYaw = Mth.wrapDegrees(currentYaw + Mth.wrapDegrees(targetYaw - currentYaw) * 0.62F);
        currentPitch += (targetPitch - currentPitch) * 0.62F;
        applyPlayerView(Minecraft.getInstance());

        float shakeSign = (impactPhase++ & 1) == 0 ? 1.0F : -1.0F;
        float verticalSign = impactFrames % 3 == 0 ? -0.60F : 1.0F;
        event.setYaw(currentYaw + impactYaw * shakeSign);
        event.setPitch(currentPitch + impactPitch * verticalSign);
        event.setRoll(impactRoll * shakeSign);
        decayImpulse();
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active) event.setFOV(FOV - impactFov);
    }

    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (active && !event.isCameraFlipped()) event.setDistance(Mth.clamp(distance - impactDistance, 2.0F, MAX_DISTANCE));
    }

    private static void decayImpulse() {
        if (impactFrames <= 0) {
            clearImpulse();
            return;
        }
        impactFrames--;
        impactYaw *= 0.72F;
        impactPitch *= 0.72F;
        impactRoll *= 0.70F;
        impactDistance *= 0.78F;
        impactFov *= 0.78F;
        if (impactFrames == 0) clearImpulse();
    }

    private static void clearImpulse() {
        impactYaw = 0.0F;
        impactPitch = 0.0F;
        impactRoll = 0.0F;
        impactDistance = 0.0F;
        impactFov = 0.0F;
        impactFrames = 0;
        impactPhase = 0;
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
