#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")
def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)
def control_line(readme: str, key: str) -> str:
    prefix = f"- `{key}`"
    return next((line for line in readme.splitlines() if line.startswith(prefix)), "")

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

m_line = control_line(readme, "M")
r_line = control_line(readme, "R")
enter_line = control_line(readme, "Enter")
backspace_line = control_line(readme, "Backspace")
require("settlement" in m_line.lower() and "infrastructure" in m_line.lower(),
        "README palette key does not describe the runtime M settlement/infrastructure action")
require("rotate" in r_line.lower() and "building" in r_line.lower(), "README rotation key missing")
require("confirm" in enter_line.lower() and all(word in enter_line.lower() for word in ("building", "road", "outpost", "civil")),
        "README confirmation key does not cover current placement modes")
require("reset" in backspace_line.lower() and "road" in backspace_line.lower() and "civil" in backspace_line.lower(),
        "README reset key does not cover road/civil first-point reset")
require(not control_line(readme, "B"), "stale B palette guidance returned")
print(f"Frontier Settlement current docs audit: PASS ({version})")
