#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"

catalog = (JAVA / "magic/SpellCatalog.java").read_text(encoding="utf-8")
data = (JAVA / "magic/MagicPlayerData.java").read_text(encoding="utf-8")
casting = (JAVA / "magic/SpellCastingService.java").read_text(encoding="utf-8")
network = (JAVA / "network/ArcaneNetwork.java").read_text(encoding="utf-8")
client = (JAVA / "client/ArcaneClient.java").read_text(encoding="utf-8")
screen = (JAVA / "client/GrimoireScreen.java").read_text(encoding="utf-8")

for token in [
    "fusionFor(state.focus, state.weave)",
    "prepareFusion",
    "masteryRequired",
    "state.known.add(resultId)",
    "new MasteryProgress(true, registered",
]:
    if token not in data:
        raise SystemExit(f"missing live fusion/mastery contract: {token}")

for token in ["cast(ServerPlayer player, boolean fusion)", "cast.fusion()", "prelude(", "sigil(", "dualSpiral("]:
    if token not in casting:
        raise SystemExit(f"missing casting/visual contract: {token}")

for token in ["InputConstants.KEY_G", "InputConstants.KEY_V", "InputConstants.KEY_B"]:
    if token not in client:
        raise SystemExit(f"missing dual-circuit hotkey: {token}")

for token in ["주문 성좌", "융합 각인", "실전 숙련", "sockets(", "masteryNode(", "core("]:
    if token not in screen:
        raise SystemExit(f"missing astral grimoire UI: {token}")

for forbidden in ["FuseSpellPayload", "handleFuse", "연구 완료"]:
    if forbidden in network or forbidden in screen:
        raise SystemExit(f"obsolete click-research contract remains: {forbidden}")

for result, first, second in [
    ("flame_lance", "arcane_dart", "ember"),
    ("ice_shackles", "frost_needle", "lesser_ward"),
    ("wind_blade", "gale_step", "arcane_dart"),
    ("fireball", "flame_lance", "ember"),
    ("frost_nova", "ice_shackles", "frost_needle"),
    ("chain_bolt", "wind_blade", "arcane_dart"),
    ("rift_step", "blink", "gale_step"),
]:
    for token in (result, first, second):
        if token not in catalog:
            raise SystemExit(f"missing fusion token {token} for {result}")

index = json.loads((ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))
if index.get("fusion_mode") != "live_pair_cast":
    raise SystemExit("fusion mode is not live_pair_cast")
if index.get("fusion_mastery", {}).get("2_circle_casts") != 4:
    raise SystemExit("2-circle mastery threshold mismatch")
if index.get("fusion_mastery", {}).get("3_circle_casts") != 7:
    raise SystemExit("3-circle mastery threshold mismatch")

print("Arcane Circle v0.3 live fusion and astral grimoire contract: PASS")
