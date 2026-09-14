#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

import validate_alpha12_acceptance as alpha12
import validate_alpha14_acceptance as alpha14
import validate_alpha15_acceptance as alpha15

PROJECT = Path(__file__).resolve().parents[1]
JAVA = PROJECT / "src/main/java"
RESOURCES = PROJECT / "src/main/resources"


class AcceptanceError(RuntimeError):
    pass


def fail(message: str) -> None:
    raise AcceptanceError(message)


def text(path: Path) -> str:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(PROJECT)}")
    return path.read_text(encoding="utf-8")


def validate_target() -> None:
    props = text(PROJECT / "gradle.properties")
    for required in ("minecraft_version=26.2", "neo_version=26.2.0.76", "mod_version=0.1.0-alpha.16"):
        if required not in props:
            fail(f"target/version contract missing: {required}")

    mod = text(JAVA / "kr/moonseungjun/earthtostars/EarthToStars.java")
    if 'VERSION = "0.1.0-alpha.16"' not in mod:
        fail("runtime version is not alpha.16")


def validate_creative_access() -> None:
    items = text(JAVA / "kr/moonseungjun/earthtostars/content/EarthToStarsItems.java")
    tabs = text(JAVA / "kr/moonseungjun/earthtostars/content/EarthToStarsCreativeTabs.java")
    mod = text(JAVA / "kr/moonseungjun/earthtostars/EarthToStars.java")

    for required in (
        "CREATIVE_ITEMS = List.of(",
        "public static List<DeferredItem<? extends Item>> creativeItems()",
    ):
        if required not in items:
            fail(f"creative item registry contract missing: {required}")

    declared = set(re.findall(
        r"public static final DeferredItem<[^>]+>\s+([A-Z0-9_]+)\s*=",
        items,
    ))
    internal_only = {
        "STARTER_CRAFT_VISUAL",
        "ORBITAL_SALVAGE_VISUAL",
        "ORBITAL_INTERCEPTOR_VISUAL",
    }
    expected = declared - internal_only

    marker = "CREATIVE_ITEMS = List.of("
    start = items.find(marker)
    if start < 0:
        fail("creative item list not found")
    end = items.find(");", start)
    if end < 0:
        fail("creative item list is not closed")
    creative_block = items[start:end]

    missing = sorted(name for name in expected if name not in creative_block)
    leaked_internal = sorted(name for name in internal_only if name in creative_block)
    if missing:
        fail("player-facing registered items missing from creative access: " + ", ".join(missing))
    if leaked_internal:
        fail("render-only internal tokens leaked into creative tab: " + ", ".join(leaked_internal))

    for required in (
        "Registries.CREATIVE_MODE_TAB",
        'Component.translatable("itemGroup.earth_to_stars.main")',
        "EarthToStarsItems.LAUNCH_CRAFT_KIT.get()",
        "EarthToStarsItems.creativeItems()",
        "output.accept(item.get())",
        "TABS.register(modBus)",
    ):
        if required not in tabs:
            fail(f"custom creative tab contract missing: {required}")

    if "EarthToStarsCreativeTabs.register(modEventBus);" not in mod:
        fail("creative tab registry is not wired into mod initialization")

    for lang in ("en_us.json", "ko_kr.json"):
        lang_text = text(RESOURCES / "assets/earth_to_stars/lang" / lang)
        if '"itemGroup.earth_to_stars.main": "EARTH TO STARS"' not in lang_text:
            fail(f"creative tab translation missing from {lang}")


def main() -> None:
    try:
        alpha12.validate_no_runtime_proxies()
        alpha12.validate_26_2_runtime_api_contract()
        alpha12.validate_recipe_syntax()
        alpha15.validate_visible_vehicle_and_boarding()
        alpha14.validate_deployment_and_service_ux()
        alpha14.validate_runtime_models()
        validate_target()
        validate_creative_access()
    except (
        OSError,
        json.JSONDecodeError,
        AcceptanceError,
        alpha12.AcceptanceError,
        alpha14.AcceptanceError,
        alpha15.AcceptanceError,
    ) as exc:
        raise SystemExit(f"ALPHA.16 ACCEPTANCE VALIDATION FAILED: {exc}") from exc

    print(
        "ALPHA.16 ACCEPTANCE VALIDATION OK: alpha.15 boarding contracts preserved; "
        "dedicated EARTH TO STARS creative tab exposes every player-facing registered item "
        "while render-only tokens remain internal"
    )


if __name__ == "__main__":
    main()
