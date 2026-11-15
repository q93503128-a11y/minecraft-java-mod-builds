#!/usr/bin/env python3
"""Static packaging checks for the marionette workbench and visual registration."""
from collections import Counter
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/anomalousclasses"
DATA = ROOT / "src/main/resources/data/anomalousclasses"


def check(ok, message):
    if not ok:
        raise SystemExit("PUPPET ASSETS FAILED: " + message)


def j(path):
    return json.loads(path.read_text(encoding="utf-8"))


def dimensions(path):
    raw = path.read_bytes()
    check(raw[:8] == b"\x89PNG\r\n\x1a\n", f"invalid PNG: {path}")
    return struct.unpack(">II", raw[16:24])


def main():
    recipe = j(DATA / "recipe/puppet_workbench.json")
    check(recipe["type"] == "minecraft:crafting_shaped", "workbench recipe type")
    check(recipe["result"] == {"id": "anomalousclasses:puppet_workbench", "count": 1},
          "workbench recipe output")
    symbols = Counter("".join(recipe["pattern"]))
    check(dict(symbols) == {"P": 3, "S": 2, "C": 1, "T": 2, "W": 1},
          "workbench ingredients must occupy nine crafting cells")
    check(len(recipe["pattern"]) == 3 and all(len(row) == 3 for row in recipe["pattern"]),
          "workbench must be 3 x 3")
    check(j(DATA / "loot_table/blocks/puppet_workbench.json")["pools"][0]["entries"][0]["name"]
          == "anomalousclasses:puppet_workbench", "workbench drops itself")
    check("anomalousclasses:block/puppet_workbench"
          == j(ASSETS / "blockstates/puppet_workbench.json")["variants"][""]["model"],
          "missing blockstate renderer")
    check(j(ASSETS / "models/item/assembled_marionette.json")["textures"]["layer0"]
          == "anomalousclasses:item/assembled_marionette", "marionette item art")
    check(dimensions(ASSETS / "textures/item/assembled_marionette.png") == (32, 32),
          "item icon dimensions")
    check(j(ASSETS / "models/item/puppet_module.json")["textures"]["layer0"]
          == "anomalousclasses:item/puppet_module", "puppet component icon model")
    check(dimensions(ASSETS / "textures/item/puppet_module.png") == (32, 32),
          "puppet component icon dimensions")
    check(dimensions(ASSETS / "textures/entity/marionette.png") == (64, 64),
          "entity texture dimensions")

    common = {
        "block.anomalousclasses.puppet_workbench",
        "item.anomalousclasses.assembled_marionette",
        "entity.anomalousclasses.marionette",
        "screen.anomalousclasses.puppet_workbench.title",
        "screen.anomalousclasses.puppet_workbench.assemble",
        "screen.anomalousclasses.puppet_workbench.rejected",
        "message.anomalousclasses.puppet.no_space_class_change",
        "tooltip.anomalousclasses.puppet.broken",
        "screen.anomalousclasses.puppet_workbench.repair",
        "screen.anomalousclasses.puppet_workbench.expand",
        "screen.anomalousclasses.puppet_workbench.dismantle",
        "screen.anomalousclasses.puppet_workbench.confirm",
        "screen.anomalousclasses.puppet_workbench.assemble_choice",
        "screen.anomalousclasses.puppet_workbench.refund",
        "screen.anomalousclasses.puppet_workbench.refit",
        "screen.anomalousclasses.puppet_workbench.dismantle_confirm",
        "screen.anomalousclasses.puppet_workbench.dismantle_warning",
        "message.anomalousclasses.puppet_ability.failed_2",
        "item.anomalousclasses.puppet_module",
        "screen.anomalousclasses.puppet_workbench.repaired",
        "screen.anomalousclasses.puppet_workbench.dismantled",
    }
    for locale in ("ko_kr", "en_us"):
        strings = j(ASSETS / f"lang/{locale}.json")
        check(all(strings.get(key) for key in common), f"missing {locale} translation")
    print("PASS: original marionette PNGs, block/loot/item/recipe assets and workshop localization")


if __name__ == "__main__":
    main()
