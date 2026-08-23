package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.FirstCircleSpellService;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

/** Alpha.74 exact-footprint overlay for first-circle ground authority. */
final class FirstCircleAuthorityOverlay {
    private FirstCircleAuthorityOverlay() {}

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double range, double elapsedSeconds, double durationSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(320);
        if (spell == null || spell.circle() != 1) return m.build();
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double t = Math.max(0.0, elapsedSeconds);
        if ("grease".equals(spell.id())) {
            double radius = FirstCircleSpellService.greaseRadius(range);
            Vec3 floor = target.add(0, .045, 0);
            double slide = (t * .85) % 1.0;
            m.circle(g, floor, radius, 52, .82F);
            m.circle(g, floor.add(0, .018, 0), radius * (.32 + .50 * slide), 40, .24F);
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2.0 / 10.0 + t * .16;
                Vec3 a0 = floor.add(g.point(a, radius * .28));
                Vec3 a1 = floor.add(g.point(a + .30, radius * .78));
                m.line(a0, a1, .26F, .72F, .24F);
            }
            return m.build();
        }
        if ("sleep".equals(spell.id())) {
            double radius = FirstCircleSpellService.sleepRadius(range);
            Vec3 floor = target.add(0, .055, 0);
            double pulse = .78 + .10 * Math.sin(t * 2.0);
            m.circle(g, floor, radius, 52, .74F);
            m.circle(g, floor.add(0, .02, 0), radius * pulse, 44, .22F);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI * 2.0 / 6.0 + t * .08;
                Vec3 p = floor.add(g.point(a, radius * .62)).add(0, .10 + .05 * Math.sin(t * 1.7 + i), 0);
                m.diamond(g, p, Math.max(.18, radius * .055), -t * .10 + i, .56F, .28F);
            }
        }
        return m.build();
    }
}
