package kr.moonseungjun.senbonzakura.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Compact client-only geometry for swords and hundreds of blade shards. */
final class BladeMesh {
    private final List<Face> faces;
    private final List<Edge> edges;

    BladeMesh(List<Face> faces, List<Edge> edges) {
        this.faces = List.copyOf(faces);
        this.edges = List.copyOf(edges);
    }

    int size() {
        return faces.size() * 2 + edges.size();
    }

    void submit(PoseStack stack, SubmitNodeCollector collector, float lineScale) {
        if (!faces.isEmpty()) {
            collector.submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose, out) -> {
                for (Face face : faces) {
                    vertex(out, pose, face.a(), face.argb());
                    vertex(out, pose, face.b(), face.argb());
                    vertex(out, pose, face.c(), face.argb());
                    vertex(out, pose, face.d(), face.argb());
                }
            });
        }
        if (!edges.isEmpty()) {
            collector.submitCustomGeometry(stack, RenderTypes.lines(), (pose, out) -> {
                for (Edge edge : edges) {
                    Vec3 delta = edge.end().subtract(edge.start());
                    if (delta.lengthSqr() < 1.0E-8) continue;
                    Vec3 normal = delta.normalize();
                    float width = Math.max(0.65F, edge.width() * lineScale);
                    out.addVertex(pose, (float) edge.start().x, (float) edge.start().y, (float) edge.start().z)
                            .setColor(edge.argb()).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                            .setLineWidth(width);
                    out.addVertex(pose, (float) edge.end().x, (float) edge.end().y, (float) edge.end().z)
                            .setColor(edge.argb()).setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                            .setLineWidth(width);
                }
            });
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 point, int argb) {
        out.addVertex(pose, (float) point.x, (float) point.y, (float) point.z).setColor(argb);
    }

    record Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {}
    record Edge(Vec3 start, Vec3 end, int argb, float width) {}

    static Builder builder(int budget) {
        return new Builder(budget);
    }

    static final class Builder {
        private final int budget;
        private final List<Face> faces = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();

        Builder(int budget) {
            this.budget = Math.max(32, budget);
        }

        boolean full() {
            return faces.size() * 2 + edges.size() >= budget;
        }

        BladeMesh build() {
            return new BladeMesh(faces, edges);
        }

        Builder quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
            if (full()) return this;
            faces.add(new Face(a, b, c, d, argb));
            if (!full()) faces.add(new Face(d, c, b, a, argb));
            return this;
        }

        Builder triangle(Vec3 a, Vec3 b, Vec3 c, int argb) {
            return quad(a, b, c, c, argb);
        }

        Builder edge(Vec3 a, Vec3 b, int argb, float width) {
            if (!full() && a.distanceToSqr(b) > 1.0E-8) edges.add(new Edge(a, b, argb, width));
            return this;
        }

        Builder glowEdge(Vec3 a, Vec3 b, int outer, int core, float width) {
            edge(a, b, outer, width * 3.4F);
            edge(a, b, core, width);
            return this;
        }

        Builder groundRing(Vec3 center, double radius, int segments, int outer, int core, float width) {
            int count = Math.max(18, segments);
            Vec3 previous = center.add(radius, 0.0, 0.0);
            for (int i = 1; i <= count && !full(); i++) {
                double angle = Math.PI * 2.0 * i / count;
                Vec3 current = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
                glowEdge(previous, current, outer, core, width);
                previous = current;
            }
            return this;
        }

        Builder longBlade(Vec3 base, Vec3 axis, Vec3 broadHint, double length, double width, double thickness,
                          int faceColor, int edgeColor) {
            Vec3 n = safe(axis, new Vec3(0.0, 1.0, 0.0));
            Frame frame = frame(n, broadHint);
            Vec3 shoulder = base.add(n.scale(length * 0.82));
            Vec3 tip = base.add(n.scale(length));
            Vec3 s = frame.side().scale(width * 0.5);
            Vec3 t = frame.thickness().scale(thickness * 0.5);

            Vec3 b1 = base.add(s).add(t), b2 = base.subtract(s).add(t);
            Vec3 b3 = base.subtract(s).subtract(t), b4 = base.add(s).subtract(t);
            Vec3 q1 = shoulder.add(s).add(t), q2 = shoulder.subtract(s).add(t);
            Vec3 q3 = shoulder.subtract(s).subtract(t), q4 = shoulder.add(s).subtract(t);
            Vec3 tipTop = tip.add(t.scale(0.18));
            Vec3 tipBottom = tip.subtract(t.scale(0.18));

            quad(b1, b2, q2, q1, faceColor);
            quad(b4, q4, q3, b3, faceColor);
            quad(b1, q1, q4, b4, faceColor);
            quad(b2, b3, q3, q2, faceColor);
            triangle(q1, q2, tipTop, faceColor);
            triangle(q4, tipBottom, q3, faceColor);
            quad(q1, tipTop, tipBottom, q4, faceColor);
            triangle(q2, q3, tipBottom, faceColor);

            edge(base.add(s), shoulder.add(s), edgeColor, 0.78F);
            edge(base.subtract(s), shoulder.subtract(s), edgeColor, 0.78F);
            edge(shoulder.add(s), tip, edgeColor, 0.88F);
            edge(shoulder.subtract(s), tip, edgeColor, 0.88F);
            return this;
        }

        Builder shard(Vec3 center, Vec3 axis, Vec3 broadHint, double length, double width, double thickness,
                      int faceColor, int edgeColor) {
            Vec3 n = safe(axis, new Vec3(0.0, 0.0, 1.0));
            Frame frame = frame(n, broadHint);
            Vec3 s = frame.side().scale(width * 0.5);
            Vec3 t = frame.thickness().scale(thickness * 0.5);
            Vec3 front = center.add(n.scale(length * 0.56));
            Vec3 back = center.subtract(n.scale(length * 0.44));
            Vec3 left = center.add(s);
            Vec3 right = center.subtract(s);
            Vec3 up = t;
            Vec3 down = t.scale(-1.0);

            quad(front.add(up), left.add(up), back.add(up), right.add(up), faceColor);
            quad(right.add(down), back.add(down), left.add(down), front.add(down), faceColor);
            quad(front.add(up), front.add(down), left.add(down), left.add(up), faceColor);
            quad(left.add(up), left.add(down), back.add(down), back.add(up), faceColor);
            quad(back.add(up), back.add(down), right.add(down), right.add(up), faceColor);
            quad(right.add(up), right.add(down), front.add(down), front.add(up), faceColor);
            if (((faceColor >>> 24) & 255) > 96) {
                edge(front, back, edgeColor, 0.56F);
            }
            return this;
        }

        Builder box(Vec3 center, Vec3 axis, Vec3 broadHint, double length, double width, double thickness,
                    int faceColor, int edgeColor) {
            Vec3 n = safe(axis, new Vec3(0.0, 1.0, 0.0));
            Frame frame = frame(n, broadHint);
            Vec3 a = n.scale(length * 0.5), s = frame.side().scale(width * 0.5), t = frame.thickness().scale(thickness * 0.5);
            Vec3[] p = {
                    center.add(a).add(s).add(t), center.add(a).subtract(s).add(t),
                    center.add(a).subtract(s).subtract(t), center.add(a).add(s).subtract(t),
                    center.subtract(a).add(s).add(t), center.subtract(a).subtract(s).add(t),
                    center.subtract(a).subtract(s).subtract(t), center.subtract(a).add(s).subtract(t)
            };
            quad(p[0], p[1], p[2], p[3], faceColor);
            quad(p[7], p[6], p[5], p[4], faceColor);
            quad(p[0], p[4], p[5], p[1], faceColor);
            quad(p[1], p[5], p[6], p[2], faceColor);
            quad(p[2], p[6], p[7], p[3], faceColor);
            quad(p[3], p[7], p[4], p[0], faceColor);
            edge(p[0], p[4], edgeColor, 0.72F);
            edge(p[1], p[5], edgeColor, 0.72F);
            edge(p[2], p[6], edgeColor, 0.72F);
            edge(p[3], p[7], edgeColor, 0.72F);
            return this;
        }

        Builder katana(Vec3 guard, Vec3 bladeDirection, Vec3 broadHint, double scale, int alpha) {
            Vec3 n = safe(bladeDirection, new Vec3(0.0, -1.0, 0.0));
            Vec3 side = frame(n, broadHint).side();
            int silver = argb(alpha, 238, 238, 246);
            int silverEdge = argb(alpha, 255, 204, 231);
            int dark = argb(alpha, 34, 25, 39);
            int darkEdge = argb(alpha, 98, 65, 88);
            int guardColor = argb(alpha, 145, 105, 118);
            longBlade(guard.add(n.scale(0.08 * scale)), n, side, 3.9 * scale, 0.34 * scale,
                    0.10 * scale, silver, silverEdge);
            box(guard.subtract(n.scale(0.82 * scale)), n, side, 1.55 * scale, 0.30 * scale,
                    0.24 * scale, dark, darkEdge);
            box(guard, side, n, 0.98 * scale, 0.12 * scale, 0.26 * scale, guardColor, silverEdge);
            return this;
        }

        private static Frame frame(Vec3 axis, Vec3 broadHint) {
            Vec3 projected = broadHint.subtract(axis.scale(broadHint.dot(axis)));
            if (projected.lengthSqr() < 1.0E-7) {
                Vec3 helper = Math.abs(axis.y) < 0.82 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
                projected = helper.subtract(axis.scale(helper.dot(axis)));
            }
            Vec3 side = projected.normalize();
            Vec3 thickness = axis.cross(side);
            if (thickness.lengthSqr() < 1.0E-7) thickness = new Vec3(0.0, 0.0, 1.0);
            return new Frame(side, thickness.normalize());
        }

        private static Vec3 safe(Vec3 value, Vec3 fallback) {
            return value.lengthSqr() < 1.0E-8 ? fallback : value.normalize();
        }
    }

    private record Frame(Vec3 side, Vec3 thickness) {}

    static int argb(int alpha, int red, int green, int blue) {
        return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    static int withAlpha(int rgb, double alpha) {
        int a = clamp((int) Math.round(clamp01(alpha) * 255.0));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
