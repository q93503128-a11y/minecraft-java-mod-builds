#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise SystemExit(f"missing anchor in {path}: {old[:100]!r}")
    write(path, text.replace(old, new, 1))


merc = JAVA / "VillageMercenarySystem.java"
replace_once(
    merc,
    "LEVELS.put(uuid, Math.max(1, Math.min(5, LEVELS.getOrDefault(uuid, 1))));",
    "LEVELS.put(uuid, Math.max(1, Math.min(MAX_LEVEL, LEVELS.getOrDefault(uuid, 1))));",
)

identity = r'''package kr.moonseungjun.villageguardians;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Server-authored identity for tactical consumables; display names alone never grant gameplay effects. */
public final class VillageConsumableIdentity {
    private static final String KEY_MARKER = "villageguardians_consumable";
    private static final String KEY_ID = "villageguardians_consumable_id";

    private VillageConsumableIdentity() {}

    public static void stamp(ItemStack stack, String id) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = tagCopy(stack);
        tag.putBoolean(KEY_MARKER, true);
        tag.putString(KEY_ID, id == null ? "" : id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String id(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        CompoundTag tag = data.copyTag();
        if (!tag.getBooleanOr(KEY_MARKER, false)) return "";
        return tag.getStringOr(KEY_ID, "");
    }

    private static CompoundTag tagCopy(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        return existing == null ? new CompoundTag() : existing.copyTag();
    }
}
'''
write(JAVA / "VillageConsumableIdentity.java", identity)

consumable = JAVA / "VillageConsumableSystem.java"
replace_once(
    consumable,
    '''        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(consumable.displayName()).withStyle(consumable.color()));
        if (!player.addItem(stack)) player.drop(stack, false);''',
    '''        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(consumable.displayName()).withStyle(consumable.color()));
        VillageConsumableIdentity.stamp(stack, consumable.id());
        if (!player.addItem(stack)) player.drop(stack, false);''',
)
replace_once(
    consumable,
    '''    private static Consumable match(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom == null) return null;
        String plain = ChatFormatting.stripFormatting(custom.getString());
        for (Consumable consumable : Consumable.values()) {
            if (stack.getItem() == consumable.item() && consumable.displayName().equals(plain)) return consumable;
        }
        return null;
    }
''',
    '''    private static Consumable match(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Consumable consumable = Consumable.fromId(VillageConsumableIdentity.id(stack));
        return consumable != null && stack.getItem() == consumable.item() ? consumable : null;
    }
''',
)

test = ROOT / "tools/test_v01818_growth_consumables.py"
text = read(test)
text = text.replace(
    '    assert "ARCANE_SURGE_UNTIL" in consumable and "1.20f" in consumable\n',
    '    assert "ARCANE_SURGE_UNTIL" in consumable and "1.20f" in consumable\n'
    '    identity = read("VillageConsumableIdentity.java")\n'
    '    assert "villageguardians_consumable_id" in identity\n'
    '    assert "VillageConsumableIdentity.stamp(stack, consumable.id())" in consumable\n'
    '    assert "Consumable.fromId(VillageConsumableIdentity.id(stack))" in consumable\n',
)
text = text.replace(
    '    assert "8.0 + Math.min(13.0, rank * 0.22)" in merc\n',
    '    assert "8.0 + Math.min(13.0, rank * 0.22)" in merc\n'
    '    assert "Math.min(5, LEVELS.getOrDefault" not in merc\n'
    '    assert "Math.min(MAX_LEVEL, LEVELS.getOrDefault(uuid, 1))" in merc\n',
)
write(test, text)

# Current canonical regression tests must track the current source version while preserving their feature contracts.
for name in (
    "test_v0189_siege_phase2.py",
    "test_v01810_ranger_ricochet.py",
    "test_v01811_defense_polish.py",
    "test_v01812_quality_audit.py",
    "test_v01813_siege_integration.py",
    "test_v01814_persistent_presentation.py",
    "test_v01815_boss_identity.py",
    "test_v01816_mobile_defense_ui.py",
):
    path = ROOT / "tools" / name
    if not path.exists():
        continue
    source = read(path)
    source = re.sub(r'mod_version=0\.18\.\d+(?:-alpha\.\d+)?',
                    'mod_version=0.18.18-alpha.1', source)
    write(path, source)

print("[PASS] v0.18.18 audit hotfix applied")
