#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("projects/survival-ascension")


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace(rel: str, old: str, new: str, count: int = 1) -> None:
    text = read(rel)
    if old not in text:
        raise SystemExit(f"{rel}: replacement marker missing: {old[:120]!r}")
    write(rel, text.replace(old, new, count))


# Runtime hardening: preserve launch-time projectile state across re-entry and clamp stored values.
replace(
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Projectile projectile)) return;\n        if (!(projectile.getOwner() instanceof ServerPlayer player)) return;",
    "if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof Projectile projectile)) return;\n        if (AscensionAffixes.isRangedProjectile(projectile)) return;\n        if (!(projectile.getOwner() instanceof ServerPlayer player)) return;",
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "return Math.max(1.0D, direct.getPersistentData().getIntOr(RANGED_DAMAGE_PERMILLE, 1000) / 1000.0D);",
    "return Math.min(1.25D, Math.max(1.0D, direct.getPersistentData().getIntOr(RANGED_DAMAGE_PERMILLE, 1000) / 1000.0D));",
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "return Math.max(1.0D, direct.getPersistentData().getIntOr(RANGED_XP_PERMILLE, 1000) / 1000.0D);",
    "return Math.min(1.50D, Math.max(1.0D, direct.getPersistentData().getIntOr(RANGED_XP_PERMILLE, 1000) / 1000.0D));",
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "return Math.max(0, direct.getPersistentData().getIntOr(RANGED_RADIUS_TENTHS, 0)) / 10.0D;",
    "return Math.min(1.5D, Math.max(0, direct.getPersistentData().getIntOr(RANGED_RADIUS_TENTHS, 0)) / 10.0D);",
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "return Math.max(0, direct.getPersistentData().getIntOr(RANGED_TARGET_BONUS, 0));",
    "return Math.min(4, Math.max(0, direct.getPersistentData().getIntOr(RANGED_TARGET_BONUS, 0)));",
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "return Math.max(0, direct.getPersistentData().getIntOr(RANGED_FRACTION_PERMILLE, 0)) / 1000.0D;",
    "return Math.min(0.15D, Math.max(0, direct.getPersistentData().getIntOr(RANGED_FRACTION_PERMILLE, 0)) / 1000.0D);",
)

replace("gradle.properties", "mod_version=0.51.0-alpha.1", "mod_version=0.52.0-alpha.1")
replace(
    "modpack/content-lock.json",
    '"version": "0.51.0-alpha.1-content-preview.1"',
    '"version": "0.52.0-alpha.1-content-preview.1"',
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "검/곡괭이/도끼/삽/괭이/방어구 태그 장비",
    "검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 태그 장비",
)
replace(
    "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java",
    "검/곡괭이/도끼/삽/괭이/방어구 표준 태그 장비 필요",
    "검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 표준 태그 장비 필요",
)

project = read("PROJECT.md")
project = project.replace("- Mod version: `0.51.0-alpha.1`", "- Mod version: `0.52.0-alpha.1`", 1)
old_compat = "- Existing-world compatibility: no new SavedData ID or migration. Existing skill XP totals, `infrastructure_v1`, `field_depots_v1`, `outpost_v1`, `production_v1`, existing affix CustomData and older data stay unchanged. 0.51 adds the existing affix category `armor` to standard humanoid armor and reads effects only from currently equipped pieces; 0.50 regional 3/6/9 admission, 0.49 cart-local frontline manifest NBT, 0.48 exact-outpost local supply, optional integrations, physical freight railheads and item-data boundaries remain unchanged."
new_compat = "- Existing-world compatibility: no new SavedData ID or migration. Existing skill XP totals, `infrastructure_v1`, `field_depots_v1`, `outpost_v1`, `production_v1`, existing affix CustomData and older data stay unchanged. 0.52 adds the existing affix category `ranged` to standard bow/crossbow tags and stores bounded launch-time affix/precision snapshots only on the physical projectile; 0.51 armor, 0.50 regional 3/6/9 admission, 0.49 cart-local frontline manifest NBT, 0.48 exact-outpost local supply, optional integrations, physical freight railheads and item-data boundaries remain unchanged."
if old_compat not in project:
    raise SystemExit("PROJECT compatibility marker missing")
project = project.replace(old_compat, new_compat, 1)
if "## 0.52 Ranged Combat Ascension / 원거리 전투 승천" not in project:
    marker = "## 0.51 Armor Ascension / 방어구 승천 성장\n"
    section = """## 0.52 Ranged Combat Ascension / 원거리 전투 승천
- NeoForge common bow/crossbow tags (`c:tools/bow`, `c:tools/crossbow`) now join Ascension Imprint, reforge, Mythic awakening, salvage and elite affix drops without optional-mod Java dependencies.
- Every player-fired tagged bow/crossbow projectile snapshots its ranged affix multipliers and Shift precision state at entity launch. Changing the held item after firing cannot change that shot's damage, Combat XP or burst modifiers.
- Combat Lv30/60/90/100 expands ranged impact bursts at the hit position: base 2.5/2, 3.5/4, 4.25/6, 5.0/8 radius/targets; Lv100 Field Mastery reaches base 6.0 blocks / 10 targets.
- Shift at launch is precision mode: direct-hit damage and snapshotted affixes remain, but the impact burst is disabled for that projectile.
- Ranged affixes reuse the five existing slots with authored roles: `강궁` direct damage (+8/+15/+25%), `산개` burst radius (+0.5/+1.0/+1.5), `숙련` Combat XP (+10/+25/+50%), `연쇄` extra targets (+1/+2/+4), `충격` burst fraction (+5/+10/+15 percentage points).
- Burst fraction is hard-capped at 65%. Persisted projectile bonuses are independently clamped to damage1.25x, XP1.50x, radius+1.5, targets+4 and fraction+0.15 so malformed NBT cannot create unbounded combat scale.
- Already-snapshotted projectiles are not re-snapshotted when re-entering the level, preserving launch-time ownership across chunk unload/reload.
- No custom projectile/entity, new SavedData, packet/protocol change, force-load, background simulation or optional-mod implementation import.

"""
    if marker not in project:
        raise SystemExit("PROJECT 0.51 marker missing")
    project = project.replace(marker, section + marker, 1)
write("PROJECT.md", project)

readme = read("README.md")
if "## 0.52.0-alpha.1 — Ranged Combat Ascension / 원거리 전투 승천" not in readme:
    marker = "## 0.51.0-alpha.1 — Armor Ascension / 방어구 승천 성장\n"
    section = """## 0.52.0-alpha.1 — Ranged Combat Ascension / 원거리 전투 승천
Combat progression now gives bows and crossbows the same physical-scale growth principle as melee combat instead of leaving ranged play as only a numeric damage multiplier. NeoForge common `c:tools/bow` / `c:tools/crossbow` items can enter Ascension Imprint and elite affix drops can roll vanilla Bow/Crossbow bases.

A fired projectile snapshots its ranged affix values and Shift precision flag at launch. Swapping weapons after release cannot change the projectile's direct-damage affix, mastery-XP affix or impact-burst modifiers. Already-snapshotted projectiles keep that launch state when re-entering the level.

Ranged impact burst grows with Combat mastery: Lv30 2.5 blocks/2 targets, Lv60 3.5/4, Lv90 4.25/6, Lv100 5/8, and Lv100 + Field Mastery 6/10 before equipment bonuses. Shift at launch disables the burst for deliberate single-target precision. Ranged affixes are 강궁 damage, 산개 radius, 숙련 Combat XP, 연쇄 target count and 충격 burst fraction. The burst fraction is capped at 65%, and projectile-stored bonuses are clamped to their authored Mythic maxima.

This uses only the actual projectile's persistent NBT; it adds no custom projectile/entity, SavedData, packet/protocol bump, force-load, background combat simulation or optional-mod implementation dependency.

"""
    if marker not in readme:
        raise SystemExit("README 0.51 marker missing")
    readme = readme.replace(marker, section + marker, 1)
write("README.md", readme)

changelog = read("CHANGELOG.md")
if "## 0.52.0-alpha.1" not in changelog:
    marker = "## 0.51.0-alpha.1\n"
    section = """## 0.52.0-alpha.1
- Added `Ranged Combat Ascension / 원거리 전투 승천`: standard NeoForge bow/crossbow tags now join imprint/reforge/Mythic awakening/salvage and elite affix drops.
- Added launch-time projectile snapshots for ranged affix damage, Combat XP, burst radius/targets/fraction and Shift precision, preventing post-shot weapon-swap changes.
- Added mastery-scaled ranged impact bursts: Lv30 2.5/2, Lv60 3.5/4, Lv90 4.25/6, Lv100 5/8, Field Mastery 6/10 base radius/targets.
- Shift-fired ranged shots remain direct single-target precision shots with no impact burst.
- Added ranged affix roles: 강궁 direct damage, 산개 radius, 숙련 Combat XP, 연쇄 target bonus, 충격 burst fraction.
- Hard-capped burst fraction at 65% and clamped persisted projectile affix values to authored maxima; already-snapshotted projectiles are not re-snapshotted on level re-entry.
- Kept network protocol8 and added no custom projectile/entity, SavedData, force-load, background simulation or optional-mod Java dependency.
- Bumped the one-import content-preview lock to `0.52.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

"""
    if marker not in changelog:
        raise SystemExit("CHANGELOG 0.51 marker missing")
    changelog = changelog.replace(marker, section + marker, 1)
write("CHANGELOG.md", changelog)

guide_rel = "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
guide = read(guide_rel)
old_external = 'h("외부 장비와 승천 각인"), p("콘텐츠 팩이나 다른 모드의 장비가 표준 검/곡괭이/도끼/삽/괭이 또는 머리/가슴/다리/발 방어구 태그를 사용하면 장비 메뉴의 승천 각인으로 Survival Ascension affix 체계에 편입할 수 있습니다. 각성 단계는 정예, 전설 단계는 승천, 종말 단계는 신화 등급으로 각인합니다."),'
new_external = 'h("외부 장비와 승천 각인"), p("콘텐츠 팩이나 다른 모드의 장비가 표준 검/활/쇠뇌/곡괭이/도끼/삽/괭이 또는 머리/가슴/다리/발 방어구 태그를 사용하면 장비 메뉴의 승천 각인으로 Survival Ascension affix 체계에 편입할 수 있습니다. 각성 단계는 정예, 전설 단계는 승천, 종말 단계는 신화 등급으로 각인합니다."),'
if old_external not in guide:
    raise SystemExit("Guide external gear marker missing")
guide = guide.replace(old_external, new_external, 1)
armor_line = 'h("방어구 affix"), p("각인 방어구는 착용 중에만 작동합니다. 수호=상시 피해 감소, 불굴=체력 절반 이하 추가 감소, 숙련=전투 숙련 XP 증가, 완강=8 이상 큰 피해 추가 감소, 보호=공격 주체가 없는 피해 추가 감소입니다. 네 부위 합산 affix 피해 감소는 최대35%, 방어구 숙련 XP는 최대32% 추가입니다."),'
ranged_line = 'h("원거리 전투 파급"), p("표준 활/쇠뇌의 발사체는 발사 순간 장비 affix와 Shift 정밀 상태를 기록합니다. 전투 Lv30=2.5블록/2체, Lv60=3.5/4, Lv90=4.25/6, Lv100=5/8, 현장 숙련=6블록/10체의 기본 충돌 파급이 열립니다. Shift 발사는 파급 없는 단일 정밀 타격이며 산개·연쇄·충격 affix가 반경·대상·파급 피해를 제한적으로 확장합니다."),'
if ranged_line not in guide:
    if armor_line not in guide:
        raise SystemExit("Guide armor marker missing")
    guide = guide.replace(armor_line, armor_line + "\n                " + ranged_line, 1)
old_unlock = 'h("승천 각인"), p("M → 장비 → 승천 각인. affix가 없는 표준 검/곡괭이/도끼/삽/괭이 태그 장비를 현재 월드 승천 단계의 정예/승천/신화 등급으로 편입합니다. 외부 모드 장비도 같은 태그를 사용하면 별도 의존성 없이 지원됩니다."),'
new_unlock = 'h("승천 각인"), p("M → 장비 → 승천 각인. affix가 없는 표준 검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 태그 장비를 현재 월드 승천 단계의 정예/승천/신화 등급으로 편입합니다. 외부 모드 장비도 같은 태그를 사용하면 별도 의존성 없이 지원됩니다."),'
if old_unlock not in guide:
    raise SystemExit("Guide imprint unlock marker missing")
guide = guide.replace(old_unlock, new_unlock, 1)
write(guide_rel, guide)

matrix = read("MODPACK_COMPAT_MATRIX.md")
ranged_bullet = "- 원거리 장비: NeoForge 공용 `c:tools/bow` / `c:tools/crossbow` 태그를 쓰는 장비는 승천 각인에 합류한다. 발사체는 발사 순간 Survival affix/Shift 정밀 상태만 자체 persistent NBT에 스냅샷하며, 외부 활/쇠뇌 구현 클래스를 직접 참조하지 않는다.\n"
if ranged_bullet not in matrix:
    marker = "- 전투: Minecraft `Enemy`와 NeoForge 공용 보스 태그를 함께 실제 전투 대상으로 취급한다. 외부 보스가 `Enemy` 구현을 쓰지 않아도 공용 보스 태그를 제공하면 파급/충격파/전투 숙련에 합류한다.\n"
    if marker not in matrix:
        raise SystemExit("compat matrix combat marker missing")
    matrix = matrix.replace(marker, marker + ranged_bullet, 1)
old_summary = "현재는 **태그/타입 기반 통합 + 외부 장비 승천 각인 + BOP 원정 바이옴 브리지 + 데이터 기반 외부 강적 가중치**까지 연결되어 있다."
new_summary = "현재는 **태그/타입 기반 통합 + 외부 장비 승천 각인 + 표준 활/쇠뇌 원거리 affix/발사체 스냅샷 + BOP 원정 바이옴 브리지 + 데이터 기반 외부 강적 가중치**까지 연결되어 있다."
if old_summary not in matrix:
    raise SystemExit("compat matrix summary marker missing")
matrix = matrix.replace(old_summary, new_summary, 1)
write("MODPACK_COMPAT_MATRIX.md", matrix)

release = read("tools/test_release_source.py")
release = release.replace('REQUIRED_VERSION = "0.51.0-alpha.1"', 'REQUIRED_VERSION = "0.52.0-alpha.1"', 1)
release = release.replace(
    'need(reforge, ["검/곡괭이/도끼/삽/괭이/방어구 태그 장비"], "0.51 armor imprint server flow")',
    'need(reforge, ["검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 태그 장비"], "0.52 ranged/armor imprint server flow")',
    1,
)
release = release.replace(
    'need(equipment_ui, ["검/곡괭이/도끼/삽/괭이/방어구 표준 태그 장비 필요"], "0.51 armor imprint UI")',
    'need(equipment_ui, ["검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 표준 태그 장비 필요"], "0.52 ranged/armor imprint UI")',
    1,
)
release = release.replace(
    'need(main_mod, ["VERSION = \\"0.51.0-alpha.1\\"", "armor affix progression"], "0.51 runtime banner")',
    'need(main_mod, ["VERSION = \\"0.52.0-alpha.1\\"", "ranged projectile snapshots/impact bursts"], "0.52 runtime banner")',
    1,
)
source_marker = "# User-facing docs are part of the release contract, not an uncommitted CI-side patch.\n"
source_block = '''# 0.52 ranged combat ascension: launch-time snapshots and bounded physical impact scale.
need(affix, [
    "Tags.Items.TOOLS_BOW", "Tags.Items.TOOLS_CROSSBOW", "Category.RANGED", 'RANGED("ranged")',
    'RANGED_PROJECTILE = "survivalascension_ranged_projectile"',
    "snapshotRangedProjectile(Projectile projectile, ItemStack weapon, boolean precision)",
    "projectileDamageMultiplier", "projectileXpMultiplier", "projectileBurstRadiusBonus", "projectileBurstTargetBonus", "projectileBurstFractionBonus",
    "Math.min(1.25D", "Math.min(1.50D", "Math.min(1.5D", "Math.min(4", "Math.min(0.15D"
], "0.52 ranged affix/projectile snapshot")
need(combat, [
    "onEntityJoin(EntityJoinLevelEvent event)", "AscensionAffixes.isRangedProjectile(projectile)",
    "snapshotRangedProjectile(projectile, weapon, player.isShiftKeyDown())", "tryRangedBurst",
    "AscensionAffixes.isPrecisionRangedProjectile(direct)", "fieldMastery ? 6.0D", "fieldMastery ? 10",
    "Math.min(0.65D", "projectileXpMultiplier(direct)"
], "0.52 ranged combat runtime")
need(main_mod, ["CombatProgression::onEntityJoin", "ranged projectile snapshots/impact bursts"], "0.52 ranged event wiring")
forbid(affix + combat, ["setChunkForced", "addRegionTicket", "getChunk("], "0.52 ranged world-loading policy")

'''
if source_block not in release:
    if source_marker not in release:
        raise SystemExit("release source insertion marker missing")
    release = release.replace(source_marker, source_block + source_marker, 1)
release = release.replace('"Mod version: `0.51.0-alpha.1`",', '"Mod version: `0.52.0-alpha.1`",', 1)
guide_check = '''need(guide, [
    'h("방어구 affix")',
    "최대35%",
    "최대32%"
], "0.51 in-game guide")
'''
docs_block = '''need(project_doc, [
    "## 0.52 Ranged Combat Ascension / 원거리 전투 승천", "6.0 blocks / 10 targets", "hard-capped at 65%"
], "0.52 PROJECT docs")
need(readme, [
    "## 0.52.0-alpha.1 — Ranged Combat Ascension / 원거리 전투 승천", "Lv100 + Field Mastery 6/10", "capped at 65%"
], "0.52 README docs")
need(changelog, ["## 0.52.0-alpha.1", "Ranged Combat Ascension / 원거리 전투 승천", "6/10", "65%"], "0.52 CHANGELOG docs")
need(guide, ['h("원거리 전투 파급")', "현장 숙련=6블록/10체", "Shift 발사는 파급 없는 단일 정밀 타격"], "0.52 in-game guide")
'''
if docs_block not in release:
    if guide_check not in release:
        raise SystemExit("release source guide marker missing")
    release = release.replace(guide_check, guide_check + docs_block, 1)
baseline_line = 'baseline = baseline.replace("MAX_DEPOTS_PER_PLAYER = 3", "MAX_DEPOTS_PER_PLAYER = 9")\n'
baseline_rewrite = 'baseline = baseline.replace("표준 검/곡괭이/도끼/삽/괭이 태그 장비", "표준 검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 태그 장비")\n'
if baseline_rewrite not in release:
    if baseline_line not in release:
        raise SystemExit("release source baseline marker missing")
    release = release.replace(baseline_line, baseline_line + baseline_rewrite, 1)
release = release.replace(
    'print("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.51")',
    'print("- 0.52 ranged launch snapshots prevent post-shot gear swapping; precision/burst scale and persisted modifiers are bounded")\nprint("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.52")',
    1,
)
write("tools/test_release_source.py", release)

content_audit = read("tools/test_release_content_pack.py")
content_audit = content_audit.replace(
    'REQUIRED_LOCK_VERSION = "0.51.0-alpha.1-content-preview.1"',
    'REQUIRED_LOCK_VERSION = "0.52.0-alpha.1-content-preview.1"',
    1,
)
content_marker = 'need(combat, ["armorDamageMultiplier", "armorXpMultiplier"], "0.51 armor runtime flow")\n'
content_block = '''need(affix, ["Tags.Items.TOOLS_BOW", "Tags.Items.TOOLS_CROSSBOW", "Category.RANGED", "snapshotRangedProjectile", "projectileDamageMultiplier", "projectileXpMultiplier"], "0.52 ranged content-pack bridge")
need(equipment_ui, ["검/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구 표준 태그 장비 필요"], "0.52 ranged imprint player flow")
need(combat, ["onEntityJoin(EntityJoinLevelEvent event)", "tryRangedBurst", "isPrecisionRangedProjectile", "fieldMastery ? 6.0D", "fieldMastery ? 10"], "0.52 ranged runtime flow")
matrix = read("MODPACK_COMPAT_MATRIX.md")
need(matrix, ["c:tools/bow", "c:tools/crossbow", "발사체 스냅샷"], "0.52 generic ranged compatibility docs")
'''
if content_block not in content_audit:
    if content_marker not in content_audit:
        raise SystemExit("release content marker missing")
    content_audit = content_audit.replace(content_marker, content_marker + content_block, 1)
content_audit = content_audit.replace("'Mod version: `0.51.0-alpha.1`'", "'Mod version: `0.52.0-alpha.1`'", 1)
content_audit = content_audit.replace(
    'print("armor_affix_content_bridge=PASS")',
    'print("armor_affix_content_bridge=PASS")\nprint("ranged_affix_projectile_bridge=PASS")',
    1,
)
write("tools/test_release_content_pack.py", content_audit)

verifier = read("tools/verify_release_jar.py")
verifier_marker = 'print("frontline_freight_manifest_runtime=present")\n'
verifier_block = '''    for token in [b"TOOLS_BOW", b"TOOLS_CROSSBOW", b"snapshotRangedProjectile", b"survivalascension_ranged_projectile", b"projectileDamageMultiplier", b"projectileXpMultiplier"]:
        if token not in affix:
            raise SystemExit(f"0.52 compiled ranged-affix token missing: {token!r}")
    for token in [b"onEntityJoin", b"tryRangedBurst", b"isPrecisionRangedProjectile", b"projectileBurstRadiusBonus", b"projectileBurstTargetBonus"]:
        if token not in combat:
            raise SystemExit(f"0.52 compiled ranged-combat token missing: {token!r}")

'''
if "0.52 compiled ranged-affix token missing" not in verifier:
    if verifier_marker not in verifier:
        raise SystemExit("release JAR verifier marker missing")
    verifier = verifier.replace(verifier_marker, verifier_block + verifier_marker, 1)
verifier = verifier.replace(
    'print("armor_affix_release_verify=PASS")',
    'print("armor_affix_release_verify=PASS")\nprint("ranged_combat_runtime=present")\nprint("ranged_combat_release_verify=PASS")',
    1,
)
write("tools/verify_release_jar.py", verifier)

print("Survival Ascension 0.52 release state synchronized")
