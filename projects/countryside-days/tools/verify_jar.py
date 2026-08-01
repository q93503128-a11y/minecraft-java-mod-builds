#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_json_utf8(archive: zipfile.ZipFile, path: str) -> dict[str, object]:
    try:
        raw = archive.read(path)
        text = raw.decode("utf-8", errors="strict")
        parsed = json.loads(text)
    except UnicodeDecodeError as error:
        fail(f"invalid UTF-8 in {path}: {error}")
    except json.JSONDecodeError as error:
        fail(f"invalid JSON in {path}: {error}")
    if not isinstance(parsed, dict):
        fail(f"localization root must be an object: {path}")
    return parsed


def export_source_snapshot() -> Path:
    project_root = Path.cwd()
    destination = project_root / "build" / "deliverables" / "countryside-days-alpha.12-source-snapshot.zip"
    destination.parent.mkdir(parents=True, exist_ok=True)
    included_roots = (
        project_root / "src",
        project_root / "tools",
    )
    included_files = (
        project_root / "build.gradle",
        project_root / "gradle.properties",
        project_root / "settings.gradle",
    )
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for root in included_roots:
            if not root.exists():
                continue
            for path in sorted(root.rglob("*")):
                if path.is_file():
                    archive.write(path, path.relative_to(project_root).as_posix())
        for path in included_files:
            if path.is_file():
                archive.write(path, path.relative_to(project_root).as_posix())
    return destination


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify a built Countryside Days NeoForge JAR")
    parser.add_argument("jar", type=Path)
    args = parser.parse_args()

    jar_path: Path = args.jar
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"missing or empty JAR: {jar_path}")

    with zipfile.ZipFile(jar_path) as archive:
        bad_entry = archive.testzip()
        if bad_entry is not None:
            fail(f"corrupt ZIP entry: {bad_entry}")

        names = archive.namelist()
        unique_names = set(names)
        if len(names) != len(unique_names):
            fail("duplicate ZIP entries detected")

        required_exact = {
            "META-INF/neoforge.mods.toml",
            "assets/countrysidedays/lang/ko_kr.json",
            "assets/countrysidedays/lang/en_us.json",
            "assets/countrysidedays/items/village_coin.json",
            "assets/countrysidedays/models/item/village_coin.json",
            "assets/countrysidedays/items/herb_tea.json",
            "assets/countrysidedays/models/item/herb_tea.json",
            "assets/countrysidedays/items/farm_breakfast.json",
            "assets/countrysidedays/models/item/farm_breakfast.json",
            "assets/countrysidedays/items/grilled_river_fish.json",
            "assets/countrysidedays/models/item/grilled_river_fish.json",
            "assets/countrysidedays/items/potato_pancake.json",
            "assets/countrysidedays/models/item/potato_pancake.json",
            "assets/countrysidedays/items/honey_carrot_salad.json",
            "assets/countrysidedays/models/item/honey_carrot_salad.json",
            "assets/countrysidedays/items/recipe_notebook.json",
            "assets/countrysidedays/models/item/recipe_notebook.json",
            "assets/countrysidedays/items/life_guide.json",
            "assets/countrysidedays/models/item/life_guide.json",
            "data/countrysidedays/recipe/herb_tea.json",
            "data/countrysidedays/recipe/farm_breakfast.json",
            "data/countrysidedays/recipe/grilled_river_fish.json",
            "data/countrysidedays/recipe/potato_pancake.json",
            "data/countrysidedays/recipe/honey_carrot_salad.json",
            "kr/countrysidedays/item/RecipeNotebookItem.class",
            "kr/countrysidedays/item/LifeGuideItem.class",
            "kr/countrysidedays/gameplay/CountrysideFishingManager.class",
            "kr/countrysidedays/gameplay/EstateWorkerManager.class",
            "kr/countrysidedays/gameplay/KitchenInteractionHandler.class",
            "kr/countrysidedays/gameplay/RanchLifeManager.class",
            "kr/countrysidedays/gameplay/RuralNpcManager.class",
            "kr/countrysidedays/gameplay/SharedRestaurantAccess.class",
            "kr/countrysidedays/gameplay/VillageLifeManager.class",
            "kr/countrysidedays/world/CountrysideWorldData.class",
            "kr/countrysidedays/world/CountrysideWorldData$PlayerEstate.class",
            "kr/countrysidedays/world/CountrysideWorldData$RanchProducts.class",
            "kr/countrysidedays/world/CountrysidePropertyManager.class",
            "kr/countrysidedays/world/CountrysideRegionManager.class",
            "kr/countrysidedays/world/FlatCountrysideBootstrap.class",
            "kr/countrysidedays/world/PlayerEstateLayout.class",
            "kr/countrysidedays/world/PublicVillageExpansionBuilder.class",
            "kr/countrysidedays/world/SharedRestaurantBuilder.class",
            "kr/countrysidedays/network/EstateHudPayload.class",
            "kr/countrysidedays/client/ClientEstateState.class",
            "kr/countrysidedays/client/CountrysideHud.class",
            "data/countrysidedays/worldgen/biome/rural_plains.json",
            "data/minecraft/worldgen/world_preset/flat.json",
            "data/minecraft/worldgen/flat_level_generator_preset/classic_flat.json",
        }
        for required in required_exact:
            if required not in unique_names:
                fail(f"required entry missing: {required}")

        korean = load_json_utf8(archive, "assets/countrysidedays/lang/ko_kr.json")
        english = load_json_utf8(archive, "assets/countrysidedays/lang/en_us.json")
        if set(korean) != set(english):
            missing_ko = sorted(set(english) - set(korean))
            missing_en = sorted(set(korean) - set(english))
            fail(f"localization key mismatch; missing Korean={missing_ko}, missing English={missing_en}")
        for key in (
            "hud.countrysidedays.shift_open",
            "hud.countrysidedays.shift_closed",
            "hud.countrysidedays.goal_open_first_shift",
            "item.countrysidedays.recipe_notebook",
            "item.countrysidedays.life_guide",
            "item.countrysidedays.grilled_river_fish",
            "item.countrysidedays.potato_pancake",
            "item.countrysidedays.honey_carrot_salad",
            "message.countrysidedays.restaurant_staff_only",
            "message.countrysidedays.restaurant_role_owner",
            "message.countrysidedays.restaurant_role_staff",
            "message.countrysidedays.fishing_public_only",
            "message.countrysidedays.worker_hired",
            "message.countrysidedays.worker_status",
            "message.countrysidedays.public_livestock_protected",
        ):
            value = korean.get(key)
            if not isinstance(value, str) or not value.strip():
                fail(f"missing Korean localization text for {key}")
        if str(korean["hud.countrysidedays.shift_closed"]).startswith("CLOSED"):
            fail("Korean HUD unexpectedly fell back to English text")

        obsolete_entries = {
            "data/countrysidedays/neoforge/biome_modifier/countryside_generation.json",
        }
        leaked = sorted(obsolete_entries.intersection(unique_names))
        if leaked:
            fail(f"obsolete ordinary-plains worldgen injection leaked into JAR: {leaked}")

        required_prefixes = {
            "kr/countrysidedays/": ".class",
            "assets/countrysidedays/": None,
            "data/countrysidedays/": None,
        }
        for prefix, suffix in required_prefixes.items():
            matches = [name for name in names if name.startswith(prefix)]
            if suffix is not None:
                matches = [name for name in matches if name.endswith(suffix)]
            if not matches:
                fail(f"required JAR content missing under {prefix}")

        forbidden = [
            name
            for name in names
            if name.endswith(".java")
            or name.startswith(".github/")
            or name.startswith("tools/")
            or name.startswith("docs/")
        ]
        if forbidden:
            fail(f"development-only files leaked into JAR: {forbidden[:10]}")

    snapshot = export_source_snapshot()
    print(f"Verified {jar_path} ({jar_path.stat().st_size} bytes)")
    print(f"Exported temporary source snapshot to {snapshot}")


if __name__ == "__main__":
    main()
