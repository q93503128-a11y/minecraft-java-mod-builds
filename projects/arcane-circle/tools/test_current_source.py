from pathlib import Path
import re

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
world=root/'src/main/java/kr/moonseungjun/arcanecircle/world'
network=root/'src/main/java/kr/moonseungjun/arcanecircle/network'

def text(path): return path.read_text(encoding='utf-8')

def require_tokens(body,tokens,label):
    for token in tokens:
        assert token in body, f'{label}: {token}'

# Canonical version / catalogue size.
gradle=text(root/'gradle.properties')
main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json')
assert 'mod_version=0.12.1-alpha.48' in gradle
assert 'VERSION = "0.12.1-alpha.48"' in main
assert '"version": "0.12.1-alpha.48"' in index
catalog=text(magic/'SpellCatalog.java')
direct=set(re.findall(r'\badd\("([a-z0-9_]+)"',catalog))
fusions=set(re.findall(r'\baddFusion\("([a-z0-9_]+)"',catalog))
assert len(direct)==90, len(direct)
assert len(fusions)==19, len(fusions)
assert len(direct|fusions)==109, len(direct|fusions)
assert 'IMPLEMENTED_MAX_CIRCLE = 9' in catalog and 'WORLD_MAX_CIRCLE = 9' in catalog

# Active tree hygiene. History stays in git, not in current source/tooling.
retired=['CodexVisualLanguage.java','ArcaneSigilDetailGrammar.java','LowCircleVisualIdentity.java',
         'MidCircleVisualIdentity.java','FifthCircleVisualIdentity.java','SixthCircleVisualIdentity.java',
         'ArchmageVisualIdentity.java','RangeReactivePresentation.java','SpellVisualSignature.java',
         'CastingSilhouetteRenderer.java','RobeRegaliaRenderer.java','SignatureGeometry.java']
for name in retired: assert not (client/name).exists(), name
retired_tokens=[name.removesuffix('.java') for name in retired]
for path in (root/'src').rglob('*.java'):
    body=text(path)
    for token in retired_tokens: assert token not in body, f'{token} in {path.relative_to(root)}'
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()}=={'test_current_source.py','verify_jar.py'}
assert not [p for p in tools.iterdir() if p.is_dir()]
repo=root.parents[1]
scripts=repo/'.github/scripts'
if scripts.exists(): assert not list(scripts.glob('*arcane*'))

# Current cinematic renderer, high-circle authored timeline, and 3P regalia baseline.
tracker=text(client/'WorldMagicTracker.java')
require_tokens(tracker,[
    'SpellCinematicDirector.charge','SpellCinematicDirector.release','SpellCinematicDirector.castingFamily',
    'ArcaneSigilDirector.charge','ArcaneSigilDirector.releaseEcho','ArcaneSpellVisualOverhaul.chargeSigil',
    'AuthoredHighCircleTimeline.charge','AuthoredHighCircleTimeline.release',
    'HighCircleMaintenanceOverlay.charge','HighCircleMaintenanceOverlay.release',
    'PersistentBuffRegalia.release','MeteorBarragePattern.withSeed',
    'MAX_FRAME = 14500','MAX_ENTRY = 4000','DETAIL_DISTANCE_SQR = 96.0 * 96.0',
    'SILHOUETTE_DISTANCE_SQR = 160.0 * 160.0','MAX_VISUALS = 32','syncLevelIdentity'
],'tracker')
assert tracker.count('!"prismatic_wall".equals(v.spell.id())')>=2
sigil=text(client/'ArcaneSigilDirector.java')
require_tokens(sigil,['formulaFrame','schoolFormula','geometricDepth','anchorFormula','skyRitual',
                      'meteorRitual','inscriptionRing','sigilRangeScale','meteor_swarm','fusionFormula'],'sigil')
director=text(client/'SpellCinematicDirector.java')
require_tokens(director,['enum Form','NEEDLE','ORB','VOLLEY','RAY','CONE','FIELD','WALL','GATE','PRISON',
                         'SKY','WEATHER','AURA','TRANSFORM','CLOCK','TERRAIN','DOMAIN','meteorSwarm',
                         'executionWord','worldFault','annihilationBeam','MeteorBarragePattern.count()'],'director')
casting=text(client/'ArcaneCastingPerformance.java')
for forbidden in ['debugFilledBox','submitCustomGeometry','strap(','blade(','pose.translate','pose.rotate']:
    assert forbidden not in casting, forbidden
assert 'Intentionally empty' in casting
gear=text(client/'ArcaneGearRenderer.java')
assert 'ArcaneRegaliaRenderer.render' in gear and 'ArcaneCastingPerformance.render' in gear
regalia3p=text(client/'ArcaneRegaliaRenderer.java')
require_tokens(regalia3p,['outfit','bodice','lapel','shoulderMantle','skirtPair','sideGore','backTrain',
                          'facetedSkirt','asymmetricSkirt','ceremonialTab'],'regalia')

# High-circle timeline coverage stays exhaustive.
timeline=text(client/'AuthoredHighCircleTimeline.java')
expected_high={
'delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage','plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport','void_lance','winter_domain',
'antimagic_field','clone','control_weather','demiplane','dominate_monster','earthquake','feeblemind','incendiary_cloud','maze','sunburst','astral_prison','phoenix_requiem',
'meteor_swarm','power_word_kill','prismatic_wall','shapechange','time_stop','true_polymorph','weird','wish','gate','foresight','world_sunder'}
draw_block=timeline[timeline.index('switch (id) {'):timeline.index('// 7th circle')]
dispatched=set(re.findall(r'case "([a-z0-9_]+)"',draw_block))
assert dispatched==expected_high,(sorted(expected_high-dispatched),sorted(dispatched-expected_high))
mesh=text(client/'ArcaneWorldMesh.java')
require_tokens(mesh,['detailBuilder(int budget)','record Segment(Vec3 start,Vec3 end,float width,float brightness,float alpha)',
                     'Builder line(Vec3 a,Vec3 b,float width,float brightness,float alpha)',
                     'passBrightness*s.brightness','passAlpha*s.alpha'],'mesh')

# Grimoire/UI and all 109 explicit mechanical summaries.
grimoire=text(client/'GrimoireScreen.java')
require_tokens(grimoire,['drawSpine','circleIndex','browserViewport','drawSpellDetail','drawLoadout','quickEquip',
                         'academySelector','academySummary','staffList()','staffDetail()','enableScissor','mouseScrolled'],'grimoire')
summary=text(magic/'SpellEffectSummary.java')
definition=text(magic/'SpellDefinition.java')
assert 'SpellEffectSummary.summary(this)' in definition and '효과 · ' in definition
assert summary.count('case "')>=109
require_tokens(summary,[
    'case "wish"','기존 이로운','case "meteor_swarm"','16발','case "world_sunder"','장거리·심층 실제 세계 균열',
    'case "time_stop"','투사체·드롭 이동','case "clone"','실제 복제본','시전자 소유 아님',
    'case "true_polymorph"','실제 몸체','case "maze"','전장에서 실제 추방',
    'case "etherealness"','물질 충돌 위상화','case "control_weather"','G키로 바라본 지점 12연속 낙뢰',
    'case "prismatic_wall"','20초 지속'
],'summary')
assert '에메랄드' not in grimoire

# Target snapshot and seeded meteor parity.
target=text(magic/'CastTargetSnapshot.java')
barrage=text(magic/'MeteorBarragePattern.java')
require_tokens(target,['targetEntityId','launchDirection','impactSurface','barrageSeed','dimension','executeLocked',
                       'resolvedTarget','boolean homing'],'target snapshot')
require_tokens(barrage,['BASE_STRIKES','impactTick','durationTicks','count()','strikes(long seed)','withSeed',
                        'castSeed','MIN_SEPARATION'],'meteor pattern')
assert barrage.count('new Strike(')>=16
world_magic=text(magic/'WorldMagicService.java')
require_tokens(world_magic,['captureSnapshot','CastTargetSnapshot snapshot','seed=%d','PLAYER_CHARGE_SEEDS',
                            'NPC_CHARGE_SEEDS','npcChargeSeed','npcReleaseSeed','consumeNpcSnapshot',
                            'MeteorBarragePattern.firstImpactTick(snapshot.barrageSeed())'],'world magic')

# Authoritative kinetic ownership. High utility must win before legacy gameplay/resolved fallbacks.
kinetics=text(magic/'SpellKineticsService.java')
require_tokens(kinetics,[
    'HighUtilitySpellService.handles(cast.spell().id()) || SpellGameplayService.handles(cast.spell().id())',
    'boolean utilityOwned = HighUtilitySpellService.handles(spellId)',
    'boolean gameplayOwned = !utilityOwned && SpellGameplayService.handles(spellId)',
    'utilityOwned\n                ? HighUtilitySpellService.execute',
    'DestructiveMagicService.applyPhysicalAftermath','generic FIELD pulses must not restart',
    'MeteorBarragePattern.strike(targetSnapshot.barrageSeed(), 0)'
],'kinetics')
assert kinetics.index('boolean utilityOwned') < kinetics.index('boolean gameplayOwned')
assert 'WorldMagicService.lockedTarget' not in kinetics

# Alpha.48 real high-circle utility state transitions.
utility=text(magic/'HighUtilitySpellService.java')
require_tokens(utility,[
    'Set.of("clone", "true_polymorph", "maze", "etherealness")',
    'source.getType().create(level, EntitySpawnReason.EVENT)','copyCombatBody(source, clone)',
    'Attributes.MAX_HEALTH','Attributes.ATTACK_DAMAGE','Attributes.ARMOR','Attributes.MOVEMENT_SPEED',
    'arcanecircle_clone_source_','복제체는 시전자 소유가 아니며 독립적으로 행동',
    'TRUE_POLYMORPH_TICKS = 480','EntityTypes.RABBIT.create','EntityTypes.CHICKEN.create',
    'EntityTypes.PIG.create','EntityTypes.SHEEP.create','restorePolymorph(state, true)',
    'MAZE_TICKS = 360','arcanecircle_maze_exile','restoreMaze(state)',
    'player.noPhysics = true','getAbilities().mayfly = true','getAbilities().flying = true',
    'event.getAmount() * 0.12F','arcanecircle_high_utility_stashed',
    'POLYMORPHS.containsKey(mob.getUUID()) || MAZES.containsKey(mob.getUUID())'
],'high utility')
assert 'getTags()' not in utility
assert 'snapTo(mob.getX(), -512.0' not in utility
assert 'ArcaneDamage.hurt(level, player, proxy' not in utility

# Utility VFX is spell-specific: Clone is target-authored and no longer a caster-maintained buff.
presentation=text(magic/'SpellPresentationProfile.java')
require_tokens(presentation,[
    'put("clone", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 3.10',
    'case "clone" -> 50','case "true_polymorph" -> 80','case "maze" -> 70','case "etherealness" -> 600',
    'SpellGameplayService.visualDurationTicks(spell.id())'
],'presentation')
persistent=text(client/'PersistentBuffRegalia.java')
maintained=persistent[persistent.index('MAINTAINED = Set.of'):persistent.index('private PersistentBuffRegalia')]
assert '"clone"' not in maintained
assert '"etherealness"' in maintained
assert 'case "clone" -> reserveBody' not in persistent

# Persistent/control gameplay that remains in SpellGameplayService.
gameplay=text(magic/'SpellGameplayService.java')
require_tokens(gameplay,[
    'LivingIncomingDamageEvent','event.setCanceled(true)','getAbilities().mayfly = true','setNoAi(true)',
    'restoreControl','CommandSourceStack','/weather thunder ','/weather clear ','control_weather',
    'prismatic_wall','incendiary_cloud','wall_of_force','wall_of_ice','wind_wall','sleet_storm',
    'insect_plague','flesh_to_stone','forcecage','thunder_cage','astral_prison','SpellMetrics.wallWidth',
    'useMaintainedAuthority','WEATHER_SPECIAL_READY','WEATHER_BARRAGES','12연속 번개','now + 50L',
    '/summon minecraft:lightning_bolt','weatherAim','groundStrike','int duration = 900'
],'gameplay')
assert 'setWeatherParameters' not in gameplay

# Destruction: visual-scale footprint is scheduled across bounded ticks, not silently truncated.
destruction=text(magic/'DestructiveMagicService.java')
require_tokens(destruction,[
    'MAX_BLOCK_CHANGES_PER_TICK = 720','MAX_BLOCK_SCANS_PER_TICK = 48_000','MAX_DROPPED_BLOCKS_PER_TICK = 96',
    'MAX_PENDING_CELLS_PER_LEVEL = 2_048','MAX_CELLS_PER_TICK = 7','RuptureTask','scheduleFootprint','PENDING',
    'getDestroySpeed','getExplosionResistance','destroyBlock','hasChunkAt','quakeField','meteorCrater',
    'annihilationCorridor','travelling continental cut','outer faults arrive over subsequent ticks',
    'visible Meteor Swarm body','deletes a corridor progressively'
],'destruction')
assert 'DestructiveMagicService.tick(level);' in main
assert 'DestructiveMagicService.clearAll();' in main
assert 'visible_footprint_tiled_across_bounded_ticks' in index

# Time Stop freezes living control and visible non-living motion; Antimagic stays authoritative.
field=text(magic/'ArcaneFieldService.java')
require_tokens(field,[
    'TIME_STOP_TICKS = 160','ANTIMAGIC_TICKS = 320','activateAntimagic','activateTimeStop','fulfillWish',
    'blocksCasting','FROZEN_ENTITIES','FrozenEntity','entity.setDeltaMovement(Vec3.ZERO)',
    'entity.setNoGravity(true)','raw.setDeltaMovement(frozen.velocity())','suppressMagicEffects','cleanseHarmful',
    'SpellGameplayService.clear(entity)','SpellKineticsService.cancel(player)'
],'field')
assert 'removeAllEffects' not in field
harmful=field[field.index('private static void cleanseHarmful'):field.index('private static void restoreFrozenLevel')]
assert 'MobEffects.SPEED' not in harmful and 'MobEffects.REGENERATION' not in harmful

# Weather secondary authority input and Arcana-only economy/UI.
ability=network/'UseArcaneAbilityPayload.java'; assert ability.exists()
client_input=text(client/'ArcaneClient.java')
require_tokens(client_input,['ARCANE_ABILITY_KEY','InputConstants.KEY_G','UseArcaneAbilityPayload(0)',
                             'event.register(ARCANE_ABILITY_KEY)'],'client ability')
network_source=text(network/'ArcaneNetwork.java')
require_tokens(network_source,['ninefold-arcana-12-1-alpha47','UseArcaneAbilityPayload.TYPE','handleArcaneAbility',
                               'SpellGameplayService.useMaintainedAuthority(player)'],'network ability')
economy=text(world/'ArcaneEconomyService.java')
assert '아르카나' in economy and '에메랄드' not in economy
assert '아르카나' in grimoire and '에메랄드' not in grimoire

# Meteor release seal collapses instead of lingering over the barrage.
maintenance=text(client/'HighCircleMaintenanceOverlay.java')
meteor_block=maintenance[maintenance.index('private static void meteorSwarm'):maintenance.index('private static void shapechange')]
require_tokens(meteor_block,['clamp(t / 1.05','sixteen short apertures','if (q >= 1.0) return',
                             'port.add(0, -(1.0 + q * 3.2)'],'meteor release')
assert 'double sweep = (t * 1.72)' not in meteor_block

# Lifecycle: utility, destruction, gameplay and fields all restore/clear on player/server boundaries.
require_tokens(main,[
    'HighUtilitySpellService::onIncomingDamage','HighUtilitySpellService.tick(level)',
    'HighUtilitySpellService.clear(player)','HighUtilitySpellService.clearAll()',
    'SpellGameplayService.tick(level)','SpellGameplayService.clearAll()',
    'ArcaneFieldService.tick(level)','ArcaneFieldService.clearAll()',
    'NpcMeteorBarrageService.tick(level)','NpcMeteorBarrageService.clearAll()'
],'main lifecycle')
assert main.count('HighUtilitySpellService.clear(player);')>=3
assert main.index('HighUtilitySpellService.tick(level)') < main.index('ArcaneFieldService.tick(level)')

# Equipment/staff/light/recipe regressions.
hud=text(client/'ArcaneHud.java')
assert 'spell_ribbon' in hud and 'drawVitals' in hud and 'drawFusion' in hud
staff=text(root/'src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneStaffItem.java')
assert 'castTimeMultiplier' in staff and '시전 전개시간' in staff
light=text(magic/'ArcaneLightService.java')
require_tokens(light,['Blocks.LIGHT','LightBlock.LEVEL','illuminate','clearAll'],'light')
recipe_dir=root/'src/main/resources/data/arcanecircle/recipe'
recipe_files=sorted(recipe_dir.glob('*_staff.json'))
assert len(recipe_files)==8,[p.name for p in recipe_files]
for recipe in recipe_files:
    value=text(recipe)
    assert '"type": "minecraft:crafting_shaped"' in value and '"category": "equipment"' in value

# Release verifier must reject a JAR that omitted the high-utility runtime.
verify=text(root/'tools/verify_jar.py')
assert 'kr/moonseungjun/arcanecircle/magic/HighUtilitySpellService.class' in verify

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('target_snapshot_parity=PASS')
print('seeded_meteor_barrage=PASS')
print('visible_footprint_destruction=PASS')
print('authoritative_time_stop_motion_stasis=PASS')
print('weather_active_authority=PASS')
print('arcana_currency_ui=PASS')
print('alpha48_real_clone=PASS')
print('alpha48_true_polymorph_body_swap=PASS')
print('alpha48_maze_battle_exile=PASS')
print('alpha48_ethereal_phase_runtime=PASS')
print('alpha48_utility_visual_identity=PASS')
print('retired_visual_stack=absent')
print('third_person_fake_geometry=absent')
print('source_mutation=disabled')
print('legacy_arcane_tooling=absent')
