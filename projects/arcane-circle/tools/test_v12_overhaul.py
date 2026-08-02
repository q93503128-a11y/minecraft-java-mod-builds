#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def need(text: str, tokens: list[str], label: str) -> None:
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"{label} missing: {missing}")


def forbid(text: str, tokens: list[str], label: str) -> None:
    found = [token for token in tokens if token in text]
    if found:
        raise SystemExit(f"{label} forbidden tokens remain: {found}")

hud = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneHud.java")
client = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClient.java")
tracker = read("src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java")
visual_service = read("src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java")
casting = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java")
network = read("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java")
handlers = read("src/main/java/kr/moonseungjun/arcanecircle/client/ClientNetworkHandlers.java")
client_main = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircleClient.java")
state = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClientState.java")
version = read("gradle.properties")

need(version, ["mod_version=0.12.1-alpha.1"], "version")
need(network, ["ninefold-arcana-12-1", "WorldMagicPayload.TYPE", "fusion_charge_ticks",
               "fusion_charge_required"], "network")
need(handlers, ["handleWorldMagic", "WorldMagicTracker.accept"], "client payload handler")
need(client_main, ["WorldMagicTracker::onExtract", "WorldMagicTracker::onSubmit"], "world render registration")

need(hud, ["int gap = width >= 520 ? 6 : 5", "int slotSize = width >= 520 ? 25",
           "tinyText", "0.50F", "inventoryLeft - panelW - 7", "fusionChargingFraction"],
     "compact separated HUD")
forbid(hud, ["drawCastingSigil", "partialRing"], "screen-space casting circle")

need(client, ["for (KeyMapping vanilla : minecraft.options.keyHotbarSlots)", "vanilla.setDown(false)",
              "getSelectedSlot()", "setSelectedSlot(protectedSelectedSlot)"], "1-9 hotbar lock")
need(state, ["fusionChargingTicks", "fusionChargingRequiredTicks", "fusionChargingFraction",
             "fusionChargingReady"], "fusion client state")

need(casting, ["requiredFusionCastTicks", "tickFusion", "chargeStartedAt", "fusionChargingSpell",
               "WorldMagicService.charge", "WorldMagicService.release", "WorldMagicService.stop"],
     "charged fusion and world visuals")
forbid(casting, ["sendParticles("], "server particle-centered spell visuals")

need(tracker, ["ExtractLevelRenderStateEvent", "SubmitCustomGeometryEvent", "submitShapeOutline",
               "for (int ring = 0; ring < spell.circle(); ring++)", "buildCharge", "buildRelease",
               "sphereLattice", "spell.sigilAnchor()"], "world-space geometry renderer")
need(visual_service, ["PacketDistributor.sendToPlayersNear", "kind=stop", "case FRONT",
                      "case GROUND_TARGET", "MagicPlayerData.CastPreparation"],
     "multiplayer world visual broadcast")

# Fusion must be slower than direct casting before mastery/registration.
need(casting, ["int direct = requiredCastTicks", "registered ? 7 : 18 + ingredientCount * 5",
               "Math.max(direct + 5"], "fusion cast-time penalty")

print("Arcane Circle v0.12.1 compact HUD, charged fusion and particle-free world geometry contract: PASS")
