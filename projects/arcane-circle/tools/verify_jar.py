#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import sys
import zipfile
from pathlib import Path

jar=Path(sys.argv[1])
if not jar.is_file(): raise SystemExit(f"missing JAR: {jar}")

staff_recipes={
 "data/arcanecircle/recipe/aegis_staff.json","data/arcanecircle/recipe/archmage_staff.json",
 "data/arcanecircle/recipe/ember_staff.json","data/arcanecircle/recipe/glacial_staff.json",
 "data/arcanecircle/recipe/rift_staff.json","data/arcanecircle/recipe/sage_staff.json",
 "data/arcanecircle/recipe/verdant_staff.json","data/arcanecircle/recipe/zephyr_staff.json",
}
required={
 "META-INF/neoforge.mods.toml","META-INF/THIRD_PARTY_NOTICES.md",
 "kr/moonseungjun/arcanecircle/ArcaneCircle.class","kr/moonseungjun/arcanecircle/network/WorldMagicPayload.class",
 "kr/moonseungjun/arcanecircle/magic/WorldMagicService.class","kr/moonseungjun/arcanecircle/magic/ArcaneLightService.class",
 "kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class","kr/moonseungjun/arcanecircle/magic/SecondCircleSpellService.class",
 "kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.class","kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.class",
 "kr/moonseungjun/arcanecircle/magic/FifthCircleSpellService.class","kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.class",
 "kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellService.class",
 "kr/moonseungjun/arcanecircle/magic/FirstCircleSpellSummary.class","kr/moonseungjun/arcanecircle/magic/SixthCircleSpellSummary.class",
 "kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellSummary.class",
 "kr/moonseungjun/arcanecircle/magic/HighUtilitySpellService.class","kr/moonseungjun/arcanecircle/magic/HighControlSpellService.class",
 "kr/moonseungjun/arcanecircle/magic/HighWardSpellService.class","kr/moonseungjun/arcanecircle/magic/PlanarSpellData.class",
 "kr/moonseungjun/arcanecircle/magic/PlanarSpellService.class","kr/moonseungjun/arcanecircle/magic/SimulacrumService.class",
 "kr/moonseungjun/arcanecircle/client/WorldMagicTracker.class","kr/moonseungjun/arcanecircle/client/AuthoredHighCircleTimeline.class",
 "kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.class","kr/moonseungjun/arcanecircle/client/ArcaneSigilDirector.class",
 "kr/moonseungjun/arcanecircle/client/PersistentBuffRegalia.class","kr/moonseungjun/arcanecircle/client/GrimoireScreen.class",
 "kr/moonseungjun/arcanecircle/client/PrimaryGrimoireScreen.class","kr/moonseungjun/arcanecircle/client/ArcaneHud.class",
 "kr/moonseungjun/arcanecircle/client/ArcaneRegaliaRenderer.class","kr/moonseungjun/arcanecircle/client/ArcaneCastingPerformance.class",
 "kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.class","kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.class",
 "kr/moonseungjun/arcanecircle/magic/SpellCatalog.class","kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.class",
 "kr/moonseungjun/arcanecircle/magic/SpellWorldLore.class","kr/moonseungjun/arcanecircle/world/ArcaneWorldData.class",
 "kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.class","kr/moonseungjun/arcanecircle/world/ArcaneAcademyBuilder.class",
 "kr/moonseungjun/arcanecircle/world/MagicWorldService.class","kr/moonseungjun/arcanecircle/network/PurchaseAcademyItemPayload.class",
 "kr/moonseungjun/arcanecircle/network/ChooseTraditionPayload.class","data/arcanecircle/spell_catalog/index.json",
 "assets/arcanecircle/items/spellbook_meteor_swarm.json","assets/arcanecircle/items/spellbook_wish.json",
 "assets/arcanecircle/items/spellbook_gate.json",
}|staff_recipes

with zipfile.ZipFile(jar) as archive:
 names=archive.namelist(); name_set=set(names)
 missing=sorted(required-name_set)
 if missing: raise SystemExit(f"missing required entries: {missing}")
 if len(names)!=len(name_set): raise SystemExit("duplicate ZIP entries")
 packaged={n for n in name_set if n.startswith('data/arcanecircle/recipe/') and n.endswith('_staff.json')}
 if packaged!=staff_recipes: raise SystemExit(f"staff recipe set mismatch: {sorted(packaged)}")
 forbidden=[n for n in names if 'villager_trade' in n or n.endswith('.java') or n.startswith(('tools/','.github/'))]
 if forbidden: raise SystemExit(f"forbidden survival/development entries: {forbidden[:8]}")
 retired=['CodexVisualLanguage','ArcaneSigilDetailGrammar','LowCircleVisualIdentity','MidCircleVisualIdentity','FifthCircleVisualIdentity','SixthCircleVisualIdentity','ArchmageVisualIdentity','RangeReactivePresentation','SpellVisualSignature','CastingSilhouetteRenderer','RobeRegaliaRenderer','SignatureGeometry','SpellSigilService']
 leaked=[n for n in names if any(n.endswith('/'+c+'.class') or ('/'+c+'$') in n for c in retired)]
 if leaked: raise SystemExit(f"retired presentation bytecode leaked: {sorted(leaked)}")

 index=json.loads(archive.read('data/arcanecircle/spell_catalog/index.json'))
 version=index.get('version')
 if version!='0.12.1-alpha.59': raise SystemExit(f"unexpected alpha.59 package version: {version}")
 if jar.name!=f'arcanecircle-{version}.jar': raise SystemExit(f"JAR/version mismatch: {jar.name} vs {version}")
 if index.get('implemented_circles')!=list(range(1,10)) or index.get('direct_spells')!=90 or index.get('fusion_spells')!=19:
  raise SystemExit('JAR catalogue is not the full 1-9 circle world')
 if index.get('crafting_progression') is not True: raise SystemExit('staff crafting progression is not enabled')
 if index.get('grimoire_effect_compendium') is not True: raise SystemExit('alpha.52 effect compendium metadata missing')
 if index.get('spell_contract_audit')!='109_explicit_summaries_and_runtime_routes': raise SystemExit('109-spell audit metadata missing')
 if set(index.get('copy_source_targeting',[]))!={'simulacrum_target_28','clone_target_32'}: raise SystemExit('copy targeting metadata mismatch')

 expected={
  'first_circle_deep_audit':{
   'magic_missile_locked_salvo','fire_bolt_nonhoming_impact','single_beam_ray_of_frost','reactive_shield',
   'lifecycle_safe_feather_fall','refcounted_real_light','persistent_grease_slip','weak_target_damage_wake_sleep',
   'physical_thunderwave','regenerating_mage_armor'},
  'second_circle_deep_audit':{
   'timed_three_ray_scorching_salvo','safe_misty_step','persistent_web_field','direct_attack_mirror_images',
   'aggro_breaking_invisibility','line_force_gust','restricted_hold_person','single_center_shatter',
   'direct_attack_blur','rise_safe_descent_levitate'},
  'third_circle_deep_audit':{
   'falloff_fireball_blast','piercing_lightning_line','lifecycle_real_flight','arcane_tempo_haste','custom_state_dispel_magic',
   'actual_damage_vampiric_drain','persistent_slow_field','energy_only_recharging_ward','casting_break_sleet_storm','safe_long_blink'},
  'fourth_circle_deep_audit':{
   'persistent_fire_wall','five_pulse_ice_storm','combat_greater_invisibility','two_way_resilient_sphere',
   'companion_dimension_door','physical_only_stoneskin','decision_scramble_confusion','anti_heal_blight',
   'movement_control_freedom','forced_flee_phantasmal_killer'},
  'fifth_circle_deep_audit':{
   'widening_cone_of_cold','spell_intercepting_force_wall','drifting_cloudkill_front','sustained_telekinesis_throw',
   'vertical_flame_strike','boss_resisted_hold_monster','allied_mass_cure','restoring_physical_passwall',
   'person_scale_combat_domination','casting_break_insect_plague'},
  'sixth_circle_deep_audit':{
   'material_disintegrate_ray','npc_parity_invulnerability_globe','behavioral_mass_suggestion','physical_move_earth',
   'piercing_sunbeam','persistent_true_seeing','fixed_freezing_sphere','fear_weakness_eyebite',
   'casting_block_petrification','weak_enemy_circle_of_death'},
  'seventh_circle_deep_audit':{
   'locked_delayed_fireball','preserved_ethereal_phase','locked_finger_of_death','six_pillar_fire_storm',
   'preserved_physical_forcecage','preserved_player_plane_shift','seven_independent_prismatic_rays',
   'maintained_reverse_gravity','preserved_commandable_simulacrum','locked_safe_teleport'},
 }
 for key,value in expected.items():
  if set(index.get(key,[]))!=value: raise SystemExit(f'{key} mismatch: {sorted(index.get(key,[]))}')
 for circle in ('first','second','third','fourth','fifth','sixth','seventh'):
  if index.get(f'{circle}_circle_npc_parity') is not True: raise SystemExit(f'{circle} circle NPC parity metadata missing')

 preserved=set(index.get('seventh_circle_preserved_authority',[]))
 if preserved!={'etherealness','forcecage','plane_shift','simulacrum'}: raise SystemExit('seventh-circle preserved authority mismatch')
 if index.get('seventh_circle_npc_plane_shift_role')!='safe_planar_disengage_without_target_damage':
  raise SystemExit('NPC Plane Shift role mismatch')
 utility=set(index.get('high_utility_identity',[]))
 if utility!={'cross_dimension_plane_shift','persistent_demiplane_room','commandable_simulacrum'}: raise SystemExit('high utility metadata mismatch')
 control=set(index.get('high_control_identity',[]))
 if control!={'behavioral_mass_suggestion','physical_forcecage','temporary_dominate_monster','spellbreaking_feeblemind'}: raise SystemExit('high control metadata mismatch')
 ward=set(index.get('high_ward_identity',[]))
 if ward!={'globe_blocks_hostile_circle_1_to_5_spells','circle_6_plus_and_physical_pass_through','player_and_npc_cast_interception'}: raise SystemExit('high ward metadata mismatch')
 notice=archive.read('META-INF/THIRD_PARTY_NOTICES.md').decode('utf-8')
 if 'Creative Commons Attribution 4.0' not in notice: raise SystemExit('SRD attribution missing from JAR')

digest=hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name+'.sha256').write_text(f'{digest}  {jar.name}\n',encoding='utf-8')
print(f'Arcane Circle v0.12.1 JAR verification: PASS ({len(names)} entries, {len(staff_recipes)} staff recipes)')
print('alpha49_high_utility_runtime=PASS')
print('alpha50_high_control_runtime=PASS')
print('alpha51_high_ward_runtime=PASS')
print('alpha52_readable_grimoire=PASS')
print('alpha52_109_spell_contract_audit=PASS')
print('alpha53_first_circle_deep_runtime=PASS')
print('alpha54_second_circle_deep_runtime=PASS')
print('alpha55_third_circle_deep_runtime=PASS')
print('alpha56_fourth_circle_deep_runtime=PASS')
print('alpha57_fifth_circle_deep_runtime=PASS')
print('alpha58_sixth_circle_deep_runtime=PASS')
print('alpha58_sixth_circle_npc_parity=PASS')
print('alpha58_globe_suggestion_petrify_authority=PASS')
print('alpha59_seventh_circle_deep_runtime=PASS')
print('alpha59_seventh_circle_npc_parity=PASS')
print('alpha59_preserved_ethereal_forcecage_plane_simulacrum=PASS')
print('alpha59_prismatic_gravity_lifecycle=PASS')
print(f'SHA-256: {digest}')
