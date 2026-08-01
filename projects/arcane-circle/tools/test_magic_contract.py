#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"

catalog = (JAVA / "magic/SpellCatalog.java").read_text(encoding="utf-8")
data = (JAVA / "magic/MagicPlayerData.java").read_text(encoding="utf-8")
casting = (JAVA / "magic/SpellCastingService.java").read_text(encoding="utf-8")
network = (JAVA / "network/ArcaneNetwork.java").read_text(encoding="utf-8")
client = (JAVA / "client/ArcaneClient.java").read_text(encoding="utf-8")
screen = (JAVA / "client/GrimoireScreen.java").read_text(encoding="utf-8")
hud = (JAVA / "client/ArcaneHud.java").read_text(encoding="utf-8")
render = (JAVA / "client/ArcaneRenderUtil.java").read_text(encoding="utf-8")
items = (JAVA / "registry/ModItems.java").read_text(encoding="utf-8")
staff_item = (JAVA / "item/ArcaneStaffItem.java").read_text(encoding="utf-8")

for token in [
    "prepareSlot(ServerPlayer player, int slot)",
    "prepareFusion(ServerPlayer player, List<String> ingredients)",
    "slot < 0 || slot >= 5",
    "effectiveStats(ServerPlayer player)",
    "state.known.add(resultId)",
]:
    if token not in data:
        raise SystemExit(f"missing five-slot/staff contract: {token}")

for token in [
    "queueFusionSlot(ServerPlayer player, int slot)",
    "commitFusion(ServerPlayer player)",
    "queue.size() >= 3",
    "spellSigil(",
    "multiSpiral(",
    "triuneBarrage(",
    "tempestAegis(",
    "phoenixField(",
]:
    if token not in casting:
        raise SystemExit(f"missing queued fusion or spell visual contract: {token}")

for token in ["InputConstants.KEY_X", "InputConstants.KEY_1", "InputConstants.KEY_5", "QueueFusionPayload", "CommitFusionPayload"]:
    if token not in client:
        raise SystemExit(f"missing hold-X numeric input contract: {token}")

for token in ["mouseDragged(MouseButtonEvent", "mouseScrolled(double mouseX", "contentScroll", "융합식", "slot(int i)"]:
    if token not in screen:
        raise SystemExit(f"missing responsive draggable grimoire contract: {token}")

for token in ["registerAboveAll", "InventoryScreen", "drawInventoryStatus", "drawFusionQueue", "drawSlot"]:
    if token not in hud:
        raise SystemExit(f"missing HUD/inventory status contract: {token}")

for token in ["cooldownArc", "spellRune"]:
    if token not in render:
        raise SystemExit(f"missing rune/cooldown renderer: {token}")

if items.count("register(\"") < 9:
    raise SystemExit("fewer than nine staves are registered")
for token in ["manaCostMultiplier", "powerMultiplier", "rangeMultiplier", "cooldownMultiplier", "regenMultiplier"]:
    if token not in staff_item:
        raise SystemExit(f"missing staff modifier family: {token}")

for token in ["QueueFusionPayload.TYPE", "CommitFusionPayload.TYPE", "slots=", "cooldowns=", "staff_power="]:
    if token not in network:
        raise SystemExit(f"missing network snapshot contract: {token}")

for result, ingredients in {
    "flame_lance": ["arcane_dart", "ember"],
    "ice_shackles": ["frost_needle", "lesser_ward"],
    "wind_blade": ["gale_step", "arcane_dart"],
    "fireball": ["flame_lance", "ember"],
    "frost_nova": ["ice_shackles", "frost_needle"],
    "chain_bolt": ["wind_blade", "arcane_dart"],
    "rift_step": ["blink", "gale_step"],
    "triune_barrage": ["arcane_dart", "ember", "frost_needle"],
    "tempest_aegis": ["gale_step", "lesser_ward", "arcane_dart"],
    "phoenix_field": ["ember", "mend", "greater_ward"],
}.items():
    for token in [result, *ingredients]:
        if token not in catalog:
            raise SystemExit(f"missing fusion token {token} for {result}")

index = json.loads((RES / "data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))
if index.get("fusion_mode") != "hold_x_queue_then_release":
    raise SystemExit("fusion mode is not hold_x_queue_then_release")
if index.get("spell_slots") != 5:
    raise SystemExit("five-slot contract mismatch")
if index.get("fusion_ingredient_limits") != [2, 3]:
    raise SystemExit("fusion ingredient limit mismatch")
if len(index.get("staffs", [])) != 9:
    raise SystemExit("staff catalog mismatch")

if (RES / "pack.mcmeta").exists():
    raise SystemExit("obsolete fixed pack.mcmeta remains and can trigger Minecraft 26.2 metadata warnings")

for staff in index["staffs"]:
    path = RES / f"assets/arcanecircle/items/{staff}.json"
    if not path.is_file():
        raise SystemExit(f"missing client item definition: {path}")
    json.loads(path.read_text(encoding="utf-8"))

print("Arcane Circle v0.4 responsive UI, five-slot HUD, triple fusion and staff contract: PASS")
