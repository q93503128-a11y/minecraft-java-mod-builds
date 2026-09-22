package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Pure horizontal geometry authority for canon-closed R01 Earthloong physical action starts.
 *
 * <p>Angles use 0 degrees directly in front, 90 at either side and 180 directly behind.
 * Claw/Tail intentionally overlap on the flank so the weighted action controller can choose
 * between them instead of creating a hard seam. Quarry Rush locks toward the target after a
 * 5-9 block clear-line validation, so its start legality is distance/line based rather than
 * a pre-existing facing cone.</p>
 */
public final class R01EarthloongSpatialAuthority {
    private R01EarthloongSpatialAuthority() {
    }

    public static SpatialSnapshot snapshot(
            LivingEntity actor,
            LivingEntity target,
            boolean clearLine
    ) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(target, "target");

        Vec3 look = actor.getLookAngle();
        return evaluate(
                actor.getX(),
                actor.getZ(),
                look.x,
                look.z,
                target.getX(),
                target.getZ(),
                clearLine
        );
    }

    public static SpatialSnapshot evaluate(
            double actorX,
            double actorZ,
            double facingX,
            double facingZ,
            double targetX,
            double targetZ,
            boolean clearLine
    ) {
        requireFinite(actorX, actorZ, facingX, facingZ, targetX, targetZ);

        double dx = targetX - actorX;
        double dz = targetZ - actorZ;
        double distance = Math.hypot(dx, dz);
        if (distance <= 1.0e-9) {
            return new SpatialSnapshot(0.0, 0.0, clearLine);
        }

        double facingLength = Math.hypot(facingX, facingZ);
        if (facingLength <= 1.0e-9) {
            throw new IllegalArgumentException("Horizontal facing vector must be non-zero.");
        }

        double dot = (facingX * dx + facingZ * dz) / (facingLength * distance);
        double clampedDot = Math.max(-1.0, Math.min(1.0, dot));
        double absoluteAngleDegrees = Math.toDegrees(Math.acos(clampedDot));
        return new SpatialSnapshot(distance, absoluteAngleDegrees, clearLine);
    }

    public static boolean isLegal(
            R01EarthloongEncounterData.PhysicalBindingRule binding,
            SpatialSnapshot snapshot
    ) {
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(snapshot, "snapshot");

        if (snapshot.horizontalDistance() < binding.minimumRange()
                || snapshot.horizontalDistance() > binding.maximumRange()) {
            return false;
        }
        if (snapshot.absoluteAngleDegrees() < binding.minimumAbsoluteAngleDegrees()
                || snapshot.absoluteAngleDegrees() > binding.maximumAbsoluteAngleDegrees()) {
            return false;
        }
        return !binding.requiresClearLine() || snapshot.clearLine();
    }

    public record SpatialSnapshot(
            double horizontalDistance,
            double absoluteAngleDegrees,
            boolean clearLine
    ) {
        public SpatialSnapshot {
            if (!Double.isFinite(horizontalDistance)
                    || !Double.isFinite(absoluteAngleDegrees)
                    || horizontalDistance < 0.0
                    || absoluteAngleDegrees < 0.0
                    || absoluteAngleDegrees > 180.0) {
                throw new IllegalArgumentException("Invalid Earthloong spatial snapshot.");
            }
        }
    }

    private static void requireFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Earthloong geometry must be finite.");
            }
        }
    }
}
