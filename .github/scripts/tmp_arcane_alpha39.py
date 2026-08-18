from pathlib import Path
import re

root = Path('projects/arcane-circle')
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, body):
    path.write_text(body, encoding='utf-8')


def rep(path, old, new, count=1):
    body = read(path)
    found = body.count(old)
    if found != count:
        raise SystemExit(f'{path}: expected {count}, found {found}: {old[:160]!r}')
    write(path, body.replace(old, new, count))


def sub(path, pattern, repl, count=1):
    body = read(path)
    body2, n = re.subn(pattern, repl, body, count=count, flags=re.S)
    if n != count:
        raise SystemExit(f'{path}: regex expected {count}, found {n}: {pattern[:160]!r}')
    write(path, body2)


overhaul = client / 'ArcaneSpellVisualOverhaul.java'
gameplay = magic / 'SpellGameplayService.java'
audit = root / 'tools/test_current_source.py'
project = root / 'PROJECT.md'

# ---------------------------------------------------------------------------
# 1. Large sigils: preserve compact circles, but large/high-circle formulae get
#    tessellated geometry, nested polygons, sector cells and multi-plane locks.
# ---------------------------------------------------------------------------
rep(overhaul, '    private static final int SIGIL_BUDGET = 1050;\n    private static final int BODY_BUDGET = 1050;\n    private static final int RELEASE_BUDGET = 1800;\n',
              '    private static final int SIGIL_BUDGET = 1900;\n    private static final int BODY_BUDGET = 1450;\n    private static final int RELEASE_BUDGET = 2600;\n')

marker = '    private static final Set<String> CATASTROPHIC = Set.of(\n'
insert = '''    private static final Set<String> SUSTAINED_DEBUFFS = Set.of(
            "sleep", "slow", "hold_person", "hold_monster", "dominate_person", "dominate_monster",
            "flesh_to_stone", "mass_suggestion", "forcecage", "maze", "true_polymorph",
            "thunder_cage", "astral_prison");
    private static final Set<String> CATASTROPHIC = Set.of(
'''
rep(overhaul, marker, insert)

rep(overhaul,
    '        if (spell.circle() >= 6) highCircleCrown(m, basis, r, p, time, seed, spell.circle());\n'
    '        if (CATASTROPHIC.contains(spell.id())) catastrophicAuthority(m, basis, r, p, time, seed, spell.id());\n',
    '        if (spell.circle() >= 6) highCircleCrown(m, basis, r, p, time, seed, spell.circle());\n'
    '        if (spell.circle() >= 6 && r >= 3.25) grandScaleArchitecture(m, basis, r, p, time, seed, spell.circle());\n'
    '        if (CATASTROPHIC.contains(spell.id())) catastrophicAuthority(m, basis, r, p, time, seed, spell.id());\n')

high_circle = r'''    private static void highCircleCrown(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
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

'''
sub(overhaul,
    r'    private static void highCircleCrown\(ArcaneWorldMesh\.Builder m, ArcaneWorldMesh\.Basis b, double r,.*?\n    /\*\* Catastrophe-only charge authority: converging break seals \+ cross-plane lock rings\. \*/\n',
    high_circle + '    /** Catastrophe-only charge authority: converging break seals + cross-plane lock rings. */\n')

# ---------------------------------------------------------------------------
# 2. Persistent states: buffs AND debuffs keep a visual identity. High circles
#    additionally carry compact 3D authority that survives for the mechanic's
#    authoritative lifetime instead of only during the cast flash.
# ---------------------------------------------------------------------------
rep(overhaul,
    '        if (PRISONS.contains(spell.id())) {\n'
    '            if ("resilient_sphere".equals(spell.id())) risingSphere(m, targetOffset, rise, elapsedSeconds, spell.circle());\n'
    '            else risingPrison(m, targetOffset, rise, elapsedSeconds, spell.circle(), spell.id().hashCode());\n'
    '            if (spell.circle() >= 7) highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);\n'
    '            return m.build();\n'
    '        }\n',
    '        if (PRISONS.contains(spell.id())) {\n'
    '            if ("resilient_sphere".equals(spell.id())) risingSphere(m, targetOffset, rise, elapsedSeconds, spell.circle());\n'
    '            else risingPrison(m, targetOffset, rise, elapsedSeconds, spell.circle(), spell.id().hashCode());\n'
    '            if (spell.circle() >= 6) persistentControlMantle(m, spell, targetOffset, rise, elapsedSeconds);\n'
    '            if (spell.circle() >= 7) highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);\n'
    '            return m.build();\n'
    '        }\n')

rep(overhaul,
    '        boolean persistentBuff = BUFFS.contains(spell.id());\n'
    '        if (persistentBuff) buffMantle(m, spell, targetOffset, rise, elapsedSeconds);\n',
    '        boolean persistentBuff = BUFFS.contains(spell.id());\n'
    '        boolean persistentDebuff = SUSTAINED_DEBUFFS.contains(spell.id());\n'
    '        if (persistentBuff) buffMantle(m, spell, targetOffset, rise, elapsedSeconds);\n'
    '        if (persistentDebuff) debuffMantle(m, spell, targetOffset, rise, elapsedSeconds);\n'
    '        if ((persistentBuff || persistentDebuff) && spell.circle() >= 6)\n'
    '            persistentAuthorityMantle(m, spell, targetOffset, rise, elapsedSeconds);\n')

persistent_methods = r'''
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

'''
rep(overhaul, '    private static void skyConvergence(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,\n',
              persistent_methods + '    private static void skyConvergence(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,\n')

# Densify the three iconic 9C releases that otherwise bypass the generic afterimage.
temporal = r'''    private static void temporalDome(ArcaneWorldMesh.Builder m, Vec3 center, double rise, double time) {
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

'''
sub(overhaul,
    r'    private static void temporalDome\(ArcaneWorldMesh\.Builder m, Vec3 center, double rise, double time\) \{.*?\n    private static void stormCrown\(',
    temporal + '    private static void stormCrown(')

# Replace generic afterimage with a denser 7/8/9C residue architecture.
afterimage = r'''    private static void highCircleAfterimage(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,
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

'''
sub(overhaul,
    r'    private static void highCircleAfterimage\(ArcaneWorldMesh\.Builder m, SpellDefinition spell, Vec3 target,.*?\n    private static Vec3 flat\(',
    afterimage + '    private static Vec3 flat(')

# ---------------------------------------------------------------------------
# 3. Match sustained debuff visuals to the authoritative mechanic lifetime.
# ---------------------------------------------------------------------------
rep(gameplay,
    '            case "feather_fall" -> 120;\n'
    '            case "mirror_image" -> 260;\n',
    '            case "feather_fall" -> 120;\n'
    '            case "sleep" -> 140;\n'
    '            case "mass_suggestion" -> 160;\n'
    '            case "mirror_image" -> 260;\n')

# ---------------------------------------------------------------------------
# 4. Version, contract and source audit.
# ---------------------------------------------------------------------------
rep(root/'gradle.properties', 'mod_version=0.12.1-alpha.38', 'mod_version=0.12.1-alpha.39')
rep(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
    'VERSION = "0.12.1-alpha.38"', 'VERSION = "0.12.1-alpha.39"')
rep(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json',
    '"version": "0.12.1-alpha.38"', '"version": "0.12.1-alpha.39"')
# SpellCatalog has carried a catalog-version string in recent builds; update only if present.
catalog = magic/'SpellCatalog.java'
body = read(catalog)
if '0.12.1-alpha.38' in body:
    write(catalog, body.replace('0.12.1-alpha.38', '0.12.1-alpha.39'))

proj = read(project)
proj += '''\n\n## Alpha.39 grand-sigil + persistent-status presentation contracts\n\n- Compact/low-circle formulae are intentionally left readable and light. Large 6C+ formulae add `grandScaleArchitecture`: nested polygon locks, tessellated sector webs, chord lattices and satellite sub-formulae. Radius growth must increase information density, not merely enlarge a circle.\n- 7C uses an independently authored second ritual plane; 8C adds a connected gyroscopic/polyhedral cage; 9C uses nine complete satellite formulae wired back to a multi-plane authority core.\n- Long-lived buffs and debuffs keep a spell-authored state silhouette for the authoritative mechanic lifetime. 6C+ status magic additionally carries compact persistent authority instead of holding the enormous cast circle on screen.\n- High-circle hard controls/prisons keep floor/top seals, cross-plane restraints and high-circle rune satellites while active. `sleep` and `mass_suggestion` visual lifetimes now follow their real 140/160 tick control windows.\n- Time Stop, Wish and Power Word Kill have dedicated dense 9C release geometry instead of relying on a generic afterimage.\n'''
write(project, proj)

# Audit version assertions.
a = read(audit)
a = a.replace('mod_version=0.12.1-alpha.38', 'mod_version=0.12.1-alpha.39')
a = a.replace('VERSION = "0.12.1-alpha.38"', 'VERSION = "0.12.1-alpha.39"')
a = a.replace('"version": "0.12.1-alpha.38"', '"version": "0.12.1-alpha.39"')
anchor = '''assert '14초 지속 7색 장벽' in summary\n\n# Active-tree hygiene: history is the archive.\n'''
if anchor not in a:
    raise SystemExit('audit insertion anchor missing')
checks = '''assert '14초 지속 7색 장벽' in summary\n\n# Alpha.39 grand-sigil / persistent status identity.\nfor token in ['SIGIL_BUDGET = 1900','SUSTAINED_DEBUFFS = Set.of','grandScaleArchitecture',\n              'geometrySides','tessellated sectors','persistentAuthorityMantle','persistentControlMantle',\n              'debuffMantle','runeChords','nine independent formulae are complete mini-circles']:\n    assert token in overhaul, token\nfor token in ['case "sleep" -> 140','case "mass_suggestion" -> 160']:\n    assert token in gameplay, token\nassert 'if ((persistentBuff || persistentDebuff) && spell.circle() >= 6)' in overhaul\nassert 'if (spell.circle() >= 6 && r >= 3.25) grandScaleArchitecture' in overhaul\nfor token in ['m.polygon(g, hub, r * .43, 12','m.star(g, center.add(0, .05, 0), r * .74',\n              'm.runeChords(face, c, r * .46, 8, 3']:\n    assert token in overhaul, token\n\n# Active-tree hygiene: history is the archive.\n'''
a = a.replace(anchor, checks, 1)
write(audit, a)

print('Arcane Circle alpha.39 grand-sigil + persistent-status overhaul applied')
