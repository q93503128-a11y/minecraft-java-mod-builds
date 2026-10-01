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
        if "100k-2048-high" in lowered:
            score += 60
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
    selected_stats = None
    glb_path = None
    compressed_fallback = None

    for index, candidate in enumerate(candidates):
        try:
            raw, final_url = request_bytes(candidate["url"])
            if not raw.startswith(b"glTF"):
                raise RuntimeError(f"not GLB bytes; prefix={raw[:16]!r}")

            probe_path = target_dir / f"candidate_{index:02d}.glb"
            probe_path.write_bytes(raw)
            stats = parse_glb(probe_path)
            required = set(stats.get("extensions_required", []))
            used = set(stats.get("extensions_used", []))
            is_draco = "KHR_draco_mesh_compression" in required or "KHR_draco_mesh_compression" in used

            attempts.append({
                "url": candidate["url"],
                "final_url": final_url,
                "sha256": stats["sha256"],
                "bytes": stats["bytes"],
                "triangles": stats["triangle_count_from_indices"],
                "extensions_required": stats["extensions_required"],
                "extensions_used": stats["extensions_used"],
                "accepted": not is_draco,
                "rejection": "KHR_draco_mesh_compression" if is_draco else None,
            })

            if is_draco:
                if compressed_fallback is None:
                    compressed_fallback = {
                        "candidate": candidate,
                        "final_url": final_url,
                        "path": probe_path,
                        "stats": stats,
                    }
                continue

            glb_path = target_dir / f"{target['slug']}_smithsonian_source.glb"
            probe_path.replace(glb_path)
            selected = {**candidate, "final_url": final_url}
            selected_stats = stats
            break
        except Exception as exc:
            attempts.append({
                "url": candidate["url"],
                "error": f"{type(exc).__name__}: {exc}",
                "accepted": False,
            })

    compressed_staging = None
    if selected is None and compressed_fallback is not None:
        staging_path = target_dir / f"{target['slug']}_smithsonian_draco_reference.glb"
        compressed_fallback["path"].replace(staging_path)
        compressed_staging = {
            **compressed_fallback["candidate"],
            "final_url": compressed_fallback["final_url"],
            "glb": compressed_fallback["stats"],
        }

    for leftover in target_dir.glob("candidate_*.glb"):
        leftover.unlink()

    report = {
        "target": target,
        "document_url": document_url,
        "document_sha256": hashlib.sha256(document_raw).hexdigest(),
        "document_cc0_mentions": cc0_mentions,
        "document_public_domain_mentions": public_domain_mentions,
        "glb_candidates": candidates[:20],
        "download_attempts": attempts,
        "selected_glb": selected,
        "compressed_reference": compressed_staging,
        "glb": selected_stats,
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
    for attempt in attempts[:12]:
        if "error" in attempt:
            print(f"  download_error {attempt['url']}: {attempt['error']}")
        else:
            print(
                "  inspected "
                + attempt["url"]
                + f" triangles={attempt['triangles']} bytes={attempt['bytes']}"
                + f" accepted={attempt['accepted']}"
                + f" required={attempt['extensions_required']}"
            )

    if selected is None:
        print("selected_uncompressed_glb=NONE")
        if compressed_staging is None:
            raise RuntimeError(f"no downloadable GLB staging source found for {target['slug']}")
        print("compressed_reference=" + compressed_staging["final_url"])
        print("compressed_reference_stats=" + json.dumps(compressed_staging["glb"], sort_keys=True))
        print("staging_status=DRACO_SOURCE_READY_FOR_LOSSLESS_DECOMPRESSION")
        return report

    print(f"selected_uncompressed_glb={selected['final_url']}")
    print("glb_stats=" + json.dumps(report["glb"], sort_keys=True))
    return report


def verify_decompressed(output: pathlib.Path) -> int:
    files = sorted(output.glob("*/*_source_uncompressed.glb"))
    if len(files) != len(TARGETS):
        raise RuntimeError(f"expected {len(TARGETS)} decompressed GLBs, found {len(files)}")

    reports = []
    for path in files:
        stats = parse_glb(path)
        extensions = set(stats["extensions_required"]) | set(stats["extensions_used"])
        if "KHR_draco_mesh_compression" in extensions:
            raise RuntimeError(f"Draco still present after copy: {path}")
        reports.append({"path": str(path), "glb": stats})
        print(f"verified_uncompressed={path}")
        print("  stats=" + json.dumps(stats, sort_keys=True))

    (output / "decompressed_report.json").write_text(
        json.dumps(reports, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print("Smithsonian lossless Draco removal verification: PASS")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=pathlib.Path)
    parser.add_argument("--verify-decompressed", action="store_true")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    if args.verify_decompressed:
        return verify_decompressed(args.output)

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

    if not all(report.get("glb") or report.get("compressed_reference") for report in reports):
        return 2
    print("Smithsonian museum asset staging probe: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
