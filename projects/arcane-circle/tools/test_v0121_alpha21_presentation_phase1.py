#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def t(rel): return (ROOT / rel).read_text(encoding='utf-8')
def need(src, token, label):
    if token not in src: raise SystemExit(f'{label}: missing {token!r}')
def forbid(src, token, label):
    if token in src: raise SystemExit(f'{label}: forbidden {token!r}')

props=t('gradle.properties')
main=t('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=t('src/main/resources/data/arcanecircle/spell_catalog/index.json')
profile=t('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java')
tracker=t('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java')
low=t('src/main/java/kr/moonseungjun/arcanecircle/client/LowCircleVisualIdentity.java')
casting=t('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java')
gear=t('src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java')
screen=t('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java')

need(props,'mod_version=0.12.1-alpha.21','version')
need(main,'VERSION = "0.12.1-alpha.21"','runtime version')
need(index,'"version": "0.12.1-alpha.21"','catalog version')
need(profile,'alpha.21 phase 1: canonical authored profiles for every 1C-3C formula','canonical low profile block')
need(tracker,'LowCircleVisualIdentity.owns(spell)','low visual route')
need(tracker,'LowCircleVisualIdentity.appendCharge','low charge route')
need(tracker,'LowCircleVisualIdentity.appendRelease','low release route')

spells = [
 'magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor',
 'scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate',
 'fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink',
 'burning_hands','ice_knife','chromatic_orb','wind_wall','counterspell','steam_burst','frost_step'
]
for spell in spells:
    need(profile, f'put("{spell}"', f'profile {spell}')
    need(low, f'case "{spell}"', f'identity {spell}')

# Quality contract: the baseline itself must contain several distinct construction and release primitives.
for token in ('missileRack(','fireIgniter(','frostAperture(','shieldLattice(','webSeal(','mirrorTriptych(',
              'fireballReactor(','lightningRail(','hasteClock(','blinkPair(','chromaticCrown(',
              'missileRelease(','fireballRelease(','lightningRelease(','windWallRelease('):
    need(low, token, 'authored low-circle grammar')

# Preserve alpha.20 interaction fixes while presentation changes land.
need(casting,'required <= 0 ? 1.0 : 0.0','zero-time held sigil')
forbid(casting,'if (required <= 0) {\n            castPrepared','zero-time immediate cast')
need(gear,'syncAtomicRobe(player)','atomic robe')
need(screen,'academyCircleViewport','responsive academy UI')

print(f'Arcane Circle alpha.21 presentation phase 1 audit: PASS ({len(spells)} authored 1C-3C formulae)')
