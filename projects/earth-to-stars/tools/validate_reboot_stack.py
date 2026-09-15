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
    deployment = PROJECT / "src/reboot/java/kr/moonseungjun/earthtostars/StarterCraftDeploymentService.java"
    control = PROJECT / "src/reboot/java/kr/moonseungjun/earthtostars/StarterCraftControlManager.java"
    content = PROJECT / "src/reboot/java/kr/moonseungjun/earthtostars/EarthToStarsContent.java"
    toml = PROJECT / "src/reboot/resources/META-INF/mods.toml"

    for needle in (
        "src/reboot/java",
        "src/reboot/resources",
        "net.neoforged.moddev.legacyforge",
        'org.valkyrienskies.core:api:${vs_core_version}',
        'org.valkyrienskies.core:util:${vs_core_version}',
        'org.valkyrienskies:valkyrienskies-120-forge:${valkyrien_skies_version}',
        "org.jetbrains.kotlin:kotlin-stdlib:2.0.0",
        "maven.modrinth:vs-genesis:1.20.1-0.7.3",
        "maven.modrinth:zps:1.20.1-2.5.1",
        "maven.modrinth:zpl:1.20.1-1.5.0",
    ):
        require(build, needle)

    for needle in (
        "minecraft_version=1.20.1",
        "forge_version=1.20.1-47.4.0",
        "valkyrien_skies_version=2.4.10",
        "vs_core_version=1.1.0+1d4a7373e9",
        "zps_version=1.20.1-2.5.1",
        "mod_version=0.2.0-alpha.2",
    ):
        require(props, needle)

    for needle in (
        'VERSION = "0.2.0-alpha.2"',
        '"valkyrienskies"',
        '"genesis"',
        '"zps"',
        '"zpl"',
        "EARTH_TO_STARS_REBOOT_STACK_BOOT_PASS",
        "EARTH_TO_STARS_STARTER_CRAFT_ASSEMBLY_PASS",
    ):
        require(mod, needle)

    for needle in (
        "ShipAssembler.assembleToShipFull",
        "OCTO_CONTROLLER",
        "THRUSTER_EXHAUST_BLOCK",
        "GYROSCOPE_BLOCK",
        'requireExternalBlock("zps", "power_cell")',
        "STARTER_FLIGHT_CORE",
    ):
        require(deployment, needle)

    for needle in (
        "OctoMountingEntity",
        "Channels.OCT_A",
        "Channels.OCT_H",
        "STARTER_CONTROL_NODE",
        "ForgeCapabilities.ENERGY",
    ):
        require(control, needle)

    for needle in (
        '"starter_craft_deployer"',
        '"starter_flight_core"',
        '"starter_control_node"',
        "CreativeModeTab.builder()",
    ):
        require(content, needle)

    for mod_id in ("valkyrienskies", "genesis", "vlib", "zps", "zpl"):
        require(toml, f'modId="{mod_id}"')

    for resource in (
        "assets/earth_to_stars/lang/en_us.json",
        "assets/earth_to_stars/lang/ko_kr.json",
        "assets/earth_to_stars/models/item/starter_craft_deployer.json",
        "assets/earth_to_stars/models/block/starter_flight_core.json",
        "assets/earth_to_stars/models/block/starter_control_node.json",
        "assets/earth_to_stars/blockstates/starter_flight_core.json",
        "assets/earth_to_stars/blockstates/starter_control_node.json",
    ):
        if not (PROJECT / "src/reboot/resources" / resource).is_file():
            raise SystemExit(f"REBOOT STACK VALIDATION FAILED: missing resource {resource}")

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

    print("REBOOT STACK VALIDATION OK: released VS 2.4.10 block assembly + ZPS 2.5.1 cockpit/power + ZPL propulsion/gyro; legacy custom ship physics excluded")


if __name__ == "__main__":
    main()
