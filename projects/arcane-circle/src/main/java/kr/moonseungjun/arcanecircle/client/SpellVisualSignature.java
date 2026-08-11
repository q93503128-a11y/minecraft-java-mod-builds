package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Deterministic per-spell visual handwriting. The motion family says what a spell does;
 * this layer prevents two formulae in that family from sharing the same finished sigil.
 */
final class SpellVisualSignature {
    private static final int[] PRISM = {
            0xFFFF2438, 0xFFFF8A18, 0xFFFFE23A, 0xFF44FF62,
            0xFF24D9FF, 0xFF5F64FF, 0xFFD936FF
    };

    private SpellVisualSignature() {}

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile,
                             ArcaneWorldMesh.Basis basis, double outer, double rotation,
                             double progress, ArcaneWorldMesh.Builder mesh) {
        fingerprint(spell, basis, outer, rotation, progress, mesh);
        if (spell.circle() < 6) return;
        switch (spell.id()) {
            case "disintegrate" -> aperture(mesh, basis, outer, rotation, progress, 3);
            case "sunbeam", "sunburst", "solar_guard" -> solar(mesh, basis, outer, rotation, progress);
            case "mass_suggestion", "dominate_monster", "feeblemind" ->
                    mindCrown(mesh, basis, outer, rotation, progress, spell.id().hashCode());
            case "true_seeing", "foresight", "eyebite" ->
                    eye(mesh, basis, outer, rotation, progress, spell.id().hashCode());
            case "freezing_sphere", "simulacrum", "clone" ->
                    crystal(mesh, basis, outer, rotation, progress, spell.id().hashCode());
            case "flesh_to_stone" -> petrificationGrid(mesh, basis, outer, rotation, progress);
            case "circle_of_death", "power_word_kill", "finger_of_death" ->
                    deathWheel(mesh, basis, outer, rotation, progress, spell.id().hashCode());
            case "delayed_blast_fireball" -> countdown(mesh, basis, outer, rotation, progress);
            case "etherealness" -> phaseShell(mesh, basis, outer, rotation, progress);
            case "prismatic_spray", "prismatic_wall" -> prismCrown(mesh, basis, outer, rotation, progress);
            case "reverse_gravity" -> gravityColumn(mesh, outer, rotation, progress);
            case "maze" -> labyrinth(mesh, basis, outer, rotation, progress);
            case "shapechange", "true_polymorph" -> metamorph(mesh, basis, outer, rotation, progress);
            case "time_stop" -> clock(mesh, basis, outer, rotation, progress);
            case "wish" -> wish(mesh, basis, outer, rotation, progress);
            case "meteor_swarm" -> meteorCrown(mesh, basis, outer, rotation, progress);
            default -> {
                if (spell.circle() >= 8)
                    highCrown(mesh, basis, outer, rotation, progress, spell.id().hashCode());
            }
        }
    }

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                              double range, double power, double age, double powerFactor,
                              ArcaneWorldMesh.Builder mesh) {
        if (spell.circle() >= 6 && !"meteor_swarm".equals(spell.id()))
            SignatureGeometry.append(spell, direction, range, power, mesh);
        impactFingerprint(spell, direction, targetOffset, age, powerFactor, mesh);
        switch (spell.id()) {
            case "time_stop" -> releaseClock(mesh, age, powerFactor);
            case "wish" -> releaseWish(mesh, age, powerFactor);
            case "reverse_gravity" -> releaseGravity(mesh, targetOffset, age, powerFactor);
            case "maze" -> releaseMaze(mesh, targetOffset, age, powerFactor);
            case "shapechange", "true_polymorph" -> releaseMorph(mesh, age, powerFactor);
            case "prismatic_spray" -> releasePrismFan(mesh, direction, targetOffset, age, powerFactor);
            case "delayed_blast_fireball" -> releaseCountdown(mesh, targetOffset, age, powerFactor);
            default -> {}
        }
    }

    static boolean isPrismatic(SpellDefinition spell) {
        return "prismatic_spray".equals(spell.id()) || "prismatic_wall".equals(spell.id());
    }

    static int prismaticColor(int index) {
        return PRISM[Math.floorMod(index, PRISM.length)];
    }

    static ArcaneWorldMesh prismaticAccent(SpellDefinition spell, Vec3 direction,
                                           Vec3 targetOffset, double range, double age, int index) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(180);
        Vec3 forward = safe(direction);
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(forward);
        if ("prismatic_spray".equals(spell.id())) {
            double length = targetOffset.lengthSqr() > 1.0E-8 ? targetOffset.length() : Math.max(6.0, range);
            double lane = (index - 3) * 0.18;
            Vec3 end = forward.scale(length * (0.78 + index * 0.018))
                    .add(facing.right().scale(lane * length))
                    .add(facing.up().scale(Math.sin(index * 1.7) * length * 0.035));
            mesh.line(Vec3.ZERO, end, 1.00F + index * 0.05F);
            if (age > 0.58) mesh.brokenBand(facing, end, 0.32 + index * 0.025, 0.46 + index * 0.03,
                    24, 4 + index % 3, 1.12F, 0.34F);
        } else {
            Vec3 flat = new Vec3(forward.x, 0.0, forward.z);
            if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
            flat = flat.normalize();
            Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
            double half = Math.min(11.0, Math.max(4.0, range * 0.22));
            double x = -half + half * 2.0 * index / 6.0;
            double height = 3.4 + spell.circle() * 0.34;
            Vec3 base = right.scale(x);
            mesh.line(base.add(0.0, -0.6, 0.0), base.add(0.0, height, 0.0), 1.16F);
            mesh.line(base.add(right.scale(-half / 7.0)).add(0.0, height * (0.42 + 0.04 * index), 0.0),
                    base.add(right.scale(half / 7.0)).add(0.0, height * (0.58 - 0.03 * index), 0.0), 0.72F);
        }
        return mesh.build();
    }

    private static void fingerprint(SpellDefinition spell, ArcaneWorldMesh.Basis basis, double outer,
                                    double rotation, double p, ArcaneWorldMesh.Builder mesh) {
        int seed = spell.id().hashCode();
        int sides = 3 + Math.floorMod(seed, 7);
        int runes = 7 + Math.floorMod(seed >>> 3, 12);
        int skip = 2 + Math.floorMod(seed >>> 7, Math.max(1, sides - 1));
        double phase = Math.floorMod(seed, 360) * Math.PI / 180.0;
        double scale = 0.34 + Math.floorMod(seed >>> 11, 23) / 100.0;
        if (p > 0.18) mesh.polygon(basis, Vec3.ZERO, outer * scale, sides, rotation + phase, 0.74F);
        if (p > 0.38) mesh.runeChords(basis, Vec3.ZERO, outer * (0.49 + Math.floorMod(seed >>> 15, 13) / 100.0),
                runes, skip, phase - rotation * 0.37, 0.58F);
        if (p > 0.62) {
            int nodes = 2 + Math.floorMod(seed >>> 19, 4);
            for (int i = 0; i < nodes && !mesh.full(); i++) {
                double a = phase + Math.PI * 2.0 * i / nodes + rotation * 0.19;
                double d = outer * (0.61 + 0.06 * (i % 2));
                Vec3 node = basis.point(a, d);
                mesh.diamond(basis, node, outer * (0.045 + 0.008 * (i % 3)), -a, 0.72F, 0.20F);
                if (i % 2 == 0) mesh.line(basis.point(a, outer * 0.28), node, 0.52F);
            }
        }
    }

    private static void impactFingerprint(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                          double age, double powerFactor, ArcaneWorldMesh.Builder mesh) {
        if (age < 0.62) return;
        int seed = spell.id().hashCode();
        double fade = 1.0 - Math.min(1.0, (age - 0.62) / 0.38);
        Vec3 center = targetOffset.lengthSqr() > 1.0E-8 ? targetOffset : Vec3.ZERO;
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(safe(direction));
        int sides = 3 + Math.floorMod(seed, 7);
        double r = (0.30 + spell.circle() * 0.055) * powerFactor * (1.0 + (age - 0.62) * 1.8);
        mesh.polygon(facing, center, r, sides, Math.floorMod(seed, 360) * Math.PI / 180.0 + age, 0.72F);
        mesh.brokenBand(facing, center, r * 1.08, r * 1.28, 28 + spell.circle() * 2,
                4 + Math.floorMod(seed >>> 5, 4), 0.78F, (float) (0.22 * fade));
    }

    private static void aperture(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p, int sides) {
        if (p < 0.35) return;
        mesh.polygon(b, Vec3.ZERO, r * 0.72, sides, rot * 1.6, 1.18F);
        mesh.polygon(b, Vec3.ZERO, r * 0.48, sides, -rot * 1.2, 0.86F);
        for (int i = 0; i < sides; i++) mesh.line(b.point(rot + Math.PI * 2 * i / sides, r * 0.18),
                b.point(rot + Math.PI * 2 * i / sides, r * 0.92), 0.74F);
    }

    private static void solar(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.28) return;
        mesh.star(b, Vec3.ZERO, r * 0.88, r * 0.32, 12, rot, 1.22F);
        mesh.band(b, Vec3.ZERO, r * 0.46, r * 0.54, 52, 1.24F, 0.30F);
        for (int i = 0; i < 12; i++) mesh.line(b.point(rot + Math.PI * i / 6, r * 0.60),
                b.point(rot + Math.PI * i / 6, r * 1.02), 0.66F);
    }

    private static void mindCrown(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p, int seed) {
        if (p < 0.34) return;
        int petals = 5 + Math.floorMod(seed, 5);
        mesh.star(b, Vec3.ZERO, r * 0.78, r * 0.50, petals, rot * 0.41, 1.08F);
        mesh.runeChords(b, Vec3.ZERO, r * 0.62, 11 + petals, 3 + petals % 4, -rot, 0.82F);
        mesh.brokenBand(b, b.up().scale(r * 0.10), r * 0.83, r * 0.94, 58, 7, 1.10F, 0.34F);
    }

    private static void eye(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p, int seed) {
        if (p < 0.30) return;
        mesh.diamond(b, Vec3.ZERO, r * 0.82, rot * 0.22, 1.18F, 0.30F);
        mesh.brokenBand(b, Vec3.ZERO, r * 0.38, r * 0.56, 48, 5, 1.22F, 0.36F);
        mesh.line(b.right().scale(-r * 0.82), b.right().scale(r * 0.82), 0.88F);
        Vec3 iris = b.point(rot + Math.floorMod(seed, 19) * 0.11, r * 0.10);
        mesh.orb(iris, r * 0.10, 14, 1.26F, 0.42F);
    }

    private static void crystal(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p, int seed) {
        if (p < 0.38) return;
        mesh.sphere(Vec3.ZERO, r * 0.42, 8, 0.76F);
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(b.normal().add(b.right().scale(0.72)), b.up());
        mesh.brokenBand(tilt, Vec3.ZERO, r * 0.50, r * 0.58, 48, 5, 0.84F, 0.28F);
        int spikes = 5 + Math.floorMod(seed, 4);
        for (int i = 0; i < spikes; i++) {
            Vec3 tip = b.point(rot + Math.PI * 2 * i / spikes, r * 0.72);
            mesh.line(tip.scale(0.46), tip, 0.68F);
        }
    }

    private static void petrificationGrid(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.32) return;
        for (int i = 1; i <= 3; i++)
            mesh.polygon(b, Vec3.ZERO, r * (0.26 + i * 0.18), 4,
                    Math.PI / 4 + rot * (i % 2 == 0 ? -0.25 : 0.25), 0.82F);
        for (int i = 0; i < 4; i++)
            mesh.line(b.point(Math.PI / 4 + i * Math.PI / 2, r * 0.18),
                    b.point(Math.PI / 4 + i * Math.PI / 2, r * 0.96), 0.76F);
    }

    private static void deathWheel(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p, int seed) {
        if (p < 0.30) return;
        int spokes = 9 + Math.floorMod(seed, 5);
        mesh.brokenBand(b, Vec3.ZERO, r * 0.78, r * 0.92, 72, 7, 1.18F, 0.38F);
        for (int i = 0; i < spokes; i++) {
            double a = rot + Math.PI * 2 * i / spokes;
            mesh.line(b.point(a, r * 0.24), b.point(a, r * (i % 3 == 0 ? 1.0 : 0.82)),
                    i % 3 == 0 ? 1.02F : 0.60F);
        }
        mesh.star(b, Vec3.ZERO, r * 0.42, r * 0.16, Math.max(4, spokes / 2), rot * 0.4, 1.10F);
    }

    private static void countdown(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.28) return;
        for (int ring = 0; ring < 4; ring++)
            mesh.brokenBand(b, Vec3.ZERO, r * (0.34 + ring * 0.15), r * (0.38 + ring * 0.15),
                    42 + ring * 8, 4 + ring, 0.86F, 0.26F);
        int active = Math.max(1, (int) Math.ceil(p * 8.0));
        for (int i = 0; i < active; i++) {
            double a = -Math.PI / 2 + Math.PI * 2 * i / 8;
            mesh.line(b.point(a, r * 0.18), b.point(a, r * 0.94), i == active - 1 ? 1.20F : 0.58F);
        }
    }

    private static void phaseShell(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.25) return;
        Vec3 n = b.normal();
        for (int i = -2; i <= 2; i++)
            mesh.brokenBand(b, n.scale(i * r * 0.075), r * (0.48 + Math.abs(i) * 0.09),
                    r * (0.54 + Math.abs(i) * 0.09), 46, 5 + Math.abs(i), 0.82F, 0.24F);
    }

    private static void prismCrown(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.26) return;
        for (int i = 0; i < 7; i++) {
            double a = rot + Math.PI * 2 * i / 7;
            mesh.line(b.point(a, r * 0.24), b.point(a + (i - 3) * 0.035, r * 0.98), 0.72F);
            mesh.diamond(b, b.point(a, r * 0.76), r * 0.07, -a, 0.68F, 0.18F);
        }
    }

    private static void gravityColumn(ArcaneWorldMesh.Builder mesh, double r, double rot, double p) {
        if (p < 0.35) return;
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        Vec3 axis = new Vec3(0.0, 1.0, 0.0);
        mesh.helix(Vec3.ZERO, axis, ground, r * 1.35, r * 0.64, 3, 62, 0.82F, true);
        mesh.brokenBand(ground, Vec3.ZERO, r * 0.72, r * 0.86, 54, 6, 1.10F, 0.30F);
    }

    private static void labyrinth(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.28) return;
        for (int i = 0; i < 5; i++) {
            int sides = 4 + (i % 3);
            double rr = r * (0.28 + i * 0.14);
            mesh.polygon(b, Vec3.ZERO, rr, sides, rot * (i % 2 == 0 ? 1 : -1) + i * 0.31, 0.72F);
        }
        for (int i = 0; i < 4; i++)
            mesh.line(b.point(rot + i * Math.PI / 2, r * 0.18),
                    b.point(rot + i * Math.PI / 2 + 0.28, r * 0.92), 0.62F);
    }

    private static void metamorph(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.24) return;
        int[] sides = {3, 5, 7, 9};
        for (int i = 0; i < sides.length; i++) {
            double rr = r * (0.28 + i * 0.16);
            mesh.polygon(b, Vec3.ZERO, rr, sides[i], rot * (i % 2 == 0 ? 1 : -0.7) + p * i, 0.70F);
        }
        mesh.sphere(Vec3.ZERO, r * 0.24, 7, 0.54F);
    }

    private static void clock(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.24) return;
        mesh.band(b, Vec3.ZERO, r * 0.72, r * 0.80, 64, 1.12F, 0.32F);
        for (int i = 0; i < 12; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 6;
            mesh.line(b.point(a, r * (i % 3 == 0 ? 0.58 : 0.64)), b.point(a, r * 0.88),
                    i % 3 == 0 ? 0.92F : 0.54F);
        }
        double hand = -Math.PI / 2 + p * Math.PI * 2;
        mesh.line(Vec3.ZERO, b.point(hand, r * 0.58), 1.20F);
        mesh.line(Vec3.ZERO, b.point(hand * 0.37 - Math.PI / 2, r * 0.38), 0.88F);
    }

    private static void wish(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.24) return;
        mesh.star(b, Vec3.ZERO, r * 0.92, r * 0.36, 9, rot, 1.24F);
        mesh.star(b, Vec3.ZERO, r * 0.64, r * 0.20, 7, -rot * 0.73, 0.92F);
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(b.normal().add(b.up().scale(0.78)), b.right());
        mesh.brokenBand(tilt, Vec3.ZERO, r * 0.54, r * 0.62, 64, 7, 1.04F, 0.30F);
    }

    private static void meteorCrown(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p) {
        if (p < 0.34) return;
        mesh.star(b, Vec3.ZERO, r * 0.56, r * 0.24, 8, rot * 0.52, 0.86F);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            Vec3 node = b.point(a, r * 0.54);
            mesh.diamond(b, node, r * 0.07, -a, 0.78F, 0.22F);
            mesh.line(node, b.point(a, r * 0.90), 0.58F);
        }
    }

    private static void highCrown(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis b, double r, double rot, double p, int seed) {
        if (p < 0.44) return;
        int nodes = 5 + Math.floorMod(seed, 5);
        mesh.brokenBand(b, Vec3.ZERO, r * 0.92, r * 1.02, 76, 6, 1.02F, 0.28F);
        for (int i = 0; i < nodes; i++) {
            double a = rot * 0.21 + Math.PI * 2 * i / nodes;
            mesh.diamond(b, b.point(a, r * 0.84), r * 0.055, -a, 0.64F, 0.18F);
        }
    }

    private static void releaseClock(ArcaneWorldMesh.Builder mesh, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.ground();
        double r = (2.2 + age * 5.5) * s;
        mesh.band(b, Vec3.ZERO, r * 0.86, r, 72, 1.16F, (float) (0.34 * (1.0 - age)));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            mesh.line(b.point(a, r * 0.55), b.point(a, r), 0.62F);
        }
    }

    private static void releaseWish(ArcaneWorldMesh.Builder mesh, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.ground();
        double r = (1.1 + age * 3.2) * s;
        mesh.star(b, Vec3.ZERO, r, r * 0.36, 9, age * 2.4, 1.18F);
        mesh.sphere(new Vec3(0, 1.0, 0), r * 0.42, 8, 0.62F);
    }

    private static void releaseGravity(ArcaneWorldMesh.Builder mesh, Vec3 target, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.ground();
        Vec3 axis = new Vec3(0, 1, 0);
        double r = (1.6 + age * 2.8) * s;
        mesh.helix(target, axis, b, 5.0 + age * 7.0, r, 4, 72, 0.92F, true);
    }

    private static void releaseMaze(ArcaneWorldMesh.Builder mesh, Vec3 target, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.ground();
        double r = (1.2 + age * 2.2) * s;
        for (int i = 0; i < 5; i++)
            mesh.polygon(b, target, r * (0.45 + i * 0.18), 4 + i % 3,
                    age * (i % 2 == 0 ? 1.7 : -1.3) + i * 0.4, 0.76F);
    }

    private static void releaseMorph(ArcaneWorldMesh.Builder mesh, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.ground();
        double r = (0.8 + age * 1.6) * s;
        mesh.sphere(new Vec3(0, 1.0, 0), r, 8, 0.70F);
        mesh.star(b, Vec3.ZERO, r * 1.2, r * 0.42, 7, age * 3.0, 0.82F);
    }

    private static void releasePrismFan(ArcaneWorldMesh.Builder mesh, Vec3 direction, Vec3 target, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.facing(safe(direction));
        Vec3 forward = target.lengthSqr() > 1e-8 ? target : safe(direction).scale(16.0);
        for (int i = 0; i < 7; i++) {
            Vec3 end = forward.add(b.point((i - 3) * 0.17, forward.length() * 0.12));
            mesh.line(Vec3.ZERO, end, 0.58F + i * 0.04F);
        }
    }

    private static void releaseCountdown(ArcaneWorldMesh.Builder mesh, Vec3 target, double age, double s) {
        ArcaneWorldMesh.Basis b = ArcaneWorldMesh.Basis.ground();
        double pulse = 0.7 + Math.sin(age * Math.PI * 10.0) * 0.12;
        mesh.orb(target.add(0, 0.4, 0), pulse * s, 24, 1.20F, 0.42F);
        for (int i = 0; i < 3; i++)
            mesh.brokenBand(b, target, (1.0 + i * 0.35) * s, (1.12 + i * 0.35) * s,
                    36 + i * 8, 5 + i, 0.84F, 0.25F);
    }

    private static Vec3 safe(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : value.normalize();
    }
}
