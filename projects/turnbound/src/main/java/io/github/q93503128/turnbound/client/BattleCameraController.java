package io.github.q93503128.turnbound.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Battle-center three-quarter camera.
 *
 * <p>The default frame is derived from the actual ally/enemy formation rather than the player's pre-battle view.
 * Manual orbit and zoom remain correction tools for unusual terrain, while Minecraft's detached-camera ray cast
 * still performs the final wall collision clamp.</p>
 */
public final class BattleCameraController {
    private static final float MIN_DISTANCE = 7.0F;
    private static final float MAX_DISTANCE = 17.0F;
    private static final float MIN_PITCH = 10.0F;
    private static final float MAX_PITCH = 44.0F;
    private static final float HORIZONTAL_DEGREES_PER_PIXEL = 0.44F;
    private static final float VERTICAL_DEGREES_PER_PIXEL = 0.36F;
    private static final float WHEEL_DISTANCE_STEP = 0.90F;
    private static final float VIEW_LERP = 0.66F;
    private static final float ZOOM_LERP = 0.38F;

    private static boolean active;
    private static boolean manualAdjusted;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static float previousYaw;
    private static float previousPitch;

    private static float baseYaw;
    private static float basePitch = 23.0F;
    private static float baseDistance = 10.5F;
    private static float baseFov = 52.0F;

    private static float currentYaw;
    private static float currentPitch;
    private static float currentDistance = baseDistance;
    private static float currentFov = baseFov;
    private static float targetYaw;
    private static float targetPitch = basePitch;
    private static float targetDistance = baseDistance;
    private static float targetFov = baseFov;

    private static float impactYaw;
    private static float impactPitch;
    private static float impactRoll;
    private static float impactDistance;
    private static float impactFov;
    private static int impactFrames;
    private static int impactPhase;

    private BattleCameraController() {}

    public record View(float yaw, float pitch, float distance, float fov) {}

    public static void enter(ClientBattleState.Snapshot snapshot) {
        if (active) return;
        Minecraft minecraft = Minecraft.getInstance();
        previousCameraType = minecraft.options.getCameraType();
        if (minecraft.player != null) {
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
        }

        BattleCameraFraming.Plan plan = BattleCameraFraming.plan(snapshot);
        applyBase(plan, true);
        manualAdjusted = false;
        clearImpulse();
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active = true;
        applyPlayerView(minecraft);
    }

    /** Compatibility entry point for tests or older callers that only know the authored arena yaw. */
    public static void enter(float authoredArenaYaw) {
        if (active) return;
        Minecraft minecraft = Minecraft.getInstance();
        previousCameraType = minecraft.options.getCameraType();
        if (minecraft.player != null) {
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
        }

        applyBase(BattleCameraFraming.fallback(authoredArenaYaw), true);
        manualAdjusted = false;
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
        manualAdjusted = false;
        baseDistance = 10.5F;
        baseFov = 52.0F;
        currentDistance = baseDistance;
        targetDistance = baseDistance;
        currentFov = baseFov;
        targetFov = baseFov;
        clearImpulse();
    }

    static void orbit(double deltaX, double deltaY) {
        if (!active) return;
        manualAdjusted = true;
        targetYaw = Mth.wrapDegrees(targetYaw - (float)deltaX * HORIZONTAL_DEGREES_PER_PIXEL);
        targetPitch = Mth.clamp(targetPitch + (float)deltaY * VERTICAL_DEGREES_PER_PIXEL, MIN_PITCH, MAX_PITCH);
    }

    static void zoom(double scrollY) {
        if (!active || scrollY == 0.0D) return;
        manualAdjusted = true;
        targetDistance = Mth.clamp(targetDistance - (float)scrollY * WHEEL_DISTANCE_STEP, MIN_DISTANCE, MAX_DISTANCE);
    }

    static void resetView() {
        if (!active) return;
        manualAdjusted = false;
        targetYaw = baseYaw;
        targetPitch = basePitch;
        targetDistance = baseDistance;
        targetFov = baseFov;
    }

    static void onSnapshotTransition(ClientBattleState.Snapshot before, ClientBattleState.Snapshot after) {
        if (!active || before == null || after == null || !after.active()) return;
        refreshFraming(after);

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
                strongestRatio = Math.max(strongestRatio, lost / (float)Math.max(1, previous.maxHp()));
                knockDown |= !previous.downed() && current.downed();
                bossImpact |= current.defId().startsWith("B");
            }
            revived |= previous.downed() && !current.downed();
        }
        if (damagedTargets > 0) impact(strongestRatio, damagedTargets, knockDown, bossImpact);
        else if (revived) revivePulse();
    }

    private static void refreshFraming(ClientBattleState.Snapshot snapshot) {
        BattleCameraFraming.Plan plan = BattleCameraFraming.plan(snapshot);
        applyBase(plan, false);
        if (!manualAdjusted) {
            targetYaw = baseYaw;
            targetPitch = basePitch;
            targetDistance = baseDistance;
            targetFov = baseFov;
        }
    }

    private static void applyBase(BattleCameraFraming.Plan plan, boolean snap) {
        baseYaw = Mth.wrapDegrees(plan.yaw());
        basePitch = Mth.clamp(plan.pitch(), MIN_PITCH, MAX_PITCH);
        baseDistance = Mth.clamp(plan.distance(), MIN_DISTANCE, MAX_DISTANCE);
        baseFov = Mth.clamp(plan.fov(), 48.0F, 64.0F);
        targetYaw = baseYaw;
        targetPitch = basePitch;
        targetDistance = baseDistance;
        targetFov = baseFov;
        if (snap) {
            currentYaw = baseYaw;
            currentPitch = basePitch;
            currentDistance = baseDistance;
            currentFov = baseFov;
        }
    }

    private static ClientBattleState.Unit unit(ClientBattleState.Snapshot snapshot, String id) {
        for (ClientBattleState.Unit unit : snapshot.units()) if (unit.id().equals(id)) return unit;
        return null;
    }

    private static void impact(float damageRatio, int targets, boolean knockDown, boolean bossImpact) {
        float strength = 0.44F + Mth.clamp(damageRatio, 0.0F, 0.60F) * 2.7F;
        if (targets > 1) strength += Math.min(0.40F, (targets - 1) * 0.11F);
        if (knockDown) strength += 0.50F;
        if (bossImpact) strength += 0.22F;
        strength = Mth.clamp(strength, 0.44F, 2.45F);
        impactYaw = Math.max(impactYaw, strength * 0.22F);
        impactPitch = Math.max(impactPitch, strength * 0.15F);
        impactRoll = Math.max(impactRoll, strength * 0.08F);
        impactDistance = Math.max(impactDistance, 0.08F + strength * 0.065F);
        impactFov = Math.max(impactFov, 0.30F + strength * 0.22F);
        impactFrames = Math.max(impactFrames, knockDown || bossImpact ? 10 : 7);
    }

    private static void revivePulse() {
        impactDistance = Math.min(impactDistance, -0.15F);
        impactFov = Math.min(impactFov, -0.55F);
        impactRoll = Math.max(impactRoll, 0.06F);
        impactFrames = Math.max(impactFrames, 7);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        currentYaw = Mth.wrapDegrees(currentYaw + Mth.wrapDegrees(targetYaw - currentYaw) * VIEW_LERP);
        currentPitch += (targetPitch - currentPitch) * VIEW_LERP;
        currentDistance += (targetDistance - currentDistance) * ZOOM_LERP;
        currentFov += (targetFov - currentFov) * ZOOM_LERP;
        applyPlayerView(Minecraft.getInstance());

        float phase = impactPhase++ * 0.92F;
        float yawWave = (float)Math.sin(phase);
        float pitchWave = (float)Math.sin(phase * 0.73F + 1.10F);
        float rollWave = (float)Math.sin(phase * 0.61F + 0.35F);
        event.setYaw(currentYaw + impactYaw * yawWave);
        event.setPitch(currentPitch + impactPitch * pitchWave);
        event.setRoll(impactRoll * rollWave);
        decayImpulse();
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active) event.setFOV(currentFov - impactFov);
    }

    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (active && !event.isCameraFlipped()) {
            event.setDistance(Mth.clamp(currentDistance - impactDistance, MIN_DISTANCE, MAX_DISTANCE));
        }
    }

    private static void decayImpulse() {
        if (impactFrames <= 0) { clearImpulse(); return; }
        impactFrames--;
        impactYaw *= 0.64F;
        impactPitch *= 0.64F;
        impactRoll *= 0.62F;
        impactDistance *= 0.72F;
        impactFov *= 0.72F;
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

    static View view() { return new View(currentYaw, currentPitch, currentDistance, currentFov); }
    static boolean active() { return active; }
    static boolean manualAdjusted() { return manualAdjusted; }
}
