from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
def read(name): return (JAVA / name).read_text(encoding="utf-8")
def main():
    assert "mod_version=" in (ROOT / "gradle.properties").read_text()
    rarity, ui = read("VillageEquipmentRaritySystem.java"), read("VillageUiController.java")
    common, data, tree_ui = read("VillageSkillTreeSystem.java"), read("VillageSkillTreeData.java"), read("VillageSkillTreeScreen.java")
    raid, enemy = read("VillageRaidSystem.java"), read("VillageEnemyArchetypeSystem.java")
    intel, loot = read("VillageWaveIntelSystem.java"), read("VillageRaidLootSystem.java")
    progression, council = read("VillageProgressionSystem.java"), read("VillageCouncilState.java")
    test, role = read("VillageSkillTestSystem.java"), read("VillageRoleSkillSystem.java")
    assert "currentEffect" in rarity and "nextEffect" in rarity
    assert "현재 수치:" in ui and "강화 후 수치:" in ui
    assert "Math.max(0, level - 1)" in common and common.count('("mobility_') == 10
    assert "Codec.LONG" in data and "spent_points_v2" in data
    assert 'node.pointCost() + "P"' in tree_ui
    assert "isEnemyIgnoredElevation" in read("VillageLocationRules.java")
    assert "if (entity == null)" in raid and "shouldDiscardStaleRaidEnemy" in raid and "entityTags()" in raid
    assert "if (VillageProgressionSystem.isGameOver()) return;" in raid
    assert "setPersistenceRequired" in enemy
    assert "List<WavePreview>" in intel and "previewArchetype" in intel
    assert "금 간 전열병 송곳니" in loot and "공포 기사의 암흑 갑편" in loot
    assert "captureNightStartSnapshot" in progression and "retryPlanLocked = true" in progression
    assert "villageDay = fromStart ? 1 : Math.max(1, villageDay)" in council
    assert "test_choose:" not in ui and "test_equip:" in ui and "VillageSkillTestSystem.equippedSkill" in role and "targetsNear" in test
    print("Village Guardians v0.17.7 contracts passed.")
if __name__ == "__main__": main()
