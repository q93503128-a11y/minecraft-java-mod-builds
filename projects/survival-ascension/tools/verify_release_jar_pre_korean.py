#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
if len(sys.argv) != 2:
    raise SystemExit("usage: verify_release_jar.py <jar>")
jar = Path(sys.argv[1]).resolve()

# Preserve every 0.58 packaged-JAR regression check, then verify the 0.59 delta.
subprocess.run([sys.executable, str(ROOT / "tools/verify_release_jar_058.py"), str(jar)], check=True)

with zipfile.ZipFile(jar) as zf:
    bridge_name = "kr/moonseungjun/survivalascension/compat/ApexContentPackBridge.class"
    apex_name = "kr/moonseungjun/survivalascension/apex/ApexHuntSystem.class"
    main_name = "kr/moonseungjun/survivalascension/SurvivalAscension.class"
    for name in [bridge_name, apex_name, main_name]:
        if name not in zf.namelist():
            raise SystemExit(f"0.59 Apex content runtime class missing: {name}")

    bridge = zf.read(bridge_name)
    apex = zf.read(apex_name)
    main = zf.read(main_name)
    for token in [b"apex_escorts_tier_0", b"apex_escorts_tier_1", b"apex_escorts_tier_2", b"randomEscortId", b"escortIds", b"apex_escort_tier_"]:
        if token not in bridge:
            raise SystemExit(f"0.59 compiled Apex bridge token missing: {token!r}")
    for token in [b"randomEscortId", b"packEscortCount", b"setGlowingTag"]:
        if token not in apex:
            raise SystemExit(f"0.59 compiled Apex hunt integration token missing: {token!r}")
    if b"data-driven Apex content escorts" not in main:
        raise SystemExit("0.59 runtime banner missing Apex content escort integration")

    tags = [
        "data/survivalascension/tags/entity_type/apex_escorts_tier_0.json",
        "data/survivalascension/tags/entity_type/apex_escorts_tier_1.json",
        "data/survivalascension/tags/entity_type/apex_escorts_tier_2.json",
    ]
    for name in tags:
        if name not in zf.namelist():
            raise SystemExit(f"0.59 packaged Apex escort tag missing: {name}")
    combined = b"\n".join(zf.read(name) for name in tags)
    for token in [b"tbos:armillary_scout", b"tbos:blank_chronist", b"tbos:gnomon_knight", b'"required": false']:
        if token not in combined:
            raise SystemExit(f"0.59 packaged Apex escort allowlist token missing: {token!r}")
    for forbidden in [b"tbos:minotaur", b"tbos:hour_cantor", b"tbos:phoenix_guardian"]:
        if forbidden in combined:
            raise SystemExit(f"0.59 unsafe Apex escort entry packaged: {forbidden!r}")

    amethyst_name = "assets/amethyst_resonance/lang/ko_kr.json"
    if amethyst_name not in zf.namelist():
        raise SystemExit(f"0.59.1 packaged Korean integration resource missing: {amethyst_name}")
    amethyst = json.loads(zf.read(amethyst_name).decode("utf-8"))
    if len(amethyst) != 31:
        raise SystemExit(f"0.59.1 Amethyst Korean key count mismatch: {len(amethyst)} != 31")
    expected_amethyst = {
        "item.amethyst_resonance.resonant_helmet": "공명 투구",
        "tooltip.amethyst_resonance.armor": "착용 시 스컬크가 감지하는 진동을 억제합니다",
        "tooltip.amethyst_resonance.helmet_gaze": "엔더맨과 눈이 마주쳐도 적대하지 않습니다",
        "tooltip.amethyst_resonance.warden": "워든의 분노가 더 천천히 쌓입니다",
        "tooltip.amethyst_resonance.silent_mining": "채굴해도 스컬크를 자극하지 않습니다",
        "item.amethyst_resonance.resonance_upgrade_smithing_template": "공명 강화 대장장이 형판",
    }
    for key, value in expected_amethyst.items():
        if amethyst.get(key) != value:
            raise SystemExit(f"0.59.1 packaged Amethyst Korean localization mismatch: {key}")

    tbos_name = "assets/tbos/lang/ko_kr.json"
    if tbos_name not in zf.namelist():
        raise SystemExit(f"0.59.1 packaged Korean integration resource missing: {tbos_name}")
    tbos = json.loads(zf.read(tbos_name).decode("utf-8"))
    expected_prefix_counts = {
        "itemGroup.": 1,
        "block.": 118,
        "item.": 21,
        "entity.": 27,
        "pickup.": 6,
        "boss.": 7,
    }
    if len(tbos) != sum(expected_prefix_counts.values()):
        raise SystemExit(f"0.59.1 TBS Korean inventory key count mismatch: {len(tbos)} != {sum(expected_prefix_counts.values())}")
    for prefix, expected in expected_prefix_counts.items():
        actual = sum(1 for key in tbos if key.startswith(prefix))
        if actual != expected:
            raise SystemExit(f"0.59.1 TBS Korean {prefix} key count mismatch: {actual} != {expected}")
    expected_tbos = {
        "itemGroup.tbos.yesterglass": "스티브의 탄생",
        "block.tbos.yesterglass": "예스터글라스",
        "block.tbos.cantor_gate": "기록보관소 보스 관문",
        "item.tbos.cracked_yesterglass_lens": "금 간 예스터글라스 렌즈",
        "item.tbos.archivists_journal": "기록관의 일지",
        "item.tbos.memory_plate.tooltip": "기억 등불에 사용하면 이 장면을 불러옵니다. 기억 판은 소모되지 않습니다.",
        "entity.tbos.parallax_wraith": "시차 망령",
        "entity.tbos.hour_cantor": "시간의 칸토르",
        "entity.tbos.phoenix_guardian": "최후의 큐레이터",
        "pickup.tbos.key": "기록보관소 열쇠",
        "boss.tbos.last_curator.title": "최후의 큐레이터",
    }
    for key, value in expected_tbos.items():
        if tbos.get(key) != value:
            raise SystemExit(f"0.59.1 packaged TBS Korean localization mismatch: {key}")

    bop_name = "assets/biomesoplenty/lang/ko_kr.json"
    if bop_name not in zf.namelist():
        raise SystemExit(f"0.59.1 packaged Korean integration resource missing: {bop_name}")
    bop = json.loads(zf.read(bop_name).decode("utf-8"))
    if len(bop) != 36:
        raise SystemExit(f"0.59.1 BOP Korean overlay key count mismatch: {len(bop)} != 36")
    expected_bop = {
        "biome.biomesoplenty.subtropics": "아열대",
        "block.biomesoplenty.chiseled_orpiment": "조각된 유황",
        "block.biomesoplenty.sphalerite_bricks": "가열된 방해석 벽돌",
        "tag.item.biomesoplenty.fir_logs": "전나무 원목",
        "tag.item.biomesoplenty.redwood_logs": "삼나무 원목",
    }
    for key, value in expected_bop.items():
        if bop.get(key) != value:
            raise SystemExit(f"0.59.1 packaged BOP Korean overlay mismatch: {key}")

    terrablender_name = "assets/terrablender/lang/ko_kr.json"
    if terrablender_name not in zf.namelist():
        raise SystemExit(f"0.59.1 packaged Korean integration resource missing: {terrablender_name}")
    terrablender = json.loads(zf.read(terrablender_name).decode("utf-8"))
    if len(terrablender) != 2:
        raise SystemExit(f"0.59.1 TerraBlender Korean overlay key count mismatch: {len(terrablender)} != 2")

print("apex_optional_escort_runtime=present")
print("apex_optional_escort_tags=present")
print("amethyst_resonance_korean_localization=present")
print("tbos_inventory_korean_localization=present")
print("biomesoplenty_korean_gap_overlay=present")
print("terrablender_korean_command_overlay=present")
print("apex_optional_escort_release_verify=PASS")
