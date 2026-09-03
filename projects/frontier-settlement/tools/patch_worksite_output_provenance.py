from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java"
text = PATH.read_text(encoding="utf-8")

if "import net.minecraft.tags.ItemTags;" not in text:
    text = text.replace("import net.minecraft.tags.BlockTags;\n", "import net.minecraft.tags.BlockTags;\nimport net.minecraft.tags.ItemTags;\n", 1)
if "import net.minecraft.world.item.BlockItem;" not in text:
    text = text.replace("import net.minecraft.world.item.Item;\n", "import net.minecraft.world.item.BlockItem;\nimport net.minecraft.world.item.Item;\n", 1)

old = '''    private static boolean isExportableWorksiteOutput(BuildingType type, ItemStack stack) {\n        if (stack == null || stack.isEmpty() || type == null) return false;\n        return switch (type) {\n            case LUMBER_CAMP -> SettlementInventory.isWood(stack);\n            case FARM -> SettlementInventory.isFood(stack);\n            case QUARRY -> SettlementInventory.isStone(stack);\n            // Mine output includes fuels, gems and other exact catalysts as well as metal-category\n            // stacks. Its managed barrel is an output buffer, not a personal chest, so export all.\n            case MINE -> true;\n            default -> false;\n        };\n    }\n'''
new = '''    private static boolean isExportableWorksiteOutput(BuildingType type, ItemStack stack) {\n        if (stack == null || stack.isEmpty() || type == null) return false;\n        return switch (type) {\n            // Managed worksite barrels are visible physical buffers and players can interact with them.\n            // Export only items that this profession can actually create; never vacuum arbitrary player\n            // storage merely because it happens to sit in a managed barrel.\n            case LUMBER_CAMP -> stack.is(ItemTags.LOGS);\n            case FARM -> stack.is(Items.WHEAT);\n            case QUARRY -> isQuarryOutputItem(stack);\n            case MINE -> isMineOutputItem(stack);\n            default -> false;\n        };\n    }\n\n    private static boolean isQuarryOutputItem(ItemStack stack) {\n        return stack.is(Items.STONE) || stack.is(Items.DEEPSLATE) || stack.is(Items.ANDESITE)\n                || stack.is(Items.DIORITE) || stack.is(Items.GRANITE) || stack.is(Items.TUFF);\n    }\n\n    private static boolean isMineOutputItem(ItemStack stack) {\n        if (stack.is(Items.RAW_IRON) || stack.is(Items.RAW_COPPER) || stack.is(Items.RAW_GOLD)\n                || stack.is(Items.COAL) || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)\n                || stack.is(Items.REDSTONE) || stack.is(Items.LAPIS_LAZULI)) return true;\n        // Unknown companion ores are mined as their ore-block item by previewMineDrop(). Preserve that\n        // compatibility without treating unrelated blocks, tools, food or equipment as mine output.\n        return stack.getItem() instanceof BlockItem blockItem\n                && blockItem.getBlock().defaultBlockState().is(Tags.Blocks.ORES);\n    }\n'''
if new not in text:
    if text.count(old) != 1:
        raise SystemExit(f"worksite export anchor count={text.count(old)}")
    text = text.replace(old, new, 1)

PATH.write_text(text, encoding="utf-8")
current = PATH.read_text(encoding="utf-8")
required = [
    "case LUMBER_CAMP -> stack.is(ItemTags.LOGS)",
    "case FARM -> stack.is(Items.WHEAT)",
    "case QUARRY -> isQuarryOutputItem(stack)",
    "case MINE -> isMineOutputItem(stack)",
    "blockItem.getBlock().defaultBlockState().is(Tags.Blocks.ORES)",
]
for token in required:
    if token not in current:
        raise SystemExit(f"worksite provenance invariant missing: {token}")
for forbidden in ["case MINE -> true;", "case FARM -> SettlementInventory.isFood(stack);", "case LUMBER_CAMP -> SettlementInventory.isWood(stack);"]:
    if forbidden in current:
        raise SystemExit(f"unsafe worksite export remains: {forbidden}")

print("WORKSITE OUTPUT PROVENANCE PATCH PASS")
