#!/usr/bin/env python3
"""Static and packaged blank-foundation gates.

This is not a Minecraft client playtest. The checks guard against accidental
reintroduction of authored world content and globally registered legacy hooks.
"""

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTENT = ROOT / "src" / "main" / "resources" / "data" / "turnbound"
JAVA = ROOT / "src" / "main" / "java"
EMPTY_FIELDS = ("characters", "enemies", "quests", "equipment", "encounters", "regions")
RETIRED_CLIENT_CLASSES = (
    "DrehmalAutoInstaller",
    "DrehmalInstallScreen",
    "DrehmalBootstrapManifest",
    "DrehmalInstallFiles",
    "Drehmal26_2CompatMigrator",
    "Drehmal26_2ResourcePackMigrator",
    "DrehmalResourceRepairDiscovery",
    "ClientWorldLoadingBootstrap",
    "WorldLoadingScreen",
)
RUNTIME_CLIENT_PREFIX = "io/github/q93503128/turnbound/client/"
AUTO_ANNOTATION = re.compile(
    r"^\\s*@(?:Mod\\.)?EventBusSubscriber\\b|"
    r"^\\s*@JourneyMapPlugin\\b|"
    r"^\\s*@net\\.neoforged\\.fml\\.common\\.Mod\\.EventBusSubscriber\\b",
    re.MULTILINE,
)


def source_errors() -> list[str]:
    errors = []
    catalog = CONTENT / "rebuild" / "foundation_v1.json"
    authored_files = sorted(p for p in CONTENT.rglob("*") if p.is_file())
    if authored_files != [catalog]:
        errors.append(
            "Unexpected authored production data: "
            + ", ".join(str(p.relative_to(ROOT)) for p in authored_files)
        )
    if not catalog.is_file():
        errors.append("Missing empty foundation catalog")
    else:
        try:
            doc = json.loads(catalog.read_text(encoding="utf-8"))
            for key in EMPTY_FIELDS:
                if doc.get(key) != []:
                    errors.append(f"Foundation content is not empty: {key}")
        except (ValueError, OSError) as error:
            errors.append(f"Unreadable foundation content manifest: {error}")

    for java in JAVA.rglob("*.java"):
        source = java.read_text(encoding="utf-8")
        for match in AUTO_ANNOTATION.finditer(source):
            errors.append(
                f"Unexpected globally discovered legacy hook: {java.relative_to(ROOT)}"
                + f" line {source.count(chr(10), 0, match.start()) + 1}"
            )
    entry = (JAVA / "io/github/q93503128/turnbound/Turnbound.java").read_text(encoding="utf-8")
    client = (JAVA / "io/github/q93503128/turnbound/TurnboundClient.java").read_text(encoding="utf-8")
    if "RebuildCampaignLifecycle::onPlayerLogin" not in entry:
        errors.append("Rebuild login lifecycle is not registered")
    for forbidden in ("ExternalWorldBootstrap::", "WorldSessionRouter::", "DrehmalAutoInstaller::",
                      "TurnboundCommands::", "FieldSessionManager::"):
        if forbidden in entry or forbidden in client:
            errors.append(f"Old runtime hook is registered from a live entrypoint: {forbidden}")
    return errors


def jar_errors(path: Path) -> list[str]:
    if not path.is_file():
        return [f"Missing JAR: {path}"]
    with zipfile.ZipFile(path) as jar:
        files = set(jar.namelist())
    errors = []
    if "data/turnbound/rebuild/foundation_v1.json" not in files:
        errors.append("Blank content manifest missing from JAR")
    for name in RETIRED_CLIENT_CLASSES:
        stem = RUNTIME_CLIENT_PREFIX + name
        for member in files:
            if member == stem + ".class" or member.startswith(stem + "$"):
                errors.append(f"Retired downloader/world-bootstrap bytecode shipped: {member}")
    if any(name.startswith("assets/turnbound/") for name in files):
        errors.append("Retired authored assets shipped in runtime JAR")
    if any(
        name.startswith("data/turnbound/") and name != "data/turnbound/rebuild/foundation_v1.json"
        for name in files
    ):
        errors.append("Retired authored data shipped in runtime JAR")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--sources", action="store_true")
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    if not args.sources and args.jar is None:
        parser.error("Use --sources or --jar <file>")
    issues = []
    if args.sources:
        issues.extend(source_errors())
    if args.jar:
        issues.extend(jar_errors(args.jar))
    for issue in issues:
        print("REBUILD BOUNDARY FAIL:", issue, file=sys.stderr)
    if not issues:
        print("REBUILD BOUNDARY PASS: requested blank-content safety checks")
    return 1 if issues else 0


if __name__ == "__main__":
    raise SystemExit(main())
