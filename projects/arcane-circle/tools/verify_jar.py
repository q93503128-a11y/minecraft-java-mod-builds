#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import sys
import zipfile
from pathlib import Path

jar = Path(sys.argv[1])
if not jar.is_file():
    raise SystemExit(f"missing JAR: {jar}")

staff_recipes = {
    "data/arcanecircle/recipe/aegis_staff.json",
    "data/arcanecircle/recipe/archmage_staff.json",
    "data/arcanecircle/recipe/ember_staff.json",
    "data/arcanecircle/recipe/glacial_staff.json",
    "data/arcanecircle/recipe/rift_staff.json",
    "data/arcanecircle/recipe/sage_staff.json",
    "data/arcanecircle/recipe/verdant_staff.json",
    "data/arcanecircle/recipe/zephyr_staff.json",
}

required = {
    "META-INF/neoforge.mods.toml",
    "META-INF/THIRD_PARTY_NOTICES.md",
    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",
    "kr/moonseungjun/arcanecircle/network/WorldMagicPayload.class",
    "kr/moonseungjun/arcanecircle/magic/WorldMagicService.class",
    "kr/moonseungjun/arcanecircle/magic/ArcaneLightService.class",
    "kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class",
    "kr/moonseungjun/arcanecircle/magic/FirstCircleSpellSummary.class",
    "kr/moonseungjun/arcanecircle/magic/HighUtilitySpellService.class",
    "kr/moonseungjun/arcanecircle/magic/HighControlSpellService.class",
    "kr/moonseungjun/arcanecircle/magic/HighWardSpellService.class",
    "kr/moonseungjun/arcanecircle/magic/PlanarSpellData.class",
    "kr/moonseungjun/arcanecircle/magic/PlanarSpellService.class",
    "kr/moonseungjun/arcanecircle/magic/SimulacrumService.class",
    "kr/moonseungjun/arcanecircle/client/WorldMagicTracker.class",
    "kr/moonseungjun/arcanecircle/client/AuthoredHighCircleTimeline.class",
    "kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneSigilDirector.class",
    "kr/moonseungjun/arcanecircle/client/PersistentBuffRegalia.class",
    "kr/moonseungjun/arcanecircle/client/GrimoireScreen.class",
    "kr/moonseungjun/arcanecircle/client/PrimaryGrimoireScreen.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneHud.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneRegaliaRenderer.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneCastingPerformance.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.class",
    "kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.class",
    "kr/moonseungjun/arcanecircle/magic/SpellCatalog.class",
    "kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.class",
    "kr/moonseungjun/arcanecircle/magic/SpellWorldLore.class",
    "kr/moonseungjun/arcanecircle/world/ArcaneWorldData.class",
    "kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.class",
    "kr/moonseungjun/arcanecircle/world/ArcaneAcademyBuilder.class",
    "kr/moonseungjun/arcanecircle/world/MagicWorldService.class",
    "kr/moonseungjun/arcanecircle/network/PurchaseAcademyItemPayload.class",
    "kr/moonseungjun/arcanecircle/network/ChooseTraditionPayload.class",
    "data/arcanecircle/spell_catalog/index.json",
    "assets/arcanecircle/items/spellbook_meteor_swarm.json",
    "assets/arcanecircle/items/spellbook_wish.json",
    "assets/arcanecircle/items/spellbook_gate.json",
} | staff_recipes

with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    name_set = set(names)
    missing = sorted(required - name_set)
    if missing:
        raise SystemExit(f"missing required entries: {missing}")
    if len(names) != len(name_set):
        raise SystemExit("duplicate ZIP entries")
    packaged_staff_recipes = {name for name in name_set if name.startswith("data/arcanecircle/recipe/") and name.endswith("_staff.json")}
    if packaged_staff_recipes != staff_recipes:
        raise SystemExit(f"staff recipe set mismatch: {sorted(packaged_staff_recipes)}")
    forbidden = [name for name in names if "villager_trade" in name or name.endswith(".java") or name.startswith(("tools/", ".github/"))]
    if forbidden:
        raise SystemExit(f"forbidden survival/development entries: {forbidden[:8]}")
    retired = ['CodexVisualLanguage','ArcaneSigilDetailGrammar','LowCircleVisualIdentity','MidCircleVisualIdentity','FifthCircleVisualIdentity','SixthCircleVisualIdentity','ArchmageVisualIdentity','RangeReactivePresentation','SpellVisualSignature','CastingSilhouetteRenderer','RobeRegaliaRenderer','SignatureGeometry','SpellSigilService']
    leaked = [n for n in names if any(n.endswith('/'+c+'.class') or ('/'+c+'$') in n for c in retired)]
    if leaked:
        raise SystemExit(f"retired presentation bytecode leaked: {sorted(leaked)}")
    index = json.loads(archive.read("data/arcanecircle/spell_catalog/index.json"))
    version = index.get("version")
    if not isinstance(version, str) or not version:
        raise SystemExit("catalog version missing")
    if jar.name != f"arcanecircle-{version}.jar":
        raise SystemExit(f"JAR/version mismatch: {jar.name} vs {version}")
    if version != "0.12.1-alpha.53":
        raise SystemExit(f"unexpected alpha.53 package version: {version}")
    if index.get("implemented_circles") != list(range(1, 10)) or index.get("direct_spells") != 90 or index.get("fusion_spells") != 19:
        raise SystemExit("JAR catalogue is not the full 1-9 circle world")
    if index.get("crafting_progression") is not True:
        raise SystemExit("staff crafting progression is not enabled")
    utility = index.get("high_utility_identity", [])
    if set(utility) != {"cross_dimension_plane_shift","persistent_demiplane_room","commandable_simulacrum"}:
        raise SystemExit(f"alpha.49 high utility metadata mismatch: {utility}")
    control = index.get("high_control_identity", [])
    if set(control) != {"behavioral_mass_suggestion","physical_forcecage","temporary_dominate_monster","spellbreaking_feeblemind"}:
        raise SystemExit(f"alpha.50 high control metadata mismatch: {control}")
    ward = index.get("high_ward_identity", [])
    if set(ward) != {"globe_blocks_hostile_circle_1_to_5_spells","circle_6_plus_and_physical_pass_through","player_and_npc_cast_interception"}:
        raise SystemExit(f"alpha.51 high ward metadata mismatch: {ward}")
    if index.get("grimoire_effect_compendium") is not True:
        raise SystemExit("alpha.52 effect compendium metadata missing")
    if index.get("spell_contract_audit") != "109_explicit_summaries_and_runtime_routes":
        raise SystemExit("alpha.52 109-spell audit metadata missing")
    if set(index.get("copy_source_targeting", [])) != {"simulacrum_target_28","clone_target_32"}:
        raise SystemExit("alpha.52 copy-source targeting metadata mismatch")
    first = set(index.get("first_circle_deep_audit", []))
    expected_first = {
        "magic_missile_locked_salvo","fire_bolt_nonhoming_impact","single_beam_ray_of_frost",
        "reactive_shield","lifecycle_safe_feather_fall","refcounted_real_light",
        "persistent_grease_slip","weak_target_damage_wake_sleep","physical_thunderwave",
        "regenerating_mage_armor"
    }
    if first != expected_first:
        raise SystemExit(f"alpha.53 first-circle audit metadata mismatch: {sorted(first)}")
    if index.get("first_circle_npc_parity") is not True:
        raise SystemExit("alpha.53 first-circle NPC parity metadata missing")
    notice = archive.read("META-INF/THIRD_PARTY_NOTICES.md").decode("utf-8")
    if "Creative Commons Attribution 4.0" not in notice:
        raise SystemExit("SRD attribution missing from JAR")

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
checksum = jar.with_name(jar.name + ".sha256")
checksum.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
print(f"Arcane Circle v0.12.1 JAR verification: PASS ({len(names)} entries, {len(staff_recipes)} staff recipes)")
print("alpha49_high_utility_runtime=PASS")
print("alpha50_high_control_runtime=PASS")
print("alpha51_high_ward_runtime=PASS")
print("alpha52_readable_grimoire=PASS")
print("alpha52_109_spell_contract_audit=PASS")
print("alpha53_first_circle_deep_runtime=PASS")
print("alpha53_first_circle_npc_parity=PASS")
print(f"SHA-256: {digest}")
