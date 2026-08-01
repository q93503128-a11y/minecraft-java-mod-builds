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
catalog = read("magic/SpellCatalog.java")
data = read("magic/MagicPlayerData.java")
casting = read("magic/SpellCastingService.java")
network = read("network/ArcaneNetwork.java")
client = read("client/ArcaneClient.java")
state = read("client/ArcaneClientState.java")
screen = read("client/GrimoireScreen.java")
hud = read("client/ArcaneHud.java")
render = read("client/ArcaneRenderUtil.java")
items = read("registry/ModItems.java")
staff_item = read("item/ArcaneStaffItem.java")
build = (ROOT / "build.gradle").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")


def require(source: str, tokens: list[str], label: str) -> None:
    for token in tokens:
        if token not in source:
            raise SystemExit(f"missing {label}: {token}")


require(properties, ["mod_version=0.5.0-alpha.1"], "version contract")
require(main, [
    'VERSION = "0.5.0-alpha.1"',
    "PlayerLoggedOutEvent", "PlayerRespawnEvent", "PlayerChangedDimensionEvent",
    "ServerStoppedEvent", "clearSession", "clearAllSessions",
    "STARTER_STAFF_TAG", "player.drop(staff, false)"
], "player lifecycle contract")

require(data, [
    "record CooldownEntry", "optionalFieldOf(\"cooldowns\"", "cooldownStatus",
    "startCooldown", "cooldownSnapshot", "serverClock", "state.cooldowns",
    "entry.cooldowns()", "Math.min(1024.0, entry.mana())",
    "if (!known.contains(this.slots.get(index)))"
], "persistent save contract")
if "static final Map<UUID, Map<String, Long>> COOLDOWNS" in casting:
    raise SystemExit("obsolete nonpersistent cooldown map remains")

require(catalog, [
    "candidatesFor(List<String> ingredients)", "canExtend(List<String> ingredients)",
    "isSortedMultisetSubset", "normalizedIngredients"
], "progressive fusion contract")
require(casting, [
    "QUEUE_TIMEOUT_TICKS = 200L", "FusionQueueState", "candidatesFor(proposed)",
    "clearSession(UUID playerId)", "clearAllSessions()", "canExecute(",
    "validTarget(ServerPlayer player, Mob mob)", "TamableAnimal",
    "player.hasLineOfSight(mob)", "findBlinkDestination", "isFaceSturdy",
    "ally.heal", "isAllied(player, mob)", "spellSigil(", "schoolMotif(",
    "triuneBarrage(", "tempestAegis(", "phoenixField("
], "safe casting and fusion contract")
cast_block = casting[casting.index("private static void castPrepared"):casting.index("private static boolean canExecute")]
if cast_block.index("if (!canExecute") > cast_block.index("prelude(level"):
    raise SystemExit("visual prelude still occurs before cast validation")

require(network, [
    'PROTOCOL_VERSION = "ninefold-arcana-5"', '"staffs"',
    "magicData.cooldownSnapshot(player)", "queue_candidates=", "queue_extend=",
    "SpellCatalog.candidatesFor(queue)"
], "network snapshot contract")
require(client, [
    "ClientTickEvent.Post", "minecraft.gui.screen()", "CommitFusionPayload(1)",
    "drainSlotClicks", "setSelectedSlot(stableHotbarSlot)", "ArcaneClientState.reset()"
], "stable numeric input contract")
require(state, ["queueCandidates()", "queueCanExtend()", "reset()", "ready()"], "client state contract")

require(hud, [
    "drawManaTop", "drawManaSide", "drawInventoryCompact", "drawInventorySide",
    "queueCandidates", "queueCanExtend", "cooldownArc", "drawSlot"
], "responsive HUD contract")
require(screen, [
    'new Tab("staffs", "지팡이")', "savedActiveSlot", "SAVED_SCROLL",
    "mouseDragged(MouseButtonEvent", "mouseScrolled(double mouseX",
    "drawStaffCard", "staffColumns", "if (!ArcaneClientState.known().contains",
    "boolean compact = c.w() < 540"
], "responsive grimoire contract")
require(render, [
    "case FIRE", "case FROST", "case WIND", "case WARD", "case LIFE", "case SPACE",
    "squarePerimeterPoint", "cooldownArc", "schoolMotif"
], "distinct visual language contract")

if items.count("register(\"") < 9:
    raise SystemExit("fewer than nine staves are registered")
require(items, ["profiles()", "recipeHint", "ARCHMAGE_PROFILE", "equipped(Player player)"], "staff progression contract")
require(staff_item, [
    "manaCostMultiplier", "powerMultiplier", "rangeMultiplier",
    "cooldownMultiplier", "regenMultiplier", "recipeHint()"
], "staff modifier tooltip contract")
require(build, ["generateStaffTextures", "staff-textures.json", "Base64.getDecoder()", "dependsOn generateStaffTextures"],
        "generated texture build contract")

formulae = {
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
}
for result, ingredients in formulae.items():
    for token in [result, *ingredients]:
        if token not in catalog:
            raise SystemExit(f"missing fusion token {token} for {result}")

index_path = RES / "data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
expected = {
    "version": "0.5.0-alpha.1",
    "spell_slots": 5,
    "fusion_mode": "hold_x_queue_then_release",
    "fusion_preview": "progressive_multiset_candidates",
    "cooldown_storage": "persistent_world_saved_data",
    "staff_survival_recipes": 8,
    "staff_custom_textures": 9,
}
for key, value in expected.items():
    if index.get(key) != value:
        raise SystemExit(f"index contract mismatch: {key}={index.get(key)!r}, expected {value!r}")
if index.get("fusion_ingredient_limits") != [2, 3]:
    raise SystemExit("fusion ingredient limit mismatch")
if len(index.get("staffs", [])) != 9:
    raise SystemExit("staff catalog mismatch")
if index.get("grimoire_tabs") != ["atlas", "recipes", "staffs", "core"]:
    raise SystemExit("grimoire tabs mismatch")

if (RES / "pack.mcmeta").exists():
    raise SystemExit("obsolete fixed pack.mcmeta remains and can trigger Minecraft 26.2 metadata warnings")

staffs = index["staffs"]
for staff in staffs:
    item_path = RES / f"assets/arcanecircle/items/{staff}.json"
    model_path = RES / f"assets/arcanecircle/models/item/{staff}.json"
    if not item_path.is_file() or not model_path.is_file():
        raise SystemExit(f"missing custom staff client model for {staff}")
    item = json.loads(item_path.read_text(encoding="utf-8"))
    model = json.loads(model_path.read_text(encoding="utf-8"))
    if item.get("model", {}).get("model") != f"arcanecircle:item/{staff}":
        raise SystemExit(f"staff item definition still points at a borrowed model: {staff}")
    if model.get("parent") != "minecraft:item/handheld":
        raise SystemExit(f"staff model is not handheld: {staff}")
    if model.get("textures", {}).get("layer0") != f"arcanecircle:item/{staff}":
        raise SystemExit(f"staff texture mapping mismatch: {staff}")

texture_catalog = json.loads((ROOT / "src/main/staff-textures.json").read_text(encoding="utf-8"))
if set(texture_catalog) != set(staffs):
    raise SystemExit("staff texture catalog IDs do not match staff registry")
texture_hashes = set()
for staff, encoded in texture_catalog.items():
    raw = base64.b64decode(encoded, validate=True)
    if raw[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"invalid PNG signature for {staff}")
    width, height = struct.unpack(">II", raw[16:24])
    if (width, height) != (32, 32):
        raise SystemExit(f"unexpected staff texture size for {staff}: {width}x{height}")
    texture_hashes.add(hashlib.sha256(raw).hexdigest())
if len(texture_hashes) != 9:
    raise SystemExit("staff textures are duplicated")

recipe_dir = RES / "data/arcanecircle/recipe"
recipe_results = set()
for path in recipe_dir.glob("*_staff.json"):
    recipe = json.loads(path.read_text(encoding="utf-8"))
    if recipe.get("type") != "minecraft:crafting_shapeless":
        raise SystemExit(f"unexpected staff recipe type: {path.name}")
    result = recipe.get("result", {}).get("id", "")
    if not result.startswith("arcanecircle:"):
        raise SystemExit(f"invalid staff recipe result: {path.name}")
    recipe_results.add(result.split(":", 1)[1])
expected_craftable = set(staffs) - {"novice_staff"}
if recipe_results != expected_craftable:
    raise SystemExit(f"staff survival recipes mismatch: {sorted(recipe_results)}")

print("Arcane Circle v0.5 full save/input/network/UI/casting/staff/resource audit contract: PASS")
