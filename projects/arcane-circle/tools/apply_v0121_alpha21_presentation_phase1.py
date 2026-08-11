#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def write(rel, text):
    (ROOT / rel).write_text(text, encoding='utf-8')

def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label}: source pattern not found')
    return text.replace(old, new, 1)

# Version bump.
props = read('gradle.properties').replace('mod_version=0.12.1-alpha.20', 'mod_version=0.12.1-alpha.21')
write('gradle.properties', props)
main = read('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java').replace(
    'VERSION = "0.12.1-alpha.20"', 'VERSION = "0.12.1-alpha.21"')
write('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java', main)
index = read('src/main/resources/data/arcanecircle/spell_catalog/index.json').replace(
    '"version": "0.12.1-alpha.20"', '"version": "0.12.1-alpha.21"')
write('src/main/resources/data/arcanecircle/spell_catalog/index.json', index)

# Every 1C-3C formula gets an authored placement/motion profile. These final puts intentionally
# override older partial authored entries so phase 1 has one canonical tuning block.
profile_path = 'src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java'
profile = read(profile_path)
marker = '        // alpha.21 phase 1: canonical authored profiles for every 1C-3C formula.'
if marker not in profile:
    block = '''
        // alpha.21 phase 1: canonical authored profiles for every 1C-3C formula.
        // Rank does not define size; each value follows the spell fiction and its launch device.
        put("magic_missile", SigilStyle.FRONT_COMPACT, MotionStyle.MISSILE_SWARM, 0.96, 2, 3, 62, 0, 0.92, 0);
        put("fire_bolt", SigilStyle.FRONT_LANCE, MotionStyle.BOLT, 0.68, 1, 0, 50, 0, 0.88, 0);
        put("ray_of_frost", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 0.76, 2, 0, 0, 0, 0.90, 0);
        put("shield", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.12, 2, 0, 0, 0, 1.02, 0);
        put("feather_fall", SigilStyle.FEET_RUNE, MotionStyle.AURA, 1.18, 2, 0, 0, 0, 0.96, 0);
        put("light", SigilStyle.BODY_HALO, MotionStyle.AURA, 0.78, 1, 0, 0, 0, 0.84, 0);
        put("grease", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 2.45, 2, 4, 0, 0, 1.04, 1);
        put("sleep", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 2.85, 2, 3, 0, 0, 1.08, 1);
        put("thunderwave", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.48, 2, 4, 0, 0, 1.06, 0);
        put("mage_armor", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.34, 2, 2, 0, 0, 1.08, 0);

        put("scorching_ray", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.08, 3, 3, 0, 0, 1.02, 0);
        put("misty_step", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.62, 2, 1, 0, 0, 0.94, 0);
        put("web", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 3.20, 3, 0, 0, 0, 1.10, 1);
        put("mirror_image", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.65, 3, 3, 0, 0, 1.04, 0);
        put("invisibility", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.10, 2, 0, 0, 0, 0.96, 0);
        put("gust_of_wind", SigilStyle.FRONT_LANCE, MotionStyle.WAVE, 1.18, 3, 2, 0, 0, 1.02, 0);
        put("hold_person", SigilStyle.TARGET_SEAL, MotionStyle.PRISON, 1.42, 3, 4, 0, 0, 1.10, 2);
        put("shatter", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.88, 3, 7, 0, 0, 1.12, 4);
        put("blur", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.18, 2, 3, 0, 0, 0.98, 0);
        put("levitate", SigilStyle.FEET_RUNE, MotionStyle.AURA, 1.34, 3, 3, 0, 0, 1.00, 0);

        put("fireball", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.46, 3, 0, 27, 0, 1.18, 0);
        put("lightning_bolt", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 1.22, 3, 0, 0, 0, 1.12, 0);
        put("fly", SigilStyle.FEET_RUNE, MotionStyle.AURA, 1.58, 3, 2, 0, 0, 1.08, 0);
        put("haste", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.36, 3, 0, 0, 0, 1.04, 0);
        put("dispel_magic", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.38, 3, 0, 0, 0, 1.02, 2);
        put("vampiric_touch", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.46, 3, 2, 0, 0, 1.08, 2);
        put("slow", SigilStyle.GROUND_SEAL, MotionStyle.FIELD, 3.65, 3, 0, 0, 0, 1.14, 2);
        put("protection_from_energy", SigilStyle.BODY_HALO, MotionStyle.AURA, 1.68, 3, 4, 0, 0, 1.12, 0);
        put("sleet_storm", SigilStyle.GROUND_SEAL, MotionStyle.STORM, 6.20, 4, 4, 0, 0, 1.30, 4);
        put("blink", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.70, 3, 2, 0, 0, 1.02, 0);

        put("burning_hands", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.18, 2, 5, 0, 0, 0.96, 0);
        put("ice_knife", SigilStyle.FRONT_LANCE, MotionStyle.DART, 0.92, 2, 0, 54, 0, 0.94, 0);
        put("chromatic_orb", SigilStyle.FRONT_COMPACT, MotionStyle.HEAVY_ORB, 1.22, 3, 7, 36, 0, 1.10, 0);
        put("wind_wall", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 4.60, 3, 5, 0, 0, 1.18, 2);
        put("counterspell", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 1.45, 3, 0, 0, 0, 1.08, 1);
        put("steam_burst", SigilStyle.FRONT_COMPACT, MotionStyle.WAVE, 1.32, 2, 2, 0, 0, 1.04, 0);
        put("frost_step", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 1.88, 3, 6, 0, 0, 1.08, 0);
'''
    anchor = '    }\n\n    private SpellPresentationProfile() {}'
    if anchor not in profile:
        raise SystemExit('profile insertion anchor missing')
    profile = profile.replace(anchor, block + '    }\n\n    private SpellPresentationProfile() {}', 1)
write(profile_path, profile)

# Route 1C-3C through the authored director before any shared school/fingerprint fallback.
tracker_path = 'src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java'
tracker = read(tracker_path)
charge_old = '''        double release = sigilPhase(p, 0.68, 1.00);\n\n        // There is deliberately no universal disc/ring prelude here.'''
charge_new = '''        double release = sigilPhase(p, 0.68, 1.00);\n\n        if (LowCircleVisualIdentity.owns(spell)) {\n            LowCircleVisualIdentity.appendCharge(spell, profile, basis, outer, rotation, p, mesh);\n            return mesh.build();\n        }\n\n        // There is deliberately no universal disc/ring prelude here.'''
tracker = replace_once(tracker, charge_old, charge_new, 'low-circle charge routing')
release_old = '''        double powerFactor = clamp(Math.pow(Math.max(0.08,\n                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0) * profile.releaseScale();\n        switch (profile.motion()) {'''
release_new = '''        double powerFactor = clamp(Math.pow(Math.max(0.08,\n                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0) * profile.releaseScale();\n        if (LowCircleVisualIdentity.owns(spell)) {\n            LowCircleVisualIdentity.appendRelease(spell, visual.direction, targetOffset(visual), age,\n                    motionProgress(visual, age), powerFactor, mesh);\n            return mesh.build();\n        }\n        switch (profile.motion()) {'''
tracker = replace_once(tracker, release_old, release_new, 'low-circle release routing')
write(tracker_path, tracker)

print('Arcane Circle alpha.21 presentation phase 1 migration: PASS')
