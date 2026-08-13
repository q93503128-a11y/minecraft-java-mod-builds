from pathlib import Path

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
world=root/'src/main/java/kr/moonseungjun/arcanecircle/world'
retired=[
    'CodexVisualLanguage.java','ArcaneSigilDetailGrammar.java','LowCircleVisualIdentity.java',
    'MidCircleVisualIdentity.java','FifthCircleVisualIdentity.java','SixthCircleVisualIdentity.java',
    'ArchmageVisualIdentity.java','RangeReactivePresentation.java','SpellVisualSignature.java',
    'CastingSilhouetteRenderer.java','RobeRegaliaRenderer.java','SignatureGeometry.java'
]
for name in retired:
    assert not (client/name).exists(), f'retired presentation file still present: {name}'

def text(path): return path.read_text(encoding='utf-8')
gradle=text(root/'gradle.properties')
main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json')
assert 'mod_version=0.12.1-alpha.33' in gradle
assert 'VERSION = "0.12.1-alpha.33"' in main
assert '"version": "0.12.1-alpha.33"' in index

tracker=text(client/'WorldMagicTracker.java')
assert 'SpellCinematicDirector.charge' in tracker and 'SpellCinematicDirector.release' in tracker
assert 'SpellCinematicDirector.castingFamily' in tracker
assert 'ArcaneSigilDirector.charge' in tracker and 'ArcaneSigilDirector.releaseEcho' in tracker
assert 'MeteorBarragePattern.withSeed' in tracker and 'longValue(values,"seed",0L)' in tracker
sigil=text(client/'ArcaneSigilDirector.java')
for token in ['formulaFrame','schoolFormula','geometricDepth','anchorFormula','skyRitual','meteorRitual','inscriptionRing','sigilRangeScale','meteor_swarm','fusionFormula']:
    assert token in sigil, f'authored sigil regression: {token}'
for token in ['LowCircleVisualIdentity','MidCircleVisualIdentity','FifthCircleVisualIdentity',
              'SixthCircleVisualIdentity','ArchmageVisualIdentity','ArcaneSigilDetailGrammar',
              'RangeReactivePresentation','SpellVisualSignature']:
    assert token not in tracker, f'legacy runtime route remains: {token}'

director=text(client/'SpellCinematicDirector.java')
for token in ['enum Form','NEEDLE','ORB','VOLLEY','RAY','CONE','FIELD','WALL','GATE','PRISON',
              'SKY','WEATHER','AURA','MARK','SHIFT','TRANSFORM','CLOCK','TERRAIN','DOMAIN',
              'meteorSwarm','executionWord','chainLightning','fireStorm','worldFault','phoenix',
              'SpellMetrics.effectRadius','SpellMetrics.wallWidth','SpellMetrics.waveLength','SpellMetrics.waveEndRadius']:
    assert token in director, f'cinematic director regression: {token}'
assert 'MeteorBarragePattern.count()' in director and 's.impactTick()' in director
assert 'double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}}' not in director
assert 'fallHeight=42.0' not in director and 'prismaticWallFrame' in director
assert 'case "power_word_kill"' in director and '?.72:1.0' in director

grimoire=text(client/'GrimoireScreen.java')
for token in ['drawSpine','circleIndex','circleStep','contentBottom','compact()','browserViewport','detailWidth','detail()','spellTile','primaryAction','drawLoadout','equipCandidateId','firstEmptySlot','quickEquip','academySelector','academySummary','academyOfferHeader','enableScissor','mouseScrolled']:
    assert token in grimoire, f'grimoire architecture regression: {token}'
assert 'CodexVisualLanguage' not in grimoire
assert 'private void request    private void request' not in grimoire
assert grimoire.count('private void request(String next)') == 1
assert 'Math.max(22,Math.min(29' not in grimoire
assert 'fineBuilder' in text(client/'ArcaneWorldMesh.java')
assert 'lineFloor' in text(client/'ArcaneWorldMesh.java')
assert 'drawCircleIndex(g,l,academyCircle,mouseX,mouseY,true)' not in grimoire
assert 'viewport().w()<410' in grimoire
assert 'MAX_FRAME = 9000' in tracker and 'MAX_ENTRY = 2800' in tracker
assert '!"prismatic_wall".equals(v.spell.id())' in tracker

assert (magic/'MeteorBarragePattern.java').exists() and (magic/'DestructiveMagicService.java').exists()
assert (magic/'CastTargetSnapshot.java').exists()
barrage=text(magic/'MeteorBarragePattern.java')
for token in ['BASE_STRIKES','impactTick','durationTicks','count()','strikes(long seed)','withSeed','castSeed','MIN_SEPARATION']:
    assert token in barrage, f'meteor barrage regression: {token}'
assert barrage.count('new Strike(') >= 16
assert 'private static final Strike[] STRIKES' not in barrage

target_snapshot=text(magic/'CastTargetSnapshot.java')
for token in ['targetEntityId','launchDirection','impactSurface','barrageSeed','dimension','executeLocked','resolvedTarget','boolean homing']:
    assert token in target_snapshot, f'target snapshot regression: {token}'
world_magic=text(magic/'WorldMagicService.java')
for token in ['captureSnapshot','CastTargetSnapshot snapshot','seed=%d','PLAYER_CHARGE_SEEDS','NPC_CHARGE_SEEDS',
              'npcChargeSeed','npcReleaseSeed','consumeNpcSnapshot',
              'MeteorBarragePattern.firstImpactTick(snapshot.barrageSeed())']:
    assert token in world_magic, f'world magic snapshot regression: {token}'

destruction=text(magic/'DestructiveMagicService.java')
for token in ['getDestroySpeed','getExplosionResistance','destroyBlock','maxBlocks','hasBlockEntity','world_sunder',
              'meteor_swarm','disintegrate','hasChunkAt','MAX_BLOCK_CHANGES_PER_TICK','MAX_BLOCK_SCANS_PER_TICK',
              'MAX_DROPPED_BLOCKS_PER_TICK','dropChangesRemaining','TerrainClass','MAJOR','CONDITIONAL',
              'lightning_bolt','thunderwave','gust_of_wind']:
    assert token in destruction, f'destructive magic regression: {token}'
assert 'case "move_earth" -> new Profile(.54, 11.0, 170, false)' in destruction
assert 'case "earthquake" -> new Profile(.58, 14.5, 240, false)' in destruction
assert 'case "world_sunder" -> new Profile(.62, 28.0, 320, false)' in destruction

kinetics=text(magic/'SpellKineticsService.java')
for token in ['CastTargetSnapshot targetSnapshot','captureSnapshot','executeLocked','targetSnapshot().validFor(player)',
              'barrageSeed','MeteorBarragePattern.strike(targetSnapshot.barrageSeed(), 0)','applyPhysicalAftermath']:
    assert token in kinetics, f'authoritative target snapshot regression: {token}'
assert 'WorldMagicService.lockedTarget' not in kinetics
assert 'lockedTarget' not in kinetics
assert 'DestructiveMagicService.impact(player,"meteor_swarm"' in text(magic/'HighCircleSpellEffects.java')

fusion=text(magic/'FusionSpellEffects.java')
assert 'DestructiveMagicService.impact(player, "world_sunder", center, radius, power)' in fusion
assert 'CastTargetSnapshot.targetOr(player, fallback)' in fusion
assert 'new AABB(lockedPoint, lockedPoint).inflate(2.4)' in fusion
assert 'targetEntity(player)' not in fusion
assert 'DestructiveMagicService.impact(player,"world_sunder",player.position()' not in fusion

npc_resolver=text(world/'NpcSpellResolver.java')
for token in ['consumeNpcSnapshot','NpcMeteorBarrageService.schedule','directAt','snapshot.launchOrigin()','lockedTarget']:
    assert token in npc_resolver, f'NPC target parity regression: {token}'
npc_barrage=text(world/'NpcMeteorBarrageService.java')
for token in ['MeteorBarragePattern.strike','barrageSeed','MAX_ACTIVE_BARRAGES','resolveStrike','nextStrike']:
    assert token in npc_barrage, f'NPC meteor scheduler regression: {token}'
assert 'NpcMeteorBarrageService.tick' in main and 'NpcMeteorBarrageService.clearAll' in main
assert 'WorldMagicService.clearAll' in main
assert main.count('SpellKineticsService.clear(player.getUUID())') >= 2

hud=text(client/'ArcaneHud.java')
assert 'spell_ribbon' in hud and 'drawSeal' in hud and 'drawVitals' in hud
assert 'drawFusion' in hud

gear=text(client/'ArcaneGearRenderer.java')
assert 'ArcaneRegaliaRenderer.render' in gear and 'ArcaneCastingPerformance.render' in gear
assert 'CastingSilhouetteRenderer' not in gear and 'RobeRegaliaRenderer' not in gear
regalia=text(client/'ArcaneRegaliaRenderer.java')
for token in ['outfit','bodice','lapel','shoulderMantle','skirtPair','sideGore','backTrain','facetedSkirt','asymmetricSkirt','ceremonialTab']:
    assert token in regalia, f'regalia garment regression: {token}'
assert 'private static void torso(' not in regalia, 'old torso-card garment returned'
casting=text(client/'ArcaneCastingPerformance.java')
for token in ['snap','aim','heavy','ground','ward','portal','ritual']:
    assert token in casting

assert 'presentationImpactDelay' in kinetics and 'WorldMagicService' in kinetics
casting_service=text(magic/'SpellCastingService.java')
assert 'READY_HOLD_TIMEOUT_TICKS' in casting_service and 'chargeTimeoutTicks' in casting_service
assert '{0, 6, 10, 16, 26, 42, 68, 105, 155, 220}' in casting_service
assert 'equipped(player).castTimeMultiplier()' in casting_service
assert 'default -> 190;' in casting_service and 'baseMinimum * staffScale' in casting_service
staff=text(root/'src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneStaffItem.java')
assert 'castTimeMultiplier' in staff and '시전 전개시간' in staff
assert not (magic/'SpellSigilService.java').exists(), 'empty legacy SpellSigilService returned'
light=text(magic/'ArcaneLightService.java')
for token in ['Blocks.LIGHT','LightBlock.LEVEL','illuminate','clearAll']:
    assert token in light, f'Light world-illumination regression: {token}'
assert 'ArcaneLightService.illuminate(player,1800)' in text(magic/'ExpandedSpellEffects.java')
mage_gear=text(magic/'MageGearService.java')
assert 'syncAtomicRobe' in mage_gear

print('Arcane Circle current-source audit: PASS')
print('retired_visual_stack=absent')
print('gameplay_content=preserved')
print('source_mutation=disabled')
print('target_snapshot_parity=PASS')
print('seeded_meteor_barrage=PASS')
print('destruction_tick_budget=PASS')
print('destruction_drop_budget=PASS')
print('npc_meteor_scheduler=PASS')
print('npc_meteor_seed_stability=PASS')
print('non_homing_target_seal=PASS')

# Active-tree hygiene. Git history is the archive; current source contains no version-migration machinery.
repo=root.parents[1]
retired_tokens=[n.removesuffix('.java') for n in retired]
for path in (root/'src').rglob('*.java'):
    body=text(path)
    for token in retired_tokens:
        assert token not in body, f'retired design reference remains: {token} in {path.relative_to(root)}'

tools_dir=root/'tools'
assert {p.name for p in tools_dir.iterdir() if p.is_file()} == {'test_current_source.py','verify_jar.py'}
assert not [p for p in tools_dir.iterdir() if p.is_dir()], 'legacy tool directories remain'

scripts_dir=repo/'.github/scripts'
if scripts_dir.exists():
    assert not (scripts_dir/'arcane-circle').exists(), 'legacy Arcane migration directory remains'
    assert not list(scripts_dir.glob('*arcane*')), 'legacy Arcane patch/migration script remains'

for obsolete in ['AUDIT_REPORT_V0.5.md','BUILD_AND_RUNTIME_REPORT.md','MAGIC_WORLD_PATCH.md',
                 'docs/ALPHA10_WORLD_COMBAT.md','docs/PRESENTATION_OVERHAUL_PHASES.md']:
    assert not (root/obsolete).exists(), f'obsolete project document remains: {obsolete}'

print('legacy_arcane_tooling=absent')
