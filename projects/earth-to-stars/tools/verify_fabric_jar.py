#!/usr/bin/env python3
import hashlib
import json
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]


def main() -> None:
    jars = sorted(
        p for p in (PROJECT / "build/libs").glob("earth_to_stars-*.jar")
        if not p.name.endswith("-sources.jar")
    )
    if len(jars) != 1:
        raise SystemExit(f"FABRIC JAR VERIFY FAILED: expected one production jar, found {[p.name for p in jars]}")

    jar = jars[0]
    with zipfile.ZipFile(jar) as zf:
        names = set(zf.namelist())
        required = {
            "fabric.mod.json",
            "kr/moonseungjun/earthtostars/fabric/EarthToStarsFabric.class",
            "kr/moonseungjun/earthtostars/ship/domain/ShipState.class",
            "kr/moonseungjun/earthtostars/ship/systems/ShipSystemsRuntime.class",
        }
        missing = sorted(required - names)
        if missing:
            raise SystemExit(f"FABRIC JAR VERIFY FAILED: missing {missing}")

        metadata = json.loads(zf.read("fabric.mod.json").decode("utf-8"))
        if metadata.get("id") != "earth_to_stars":
            raise SystemExit("FABRIC JAR VERIFY FAILED: wrong mod id")
        depends = metadata.get("depends", {})
        if depends.get("minecraft") != "~26.2" or depends.get("fabricloader") != ">=0.19.5":
            raise SystemExit("FABRIC JAR VERIFY FAILED: loader/game dependency contract mismatch")

        forbidden_markers = (
            "META-INF/mods.toml",
            "META-INF/neoforge.mods.toml",
            "kr/moonseungjun/earthtostars/StarterCraftDeploymentService.class",
        )
        leaked = [name for name in forbidden_markers if name in names]
        if leaked:
            raise SystemExit(f"FABRIC JAR VERIFY FAILED: retired Forge reboot content leaked into jar: {leaked}")

    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    sha_file = jar.with_suffix(jar.suffix + ".sha256")
    sha_file.write_text(digest + "\n", encoding="utf-8")
    print(f"FABRIC JAR VERIFY OK: {jar.name}")
    print(f"SHA-256: {digest}")


if __name__ == "__main__":
    main()
