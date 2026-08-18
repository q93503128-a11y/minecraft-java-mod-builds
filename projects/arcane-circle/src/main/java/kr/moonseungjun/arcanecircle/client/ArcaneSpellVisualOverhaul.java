package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Spell-authored presentation layer.
 *
 * The old presentation was technically varied but visually collapsed into the same concentric
 * formula + a handful of generic release bodies.  This layer adds readable magical grammar per
 * spell family while keeping every mesh bounded and particle-free.  Portals and prisons are
 * replaced outright: their geometry now grows upward from a floor contract instead of centering
 * vertically around the caster/target and clipping into caves or trapping the caster inside it.
 */
final class ArcaneSpellVisualOverhaul {
    private static final int SIGIL_BUDGET = 1900;
    private static final int BODY_BUDGET = 1450;
    private static final int RELEASE_BUDGET = 2600;

    private static final Set<String> PORTALS = Set.of(
            "misty_step", "dimension_door", "passwall", "plane_shift", "teleport",
            "demiplane", "gate", "teleportation_circle");
    private static final Set<String> PRISONS = Set.of(
            "hold_person", "hold_monster", "resilient_sphere", "forcecage", "astral_prison",
            "maze", "thunder_cage");
    private static final Set<String> WALLS = Set.of(
            "wall_of_fire", "wall_of_force", "wind_wall", "wall_of_ice", "prismatic_wall");
    private static final Set<String> DEATH = Set.of(
            "phantasmal_killer", "eyebite", "finger_of_death", "circle_of_death",
            "power_word_kill", "feeblemind", "weird");
    private static final Set<String> TERRAIN = Set.of(
            "move_earth", "reverse_gravity", "earthquake", "world_sunder");
    private static final Set<String> CELESTIAL = Set.of(
            "ice_storm", "flame_strike", "fire_storm", "control_weather", "insect_plague",
            "incendiary_cloud", "sunburst", "meteor_swarm", "phoenix_requiem");
    private static final Set<String> BUFFS = Set.of(
            "shield", "feather_fall", "mage_armor", "mirror_image", "invisibility", "blur", "fly", "haste",
            "protection_from_energy", "greater_invisibility", "resilient_sphere", "stoneskin",
            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "simulacrum", "clone",
            "etherealness", "fire_shield", "solar_guard", "shapechange", "foresight");
    private static final Set<String> SUSTAINED_DEBUFFS = Set.of(
            "sleep", "slow", "hold_person", "hold_monster", "dominate_person", "dominate_monster",
            "flesh_to_stone", "mass_suggestion", "forcecage", "maze", "true_polymorph",
            "thunder_cage", "astral_prison");
    private static final Set<String> CATASTROPHIC = Set.of(
            "delayed_blast_fireball", "fire_storm", "earthquake", "meteor_swarm",
            "world_sunder", "arcane_annihilation");

    private ArcaneSpellVisualOverhaul() {}

    static boolean replacesBaseSigil(SpellDefinition spell) {
        return PORTALS.contains(spell.id()) || PRISONS.contains(spell.id());
    }

    static boolean replacesBaseChargeBody(SpellDefinition spell) {
        return PORTALS.contains(spell.id()) || PRISONS.contains(spell.id());
    }

    static boolean replacesBaseRelease(SpellDefinition spell) {
        return PORTALS.contains(spell.id()) || PRISONS.contains(spell.id());
    }

    static ArcaneWorldMesh chargeSigil(SpellDefinition spell, Vec3 direction, double progress,
                                       double range, long startedAtNanos) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.fineBuilder(SIGIL_BUDGET);
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        double p = smooth(clamp(progress, 0.0, 1.0));
        double time = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        double r = Math.max(.62, profile.radius()) * (.44 + .56 * p);
        int seed = spell.id().hashCode();
        ArcaneWorldMesh.Basis basis = signatureBasis(spell, direction);

        if (PORTALS.contains(spell.id()))
            portalContract(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else if (PRISONS.contains(spell.id()))
            bindingContract(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else if ("time_stop".equals(spell.id()))
            temporalAstrolabe(m, basis, r, p, time, seed);
        else if ("wish".equals(spell.id()))
            wishCrown(m, basis, r, p, time, seed);
        else if (DEATH.contains(spell.id()))
            executionFormula(m, basis, r, p, time, seed, spell.circle());
        else if (TERRAIN.contains(spell.id()))
            tectonicFormula(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else if (WALLS.contains(spell.id()))
            wallCovenant(m, basis, r, p, time, seed, spell.circle());
        else if (CELESTIAL.contains(spell.id()))
            celestialFormula(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else {
            switch (spell.school()) {
                case FIRE -> combustionFormula(m, basis, r, p, time, seed, spell.circle());
                case FROST -> frostFormula(m, basis, r, p, time, seed, spell.circle());
                case WIND -> windFormula(m, basis, r, p, time, seed, spell.circle());
                case WARD -> wardFormula(m, basis, r, p, time, seed, spell.circle());
                case LIFE -> lifeFormula(m, basis, r, p, time, seed, spell.circle());
                case SPACE -> spaceFormula(m, basis, r, p, time, seed, spell.circle());
                case ARCANE -> arcaneFormula(m, basis, r, p, time, seed, spell.circle());
            }
        }
        if (spell.circle() >= 6) highCircleCrown(m, basis, r, p, time, seed, spell.circle());
        if (spell.circle() >= 6 && r >= 3.25) grandScaleArchitecture(m, basis, r, p, time, seed, spell.circle());
        if (CATASTROPHIC.contains(spell.id())) catastrophicAuthority(m, basis, r, p, time, seed, spell.id());
        return m.build();
    }

    static ArcaneWorldMesh chargeBody(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                      double progress, double range, long startedAtNanos) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.builder(BODY_BUDGET);
        double p = smooth(clamp(progress, 0.0, 1.0));
        double time = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        if (PORTALS.contains(spell.id())) {
            portalPair(m, spell, direction, targetOffset, p, time, false);
        } else if (PRISONS.contains(spell.id())) {
            if ("resilient_sphere".equals(spell.id())) risingSphere(m, Vec3.ZERO, p, time, spell.circle());
            else risingPrison(m, Vec3.ZERO, p, time, spell.circle(), spell.id().hashCode());
        }
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                   double range, double power, double age, double elapsedSeconds,
                                   double durationSeconds, long seed) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.builder(RELEASE_BUDGET);
        double rise = smooth(clamp(elapsedSeconds / .34, 0.0, 1.0));
        double pulse = .96 + Math.sin(elapsedSeconds * 2.1 + spell.id().hashCode() * .001) * .04;

        if (PORTALS.contains(spell.id())) {
            portalPair(m, spell, direction, targetOffset, rise, elapsedSeconds, true);
            if (spell.circle() >= 7) highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);
            return m.build();
        }
        if (PRISONS.contains(spell.id())) {
            if ("resilient_sphere".equals(spell.id())) risingSphere(m, targetOffset, rise, elapsedSeconds, spell.circle());
            else risingPrison(m, targetOffset, rise, elapsedSeconds, spell.circle(), spell.id().hashCode());
            if (spell.circle() >= 6) persistentControlMantle(m, spell, targetOffset, rise, elapsedSeconds);
            if (spell.circle() >= 7) highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);
            return m.build();
        }
        if ("prismatic_wall".equals(spell.id())) return m.build();

        SpellPresentationProfile.MotionStyle motion = SpellPresentationProfile.profile(spell).motion();
        boolean persistentBuff = BUFFS.contains(spell.id());
        boolean persistentDebuff = SUSTAINED_DEBUFFS.contains(spell.id());
        if (persistentBuff) buffMantle(m, spell, targetOffset, rise, elapsedSeconds);
        if (persistentDebuff) debuffMantle(m, spell, targetOffset, rise, elapsedSeconds);
        if ((persistentBuff || persistentDebuff) && spell.circle() >= 6)
            persistentAuthorityMantle(m, spell, targetOffset, rise, elapsedSeconds);
        switch (motion) {
            case WALL -> materialWall(m, spell, direction, targetOffset, range, rise, elapsedSeconds);
            case FIELD, STORM -> fieldAtmosphere(m, spell, targetOffset, range, rise, pulse, elapsedSeconds);
            case AURA -> {
                if (!persistentBuff) auraMantle(m, spell, targetOffset, rise, elapsedSeconds);
            }
            case SKY_DROP -> skyConvergence(m, spell, targetOffset, range, rise, elapsedSeconds);
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE, BEAM, WAVE, TARGET_BURST ->
                    impactFormula(m, spell, direction, targetOffset, rise, elapsedSeconds);
            default -> { }
        }

        if ("time_stop".equals(spell.id())) temporalDome(m, targetOffset, rise, elapsedSeconds);
        else if ("wish".equals(spell.id())) wishRelease(m, targetOffset, rise, elapsedSeconds);
        else if ("power_word_kill".equals(spell.id())) executionRelease(m, targetOffset, rise, elapsedSeconds);
        else if ("control_weather".equals(spell.id())) stormCrown(m, targetOffset, range, rise, elapsedSeconds);
        else if ("sunburst".equals(spell.id())) sunburstCorona(m, targetOffset, rise, elapsedSeconds);
        else if ("phoenix_requiem".equals(spell.id())) phoenixHalo(m, targetOffset, rise, elapsedSeconds);
        else if (TERRAIN.contains(spell.id())) terrainLift(m, targetOffset, range, rise, elapsedSeconds, seed);
        else if (spell.circle() >= 7 && !CELESTIAL.contains(spell.id()))
            highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);
        return m.build();
    }

    static ArcaneWorldMesh prismaticWallLayer(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                              double range, double age, double elapsedSeconds, int layer) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.builder(210);
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(flat(direction));
        double width = SpellMetrics.wallWidth(spell.id(), range, spell.circle());
        double panel = width / 7.0;
        double x0 = -width * .5 + layer * panel;
        double x1 = x0 + panel;
        double rise = smooth(clamp(elapsedSeconds / .30, 0.0, 1.0));
        double height = (5.1 + spell.circle() * .20) * rise;
        // Stay solid for 90% of its lifetime.  The former full-life alpha decay made 14s feel
        // even shorter than it actually was.
        double fade = age < .90 ? 1.0 : clamp((1.0 - age) / .10, 0.0, 1.0);
        Vec3 right = face.right();
        Vec3 a = targetOffset.add(right.scale(x0));
        Vec3 b = targetOffset.add(right.scale(x1));
        Vec3 up = new Vec3(0, height, 0);
        float alpha = (float) (.36 * fade);
        m.face(a, b, b.add(up), a.add(up), 1.12F, alpha);
        m.line(a, a.add(up), layer == 0 ? 1.38F : .84F);
        m.line(b, b.add(up), layer == 6 ? 1.38F : .84F);
        m.line(a.add(up), b.add(up), 1.04F);
        m.line(a, b, .64F);
        Vec3 center = a.add(b).scale(.5).add(0,
                height * (.42 + .05 * Math.sin(elapsedSeconds * 1.5 + layer)), 0);
        double rune = Math.max(.22, panel * .24);
        m.diamond(face, center, rune, elapsedSeconds * (layer % 2 == 0 ? .22 : -.18),
                1.18F, (float) (.25 * fade));
        if (height > 1.0) {
            m.runeGlyph(face, center, rune * .62, spell.id().hashCode() + layer * 103,
                    elapsedSeconds * (layer % 2 == 0 ? .08 : -.07), .54F);
        }
        return m.build();
    }

    private static ArcaneWorldMesh.Basis signatureBasis(SpellDefinition spell, Vec3 direction) {
        if (PORTALS.contains(spell.id()) || PRISONS.contains(spell.id())) return ArcaneWorldMesh.Basis.ground();
        return switch (SpellPresentationProfile.profile(spell).sigil()) {
            case GROUND_SEAL, QUAD_ARRAY, FEET_RUNE, SKY_RITUAL -> ArcaneWorldMesh.Basis.ground();
            default -> ArcaneWorldMesh.Basis.facing(direction);
        };
    }

    private static void combustionFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                          double p, double t, int seed, int circle) {
        double rr = r * (.58 + .16 * p);
        m.star(b, Vec3.ZERO, rr, rr * .28, 3, t * .31, 1.02F);
        m.polygon(b, Vec3.ZERO, rr * .78, 3, -t * .23 + Math.PI / 2.0, .62F);
        int n = 3 + Math.min(5, circle / 2);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n - t * .10;
            m.runeGlyph(b, b.point(a, r * .76), r * .055, seed + i * 29, a, .42F);
        }
    }

    private static void frostFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                     double p, double t, int seed, int circle) {
        double rr = r * (.68 + .10 * p);
        m.polygon(b, Vec3.ZERO, rr, 6, t * .07, .86F);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0 + t * .05;
            Vec3 mid = b.point(a, rr * .54), tip = b.point(a, rr);
            m.line(Vec3.ZERO, tip, i % 2 == 0 ? .92F : .58F);
            m.line(mid, mid.add(b.point(a + .52, rr * .20)), .46F);
            m.line(mid, mid.add(b.point(a - .52, rr * .20)), .46F);
        }
        m.circle(b, Vec3.ZERO, rr * .33, 32, .48F);
    }

    private static void windFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                    double p, double t, int seed, int circle) {
        for (int i = 0; i < 4; i++) {
            double rr = r * (.28 + i * .13);
            m.arc(b, Vec3.ZERO, rr, t * (.34 - i * .11) + i * 1.37,
                    Math.PI * (1.05 + i * .08), 22 + i * 4, i == 2 ? .88F : .52F);
        }
        int n = Math.max(3, circle / 2);
        for (int i = 0; i < n; i++) {
            double a = t * .18 + i * Math.PI * 2.0 / n;
            m.line(b.point(a, r * .18), b.point(a + .38, r * .76), .42F);
        }
    }

    private static void wardFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                    double p, double t, int seed, int circle) {
        m.polygon(b, Vec3.ZERO, r * .72, 8, t * .05, .86F);
        m.polygon(b, Vec3.ZERO, r * .55, 4, -t * .08 + Math.PI / 4.0, .68F);
        for (int i = 0; i < 4; i++) {
            Vec3 c = b.point(Math.PI / 4.0 + i * Math.PI / 2.0, r * .63);
            m.runeGlyph(b, c, r * .07, seed + i * 41, -t * .11, .48F);
        }
    }

    private static void lifeFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                    double p, double t, int seed, int circle) {
        int petals = 6 + Math.min(3, circle / 3);
        for (int i = 0; i < petals; i++) {
            double a = i * Math.PI * 2.0 / petals + t * .05;
            m.circle(b, b.point(a, r * .34), r * .22, 18, i % 2 == 0 ? .68F : .48F);
        }
        m.star(b, Vec3.ZERO, r * .43, r * .22, petals, -t * .06, .58F);
    }

    private static void spaceFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                     double p, double t, int seed, int circle) {
        m.polygon(b, Vec3.ZERO, r * .74, 4, t * .12 + Math.PI / 4.0, .92F);
        m.polygon(b, Vec3.ZERO, r * .58, 4, -t * .16, .68F);
        m.brokenBand(b, Vec3.ZERO, r * .31, r * .37, 42, 5, 1.08F, .16F);
        m.runeChords(b, Vec3.ZERO, r * .24, 7, 3, t * .06, .42F);
    }

    private static void arcaneFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                      double p, double t, int seed, int circle) {
        int n = 6 + Math.min(5, circle);
        m.runeChords(b, Vec3.ZERO, r * .69, n, Math.max(2, n / 3), t * .045, .62F);
        m.circle(b, Vec3.ZERO, r * .47, 48, .52F);
        int seals = Math.max(3, circle / 2);
        for (int i = 0; i < seals; i++) {
            double a = i * Math.PI * 2.0 / seals - t * .06;
            m.runeGlyph(b, b.point(a, r * .80), r * .055, seed + i * 73, a, .42F);
        }
    }

    private static void highCircleCrown(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                        double p, double t, int seed, int circle) {
        if (p < .30) return;
        double reveal = smooth(clamp((p - .30) / .70, 0.0, 1.0));
        double authority = r * (.94 + .11 * reveal);
        int seals = Math.min(14, 5 + circle);
        m.brokenBand(b, Vec3.ZERO, authority * .90, authority, 88 + circle * 5, 8,
                1.08F, (float) (.10 + .07 * reveal));
        m.runeRing(b, Vec3.ZERO, authority * .81, seals, r * .034, seed ^ 0x7A11,
                -t * .028, .36F);

        // High circles read as constructed formulae, not enlarged circles: nested polygon locks
        // and alternating sector chords keep the center visually dense at large world scale.
        if (p > .40) {
            int geometrySides = circle >= 9 ? 12 : circle >= 8 ? 10 : 8;
            m.polygon(b, Vec3.ZERO, authority * .72, geometrySides, t * .018, .60F);
            m.polygon(b, Vec3.ZERO, authority * .54, Math.max(6, geometrySides - 2), -t * .024 + .17, .46F);
            m.runeChords(b, Vec3.ZERO, authority * .48, geometrySides, circle >= 8 ? 3 : 2,
                    t * .012, .34F);
            for (int i = 0; i < geometrySides; i++) {
                double a = i * Math.PI * 2.0 / geometrySides + t * .010;
                Vec3 outer = b.point(a, authority * .88);
                Vec3 mid = b.point(a + (i % 2 == 0 ? .105 : -.085), authority * .68);
                Vec3 inner = b.point(a + (i % 3 - 1) * .075, authority * .43);
                m.line(outer, mid, i % 3 == 0 ? .54F : .30F);
                m.line(mid, inner, .28F);
                if (i % 2 == 0) {
                    Vec3 nextInner = b.point(a + Math.PI * 2.0 / geometrySides + .055, authority * .43);
                    m.line(mid, nextInner, .22F);
                }
            }
        }

        // 7C: a real second ritual plane with its own polygon lock and satellite seals.
        if (circle >= 7 && p > .46) {
            ArcaneWorldMesh.Basis cross = ArcaneWorldMesh.Basis.fromNormal(b.right(), b.up());
            m.circle(cross, Vec3.ZERO, r * .50, 58, .48F);
            m.polygon(cross, Vec3.ZERO, r * .41, 6, -t * .022, .38F);
            m.circle(cross, b.normal().scale(r * .08), r * .30, 44, .28F);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0 + t * .016;
                Vec3 c = cross.point(a, r * .47);
                m.runeGlyph(cross, c, r * .040, seed + i * 113, -a, .30F);
            }
        }

        // 8C: counter-rotating gyroscope plus a connected polyhedral cage.
        if (circle >= 8 && p > .58) {
            ArcaneWorldMesh.Basis cross2 = ArcaneWorldMesh.Basis.fromNormal(b.up(), b.normal());
            m.circle(cross2, Vec3.ZERO, r * .62, 66, .54F);
            m.polygon(cross2, Vec3.ZERO, r * .52, 8, t * .020 + Math.PI / 8.0, .42F);
            m.brokenBand(cross2, Vec3.ZERO, r * .37, r * .44, 52, 7, 1.0F, .10F);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0 + t * .014;
                Vec3 c = b.point(a, r * .72).add(b.normal().scale((i % 2 == 0 ? 1 : -1) * r * .17));
                Vec3 mate = b.point(a + Math.PI / 4.0, r * .54).add(b.normal().scale((i % 2 == 0 ? -1 : 1) * r * .11));
                m.runeGlyph(b, c, r * .047, seed + i * 137, -t * .035 + a, .38F);
                m.line(c, mate, i % 2 == 0 ? .38F : .26F);
            }
        }

        // 9C: nine independent formulae are complete mini-circles, each wired back to the core.
        if (circle >= 9 && p > .70) {
            Vec3 n = b.normal();
            m.brokenBand(b, n.scale(r * .12), r * 1.08, r * 1.17, 104, 9, 1.12F, .13F);
            m.brokenBand(b, n.scale(-r * .09), r * .68, r * .76, 78, 8, 1.04F, .10F);
            m.polygon(b, n.scale(r * .045), r * .92, 9, -t * .014 + Math.PI / 9.0, .46F);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0 - t * .018;
                Vec3 c = b.point(a, r * 1.09).add(n.scale(((i % 3) - 1) * r * .09));
                double sr = r * (.060 + (i % 2) * .010);
                m.circle(b, c, sr, 20, .44F);
                m.polygon(b, c, sr * .72, 3 + i % 4, a + t * .02, .36F);
                m.runeGlyph(b, c, sr * .38, seed + i * 197, -a + t * .018, .28F);
                m.line(c, b.point(a + (i % 2 == 0 ? .035 : -.035), r * .77), .26F);
            }
        }
    }

    /**
     * Extra architecture only for physically large formulae. Compact circles deliberately skip
     * this layer; large rituals receive tessellated sectors, nested polygon cells and satellite
     * sub-formulae so increasing radius also increases information density.
     */
    private static void grandScaleArchitecture(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                               double p, double t, int seed, int circle) {
        if (p < .38 || r < 3.25) return;
        double reveal = smooth(clamp((p - .38) / .62, 0.0, 1.0));
        double outer = r * (1.02 + .055 * reveal);
        int sectors = circle >= 9 ? 18 : circle >= 8 ? 16 : 14;
        int polygonSides = circle >= 9 ? 12 : circle >= 8 ? 10 : 8;
        m.polygon(b, Vec3.ZERO, outer * .94, polygonSides, t * .010, .48F);
        m.polygon(b, Vec3.ZERO, outer * .74, Math.max(6, polygonSides - 2), -t * .014 + .11, .38F);
        m.polygon(b, Vec3.ZERO, outer * .48, 6, t * .018 + Math.PI / 6.0, .34F);
        for (int i = 0; i < sectors; i++) {
            double a = i * Math.PI * 2.0 / sectors + t * .006;
            double alt = i % 2 == 0 ? .082 : -.064;
            Vec3 o = b.point(a, outer * .96);
            Vec3 m0 = b.point(a + alt, outer * .74);
            Vec3 m1 = b.point(a - alt * .58, outer * .55);
            Vec3 in = b.point(a + alt * .35, outer * .38);
            m.line(o, m0, i % 3 == 0 ? .42F : .24F);
            m.line(m0, m1, .22F);
            m.line(m1, in, .20F);
            if (i % 3 == 0) {
                double sr = r * (.043 + .004 * (i % 2));
                m.circle(b, o, sr, 16, .30F);
                m.polygon(b, o, sr * .72, 3 + (i / 3) % 4, -a + t * .012, .26F);
            }
        }
        int nodes = circle >= 9 ? 9 : circle >= 8 ? 8 : 6;
        for (int i = 0; i < nodes; i++) {
            double a = i * Math.PI * 2.0 / nodes - t * .010;
            Vec3 c = b.point(a, outer * .83);
            Vec3 left = b.point(a - .16, outer * .66);
            Vec3 right = b.point(a + .16, outer * .66);
            m.line(c, left, .24F);
            m.line(c, right, .24F);
            m.runeGlyph(b, c, r * .040, seed ^ (i * 257 + 0x391), a + t * .012, .28F);
        }
    }

    /** Catastrophe-only charge authority: converging break seals + cross-plane lock rings. */
    private static void catastrophicAuthority(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                              double p, double t, int seed, String id) {
        if (p < .28) return;
        double wake = smooth(clamp((p - .28) / .72, 0.0, 1.0));
        double outer = r * (1.08 + .10 * wake);
        m.brokenBand(b, Vec3.ZERO, outer * .90, outer, 88, 9, 1.14F, .11F);
        m.brokenBand(b, Vec3.ZERO, outer * .66, outer * .72, 72, 7, 1.06F, .09F);
        int anchors = "meteor_swarm".equals(id) ? 8 : 6;
        for (int i = 0; i < anchors; i++) {
            double a = i * Math.PI * 2.0 / anchors + t * (i % 2 == 0 ? .018 : -.015);
            Vec3 outerNode = b.point(a, outer * .96);
            Vec3 innerNode = b.point(a + (i % 2 == 0 ? .13 : -.11), outer * .72);
            m.line(outerNode, innerNode, i % 3 == 0 ? .68F : .34F);
            m.runeGlyph(b, outerNode, r * .052, seed + i * 211, -a + t * .025, .38F);
        }
        if (p > .58) {
            ArcaneWorldMesh.Basis cross = ArcaneWorldMesh.Basis.fromNormal(b.right(), b.normal());
            double crossR = r * (.42 + .16 * wake);
            m.circle(cross, b.normal().scale(r * .06), crossR, 54, .46F);
            m.brokenBand(cross, b.normal().scale(-r * .05), crossR * .72, crossR * .82,
                    48, 6, 1.08F, .08F);
        }
        if (p > .76) {
            Vec3 n = b.normal();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4.0 + i * Math.PI / 2.0;
                Vec3 base = b.point(a, r * .52);
                double h = r * (.24 + .05 * (i % 2));
                m.line(base.add(n.scale(-h)), base.add(n.scale(h)), i % 2 == 0 ? .66F : .42F);
                m.diamond(b, base.add(n.scale(h)), r * .045, a + t * .03, 1.10F, .14F);
            }
        }
    }

    private static void portalContract(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double r,
                                       double p, double t, int seed, int circle) {
        double rr = Math.min(r, circle >= 8 ? 7.2 : 4.8) * (.50 + .30 * p);
        m.polygon(g, Vec3.ZERO, rr, 4, t * .08 + Math.PI / 4.0, .92F);
        m.polygon(g, Vec3.ZERO, rr * .78, 4, -t * .10, .62F);
        m.brokenBand(g, Vec3.ZERO, rr * .48, rr * .57, 56, 6, 1.08F, .18F);
        int n = 4 + Math.min(4, circle / 2);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n + t * .04;
            Vec3 c = g.point(a, rr * .90);
            m.runeGlyph(g, c, rr * .07, seed + i * 97, a, .48F);
            m.line(c, g.point(a, rr * .64), .34F);
        }
    }

    private static void bindingContract(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double r,
                                        double p, double t, int seed, int circle) {
        double rr = Math.min(r, 5.4) * (.52 + .34 * p);
        m.polygon(g, Vec3.ZERO, rr, 8, t * .045, .88F);
        m.polygon(g, Vec3.ZERO, rr * .74, 4, -t * .065 + Math.PI / 4.0, .62F);
        int seals = 4 + Math.min(6, circle / 2);
        for (int i = 0; i < seals; i++) {
            double a = i * Math.PI * 2.0 / seals;
            Vec3 c = g.point(a, rr * .86);
            m.circle(g, c, rr * .075, 14, .44F);
            m.line(c, g.point(a + .10 * (i % 2 == 0 ? 1 : -1), rr * .46), .38F);
        }
        m.runeGlyph(g, Vec3.ZERO, rr * .18, seed ^ 0xB1AD, -t * .08, .62F);
    }

    private static void temporalAstrolabe(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                          double p, double t, int seed) {
        m.circle(b, Vec3.ZERO, r * .78, 96, .88F);
        m.circle(b, Vec3.ZERO, r * .61, 84, .48F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0 / 12.0;
            m.line(b.point(a, r * .66), b.point(a, r * .78), i % 3 == 0 ? .92F : .42F);
        }
        m.line(Vec3.ZERO, b.point(-Math.PI / 2.0 + t * .08, r * .52), 1.02F);
        m.line(Vec3.ZERO, b.point(-Math.PI / 2.0 - t * .21, r * .34), .68F);
        m.brokenBand(b, Vec3.ZERO, r * .84, r * .91, 72, 7, 1.02F, .14F);
    }

    private static void wishCrown(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                  double p, double t, int seed) {
        m.star(b, Vec3.ZERO, r * .72, r * .31, 9, -t * .035, .88F);
        m.circle(b, Vec3.ZERO, r * .51, 64, .46F);
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0 + t * .025;
            m.diamond(b, b.point(a, r * .84), r * .055, a, 1.12F, .20F);
        }
        m.runeGlyph(b, Vec3.ZERO, r * .17, seed ^ 0x71A5, t * .04, .58F);
    }

    private static void executionFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                         double p, double t, int seed, int circle) {
        double rr = r * .66;
        m.polygon(b, Vec3.ZERO, rr, 4, Math.PI / 4.0, .92F);
        m.star(b, Vec3.ZERO, rr * .88, rr * .30, 4, t * .03, .72F);
        m.line(b.point(0, rr * 1.05), b.point(Math.PI, rr * 1.05), .68F);
        m.line(b.point(Math.PI / 2, rr * 1.05), b.point(-Math.PI / 2, rr * 1.05), .68F);
        if (circle >= 7) m.runeRing(b, Vec3.ZERO, rr * .78, 8, rr * .065, seed, -t * .04, .40F);
    }

    private static void tectonicFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double r,
                                        double p, double t, int seed, int circle) {
        double rr = r * (.52 + .28 * p);
        int cracks = 8 + circle;
        for (int i = 0; i < cracks; i++) {
            double a = i * Math.PI * 2.0 / cracks + (seed & 7) * .013;
            Vec3 a0 = g.point(a, rr * .12);
            Vec3 a1 = g.point(a + Math.sin(i * 1.7) * .09, rr * .55);
            Vec3 a2 = g.point(a - Math.cos(i * .9) * .07, rr);
            m.line(a0, a1, i % 3 == 0 ? .82F : .46F);
            m.line(a1, a2, .38F);
        }
        m.brokenBand(g, Vec3.ZERO, rr * .63, rr * .70, 58, 5, 1.02F, .13F);
    }

    private static void wallCovenant(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                     double p, double t, int seed, int circle) {
        double rr = r * .68;
        m.polygon(b, Vec3.ZERO, rr, 4, Math.PI / 4.0, .82F);
        m.polygon(b, Vec3.ZERO, rr * .78, 8, t * .035, .54F);
        for (int i = -2; i <= 2; i++) {
            Vec3 c = b.right().scale(i * rr * .30);
            m.runeGlyph(b, c, rr * .075, seed + i * 53, -t * .05, .42F);
        }
    }

    private static void celestialFormula(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double r,
                                         double p, double t, int seed, int circle) {
        double rr = r * (.55 + .22 * p);
        m.circle(g, Vec3.ZERO, rr, 96, .78F);
        m.star(g, Vec3.ZERO, rr * .80, rr * .39, 6 + Math.min(3, circle / 3), t * .025, .56F);
        int n = 6 + Math.min(6, circle);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n - t * .025;
            Vec3 c = g.point(a, rr * .89);
            m.runeGlyph(g, c, rr * .045, seed + i * 107, a, .34F);
        }
    }

    private static void portalPair(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 direction,
                                   Vec3 target, double rise, double time, boolean released) {
        Vec3 flat = flat(direction);
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(flat);
        // The near structure is deliberately offset in front of the player so a 9C Gate never
        // cages the caster.  The destination gate can be enormous without consuming the caster.
        Vec3 near = flat.scale("gate".equals(spell.id()) ? 2.35 : 1.55);
        double fullHeight = "gate".equals(spell.id()) ? 8.2
                : "demiplane".equals(spell.id()) ? 6.3 : 3.2 + spell.circle() * .30;
        double farHeight = fullHeight * ("gate".equals(spell.id()) ? 1.22 : 1.0);
        double nearHeight = "gate".equals(spell.id()) ? 3.45 : Math.min(4.7, fullHeight * .86);
        risingPortal(m, face, near, nearHeight, nearHeight * .58, rise, time, spell.id().hashCode());
        if (target.lengthSqr() > 2.0) {
            risingPortal(m, face, target, farHeight, farHeight * .58, rise,
                    -time * .82, spell.id().hashCode() ^ 0x5EED);
            if (released && rise > .65) {
                Vec3 a = near.add(0, nearHeight * .52, 0);
                Vec3 b = target.add(0, farHeight * .52, 0);
                Vec3 axis = b.subtract(a);
                double len = axis.length();
                if (len > .5) m.helix(a, axis, ArcaneWorldMesh.Basis.facing(axis), len,
                        Math.min(.70, nearHeight * .16), 3, 42, .48F, true);
            }
        }
    }

    private static void risingPortal(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis face, Vec3 base,
                                     double height, double width, double rise, double time, int seed) {
        double h = Math.max(.08, height * rise);
        double half = width * .5 * (.76 + .24 * rise);
        // Bottom is always the floor.  Every visible part grows upward.
        Vec3 center = base.add(0, Math.max(.12, h * .52), 0);
        double radius = Math.min(half, h * .42);
        for (int i = -2; i <= 2; i++) {
            Vec3 c = center.add(face.normal().scale(i * .055 * Math.max(1.0, width)));
            double rr = radius * (1.0 - Math.abs(i) * .055);
            m.circle(face, c, Math.max(.08, rr), 42, i == 0 ? 1.10F : .54F);
        }
        m.line(base.add(face.right().scale(-half)), base.add(face.right().scale(-half)).add(0, h * .84, 0), .92F);
        m.line(base.add(face.right().scale(half)), base.add(face.right().scale(half)).add(0, h * .84, 0), .92F);
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        m.brokenBand(ground, base.add(0, .03, 0), half * .72, half * .90, 40, 5, 1.04F, .16F);
        if (rise > .55) m.runeGlyph(face, center, Math.max(.12, radius * .28), seed, time * .12, .62F);
    }

    private static void risingPrison(ArcaneWorldMesh.Builder m, Vec3 base, double rise, double time,
                                     int circle, int seed) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 1.05 + circle * .18;
        double h = (1.65 + circle * .25) * rise;
        int sides = 6 + Math.min(6, circle / 2);
        m.band(g, base.add(0, .035, 0), r * .84, r, 40, 1.08F, .16F);
        for (int i = 0; i < sides; i++) {
            double a = time * .045 + i * Math.PI * 2.0 / sides;
            Vec3 foot = base.add(g.point(a, r));
            Vec3 top = foot.add(0, h, 0);
            m.line(foot, top, i % 3 == 0 ? 1.16F : .62F);
            if (h > .25) m.line(top, base.add(0, h, 0), .38F);
        }
        if (h > .35) {
            m.circle(g, base.add(0, h, 0), r, 42, .82F);
            m.runeGlyph(g, base.add(0, h + .025, 0), r * .26, seed, -time * .08, .54F);
        }
    }

    private static void risingSphere(ArcaneWorldMesh.Builder m, Vec3 base, double rise, double time, int circle) {
        double r = (1.25 + circle * .10) * (.30 + .70 * rise);
        Vec3 c = base.add(0, Math.max(r + .18, 1.55), 0);
        m.sphere(c, r, 5, .76F);
        m.circle(ArcaneWorldMesh.Basis.ground(), base.add(0, .04, 0), r * 1.05, 44, .58F);
    }

    private static void materialWall(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 direction,
                                     Vec3 base, double range, double rise, double time) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(flat(direction));
        ArcaneWorldMesh.Basis ground = ArcaneWorldMesh.Basis.ground();
        double width = SpellMetrics.wallWidth(spell.id(), range, spell.circle());
        double height = (2.5 + spell.circle() * .22) * rise;
        int nodes = 9 + spell.circle();
        for (int i = 0; i < nodes; i++) {
            double f = i / (double) Math.max(1, nodes - 1);
            double x = (f - .5) * width;
            Vec3 foot = base.add(face.right().scale(x));
            if ("wall_of_fire".equals(spell.id())) {
                double h = height * (.62 + .38 * Math.abs(Math.sin(i * 1.7 + time * 1.3)));
                m.shard(foot.add(0, h * .45, 0), new Vec3(0, 1, 0), ground,
                        Math.max(.15, h), .13 + .03 * (i % 3), 1.12F, .22F);
            } else if ("wall_of_ice".equals(spell.id())) {
                double h = height * (.72 + .28 * ((i * 37) % 5) / 4.0);
                m.shard(foot.add(0, h * .48, 0), new Vec3(0, 1, 0), ground,
                        Math.max(.18, h), .18 + .025 * (i % 4), 1.08F, .25F);
            } else if ("wall_of_force".equals(spell.id())) {
                Vec3 c = foot.add(0, height * (.35 + .30 * (i % 2)), 0);
                m.polygon(face, c, Math.max(.22, width / nodes * .62), 6,
                        (i % 2 == 0 ? 1 : -1) * time * .08, i % 3 == 0 ? .84F : .48F);
            } else if ("wind_wall".equals(spell.id())) {
                Vec3 a = foot.add(0, .15, 0);
                Vec3 b = foot.add(face.right().scale(Math.sin(i + time) * .32)).add(0, height, 0);
                m.line(a, b, i % 3 == 0 ? .82F : .46F);
            }
        }
    }

    private static void fieldAtmosphere(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                        double range, double rise, double pulse, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = Math.max(2.8, SpellMetrics.effectRadius(spell.id(), range, spell.circle()));
        double rr = r * (.92 + .08 * pulse);
        m.brokenBand(g, center.add(0, .04, 0), rr * .82, rr, 70, 7, 1.03F, .14F);
        int n = spell.circle() >= 7 ? 14 : 9;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n + time * .035;
            Vec3 foot = center.add(g.point(a, rr * (.62 + .08 * (i % 3))));
            double h = rise * (.55 + spell.circle() * .12) * (1.0 + .28 * Math.sin(time * 1.2 + i));
            m.line(foot, foot.add(0, Math.max(.10, h), 0), i % 4 == 0 ? .72F : .38F);
        }
    }

    private static void auraMantle(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                   double rise, double time) {
        double r = (.78 + spell.circle() * .10) * (.55 + .45 * rise);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        Vec3 c = center.add(0, .25, 0);
        m.arc(g, c, r, time * .32, Math.PI * 1.55, 32, .68F);
        m.arc(x, c, r * .82, -time * .23, Math.PI * 1.48, 30, .52F);
        m.arc(z, c, r * .68, time * .18 + 1.2, Math.PI * 1.42, 28, .42F);
    }


    private static void buffMantle(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                   double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis front = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        int seed = spell.id().hashCode();
        double pulse = .96 + .04 * Math.sin(time * 2.0);
        switch (spell.id()) {
            case "feather_fall" -> {
                for (int sideSign : new int[]{-1, 1}) {
                    for (int i = 0; i < 3; i++) {
                        double y = .45 + i * .42;
                        Vec3 root = center.add(sideSign * .20, y, 0);
                        Vec3 tip = center.add(sideSign * (.62 + i * .14), y + .18, .08 * Math.sin(time * 1.4 + i));
                        m.line(root, tip, i == 0 ? .64F : .38F);
                        m.line(tip, tip.add(sideSign * .18, -.12, 0), .30F);
                    }
                }
                m.arc(g, center.add(0, .08, 0), .72, time * .18, Math.PI * 1.25, 28, .42F);
            }
            case "fly" -> {
                for (int sideSign : new int[]{-1, 1}) {
                    Vec3 root = center.add(sideSign * .18, 1.15, 0);
                    for (int i = 0; i < 4; i++) {
                        Vec3 elbow = center.add(sideSign * (.55 + i * .22), 1.55 - i * .13, .08 * Math.sin(time * 1.7 + i));
                        Vec3 tip = center.add(sideSign * (1.05 + i * .18), 1.20 - i * .20, .12 * Math.cos(time * 1.3 + i));
                        m.line(root, elbow, i == 0 ? .72F : .46F);
                        m.line(elbow, tip, .38F);
                    }
                }
                m.helix(center.add(0, .05, 0), new Vec3(0, 1, 0), front, 1.55, .38, 2, 34, .34F, true);
            }
            case "simulacrum" -> {
                Vec3 echo = center.add(-.92, .98, .12);
                m.diamond(front, echo, .52, time * .035, 1.08F, .18F);
                m.runeGlyph(front, echo, .23, seed ^ 0x51A0, -time * .05, .48F);
                m.line(center.add(0, .80, 0), echo, .30F);
                m.circle(g, center.add(0, .04, 0), .82, 40, .38F);
            }
            case "clone" -> {
                Vec3 core = center.add(0, .52, -.72);
                m.circle(front, core, .58, 38, .58F);
                m.polygon(front, core, .43, 6, time * .045, .42F);
                m.runeGlyph(front, core, .20, seed ^ 0xC10E, -time * .035, .46F);
                m.brokenBand(g, center.add(0, .04, 0), .96, 1.08, 48, 7, 1.02F, .10F);
            }
            case "shield" -> {
                Vec3 c = center.add(0, 1.15, .62);
                for (int i = 0; i < 3; i++) m.polygon(front, c.add(0, 0, i * .045),
                        (.70 - i * .11) * pulse, 6, time * (i % 2 == 0 ? .08 : -.06), i == 0 ? .92F : .46F);
            }
            case "mage_armor" -> {
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 4.0 + i * Math.PI / 2.0 + time * .08;
                    Vec3 c = center.add(g.point(a, .72)).add(0, .75 + (i % 2) * .62, 0);
                    m.diamond(front, c, .28, -a, 1.10F, .20F);
                    m.runeGlyph(front, c, .13, seed + i * 31, a, .42F);
                }
            }
            case "mirror_image" -> {
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2.0 / 3.0 + time * .62;
                    Vec3 c = center.add(g.point(a, 1.15)).add(0, .85 + .18 * Math.sin(time * 1.4 + i), 0);
                    m.circle(front, c, .34, 24, .58F);
                    m.line(c.add(0, -.42, 0), c.add(0, .42, 0), .34F);
                }
            }
            case "invisibility", "greater_invisibility" -> {
                int rings = "greater_invisibility".equals(spell.id()) ? 4 : 2;
                for (int i = 0; i < rings; i++) {
                    double y = .35 + i * .38;
                    m.arc(g, center.add(0, y, 0), .72 + i * .13, time * (.45 - i * .09) + i,
                            Math.PI * 1.35, 30, i == 0 ? .66F : .38F);
                }
                if (rings > 2) m.circle(side, center.add(0, 1.0, 0), .78, 34, .38F);
            }
            case "blur" -> {
                for (int i = -2; i <= 2; i++) {
                    double x = i * .18 + Math.sin(time * 5.0 + i) * .08;
                    m.arc(front, center.add(x, 1.0, 0), .76, time * .8 + i, Math.PI * 1.18, 24,
                            i == 0 ? .70F : .30F);
                }
            }
            case "haste" -> {
                m.circle(g, center.add(0, .04, 0), 1.05, 54, .72F);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2.0 / 12.0 + time * .30;
                    m.line(center.add(g.point(a, .82)), center.add(g.point(a, 1.08)), i % 3 == 0 ? .68F : .32F);
                }
                m.helix(center.add(0, .05, 0), new Vec3(0, 1, 0), front, 1.65, .42, 2, 36, .42F, true);
            }
            case "protection_from_energy" -> {
                for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2.0 / 5.0 - time * .22;
                    Vec3 c = center.add(g.point(a, 1.02)).add(0, .95, 0);
                    m.diamond(front, c, .30, a + time * .08, 1.14F, .22F);
                }
                m.circle(g, center.add(0, .05, 0), .78, 40, .42F);
            }
            case "resilient_sphere", "globe_of_invulnerability" -> {
                double r = "globe_of_invulnerability".equals(spell.id()) ? 1.85 : 1.25;
                Vec3 c = center.add(0, 1.05, 0);
                m.sphere(c, r * pulse, 6, .62F);
                m.brokenBand(g, center.add(0, .04, 0), r * .92, r * 1.02, 58, 7, 1.05F, .12F);
            }
            case "stoneskin" -> {
                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2.0 / 7.0 + time * .07;
                    Vec3 c = center.add(g.point(a, .72 + .12 * (i % 2))).add(0, .45 + .22 * (i % 4), 0);
                    m.polygon(front, c, .24 + .03 * (i % 3), 5, -a, i % 2 == 0 ? .66F : .38F);
                }
            }
            case "freedom_of_movement" -> {
                m.helix(center.add(0, .02, 0), new Vec3(0, 1, 0), front, 1.85, .68, 3, 42, .52F, true);
                m.arc(g, center.add(0, .05, 0), 1.12, time * .48, Math.PI * 1.55, 34, .72F);
            }
            case "true_seeing" -> {
                Vec3 eye = center.add(0, 1.65, 0);
                m.arc(front, eye, .92, Math.PI * .10, Math.PI * .80, 34, .72F);
                m.arc(front, eye, .92, Math.PI * 1.10, Math.PI * .80, 34, .72F);
                m.circle(front, eye, .28, 28, .78F);
                m.runeGlyph(front, eye, .16, seed, -time * .08, .42F);
                m.brokenBand(g, center.add(0, .05, 0), 1.25, 1.35, 54, 8, 1.02F, .10F);
            }
            case "fire_shield", "solar_guard" -> {
                int n = "solar_guard".equals(spell.id()) ? 8 : 6;
                double orbit = "solar_guard".equals(spell.id()) ? 1.22 : .92;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2.0 / n + time * .18;
                    Vec3 c = center.add(g.point(a, orbit)).add(0, .82 + .30 * Math.sin(a + time), 0);
                    m.star(front, c, .25, .10, 4, -a, i % 2 == 0 ? .74F : .42F);
                }
                if ("solar_guard".equals(spell.id())) m.circle(front, center.add(0, 1.05, 0), .70, 42, .62F);
            }
            case "shapechange" -> {
                for (int i = 0; i < 6; i++) {
                    double y = .18 + i * .30;
                    double rr = .48 + i * .10 + .08 * Math.sin(time * 1.2 + i);
                    m.polygon(g, center.add(0, y, 0), rr, 3 + (i + seed & 3),
                            time * (i % 2 == 0 ? .18 : -.14), i % 2 == 0 ? .66F : .38F);
                }
            }
            case "foresight" -> {
                Vec3 eye = center.add(0, 2.05, 0);
                m.circle(front, eye, .82, 52, .62F);
                m.runeGlyph(front, eye, .28, seed ^ 0xF012, -time * .04, .58F);
                m.circle(g, center.add(0, .05, 0), 1.32, 62, .52F);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2.0 / 12.0 - time * .08;
                    m.line(center.add(g.point(a, 1.12)), center.add(g.point(a, 1.34)), i % 3 == 0 ? .72F : .34F);
                }
                m.circle(side, center.add(0, 1.05, 0), .92, 42, .36F);
            }
            default -> auraMantle(m, spell, center, rise, time);
        }
    }


    private static void debuffMantle(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                     double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis front = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        int seed = spell.id().hashCode();
        switch (spell.id()) {
            case "sleep" -> {
                Vec3 crown = center.add(0, 1.85, 0);
                m.arc(front, crown, .58, time * .08, Math.PI * 1.42, 30, .52F);
                m.arc(front, crown.add(.18, .08, 0), .34, -time * .06 + 1.1, Math.PI * 1.24, 24, .34F);
                m.brokenBand(g, center.add(0, .035, 0), .62, .78, 36, 6, .96F, .08F);
            }
            case "slow" -> {
                m.brokenBand(g, center.add(0, .04, 0), .82, 1.02, 48, 8, 1.02F, .10F);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0 - time * .055;
                    m.line(center.add(g.point(a, .72)), center.add(g.point(a, 1.02)), i % 2 == 0 ? .52F : .28F);
                }
            }
            case "dominate_person", "dominate_monster", "mass_suggestion" -> {
                Vec3 eye = center.add(0, 1.72, 0);
                m.polygon(front, eye, .58 + spell.circle() * .025, 6, time * .035, .58F);
                m.runeGlyph(front, eye, .21, seed ^ 0xD0A1, -time * .05, .46F);
                int n = "mass_suggestion".equals(spell.id()) ? 6 : 4;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2.0 / n + time * .09;
                    Vec3 c = center.add(g.point(a, .88)).add(0, .75 + .18 * (i % 3), 0);
                    m.diamond(front, c, .16, -a, 1.02F, .12F);
                    m.line(c, eye, .24F);
                }
            }
            case "flesh_to_stone" -> {
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3.0 + time * .025;
                    Vec3 c = center.add(g.point(a, .62 + .08 * (i % 2))).add(0, .35 + .24 * (i % 4), 0);
                    m.polygon(front, c, .20 + .025 * (i % 3), 5, -a, i % 2 == 0 ? .54F : .30F);
                    m.line(c, center.add(0, .90, 0), .20F);
                }
                m.brokenBand(g, center.add(0, .04, 0), .72, .92, 42, 7, 1.02F, .08F);
            }
            case "true_polymorph" -> {
                for (int i = 0; i < 5; i++) {
                    double y = .28 + i * .34;
                    double rr = .48 + .08 * i + .04 * Math.sin(time * 1.1 + i);
                    m.polygon(g, center.add(0, y, 0), rr, 3 + (i % 4), time * (i % 2 == 0 ? .11 : -.09),
                            i % 2 == 0 ? .52F : .30F);
                }
                m.circle(side, center.add(0, 1.05, 0), .74, 36, .32F);
            }
            default -> {
                m.brokenBand(g, center.add(0, .04, 0), .72, .92, 42, 7, 1.0F, .08F);
                m.circle(front, center.add(0, 1.05, 0), .58, 30, .32F);
                m.runeGlyph(front, center.add(0, 1.05, 0), .18, seed, -time * .05, .34F);
            }
        }
    }

    /** Compact persistent authority for 6C+ status magic. It scales by topology, not by screen size. */
    private static void persistentAuthorityMantle(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                                  double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis front = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        int circle = spell.circle();
        int seed = spell.id().hashCode();
        double r = (1.05 + Math.max(0, circle - 6) * .16) * (.72 + .28 * rise);
        m.brokenBand(g, center.add(0, .035, 0), r * .82, r, 52 + circle * 3, 7, 1.02F, .085F);
        m.polygon(g, center.add(0, .045, 0), r * .72, circle >= 9 ? 9 : circle >= 8 ? 8 : 6,
                time * .025, .30F);
        int runes = circle >= 9 ? 9 : circle >= 8 ? 8 : 6;
        for (int i = 0; i < runes; i++) {
            double a = i * Math.PI * 2.0 / runes - time * .025;
            Vec3 c = center.add(g.point(a, r * .90)).add(0, .10 + .10 * (i % 3), 0);
            m.runeGlyph(g, c, r * .055, seed + i * 149, a, .27F);
        }
        if (circle >= 7) {
            Vec3 c = center.add(0, 1.02, 0);
            m.circle(front, c, r * .56, 38, .30F);
            m.polygon(front, c, r * .43, 6, -time * .030, .25F);
        }
        if (circle >= 8) {
            Vec3 c = center.add(0, 1.02, 0);
            m.circle(side, c, r * .68, 44, .32F);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4.0 + i * Math.PI / 2.0 + time * .025;
                Vec3 d = center.add(g.point(a, r * .78)).add(0, .72 + .18 * (i % 2), 0);
                m.diamond(front, d, r * .12, -a, 1.02F, .10F);
            }
        }
        if (circle >= 9) {
            Vec3 crown = center.add(0, 2.18, 0);
            m.brokenBand(g, crown, r * .42, r * .56, 46, 9, 1.04F, .09F);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0 + time * .030;
                Vec3 d = crown.add(g.point(a, r * .50));
                m.diamond(g, d, r * .065, a, 1.0F, .08F);
                if (i % 3 == 0) m.line(d, center.add(0, 1.05, 0), .18F);
            }
        }
        if ("foresight".equals(spell.id())) {
            Vec3 eye = center.add(0, 2.10, 0);
            m.runeChords(front, eye, r * .38, 9, 4, -time * .018, .26F);
        } else if ("shapechange".equals(spell.id())) {
            for (int i = 0; i < 3; i++) {
                double y = .48 + i * .48;
                m.polygon(g, center.add(0, y, 0), r * (.45 + .08 * i), 5 + i,
                        time * (i % 2 == 0 ? .07 : -.06), .24F);
            }
        } else if ("clone".equals(spell.id()) || "simulacrum".equals(spell.id())) {
            Vec3 contract = center.add(.0, .95, -.82);
            m.runeChords(front, contract, r * .30, circle >= 8 ? 8 : 7, 3, time * .016, .25F);
        }
    }

    /** Persistent hard-control seal for high-circle prisons/maze/astral restraints. */
    private static void persistentControlMantle(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                                double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis front = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        double r = (1.15 + spell.circle() * .10) * (.70 + .30 * rise);
        double topY = 1.75 + spell.circle() * .16;
        Vec3 top = center.add(0, topY, 0);
        m.brokenBand(g, center.add(0, .035, 0), r * .72, r, 54, 8, 1.04F, .09F);
        m.polygon(g, center.add(0, .045, 0), r * .73, spell.circle() >= 9 ? 9 : 8, time * .025, .32F);
        m.circle(g, top, r * .72, 42, .34F);
        m.polygon(g, top, r * .56, 6, -time * .032, .28F);
        if (spell.circle() >= 7) {
            m.circle(front, center.add(0, topY * .52, 0), r * .64, 40, .30F);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3.0 + time * .018;
                Vec3 foot = center.add(g.point(a, r * .82));
                Vec3 crown = top.add(g.point(a + .10, r * .54));
                m.line(foot, crown, i % 2 == 0 ? .36F : .22F);
            }
        }
        if (spell.circle() >= 8) m.circle(side, center.add(0, topY * .52, 0), r * .78, 46, .34F);
        if (spell.circle() >= 9) {
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0 - time * .025;
                Vec3 c = top.add(g.point(a, r * .72));
                m.runeGlyph(g, c, r * .055, spell.id().hashCode() + i * 173, -a, .28F);
            }
        }
    }

    private static void skyConvergence(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,
                                       double range, double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double altitude = Math.max(7.0, SpellPresentationProfile.profile(spell).skyHeight() * .55);
        double r = Math.max(2.6, SpellMetrics.effectRadius(spell.id(), range, spell.circle()) * .55);
        Vec3 sky = target.add(0, altitude, 0);
        m.brokenBand(g, sky, r * .72, r, 64, 7, 1.08F, .15F);
        int n = 6 + Math.min(6, spell.circle());
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n + time * .025;
            Vec3 c = sky.add(g.point(a, r * .86));
            m.line(c, target.add(g.point(a, r * .25)), i % 3 == 0 ? .62F : .34F);
        }
    }

    private static void impactFormula(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 direction,
                                      Vec3 target, double rise, double time) {
        if (target.lengthSqr() < .01) return;
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(direction);
        double r = (.30 + spell.circle() * .065) * (.55 + .45 * rise);
        int sides = switch (spell.school()) {
            case FIRE -> 3; case FROST -> 6; case WIND -> 5; case WARD -> 8;
            case LIFE -> 7; case SPACE -> 4; case ARCANE -> 6;
        };
        m.polygon(face, target, r, sides, time * .18, .62F);
        if (spell.circle() >= 4) m.circle(face, target, r * .62, 24, .38F);
        if (spell.circle() >= 7) m.runeGlyph(face, target, r * .28, spell.id().hashCode(), -time * .12, .42F);
    }

    private static void temporalDome(ArcaneWorldMesh.Builder m, Vec3 center, double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis front = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        double r = 5.3 * (.35 + .65 * rise);
        m.brokenBand(g, center.add(0, .05, 0), r * .88, r, 96, 12, 1.08F, .12F);
        m.polygon(g, center.add(0, .06, 0), r * .78, 12, -time * .012, .46F);
        m.circle(g, center.add(0, .07, 0), r * .62, 84, .52F);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2.0 / 12.0;
            Vec3 inner = center.add(g.point(a, r * .62));
            Vec3 outer = center.add(g.point(a + (i % 2 == 0 ? .035 : -.035), r));
            m.line(inner, outer, i % 3 == 0 ? .74F : .36F);
            Vec3 foot = center.add(g.point(a, r * .94));
            m.line(foot, foot.add(0, 1.15 + (i % 3) * .34, 0), i % 3 == 0 ? .76F : .36F);
        }
        Vec3 hub = center.add(0, 1.72, 0);
        m.circle(g, hub, r * .56, 66, .48F);
        m.polygon(g, hub, r * .43, 12, time * .014, .34F);
        m.circle(front, hub, r * .31, 52, .30F);
        m.circle(side, hub, r * .39, 56, .32F);
        m.runeChords(g, hub, r * .32, 12, 5, -time * .010, .28F);
    }

    private static void wishRelease(ArcaneWorldMesh.Builder m, Vec3 center, double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 2.65 * (.45 + .55 * rise);
        m.star(g, center.add(0, .05, 0), r * .74, r * .30, 9, -time * .022, .58F);
        m.polygon(g, center.add(0, .06, 0), r * .61, 9, time * .017 + Math.PI / 9.0, .40F);
        m.runeChords(g, center.add(0, .07, 0), r * .48, 9, 4, -time * .012, .30F);
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2.0 / 9.0 - time * .07;
            Vec3 c = center.add(g.point(a, r)).add(0, .48 + (i % 3) * .46, 0);
            m.circle(g, c, .20 + .025 * (i % 2), 18, .34F);
            m.diamond(g, c, .16 + .025 * (i % 2), a, 1.12F, .18F);
            m.line(center.add(0, .90, 0), c, .30F);
        }
        Vec3 crown = center.add(0, 2.30, 0);
        m.brokenBand(g, crown, r * .32, r * .46, 54, 9, 1.04F, .09F);
        m.polygon(g, crown, r * .38, 9, time * .018, .30F);
    }

    private static void executionRelease(ArcaneWorldMesh.Builder m, Vec3 target, double rise, double time) {
        ArcaneWorldMesh.Basis face = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        double r = 1.45 * (.55 + .45 * rise);
        Vec3 c = target.add(0, 1.0, 0);
        m.polygon(face, c, r, 4, Math.PI / 4.0, 1.02F);
        m.polygon(face, c, r * .78, 8, -time * .025 + Math.PI / 8.0, .54F);
        m.polygon(face, c, r * .52, 4, time * .035, .44F);
        m.runeChords(face, c, r * .46, 8, 3, -time * .018, .36F);
        m.line(c.add(-r * 1.2, 0, 0), c.add(r * 1.2, 0, 0), .72F);
        m.line(c.add(0, -r * 1.2, 0), c.add(0, r * 1.2, 0), .72F);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4.0 + i * Math.PI / 2.0;
            Vec3 seal = c.add(face.point(a, r * 1.03));
            m.runeGlyph(face, seal, r * .09, 0x9C11 + i * 97, -a + time * .02, .34F);
            m.line(seal, c.add(face.point(a, r * .58)), .24F);
        }
    }

    private static void stormCrown(ArcaneWorldMesh.Builder m, Vec3 target, double range, double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = Math.max(7.0, range * .55) * (.50 + .50 * rise);
        Vec3 sky = target.add(0, 13.0, 0);
        m.brokenBand(g, sky, r * .74, r, 84, 7, 1.08F, .18F);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI * 2.0 / 8.0 + time * .04;
            Vec3 c = sky.add(g.point(a, r * .83));
            m.line(c, c.add(0, -2.2 - (i % 3) * .6, 0), i % 2 == 0 ? .72F : .42F);
        }
    }

    private static void sunburstCorona(ArcaneWorldMesh.Builder m, Vec3 target, double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 5.0 * (.35 + .65 * rise);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2.0 / 16.0;
            m.line(target.add(g.point(a, r * .32)), target.add(g.point(a, r * (1.0 + .18 * (i % 3)))),
                    i % 4 == 0 ? .92F : .48F);
        }
        m.circle(g, target.add(0, .06, 0), r * .44, 48, .62F);
    }

    private static void phoenixHalo(ArcaneWorldMesh.Builder m, Vec3 target, double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 4.0 * (.35 + .65 * rise);
        m.arc(g, target.add(0, .08, 0), r, time * .12, Math.PI * 1.72, 58, .82F);
        m.arc(g, target.add(0, .10, 0), r * .72, -time * .18 + 1.1, Math.PI * 1.65, 52, .54F);
    }

    private static void terrainLift(ArcaneWorldMesh.Builder m, Vec3 target, double range, double rise,
                                    double time, long seed) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = Math.max(5.0, range * .36) * (.45 + .55 * rise);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI * 2.0 / 10.0 + Math.floorMod((int) seed, 31) * .01;
            double d = r * (.35 + .055 * (i % 6));
            Vec3 c = target.add(g.point(a, d)).add(0, .15 + .10 * (i % 4), 0);
            double h = .35 + .16 * (i % 5);
            m.shard(c.add(0, h * .5, 0), new Vec3(0, 1, 0), g, h,
                    .13 + .02 * (i % 3), 1.0F, .22F);
        }
    }

    private static void highCircleAfterimage(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,
                                             double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        double r = (1.72 + spell.circle() * .30) * (.45 + .55 * rise);
        int sides = spell.circle() >= 9 ? 9 : spell.circle() >= 8 ? 8 : 7;
        m.brokenBand(g, target.add(0, .04, 0), r * .80, r, 72, 7, 1.06F, .12F);
        m.polygon(g, target.add(0, .05, 0), r * .70, sides, time * .024, .38F);
        m.runeChords(g, target.add(0, .055, 0), r * .52, sides, Math.max(2, sides / 3), -time * .015, .28F);
        if (spell.circle() >= 7) {
            int satellites = 6;
            for (int i = 0; i < satellites; i++) {
                double a = i * Math.PI * 2.0 / satellites - time * .025;
                Vec3 c = target.add(g.point(a, r * .86)).add(0, .16 + .12 * (i % 2), 0);
                m.circle(g, c, r * .055, 16, .28F);
                m.runeGlyph(g, c, r * .035, spell.id().hashCode() + i * 71, a, .26F);
            }
        }
        if (spell.circle() >= 8) {
            Vec3 c = target.add(0, 1.15, 0);
            m.circle(x, c, r * .44, 44, .34F);
            m.polygon(x, c, r * .36, 8, time * .022, .28F);
            m.circle(z, c, r * .57, 50, .40F);
            m.polygon(z, c, r * .47, 8, -time * .018 + Math.PI / 8.0, .30F);
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4.0 + i * Math.PI / 2.0;
                Vec3 base = target.add(g.point(a, r * .66));
                m.line(base, c.add(g.point(a + .12, r * .24)), .22F);
            }
        }
        if (spell.circle() >= 9) {
            Vec3 crown = target.add(0, 2.05, 0);
            m.brokenBand(g, crown, r * .36, r * .52, 54, 9, 1.02F, .09F);
            m.polygon(g, crown, r * .44, 9, time * .018, .28F);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0 + time * .035;
                Vec3 c = target.add(g.point(a, r * .93)).add(0, .22 + (i % 3) * .28, 0);
                m.circle(g, c, r * .050, 16, .25F);
                m.runeGlyph(g, c, r * .040, spell.id().hashCode() + i * 101, -time * .04, .32F);
                if (i % 3 == 0) m.line(c, crown, .18F);
            }
        }
    }

    private static Vec3 flat(Vec3 value) {
        Vec3 flat = new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static double smooth(double t) { return t * t * (3.0 - 2.0 * t); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
