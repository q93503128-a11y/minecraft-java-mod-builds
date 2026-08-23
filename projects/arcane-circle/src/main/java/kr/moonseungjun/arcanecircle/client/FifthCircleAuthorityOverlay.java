package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.FifthCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

/** Alpha.70 exact-footprint overlay for fifth-circle effects whose maintained geometry matters. */
final class FifthCircleAuthorityOverlay {
    private FifthCircleAuthorityOverlay() {}

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double range, double elapsedSeconds, double durationSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(520);
        if (spell == null || spell.circle() != 5 || !"flame_strike".equals(spell.id())) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double radius = FifthCircleSpellService.flameStrikeRadius(range);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 floor = target;
        Vec3 top = floor.add(0.0, 13.5, 0.0);
        double ignite = smooth(clamp(t / .28, 0.0, 1.0));
        double pulsePhase = (t % .50) / .50;
        double pulse = 1.0 - pulsePhase;

        m.circle(g, floor.add(0, .04, 0), radius, 72, 1.06F);
        m.circle(g, floor.add(0, .07, 0), radius * (.32 + .60 * ignite), 54, .54F);
        m.circle(g, floor.add(0, .10, 0), radius * (.30 + .70 * pulse), 58, .46F);

        int columns = 8;
        for (int i = 0; i < columns; i++) {
            double a = i * Math.PI * 2.0 / columns;
            Vec3 low = floor.add(g.point(a, radius * .72));
            Vec3 high = top.add(g.point(a + .12 * ((i & 1) == 0 ? 1 : -1), radius * .34));
            m.line(low, high, i % 2 == 0 ? .78F : .34F, 1.0F, i % 2 == 0 ? .72F : .30F);
        }
        m.circle(g, top, radius * .52, 54, .62F);
        m.polygon(g, top, radius * .36, 5, t * .10, .50F);
        return m.build();
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double smooth(double v) { double x = clamp(v, 0.0, 1.0); return x * x * (3.0 - 2.0 * x); }
}
