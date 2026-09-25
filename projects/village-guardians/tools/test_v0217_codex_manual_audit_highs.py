#!/usr/bin/env python3
"""Locks the confirmed HIGH findings from the 2026-09-25 full Codex manual audit."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    assert start in source, start
    part = source.split(start, 1)[1]
    assert end in part, end
    return part.split(end, 1)[0]

def main() -> None:
    raid = read("VillageRaidSystem.java")
    relic = read("VillageRelicSystem.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    abilities = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")
    effect_entity = read("VillageSkillEffectEntity.java")
    bosses = read("VillageSiegeBossSystem.java")
    archetypes = read("VillageEnemyArchetypeSystem.java")
    test_mode = read("VillageSkillTestSystem.java")
    guardians = read("VillageGuardians.java")
    respawn = read("VillageRespawnSystem.java")
    progression = read("VillageProgressionSystem.java")
    fusion = read("VillageFusionSafeScreen.java")
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")

    # H01/H16: mandatory bosses are outside the ordinary population cap and retry resumes failed wave.
    spawn = section(raid, "private static void spawnWave", "private static void directEnemies")
    assert "bossCount = Math.max(0, VillageWarfrontSystem.bonusBossCount" in spawn
    assert "normalCapacity = Math.max(0, MAX_ACTIVE_ENEMIES - ACTIVE_ENEMIES.size())" in spawn
    assert "count = bossCount + normalCount" in spawn
    assert "captureRetryWaveCheckpoint" in raid and '"retry_wave"' in raid
    assert "scheduledStartWave" in raid
    assert "VillageRaidSystem.captureRetryWaveCheckpoint()" in progression
    assert "VillageRaidSystem.clearRetryWaveCheckpoint()" in progression

    # H02/H03: queued relic choices and fusion keep their real reward identity.
    choices = section(relic, "private static List<Relic> choicesFor", "private static List<Relic> pendingChoices")
    assert "!reserved.contains(relic)" not in choices
    pending = section(relic, "private static List<Relic> pendingChoices", "private static java.util.Set<Relic> pendingRelics")
    assert "result.size() >= 3" in pending and "(mask & relic.bit()) == 0" in pending
    fusion_candidates = section(rarity, "public static List<FusionCandidate> fusionCandidates", "public static String combineSelected")
    combine = section(rarity, "public static String combineSelected", "public static List<EnhancementCandidate>")
    assert "VillageEquipmentIdentity.offer(stack)" in fusion_candidates
    assert "VillageEquipmentIdentity.offer(first)" in combine
    assert "VillageEquipmentIdentity.stampOffer(result, offerId)" in combine

    # H04/H05: late skill formulas are compressed and rapid derived arrows share the real 3/5/7 budget.
    assert "RpgProgress.combatScalingLevel(VillageCouncilState.levelOf(id)) * 0.16f" in abilities
    assert "RpgProgress.combatScalingLevel(VillageCouncilState.levelOf(owner.getUUID())) * 0.42f" in abilities
    rapid = section(abilities, "private static void spawnSideArrow", "private static Mob lockArrowOnTarget")
    assert "int specialRank" in rapid
    assert "power, specialRank" in rapid

    # H06/H07: targeting and sequencing are behavior contracts, not just descriptions.
    hawk = section(abilities, "case RANGER_HAWK_MARK -> {", "case RANGER_SPLIT_SHOT -> {")
    sword = section(abilities, "case VANGUARD_SWORD_CHAIN -> {", "case VANGUARD_LIFE_SEVER -> {")
    aa = section(abilities, "case RANGER_AA_INTERCEPT -> {", "case RANGER_DOWNPOUR -> {")
    chain = section(abilities, "case ARCANIST_LIGHTNING_CHAIN -> {", "case ARCANIST_GRAVITY_STORM -> {")
    assert "bestHawkMarkTarget" in hawk
    assert "ActionKind.PROMOTION_BLADE" in sword and "* 2L" in sword
    assert "PromotionSequenceKind.AA_INTERCEPT" in aa and "* 3L" in aa
    assert "PromotionSequenceKind.LIGHTNING_CHAIN" in chain and "* 4L" in chain
    assert "hasClearFlightPath" in chain

    # H08-H10/H17: authored visual radius/arrival/recipient/direction match the server action.
    downpour = section(abilities, "case RANGER_DOWNPOUR -> {", "case RANGER_STAR_TRACKER -> {")
    sky = section(abilities, "case RANGER_SKY_LOCK -> {", "case RANGER_METEOR_BOW -> {")
    meteor = section(abilities, "case RANGER_METEOR_BOW -> {", "case ARCANIST_LAVA_CORE -> {")
    guardian = section(abilities, "case LUMINAR_GUARDIAN_LIGHT -> {", "case LUMINAR_HOLY_PURGE -> {")
    returning = section(abilities, "case LUMINAR_RETURNING_LIGHT -> {", "case LUMINAR_RESURRECTION_HYMN -> {")
    forced = section(abilities, "case WARDEN_FORCED_CHALLENGE -> {", "case WARDEN_GUARD_BARRIER -> {")
    unbroken = section(abilities, "case WARDEN_UNBROKEN_WALL -> {", "case WARDEN_FORTRESS_CHARGE -> {")
    base_tornado = section(abilities, "case ARCANIST_CHAIN -> {", "case ARCANIST_NOVA -> {")
    assert "areaRadius(8.5, specialRank + 1)" in downpour
    assert "double radius = 28.0" in sky and "190, radius" in sky
    assert "speed = (float) (distance / travel)" in meteor
    assert "target.position()" in guardian and "target.position()" in returning
    assert "challengeDuration = 430" in forced and "challengeDuration, radius" in forced
    assert "tauntDuration = 560" in unbroken and "tauntDuration, radius" in unbroken
    assert "specialRank, 0, forward" in base_tornado
    assert '"arcanist_tornado".equals(kind())' not in effect_entity
    assert "Math.max(1, duration)" in effects

    # H11/H12: taunt no longer suppresses boss doctrine; impact requires a recorded warning.
    taunt = section(raid, "public static int tauntEnemies", "public static boolean isAerialEnemy")
    assert "Math.max(radius, 44.0)" not in taunt
    assert "Math.max(160, limit)" not in taunt
    assert "if (VillageRaidSystem.hasActiveTaunt(level, mob))" not in section(
        bosses, "public static void tick(MinecraftServer server)", "public static String previewBossMechanic")
    assert "SIGNATURE_WARNED" in archetypes
    for phase in ("88", "102", "126", "76"):
        assert f"phase == {phase}" in archetypes
    assert archetypes.count("SIGNATURE_WARNED.add(mob.getUUID())") >= 4
    assert archetypes.count("!SIGNATURE_WARNED.remove(mob.getUUID())") >= 4

    # H13/H14: test privilege and combat entities are bounded by real context/lifecycle.
    assert "public static boolean isRegistered" in test_mode
    assert "validTestContext" in test_mode
    assert "VillageCouncilState.currentPhase() != VillageTimePhase.DAY" in test_mode
    assert "VillageRaidSystem.isRaidLocked()" in test_mode
    assert "ARENA_RADIUS + 4" in test_mode
    assert "VillageSkillTestSystem.tick(event.getServer())" in guardians
    assert "VillageSkillTestSystem.isRegistered(player)" in guardians
    assert "VillageSkillTestSystem.isRegistered(player)" in respawn
    assert "inflate(512.0)" in abilities
    assert "inflate(512.0)" in effects

    # H15: clipped candidates cannot steal/trigger the combine hit region.
    click = section(fusion, "public boolean mouseClicked", "public boolean mouseScrolled")
    assert "cellBottom <= grid.top() || y >= grid.bottom()" in click
    assert "visibleTop" in click and "visibleBottom" in click
    assert click.index("toggle(candidates.get(index))") < click.index('"fusion_combine:"')

    assert "mod_version=0.18.48-alpha.1" in props

    print("[PASS] Codex audit HIGH raid/reward/fusion identity contracts are locked")
    print("[PASS] Codex audit HIGH skill targeting, sequencing and VFX semantics are locked")
    print("[PASS] Codex audit HIGH boss warning/taunt and test/retry lifecycle boundaries are locked")

if __name__ == "__main__":
    main()
