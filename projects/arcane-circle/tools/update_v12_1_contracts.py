#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
full = root / "tools/test_magic_contract.py"
text = full.read_text(encoding="utf-8")
text = text.replace("0.12.0-alpha.1", "0.12.1-alpha.1")
text = text.replace("ninefold-arcana-12", "ninefold-arcana-12-1")
text = text.replace(
'''need(sigils, [
    "renderChargeStep", "renderRelease", "CHARGE_STAGES", "radialCompartments",
    "centralSeal", "runeTicks", "spell.id().hashCode()", "case LANCE", "CROWN"
], "single-pass per-spell sigil rendering")
if "renderReadyPulse" in sigils:
    raise SystemExit("ready-loop sigil regeneration remains")
''',
'''world_geometry = read("client/WorldMagicTracker.java")
need(world_geometry, [
    "ExtractLevelRenderStateEvent", "SubmitCustomGeometryEvent", "submitShapeOutline",
    "buildCharge", "buildRelease", "for (int ring = 0; ring < spell.circle(); ring++)"
], "multiplayer world-space sigil geometry")
need(sigils, ["@Deprecated", "WorldMagicTracker"], "retired compatibility sigil service")
''')
text = text.replace(
'''need(casting, [
     "requiredCastTicks",
    "HighCircleSpellEffects.execute", "marksEarned"
], "casting integration")
''',
'''need(casting, [
    "requiredCastTicks", "requiredFusionCastTicks", "tickFusion",
    "HighCircleSpellEffects.execute", "WorldMagicService.release", "marksEarned"
], "casting integration")
''')
text = text.replace(
'print("Arcane Circle v0.12 nine-circle world geometry and charged fusion contract: PASS")',
'print("Arcane Circle v0.12.1 nine-circle particle-free world geometry contract: PASS")')
full.write_text(text, encoding="utf-8")

v12 = root / "tools/test_v12_overhaul.py"
v12_text = v12.read_text(encoding="utf-8")
v12_text = v12_text.replace("0.12.0-alpha.1", "0.12.1-alpha.1")
v12_text = v12_text.replace("ninefold-arcana-12", "ninefold-arcana-12-1")
v12_text = v12_text.replace(
    "Arcane Circle v0.12 compact HUD, charged fusion and particle-free world geometry contract: PASS",
    "Arcane Circle v0.12.1 compact HUD, charged fusion and particle-free world geometry contract: PASS")
v12.write_text(v12_text, encoding="utf-8")
print("v0.12.1 full contracts normalized")
