#!/usr/bin/env python3
"""Inventory external runtime mod assets for Campfire client visual review.

Read-only: the script inspects JAR metadata/resources and writes a report. It does
not extract or repackage third-party assets.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import tomllib
import zipfile
from collections import Counter

TARGET_HINTS = {
    "furniture": ("chair", "table", "sofa", "couch", "bed", "cabinet", "shelf", "lamp", "desk", "bench", "stool"),
    "boat": ("boat", "ship", "sloop", "schooner", "raft", "canoe", "helm", "sail"),
    "crop_food": ("seed", "crop", "fruit", "berry", "apple", "food", "pie", "jam", "juice", "bread", "rice", "tomato", "corn"),
    "kitchen": ("kitchen", "fridge", "oven", "sink", "counter", "cabinet", "cooking", "spice", "rack"),
    "mushroom": ("mushroom", "shroom", "fung", "mycel"),
    "storage": ("backpack", "bag", "storage", "pouch", "toolbelt", "tool_belt"),
}

TARGET_ARTIFACT_MARKERS = (
    "skniro", "peterwolf", "croptopia", "epherolib", "vJnhuDde", "MBAkmtvl",
    "shroom", "puzzles", "sophisticated", "traveler", "player-animation",
    "geckolib", "pezpt98N",
)


def parse_mod_ids(zf: zipfile.ZipFile) -> list[str]:
    candidates = ("META-INF/neoforge.mods.toml", "META-INF/mods.toml")
    for name in candidates:
        if name not in zf.namelist():
            continue
        try:
            doc = tomllib.loads(zf.read(name).decode("utf-8", errors="replace"))
        except Exception:
            continue
        values = []
        for mod in doc.get("mods", []):
            if isinstance(mod, dict):
                mod_id = mod.get("modId")
                if isinstance(mod_id, str) and mod_id:
                    values.append(mod_id)
        if values:
            return values
    return []


def read_lang(zf: zipfile.ZipFile, namespace: str) -> dict[str, str]:
    for name in (
        f"assets/{namespace}/lang/en_us.json",
        f"assets/{namespace}/lang/en_US.json",
    ):
        if name in zf.namelist():
            try:
                data = json.loads(zf.read(name).decode("utf-8", errors="replace"))
                return {str(k): str(v) for k, v in data.items()}
            except Exception:
                return {}
    return {}


def display_name(lang: dict[str, str], namespace: str, kind: str, path: str) -> str | None:
    keys = []
    if kind == "item":
        keys.extend((f"item.{namespace}.{path}", f"block.{namespace}.{path}"))
    else:
        keys.extend((f"block.{namespace}.{path}", f"item.{namespace}.{path}"))
    for key in keys:
        if key in lang:
            return lang[key]
    return None


def classify(identifier: str, label: str | None) -> list[str]:
    haystack = (identifier + " " + (label or "")).lower()
    return [category for category, hints in TARGET_HINTS.items() if any(hint in haystack for hint in hints)]


def collect_ids(names: list[str], namespace: str, kind: str) -> set[str]:
    found = set()
    if kind == 'item':
        patterns = [
            re.compile(rf"^assets/{re.escape(namespace)}/items/(.+)\.json$"),
            re.compile(rf"^assets/{re.escape(namespace)}/models/item/(.+)\.json$"),
        ]
    else:
        patterns = [
            re.compile(rf"^assets/{re.escape(namespace)}/blockstates/(.+)\.json$"),
            re.compile(rf"^assets/{re.escape(namespace)}/models/block/(.+)\.json$"),
        ]
    for name in names:
        for pattern in patterns:
            match = pattern.match(name)
            if match:
                found.add(match.group(1))
                break
    return found


def inspect_jar(path: pathlib.Path) -> dict | None:
    if not zipfile.is_zipfile(path):
        return None
    with zipfile.ZipFile(path) as zf:
        names = zf.namelist()
        mod_ids = parse_mod_ids(zf)
        namespaces = sorted({
            parts[1]
            for name in names
            if name.startswith("assets/") and len((parts := name.split("/"))) >= 3
        })
        if not mod_ids and not any(marker.lower() in path.name.lower() for marker in TARGET_ARTIFACT_MARKERS):
            return None

        namespace_reports = []
        candidates = []
        for namespace in namespaces:
            if namespace in {"minecraft", "neoforge"}:
                continue
            lang = read_lang(zf, namespace)
            items = collect_ids(names, namespace, "item")
            blocks = collect_ids(names, namespace, "block")
            for kind, paths in (("item", items), ("block", blocks)):
                for asset_path in sorted(paths):
                    label = display_name(lang, namespace, kind, asset_path)
                    categories = classify(asset_path, label)
                    if categories:
                        candidates.append({
                            "id": f"{namespace}:{asset_path}",
                            "kind": kind,
                            "name": label,
                            "categories": categories,
                        })
            namespace_reports.append({
                "namespace": namespace,
                "items": len(items),
                "blocks": len(blocks),
                "lang_entries": len(lang),
            })

        counts = Counter()
        for candidate in candidates:
            counts.update(candidate["categories"])

        return {
            "jar": path.name,
            "mod_ids": mod_ids,
            "namespaces": namespace_reports,
            "candidate_counts": dict(sorted(counts.items())),
            "candidates": candidates[:400],
        }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=pathlib.Path)
    parser.add_argument("--json-output", type=pathlib.Path, required=True)
    parser.add_argument("--markdown-output", type=pathlib.Path, required=True)
    args = parser.parse_args()

    jars = sorted(args.directory.glob("*.jar"))
    reports = []
    for jar in jars:
        report = inspect_jar(jar)
        if report is not None:
            reports.append(report)

    payload = {
        "jar_count_scanned": len(jars),
        "relevant_mod_count": len(reports),
        "mods": reports,
    }
    args.json_output.parent.mkdir(parents=True, exist_ok=True)
    args.json_output.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\\n", encoding="utf-8")

    lines = [
        "# Campfire external asset review inventory",
        "",
        f"Scanned runtime JARs: {len(jars)}",
        f"Relevant external mods/libraries: {len(reports)}",
        "",
    ]
    for report in reports:
        lines.append(f"## {report['jar']}")
        lines.append(f"- mod ids: {', '.join(report['mod_ids']) if report['mod_ids'] else '(metadata not parsed)'}")
        for ns in report["namespaces"]:
            lines.append(
                f"- namespace {ns['namespace']}: {ns['items']} item definitions, "
                f"{ns['blocks']} block definitions, {ns['lang_entries']} en_us entries"
            )
        if report["candidate_counts"]:
            lines.append("- review categories: " + ", ".join(
                f"{key}={value}" for key, value in report["candidate_counts"].items()
            ))
        for category in TARGET_HINTS:
            selected = [c for c in report["candidates"] if category in c["categories"]][:24]
            if not selected:
                continue
            lines.append(f"### {category}")
            for item in selected:
                label = f" — {item['name']}" if item["name"] else ""
                lines.append(f"- {item['id']} ({item['kind']}){label}")
        lines.append("")

    args.markdown_output.write_text("\\n".join(lines) + "\\n", encoding="utf-8")
    print(f"asset review inventory: {len(reports)} relevant mods from {len(jars)} runtime jars")
    for report in reports:
        print(
            report["jar"],
            "mod_ids=", report["mod_ids"],
            "namespaces=", [(x["namespace"], x["items"], x["blocks"]) for x in report["namespaces"]],
            "categories=", report["candidate_counts"],
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
