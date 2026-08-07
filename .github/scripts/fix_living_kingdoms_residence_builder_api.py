from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceBuilder.java")
text = path.read_text(encoding="utf-8")

replacements = {
    "footprint.chunkX() != chunk.x || footprint.chunkZ() != chunk.z":
        "footprint.chunkX() != (chunk.getMinBlockX() >> 4) || footprint.chunkZ() != (chunk.getMinBlockZ() >> 4)",
    "Blocks.WHITE_BED": "Blocks.RED_BED",
    "plan.addSet(lower, lowerState);": "addSet(plan, lower, lowerState);",
    "plan.addSet(lower.above(), upperState);": "addSet(plan, lower.above(), upperState);",
    "plan.addSet(footPos, foot);": "addSet(plan, footPos, foot);",
    "plan.addSet(footPos.east(), head);": "addSet(plan, footPos.east(), head);",
    "plan.addSet(storagePosition(plot), Blocks.BARREL);": "addSet(plan, storagePosition(plot), Blocks.BARREL);",
    "plan.addSet(hearthPosition(plot), Blocks.FURNACE);": "addSet(plan, hearthPosition(plot), Blocks.FURNACE);",
    "plan.addSet(workPosition(plot), Blocks.CRAFTING_TABLE);": "addSet(plan, workPosition(plot), Blocks.CRAFTING_TABLE);",
    "plan.addSet(lightPosition(plot), Blocks.LANTERN);": "addSet(plan, lightPosition(plot), Blocks.LANTERN);",
    "plan.addSet(position, Blocks.DIRT_PATH);": "addSet(plan, position, Blocks.DIRT_PATH);",
    "plan.addSet(step, Blocks.COBBLESTONE);": "addSet(plan, step, Blocks.COBBLESTONE);",
}
for old, new in replacements.items():
    if old in text:
        text = text.replace(old, new)
    elif new not in text:
        raise SystemExit(f"missing residence API pattern: {old}")

helper_anchor = """    private static Block wallFor(String role) {
"""
helpers = """    private static void addSet(
            IncrementalWorldEditPlan plan,
            BlockPos position,
            Block block) {
        plan.addSet(position.getX(), position.getY(), position.getZ(), block);
    }

    private static void addSet(
            IncrementalWorldEditPlan plan,
            BlockPos position,
            BlockState state) {
        plan.addSet(position.getX(), position.getY(), position.getZ(), state);
    }

""" + helper_anchor
if "private static void addSet(\n            IncrementalWorldEditPlan plan,\n            BlockPos position,\n            Block block)" not in text:
    if text.count(helper_anchor) != 1:
        raise SystemExit("residence addSet helper insertion point missing")
    text = text.replace(helper_anchor, helpers, 1)

path.write_text(text, encoding="utf-8")
