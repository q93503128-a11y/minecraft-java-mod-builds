from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceBuilder.java")
text = path.read_text(encoding="utf-8")

import_anchor = "import net.minecraft.core.BlockPos;\n"
imports = import_anchor + "import net.minecraft.core.registries.BuiltInRegistries;\nimport net.minecraft.resources.Identifier;\n"
if "import net.minecraft.core.registries.BuiltInRegistries;" not in text:
    if text.count(import_anchor) != 1:
        raise SystemExit("residence registry import point missing")
    text = text.replace(import_anchor, imports, 1)

constant_anchor = """    public static final int BEDS_PER_RESIDENCE = 3;

"""
constant = constant_anchor + """    private static final Identifier BED_ID =
            Identifier.fromNamespaceAndPath("minecraft", "red_bed");

"""
if "private static final Identifier BED_ID" not in text:
    if text.count(constant_anchor) != 1:
        raise SystemExit("residence bed identifier insertion point missing")
    text = text.replace(constant_anchor, constant, 1)

text = text.replace("Blocks.RED_BED", "bedBlock()")
if "Blocks.RED_BED" in text:
    raise SystemExit("static colored bed constant remains")

method_anchor = """    private static void addSet(
            IncrementalWorldEditPlan plan,
            BlockPos position,
            Block block) {
"""
method = """    private static Block bedBlock() {
        return BuiltInRegistries.BLOCK.getOptional(BED_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing registered Erden residence bed " + BED_ID));
    }

""" + method_anchor
if "private static Block bedBlock()" not in text:
    if text.count(method_anchor) != 1:
        raise SystemExit("residence bed lookup insertion point missing")
    text = text.replace(method_anchor, method, 1)

path.write_text(text, encoding="utf-8")
