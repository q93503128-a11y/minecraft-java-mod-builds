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
need(gradle,'mod_version=0.12.1-alpha.55')
need(main,'VERSION = "0.12.1-alpha.55"')
need(index,'"version": "0.12.1-alpha.55"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"copy_source_targeting": ["simulacrum_target_28","clone_target_32"]',
     '"first_circle_npc_parity": true','"second_circle_npc_parity": true','"third_circle_npc_parity": true',
     '"falloff_fireball_blast"','"lifecycle_real_flight"','"custom_state_dispel_magic"',
     '"actual_damage_vampiric_drain"','"energy_only_recharging_ward"','"casting_break_sleet_storm"','"safe_long_blink"')

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
     'case "fireball" -> "고정 착탄점 화염 폭발 · 중심부 강한 피해/거리 감쇠 + 화상 + 주변 지형 일부 파괴"',
     'case "fly" -> "30초 실제 자유 비행 · 종료 시 기존 비행 권한 복원 + 안전 낙하"',
     'case "dispel_magic" -> "조준 대상의 유지형 강화·제어 마법 제거 · 대상이 없으면 자신의 해로운 상태 정화"',
     'case "vampiric_touch" -> "근거리 생명력 흡수 · 실제로 잃게 한 체력/흡수량의 60%만큼 회복"',
     'case "protection_from_energy" -> "30초 · 5중 공명막이 Arcane/화염/투사체성 충격만 45% 경감 · 3.5초마다 재충전"',
     'case "sleet_storm" -> "9초 진눈깨비 영역 · 반복 냉기 피해·동결·암흑·미끄럼 + 내부 적대 Arcane 시전 방해"')

runtime_files=[
 'FirstCircleSpellService.java','SecondCircleSpellService.java','ThirdCircleSpellService.java',
 'ExpandedSpellEffects.java','HighCircleSpellEffects.java','FusionSpellEffects.java',
 'SpellGameplayService.java','HighUtilitySpellService.java','HighControlSpellService.java',
 'HighWardSpellService.java','PlanarSpellService.java','SimulacrumService.java','ArcaneFieldService.java',
 'SpellKineticsService.java','SpellCastingService.java']
runtime='\n'.join(text(magic/name) for name in runtime_files)
missing=sorted(x for x in spells if f'"{x}"' not in runtime)
assert not missing,f'no runtime route: {missing}'

# UI/109-spell catalogue contract remains intact.
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

# alpha.53 first circle preserved.
first=text(magic/'FirstCircleSpellService.java')
for spell in ['magic_missile','fire_bolt','ray_of_frost','shield','feather_fall',
              'light','grease','sleep','thunderwave','mage_armor']:
    need(first,f'"{spell}"')
need(first,'private static final int SLEEP_TICKS = 140;','private static final int GREASE_TICKS = 160;',
     'ArcaneBuffRuntime.apply(player, "shield", power, range)','ArcaneLightService.illuminate(player, 1800)',
     'sleepEligible(target, power)','public static boolean dispel(LivingEntity subject)',
     'public static void onIncomingDamage(LivingIncomingDamageEvent event)',
     'DestructiveMagicService.applyPhysicalAftermath(player, "thunderwave"','public static boolean executeNpc(')

# alpha.54 second circle preserved.
second=text(magic/'SecondCircleSpellService.java')
for spell in ['scorching_ray','misty_step','web','mirror_image','invisibility',
              'gust_of_wind','hold_person','shatter','blur','levitate']:
    need(second,f'"{spell}"')
need(second,'public static final int WEB_TICKS = 220;','public static final int MIRROR_TICKS = 260;',
     'private static final int RAY_GAP = 10;','new RaySalvo(level, caster.getUUID(), target.getUUID()',
     'clearAggro(level, caster, 48.0)','event.getSource().getDirectEntity()',
     'target.getRandom().nextFloat() < .35F','stripFragileWindBlocks(level, origin, direction, length)',
     'holdEligible(target)','shatterBrittle(level, center, radius)','finishLevitation(state, true)',
     'public static boolean executeNpc(')
assert 'ReductionWard' not in second
assert 'setNoAi(true)' not in second

# alpha.55 every direct third-circle spell has one dedicated semantic runtime.
third=text(magic/'ThirdCircleSpellService.java')
for spell in ['fireball','lightning_bolt','fly','haste','dispel_magic',
              'vampiric_touch','slow','protection_from_energy','sleet_storm','blink']:
    need(third,f'"{spell}"')
need(third,
     'public static final int FLY_TICKS = 600;','public static final int HASTE_TICKS = 600;',
     'public static final int SLOW_TICKS = 180;','public static final int ENERGY_TICKS = 600;',
     'public static final int SLEET_TICKS = 180;','private static final int ENERGY_MAX_CHARGES = 5;',
     'private static final int ENERGY_RECHARGE_TICKS = 70;',
     'ArcaneBuffRuntime.apply(caster, "haste", power, range)',
     'public static boolean blocksCasting(LivingEntity caster)',
     'ArcaneDamage.isResolving()','DamageTypeTags.IS_FIRE',
     'event.setAmount((float) Math.max(0.0, event.getAmount() * .55))',
     'FirstCircleSpellService.dispel(target)','SecondCircleSpellService.clear(target)',
     'float actual = Math.max(0.0F, before - after)',
     'SLEET_ZONES.add(new SleetZone','MobEffects.DARKNESS','Set.<Relative>of()',
     'public static boolean executeNpc(')
assert 'ParticleTypes' not in third

arcane_damage=text(magic/'ArcaneDamage.java')
need(arcane_damage,'ThreadLocal<Integer> RESOLVING','public static boolean isResolving()',
     'RESOLVING.set(depth + 1)','if (depth == 0) RESOLVING.remove()')

# Dedicated ownership order: 1C -> 2C -> 3C -> special/high-circle -> generic.
kinetics=text(magic/'SpellKineticsService.java')
need(kinetics,'ThirdCircleSpellService.handles(cast.spell().id())',
     'boolean firstCircleOwned = FirstCircleSpellService.handles(spellId);',
     'boolean secondCircleOwned = !firstCircleOwned && SecondCircleSpellService.handles(spellId);',
     'boolean thirdCircleOwned = !firstCircleOwned && !secondCircleOwned && ThirdCircleSpellService.handles(spellId);',
     'ThirdCircleSpellService.execute(player, spellId, range, power, targetSnapshot)',
     'boolean planarOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && PlanarSpellService.handles(spellId);')
assert kinetics.index('boolean firstCircleOwned') < kinetics.index('boolean secondCircleOwned') < kinetics.index('boolean thirdCircleOwned') < kinetics.index('boolean planarOwned')

# Ray of Frost remains one beam, not generic channel cadence.
archetype=text(magic/'SpellArchetype.java')
channels=archetype[archetype.index('private static final Set<String> CHANNELS'):archetype.index('private static final Set<String> FIELDS')]
assert '"ray_of_frost"' not in channels

# Real LightBlocks retain shared ownership.
light=text(magic/'ArcaneLightService.java')
need(light,'Map<LightKey, Integer> REF_COUNTS','private static boolean claim(ServerLevel level, BlockPos pos)',
     'REF_COUNTS.put(key, count + 1)','if (count > 1)','REF_COUNTS.put(key, count - 1)',
     'private record LightKey(ResourceKey<Level> dimension, BlockPos pos)')

# NPCs use dedicated 1C/2C/3C paths before generic damage.
npc=text(world/'NpcSpellResolver.java')
need(npc,'FirstCircleSpellService.handles(spell.id())','SecondCircleSpellService.handles(spell.id())',
     'ThirdCircleSpellService.handles(spell.id())',
     'ThirdCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)',
     'HighWardSpellService.intercepts(caster, spell, snapshot, range)')
npc_execute=npc[npc.index('static boolean execute('):]
assert npc_execute.index('FirstCircleSpellService.handles') < npc_execute.index('SecondCircleSpellService.handles') < npc_execute.index('ThirdCircleSpellService.handles') < npc_execute.index('"meteor_swarm".equals(spell.id())')

# Preserve copy targeting and high-circle identities.
world_magic=text(magic/'WorldMagicService.java')
need(world_magic,'Optional<Mob> aimed = aimedMob(player, range, direction);',
     'case FRONT, TARGET, GROUND_TARGET -> aimed.map(Mob::getUUID).orElse(null);')
sim=text(magic/'SimulacrumService.java')
need(sim,'snapshot.targetEntity(caster).orElse(null)','Attributes.MAX_HEALTH, .50','Attributes.ATTACK_DAMAGE, .72',
     'Mode.FOLLOW','Mode.GUARD','Mode.ASSAULT')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")','Mob source = targetMob(player, snapshot);','copyCombatBody(source, clone)')
planar=text(magic/'PlanarSpellService.java')
need(planar,'Set.of("plane_shift", "demiplane")','if (vertical > .35) return Level.END','if (vertical < -.35) return Level.NETHER',
     'x /= 8.0; z /= 8.0','x *= 8.0; z *= 8.0','ROOM_HALF = 10','G키 또는 재시전으로 귀환')
control=text(magic/'HighControlSpellService.java')
need(control,'"mass_suggestion", "forcecage", "dominate_monster", "feeblemind"','MASS_SUGGESTION_TICKS = 160','FORCECAGE_TICKS = 400')
ward=text(magic/'HighWardSpellService.java')
need(ward,'GLOBE_TICKS = 520','MAX_BLOCKED_CIRCLE = 5','public static boolean intercepts(','segmentDistanceSqr')

# Central field authority owns Sleet casting denial and Antimagic clears lower-circle state.
field=text(magic/'ArcaneFieldService.java')
need(field,'SecondCircleSpellService.blocksCasting(caster)','ThirdCircleSpellService.blocksCasting(caster)',
     'FirstCircleSpellService.dispel(entity)','SecondCircleSpellService.clear(entity)','ThirdCircleSpellService.clear(entity)',
     'HighWardSpellService.clear(entity)','HighControlSpellService.clear(entity)',
     'TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')

# Hard lifecycle boundaries clear 1C/2C/3C sustained state.
need(main,'FirstCircleSpellService::onIncomingDamage','SecondCircleSpellService::onIncomingDamage',
     'ThirdCircleSpellService::onIncomingDamage','FirstCircleSpellService.tick(level)',
     'SecondCircleSpellService.tick(level)','ThirdCircleSpellService.tick(level)',
     'FirstCircleSpellService.clearAll()','SecondCircleSpellService.clearAll()','ThirdCircleSpellService.clearAll()',
     'HighUtilitySpellService.tick(level)','HighWardSpellService.tick(level)','HighControlSpellService.tick(level)','ArcaneFieldService.tick(level)')
assert main.count('FirstCircleSpellService.clear(player);') >= 3
assert main.count('SecondCircleSpellService.clear(player);') >= 3
assert main.count('ThirdCircleSpellService.clear(player);') >= 3

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.55','fireball','energy-only','casting denial','actual-damage','NPC')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'FirstCircleSpellService.class','SecondCircleSpellService.class','ThirdCircleSpellService.class',
     'HighUtilitySpellService.class','PlanarSpellService.class','SimulacrumService.class',
     'HighControlSpellService.class','HighWardSpellService.class','PrimaryGrimoireScreen.class')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('all_109_runtime_route_presence=PASS')
print('alpha52_readable_main_and_effect_compendium=PASS')
print('alpha53_first_circle_deep_runtime=PASS')
print('alpha54_second_circle_deep_runtime=PASS')
print('alpha55_third_circle_deep_runtime=PASS')
print('alpha55_third_circle_npc_parity=PASS')
print('alpha55_energy_selective_protection=PASS')
print('alpha49_54_runtime_regressions=PASS')
