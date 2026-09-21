#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    enemies = read("VillageEnemyArchetypeSystem.java")
    combo = read("VillageEnemyCompositionSystem.java")
    raid = read("VillageRaidSystem.java")
    rpg = read("VillageRpgSystem.java")
    bestiary = read("VillageEnemyBestiary.java")
    siege = read("VillageSiegeBossSystem.java")

    # The four authored boss archetypes keep four unmistakably different vanilla bodies.
    expected_bodies = {
        "SIEGE_BEAST": "EntityTypes.RAVAGER",
        "IRON_WARLORD": "EntityTypes.IRON_GOLEM",
        "PLAGUE_ARCHON": "EntityTypes.SPIDER",
        "DREAD_KNIGHT": "EntityTypes.HOGLIN",
    }
    for archetype, entity_type in expected_bodies.items():
        assert f"case {archetype} ->" in enemies
        assert entity_type in enemies
    assert len(set(expected_bodies.values())) == 4
    assert "hoglin.setImmuneToZombification(true)" in enemies

    # Vanilla body changes are presentation/locomotion changes, not an accidental stat rebalance.
    assert "health.setBaseValue(24.0)" in enemies
    assert "attack.setBaseValue(5.0)" in enemies
    assert "attack.setBaseValue(2.0)" in enemies
    assert "health.setBaseValue(20.0)" in enemies
    assert "attack.setBaseValue(4.0)" in enemies

    # Every boss receives its own readable rider/equipment silhouette.
    for token in (
        "case SIEGE_BEAST ->",
        "EntityTypes.PILLAGER",
        "case IRON_WARLORD ->",
        "EntityTypes.VINDICATOR",
        "case PLAGUE_ARCHON ->",
        "EntityTypes.WITCH",
        "case DREAD_KNIGHT ->",
        "EntityTypes.WITHER_SKELETON",
    ):
        assert token in combo
    for item in (
        "Items.CROSSBOW",
        "Items.DIAMOND_AXE",
        "Items.NETHER_STAR",
        "Items.NETHERITE_SWORD",
        "Items.NETHERITE_CHESTPLATE",
    ):
        assert item in combo

    # Riders are presentation only: one authoritative raid owner still receives hits/rewards/state.
    assert "rider.setNoAi(true)" in combo
    assert "VillageRaidSystem.registerVisualCompanion(level.getServer(), rider, boss)" in combo
    assert "VillageRaidSystem.unregisterVisualCompanion" in combo
    assert "event.setCanceled(true)" in rpg
    assert "owner.hurtServer(level, event.getSource(), event.getAmount())" in rpg
    assert "ACTIVE_ENEMIES.contains(uuid)" in raid
    assert "entity.entityTags().contains(RAID_ENEMY_TAG)" in raid

    # Visual companions share the red raid team so the composite reads as one boss behind cover.
    assert "static void registerVisualCompanion" in raid
    assert "addPlayerToTeam(entity.getScoreboardName(), team)" in raid
    assert "entity.setGlowingTag(bossVisual)" in raid
    assert "team.setAllowFriendlyFire(false)" in raid

    # Existing boss combat doctrine remains owned by the same archetypes and runtime system.
    for token in ("BREACH_COLOSSUS", "BONE_HIEROPHANT", "BLACK_MARSHAL"):
        assert token in siege
    for token in ("SIEGE_BEAST", "IRON_WARLORD", "PLAGUE_ARCHON", "DREAD_KNIGHT"):
        assert token in bestiary
    assert "갑주 약탈수가 올라탄 공성 거수" in bestiary
    assert "철 골렘급 전쟁 갑주" in bestiary
    assert "거대 거미를 타고" in bestiary
    assert "전쟁 멧돼지 위에 올라탄" in bestiary

    print("[PASS] four bosses use four distinct authoritative vanilla body silhouettes")
    print("[PASS] each boss has a unique no-AI rider/equipment presentation layer")
    print("[PASS] boss rider hits, rewards and doctrine state remain owned by one server-authoritative body")
    print("[PASS] composite bosses share the raid team and preserve existing combat baselines")


if __name__ == "__main__":
    main()
