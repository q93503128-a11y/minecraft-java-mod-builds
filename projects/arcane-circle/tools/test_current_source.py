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
need(gradle,'mod_version=0.12.1-alpha.57')
need(main,'VERSION = "0.12.1-alpha.57"')
need(index,'"version": "0.12.1-alpha.57"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"first_circle_npc_parity": true','"second_circle_npc_parity": true',
     '"third_circle_npc_parity": true','"fourth_circle_npc_parity": true','"fifth_circle_npc_parity": true',
     '"widening_cone_of_cold"','"spell_intercepting_force_wall"','"drifting_cloudkill_front"',
     '"sustained_telekinesis_throw"','"vertical_flame_strike"','"boss_resisted_hold_monster"',
     '"allied_mass_cure"','"restoring_physical_passwall"','"person_scale_combat_domination"',
     '"casting_break_insect_plague"')

catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog))
fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog))
spells=direct|fusions
assert (len(direct),len(fusions),len(spells))==(90,19,109)
need(catalog,'IMPLEMENTED_MAX_CIRCLE = 9','WORLD_MAX_CIRCLE = 9')

summary=text(magic/'SpellEffectSummary.java')
summary_ids=set(re.findall(r'case "([a-z0-9_]+)"',summary))
assert summary_ids==spells,(sorted(spells-summary_ids),sorted(summary_ids-spells))
need(summary,
     'case "wall_of_force" -> "12초 실제 역장벽 · 적대 생명체 통과 저지 + 벽을 가로지르는 적대 Arcane 주문 차단"',
     'case "cloudkill" -> "11초 이동 독구름 · 시전 방향으로 천천히 전진하며 반복 독성 피해 · 약해진 대상 피해 증폭"',
     'case "telekinesis" -> "5초 동안 대상을 시선 앞에 붙잡아 조종 · 종료 순간 현재 시선 방향으로 강하게 투척"',
     'case "passwall" -> "실제 벽에 약 12초 임시 통로 생성 · 내부가 비면 원래 블록을 안전 복원 · 보호 블록은 관통 불가"',
     'case "dominate_person" -> "인간형 체급 적을 13초 전투 대리체로 지배 · 주변 위협 공격/비전투 추종 + Arcane 시전 봉쇄"')

runtime_files=[
 'FirstCircleSpellService.java','SecondCircleSpellService.java','ThirdCircleSpellService.java',
 'FourthCircleSpellService.java','FifthCircleSpellService.java','ExpandedSpellEffects.java',
 'HighCircleSpellEffects.java','FusionSpellEffects.java','SpellGameplayService.java',
 'HighUtilitySpellService.java','HighControlSpellService.java','HighWardSpellService.java',
 'PlanarSpellService.java','SimulacrumService.java','ArcaneFieldService.java',
 'SpellKineticsService.java','SpellCastingService.java']
runtime='\n'.join(text(magic/name) for name in runtime_files)
missing=sorted(x for x in spells if f'"{x}"' not in runtime)
assert not missing,f'no runtime route: {missing}'

# UI and copy-target contracts stay intact.
definition=text(magic/'SpellDefinition.java')
primary=text(client/'PrimaryGrimoireScreen.java')
handlers=text(client/'ClientNetworkHandlers.java')
need(definition,'public String description()','return description;','public String effectSummary()',
     'SpellEffectSummary.summary(this)','case "simulacrum" -> 28.0;','case "clone" -> 32.0;',
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
}
for filename,ids in circle_specs.items():
    body=text(magic/filename)
    for spell in ids: need(body,f'"{spell}"')
    need(body,'public static boolean executeNpc(')

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
     'FourthCircleSpellService.clear(target);','FifthCircleSpellService.clear(target);')
fourth=text(magic/'FourthCircleSpellService.java')
need(fourth,'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm',
     'target.getRandom().nextFloat() < .45F','event.setAmount(Math.max(.05F, event.getAmount() * .50F))',
     'event.setAmount(event.getAmount() * .20F)','randomConfusionTarget(level, mob)',
     'public static boolean hasFreedom(LivingEntity target)','public static boolean blocksCasting(LivingEntity caster)')
assert 'setNoAi(true)' not in fourth

# alpha.57 dedicated fifth-circle authority.
fifth=text(magic/'FifthCircleSpellService.java')
need(fifth,
     'public static final int FORCE_WALL_TICKS = 240;','public static final int CLOUDKILL_TICKS = 220;',
     'public static final int TELEKINESIS_TICKS = 100;','public static final int HOLD_MONSTER_TICKS = 300;',
     'public static final int PASSWALL_TICKS = 240;','public static final int DOMINATE_PERSON_TICKS = 260;',
     'public static final int INSECT_PLAGUE_TICKS = 220;',
     'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)',
     'return lateral <= wall.halfWidth + .8 && vertical >= -.5 && vertical <= 5.5;',
     'state.center = state.center.add(state.drift.scale(.45));',
     'TELEKINESIS.put(target.getUUID(), new TelekinesisState','target.setNoGravity(state.wasNoGravity);',
     'DestructiveMagicService.impact(player, "flame_strike", center, radius, power)',
     'target.getMaxHealth() > 220.0F','target.heal(amount);',
     'changed.add(new ChangedBlock(pos.immutable(), original));','level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());',
     'if (level.getBlockState(block.pos).isAir()) level.setBlockAndUpdate(block.pos, block.original);',
     'DOMINATED.put(mob.getUUID(), new DominateState','target.getNavigation().moveTo(owner, 1.10);',
     'SWARM_JAM.put(target.getUUID(), new JamState(level, now + 12L));','interruptCasting(target);')
assert 'setNoAi(true)' not in fifth
assert 'ParticleTypes' not in fifth

# Dedicated player ownership order is 1C -> 2C -> 3C -> 4C -> 5C -> specials -> generic.
kinetics=text(magic/'SpellKineticsService.java')
for token in ['FirstCircleSpellService.handles(cast.spell().id())','SecondCircleSpellService.handles(cast.spell().id())',
              'ThirdCircleSpellService.handles(cast.spell().id())','FourthCircleSpellService.handles(cast.spell().id())',
              'FifthCircleSpellService.handles(cast.spell().id())','FifthCircleSpellService.intercepts(player, targetSnapshot)',
              'FifthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)']:
    need(kinetics,token)
order=['boolean firstCircleOwned','boolean secondCircleOwned','boolean thirdCircleOwned','boolean fourthCircleOwned','boolean fifthCircleOwned','boolean planarOwned']
assert [kinetics.index(x) for x in order]==sorted(kinetics.index(x) for x in order)

# NPCs have the same 1C-5C ownership and Force Wall interception.
npc=text(world/'NpcSpellResolver.java')
for token in ['FirstCircleSpellService.handles(spell.id())','SecondCircleSpellService.handles(spell.id())',
              'ThirdCircleSpellService.handles(spell.id())','FourthCircleSpellService.handles(spell.id())',
              'FifthCircleSpellService.handles(spell.id())','FifthCircleSpellService.intercepts(caster, snapshot)',
              'FifthCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)']:
    need(npc,token)
npc_execute=npc[npc.index('static boolean execute('):]
order=['FirstCircleSpellService.handles','SecondCircleSpellService.handles','ThirdCircleSpellService.handles',
       'FourthCircleSpellService.handles','FifthCircleSpellService.handles','"meteor_swarm".equals(spell.id())']
assert [npc_execute.index(x) for x in order]==sorted(npc_execute.index(x) for x in order)

# Field priority: Freedom only bypasses lower movement locks; fifth hard control still wins.
field=text(magic/'ArcaneFieldService.java')
need(field,'boolean movementFree = FourthCircleSpellService.hasFreedom(caster);',
     'if (!movementFree && SecondCircleSpellService.blocksCasting(caster)) return true;',
     'if (!movementFree && ThirdCircleSpellService.blocksCasting(caster)) return true;',
     'if (FourthCircleSpellService.blocksCasting(caster)) return true;',
     'if (FifthCircleSpellService.blocksCasting(caster)) return true;',
     'FifthCircleSpellService.clear(entity);','HighControlSpellService.blocksCasting(caster)',
     'TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')

# Hard lifecycle boundaries clear fifth-circle world/motion state.
need(main,'FifthCircleSpellService.clear(player);','FifthCircleSpellService.tick(level);','FifthCircleSpellService.clearAll();')
assert main.count('FifthCircleSpellService.clear(player);') >= 3

# Preserve high-circle established identities.
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
ward=text(magic/'HighWardSpellService.java')
need(ward,'GLOBE_TICKS = 520','MAX_BLOCKED_CIRCLE = 5','public static boolean intercepts(','segmentDistanceSqr')
control=text(magic/'HighControlSpellService.java')
need(control,'"mass_suggestion", "forcecage", "dominate_monster", "feeblemind"','FORCECAGE_TICKS = 400')

# Ray of Frost remains one beam, real lights are ref-counted.
archetype=text(magic/'SpellArchetype.java')
channels=archetype[archetype.index('private static final Set<String> CHANNELS'):archetype.index('private static final Set<String> FIELDS')]
assert '"ray_of_frost"' not in channels
light=text(magic/'ArcaneLightService.java')
need(light,'Map<LightKey, Integer> REF_COUNTS','REF_COUNTS.put(key, count + 1)','if (count > 1)','REF_COUNTS.put(key, count - 1)')

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.57','body + Arcane trajectory barrier','real tunnel + safe restore','person-scale combat proxy','Dispel/Antimagic/lifecycle cleanup')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'FirstCircleSpellService.class','SecondCircleSpellService.class','ThirdCircleSpellService.class',
     'FourthCircleSpellService.class','FifthCircleSpellService.class','HighUtilitySpellService.class',
     'PlanarSpellService.class','SimulacrumService.class','HighControlSpellService.class',
     'HighWardSpellService.class','PrimaryGrimoireScreen.class')

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
print('alpha57_fifth_circle_npc_parity=PASS')
print('alpha57_force_wall_and_passwall_authority=PASS')
print('alpha49_56_runtime_regressions=PASS')
