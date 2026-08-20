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
need(gradle,'mod_version=0.12.1-alpha.52')
need(main,'VERSION = "0.12.1-alpha.52"')
need(index,'"version": "0.12.1-alpha.52"','"grimoire_effect_compendium": true',
     '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"',
     '"copy_source_targeting": ["simulacrum_target_28","clone_target_32"]')

catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog))
fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog))
spells=direct|fusions
assert (len(direct),len(fusions),len(spells))==(90,19,109)
need(catalog,'IMPLEMENTED_MAX_CIRCLE = 9','WORLD_MAX_CIRCLE = 9')

summary=text(magic/'SpellEffectSummary.java')
summary_ids=set(re.findall(r'case "([a-z0-9_]+)"',summary))
assert summary_ids==spells,(sorted(spells-summary_ids),sorted(summary_ids-spells))

runtime_files=[
 'ExpandedSpellEffects.java','HighCircleSpellEffects.java','FusionSpellEffects.java',
 'SpellGameplayService.java','HighUtilitySpellService.java','HighControlSpellService.java',
 'HighWardSpellService.java','PlanarSpellService.java','SimulacrumService.java',
 'ArcaneFieldService.java','SpellKineticsService.java','SpellCastingService.java']
runtime='\n'.join(text(magic/name) for name in runtime_files)
missing=sorted(x for x in spells if f'"{x}"' not in runtime)
assert not missing,f'no runtime route: {missing}'

# alpha.52 UI: main information stays readable; mechanical detail has its own compendium.
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

# Target snapshot must now carry the aimed creature for clone/simulacrum.
world_magic=text(magic/'WorldMagicService.java')
need(world_magic,'Optional<Mob> aimed = aimedMob(player, range, direction);',
     'case FRONT, TARGET, GROUND_TARGET -> aimed.map(Mob::getUUID).orElse(null);')
sim=text(magic/'SimulacrumService.java')
need(sim,'snapshot.targetEntity(caster).orElse(null)','Attributes.MAX_HEALTH, .50',
     'Attributes.ATTACK_DAMAGE, .72','Mode.FOLLOW','Mode.GUARD','Mode.ASSAULT')
utility=text(magic/'HighUtilitySpellService.java')
need(utility,'Set.of("clone", "true_polymorph", "maze", "etherealness")',
     'Mob source = targetMob(player, snapshot);','copyCombatBody(source, clone)')

# Preserve alpha.49-51 authoritative identities while alpha.52 changes UI/targeting.
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
npc=text(world/'NpcSpellResolver.java')
need(npc,'HighWardSpellService.intercepts(caster, spell, snapshot, range)')
field=text(magic/'ArcaneFieldService.java')
need(field,'HighWardSpellService.clear(entity)','HighControlSpellService.clear(entity)',
     'TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES')

kinetics=text(magic/'SpellKineticsService.java')
for token in ['PlanarSpellService.handles','SimulacrumService.handles','HighUtilitySpellService.handles',
              'HighWardSpellService.handles','HighControlSpellService.handles','SpellGameplayService.handles']:
    need(kinetics,token)
need(main,'SimulacrumService.tick(level)','HighUtilitySpellService.tick(level)',
     'HighWardSpellService.tick(level)','HighControlSpellService.tick(level)','ArcaneFieldService.tick(level)')

# Audit queue and package guards.
audit=text(root/'SPELL_AUDIT.md')
assert audit.count('| PASS | PASS |')==109
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
verify=text(tools/'verify_jar.py')
need(verify,'HighUtilitySpellService.class','PlanarSpellService.class','SimulacrumService.class',
     'HighControlSpellService.class','HighWardSpellService.class','PrimaryGrimoireScreen.class')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('all_109_runtime_route_presence=PASS')
print('alpha52_readable_main_and_effect_compendium=PASS')
print('alpha52_clone_and_simulacrum_targeting=PASS')
print('alpha49_51_runtime_regressions=PASS')
