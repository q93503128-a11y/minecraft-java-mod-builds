package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Phase 2A authored presentation for every 4C normal/fusion formula.
 *
 * The fourth circle is the first tier where the spell routinely owns target/world space instead
 * of merely decorating the caster. Walls assemble where they will stand, storms occupy a vertical
 * volume, restraints close around the victim, body wards build structural shells, and spatial
 * movement shows an actual corridor between two apertures. 5C-6C deliberately remain outside this
 * director until their own Phase 2B/2C passes so this class cannot become a scaled low-circle copy.
 */
final class MidCircleVisualIdentity {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private MidCircleVisualIdentity() {}

    static boolean owns(SpellDefinition spell) {
        return spell != null && spell.circle() == 4;
    }

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile,
                             double outer, double rotation, double progress, Vec3 direction,
                             Vec3 targetOffset, ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "wall_of_fire" -> fireWallInstallation(targetOffset, direction, outer, rotation, progress, mesh);
            case "ice_storm" -> iceStormCanopy(targetOffset, outer, rotation, progress, mesh);
            case "greater_invisibility" -> invisibilityErasure(outer, rotation, progress, mesh);
            case "resilient_sphere" -> resilientSphereClosure(outer, rotation, progress, mesh);
            case "dimension_door" -> dimensionDoorCorridor(targetOffset, direction, outer, rotation, progress, mesh);
            case "stoneskin" -> stoneSkinPlating(outer, rotation, progress, mesh);
            case "confusion" -> confusionCompass(targetOffset, outer, rotation, progress, mesh);
            case "blight" -> blightCage(targetOffset, outer, rotation, progress, mesh);
            case "freedom_of_movement" -> freedomShackles(outer, rotation, progress, mesh);
            case "phantasmal_killer" -> phantasmalMask(targetOffset, direction, outer, rotation, progress, mesh);
            case "fire_shield" -> fireShieldBastion(outer, rotation, progress, mesh);
            case "wall_of_ice" -> iceWallButtress(targetOffset, direction, outer, rotation, progress, mesh);
            case "thunder_cage" -> thunderCagePylons(targetOffset, direction, outer, rotation, progress, mesh);
            default -> { }
        }
    }

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                              double age, double travel, double powerFactor,
                              ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "wall_of_fire" -> fireWallRelease(targetOffset, direction, age, powerFactor, mesh);
            case "ice_storm" -> iceStormRelease(targetOffset, age, powerFactor, mesh);
            case "greater_invisibility" -> invisibilityRelease(age, powerFactor, mesh);
            case "resilient_sphere" -> resilientSphereRelease(age, powerFactor, mesh);
            case "dimension_door" -> dimensionDoorRelease(targetOffset, direction, age, powerFactor, mesh);
            case "stoneskin" -> stoneSkinRelease(age, powerFactor, mesh);
            case "confusion" -> confusionRelease(targetOffset, age, powerFactor, mesh);
            case "blight" -> blightRelease(targetOffset, direction, age, powerFactor, mesh);
            case "freedom_of_movement" -> freedomRelease(age, powerFactor, mesh);
            case "phantasmal_killer" -> phantasmalRelease(targetOffset, direction, age, powerFactor, mesh);
            case "fire_shield" -> fireShieldRelease(age, powerFactor, mesh);
            case "wall_of_ice" -> iceWallRelease(targetOffset, direction, age, powerFactor, mesh);
            case "thunder_cage" -> thunderCageRelease(targetOffset, direction, age, powerFactor, mesh);
            default -> { }
        }
    }

    private static void fireWallInstallation(Vec3 targetOffset, Vec3 direction, double outer,
                                             double rotation, double p, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, outer * 1.8);
        ArcaneWorldMesh.Basis wall = verticalBasis(direction);
        Vec3 right = wall.right();
        double half = Math.max(3.0, outer * 0.88);
        int anchors = 9;
        int active = activeCount(p, 0.05, 0.54, anchors);
        for (int i = 0; i < active && !mesh.full(); i++) {
            double x = lerp(-half, half, i / (double) (anchors - 1));
            Vec3 foot = target.add(right.scale(x));
            double local = phase(p, 0.05 + i * 0.035, 0.55 + i * 0.020);
            mesh.diamond(ArcaneWorldMesh.Basis.ground(), foot, 0.24 + local * 0.18,
                    rotation + i * 0.37, 1.28F, (float) (0.20 + local * 0.22));
            mesh.line(foot, foot.add(0.0, 0.35 + local * 2.45, 0.0), 1.18F);
            if (i > 0) {
                Vec3 previous = target.add(right.scale(lerp(-half, half, (i - 1) / (double) (anchors - 1))));
                mesh.line(previous, foot, 0.92F);
            }
        }
        double panel = phase(p, 0.40, 0.90);
        int panels = activeCount(panel, 0.0, 1.0, anchors - 1);
        for (int i = 0; i < panels && !mesh.full(); i++) {
            double a = lerp(-half, half, i / (double) (anchors - 1));
            double b = lerp(-half, half, (i + 1) / (double) (anchors - 1));
            Vec3 p0 = target.add(right.scale(a));
            Vec3 p1 = target.add(right.scale(b));
            double crest = 1.55 + (i % 3) * 0.38 + panel * 1.15;
            mesh.face(p0, p1, p1.add(0.0, crest, 0.0), p0.add(0.0, crest * 0.86, 0.0),
                    i % 2 == 0 ? 1.20F : 0.96F, (float) (0.12 + panel * 0.18));
        }
        if (p > 0.76) {
            mesh.runeChords(ArcaneWorldMesh.Basis.ground(), target, half * 0.94,
                    9, 4, rotation * 0.18, 0.74F);
        }
    }

    private static void iceWallButtress(Vec3 targetOffset, Vec3 direction, double outer,
                                        double rotation, double p, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, outer * 1.8);
        ArcaneWorldMesh.Basis wall = verticalBasis(direction);
        Vec3 right = wall.right();
        double half = Math.max(3.2, outer * 0.90);
        int buttresses = 8;
        int active = activeCount(p, 0.04, 0.58, buttresses);
        for (int i = 0; i < active && !mesh.full(); i++) {
            double x = lerp(-half, half, i / (double) (buttresses - 1));
            Vec3 foot = target.add(right.scale(x));
            double local = phase(p, 0.04 + i * 0.045, 0.60 + i * 0.018);
            double h = 0.55 + local * (2.1 + (i % 3) * 0.42);
            Vec3 tip = foot.add(0.0, h, 0.0);
            mesh.shard(foot.add(0.0, h * 0.44, 0.0), UP, wall, h * 1.18,
                    0.16 + local * 0.12, 1.18F, (float) (0.20 + local * 0.24));
            mesh.line(foot.add(right.scale(-0.34)), tip, 0.88F);
            mesh.line(foot.add(right.scale(0.34)), tip, 0.88F);
        }
        double plate = phase(p, 0.42, 0.94);
        int panels = activeCount(plate, 0.0, 1.0, buttresses - 1);
        for (int i = 0; i < panels && !mesh.full(); i++) {
            double a = lerp(-half, half, i / (double) (buttresses - 1));
            double b = lerp(-half, half, (i + 1) / (double) (buttresses - 1));
            Vec3 p0 = target.add(right.scale(a));
            Vec3 p1 = target.add(right.scale(b));
            double h0 = 2.05 + (i % 2) * 0.70;
            double h1 = 2.05 + ((i + 1) % 2) * 0.70;
            Vec3 notch = target.add(right.scale((a + b) * 0.5)).add(0.0, Math.max(h0, h1) + 0.65, 0.0);
            mesh.triangle(p0, p1, notch, 1.02F, (float) (0.10 + plate * 0.18));
            mesh.line(p0, notch, 1.02F);
            mesh.line(p1, notch, 1.02F);
        }
    }

    private static void iceStormCanopy(Vec3 targetOffset, double outer, double rotation,
                                       double p, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = targetOffset.lengthSqr() < 1.0E-8 ? Vec3.ZERO : targetOffset;
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = Math.max(3.6, outer * 0.78);
        double ceilingY = 7.0 + outer * 0.35;
        Vec3 ceiling = target.add(0.0, ceilingY, 0.0);
        double lock = phase(p, 0.10, 0.68);
        mesh.polygon(ground, target, radius * (0.34 + lock * 0.62), 8,
                rotation * 0.20, 1.02F);
        if (lock > 0.08) {
            mesh.brokenBand(ground, ceiling, radius * 0.76, radius,
                    84, 6, 1.18F, (float) (0.16 + lock * 0.24));
            mesh.runeChords(ground, ceiling, radius * 0.70, 12, 5, -rotation * 0.24, 0.82F);
        }
        int cells = 7;
        int active = activeCount(p, 0.34, 0.96, cells);
        for (int i = 0; i < active && !mesh.full(); i++) {
            double angle = rotation * 0.28 + Math.PI * 2.0 * i / cells;
            double radial = radius * (i == 0 ? 0.0 : 0.28 + (i % 3) * 0.19);
            Vec3 node = ceiling.add(ground.point(angle, radial));
            mesh.polygonPlate(ground, node, 0.42 + (i % 2) * 0.15, 6,
                    angle, 1.12F, 0.22F);
            mesh.line(node, node.add(0.0, -1.2 - (i % 3) * 0.55, 0.0), 0.82F);
        }
    }

    private static void dimensionDoorCorridor(Vec3 targetOffset, Vec3 direction, double outer,
                                              double rotation, double p, ArcaneWorldMesh.Builder mesh) {
        Vec3 dir = safeDirection(direction);
        Vec3 far = target(targetOffset, dir, Math.max(4.0, outer * 3.2));
        ArcaneWorldMesh.Basis nearBasis = verticalBasis(dir);
        ArcaneWorldMesh.Basis farBasis = verticalBasis(far);
        double nearR = Math.max(1.25, outer * 0.58);
        double farR = nearR * 1.05;
        double nearOpen = phase(p, 0.04, 0.42);
        double railLock = phase(p, 0.26, 0.76);
        double farOpen = phase(p, 0.44, 0.94);
        Vec3 near = dir.scale(0.45);
        if (nearOpen > 0.01) {
            mesh.brokenBand(nearBasis, near, nearR * 0.74 * nearOpen, nearR * nearOpen,
                    58, 5, 1.24F, (float) (0.18 + nearOpen * 0.28));
            mesh.polygon(nearBasis, near, nearR * 0.66 * nearOpen, 6,
                    rotation * 0.40, 0.96F);
        }
        if (farOpen > 0.01) {
            mesh.brokenBand(farBasis, far, farR * 0.72 * farOpen, farR * farOpen,
                    64, 6, 1.22F, (float) (0.16 + farOpen * 0.28));
            mesh.polygon(farBasis, far, farR * 0.62 * farOpen, 6,
                    -rotation * 0.34, 0.92F);
        }
        if (railLock > 0.01) {
            Vec3 nr = nearBasis.right().scale(nearR * 0.55 * railLock);
            Vec3 nu = nearBasis.up().scale(nearR * 0.68 * railLock);
            Vec3 fr = farBasis.right().scale(farR * 0.55 * railLock);
            Vec3 fu = farBasis.up().scale(farR * 0.68 * railLock);
            mesh.line(near.add(nr).add(nu), far.add(fr).add(fu), 0.86F);
            mesh.line(near.subtract(nr).add(nu), far.subtract(fr).add(fu), 0.86F);
            mesh.line(near.add(nr).subtract(nu), far.add(fr).subtract(fu), 0.86F);
            mesh.line(near.subtract(nr).subtract(nu), far.subtract(fr).subtract(fu), 0.86F);
            int slices = Math.min(6, 1 + (int) Math.floor(railLock * 6.0));
            for (int i = 1; i <= slices && !mesh.full(); i++) {
                double t = i / (double) (slices + 1);
                Vec3 c = near.scale(1.0 - t).add(far.scale(t));
                double r = nearR * (0.72 + 0.08 * Math.sin(t * Math.PI));
                mesh.brokenBand(verticalBasis(far.subtract(near)), c, r * 0.82, r,
                        34, 5, 0.78F, 0.16F);
            }
        }
    }

    private static void resilientSphereClosure(double outer, double rotation, double p,
                                               ArcaneWorldMesh.Builder mesh) {
        Vec3 center = new Vec3(0.0, -0.55, 0.0);
        double radius = Math.max(1.0, outer * 0.54);
        double close = phase(p, 0.06, 0.88);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis verticalX = ArcaneWorldMesh.Basis.facing(new Vec3(1.0, 0.0, 0.0));
        ArcaneWorldMesh.Basis verticalZ = ArcaneWorldMesh.Basis.facing(new Vec3(0.0, 0.0, 1.0));
        mesh.arc(ground, center, radius, -Math.PI / 2.0,
                Math.PI * 2.0 * close, 52, 1.18F);
        if (close > 0.20) mesh.arc(verticalX, center, radius * 0.98, -Math.PI / 2.0,
                Math.PI * 2.0 * phase(close, 0.20, 1.0), 48, 1.10F);
        if (close > 0.40) mesh.arc(verticalZ, center, radius * 0.96, -Math.PI / 2.0,
                Math.PI * 2.0 * phase(close, 0.40, 1.0), 48, 1.06F);
        if (close > 0.62) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    new Vec3(1.0, 0.8, 1.0), UP);
            mesh.brokenBand(tilt, center, radius * 0.91, radius,
                    52, 6, 0.92F, (float) (0.14 + close * 0.18));
        }
    }

    private static void fireShieldBastion(double outer, double rotation, double p,
                                          ArcaneWorldMesh.Builder mesh) {
        Vec3 center = new Vec3(0.0, -0.55, 0.0);
        double radius = Math.max(1.0, outer * 0.48);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double base = phase(p, 0.02, 0.46);
        double armor = phase(p, 0.28, 0.84);
        mesh.polygon(ground, center.add(0.0, -0.75, 0.0), radius * (0.55 + base * 0.50),
                8, rotation * 0.18, 1.18F);
        int plates = activeCount(armor, 0.0, 1.0, 8);
        for (int i = 0; i < plates && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / 8.0 + rotation * 0.12;
            Vec3 lower = center.add(ground.point(angle, radius * 0.82)).add(0.0, -0.45, 0.0);
            Vec3 upper = center.add(ground.point(angle, radius * 0.98)).add(0.0, 1.05, 0.0);
            Vec3 tangent = ground.point(angle + Math.PI / 2.0, radius * 0.22);
            mesh.face(lower.subtract(tangent), lower.add(tangent), upper.add(tangent), upper.subtract(tangent),
                    i % 2 == 0 ? 1.18F : 0.92F, (float) (0.14 + armor * 0.18));
            mesh.shard(upper.add(0.0, 0.22, 0.0), UP, verticalBasis(ground.point(angle, 1.0)),
                    0.85 + (i % 2) * 0.20, 0.14, 1.24F, 0.28F);
        }
        if (p > 0.72) mesh.brokenBand(ground, center.add(0.0, 1.15, 0.0), radius * 0.78,
                radius * 1.08, 54, 5, 1.26F, 0.28F);
    }

    private static void invisibilityErasure(double outer, double rotation, double p,
                                            ArcaneWorldMesh.Builder mesh) {
        double radius = Math.max(0.86, outer * 0.62);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        int shutters = 6;
        int active = activeCount(p, 0.04, 0.78, shutters);
        for (int i = 0; i < active && !mesh.full(); i++) {
            double y = -1.05 + i * 0.42;
            double erase = phase(p, 0.06 + i * 0.07, 0.72 + i * 0.035);
            double start = rotation * 0.22 + i * 0.71;
            double sweep = Math.PI * (1.35 - erase * 0.92);
            mesh.arc(ground, new Vec3(0.0, y, 0.0), radius * (0.72 + i * 0.045),
                    start, sweep, 26, i % 2 == 0 ? 1.02F : 0.70F);
            Vec3 sliceA = ground.point(start, radius * 0.90).add(0.0, y, 0.0);
            Vec3 sliceB = ground.point(start + sweep, radius * 0.90).add(0.0, y, 0.0);
            mesh.line(sliceA, sliceB, 0.62F);
        }
        double peel = phase(p, 0.58, 1.0);
        if (peel > 0.01) {
            ArcaneWorldMesh.Basis vertical = ArcaneWorldMesh.Basis.facing(new Vec3(1.0, 0.0, 0.0));
            mesh.arc(vertical, new Vec3(0.0, -0.30 + peel * 0.45, 0.0), radius,
                    Math.PI * 0.20, Math.PI * (1.45 - peel * 0.90), 34, 0.72F);
        }
    }

    private static void stoneSkinPlating(double outer, double rotation, double p,
                                         ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = Math.max(0.82, outer * 0.56);
        int levels = 5;
        int active = activeCount(p, 0.04, 0.86, levels);
        for (int level = 0; level < active && !mesh.full(); level++) {
            double y = -1.05 + level * 0.50;
            double r = radius * (0.92 - Math.abs(level - 2) * 0.055);
            int sides = 5 + (level % 2);
            mesh.polygonPlate(ground, new Vec3(0.0, y, 0.0), r, sides,
                    rotation * 0.11 + level * 0.31, 0.92F + level * 0.04F, 0.16F);
            if (level > 0) {
                for (int i = 0; i < sides; i += 2) {
                    double a = rotation * 0.11 + Math.PI * 2.0 * i / sides;
                    mesh.line(ground.point(a, r).add(0.0, y - 0.48, 0.0),
                            ground.point(a + 0.10, r * 0.96).add(0.0, y, 0.0), 0.74F);
                }
            }
        }
    }

    private static void confusionCompass(Vec3 targetOffset, double outer, double rotation, double p,
                                         ArcaneWorldMesh.Builder mesh) {
        Vec3 target = targetOffset.lengthSqr() < 1.0E-8 ? Vec3.ZERO : targetOffset;
        double radius = Math.max(1.25, outer * 0.48);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double lock = phase(p, 0.06, 0.90);
        Vec3 low = target.add(0.0, 0.12, 0.0);
        Vec3 mid = target.add(0.32 * lock, 1.05, -0.24 * lock);
        Vec3 high = target.add(-0.28 * lock, 1.95, 0.31 * lock);
        mesh.star(ground, low, radius * (0.38 + lock * 0.55), radius * 0.22,
                4, rotation * 0.31, 1.04F);
        ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(new Vec3(1.0, 0.72, 0.28), UP);
        ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(new Vec3(-0.36, 0.62, 1.0), UP);
        if (lock > 0.22) mesh.brokenBand(tiltA, mid, radius * 0.56, radius * 0.76,
                42, 5, 0.92F, 0.20F);
        if (lock > 0.46) mesh.polygon(tiltB, high, radius * 0.66, 5,
                -rotation * 0.46, 0.92F);
        if (lock > 0.68) {
            mesh.line(low.add(ground.right().scale(radius * 0.72)), high.subtract(tiltB.up().scale(radius * 0.42)), 0.68F);
            mesh.line(low.subtract(ground.right().scale(radius * 0.72)), mid.add(tiltA.right().scale(radius * 0.48)), 0.68F);
        }
    }

    private static void blightCage(Vec3 targetOffset, double outer, double rotation, double p,
                                  ArcaneWorldMesh.Builder mesh) {
        Vec3 target = targetOffset.lengthSqr() < 1.0E-8 ? Vec3.ZERO : targetOffset;
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = Math.max(0.95, outer * 0.58);
        double grow = phase(p, 0.04, 0.92);
        int veins = 7;
        int active = activeCount(grow, 0.0, 1.0, veins);
        Vec3 heart = target.add(0.0, 0.82, 0.0);
        for (int i = 0; i < active && !mesh.full(); i++) {
            double angle = rotation * 0.16 + Math.PI * 2.0 * i / veins;
            Vec3 root = target.add(ground.point(angle, radius * (0.92 - grow * 0.18)));
            Vec3 joint = target.add(ground.point(angle + 0.28, radius * 0.62)).add(0.0, 0.55 + (i % 3) * 0.22, 0.0);
            Vec3 thorn = heart.add(ground.point(angle + Math.PI, radius * 0.22));
            mesh.line(root, joint, i % 2 == 0 ? 1.02F : 0.72F);
            mesh.line(joint, thorn, 0.88F);
            mesh.shard(thorn, heart.subtract(thorn), verticalBasis(heart.subtract(thorn)),
                    0.54, 0.08, 1.10F, 0.24F);
        }
        if (grow > 0.68) mesh.brokenBand(ground, heart, radius * 0.28, radius * 0.44,
                30, 4, 1.08F, 0.22F);
    }

    private static void freedomShackles(double outer, double rotation, double p,
                                        ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = Math.max(0.92, outer * 0.54);
        double open = phase(p, 0.04, 0.82);
        for (int level = 0; level < 3 && !mesh.full(); level++) {
            double y = -0.85 + level * 0.80;
            double r = radius * (0.82 + level * 0.09);
            double gap = 0.32 + open * 1.30;
            double start = rotation * 0.16 + level * 0.68 + gap * 0.5;
            mesh.arc(ground, new Vec3(0.0, y, 0.0), r, start,
                    Math.PI * 2.0 - gap, 34, 1.08F);
            Vec3 left = ground.point(start, r).add(0.0, y, 0.0);
            Vec3 right = ground.point(start + Math.PI * 2.0 - gap, r).add(0.0, y, 0.0);
            mesh.line(left, left.add(0.0, 0.28 + open * 0.34, 0.0), 0.72F);
            mesh.line(right, right.add(0.0, 0.28 + open * 0.34, 0.0), 0.72F);
        }
        if (open > 0.44) {
            for (int i = 0; i < 4; i++) {
                Vec3 foot = ground.point(rotation * 0.10 + Math.PI * 2.0 * i / 4.0, radius * 0.62);
                mesh.line(foot.add(0.0, -0.95, 0.0), foot.add(0.0, 1.45 + open * 0.55, 0.0), 0.62F);
            }
        }
    }

    private static void phantasmalMask(Vec3 targetOffset, Vec3 direction, double outer,
                                       double rotation, double p, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, Math.max(2.0, outer * 1.8)).add(0.0, 1.1, 0.0);
        ArcaneWorldMesh.Basis face = verticalBasis(direction);
        double size = Math.max(0.90, outer * 0.60);
        double form = phase(p, 0.06, 0.86);
        Vec3 leftEye = target.add(face.right().scale(-size * 0.34)).add(face.up().scale(size * 0.18));
        Vec3 rightEye = target.add(face.right().scale(size * 0.34)).add(face.up().scale(size * 0.18));
        if (form > 0.08) {
            mesh.diamond(face, leftEye, size * 0.23 * form, rotation * 0.18, 1.12F, 0.22F);
            mesh.diamond(face, rightEye, size * 0.18 * form, -rotation * 0.22, 0.92F, 0.20F);
        }
        if (form > 0.30) {
            Vec3 templeL = target.add(face.right().scale(-size * 0.90));
            Vec3 templeR = target.add(face.right().scale(size * 0.90));
            Vec3 jaw = target.subtract(face.up().scale(size * (0.68 - form * 0.10)));
            mesh.line(templeL, jaw.add(face.right().scale(-size * 0.22)), 1.08F);
            mesh.line(templeR, jaw.add(face.right().scale(size * 0.22)), 1.08F);
            mesh.line(templeL, target.add(face.up().scale(size * 0.78)), 0.78F);
            mesh.line(templeR, target.add(face.up().scale(size * 0.62)), 0.78F);
        }
        if (form > 0.62) {
            mesh.arc(face, target.subtract(face.up().scale(size * 0.34)), size * 0.42,
                    Math.PI * 0.05, Math.PI * 0.90, 20, 0.72F);
        }
    }

    private static void thunderCagePylons(Vec3 targetOffset, Vec3 direction, double outer,
                                          double rotation, double p, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, Math.max(2.0, outer * 1.8));
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = Math.max(1.20, outer * 0.52);
        int active = activeCount(p, 0.04, 0.56, 4);
        Vec3[] corners = {
                target.add(radius, 0.0, radius), target.add(-radius, 0.0, radius),
                target.add(-radius, 0.0, -radius), target.add(radius, 0.0, -radius)
        };
        for (int i = 0; i < active && !mesh.full(); i++) {
            double local = phase(p, 0.04 + i * 0.08, 0.58 + i * 0.05);
            Vec3 foot = corners[i];
            Vec3 top = foot.add(0.0, 2.0 + local * 1.05, 0.0);
            mesh.polygonPlate(ground, foot, 0.28, 4, Math.PI / 4.0, 1.16F, 0.20F);
            mesh.line(foot, top, 1.18F);
            mesh.diamond(ground, top, 0.24 + local * 0.12, rotation + i, 1.22F, 0.24F);
        }
        double rails = phase(p, 0.42, 0.94);
        if (rails > 0.01) {
            int levels = Math.min(4, 1 + (int) Math.floor(rails * 4.0));
            for (int level = 0; level < levels && !mesh.full(); level++) {
                double y = 0.55 + level * 0.72;
                for (int i = 0; i < 4; i++) {
                    Vec3 a = corners[i].add(0.0, y, 0.0);
                    Vec3 b = corners[(i + 1) % 4].add(0.0, y + ((i + level) % 2 == 0 ? 0.16 : -0.12), 0.0);
                    mesh.line(a, b, (i + level) % 2 == 0 ? 1.10F : 0.76F);
                }
            }
        }
    }

    private static void fireWallRelease(Vec3 targetOffset, Vec3 direction, double age,
                                        double powerFactor, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, 6.0);
        ArcaneWorldMesh.Basis wall = verticalBasis(direction);
        Vec3 right = wall.right();
        double half = 5.4 * powerFactor;
        double rise = easeOut(phase(age, 0.0, 0.24));
        double fade = fade(age, 0.78);
        int panels = 10;
        for (int i = 0; i < panels && !mesh.full(); i++) {
            double a = lerp(-half, half, i / (double) panels);
            double b = lerp(-half, half, (i + 1) / (double) panels);
            Vec3 p0 = target.add(right.scale(a));
            Vec3 p1 = target.add(right.scale(b));
            double h0 = (2.4 + 0.75 * Math.sin(i * 1.73 + age * 8.0)) * rise;
            double h1 = (2.4 + 0.75 * Math.sin((i + 1) * 1.73 + age * 8.0)) * rise;
            mesh.face(p0, p1, p1.add(0.0, h1, 0.0), p0.add(0.0, h0, 0.0),
                    i % 2 == 0 ? 1.24F : 0.96F, (float) (0.28 * fade));
            if (i % 2 == 0) {
                Vec3 crest = target.add(right.scale((a + b) * 0.5)).add(0.0, Math.max(h0, h1), 0.0);
                mesh.shard(crest, UP, wall, 0.95 + 0.35 * Math.sin(age * 11.0 + i),
                        0.13, 1.28F, (float) (0.34 * fade));
            }
        }
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        mesh.line(target.add(right.scale(-half)), target.add(right.scale(half)), 1.26F);
        if (age > 0.56) mesh.brokenBand(ground, target, half * 0.58, half * 0.70,
                48, 6, 1.08F, (float) (0.18 * fade));
    }

    private static void iceWallRelease(Vec3 targetOffset, Vec3 direction, double age,
                                       double powerFactor, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, 6.0);
        ArcaneWorldMesh.Basis wall = verticalBasis(direction);
        Vec3 right = wall.right();
        double half = 5.6 * powerFactor;
        double rise = easeOut(phase(age, 0.0, 0.28));
        double fade = fade(age, 0.80);
        int teeth = 9;
        for (int i = 0; i < teeth && !mesh.full(); i++) {
            double x = lerp(-half, half, i / (double) (teeth - 1));
            double h = (2.3 + (i % 3) * 0.62) * rise;
            Vec3 foot = target.add(right.scale(x));
            mesh.shard(foot.add(0.0, h * 0.48, 0.0), UP, wall, h * 1.22,
                    0.28 + (i % 2) * 0.07, 1.18F, (float) (0.32 * fade));
            if (i < teeth - 1) {
                Vec3 next = target.add(right.scale(lerp(-half, half, (i + 1) / (double) (teeth - 1))));
                Vec3 apex = foot.add(next).scale(0.5).add(0.0, h * 0.80, 0.0);
                mesh.triangle(foot, next, apex, 0.92F, (float) (0.15 * fade));
            }
        }
        if (age > 0.50) {
            ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
            for (int i = 0; i < 5; i++) {
                Vec3 crack = target.add(right.scale(lerp(-half * 0.8, half * 0.8, i / 4.0)));
                mesh.line(crack, crack.add(ground.point(i * 1.7 + age, 1.1 + i * 0.16)), 0.66F);
            }
        }
    }

    private static void iceStormRelease(Vec3 targetOffset, double age, double powerFactor,
                                        ArcaneWorldMesh.Builder mesh) {
        Vec3 target = targetOffset.lengthSqr() < 1.0E-8 ? Vec3.ZERO : targetOffset;
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = 5.4 * powerFactor;
        double ceilingY = 9.0;
        double fade = fade(age, 0.84);
        Vec3 ceiling = target.add(0.0, ceilingY, 0.0);
        mesh.brokenBand(ground, ceiling, radius * 0.78, radius,
                72, 6, 1.12F, (float) (0.30 * fade));
        int hail = 12;
        for (int i = 0; i < hail && !mesh.full(); i++) {
            double start = i * 0.028;
            double fall = easeIn(phase(age, start, 0.58 + start));
            double angle = i * 2.399963229728653 + age * 0.45;
            double radial = radius * (0.18 + ((i * 37) % 73) / 100.0);
            Vec3 impact = target.add(ground.point(angle, radial));
            Vec3 spawn = impact.add(0.0, ceilingY - (i % 4) * 0.72, 0.0);
            Vec3 position = spawn.scale(1.0 - fall).add(impact.scale(fall));
            Vec3 axis = safeDirection(impact.subtract(spawn));
            mesh.shard(position, axis, verticalBasis(axis), 0.85 + (i % 3) * 0.22,
                    0.11 + (i % 2) * 0.03, 1.20F, (float) (0.40 * fade));
            if (fall > 0.82) mesh.brokenBand(ground, impact, 0.20, 0.34 + fall * 0.22,
                    18, 4, 0.92F, (float) (0.22 * fade));
        }
        if (age > 0.56) {
            double spread = phase(age, 0.56, 0.82);
            mesh.brokenBand(ground, target, radius * (0.46 + spread * 0.30),
                    radius * (0.54 + spread * 0.34), 66, 7, 1.14F, (float) (0.26 * fade));
        }
    }

    private static void dimensionDoorRelease(Vec3 targetOffset, Vec3 direction, double age,
                                             double powerFactor, ArcaneWorldMesh.Builder mesh) {
        Vec3 dir = safeDirection(direction);
        Vec3 far = target(targetOffset, dir, 6.0);
        Vec3 near = dir.scale(0.45);
        ArcaneWorldMesh.Basis corridor = verticalBasis(far.subtract(near));
        double r = 1.55 * powerFactor;
        double open = easeOut(phase(age, 0.0, 0.18));
        double fade = fade(age, 0.76);
        mesh.band(corridor, near, r * 0.74 * open, r * open, 58, 1.22F, (float) (0.30 * fade));
        mesh.band(corridor, far, r * 0.76 * open, r * 1.04 * open, 60, 1.20F, (float) (0.28 * fade));
        double tunnel = phase(age, 0.10, 0.52);
        int slices = 7;
        int active = activeCount(tunnel, 0.0, 1.0, slices);
        for (int i = 1; i <= active && !mesh.full(); i++) {
            double t = i / (double) (slices + 1);
            Vec3 c = near.scale(1.0 - t).add(far.scale(t));
            mesh.brokenBand(corridor, c, r * 0.61, r * 0.78,
                    32, 5, 0.82F, (float) (0.17 * fade));
        }
        if (age > 0.30 && age < 0.66) {
            double surge = phase(age, 0.30, 0.66);
            Vec3 pulse = near.scale(1.0 - surge).add(far.scale(surge));
            mesh.disc(corridor, pulse, r * (0.26 + 0.16 * Math.sin(age * 15.0)),
                    24, 1.26F, (float) (0.24 * fade));
        }
    }

    private static void resilientSphereRelease(double age, double powerFactor,
                                                ArcaneWorldMesh.Builder mesh) {
        Vec3 center = new Vec3(0.0, -0.55, 0.0);
        double radius = 1.32 * powerFactor;
        double close = easeOut(phase(age, 0.0, 0.22));
        double fade = fade(age, 0.82);
        mesh.sphere(center, radius * close, 5, 1.20F);
        if (close > 0.45) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(new Vec3(1.0, 1.0, 0.65), UP);
            mesh.brokenBand(tilt, center, radius * 0.92, radius * 1.04,
                    50, 6, 1.10F, (float) (0.24 * fade));
        }
        if (age > 0.55) {
            double pulse = phase(age, 0.55, 0.84);
            mesh.orb(center, radius * (0.62 + pulse * 0.72), 24,
                    0.86F, (float) (0.12 * fade));
        }
    }

    private static void invisibilityRelease(double age, double powerFactor,
                                             ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = 1.12 * powerFactor;
        double erase = phase(age, 0.0, 0.48);
        double fade = 1.0 - phase(age, 0.16, 0.76);
        for (int i = 0; i < 6 && !mesh.full(); i++) {
            double y = -1.05 + i * 0.43 + erase * 0.22;
            double gap = 0.45 + erase * 2.20;
            mesh.arc(ground, new Vec3(0.0, y, 0.0), radius * (0.76 + i * 0.035),
                    i * 0.82 + age * 2.0 + gap * 0.5,
                    Math.max(0.18, Math.PI * 2.0 - gap), 28, i % 2 == 0 ? 0.92F : 0.62F);
        }
        if (fade > 0.01) mesh.orb(new Vec3(0.0, -0.36, 0.0), radius * (0.76 + erase * 0.25),
                20, 0.72F, (float) (0.10 * fade));
    }

    private static void stoneSkinRelease(double age, double powerFactor,
                                         ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = 0.98 * powerFactor;
        double settle = easeOut(phase(age, 0.0, 0.22));
        double fade = fade(age, 0.84);
        for (int level = 0; level < 5 && !mesh.full(); level++) {
            double y = -1.05 + level * 0.50;
            double r = radius * (0.92 - Math.abs(level - 2) * 0.045) * settle;
            mesh.polygonPlate(ground, new Vec3(0.0, y, 0.0), r,
                    5 + level % 2, level * 0.43 + age * 0.18, 0.94F, (float) (0.20 * fade));
        }
        if (age > 0.58) {
            double shed = phase(age, 0.58, 0.92);
            for (int i = 0; i < 6; i++) {
                Vec3 shard = ground.point(i * Math.PI / 3.0 + age, radius * (0.78 + shed * 0.55)).add(0.0, -0.20 + (i % 3) * 0.44, 0.0);
                mesh.shard(shard, ground.point(i * Math.PI / 3.0, 1.0), verticalBasis(ground.point(i, 1.0)),
                        0.42, 0.07, 0.82F, (float) (0.16 * fade));
            }
        }
    }

    private static void confusionRelease(Vec3 targetOffset, double age, double powerFactor,
                                         ArcaneWorldMesh.Builder mesh) {
        Vec3 target = targetOffset.lengthSqr() < 1.0E-8 ? Vec3.ZERO : targetOffset;
        double radius = 1.55 * powerFactor;
        double enter = easeOut(phase(age, 0.0, 0.18));
        double fade = fade(age, 0.82);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(new Vec3(1.0, 0.55, 0.25), UP);
        ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(new Vec3(-0.35, 0.68, 1.0), UP);
        mesh.star(ground, target, radius * 0.92 * enter, radius * 0.30,
                4, age * 5.0, 1.06F);
        mesh.brokenBand(tiltA, target.add(0.32, 1.0, -0.28), radius * 0.48, radius * 0.70,
                40, 5, 0.92F, (float) (0.22 * fade));
        mesh.polygon(tiltB, target.add(-0.30, 1.90, 0.34), radius * 0.62,
                5, -age * 4.3, 0.88F);
        for (int i = 0; i < 5; i++) {
            Vec3 mote = target.add(ground.point(age * 4.0 + i * 1.31,
                    radius * (0.44 + (i % 2) * 0.20))).add(0.0, 0.35 + (i % 3) * 0.62, 0.0);
            mesh.orb(mote, 0.09 + (i % 2) * 0.025, 10, 1.10F, (float) (0.24 * fade));
        }
    }

    private static void blightRelease(Vec3 targetOffset, Vec3 direction, double age,
                                      double powerFactor, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, 3.0);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = 1.18 * powerFactor;
        double squeeze = easeIn(phase(age, 0.0, 0.46));
        double fade = fade(age, 0.80);
        Vec3 heart = target.add(0.0, 0.80, 0.0);
        int veins = 8;
        for (int i = 0; i < veins && !mesh.full(); i++) {
            double angle = i * Math.PI * 2.0 / veins + age * 0.35;
            Vec3 root = target.add(ground.point(angle, radius * (1.05 - squeeze * 0.56)));
            Vec3 joint = heart.add(ground.point(angle + 0.28, radius * (0.46 - squeeze * 0.20))).add(0.0, (i % 2) * 0.24, 0.0);
            mesh.line(root, joint, i % 2 == 0 ? 1.02F : 0.70F);
            mesh.line(joint, heart, 0.82F);
        }
        mesh.orb(heart, radius * (0.18 + squeeze * 0.20), 18, 0.92F, (float) (0.24 * fade));
        if (age > 0.48) {
            double residue = phase(age, 0.48, 0.88);
            mesh.brokenBand(ground, target, radius * (0.25 + residue * 0.22),
                    radius * (0.38 + residue * 0.34), 34, 5, 0.88F, (float) (0.16 * fade));
        }
    }

    private static void freedomRelease(double age, double powerFactor,
                                       ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = 1.12 * powerFactor;
        double burst = easeOut(phase(age, 0.0, 0.30));
        double fade = fade(age, 0.78);
        for (int level = 0; level < 3 && !mesh.full(); level++) {
            double y = -0.86 + level * 0.82 + burst * 0.35;
            double gap = 0.75 + burst * 2.10;
            double start = level * 0.72 + age * 1.4 + gap * 0.5;
            mesh.arc(ground, new Vec3(0.0, y, 0.0), radius * (0.82 + level * 0.10),
                    start, Math.max(0.12, Math.PI * 2.0 - gap), 30, 1.02F);
        }
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2.0 / 5.0 + age;
            Vec3 base = ground.point(a, radius * 0.55);
            mesh.line(base.add(0.0, -0.90, 0.0), base.add(0.0, 0.40 + burst * 2.2, 0.0), 0.62F);
        }
        if (age > 0.46) mesh.brokenBand(ground, new Vec3(0.0, 1.20 + burst * 0.60, 0.0),
                radius * 0.80, radius * 1.16, 42, 5, 0.88F, (float) (0.16 * fade));
    }

    private static void phantasmalRelease(Vec3 targetOffset, Vec3 direction, double age,
                                          double powerFactor, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, 3.0).add(0.0, 1.10, 0.0);
        ArcaneWorldMesh.Basis face = verticalBasis(direction);
        double size = 1.28 * powerFactor;
        double close = easeIn(phase(age, 0.0, 0.50));
        double fade = fade(age, 0.78);
        Vec3 leftEye = target.add(face.right().scale(-size * 0.33)).add(face.up().scale(size * 0.18));
        Vec3 rightEye = target.add(face.right().scale(size * 0.33)).add(face.up().scale(size * 0.18));
        mesh.diamond(face, leftEye, size * (0.22 - close * 0.06), age * 2.0, 1.18F, (float) (0.26 * fade));
        mesh.diamond(face, rightEye, size * (0.18 - close * 0.05), -age * 2.2, 0.98F, (float) (0.24 * fade));
        Vec3 jaw = target.subtract(face.up().scale(size * (0.70 - close * 0.26)));
        Vec3 templeL = target.add(face.right().scale(-size * (0.92 - close * 0.34)));
        Vec3 templeR = target.add(face.right().scale(size * (0.92 - close * 0.34)));
        mesh.line(templeL, jaw.add(face.right().scale(-size * 0.20)), 1.12F);
        mesh.line(templeR, jaw.add(face.right().scale(size * 0.20)), 1.12F);
        mesh.line(templeL, target.add(face.up().scale(size * 0.72)), 0.82F);
        mesh.line(templeR, target.add(face.up().scale(size * 0.60)), 0.82F);
        if (age > 0.50) {
            double fracture = phase(age, 0.50, 0.90);
            for (int i = 0; i < 4; i++) {
                Vec3 origin = target.add(face.point(i * Math.PI / 2.0 + 0.25, size * 0.42));
                Vec3 end = origin.add(face.point(i * 1.71 + age, size * fracture));
                mesh.line(origin, end, 0.58F);
            }
        }
    }

    private static void fireShieldRelease(double age, double powerFactor,
                                          ArcaneWorldMesh.Builder mesh) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        Vec3 center = new Vec3(0.0, -0.48, 0.0);
        double radius = 1.34 * powerFactor;
        double close = easeOut(phase(age, 0.0, 0.22));
        double fade = fade(age, 0.84);
        int plates = 10;
        for (int i = 0; i < plates && !mesh.full(); i++) {
            double angle = i * Math.PI * 2.0 / plates + age * 0.22;
            Vec3 lower = center.add(ground.point(angle, radius * 0.84 * close)).add(0.0, -0.55, 0.0);
            Vec3 upper = center.add(ground.point(angle, radius * close)).add(0.0, 1.0 + (i % 2) * 0.18, 0.0);
            Vec3 tangent = ground.point(angle + Math.PI / 2.0, radius * 0.20);
            mesh.face(lower.subtract(tangent), lower.add(tangent), upper.add(tangent), upper.subtract(tangent),
                    i % 2 == 0 ? 1.22F : 0.94F, (float) (0.24 * fade));
            if (i % 2 == 0) mesh.shard(upper.add(0.0, 0.26, 0.0), UP, verticalBasis(ground.point(angle, 1.0)),
                    0.82, 0.12, 1.30F, (float) (0.30 * fade));
        }
        mesh.brokenBand(ground, center.add(0.0, 1.18, 0.0), radius * 0.82, radius * 1.08,
                52, 5, 1.22F, (float) (0.28 * fade));
    }

    private static void thunderCageRelease(Vec3 targetOffset, Vec3 direction, double age,
                                           double powerFactor, ArcaneWorldMesh.Builder mesh) {
        Vec3 target = target(targetOffset, direction, 3.0);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double radius = 1.48 * powerFactor;
        double erect = easeOut(phase(age, 0.0, 0.18));
        double fade = fade(age, 0.84);
        Vec3[] corners = {
                target.add(radius, 0.0, radius), target.add(-radius, 0.0, radius),
                target.add(-radius, 0.0, -radius), target.add(radius, 0.0, -radius)
        };
        double height = 3.15 * erect;
        for (int i = 0; i < 4; i++) {
            mesh.line(corners[i], corners[i].add(0.0, height, 0.0), 1.22F);
            mesh.diamond(ground, corners[i].add(0.0, height, 0.0), 0.28,
                    age * 2.0 + i, 1.24F, (float) (0.26 * fade));
        }
        for (int level = 0; level < 5 && !mesh.full(); level++) {
            double y = height * (0.16 + level * 0.18);
            for (int i = 0; i < 4; i++) {
                Vec3 a = corners[i].add(0.0, y, 0.0);
                Vec3 b = corners[(i + 1) % 4].add(0.0, y + ((i + level) % 2 == 0 ? 0.18 : -0.13), 0.0);
                mesh.line(a, b, (i + level) % 2 == 0 ? 1.16F : 0.70F);
            }
        }
        double arc = phase(age, 0.14, 0.76);
        if (arc > 0.01) {
            for (int i = 0; i < 4; i++) {
                Vec3 a = corners[i].add(0.0, 0.45 + ((i + 1) % 3) * 0.62, 0.0);
                Vec3 b = corners[(i + 2) % 4].add(0.0, 1.15 + (i % 2) * 0.78, 0.0);
                Vec3 kink = a.scale(0.48).add(b.scale(0.52)).add(ground.point(i * 1.33 + age * 7.0, 0.32 + arc * 0.20));
                mesh.line(a, kink, 1.02F);
                mesh.line(kink, b, 1.02F);
            }
        }
        if (age > 0.55) mesh.brokenBand(ground, target, radius * 0.72, radius * 1.02,
                44, 5, 1.14F, (float) (0.22 * fade));
    }

    private static Vec3 target(Vec3 targetOffset, Vec3 direction, double fallbackDistance) {
        if (targetOffset != null && targetOffset.lengthSqr() > 1.0E-8) return targetOffset;
        return safeDirection(direction).scale(Math.max(0.5, fallbackDistance));
    }

    private static ArcaneWorldMesh.Basis verticalBasis(Vec3 direction) {
        Vec3 horizontal = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 1.0E-8) horizontal = new Vec3(0.0, 0.0, 1.0);
        return ArcaneWorldMesh.Basis.fromNormal(horizontal.normalize(), UP);
    }

    private static Vec3 safeDirection(Vec3 direction) {
        return direction == null || direction.lengthSqr() < 1.0E-8
                ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
    }

    private static int activeCount(double progress, double start, double end, int count) {
        if (count <= 0) return 0;
        double q = phase(progress, start, end);
        return q <= 0.0 ? 0 : Math.min(count, Math.max(1, (int) Math.ceil(q * count)));
    }

    private static double phase(double value, double start, double end) {
        if (end <= start) return value >= end ? 1.0 : 0.0;
        return clamp((value - start) / (end - start));
    }

    private static double fade(double age, double start) {
        return 1.0 - phase(age, start, 1.0);
    }

    private static double easeOut(double t) {
        t = clamp(t);
        return 1.0 - Math.pow(1.0 - t, 2.25);
    }

    private static double easeIn(double t) {
        t = clamp(t);
        return t * t * (0.65 + 0.35 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp(t);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
