#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def need(source: str, tokens: tuple[str, ...], label: str) -> None:
    missing = [token for token in tokens if token not in source]
    if missing:
        raise SystemExit(f"{label} missing: {missing}")


props = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java")
network = read("src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java")
hud = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneHud.java")
client = read("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircleClient.java")
tracker = read("src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java")
signature = read("src/main/java/kr/moonseungjun/arcanecircle/client/SignatureGeometry.java")
mesh = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java")
vitality = read("src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneVitalityService.java")
gear_renderer = read("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.java")
materials = read("src/main/java/kr/moonseungjun/arcanecircle/registry/ArcaneArmorMaterials.java")
items = read("src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java")
catalog = read("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java")

need(props, ("mod_version=0.12.1-alpha.8",), "version")
need(main, ('VERSION = "0.12.1-alpha.8"', "ArcaneVitalityService::onIncomingDamage",
            "ArcaneVitalityService::onHeal"), "lifecycle")
need(network, ('ninefold-arcana-12-1-alpha8', '";health="', '";health_max="'), "network health")
index = json.loads(read("src/main/resources/data/arcanecircle/spell_catalog/index.json"))
if index.get("version") != "0.12.1-alpha.8":
    raise SystemExit("alpha.8 spell index version missing")

for label, source in (("world tracker", tracker), ("signature geometry", signature)):
    forbidden = ("import net.minecraft.world.phys.shapes.VoxelShape", "Shapes.create(",
                 "submitShapeOutline(", "new AABB(")
    found = [token for token in forbidden if token in source]
    if found:
        raise SystemExit(f"{label} still uses pixel-box geometry: {found}")
need(mesh, ("submitCustomGeometry", "RenderTypes.lines()", "setLineWidth", "submitPass",
            "Builder arc", "Builder helix", "Builder cone"), "continuous mesh")
need(tracker, ("rangeScale", "powerScale", "appendSchoolSeal", "High-circle circles",
               "BEAMS", "PROJECTILES", "WAVES", "SignatureGeometry.append"), "spell visual grammar")
need(signature, ("meteor(", "storm(", "portal(", "cage(", "wall(", "dome(",
                 "faultField(", "executionSeal("), "signature spell silhouettes")

need(vitality, ("case 1 -> 100", "case 5 -> 1_300", "case 9 -> 18_000",
                 "LivingIncomingDamageEvent", "LivingHealEvent", "convertToVanilla",
                 "BYPASSES_INVULNERABILITY"), "RPG vitality")
need(hud, ("VanillaGuiLayers.PLAYER_HEALTH", "event.setCanceled(true)",
           '"HP " + health + " / " + maximum', "drawHealth"), "health HUD")
if '"X " + chain' in hud or 'X 융합' in hud:
    raise SystemExit("obsolete X fusion HUD text remains")
need(client, ("ArcaneGearRenderer::registerStateModifiers", "ArcaneHud::onVanillaLayer",
              "ArcaneGearRenderer::onPlayerRender"), "client registrations")

need(materials, ("MAGE_ASSET", "SAGE_ASSET", "ARCHMAGE_ASSET", "EquipmentAssets.ROOT_ID"),
     "custom equipment materials")
need(items, ("ArcaneArmorMaterials.MAGE", "ArcaneArmorMaterials.SAGE",
             "ArcaneArmorMaterials.ARCHMAGE"), "custom material use")
if "ArmorMaterials.LEATHER" in items or "ArmorMaterials.DIAMOND" in items:
    raise SystemExit("vanilla inherited armor material remains")
need(gear_renderer, ("pointed hat", "submitHat", "submitRobe", "submitBoots",
                     "RenderTypes.debugFilledBox()", "registerAvatarEntityModifier"),
     "mage silhouette renderer")

need(catalog, ("case 2 -> 1.55", "case 5 -> 5.40", "case 7 -> 12.50",
               "case 8 -> 19.00", "case 9 -> 29.00"), "circle damage hierarchy")

for tier in ("mage", "sage", "archmage"):
    equipment = ROOT / f"src/main/resources/assets/arcanecircle/equipment/{tier}.json"
    outer = ROOT / f"src/main/resources/assets/arcanecircle/textures/entity/equipment/humanoid/{tier}/outer.png"
    inner = ROOT / f"src/main/resources/assets/arcanecircle/textures/entity/equipment/humanoid_leggings/{tier}/inner.png"
    for path in (equipment, outer, inner):
        if not path.is_file() or path.stat().st_size < 32:
            raise SystemExit(f"missing generated equipment asset: {path.relative_to(ROOT)}")

for item_id in (
        "mage_hat", "sage_hat", "archmage_crown",
        "mage_robe", "sage_robe", "archmage_robe",
        "mage_robe_hem", "sage_robe_hem", "archmage_robe_hem",
        "mage_boots", "skywalker_boots", "froststep_boots"):
    icon = ROOT / f"src/main/resources/assets/arcanecircle/textures/item/{item_id}.png"
    model = ROOT / f"src/main/resources/assets/arcanecircle/models/item/{item_id}.json"
    if not icon.is_file() or icon.stat().st_size < 32 or not model.is_file():
        raise SystemExit(f"missing custom mage item visual: {item_id}")

for source in (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic").glob("*.java"):
    if "sendParticles" in source.read_text(encoding="utf-8"):
        raise SystemExit(f"particle-centered spell call returned: {source.name}")

print("Arcane Circle v0.12.1-alpha.8 continuous magic, mage equipment, vitality and HUD contract: PASS")
