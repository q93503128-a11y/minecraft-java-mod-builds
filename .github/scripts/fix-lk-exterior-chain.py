from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
text = path.read_text(encoding="utf-8")
assert text.count("Blocks.CHAIN") == 1, "expected one legacy chain block reference"
path.write_text(text.replace("Blocks.CHAIN", "Blocks.IRON_CHAIN"), encoding="utf-8")
