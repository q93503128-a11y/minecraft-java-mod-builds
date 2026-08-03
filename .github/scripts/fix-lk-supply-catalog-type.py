from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomSupplyManager.java")
text = path.read_text(encoding="utf-8")
old = "for (NodeTemplate template : NODES)"
new = "for (ErdenKingdomSupplyCatalog.SupplyNode template : NODES)"
assert text.count(old) == 1, "expected one legacy supply node loop type"
path.write_text(text.replace(old, new), encoding="utf-8")
