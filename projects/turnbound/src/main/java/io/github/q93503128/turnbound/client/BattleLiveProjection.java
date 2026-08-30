package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Uses Minecraft's actual post-collision camera projection so click hitboxes match rendered actors. */
final class BattleLiveProjection {
    record ScreenPoint(double x, double y, double depth) {}

    private BattleLiveProjection() {}

    static ScreenPoint project(double worldX, double worldY, double worldZ, int width, int height) {
        if (width <= 0 || height <= 0) return null;
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 ndc = minecraft.gameRenderer.projectPointToScreen(new Vec3(worldX, worldY, worldZ));
        if (!Double.isFinite(ndc.x) || !Double.isFinite(ndc.y) || !Double.isFinite(ndc.z)) return null;
        // transformProject returns normalized device coordinates. +1 is behind the camera in current renderer convention.
        if (ndc.z < -1.25 || ndc.z > 1.25) return null;
        return new ScreenPoint(
                (ndc.x + 1.0) * 0.5 * width,
                (1.0 - ndc.y) * 0.5 * height,
                ndc.z);
    }

    static int pick(List<ClientBattleState.Unit> units, String rule, String actorId,
                    int width, int height, double mouseX, double mouseY) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        double radius = Math.max(18.0, Math.min(34.0, Math.min(width, height) * 0.065));
        for (int i = 0; i < units.size(); i++) {
            ClientBattleState.Unit unit = units.get(i);
            if (!BattleTargeting.validTarget(rule, unit, actorId)) continue;
            ScreenPoint feet = project(unit.x(), unit.y() + 0.10, unit.z(), width, height);
            ScreenPoint torso = project(unit.x(), unit.y() + 1.02, unit.z(), width, height);
            ScreenPoint head = project(unit.x(), unit.y() + 1.92, unit.z(), width, height);
            if (torso == null) continue;
            double distance = feet != null && head != null
                    ? distanceToSegment(mouseX, mouseY, feet.x(), feet.y(), head.x(), head.y())
                    : Math.hypot(mouseX - torso.x(), mouseY - torso.y());
            if (distance <= radius && distance < bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double vx = bx - ax;
        double vy = by - ay;
        double lengthSquared = vx * vx + vy * vy;
        if (lengthSquared <= 0.0001) return Math.hypot(px - ax, py - ay);
        double t = ((px - ax) * vx + (py - ay) * vy) / lengthSquared;
        t = Math.max(0.0, Math.min(1.0, t));
        double cx = ax + vx * t;
        double cy = ay + vy * t;
        return Math.hypot(px - cx, py - cy);
    }
}
