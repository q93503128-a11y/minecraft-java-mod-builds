from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorInventoryManager.java")
text = path.read_text(encoding="utf-8")
anchor = "package kr.moonseungjun.livingkingdoms.world;\n\n"
addition = anchor + "import kr.moonseungjun.livingkingdoms.LivingKingdoms;\n"
assert text.count(anchor) == 1, "package anchor changed"
assert "import kr.moonseungjun.livingkingdoms.LivingKingdoms;" not in text
path.write_text(text.replace(anchor, addition), encoding="utf-8")
