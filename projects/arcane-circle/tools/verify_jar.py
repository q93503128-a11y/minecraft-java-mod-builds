#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import sys
import zipfile
from pathlib import Path

jar = Path(sys.argv[1])
if not jar.is_file():
    raise SystemExit(f'missing JAR: {jar}')

staff_recipes = {
    'data/arcanecircle/recipe/aegis_staff.json',
    'data/arcanecircle/recipe/archmage_staff.json',
    'data/arcanecircle/recipe/ember_staff.json',
    'data/arcanecircle/recipe/glacial_staff.json',
    'data/arcanecircle/recipe/rift_staff.json',
    'data/arcanecircle/recipe/sage_staff.json',
    'data/arcanecircle/recipe/verdant_staff.json',
    'data/arcanecircle/recipe/zephyr_staff.json',
}
required = {
    'META-INF/neoforge.mods.toml',
    'META-INF/THIRD_PARTY_NOTICES.md',
    'kr/moonseungjun/arcanecircle/ArcaneCircle.class',
    'kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/SecondCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/client/ThirdCircleAuthorityOverlay.class',
    'kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/FourthCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/client/FourthCircleAuthorityOverlay.class',
    'kr/moonseungjun/arcanecircle/magic/FifthCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/FifthCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/client/FifthCircleAuthorityOverlay.class',
    'kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/SixthCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/magic/MoveEarthService.class',
    'kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/magic/EighthCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/EighthCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/magic/NinthCircleSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/Alpha65NinthCircleRuntime.class',
    'kr/moonseungjun/arcanecircle/magic/GroundTargetResolver.class',
    'kr/moonseungjun/arcanecircle/magic/NinthCircleMagnitude.class',
    'kr/moonseungjun/arcanecircle/magic/DeathDoctrineService.class',
    'kr/moonseungjun/arcanecircle/magic/MeteorBarragePattern.class',
    'kr/moonseungjun/arcanecircle/magic/MeteorCataclysmService.class',
    'kr/moonseungjun/arcanecircle/magic/NinthCircleSpellSummary.class',
    'kr/moonseungjun/arcanecircle/magic/SpellKineticsService.class',
    'kr/moonseungjun/arcanecircle/magic/ArcaneFieldService.class',
    'kr/moonseungjun/arcanecircle/magic/HighUtilitySpellService.class',
    'kr/moonseungjun/arcanecircle/magic/HighControlSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/HighWardSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/PlanarSpellService.class',
    'kr/moonseungjun/arcanecircle/magic/SimulacrumService.class',
    'kr/moonseungjun/arcanecircle/world/NpcMeteorBarrageService.class',
    'kr/moonseungjun/arcanecircle/client/SixthCircleAuthorityOverlay.class',
    'kr/moonseungjun/arcanecircle/client/AuthoredHighCircleTimeline.class',
    'kr/moonseungjun/arcanecircle/client/HighCircleMaintenanceOverlay.class',
    'kr/moonseungjun/arcanecircle/client/HighCirclePrestigeOverlay.class',
    'kr/moonseungjun/arcanecircle/client/CircleScaleEnvelope.class',
    'kr/moonseungjun/arcanecircle/client/PrimaryGrimoireScreen.class',
    'data/arcanecircle/spell_catalog/index.json',
    'assets/arcanecircle/items/spellbook_meteor_swarm.json',
    'assets/arcanecircle/items/spellbook_wish.json',
    'assets/arcanecircle/items/spellbook_gate.json',
} | staff_recipes

with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    name_set = set(names)
    missing = sorted(required - name_set)
    if missing:
        raise SystemExit(f'missing required entries: {missing}')
    if len(names) != len(name_set):
        raise SystemExit('duplicate ZIP entries')
    forbidden = [n for n in names if n.endswith('.java') or n.startswith(('tools/', '.github/')) or 'villager_trade' in n]
    if forbidden:
        raise SystemExit(f'forbidden package entries: {forbidden[:8]}')
    packaged = {n for n in name_set if n.startswith('data/arcanecircle/recipe/') and n.endswith('_staff.json')}
    if packaged != staff_recipes:
        raise SystemExit(f'staff recipe set mismatch: {sorted(packaged)}')

    index = json.loads(archive.read('data/arcanecircle/spell_catalog/index.json'))
    version = index.get('version')
    if version != '0.12.1-alpha.72':
        raise SystemExit(f'unexpected alpha.72 package version: {version}')
    if jar.name != f'arcanecircle-{version}.jar':
        raise SystemExit(f'JAR/version mismatch: {jar.name} vs {version}')
    if index.get('implemented_circles') != list(range(1, 10)) or index.get('direct_spells') != 90 or index.get('fusion_spells') != 19:
        raise SystemExit('catalogue is not full 1-9 / 90+19')
    if index.get('spell_contract_audit') != '109_explicit_summaries_and_runtime_routes':
        raise SystemExit('109-spell audit metadata missing')
    for c in ('first','second','third','fourth','fifth','sixth','seventh','eighth','ninth'):
        if index.get(f'{c}_circle_npc_parity') is not True:
            raise SystemExit(f'{c} NPC parity metadata missing')

    expected3 = {
        'haste': '30s_player_and_npc_arcane_tempo_acceleration_0.72_cast_0.85_cooldown',
        'dispel_magic': 'deterministic_maintained_magic_purge_circles_1_to_3_only',
        'blink': 'solo_safe_endpoint_phase_traversal_ignoring_intervening_solid_geometry_up_to_20m',
    }
    if index.get('third_circle_value_pass_1') != expected3:
        raise SystemExit(f'alpha.72 third-circle value metadata mismatch: {index.get("third_circle_value_pass_1")}')
    roles3 = index.get('third_circle_role_audit', {})
    expected_roles3 = {'fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'}
    if set(roles3) != expected_roles3 or len(set(roles3.values())) != 10:
        raise SystemExit('alpha.72 third-circle role separation contract missing')
    if index.get('third_circle_dispel_ceiling') != 'circles_1_to_3_deterministic_only':
        raise SystemExit('alpha.72 Dispel ceiling contract missing')

    expected4 = {
        'ice_storm': '6s_fixed_anti_air_hail_suppression_with_0.5s_pulses',
        'phantasmal_killer': '14s_single_target_terror_bond_forced_retreat_and_owner_damage_denial',
    }
    if index.get('fourth_circle_value_pass_1') != expected4:
        raise SystemExit(f'alpha.71 fourth-circle value metadata mismatch: {index.get("fourth_circle_value_pass_1")}')
    roles4 = index.get('fourth_circle_role_audit', {})
    expected_roles4 = {'wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door','stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer'}
    if set(roles4) != expected_roles4 or len(set(roles4.values())) != 10:
        raise SystemExit('alpha.71 fourth-circle role separation contract missing')

    expected5 = {
        'flame_strike': '4s_locked_vertical_fire_column_with_initial_breach_and_0.5s_pulses',
        'dominate_person': '30s_person_scale_combat_asset_control',
    }
    if index.get('fifth_circle_value_pass_1') != expected5:
        raise SystemExit(f'alpha.70 fifth-circle value metadata mismatch: {index.get("fifth_circle_value_pass_1")}')
    roles5 = index.get('fifth_circle_role_audit', {})
    expected_roles5 = {'cone_of_cold','wall_of_force','cloudkill','telekinesis','flame_strike','hold_monster','mass_cure_wounds','passwall','dominate_person','insect_plague'}
    if set(roles5) != expected_roles5 or len(set(roles5.values())) != 10:
        raise SystemExit('alpha.70 fifth-circle role separation contract missing')

    expected6 = {
        'mass_suggestion': '20s_group_retreat_and_arcane_suppression',
        'move_earth': '20_to_36m_directional_trench_and_dual_berm_using_relocated_surface_blocks',
        'sunbeam': '6s_locked_piercing_solar_corridor_with_0.5s_pulses',
        'freezing_sphere': '10s_10.5_to_15.5m_cryogenic_denial_with_0.5s_refreeze_pulses',
    }
    if index.get('sixth_circle_value_pass_1') != expected6:
        raise SystemExit(f'alpha.69 sixth-circle value metadata mismatch: {index.get("sixth_circle_value_pass_1")}')
    roles6 = index.get('sixth_circle_role_audit', {})
    expected_roles6 = {'disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam','true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death'}
    if set(roles6) != expected_roles6 or len(set(roles6.values())) != 10:
        raise SystemExit('alpha.69 sixth-circle role separation contract missing')
    if index.get('sixth_circle_npc_terrain_safety') != 'move_earth_retains_battlefield_split_but_skips_npc_world_edit':
        raise SystemExit('alpha.69 Move Earth NPC terrain safety contract missing')

    expected7 = {
        'delayed_blast_fireball': '3.6s_time_locked_siege_core_with_primary_breach_and_overpressure_shock',
        'finger_of_death': 'canonical_death_doctrine_single_soul_rupture_no_execution_threshold',
        'fire_storm': 'six_step_0.4s_siege_bombardment_with_repeat_hit_attenuation',
        'teleport': 'same_dimension_7m_gather_tactical_group_relocation_up_to_6_companions',
    }
    if index.get('seventh_circle_value_pass_1') != expected7:
        raise SystemExit('alpha.68 seventh-circle value metadata mismatch')
    roles7 = index.get('seventh_circle_role_audit', {})
    if len(roles7) != 10 or len(set(roles7.values())) != 10:
        raise SystemExit('alpha.68 seventh-circle role separation contract missing')

    expected8a = {
        'clone': '90s_single_bound_combat_copy_no_equipment_duplication',
        'dominate_monster': '60s_enemy_combat_asset_theft',
        'feeblemind': '90s_total_arcane_shutdown_and_severe_combat_degradation',
        'maze': '24s_total_battlefield_exile_plus_6s_aftershock',
    }
    expected8b = {
        'earthquake': '9s_regional_fault_disaster_with_budgeted_player_terrain_and_unstable_ground',
        'incendiary_cloud': '12s_moving_firefront_with_scorched_wake_route_denial',
        'sunburst': '12s_solar_revelation_purification_and_darkness_denial_domain',
    }
    if index.get('eighth_circle_value_pass_1') != expected8a or index.get('eighth_circle_value_pass_2') != expected8b:
        raise SystemExit('alpha.66/67 eighth-circle value metadata mismatch')
    roles8 = index.get('eighth_circle_role_audit', {})
    if len(roles8) != 10 or len(set(roles8.values())) != 10:
        raise SystemExit('alpha.67 eighth-circle role separation contract missing')

    death = index.get('death_doctrine', {})
    if death != {
        'circle_of_death': 'wide_life_erosion_no_execution',
        'finger_of_death': 'single_target_soul_rupture_no_threshold_execution',
        'power_word_kill': 'exclusive_ninth_circle_execution_with_fallback_life_collapse',
    }:
        raise SystemExit('death hierarchy metadata mismatch')

    city = index.get('meteor_cityfall', {})
    if city.get('baseline_radius') != 112 or city.get('baseline_strikes') != 49 or city.get('range_scaled') is not True:
        raise SystemExit('alpha.65 Meteor Cityfall magnitude metadata mismatch')
    if city.get('impact_surface') != 'actual_loaded_surface_per_strike_xz':
        raise SystemExit('alpha.65 grounded Meteor metadata mismatch')
    if index.get('common_high_circle_array_overlay') != 'removed_and_forbidden':
        raise SystemExit('common high-circle array ban metadata missing')
    if index.get('gate_target_contract') != 'entity_independent_safe_ground_pair':
        raise SystemExit('Gate target contract missing')
    if index.get('weird_contract') != 'caster_excluded_escape_or_die_domain':
        raise SystemExit('Weird contract missing')
    if index.get('world_sunder_contract') != 'horizontal_fault_corridor':
        raise SystemExit('World Sunder contract missing')
    if index.get('grimoire_ui') != 'isolated_slot_strip_plus_clicked_effect_detail_inspector':
        raise SystemExit('grimoire layout metadata missing')

    notice = archive.read('META-INF/THIRD_PARTY_NOTICES.md').decode('utf-8')
    if 'Creative Commons Attribution 4.0' not in notice:
        raise SystemExit('SRD attribution missing')

digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name + '.sha256').write_text(f'{digest}  {jar.name}\n', encoding='utf-8')
print('Arcane Circle alpha.72 JAR verification: PASS')
print('alpha72_third_circle_value_pass_1=PASS')
print('alpha72_haste_player_npc_arcane_tempo_parity=PASS')
print('alpha72_dispel_magic_circle_1_to_3_ceiling=PASS')
print('alpha72_blink_solid_geometry_phase_relocation=PASS')
print('alpha72_third_circle_role_separation=PASS')
print('alpha72_third_circle_npc_parity=PASS')
print('alpha71_fourth_circle_value_pass_1=PASS')
print('alpha71_ice_storm_6s_anti_air_suppression=PASS')
print('alpha71_phantasmal_killer_14s_terror_bond=PASS')
print('alpha71_fourth_circle_role_separation=PASS')
print('alpha71_fourth_circle_npc_parity=PASS')
print('alpha70_fifth_circle_value_pass_1=PASS')
print('alpha70_flame_strike_4s_vertical_column=PASS')
print('alpha70_dominate_person_30s_person_scale_control=PASS')
print('alpha70_fifth_circle_role_separation=PASS')
print('alpha70_fifth_circle_npc_parity=PASS')
print('alpha69_sixth_circle_value_pass_1=PASS')
print('alpha69_mass_suggestion_20s_group_disengage=PASS')
print('alpha69_move_earth_precision_engineering=PASS')
print('alpha69_sunbeam_6s_solar_corridor=PASS')
print('alpha69_freezing_sphere_10s_denial=PASS')
print('alpha69_circle_of_death_no_execution_reentry=PASS')
print('alpha69_sixth_circle_role_separation=PASS')
print('alpha69_sixth_circle_npc_parity=PASS')
print('alpha68_seventh_circle_value_pass_1=PASS')
print('alpha67_eighth_circle_value_pass_2=PASS')
print('alpha66_eighth_circle_value_pass=PASS')
print('alpha65_ninth_circle_regressions=PASS')
print(f'SHA-256: {digest}')
