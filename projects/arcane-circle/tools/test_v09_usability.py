#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"

def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")

def require(text: str, token: str, label: str) -> None:
    if token not in text:
        raise SystemExit(f"missing {label}: {token}")

def forbid(text: str, token: str, label: str) -> None:
    if token in text:
        raise SystemExit(f"forbidden {label}: {token}")

props = read("gradle.properties")
require(props, "mod_version=0.9.0-alpha.1", "v0.9 version")

screen = read("src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java")
for token in ("private static int atlasCircle", "if (atlasCircle == 0)", "circleCard(circle)",
              "academyCircle == 0", "Math.min(720", "써클 선택"):
    require(screen, token, "fixed hierarchical grimoire")
for token in ("dragging", "savedOffsetX", "savedOffsetY", "mouseDragged", "dragBar()", "상단을 드래그"):
    forbid(screen, token, "movable full-screen grimoire")

casting = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
for token in ("requiredCastTicks", "chargingRequiredTicks", "existing != null && existing.slot == slot",
              "SpellSigilService.renderChargeStep", "SpellSigilService.renderReadyPulse", "charge.lastStage"):
    require(casting, token, "staged casting")
forbid(casting, "Math.floorMod(player.tickCount, 4)", "four-tick full sigil redraw")

sigil = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellSigilService.java")
for token in ("CHARGE_STAGES = 7", "radialCompartments", "centralSeal", "runeTicks",
              "Math.min(1.20", "look.scale(1.78)", "up.scale(-0.34)"):
    require(sigil, token, "compact ceremonial sigil")
forbid(sigil, "42 + circle * 5", "old particle wall")

network = read("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java")
require(network, 'PROTOCOL_VERSION = "ninefold-arcana-9"', "v0.9 protocol")
require(network, '";charge_required="', "cast-time sync")

client = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClientState.java")
for token in ("chargingRequiredTicks", "chargingFraction", "chargingReady"):
    require(client, token, "client cast progress")

hud = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneHud.java")
for token in ("width >= 600 ? 58", "fitName(font, spell.name()", "chargingFraction()"):
    require(hud, token, "readable spell HUD")
forbid(hud, "compactName(spell.name(), 3)", "three-character spell truncation")
forbid(hud, '"READY"', "jumping charge label")

world = read("src/main/java/kr/moonseungjun/arcanecircle/world/MagicWorldService.java")
require(world, "GameType.SURVIVAL", "break/place capable shell")
require(world, "setFoodLevel(20)", "disabled hunger progression")
forbid(world, "GameType.ADVENTURE", "forced adventure mode")

catalog = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java")
for name in ("매직 미사일", "파이어 볼트", "프로스트 레이", "아케인 실드", "파이어볼",
             "라이트닝 볼트", "메테오 스트라이크", "타임 스톱", "플레임 버스트",
             "프로스트 랜스", "스펠 브레이커", "아케인 게이트"):
    require(catalog, name, "classic fantasy spell naming")

print("Arcane Circle v0.9 usability contract: PASS")
