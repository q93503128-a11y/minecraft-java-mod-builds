#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit("usage: verify_korean_runtime_pack.py <jar>")
jar = Path(sys.argv[1]).resolve()

TBOS_PACKS = [f"korean_tbos_{index}" for index in range(1, 7)]
MISC_PACK = "korean_external_misc"
BASE = "resourcepacks"

with zipfile.ZipFile(jar) as zf:
    names = set(zf.namelist())

    client_class = "kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.class"
    if client_class not in names:
        raise SystemExit(f"Korean runtime client class missing: {client_class}")
    client_bytes = zf.read(client_class)
    for token in [b"korean_tbos_1", b"korean_tbos_6", b"korean_external_misc", b"resourcepacks/"]:
        if token not in client_bytes:
            raise SystemExit(f"Korean runtime pack registration token missing: {token!r}")

    restore_class = "kr/moonseungjun/survivalascension/compat/TbsJournalRestorationService.class"
    if restore_class not in names:
        raise SystemExit(f"TBS journal restoration class missing: {restore_class}")
    restore_bytes = zf.read(restore_class)
    for token in [b"tbos", b"archivists_journal", b"survivalascension.tbs_journal_restored_v1"]:
        if token not in restore_bytes:
            raise SystemExit(f"TBS journal restoration token missing: {token!r}")

    for pack in [*TBOS_PACKS, MISC_PACK]:
        meta = f"{BASE}/{pack}/pack.mcmeta"
        if meta not in names:
            raise SystemExit(f"forced Korean pack metadata missing: {meta}")
        parsed = json.loads(zf.read(meta).decode("utf-8"))
        pack_meta = parsed.get("pack", {})
        if "pack_format" in pack_meta:
            raise SystemExit(f"legacy pack_format must not be used for modern 26.2 forced pack: {meta}")
        if pack_meta.get("min_format") != [88, 0] or pack_meta.get("max_format") != [88, 0]:
            raise SystemExit(f"forced Korean modern min/max format mismatch: {meta}")

    merged_tbos: dict[str, str] = {}
    for pack in TBOS_PACKS:
        path = f"{BASE}/{pack}/assets/tbos/lang/ko_kr.json"
        if path not in names:
            raise SystemExit(f"forced TBS Korean fragment missing: {path}")
        fragment = json.loads(zf.read(path).decode("utf-8"))
        overlap = set(merged_tbos).intersection(fragment)
        if overlap:
            raise SystemExit(f"duplicate TBS Korean keys across forced packs: {sorted(overlap)[:5]}")
        merged_tbos.update(fragment)

    if len(merged_tbos) != 507:
        raise SystemExit(f"forced TBS Korean key count mismatch: {len(merged_tbos)} != 507")

    expected_tbos = {
        "message.tbos.onboarding.welcome": "이 세계의 무언가가 끝나지 않았던 어느 하루를 기억하고 있습니다.",
        "message.tbos.onboarding.journal": "기록관의 일지가 인벤토리에 추가되었습니다. 어디서 시작해야 할지 읽어 보세요.",
        "message.tbos.onboarding.shrines": "먼저 균열 성소를 찾아보세요. 이 세계에는 스폰 지점에서 %s~%s블록 떨어진 고정 위치에 세 곳이 있습니다.",
        "item.tbos.archivists_journal": "기록관의 일지",
        "item.tbos.memory_plate.named": "기억 판: %s",
        "item.tbos.memory_plate.tooltip": "기억 등불에 사용하면 이 장면을 불러옵니다. 기억 판은 소모되지 않습니다.",
        "memory_scene.tbos.celestial_family.title": "합 아래의 가족",
        "memory_scene.tbos.celestial_family.description": "세 사람이 기록보관소 지붕을 가로지르는 붙잡힌 별을 바라봅니다. 그들은 직원이 아닌 방문객이었지만, 기록은 그들마저 남겼습니다.",
        "floor.tbos.name.0": "시차의 각성",
        "journal.tbos.quest.4.description": "탐사 지도를 따라 자오선 기록보관소를 찾아 문턱을 넘으세요.",
        "tome.tbos.page.0": "혼천의는 장식이 아니었습니다. 우리는 살아 움직이는 고리를 정해진 합으로 이끌고, 지나가는 모든 궤도를 기록했습니다. 회상했을 때도 고리가 움직인다면 아직 일이 끝나지 않은 것입니다.",
    }
    for key, value in expected_tbos.items():
        if merged_tbos.get(key) != value:
            raise SystemExit(f"forced TBS Korean localization mismatch: {key}")

    misc_prefix = f"{BASE}/{MISC_PACK}/assets"
    misc_files = {
        "amethyst": f"{misc_prefix}/amethyst_resonance/lang/ko_kr.json",
        "bop": f"{misc_prefix}/biomesoplenty/lang/ko_kr.json",
        "terrablender": f"{misc_prefix}/terrablender/lang/ko_kr.json",
    }
    for label, path in misc_files.items():
        if path not in names:
            raise SystemExit(f"forced {label} Korean resource missing: {path}")

    amethyst = json.loads(zf.read(misc_files["amethyst"]).decode("utf-8"))
    if len(amethyst) != 31:
        raise SystemExit(f"forced Amethyst Korean key count mismatch: {len(amethyst)} != 31")
    expected_amethyst = {
        "item.amethyst_resonance.resonant_pickaxe": "공명 곡괭이",
        "tooltip.amethyst_resonance.tool": "수정 계열 블록과 공명해 채굴 효율이 높아집니다",
        "tooltip.amethyst_resonance.silent_mining": "채굴해도 스컬크를 자극하지 않습니다",
        "item.amethyst_resonance.resonant_helmet": "공명 투구",
        "tooltip.amethyst_resonance.helmet_gaze": "엔더맨과 눈이 마주쳐도 적대하지 않습니다",
    }
    for key, value in expected_amethyst.items():
        if amethyst.get(key) != value:
            raise SystemExit(f"forced Amethyst Korean localization mismatch: {key}")

    bop = json.loads(zf.read(misc_files["bop"]).decode("utf-8"))
    if len(bop) != 36:
        raise SystemExit(f"forced BOP Korean gap key count mismatch: {len(bop)} != 36")
    if bop.get("tag.item.biomesoplenty.redwood_logs") != "삼나무 원목":
        raise SystemExit("forced BOP Korean gap overlay mismatch")

    terrablender = json.loads(zf.read(misc_files["terrablender"]).decode("utf-8"))
    if len(terrablender) != 2:
        raise SystemExit(f"forced TerraBlender Korean key count mismatch: {len(terrablender)} != 2")

print("forced_korean_builtin_pack_registration=present")
print("forced_korean_pack_metadata=modern_88_0")
print("forced_tbos_korean_keys=507/507")
print("forced_tbos_onboarding_memory_journal=present")
print("tbs_journal_one_time_restoration=present")
print("forced_amethyst_tooltips=present")
print("forced_bop_terrablender_overlay=present")
print("KOREAN RUNTIME PACK VERIFY PASS")
