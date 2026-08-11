from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def text(path): return (ROOT/path).read_text(encoding='utf-8')
def need(hay, token, why):
    if token not in hay: raise SystemExit(f'missing {why}: {token}')

def forbid(hay, token, why):
    if token in hay: raise SystemExit(f'forbidden {why}: {token}')

low=text('src/main/java/kr/moonseungjun/arcanecircle/client/LowCircleVisualIdentity.java')
mid=text('src/main/java/kr/moonseungjun/arcanecircle/client/MidCircleVisualIdentity.java')
fifth=text('src/main/java/kr/moonseungjun/arcanecircle/client/FifthCircleVisualIdentity.java')
rangefx=text('src/main/java/kr/moonseungjun/arcanecircle/client/RangeReactivePresentation.java')
tracker=text('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java')
metrics=text('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellMetrics.java')
fusion=text('src/main/java/kr/moonseungjun/arcanecircle/magic/FusionSpellEffects.java')
expanded=text('src/main/java/kr/moonseungjun/arcanecircle/magic/ExpandedSpellEffects.java')
profiles=text('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java')
casting=text('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java')
gear=text('src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java')
ui=text('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java')
props=text('gradle.properties')
main=text('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text('src/main/resources/data/arcanecircle/spell_catalog/index.json')

for token in ('missileRack','fireIgniter','frostAperture','webSeal','fireballReactor','lightningRail','chromaticCrown','windWallRelease'):
    need(low,token,'phase1 authored identity regression')
for token in ('fireWallInstallation','iceStormCanopy','dimensionDoorCorridor','resilientSphereClosure','thunderCagePylons','phantasmalMask','iceWallButtress'):
    need(mid,token,'phase2A authored identity regression')
for token in ('cloudkillCollectors','forceWallAnchors','holdMonsterBraces','passwallFrames','insectPlagueBeacons','telekinesisVectorRig','coneColdAperture','flameStrikeJudgement','dominateCrown','massCureLattice','chainLightningRouter','arcaneHandAssembly','teleportCircleAddress'):
    need(fifth,token,'phase2B authored identity')
for spell in ('cloudkill','wall_of_force','hold_monster','passwall','insect_plague','telekinesis','cone_of_cold','flame_strike','dominate_person','mass_cure_wounds','chain_lightning','arcane_hand','teleportation_circle'):
    need(fifth,f'case "{spell}"','5C ownership')
need(fifth,'spell.circle() == 5','5C-only director')

charge_low=tracker.index('if (LowCircleVisualIdentity.owns(spell))')
charge_mid=tracker.index('if (MidCircleVisualIdentity.owns(spell))')
charge_fifth=tracker.index('if (FifthCircleVisualIdentity.owns(spell))')
generic_charge=tracker.index('switch (profile.sigil())',charge_fifth)
if not charge_low < charge_mid < charge_fifth < generic_charge: raise SystemExit('charge route is not Low -> Mid -> Fifth -> generic')
release_low=tracker.index('if (LowCircleVisualIdentity.owns(spell))',charge_low+1)
release_mid=tracker.index('if (MidCircleVisualIdentity.owns(spell))',charge_mid+1)
release_fifth=tracker.index('if (FifthCircleVisualIdentity.owns(spell))',charge_fifth+1)
generic_release=tracker.index('switch (profile.motion())',release_fifth)
if not release_low < release_mid < release_fifth < generic_release: raise SystemExit('release route is not Low -> Mid -> Fifth -> generic')
need(tracker,'RangeReactivePresentation.appendRelease','range-reactive authored release wiring')
need(tracker,'visual.range, age','effective range forwarded to authored director')

need(metrics,'wallWidth(','shared wall metric')
need(metrics,'case"steam_burst"->.42','steam cone metric')
need(rangefx,'steamBurstEnvelope','steam visible envelope')
need(rangefx,'SpellMetrics.waveLength(range)','shared visible wave length')
need(rangefx,'SpellMetrics.wallWidth','shared visible wall width')
forbid(fusion,'Math.min(11.0, range)','legacy steam 11-block cap')
need(fusion,'SpellMetrics.waveEndRadius("steam_burst"','server steam cone metric')
need(fusion,'lateralSq','server steam cone lateral hit test')
forbid(expanded,'hinderingField(player, range, 4.0','fixed grease radius')
forbid(expanded,'hinderingField(player, range, 5.0','fixed slow radius')
forbid(expanded,'nearby(player, center, 4.5, 3.0)','fixed sleep radius')
forbid(expanded,'nearby(player, center, 5.0, 3.2)','fixed web radius')
forbid(expanded,'Math.min(10.0, range * 0.36)','legacy wall hard cap')
need(expanded,'SpellMetrics.wallWidth(id, range','server wall range metric')
need(expanded,'storm(player, "ice_storm", range','ice storm id routing')
need(expanded,'SpellMetrics.effectRadius(id, range, 4)','storm shared range metric')

for spell in ('cloudkill','wall_of_force','hold_monster','passwall','insect_plague','telekinesis','cone_of_cold','flame_strike','dominate_person','mass_cure_wounds','chain_lightning','arcane_hand','teleportation_circle'):
    need(profiles,f'put("{spell}"','5C authored profile')
for token in ('READY_HOLD_TIMEOUT_TICKS','chargeTimeoutTicks'):
    need(casting,token,'held cast regression')
need(gear,'syncAtomicRobe','atomic robe regression')
need(ui,'academyCircleViewport()','responsive academy viewport')
need(ui,'enableScissor(','responsive scissor')
need(ui,'scroll','responsive scroll')

for hay in (props,main,index): need(hay,'0.12.1-alpha.23','alpha.23 version')
print('Arcane Circle alpha.23 Phase 2B + range sync audit: PASS')
