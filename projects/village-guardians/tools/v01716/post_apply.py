#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MESH = ROOT / "src/main/java/kr/moonseungjun/villageguardians/VillageSkillMeshLibrary.java"

text = MESH.read_text(encoding="utf-8")
old = '''    private static void renderRangerFocus(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.20);
        Vec3 core = b.local(0.0, 1.52, 0.65);
        sphere(pose, out, core, 0.18 + Math.sin(age * 0.25) * 0.035,
                8, 12, rgba(255, 211, 88, (int) (210 * fade)));
        ringVertical(pose, out, b, 0.48, 1.52, 0.035, 44,
                rgba(255, 235, 150, (int) (160 * fade)), age * 0.06);
    }
'''
new = '''    private static void renderRangerFocus(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.20);
        Vec3 core = b.local(0.0, 0.0, 0.18);
        sphere(pose, out, core, 0.18 + Math.sin(age * 0.25) * 0.035,
                8, 12, rgba(255, 211, 88, (int) (210 * fade)));
        ringVertical(pose, out, b, 0.48, 0.0, 0.035, 44,
                rgba(255, 235, 150, (int) (160 * fade)), age * 0.06);
    }
'''
if text.count(old) != 1:
    raise SystemExit(f"ranger focus marker count={text.count(old)}")
MESH.write_text(text.replace(old, new, 1), encoding="utf-8")

# Earlier contracts intentionally follow the current build version. Keep their
# gameplay assertions while migrating only stale version, wording and requested HUD coordinates.
for test in sorted((ROOT / "tools").glob("test_*.py")):
    source = test.read_text(encoding="utf-8")
    migrated = source.replace("mod_version=0.17.15-alpha.1", "mod_version=0.17.16-alpha.1")
    migrated = migrated.replace('assert "다음 화살" in abilities', 'assert "다음 활" in abilities')
    migrated = migrated.replace('assert "graphics.guiHeight() - 82" in overlay',
                                'assert "graphics.guiHeight() - 92" in overlay')
    if migrated != source:
        test.write_text(migrated, encoding="utf-8")

print("Placed ranger ready focus directly in front and migrated contracts to v0.17.16")
