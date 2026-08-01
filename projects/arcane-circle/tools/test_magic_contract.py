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
expanded = read("magic/ExpandedSpellEffects.java")
growth = read("magic/CombatGrowthService.java")
network = read("network/ArcaneNetwork.java")
client = read("client/ArcaneClient.java")
client_boot = read("ArcaneCircleClient.java")
state = read("client/ArcaneClientState.java")
hud = read("client/ArcaneHud.java")
screen = read("client/GrimoireScreen.java")
items = read("registry/ModItems.java")
book = read("item/SpellbookItem.java")
primer = read("item/BeginnerGrimoireItem.java")
build = (ROOT / "build.gradle").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
workflow = (ROOT.parents[1] / ".github/workflows/build-arcane-circle.yml").read_text(encoding="utf-8")

need(properties, ["mod_version=0.7.0-alpha.1"], "version")
need(workflow, ["0.7.0-alpha.1", "apply_v07_upgrade.py", "classic spell combat mastery build"], "workflow")
need(main, ['VERSION = "0.7.0-alpha.1"', "grantStarterPrimerOnce", "tickCharge(player)"], "lifecycle")
if "PlayerSwitchHotbarSlotEvent" in main:
    raise SystemExit("unsupported hotbar event fallback remains")

need(definition, ["BOOK(\"주문서\")", "SigilAnchor", "FRONT", "GROUND_TARGET", "TARGET"], "spell definition")
if catalog.count('\n        add("') != 50:
    raise SystemExit(f"expected 50 direct spells, found {catalog.count(chr(10) + '        add(\"')}")
if catalog.count('\n        addFusion("') != 10:
    raise SystemExit("expected ten classic fusion spells")
need(catalog, [
    "IMPLEMENTED_MAX_CIRCLE = 5", "WORLD_MAX_CIRCLE = 9", "spellsInCircle",
    "magic_missile", "scorching_ray", "fireball", "wall_of_fire", "cone_of_cold",
    "chain_lightning", "teleportation_circle", "masteryTier", "case 5 -> 1600"
], "classic spell catalogue")
for obsolete in ["arcane_dart", "ember", "frost_needle", "inferno_domain", "arcane_annihilation"]:
    if f'"{obsolete}"' in catalog:
        raise SystemExit(f"obsolete invented spell remains: {obsolete}")

need(data, [
    "CombatGrowthService.Impact impact", "masteryGain", "result.insightGain()",
    "masteryMana", "masteryCooldown", "masteryRange", "masteryPower",
    "state.circle < SpellCatalog.IMPLEMENTED_MAX_CIRCLE"
], "combat mastery persistence")
need(growth, ["maxHealth >= 300.0F", "kills * 3", "masteryGain", "insightGain"], "strong enemy growth")
need(casting, [
    "CombatGrowthService.capture", "CombatGrowthService.measure", "Math.floorMod(player.tickCount, 4)",
    "rangeRatio", "planeRing", "planePolygon", "ParticleTypes.END_ROD",
    "ExpandedSpellEffects.execute(player, id, range, power)"
], "fixed charge sigil and range visuals")
need(expanded, [
    "magic_missile", "thunderwave", "misty_step", "lightning_bolt", "dimension_door",
    "wall_of_force", "cloudkill", "flame_strike", "chain_lightning", "teleportation_circle",
    "Math.max(6.0, range * 0.34)", "Math.max(4.5, Math.min(10.0, range * 0.36))"
], "expanded spell execution")
need(network, ["ninefold-arcana-7", "SpellCatalog.spells().values().stream()", "mastery="], "all-spell mastery network")
need(client, ["onClientTickPre", "keyHotbarSlots[slot].consumeClick()", "BeginCastPayload", "ReleaseCastPayload"], "client input")
if "setSelectedSlot" in client:
    raise SystemExit("old hotbar restore workaround remains")
need(client_boot, ["onClientTickPre", "onClientTickPost"], "client registration")
need(state, ["splitPreserve", "chargingSpell", "isChargingSlot", "mastery(String spellId)"], "client state")
need(hud, ["Math.min(27", "height - slotSize - 59", "legacySlot = 38", "READY"], "compact HUD")
need(screen, ["savedCircleFilter", "drawCircleFilters", "visibleSpells", "circleFilter(int circle)", "사용 가능 · 숙련"], "circle-category grimoire")
need(items, ["BEGINNER_GRIMOIRE", "registerSpellbooks", "280, 0.46, 2.20, 1.80, 0.43, 2.15"], "items")
need(book, ["learnSpell", "shrink(1)"], "spellbook")
need(primer, ["learnPrimer", "shrink(1)"], "primer")
need(build, ["generateSpellbookResources", "villager_trade/librarian", "dependsOn generateSpellbookResources"], "resource generation")

spellbooks = json.loads((ROOT / "src/main/spellbooks.json").read_text(encoding="utf-8"))
expected = {1: 5, 2: 10, 3: 10, 4: 10, 5: 10}
if len(spellbooks) != 45:
    raise SystemExit(f"expected 45 spellbooks, found {len(spellbooks)}")
for circle, count in expected.items():
    actual = sum(entry["circle"] == circle for entry in spellbooks)
    if actual != count:
        raise SystemExit(f"circle {circle} spellbooks: {actual}, expected {count}")
prices = [min(e["price_count"] * (9 if e["price_item"].endswith("emerald_block") else 1)
              for e in spellbooks if e["circle"] == circle) for circle in range(1, 6)]
if prices != sorted(prices) or len(set(prices)) != 5:
    raise SystemExit(f"spellbook prices are not strictly rising: {prices}")

ko = json.loads((RES / "assets/arcanecircle/lang/ko_kr.json").read_text(encoding="utf-8"))
for entry in spellbooks:
    if f'item.arcanecircle.spellbook_{entry["id"]}' not in ko:
        raise SystemExit(f"missing localization for {entry['id']}")

index = json.loads((RES / "data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))
if index.get("total_spells") != 60 or index.get("spellbooks") != 45:
    raise SystemExit("catalog index counts mismatch")
if index.get("world_max_circle") != 9:
    raise SystemExit("world maximum circle changed")

textures = json.loads((ROOT / "src/main/staff-textures.json").read_text(encoding="utf-8"))
hashes = set()
for staff, encoded in textures.items():
    raw = base64.b64decode(encoded)
    if raw[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", raw[16:24]) != (32, 32):
        raise SystemExit(f"invalid staff texture: {staff}")
    hashes.add(hashlib.sha256(raw).hexdigest())
if len(textures) != 9 or len(hashes) != 9:
    raise SystemExit("staff textures are missing or duplicated")

print("Arcane Circle v0.7 classic spells, fixed sigils, circle filters and combat mastery audit: PASS")
