package io.github.q93503128.turnbound.client;

import java.util.List;

/** Lightweight world-to-screen projection used for mouse target picking while the battle Screen owns the cursor. */
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
        double radius = Math.max(18.0, Math.min(34.0, Math.min(width, height) * 0.075));

        for (int i = 0; i < units.size(); i++) {
            ClientBattleState.Unit unit = units.get(i);
            if (!BattleTargeting.validTarget(rule, unit, actorId)) continue;
            ScreenPoint point = project(arenaX, arenaY, arenaZ, view, width, height,
                    unit.x(), unit.y() + 1.15, unit.z());
            if (point == null) continue;
            double dx = mouseX - point.x();
            double dy = mouseY - point.y();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= radius && (distance < bestDistance || (distance == bestDistance && point.depth() < bestDepth))) {
                best = i;
                bestDistance = distance;
                bestDepth = point.depth();
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
}