#!/usr/bin/env python3
from pathlib import Path
import base64, hashlib, json, struct

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"

def read(path):
    return (JAVA / path).read_text(encoding="utf-8")

def need(source, tokens, label):
    missing = [token for token in tokens if token not in source]
    if missing:
        raise SystemExit(f"missing {label}: {missing[0]}")

main = read("ArcaneCircle.java")
definition = read("magic/SpellDefinition.java")
catalog = read("magic/SpellCatalog.java")
data = read("magic/MagicPlayerData.java")
casting = read("magic/SpellCastingService.java")
network = read("network/ArcaneNetwork.java")
client = read("client/ArcaneClient.java")
client_boot = read("ArcaneCircleClient.java")
state = read("client/ArcaneClientState.java")
hud = read("client/ArcaneHud.java")
items = read("registry/ModItems.java")
book = read("item/SpellbookItem.java")
primer = read("item/BeginnerGrimoireItem.java")
build = (ROOT / "build.gradle").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
workflow = (ROOT.parents[1] / ".github/workflows/build-arcane-circle.yml").read_text(encoding="utf-8")

need(properties, ["mod_version=0.6.0-alpha.1"], "version")
need(workflow, ["0.6.0-alpha.1", "five-circle audit build"], "workflow")
need(main, ['VERSION = "0.6.0-alpha.1"', "grantStarterPrimerOnce", "tickCharge(player)"], "lifecycle")
if "PlayerSwitchHotbarSlotEvent" in main:
    raise SystemExit("unsupported hotbar event fallback remains")

need(definition, ["BOOK(\"주문서\")", "SigilAnchor", "FRONT", "GROUND_TARGET", "TARGET"], "spell definition")
if catalog.count('\n        add("') < 25 or catalog.count('\n        addFusion("') < 10:
    raise SystemExit("five-circle spell count contract failed")
need(catalog, ["meteor_shard", "inferno_domain", "absolute_zero", "arcane_annihilation", "case 5 -> 800"], "five-circle catalog")
need(data, ["starter_primer_granted", "learnPrimer", "learnSpell", "state.circle < 5", "case 5 -> 750"], "persistent learning")
need(casting, ["beginSlotCharge", "releaseSlotCharge", "tickCharge", "verticalSigil", "horizontalSigil", "Comparator.<Mob>comparingDouble", "meteorShard", "aegisCitadel"], "charged casting")
need(network, ["ninefold-arcana-6", "BeginCastPayload.TYPE", "ReleaseCastPayload.TYPE", "charging_slot="], "network")
need(client, ["onClientTickPre", "keyHotbarSlots[slot].consumeClick()", "BeginCastPayload", "ReleaseCastPayload"], "client input")
if "setSelectedSlot" in client:
    raise SystemExit("old hotbar restore workaround remains")
need(client_boot, ["onClientTickPre", "onClientTickPost"], "client registration")
need(state, ["splitPreserve", "chargingSpell", "isChargingSlot"], "client state")
need(hud, ["Math.min(27", "height - slotSize - 59", "legacySlot = 38", "READY"], "compact HUD")
need(items, ["BEGINNER_GRIMOIRE", "registerSpellbooks", "280, 0.46, 2.20, 1.80, 0.43, 2.15"], "items")
need(book, ["learnSpell", "shrink(1)"], "spellbook")
need(primer, ["learnPrimer", "shrink(1)"], "primer")
need(build, ["generateSpellbookResources", "villager_trade/librarian", "dependsOn generateSpellbookResources"], "resource generation")

spellbooks = json.loads((ROOT / "src/main/spellbooks.json").read_text(encoding="utf-8"))
if len(spellbooks) != 20 or any(sum(e["circle"] == c for e in spellbooks) != 5 for c in range(2, 6)):
    raise SystemExit("spellbook count by circle failed")
prices = [min(e["price_count"] * (9 if e["price_item"].endswith("emerald_block") else 1) for e in spellbooks if e["circle"] == c) for c in range(2, 6)]
if prices != sorted(prices) or len(set(prices)) != 4:
    raise SystemExit(f"spellbook prices are not rising: {prices}")

ko = json.loads((RES / "assets/arcanecircle/lang/ko_kr.json").read_text(encoding="utf-8"))
for entry in spellbooks:
    if f'item.arcanecircle.spellbook_{entry["id"]}' not in ko:
        raise SystemExit(f"missing localization for {entry['id']}")

textures = json.loads((ROOT / "src/main/staff-textures.json").read_text(encoding="utf-8"))
hashes = set()
for staff, encoded in textures.items():
    raw = base64.b64decode(encoded)
    if raw[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", raw[16:24]) != (32, 32):
        raise SystemExit(f"invalid staff texture: {staff}")
    hashes.add(hashlib.sha256(raw).hexdigest())
if len(textures) != 9 or len(hashes) != 9:
    raise SystemExit("staff textures are missing or duplicated")

print("Arcane Circle v0.6 charged casting, five-circle, spellbook and HUD audit: PASS")
