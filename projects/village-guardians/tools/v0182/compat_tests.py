from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"

for path in sorted(TOOLS.glob("test_*.py")):
    text = path.read_text(encoding="utf-8")
    updated = text.replace("mod_version=0.18.1-alpha.1", "mod_version=0.18.2-alpha.1")
    updated = updated.replace("v0.18.0-alpha.1 version is active", "v0.18.2-alpha.1 version is active")
    updated = updated.replace("v0.18.2 version is active", "v0.18.2-alpha.1 version is active")
    if path.name == "test_runtime_safety.py":
        old = '    assert "VillageRelicSystem" in shop\n'
        new = '    assert "VillageRelicSystem" not in shop\n'
        if old not in updated:
            raise RuntimeError("test_runtime_safety.py no longer contains the legacy relic-shop assertion")
        updated = updated.replace(old, new, 1)
    if updated != text:
        path.write_text(updated, encoding="utf-8")

print("Updated legacy Village Guardians tests for v0.18.2 contracts.")
