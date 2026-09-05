package io.github.q93503128.turnbound.presentation;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Canon-authored elite telegraphs that must precede their impact language. */
public final class EliteVfxTelegraphs {
    private EliteVfxTelegraphs() {}

    /** EL04 canon: exactly three ground crack lines appear before Collapse's AoE impact burst. */
    public static void el04CollapseCracks(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return;
        double[] angles = {-1.42, 0.63, 2.76};
        double[] lengths = {1.95, 1.72, 2.08};
        Vec3 start = center.add(0, 0.08, 0);
        for (int crack = 0; crack < 3; crack++) {
            double angle = angles[crack];
            Vec3 end = start.add(Math.cos(angle) * lengths[crack], 0, Math.sin(angle) * lengths[crack]);
            crackLine(level, start, end, crack);
        }
    }

    private static void crackLine(ServerLevel level, Vec3 from, Vec3 to, int variant) {
        Vec3 delta = to.subtract(from);
        int steps = 9;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double sideways = Math.sin((i + variant * 2.0) * 1.7) * 0.08 * (0.35 + t);
            Vec3 normal = new Vec3(-delta.z, 0, delta.x).normalize().scale(sideways);
            Vec3 p = from.add(delta.scale(t)).add(normal);
            PersonalPresentationIsolation.particles(level, ParticleTypes.FLAME, p.x, p.y, p.z,
                    1, 0.015, 0.01, 0.015, 0.0);
            if ((i + variant) % 3 == 0) {
                PersonalPresentationIsolation.particles(level, ParticleTypes.SMOKE, p.x, p.y + 0.02, p.z,
                        1, 0.025, 0.01, 0.025, 0.0);
            }
        }
    }
}
