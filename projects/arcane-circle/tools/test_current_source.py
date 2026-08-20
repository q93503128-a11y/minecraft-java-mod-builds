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
need(gradle,'mod_version=0.12.1-alpha.54')
need(main,'VERSION = "0.12.1-alpha.54"')
need(index,'"version": "0.12.1-alpha.54"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"copy_source_targeting": ["simulacrum_target_28","clone_target_32"]',
     '"first_circle_npc_parity": true','"second_circle_npc_parity": true',
     '"timed_three_ray_scorching_salvo"','"direct_attack_mirror_images"',
     '"aggro_breaking_invisibility"','"restricted_hold_person"','"direct_attack_blur"',
     '"rise_safe_descent_levitate"')

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
     'case "mirror_image" -> "13초 · 환영 3체가 적대 직접 공격 3회를 대신 받음 · 환경 피해는 그대로 받음"',
     'case "invisibility" -> "21초 투명화 · 주변 적대 추적 해제 + 첫 적대 직접 공격 1회 회피 후 은신 해제"',
     'case "blur" -> "18초 · 적대 직접 공격이 35% 확률로 빗나감 · 환경 피해에는 적용되지 않음"')

runtime_files=[
 'FirstCircleSpellService.java','SecondCircleSpellService.java','ExpandedSpellEffects.java','HighCircleSpellEffects.java',
 'FusionSpellEffects.java','SpellGameplayService.java','HighUtilitySpellService.java','HighControlSpellService.java',
 'HighWardSpellService.java','PlanarSpellService.java','SimulacrumService.java','ArcaneFieldService.java',
 'SpellKineticsService.java','SpellCastingService.java']
runtime='\n'.join(text(magic/name) for name in runtime_files)
missing=sorted(x for x in spells if f'"{x}"' not in runtime)
assert not missing,f'no runtime route: {missing}'

# alpha.52 UI remains readable: primary information and detailed effect compendium are separate.
definition=text(magic/'SpellDefinition.java')
primary=text(client/'PrimaryGrimoireScreen.java')
handlers=text(client/'ClientNetworkHandlers.java')
need(definition,'public String description()','return description;','public String effectSummary()',
     'SpellEffectSummary.summary(this)','case "simulacrum" -> 28.0;','case "clone" -> 32.0;',
     'case "simulacrum", "clone" -> SigilAnchor.TARGET;')
need(primary,'public final class PrimaryGrimoireScreen extends Screen','"효과 도감"','s.effectSummary()',
     '"세부 판정은 효과 도감에서 확인"','"강점 · "','"약점 · "','"본거지 · "',
     'Rect academyInfo()','"주문서 상점"')
need(handlers,'new PrimaryGrimoireScreen(payload.page())','new GrimoireScreen(payload.page())')

# alpha.53: every direct first-circle spell remains explicitly owned by one authoritative runtime.
first=text(magic/'FirstCircleSpellService.java')
for spell in ['magic_missile','fire_bolt','ray_of_frost','shield','feather_fall',
              'light','grease','sleep','thunderwave','mage_armor']:
    need(first,f'"{spell}"')
need(first,'private static final int SLEEP_TICKS = 140;','private static final int GREASE_TICKS = 160;',
     'snapshot.targetEntity(player).orElse(null)','ArcaneBuffRuntime.apply(player, "shield", power, range)',
     'ArcaneBuffRuntime.apply(player, "mage_armor", power, range)','MageGearService.grantStableDescent(player, 120)',
     'ArcaneLightService.illuminate(player, 1800)','sleepEligible(target, power)',
     'public static void onIncomingDamage(LivingIncomingDamageEvent event)','restoreSleep(state)',
     'DestructiveMagicService.applyPhysicalAftermath(player, "thunderwave"','public static boolean executeNpc(')

# alpha.54: every direct second-circle spell is owned by a dedicated semantic runtime.
second=text(magic/'SecondCircleSpellService.java')
for spell in ['scorching_ray','misty_step','web','mirror_image','invisibility',
              'gust_of_wind','hold_person','shatter','blur','levitate']:
    need(second,f'"{spell}"')
need(second,
     'public static final int WEB_TICKS = 220;','public static final int MIRROR_TICKS = 260;',
     'public static final int INVISIBILITY_TICKS = 420;','public static final int HOLD_PERSON_TICKS = 180;',
     'public static final int BLUR_TICKS = 360;','private static final int LEVITATE_RISE_TICKS = 60;',
     'private static final int RAY_GAP = 10;','new RaySalvo(level, caster.getUUID(), target.getUUID()',
     'caster.teleportTo(level','WEBS.add(new WebZone','new MirrorState(level, 3',
     'clearAggro(level, caster, 48.0)','event.getSource().getDirectEntity()',
     'target.getRandom().nextFloat() < .35F','stripFragileWindBlocks(level, origin, direction, length)',
     'holdEligible(target)','shatterBrittle(level, center, radius)','finishLevitation(state, true)',
     'public static boolean executeNpc(')
assert 'ReductionWard' not in second
assert 'setNoAi(true)' not in second

# First- then second-circle authority precedes all legacy aliases and still obeys visual impact timing.
kinetics=text(magic/'SpellKineticsService.java')
need(kinetics,'FirstCircleSpellService.handles(cast.spell().id())',
     'SecondCircleSpellService.handles(cast.spell().id())',
     'boolean firstCircleOwned = FirstCircleSpellService.handles(spellId);',
     'boolean secondCircleOwned = !firstCircleOwned && SecondCircleSpellService.handles(spellId);',
     'SecondCircleSpellService.execute(player, spellId, range, power, targetSnapshot)',
     'boolean planarOwned = !firstCircleOwned && !secondCircleOwned && PlanarSpellService.handles(spellId);')
assert kinetics.index('boolean firstCircleOwned') < kinetics.index('boolean secondCircleOwned') < kinetics.index('boolean planarOwned')

# Ray of Frost remains one beam, not a generic multi-pulse channel.
archetype=text(magic/'SpellArchetype.java')
channels=archetype[archetype.index('private static final Set<String> CHANNELS'):archetype.index('private static final Set<String> FIELDS')]
assert '"ray_of_frost"' not in channels

# Real LightBlocks retain shared ownership.
light=text(magic/'ArcaneLightService.java')
need(light,'Map<LightKey, Integer> REF_COUNTS','private static boolean claim(ServerLevel level, BlockPos pos)',
     'REF_COUNTS.put(key, count + 1)','if (count > 1)','REF_COUNTS.put(key, count - 1)',
     'private record LightKey(ResourceKey<Level> dimension, BlockPos pos)')

# NPCs use the same 1C and 2C identities before generic damage resolution.
npc=text(world/'NpcSpellResolver.java')
need(npc,'FirstCircleSpellService.handles(spell.id())',
     'FirstCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)',
     'SecondCircleSpellService.handles(spell.id())',
     'SecondCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)',
     'HighWardSpellService.intercepts(caster, spell, snapshot, range)')
npc_execute=npc[npc.index('static boolean execute('):]
assert npc_execute.index('FirstCircleSpellService.handles') < npc_execute.index('SecondCircleSpellService.handles') < npc_execute.index('"meteor_swarm".equals(spell.id())')

# Target snapshot still carries aimed creatures for copy spells.
world_magic=text(magic/'WorldMagicService.java')
need(world_magic,'Optional<Mob> aimed = aimedMob(player, range, direction);',
     'case FRONT, TARGET, GROUND_TARGET -> aimed.map(Mob::getUUID).orElse(null);')
sim=text(magic/'SimulacrumService.java')
need(sim,'snapshot.targetEntity(caster).orElse(null)','Attributes.MAX_HEALTH, .50',
     'Attributes.ATTACK_DAMAGE, .72','Mode.FOLLOW','Mode.GUARD','Mode.ASSAULT')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")',
     'Mob source = targetMob(player, snapshot);','copyCombatBody(source, clone)')

# Preserve alpha.49-51 high-circle identity while auditing lower circles.
planar=text(magic/'PlanarSpellService.java')
need(planar,'Set.of("plane_shift", "demiplane")','if (vertical > .35) return Level.END',
     'if (vertical < -.35) return Level.NETHER','x /= 8.0; z /= 8.0','x *= 8.0; z *= 8.0',
     'ROOM_HALF = 10','G키 또는 재시전으로 귀환')
control=text(magic/'HighControlSpellService.java')
need(control,'"mass_suggestion", "forcecage", "dominate_monster", "feeblemind"',
     'MASS_SUGGESTION_TICKS = 160','FORCECAGE_TICKS = 400','DOMINATE_TICKS = 480','FEEBLEMIND_TICKS = 700')
ward=text(magic/'HighWardSpellService.java')
need(ward,'GLOBE_TICKS = 520','MAX_BLOCKED_CIRCLE = 5','public static boolean intercepts(',
     'SpellMetrics.effectRadius','segmentDistanceSqr','6써클 이상 주문과 물리 공격은 그대로 통과')
field=text(magic/'ArcaneFieldService.java')
need(field,'SecondCircleSpellService.blocksCasting(caster)','SecondCircleSpellService.clear(entity)',
     'HighWardSpellService.clear(entity)','HighControlSpellService.clear(entity)',
     'TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')

# Deep lower-circle state is cleaned at every hard lifecycle boundary.
need(main,'FirstCircleSpellService::onIncomingDamage','SecondCircleSpellService::onIncomingDamage',
     'FirstCircleSpellService.tick(level)','SecondCircleSpellService.tick(level)',
     'FirstCircleSpellService.clear(player)','SecondCircleSpellService.clear(player)',
     'FirstCircleSpellService.clearAll()','SecondCircleSpellService.clearAll()',
     'MageGearService.clear(player.getUUID())','SimulacrumService.tick(level)',
     'HighUtilitySpellService.tick(level)','HighWardSpellService.tick(level)',
     'HighControlSpellService.tick(level)','ArcaneFieldService.tick(level)')
assert main.count('FirstCircleSpellService.clear(player);') >= 3
assert main.count('SecondCircleSpellService.clear(player);') >= 3
assert main.count('MageGearService.clear(player.getUUID());') >= 2

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.54','scorching_ray','direct attack','35%','NPC')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'FirstCircleSpellService.class','SecondCircleSpellService.class','HighUtilitySpellService.class',
     'PlanarSpellService.class','SimulacrumService.class','HighControlSpellService.class',
     'HighWardSpellService.class','PrimaryGrimoireScreen.class')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('all_109_runtime_route_presence=PASS')
print('alpha52_readable_main_and_effect_compendium=PASS')
print('alpha53_first_circle_deep_runtime=PASS')
print('alpha53_first_circle_npc_parity=PASS')
print('alpha54_second_circle_deep_runtime=PASS')
print('alpha54_second_circle_npc_parity=PASS')
print('alpha54_direct_attack_illusion_semantics=PASS')
print('alpha49_51_runtime_regressions=PASS')
