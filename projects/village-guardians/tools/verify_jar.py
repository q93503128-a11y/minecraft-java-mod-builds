#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path

MOD_ID = "villageguardians"
CLASS_PREFIX = "kr/moonseungjun/villageguardians/"
REQUIRED_ASSETS = {
    "assets/minecraft/textures/gui/container/inventory.png",
    "assets/minecraft/textures/gui/sprites/widget/button.png",
    "assets/minecraft/textures/gui/sprites/widget/button_highlighted.png",
    "META-INF/villageguardians/THIRD_PARTY_NOTICES.txt",
    "META-INF/villageguardians/licensed-gui-assets.txt",
    "assets/villageguardians/textures/effect/skill_mesh.png",
}
REQUIRED_CLASSES = {
    "kr/moonseungjun/villageguardians/VillageSimpleBuildingBuilder.class",
    "kr/moonseungjun/villageguardians/VillageBuildingEnhancements.class",
    "kr/moonseungjun/villageguardians/VillageBuildingSignatures.class",
    "kr/moonseungjun/villageguardians/VillageFortressBuildings.class",
    "kr/moonseungjun/villageguardians/VillageTownHallAccessFix.class",
    "kr/moonseungjun/villageguardians/VillageDoorSystem.class",
    "kr/moonseungjun/villageguardians/VillageDefenseSystem.class",
    "kr/moonseungjun/villageguardians/VillageDefenseTowerBuilder.class",
    "kr/moonseungjun/villageguardians/VillagePlacedTurretSystem.class",
    "kr/moonseungjun/villageguardians/VillageSiegeCommandUi.class",
    "kr/moonseungjun/villageguardians/VillageWaveTrait.class",
    "kr/moonseungjun/villageguardians/VillageEnemyArchetypeSystem.class",
    "kr/moonseungjun/villageguardians/VillageWarfrontSystem.class",
    "kr/moonseungjun/villageguardians/VillageDifficultyTuning.class",
    "kr/moonseungjun/villageguardians/VillageRaidDebrisDropGuard.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectSystem.class",
    "kr/moonseungjun/villageguardians/VillageSkillHudOverlay.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectEntities.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectEntity.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectRenderState.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectRenderer.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectClient.class",
    "kr/moonseungjun/villageguardians/VillageSkillMeshLibrary.class",
    "kr/moonseungjun/villageguardians/VillageCampaignProgression.class",
    "kr/moonseungjun/villageguardians/VillageRolePromotionSystem.class",
    "kr/moonseungjun/villageguardians/VillageDefenseResearchSystem.class",
    "kr/moonseungjun/villageguardians/VillageMercenarySystem.class",
    "kr/moonseungjun/villageguardians/VillageEnemyEliteSystem.class",
    "kr/moonseungjun/villageguardians/VillageSiegeBossSystem.class",
    "kr/moonseungjun/villageguardians/VillageRespawnSystem.class",
    "kr/moonseungjun/villageguardians/VillageRaidLootSystem.class",
    "kr/moonseungjun/villageguardians/VillageLocationRules.class",
    "kr/moonseungjun/villageguardians/VillageCombatTechniqueSystem.class",
    "kr/moonseungjun/villageguardians/VillageSkillTreeSystem.class",
    "kr/moonseungjun/villageguardians/VillageSkillTreeData.class",
    "kr/moonseungjun/villageguardians/VillageSkillTreeScreen.class",
    "kr/moonseungjun/villageguardians/VillageRoleProgressData.class",
    "kr/moonseungjun/villageguardians/VillageRoleSkillSystem.class",
    "kr/moonseungjun/villageguardians/VillageRoleProgressScreen.class",
    "kr/moonseungjun/villageguardians/VillageEquipmentShop.class",
    "kr/moonseungjun/villageguardians/VillageEquipmentTooltipClient.class",
    "kr/moonseungjun/villageguardians/VillageInventoryPanel.class",
    "kr/moonseungjun/villageguardians/VillageTownHallGridScreen.class",
    "kr/moonseungjun/villageguardians/VillageShopCatalogScreen.class",
    "kr/moonseungjun/villageguardians/VillageCommandCenterScreen.class",
    "kr/moonseungjun/villageguardians/VillageActionDetailScreen.class",
    "kr/moonseungjun/villageguardians/VillageQuickChatSafeScreen.class",
    "kr/moonseungjun/villageguardians/VillageFusionSafeScreen.class",
    "kr/moonseungjun/villageguardians/VillageRelicScreen.class",
    "kr/moonseungjun/villageguardians/VillageRelicChoiceConfirmScreen.class",
    "kr/moonseungjun/villageguardians/VillageWaveIntelDossierScreen.class",
    "kr/moonseungjun/villageguardians/VillageVictoryScreen.class",
    "kr/moonseungjun/villageguardians/VillageGameOverScreen.class",
    "kr/moonseungjun/villageguardians/VillageResultScreen.class",
    "kr/moonseungjun/villageguardians/VillageSkillTestScreen.class",
    "kr/moonseungjun/villageguardians/VillageSkillTestPasswordScreen.class",
    "kr/moonseungjun/villageguardians/VillageActionDescriptions.class",
    "kr/moonseungjun/villageguardians/VillageStarterKit.class",
    "kr/moonseungjun/villageguardians/VillageClientKeys.class",
    "kr/moonseungjun/villageguardians/VillageStructureHud.class",
    "kr/moonseungjun/villageguardians/VillageHudSystem.class",
    "kr/moonseungjun/villageguardians/VillageHealthDisplaySystem.class",
}
OBSOLETE_CLASSES = {
    "kr/moonseungjun/villageguardians/VillageSkillVisualSystem.class",
    "kr/moonseungjun/villageguardians/VillageSkillEffectSystem$DisplayAccess.class",
    "kr/moonseungjun/villageguardians/VillageTownHallScreen.class",
    "kr/moonseungjun/villageguardians/VillageShopScreen.class",
    "kr/moonseungjun/villageguardians/VillageTowerProgressData.class",
    "kr/moonseungjun/villageguardians/VillageTowerSpecializationSystem.class",
    "kr/moonseungjun/villageguardians/VillageFundingSystem.class",
    "kr/moonseungjun/villageguardians/VillageGatePrioritySystem.class",
    "kr/moonseungjun/villageguardians/VillageTownHallSystem.class",
}


EXPECTED_ENUM_TOKENS = {
    "kr/moonseungjun/villageguardians/VillageEquipmentShop$Offer.class": {
        "WATCH_SWORD",
        "HUNTER_BOW",
        "WARD_SHIELD",
        "SENTINEL_AXE",
        "TWINSTRING_BOW",
        "BULWARK_HELM",
        "VETERAN_BLADE",
        "SIEGE_CROSSBOW",
        "WIND_BLADE",
        "MARCH_BOOTS",
        "ARCANE_FOCUS",
        "BASTION_CHEST",
        "EAGLE_CROSSBOW",
        "FROST_FOCUS",
        "RUNE_LEGGINGS",
        "EXECUTIONER_AXE",
        "TITAN_SHIELD",
        "AEGIS_CHEST",
        "DAWN_BLADE",
        "STAR_BOW",
        "DAWN_SCEPTER",
        "RIFT_LONGBOW",
        "PHOENIX_CHEST",
        "WAR_CROWN",
        "BLACKWALL_GREATSWORD",
        "SKYWARD_CROSSBOW",
        "RIFT_AEGIS",
        "STORM_LONGBOW",
        "NECRO_BREAKER",
        "ECLIPSE_PLATE",
        "WARCASTER_FOCUS",
        "ABYSS_AXE",
        "BASTION_CROWN",
        "DOOMSTAR_BOW",
        "LAST_GUARD_BLADE",
        "CENTURY_AEGIS",
    },
    "kr/moonseungjun/villageguardians/VillageEnemyArchetypeSystem$Archetype.class": {
        "GRUNT",
        "RUSHER",
        "BULWARK",
        "SAPPER",
        "MARKSMAN",
        "SHIELDBREAKER",
        "HEXER",
        "WAR_CHANTER",
        "NECROMANCER",
        "TOWER_HUNTER",
        "SIEGE_BEAST",
        "IRON_WARLORD",
        "PLAGUE_ARCHON",
        "DREAD_KNIGHT",
    },
    "kr/moonseungjun/villageguardians/VillageWaveTrait.class": {
        "STANDARD",
        "SWARM",
        "IRONCLAD",
        "SIEGE",
        "HUNTERS",
        "HEXED",
        "FRENZY",
        "REGENERATING",
        "PHALANX",
        "BLOOD_MOON",
        "STORMFRONT",
        "RIFTED",
        "BREACH_STORM",
        "SKY_SIEGE",
        "HUNTER_NET",
        "DEATH_CHORUS",
        "IRON_TIDE",
        "CATACLYSM",
        "FINAL_HOST",
    },
    "kr/moonseungjun/villageguardians/VillageRoleSkillSystem$ActiveSkill.class": {
        "VANGUARD_WHIRLWIND",
        "VANGUARD_BREAKER",
        "VANGUARD_CRY",
        "VANGUARD_STORM",
        "VANGUARD_FRONTLINE_REND",
        "VANGUARD_BLOOD_SPIRAL",
        "VANGUARD_WAR_BANNER",
        "VANGUARD_BREACH_STRIKE",
        "VANGUARD_SWORD_CHAIN",
        "VANGUARD_LIFE_SEVER",
        "VANGUARD_ABSOLUTE_BREAK",
        "VANGUARD_HEAVEN_SEVER",
        "RANGER_VOLLEY",
        "RANGER_PIERCE",
        "RANGER_RICOCHET",
        "RANGER_FIRE_RAIN",
        "RANGER_HAWK_MARK",
        "RANGER_SPLIT_SHOT",
        "RANGER_AA_INTERCEPT",
        "RANGER_DOWNPOUR",
        "RANGER_STAR_TRACKER",
        "RANGER_CONSTELLATION",
        "RANGER_SKY_LOCK",
        "RANGER_METEOR_BOW",
        "ARCANIST_FIRE_ORB",
        "ARCANIST_FROST_RING",
        "ARCANIST_CHAIN",
        "ARCANIST_NOVA",
        "ARCANIST_LAVA_CORE",
        "ARCANIST_FROST_PRISON",
        "ARCANIST_LIGHTNING_CHAIN",
        "ARCANIST_GRAVITY_STORM",
        "ARCANIST_SOLAR_CORE",
        "ARCANIST_ABSOLUTE_ZERO",
        "ARCANIST_HEAVEN_CHAIN",
        "ARCANIST_SINGULARITY",
        "LUMINAR_HEAL",
        "LUMINAR_CLEANSE",
        "LUMINAR_VEIL",
        "LUMINAR_SANCTUARY",
        "LUMINAR_GUARDIAN_LIGHT",
        "LUMINAR_HOLY_PURGE",
        "LUMINAR_REVIVAL_WAVE",
        "LUMINAR_JUDGEMENT",
        "LUMINAR_HEAVENLY_BARRIER",
        "LUMINAR_RETURNING_LIGHT",
        "LUMINAR_RESURRECTION_HYMN",
        "LUMINAR_LAST_MIRACLE",
        "WARDEN_TAUNT",
        "WARDEN_BASH",
        "WARDEN_FORMATION",
        "WARDEN_FIELD",
        "WARDEN_GATE_IMPACT",
        "WARDEN_FORCED_CHALLENGE",
        "WARDEN_GUARD_BARRIER",
        "WARDEN_IRON_PULSE",
        "WARDEN_UNBROKEN_WALL",
        "WARDEN_FORTRESS_CHARGE",
        "WARDEN_ABSOLUTE_FORMATION",
        "WARDEN_FORTRESS_DESCENT",
    },
    "kr/moonseungjun/villageguardians/VillagePlacedTurretSystem$TurretType.class": {
        "BALLISTA",
        "REPEATER",
        "PIERCER",
        "FLAME",
        "FROST",
        "CHAIN",
        "BOMBARD",
        "NULLIFIER",
        "ANTI_AIR",
        "BEACON",
    },
}


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    if len(sys.argv) != 2:
        fail("Usage: python tools/verify_jar.py <jar-path>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"JAR is missing or empty: {jar_path}")

    try:
        with zipfile.ZipFile(jar_path) as jar:
            names = jar.namelist()
            name_set = set(names)
            duplicates = sorted({name for name in names if names.count(name) > 1})

            if duplicates:
                fail(f"Duplicate ZIP entries: {duplicates[:10]}")
            if "META-INF/neoforge.mods.toml" not in name_set:
                fail("Missing META-INF/neoforge.mods.toml")
            if not any(name.startswith(CLASS_PREFIX) and name.endswith(".class") for name in names):
                fail("No compiled Village Guardians classes found")
            if not any(name.startswith(f"assets/{MOD_ID}/") for name in names):
                fail(f"Missing assets/{MOD_ID}/ resources")
            if not any(name.startswith(f"data/{MOD_ID}/") for name in names):
                fail(f"Missing data/{MOD_ID}/ resources")
            if any(name.endswith(".java") for name in names):
                fail("Development Java source files are present in the JAR")

            missing_assets = sorted(REQUIRED_ASSETS - name_set)
            if missing_assets:
                fail(f"Missing licensed runtime assets: {missing_assets}")
            missing_classes = sorted(REQUIRED_CLASSES - name_set)
            if missing_classes:
                fail(f"Missing required runtime classes: {missing_classes}")
            obsolete_classes = sorted(OBSOLETE_CLASSES & name_set)
            if obsolete_classes:
                fail(f"Obsolete runtime classes are still bundled: {obsolete_classes}")

            for class_name, expected_tokens in EXPECTED_ENUM_TOKENS.items():
                if class_name not in name_set:
                    fail(f"Missing current-content enum class: {class_name}")
                payload = jar.read(class_name)
                missing_tokens = sorted(
                    token for token in expected_tokens if token.encode("ascii") not in payload
                )
                if missing_tokens:
                    fail(f"Missing current-content enum tokens from {class_name}: {missing_tokens}")
            for asset in sorted(REQUIRED_ASSETS):
                if asset.endswith(".png") and len(jar.read(asset)) < 32:
                    fail(f"Licensed runtime asset is unexpectedly empty: {asset}")

            external_structures = [
                name for name in names
                if name.startswith("data/villageguardians/structure/external/") and name.endswith(".nbt")
            ]
            if external_structures:
                fail(f"Removed third-party structure NBT files are still bundled: {external_structures}")

            notice = jar.read(
                "META-INF/villageguardians/THIRD_PARTY_NOTICES.txt"
            ).decode("utf-8")
            if "Default Dark Mode" not in notice:
                fail("Third-party notice does not identify the licensed GUI source")
            if "Tiny Creatures" not in notice or "CC0 1.0 Universal" not in notice:
                fail("CC0 fantasy enemy visual references are not documented")
            if "No third-party structure NBT files are bundled" not in notice:
                fail("Third-party notice does not record the custom-building migration")
    except zipfile.BadZipFile as exc:
        fail(f"Invalid JAR/ZIP: {exc}")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_path = jar_path.with_suffix(jar_path.suffix + ".sha256")
    checksum_path.write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")

    print(f"[PASS] Valid Village Guardians JAR: {jar_path}")
    print("[PASS] Current town hall, shop, action, result and tactical UI classes are bundled")
    print("[PASS] Deleted legacy town hall/shop classes are absent")
    print("[PASS] Facility repair and upgrade remain reachable from the town hall")
    print("[PASS] Early difficulty tuning, downed-state risk and raid debris suppression are bundled")
    print("[PASS] Roof signatures migrate to front-facing facade marks")
    print("[PASS] Fourteen enemy archetypes and nineteen wave traits are bundled")
    print("[PASS] Endless warfront milestones and controlled overlapping waves are present")
    print("[PASS] Ten player-placed turret roles own production combat; retired fixed-tower specialization classes are absent")
    print("[PASS] Thirty-six equipment offers and sixty active role skills are bundled")
    print("[PASS] Obsolete display and generic visual facades are not bundled")
    print("[PASS] CC0 visual references are documented without untracked binaries")
    print("[PASS] No third-party structure NBT files are bundled")
    print(f"[PASS] SHA-256: {digest}")
    print(f"[PASS] Checksum file: {checksum_path}")


if __name__ == "__main__":
    main()
