#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
CURRENT_LOCK_VERSION = "0.61.0-alpha.1-content-preview.1"
PREVIOUS_DOC_VERSION = "0.59.0-alpha.1"


def read(rel: str) -> str:
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")


def need(text: str, needles: list[str], label: str) -> None:
    for needle in needles:
        if needle not in text:
            errors.append(f"{label} missing: {needle}")


# Preserve the complete 0.58 content-pack regression contract while advancing the locked pack
# identity. PROJECT remains the historical 0.59 long-form baseline.
legacy_path = ROOT / "tools/test_release_content_pack_058.py"
legacy = legacy_path.read_text(encoding="utf-8")
legacy = legacy.replace('REQUIRED_LOCK_VERSION = "0.58.0-alpha.1-content-preview.1"',
                        f'REQUIRED_LOCK_VERSION = "{CURRENT_LOCK_VERSION}"')
legacy = legacy.replace("'Mod version: `0.48.0-alpha.1`', 'Mod version: `0.58.0-alpha.1`'",
                        f"'Mod version: `0.48.0-alpha.1`', 'Mod version: `{PREVIOUS_DOC_VERSION}`'")
namespace = {"__file__": str(legacy_path), "__name__": "__main__"}
buffer = io.StringIO()
exit_code = 0
try:
    with contextlib.redirect_stdout(buffer):
        exec(compile(legacy, str(legacy_path), "exec"), namespace)
except (SystemExit, AssertionError) as exc:
    exit_code = int(exc.code or 0) if isinstance(exc, SystemExit) else 1
    if not isinstance(exc, SystemExit):
        print(f"0.58 content-pack regression assertion: {exc}", file=sys.stderr)
print(buffer.getvalue(), end="")
if exit_code != 0:
    print("RELEASE CONTENT-PACK AUDIT FAIL: 0.58 regression contract failed under 0.61 pack identity")
    sys.exit(exit_code)

bridge = read("src/main/java/kr/moonseungjun/survivalascension/compat/ApexContentPackBridge.java")
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
compat = read("src/main/java/kr/moonseungjun/survivalascension/compat/ContentPackCompatibility.java")
apex0 = read("src/main/resources/data/survivalascension/tags/entity_type/apex_escorts_tier_0.json")
apex1 = read("src/main/resources/data/survivalascension/tags/entity_type/apex_escorts_tier_1.json")
apex2 = read("src/main/resources/data/survivalascension/tags/entity_type/apex_escorts_tier_2.json")
exp2 = read("src/main/resources/data/survivalascension/tags/entity_type/expedition_reinforcements_tier_2.json")
resonance = read("src/main/resources/data/survivalascension/tags/item/expedition_resonance_rewards.json")
amethyst_ko = read("src/main/resources/assets/amethyst_resonance/lang/ko_kr.json")
tbos_ko = read("src/main/resources/assets/tbos/lang/ko_kr.json")
bop_ko = read("src/main/resources/assets/biomesoplenty/lang/ko_kr.json")
terrablender_ko = read("src/main/resources/assets/terrablender/lang/ko_kr.json")
lock = read("modpack/content-lock.json")
release_061 = read("RELEASE-0.61.0-alpha.1.md")

need(lock, [f'"version": "{CURRENT_LOCK_VERSION}"', '"The Birth of Steve"', '"Amethyst Resonance"', '"Biomes O\' Plenty"'],
     "0.61 locked content identity")
need(bridge, [
    "APEX_ESCORTS_TIER_0", "APEX_ESCORTS_TIER_1", "APEX_ESCORTS_TIER_2",
    "randomEscortId", "escortIds", "apex_escort_tier_", "Tags.EntityTypes.BOSSES"
], "0.59 data-driven Apex escort bridge")
need(apex, [
    "ApexContentPackBridge.randomEscortId", "archetype.aquatic() ? null", "packSlot",
    "if (escort == null && packEscort)", "escort.setGlowingTag(true)", "hunt.packEscortCount++",
    "이변 호위 1체 포함"
], "0.59 bounded Apex escort replacement runtime")
need(apex0, ["tbos:armillary_scout", '"required": false'], "0.59 stage-0 Apex escort allowlist")
need(apex1, ["tbos:armillary_scout", "tbos:blank_chronist", '"required": false'], "0.59 stage-1 Apex escort allowlist")
need(apex2, ["tbos:blank_chronist", "tbos:gnomon_knight", '"required": false'], "0.59 stage-2 Apex escort allowlist")
if "tbos:minotaur" in apex0 + apex1 + apex2:
    errors.append("0.59 Apex escort safety: tbos:minotaur must stay out of mixed Apex allowlists")
for forbidden in ("tbos:hour_cantor", "tbos:phoenix_guardian"):
    if forbidden in apex0 + apex1 + apex2:
        errors.append(f"0.59 Apex escort safety: boss {forbidden} must stay out of escort allowlists")

need(exp2, [
    "tbos:parallax_wraith", "tbos:shard_drifter", "tbos:wake_cutter", "tbos:memory_leech",
    "tbos:prism_stalker", "tbos:null_portrait", "tbos:meridian_sentinel", "tbos:hour_hand_wraith",
    '"required": false'
], "0.59.1 expanded TBS expedition pool")
need(resonance, [
    "amethyst_resonance:resonant_pickaxe", "amethyst_resonance:resonant_axe",
    "amethyst_resonance:resonant_shovel", "amethyst_resonance:resonant_hoe",
    "amethyst_resonance:resonant_sword", "amethyst_resonance:resonant_helmet",
    "amethyst_resonance:resonant_chestplate", "amethyst_resonance:resonant_leggings",
    "amethyst_resonance:resonant_boots", '"required":false'
], "0.59.1 Resonance reward pool")
need(compat, ["randomIncidentReinforcementId", "resonanceOperationRewardIds", "censusLines"],
     "0.59.1 runtime registry/tag compatibility")

need(amethyst_ko, [
    '"item.amethyst_resonance.resonant_crystal": "공명 수정"',
    '"item.amethyst_resonance.resonant_sword": "공명 검"',
    '"item.amethyst_resonance.resonant_pickaxe": "공명 곡괭이"',
    '"item.amethyst_resonance.resonant_axe": "공명 도끼"',
    '"item.amethyst_resonance.resonant_shovel": "공명 삽"',
    '"item.amethyst_resonance.resonant_hoe": "공명 괭이"',
    '"item.amethyst_resonance.resonant_helmet": "공명 투구"',
    '"item.amethyst_resonance.resonant_chestplate": "공명 흉갑"',
    '"item.amethyst_resonance.resonant_leggings": "공명 각반"',
    '"item.amethyst_resonance.resonant_boots": "공명 장화"',
    '"tooltip.amethyst_resonance.armor": "착용 시 스컬크가 감지하는 진동을 억제합니다"',
    '"tooltip.amethyst_resonance.helmet_gaze": "엔더맨과 눈이 마주쳐도 적대하지 않습니다"',
    '"tooltip.amethyst_resonance.warden": "워든의 분노가 더 천천히 쌓입니다"',
    '"tooltip.amethyst_resonance.tool": "수정 계열 블록과 공명해 채굴 효율이 높아집니다"',
    '"tooltip.amethyst_resonance.silent_mining": "채굴해도 스컬크를 자극하지 않습니다"',
    '"tooltip.amethyst_resonance.infused": "공명 주입됨 ✦"',
    '"item.amethyst_resonance.resonance_upgrade_smithing_template": "공명 강화 대장장이 형판"'
], "0.59.1 Amethyst Resonance Korean localization")

need(tbos_ko, [
    '"itemGroup.tbos.yesterglass": "스티브의 탄생"',
    '"block.tbos.archive_stone": "기록보관소 석재"',
    '"block.tbos.yesterglass": "예스터글라스"',
    '"block.tbos.cantor_gate": "기록보관소 보스 관문"',
    '"item.tbos.cracked_yesterglass_lens": "금 간 예스터글라스 렌즈"',
    '"item.tbos.archivists_journal": "기록관의 일지"',
    '"item.tbos.memory_plate.tooltip": "기억 등불에 사용하면 이 장면을 불러옵니다. 기억 판은 소모되지 않습니다."',
    '"entity.tbos.parallax_wraith": "시차 망령"',
    '"entity.tbos.hour_cantor": "시간의 칸토르"',
    '"entity.tbos.phoenix_guardian": "최후의 큐레이터"',
    '"pickup.tbos.key": "기록보관소 열쇠"',
    '"boss.tbos.last_curator.title": "최후의 큐레이터"'
], "0.59.1 TBS Korean inventory localization")

need(bop_ko, [
    '"biome.biomesoplenty.subtropics": "아열대"',
    '"block.biomesoplenty.chiseled_orpiment": "조각된 유황"',
    '"block.biomesoplenty.sphalerite_bricks": "가열된 방해석 벽돌"',
    '"tag.item.biomesoplenty.fir_logs": "전나무 원목"',
    '"tag.item.biomesoplenty.redwood_logs": "삼나무 원목"',
    '"tag.item.biomesoplenty.willow_logs": "버드나무 원목"'
], "0.59.1 BOP Korean gap overlay")

need(terrablender_ko, [
    '"commands.terrablender.biomeparams.success": "생물군계 매개변수 정보를 생성했습니다."',
    '"commands.terrablender.biomeparams.failed": "생물군계 매개변수 정보를 생성하지 못했습니다."'
], "0.59.1 TerraBlender Korean overlay")

# 0.61 changes no external content file. The lock identity advances only with the Survival release.
need(release_061, [
    "external project/version IDs remain unchanged", "0.61.0-alpha.1-content-preview.1", "Network protocol 9"
], "0.61 content-pack release note")

if errors:
    print("RELEASE CONTENT-PACK AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("apex_optional_escort_replacement_bridge=PASS")
print("expanded_tbs_expedition_pool=PASS")
print("resonance_nine_slot_reward_pool=PASS")
print("amethyst_resonance_korean_localization=PASS")
print("tbos_inventory_korean_localization=PASS")
print("biomesoplenty_korean_gap_overlay=PASS")
print("terrablender_korean_command_overlay=PASS")
print("content_pack_external_versions_unchanged=PASS")
print("RELEASE CONTENT-PACK AUDIT PASS")
