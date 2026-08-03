#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"

# v0.17.13 used to verify one specific R/G migration implementation. The new
# contract verifies the general unsafe/conflicting-key migration instead.
path = TOOLS / "test_v01713_effects_and_keys.py"
text = path.read_text(encoding="utf-8")
old = '''    assert "GLFW.GLFW_KEY_R" in keys and "GLFW.GLFW_KEY_G" in keys
    assert "ROLE_SKILL_ONE.setKey" in keys and "GLFW.GLFW_KEY_Z" in keys
    assert "ROLE_SKILL_TWO.setKey" in keys and "GLFW.GLFW_KEY_V" in keys
    assert "KeyMapping.resetMapping()" in keys and "minecraft.options.save()" in keys
'''
new = '''    assert "VANILLA_RESERVED" in keys and "migrateUnsafeBindings" in keys
    assert "GLFW.GLFW_KEY_Z" in keys and "GLFW.GLFW_KEY_V" in keys
    assert "!used.add(value)" in keys
    assert "KeyMapping.resetMapping()" in keys and "minecraft.options.save()" in keys
'''
if old not in text:
    raise SystemExit("v01713 legacy key contract marker missing")
text = text.replace(old, new, 1)
text = text.replace(
    "[PASS] Saved legacy R/G skill bindings migrate to real Z/V and persist",
    "[PASS] Unsafe vanilla/conflicting bindings migrate to safe Z/V/B/H/J/K and persist",
)
path.write_text(text, encoding="utf-8")

# v0.17.18 fixed the oversized contact sphere. v0.17.19 keeps the separation
# but replaces it with target-size-aware AABB contact.
path = TOOLS / "test_v01718_bow_shortcuts.py"
text = path.read_text(encoding="utf-8")
old = '''        "fireOrbContactRadius" in ability
        and "1.40 + Math.max(0, specialRank) * 0.08" in ability
        and "contactRadius" in ability
        and "targetsNear(level, owner, position, moving.radius(), 40)" in ability,
'''
new = '''        "fireOrbContacts" in ability
        and "fireOrbContactPadding" in ability
        and "target.getBoundingBox().inflate(padding).contains(position)" in ability
        and "targetsNear(level, owner, position, moving.radius(), 40)" in ability,
'''
if old not in text:
    raise SystemExit("v01718 fire-orb contract marker missing")
text = text.replace(old, new, 1)
text = text.replace("v0.17.18 version is active", "v0.17.19 version is active")
path.write_text(text, encoding="utf-8")

# v0.17.7's one-slot test_choose action has been superseded by the two-slot
# test_equip path and is intentionally removed as stale code.
path = TOOLS / "test_v0177_gameplay.py"
text = path.read_text(encoding="utf-8")
old = '    assert "test_choose:" in ui and "VillageSkillTestSystem.equippedSkill" in role and "targetsNear" in test\n'
new = '    assert "test_choose:" not in ui and "test_equip:" in ui and "VillageSkillTestSystem.equippedSkill" in role and "targetsNear" in test\n'
if old not in text:
    raise SystemExit("v0177 test action contract marker missing")
path.write_text(text.replace(old, new, 1), encoding="utf-8")

print("Migrated legacy contracts to v0.17.19 behavior")
