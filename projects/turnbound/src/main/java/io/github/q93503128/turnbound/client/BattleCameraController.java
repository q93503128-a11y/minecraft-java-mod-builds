package io.github.q93503128.turnbound.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * High three-quarter battle camera derived from the reference game's combat composition.
 * The default view must show both formations at once; manual orbit is a correction tool, not the primary way to read battle.
 */
public final class BattleCameraController {
    private static final float DEFAULT_DISTANCE = 10.4F;
    private static final float MIN_DISTANCE = 6.2F;
    private static final float MAX_DISTANCE = 16.0F;
    private static final float DEFAULT_PITCH = 29.0F;
    private static final float MIN_PITCH = 14.0F;
    private static final float MAX_PITCH = 53.0F;
    private static final float DEFAULT_YAW_OFFSET = 18.0F;
    private static final float HORIZONTAL_DEGREES_PER_PIXEL = 0.46F;
    private static final float VERTICAL_DEGREES_PER_PIXEL = 0.38F;
    private static final float WHEEL_DISTANCE_STEP = 1.0F;
    private static final float FOV = 56.0F;

    private static boolean active;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;
    private static float previousYaw;
    private static float previousPitch;
    private static float arenaYaw;
    private static float currentYaw;
    private static float currentPitch;
    private static float targetYaw;
    private static float targetPitch;
    private static float distance = DEFAULT_DISTANCE;
    private static float impactYaw;
    private static float impactPitch;
    private static float impactRoll;
    private static float impactDistance;
    private static float impactFov;
    private static int impactFrames;
    private static int impactPhase;

    private BattleCameraController() {}

    public record View(float yaw, float pitch, float distance, float fov) {}

    public static void enter(float authoredArenaYaw) {
        if (active) return;
        Minecraft minecraft = Minecraft.getInstance();
        previousCameraType = minecraft.options.getCameraType();
        if (minecraft.player != null) {
            previousYaw = minecraft.player.getYRot();
            previousPitch = minecraft.player.getXRot();
        }
        arenaYaw = authoredArenaYaw;
        currentYaw = Mth.wrapDegrees(arenaYaw + DEFAULT_YAW_OFFSET);
        targetYaw = currentYaw;
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
        targetYaw = Mth.wrapDegrees(targetYaw - (float)deltaX * HORIZONTAL_DEGREES_PER_PIXEL);
        targetPitch = Mth.clamp(targetPitch + (float)deltaY * VERTICAL_DEGREES_PER_PIXEL, MIN_PITCH, MAX_PITCH);
    }

    static void zoom(double scrollY) {
        if (!active || scrollY == 0.0D) return;
        distance = Mth.clamp(distance - (float)scrollY * WHEEL_DISTANCE_STEP, MIN_DISTANCE, MAX_DISTANCE);
    }

    static void resetView() {
        if (!active) return;
        targetYaw = Mth.wrapDegrees(arenaYaw + DEFAULT_YAW_OFFSET);
        targetPitch = DEFAULT_PITCH;
        distance = DEFAULT_DISTANCE;
    }

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
                strongestRatio = Math.max(strongestRatio, lost / (float)Math.max(1, previous.maxHp()));
                knockDown |= !previous.downed() && current.downed();
                bossImpact |= current.defId().startsWith("B");
            }
            revived |= previous.downed() && !current.downed();
        }
        if (damagedTargets > 0) impact(strongestRatio, damagedTargets, knockDown, bossImpact);
        else if (revived) revivePulse();
    }

    private static ClientBattleState.Unit unit(ClientBattleState.Snapshot snapshot, String id) {
        for (ClientBattleState.Unit unit : snapshot.units()) if (unit.id().equals(id)) return unit;
        return null;
    }

    private static void impact(float damageRatio, int targets, boolean knockDown, boolean bossImpact) {
        float strength = 0.48F + Mth.clamp(damageRatio, 0.0F, 0.60F) * 3.2F;
        if (targets > 1) strength += Math.min(0.50F, (targets - 1) * 0.14F);
        if (knockDown) strength += 0.62F;
        if (bossImpact) strength += 0.28F;
        strength = Mth.clamp(strength, 0.50F, 2.9F);
        impactYaw = Math.max(impactYaw, strength * 0.30F);
        impactPitch = Math.max(impactPitch, strength * 0.22F);
        impactRoll = Math.max(impactRoll, strength * 0.13F);
        impactDistance = Math.max(impactDistance, 0.10F + strength * 0.08F);
        impactFov = Math.max(impactFov, 0.38F + strength * 0.26F);
        impactFrames = Math.max(impactFrames, knockDown || bossImpact ? 12 : 8);
    }

    private static void revivePulse() {
        impactDistance = Math.min(impactDistance, -0.18F);
        impactFov = Math.min(impactFov, -0.70F);
        impactRoll = Math.max(impactRoll, 0.10F);
        impactFrames = Math.max(impactFrames, 8);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;
        currentYaw = Mth.wrapDegrees(currentYaw + Mth.wrapDegrees(targetYaw - currentYaw) * 0.72F);
        currentPitch += (targetPitch - currentPitch) * 0.72F;
        applyPlayerView(Minecraft.getInstance());
        float shakeSign = (impactPhase++ & 1) == 0 ? 1.0F : -1.0F;
        float verticalSign = impactFrames % 3 == 0 ? -0.55F : 1.0F;
        event.setYaw(currentYaw + impactYaw * shakeSign);
        event.setPitch(currentPitch + impactPitch * verticalSign);
        event.setRoll(impactRoll * shakeSign);
        decayImpulse();
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (active) event.setFOV(FOV - impactFov);
    }

    public static void onDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (active && !event.isCameraFlipped()) {
            event.setDistance(Mth.clamp(distance - impactDistance, MIN_DISTANCE, MAX_DISTANCE));
        }
    }

    private static void decayImpulse() {
        if (impactFrames <= 0) { clearImpulse(); return; }
        impactFrames--;
        impactYaw *= 0.68F;
        impactPitch *= 0.68F;
        impactRoll *= 0.66F;
        impactDistance *= 0.74F;
        impactFov *= 0.74F;
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

    static View view() { return new View(currentYaw, currentPitch, distance, FOV); }
    static boolean active() { return active; }
}
