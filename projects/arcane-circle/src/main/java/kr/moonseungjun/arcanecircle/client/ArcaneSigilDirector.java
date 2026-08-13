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
    private static final int BUDGET = 2200;

    private ArcaneSigilDirector() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                  double range, double progress, boolean fusion, long startedAtNanos) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.fineBuilder(BUDGET);
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
        geometricDepth(mesh, spell, profile, primary, radius, p, rotation, seed);
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
        int detail=profile.complexity();
        double outer=r*(.985+.010*Math.sin(rotation*1.3));
        // Reference language: thin concentric rules first, ornament second. No giant filled ring.
        m.circle(basis,Vec3.ZERO,outer,92+detail*8,.76F);
        m.circle(basis,Vec3.ZERO,outer*.955,84+detail*7,.42F);
        m.circle(basis,Vec3.ZERO,outer*.875,76+detail*6,.54F);
        if(p>.10){
            for(int i=0;i<10;i++){
                double start=rotation*.08+i*Math.PI*2.0/10.0;
                m.arc(basis,Vec3.ZERO,outer*.925,start,Math.PI*.115,12,.54F);
            }
        }
        if(p>.22){
            inscriptionRing(m,basis,outer*.905,10+detail*3,r*(.020+detail*.0018),seed,-rotation*.10,.50F);
        }
        if(p>.32){
            int sides=switch(spell.school()){
                case FIRE -> 3; case FROST,ARCANE -> 6; case WIND -> 5; case WARD -> 8;
                case LIFE -> 7; case SPACE -> 4;
            };
            double geo=outer*.625;
            m.polygon(basis,Vec3.ZERO,geo,sides,rotation*.10,.62F);
            m.polygon(basis,Vec3.ZERO,geo,sides,rotation*.10+Math.PI/Math.max(3,sides),.48F);
            m.circle(basis,Vec3.ZERO,outer*.535,60+detail*4,.40F);
        }
        if(p>.46){
            m.runeChords(basis,Vec3.ZERO,outer*.405,7+detail,2+Math.floorMod(seed,3),rotation*.055,.42F);
            m.circle(basis,Vec3.ZERO,outer*.235,36,.52F);
            m.runeGlyph(basis,Vec3.ZERO,outer*.135,seed^0x51A7,rotation*.16,.58F);
        }
        if(p>.60&&detail>=4){
            int seals=Math.min(6,4+(detail-4));
            for(int i=0;i<seals;i++){
                double a=rotation*.035+i*Math.PI*2.0/seals;
                Vec3 c=basis.point(a,outer*.735);
                double sr=outer*(.068+(i%2)*.010);
                m.circle(basis,c,sr,22,.48F);
                m.circle(basis,c,sr*.72,18,.34F);
                m.polygon(basis,c,sr*.52,3+i%3,-a+.2,.38F);
                m.runeGlyph(basis,c,sr*.30,seed+i*67,-a,.36F);
            }
        }
        if(p>.76&&detail>=5){
            Vec3 n=basis.normal();double d=Math.min(r*.045,.28+detail*.018);
            m.circle(basis,n.scale(d),outer*.49,48,.34F);
            m.circle(basis,n.scale(-d),outer*.355,42,.30F);
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

    private static void geometricDepth(ArcaneWorldMesh.Builder m,SpellDefinition spell,SpellPresentationProfile.Profile profile,ArcaneWorldMesh.Basis b,double r,double p,double rotation,int seed){
        int detail=profile.complexity();if(detail<5||p<.58)return;Vec3 n=b.normal();double depth=Math.min(r*.060,.34+r*.010);double rr=r*(detail>=7?.31:.26);
        m.circle(b,n.scale(depth),rr,40,.34F);m.circle(b,n.scale(-depth*.75),rr*.78,36,.28F);
        for(int i=0;i<4;i++){double a=Math.PI/4.0+i*Math.PI/2.0;Vec3 a0=b.point(a,rr).add(n.scale(depth)),a1=b.point(a+.20,rr*.78).add(n.scale(-depth*.75));m.line(a0,a1,.30F);}
        switch(profile.sigil()){
            case GROUND_SEAL,QUAD_ARRAY,SKY_RITUAL -> {ArcaneWorldMesh.Basis cross=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(cross,Vec3.ZERO,r*.205,34,.30F);if(detail>=7)m.circle(cross,n.scale(depth*.25),r*.265,38,.26F);}
            case FRONT_LANCE,PORTAL_GATE -> {if(detail>=6){ArcaneWorldMesh.Basis cross=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(cross,Vec3.ZERO,r*.18,30,.28F);}}
            case TARGET_SEAL,WALL_MATRIX -> {for(int i=0;i<4;i++){double a=i*Math.PI/2.0;Vec3 c=b.point(a,r*.42);m.line(c.add(n.scale(-depth)),c.add(n.scale(depth)),.28F);}}
            case FRONT_COMPACT,BODY_HALO,FEET_RUNE -> {}
        }
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
        double outer=r*(.84+.16*p),inner=outer*.71;Vec3 n=b.normal();double depth=Math.min(r*.050,.42);
        // Celestial formula inspired by dense astrolabe/alchemical diagrams: many fine layers, no iron-frame octagon.
        m.circle(b,Vec3.ZERO,outer,112,.78F);
        m.circle(b,Vec3.ZERO,outer*.965,104,.40F);
        m.circle(b,Vec3.ZERO,outer*.885,92,.52F);
        for(int i=0;i<12;i++){double a=rotation*.035+i*Math.PI*2.0/12.0;m.arc(b,Vec3.ZERO,outer*.925,a,Math.PI*.105,13,.48F);}
        inscriptionRing(m,b,outer*.905,24,outer*.022,seed,-rotation*.075,.48F);

        // Two interlocked triangles + hexagonal calculation chamber, matching the requested classic magic-circle grammar.
        double tri=inner*.82;
        m.polygon(b,Vec3.ZERO,tri,3,rotation*.045+Math.PI/2.0,.60F);
        m.polygon(b,Vec3.ZERO,tri,3,rotation*.045-Math.PI/2.0,.60F);
        m.polygon(b,Vec3.ZERO,inner*.665,6,-rotation*.035,.42F);
        m.circle(b,Vec3.ZERO,inner*.535,64,.38F);
        m.circle(b,Vec3.ZERO,inner*.315,46,.48F);
        m.runeChords(b,Vec3.ZERO,inner*.285,8,3,rotation*.040,.34F);
        m.runeGlyph(b,Vec3.ZERO,inner*.145,seed^0x5A71,-rotation*.08,.48F);

        // Four major drop seals are readable sub-circles rather than giant cubes/rails.
        double orbit=outer*.695,child=outer*.102;
        for(int i=0;i<4;i++){
            double a=Math.PI/4.0+i*Math.PI/2.0+rotation*.020;
            Vec3 c=b.point(a,orbit);
            m.circle(b,c,child,30,.56F);m.circle(b,c,child*.76,24,.36F);
            m.polygon(b,c,child*.58,3,-a+Math.PI/2.0,.42F);
            m.polygon(b,c,child*.58,3,-a-Math.PI/2.0,.42F);
            m.runeGlyph(b,c,child*.30,seed+i*101,-rotation*.06,.38F);
            m.line(b.point(a,inner*.69),c,.30F);
            if(p>.72)m.line(c.add(n.scale(depth*.30)),c.add(n.scale(-Math.max(.85,r*.050))),.30F);
        }
        // Minor zodiac/coordinate marks fill the outer ring without turning into a central line knot.
        if(p>.58){for(int i=0;i<8;i++){double a=i*Math.PI/4.0-rotation*.025;Vec3 c=b.point(a,outer*.795);m.runeGlyph(b,c,outer*.030,seed^((i+1)*131),a,.32F);}}
        if(p>.80){
            m.circle(b,n.scale(depth),inner*.47,52,.30F);m.circle(b,n.scale(-depth*.70),inner*.365,46,.26F);
            ArcaneWorldMesh.Basis cross=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(cross,Vec3.ZERO,inner*.205,34,.26F);
        }
    }

    private static void inscriptionRing(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,double radius,int count,double size,int seed,double rotation,float width){
        int n=Math.max(8,count);for(int i=0;i<n;i++){double a=rotation+i*Math.PI*2.0/n;Vec3 c=b.point(a,radius),t=b.point(a+Math.PI/2.0,size),rad=b.point(a,size*.55);m.line(c.subtract(t),c.add(t),width*(i%4==0?1.0F:.72F));if(((seed+i)&1)==0)m.line(c.subtract(rad),c.add(t.scale(.55)),width*.68F);else m.line(c.add(rad),c.subtract(t.scale(.48)),width*.62F);if(Math.floorMod(seed+i,3)==0)m.line(c.subtract(t.scale(.38)).subtract(rad.scale(.70)),c.add(t.scale(.32)).add(rad.scale(.65)),width*.54F);}
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
