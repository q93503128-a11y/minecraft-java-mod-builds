package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Authored casting-circle layer. Every spell owns a readable magical formula while the physical
 * spell body remains the responsibility of SpellCinematicDirector. Size follows the presentation
 * profile rather than circle rank alone: Power Word Kill stays compact; Meteor Swarm owns the sky.
 */
final class ArcaneSigilDirector {
    private static final int BUDGET = 1400;

    private ArcaneSigilDirector() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                  double range, double progress, boolean fusion, long startedAtNanos) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(BUDGET);
        double p = smooth(clamp(progress, 0.0, 1.0));
        double time = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        double breath = 1.0 + Math.sin(time * (1.25 + profile.complexity() * .12)) * .025 * p;
        double rangeScale = sigilRangeScale(spell, profile, range);
        double radius = Math.max(.42, profile.radius()) * rangeScale * (.58 + .42 * p) * breath * (fusion ? 1.10 : 1.0);
        int seed = spell.id().hashCode();
        double rotation = time * (.11 + profile.complexity() * .018) + seed * .00031;
        ArcaneWorldMesh.Basis primary = primaryBasis(profile.sigil(), direction);

        if ("meteor_swarm".equals(spell.id())) {
            meteorRitual(mesh, primary, radius, p, rotation, seed);
            if (fusion) fusionFormula(mesh, primary, radius, p, rotation, seed);
            return mesh.build();
        }
        formulaFrame(mesh, spell, profile, primary, radius, p, rotation, seed);
        schoolFormula(mesh, spell, primary, radius, p, rotation, seed);
        anchorFormula(mesh, spell, profile, primary, direction, targetOffset, radius, p, rotation, seed);
        if (fusion) fusionFormula(mesh, primary, radius, p, rotation, seed);
        return mesh.build();
    }

    static ArcaneWorldMesh releaseEcho(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                       double range, double age, boolean fusion, long startedAtNanos) {
        if (age >= .30) return ArcaneWorldMesh.builder(8).build();
        return charge(spell, direction, targetOffset, range, 1.0, fusion, startedAtNanos);
    }

    static int releaseEchoColor(int argb, double age) {
        double fade = clamp(1.0 - age / .30, 0.0, 1.0);
        int a = (int) Math.round(210.0 * fade);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static void formulaFrame(ArcaneWorldMesh.Builder m, SpellDefinition spell,
                                     SpellPresentationProfile.Profile profile, ArcaneWorldMesh.Basis basis,
                                     double r, double p, double rotation, int seed) {
        int detail = profile.complexity();
        double inner = r * (.71 + .02 * Math.sin(rotation * 1.7));
        m.brokenBand(basis, Vec3.ZERO, r * .91, r, 56 + detail * 10,
                Math.max(3, 9 - detail), 1.18F, .16F + (float) p * .12F);
        m.circle(basis, Vec3.ZERO, inner, 44 + detail * 8, 1.04F);
        if (p > .12) {
            int sides = switch (spell.school()) {
                case FIRE -> 3;
                case FROST -> 6;
                case WIND -> 5;
                case WARD -> 8;
                case LIFE -> 7;
                case SPACE -> 4;
                case ARCANE -> 6;
            };
            m.polygon(basis, Vec3.ZERO, r * .58, sides, rotation * .64, 1.04F);
            m.polygon(basis, Vec3.ZERO, r * .45, sides, -rotation * .41 + .31, .72F);
        }
        if (p > .28) {
            int runeCount = 5 + detail * 2;
            m.runeRing(basis, Vec3.ZERO, r * .79, runeCount,
                    r * (.055 + detail * .003), seed, -rotation * .33, .74F);
        }
        if (p > .46) {
            m.runeChords(basis, Vec3.ZERO, r * .39, 6 + detail * 2,
                    2 + Math.floorMod(seed, 3), rotation * .24, .64F);
            m.runeGlyph(basis, Vec3.ZERO, r * .18, seed ^ 0x51A7, rotation * .52, 1.18F);
        }
        if (p > .72 && detail >= 4) {
            Vec3 normal = basis.normal();
            double depth = Math.min(r * .12, .42 + detail * .04);
            Vec3 a = normal.scale(depth), b = normal.scale(-depth);
            m.brokenBand(basis, a, r * .49, r * .54, 42 + detail * 5, 5, .92F, .10F);
            m.brokenBand(basis, b, r * .31, r * .35, 36 + detail * 4, 4, .88F, .10F);
        }
    }

    private static void schoolFormula(ArcaneWorldMesh.Builder m, SpellDefinition spell,
                                      ArcaneWorldMesh.Basis b, double r, double p,
                                      double rotation, int seed) {
        if (p < .18) return;
        switch (spell.school()) {
            case FIRE -> {
                m.star(b, Vec3.ZERO, r * .52, r * .18, 3, rotation * .80, 1.16F);
                for (int i = 0; i < 3; i++) {
                    double a = rotation * .34 + i * Math.PI * 2.0 / 3.0;
                    Vec3 c = b.point(a, r * .65);
                    m.diamond(b, c, r * .075, a, 1.18F, .23F);
                }
            }
            case FROST -> {
                m.polygon(b, Vec3.ZERO, r * .52, 6, rotation * .22, 1.08F);
                for (int i = 0; i < 6; i++) {
                    double a = rotation * .15 + i * Math.PI / 3.0;
                    Vec3 a0 = b.point(a, r * .12), a1 = b.point(a, r * .56);
                    m.line(a0, a1, i % 2 == 0 ? 1.02F : .66F);
                }
            }
            case WIND -> {
                for (int i = 0; i < 3; i++) {
                    double rr = r * (.30 + i * .11);
                    m.arc(b, Vec3.ZERO, rr, rotation * (.25 + i * .05) + i * 2.05,
                            Math.PI * 1.22, 22 + i * 5, i == 1 ? 1.05F : .70F);
                }
                m.runeGlyph(b, b.point(rotation, r * .24), r * .12, seed + 17, rotation, .82F);
            }
            case WARD -> {
                m.polygon(b, Vec3.ZERO, r * .55, 4, rotation * .16 + Math.PI / 4.0, 1.20F);
                m.polygon(b, Vec3.ZERO, r * .47, 8, -rotation * .12, .78F);
                m.runeChords(b, Vec3.ZERO, r * .31, 8, 3, rotation * .09, .72F);
            }
            case LIFE -> {
                int petals = 6;
                for (int i = 0; i < petals; i++) {
                    double a = rotation * .12 + i * Math.PI * 2.0 / petals;
                    Vec3 c = b.point(a, r * .32);
                    m.circle(b, c, r * .18, 16, i % 2 == 0 ? .92F : .68F);
                }
                m.star(b, Vec3.ZERO, r * .38, r * .20, 6, -rotation * .14, .78F);
            }
            case SPACE -> {
                m.polygon(b, Vec3.ZERO, r * .53, 4, rotation * .31 + Math.PI / 4.0, 1.16F);
                m.polygon(b, Vec3.ZERO, r * .39, 4, -rotation * .27, .88F);
                m.brokenBand(b, Vec3.ZERO, r * .22, r * .27, 32, 3, 1.08F, .18F);
            }
            case ARCANE -> {
                m.polygon(b, Vec3.ZERO, r * .53, 6, rotation * .20, 1.04F);
                m.runeChords(b, Vec3.ZERO, r * .48, 9, 4, -rotation * .18, .72F);
                m.diamond(b, Vec3.ZERO, r * .15, rotation * .55, 1.18F, .22F);
            }
        }
    }

    private static void anchorFormula(ArcaneWorldMesh.Builder m, SpellDefinition spell,
                                      SpellPresentationProfile.Profile profile, ArcaneWorldMesh.Basis b,
                                      Vec3 direction, Vec3 targetOffset, double r, double p,
                                      double rotation, int seed) {
        if (p < .34) return;
        switch (profile.sigil()) {
            case FRONT_COMPACT -> frontSatellites(m, b, r, profile.satellites(), p, rotation, seed, .68);
            case FRONT_LANCE -> {
                frontSatellites(m, b, r, Math.max(2, profile.satellites()), p, rotation, seed, .78);
                Vec3 n = b.normal();
                m.circle(b, n.scale(r * .16), r * .28, 28, .92F);
                m.circle(b, n.scale(r * .31), r * .16, 22, .68F);
            }
            case GROUND_SEAL -> groundSatellites(m, b, r, profile.satellites(), rotation, seed);
            case QUAD_ARRAY -> quadArray(m, b, r, rotation, seed);
            case TARGET_SEAL -> targetLock(m, b, r, rotation, seed);
            case BODY_HALO -> bodyHalo(m, direction, r, rotation, seed);
            case FEET_RUNE -> groundSatellites(m, b, r, Math.max(3, profile.satellites()), rotation, seed);
            case SKY_RITUAL -> skyRitual(m, spell, b, r, profile.satellites(), rotation, seed);
            case WALL_MATRIX -> wallMatrix(m, b, r, rotation, seed);
            case PORTAL_GATE -> portalDepth(m, b, r, rotation, seed);
        }
    }

    private static void frontSatellites(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                        int requested, double p, double rotation, int seed, double orbit) {
        int n = Math.max(2, Math.min(6, requested <= 0 ? 2 : requested));
        for (int i = 0; i < n; i++) {
            double a = rotation * (i % 2 == 0 ? .42 : -.31) + i * Math.PI * 2.0 / n;
            Vec3 c = b.point(a, r * orbit);
            double sr = r * (.11 + (i % 3) * .018) * (.72 + .28 * p);
            m.circle(b, c, sr, 18 + i * 2, i % 2 == 0 ? .96F : .68F);
            m.runeGlyph(b, c, sr * .62, seed + i * 31, -a, .62F);
        }
    }

    private static void groundSatellites(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                         int requested, double rotation, int seed) {
        int n = Math.max(4, Math.min(10, requested <= 0 ? 4 : requested));
        for (int i = 0; i < n; i++) {
            double a = rotation * .10 + i * Math.PI * 2.0 / n;
            Vec3 c = b.point(a, r * .72);
            m.polygon(b, c, r * .075, 3 + i % 4, -a, i % 3 == 0 ? .96F : .62F);
        }
    }

    private static void quadArray(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                  double r, double rotation, int seed) {
        double orbit = r * .58;
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4.0 + i * Math.PI / 2.0;
            Vec3 c = b.point(a, orbit);
            double sr = r * .22;
            m.brokenBand(b, c, sr * .76, sr, 30, 4, 1.08F, .16F);
            m.runeGlyph(b, c, sr * .48, seed + i * 73, rotation * (i % 2 == 0 ? .3 : -.3), .78F);
            m.line(c, Vec3.ZERO, i % 2 == 0 ? .78F : .56F);
        }
    }

    private static void targetLock(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                   double r, double rotation, int seed) {
        ArcaneWorldMesh.Basis cross = ArcaneWorldMesh.Basis.fromNormal(b.right(), b.up());
        m.brokenBand(cross, Vec3.ZERO, r * .43, r * .48, 34, 4, .92F, .12F);
        m.polygon(b, Vec3.ZERO, r * .31, 4, rotation * .22 + Math.PI / 4.0, 1.02F);
        m.runeGlyph(b, Vec3.ZERO, r * .15, seed ^ 0xB10C, -rotation * .28, 1.04F);
    }

    private static void bodyHalo(ArcaneWorldMesh.Builder m, Vec3 direction,
                                 double r, double rotation, int seed) {
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(direction);
        m.brokenBand(ground, Vec3.ZERO, r * .72, r * .80, 52, 6, .98F, .12F);
        m.circle(facing, Vec3.ZERO, r * .55, 42, .92F);
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.fromNormal(facing.right(), new Vec3(0,1,0));
        m.circle(side, Vec3.ZERO, r * .47, 38, .70F);
        m.runeGlyph(facing, Vec3.ZERO, r * .18, seed, rotation * .22, 1.02F);
    }

    private static double sigilRangeScale(SpellDefinition spell, SpellPresentationProfile.Profile profile, double range) {
        double base=Math.max(1.0,spell.range());
        double ratio=clamp(Math.max(.1,range)/base,.45,4.0);
        double exponent=switch(profile.sigil()){
            case SKY_RITUAL -> .52;
            case GROUND_SEAL, QUAD_ARRAY, WALL_MATRIX -> .44;
            case PORTAL_GATE -> .30;
            case TARGET_SEAL -> .20;
            case FRONT_COMPACT, FRONT_LANCE -> .12;
            case BODY_HALO, FEET_RUNE -> .08;
        };
        double max=profile.sigil()==SpellPresentationProfile.SigilStyle.SKY_RITUAL?1.78:1.52;
        return clamp(Math.pow(ratio,exponent),.78,max);
    }

    private static void meteorRitual(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                     double r, double p, double rotation, int seed) {
        double outer=r*(.78+.22*p),inner=outer*.72;
        m.circle(b,Vec3.ZERO,outer,96,1.34F);
        m.circle(b,Vec3.ZERO,outer*.89,84,.66F);
        m.circle(b,b.normal().scale(r*.045),inner,72,.78F);
        m.runeRing(b,Vec3.ZERO,outer*.94,12,r*.050,seed,-rotation*.18,.72F);
        m.star(b,Vec3.ZERO,inner*.58,inner*.34,8,rotation*.16,1.04F);
        m.polygon(b,Vec3.ZERO,inner*.42,4,-rotation*.12+Math.PI/4.0,.72F);
        m.circle(b,Vec3.ZERO,inner*.23,40,1.18F);
        double orbit=outer*.63,child=outer*.145;
        Vec3 down=b.normal().scale(Math.max(1.8,r*.11));
        for(int i=0;i<4;i++){
            double a=Math.PI/4.0+i*Math.PI/2.0+rotation*.035;
            Vec3 c=b.point(a,orbit),rail=b.point(a,inner*.72);
            m.line(rail,c,i%2==0?1.12F:.78F);
            m.circle(b,c,child,34,1.22F);
            m.polygon(b,c,child*.68,4,-a+Math.PI/4.0,.86F);
            m.runeGlyph(b,c,child*.42,seed+i*101,-rotation*.20,.82F);
            m.line(c,c.add(down),1.22F);
            if(p>.72)m.circle(b,c.add(down.scale(.42)),child*.58,24,.64F);
        }
        if(p>.82){
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2.0+rotation*.08;
                m.arc(b,Vec3.ZERO,outer*1.07,a,Math.PI*.34,20,i==0?1.08F:.66F);
            }
        }
    }

    private static void skyRitual(ArcaneWorldMesh.Builder m, SpellDefinition spell,
                                  ArcaneWorldMesh.Basis b, double r, int satellites,
                                  double rotation, int seed) {
        int n = "meteor_swarm".equals(spell.id()) ? 4 : Math.max(3, Math.min(8, satellites <= 0 ? 4 : satellites));
        double orbit = "meteor_swarm".equals(spell.id()) ? r * .58 : r * .66;
        double child = r * ("meteor_swarm".equals(spell.id()) ? .15 : .105);
        for (int i = 0; i < n; i++) {
            double a = Math.PI / 4.0 + i * Math.PI * 2.0 / n + rotation * .06;
            Vec3 c = b.point(a, orbit);
            m.brokenBand(b, c, child * .76, child, 28, 4, 1.16F, .16F);
            m.runeGlyph(b, c, child * .46, seed + i * 101, -rotation * .22, .76F);
            m.line(c, b.point(a, r * .38), i % 2 == 0 ? .78F : .55F);
        }
        if ("meteor_swarm".equals(spell.id())) {
            m.runeChords(b, Vec3.ZERO, r * .34, 12, 5, rotation * .10, 1.04F);
        }
    }

    private static void wallMatrix(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                   double r, double rotation, int seed) {
        int columns = 5;
        for (int i = -columns; i <= columns; i++) {
            double x = i * r * .10;
            Vec3 c = b.right().scale(x);
            m.line(c.add(b.up().scale(-r * .42)), c.add(b.up().scale(r * .42)), i % 2 == 0 ? .76F : .48F);
        }
        for (int i = -2; i <= 2; i++) {
            Vec3 c = b.up().scale(i * r * .16);
            m.line(c.add(b.right().scale(-r * .52)), c.add(b.right().scale(r * .52)), i == 0 ? .82F : .48F);
        }
        m.runeGlyph(b, Vec3.ZERO, r * .19, seed, rotation * .20, 1.0F);
    }

    private static void portalDepth(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                    double r, double rotation, int seed) {
        Vec3 n = b.normal();
        for (int i = -2; i <= 2; i++) {
            double d = i * r * .055;
            double rr = r * (1.0 - Math.abs(i) * .055);
            m.circle(b, n.scale(d), rr, 48, i == 0 ? 1.12F : .66F);
        }
        m.runeChords(b, Vec3.ZERO, r * .55, 10, 3, -rotation * .14, .72F);
    }

    private static void fusionFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                      double r, double p, double rotation, int seed) {
        if (p < .22) return;
        m.brokenBand(b, Vec3.ZERO, r * 1.08, r * 1.15, 64, 3, 1.18F, .18F);
        int n = 3;
        for (int i = 0; i < n; i++) {
            double a = -rotation * .18 + i * Math.PI * 2.0 / n;
            Vec3 c = b.point(a, r * .96);
            m.runeGlyph(b, c, r * .10, seed ^ (i * 0x9E37), a, .82F);
        }
    }

    private static ArcaneWorldMesh.Basis primaryBasis(SpellPresentationProfile.SigilStyle style, Vec3 direction) {
        return switch (style) {
            case GROUND_SEAL, QUAD_ARRAY, FEET_RUNE, SKY_RITUAL -> ArcaneWorldMesh.Basis.ground();
            case FRONT_COMPACT, FRONT_LANCE, TARGET_SEAL, BODY_HALO, WALL_MATRIX, PORTAL_GATE ->
                    ArcaneWorldMesh.Basis.facing(direction);
        };
    }

    private static double smooth(double t) { return t * t * (3.0 - 2.0 * t); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
