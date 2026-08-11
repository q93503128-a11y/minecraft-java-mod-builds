from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def need(text, token, label):
    if token not in text:
        raise SystemExit(f'missing {label}: {token!r}')

def forbid(text, token, label):
    if token in text:
        raise SystemExit(f'forbidden {label}: {token!r}')

props = read('gradle.properties')
main = read('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index = json.loads(read('src/main/resources/data/arcanecircle/spell_catalog/index.json'))
profile = read('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java')
tracker = read('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java')
low = read('src/main/java/kr/moonseungjun/arcanecircle/client/LowCircleVisualIdentity.java')
mid = read('src/main/java/kr/moonseungjun/arcanecircle/client/MidCircleVisualIdentity.java')
casting = read('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java')
gear = read('src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java')
screen = read('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java')
doc = read('docs/PRESENTATION_OVERHAUL_PHASES.md')

VERSION = '0.12.1-alpha.22'
need(props, f'mod_version={VERSION}', 'Gradle version')
need(main, f'VERSION = "{VERSION}"', 'runtime version')
if index.get('version') != VERSION:
    raise SystemExit(f'spell catalog version mismatch: {index.get("version")!r}')

# Phase 1 remains the immutable quality/regression baseline.
need(profile, 'alpha.21 phase 1: canonical authored profiles for every 1C-3C formula', 'phase1 profile baseline')
phase1 = [
    'magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor',
    'scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate',
    'fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink',
    'burning_hands','ice_knife','chromatic_orb','wind_wall','counterspell','steam_burst','frost_step'
]
for spell_id in phase1:
    need(profile, f'put("{spell_id}"', f'phase1 profile {spell_id}')
    need(low, f'case "{spell_id}"', f'phase1 authored identity {spell_id}')
for token in (
    'missileRack(', 'fireIgniter(', 'frostAperture(', 'shieldLattice(', 'webSeal(',
    'mirrorTriptych(', 'fireballReactor(', 'lightningRail(', 'hasteClock(', 'blinkPair(',
    'chromaticCrown(', 'missileRelease(', 'fireballRelease(', 'lightningRelease(', 'windWallRelease('
):
    need(low, token, 'phase1 authored visual grammar')

# Phase 2A owns every 4C normal/fusion formula and nothing above it yet.
need(mid, 'spell.circle() == 4', 'phase2A ownership boundary')
phase2a = [
    'wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door',
    'stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer',
    'fire_shield','wall_of_ice','thunder_cage'
]
for spell_id in phase2a:
    need(profile, f'put("{spell_id}"', f'phase2A profile {spell_id}')
    need(mid, f'case "{spell_id}"', f'phase2A authored identity {spell_id}')

for token in (
    'fireWallInstallation(', 'iceStormCanopy(', 'invisibilityErasure(',
    'resilientSphereClosure(', 'dimensionDoorCorridor(', 'stoneSkinPlating(',
    'confusionCompass(', 'blightCage(', 'freedomShackles(', 'phantasmalMask(',
    'fireShieldBastion(', 'iceWallButtress(', 'thunderCagePylons(',
    'fireWallRelease(', 'iceStormRelease(', 'dimensionDoorRelease(',
    'thunderCageRelease('
):
    need(mid, token, 'phase2A authored visual grammar')

# Mid-circle routing must happen after the protected Phase 1 route but before both generic paths.
charge_low = tracker.index('if (LowCircleVisualIdentity.owns(spell))')
charge_mid = tracker.index('if (MidCircleVisualIdentity.owns(spell))')
generic_charge = tracker.index('switch (profile.sigil())', charge_mid)
if not (charge_low < charge_mid < generic_charge):
    raise SystemExit('charge routing order is not Low -> Mid -> generic')
release_low = tracker.index('if (LowCircleVisualIdentity.owns(spell))', charge_low + 1)
release_mid = tracker.index('if (MidCircleVisualIdentity.owns(spell))', charge_mid + 1)
generic_release = tracker.index('switch (profile.motion())', release_mid)
if not (release_low < release_mid < generic_release):
    raise SystemExit('release routing order is not Low -> Mid -> generic')
need(tracker, 'targetOffset(visual), mesh);', 'target-space authored routing')

# Existing input/equipment/UI contracts remain mandatory.
need(casting, 'required <= 0 ? 1.0 : 0.0', 'zero-time ready-hold contract')
need(casting, 'READY_HOLD_TIMEOUT_TICKS', 'ready hold timeout')
forbid(casting, 'if (required <= 0) {\n            cast', 'immediate zero-time auto fire')
need(gear, 'syncAtomicRobe(player)', 'atomic robe contract')
need(screen, 'academyCircleViewport()', 'responsive academy viewport')
need(screen, 'enableScissor(', 'responsive scissor')
need(doc, 'Phase 2A — 4C authored battlefield presentation (alpha.22)', 'phase2A documentation')
need(doc, 'Phase 2B — 5C', 'phase2B continuation')
need(doc, 'Phase 2C — 6C', 'phase2C continuation')

print('Arcane Circle alpha.22 Presentation Phase 2A audit: PASS')
print(f'Phase 1 authored formulas preserved: {len(phase1)}')
print(f'Phase 2A authored 4C formulas: {len(phase2a)}')
print('source_mutation=disabled')
