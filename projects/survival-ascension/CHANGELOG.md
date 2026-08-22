# Changelog

## 0.5.0-alpha.1
- Activated Combat as the fourth live skill.
- Added kill-based Combat XP with high hostile-mob weighting and reduced passive-creature weighting.
- Added smooth outgoing player damage growth reaching about 1.8x at Combat Lv.100.
- Added melee cleave at Lv.30/60/90, hitting up to 2/4/8 nearby hostile enemies at 25%/42%/60% propagated damage.
- Increased cleave radius through 1.75/2.75/4.0 blocks.
- Kept ranged attacks eligible for damage growth but excluded them from melee cleave.
- Added a cleave recursion guard and Enemy-only secondary target filter to avoid runaway chains and accidental passive/villager damage.
- Added Combat to the K skills screen, `/ascension stats`, and the test setlevel command.

## 0.4.0-alpha.1
- Expanded Mining terrain excavation to 9x9 at level 90.
- Added connected ore-vein extraction: 24/64/128 blocks at Lv.30/60/90.
- Added ore-family equivalence for major stone/deepslate ore variants.
- Adapted Veinminer++ MIT ore matching and bounded flood-fill patterns with packaged attribution.

## 0.3.0-alpha.1
- Activated Harvesting with mature-only XP and 3x3/5x5/7x7/9x9 hoe harvesting.
- Added the K-key six-skill overview and shared mastery tiers I-V.

## 0.2.0-alpha.1
- Added generic per-skill XP storage, synchronized HUD and Woodcutting progression.
- Adapted Skill Proficiencies MIT architecture with attribution.

## 0.1.0-alpha.1
- Initial Mining progression prototype.
