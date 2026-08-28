#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
if len(sys.argv) != 2:
    raise SystemExit("usage: verify_release_jar.py <jar>")
jar = Path(sys.argv[1]).resolve()

subprocess.run([sys.executable, str(ROOT / "tools/verify_release_jar_pre_korean.py"), str(jar)], check=True)
subprocess.run([sys.executable, str(ROOT / "tools/verify_korean_runtime_pack.py"), str(jar)], check=True)

with zipfile.ZipFile(jar) as zf:
    final_gate = "kr/moonseungjun/survivalascension/endgame/FinalAscensionProgression.class"
    final_system = "kr/moonseungjun/survivalascension/endgame/FinalAscensionSystem.class"
    final_boss = "kr/moonseungjun/survivalascension/endgame/FinalAscensionBossSystem.class"
    final_data = "kr/moonseungjun/survivalascension/endgame/FinalAscensionData.class"
    mobility = "kr/moonseungjun/survivalascension/mobility/MobilityProgression.class"
    construction = "kr/moonseungjun/survivalascension/construction/ConstructionProgression.class"
    elite = "kr/moonseungjun/survivalascension/elite/EliteMobSystem.class"
    infrastructure = "kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.class"
    menu = "kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.class"
    main = "kr/moonseungjun/survivalascension/SurvivalAscension.class"
    for name in [final_gate, final_system, final_boss, final_data, mobility, construction, elite, infrastructure, menu, main]:
        if name not in zf.namelist():
            raise SystemExit(f"0.61 final ascension runtime class missing: {name}")

    compiled_gate = zf.read(final_gate)
    for token in [b"REQUIRED_EXPEDITIONS", b"REQUIRED_APEX", b"missingRequirements", b"isReady", b"FinalAscensionData"]:
        if token not in compiled_gate:
            raise SystemExit(f"final ascension compiled gate token missing: {token!r}")

    compiled_system = zf.read(final_system)
    for token in [
        b"ACT1_MINING", b"ACT1_BUILD", b"ACT1_COMBAT", b"ACT1_MOVE",
        b"ACT2_SET_ONE", b"ACT2_SET_TWO", b"ACT2_SET_THREE",
        b"ACT3_SEAL_ONE", b"ACT3_SEAL_TWO", b"ACT3_SEAL_THREE",
        b"survivalascension_final_ascension_owner", b"FinalAscensionBossSystem"
    ]:
        if token not in compiled_system:
            raise SystemExit(f"final ascension compiled acts token missing: {token!r}")

    compiled_boss = zf.read(final_boss)
    for token in [
        b"survivalascension_final_boss", b"survivalascension_final_boss_owner",
        b"minecraft:warden", b"OPENING", b"ANCHORS", b"BREAKTHROUGH", b"FINAL",
        b"LINE", b"RING", b"MARKED", b"renderTelegraph", b"insideLine",
        b"FinalAscensionData", b"createEliteDrop", b"awaken",
        b"maintainBossAggro", b"increaseAngerAt", b"setAttackTarget", b"WARDEN_AGGRO_REFRESH_TICKS"
    ]:
        if token not in compiled_boss:
            raise SystemExit(f"0.61 compiled final boss token missing: {token!r}")

    compiled_data = zf.read(final_data)
    for token in [b"final_ascension_v1", b"complete", b"first_conqueror", b"completed_game_time", b"setDirty"]:
        if token not in compiled_data:
            raise SystemExit(f"0.61 compiled final completion token missing: {token!r}")

    compiled_mobility = zf.read(mobility)
    for token in [b"FinalAscensionData", b"finalAscensionComplete", b"maxAirDashes"]:
        if token not in compiled_mobility:
            raise SystemExit(f"0.61 compiled final mobility token missing: {token!r}")

    compiled_construction = zf.read(construction)
    for token in [b"FinalAscensionData", b"finalAscensionComplete", b"CONSTRUCTION_LENGTHS", b"maxUnlockedLength"]:
        if token not in compiled_construction:
            raise SystemExit(f"0.61 compiled final construction token missing: {token!r}")

    compiled_elite = zf.read(elite)
    if b"isInternalSpawn" not in compiled_elite:
        raise SystemExit("0.61 final boss elite-isolation guard missing from packaged runtime")

    compiled_infrastructure = zf.read(infrastructure)
    if b"final_ascension" not in compiled_infrastructure:
        raise SystemExit("final ascension server action missing from packaged infrastructure service")

    compiled_menu = zf.read(menu)
    if "최후의 승천".encode("utf-8") not in compiled_menu:
        raise SystemExit("final ascension menu label missing from packaged client runtime")

    compiled_main = zf.read(main)
    for token in [b"0.61.0-alpha.1", b"three-phase Final Ascension boundary boss", b"FinalAscensionBossSystem"]:
        if token not in compiled_main:
            raise SystemExit(f"0.61 compiled release identity/wiring token missing: {token!r}")

print("final_ascension_canonical_gate_runtime=present")
print("final_ascension_acts_1_3_runtime=present")
print("final_boundary_boss_runtime=present")
print("final_boundary_warden_aggro_runtime=present")
print("final_completion_saved_data_runtime=present")
print("final_mobility_construction_authority_runtime=present")
print("final_ascension_menu_action_runtime=present")
print("survival_ascension_release_verify=PASS")
