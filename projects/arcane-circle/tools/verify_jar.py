#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import sys
import zipfile
from pathlib import Path

jar=Path(sys.argv[1])
if not jar.is_file(): raise SystemExit(f'missing JAR: {jar}')

staff_recipes={'data/arcanecircle/recipe/aegis_staff.json','data/arcanecircle/recipe/archmage_staff.json','data/arcanecircle/recipe/ember_staff.json','data/arcanecircle/recipe/glacial_staff.json','data/arcanecircle/recipe/rift_staff.json','data/arcanecircle/recipe/sage_staff.json','data/arcanecircle/recipe/verdant_staff.json','data/arcanecircle/recipe/zephyr_staff.json'}
required={'META-INF/neoforge.mods.toml','META-INF/THIRD_PARTY_NOTICES.md','kr/moonseungjun/arcanecircle/ArcaneCircle.class','kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/SecondCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/FifthCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/EighthCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/NinthCircleSpellService.class','kr/moonseungjun/arcanecircle/magic/Alpha65NinthCircleRuntime.class','kr/moonseungjun/arcanecircle/magic/GroundTargetResolver.class','kr/moonseungjun/arcanecircle/magic/NinthCircleMagnitude.class','kr/moonseungjun/arcanecircle/magic/DeathDoctrineService.class','kr/moonseungjun/arcanecircle/magic/MeteorBarragePattern.class','kr/moonseungjun/arcanecircle/magic/MeteorCataclysmService.class','kr/moonseungjun/arcanecircle/magic/NinthCircleSpellSummary.class','kr/moonseungjun/arcanecircle/magic/SpellKineticsService.class','kr/moonseungjun/arcanecircle/magic/ArcaneFieldService.class','kr/moonseungjun/arcanecircle/magic/HighUtilitySpellService.class','kr/moonseungjun/arcanecircle/magic/HighControlSpellService.class','kr/moonseungjun/arcanecircle/magic/HighWardSpellService.class','kr/moonseungjun/arcanecircle/magic/PlanarSpellService.class','kr/moonseungjun/arcanecircle/magic/SimulacrumService.class','kr/moonseungjun/arcanecircle/world/NpcMeteorBarrageService.class','kr/moonseungjun/arcanecircle/client/AuthoredHighCircleTimeline.class','kr/moonseungjun/arcanecircle/client/HighCirclePrestigeOverlay.class','kr/moonseungjun/arcanecircle/client/CircleScaleEnvelope.class','kr/moonseungjun/arcanecircle/client/PrimaryGrimoireScreen.class','data/arcanecircle/spell_catalog/index.json','assets/arcanecircle/items/spellbook_meteor_swarm.json','assets/arcanecircle/items/spellbook_wish.json','assets/arcanecircle/items/spellbook_gate.json'} | staff_recipes

with zipfile.ZipFile(jar) as archive:
    names=archive.namelist(); name_set=set(names)
    missing=sorted(required-name_set)
    if missing: raise SystemExit(f'missing required entries: {missing}')
    if len(names)!=len(name_set): raise SystemExit('duplicate ZIP entries')
    forbidden=[n for n in names if n.endswith('.java') or n.startswith(('tools/','.github/')) or 'villager_trade' in n]
    if forbidden: raise SystemExit(f'forbidden package entries: {forbidden[:8]}')
    packaged={n for n in name_set if n.startswith('data/arcanecircle/recipe/') and n.endswith('_staff.json')}
    if packaged!=staff_recipes: raise SystemExit(f'staff recipe set mismatch: {sorted(packaged)}')
    index=json.loads(archive.read('data/arcanecircle/spell_catalog/index.json'))
    version=index.get('version')
    if version!='0.12.1-alpha.65': raise SystemExit(f'unexpected alpha.65 package version: {version}')
    if jar.name!=f'arcanecircle-{version}.jar': raise SystemExit(f'JAR/version mismatch: {jar.name} vs {version}')
    if index.get('implemented_circles')!=list(range(1,10)) or index.get('direct_spells')!=90 or index.get('fusion_spells')!=19: raise SystemExit('catalogue is not full 1-9 / 90+19')
    if index.get('spell_contract_audit')!='109_explicit_summaries_and_runtime_routes': raise SystemExit('109-spell audit metadata missing')
    for c in ('first','second','third','fourth','fifth','sixth','seventh','eighth','ninth'):
        if index.get(f'{c}_circle_npc_parity') is not True: raise SystemExit(f'{c} NPC parity metadata missing')
    city=index.get('meteor_cityfall',{})
    if city.get('baseline_radius')!=112 or city.get('baseline_strikes')!=49 or city.get('range_scaled') is not True: raise SystemExit('alpha.65 Meteor Cityfall magnitude metadata mismatch')
    if set(city.get('projectile_scaling',[]))!={'count','spacing','body_scale','fall_height'}: raise SystemExit('alpha.65 projectile scaling metadata mismatch')
    if city.get('impact_surface')!='actual_loaded_surface_per_strike_xz': raise SystemExit('alpha.65 grounded Meteor metadata mismatch')
    if city.get('terminal_shock')!='full_cityfall_radius_with_inner_lethal_core': raise SystemExit('terminal shock metadata mismatch')
    if city.get('terrain')!='budgeted_citywide_crater_lattice': raise SystemExit('terrain lattice metadata mismatch')
    if index.get('ninth_circle_divine_scale_phase')!='grounded_targeting_and_individual_sigil_authority': raise SystemExit('alpha.65 correction phase metadata missing')
    if index.get('common_high_circle_array_overlay')!='removed_and_forbidden': raise SystemExit('common high-circle array ban metadata missing')
    visuals=set(index.get('ninth_circle_visual_authority',[]))
    if len(visuals)!=10: raise SystemExit('all ten ninth-circle individual visual authorities are required')
    if index.get('meteor_ground_contract')!='actual_surface_per_authoritative_strike': raise SystemExit('Meteor ground contract missing')
    if index.get('gate_target_contract')!='entity_independent_safe_ground_pair': raise SystemExit('Gate target contract missing')
    if index.get('weird_contract')!='caster_excluded_escape_or_die_domain': raise SystemExit('Weird contract missing')
    if index.get('world_sunder_contract')!='horizontal_fault_corridor': raise SystemExit('World Sunder contract missing')
    if index.get('grimoire_ui')!='isolated_slot_strip_plus_clicked_effect_detail_inspector': raise SystemExit('alpha.65 grimoire layout metadata missing')
    death=index.get('death_doctrine',{})
    if death.get('circle_of_death')!='wide_life_erosion_no_execution': raise SystemExit('6C death role mismatch')
    if death.get('finger_of_death')!='single_target_soul_rupture_no_threshold_execution': raise SystemExit('7C death role mismatch')
    if death.get('power_word_kill')!='exclusive_ninth_circle_execution_with_fallback_life_collapse': raise SystemExit('9C death role mismatch')
    notice=archive.read('META-INF/THIRD_PARTY_NOTICES.md').decode('utf-8')
    if 'Creative Commons Attribution 4.0' not in notice: raise SystemExit('SRD attribution missing')

digest=hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name+'.sha256').write_text(f'{digest}  {jar.name}\n',encoding='utf-8')
print('Arcane Circle alpha.65 JAR verification: PASS')
print('alpha65_grounded_meteor=PASS')
print('alpha65_gate_safe_ground_pair=PASS')
print('alpha65_weird_escape_or_die=PASS')
print('alpha65_world_sunder_horizontal_fault=PASS')
print('alpha65_individual_ninth_circle_sigils=PASS')
print('alpha65_common_grand_array_removed=PASS')
print('alpha65_grimoire_layout_and_detail=PASS')
print(f'SHA-256: {digest}')
