from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceBuilder.java")
text = path.read_text(encoding="utf-8")
old = "addSet(plan, lightPosition(plot), Blocks.LANTERN);"
new = """addSet(plan, lightPosition(plot), Blocks.LANTERN.defaultBlockState()
                .setValue(BlockStateProperties.HANGING, true));"""
if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit("residence lantern pattern missing")
text = text.replace(
    "door.relative(outward, distance).below(2)",
    "door.relative(outward, distance).below()")
text = text.replace(
    "door.relative(footprint.doorFacing()).below(2)",
    "door.relative(footprint.doorFacing()).below()")
if ".below(2)" in text:
    raise SystemExit("recessed residence path calculation remains")
path.write_text(text, encoding="utf-8")
