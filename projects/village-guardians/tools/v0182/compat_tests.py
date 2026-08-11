from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"
SRC = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

# Minecraft 26.2 renamed the scoreboard-tag read accessor from getTags() to entityTags().
raid = SRC / "VillageRaidSystem.java"
raid_text = raid.read_text(encoding="utf-8")
if "getTags().contains(RAID_ENEMY_TAG)" in raid_text:
    raid_text = raid_text.replace("getTags().contains(RAID_ENEMY_TAG)", "entityTags().contains(RAID_ENEMY_TAG)")
    raid.write_text(raid_text, encoding="utf-8")
if "getTags().contains(RAID_ENEMY_TAG)" in raid.read_text(encoding="utf-8"):
    raise RuntimeError("VillageRaidSystem still uses removed Entity#getTags() API")
if raid.read_text(encoding="utf-8").count("entityTags().contains(RAID_ENEMY_TAG)") < 2:
    raise RuntimeError("VillageRaidSystem raid persistence marker is not checked through Entity#entityTags()")

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

    if path.name == "test_v0177_gameplay.py":
        old = '    assert "entity != null && !entity.isAlive()" in raid\n'
        new = '    assert "if (entity == null)" in raid and "shouldDiscardStaleRaidEnemy" in raid and "entityTags()" in raid\n'
        if old not in updated:
            raise RuntimeError("test_v0177_gameplay.py no longer contains the legacy missing-entity assertion")
        updated = updated.replace(old, new, 1)

    if path.name == "test_v0181_relics.py":
        old = '    assert "Math.min(4, summary.size())" in screen\n'
        new = '    assert "Math.min(7, summary.size())" in screen\n'
        if old not in updated:
            raise RuntimeError("test_v0181_relics.py no longer contains the legacy four-line summary assertion")
        updated = updated.replace(old, new, 1)

    if updated != text:
        path.write_text(updated, encoding="utf-8")

print("Updated Minecraft 26.2 API usage and legacy Village Guardians tests for v0.18.2 contracts.")
