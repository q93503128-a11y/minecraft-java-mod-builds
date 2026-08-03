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
print("Placed ranger ready focus directly in front of the player")
