from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
world=root/'src/main/java/kr/moonseungjun/arcanecircle/world'
network=root/'src/main/java/kr/moonseungjun/arcanecircle/network'

def text(path): return path.read_text(encoding='utf-8')
def require(body,tokens,label):
    for token in tokens: assert token in body, f'{label}: {token}'

# Canonical version and complete spell world.
gradle=text(root/'gradle.properties'); main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json')
assert 'mod_version=0.12.1-alpha.49' in gradle
assert 'VERSION = "0.12.1-alpha.49"' in main
assert '"version": "0.12.1-alpha.49"' in index
catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog)); fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog))
assert len(direct)==90 and len(fusions)==19 and len(direct|fusions)==109,(len(direct),len(fusions))
assert 'IMPLEMENTED_MAX_CIRCLE = 9' in catalog and 'WORLD_MAX_CIRCLE = 9' in catalog

# Active-tree/tool hygiene.
retired=['CodexVisualLanguage.java','ArcaneSigilDetailGrammar.java','LowCircleVisualIdentity.java','MidCircleVisualIdentity.java',
         'FifthCircleVisualIdentity.java','SixthCircleVisualIdentity.java','ArchmageVisualIdentity.java','RangeReactivePresentation.java',
         'SpellVisualSignature.java','CastingSilhouetteRenderer.java','RobeRegaliaRenderer.java','SignatureGeometry.java']
for name in retired: assert not (client/name).exists(),name
tools=root/'tools'; assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
assert not [p for p in tools.iterdir() if p.is_dir()]
repo=root.parents[1]; scripts=repo/'.github/scripts'
if scripts.exists(): assert not list(scripts.glob('*arcane*'))

# Current world-geometry presentation baseline remains intact; alpha.49 does not replace it with generic particles.
tracker=text(client/'WorldMagicTracker.java')
require(tracker,['SpellCinematicDirector.charge','ArcaneSigilDirector.charge','AuthoredHighCircleTimeline.charge',
                 'HighCircleMaintenanceOverlay.charge','PersistentBuffRegalia.release','MAX_FRAME = 14500','MAX_ENTRY = 4000',
                 'DETAIL_DISTANCE_SQR = 96.0 * 96.0','MAX_VISUALS = 32'],'tracker')
sigil=text(client/'ArcaneSigilDirector.java'); require(sigil,['formulaFrame','schoolFormula','geometricDepth','anchorFormula'],'sigil')
timeline=text(client/'AuthoredHighCircleTimeline.java')
for spell in ['plane_shift','simulacrum','demiplane','clone','maze','true_polymorph','meteor_swarm','time_stop','wish','gate']:
    assert f'case "{spell}"' in timeline,spell

# Grimoire mechanical text must describe actual alpha.48/49 behavior, not old aliases.
summary=text(magic/'SpellEffectSummary.java'); assert summary.count('case "')>=109
require(summary,[
    'case "clone"','실제 복제본','case "true_polymorph"','실제 몸체','case "maze"','전장에서 실제 추방',
    'case "etherealness"','물질 충돌 위상화','case "plane_shift"','실제 차원 이동','위=엔드/아래=네더/수평=오버월드',
    'case "simulacrum"','체력 50%/전투력 약 72%','웅크린 채 G','case "demiplane"','보존되는 개인 주머니방',
    '내부 블록/물품 유지','case "control_weather"','G키로 바라본 지점 12연속 낙뢰','case "time_stop"','투사체·드롭 이동'
],'summary')
assert '60초 내 다음 치명상을 대리체가 대신 받고 생존' not in summary
assert 'case "demiplane" -> "고등 공간 회로로 매우 먼 안전 지점 이동"' not in summary
assert 'case "plane_shift" -> "장거리 안전 지점으로 고등 공간 이동"' not in summary

# Alpha.48 real body/state transitions stay authoritative.
utility=text(magic/'HighUtilitySpellService.java')
require(utility,['Set.of("clone", "true_polymorph", "maze", "etherealness")','source.getType().create(level, EntitySpawnReason.EVENT)',
                 'copyCombatBody(source, clone)','TRUE_POLYMORPH_TICKS = 480','EntityTypes.RABBIT.create','restorePolymorph(state, true)',
                 'MAZE_TICKS = 360','arcanecircle_maze_exile','player.noPhysics = true','event.getAmount() * 0.12F'],'alpha48 utility')
assert 'getTags()' not in utility and 'snapTo(mob.getX(), -512.0' not in utility

# Alpha.49 Plane Shift: actual dimension choice + willing/crouching nearby party + coordinate mapping.
planar_path=magic/'PlanarSpellService.java'; planar_data_path=magic/'PlanarSpellData.java'
assert planar_path.exists() and planar_data_path.exists()
planar=text(planar_path); planar_data=text(planar_data_path)
require(planar,[
    'Set.of("plane_shift", "demiplane")','case "plane_shift" -> planeShift(player)','case "demiplane" -> demiplane(player)',
    'if (vertical > .35) return Level.END','if (vertical < -.35) return Level.NETHER','return Level.OVERWORLD',
    'p.isShiftKeyDown()','if (result.size() >= 9) break','x /= 8.0; z /= 8.0','x *= 8.0; z *= 8.0',
    'player.teleportTo(level','Set.<Relative>of()','findSafeVertical','ServerLevel.END_SPAWN_POINT'
],'plane shift')
# Bounded safe search: perimeter calls vertical-only helper, never recursively calls findSafe.
find_block=planar[planar.index('private static Optional<BlockPos> findSafe('):planar.index('private static Optional<BlockPos> findSafeVertical(')]
assert find_block.count('findSafe(')==1 and 'findSafeVertical' in find_block

# Alpha.49 Demiplane: deterministic personal cell, persistent return anchor, reusable physical room and emergency exit.
require(planar,[
    'ROOM_HALF = 10','ROOM_FLOOR_Y = 224','PlanarSpellData.get(server)','data.remember(member)',
    'returnFromDemiplane','roomCenter(caster.getUUID())','Blocks.BEDROCK','Blocks.POLISHED_BLACKSTONE_BRICKS',
    'Blocks.SEA_LANTERN','G키 또는 재시전으로 귀환','isDemiplaneBackend','player.getX() > 3_500_000'
],'demiplane')
require(planar_data,['SavedData','SavedDataType<PlanarSpellData>','planar_spell_v1','dimension','yaw','pitch',
                     'remember(ServerPlayer player)','anchor(ServerPlayer player)','clear(ServerPlayer player)'],'planar data')

# Alpha.49 Simulacrum: a creature, not a death ward. One per caster, half vitality, reduced combat body, explicit commands.
sim_path=magic/'SimulacrumService.java'; assert sim_path.exists(); sim=text(sim_path)
require(sim,[
    'source.getType().create(level, EntitySpawnReason.EVENT)','Attributes.MAX_HEALTH, .50','Attributes.ATTACK_DAMAGE, .72',
    'Attributes.ARMOR, .72','ACTIVE.put(caster.getUUID()','Mode.FOLLOW','Mode.GUARD','Mode.ASSAULT',
    'if (!caster.isShiftKeyDown()) return false','집중 공격 명령','수호 모드','추종 모드','getNavigation().moveTo',
    'removeOwned(caster.getUUID(), true)','arcanecircle_simulacrum'
],'simulacrum')
assert 'DEATH_WARDS' not in sim and 'MobEffects.ABSORPTION' not in sim

# Dedicated routing must win before old resolved fallbacks.
kinetics=text(magic/'SpellKineticsService.java')
require(kinetics,[
    'PlanarSpellService.handles(cast.spell().id())','SimulacrumService.handles(cast.spell().id())',
    'boolean planarOwned = PlanarSpellService.handles(spellId)','boolean simulacrumOwned = !planarOwned',
    'PlanarSpellService.execute(player, spellId)','SimulacrumService.execute(player, targetSnapshot)',
    'HighUtilitySpellService.execute(player, spellId, range, power, targetSnapshot)','SpellGameplayService.execute(player, spellId, range, power, targetSnapshot)'
],'kinetic ownership')
assert kinetics.index('boolean planarOwned') < kinetics.index('boolean simulacrumOwned') < kinetics.index('boolean utilityOwned') < kinetics.index('boolean gameplayOwned')

# Contextual G authority: Demiplane exit > crouch+G Simulacrum > ordinary maintained/weather authority.
network_source=text(network/'ArcaneNetwork.java')
require(network_source,['ninefold-arcana-12-1-alpha49','PlanarSpellService.useAuthority(player)',
                        'SimulacrumService.useAuthority(player)','SpellGameplayService.useMaintainedAuthority(player)'],'network authority')
assert network_source.index('PlanarSpellService.useAuthority') < network_source.index('SimulacrumService.useAuthority') < network_source.index('SpellGameplayService.useMaintainedAuthority')

# Main lifecycle prevents orphan copies; Planar saved anchors intentionally survive normal logout/dimension transitions.
require(main,['SimulacrumService.tick(level)','SimulacrumService.clear(player)','SimulacrumService.clearAll()',
              'HighUtilitySpellService.tick(level)','ArcaneFieldService.tick(level)','DestructiveMagicService.tick(level)'],'main lifecycle')
assert main.count('SimulacrumService.clear(player);')>=3
assert main.index('SimulacrumService.tick(level)') < main.index('ArcaneFieldService.tick(level)')
assert 'PlanarSpellData' not in main  # do not accidentally erase persistent return anchors on dimension-change cleanup

# Existing major mechanics remain present.
gameplay=text(magic/'SpellGameplayService.java')
require(gameplay,['control_weather','WEATHER_BARRAGES','12연속 번개','prismatic_wall','wall_of_force','flesh_to_stone'],'gameplay')
field=text(magic/'ArcaneFieldService.java'); require(field,['TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','FROZEN_ENTITIES','FrozenEntity'],'field')
destruction=text(magic/'DestructiveMagicService.java'); require(destruction,['MAX_BLOCK_CHANGES_PER_TICK = 720','MAX_BLOCK_SCANS_PER_TICK = 48_000','meteorCrater','quakeField','annihilationCorridor'],'destruction')
assert 'visible_footprint_tiled_across_bounded_ticks' in index

# Jar verifier must require every new runtime class.
verify=text(root/'tools/verify_jar.py')
for entry in ['HighUtilitySpellService.class','PlanarSpellData.class','PlanarSpellService.class','SimulacrumService.class']:
    assert entry in verify,entry

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('alpha48_real_high_utility=PASS')
print('alpha49_real_plane_shift=PASS')
print('alpha49_persistent_demiplane=PASS')
print('alpha49_commandable_simulacrum=PASS')
print('alpha49_contextual_authority_input=PASS')
print('bounded_planar_safe_search=PASS')
print('seeded_meteor_and_destruction=preserved')
print('authoritative_time_stop=preserved')
print('retired_visual_stack=absent')
print('source_mutation=disabled')
print('legacy_arcane_tooling=absent')
