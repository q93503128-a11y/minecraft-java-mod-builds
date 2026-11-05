package io.github.q93503128.turnbound.client;

import java.util.List;

/** World-to-screen projection and forgiving body-capsule picking for mouse target selection. */
final class BattleTargetProjection {
    record ScreenPoint(double x, double y, double depth) {}

    private BattleTargetProjection() {}

    static int pick(
            List<ClientBattleState.Unit> units,
            String rule,
            String actorId,
            double arenaX,
            double arenaY,
            double arenaZ,
            BattleCameraController.View view,
            int width,
            int height,
            double mouseX,
            double mouseY
    ) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        double bestDepth = Double.MAX_VALUE;
        double radius = Math.max(22.0, Math.min(42.0, Math.min(width, height) * 0.085));

        for (int i = 0; i < units.size(); i++) {
            ClientBattleState.Unit unit = units.get(i);
            if (!BattleTargeting.validTarget(rule, unit, actorId)) continue;

            ScreenPoint feet = project(arenaX, arenaY, arenaZ, view, width, height,
                    unit.x(), unit.y() + 0.12, unit.z());
            ScreenPoint torso = project(arenaX, arenaY, arenaZ, view, width, height,
                    unit.x(), unit.y() + 1.00, unit.z());
            ScreenPoint head = project(arenaX, arenaY, arenaZ, view, width, height,
                    unit.x(), unit.y() + 1.85, unit.z());
            if (torso == null) continue;

            double distance;
            if (feet != null && head != null) {
                distance = distanceToSegment(mouseX, mouseY, feet.x(), feet.y(), head.x(), head.y());
            } else {
                double dx = mouseX - torso.x();
                double dy = mouseY - torso.y();
                distance = Math.sqrt(dx * dx + dy * dy);
            }

            double depth = torso.depth();
            if (distance <= radius && (distance < bestDistance - 0.001
                    || (Math.abs(distance - bestDistance) <= 0.001 && depth < bestDepth))) {
                best = i;
                bestDistance = distance;
                bestDepth = depth;
            }
        }
        return best;
    }

    static ScreenPoint project(
            double arenaX,
            double arenaY,
            double arenaZ,
            BattleCameraController.View view,
            int width,
            int height,
            double worldX,
            double worldY,
            double worldZ
    ) {
        if (width <= 0 || height <= 0) return null;
        double yaw = Math.toRadians(view.yaw());
        double pitch = Math.toRadians(view.pitch());
        double cosPitch = Math.cos(pitch);

        double fx = -Math.sin(yaw) * cosPitch;
        double fy = -Math.sin(pitch);
        double fz = Math.cos(yaw) * cosPitch;
        double rx = -Math.cos(yaw);
        double ry = 0.0;
        double rz = -Math.sin(yaw);
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;

        double pivotX = arenaX;
        double pivotY = arenaY + 1.62;
        double pivotZ = arenaZ;
        double cameraX = pivotX - fx * view.distance();
        double cameraY = pivotY - fy * view.distance();
        double cameraZ = pivotZ - fz * view.distance();

        double dx = worldX - cameraX;
        double dy = worldY - cameraY;
        double dz = worldZ - cameraZ;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth <= 0.05) return null;

        double horizontal = dx * rx + dy * ry + dz * rz;
        double vertical = dx * ux + dy * uy + dz * uz;
        double focal = height / (2.0 * Math.tan(Math.toRadians(view.fov()) * 0.5));
        return new ScreenPoint(
                width * 0.5 + horizontal / depth * focal,
                height * 0.5 - vertical / depth * focal,
                depth);
    }

    private static double distanceToSegment(
            double px, double py,
            double ax, double ay,
            double bx, double by
    ) {
        double vx = bx - ax;
        double vy = by - ay;
        double lengthSquared = vx * vx + vy * vy;
        if (lengthSquared <= 0.0001) {
            double dx = px - ax;
            double dy = py - ay;
            return Math.sqrt(dx * dx + dy * dy);
        }
        double t = ((px - ax) * vx + (py - ay) * vy) / lengthSquared;
        t = Math.max(0.0, Math.min(1.0, t));
        double cx = ax + vx * t;
        double cy = ay + vy * t;
        double dx = px - cx;
        double dy = py - cy;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
