package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import net.minecraft.world.phys.Vec3;

/** Draws the gameplay-sized boundary/body for range-reactive authored spells. */
final class RangeReactivePresentation {
    private RangeReactivePresentation() {}

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                              double effectiveRange, double age, double powerFactor,
                              ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "steam_burst" -> steamBurstEnvelope(spell, direction, effectiveRange, age, powerFactor, mesh);
            case "burning_hands", "thunderwave" -> waveEnvelope(spell, direction, effectiveRange, age, mesh);
            case "gust_of_wind" -> lineEnvelope(direction, effectiveRange, age, mesh);
            case "grease", "sleep", "web", "slow", "sleet_storm", "ice_storm" ->
                    fieldEnvelope(spell, targetOffset, effectiveRange, age, mesh);
            case "wind_wall", "wall_of_fire", "wall_of_ice" ->
                    wallEnvelope(spell, direction, targetOffset, effectiveRange, age, mesh);
            default -> { }
        }
    }

    private static void steamBurstEnvelope(SpellDefinition spell, Vec3 direction, double range,
                                           double age, double pf, ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        double open = ease(Math.min(1.0, age / 0.34));
        double fade = fade(age, 0.72);
        double length = SpellMetrics.waveLength(range) * open;
        double end = SpellMetrics.waveEndRadius(spell.id(), range, spell.circle()) * open * pf;
        if (length < 0.15 || end < 0.08) return;
        mesh.cone(Vec3.ZERO, direction, facing, length, end, 12, 4, 0.86F);
        for (int i = 2; i <= 5; i++) {
            double t = i / 5.0;
            Vec3 at = direction.scale(length * t);
            double r = end * t;
            mesh.brokenBand(facing, at, r * 0.72, r, 38, 5 + i % 2,
                    1.08F, (float) (0.24 * fade));
            if (i >= 3) mesh.orb(at.add(facing.point(age * 5.0 + i, r * 0.28)),
                    Math.max(0.12, r * 0.18), 14, 0.88F, (float) (0.14 * fade));
        }
        Vec3 front = direction.scale(length);
        mesh.brokenBand(facing, front, end * 0.80, end * 1.08, 48, 6,
                1.18F, (float) (0.34 * fade));
    }

    private static void waveEnvelope(SpellDefinition spell, Vec3 direction, double range,
                                     double age, ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        double p = ease(Math.min(1.0, age / 0.42));
        double length = SpellMetrics.waveLength(range) * p;
        double radius = SpellMetrics.waveEndRadius(spell.id(), range, spell.circle()) * p;
        double fade = fade(age, 0.70);
        for (int i = 1; i <= 4; i++) {
            double t = i / 4.0;
            mesh.brokenBand(facing, direction.scale(length * t), radius * t * 0.82,
                    radius * t, 34, 5, 0.96F, (float) (0.18 * fade));
        }
    }


    private static void lineEnvelope(Vec3 direction, double range, double age, ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        double p = ease(Math.min(1.0, age / 0.34));
        double fade = fade(age, 0.74);
        double length = SpellMetrics.waveLength(range) * p;
        double half = 2.1;
        for (int lane = -2; lane <= 2; lane++) {
            Vec3 off = facing.right().scale(lane * half / 2.0);
            mesh.line(off, off.add(direction.scale(length)), lane == 0 ? 1.04F : 0.64F);
        }
        mesh.brokenBand(facing, direction.scale(length), half * 0.78, half, 36, 5, 0.92F, (float) (0.20 * fade));
    }

    private static void fieldEnvelope(SpellDefinition spell, Vec3 targetOffset, double range,
                                      double age, ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double p = ease(Math.min(1.0, age / 0.35));
        double fade = fade(age, 0.72);
        double radius = SpellMetrics.effectRadius(spell.id(), range, spell.circle()) * p;
        Vec3 center = horizontalTarget(targetOffset);
        mesh.band(ground, center, radius * 0.90, radius, 64, 1.08F, (float) (0.24 * fade));
        mesh.brokenBand(ground, center, radius * 0.62, radius * 0.68, 52, 6,
                0.86F, (float) (0.16 * fade));
    }

    private static void wallEnvelope(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                     double range, double age, ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        Vec3 right = facing.right();
        Vec3 center = horizontalTarget(targetOffset);
        double p = ease(Math.min(1.0, age / 0.32));
        double fade = fade(age, 0.78);
        double width = SpellMetrics.wallWidth(spell.id(), range, spell.circle()) * p;
        double half = width * 0.5;
        int anchors = Math.max(4, Math.min(14, (int) Math.ceil(width / 2.4)));
        for (int i = 0; i <= anchors; i++) {
            double x = -half + width * i / anchors;
            Vec3 base = center.add(right.scale(x));
            mesh.line(base, base.add(0.0, 0.35 + spell.circle() * 0.18, 0.0), i % 2 == 0 ? 1.18F : 0.72F);
        }
        Vec3 a = center.add(right.scale(-half));
        Vec3 b = center.add(right.scale(half));
        mesh.line(a, b, 1.16F);
        mesh.line(a.add(0.0, 0.16, 0.0), b.add(0.0, 0.16, 0.0), (float) (0.86 * fade));
    }

    private static Vec3 horizontalTarget(Vec3 targetOffset) {
        return new Vec3(targetOffset.x, Math.max(-1.4, Math.min(1.4, targetOffset.y)), targetOffset.z);
    }

    private static double ease(double t) { return 1.0 - Math.pow(1.0 - clamp(t), 2.2); }
    private static double fade(double age, double start) { return 1.0 - clamp((age - start) / Math.max(0.05, 1.0 - start)); }
    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
