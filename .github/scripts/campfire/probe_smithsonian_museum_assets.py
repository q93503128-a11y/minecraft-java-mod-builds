#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import struct
import sys
import urllib.error
import urllib.parse
import urllib.request

TARGETS = [
    {
        "slug": "triceratops_horridus",
        "uuid": "d8c623be-4ebc-11ea-b77f-2e728ce88125",
        "record_id": "nmnhpaleobiology_3572783",
        "catalog": "USNM PAL500000",
        "license": "CC0",
        "object_url": "https://3d.si.edu/object/3d/triceratops-horridus-marsh-1889:d8c623be-4ebc-11ea-b77f-2e728ce88125",
    },
    {
        "slug": "mammuthus_primigenius",
        "uuid": "341c96cd-f967-4540-8ed1-d3fc56d31f12",
        "record_id": "nmnhpaleobiology_3447777",
        "catalog": "USNM V23792",
        "license": "CC0",
        "object_url": "https://3d.si.edu/object/3d/mammuthus-primigenius-blumbach:341c96cd-f967-4540-8ed1-d3fc56d31f12",
    },
]

USER_AGENT = "CampfireSessionsMuseumAssetProbe/1.0 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"
MAX_DOWNLOAD_BYTES = 250 * 1024 * 1024


def request_bytes(url: str) -> tuple[bytes, str]:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "*/*"})
    with urllib.request.urlopen(req, timeout=60) as response:
        data = response.read(MAX_DOWNLOAD_BYTES + 1)
        if len(data) > MAX_DOWNLOAD_BYTES:
            raise RuntimeError(f"download exceeds {MAX_DOWNLOAD_BYTES} bytes: {url}")
        return data, response.geturl()


def fetch_document(target: dict) -> tuple[dict, str, bytes]:
    candidates = [
        f"https://3d-api.si.edu/content/document/{target['uuid']}/document.json",
        f"https://3d-api.si.edu/content/document/3d_package:{target['uuid']}/document.json",
    ]
    errors = []
    for url in candidates:
        try:
            raw, final_url = request_bytes(url)
            return json.loads(raw.decode("utf-8")), final_url, raw
        except Exception as exc:
            errors.append(f"{url}: {type(exc).__name__}: {exc}")
    raise RuntimeError("unable to fetch Smithsonian document:\n" + "\n".join(errors))


def walk_strings(value, path="$", parent=None):
    if isinstance(value, dict):
        for key, child in value.items():
            yield from walk_strings(child, f"{path}.{key}", value)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from walk_strings(child, f"{path}[{index}]", value)
    elif isinstance(value, str):
        yield path, value, parent


def candidate_urls(document: dict, document_url: str) -> list[dict]:
    candidates = []
    seen = set()
    for path, value, parent in walk_strings(document):
        lowered = value.lower()
        if ".glb" not in lowered:
            continue
        context = json.dumps(parent, ensure_ascii=False, sort_keys=True) if isinstance(parent, dict) else value
        context_lower = context.lower()
        if "draco" in context_lower or "draco" in lowered:
            continue

        if value.startswith(("http://", "https://")):
            url = value
        elif value.startswith("//"):
            url = "https:" + value
        else:
            url = urllib.parse.urljoin(document_url, value)

        url = urllib.parse.quote(url, safe=":/?&=%+@,;")
        if url in seen:
            continue
        seen.add(url)

        score = 0
        if "low" in context_lower:
            score += 20
        if "low resolution" in context_lower:
            score += 20
        if "scale in m" in context_lower or "meter" in context_lower:
            score += 8
        if lowered.endswith(".glb"):
            score += 5
        if "full" in context_lower:
            score -= 10

        candidates.append({
            "path": path,
            "raw": value,
            "url": url,
            "score": score,
            "context": context[:1200],
        })
    return sorted(candidates, key=lambda item: (-item["score"], item["url"]))


def parse_glb(path: pathlib.Path) -> dict:
    data = path.read_bytes()
    if len(data) < 20:
        raise RuntimeError("GLB too short")
    magic, version, declared_length = struct.unpack_from("<4sII", data, 0)
    if magic != b"glTF":
        raise RuntimeError(f"not a GLB: magic={magic!r}")
    if declared_length != len(data):
        raise RuntimeError(f"GLB length mismatch declared={declared_length} actual={len(data)}")

    offset = 12
    json_doc = None
    while offset + 8 <= len(data):
        chunk_length, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        chunk = data[offset:offset + chunk_length]
        offset += chunk_length
        if chunk_type == 0x4E4F534A:
            json_doc = json.loads(chunk.rstrip(b" \t\r\n\x00").decode("utf-8"))
            break
    if json_doc is None:
        raise RuntimeError("GLB JSON chunk not found")

    accessors = json_doc.get("accessors", [])
    meshes = json_doc.get("meshes", [])
    primitive_count = 0
    triangle_count = 0
    position_bounds = []

    for mesh in meshes:
        for primitive in mesh.get("primitives", []):
            primitive_count += 1
            mode = primitive.get("mode", 4)
            if mode == 4 and isinstance(primitive.get("indices"), int):
                accessor_index = primitive["indices"]
                if 0 <= accessor_index < len(accessors):
                    triangle_count += int(accessors[accessor_index].get("count", 0)) // 3

            position_accessor = primitive.get("attributes", {}).get("POSITION")
            if isinstance(position_accessor, int) and 0 <= position_accessor < len(accessors):
                accessor = accessors[position_accessor]
                if "min" in accessor and "max" in accessor:
                    position_bounds.append({"min": accessor["min"], "max": accessor["max"]})

    aggregate_bounds = None
    if position_bounds:
        mins = [min(bounds["min"][axis] for bounds in position_bounds) for axis in range(3)]
        maxs = [max(bounds["max"][axis] for bounds in position_bounds) for axis in range(3)]
        aggregate_bounds = {
            "min": mins,
            "max": maxs,
            "size": [maxs[i] - mins[i] for i in range(3)],
        }

    return {
        "glb_version": version,
        "bytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "scene_count": len(json_doc.get("scenes", [])),
        "node_count": len(json_doc.get("nodes", [])),
        "mesh_count": len(meshes),
        "primitive_count": primitive_count,
        "triangle_count_from_indices": triangle_count,
        "material_count": len(json_doc.get("materials", [])),
        "texture_count": len(json_doc.get("textures", [])),
        "image_count": len(json_doc.get("images", [])),
        "skin_count": len(json_doc.get("skins", [])),
        "animation_count": len(json_doc.get("animations", [])),
        "extensions_used": json_doc.get("extensionsUsed", []),
        "extensions_required": json_doc.get("extensionsRequired", []),
        "bounds_from_accessors": aggregate_bounds,
    }


def acquire_target(target: dict, output: pathlib.Path) -> dict:
    document, document_url, document_raw = fetch_document(target)
    target_dir = output / target["slug"]
    target_dir.mkdir(parents=True, exist_ok=True)
    (target_dir / "smithsonian_document.json").write_bytes(document_raw)

    strings = [text.lower() for _, text, _ in walk_strings(document)]
    cc0_mentions = sum("cc0" in text for text in strings)
    public_domain_mentions = sum("public domain" in text for text in strings)

    candidates = candidate_urls(document, document_url)
    attempts = []
    selected = None
    glb_path = None
    for candidate in candidates:
        try:
            raw, final_url = request_bytes(candidate["url"])
            if not raw.startswith(b"glTF"):
                raise RuntimeError(f"not GLB bytes; prefix={raw[:16]!r}")
            glb_path = target_dir / f"{target['slug']}_smithsonian_low.glb"
            glb_path.write_bytes(raw)
            selected = {**candidate, "final_url": final_url}
            break
        except Exception as exc:
            attempts.append({
                "url": candidate["url"],
                "error": f"{type(exc).__name__}: {exc}",
            })

    report = {
        "target": target,
        "document_url": document_url,
        "document_sha256": hashlib.sha256(document_raw).hexdigest(),
        "document_cc0_mentions": cc0_mentions,
        "document_public_domain_mentions": public_domain_mentions,
        "glb_candidates": candidates[:20],
        "download_attempts": attempts,
        "selected_glb": selected,
        "glb": parse_glb(glb_path) if glb_path else None,
    }

    (target_dir / "report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"## {target['slug']}")
    print(f"catalog={target['catalog']} record={target['record_id']} expected_license={target['license']}")
    print(f"document={document_url}")
    print(f"document_sha256={report['document_sha256']}")
    print(f"document_cc0_mentions={cc0_mentions} public_domain_mentions={public_domain_mentions}")
    print(f"glb_candidates={len(candidates)}")
    for candidate in candidates[:8]:
        print(f"  candidate score={candidate['score']} {candidate['url']}")
    if selected is None:
        print("selected_glb=NONE")
        for attempt in attempts[:8]:
            print(f"  failed {attempt['url']}: {attempt['error']}")
        raise RuntimeError(f"no downloadable non-Draco GLB found for {target['slug']}")

    print(f"selected_glb={selected['final_url']}")
    print("glb_stats=" + json.dumps(report["glb"], sort_keys=True))
    return report


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=pathlib.Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    reports = []
    for target in TARGETS:
        reports.append(acquire_target(target, args.output))

    summary = {
        "source": "Smithsonian 3D",
        "policy": "Only curated object records independently verified as CC0/public-domain are listed here.",
        "reports": reports,
    }
    (args.output / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    if not all(report.get("glb") for report in reports):
        return 2
    print("Smithsonian museum asset probe: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
