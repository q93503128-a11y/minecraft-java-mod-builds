package kr.moonseungjun.turnboundre.client.camera.external;

import net.minecraft.util.Mth;

/**
 * Battle-camera rotation smoothing adapted from Cukkoo12/free-camera's
 * {@code CinematicRotationSmoother} and CINEMATIC profile.
 *
 * <p>Upstream: https://github.com/Cukkoo12/free-camera<br>
 * commit: 9dc299c70e19cfbd297a65912ea3e70809548b9d<br>
 * source: neoforge-26.2-3.0/src/main/java/com/cukkoo/freecamera/cinematic/CinematicRotationSmoother.java<br>
 * profile: neoforge-26.2-3.0/src/main/java/com/cukkoo/freecamera/cinematic/CinematicMotionProfile.java<br>
 * license: MIT, Copyright (c) 2025 Cukkoo. See third_party/licenses/Cukkoo12_free-camera_MIT.txt.</p>
 *
 * <p>TURNBOUND's adaptation changes the input boundary: instead of reading free-camera input deltas,
 * the normal Minecraft camera angle supplied by NeoForge becomes the target each frame. The upstream
 * critically damped exponential integration and CINEMATIC rotation frequency (7.0) are retained.</p>
 */
public final class FreeCameraRotationSmoother {
    private static final double ANGLE_EPSILON = 1.0E-5;
    private static final double VELOCITY_EPSILON = 1.0E-5;
    private static final double CINEMATIC_ROTATION_FREQUENCY = 7.0;

    private double outputYaw;
    private double outputPitch;
    private double targetYaw;
    private double targetPitch;
    private double yawVelocity;
    private double pitchVelocity;
    private boolean initialized;
    private long lastFrameNanos;

    public void setTarget(float yaw, float pitch) {
        if (!initialized) {
            initialize(yaw, pitch);
            return;
        }
        targetYaw = Mth.wrapDegrees((double) yaw);
        targetPitch = Math.clamp((double) pitch, -90.0, 90.0);
    }

    public boolean advance(double elapsedSeconds) {
        if (!initialized || elapsedSeconds == 0.0) return false;
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            initialize((float) targetYaw, (float) targetPitch);
            return false;
        }
        integrateAxis(CINEMATIC_ROTATION_FREQUENCY, elapsedSeconds);
        return true;
    }

    public float outputYaw() {
        return (float) Mth.wrapDegrees(outputYaw);
    }

    public float outputPitch() {
        return (float) Math.clamp(outputPitch, -90.0, 90.0);
    }

    public double elapsedSeconds(long nowNanos) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = nowNanos;
            return 0.0;
        }
        double elapsed = (nowNanos - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = nowNanos;
        return elapsed;
    }

    public void clear() {
        outputYaw = 0.0;
        outputPitch = 0.0;
        targetYaw = 0.0;
        targetPitch = 0.0;
        yawVelocity = 0.0;
        pitchVelocity = 0.0;
        initialized = false;
        lastFrameNanos = 0L;
    }

    private void initialize(float yaw, float pitch) {
        outputYaw = Mth.wrapDegrees((double) yaw);
        outputPitch = Math.clamp((double) pitch, -90.0, 90.0);
        targetYaw = outputYaw;
        targetPitch = outputPitch;
        yawVelocity = 0.0;
        pitchVelocity = 0.0;
        initialized = true;
        lastFrameNanos = 0L;
    }

    private void integrateAxis(double frequency, double elapsedSeconds) {
        double yawError = shortestDelta(targetYaw, outputYaw);
        double yawCoefficient = yawVelocity + frequency * yawError;
        double decay = Math.exp(-frequency * elapsedSeconds);
        double nextYawError = (yawError + yawCoefficient * elapsedSeconds) * decay;
        yawVelocity = (yawVelocity - yawCoefficient * frequency * elapsedSeconds) * decay;
        outputYaw = Mth.wrapDegrees(targetYaw + nextYawError);

        double pitchError = outputPitch - targetPitch;
        double pitchCoefficient = pitchVelocity + frequency * pitchError;
        double nextPitchError = (pitchError + pitchCoefficient * elapsedSeconds) * decay;
        pitchVelocity = (pitchVelocity - pitchCoefficient * frequency * elapsedSeconds) * decay;
        outputPitch = Math.clamp(targetPitch + nextPitchError, -90.0, 90.0);
        settleIfClose();
    }

    private void settleIfClose() {
        if (Math.abs(shortestDelta(outputYaw, targetYaw)) <= ANGLE_EPSILON
                && Math.abs(yawVelocity) <= VELOCITY_EPSILON) {
            outputYaw = targetYaw;
            yawVelocity = 0.0;
        }
        if (Math.abs(outputPitch - targetPitch) <= ANGLE_EPSILON
                && Math.abs(pitchVelocity) <= VELOCITY_EPSILON) {
            outputPitch = targetPitch;
            pitchVelocity = 0.0;
        }
    }

    private static double shortestDelta(double from, double to) {
        return Mth.wrapDegrees(to - from);
    }

    private static boolean isAcceptedElapsedTime(double elapsedSeconds) {
        return Double.isFinite(elapsedSeconds) && elapsedSeconds > 0.0 && elapsedSeconds <= 0.25;
    }
}
