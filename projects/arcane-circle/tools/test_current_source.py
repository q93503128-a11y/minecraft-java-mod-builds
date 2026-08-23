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

gradle=text(root/'gradle.properties'); main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=json.loads(text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json'))
need(gradle,'mod_version=0.12.1-alpha.66'); need(main,'VERSION = "0.12.1-alpha.66"')
assert index['version']=='0.12.1-alpha.66' and index['implemented_circles']==list(range(1,10))
assert index['direct_spells']==90 and index['fusion_spells']==19

catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog)); fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog)); spells=direct|fusions
assert (len(direct),len(fusions),len(spells))==(90,19,109)
summary=text(magic/'SpellEffectSummary.java'); summary_ids=set(re.findall(r'case "([a-z0-9_]+)"',summary)); assert summary_ids==spells
need(text(magic/'SpellDefinition.java'),'NinthCircleSpellSummary.summary(id)')

circle_specs={
 'FirstCircleSpellService.java':['magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor'],
 'SecondCircleSpellService.java':['scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'],
 'ThirdCircleSpellService.java':['fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'],
 'FourthCircleSpellService.java':['wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door','stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer'],
 'FifthCircleSpellService.java':['cone_of_cold','wall_of_force','cloudkill','telekinesis','flame_strike','hold_monster','mass_cure_wounds','passwall','dominate_person','insect_plague'],
 'SixthCircleSpellService.java':['disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam','true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death'],
 'SeventhCircleSpellService.java':['delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage','plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport'],
 'EighthCircleSpellService.java':['antimagic_field','clone','control_weather','demiplane','dominate_monster','earthquake','feeblemind','incendiary_cloud','maze','sunburst'],
 'NinthCircleSpellService.java':['meteor_swarm','power_word_kill','prismatic_wall','shapechange','time_stop','true_polymorph','weird','wish','gate','foresight']}
for filename,ids in circle_specs.items():
    body=text(magic/filename)
    for spell in ids: need(body,f'"{spell}"')
    need(body,'public static boolean executeNpc(')

need(text(magic/'FirstCircleSpellService.java'),'sleepEligible(target, power)','ArcaneLightService.illuminate(player, 1800)')
need(text(magic/'SecondCircleSpellService.java'),'new RaySalvo(level, caster.getUUID(), target.getUUID()','holdEligible(target)')
need(text(magic/'ThirdCircleSpellService.java'),'ArcaneDamage.isResolving()','SLEET_ZONES.add(new SleetZone')
need(text(magic/'FourthCircleSpellService.java'),'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm')
need(text(magic/'FifthCircleSpellService.java'),'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)','DOMINATED.put')
need(text(magic/'SixthCircleSpellService.java'),'DestructiveMagicService.ray(player, "disintegrate"','NPC_PETRIFY.put')
need(text(magic/'SeventhCircleSpellService.java'),'case "plane_shift" -> PlanarSpellService.execute(caster, spellId);','for (int band = 0; band < 7; band++)')

eighth=text(magic/'EighthCircleSpellService.java')
need(eighth,'EARTHQUAKES.add(new EarthquakeField','INCENDIARY_CLOUDS.add(new IncendiaryCloudField',
     'NPC_CLONE_TICKS = 1800','NPC_DOMINATE_TICKS = 1200','NPC_FEEBLEMIND_TICKS = 1800','NPC_MAZE_TICKS = 480',
     'new NpcCloneState(level, caster.getUUID(), copy.getUUID(),','now >= state.expiresAt',
     'MobEffects.WEAKNESS, 12, 7','MobEffects.MINING_FATIGUE, 12, 6','MobEffects.NAUSEA, 120, 1')

utility=text(magic/'HighUtilitySpellService.java')
need(utility,'CLONE_TICKS = 1800','MAZE_TICKS = 480','private static final Map<UUID, CloneState> CLONES',
     'tickClones(level, now)','clone.getNavigation().moveTo(owner, 1.16)','MobEffects.NAUSEA, 120, 1',
     'MobEffects.SLOWNESS, 120, 3','MobEffects.WEAKNESS, 120, 3')
assert 'ItemStack' not in utility and 'EquipmentSlot' not in utility

control=text(magic/'HighControlSpellService.java')
need(control,'DOMINATE_TICKS = 1200','FEEBLEMIND_TICKS = 1800','power * .65',
     'mob.getBoundingBox().inflate(28.0)','target.getNavigation().moveTo(owner, 1.12)',
     'MobEffects.WEAKNESS, 12, 7','MobEffects.MINING_FATIGUE, 12, 6','MobEffects.SLOWNESS, 12, 2')

summary8=text(magic/'EighthCircleSpellSummary.java')
need(summary8,'90초간 실제 복제','60초 동안 탈취','90초 동안 붕괴','24초간 전장에서 완전히 추방','6초 동안')

ninth=text(magic/'NinthCircleSpellService.java')
need(ninth,'private static final Set<String> HANDLED = Set.of(','case "shapechange", "foresight" -> ArcaneBuffRuntime.apply(caster, spellId, power, range);','case "time_stop", "wish" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);','case "true_polymorph" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);','public static final int PRISMATIC_WALL_TICKS = 400;','public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)','double halfWidth = Math.max(13.0, Math.min(26.0, range * .32));')
assert 'ParticleTypes' not in ninth

death=text(magic/'DeathDoctrineService.java'); need(death,'Set.of("circle_of_death", "finger_of_death", "power_word_kill")','double threshold = Math.max(180.0, power * 1.24);','boolean executed = pool <= threshold;')
circle_section=death[death.index('private static boolean circleOfDeath'):death.index('private static boolean fingerOfDeath')]; finger_section=death[death.index('private static boolean fingerOfDeath'):death.index('private static boolean powerWordKill')]
assert 'threshold' not in circle_section and 'executed' not in circle_section and 'threshold' not in finger_section and 'executed' not in finger_section

magnitude=text(magic/'NinthCircleMagnitude.java')
need(magnitude,'BASE_METEOR_CAST_RANGE = 72.0','BASE_METEOR_CITY_RADIUS = 112.0','return clamp(Math.max(110.0, range * 1.5555555556), 110.0, 168.0);','return clampInt((int) Math.round(31.0 + field * .16), 49, 61);')
pattern=text(magic/'MeteorBarragePattern.java')
need(pattern,'GOLDEN_ANGLE','NinthCircleMagnitude.meteorStrikeCount(range)','NinthCircleMagnitude.meteorFieldRadius(effectiveRange)','rememberRange(long seed,double range)','rememberPayload(String state)','withContext(long seed,double range,Supplier<T>action)')
assert 'BASE_STRIKES' not in pattern

ground=text(magic/'GroundTargetResolver.java')
need(ground,'public static Vec3 surface(ServerLevel level, Vec3 desired)','public static Optional<BlockPos> safeStanding','level.getMaxY() - 2','validStanding(level','horizontalDistanceSqr')

alpha65=text(magic/'Alpha65NinthCircleRuntime.java')
need(alpha65,'WEIRD_ESCAPE_TICKS = 300','GATE_TICKS = 600','case "gate" -> gate(level, caster, snapshot, range);','case "weird" -> weird(level, caster, snapshot.target(), range, power);','GroundTargetResolver.surface(level, nominal)','captureNightmareVictims(field, caster)','value -> value != owner && value.isAlive()','if (!insideNightmare(field, target))','float fatal = Math.max(4096.0F','GroundTargetResolver.safeStanding(level, requested, 18)','caster.position().add(forward.scale(2.6))','processGateEndpoint(level, gate, gate.source','public static boolean worldSunder','double along = flat.dot(forward)','double lateral = Math.abs(flat.dot(right))','for (int i = -6; i <= 6; i++)')
assert 'TeleportTransition.Relative' not in alpha65
need(alpha65,'Set.<Relative>of()')

kinetics=text(magic/'SpellKineticsService.java')
need(kinetics,'import net.minecraft.world.phys.Vec3;','MeteorBarragePattern.rememberRange(targetSnapshot.barrageSeed(), cast.range())','Alpha65NinthCircleRuntime.worldSunder(player, range, power, targetSnapshot)','Alpha65NinthCircleRuntime.executeOrDelegate(player, spellId, range, power, targetSnapshot)','Alpha65NinthCircleRuntime.meteorImpact(player','Alpha65NinthCircleRuntime.groundedBarrageCenter')
world_magic=text(magic/'WorldMagicService.java')
need(world_magic,'GroundTargetResolver.surface((ServerLevel) caster.level(), around)','GroundTargetResolver.safeStanding(level, desired, 10)','MeteorBarragePattern.firstImpactTick(snapshot.barrageSeed(), cast.range())','MeteorBarragePattern.durationTicks(snapshot.barrageSeed(), cast.range())','EighthCircleSpellService.NPC_DOMINATE_TICKS','EighthCircleSpellService.NPC_FEEBLEMIND_TICKS','EighthCircleSpellService.NPC_MAZE_TICKS')
assert 'scale(Math.min(3.0, range))' not in world_magic

gameplay=text(magic/'SpellGameplayService.java')
need(gameplay,'case "weird" -> Alpha65NinthCircleRuntime.WEIRD_ESCAPE_TICKS;','case "gate" -> Alpha65NinthCircleRuntime.GATE_TICKS;','case "prismatic_wall" -> 400;','case "true_polymorph" -> 480;')

npc_resolver=text(world/'NpcSpellResolver.java'); need(npc_resolver,'Alpha65NinthCircleRuntime.executeNpcOrDelegate')
npc_meteor=text(world/'NpcMeteorBarrageService.java'); need(npc_meteor,'Alpha65NinthCircleRuntime.meteorImpactNpc','Alpha65NinthCircleRuntime.groundedBarrageCenter')

prestige=text(client/'HighCirclePrestigeOverlay.java')
assert 'denseGrandArray' not in prestige and 'tierScaffold' not in prestige
need(prestige,'spell.circle() != 9','case "meteor_swarm" -> meteorArtillery','case "power_word_kill" -> executionJudgment','case "prismatic_wall" -> sevenLawWall','case "shapechange" -> mythicBody','case "time_stop" -> frozenClockwork','case "true_polymorph" -> morphBlueprint','case "weird" -> nightmareVerdict','case "wish" -> realityManuscript','case "gate" -> pairedWorldDoor','case "foresight" -> causalityFan','private static void meteorArtillery','private static void executionJudgment','private static void sevenLawWall','private static void mythicBody','private static void frozenClockwork','private static void morphBlueprint','private static void nightmareVerdict','private static void realityManuscript','private static void pairedWorldDoor','private static void causalityFan','double width = Math.max(26.0, Math.min(52.0, range * .64));','Vec3 near = forward.scale(2.6).add(0, 2.25, 0);')
assert 'default -> tier' not in prestige and 'ParticleTypes' not in prestige

primary=text(client/'PrimaryGrimoireScreen.java')
need(primary,'Rect slotStrip()','Rect browserMain()','Rect browserListViewport()','Rect browserDetail()','Rect effectListViewport()','Rect effectDetail()','inspectedSpellId = spells.get(i).id();','drawEffectCompendium(g, l, mouseX, mouseY)','"개요"','"실제 효과 / 판정"','s.effectSummary()','왼쪽 주문을 클릭하면 이 설명이 바뀝니다.')
assert 'if (effects) return false;' not in primary

handlers=text(client/'ClientNetworkHandlers.java'); need(handlers,'MeteorBarragePattern.rememberPayload(payload.state());','new PrimaryGrimoireScreen(payload.page())')
need(main,'Alpha65NinthCircleRuntime.clear(player);','Alpha65NinthCircleRuntime.tick(level);','Alpha65NinthCircleRuntime.clearAll();','NinthCircleSpellService::onIncomingDamage')

assert index['presentation_scale_grammar']=={'1-2':'hand_scale_immediate','3-4':'combat_space','5-6':'multi_plane_grand_magic','7':'fortress_planar_authority','8':'regional_reality_authority','9':'world_law_catastrophe'}
assert index['ninth_circle_divine_scale_phase']=='grounded_targeting_and_individual_sigil_authority'
assert index['common_high_circle_array_overlay']=='removed_and_forbidden'
assert len(index['ninth_circle_visual_authority'])==10
city=index['meteor_cityfall']; assert city['baseline_radius']==112 and city['baseline_strikes']==49 and city['range_scaled'] is True and city['impact_surface']=='actual_loaded_surface_per_strike_xz'
assert set(city['projectile_scaling'])=={'count','spacing','body_scale','fall_height'}
assert index['meteor_ground_contract']=='actual_surface_per_authoritative_strike'
assert index['gate_target_contract']=='entity_independent_safe_ground_pair'
assert index['weird_contract']=='caster_excluded_escape_or_die_domain'
assert index['world_sunder_contract']=='horizontal_fault_corridor'
assert index['grimoire_ui']=='isolated_slot_strip_plus_clicked_effect_detail_inspector'
assert index['eighth_circle_preserved_authority']==['antimagic_field','control_weather','demiplane']
value=index['eighth_circle_value_pass_1']
assert set(value)=={'clone','dominate_monster','feeblemind','maze'}
assert value['clone']=='90s_single_bound_combat_copy_no_equipment_duplication'
assert value['dominate_monster']=='60s_enemy_combat_asset_theft'
assert value['feeblemind']=='90s_total_arcane_shutdown_and_severe_combat_degradation'
assert value['maze']=='24s_total_battlefield_exile_plus_6s_aftershock'

need(text(magic/'HighControlSpellService.java'),'NinthCircleSpellService.clear(subject);'); need(text(magic/'ThirdCircleSpellService.java'),'HighControlSpellService.clear(target);'); need(text(magic/'ArcaneFieldService.java'),'HighControlSpellService.clear(entity);')
verify=text(root/'tools/verify_jar.py'); need(verify,'0.12.1-alpha.66','Alpha65NinthCircleRuntime.class','GroundTargetResolver.class','alpha65_grounded_meteor=PASS','alpha66_eighth_circle_value_pass=PASS')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS'); print('all_109_explicit_effect_summaries=PASS'); print('alpha53_65_regression_anchors=PASS')
print('alpha66_bound_clone=PASS'); print('alpha66_dominate_faction_theft=PASS'); print('alpha66_feeblemind_arcane_shutdown=PASS')
print('alpha66_maze_exile_aftershock=PASS'); print('alpha66_eighth_circle_npc_parity=PASS'); print('alpha66_eighth_circle_value_pass=PASS')
print('alpha65_ground_target_contract=PASS'); print('alpha65_meteor_surface_per_strike=PASS'); print('alpha65_gate_safe_ground_pair=PASS')
print('alpha65_weird_escape_or_die=PASS'); print('alpha65_world_sunder_horizontal_fault=PASS')
print('alpha65_individual_ninth_circle_visuals=PASS'); print('alpha65_common_grand_array_forbidden=PASS')
print('alpha65_prismatic_wall_visual_hitbox_match=PASS'); print('alpha65_gate_visual_source_match=PASS')
print('alpha65_weird_gate_visual_lifetime_match=PASS')
print('alpha65_grimoire_slot_isolation_and_effect_detail=PASS'); print('alpha65_lifecycle=PASS')
