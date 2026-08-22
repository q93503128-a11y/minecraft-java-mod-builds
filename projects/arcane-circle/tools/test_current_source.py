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
need(gradle,'mod_version=0.12.1-alpha.64'); need(main,'VERSION = "0.12.1-alpha.64"')
assert index['version']=='0.12.1-alpha.64' and index['implemented_circles']==list(range(1,10))
assert index['direct_spells']==90 and index['fusion_spells']==19

catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog)); fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog)); spells=direct|fusions
assert (len(direct),len(fusions),len(spells))==(90,19,109)
summary=text(magic/'SpellEffectSummary.java'); summary_ids=set(re.findall(r'case "([a-z0-9_]+)"',summary)); assert summary_ids==spells
need(text(magic/'SpellDefinition.java'),'NinthCircleSpellSummary.summary(id)','하늘 전체를 낙성 회로로 바꾸어 도시권을 수십 발의 거대 운석')

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
need(text(magic/'EighthCircleSpellService.java'),'EARTHQUAKES.add(new EarthquakeField','INCENDIARY_CLOUDS.add(new IncendiaryCloudField')

ninth=text(magic/'NinthCircleSpellService.java')
need(ninth,'private static final Set<String> HANDLED = Set.of(','case "shapechange", "foresight" -> ArcaneBuffRuntime.apply(caster, spellId, power, range);','case "time_stop", "wish" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);','case "true_polymorph" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);','public static final int PRISMATIC_WALL_TICKS = 400;','public static final int WEIRD_TICKS = 300;','public static final int GATE_TICKS = 600;','public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)')
assert 'ParticleTypes' not in ninth

death=text(magic/'DeathDoctrineService.java'); need(death,'Set.of("circle_of_death", "finger_of_death", "power_word_kill")','double threshold = Math.max(180.0, power * 1.24);','boolean executed = pool <= threshold;')
circle_section=death[death.index('private static boolean circleOfDeath'):death.index('private static boolean fingerOfDeath')]; finger_section=death[death.index('private static boolean fingerOfDeath'):death.index('private static boolean powerWordKill')]
assert 'threshold' not in circle_section and 'executed' not in circle_section and 'threshold' not in finger_section and 'executed' not in finger_section

magnitude=text(magic/'NinthCircleMagnitude.java')
need(magnitude,'BASE_METEOR_CAST_RANGE = 72.0','BASE_METEOR_CITY_RADIUS = 112.0','return clamp(Math.max(110.0, range * 1.5555555556), 110.0, 168.0);','return clampInt((int) Math.round(31.0 + field * .16), 49, 61);','return clamp((1.45 + radial * 1.18 + jitter * .62) * Math.sqrt(fieldFactor), 1.50, 4.20);','return clamp(5.25 + meteorFieldRadius(effectiveRange) / BASE_METEOR_CITY_RADIUS * .72, 5.80, 6.80);','public static double crownLethalRadius(double effectiveRange)','public static double crownShockRadius(double effectiveRange)')

pattern=text(magic/'MeteorBarragePattern.java')
need(pattern,'GOLDEN_ANGLE','NinthCircleMagnitude.meteorStrikeCount(range)','NinthCircleMagnitude.meteorFieldRadius(effectiveRange)','NinthCircleMagnitude.meteorOrdinaryScale','NinthCircleMagnitude.meteorFallHeight','NinthCircleMagnitude.crownScale','NinthCircleMagnitude.crownFallHeight','rememberRange(long seed,double range)','rememberPayload(String state)','withContext(long seed,double range,Supplier<T>action)','SAFE_VISUAL_RANGE = 108.0')
assert 'BASE_STRIKES' not in pattern

cataclysm=text(magic/'MeteorCataclysmService.java')
need(cataclysm,'NinthCircleMagnitude.crownLethalRadius(range)','NinthCircleMagnitude.crownShockRadius(range)','double[] fractions = {.24, .47, .70, .92};','int[] points = {8, 12, 16, 20};','DestructiveMagicService.impact(caster, "meteor_swarm"'); assert 'destroyBlock(' not in cataclysm

kinetics=text(magic/'SpellKineticsService.java')
need(kinetics,'MeteorBarragePattern.rememberRange(seed, cast.range());','MeteorBarragePattern.count(cast.range())','MeteorBarragePattern.strike(seed, cast.range(), 0)','MeteorBarragePattern.withContext(seed, range,','MeteorCataclysmService.crownImpact(player, pending.targetSnapshot().target(),','MeteorBarragePattern.isCrownStrike(range, pending.pulseIndex())')

npc_meteor=text(world/'NpcMeteorBarrageService.java')
need(npc_meteor,'MeteorBarragePattern.rememberRange(targetSnapshot.barrageSeed(), range);','int count = MeteorBarragePattern.count(barrage.range());','int strikeIndex = next;','MeteorBarragePattern.withContext(seed, barrage.range(),','MeteorCataclysmService.crownImpactNpc(level, caster,')

handlers=text(client/'ClientNetworkHandlers.java'); need(handlers,'MeteorBarragePattern.rememberPayload(payload.state());','new PrimaryGrimoireScreen(payload.page())'); assert 'new ReadableGrimoireScreen(payload.page())' not in handlers

prestige=text(client/'HighCirclePrestigeOverlay.java')
need(prestige,'private static void denseGrandArray(','multiple border bands, rune belts, chords, satellite','radius * .91, radius * .955','radius * .74, radius * .785','radius * .57, radius * .615','m.runeRing(basis, center, radius * .972','m.runeRing(basis, center, radius * .805','m.runeChords(basis, center, radius * .52','double field = NinthCircleMagnitude.meteorFieldRadius(range);','NinthCircleMagnitude.cityfallSkyHeight(range)','List<MeteorBarragePattern.Strike> strikes = MeteorBarragePattern.strikes(seed, range);','case "time_stop" -> temporalLaw(m, range','double r = Math.max(20.0, Math.min(48.0, range * .75));','case "prismatic_wall" -> prismAuthority')
for preserved in ['fireStormDominion','gravityCathedral','planarTransit','forceCitadel','prismaticFan','executionLaw','realityRewrite','worldGate','worldAxis','regionalFault','solarJudgment','weatherThrone']: need(prestige,f'void {preserved}')
assert 'ParticleTypes' not in prestige

primary=text(client/'PrimaryGrimoireScreen.java'); need(primary,'private List<String> wrap(String value, int pixels, int maxLines)','private String fit(String value, int pixels)','Rect academyInfo()','Rect join()')
assert index['presentation_scale_grammar']=={'1-2':'hand_scale_immediate','3-4':'combat_space','5-6':'multi_plane_grand_magic','7':'fortress_planar_authority','8':'regional_reality_authority','9':'world_law_catastrophe'}
assert index['ninth_circle_divine_scale_phase']=='meteor_cityfall_foundation'; city=index['meteor_cityfall']; assert city['baseline_radius']==112 and city['baseline_strikes']==49 and city['range_scaled'] is True
assert set(city['projectile_scaling'])=={'count','spacing','body_scale','fall_height'} and city['terminal_shock']=='full_cityfall_radius_with_inner_lethal_core' and city['terrain']=='budgeted_citywide_crater_lattice'
assert index['crown_meteor']=='range_scaled_cityfall_barrage_then_delayed_crown_cataclysm' and 'range_scaled_cityfall_meteor_swarm' in index['ninth_circle_deep_audit'] and index['grimoire_ui']=='primary_book_style_restored_for_atlas_and_academy'

need(text(magic/'HighControlSpellService.java'),'NinthCircleSpellService.clear(subject);'); need(text(magic/'ThirdCircleSpellService.java'),'HighControlSpellService.clear(target);'); need(text(magic/'ArcaneFieldService.java'),'HighControlSpellService.clear(entity);')
need(main,'NinthCircleSpellService::onIncomingDamage','NinthCircleSpellService.clear(player);','NinthCircleSpellService.tick(level);','NinthCircleSpellService.clearAll();')
verify=text(root/'tools/verify_jar.py'); need(verify,'0.12.1-alpha.64','NinthCircleMagnitude.class','HighCirclePrestigeOverlay.class','alpha64_meteor_cityfall=PASS','alpha64_dense_grand_arrays=PASS')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS'); print('all_109_explicit_effect_summaries=PASS'); print('alpha53_63_regression_anchors=PASS')
print('alpha64_meteor_cityfall_server_client_parity=PASS'); print('alpha64_range_scaled_projectiles=PASS'); print('alpha64_dense_grand_arrays=PASS'); print('alpha64_primary_grimoire_preserved=PASS'); print('alpha64_dispel_antimagic_lifecycle=PASS')
