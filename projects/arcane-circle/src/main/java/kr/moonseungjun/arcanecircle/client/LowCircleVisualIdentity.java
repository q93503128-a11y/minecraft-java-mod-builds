package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Hand-authored presentation grammar for every 1C-3C spell and fusion.
 *
 * The low circles are the quality baseline for the rest of the overhaul: no hash-only fallback,
 * no recoloured copy of the same circle, and no detached projectile that appears from nowhere.
 * Each formula owns a readable preparation device and a release body/field that explains what the
 * spell is doing before the player ever reads its tooltip.
 */
final class LowCircleVisualIdentity {
    private LowCircleVisualIdentity() {}

    static boolean owns(SpellDefinition spell) {
        return spell.circle() >= 1 && spell.circle() <= 3;
    }

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile,
                             ArcaneWorldMesh.Basis b, double r, double rot, double p,
                             ArcaneWorldMesh.Builder m) {
        double lock = phase(p, 0.16, 0.68);
        double finish = phase(p, 0.48, 1.0);
        switch (spell.id()) {
            case "magic_missile" -> missileRack(m, b, r, rot, lock, finish);
            case "fire_bolt" -> fireIgniter(m, b, r, rot, lock, finish);
            case "ray_of_frost" -> frostAperture(m, b, r, rot, lock, finish);
            case "shield" -> shieldLattice(m, b, r, rot, lock, finish);
            case "feather_fall" -> featherRune(m, b, r, rot, lock, finish);
            case "light" -> lightHalo(m, b, r, rot, lock, finish);
            case "grease" -> greaseSeal(m, b, r, rot, lock, finish);
            case "sleep" -> sleepSpiral(m, b, r, rot, lock, finish);
            case "thunderwave" -> thunderGate(m, b, r, rot, lock, finish);
            case "mage_armor" -> armorLattice(m, b, r, rot, lock, finish);

            case "scorching_ray" -> scorchingRack(m, b, r, rot, lock, finish);
            case "misty_step" -> mistGate(m, b, r, rot, lock, finish);
            case "web" -> webSeal(m, b, r, rot, lock, finish);
            case "mirror_image" -> mirrorTriptych(m, b, r, rot, lock, finish);
            case "invisibility" -> invisibilityBreak(m, b, r, rot, lock, finish);
            case "gust_of_wind" -> windNozzle(m, b, r, rot, lock, finish);
            case "hold_person" -> holdCross(m, b, r, rot, lock, finish);
            case "shatter" -> fractureSeal(m, b, r, rot, lock, finish);
            case "blur" -> blurOscillator(m, b, r, rot, lock, finish);
            case "levitate" -> levitationStack(m, b, r, rot, lock, finish);

            case "fireball" -> fireballReactor(m, b, r, rot, lock, finish);
            case "lightning_bolt" -> lightningRail(m, b, r, rot, lock, finish);
            case "fly" -> flightWings(m, b, r, rot, lock, finish);
            case "haste" -> hasteClock(m, b, r, rot, lock, finish);
            case "dispel_magic" -> dispelLock(m, b, r, rot, lock, finish);
            case "vampiric_touch" -> vampiricTether(m, b, r, rot, lock, finish);
            case "slow" -> slowClock(m, b, r, rot, lock, finish);
            case "protection_from_energy" -> energyWard(m, b, r, rot, lock, finish);
            case "sleet_storm" -> sleetArray(m, b, r, rot, lock, finish);
            case "blink" -> blinkPair(m, b, r, rot, lock, finish);

            case "burning_hands" -> burningPalm(m, b, r, rot, lock, finish);
            case "ice_knife" -> iceKnife(m, b, r, rot, lock, finish);
            case "chromatic_orb" -> chromaticCrown(m, b, r, rot, lock, finish);
            case "wind_wall" -> windWallMatrix(m, b, r, rot, lock, finish);
            case "counterspell" -> counterSeal(m, b, r, rot, lock, finish);
            case "steam_burst" -> steamChamber(m, b, r, rot, lock, finish);
            case "frost_step" -> frostStepGate(m, b, r, rot, lock, finish);
            default -> formulaSeal(m, b, r, rot, lock, finish, spell.id().hashCode());
        }
    }

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                              double age, double travel, double powerFactor,
                              ArcaneWorldMesh.Builder m) {
        Vec3 forward = safe(direction);
        Vec3 target = targetOffset.lengthSqr() < 1.0E-8 ? forward.scale(Math.max(2.0, spell.range())) : targetOffset;
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(forward);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        switch (spell.id()) {
            case "magic_missile" -> missileRelease(m, face, target, age, travel, powerFactor);
            case "fire_bolt" -> fireBoltRelease(m, face, forward, target, age, travel, powerFactor);
            case "ray_of_frost" -> frostRayRelease(m, face, target, age, powerFactor);
            case "shield" -> shieldRelease(m, powerFactor, age);
            case "feather_fall" -> featherRelease(m, ground, age, powerFactor);
            case "light" -> lightRelease(m, face, age, powerFactor);
            case "grease" -> greaseRelease(m, ground, target, age, powerFactor);
            case "sleep" -> sleepRelease(m, ground, target, age, powerFactor);
            case "thunderwave" -> thunderRelease(m, face, forward, age, powerFactor);
            case "mage_armor" -> armorRelease(m, age, powerFactor);

            case "scorching_ray" -> scorchingRelease(m, face, target, age, powerFactor);
            case "misty_step" -> mistRelease(m, face, forward, age, powerFactor);
            case "web" -> webRelease(m, ground, target, age, powerFactor);
            case "mirror_image" -> mirrorRelease(m, face, age, powerFactor);
            case "invisibility" -> invisibilityRelease(m, age, powerFactor);
            case "gust_of_wind" -> gustRelease(m, face, forward, age, powerFactor);
            case "hold_person" -> holdRelease(m, ground, target, age, powerFactor);
            case "shatter" -> shatterRelease(m, face, target, age, powerFactor);
            case "blur" -> blurRelease(m, face, age, powerFactor);
            case "levitate" -> levitateRelease(m, ground, age, powerFactor);

            case "fireball" -> fireballRelease(m, face, forward, target, age, travel, powerFactor);
            case "lightning_bolt" -> lightningRelease(m, face, forward, target, age, powerFactor);
            case "fly" -> flyRelease(m, face, age, powerFactor);
            case "haste" -> hasteRelease(m, face, age, powerFactor);
            case "dispel_magic" -> dispelRelease(m, face, target, age, powerFactor);
            case "vampiric_touch" -> vampiricRelease(m, face, target, age, powerFactor);
            case "slow" -> slowRelease(m, ground, target, age, powerFactor);
            case "protection_from_energy" -> energyWardRelease(m, age, powerFactor);
            case "sleet_storm" -> sleetRelease(m, ground, target, age, powerFactor);
            case "blink" -> blinkRelease(m, face, forward, age, powerFactor);

            case "burning_hands" -> burningHandsRelease(m, face, forward, age, powerFactor);
            case "ice_knife" -> iceKnifeRelease(m, face, forward, target, age, travel, powerFactor);
            case "chromatic_orb" -> chromaticRelease(m, face, forward, target, age, travel, powerFactor);
            case "wind_wall" -> windWallRelease(m, ground, target, direction, age, powerFactor);
            case "counterspell" -> counterRelease(m, face, target, age, powerFactor);
            case "steam_burst" -> steamRelease(m, face, forward, age, powerFactor);
            case "frost_step" -> frostStepRelease(m, ground, face, forward, age, powerFactor);
            default -> impact(m, face, target, age, 0.34 * powerFactor, 5);
        }
    }

    private static void missileRack(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.diamond(b, Vec3.ZERO, r * (0.20 + 0.18 * l), rot, 1.12F, 0.24F);
        for (int i = 0; i < 3; i++) {
            double a = rot + Math.PI * 2.0 * i / 3.0;
            Vec3 node = b.point(a, r * (0.40 + 0.22 * l));
            m.band(b, node, r * 0.095, r * 0.15, 26, 1.24F, 0.32F);
            m.line(b.point(a, r * 0.16), node, 0.82F);
        }
        if (f > 0.05) m.runeChords(b, Vec3.ZERO, r * 0.72, 9, 4, -rot * 0.72, 0.72F);
    }

    private static void fireIgniter(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.starPlate(b, Vec3.ZERO, r * (0.24 + l * 0.30), r * 0.12, 3, rot, 1.18F, 0.28F);
        m.polygon(b, Vec3.ZERO, r * (0.42 + l * 0.28), 3, -rot * 0.42, 1.06F);
        if (f > 0.05) {
            m.brokenBand(b, Vec3.ZERO, r * 0.70, r * 0.82, 40, 5, 1.22F, 0.32F);
            m.line(b.point(rot, r * 0.24), b.point(rot, r * 0.98), 1.18F);
        }
    }

    private static void frostAperture(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        int arms = 6;
        for (int i = 0; i < arms; i++) {
            double a = rot + Math.PI * 2.0 * i / arms;
            Vec3 a0 = b.point(a, r * 0.16), a1 = b.point(a, r * (0.38 + 0.40 * l));
            m.line(a0, a1, i % 2 == 0 ? 1.04F : 0.72F);
            if (f > 0.12) {
                Vec3 branch = b.point(a + Math.PI / 9.0, r * 0.58);
                m.line(b.point(a, r * 0.43), branch, 0.58F);
            }
        }
        m.polygon(b, Vec3.ZERO, r * (0.32 + 0.40 * l), 6, -rot * 0.34, 0.84F);
    }

    private static void shieldLattice(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygonPlate(b, Vec3.ZERO, r * (0.34 + 0.46 * l), 6, rot * 0.18, 1.04F, 0.20F);
        m.diamond(b, Vec3.ZERO, r * 0.36, -rot, 1.24F, 0.26F);
        if (f > 0.08) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(b.normal().add(b.right().scale(0.62)), b.up());
            m.brokenBand(tilt, Vec3.ZERO, r * 0.62, r * 0.70, 42, 6, 0.88F, 0.24F);
        }
    }

    private static void featherRune(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.diamond(b, Vec3.ZERO, r * (0.26 + 0.26 * l), rot * 0.24, 0.92F, 0.18F);
        for (int side : new int[]{-1, 1}) {
            Vec3 root = b.right().scale(side * r * 0.12);
            for (int i = 0; i < 3; i++) {
                double y = r * (0.14 + i * 0.13);
                m.line(root.add(b.up().scale(y * 0.20)), b.right().scale(side * r * (0.42 + i * 0.08)).add(b.up().scale(y)), 0.70F);
            }
        }
        if (f > 0.12) m.brokenBand(b, Vec3.ZERO, r * 0.68, r * 0.75, 38, 8, 0.86F, 0.20F);
    }

    private static void lightHalo(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.orb(Vec3.ZERO, r * (0.10 + 0.12 * l), 18, 1.24F, 0.38F);
        int rays = 8;
        for (int i = 0; i < rays; i++) {
            double a = rot + Math.PI * 2.0 * i / rays;
            m.line(b.point(a, r * 0.30), b.point(a, r * (0.48 + 0.28 * f)), i % 2 == 0 ? 0.92F : 0.62F);
        }
        m.band(b, Vec3.ZERO, r * 0.30, r * 0.36, 34, 1.08F, 0.24F);
    }

    private static void greaseSeal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.brokenBand(b, Vec3.ZERO, r * (0.34 + 0.28 * l), r * (0.44 + 0.30 * l), 46, 7, 0.84F, 0.20F);
        for (int i = 0; i < 4; i++) {
            Vec3 pool = b.point(rot + i * 1.57, r * 0.38);
            m.band(b, pool, r * 0.06, r * (0.12 + 0.05 * f), 18, 0.72F, 0.18F);
        }
    }

    private static void sleepSpiral(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        for (int i = 0; i < 3; i++) {
            double rr = r * (0.18 + i * 0.17 + l * 0.06);
            Vec3 c = b.point(rot * 0.30 + i * 2.1, r * 0.10 * i);
            m.brokenBand(b, c, rr * 0.82, rr, 30 + i * 8, 5 + i, 0.72F + i * 0.08F, 0.18F);
        }
        if (f > 0.18) for (int i = 0; i < 3; i++) m.orb(b.point(rot + i * 2.09, r * 0.58), r * 0.06, 10, 0.86F, 0.22F);
    }

    private static void thunderGate(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygon(b, Vec3.ZERO, r * (0.36 + 0.42 * l), 4, Math.PI / 4.0, 1.12F);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4.0 + i * Math.PI / 2.0;
            m.line(b.point(a, r * 0.18), b.point(a, r * (0.62 + 0.34 * f)), i == 0 ? 1.26F : 0.78F);
        }
        if (f > 0.10) m.runeChords(b, Vec3.ZERO, r * 0.66, 8, 3, -rot, 0.62F);
    }

    private static void armorLattice(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygonPlate(b, Vec3.ZERO, r * (0.30 + 0.40 * l), 8, rot * 0.16, 0.92F, 0.18F);
        m.diamond(b, Vec3.ZERO, r * 0.46, -rot * 0.40, 1.10F, 0.22F);
        if (f > 0.05) {
            ArcaneWorldMesh.Basis a = ArcaneWorldMesh.Basis.fromNormal(b.normal().add(b.up().scale(0.7)), b.right());
            ArcaneWorldMesh.Basis c = ArcaneWorldMesh.Basis.fromNormal(b.normal().add(b.right().scale(0.7)), b.up());
            m.brokenBand(a, Vec3.ZERO, r * 0.54, r * 0.62, 42, 5, 0.78F, 0.18F);
            m.brokenBand(c, Vec3.ZERO, r * 0.64, r * 0.70, 42, 6, 0.82F, 0.18F);
        }
    }

    private static void scorchingRack(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygon(b, Vec3.ZERO, r * 0.40, 3, rot, 0.86F);
        for (int i = 0; i < 3; i++) {
            double a = rot + Math.PI * 2.0 * i / 3.0;
            Vec3 node = b.point(a, r * (0.40 + 0.22 * l));
            m.band(b, node, r * 0.12, r * 0.19, 28, 1.26F, 0.34F);
            if (f > 0.10) m.line(node, b.point(a, r * 0.96), 1.02F);
        }
    }

    private static void mistGate(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        Vec3 n = b.normal();
        m.brokenBand(b, n.scale(-r * 0.12), r * 0.54, r * (0.66 + 0.22 * l), 48, 7, 0.94F, 0.24F);
        m.brokenBand(b, n.scale(r * 0.12), r * 0.34, r * (0.44 + 0.20 * l), 42, 5, 1.10F, 0.26F);
        if (f > 0.15) for (int i = 0; i < 4; i++) m.line(b.point(rot + i * 1.57, r * 0.48).add(n.scale(-r * 0.12)), b.point(rot + 0.2 + i * 1.57, r * 0.34).add(n.scale(r * 0.12)), 0.60F);
    }

    private static void webSeal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        int spokes = 8;
        for (int i = 0; i < spokes; i++) {
            double a = rot + Math.PI * 2.0 * i / spokes;
            m.line(Vec3.ZERO, b.point(a, r * (0.42 + 0.40 * l)), i % 2 == 0 ? 0.78F : 0.52F);
        }
        for (int ring = 1; ring <= 3; ring++) m.brokenBand(b, Vec3.ZERO, r * (0.16 + ring * 0.16), r * (0.18 + ring * 0.16), 42, 8 - ring, 0.64F, 0.16F + (float) f * 0.05F);
    }

    private static void mirrorTriptych(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        for (int i = -1; i <= 1; i++) {
            Vec3 c = b.right().scale(i * r * 0.48);
            m.diamond(b, c, r * (0.24 + 0.18 * l), rot * (i == 0 ? -0.22 : 0.22) + i * 0.15, i == 0 ? 1.18F : 0.78F, 0.20F);
            if (f > 0.12) m.band(b, c, r * 0.28, r * 0.34, 30, 0.74F, 0.16F);
        }
    }

    private static void invisibilityBreak(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.brokenBand(b, Vec3.ZERO, r * 0.42, r * (0.56 + 0.18 * l), 52, 11, 0.72F, 0.15F);
        m.brokenBand(b, Vec3.ZERO, r * 0.68, r * (0.74 + 0.12 * l), 60, 13, 0.58F, 0.12F);
        if (f > 0.18) m.diamond(b, b.point(rot, r * 0.16), r * 0.22, -rot, 0.66F, 0.12F);
    }

    private static void windNozzle(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        Vec3 n = b.normal();
        for (int i = 0; i < 3; i++) {
            double z = -r * 0.18 + i * r * 0.18;
            double rr = r * (0.32 + i * 0.15 + l * 0.08);
            m.brokenBand(b, n.scale(z), rr * 0.78, rr, 42, 6 + i, 0.78F + i * 0.12F, 0.18F);
        }
        if (f > 0.08) m.cone(Vec3.ZERO, n, b, r * 0.72, r * 0.54, 7, 2, 0.56F);
    }

    private static void holdCross(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygon(b, Vec3.ZERO, r * (0.34 + l * 0.34), 4, Math.PI / 4, 0.92F);
        m.line(b.right().scale(-r * 0.72), b.right().scale(r * 0.72), 1.04F);
        m.line(b.up().scale(-r * 0.72), b.up().scale(r * 0.72), 1.04F);
        if (f > 0.10) for (int i = 0; i < 4; i++) m.diamond(b, b.point(rot + i * Math.PI / 2, r * 0.70), r * 0.10, -rot, 0.88F, 0.20F);
    }

    private static void fractureSeal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        int rays = 7;
        m.polygon(b, Vec3.ZERO, r * (0.28 + l * 0.30), 7, rot * 0.26, 0.76F);
        for (int i = 0; i < rays; i++) {
            double a = rot + Math.PI * 2.0 * i / rays;
            Vec3 mid = b.point(a + 0.10 * Math.sin(i * 2.1), r * 0.48);
            m.line(b.point(a, r * 0.20), mid, 0.68F);
            if (f > 0.06) m.line(mid, b.point(a + 0.18 * Math.cos(i), r * 0.94), i % 2 == 0 ? 1.02F : 0.62F);
        }
    }

    private static void blurOscillator(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        for (int i = -1; i <= 1; i++) {
            Vec3 c = b.right().scale(i * r * 0.12 * (0.4 + l));
            m.brokenBand(b, c, r * (0.44 + i * 0.03), r * (0.54 + i * 0.03), 46, 7 + Math.abs(i), i == 0 ? 1.02F : 0.60F, i == 0 ? 0.22F : 0.12F);
        }
        if (f > 0.10) m.runeChords(b, Vec3.ZERO, r * 0.64, 10, 3, rot * 1.8, 0.52F);
    }

    private static void levitationStack(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        for (int i = 0; i < 3; i++) {
            Vec3 c = new Vec3(0, i * r * 0.24, 0);
            m.band(b, c, r * (0.20 + i * 0.08), r * (0.26 + i * 0.08 + l * 0.04), 32, 0.72F + i * 0.16F, 0.18F);
        }
        if (f > 0.12) {
            Vec3 tip = new Vec3(0, r * 1.0, 0);
            m.line(Vec3.ZERO, tip, 0.84F);
            m.line(tip, b.right().scale(r * 0.16).add(new Vec3(0, r * 0.76, 0)), 0.72F);
            m.line(tip, b.right().scale(-r * 0.16).add(new Vec3(0, r * 0.76, 0)), 0.72F);
        }
    }

    private static void fireballReactor(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.orb(Vec3.ZERO, r * (0.08 + 0.12 * l), 18, 1.28F, 0.34F);
        m.star(b, Vec3.ZERO, r * (0.28 + 0.22 * l), r * 0.14, 6, rot, 1.12F);
        m.brokenBand(b, Vec3.ZERO, r * 0.58, r * (0.68 + 0.18 * l), 60, 6, 1.20F, 0.30F);
        if (f > 0.08) m.runeRing(b, Vec3.ZERO, r * 0.76, 15, r * 0.025, 0xF1A3, -rot * 0.86, 0.74F);
    }

    private static void lightningRail(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        Vec3 n = b.normal();
        for (int i = 0; i < 3; i++) {
            Vec3 c = n.scale((i - 1) * r * 0.16);
            m.polygon(b, c, r * (0.32 + i * 0.10 + l * 0.06), 6, rot * (i % 2 == 0 ? 0.42 : -0.48), 0.72F + i * 0.16F);
        }
        if (f > 0.05) {
            Vec3 prev = b.point(rot, r * 0.14);
            for (int i = 1; i <= 5; i++) {
                Vec3 next = b.point(rot + (i % 2 == 0 ? -0.22 : 0.22), r * (0.14 + i * 0.15));
                m.line(prev, next, i == 5 ? 1.34F : 0.88F); prev = next;
            }
        }
    }

    private static void flightWings(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.diamond(b, Vec3.ZERO, r * 0.26, rot * 0.18, 0.88F, 0.18F);
        for (int side : new int[]{-1, 1}) {
            Vec3 root = b.right().scale(side * r * 0.14);
            Vec3 tip = b.right().scale(side * r * (0.58 + 0.24 * l)).add(b.up().scale(r * 0.26));
            m.line(root, tip, 1.04F);
            m.line(root, b.right().scale(side * r * 0.66).add(b.up().scale(-r * 0.08)), 0.70F);
            if (f > 0.10) m.line(b.right().scale(side * r * 0.30), b.right().scale(side * r * 0.80).add(b.up().scale(r * 0.10)), 0.62F);
        }
    }

    private static void hasteClock(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.band(b, Vec3.ZERO, r * 0.46, r * (0.54 + 0.14 * l), 54, 1.08F, 0.22F);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            m.line(b.point(a, r * 0.40), b.point(a, r * 0.70), i % 2 == 0 ? 0.84F : 0.54F);
        }
        m.line(Vec3.ZERO, b.point(rot * 2.8, r * (0.36 + f * 0.22)), 1.18F);
        m.line(Vec3.ZERO, b.point(-rot * 1.7, r * 0.28), 0.82F);
    }

    private static void dispelLock(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.brokenBand(b, Vec3.ZERO, r * 0.42, r * (0.56 + l * 0.12), 50, 5, 1.02F, 0.22F);
        m.brokenBand(b, Vec3.ZERO, r * 0.62, r * (0.72 + l * 0.10), 56, 7, 0.72F, 0.16F);
        if (f > 0.08) {
            m.line(b.point(rot + Math.PI * 0.15, r * 0.18), b.point(rot + Math.PI * 1.15, r * 0.88), 1.32F);
            m.line(b.point(rot - Math.PI * 0.15, r * 0.18), b.point(rot - Math.PI * 1.15, r * 0.88), 0.70F);
        }
    }

    private static void vampiricTether(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        Vec3 left = b.right().scale(-r * 0.22), right = b.right().scale(r * 0.22);
        m.brokenBand(b, left, r * 0.24, r * (0.34 + 0.10 * l), 36, 5, 0.86F, 0.20F);
        m.brokenBand(b, right, r * 0.24, r * (0.34 + 0.10 * l), 36, 5, 0.86F, 0.20F);
        m.line(left, right, 1.18F);
        if (f > 0.08) m.star(b, Vec3.ZERO, r * 0.42, r * 0.12, 4, rot, 1.08F);
    }

    private static void slowClock(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.band(b, Vec3.ZERO, r * 0.48, r * (0.60 + l * 0.12), 56, 0.78F, 0.20F);
        for (int i = 0; i < 12; i++) m.line(b.point(i * Math.PI / 6, r * 0.46), b.point(i * Math.PI / 6, r * 0.66), i % 3 == 0 ? 0.86F : 0.48F);
        m.line(Vec3.ZERO, b.point(rot * 0.34, r * (0.24 + f * 0.12)), 1.12F);
        if (f > 0.10) m.brokenBand(b, Vec3.ZERO, r * 0.26, r * 0.32, 30, 4, 0.62F, 0.16F);
    }

    private static void energyWard(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygon(b, Vec3.ZERO, r * (0.34 + l * 0.38), 4, Math.PI / 4, 1.02F);
        for (int i = 0; i < 4; i++) {
            Vec3 node = b.point(rot + i * Math.PI / 2, r * 0.54);
            m.diamond(b, node, r * (0.10 + 0.05 * f), -rot + i, 0.86F, 0.18F);
            m.line(node.scale(0.46), node, 0.62F);
        }
        if (f > 0.10) m.band(b, Vec3.ZERO, r * 0.68, r * 0.74, 54, 0.86F, 0.18F);
    }

    private static void sleetArray(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.brokenBand(b, Vec3.ZERO, r * 0.58, r * (0.72 + l * 0.16), 70, 9, 0.82F, 0.18F);
        for (int i = 0; i < 6; i++) {
            double a = rot + i * Math.PI / 3;
            m.line(b.point(a, r * 0.18), b.point(a, r * 0.72), 0.64F);
            if (f > 0.06) m.line(b.point(a, r * 0.46), b.point(a + Math.PI / 8, r * 0.62), 0.48F);
        }
        for (int i = 0; i < 4; i++) m.band(b, b.point(rot * 0.2 + i * 1.57, r * 0.42), r * 0.07, r * 0.11, 18, 0.62F, 0.14F);
    }

    private static void blinkPair(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        Vec3 n = b.normal();
        m.brokenBand(b, n.scale(-r * 0.20), r * 0.48, r * (0.60 + 0.12 * l), 48, 6, 0.92F, 0.22F);
        m.brokenBand(b, n.scale(r * 0.20), r * 0.48, r * (0.60 + 0.12 * l), 48, 6, 1.10F, 0.24F);
        if (f > 0.10) for (int i = 0; i < 6; i++) m.line(b.point(rot + i * Math.PI / 3, r * 0.48).add(n.scale(-r * 0.20)), b.point(rot + 0.24 + i * Math.PI / 3, r * 0.48).add(n.scale(r * 0.20)), 0.58F);
    }

    private static void burningPalm(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.polygonPlate(b, Vec3.ZERO, r * 0.34, 5, rot, 0.94F, 0.18F);
        for (int i = 0; i < 5; i++) {
            double a = rot + (i - 2) * 0.24;
            Vec3 root = b.point(a, r * 0.22), tip = b.point(a, r * (0.54 + 0.32 * l));
            m.line(root, tip, i == 2 ? 1.18F : 0.78F);
            if (f > 0.12) m.diamond(b, tip, r * 0.08, -a, 0.84F, 0.18F);
        }
    }

    private static void iceKnife(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.diamond(b, Vec3.ZERO, r * (0.24 + l * 0.22), rot, 1.10F, 0.22F);
        m.line(b.up().scale(-r * 0.68), b.up().scale(r * (0.48 + f * 0.34)), 1.22F);
        for (int side : new int[]{-1, 1}) m.line(b.up().scale(r * 0.18), b.right().scale(side * r * 0.30).add(b.up().scale(-r * 0.10)), 0.68F);
        if (f > 0.10) m.brokenBand(b, Vec3.ZERO, r * 0.58, r * 0.68, 42, 6, 0.82F, 0.18F);
    }

    private static void chromaticCrown(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.orb(Vec3.ZERO, r * 0.10, 16, 1.20F, 0.30F);
        for (int i = 0; i < 7; i++) {
            double a = rot + i * Math.PI * 2.0 / 7.0;
            Vec3 n = b.point(a, r * (0.42 + l * 0.18));
            m.band(b, n, r * 0.07, r * 0.11, 18, 0.72F + (i % 3) * 0.12F, 0.18F);
            if (f > 0.08) m.line(n.scale(0.52), n, 0.56F);
        }
        if (f > 0.10) m.polygon(b, Vec3.ZERO, r * 0.72, 7, -rot * 0.44, 0.74F);
    }

    private static void windWallMatrix(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        for (int i = -2; i <= 2; i++) {
            Vec3 base = b.right().scale(i * r * 0.26);
            m.line(base.add(b.up().scale(-r * 0.42)), base.add(b.up().scale(r * (0.38 + l * 0.26))), i == 0 ? 1.04F : 0.60F);
        }
        for (int j = -1; j <= 1; j++) m.line(b.right().scale(-r * 0.68).add(b.up().scale(j * r * 0.28)), b.right().scale(r * 0.68).add(b.up().scale(j * r * 0.28)), 0.58F);
        if (f > 0.12) m.runeChords(b, Vec3.ZERO, r * 0.72, 10, 3, rot, 0.52F);
    }

    private static void counterSeal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.brokenBand(b, Vec3.ZERO, r * 0.44, r * (0.58 + l * 0.14), 52, 5, 1.06F, 0.24F);
        m.brokenBand(b, Vec3.ZERO, r * 0.64, r * (0.72 + l * 0.10), 58, 7, 0.68F, 0.15F);
        m.line(b.point(rot + 0.45, r * 0.16), b.point(rot + Math.PI + 0.45, r * 0.90), 1.34F);
        if (f > 0.08) m.diamond(b, Vec3.ZERO, r * 0.30, -rot * 1.4, 0.88F, 0.18F);
    }

    private static void steamChamber(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        Vec3 left = b.right().scale(-r * 0.28), right = b.right().scale(r * 0.28);
        m.band(b, left, r * 0.16, r * (0.24 + 0.10 * l), 28, 1.08F, 0.22F);
        m.band(b, right, r * 0.16, r * (0.24 + 0.10 * l), 28, 0.82F, 0.18F);
        m.line(left, Vec3.ZERO, 0.82F); m.line(right, Vec3.ZERO, 0.82F);
        if (f > 0.10) {
            m.orb(Vec3.ZERO, r * (0.10 + 0.08 * f), 16, 1.24F, 0.32F);
            m.brokenBand(b, Vec3.ZERO, r * 0.52, r * 0.66, 46, 8, 0.72F, 0.18F);
        }
    }

    private static void frostStepGate(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f) {
        m.diamond(b, Vec3.ZERO, r * (0.30 + 0.22 * l), rot * 0.30, 0.86F, 0.18F);
        m.brokenBand(b, Vec3.ZERO, r * 0.46, r * (0.58 + 0.18 * l), 50, 6, 1.02F, 0.22F);
        for (int i = 0; i < 6; i++) {
            double a = rot + i * Math.PI / 3;
            m.line(b.point(a, r * 0.22), b.point(a, r * (0.52 + 0.20 * f)), 0.62F);
        }
    }

    private static void formulaSeal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r, double rot, double l, double f, int seed) {
        int sides = 4 + Math.floorMod(seed, 5);
        m.polygon(b, Vec3.ZERO, r * (0.30 + l * 0.36), sides, rot, 0.86F);
        if (f > 0.08) m.runeChords(b, Vec3.ZERO, r * 0.68, 9 + sides, 3, -rot, 0.58F);
    }

    private static void missileRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double travel, double pf) {
        for (int i = 0; i < 3; i++) {
            double local = clamp((travel - i * 0.07) / 0.86, 0, 1);
            Vec3 pos = target.scale(local).add(b.point(age * 5.0 + i * 2.09, (1.0 - local) * 0.34));
            m.orb(pos, 0.13 * pf, 14, 1.24F, 0.42F);
            m.shard(pos, target.normalize(), b, 0.40 * pf, 0.07 * pf, 0.92F, 0.30F);
            m.band(b, pos, 0.18 * pf, 0.24 * pf, 20, 0.76F, 0.18F);
        }
        impact(m, b, target, age, 0.30 * pf, 6);
    }

    private static void fireBoltRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, Vec3 target, double age, double travel, double pf) {
        Vec3 pos = target.scale(travel);
        m.shard(pos, forward, b, 0.74 * pf, 0.13 * pf, 1.22F, 0.48F);
        m.orb(pos.add(forward.scale(0.10)), 0.16 * pf, 18, 1.28F, 0.40F);
        m.ribbon(pos.subtract(forward.scale(0.62 * pf)), forward, b, 0.70 * pf, 0.20 * pf, 2, 14, 0.94F, 0.20F);
        impact(m, b, target, age, 0.34 * pf, 5);
    }

    private static void frostRayRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double pf) {
        int segments = 10;
        Vec3 prev = Vec3.ZERO;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 next = target.scale(t).add(b.point(i * 1.73, Math.sin(Math.PI * t) * 0.045 * pf));
            m.line(prev, next, i % 3 == 0 ? 1.10F : 0.72F); prev = next;
        }
        for (int i = 0; i < 5; i++) m.shard(target.add(b.point(i * 1.26 + age, 0.24 * pf)), target.normalize(), b, 0.34 * pf, 0.055 * pf, 0.88F, 0.28F);
        impact(m, b, target, age, 0.28 * pf, 6);
    }

    private static void shieldRelease(ArcaneWorldMesh.Builder m, double pf, double age) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        m.sphere(new Vec3(0, 0.25, 0), 1.10 * pf, 7, 0.70F);
        m.polygonPlate(g, Vec3.ZERO, 0.82 * pf, 6, age * 0.7, 1.06F, 0.20F);
    }

    private static void featherRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double age, double pf) {
        for (int side : new int[]{-1, 1}) for (int i = 0; i < 4; i++) {
            Vec3 a = g.right().scale(side * (0.12 + i * 0.11) * pf).add(0, 0.16 + i * 0.18, 0);
            Vec3 z = g.right().scale(side * (0.38 + i * 0.10) * pf).add(0, 0.28 + i * 0.22 + Math.sin(age * 7 + i) * 0.04, 0);
            m.line(a, z, 0.56F);
        }
        m.brokenBand(g, Vec3.ZERO, 0.42 * pf, 0.54 * pf, 34, 8, 0.68F, 0.16F);
    }

    private static void lightRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double age, double pf) {
        m.orb(new Vec3(0, 0.3, 0), 0.24 * pf, 22, 1.30F, 0.48F);
        for (int i = 0; i < 8; i++) m.line(b.point(age + i * Math.PI / 4, 0.30 * pf), b.point(age + i * Math.PI / 4, 0.60 * pf), 0.52F);
    }

    private static void greaseRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, double age, double pf) {
        double grow = 0.7 + Math.sin(Math.min(1, age) * Math.PI) * 0.35;
        m.brokenBand(g, target, 1.15 * pf * grow, 1.55 * pf * grow, 58, 9, 0.64F, 0.18F);
        for (int i = 0; i < 5; i++) m.band(g, target.add(g.point(i * 1.7, 0.72 * pf)), 0.12 * pf, 0.22 * pf, 18, 0.58F, 0.14F);
    }

    private static void sleepRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, double age, double pf) {
        for (int i = 0; i < 4; i++) {
            Vec3 c = target.add(0, 0.22 + i * 0.34 + age * 0.22, 0).add(g.point(age * 1.4 + i * 1.8, 0.18 + i * 0.08));
            m.brokenBand(g, c, 0.18 * pf, 0.27 * pf, 24, 6, 0.64F, 0.15F);
        }
    }

    private static void thunderRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, double age, double pf) {
        double x = Math.min(1, age * 2.1);
        for (int ring = 0; ring < 4; ring++) {
            Vec3 c = forward.scale((0.5 + ring * 0.85) * x * pf);
            double r = (0.34 + ring * 0.23) * x * pf;
            m.brokenBand(b, c, r * 0.78, r, 42, 5 + ring, 1.12F - ring * 0.10F, 0.24F);
        }
        m.cone(Vec3.ZERO, forward, b, 3.4 * x * pf, 1.20 * x * pf, 8, 3, 0.62F);
    }

    private static void armorRelease(ArcaneWorldMesh.Builder m, double age, double pf) {
        m.sphere(new Vec3(0, 0.22, 0), 1.16 * pf, 8, 0.62F);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        m.polygon(g, Vec3.ZERO, 0.90 * pf, 8, age * 0.34, 0.70F);
        m.diamond(g, Vec3.ZERO, 0.56 * pf, -age * 0.48, 0.82F, 0.18F);
    }

    private static void scorchingRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double pf) {
        for (int i = -1; i <= 1; i++) {
            Vec3 start = b.right().scale(i * 0.22 * pf);
            Vec3 end = target.add(b.right().scale(i * 0.36 * pf));
            m.line(start, end, i == 0 ? 1.18F : 0.90F);
            m.band(b, end, 0.16 * pf, 0.24 * pf, 20, 1.08F, 0.20F);
        }
        impact(m, b, target, age, 0.38 * pf, 7);
    }

    private static void mistRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, double age, double pf) {
        for (int i = 0; i < 5; i++) {
            Vec3 c = forward.scale((i - 2) * 0.25).add(b.point(age * 2.2 + i * 1.2, 0.30 + i * 0.06));
            m.brokenBand(b, c, 0.18 * pf, 0.28 * pf, 24, 7, 0.56F, 0.13F);
        }
        m.band(b, Vec3.ZERO, 0.60 * pf, 0.76 * pf, 42, 0.88F, 0.18F);
    }

    private static void webRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, double age, double pf) {
        int spokes = 10;
        double r = (1.1 + Math.min(1, age * 1.7) * 1.2) * pf;
        for (int i = 0; i < spokes; i++) m.line(target, target.add(g.point(i * Math.PI * 2 / spokes, r)), 0.54F);
        for (int j = 1; j <= 4; j++) m.brokenBand(g, target, r * j / 5.0, r * j / 5.0 + 0.04, 50, 10 - j, 0.52F, 0.13F);
    }

    private static void mirrorRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double age, double pf) {
        for (int i = -1; i <= 1; i++) {
            Vec3 c = b.right().scale(i * 0.62 * pf).add(b.up().scale(Math.sin(age * 5 + i) * 0.08));
            m.diamond(b, c, 0.48 * pf, age * (i == 0 ? -0.6 : 0.6), i == 0 ? 1.10F : 0.66F, 0.16F);
        }
    }

    private static void invisibilityRelease(ArcaneWorldMesh.Builder m, double age, double pf) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        for (int i = 0; i < 4; i++) m.brokenBand(g, new Vec3(0, i * 0.42, 0), (0.42 + i * 0.06) * pf, (0.52 + i * 0.06) * pf, 40, 11 + i, 0.48F, (float) (0.15 * (1 - age * 0.55)));
    }

    private static void gustRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, double age, double pf) {
        double x = Math.min(1, age * 1.8);
        m.cone(Vec3.ZERO, forward, b, 5.2 * x * pf, 1.5 * x * pf, 9, 4, 0.54F);
        for (int i = 0; i < 3; i++) m.ribbon(forward.scale(i * 0.42), forward, b, 4.6 * x * pf, 0.34 * pf, 3 + i, 28, 0.64F, 0.13F);
    }

    private static void holdRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, double age, double pf) {
        double h = 2.0 * pf;
        m.polygon(g, target, 0.72 * pf, 4, Math.PI / 4, 0.82F);
        for (int i = 0; i < 4; i++) {
            Vec3 base = target.add(g.point(Math.PI / 4 + i * Math.PI / 2, 0.72 * pf));
            m.line(base, base.add(0, h * Math.min(1, age * 2), 0), 0.78F);
        }
        if (age > 0.35) m.polygon(g, target.add(0, h, 0), 0.72 * pf, 4, Math.PI / 4, 0.70F);
    }

    private static void shatterRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double pf) {
        double grow = Math.min(1, age * 2.0);
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 9;
            Vec3 mid = target.add(b.point(a + 0.10 * Math.sin(i), 0.46 * grow * pf));
            m.line(target, mid, 0.70F);
            m.line(mid, target.add(b.point(a + 0.18 * Math.cos(i), 1.05 * grow * pf)), i % 3 == 0 ? 1.12F : 0.62F);
        }
        impact(m, b, target, age, 0.46 * pf, 7);
    }

    private static void blurRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double age, double pf) {
        for (int i = -2; i <= 2; i++) {
            Vec3 c = b.right().scale(i * 0.10 * pf * Math.sin(age * 8 + i));
            m.brokenBand(b, c, 0.46 * pf, 0.58 * pf, 42, 8 + Math.abs(i), i == 0 ? 0.88F : 0.44F, i == 0 ? 0.18F : 0.10F);
        }
    }

    private static void levitateRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double age, double pf) {
        for (int i = 0; i < 4; i++) m.band(g, new Vec3(0, i * 0.42 + age * 0.10, 0), (0.24 + i * 0.08) * pf, (0.31 + i * 0.08) * pf, 30, 0.64F + i * 0.08F, 0.14F);
        m.line(Vec3.ZERO, new Vec3(0, 1.75 * pf, 0), 0.64F);
    }

    private static void fireballRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, Vec3 target, double age, double travel, double pf) {
        Vec3 pos = target.scale(travel);
        double core = 0.34 * pf * (0.85 + travel * 0.22);
        m.orb(pos, core, 26, 1.28F, 0.48F);
        m.starPlate(b, pos, core * 1.55, core * 0.62, 6, age * 3.4, 1.08F, 0.28F);
        m.ribbon(pos.subtract(forward.scale(0.9 * pf)), forward, b, 1.0 * pf, 0.40 * pf, 3, 22, 1.02F, 0.20F);
        if (age > 0.62) {
            double burst = clamp((age - 0.62) / 0.38, 0, 1);
            m.orb(target, (0.42 + burst * 1.45) * pf, 30, 1.24F, (float) (0.38 * (1 - burst)));
            m.brokenBand(b, target, (0.52 + burst) * pf, (0.70 + burst * 1.35) * pf, 58, 7, 1.18F, (float) (0.32 * (1 - burst)));
        }
    }

    private static void lightningRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, Vec3 target, double age, double pf) {
        int segments = 16;
        Vec3 prev = Vec3.ZERO;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            double jitter = (i == segments ? 0 : Math.sin(i * 8.17 + age * 23) * 0.12 * pf);
            Vec3 next = target.scale(t).add(b.point(i * 2.31, jitter));
            m.line(prev, next, i % 4 == 0 ? 1.36F : 0.88F); prev = next;
        }
        impact(m, b, target, age, 0.48 * pf, 8);
    }

    private static void flyRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double age, double pf) {
        for (int side : new int[]{-1, 1}) {
            Vec3 root = b.right().scale(side * 0.18 * pf).add(0, 0.45, 0);
            Vec3 tip = b.right().scale(side * 0.92 * pf).add(b.up().scale(0.34 * pf + Math.sin(age * 6) * 0.08));
            m.line(root, tip, 1.04F);
            m.line(root, b.right().scale(side * 0.78 * pf).add(b.up().scale(-0.12 * pf)), 0.64F);
        }
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        m.brokenBand(g, Vec3.ZERO, 0.58 * pf, 0.70 * pf, 42, 8, 0.66F, 0.14F);
    }

    private static void hasteRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double age, double pf) {
        m.band(b, Vec3.ZERO, 0.48 * pf, 0.58 * pf, 42, 0.88F, 0.18F);
        for (int i = 0; i < 8; i++) m.line(b.point(i * Math.PI / 4, 0.42 * pf), b.point(i * Math.PI / 4, 0.68 * pf), 0.52F);
        m.line(Vec3.ZERO, b.point(age * 8.4, 0.42 * pf), 1.18F);
        m.line(Vec3.ZERO, b.point(-age * 5.2, 0.28 * pf), 0.76F);
    }

    private static void dispelRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double pf) {
        double shrink = Math.max(0.08, 1 - age * 0.84);
        m.brokenBand(b, target, 0.62 * pf * shrink, 0.82 * pf * shrink, 54, 7, 1.06F, 0.22F);
        m.line(target.add(b.point(0.45, 0.72 * pf)), target.add(b.point(Math.PI + 0.45, 0.72 * pf)), 1.28F);
    }

    private static void vampiricRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double pf) {
        int segments = 14;
        Vec3 prev = target;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 next = target.scale(1 - t).add(b.point(age * 3 + i, Math.sin(Math.PI * t) * 0.16 * pf));
            m.line(prev, next, i % 3 == 0 ? 1.02F : 0.64F); prev = next;
        }
        m.orb(target, 0.24 * pf, 20, 0.92F, 0.26F);
        m.orb(Vec3.ZERO, 0.18 * pf * (0.8 + age * 0.5), 18, 1.18F, 0.28F);
    }

    private static void slowRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, double age, double pf) {
        m.band(g, target, 0.72 * pf, 0.86 * pf, 50, 0.74F, 0.18F);
        for (int i = 0; i < 12; i++) m.line(target.add(g.point(i * Math.PI / 6, 0.68 * pf)), target.add(g.point(i * Math.PI / 6, 0.92 * pf)), 0.44F);
        m.line(target, target.add(g.point(age * 0.55, 0.58 * pf)), 1.00F);
    }

    private static void energyWardRelease(ArcaneWorldMesh.Builder m, double age, double pf) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        m.sphere(new Vec3(0, 0.30, 0), 1.34 * pf, 9, 0.56F);
        for (int i = 0; i < 4; i++) m.diamond(g, g.point(age * 0.8 + i * Math.PI / 2, 0.88 * pf), 0.20 * pf, -age + i, 0.72F, 0.14F);
    }

    private static void sleetRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, double age, double pf) {
        double radius = 2.4 * pf;
        m.brokenBand(g, target, radius * 0.72, radius, 70, 10, 0.66F, 0.14F);
        for (int i = 0; i < 7; i++) {
            double a = i * 2.31 + age * 1.3;
            Vec3 top = target.add(g.point(a, radius * (0.25 + (i % 3) * 0.18))).add(0, 2.0 + (i % 2) * 0.8, 0);
            Vec3 bottom = new Vec3(top.x, target.y + 0.05, top.z);
            m.line(top, bottom, i % 3 == 0 ? 0.82F : 0.50F);
            m.shard(bottom, new Vec3(0, -1, 0), ArcaneWorldMesh.Basis.facing(new Vec3(0, -1, 0)), 0.26 * pf, 0.045 * pf, 0.72F, 0.18F);
        }
    }

    private static void blinkRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, double age, double pf) {
        Vec3 n = forward;
        m.brokenBand(b, n.scale(-0.18), 0.50 * pf, 0.66 * pf, 42, 8, 0.88F, (float) (0.20 * (1 - age * 0.5)));
        m.brokenBand(b, n.scale(0.36), 0.50 * pf, 0.66 * pf, 42, 8, 1.10F, (float) (0.24 * (1 - age * 0.4)));
        for (int i = 0; i < 5; i++) m.line(b.point(age * 3 + i * 1.26, 0.44 * pf).add(n.scale(-0.18)), b.point(age * 3 + 0.25 + i * 1.26, 0.44 * pf).add(n.scale(0.36)), 0.50F);
    }

    private static void burningHandsRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, double age, double pf) {
        double x = Math.min(1, age * 2.2);
        for (int i = -2; i <= 2; i++) {
            Vec3 axis = forward.add(b.right().scale(i * 0.12)).normalize();
            m.ribbon(axis.scale(0.18), axis, b, 2.8 * x * pf, (0.24 + 0.05 * Math.abs(i)) * pf, 2 + Math.abs(i) % 2, 20, i == 0 ? 1.18F : 0.86F, 0.20F);
        }
        m.cone(Vec3.ZERO, forward, b, 2.6 * x * pf, 1.25 * x * pf, 10, 3, 0.54F);
    }

    private static void iceKnifeRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, Vec3 target, double age, double travel, double pf) {
        Vec3 pos = target.scale(travel);
        m.shard(pos, forward, b, 0.92 * pf, 0.16 * pf, 1.18F, 0.52F);
        for (int i = 0; i < 3; i++) m.shard(pos.add(b.point(age * 4 + i * 2.09, 0.20 * pf)), forward, b, 0.34 * pf, 0.05 * pf, 0.76F, 0.20F);
        if (age > 0.65) for (int i = 0; i < 7; i++) m.shard(target, b.point(i * Math.PI * 2 / 7, 1).add(new Vec3(0, 0.18, 0)), b, (0.26 + (age - 0.65) * 0.8) * pf, 0.055 * pf, 0.90F, 0.24F);
    }

    private static void chromaticRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, Vec3 target, double age, double travel, double pf) {
        Vec3 pos = target.scale(travel);
        m.orb(pos, 0.30 * pf, 24, 1.20F, 0.42F);
        for (int i = 0; i < 7; i++) {
            Vec3 node = pos.add(b.point(age * 4.6 + i * Math.PI * 2 / 7, 0.42 * pf));
            m.orb(node, 0.07 * pf, 10, 0.76F + (i % 3) * 0.12F, 0.20F);
            m.line(pos, node, 0.46F);
        }
        m.ribbon(pos.subtract(forward.scale(0.62 * pf)), forward, b, 0.7 * pf, 0.24 * pf, 4, 20, 0.78F, 0.16F);
        impact(m, b, target, age, 0.42 * pf, 7);
    }

    private static void windWallRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 target, Vec3 direction, double age, double pf) {
        Vec3 f = new Vec3(direction.x, 0, direction.z); if (f.lengthSqr() < 1.0E-8) f = new Vec3(0, 0, 1); f = f.normalize();
        Vec3 right = new Vec3(-f.z, 0, f.x);
        for (int i = -4; i <= 4; i++) {
            Vec3 base = target.add(right.scale(i * 0.48 * pf));
            m.ribbon(base, new Vec3(0, 1, 0), ArcaneWorldMesh.Basis.facing(new Vec3(0, 1, 0)), 2.8 * pf, 0.20 * pf, 3 + Math.abs(i) % 2, 22, i == 0 ? 0.92F : 0.58F, 0.14F);
        }
    }

    private static void counterRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double pf) {
        double collapse = Math.max(0.06, 1 - age * 1.1);
        m.brokenBand(b, target, 0.58 * pf * collapse, 0.78 * pf * collapse, 48, 5, 1.16F, 0.24F);
        m.line(target.add(b.point(0.4, 0.70 * pf)), target.add(b.point(Math.PI + 0.4, 0.70 * pf)), 1.34F);
        if (age > 0.45) m.orb(target, 0.18 * pf * collapse, 16, 0.92F, 0.18F);
    }

    private static void steamRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 forward, double age, double pf) {
        double grow = Math.min(1, age * 2.0);
        m.orb(forward.scale(0.8 * grow), (0.28 + grow * 0.56) * pf, 28, 0.92F, (float) (0.28 * (1 - age * 0.45)));
        for (int i = 0; i < 5; i++) m.ribbon(forward.scale(0.35 * grow).add(b.point(age * 4 + i * 1.26, 0.16 * pf)), forward, b, 1.6 * grow * pf, 0.24 * pf, 3 + i % 2, 18, 0.66F, 0.13F);
    }

    private static void frostStepRelease(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, ArcaneWorldMesh.Basis face, Vec3 forward, double age, double pf) {
        m.brokenBand(face, forward.scale(0.28), 0.52 * pf, 0.68 * pf, 44, 6, 0.94F, 0.22F);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + age * 0.8;
            Vec3 tip = g.point(a, (0.48 + age * 1.2) * pf);
            m.line(Vec3.ZERO, tip, 0.58F);
            m.line(tip.scale(0.70), g.point(a + Math.PI / 9, (0.58 + age) * pf), 0.46F);
        }
    }

    private static void impact(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 target, double age, double base, int sides) {
        if (age < 0.62) return;
        double t = clamp((age - 0.62) / 0.38, 0, 1);
        double r = base * (0.7 + t * 2.0);
        m.polygon(b, target, r, sides, age * 2.0, 0.82F);
        m.brokenBand(b, target, r * 0.86, r * 1.18, 38, 5 + sides % 3, 0.78F, (float) (0.24 * (1 - t)));
    }

    private static double phase(double value, double start, double end) {
        return clamp((value - start) / Math.max(0.0001, end - start), 0, 1);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Vec3 safe(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0, 0, 1) : value.normalize();
    }
}