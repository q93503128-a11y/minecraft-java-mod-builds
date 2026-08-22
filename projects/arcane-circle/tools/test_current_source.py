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
need(gradle,'mod_version=0.12.1-alpha.63')
need(main,'VERSION = "0.12.1-alpha.63"')
assert index['version']=='0.12.1-alpha.63'
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

# alpha.53-60 regression anchors remain mandatory.
need(text(magic/'FirstCircleSpellService.java'),'sleepEligible(target, power)','ArcaneLightService.illuminate(player, 1800)')
need(text(magic/'SecondCircleSpellService.java'),'new RaySalvo(level, caster.getUUID(), target.getUUID()','holdEligible(target)')
need(text(magic/'ThirdCircleSpellService.java'),'ArcaneDamage.isResolving()','SLEET_ZONES.add(new SleetZone')
need(text(magic/'FourthCircleSpellService.java'),'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm')
need(text(magic/'FifthCircleSpellService.java'),'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)','DOMINATED.put')
need(text(magic/'SixthCircleSpellService.java'),'DestructiveMagicService.ray(player, "disintegrate"','NPC_PETRIFY.put')
need(text(magic/'SeventhCircleSpellService.java'),'case "plane_shift" -> PlanarSpellService.execute(caster, spellId);','for (int band = 0; band < 7; band++)')
need(text(magic/'EighthCircleSpellService.java'),'EARTHQUAKES.add(new EarthquakeField','INCENDIARY_CLOUDS.add(new IncendiaryCloudField')

# alpha.61 ninth-circle authority remains preserved underneath alpha.62 role/presentation layers.
ninth=text(magic/'NinthCircleSpellService.java')
need(ninth,
     'private static final Set<String> HANDLED = Set.of(',
     'case "shapechange", "foresight" -> ArcaneBuffRuntime.apply(caster, spellId, power, range);',
     'case "time_stop", "wish" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);',
     'case "true_polymorph" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);',
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

field=text(magic/'ArcaneFieldService.java')
need(field,'"antimagic_field".equals(spellId)','"time_stop".equals(spellId)','"wish".equals(spellId)',
     'TIME_STOP_TICKS = 160','case "wish" -> fulfillWish(player);')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")','TRUE_POLYMORPH_TICKS = 480','setNoAi(true)')
buffs=text(magic/'ArcaneBuffRuntime.java')
need(buffs,'case "shapechange" -> 1800;','case "foresight" -> 2400;','event.setCanceled(true);',
     'multiplier = Math.min(multiplier, .50);','multiplier = Math.min(multiplier, .75);')
player_data=text(magic/'MagicPlayerData.java')
need(player_data,'if ("wish".equals(cast.spell().id()))','state.mana = effectiveStats(player).maxMana();',
     'state.cooldowns.entrySet().removeIf(entry -> !"wish".equals(entry.getKey()));')

# alpha.62 death-school hierarchy: 6C erosion -> 7C soul rupture -> 9C exclusive execution.
death=text(magic/'DeathDoctrineService.java')
need(death,
     'Set.of("circle_of_death", "finger_of_death", "power_word_kill")',
     'double radius = Math.max(11.0, Math.min(18.0,',
     'double vitalityTax = Math.min(power * .38, target.getMaxHealth() * .11);',
     'double soulPressure = Math.min(power * .62, target.getMaxHealth() * .16);',
     'power * 1.92 + soulPressure',
     'double threshold = Math.max(180.0, power * 1.24);',
     'boolean executed = pool <= threshold;',
     'double lawDamage = power * 1.08 + Math.min(power * .80, target.getMaxHealth() * .20);',
     'return null; // A vanished locked target never retargets another creature.')
circle_section=death[death.index('private static boolean circleOfDeath'):death.index('private static boolean fingerOfDeath')]
finger_section=death[death.index('private static boolean fingerOfDeath'):death.index('private static boolean powerWordKill')]
assert 'threshold' not in circle_section and 'executed' not in circle_section
assert 'threshold' not in finger_section and 'executed' not in finger_section

sixth_summary=text(magic/'SixthCircleSpellSummary.java')
seventh_summary=text(magic/'SeventhCircleSpellSummary.java')
ninth_summary=text(magic/'NinthCircleSpellSummary.java')
need(sixth_summary,'처형 판정은 전혀 없고')
need(seventh_summary,'즉사 역치는 없으며')
need(ninth_summary,'9써클의 유일한 법칙 처형','15발의 seeded 운석','Crown Meteor')

# Player dispatch must route death doctrine before the old circle coordinators.
kinetics=text(magic/'SpellKineticsService.java')
need(kinetics,
     'if (DeathDoctrineService.handles(spellId))',
     'DeathDoctrineService.execute(player, spellId, range, power, targetSnapshot)',
     '15발 전장 파쇄 후 Crown Meteor 종말 낙하',
     'MeteorBarragePattern.isCrownStrike(pending.pulseIndex())',
     'MeteorCataclysmService.crownImpact(player, pending.targetSnapshot().target()',
     'NinthCircleSpellService.meteorImpact(player',
     'NinthCircleSpellService.intercepts(player, targetSnapshot)',
     'NinthCircleSpellService.blocksCasting(player)')
assert kinetics.index('if (DeathDoctrineService.handles(spellId))') < kinetics.index('boolean firstCircleOwned')
order=['boolean firstCircleOwned','boolean secondCircleOwned','boolean thirdCircleOwned','boolean fourthCircleOwned',
       'boolean fifthCircleOwned','boolean sixthCircleOwned','boolean seventhCircleOwned','boolean eighthCircleOwned',
       'boolean ninthCircleOwned','boolean planarOwned']
assert [kinetics.index(x) for x in order]==sorted(kinetics.index(x) for x in order)

# NPC death doctrine and Crown Meteor finale have role parity before generic resolution.
npc=text(world/'NpcSpellResolver.java')
need(npc,'DeathDoctrineService.handles(spell.id())',
     'DeathDoctrineService.executeNpc(level, caster, target, spell.id(), range, power, snapshot)',
     'NinthCircleSpellService.blocksCasting(caster)','NinthCircleSpellService.intercepts(caster, snapshot)')
assert npc.index('DeathDoctrineService.handles(spell.id())') < npc.index('FirstCircleSpellService.handles(spell.id())')
npc_meteor=text(world/'NpcMeteorBarrageService.java')
need(npc_meteor,
     'NinthCircleSpellService.resolveNpcMeteorImpact(level, caster',
     'MeteorBarragePattern.isCrownStrike(next)',
     'MeteorCataclysmService.crownImpactNpc(level, caster',
     'NinthCircleSpellService.blocksCasting(caster)')

# Crown Meteor is a deliberate delayed sixteenth event, and its terrain work stays budgeted.
pattern=text(magic/'MeteorBarragePattern.java')
need(pattern,
     'new Strike(0.0, 0.0, 74, 2.04, 58)',
     'public static int crownIndex()',
     'public static boolean isCrownStrike(int index)',
     'scale = clamp(base.scale() * (.97 + random.nextDouble() * .06), 1.94, 2.16);',
     'fallHeight = clamp(base.fallHeight() + (random.nextDouble() - .5) * 5.0, 54.0, 64.0);',
     'tick = Math.max(previousTick + 8, base.impactTick() + random.nextInt(3) - 1);')
cataclysm=text(magic/'MeteorCataclysmService.java')
need(cataclysm,
     'double killRadius = 19.0;',
     'DestructiveMagicService.impact(caster, "meteor_swarm", center.add(0, -1.0, 0),',
     '22.0, power * 2.30',
     'for (int i = 0; i < 12; i++)',
     'for (int i = 0; i < 8; i++)',
     'public static void crownImpactNpc(ServerLevel level, Mob caster',
     'entityCatastrophe(level, caster, center, power);')
assert 'destroyBlock(' not in cataclysm

# Global 1-6C scale envelope: presentation-only hierarchy and 5-6C multi-plane staging.
envelope=text(client/'CircleScaleEnvelope.java')
need(envelope,
     'spell.circle() < 1 || spell.circle() > 6',
     'case 1 -> .62;','case 2 -> .86;','case 3 -> 1.16;','case 4 -> 1.55;','case 5 -> 2.15;','default -> 2.95;',
     'case 5 -> 14.0;','default -> 19.0;',
     'int planes = circle == 5 ? 3 : 5;',
     'This layer is presentation-only. It never changes the authoritative damage/field footprint.')
assert 'ParticleTypes' not in envelope

# 7-9C prestige is special-scene authored instead of one global radius multiplier.
prestige=text(client/'HighCirclePrestigeOverlay.java')
for spell in ['fire_storm','reverse_gravity','plane_shift','forcecage','prismatic_spray',
              'meteor_swarm','power_word_kill','time_stop','wish','gate','world_sunder',
              'earthquake','sunburst','control_weather','prismatic_wall']:
    need(prestige,f'case "{spell}"')
need(prestige,
     'Vec3 sky = target.add(0, 48.0, 0);','double r = 22.0;',
     'double r = 12.0;','Math.min(104.0, range * 1.06)','Math.min(34.0, range * .58)',
     'Math.min(38.0, range * .62)','double height = 14.0;',
     'case 7 -> 4.2 + Math.min(2.6, range * .040);',
     'case 8 -> 7.2 + Math.min(4.0, range * .050);',
     'default -> 11.2 + Math.min(5.8, range * .055);')
assert 'ParticleTypes' not in prestige

tracker=text(client/'WorldMagicTracker.java')
need(tracker,
     'private static final int MAX_FRAME = 20000;',
     'private static final int MAX_ENTRY = 4600;',
     'CircleScaleEnvelope.charge(v.spell,v.direction,targetOffset(v),v.range,v.progress,v.startedAt)',
     'CircleScaleEnvelope.release(v.spell,v.direction,targetOffset(v),v.range,elapsedSeconds)',
     'HighCirclePrestigeOverlay.charge(',
     'HighCirclePrestigeOverlay.release(')

# alpha.63 restores the established book UI for atlas/academy instead of the alpha.62 experimental cards.
primary=text(client/'PrimaryGrimoireScreen.java')
handlers=text(client/'ClientNetworkHandlers.java')
need(handlers,'new PrimaryGrimoireScreen(payload.page())')
assert 'new ReadableGrimoireScreen(payload.page())' not in handlers
need(primary,
     'for (int slot = 0; slot < 5; slot++) drawSlot',
     'private List<String> wrap(String value, int pixels, int maxLines)',
     'private String fit(String value, int pixels)',
     'Rect academyInfo()','Rect join()',
     'current == inspectedTradition ? "현재 소속" : "소속 등록"')

# Preserve Dispel/Antimagic/lifecycle cleanup for high-circle state.
control=text(magic/'HighControlSpellService.java')
need(control,'NinthCircleSpellService.clear(subject);')
third=text(magic/'ThirdCircleSpellService.java')
need(third,'HighControlSpellService.clear(target);')
need(field,'HighControlSpellService.clear(entity);')
need(main,'NinthCircleSpellService::onIncomingDamage','NinthCircleSpellService.clear(player);',
     'NinthCircleSpellService.tick(level);','NinthCircleSpellService.clearAll();')

# Metadata records the exact visual and role hierarchy.
assert index['presentation_scale_grammar']=={
 '1-2':'hand_scale_immediate','3-4':'combat_space','5-6':'multi_plane_grand_magic',
 '7':'fortress_planar_authority','8':'regional_reality_authority','9':'world_law_catastrophe'}
assert set(index['high_circle_prestige'])=={
 'seven_circle_fortress_scale','eight_circle_regional_scale','nine_circle_world_law_scale',
 'precision_spells_use_dense_compact_seals','catastrophes_use_world_footprints'}
assert index['death_doctrine']=={
 'circle_of_death':'wide_life_erosion_no_execution',
 'finger_of_death':'single_target_soul_rupture_no_threshold_execution',
 'power_word_kill':'exclusive_ninth_circle_execution_with_fallback_life_collapse'}
assert index['crown_meteor']=='fifteenth_barrage_then_delayed_sixteenth_crown_cataclysm'
assert index['grimoire_ui']=='primary_book_style_restored_for_atlas_and_academy'
assert set(index['ninth_circle_deep_audit'])=={
 'crown_cataclysm_meteor_swarm','exclusive_execution_power_word_kill','seven_layer_physical_prismatic_wall',
 'preserved_shapechange','preserved_time_stop','preserved_true_polymorph','maintained_behavioral_weird',
 'preserved_reality_wish','two_way_safe_gate','preserved_foresight'}
assert set(index['ninth_circle_preserved_authority'])=={'shapechange','time_stop','true_polymorph','wish','foresight'}
assert index['ninth_circle_npc_parity'] is True
assert index['ninth_circle_npc_true_polymorph_player_role']=='combat_and_casting_suppression_without_player_entity_type_swap'
assert index['ninth_circle_npc_wish_role']=='full_recovery_cleanse_without_player_mana_cooldown_state'
assert index['ninth_circle_gate_role']=='two_way_same_dimension_safe_entity_portal'

# Authored high-circle presentation and packaging guards.
timeline=text(client/'AuthoredHighCircleTimeline.java')
for spell in circle_specs['NinthCircleSpellService.java']: need(timeline,f'"{spell}"')
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.62','CircleScaleEnvelope','HighCirclePrestigeOverlay','DeathDoctrineService','Crown Meteor','ReadableGrimoireScreen')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,
     '0.12.1-alpha.63','DeathDoctrineService.class','MeteorCataclysmService.class',
     'HighCirclePrestigeOverlay.class','CircleScaleEnvelope.class','PrimaryGrimoireScreen.class',
     'alpha63_primary_grimoire_restore=PASS','alpha62_high_circle_prestige=PASS','alpha62_crown_meteor=PASS')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('alpha53_60_circle_regression_anchors=PASS')
print('alpha61_ninth_circle_authority_preserved=PASS')
print('alpha63_primary_grimoire_restore=PASS')
print('alpha62_circle_scale_hierarchy=PASS')
print('alpha62_high_circle_prestige=PASS')
print('alpha62_death_doctrine=PASS')
print('alpha62_crown_meteor_player_npc_parity=PASS')
print('alpha62_dispel_antimagic_lifecycle=PASS')
