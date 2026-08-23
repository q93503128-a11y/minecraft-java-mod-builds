package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SecondCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

/** Alpha.73 exact-footprint overlay for maintained second-circle battlefield authority. */
final class SecondCircleAuthorityOverlay {
    private SecondCircleAuthorityOverlay() {}

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double range, double elapsedSeconds, double durationSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(360);
        if (spell == null || spell.circle() != 2 || !"web".equals(spell.id())) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double radius = SecondCircleSpellService.webRadius(range);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 floor = target;
        double pulse = 1.0 - ((t % .20) / .20);
        m.circle(g, floor.add(0, .045, 0), radius, 56, .90F);
        m.circle(g, floor.add(0, .075, 0), radius * (.42 + .48 * pulse), 44, .32F);
        for (int ring = 1; ring <= 3; ring++) {
            double r = radius * ring / 4.0;
            m.circle(g, floor.add(0, .055 + ring * .006, 0), r, 32 + ring * 4, .24F);
        }
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0 / 12.0 + Math.sin(t * 1.1) * .025;
            Vec3 inner = floor.add(g.point(a, radius * .12)).add(0, .06, 0);
            Vec3 outer = floor.add(g.point(a, radius)).add(0, .06, 0);
            m.line(inner, outer, .30F, .86F, .28F);
        }
        return m.build();
    }
}
