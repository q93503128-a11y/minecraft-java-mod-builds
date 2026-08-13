from pathlib import Path

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
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
assert 'mod_version=0.12.1-alpha.28' in gradle
assert 'VERSION = "0.12.1-alpha.28"' in main
assert '"version": "0.12.1-alpha.28"' in index

tracker=text(client/'WorldMagicTracker.java')
assert 'SpellCinematicDirector.charge' in tracker and 'SpellCinematicDirector.release' in tracker
assert 'SpellCinematicDirector.castingFamily' in tracker
assert 'ArcaneSigilDirector.charge' in tracker and 'ArcaneSigilDirector.releaseEcho' in tracker
sigil=text(client/'ArcaneSigilDirector.java')
for token in ['formulaFrame','schoolFormula','anchorFormula','skyRitual','meteor_swarm','runeRing','brokenBand','fusionFormula']:
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
assert 'double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}}' in director
assert 'add(0,28*(1-easeIn(t)),0)' in director
assert 'case "power_word_kill"' in director and '?.72:1.0' in director

grimoire=text(client/'GrimoireScreen.java')
for token in ['drawSpine','circleIndex','browserViewport','detail()','spellTile','primaryAction','drawLoadout','enableScissor','mouseScrolled']:
    assert token in grimoire, f'grimoire architecture regression: {token}'
assert 'CodexVisualLanguage' not in grimoire
assert 'Math.max(125,Math.min(205,b.w()/3))' in grimoire

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

kinetics=text(magic/'SpellKineticsService.java')
assert 'presentationImpactDelay' in kinetics and 'WorldMagicService' in kinetics
casting_service=text(magic/'SpellCastingService.java')
assert 'READY_HOLD_TIMEOUT_TICKS' in casting_service and 'chargeTimeoutTicks' in casting_service
assert '{0, 6, 10, 16, 26, 42, 68, 105, 155, 220}' in casting_service
assert 'equipped(player).castTimeMultiplier()' in casting_service
assert 'default -> 190;' in casting_service and 'baseMinimum * staffScale' in casting_service
staff=text(root/'src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneStaffItem.java')
assert 'castTimeMultiplier' in staff and '시전 전개시간' in staff
assert not (magic/'SpellSigilService.java').exists(), 'empty legacy SpellSigilService returned'
mage_gear=text(magic/'MageGearService.java')
assert 'syncAtomicRobe' in mage_gear

print('Arcane Circle current-source audit: PASS')
print('retired_visual_stack=absent')
print('gameplay_content=preserved')
print('source_mutation=disabled')


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
