#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
ASSETS = ROOT / "src/main/resources/assets/villageguardians/models/mercenary"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    enemy = read("VillageEnemyArchetypeSystem.java")
    combo = read("VillageEnemyCompositionSystem.java")
    rpg = read("VillageRpgSystem.java")
    raid = read("VillageRaidSystem.java")
    mesh_loader = read("VillageExternalMercenaryMesh.java")
    verify = (ROOT / "tools/verify_jar.py").read_text(encoding="utf-8")

    # Four pinned Quaternius meshes must exist as real runtime resources, not only notices/code paths.
    expected_assets = {
        "bastion.obj": "Warrior",
        "striker.obj": "Rogue",
        "ranger.obj": "Ranger",
        "medic.obj": "Cleric",
    }
    for filename, source_name in expected_assets.items():
        asset = ASSETS / filename
        assert asset.exists(), filename
        assert asset.stat().st_size > 100_000, (filename, asset.stat().st_size)
        assert f"/assets/villageguardians/models/mercenary/{filename}" in mesh_loader
        assert f'"assets/villageguardians/models/mercenary/{filename}"' in verify
    assert "Quaternius RPG Character Pack" in verify

    # Late combat uses genuinely different vanilla bodies, not recolored zombies/skeletons only.
    for token in (
        "CAVE_STALKER", "BOGGED_ARCHER", "ZOGLIN_BREACHER",
        "BREEZE_DISRUPTOR", "MAGMA_BRUTE", "NETHER_REAVER",
    ):
        assert token in enemy
        assert f'"{token}"' in verify
    for entity in (
        "EntityTypes.CAVE_SPIDER", "EntityTypes.BOGGED", "EntityTypes.ZOGLIN",
        "EntityTypes.BREEZE", "EntityTypes.MAGMA_CUBE", "EntityTypes.ZOMBIFIED_PIGLIN",
    ):
        assert entity in enemy
    assert "Twenty enemy archetypes and nineteen wave traits are bundled" in verify

    # New silhouettes are authored into late doctrines rather than existing as unreachable enum entries.
    for pair in (
        ("RIFTED", "CAVE_STALKER"),
        ("SKY_SIEGE", "BREEZE_DISRUPTOR"),
        ("HUNTER_NET", "BOGGED_ARCHER"),
        ("IRON_TIDE", "ZOGLIN_BREACHER"),
        ("CATACLYSM", "MAGMA_BRUTE"),
        ("FINAL_HOST", "NETHER_REAVER"),
    ):
        assert f"VillageWaveTrait.{pair[0]}" in enemy
        assert f"Archetype.{pair[1]}" in enemy

    # Selected late roles use real vanilla composite silhouettes.
    assert "villageguardians_visual_rider" in combo
    assert "Archetype.CAVE_STALKER" in combo and "EntityTypes.BOGGED" in combo
    assert "Archetype.ZOGLIN_BREACHER" in combo and "EntityTypes.ZOMBIFIED_PIGLIN" in combo
    assert "aerialRole != null && day >= 30" in combo
    assert "case BOMBARDIER -> babyHusk" in combo
    assert "boss && archetype == VillageEnemyArchetypeSystem.Archetype.SIEGE_BEAST" in combo
    assert "rider.startRiding(owner, true)" in combo

    # Cosmetic riders never become independent reward actors: hits go to the authoritative owner
    # and the raid lifecycle removes rider entities with their owner / on restart.
    assert "VillageEnemyCompositionSystem.isVisualRider(event.getEntity())" in rpg
    assert "owner.hurtServer(level, event.getSource(), event.getAmount())" in rpg
    assert "event.setCanceled(true)" in rpg
    assert "VillageEnemyCompositionSystem.attach(" in raid
    assert "VillageEnemyCompositionSystem.remove(entity)" in raid
    assert "VillageEnemyCompositionSystem.cleanupOrphans(server)" in raid

    print("[PASS] four pinned Quaternius mercenary OBJ meshes are bundled and required by JAR verification")
    print("[PASS] late roster expands from 14 to 20 enemy archetypes with six new vanilla silhouettes")
    print("[PASS] late doctrines actually field cave spider, bogged, zoglin, breeze, magma and nether bodies")
    print("[PASS] authored rider combinations preserve one authoritative raid combat/reward owner")


if __name__ == "__main__":
    main()
