import json
import re
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java/kr/moonseungjun/anomalousclasses"
RESOURCES = ROOT / "src/main/resources"


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def rectangle(x, y, width, height):
    return x, y, x + width, y + height


def overlaps(first, second):
    return first[0] < second[2] and first[2] > second[0] and first[1] < second[3] and first[3] > second[1]


menu_mixin = (MAIN / "mixin/InventoryMenuFocusMixin.java").read_text(encoding="utf-8")
screen_mixin = (MAIN / "mixin/InventoryScreenFocusMixin.java").read_text(encoding="utf-8")
mixins = json.loads((RESOURCES / "anomalousclasses.mixins.json").read_text(encoding="utf-8"))
require("anomalousclasses$focusIndex = 46" in menu_mixin, "focus menu slot is not index 46")
require("anomalousclasses$focusX = 77" in menu_mixin and "anomalousclasses$focusY = 35" in menu_mixin,
        "focus menu position changed without updating the layout contract")
require("InventoryScreenFocusMixin" in mixins.get("client", []), "client inventory frame mixin is not client-only registered")
require('method = "renderBg"' in screen_mixin and '"container/slot"' in screen_mixin,
        "vanilla slot frame is not drawn in InventoryScreen background")
require("textures/gui/sprites/focus_slot/empty.png" in screen_mixin,
        "dedicated empty-slot marker is not rendered from its standalone texture")
require("focusSlot.hasItem()" in screen_mixin and "graphics.blit(anomalousclasses$emptyMarker" in screen_mixin,
        "empty marker must only appear when the real synchronized slot is empty")
require("leftPos + 77, topPos + 35" in screen_mixin and "16, 16, 16, 16" in screen_mixin,
        "empty marker render rectangle must match the real 16x16 clickable slot")
require("setBackground(" not in menu_mixin,
        "standalone marker must not depend on a sprite atlas registration")
require("leftPos + 76, topPos + 34, 18, 18" in screen_mixin,
        "vanilla slot frame does not align around slot 46")
require("mouseClicked" not in screen_mixin and "quickMove" not in screen_mixin,
        "UI-only inventory mixin must not alter slot transactions")

marker_path = RESOURCES / "assets/anomalousclasses/textures/gui/sprites/focus_slot/empty.png"
marker = marker_path.read_bytes()
require(marker[:8] == b"\x89PNG\r\n\x1a\n", "focus empty marker is not a PNG")
width, height, bit_depth, color_type = struct.unpack(">IIBB", marker[16:26])
require((width, height, bit_depth, color_type) == (16, 16, 8, 6),
        "empty marker must be a 16x16 RGBA sprite for one vanilla slot")
idat = bytearray()
offset = 8
while offset < len(marker):
    length = struct.unpack(">I", marker[offset:offset + 4])[0]
    kind = marker[offset + 4:offset + 8]
    data = marker[offset + 8:offset + 8 + length]
    if kind == b"IDAT":
        idat.extend(data)
    offset += length + 12
raw = zlib.decompress(idat)
require(len(raw) == 16 * (1 + 16 * 4), "unexpected empty marker pixel payload")
alpha_values = []
for y in range(16):
    row = raw[y * 65:(y + 1) * 65]
    require(row[0] == 0, "empty marker uses an unsupported scanline filter")
    alpha_values.extend(row[4::4])
require(any(alpha > 0 for alpha in alpha_values) and any(alpha == 0 for alpha in alpha_values),
        "empty marker must have visible pixels and transparent margins")

# Vanilla InventoryScreen 1.21.1 renders its player preview in the [26,75) x [8,78) area.
focus_click = rectangle(77, 35, 16, 16)
focus_frame = rectangle(76, 34, 18, 18)
player_preview = rectangle(26, 8, 49, 70)
vanilla_slots = [
    rectangle(8, 8 + row * 18, 16, 16) for row in range(4)
]
vanilla_slots.append(rectangle(77, 62, 16, 16))  # offhand
vanilla_slots.extend(rectangle(98 + column * 18, 26 + row * 18, 16, 16)
                     for row in range(3) for column in range(3))
vanilla_slots.append(rectangle(154, 28, 16, 16))  # crafting result
vanilla_slots.extend(rectangle(8 + column * 18, 84 + row * 18, 16, 16)
                     for row in range(3) for column in range(9))
vanilla_slots.extend(rectangle(8 + column * 18, 142, 16, 16) for column in range(9))
require(not overlaps(focus_frame, player_preview), "slot frame overlaps the vanilla player preview")
require(all(not overlaps(focus_click, slot) for slot in vanilla_slots), "slot 46 overlaps a vanilla slot")
require(focus_frame[0] + 18 < 98 and focus_frame[1] + 18 < 62,
        "slot frame does not keep clearance from crafting/offhand slots")

viewport_sizes = (("16:9", 1920, 1080), ("16:10", 1680, 1050), ("4:3", 1440, 1080))
for aspect, pixel_width, pixel_height in viewport_sizes:
    for gui_scale in (2, 3, 4):
        logical_width = (pixel_width + gui_scale - 1) // gui_scale
        logical_height = (pixel_height + gui_scale - 1) // gui_scale
        left = (logical_width - 176) // 2
        top = (logical_height - 166) // 2
        frame = rectangle(left + 76, top + 34, 18, 18)
        marker = rectangle(left + 77, top + 35, 16, 16)
        require(frame[0] >= 0 and frame[1] >= 0 and frame[2] <= logical_width and frame[3] <= logical_height,
                f"slot frame leaves the screen at {aspect}, GUI Scale {gui_scale}")
        require(marker[0] == frame[0] + 1 and marker[1] == frame[1] + 1
                and marker[2] == frame[2] - 1 and marker[3] == frame[3] - 1,
                f"marker and 16x16 hitbox do not align at {aspect}, GUI Scale {gui_scale}")

screen = (MAIN / "client/ClassFocusScreen.java").read_text(encoding="utf-8")
require("ClassFocusActionPayload" not in screen and "sendAction(" not in screen,
        "recipe guide still exposes focus equipment transactions")
require("equipButton" not in screen and "unequipButton" not in screen,
        "recipe guide still creates equip or unequip controls")
require("ingredientAt(row, column)" in screen and "renderTooltip(font, ClassFocusItems.stack" in screen,
        "recipe ingredient/result hover tooltips are missing")

rules = (MAIN / "state/MentalDominatorRules.java").read_text(encoding="utf-8")
for rule_name, ui_name in (
    ("FOCUS_MAX_PSYCHIC", "MAX_PSYCHE_PERCENT"),
    ("FOCUS_UPKEEP_REDUCTION", "UPKEEP_REDUCTION_PERCENT"),
    ("FOCUS_INTRUSION_BONUS", "INTRUSION_PROGRESS_PERCENT"),
):
    server_match = re.search(rf"{rule_name}\s*=\s*\{{([^}}]+)}}", rules)
    ui_match = re.search(rf"{ui_name}\s*=\s*\{{([^}}]+)}}", screen)
    require(server_match and ui_match, f"missing server/UI focus effect values: {rule_name}/{ui_name}")
    server_values = [round(float(value.strip()) * 100) for value in server_match.group(1).split(",")]
    ui_values = [int(value.strip()) for value in ui_match.group(1).split(",")]
    require(server_values == ui_values, f"displayed effects differ from server values: {rule_name}")

for locale in ("en_us", "ko_kr"):
    lang = json.loads((RESOURCES / f"assets/anomalousclasses/lang/{locale}.json").read_text(encoding="utf-8"))
    for key in (
        "screen.anomalousclasses.class_focus.effect.max_psyche",
        "screen.anomalousclasses.class_focus.effect.upkeep",
        "screen.anomalousclasses.class_focus.effect.intrusion",
        "screen.anomalousclasses.mental.focus",
    ):
        require(key in lang, f"missing {locale} translation: {key}")

print("Focus inventory UI: slot 46 geometry, vanilla slot/player-preview clearance, marker texture, 9 aspect/GUI-scale viewports, read-only recipe actions, effect values, and ko/en strings passed.")
