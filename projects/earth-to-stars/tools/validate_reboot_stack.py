#!/usr/bin/env python3
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]


def require(path: Path, needle: str) -> None:
    text = path.read_text(encoding="utf-8")
    if needle not in text:
        raise SystemExit(f"REBOOT STACK VALIDATION FAILED: {path.relative_to(PROJECT)} missing {needle!r}")


def main() -> None:
    build = PROJECT / "build.gradle"
    props = PROJECT / "gradle.properties"
    mod = PROJECT / "src/reboot/java/kr/moonseungjun/earthtostars/EarthToStars.java"
    toml = PROJECT / "src/reboot/resources/META-INF/mods.toml"

    for needle in (
        "src/reboot/java",
        "src/reboot/resources",
        "net.neoforged.moddev.legacyforge",
        "maven.modrinth:valkyrien-skies:1.20.1-forge-2.4.10",
        "maven.modrinth:vs-genesis:1.20.1-0.7.3",
        "maven.modrinth:zps:1.20.1-2.4.0",
        "maven.modrinth:zpl:1.20.1-1.5.0",
    ):
        require(build, needle)

    for needle in (
        "minecraft_version=1.20.1",
        "forge_version=1.20.1-47.4.0",
        "mod_version=0.2.0-alpha.1",
    ):
        require(props, needle)

    for needle in (
        'VERSION = "0.2.0-alpha.1"',
        '"valkyrienskies"',
        '"genesis"',
        '"zps"',
        '"zpl"',
        "EARTH_TO_STARS_REBOOT_STACK_BOOT_PASS",
    ):
        require(mod, needle)

    for mod_id in ("valkyrienskies", "genesis", "vlib", "zps", "zpl"):
        require(toml, f'modId="{mod_id}"')

    # The reboot must not compile the failed 26.2 Display-entity ship implementation.
    build_text = build.read_text(encoding="utf-8")
    if "src/main/java" in build_text or "src/main/resources" in build_text:
        raise SystemExit("REBOOT STACK VALIDATION FAILED: legacy 26.2 source tree leaked into reboot sourceSets")

    reboot_text = "\n".join(
        path.read_text(encoding="utf-8")
        for path in (PROJECT / "src/reboot/java").rglob("*.java")
    )
    for forbidden in ("Display.ItemDisplay", "ShipMovementSimulator", "setNoPhysics", "setPos("):
        if forbidden in reboot_text:
            raise SystemExit(f"REBOOT STACK VALIDATION FAILED: custom vehicle implementation leaked into reboot: {forbidden}")

    print("REBOOT STACK VALIDATION OK: Forge 1.20.1 + Valkyrien Skies + Genesis + ZPS/ZPL; legacy custom ship physics excluded")


if __name__ == "__main__":
    main()
