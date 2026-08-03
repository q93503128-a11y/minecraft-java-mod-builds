package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight continuous line mesh for world-space magic.
 *
 * <p>The old renderer submitted hundreds of tiny VoxelShape boxes. That produced a dotted,
 * pixel-block outline and was expensive to merge and draw. This mesh submits connected line
 * vertices in one node per pass, preserving smooth curves without returning to particles.</p>
 */
final class ArcaneWorldMesh {
    private final List<Segment> segments;

    ArcaneWorldMesh(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    int size() {
        return segments.size();
    }

    void submit(PoseStack poseStack, SubmitNodeCollector collector, int argb, float windowScale) {
        if (segments.isEmpty()) return;
        int edge = shade(withAlpha(argb, Math.max(120, (argb >>> 24) & 0xFF)), 0.24);
        int aura = withAlpha(argb, Math.max(42, ((argb >>> 24) & 0xFF) / 3));
        submitPass(poseStack, collector, edge, windowScale * 3.55F);
        submitPass(poseStack, collector, aura, windowScale * 2.55F);
        submitPass(poseStack, collector, argb, windowScale * 1.42F);
    }

    private void submitPass(PoseStack poseStack, SubmitNodeCollector collector, int argb, float scale) {
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (PoseStack.Pose pose, VertexConsumer consumer) -> {
            for (Segment segment : segments) {
                Vec3 delta = segment.end.subtract(segment.start);
                if (delta.lengthSqr() < 0.0000001) continue;
                Vec3 normal = delta.normalize();
                float width = Math.max(0.88F, segment.width * scale);
                consumer.addVertex(pose, (float) segment.start.x, (float) segment.start.y, (float) segment.start.z)
                        .setColor(argb)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                        .setLineWidth(width);
                consumer.addVertex(pose, (float) segment.end.x, (float) segment.end.y, (float) segment.end.z)
                        .setColor(argb)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                        .setLineWidth(width);
            }
        });
    }

    private static int shade(int argb, double factor) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (int) Math.round(((argb >>> 16) & 0xFF) * factor);
        int green = (int) Math.round(((argb >>> 8) & 0xFF) * factor);
        int blue = (int) Math.round((argb & 0xFF) * factor);
        return (alpha << 24) | (Math.max(0, Math.min(255, red)) << 16)
                | (Math.max(0, Math.min(255, green)) << 8)
                | Math.max(0, Math.min(255, blue));
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    static Builder builder(int budget) {
        return new Builder(budget);
    }

    record Segment(Vec3 start, Vec3 end, float width) {}

    static final class Builder {
        private final int budget;
        private final List<Segment> segments;

        Builder(int budget) {
            this.budget = Math.max(1, budget);
            this.segments = new ArrayList<>(Math.min(this.budget, 512));
        }

        int size() {
            return segments.size();
        }

        boolean full() {
            return segments.size() >= budget;
        }

        ArcaneWorldMesh build() {
            return new ArcaneWorldMesh(segments);
        }

        Builder line(Vec3 start, Vec3 end, float width) {
            if (!full() && start != null && end != null && start.distanceToSqr(end) > 0.0000001) {
                segments.add(new Segment(start, end, width));
            }
            return this;
        }

        Builder polyline(List<Vec3> points, float width, boolean closed) {
            if (points == null || points.size() < 2) return this;
            for (int i = 1; i < points.size() && !full(); i++) line(points.get(i - 1), points.get(i), width);
            if (closed && !full()) line(points.getLast(), points.getFirst(), width);
            return this;
        }

        Builder arc(Basis basis, Vec3 center, double radius, double start, double sweep,
                    int segments, float width) {
            int count = Math.max(2, segments);
            Vec3 previous = center.add(basis.point(start, radius));
            for (int i = 1; i <= count && !full(); i++) {
                double angle = start + sweep * i / count;
                Vec3 current = center.add(basis.point(angle, radius));
                line(previous, current, width);
                previous = current;
            }
            return this;
        }

        Builder circle(Basis basis, Vec3 center, double radius, int segments, float width) {
            return arc(basis, center, radius, -Math.PI / 2.0, Math.PI * 2.0, segments, width);
        }

        Builder polygon(Basis basis, Vec3 center, double radius, int sides,
                        double rotation, float width) {
            int count = Math.max(3, sides);
            List<Vec3> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                points.add(center.add(basis.point(rotation + Math.PI * 2.0 * i / count, radius)));
            }
            return polyline(points, width, true);
        }

        Builder star(Basis basis, Vec3 center, double outer, double inner, int points,
                     double rotation, float width) {
            int count = Math.max(3, points);
            List<Vec3> vertices = new ArrayList<>(count * 2);
            for (int i = 0; i < count * 2; i++) {
                double radius = (i & 1) == 0 ? outer : inner;
                vertices.add(center.add(basis.point(rotation + Math.PI * i / count, radius)));
            }
            return polyline(vertices, width, true);
        }

        Builder runeChords(Basis basis, Vec3 center, double radius, int count,
                           int skip, double rotation, float width) {
            int safeCount = Math.max(3, count);
            int safeSkip = Math.max(1, Math.min(safeCount - 1, skip));
            List<Vec3> points = new ArrayList<>(safeCount);
            for (int i = 0; i < safeCount; i++) {
                points.add(center.add(basis.point(rotation + Math.PI * 2.0 * i / safeCount, radius)));
            }
            for (int i = 0; i < safeCount && !full(); i++) {
                line(points.get(i), points.get((i + safeSkip) % safeCount), width);
            }
            return this;
        }

        Builder sphere(Vec3 center, double radius, int detail, float width) {
            int segments = Math.max(20, 24 + detail * 4);
            circle(Basis.ground(), center, radius, segments, width);
            circle(new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0)), center, radius, segments, width);
            circle(new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0)), center, radius, segments, width);
            int latitudes = Math.min(5, Math.max(0, detail / 2));
            for (int i = 1; i <= latitudes && !full(); i++) {
                double y = radius * (-0.72 + i * 1.44 / (latitudes + 1));
                double r = Math.sqrt(Math.max(0.0, radius * radius - y * y));
                circle(Basis.ground(), center.add(0, y, 0), r, Math.max(18, segments - 8), width * 0.85F);
            }
            return this;
        }

        Builder helix(Vec3 origin, Vec3 axis, Basis basis, double length, double radius,
                      int turns, int segments, float width, boolean taper) {
            Vec3 safeAxis = axis.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : axis.normalize();
            int count = Math.max(8, segments);
            Vec3 previous = origin.add(basis.point(0.0, taper ? 0.0 : radius));
            for (int i = 1; i <= count && !full(); i++) {
                double t = i / (double) count;
                double localRadius = taper ? radius * Math.sin(Math.PI * t) : radius;
                double angle = Math.PI * 2.0 * turns * t;
                Vec3 current = origin.add(safeAxis.scale(length * t)).add(basis.point(angle, localRadius));
                line(previous, current, width);
                previous = current;
            }
            return this;
        }

        Builder cone(Vec3 origin, Vec3 axis, Basis basis, double length, double endRadius,
                     int ribs, int rings, float width) {
            Vec3 safeAxis = axis.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : axis.normalize();
            int safeRibs = Math.max(3, ribs);
            for (int i = 0; i < safeRibs && !full(); i++) {
                double angle = Math.PI * 2.0 * i / safeRibs;
                line(origin, origin.add(safeAxis.scale(length)).add(basis.point(angle, endRadius)), width);
            }
            for (int ring = 1; ring <= Math.max(1, rings) && !full(); ring++) {
                double t = ring / (double) Math.max(1, rings);
                circle(basis, origin.add(safeAxis.scale(length * t)), endRadius * t,
                        Math.max(18, safeRibs * 4), width * 0.86F);
            }
            return this;
        }
    }

    record Basis(Vec3 right, Vec3 up) {
        Basis {
            right = right.lengthSqr() < 0.00001 ? new Vec3(1, 0, 0) : right.normalize();
            up = up.lengthSqr() < 0.00001 ? new Vec3(0, 1, 0) : up.normalize();
        }

        static Basis ground() {
            return new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1));
        }

        static Basis facing(Vec3 normal) {
            Vec3 safe = normal.lengthSqr() < 0.00001 ? new Vec3(0, 0, 1) : normal.normalize();
            Vec3 reference = Math.abs(safe.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = safe.cross(reference).normalize();
            return new Basis(right, right.cross(safe).normalize());
        }

        static Basis fromNormal(Vec3 normal, Vec3 hint) {
            Vec3 safe = normal.lengthSqr() < 0.00001 ? new Vec3(0, 1, 0) : normal.normalize();
            Vec3 reference = hint.lengthSqr() < 0.00001 ? new Vec3(1, 0, 0) : hint.normalize();
            Vec3 right = safe.cross(reference);
            if (right.lengthSqr() < 0.00001) right = safe.cross(new Vec3(0, 0, 1));
            right = right.normalize();
            return new Basis(right, right.cross(safe).normalize());
        }

        Vec3 point(double angle, double radius) {
            return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
        }

        Vec3 normal() {
            Vec3 normal = right.cross(up);
            return normal.lengthSqr() < 0.00001 ? new Vec3(0, 1, 0) : normal.normalize();
        }
    }
}
