#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

import validate_alpha12_acceptance as alpha12
import validate_alpha14_acceptance as alpha14

PROJECT = Path(__file__).resolve().parents[1]
JAVA = PROJECT / "src/main/java"


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
    for required in ("minecraft_version=26.2", "neo_version=26.2.0.76", "mod_version=0.1.0-alpha.15"):
        if required not in props:
            fail(f"target/version contract missing: {required}")


def validate_visible_vehicle_and_boarding() -> None:
    exterior = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipExteriorEntity.java")
    if "extends Display.ItemDisplay" not in exterior:
        fail("starter craft exterior is no longer the visible vehicle")
    for required in ("setVisualItem", "isPickable()", "boardAndControl(serverPlayer, this)", "public boolean shouldBeSaved()", "return false;"):
        if required not in exterior:
            fail(f"visible/ephemeral vehicle contract missing: {required}")

    entities = text(JAVA / "kr/moonseungjun/earthtostars/content/EarthToStarsEntities.java")
    if ".noSave()" in entities:
        fail("ship exterior EntityType is still non-serializable; Minecraft 26.2 startRiding will always reject it")
    for required in (".sized(5.4F, 2.3F)", "EntityAttachment.PASSENGER, 0.0F, 1.20F, 0.35F"):
        if required not in entities:
            fail(f"starter hull/seat contract missing: {required}")

    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")
    for required in (
        "player.startRiding(exterior, true, true)",
        "teleported.startRiding(nextPair.exterior(), true, true)",
        "returned.startRiding(entry.exterior(), true, true)",
        "if (!boardAndControl(player, entry.exterior(), tick))",
        "rollbackFailedDeployment(level.getServer(), entry)",
        "private static void rollbackFailedDeployment",
        "new VehiclePair(exterior, exterior)",
    ):
        if required not in runtime:
            fail(f"authoritative boarding/rollback contract missing: {required}")
    if "player.startRiding(exterior)" in runtime:
        fail("non-forced primary boarding path remains")

    probe = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipLifecycleProbe.java")
    for required in ("boarding_contract", "type.canSerialize()", "exterior.shouldBeSaved()", "EARTH_TO_STARS_ALPHA15_BOARDING_ENTITY_CONTRACT_PASS"):
        if required not in probe:
            fail(f"dedicated boarding entity probe missing: {required}")


def main() -> None:
    try:
        alpha12.validate_no_runtime_proxies()
        alpha12.validate_26_2_runtime_api_contract()
        alpha12.validate_recipe_syntax()
        validate_target()
        validate_visible_vehicle_and_boarding()
        alpha14.validate_deployment_and_service_ux()
        alpha14.validate_runtime_models()
    except (OSError, json.JSONDecodeError, AcceptanceError, alpha12.AcceptanceError, alpha14.AcceptanceError) as exc:
        raise SystemExit(f"ALPHA.15 ACCEPTANCE VALIDATION FAILED: {exc}") from exc
    print("ALPHA.15 ACCEPTANCE VALIDATION OK: rideable serializable EntityType, non-persisted runtime exterior, forced authorized mounting, failed-deployment rollback and alpha.14 visual/UX contracts verified")


if __name__ == "__main__":
    main()
