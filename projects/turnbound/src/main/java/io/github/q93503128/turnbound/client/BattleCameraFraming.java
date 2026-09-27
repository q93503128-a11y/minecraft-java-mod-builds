package io.github.q93503128.turnbound.client;

import java.util.List;

/**
 * Pure framing math for the battle camera.
 *
 * <p>The important value is the pivot: it is the midpoint between the ally and enemy rows, not the Minecraft
 * player shell. Orbit/zoom must rotate around this pivot so the battlefield stays readable from every angle.</p>
 */
final class BattleCameraFraming {
    static final float THREE_QUARTER_OFFSET = 0.0F;
    private static final float MIN_DISTANCE = 8.5F;
    private static final float MAX_DISTANCE = 22.0F;
    private static final float MIN_PITCH = 18.0F;
    private static final float MAX_PITCH = 38.0F;
    private static final float MIN_FOV = 52.0F;
    private static final float MAX_FOV = 64.0F;

    record Plan(
            double pivotX, double pivotY, double pivotZ,
            float axisYaw, float yaw, float pitch, float distance, float fov
    ) {}

    private record Center(double x, double y, double z, int count) {}

    private BattleCameraFraming() {}

    static Plan plan(ClientBattleState.Snapshot snapshot) {
        if (snapshot == null) return fallback(0.0F, 0.0D, 0.0D, 0.0D);
        List<ClientBattleState.Unit> units = snapshot.units() == null ? List.of() : snapshot.units();
        Center allies = center(units, "ALLY");
        Center enemies = center(units, "ENEMY");

        float axisYaw = snapshot.arenaYaw();
        if (allies.count() > 0 && enemies.count() > 0) {
            double dx = enemies.x() - allies.x();
            double dz = enemies.z() - allies.z();
            if (dx * dx + dz * dz > 0.25D) {
                axisYaw = normalize((float)Math.toDegrees(Math.atan2(-dx, dz)));
            }
        }

        double centerX;
        double centerY;
        double centerZ;
        if (allies.count() > 0 && enemies.count() > 0) {
            // Equal side weighting is deliberate. A 5-enemy wave must not pull the camera target away from the
            // visual gap between formations just because that side has more bodies.
            centerX = (allies.x() + enemies.x()) * 0.5D;
            centerY = (allies.y() + enemies.y()) * 0.5D;
            centerZ = (allies.z() + enemies.z()) * 0.5D;
        } else if (allies.count() > 0) {
            centerX = allies.x();
            centerY = allies.y();
            centerZ = allies.z();
        } else if (enemies.count() > 0) {
            centerX = enemies.x();
            centerY = enemies.y();
            centerZ = enemies.z();
        } else {
            return fallback(axisYaw, snapshot.arenaX(), snapshot.arenaY(), snapshot.arenaZ());
        }

        double radians = Math.toRadians(axisYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = -forwardZ;
        double rightZ = forwardX;
        double lateralRadius = 0.0D;
        double depthRadius = 0.0D;
        double minY = centerY;
        double maxY = centerY;
        int positioned = 0;

        for (ClientBattleState.Unit unit : units) {
            if (!finite(unit.x()) || !finite(unit.y()) || !finite(unit.z())) continue;
            double dx = unit.x() - centerX;
            double dz = unit.z() - centerZ;
            lateralRadius = Math.max(lateralRadius, Math.abs(dx * rightX + dz * rightZ));
            depthRadius = Math.max(depthRadius, Math.abs(dx * forwardX + dz * forwardZ));
            minY = Math.min(minY, unit.y());
            maxY = Math.max(maxY, unit.y());
            positioned++;
        }

        if (positioned == 0) return fallback(axisYaw, centerX, centerY, centerZ);

        float distance = clamp((float)(8.4D + lateralRadius * 0.78D + depthRadius * 0.25D),
                MIN_DISTANCE, MAX_DISTANCE);
        float heightSpread = (float)Math.max(0.0D, maxY - minY);
        float pitch = clamp(20.5F + (distance - MIN_DISTANCE) * 0.8F
                        + Math.min(6.0F, heightSpread * 1.2F),
                MIN_PITCH, MAX_PITCH);
        float fov = clamp(52.0F + Math.max(0.0F, (float)lateralRadius - 4.0F) * 1.5F,
                MIN_FOV, MAX_FOV);
        return new Plan(
                centerX, centerY, centerZ,
                axisYaw, normalize(axisYaw + THREE_QUARTER_OFFSET), pitch, distance, fov);
    }

    static Plan fallback(float arenaYaw) {
        return fallback(arenaYaw, 0.0D, 0.0D, 0.0D);
    }

    static Plan fallback(float arenaYaw, double pivotX, double pivotY, double pivotZ) {
        float axis = normalize(arenaYaw);
        return new Plan(pivotX, pivotY, pivotZ, axis, normalize(axis + THREE_QUARTER_OFFSET), 23.0F, 10.5F, 52.0F);
    }

    private static Center center(List<ClientBattleState.Unit> units, String side) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;
        for (ClientBattleState.Unit unit : units) {
            if (!side.equals(unit.side())) continue;
            if (!finite(unit.x()) || !finite(unit.y()) || !finite(unit.z())) continue;
            x += unit.x();
            y += unit.y();
            z += unit.z();
            count++;
        }
        return count == 0 ? new Center(0.0D, 0.0D, 0.0D, 0)
                : new Center(x / count, y / count, z / count, count);
    }

    private static boolean finite(double value) {
        return Double.isFinite(value);
    }

    private static float normalize(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
