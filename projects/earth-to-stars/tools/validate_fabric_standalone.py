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


def main() -> None:
    build = PROJECT / "build.gradle"
    props = PROJECT / "gradle.properties"
    settings = PROJECT / "settings.gradle"
    metadata = PROJECT / "src/fabric/resources/fabric.mod.json"
    entrypoint = PROJECT / "src/fabric/java/kr/moonseungjun/earthtostars/fabric/EarthToStarsFabric.java"

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
        "Fabric 26.2 standalone kernel loaded",
    ):
        require(entrypoint, needle)

    # The standalone product may research or port permissively licensed code, but it must
    # not silently revert to requiring whole external ship/space mods at runtime.
    for path in (build, metadata):
        for forbidden in ("valkyrienskies", "vs-genesis", "genesis", "zps", "zpl", "kotlinforforge"):
            forbid(path, forbidden)

    print(
        "FABRIC STANDALONE VALIDATION OK: Minecraft 26.2 + Fabric Loader 0.19.5 + "
        "Fabric API 0.160.0; loader-neutral ship kernel retained; whole external ship/space "
        "mods are not runtime dependencies"
    )


if __name__ == "__main__":
    main()
