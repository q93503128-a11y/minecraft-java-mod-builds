#!/usr/bin/env python3
import hashlib
import sys
import zipfile
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(f"REBOOT JAR VERIFY FAILED: {message}")


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    if len(sys.argv) > 1:
        jar = Path(sys.argv[1])
    else:
        candidates = sorted(
            path for path in (project / "build/libs").glob("earth_to_stars-*.jar")
            if not path.name.endswith("-sources.jar")
        )
        if len(candidates) != 1:
            fail(f"expected exactly one production jar, found {len(candidates)}")
        jar = candidates[0]
    if not jar.is_file():
        fail("production jar missing")

    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        for required in (
            "META-INF/mods.toml",
            "kr/moonseungjun/earthtostars/EarthToStars.class",
            "kr/moonseungjun/earthtostars/EarthToStarsContent.class",
            "kr/moonseungjun/earthtostars/StarterCraftDeploymentItem.class",
            "kr/moonseungjun/earthtostars/StarterCraftDeploymentService.class",
            "kr/moonseungjun/earthtostars/StarterCraftControlManager.class",
            "kr/moonseungjun/earthtostars/StarterControlNodeBlock.class",
            "assets/earth_to_stars/lang/en_us.json",
            "assets/earth_to_stars/lang/ko_kr.json",
            "assets/earth_to_stars/models/item/starter_craft_deployer.json",
            "assets/earth_to_stars/models/block/starter_flight_core.json",
            "assets/earth_to_stars/models/block/starter_control_node.json",
            "pack.mcmeta",
        ):
            if required not in names:
                fail(f"missing {required}")
        legacy_prefix = "kr/moonseungjun/earthtostars/ship/"
        leaked = sorted(name for name in names if name.startswith(legacy_prefix) and name.endswith(".class"))
        if leaked:
            fail("legacy 26.2 custom ship classes leaked into reboot jar: " + ", ".join(leaked[:5]))
        if any(name.endswith("ShipExteriorEntity.class") or name.endswith("ShipMovementSimulator.class") for name in names):
            fail("failed custom vehicle implementation leaked into reboot jar")

        mods_toml = archive.read("META-INF/mods.toml").decode("utf-8")
        for mod_id in ("valkyrienskies", "genesis", "vlib", "zps", "zpl"):
            if f'modId="{mod_id}"' not in mods_toml:
                fail(f"required runtime dependency missing from metadata: {mod_id}")

    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    sha_path = jar.with_suffix(jar.suffix + ".sha256")
    sha_path.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
    print(f"REBOOT JAR VERIFY OK: {jar.name}")
    print(f"SHA-256: {digest}")


if __name__ == "__main__":
    main()
