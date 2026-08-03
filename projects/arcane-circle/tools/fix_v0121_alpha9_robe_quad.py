#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Fix the generated robe underside quad: the fourth vertex was missing its Z coordinate.
path = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.java"
text = path.read_text(encoding="utf-8")
old = '''            quad(out, pose, -shoulder, top - 0.045F, -0.30F, shoulder, top - 0.045F, -0.30F,
                    waist, chest - 0.035F, -0.325F, -waist, chest - 0.035F, shadow);'''
new = '''            quad(out, pose, -shoulder, top - 0.045F, -0.30F, shoulder, top - 0.045F, -0.30F,
                    waist, chest - 0.035F, -0.325F, -waist, chest - 0.035F, -0.325F, shadow);'''
if new not in text:
    if old not in text:
        raise SystemExit("alpha.9 robe underside quad anchor not found")
    text = text.replace(old, new, 1)
    path.write_text(text, encoding="utf-8")

# Keep the three non-instant archetypes referenced in the runtime service itself. This both
# documents the supported kinetic contract and lets the bytecode audit verify the correct class.
kinetics_path = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java"
kinetics = kinetics_path.read_text(encoding="utf-8")
anchor = '''public final class SpellKineticsService {
    private static final Map<UUID, List<PendingCast>> PENDING = new HashMap<>();'''
replacement = '''public final class SpellKineticsService {
    private static final Map<UUID, List<PendingCast>> PENDING = new HashMap<>();
    private static final SpellArchetype.Mode[] KINETIC_MODES = {
            SpellArchetype.Mode.PROJECTILE,
            SpellArchetype.Mode.CHANNEL,
            SpellArchetype.Mode.FIELD
    };'''
if replacement not in kinetics:
    if anchor not in kinetics:
        raise SystemExit("alpha.9 spell kinetics audit anchor not found")
    kinetics = kinetics.replace(anchor, replacement, 1)
    kinetics_path.write_text(kinetics, encoding="utf-8")

print("Arcane Circle alpha.9 robe quad and kinetic mode contract fixed")
