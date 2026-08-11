#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATH=ROOT/"src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"

def read(): return PATH.read_text(encoding="utf-8")
def write(s): PATH.write_text(s,encoding="utf-8")
def once(old,new,label):
    s=read()
    if new in s and old not in s: return
    if old not in s: raise SystemExit(f"{label}: marker missing")
    write(s.replace(old,new,1))
def between(start,end,replacement,label):
    s=read(); a=s.find(start); b=s.find(end,a+len(start))
    if a<0 or b<0: raise SystemExit(f"{label}: bounds missing")
    write(s[:a]+replacement+s[b:])

once("    private static final int MAX_RELEASE_GEOMETRY = 2200;\n",
     "    private static final int MAX_RELEASE_GEOMETRY = 3600;\n","release budget")
once("    private static final int MAX_FRAME = 18000;\n",
     "    private static final int MAX_FRAME = 26000;\n","frame budget")

once('''        for (Visual visual : RELEASES) {
            double age = clamp((now - visual.startedAt) / (double) Math.max(1L,
                    visual.expiresAt - visual.startedAt), 0.0, 1.0);
            entries.add(new RenderEntry(visual.center, buildRelease(visual, age), color(visual.spell)));
        }
''','''        for (Visual visual : RELEASES) {
            double age = clamp((now - visual.startedAt) / (double) Math.max(1L,
                    visual.expiresAt - visual.startedAt), 0.0, 1.0);
            entries.add(new RenderEntry(visual.center, buildRelease(visual, age), color(visual.spell)));
            if (SpellVisualSignature.isPrismatic(visual.spell)) {
                Vec3 targetOffset = targetOffset(visual);
                for (int layer = 0; layer < 7; layer++) {
                    ArcaneWorldMesh accent = SpellVisualSignature.prismaticAccent(
                            visual.spell, visual.direction, targetOffset, visual.range, age, layer);
                    entries.add(new RenderEntry(visual.center, accent,
                            SpellVisualSignature.prismaticColor(layer)));
                }
            }
        }
''',"prismatic palette layers")

once('''        if (visual.fusion && release > 0.02) {
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.08, outer * 1.14,
                    72 + complexity * 10, 6, 1.30F, (float) (0.24 + release * 0.24));
        }
''','''        SpellVisualSignature.appendCharge(spell, profile, basis, outer, rotation, p, mesh);
        if (visual.fusion && release > 0.02) {
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.08, outer * 1.14,
                    72 + complexity * 10, 6, 1.30F, (float) (0.24 + release * 0.24));
        }
''',"charge signature")

once('''        }
        return mesh.build();
    }

    private static ArcaneWorldMesh.Basis presentationBasis''','''        }
        SpellVisualSignature.appendRelease(spell, visual.direction, targetOffset(visual),
                visual.range, visual.power, age, powerFactor, mesh);
        return mesh.build();
    }

    private static ArcaneWorldMesh.Basis presentationBasis''',"release signature")

color='''    private static int color(SpellDefinition spell) {
        return switch (spell.id()) {
            case "disintegrate" -> 0xFF66FF19;
            case "sunbeam", "sunburst", "foresight", "true_seeing", "solar_guard" -> 0xFFFFE34F;
            case "flame_strike" -> 0xFFFF6A18;
            case "circle_of_death", "finger_of_death", "power_word_kill", "eyebite" -> 0xFFFF174D;
            case "weird", "phantasmal_killer", "feeblemind" -> 0xFFFF22C8;
            case "flesh_to_stone" -> 0xFFD2DAE8;
            case "move_earth", "earthquake" -> 0xFFFFA52E;
            case "time_stop" -> 0xFF55E8FF;
            case "wish" -> 0xFFF0A0FF;
            case "prismatic_spray", "prismatic_wall" -> 0xFFFFFFFF;
            case "control_weather", "reverse_gravity" -> 0xFF24D8FF;
            case "clone", "simulacrum" -> 0xFF9AF4FF;
            case "shapechange", "true_polymorph" -> 0xFF20FFB4;
            default -> switch (spell.school()) {
                case FIRE -> 0xFFFF2100;
                case FROST -> 0xFF00CFFF;
                case WIND -> 0xFF00FF9C;
                case WARD -> 0xFF8E22FF;
                case LIFE -> 0xFF18F044;
                case SPACE -> 0xFFD000FF;
                default -> 0xFF3454FF;
            };
        };
    }

'''
between("    private static int color(SpellDefinition spell) {\n",
        "    private static int clampCircle(int circle) {\n",color,"spell palette")
print("Arcane Circle alpha.20 visual migration: PASS")
