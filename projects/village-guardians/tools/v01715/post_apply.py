#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

entity = JAVA / "VillageSkillEffectEntity.java"
text = entity.read_text(encoding="utf-8")
text = text.replace("        noCulling = true;\n", "")
entity.write_text(text, encoding="utf-8")

entities = JAVA / "VillageSkillEffectEntities.java"
text = entities.read_text(encoding="utf-8")
old = ".sized(0.1f, 0.1f)"
if old not in text:
    raise SystemExit("effect entity size marker missing")
text = text.replace(old, ".sized(24.0f, 16.0f)", 1)
entities.write_text(text, encoding="utf-8")

overlay = JAVA / "VillageSkillHudOverlay.java"
text = overlay.read_text(encoding="utf-8")
old = "        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null\n                || text.isBlank() || System.currentTimeMillis() > expiresAt) {\n"
new = "        if (minecraft.player == null || text.isBlank()\n                || System.currentTimeMillis() > expiresAt) {\n"
if old not in text:
    raise SystemExit("HUD visibility marker missing")
text = text.replace(old, new, 1)
overlay.write_text(text, encoding="utf-8")

abilities = JAVA / "VillageRoleAbilitySystem.java"
text = abilities.read_text(encoding="utf-8")
old = "launchMovingAt(level, player, MovingKind.BLADE, new ItemStack(Items.IRON_SWORD),"
new = "launchMovingAt(level, player, MovingKind.BLADE, ItemStack.EMPTY,"
if old not in text:
    raise SystemExit("blade carrier item marker missing")
text = text.replace(old, new, 1)
abilities.write_text(text, encoding="utf-8")

print("Applied NeoForge 26.2 effect bounds, HUD and empty carrier compatibility fixes")
