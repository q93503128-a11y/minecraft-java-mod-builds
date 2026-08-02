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
need(props, ("mod_version=0.11.0-alpha.1",), "version")

hud = read("client/ArcaneHud.java")
need(hud, (
    "int slotSize = width >= 520 ? 36", "drawCastingSigil", "partialRing",
    "for (int ring = 0; ring < spell.circle(); ring++)", "1C = one concentric boundary",
    "x + size / 2, y + size - 10"
), "small square HUD and exact-circle vector seal")
forbid(hud, (
    "int slotW = width >= 520 ? 82", "ParticleTypes", "SpellSigilService",
    '"MP " + spell.manaCost()'
), "large HUD or particle seal")

client = read("client/ArcaneClient.java")
need(client, (
    "slot < minecraft.options.keyHotbarSlots.length", "keyHotbarSlots[slot].consumeClick()",
    "all physical 1-9 presses are consumed"
), "1-9 vanilla hotbar blocking")

casting = read("magic/SpellCastingService.java")
need(casting, (
    "releaseSlotCharge", "castPrepared(player, data, cast);",
    "Completion only arms the spell", "return Math.max(2, base - circleGapReduction - masteryReduction);",
    "arcaneDart", "emberShot", "frostNeedle"
), "release-only cast and starter spell detail")
forbid(casting, (
    "SpellSigilService.renderChargeStep", "SpellSigilService.renderRelease",
    "CHARGES.remove(player.getUUID());\n            castPrepared(player, data, cast);\n        }\n    }\n\n    public static void cancelCharge"
), "particle seal or automatic completion cast")

screen = read("client/GrimoireScreen.java")
need(screen, (
    "int panelW = Math.min(720", "int panelH = Math.min(410",
    "int cols=c.w()>=420?9:3"
), "restored large grimoire with dense circle row")
forbid(screen, ("int targetH = switch (page)", "Math.min(620"), "circleSubtitle(circle)"),
       "shrunk window or decorative circle subtitles")

network = read("network/ArcaneNetwork.java")
need(network, ('PROTOCOL_VERSION = "ninefold-arcana-11"',), "v0.11 protocol")
main = read("ArcaneCircle.java")
need(main, ('VERSION = "0.11.0-alpha.1"',), "v0.11 lifecycle")

print("Arcane Circle v0.11 compact square HUD, vector seal and release-cast contract: PASS")
