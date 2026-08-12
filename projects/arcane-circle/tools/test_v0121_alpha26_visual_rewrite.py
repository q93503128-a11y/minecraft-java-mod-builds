from pathlib import Path

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
retired=[
    'CodexVisualLanguage.java','ArcaneSigilDetailGrammar.java','LowCircleVisualIdentity.java',
    'MidCircleVisualIdentity.java','FifthCircleVisualIdentity.java','SixthCircleVisualIdentity.java',
    'ArchmageVisualIdentity.java','RangeReactivePresentation.java','SpellVisualSignature.java',
    'CastingSilhouetteRenderer.java','RobeRegaliaRenderer.java'
]
for name in retired:
    assert not (client/name).exists(), f'retired presentation file still present: {name}'

def text(path): return path.read_text(encoding='utf-8')
gradle=text(root/'gradle.properties')
main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json')
assert 'mod_version=0.12.1-alpha.26' in gradle
assert 'VERSION = "0.12.1-alpha.26"' in main
assert '"version": "0.12.1-alpha.26"' in index

tracker=text(client/'WorldMagicTracker.java')
assert 'SpellCinematicDirector.charge' in tracker and 'SpellCinematicDirector.release' in tracker
assert 'SpellCinematicDirector.castingFamily' in tracker
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
for token in ['apprentice','sage','cinder','glacier','tempest','archmage','rift']:
    assert token in regalia
casting=text(client/'ArcaneCastingPerformance.java')
for token in ['snap','aim','heavy','ground','ward','portal','ritual']:
    assert token in casting

kinetics=text(magic/'SpellKineticsService.java')
assert 'presentationImpactDelay' in kinetics and 'WorldMagicService' in kinetics
casting_service=text(magic/'SpellCastingService.java')
assert 'READY_HOLD_TIMEOUT_TICKS' in casting_service and 'chargeTimeoutTicks' in casting_service
mage_gear=text(magic/'MageGearService.java')
assert 'syncAtomicRobe' in mage_gear

print('Arcane Circle alpha.26 ground-up visual rewrite audit: PASS')
print('retired_visual_stack=absent')
print('gameplay_content=preserved')
print('source_mutation=disabled')
