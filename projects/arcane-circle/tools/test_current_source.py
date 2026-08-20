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
need(gradle,'mod_version=0.12.1-alpha.56')
need(main,'VERSION = "0.12.1-alpha.56"')
need(index,'"version": "0.12.1-alpha.56"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"copy_source_targeting": ["simulacrum_target_28","clone_target_32"]',
     '"first_circle_npc_parity": true','"second_circle_npc_parity": true',
     '"third_circle_npc_parity": true','"fourth_circle_npc_parity": true',
     '"falloff_fireball_blast"','"energy_only_recharging_ward"','"casting_break_sleet_storm"',
     '"persistent_fire_wall"','"five_pulse_ice_storm"','"two_way_resilient_sphere"',
     '"companion_dimension_door"','"physical_only_stoneskin"','"decision_scramble_confusion"',
     '"anti_heal_blight"','"movement_control_freedom"','"forced_flee_phantasmal_killer"')

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
     'case "protection_from_energy" -> "30초 · 5중 공명막이 Arcane/화염/투사체성 충격만 45% 경감 · 3.5초마다 재충전"',
     'case "resilient_sphere" -> "20초 완전 격리막 · 안팎의 피해 모두 차단 · 내부 Arcane 시전 불가"',
     'case "dimension_door" -> "최대 약 36m 안전 공간 이동 · 3m 내 웅크린 플레이어 1명 동행 가능"',
     'case "stoneskin" -> "38초 · 적이 가하는 비마법 물리 공격만 50% 경감 · 화염/Arcane/환경 피해는 통과"',
     'case "blight" -> "단일 생명 쇠퇴 · 8초 추가 흡수 피해 + 받는 치유량 80% 감소"',
     'case "phantasmal_killer" -> "11초 단일 공포 환상 · 대상이 시전자에게서 실제로 도주하며 주기적 정신 피해"')

runtime_files=[
 'FirstCircleSpellService.java','SecondCircleSpellService.java','ThirdCircleSpellService.java',
 'FourthCircleSpellService.java','ExpandedSpellEffects.java','HighCircleSpellEffects.java',
 'FusionSpellEffects.java','SpellGameplayService.java','HighUtilitySpellService.java',
 'HighControlSpellService.java','HighWardSpellService.java','PlanarSpellService.java',
 'SimulacrumService.java','ArcaneFieldService.java','SpellKineticsService.java','SpellCastingService.java']
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

# alpha.55 third circle preserved.
third=text(magic/'ThirdCircleSpellService.java')
for spell in ['fireball','lightning_bolt','fly','haste','dispel_magic',
              'vampiric_touch','slow','protection_from_energy','sleet_storm','blink']:
    need(third,f'"{spell}"')
need(third,'public static final int FLY_TICKS = 600;','public static final int HASTE_TICKS = 600;',
     'public static final int SLOW_TICKS = 180;','public static final int ENERGY_TICKS = 600;',
     'public static final int SLEET_TICKS = 180;','private static final int ENERGY_MAX_CHARGES = 5;',
     'private static final int ENERGY_RECHARGE_TICKS = 70;',
     'ArcaneBuffRuntime.apply(caster, "haste", power, range)',
     'public static boolean blocksCasting(LivingEntity caster)','ArcaneDamage.isResolving()',
     'DamageTypeTags.IS_FIRE','event.setAmount((float) Math.max(0.0, event.getAmount() * .55))',
     'float actual = Math.max(0.0F, before - after)','SLEET_ZONES.add(new SleetZone',
     'MobEffects.DARKNESS','Set.<Relative>of()','public static boolean executeNpc(')
assert 'ParticleTypes' not in third

arcane_damage=text(magic/'ArcaneDamage.java')
need(arcane_damage,'ThreadLocal<Integer> RESOLVING','public static boolean isResolving()',
     'RESOLVING.set(depth + 1)','if (depth == 0) RESOLVING.remove()')

# alpha.56: all ten 4C spells are dedicated semantic mechanics, not legacy aliases.
fourth=text(magic/'FourthCircleSpellService.java')
for spell in ['wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door',
              'stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer']:
    need(fourth,f'"{spell}"')
need(fourth,
     'public static final int WALL_TICKS = 240;','public static final int ICE_STORM_PULSES = 5;',
     'public static final int GREATER_INVISIBILITY_TICKS = 780;','public static final int SPHERE_TICKS = 400;',
     'public static final int STONESKIN_TICKS = 760;','public static final int CONFUSION_TICKS = 240;',
     'public static final int BLIGHT_TICKS = 160;','public static final int FREEDOM_TICKS = 520;',
     'public static final int PHANTASM_TICKS = 220;',
     'FIRE_WALLS.add(new FireWall','ICE_STORMS.add(new IceStorm',
     'target.getRandom().nextFloat() < .45F','event.setAmount(Math.max(.05F, event.getAmount() * .50F))',
     'event.setAmount(event.getAmount() * .20F)','randomConfusionTarget(level, mob)',
     'mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35)',
     'public static boolean hasFreedom(LivingEntity target)','public static boolean blocksCasting(LivingEntity caster)',
     'public static boolean executeNpc(')
assert 'setNoAi(true)' not in fourth
assert 'ParticleTypes' not in fourth

# Dedicated ownership order: 1C -> 2C -> 3C -> 4C -> special/high-circle -> generic.
kinetics=text(magic/'SpellKineticsService.java')
need(kinetics,'FourthCircleSpellService.handles(cast.spell().id())',
     'boolean firstCircleOwned = FirstCircleSpellService.handles(spellId);',
     'boolean secondCircleOwned = !firstCircleOwned && SecondCircleSpellService.handles(spellId);',
     'boolean thirdCircleOwned = !firstCircleOwned && !secondCircleOwned && ThirdCircleSpellService.handles(spellId);',
     'boolean fourthCircleOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned',
     'FourthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)',
     '&& PlanarSpellService.handles(spellId);')
assert kinetics.index('boolean firstCircleOwned') < kinetics.index('boolean secondCircleOwned') \
       < kinetics.index('boolean thirdCircleOwned') < kinetics.index('boolean fourthCircleOwned') \
       < kinetics.index('boolean planarOwned')

# Ray of Frost remains one beam, not generic channel cadence.
archetype=text(magic/'SpellArchetype.java')
channels=archetype[archetype.index('private static final Set<String> CHANNELS'):archetype.index('private static final Set<String> FIELDS')]
assert '"ray_of_frost"' not in channels

# Real LightBlocks retain shared ownership.
light=text(magic/'ArcaneLightService.java')
need(light,'Map<LightKey, Integer> REF_COUNTS','private static boolean claim(ServerLevel level, BlockPos pos)',
     'REF_COUNTS.put(key, count + 1)','if (count > 1)','REF_COUNTS.put(key, count - 1)',
     'private record LightKey(ResourceKey<Level> dimension, BlockPos pos)')

# NPCs use dedicated 1C/2C/3C/4C paths before generic damage.
npc=text(world/'NpcSpellResolver.java')
need(npc,'FirstCircleSpellService.handles(spell.id())','SecondCircleSpellService.handles(spell.id())',
     'ThirdCircleSpellService.handles(spell.id())','FourthCircleSpellService.handles(spell.id())',
     'FourthCircleSpellService.executeNpc(level, caster, target, spell, range, power, snapshot)',
     'HighWardSpellService.intercepts(caster, spell, snapshot, range)')
npc_execute=npc[npc.index('static boolean execute('):]
assert npc_execute.index('FirstCircleSpellService.handles') < npc_execute.index('SecondCircleSpellService.handles') \
       < npc_execute.index('ThirdCircleSpellService.handles') < npc_execute.index('FourthCircleSpellService.handles') \
       < npc_execute.index('"meteor_swarm".equals(spell.id())')

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

# Central field authority: Freedom bypasses lower movement locks only; 4C isolation/confusion,
# Antimagic, Time Stop and high-circle control still win. Antimagic clears 1C-4C state.
field=text(magic/'ArcaneFieldService.java')
need(field,'boolean movementFree = FourthCircleSpellService.hasFreedom(caster);',
     'if (!movementFree && SecondCircleSpellService.blocksCasting(caster)) return true;',
     'if (!movementFree && ThirdCircleSpellService.blocksCasting(caster)) return true;',
     'if (FourthCircleSpellService.blocksCasting(caster)) return true;',
     'HighControlSpellService.blocksCasting(caster)',
     'FirstCircleSpellService.dispel(entity)','SecondCircleSpellService.clear(entity)',
     'ThirdCircleSpellService.clear(entity)','FourthCircleSpellService.clear(entity)',
     'HighWardSpellService.clear(entity)','HighControlSpellService.clear(entity)',
     'TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')

# Hard lifecycle boundaries clear 1C/2C/3C/4C sustained state and wire damage/heal hooks.
need(main,'FirstCircleSpellService::onIncomingDamage','SecondCircleSpellService::onIncomingDamage',
     'ThirdCircleSpellService::onIncomingDamage','FourthCircleSpellService::onIncomingDamage',
     'FourthCircleSpellService::onHeal','FirstCircleSpellService.tick(level)','SecondCircleSpellService.tick(level)',
     'ThirdCircleSpellService.tick(level)','FourthCircleSpellService.tick(level)',
     'FirstCircleSpellService.clearAll()','SecondCircleSpellService.clearAll()',
     'ThirdCircleSpellService.clearAll()','FourthCircleSpellService.clearAll()',
     'HighUtilitySpellService.tick(level)','HighWardSpellService.tick(level)',
     'HighControlSpellService.tick(level)','ArcaneFieldService.tick(level)')
assert main.count('FirstCircleSpellService.clear(player);') >= 3
assert main.count('SecondCircleSpellService.clear(player);') >= 3
assert main.count('ThirdCircleSpellService.clear(player);') >= 3
assert main.count('FourthCircleSpellService.clear(player);') >= 3

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
need(audit,'alpha.56','two-way isolation','physical-only','anti-heal','forced flee','NPC')
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'FirstCircleSpellService.class','SecondCircleSpellService.class','ThirdCircleSpellService.class',
     'FourthCircleSpellService.class','HighUtilitySpellService.class','PlanarSpellService.class',
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
print('alpha56_fourth_circle_npc_parity=PASS')
print('alpha56_two_way_sphere_and_physical_stoneskin=PASS')
print('alpha49_55_runtime_regressions=PASS')
