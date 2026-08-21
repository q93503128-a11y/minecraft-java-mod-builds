from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
world=root/'src/main/java/kr/moonseungjun/arcanecircle/world'

def text(p): return p.read_text(encoding='utf-8')
def need(body,*tokens):
    for token in tokens: assert token in body, token

gradle=text(root/'gradle.properties')
main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json')
need(gradle,'mod_version=0.12.1-alpha.60')
need(main,'VERSION = "0.12.1-alpha.60"')
need(index,'"version": "0.12.1-alpha.60"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"first_circle_npc_parity": true','"second_circle_npc_parity": true',
     '"third_circle_npc_parity": true','"fourth_circle_npc_parity": true',
     '"fifth_circle_npc_parity": true','"sixth_circle_npc_parity": true',
     '"seventh_circle_npc_parity": true','"eighth_circle_npc_parity": true',
     '"maintained_earthquake"','"drifting_incendiary_cloud"','"dedicated_sunburst"',
     '"timed_pocket_sanctuary_without_persistent_player_room"',
     '"combat_exile_without_cross_dimension_player_relocation"','"casting_and_combat_compulsion"')

catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog))
fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog))
spells=direct|fusions
assert (len(direct),len(fusions),len(spells))==(90,19,109)
need(catalog,'IMPLEMENTED_MAX_CIRCLE = 9','WORLD_MAX_CIRCLE = 9')

summary=text(magic/'SpellEffectSummary.java')
summary_ids=set(re.findall(r'case "([a-z0-9_]+)"',summary))
assert summary_ids==spells,(sorted(spells-summary_ids),sorted(summary_ids-spells))

sixth_summary=text(magic/'SixthCircleSpellSummary.java')
for spell in ['disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam',
              'true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death']:
    need(sixth_summary,f'case "{spell}"')
need(sixth_summary,'실제 물질 파괴','1~5써클 Arcane 주문','실제로 전장에서 후퇴','실제 지형 변형',
     '복수 대상 피해','주변 은신을 주기적으로 벗기고','초강력 동결',
     '효과가 끝날 때까지 강제 도주하며 Arcane 시전도 중단','Arcane 시전 봉쇄','대형/보스급은 처형 제외')

seventh_summary=text(magic/'SeventhCircleSpellSummary.java')
for spell in ['delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage',
              'plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport']:
    need(seventh_summary,f'case "{spell}"')
need(seventh_summary,'일반 피해의 88%','감옥 안의 이동·공격·시전 자체를 마비시키지는 않습니다',
     '실제 차원을 전환','7개의 좁은 프리즘 광선을 독립 발사','종료·디스펠·반마법 해제 시 원래 중력 상태를 복원',
     '최대 체력 50%','웅크린 G키','막힌 지점이나 공중에는 강제로 박히지 않습니다')

eighth_summary=text(magic/'EighthCircleSpellSummary.java')
for spell in ['antimagic_field','clone','control_weather','demiplane','dominate_monster',
              'earthquake','feeblemind','incendiary_cloud','maze','sunburst']:
    need(eighth_summary,f'case "{spell}"')
need(eighth_summary,'실제 반마법장','실제 생명체 복제본','45초간 실제 폭우·뇌우','개인 주머니 공간',
     '실제 전투 진영','9초간 반복 지진','Arcane 시전 봉쇄','12초간 이동하는 소이 구름',
     '18초간 전투에서 추방','광역 태양광')

runtime_files=[
 'FirstCircleSpellService.java','SecondCircleSpellService.java','ThirdCircleSpellService.java',
 'FourthCircleSpellService.java','FifthCircleSpellService.java','SixthCircleSpellService.java',
 'SeventhCircleSpellService.java','EighthCircleSpellService.java','ExpandedSpellEffects.java','HighCircleSpellEffects.java',
 'FusionSpellEffects.java','SpellGameplayService.java','HighUtilitySpellService.java','HighControlSpellService.java',
 'HighWardSpellService.java','PlanarSpellService.java','SimulacrumService.java','ArcaneFieldService.java',
 'SpellKineticsService.java','SpellCastingService.java']
runtime='\n'.join(text(magic/name) for name in runtime_files)
missing=sorted(x for x in spells if f'"{x}"' not in runtime)
assert not missing,f'no runtime route: {missing}'

# UI/effect compendium and copy-target contracts.
definition=text(magic/'SpellDefinition.java')
primary=text(client/'PrimaryGrimoireScreen.java')
handlers=text(client/'ClientNetworkHandlers.java')
need(definition,'public String description()','return description;','public String effectSummary()',
     'FirstCircleSpellSummary.summary(id)','SixthCircleSpellSummary.summary(id)',
     'SeventhCircleSpellSummary.summary(id)','EighthCircleSpellSummary.summary(id)','SpellEffectSummary.summary(this)',
     'case "simulacrum" -> 28.0;','case "clone" -> 32.0;','case "simulacrum", "clone" -> SigilAnchor.TARGET;')
need(primary,'public final class PrimaryGrimoireScreen extends Screen','"효과 도감"','s.effectSummary()',
     '"세부 판정은 효과 도감에서 확인"','"강점 · "','"약점 · "','"본거지 · "')
need(handlers,'new PrimaryGrimoireScreen(payload.page())','new GrimoireScreen(payload.page())')

circle_specs={
 'FirstCircleSpellService.java':['magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor'],
 'SecondCircleSpellService.java':['scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'],
 'ThirdCircleSpellService.java':['fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'],
 'FourthCircleSpellService.java':['wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door','stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer'],
 'FifthCircleSpellService.java':['cone_of_cold','wall_of_force','cloudkill','telekinesis','flame_strike','hold_monster','mass_cure_wounds','passwall','dominate_person','insect_plague'],
 'SixthCircleSpellService.java':['disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam','true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death'],
 'SeventhCircleSpellService.java':['delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage','plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport'],
 'EighthCircleSpellService.java':['antimagic_field','clone','control_weather','demiplane','dominate_monster','earthquake','feeblemind','incendiary_cloud','maze','sunburst'],
}
for filename,ids in circle_specs.items():
    body=text(magic/filename)
    for spell in ids: need(body,f'"{spell}"')
    need(body,'public static boolean executeNpc(')

# 1C-4C established deep semantics.
first=text(magic/'FirstCircleSpellService.java')
need(first,'sleepEligible(target, power)','ArcaneLightService.illuminate(player, 1800)',
     'DestructiveMagicService.applyPhysicalAftermath(player, "thunderwave"')
second=text(magic/'SecondCircleSpellService.java')
need(second,'new RaySalvo(level, caster.getUUID(), target.getUUID()','target.getRandom().nextFloat() < .35F',
     'stripFragileWindBlocks(level, origin, direction, length)','holdEligible(target)','finishLevitation(state, true)')
assert 'setNoAi(true)' not in second
third=text(magic/'ThirdCircleSpellService.java')
need(third,'ArcaneDamage.isResolving()','event.setAmount((float) Math.max(0.0, event.getAmount() * .55))',
     'float actual = Math.max(0.0F, before - after)','SLEET_ZONES.add(new SleetZone',
     'FourthCircleSpellService.clear(target);','FifthCircleSpellService.clear(target);',
     'SixthCircleSpellService.clear(target);','SeventhCircleSpellService.clear(target);',
     'EighthCircleSpellService.clear(target);','HighUtilitySpellService.clear(player);','SimulacrumService.clear(player);')
fourth=text(magic/'FourthCircleSpellService.java')
need(fourth,'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm',
     'target.getRandom().nextFloat() < .45F','event.setAmount(Math.max(.05F, event.getAmount() * .50F))',
     'event.setAmount(event.getAmount() * .20F)','randomConfusionTarget(level, mob)',
     'public static boolean hasFreedom(LivingEntity target)','public static boolean blocksCasting(LivingEntity caster)')
assert 'setNoAi(true)' not in fourth

# 5C battlefield authority.
fifth=text(magic/'FifthCircleSpellService.java')
need(fifth,'public static final int FORCE_WALL_TICKS = 240;','public static final int CLOUDKILL_TICKS = 220;',
     'public static final int TELEKINESIS_TICKS = 100;','public static final int HOLD_MONSTER_TICKS = 300;',
     'public static final int PASSWALL_TICKS = 240;','public static final int DOMINATE_PERSON_TICKS = 260;',
     'public static final int INSECT_PLAGUE_TICKS = 220;',
     'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)',
     'double normal = motion.dot(wall.forward);',
     'if (normal * side < 0.0) target.setDeltaMovement(motion.subtract(wall.forward.scale(normal)));',
     'state.center = state.center.add(state.drift.scale(.45));','TELEKINESIS.put(target.getUUID(), new TelekinesisState',
     'target.setNoGravity(state.wasNoGravity);','DestructiveMagicService.impact(player, "flame_strike", center, radius, power)',
     'target.getMaxHealth() > 220.0F','target.heal(amount);','changed.add(new ChangedBlock(pos.immutable(), original));',
     'level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());','DOMINATED.put(mob.getUUID(), new DominateState',
     'target.getNavigation().moveTo(owner, 1.10);','SWARM_JAM.put(target.getUUID(), new JamState(level, now + 12L));',
     'interruptCasting(target);')
assert 'double into = motion.dot(wall.forward) * side;' not in fifth
assert 'setNoAi(true)' not in fifth
assert 'ParticleTypes' not in fifth

# 6C authority remains unchanged.
sixth=text(magic/'SixthCircleSpellService.java')
need(sixth,'public static final int NPC_GLOBE_TICKS = HighWardSpellService.GLOBE_TICKS;',
     'public static final int NPC_SUGGESTION_TICKS = 160;','public static final int NPC_TRUE_SEEING_TICKS = 1200;',
     'public static final int NPC_PETRIFY_TICKS = 360;','private static final Map<UUID, FearState> EYEBITE_FEAR = new HashMap<>();',
     'case "globe_of_invulnerability" -> HighWardSpellService.execute(caster, spellId, range, power, snapshot);',
     'case "mass_suggestion" -> HighControlSpellService.execute(caster, spellId, range, power, snapshot);',
     'DestructiveMagicService.ray(player, "disintegrate", start, end, power);',
     'DestructiveMagicService.impact(player, "move_earth", center, radius, power);',
     'target.getTicksRequiredToFreeze() + 520','EYEBITE_TICKS = 360;',
     'target.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.22);',
     'NPC_PETRIFY.put(target.getUUID(), new PetrifyState','boolean weak = effectivePool <= Math.max(18.0, power * .92);')
assert 'ParticleTypes' not in sixth
assert 'setNoAi(true)' not in sixth

# 7C authority and preserved strong semantics.
seventh=text(magic/'SeventhCircleSpellService.java')
need(seventh,'private static final Set<String> HANDLED = Set.of(',
     'case "etherealness" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);',
     'case "forcecage" -> HighControlSpellService.execute(caster, spellId, range, power, snapshot);',
     'case "plane_shift" -> PlanarSpellService.execute(caster, spellId);',
     'case "simulacrum" -> SimulacrumService.execute(caster, snapshot);',
     'DestructiveMagicService.impact(player, "delayed_blast_fireball", center, radius, power);',
     'for (Vec3 pillar : pillars)','for (int band = 0; band < 7; band++)','!alreadyHit.contains(value.getUUID())',
     'private static final List<GravityField> GRAVITY_FIELDS = new ArrayList<>();','tickReverseGravity(level, now);',
     'case "plane_shift" -> npcPlaneShift(level, caster, target, snapshot);',
     'case "simulacrum" -> npcSimulacrum(level, caster, target, snapshot);','Attributes.MAX_HEALTH, .50',
     'BlockPos destination = findSafe(level, desired, 16);')
assert 'ParticleTypes' not in seventh
assert 'setNoAi(true)' not in seventh
assert '|| true' not in seventh

# alpha.60 8C: strong player meanings are delegated; weak generic spells are now maintained authorities.
eighth=text(magic/'EighthCircleSpellService.java')
need(eighth,'private static final Set<String> HANDLED = Set.of(',
     'case "antimagic_field" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);',
     'case "clone" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);',
     'case "control_weather" -> SpellGameplayService.execute(caster, spellId, range, power, snapshot);',
     'case "demiplane" -> PlanarSpellService.execute(caster, spellId);',
     'HighControlSpellService.execute(caster, spellId, range, power, snapshot);',
     'case "maze" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);',
     'public static final int EARTHQUAKE_TICKS = 180;','public static final int INCENDIARY_CLOUD_TICKS = 240;',
     'EARTHQUAKES.add(new EarthquakeField','DestructiveMagicService.impact(player, "earthquake", center, radius, power);',
     'field.nextPulse = now + 10L;','INCENDIARY_CLOUDS.add(new IncendiaryCloudField',
     'field.center = field.center.add(field.drift);','field.nextPulse = now + 8L;',
     'target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 260, 2, true, false));',
     'target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, true, false));',
     'case "antimagic_field" -> npcAntimagic(level, caster, range);','case "clone" -> npcClone(level, caster, target);',
     'case "control_weather" -> npcControlWeather(level, caster, range, power);',
     'case "demiplane" -> npcDemiplane(level, caster);','case "dominate_monster" -> npcDominate(level, caster, target, snapshot);',
     'case "feeblemind" -> npcFeeblemind(level, caster, target, snapshot);','case "maze" -> npcMaze(level, caster, target, snapshot);',
     'copyAttribute(caster, copy, Attributes.MAX_HEALTH);','summonLightning(level, target.position());',
     'caster.setInvulnerable(true);','target.setInvulnerable(true);','suppressPlayerCasting(player);',
     'public static boolean blocksCasting(LivingEntity caster)','tickNpcAntimagic(level, now);','tickNpcMaze(level, now);')
assert 'ParticleTypes' not in eighth
assert 'setNoAi(true)' not in eighth

# Dedicated player ownership order is 1C -> ... -> 8C -> preserved specials -> generic.
kinetics=text(magic/'SpellKineticsService.java')
for token in ['FirstCircleSpellService.handles(cast.spell().id())','SecondCircleSpellService.handles(cast.spell().id())',
              'ThirdCircleSpellService.handles(cast.spell().id())','FourthCircleSpellService.handles(cast.spell().id())',
              'FifthCircleSpellService.handles(cast.spell().id())','SixthCircleSpellService.handles(cast.spell().id())',
              'SeventhCircleSpellService.handles(cast.spell().id())','EighthCircleSpellService.handles(cast.spell().id())',
              'EighthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)']:
    need(kinetics,token)
order=['boolean firstCircleOwned','boolean secondCircleOwned','boolean thirdCircleOwned','boolean fourthCircleOwned',
       'boolean fifthCircleOwned','boolean sixthCircleOwned','boolean seventhCircleOwned','boolean eighthCircleOwned','boolean planarOwned']
assert [kinetics.index(x) for x in order]==sorted(kinetics.index(x) for x in order)

# NPC 1C-8C dedicated routes all precede meteor/generic fallback.
npc=text(world/'NpcSpellResolver.java')
for token in ['FirstCircleSpellService.handles(spell.id())','SecondCircleSpellService.handles(spell.id())',
              'ThirdCircleSpellService.handles(spell.id())','FourthCircleSpellService.handles(spell.id())',
              'FifthCircleSpellService.handles(spell.id())','SixthCircleSpellService.handles(spell.id())',
              'SeventhCircleSpellService.handles(spell.id())','EighthCircleSpellService.handles(spell.id())',
              'EighthCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)',
              'EighthCircleSpellService.blocksCasting(caster)']:
    need(npc,token)
npc_execute=npc[npc.index('static boolean execute('):]
order=['FirstCircleSpellService.handles','SecondCircleSpellService.handles','ThirdCircleSpellService.handles',
       'FourthCircleSpellService.handles','FifthCircleSpellService.handles','SixthCircleSpellService.handles',
       'SeventhCircleSpellService.handles','EighthCircleSpellService.handles','"meteor_swarm".equals(spell.id())']
assert [npc_execute.index(x) for x in order]==sorted(npc_execute.index(x) for x in order)

# Established strong high-circle source-of-truth services remain intact.
ward=text(magic/'HighWardSpellService.java')
need(ward,'GLOBE_TICKS = 520','MAX_BLOCKED_CIRCLE = 5','public static boolean intercepts(','segmentDistanceSqr')
control=text(magic/'HighControlSpellService.java')
need(control,'"mass_suggestion", "forcecage", "dominate_monster", "feeblemind"',
     'MASS_SUGGESTION_TICKS = 160','FORCECAGE_TICKS = 400','FORCECAGE_RADIUS = 3.1',
     'return !"forcecage".equals(state.spellId);','applyForcecage(target, state)',
     'DOMINATE_TICKS = 480','FEEBLEMIND_TICKS = 700')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")','copyCombatBody(source, clone)',
     'player.noPhysics = true;','player.getAbilities().mayfly = true;','event.getAmount() * 0.12F',
     'WorldMagicService.cancelRelease(player, "etherealness");','MAZE_TICKS = 360')
planar=text(magic/'PlanarSpellService.java')
need(planar,'Set.of("plane_shift", "demiplane")','ROOM_HALF = 10','x /= 8.0; z /= 8.0','x *= 8.0; z *= 8.0',
     'List<ServerPlayer> party = consentingParty(caster, origin);','teleport(member, target')
sim=text(magic/'SimulacrumService.java')
need(sim,'snapshot.targetEntity(caster).orElse(null)','Attributes.MAX_HEALTH, .50','Attributes.ATTACK_DAMAGE, .72',
     'Mode.FOLLOW','Mode.GUARD','Mode.ASSAULT')
gameplay=text(magic/'SpellGameplayService.java')
need(gameplay,'case "control_weather"','duration = 900','case "control_weather" -> useWeatherAuthority(player, state);')

# Antimagic, Dispel and lifecycle all extend through 8C.
field=text(magic/'ArcaneFieldService.java')
need(field,'if (EighthCircleSpellService.blocksCasting(caster)) return true;',
     'EighthCircleSpellService.clear(entity);','TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')
need(third,'EighthCircleSpellService.clear(target);')
need(main,'EighthCircleSpellService.clear(player);','EighthCircleSpellService.tick(level);','EighthCircleSpellService.clearAll();')
assert main.count('EighthCircleSpellService.clear(player);') >= 3

# Maintained 7C/8C presentation lifetime follows server authority.
worldmagic=text(magic/'WorldMagicService.java')
need(worldmagic,'seventhCircleVisualDuration','eighthCircleVisualDuration',
     'case "antimagic_field" -> Math.max(baseDuration, EighthCircleSpellService.NPC_ANTIMAGIC_TICKS);',
     'case "earthquake" -> Math.max(baseDuration, EighthCircleSpellService.EARTHQUAKE_TICKS);',
     'case "incendiary_cloud" -> Math.max(baseDuration, EighthCircleSpellService.INCENDIARY_CLOUD_TICKS);',
     'case "maze" -> Math.max(baseDuration, EighthCircleSpellService.NPC_MAZE_TICKS);')

# Ray of Frost remains one beam; real lights remain ref-counted.
archetype=text(magic/'SpellArchetype.java')
channels=archetype[archetype.index('private static final Set<String> CHANNELS'):archetype.index('private static final Set<String> FIELDS')]
assert '"ray_of_frost"' not in channels
light=text(magic/'ArcaneLightService.java')
need(light,'Map<LightKey, Integer> REF_COUNTS','REF_COUNTS.put(key, count + 1)','if (count > 1)','REF_COUNTS.put(key, count - 1)')

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.58','alpha.59','alpha.60','material-breaking narrow ray','locked delayed detonation',
     'preserved cross-dimension plane shift','maintained reverse gravity','preserved antimagic field',
     'preserved living clone','persistent demiplane','maintained earthquake','drifting incendiary cloud',
     'preserved maze exile','dedicated sunburst','NPC pocket-sanctuary role','NPC combat-exile role')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'FirstCircleSpellService.class','SecondCircleSpellService.class','ThirdCircleSpellService.class',
     'FourthCircleSpellService.class','FifthCircleSpellService.class','SixthCircleSpellService.class',
     'SeventhCircleSpellService.class','EighthCircleSpellService.class','SixthCircleSpellSummary.class',
     'SeventhCircleSpellSummary.class','EighthCircleSpellSummary.class','HighUtilitySpellService.class',
     'PlanarSpellService.class','SimulacrumService.class','HighControlSpellService.class','HighWardSpellService.class',
     'PrimaryGrimoireScreen.class','0.12.1-alpha.60')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('all_109_runtime_route_presence=PASS')
print('alpha52_readable_main_and_effect_compendium=PASS')
print('alpha53_first_circle_deep_runtime=PASS')
print('alpha54_second_circle_deep_runtime=PASS')
print('alpha55_third_circle_deep_runtime=PASS')
print('alpha56_fourth_circle_deep_runtime=PASS')
print('alpha57_fifth_circle_deep_runtime=PASS')
print('alpha58_sixth_circle_deep_runtime=PASS')
print('alpha58_sixth_circle_npc_parity=PASS')
print('alpha59_seventh_circle_deep_runtime=PASS')
print('alpha59_seventh_circle_npc_parity=PASS')
print('alpha59_preserved_ethereal_forcecage_plane_simulacrum=PASS')
print('alpha60_eighth_circle_deep_runtime=PASS')
print('alpha60_eighth_circle_npc_parity=PASS')
print('alpha60_preserved_antimagic_clone_weather_demiplane_dominate_feeblemind_maze=PASS')
print('alpha60_earthquake_cloud_sunburst_authority=PASS')
print('alpha60_dispel_antimagic_lifecycle=PASS')
print('alpha49_60_runtime_regressions=PASS')
