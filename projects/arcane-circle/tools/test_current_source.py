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
need(gradle,'mod_version=0.12.1-alpha.58')
need(main,'VERSION = "0.12.1-alpha.58"')
need(index,'"version": "0.12.1-alpha.58"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"first_circle_npc_parity": true','"second_circle_npc_parity": true',
     '"third_circle_npc_parity": true','"fourth_circle_npc_parity": true',
     '"fifth_circle_npc_parity": true','"sixth_circle_npc_parity": true',
     '"material_disintegrate_ray"','"npc_parity_invulnerability_globe"','"behavioral_mass_suggestion"',
     '"physical_move_earth"','"piercing_sunbeam"','"persistent_true_seeing"',
     '"fixed_freezing_sphere"','"fear_weakness_eyebite"','"casting_block_petrification"',
     '"weak_enemy_circle_of_death"')

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
need(sixth_summary,'실제 물질 파괴','1~5써클 Arcane 주문','실제로 전장에서 후퇴',
     '실제 지형 변형','복수 대상 피해','주변 은신을 주기적으로 벗기고',
     '초강력 동결','공포·쇠약','Arcane 시전 봉쇄','대형/보스급은 처형 제외')

runtime_files=[
 'FirstCircleSpellService.java','SecondCircleSpellService.java','ThirdCircleSpellService.java',
 'FourthCircleSpellService.java','FifthCircleSpellService.java','SixthCircleSpellService.java',
 'ExpandedSpellEffects.java','HighCircleSpellEffects.java','FusionSpellEffects.java','SpellGameplayService.java',
 'HighUtilitySpellService.java','HighControlSpellService.java','HighWardSpellService.java',
 'PlanarSpellService.java','SimulacrumService.java','ArcaneFieldService.java',
 'SpellKineticsService.java','SpellCastingService.java']
runtime='\n'.join(text(magic/name) for name in runtime_files)
missing=sorted(x for x in spells if f'"{x}"' not in runtime)
assert not missing,f'no runtime route: {missing}'

# UI, effect compendium and copy-target contracts stay intact.
definition=text(magic/'SpellDefinition.java')
primary=text(client/'PrimaryGrimoireScreen.java')
handlers=text(client/'ClientNetworkHandlers.java')
need(definition,'public String description()','return description;','public String effectSummary()',
     'FirstCircleSpellSummary.summary(id)','SixthCircleSpellSummary.summary(id)','SpellEffectSummary.summary(this)',
     'case "simulacrum" -> 28.0;','case "clone" -> 32.0;',
     'case "simulacrum", "clone" -> SigilAnchor.TARGET;')
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
}
for filename,ids in circle_specs.items():
    body=text(magic/filename)
    for spell in ids: need(body,f'"{spell}"')
    need(body,'public static boolean executeNpc(')

# Preserve 1C-4C established deep semantics.
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
     'SixthCircleSpellService.clear(target);')
fourth=text(magic/'FourthCircleSpellService.java')
need(fourth,'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm',
     'target.getRandom().nextFloat() < .45F','event.setAmount(Math.max(.05F, event.getAmount() * .50F))',
     'event.setAmount(event.getAmount() * .20F)','randomConfusionTarget(level, mob)',
     'public static boolean hasFreedom(LivingEntity target)','public static boolean blocksCasting(LivingEntity caster)')
assert 'setNoAi(true)' not in fourth

# Preserve alpha.57 fifth-circle battlefield authority including symmetric Force Wall collision.
fifth=text(magic/'FifthCircleSpellService.java')
need(fifth,
     'public static final int FORCE_WALL_TICKS = 240;','public static final int CLOUDKILL_TICKS = 220;',
     'public static final int TELEKINESIS_TICKS = 100;','public static final int HOLD_MONSTER_TICKS = 300;',
     'public static final int PASSWALL_TICKS = 240;','public static final int DOMINATE_PERSON_TICKS = 260;',
     'public static final int INSECT_PLAGUE_TICKS = 220;',
     'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)',
     'double normal = motion.dot(wall.forward);',
     'if (normal * side < 0.0) target.setDeltaMovement(motion.subtract(wall.forward.scale(normal)));',
     'state.center = state.center.add(state.drift.scale(.45));',
     'TELEKINESIS.put(target.getUUID(), new TelekinesisState','target.setNoGravity(state.wasNoGravity);',
     'DestructiveMagicService.impact(player, "flame_strike", center, radius, power)',
     'target.getMaxHealth() > 220.0F','target.heal(amount);',
     'changed.add(new ChangedBlock(pos.immutable(), original));','level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());',
     'if (level.getBlockState(block.pos).isAir()) level.setBlockAndUpdate(block.pos, block.original);',
     'DOMINATED.put(mob.getUUID(), new DominateState','target.getNavigation().moveTo(owner, 1.10);',
     'SWARM_JAM.put(target.getUUID(), new JamState(level, now + 12L));','interruptCasting(target);')
assert 'double into = motion.dot(wall.forward) * side;' not in fifth
assert 'setNoAi(true)' not in fifth
assert 'ParticleTypes' not in fifth

# alpha.58 sixth-circle authority: dedicated snapshot attacks + reused strong player semantics + NPC parity.
sixth=text(magic/'SixthCircleSpellService.java')
need(sixth,
     'public static final int NPC_GLOBE_TICKS = HighWardSpellService.GLOBE_TICKS;',
     'public static final int NPC_SUGGESTION_TICKS = 160;',
     'public static final int NPC_TRUE_SEEING_TICKS = 1200;',
     'public static final int NPC_PETRIFY_TICKS = 360;',
     'case "globe_of_invulnerability" -> HighWardSpellService.execute(caster, spellId, range, power, snapshot);',
     'case "mass_suggestion" -> HighControlSpellService.execute(caster, spellId, range, power, snapshot);',
     'case "true_seeing" -> SpellGameplayService.execute(caster, spellId, range, power, snapshot);',
     'case "flesh_to_stone" -> SpellGameplayService.execute(caster, spellId, range, power, snapshot);',
     'public static boolean intercepts(LivingEntity caster, SpellDefinition spell,',
     'spell.circle() > MAX_GLOBE_BLOCKED_CIRCLE','segmentDistanceSqr(snapshot.launchOrigin(), target, center)',
     'DestructiveMagicService.ray(player, "disintegrate", start, end, power);',
     'DestructiveMagicService.impact(player, "move_earth", center, radius, power);',
     'lineDamage(level, caster, start, end, 1.55, power','target.setRemainingFireTicks(0);',
     'target.getTicksRequiredToFreeze() + 520','EYEBITE_TICKS = 360;',
     'NPC_SUGGESTIONS.put(target.getUUID(), new RetreatState','NPC_TRUE_SIGHT.put(caster.getUUID()',
     'NPC_PETRIFY.put(target.getUUID(), new PetrifyState','SpellKineticsService.cancel(player);',
     'boolean ordinary = target.getMaxHealth() <= Math.max(80.0, power * 1.55)',
     'boolean weak = effectivePool <= Math.max(18.0, power * .92);')
assert 'ParticleTypes' not in sixth
assert 'setNoAi(true)' not in sixth

# Dedicated player ownership order is 1C -> 2C -> 3C -> 4C -> 5C -> 6C -> specials -> generic.
kinetics=text(magic/'SpellKineticsService.java')
for token in ['FirstCircleSpellService.handles(cast.spell().id())','SecondCircleSpellService.handles(cast.spell().id())',
              'ThirdCircleSpellService.handles(cast.spell().id())','FourthCircleSpellService.handles(cast.spell().id())',
              'FifthCircleSpellService.handles(cast.spell().id())','SixthCircleSpellService.handles(cast.spell().id())',
              'FifthCircleSpellService.intercepts(player, targetSnapshot)',
              'SixthCircleSpellService.intercepts(player, spell, targetSnapshot, range)',
              'SixthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)']:
    need(kinetics,token)
order=['boolean firstCircleOwned','boolean secondCircleOwned','boolean thirdCircleOwned','boolean fourthCircleOwned',
       'boolean fifthCircleOwned','boolean sixthCircleOwned','boolean planarOwned']
assert [kinetics.index(x) for x in order]==sorted(kinetics.index(x) for x in order)

# NPCs use 1C-6C dedicated paths and both 5C Force Wall / NPC 6C Globe interception.
npc=text(world/'NpcSpellResolver.java')
for token in ['FirstCircleSpellService.handles(spell.id())','SecondCircleSpellService.handles(spell.id())',
              'ThirdCircleSpellService.handles(spell.id())','FourthCircleSpellService.handles(spell.id())',
              'FifthCircleSpellService.handles(spell.id())','SixthCircleSpellService.handles(spell.id())',
              'FifthCircleSpellService.intercepts(caster, snapshot)',
              'SixthCircleSpellService.intercepts(caster, spell, snapshot, range)',
              'SixthCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)']:
    need(npc,token)
npc_execute=npc[npc.index('static boolean execute('):]
order=['FirstCircleSpellService.handles','SecondCircleSpellService.handles','ThirdCircleSpellService.handles',
       'FourthCircleSpellService.handles','FifthCircleSpellService.handles','SixthCircleSpellService.handles',
       '"meteor_swarm".equals(spell.id())']
assert [npc_execute.index(x) for x in order]==sorted(npc_execute.index(x) for x in order)

# Existing player Globe/Mass Suggestion contracts remain stronger than old HighCircle aliases.
ward=text(magic/'HighWardSpellService.java')
need(ward,'GLOBE_TICKS = 520','MAX_BLOCKED_CIRCLE = 5','public static boolean intercepts(','segmentDistanceSqr')
control=text(magic/'HighControlSpellService.java')
need(control,'"mass_suggestion", "forcecage", "dominate_monster", "feeblemind"',
     'MASS_SUGGESTION_TICKS = 160','target.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.20)')
buff=text(magic/'ArcaneBuffRuntime.java')
need(buff,'case "true_seeing" -> 1200;','if ("true_seeing".equals(state.spellId) && now % 10L == 0L) reveal(level, player, state.radius);',
     'entity.removeEffect(MobEffects.INVISIBILITY);','entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 18, 0, true, false));')

# Field priority: Freedom only bypasses lower movement locks; 5C/6C hard control still wins.
field=text(magic/'ArcaneFieldService.java')
need(field,'boolean movementFree = FourthCircleSpellService.hasFreedom(caster);',
     'if (!movementFree && SecondCircleSpellService.blocksCasting(caster)) return true;',
     'if (!movementFree && ThirdCircleSpellService.blocksCasting(caster)) return true;',
     'if (FourthCircleSpellService.blocksCasting(caster)) return true;',
     'if (FifthCircleSpellService.blocksCasting(caster)) return true;',
     'if (SixthCircleSpellService.blocksCasting(caster)) return true;',
     'FifthCircleSpellService.clear(entity);','SixthCircleSpellService.clear(entity);',
     'HighControlSpellService.blocksCasting(caster)','TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')

# Hard lifecycle boundaries clear fifth/sixth maintained world/control state.
need(main,'FifthCircleSpellService.clear(player);','FifthCircleSpellService.tick(level);','FifthCircleSpellService.clearAll();',
     'SixthCircleSpellService.clear(player);','SixthCircleSpellService.tick(level);','SixthCircleSpellService.clearAll();')
assert main.count('FifthCircleSpellService.clear(player);') >= 3
assert main.count('SixthCircleSpellService.clear(player);') >= 3

# Preserve high-circle established identities for next passes.
world_magic=text(magic/'WorldMagicService.java')
need(world_magic,'Optional<Mob> aimed = aimedMob(player, range, direction);',
     'case FRONT, TARGET, GROUND_TARGET -> aimed.map(Mob::getUUID).orElse(null);')
sim=text(magic/'SimulacrumService.java')
need(sim,'snapshot.targetEntity(caster).orElse(null)','Attributes.MAX_HEALTH, .50','Attributes.ATTACK_DAMAGE, .72',
     'Mode.FOLLOW','Mode.GUARD','Mode.ASSAULT')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")','copyCombatBody(source, clone)')
planar=text(magic/'PlanarSpellService.java')
need(planar,'Set.of("plane_shift", "demiplane")','ROOM_HALF = 10','x /= 8.0; z /= 8.0','x *= 8.0; z *= 8.0')

# Ray of Frost remains one beam, real lights remain ref-counted.
archetype=text(magic/'SpellArchetype.java')
channels=archetype[archetype.index('private static final Set<String> CHANNELS'):archetype.index('private static final Set<String> FIELDS')]
assert '"ray_of_frost"' not in channels
light=text(magic/'ArcaneLightService.java')
need(light,'Map<LightKey, Integer> REF_COUNTS','REF_COUNTS.put(key, count + 1)','if (count > 1)','REF_COUNTS.put(key, count - 1)')

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.58','material-breaking narrow ray','player/NPC 1~5C boundary denial',
     'behavioral multi-retreat','persistent invisibility reveal','casting-block petrification',
     'weak ordinary execution pressure')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'FirstCircleSpellService.class','SecondCircleSpellService.class','ThirdCircleSpellService.class',
     'FourthCircleSpellService.class','FifthCircleSpellService.class','SixthCircleSpellService.class',
     'SixthCircleSpellSummary.class','HighUtilitySpellService.class','PlanarSpellService.class',
     'SimulacrumService.class','HighControlSpellService.class','HighWardSpellService.class','PrimaryGrimoireScreen.class')

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
print('alpha58_globe_suggestion_petrify_authority=PASS')
print('alpha49_57_runtime_regressions=PASS')
