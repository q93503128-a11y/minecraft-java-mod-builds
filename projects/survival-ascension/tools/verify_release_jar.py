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

    lang_name = "assets/amethyst_resonance/lang/ko_kr.json"
    if lang_name not in zf.namelist():
        raise SystemExit(f"0.59.1 packaged Korean integration resource missing: {lang_name}")
    lang = json.loads(zf.read(lang_name).decode("utf-8"))
    expected_localization = {
        "item.amethyst_resonance.resonant_helmet": "공명 투구",
        "tooltip.amethyst_resonance.armor": "착용 시 스컬크가 감지하는 진동을 억제합니다",
        "tooltip.amethyst_resonance.helmet_gaze": "엔더맨과 눈이 마주쳐도 적대하지 않습니다",
        "tooltip.amethyst_resonance.warden": "워든의 분노가 더 천천히 쌓입니다",
        "tooltip.amethyst_resonance.silent_mining": "채굴해도 스컬크를 자극하지 않습니다",
        "item.amethyst_resonance.resonance_upgrade_smithing_template": "공명 강화 대장장이 형판",
    }
    for key, value in expected_localization.items():
        if lang.get(key) != value:
            raise SystemExit(f"0.59.1 packaged Korean localization mismatch: {key}")

print("apex_optional_escort_runtime=present")
print("apex_optional_escort_tags=present")
print("amethyst_resonance_korean_localization=present")
print("apex_optional_escort_release_verify=PASS")
