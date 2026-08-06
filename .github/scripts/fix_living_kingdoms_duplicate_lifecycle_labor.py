from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorWorkforceManager.java")
text = path.read_text(encoding="utf-8")
block = """        ErdenExteriorLifecycleManager.LaborContribution lifecycleLabor =
                ErdenExteriorLifecycleManager.additionalLabor(level, node.id, node.role, day);
        alive += lifecycleLabor.alive();
        attended += lifecycleLabor.attended();
        absent += lifecycleLabor.absent();
        dead += lifecycleLabor.dead();
"""
duplicate = block + block
if text.count(duplicate) != 1:
    raise SystemExit(f"duplicate lifecycle labor sequence count={text.count(duplicate)}")
text = text.replace(duplicate, block, 1)
if text.count("additionalLabor(level, node.id, node.role, day)") != 1:
    raise SystemExit("lifecycle labor contribution must occur exactly once")
path.write_text(text, encoding="utf-8")
