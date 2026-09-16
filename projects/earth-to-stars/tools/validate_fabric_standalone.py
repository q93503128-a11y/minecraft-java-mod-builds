#!/usr/bin/env python3
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]


def require(path: Path, needle: str) -> None:
    text = path.read_text(encoding="utf-8")
    if needle not in text:
        raise SystemExit(f"FABRIC STANDALONE VALIDATION FAILED: {path.relative_to(PROJECT)} missing {needle!r}")


def forbid(path: Path, needle: str) -> None:
    text = path.read_text(encoding="utf-8").lower()
    if needle.lower() in text:
        raise SystemExit(f"FABRIC STANDALONE VALIDATION FAILED: {path.relative_to(PROJECT)} still contains runtime dependency marker {needle!r}")


def require_file(path: Path) -> None:
    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit(f"FABRIC STANDALONE VALIDATION FAILED: missing non-empty {path.relative_to(PROJECT)}")


def main() -> None:
    build = PROJECT / "build.gradle"
    props = PROJECT / "gradle.properties"
    settings = PROJECT / "settings.gradle"
    metadata = PROJECT / "src/fabric/resources/fabric.mod.json"
    entrypoint = PROJECT / "src/fabric/java/kr/moonseungjun/earthtostars/fabric/EarthToStarsFabric.java"
    items = PROJECT / "src/fabric/java/kr/moonseungjun/earthtostars/fabric/content/EarthToStarsFabricItems.java"
    networking = PROJECT / "src/fabric/java/kr/moonseungjun/earthtostars/fabric/networking/EarthToStarsFabricNetworking.java"
    authority = PROJECT / "src/fabric/java/kr/moonseungjun/earthtostars/fabric/ship/EarthToStarsFabricShipAuthority.java"
    saved_data = PROJECT / "src/fabric/java/kr/moonseungjun/earthtostars/fabric/persistence/EarthToStarsFabricShipSavedData.java"

    for needle in (
        "net.fabricmc.fabric-loom",
        "com.mojang:minecraft:${project.minecraft_version}",
        "net.fabricmc:fabric-loader:${project.loader_version}",
        "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}",
        "src/fabric/java",
        "src/main/java",
        "ship/client/**",
        "ship/networking/**",
        "ship/persistence/minecraft/**",
        "ship/runtime/minecraft/**",
        "options.release = 25",
    ):
        require(build, needle)

    for needle in (
        "minecraft_version=26.2",
        "loader_version=0.19.5",
        "loom_version=1.17.19",
        "fabric_api_version=0.160.0+26.2",
        "mod_version=0.3.0-alpha.1",
    ):
        require(props, needle)

    require(settings, "https://maven.fabricmc.net/")

    for needle in (
        '"id": "earth_to_stars"',
        '"fabricloader": ">=0.19.5"',
        '"minecraft": "~26.2"',
        '"java": ">=25"',
        '"fabric-api": ">=0.160.0"',
        "kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric",
    ):
        require(metadata, needle)

    for needle in (
        "implements ModInitializer",
        'VERSION = "0.3.0-alpha.1"',
        "ShipBootstrapCatalog.create()",
        "EarthToStarsFabricItems.initialize()",
        "EarthToStarsFabricNetworking.initialize()",
        "EarthToStarsFabricShipAuthority.initializeLifecycle()",
        "Fabric 26.2 standalone kernel loaded",
    ):
        require(entrypoint, needle)

    for needle in (
        'key("reinforced_frame")',
        'key("avionics_unit")',
        'key("life_support_unit")',
        "properties.setId(key)",
        "Registry.register(BuiltInRegistries.ITEM, key, item)",
        "CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)",
    ):
        require(items, needle)

    for needle in (
        "PayloadTypeRegistry.serverboundPlay().register",
        "PayloadTypeRegistry.clientboundPlay().register",
        "ServerPlayNetworking.registerGlobalReceiver",
        "EarthToStarsFabricShipAuthority.acceptControlInput",
    ):
        require(networking, needle)

    for needle in (
        "ShipRepository",
        "EarthToStarsFabricShipSavedData.get(server)",
        "ServerLifecycleEvents.SERVER_STARTED.register",
        "ServerLifecycleEvents.SERVER_STOPPED.register",
        "runtime.requestControl",
        "runtime.acceptInput",
        "CONTROLLER_BINDINGS",
    ):
        require(authority, needle)

    for needle in (
        "SavedDataType",
        "ShipStateCodec.encode(state)",
        "ShipStateCodec.decode",
        "setDirty()",
    ):
        require(saved_data, needle)

    for relative in (
        "src/fabric/java/kr/moonseungjun/earthtostars/fabric/networking/ShipControlInputPayload.java",
        "src/fabric/java/kr/moonseungjun/earthtostars/fabric/networking/ShipControlSessionPayload.java",
        "src/fabric/resources/assets/earth_to_stars/items/reinforced_frame.json",
        "src/fabric/resources/assets/earth_to_stars/items/avionics_unit.json",
        "src/fabric/resources/assets/earth_to_stars/items/life_support_unit.json",
        "src/fabric/resources/assets/earth_to_stars/models/item/reinforced_frame.json",
        "src/fabric/resources/assets/earth_to_stars/models/item/avionics_unit.json",
        "src/fabric/resources/assets/earth_to_stars/models/item/life_support_unit.json",
        "src/fabric/resources/assets/earth_to_stars/lang/ko_kr.json",
        "src/fabric/resources/assets/earth_to_stars/lang/en_us.json",
        "src/fabric/resources/data/earth_to_stars/bootstrap/kernel.json",
    ):
        require_file(PROJECT / relative)

    # The standalone product may research or port permissively licensed code, but it must
    # not silently revert to requiring whole external ship/space mods at runtime.
    for path in (build, metadata):
        for forbidden in ("valkyrienskies", "vs-genesis", "genesis", "zps", "zpl", "kotlinforforge"):
            forbid(path, forbidden)

    print(
        "FABRIC STANDALONE VALIDATION OK: Minecraft 26.2 + Fabric Loader 0.19.5 + "
        "Fabric API 0.160.0; loader-neutral ship kernel retained; Fabric construction components, "
        "network session authority and ShipStateCodec-backed SavedData bridge present; whole external "
        "ship/space mods are not runtime dependencies"
    )


if __name__ == "__main__":
    main()
