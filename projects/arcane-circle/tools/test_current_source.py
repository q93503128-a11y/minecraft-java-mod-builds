from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'
world = root / 'src/main/java/kr/moonseungjun/arcanecircle/world'
network = root / 'src/main/java/kr/moonseungjun/arcanecircle/network'

def text(path):
    return path.read_text(encoding='utf-8')

def require(body, tokens, label):
    for token in tokens:
        assert token in body, f'{label}: missing {token!r}'

# Canonical version and full catalogue.
gradle = text(root / 'gradle.properties')
main = text(root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index = text(root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json')
assert 'mod_version=0.12.1-alpha.52' in gradle
assert 'VERSION = "0.12.1-alpha.52"' in main
assert '"version": "0.12.1-alpha.52"' in index

catalog = text(magic / 'SpellCatalog.java')
direct = set(re.findall(r'\badd\("([a-z0-9_]+)"', catalog))
fusions = set(re.findall(r'\baddFusion\("([a-z0-9_]+)"', catalog))
all_spells = direct | fusions
assert len(direct) == 90 and len(fusions) == 19 and len(all_spells) == 109, (len(direct), len(fusions))
assert 'IMPLEMENTED_MAX_CIRCLE = 9' in catalog and 'WORLD_MAX_CIRCLE = 9' in catalog

# Every single spell must have explicit mechanical compendium text: no generic fallback may hide omissions.
summary = text(magic / 'SpellEffectSummary.java')
summary_cases = set(re.findall(r'case "([a-z0-9_]+)"', summary))
assert summary_cases == all_spells, (
    f'missing summaries={sorted(all_spells-summary_cases)} extra summaries={sorted(summary_cases-all_spells)}'
)

# Every spell must be owned by at least one real server runtime source.
runtime_paths = [
    magic/'ExpandedSpellEffects.java',
    magic/'HighCircleSpellEffects.java',
    magic/'FusionSpellEffects.java',
    magic/'SpellGameplayService.java',
    magic/'HighUtilitySpellService.java',
    magic/'HighControlSpellService.java',
    magic/'HighWardSpellService.java',
    magic/'PlanarSpellService.java',
    magic/'SimulacrumService.java',
    magic/'ArcaneFieldService.java',
    magic/'SpellKineticsService.java',
]
runtime_union = '\n'.join(text(path) for path in runtime_paths)
missing_runtime = sorted(spell for spell in all_spells if f'"{spell}"' not in runtime_union)
assert not missing_runtime, f'spells without explicit runtime ownership: {missing_runtime}'

# Main browsing text and mechanical effects are separate surfaces.
definition = text(magic/'SpellDefinition.java')
primary = text(client/'PrimaryGrimoireScreen.java')
handlers = text(client/'ClientNetworkHandlers.java')
require(definition, [
    'public String description()',
    'return description;',
    'public String effectSummary()',
    'SpellEffectSummary.summary(this)',
], 'spell definition display split')
require(primary, [
    'public final class PrimaryGrimoireScreen extends Screen',
    'private static boolean effects;',
    '"효과 도감"',
    's.effectSummary()',
    '"세부 판정은 효과 도감에서 확인"',
    '"주문서 상점"',
    '"강점 · "',
    '"약점 · "',
    '"본거지 · "',
    'Rect academyInfo()',
], 'primary grimoire information hierarchy')
require(handlers, [
    '"atlas".equals(payload.page()) || "academy".equals(payload.page())',
    'new PrimaryGrimoireScreen(payload.page())',
    'new GrimoireScreen(payload.page())',
], 'primary grimoire routing')
assert 'SpellEffectSummary.summary(s)' not in primary

# alpha.52 copy-source targeting fix: dedicated copy runtimes require a real target, not BODY/0m self semantics.
require(definition, [
    'case "simulacrum" -> 28.0;',
    'case "clone" -> 32.0;',
    'case "simulacrum", "clone" -> SigilAnchor.TARGET;',
], 'copy source target contract')
world_magic = text(magic/'WorldMagicService.java')
require(world_magic, [
    'Optional<Mob> aimed = aimedMob(player, range, direction);',
    'case FRONT, TARGET, GROUND_TARGET -> aimed.map(Mob::getUUID).orElse(null);',
], 'snapshot target capture')
sim = text(magic/'SimulacrumService.java')
utility = text(magic/'HighUtilitySpellService.java')
require(sim, [
    'snapshot.targetEntity(caster).orElse(null)',
    'source.getType().create(level, EntitySpawnReason.EVENT)',
    'Attributes.MAX_HEALTH, .50',
    'Attributes.ATTACK_DAMAGE, .72',
    'Mode.FOLLOW', 'Mode.GUARD', 'Mode.ASSAULT',
], 'simulacrum')
require(utility, [
    'Set.of("clone", "true_polymorph", "maze", "etherealness")',
    'Mob source = targetMob(player, snapshot);',
    'source.getType().create(level, EntitySpawnReason.EVENT)',
    'copyCombatBody(source, clone)',
], 'clone/high utility')

# alpha.49 planar identities stay real and persistent.
planar = text(magic/'PlanarSpellService.java')
planar_data = text(magic/'PlanarSpellData.java')
require(planar, [
    'Set.of("plane_shift", "demiplane")',
    'if (vertical > .35) return Level.END',
    'if (vertical < -.35) return Level.NETHER',
    'return Level.OVERWORLD',
    'p.isShiftKeyDown()',
    'x /= 8.0; z /= 8.0',
    'x *= 8.0; z *= 8.0',
    'ROOM_HALF = 10',
    'ROOM_FLOOR_Y = 224',
    'Blocks.BEDROCK',
    'G키 또는 재시전으로 귀환',
], 'alpha49 planar')
require(planar_data, [
    'SavedData',
    'planar_spell_v1',
    'remember(ServerPlayer player)',
    'anchor(ServerPlayer player)',
], 'alpha49 planar data')

# alpha.50 distinct control ownership must remain.
control = text(magic/'HighControlSpellService.java')
require(control, [
    '"mass_suggestion", "forcecage", "dominate_monster", "feeblemind"',
    'MASS_SUGGESTION_TICKS = 160',
    'FORCECAGE_TICKS = 400',
    'DOMINATE_TICKS = 480',
    'FEEBLEMIND_TICKS = 700',
    'retreatDestination',
    'FORCECAGE_RADIUS = 3.1',
    'applyDomination',
    'MobEffects.MINING_FATIGUE',
], 'alpha50 high control')
assert 'setNoAi(true)' not in control

# alpha.51 Globe remains spell-vs-spell, with player/NPC parity and Antimagic cleanup.
ward = text(magic/'HighWardSpellService.java')
require(ward, [
    'GLOBE_TICKS = 520',
    'MAX_BLOCKED_CIRCLE = 5',
    'public static boolean intercepts(LivingEntity caster, SpellDefinition spell',
    'SpellMetrics.effectRadius',
    'SpellMetrics.wallWidth',
    'SpellMetrics.waveEndRadius',
    'segmentDistanceSqr',
    '6써클 이상 주문과 물리 공격은 그대로 통과',
], 'alpha51 globe')
assert 'onIncomingDamage' not in ward and 'ReductionWard' not in ward

npc = text(world/'NpcSpellResolver.java')
require(npc, [
    'HighWardSpellService.intercepts(caster, spell, snapshot, range)',
], 'NPC globe parity')

field = text(magic/'ArcaneFieldService.java')
require(field, [
    'HighWardSpellService.clear(entity)',
    'HighControlSpellService.clear(entity)',
    'TIME_STOP_TICKS = 160',
    'ANTIMAGIC_TICKS = 320',
    'FROZEN_ENTITIES',
], 'field cleanup')

# Dedicated runtime priority prevents old generic aliases from taking authority.
kinetics = text(magic/'SpellKineticsService.java')
require(kinetics, [
    'PlanarSpellService.handles(cast.spell().id())',
    'SimulacrumService.handles(cast.spell().id())',
    'HighWardSpellService.handles(cast.spell().id())',
    'HighControlSpellService.handles(cast.spell().id())',
    'boolean planarOwned = PlanarSpellService.handles(spellId)',
    'boolean simulacrumOwned = !planarOwned',
    'boolean wardOwned = !planarOwned && !simulacrumOwned && !utilityOwned',
    'HighUtilitySpellService.execute(player, spellId, range, power, targetSnapshot)',
    'HighWardSpellService.execute(player, spellId, range, power, targetSnapshot)',
    'HighControlSpellService.execute(player, spellId, range, power, targetSnapshot)',
], 'kinetic ownership')
assert kinetics.index('boolean planarOwned') < kinetics.index('boolean simulacrumOwned')
assert kinetics.index('boolean simulacrumOwned') < kinetics.index('boolean utilityOwned')
assert kinetics.index('boolean utilityOwned') < kinetics.index('boolean wardOwned')
assert kinetics.index('boolean wardOwned') < kinetics.index('boolean controlOwned')
assert kinetics.index('boolean controlOwned') < kinetics.index('boolean gameplayOwned')

# Lifecycle and visual architecture regression guard.
require(main, [
    'SimulacrumService.tick(level)',
    'HighUtilitySpellService.tick(level)',
    'HighWardSpellService.tick(level)',
    'HighControlSpellService.tick(level)',
    'ArcaneFieldService.tick(level)',
    'DestructiveMagicService.tick(level)',
    'HighWardSpellService.clearAll()',
    'HighControlSpellService.clearAll()',
], 'main lifecycle')

tracker = text(client/'WorldMagicTracker.java')
require(tracker, [
    'SpellCinematicDirector.charge',
    'ArcaneSigilDirector.charge',
    'AuthoredHighCircleTimeline.charge',
    'PersistentBuffRegalia.release',
    'MAX_FRAME = 14500',
    'MAX_VISUALS = 32',
], 'world geometry presentation')
assert '"grimoire_effect_compendium": true' in index
assert '"spell_contract_audit": "109_explicit_summaries_and_runtime_routes"' in index
assert '"copy_source_targeting": ["simulacrum_target_28","clone_target_32"]' in index

# Tool/repository hygiene and JAR verifier.
retired = [
    'CodexVisualLanguage.java','ArcaneSigilDetailGrammar.java','LowCircleVisualIdentity.java',
    'MidCircleVisualIdentity.java','FifthCircleVisualIdentity.java','SixthCircleVisualIdentity.java',
    'ArchmageVisualIdentity.java','RangeReactivePresentation.java','SpellVisualSignature.java',
    'CastingSilhouetteRenderer.java','RobeRegaliaRenderer.java','SignatureGeometry.java'
]
for name in retired:
    assert not (client/name).exists(), name
tools = root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()} == {'test_current_source.py','verify_jar.py'}
assert not [p for p in tools.iterdir() if p.is_dir()]
verify = text(root/'tools/verify_jar.py')
for entry in [
    'HighUtilitySpellService.class','PlanarSpellData.class','PlanarSpellService.class',
    'SimulacrumService.class','HighControlSpellService.class','HighWardSpellService.class'
]:
    assert entry in verify, entry

audit_doc = text(root/'SPELL_AUDIT.md')
assert audit_doc.count('| PASS | PASS |') == 109
require(audit_doc, ['`clone`','`simulacrum`','T FIXED','109 Spell Audit Queue'], 'audit queue')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('all_109_runtime_route_presence=PASS')
print('alpha52_grimoire_information_hierarchy=PASS')
print('alpha52_effect_compendium=PASS')
print('alpha52_clone_target_contract=PASS')
print('alpha52_simulacrum_target_contract=PASS')
print('alpha49_high_utility_and_planar=preserved')
print('alpha50_high_control=preserved')
print('alpha51_high_ward=preserved')
print('source_mutation=disabled')
print('legacy_arcane_tooling=absent')
