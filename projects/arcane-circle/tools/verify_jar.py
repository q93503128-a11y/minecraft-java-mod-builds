#!/usr/bin/env python3
from __future__ import annotations
import hashlib
import json
import sys
import zipfile
from pathlib import Path

jar=Path(sys.argv[1])
if not jar.is_file(): raise SystemExit(f'missing JAR: {jar}')

staff_recipes={
 'data/arcanecircle/recipe/aegis_staff.json','data/arcanecircle/recipe/archmage_staff.json',
 'data/arcanecircle/recipe/ember_staff.json','data/arcanecircle/recipe/glacial_staff.json',
 'data/arcanecircle/recipe/rift_staff.json','data/arcanecircle/recipe/sage_staff.json',
 'data/arcanecircle/recipe/verdant_staff.json','data/arcanecircle/recipe/zephyr_staff.json',
}
required={
 'META-INF/neoforge.mods.toml','META-INF/THIRD_PARTY_NOTICES.md',
 'kr/moonseungjun/arcanecircle/ArcaneCircle.class',
 'kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/SecondCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/FifthCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/EighthCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/NinthCircleSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/DeathDoctrineService.class',
 'kr/moonseungjun/arcanecircle/magic/MeteorCataclysmService.class',
 'kr/moonseungjun/arcanecircle/magic/SixthCircleSpellSummary.class',
 'kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellSummary.class',
 'kr/moonseungjun/arcanecircle/magic/EighthCircleSpellSummary.class',
 'kr/moonseungjun/arcanecircle/magic/NinthCircleSpellSummary.class',
 'kr/moonseungjun/arcanecircle/magic/ArcaneFieldService.class',
 'kr/moonseungjun/arcanecircle/magic/HighUtilitySpellService.class',
 'kr/moonseungjun/arcanecircle/magic/HighControlSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/HighWardSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/PlanarSpellService.class',
 'kr/moonseungjun/arcanecircle/magic/SimulacrumService.class',
 'kr/moonseungjun/arcanecircle/magic/SpellKineticsService.class',
 'kr/moonseungjun/arcanecircle/world/NpcMeteorBarrageService.class',
 'kr/moonseungjun/arcanecircle/client/AuthoredHighCircleTimeline.class',
 'kr/moonseungjun/arcanecircle/client/HighCirclePrestigeOverlay.class',
 'kr/moonseungjun/arcanecircle/client/CircleScaleEnvelope.class',
 'kr/moonseungjun/arcanecircle/client/ReadableGrimoireScreen.class',
 'kr/moonseungjun/arcanecircle/client/PrimaryGrimoireScreen.class',
 'data/arcanecircle/spell_catalog/index.json',
 'assets/arcanecircle/items/spellbook_meteor_swarm.json',
 'assets/arcanecircle/items/spellbook_wish.json',
 'assets/arcanecircle/items/spellbook_gate.json',
} | staff_recipes

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
    if version!='0.12.1-alpha.62': raise SystemExit(f'unexpected alpha.62 package version: {version}')
    if jar.name!=f'arcanecircle-{version}.jar': raise SystemExit(f'JAR/version mismatch: {jar.name} vs {version}')
    if index.get('implemented_circles')!=list(range(1,10)) or index.get('direct_spells')!=90 or index.get('fusion_spells')!=19:
        raise SystemExit('catalogue is not full 1-9 / 90+19')
    if index.get('spell_contract_audit')!='109_explicit_summaries_and_runtime_routes':
        raise SystemExit('109-spell audit metadata missing')
    for c in ('first','second','third','fourth','fifth','sixth','seventh','eighth','ninth'):
        if index.get(f'{c}_circle_npc_parity') is not True: raise SystemExit(f'{c} NPC parity metadata missing')

    if index.get('presentation_scale_grammar')!={
      '1-2':'hand_scale_immediate','3-4':'combat_space','5-6':'multi_plane_grand_magic',
      '7':'fortress_planar_authority','8':'regional_reality_authority','9':'world_law_catastrophe'}:
        raise SystemExit('circle presentation scale grammar mismatch')
    death=index.get('death_doctrine',{})
    if death.get('circle_of_death')!='wide_life_erosion_no_execution': raise SystemExit('6C death role mismatch')
    if death.get('finger_of_death')!='single_target_soul_rupture_no_threshold_execution': raise SystemExit('7C death role mismatch')
    if death.get('power_word_kill')!='exclusive_ninth_circle_execution_with_fallback_life_collapse': raise SystemExit('9C death role mismatch')
    if index.get('crown_meteor')!='fifteenth_barrage_then_delayed_sixteenth_crown_cataclysm':
        raise SystemExit('Crown Meteor metadata missing')
    if index.get('grimoire_ui')!='readable_two_pane_cards_with_named_loadout_slots':
        raise SystemExit('readable grimoire metadata missing')

    expected9={
      'crown_cataclysm_meteor_swarm','exclusive_execution_power_word_kill','seven_layer_physical_prismatic_wall',
      'preserved_shapechange','preserved_time_stop','preserved_true_polymorph','maintained_behavioral_weird',
      'preserved_reality_wish','two_way_safe_gate','preserved_foresight'}
    if set(index.get('ninth_circle_deep_audit',[]))!=expected9: raise SystemExit('9C deep audit metadata mismatch')
    if set(index.get('ninth_circle_preserved_authority',[]))!={'shapechange','time_stop','true_polymorph','wish','foresight'}:
        raise SystemExit('9C preserved authority mismatch')
    if index.get('ninth_circle_npc_true_polymorph_player_role')!='combat_and_casting_suppression_without_player_entity_type_swap':
        raise SystemExit('NPC true polymorph player role mismatch')
    if index.get('ninth_circle_npc_wish_role')!='full_recovery_cleanse_without_player_mana_cooldown_state':
        raise SystemExit('NPC Wish role mismatch')
    if index.get('ninth_circle_gate_role')!='two_way_same_dimension_safe_entity_portal':
        raise SystemExit('Gate role mismatch')

    notice=archive.read('META-INF/THIRD_PARTY_NOTICES.md').decode('utf-8')
    if 'Creative Commons Attribution 4.0' not in notice: raise SystemExit('SRD attribution missing')

digest=hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_name(jar.name+'.sha256').write_text(f'{digest}  {jar.name}\n',encoding='utf-8')
print(f'Arcane Circle alpha.62 JAR verification: PASS ({len(names)} entries)')
print('alpha62_readable_grimoire=PASS')
print('alpha62_circle_scale_hierarchy=PASS')
print('alpha62_high_circle_prestige=PASS')
print('alpha62_death_doctrine=PASS')
print('alpha62_crown_meteor=PASS')
print(f'SHA-256: {digest}')