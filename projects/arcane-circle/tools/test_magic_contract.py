#!/usr/bin/env python3
from pathlib import Path
import base64, hashlib, json, struct, subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"

subprocess.run([sys.executable, str(ROOT / "tools/apply_v08_26_2_compat.py")], check=True)


def read(rel: str) -> str:
    return (JAVA / rel).read_text(encoding="utf-8")


def need(source: str, tokens: list[str], label: str) -> None:
    for token in tokens:
        if token not in source:
            raise SystemExit(f"missing {label}: {token}")

main = read("ArcaneCircle.java")
catalog = read("magic/SpellCatalog.java")
data = read("magic/MagicPlayerData.java")
casting = read("magic/SpellCastingService.java")
sigils = read("magic/SpellSigilService.java")
high = read("magic/HighCircleSpellEffects.java")
lore = read("magic/SpellWorldLore.java")
world_data = read("world/ArcaneWorldData.java")
economy = read("world/ArcaneEconomyService.java")
offers = read("world/AcademyOfferCatalog.java")
world = read("world/MagicWorldService.java")
network = read("network/ArcaneNetwork.java")
screen = read("client/GrimoireScreen.java")
hud = read("client/ArcaneHud.java")
items = read("registry/ModItems.java")
build = (ROOT / "build.gradle").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
workflow = (ROOT.parents[1] / ".github/workflows/build-arcane-circle.yml").read_text(encoding="utf-8")

need(properties, ["mod_version=0.10.0-alpha.1"], "version")
need(workflow, ["0.10.0-alpha.1", "apply_v10_overhaul.py", "single-pass casting"], "workflow")
need(main, ['VERSION = "0.10.0-alpha.1"', "MagicWorldService", "ArcaneEconomyService"], "lifecycle")
need(catalog, [
    "IMPLEMENTED_MAX_CIRCLE = 9", "WORLD_MAX_CIRCLE = 9", "meteor_swarm",
    "power_word_kill", "prismatic_wall", "shapechange", "time_stop", "wish", "gate",
    "case 9 -> 32000", "case 9 -> 3000000"
], "nine-circle spell catalogue")
if catalog.count('\n        add("') != 90:
    raise SystemExit(f"expected 90 direct spells, found {catalog.count(chr(10) + '        add(\"')}")
if catalog.count('\n        addFusion("') != 10:
    raise SystemExit("expected 10 fusion spells")

need(lore, [
    "CC BY 4.0", "SigilFamily", "LANCE", "STAR", "HEX", "PORTAL", "EYE",
    "SEAL", "CLOCK", "SPIRAL", "STORM", "CROWN"
], "licensed lore and sigil grammar")
need(sigils, [
    "renderChargeStep", "renderRelease", "CHARGE_STAGES", "radialCompartments",
    "centralSeal", "runeTicks", "spell.id().hashCode()", "case LANCE", "CROWN"
], "single-pass per-spell sigil rendering")
if "renderReadyPulse" in sigils:
    raise SystemExit("ready-loop sigil regeneration remains")
need(high, [
    "disintegrate", "forcecage", "antimagic_field", "earthquake", "meteor_swarm",
    "power_word_kill", "prismatic_wall", "time_stop", "wish", "gate"
], "high-circle effects")
need(casting, [
    "SpellSigilService.renderChargeStep", "SpellSigilService.renderRelease", "requiredCastTicks",
    "HighCircleSpellEffects.execute", "marksEarned"
], "casting integration")
if "SpellSigilService.renderReadyPulse" in casting:
    raise SystemExit("ready-loop casting remains")
need(data, [
    "SpellCatalog.IMPLEMENTED_MAX_CIRCLE", "facultyMana", "facultyPower",
    "facultyRange", "facultyCooldown"
], "nine-circle persistence and faculties")

need(world_data, ["long marks", "MagicTradition tradition", "academyBuilt", "balance("], "world wallet")
need(economy, ["priceFor", "purchase", "chooseTradition", "awardCombat"], "Arcana economy")
need(offers, ["SPELLBOOK", "STAFF", "forCircle"], "academy offers")
need(world, ["setFoodLevel(20)", "GameType.SURVIVAL", "teleportToAcademy", "level.getGameTime()"],
     "Minecraft 26.2 natural magic-world shell")
if "ArcaneAcademyBuilder.build" in world:
    raise SystemExit("placeholder academy generation remains")
if "GameType.ADVENTURE" in world:
    raise SystemExit("forced adventure mode remains")
for obsolete in ("GameRules", "getSharedSpawnPos", "setDefaultSpawnPos", "setExhaustion", "getDayTime"):
    if obsolete in world:
        raise SystemExit(f"obsolete pre-26.2 world API remains: {obsolete}")
need(network, [
    'PROTOCOL_VERSION = "ninefold-arcana-10"', "PurchaseAcademyItemPayload.TYPE",
    "ChooseTraditionPayload.TYPE", '"academy"', '"marks="', '"tradition="', '";charge_required="'
], "academy and cast-time network")
need(screen, [
    'new Tab("academy", "학원")', "atlasCircle == 0", "circleCard(circle)",
    "academyCircle == 0", "offerCard", "아르카나"
], "dense hierarchical academy UI")
for movable in ("dragging", "savedOffsetX", "mouseDragged", "상단을 드래그"):
    if movable in screen:
        raise SystemExit(f"movable full-screen UI remains: {movable}")
need(hud, ["int slotW", "int slotH = 28", "fitName", "chargingFraction"], "compact spell HUD")
if "int desired = width >= 600 ? 58" in hud or "int slotSize" in hud:
    raise SystemExit("large square spell HUD remains")
need(items, ["ARCHMAGE_PROFILE", "spellbooks"], "magic equipment")

if "villager_trade" in build or "crafting_shaped" in build:
    raise SystemExit("survival recipe or villager currency generation remains in build.gradle")
need(build, ["generateSpellbookResources", "enchanted_book"], "academy item model generation")

books = json.loads((ROOT / "src/main/spellbooks.json").read_text(encoding="utf-8"))
by_circle = {circle: sum(entry["circle"] == circle for entry in books) for circle in range(1, 10)}
if len(books) != 85 or by_circle[1] != 5 or any(by_circle[circle] != 10 for circle in range(2, 10)):
    raise SystemExit(f"spellbook distribution mismatch: total={len(books)}, circles={by_circle}")

index = json.loads((RES / "data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))
if index.get("version") != "0.10.0-alpha.1" or index.get("direct_spells") != 90:
    raise SystemExit("v0.10 spell catalogue index mismatch")
if index.get("economy") != "single persistent Arcana wallet" or index.get("crafting_progression") is not False:
    raise SystemExit("magic-world economy index mismatch")
if index.get("implemented_circles") != list(range(1, 10)):
    raise SystemExit("implemented circle list is not 1-9")

notice = (RES / "META-INF/THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(notice, ["Creative Commons Attribution 4.0", "No proprietary setting", "No third-party Minecraft schematic"], "third-party notices")

textures = json.loads((ROOT / "src/main/staff-textures.json").read_text(encoding="utf-8"))
hashes = set()
for staff, encoded in textures.items():
    raw = base64.b64decode(encoded)
    if raw[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", raw[16:24]) != (32, 32):
        raise SystemExit(f"invalid staff texture: {staff}")
    hashes.add(hashlib.sha256(raw).hexdigest())
if len(textures) != 9 or len(hashes) != 9:
    raise SystemExit("staff textures are missing or duplicated")

print("Arcane Circle v0.10 dense UI and single-pass casting contract: PASS")
