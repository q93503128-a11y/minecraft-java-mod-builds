#!/usr/bin/env python3
from pathlib import Path
import base64
import hashlib
import json
import struct

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"

read = lambda relative: (JAVA / relative).read_text(encoding="utf-8")
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
spellbook_item = read("item/SpellbookItem.java")
primer_item = read("item/BeginnerGrimoireItem.java")
build = (ROOT / "build.gradle").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
workflow = (ROOT.parents[1] / ".github/workflows/build-arcane-circle.yml").read_text(encoding="utf-8")


def require(source: str, tokens: list[str], label: str) -> None:
    for token in tokens:
        if token not in source:
            raise SystemExit(f"missing {label}: {token}")


require(properties, ["mod_version=0.6.0-alpha.1"], "version contract")
require(workflow, ["0.6.0-alpha.1", "five-circle audit build"], "CI contract")
require(main, [
    'VERSION = "0.6.0-alpha.1"', "grantStarterPrimerOnce", "BEGINNER_GRIMOIRE",
    "PlayerSwitchHotbarSlotEvent.Pre", "shouldBlockHotbarSwitch", "tickCharge(player)"
], "bootstrap and lifecycle contract")

require(definition, [
    "Acquisition", "BOOK(\"주문서\")", "SigilAnchor", "FRONT(\"전방 전개\")",
    "GROUND_TARGET(\"조준 지면\")", "TARGET(\"대상 결속\")"
], "spell definition contract")

if catalog.count('\n        add("') < 25:
    raise SystemExit("fewer than 25 direct 1-5 circle spells")
if catalog.count('\n        addFusion("') < 10:
    raise SystemExit("fewer than ten fusion formulae")
for token in [
    '"arcane_dart"', '"stone_skin"', '"greater_ward"', '"meteor_shard"',
    '"inferno_domain"', '"absolute_zero"', '"tempest_domain"',
    '"aegis_citadel"', '"arcane_annihilation"',
    "circleInsightThreshold", "case 5 -> 800", "return List.of();"
]:
    if token not in catalog:
        raise SystemExit(f"missing five-circle catalog contract: {token}")

require(data, [
    'optionalFieldOf("starter_primer_granted", false)', "claimStarterPrimer",
    "learnPrimer(ServerPlayer player)", "learnSpell(ServerPlayer player, String spellId)",
    "Math.min(5, entry.circle())", "while (state.circle < 5",
    "case 5 -> 750", "case 5 -> 7.0", "SpellCatalog.starterSlots()"
], "persistent spell learning contract")
if "known.addAll(SpellCatalog.starterKnownSpells())" in data:
    raise SystemExit("new profiles still auto-learn starter spells")

require(casting, [
    "beginSlotCharge", "releaseSlotCharge", "tickCharge", "ChargeState",
    "renderCharge", "verticalSigil", "horizontalSigil", "renderAnchoredSigil",
    "case FRONT", "case GROUND_TARGET", "case TARGET",
    "meteorShard", "blizzardField", "thunderPrison", "massMend", "spatial_gate",
    "infernoDomain", "absoluteZero", "tempestDomain", "aegisCitadel", "arcaneAnnihilation",
    "validTarget(ServerPlayer player, Mob mob)", "isAlly(ServerPlayer player, LivingEntity entity)"
], "charge and five-circle execution contract")

require(network, [
    'PROTOCOL_VERSION = "ninefold-arcana-6"', "BeginCastPayload.TYPE", "ReleaseCastPayload.TYPE",
    "charging=", "charging_slot=", "charge_ticks=", "spell_count="
], "network charge contract")

require(client, [
    "onClientTickPre(ClientTickEvent.Pre", "onClientTickPost(ClientTickEvent.Post",
    "keyHotbarSlots[slot].consumeClick()", "new BeginCastPayload(slot)",
    "new ReleaseCastPayload(slot)", "SLOT_WAS_DOWN"
], "client hold-release input contract")
if "setSelectedSlot" in client or "stableHotbarSlot" in client:
    raise SystemExit("obsolete hotbar restore workaround remains")
require(client_boot, ["onClientTickPre", "onClientTickPost"], "client event phase registration")
require(state, ["splitPreserve", "chargingSpell", "chargingSlot", "chargingTicks", "isChargingSlot"],
        "client charge state contract")

require(hud, [
    "Math.min(27", "height - slotSize - 59", "legacySlot = 38", "isChargingSlot(slot)",
    '"READY"'
], "compact raised spell HUD contract")
if "Math.min(38" in hud:
    raise SystemExit("old oversized 38px spell slot rule remains")

require(spellbook_item, ["learnSpell(serverPlayer, spellId)", "shrink(1)", "emeraldEquivalentPrice"],
        "spellbook consumption contract")
require(primer_item, ["learnPrimer(serverPlayer)", "shrink(1)", "primerSpells"],
        "beginner grimoire contract")
require(items, [
    "BEGINNER_GRIMOIRE", "registerSpellbooks", "SpellbookItem", "spellbooks()",
    "280, 0.46, 2.20, 1.80, 0.43, 2.15"
], "item and late-staff progression contract")

spellbooks = json.loads((ROOT / "src/main/spellbooks.json").read_text(encoding="utf-8"))
if len(spellbooks) != 20:
    raise SystemExit(f"expected 20 circle 2-5 spellbooks, found {len(spellbooks)}")
for circle in range(2, 6):
    count = sum(1 for entry in spellbooks if entry["circle"] == circle)
    if count != 5:
        raise SystemExit(f"circle {circle} has {count} spellbooks instead of 5")
prices = {circle: min(
    entry["price_count"] * (9 if entry["price_item"] == "minecraft:emerald_block" else 1)
    for entry in spellbooks if entry["circle"] == circle
) for circle in range(2, 6)}
if not prices[2] < prices[3] < prices[4] < prices[5]:
    raise SystemExit(f"spellbook trade prices are not exponential enough: {prices}")

require(build, [
    "generateSpellbookResources", "villager_trade/librarian/level_1", "spellbook_${spellId}",
    "data/minecraft/tags/villager_trade/librarian/level_${level}.json",
    "dependsOn generateSpellbookResources"
], "generated spellbook resource contract")

ko = json.loads((RES / "assets/arcanecircle/lang/ko_kr.json").read_text(encoding="utf-8"))
if "item.arcanecircle.beginner_grimoire" not in ko:
    raise SystemExit("missing beginner grimoire localization")
for entry in spellbooks:
    key = f'item.arcanecircle.spellbook_{entry["id"]}'
    if key not in ko:
        raise SystemExit(f"missing Korean spellbook localization: {key}")

staff_catalog = json.loads((ROOT / "src/main/staff-textures.json").read_text(encoding="utf-8"))
if len(staff_catalog) != 9:
    raise SystemExit("staff texture catalog must contain nine textures")
texture_hashes = set()
for staff, encoded in staff_catalog.items():
    raw = base64.b64decode(encoded)
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"invalid PNG signature: {staff}")
    width, height = struct.unpack(">II", raw[16:24])
    if (width, height) != (32, 32):
        raise SystemExit(f"staff texture {staff} is {width}x{height}, expected 32x32")
    texture_hashes.add(hashlib.sha256(raw).hexdigest())
if len(texture_hashes) != 9:
    raise SystemExit("staff texture catalog contains duplicate images")

if (RES / "pack.mcmeta").exists():
    raise SystemExit("obsolete fixed pack.mcmeta remains")

print("Arcane Circle v0.6 hold-release/five-circle/spellbook/HUD/staff audit contract: PASS")
