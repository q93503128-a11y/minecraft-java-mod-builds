package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.ThirdCircleSpellService;
import net.minecraft.world.phys.Vec3;

/** Alpha.72 exact-footprint overlay for maintained third-circle battlefield zones. */
final class ThirdCircleAuthorityOverlay {
    private ThirdCircleAuthorityOverlay() {}

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double range, double elapsedSeconds, double durationSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(420);
        if (spell == null || spell.circle() != 3) return m.build();
        boolean slow = "slow".equals(spell.id());
        boolean sleet = "sleet_storm".equals(spell.id());
        if (!slow && !sleet) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double radius = slow ? ThirdCircleSpellService.slowRadius(range)
                : ThirdCircleSpellService.sleetStormRadius(range);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 floor = target;
        double pulsePeriod = slow ? .20 : .50;
        double pulse = 1.0 - ((t % pulsePeriod) / pulsePeriod);
        m.circle(g, floor.add(0, .045, 0), radius, 62, .88F);
        m.circle(g, floor.add(0, .075, 0), radius * (.40 + .50 * pulse), 48, .38F);
        if (slow) {
            m.polygon(g, floor.add(0, .10, 0), radius * .70, 6, -t * .12, .34F);
            m.polygon(g, floor.add(0, .13, 0), radius * .42, 6, t * .18, .28F);
        } else {
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2.0 / 10.0 + t * .21;
                Vec3 p = floor.add(g.point(a, radius * (.28 + .055 * (i % 5))));
                m.line(p.add(0, 3.8 + (i % 3), 0), p.add(0, .25, 0), .30F, .82F, .34F);
            }
        }
        return m.build();
    }
}
