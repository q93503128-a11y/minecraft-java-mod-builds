#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def read(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def need(text: str, tokens: tuple[str, ...], label: str) -> None:
    for token in tokens:
        if token not in text:
            raise SystemExit(f"missing {label}: {token}")


def forbid(text: str, tokens: tuple[str, ...], label: str) -> None:
    for token in tokens:
        if token in text:
            raise SystemExit(f"forbidden {label}: {token}")

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
need(props, ("mod_version=0.10.0-alpha.1",), "version")

screen = read("client/GrimoireScreen.java")
need(screen, (
    "atlasCircle == 0", "int cols=c.w()>=520?9:3", "int cols=v.w()>=540?5",
    "int cols=v.w()>=540?4:2", "slot + 1) + \"  \" + spell.name()",
    'String meta = "MP " + spell.manaCost()', "targetH = switch (page)",
    'case "atlas" -> atlasCircle == 0 ? 150 : 238',
    "v0.10 intentionally leaves the bottom edge empty"
), "dense grimoire")
forbid(screen, (
    "circleSubtitle(circle)", "spell.description()", "offer.description()", "t.description()",
    "상위 써클일수록", "슬롯 선택 →", "구매할 써클 선택"
), "large explanatory UI")

hud = read("client/ArcaneHud.java")
need(hud, (
    "int slotW = width >= 520 ? 82", "int slotH = 28", "int x, int y, int w, int h, int slot",
    "x + 28, y + 4", '"MP " + spell.manaCost()'
), "compact rectangular HUD")
forbid(hud, (
    "int desired = width >= 600 ? 58", "int slotSize", "size / 2", "size - 10"
), "large square HUD")

casting = read("magic/SpellCastingService.java")
need(casting, (
    "if (elapsed >= charge.requiredTicks)", "CHARGES.remove(player.getUUID());",
    "castPrepared(player, data, cast);", "int base = 4 + spell.circle() * 4",
    "int circleGapReduction = circleGap * 4"
), "single-pass automatic cast")
forbid(casting, ("renderReadyPulse", "lastReadyPulse >=",), "ready-loop regeneration")

sigil = read("magic/SpellSigilService.java")
need(sigil, (
    "CHARGE_STAGES = 5", "private static final ParticleOptions INK = ParticleTypes.END_ROD",
    "Math.min(0.78", "look.scale(1.28)", "up.scale(-0.46)",
    "renderChargeStep", "renderRelease"
), "compact non-falling sigil")
forbid(sigil, (
    "ParticleTypes.SNOWFLAKE", "ParticleTypes.CLOUD", "ParticleTypes.HAPPY_VILLAGER",
    "renderReadyPulse", "for (int step = 0; step < CHARGE_STAGES"
), "falling or repeated sigil particles")

world = read("world/MagicWorldService.java")
need(world, ("setFoodLevel(20)", "GameType.SURVIVAL", "physical academy",), "natural magic-world rules")
forbid(world, (
    "ArcaneAcademyBuilder", "teleportTo(arrival", "setAcademy(", "academyBuilt()"
), "placeholder academy usage")

network = read("network/ArcaneNetwork.java")
need(network, ('PROTOCOL_VERSION = "ninefold-arcana-10"',), "v0.10 protocol")
main = read("ArcaneCircle.java")
need(main, ('VERSION = "0.10.0-alpha.1"',), "v0.10 lifecycle")

print("Arcane Circle v0.10 overhaul contract: PASS")
