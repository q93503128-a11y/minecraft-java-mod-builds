#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")
def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)

props = read("gradle.properties")
readme = read("README.md")
client = read("src/main/java/kr/moonseungjun/frontiersettlement/client/BuildingPlacementClient.java")
match = re.search(r"^mod_version=(.+)$", props, re.MULTILINE)
require(match is not None, "Frontier mod_version missing")
version = match.group(1).strip()
require(version.startswith("0.1.0-alpha."), "unexpected Frontier version family")
require(f"## Current version: {version}" in readme, "README current version does not match gradle.properties")
require("## Current version: 0.1.0-alpha.79" not in readme, "stale Alpha.79 current-version label returned")
require("minecraft_version=26.2" in props, "Minecraft version drift")
require("neo_version=26.2.0.38-beta" in props, "NeoForge version drift")
for doc in ("ORIGINAL_DESIGN_v0.2.md", "CANONICAL_PLAN.md", "COMPLETION_GAP_AUDIT.md"):
    require((ROOT / doc).is_file(), f"canonical direction document missing: {doc}")
require("GLFW.GLFW_KEY_M" in client, "actual settlement palette key is no longer M")
require("GLFW.GLFW_KEY_R" in client, "actual rotation key is no longer R")
require("GLFW.GLFW_KEY_ENTER" in client, "actual confirmation key is no longer Enter")
require("GLFW.GLFW_KEY_BACKSPACE" in client, "actual reset key is no longer Backspace")
require("- `M` — settlement/infrastructure palette;" in readme, "README palette key does not match runtime M")
require("- `R` — rotate an ordinary building placement;" in readme, "README rotation key missing")
require("- `Enter` — confirm the current building/road/outpost/civil-work selection step;" in readme, "README confirmation key missing")
require("- `B` — settlement/infrastructure palette;" not in readme, "stale B palette guidance returned")
print(f"Frontier Settlement current docs audit: PASS ({version})")
