from pathlib import Path
import json
import re

root=Path(__file__).resolve().parents[1]
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
world=root/'src/main/java/kr/moonseungjun/arcanecircle/world'
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'

def text(p): return p.read_text(encoding='utf-8')
def need(body,*tokens):
    for token in tokens: assert token in body, token

# Canonical version/package metadata.
gradle=text(root/'gradle.properties')
main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=json.loads(text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json'))
need(gradle,'mod_version=0.12.1-alpha.61')
need(main,'VERSION = "0.12.1-alpha.61"')
assert index['version']=='0.12.1-alpha.61'
assert index['implemented_circles']==list(range(1,10))
assert index['direct_spells']==90 and index['fusion_spells']==19

# Full 109-spell source and summary inventory remains closed.
catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog))
fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog))
spells=direct|fusions
assert (len(direct),len(fusions),len(spells))==(90,19,109)
summary=text(magic/'SpellEffectSummary.java')
summary_ids=set(re.findall(r'case "([a-z0-9_]+)"',summary))
assert summary_ids==spells,(sorted(spells-summary_ids),sorted(summary_ids-spells))
definition=text(magic/'SpellDefinition.java')
need(definition,'NinthCircleSpellSummary.summary(id)')

circle_specs={
 'FirstCircleSpellService.java':['magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor'],
 'SecondCircleSpellService.java':['scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'],
 'ThirdCircleSpellService.java':['fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'],
 'FourthCircleSpellService.java':['wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door','stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer'],
 'FifthCircleSpellService.java':['cone_of_cold','wall_of_force','cloudkill','telekinesis','flame_strike','hold_monster','mass_cure_wounds','passwall','dominate_person','insect_plague'],
 'SixthCircleSpellService.java':['disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam','true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death'],
 'SeventhCircleSpellService.java':['delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage','plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport'],
 'EighthCircleSpellService.java':['antimagic_field','clone','control_weather','demiplane','dominate_monster','earthquake','feeblemind','incendiary_cloud','maze','sunburst'],
 'NinthCircleSpellService.java':['meteor_swarm','power_word_kill','prismatic_wall','shapechange','time_stop','true_polymorph','weird','wish','gate','foresight'],
}
for filename,ids in circle_specs.items():
    body=text(magic/filename)
    for spell in ids: need(body,f'"{spell}"')
    need(body,'public static boolean executeNpc(')

# 1C-8C regression anchors remain present.
need(text(magic/'FirstCircleSpellService.java'),'sleepEligible(target, power)','ArcaneLightService.illuminate(player, 1800)')
need(text(magic/'SecondCircleSpellService.java'),'new RaySalvo(level, caster.getUUID(), target.getUUID()','holdEligible(target)')
need(text(magic/'ThirdCircleSpellService.java'),'ArcaneDamage.isResolving()','SLEET_ZONES.add(new SleetZone')
need(text(magic/'FourthCircleSpellService.java'),'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm')
need(text(magic/'FifthCircleSpellService.java'),'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)','DOMINATED.put')
need(text(magic/'SixthCircleSpellService.java'),'DestructiveMagicService.ray(player, "disintegrate"','NPC_PETRIFY.put')
need(text(magic/'SeventhCircleSpellService.java'),'case "plane_shift" -> PlanarSpellService.execute(caster, spellId);','for (int band = 0; band < 7; band++)')
need(text(magic/'EighthCircleSpellService.java'),'EARTHQUAKES.add(new EarthquakeField','INCENDIARY_CLOUDS.add(new IncendiaryCloudField')

# alpha.61 ninth-circle authority.
ninth=text(magic/'NinthCircleSpellService.java')
need(ninth,
     'private static final Set<String> HANDLED = Set.of(',
     'case "shapechange", "foresight" -> ArcaneBuffRuntime.apply(caster, spellId, power, range);',
     'case "time_stop", "wish" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);',
     'case "true_polymorph" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);',
     'DestructiveMagicService.meteorCrater(caster, impact, radius, strikePower);',
     'double threshold = Math.max(100.0, power * 1.05);',
     'public static final int PRISMATIC_WALL_TICKS = 400;',
     'switch (layer)','case 0 ->','case 1 ->','case 2 ->','case 3 ->','case 4 ->','case 5 ->','default ->',
     'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)',
     'public static final int NPC_TIME_STOP_TICKS = ArcaneFieldService.TIME_STOP_TICKS;',
     'target.setNoAi(true);','restoreFrozenMob(frozen);','restoreFrozenEntity(frozen);',
     'EntityTypes.RABBIT.create(level, EntitySpawnReason.EVENT)','stashMob(original);','restoreNpcPolymorph(state, false);',
     'public static final int WEIRD_TICKS = 300;','mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.28);',
     'private static boolean npcWish(Mob caster)','caster.setHealth(caster.getMaxHealth());',
     'public static final int GATE_TICKS = 600;','processGateEndpoint(level, gate, gate.source, gate.targetArrival, now);',
     'processGateEndpoint(level, gate, gate.target, gate.sourceArrival, now);',
     'public static final int NPC_FORESIGHT_TICKS = 2400;','event.setAmount(Math.max(.05F, event.getAmount() * .75F));')
assert 'ParticleTypes' not in ninth

# Detailed 9C grimoire contract is explicit and truthful about NPC role limits.
ninth_summary=text(magic/'NinthCircleSpellSummary.java')
for spell in circle_specs['NinthCircleSpellService.java']: need(ninth_summary,f'case "{spell}"')
need(ninth_summary,'실제 크레이터','단일 생명체만','7겹 프리즘 전선','기존 실제 시간 정지 권한',
     '실제 임시 생물 몸체','최대 16명의 적','Wish 자신을 제외한 주문 쿨타임','실제 양방향 월드 게이트','2초마다 다음 공격을 완전 회피')

# Preserve the existing strong source-of-truth player semantics rather than replacing them.
field=text(magic/'ArcaneFieldService.java')
need(field,'"antimagic_field".equals(spellId)','"time_stop".equals(spellId)','"wish".equals(spellId)',
     'TIME_STOP_TICKS = 160','case "wish" -> fulfillWish(player);')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")','TRUE_POLYMORPH_TICKS = 480','setNoAi(true)')
buffs=text(magic/'ArcaneBuffRuntime.java')
need(buffs,'case "shapechange" -> 1800;','case "foresight" -> 2400;','event.setCanceled(true);','return .50;','return .75;')
player_data=text(magic/'MagicPlayerData.java')
need(player_data,'if ("wish".equals(cast.spell().id()))','state.mana = effectiveStats(player).maxMana();',
     'state.cooldowns.entrySet().removeIf(entry -> !"wish".equals(entry.getKey()));')

# Player ownership is 1C->...->9C before old high-circle/generic services, and meteor hits use 9C.
kinetics=text(magic/'SpellKineticsService.java')
for token in ['FirstCircleSpellService.handles(cast.spell().id())','EighthCircleSpellService.handles(cast.spell().id())',
              'NinthCircleSpellService.handles(cast.spell().id())','boolean ninthCircleOwned',
              'NinthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)',
              'NinthCircleSpellService.meteorImpact(player','NinthCircleSpellService.intercepts(player, targetSnapshot)',
              'NinthCircleSpellService.blocksCasting(player)']:
    need(kinetics,token)
order=['boolean firstCircleOwned','boolean secondCircleOwned','boolean thirdCircleOwned','boolean fourthCircleOwned',
       'boolean fifthCircleOwned','boolean sixthCircleOwned','boolean seventhCircleOwned','boolean eighthCircleOwned',
       'boolean ninthCircleOwned','boolean planarOwned']
assert [kinetics.index(x) for x in order]==sorted(kinetics.index(x) for x in order)
assert 'HighCircleSpellEffects.meteorImpact' not in kinetics

# NPC 9C dispatch and seeded meteor parity precede generic motion resolution.
npc=text(world/'NpcSpellResolver.java')
need(npc,'NinthCircleSpellService.blocksCasting(caster)','NinthCircleSpellService.intercepts(caster, snapshot)',
     'NinthCircleSpellService.handles(spell.id())','NinthCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)')
npc_meteor=text(world/'NpcMeteorBarrageService.java')
need(npc_meteor,'NinthCircleSpellService.resolveNpcMeteorImpact(level, caster','NinthCircleSpellService.blocksCasting(caster)')

# Dispel/Antimagic already invoke HighControl clear; alpha.61 extends that unified cleanup to 9C.
control=text(magic/'HighControlSpellService.java')
need(control,'NinthCircleSpellService.clear(subject);')
third=text(magic/'ThirdCircleSpellService.java')
need(third,'HighControlSpellService.clear(target);')
need(field,'HighControlSpellService.clear(entity);')

# Explicit player lifecycle and server-stop restoration.
need(main,'NinthCircleSpellService::onIncomingDamage','NinthCircleSpellService.clear(player);',
     'NinthCircleSpellService.tick(level);','NinthCircleSpellService.clearAll();')

# Metadata states the exact NPC role-parity boundaries.
assert set(index['ninth_circle_deep_audit'])=={
 'seeded_cratering_meteor_swarm','locked_power_word_kill','seven_layer_physical_prismatic_wall',
 'preserved_shapechange','preserved_time_stop','preserved_true_polymorph','maintained_behavioral_weird',
 'preserved_reality_wish','two_way_safe_gate','preserved_foresight'}
assert set(index['ninth_circle_preserved_authority'])=={'shapechange','time_stop','true_polymorph','wish','foresight'}
assert index['ninth_circle_npc_parity'] is True
assert index['ninth_circle_npc_true_polymorph_player_role']=='combat_and_casting_suppression_without_player_entity_type_swap'
assert index['ninth_circle_npc_wish_role']=='full_recovery_cleanse_without_player_mana_cooldown_state'
assert index['ninth_circle_gate_role']=='two_way_same_dimension_safe_entity_portal'

# Authored high-circle presentation remains present for all 9C; no particle gameplay in new runtime.
timeline=text(client/'AuthoredHighCircleTimeline.java')
for spell in circle_specs['NinthCircleSpellService.java']: need(timeline,f'"{spell}"')

# Audit/verifier/package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.61','seeded cratering meteor','seven-layer physical prism wall','two-way world gate')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'NinthCircleSpellService.class','NinthCircleSpellSummary.class','0.12.1-alpha.61')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('alpha53_60_circle_regression_anchors=PASS')
print('alpha61_ninth_circle_deep_runtime=PASS')
print('alpha61_ninth_circle_npc_parity=PASS')
print('alpha61_seeded_meteor_crater_authority=PASS')
print('alpha61_power_word_prismatic_weird_gate=PASS')
print('alpha61_preserved_shape_time_poly_wish_foresight=PASS')
print('alpha61_wish_mana_cooldown_authority=PASS')
print('alpha61_dispel_antimagic_lifecycle=PASS')
