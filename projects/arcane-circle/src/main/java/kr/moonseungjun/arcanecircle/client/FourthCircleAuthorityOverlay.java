package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.FourthCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

/** Alpha.71 exact-footprint overlay for maintained fourth-circle battlefield authority. */
final class FourthCircleAuthorityOverlay {
    private FourthCircleAuthorityOverlay() {}

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double range, double elapsedSeconds, double durationSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(560);
        if (spell == null || spell.circle() != 4 || !"ice_storm".equals(spell.id())) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double radius = FourthCircleSpellService.iceStormRadius(range);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 floor = target;
        double pulse = 1.0 - ((t % .50) / .50);

        m.circle(g, floor.add(0, .05, 0), radius, 68, .96F);
        m.circle(g, floor.add(0, .08, 0), radius * (.45 + .45 * pulse), 54, .42F);
        m.polygon(g, floor.add(0, .11, 0), radius * .72, 8, t * .18, .34F);

        int shafts = 12;
        for (int i = 0; i < shafts; i++) {
            double a = i * Math.PI * 2.0 / shafts + .23 * Math.sin(t * 1.8 + i * .7);
            double r = radius * (.28 + .58 * ((i % 4) / 3.0));
            Vec3 base = floor.add(g.point(a, r));
            double drop = (t * 9.0 + i * 1.7) % 11.0;
            Vec3 high = base.add(0.0, 11.5 - drop, 0.0);
            Vec3 low = base.add(0.0, Math.max(.25, 9.8 - drop), 0.0);
            m.line(high, low, i % 3 == 0 ? .72F : .38F, .88F, i % 3 == 0 ? .80F : .38F);
        }
        return m.build();
    }
}
