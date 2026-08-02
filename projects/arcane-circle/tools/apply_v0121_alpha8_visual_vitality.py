#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json
import re
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")


def replace(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"missing alpha.8 patch anchor: {label}")
    return text.replace(old, new, 1)


def replace_regex(text: str, pattern: str, replacement: str, label: str) -> str:
    changed, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count == 0:
        if replacement in text:
            return text
        raise SystemExit(f"missing alpha.8 regex anchor: {label}")
    return changed


# Version and protocol.
props = read("gradle.properties")
props = props.replace("mod_version=0.12.1-alpha.7", "mod_version=0.12.1-alpha.8")
write("gradle.properties", props)

index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = "0.12.1-alpha.8"
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Stronger circle separation: each high-circle cast must be qualitatively stronger than low-circle spam.
catalog_path = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCatalog.java"
catalog = read(catalog_path)
old_damage = '''    public static double damageTierMultiplier(int circle) {
        return switch (circle) {
            case 2 -> 1.18;
            case 3 -> 1.42;
            case 4 -> 1.75;
            case 5 -> 2.15;
            case 6 -> 2.65;
            case 7 -> 3.25;
            case 8 -> 4.00;
            case 9 -> 5.00;
            default -> 1.00;
        };
    }'''
new_damage = '''    public static double damageTierMultiplier(int circle) {
        // Per-cast hierarchy is deliberately steep. High-circle spells also have longer cast times,
        // so this prevents rapid 1C spam from eclipsing a completed great ritual.
        return switch (circle) {
            case 2 -> 1.55;
            case 3 -> 2.35;
            case 4 -> 3.55;
            case 5 -> 5.40;
            case 6 -> 8.20;
            case 7 -> 12.50;
            case 8 -> 19.00;
            case 9 -> 29.00;
            default -> 1.00;
        };
    }'''
catalog = replace(catalog, old_damage, new_damage, "damage tier curve")
write(catalog_path, catalog)

# Snapshot effective HP alongside MP.
network_path = "src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java"
network = read(network_path)
network = replace(network,
                  "import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;",
                  "import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;\nimport kr.moonseungjun.arcanecircle.magic.ArcaneVitalityService;",
                  "vitality import")
network = network.replace('ninefold-arcana-12-1-alpha7', 'ninefold-arcana-12-1-alpha8')
network = replace(network,
                  '                + ";max=" + stats.maxMana()\n',
                  '                + ";max=" + stats.maxMana()\n'
                  '                + ";health=" + ArcaneVitalityService.effectiveHealth(player)\n'
                  '                + ";health_max=" + ArcaneVitalityService.effectiveMaxHealth(player)\n',
                  "health snapshot")
write(network_path, network)

# Custom HP bar and vanilla heart suppression.
hud_path = "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneHud.java"
hud = read(hud_path)
hud = replace(hud,
              "import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;\nimport net.neoforged.neoforge.client.event.ScreenEvent;",
              "import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;\n"
              "import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;\n"
              "import net.neoforged.neoforge.client.event.ScreenEvent;\n"
              "import net.neoforged.neoforge.client.gui.VanillaGuiLayers;",
              "HUD layer imports")
hud = replace(hud,
              "        for (int slot = 0; slot < 5; slot++) {\n"
              "            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);\n"
              "        }\n"
              "        drawFusionQueue(g, font, width, y - 15);",
              "        for (int slot = 0; slot < 5; slot++) {\n"
              "            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);\n"
              "        }\n"
              "        drawHealth(g, font, width, y + slotSize + 5);\n"
              "        drawFusionQueue(g, font, width, y - 15);",
              "health bar call")
health_method = '''
    public static void onVanillaLayer(RenderGuiLayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ArcaneClientState.ready()
                && VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static void drawHealth(GuiGraphicsExtractor g, Font font, int width, int y) {
        int health = Math.max(0, ArcaneClientState.integer("health", 0));
        int maximum = Math.max(1, ArcaneClientState.integer("health_max", 100));
        double ratio = Math.max(0.0, Math.min(1.0, health / (double) maximum));
        int barW = Math.min(194, Math.max(138, width / 4));
        int barH = 10;
        int x = (width - barW) / 2;
        int fill = (int) Math.round((barW - 4) * ratio);
        int red = ratio <= 0.22 ? 0xFFF12E3D : ratio <= 0.48 ? 0xFFE34A50 : 0xFFCE3545;
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xEF02040A);
        g.fill(x, y, x + barW, y + barH, 0xF0140A10);
        g.fill(x + 2, y + 2, x + 2 + fill, y + barH - 2, red);
        g.fill(x + 2, y + 2, x + 2 + fill, y + 3, 0xFFFF7378);
        tinyText(g, font, "HP " + health + " / " + maximum,
                width / 2, y + 1, 0xFFFFFFFF, 0.58F, true);
    }
'''
hud = replace(hud,
              "    private static void drawMana(GuiGraphicsExtractor g, Font font, int startX, int y) {",
              health_method + "\n    private static void drawMana(GuiGraphicsExtractor g, Font font, int startX, int y) {",
              "health methods")
hud = hud.replace('compactName("X " + chain + suffix, 34)', 'compactName(chain + suffix, 34)')
hud = hud.replace('"C 마도서 · 1~5 시전 · X 융합"', '"C 마도서 · 1~5 시전 · 숫자키 조합 융합"')
write(hud_path, hud)

client_path = "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircleClient.java"
client = read(client_path)
client = replace(client,
                 "import kr.moonseungjun.arcanecircle.client.ArcaneHud;",
                 "import kr.moonseungjun.arcanecircle.client.ArcaneHud;\n"
                 "import kr.moonseungjun.arcanecircle.client.ArcaneGearRenderer;",
                 "gear renderer import")
client = replace(client,
                 "        modEventBus.addListener(ArcaneHud::registerLayers);",
                 "        modEventBus.addListener(ArcaneHud::registerLayers);\n"
                 "        modEventBus.addListener(ArcaneGearRenderer::registerStateModifiers);",
                 "gear modifier registration")
client = replace(client,
                 "        NeoForge.EVENT_BUS.addListener(ArcaneHud::onScreenRender);",
                 "        NeoForge.EVENT_BUS.addListener(ArcaneHud::onScreenRender);\n"
                 "        NeoForge.EVENT_BUS.addListener(ArcaneHud::onVanillaLayer);\n"
                 "        NeoForge.EVENT_BUS.addListener(ArcaneGearRenderer::onPlayerRender);",
                 "client render listeners")
write(client_path, client)

# Replace inherited leather/gold/diamond equipment assets with three Arcane materials.
items_path = "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java"
items = read(items_path)
items = items.replace("import net.minecraft.world.item.equipment.ArmorMaterials;\n", "")
material_for = {
    "MAGE_HAT": "ArcaneArmorMaterials.MAGE",
    "MAGE_ROBE": "ArcaneArmorMaterials.MAGE",
    "MAGE_ROBE_HEM": "ArcaneArmorMaterials.MAGE",
    "MAGE_BOOTS": "ArcaneArmorMaterials.MAGE",
    "SAGE_HAT": "ArcaneArmorMaterials.SAGE",
    "SAGE_ROBE": "ArcaneArmorMaterials.SAGE",
    "SAGE_ROBE_HEM": "ArcaneArmorMaterials.SAGE",
    "SKYWALKER_BOOTS": "ArcaneArmorMaterials.SAGE",
    "ARCHMAGE_CROWN": "ArcaneArmorMaterials.ARCHMAGE",
    "ARCHMAGE_ROBE": "ArcaneArmorMaterials.ARCHMAGE",
    "ARCHMAGE_ROBE_HEM": "ArcaneArmorMaterials.ARCHMAGE",
    "FROSTSTEP_BOOTS": "ArcaneArmorMaterials.ARCHMAGE",
}
for field, material in material_for.items():
    pattern = rf'(public static final DeferredItem<Item> {field} = ITEMS\.registerItem\("[^"]+",\s*properties -> new Item\(properties\.rarity\(Rarity\.[A-Z]+\)\s*\.humanoidArmor\()ArmorMaterials\.[A-Z]+'
    replacement = rf'\1{material}'
    items, count = re.subn(pattern, replacement, items, count=1, flags=re.S)
    if count != 1 and material not in items:
        raise SystemExit(f"failed to install custom material for {field}")
write(items_path, items)

# Item icons and wearable texture layers, generated deterministically with no image-generation service.
ASSET = ROOT / "src/main/resources/assets/arcanecircle"


def png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    encoded = b"\x89PNG\r\n\x1a\n"
    encoded += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    encoded += chunk(b"IDAT", zlib.compress(raw, 9))
    encoded += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(encoded)


def canvas(w: int, h: int) -> list[list[tuple[int, int, int, int]]]:
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]


def rect(img, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(img), y1)):
        for x in range(max(0, x0), min(len(img[0]), x1)):
            img[y][x] = color


def line(img, x0, y0, x1, y1, color):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        if 0 <= x0 < len(img[0]) and 0 <= y0 < len(img):
            img[y0][x0] = color
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


palettes = {
    "mage": ((52, 35, 77, 255), (105, 69, 139, 255), (239, 182, 255, 255)),
    "sage": ((28, 48, 92, 255), (48, 102, 145, 255), (134, 223, 255, 255)),
    "archmage": ((42, 18, 66, 255), (100, 48, 114, 255), (255, 213, 106, 255)),
}

def hat_icon(tier: str, crown: bool = False):
    dark, body, trim = palettes[tier]
    img = canvas(32, 32)
    if crown:
        rect(img, 7, 18, 25, 23, body)
        for x in (8, 13, 18, 23):
            line(img, x, 18, x + (1 if x % 2 else -1), 8 + (x % 3), trim)
            rect(img, x - 1, 8 + (x % 3), x + 2, 11 + (x % 3), trim)
        rect(img, 6, 23, 26, 26, dark)
    else:
        for y in range(5, 23):
            half = max(1, (y - 4) // 2)
            rect(img, 16 - half, y, 17 + half, y + 1, body if y % 3 else dark)
        line(img, 16, 5, 22, 10, trim)
        rect(img, 5, 22, 27, 26, dark)
        rect(img, 7, 21, 25, 23, body)
        rect(img, 12, 16, 20, 18, trim)
    return img


def robe_icon(tier: str):
    dark, body, trim = palettes[tier]
    img = canvas(32, 32)
    rect(img, 9, 5, 23, 12, body)
    rect(img, 6, 7, 10, 17, dark)
    rect(img, 22, 7, 26, 17, dark)
    for y in range(12, 29):
        spread = 2 + (y - 12) // 4
        rect(img, 16 - spread, y, 16 + spread + 1, y + 1, body if y % 2 else dark)
    rect(img, 9, 12, 23, 15, trim)
    line(img, 16, 15, 16, 28, trim)
    line(img, 8, 28, 24, 28, trim)
    return img


def boots_icon(tier: str):
    dark, body, trim = palettes[tier]
    img = canvas(32, 32)
    rect(img, 7, 7, 15, 23, body)
    rect(img, 18, 7, 26, 23, body)
    rect(img, 5, 21, 15, 27, dark)
    rect(img, 18, 21, 28, 27, dark)
    rect(img, 7, 13, 15, 16, trim)
    rect(img, 18, 13, 26, 16, trim)
    line(img, 6, 25, 15, 25, trim)
    line(img, 18, 25, 27, 25, trim)
    return img

icons = {
    "mage_hat": hat_icon("mage"), "sage_hat": hat_icon("sage"),
    "archmage_crown": hat_icon("archmage", True),
    "mage_robe": robe_icon("mage"), "mage_robe_hem": robe_icon("mage"),
    "sage_robe": robe_icon("sage"), "sage_robe_hem": robe_icon("sage"),
    "archmage_robe": robe_icon("archmage"), "archmage_robe_hem": robe_icon("archmage"),
    "mage_boots": boots_icon("mage"), "skywalker_boots": boots_icon("sage"),
    "froststep_boots": boots_icon("archmage"),
}
for item_id, pixels in icons.items():
    png(ASSET / f"textures/item/{item_id}.png", pixels)
    write(f"src/main/resources/assets/arcanecircle/models/item/{item_id}.json",
          json.dumps({"parent": "minecraft:item/generated",
                      "textures": {"layer0": f"arcanecircle:item/{item_id}"}}, separators=(",", ":")) + "\n")


def equipment_texture(tier: str, leggings: bool):
    dark, body, trim = palettes[tier]
    img = canvas(64, 32)
    # Cover the standard humanoid equipment UV islands, then add readable runic bands.
    rect(img, 0, 0, 64, 32, body)
    rect(img, 0, 0, 64, 4, dark)
    rect(img, 0, 28, 64, 32, dark)
    for x in range(2, 62, 8):
        line(img, x, 5, x + 3, 8, trim)
        line(img, x + 3, 8, x + 6, 5, trim)
    if leggings:
        rect(img, 0, 14, 64, 18, dark)
        for x in range(4, 60, 12):
            line(img, x, 15, x + 4, 17, trim)
            line(img, x + 4, 17, x + 8, 15, trim)
    else:
        rect(img, 16, 8, 48, 12, dark)
        rect(img, 27, 12, 37, 16, trim)
    return img

for tier in palettes:
    equipment = {
        "layers": {
            "humanoid": [{"texture": f"arcanecircle:{tier}/outer"}],
            "humanoid_leggings": [{"texture": f"arcanecircle:{tier}/inner"}],
        }
    }
    write(f"src/main/resources/assets/arcanecircle/equipment/{tier}.json",
          json.dumps(equipment, ensure_ascii=False, indent=2) + "\n")
    png(ASSET / f"textures/entity/equipment/humanoid/{tier}/outer.png",
        equipment_texture(tier, False))
    png(ASSET / f"textures/entity/equipment/humanoid_leggings/{tier}/inner.png",
        equipment_texture(tier, True))

print("Arcane Circle v0.12.1-alpha.8 visual, vitality and equipment migration applied")
