#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK_PATH = ROOT / "COMPANION_LOCK.json"
RUNTIME_SOURCES_PATH = Path(__file__).resolve().with_name("runtime-sources.json")
PINNED_RESOLVED_PATH = Path(__file__).resolve().with_name("resolved-lock.client.json")
USER_AGENT = "FrontierSettlementCompanionResolver/1.1 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def request_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json, application/octet-stream;q=0.9, */*;q=0.1"})
    with urllib.request.urlopen(req, timeout=90) as response:
        return response.read()


def request_json(url: str):
    return json.loads(request_bytes(url).decode("utf-8"))


def digest(data: bytes, algorithm: str) -> str:
    h = hashlib.new(algorithm)
    h.update(data)
    return h.hexdigest()


def verify_hash(data: bytes, expected: str | None, algorithm: str, label: str):
    actual = digest(data, algorithm)
    if expected and actual.lower() != expected.lower():
        raise RuntimeError(f"{label}: {algorithm} mismatch: expected {expected}, got {actual}")
    return actual


def resolve_modrinth(entry: dict) -> dict:
    version_id = entry["version_id"]
    meta = request_json(f"https://api.modrinth.com/v2/version/{version_id}")
    if meta.get("project_id") != entry.get("project_id"):
        raise RuntimeError(f"{entry['id']}: project id mismatch")
    if "26.2" not in meta.get("game_versions", []):
        raise RuntimeError(f"{entry['id']}: locked Modrinth version does not advertise Minecraft 26.2")
    loaders = set(meta.get("loaders", []))
    if entry.get("loader") == "neoforge" and "neoforge" not in loaders:
        raise RuntimeError(f"{entry['id']}: locked Modrinth version does not advertise NeoForge")
    files = meta.get("files", [])
    if not files:
        raise RuntimeError(f"{entry['id']}: Modrinth version has no files")
    selected = next((f for f in files if f.get("primary") and str(f.get("filename", "")).endswith(".jar")), None)
    if selected is None:
        selected = next((f for f in files if str(f.get("filename", "")).endswith(".jar")), files[0])
    return {
        "source": "modrinth",
        "project_id": entry["project_id"],
        "version_id": version_id,
        "version": entry["version"],
        "filename": selected["filename"],
        "url": selected["url"],
        "hashes": selected.get("hashes", {}),
    }


def resolve_curseforge(entry: dict, runtime_sources: dict) -> dict:
    source = runtime_sources.get("sources", {}).get(entry["id"])
    if not source:
        raise RuntimeError(f"{entry['id']}: runtime source missing")
    if int(source.get("file_id", -1)) <= 0:
        raise RuntimeError(f"{entry['id']}: invalid CurseForge file id")
    return {
        "source": "curseforge",
        "project_id": entry.get("curseforge_project_id"),
        "file_id": source["file_id"],
        "version": entry["version"],
        "filename": source["file_name"],
        "url": source["download_url"],
        "hashes": {"sha256": source.get("sha256")},
        "source_page": source.get("source_page"),
    }


def should_install(entry: dict, profile: str) -> bool:
    if not entry.get("required", False):
        return False
    if profile == "server" and entry.get("side") == "client_preferred":
        return False
    return True


def write_binary(path: Path, data: bytes):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def pinned_files() -> dict[str, dict]:
    if not PINNED_RESOLVED_PATH.is_file():
        return {}
    resolved = load_json(PINNED_RESOLVED_PATH)
    if resolved.get("profile") != "client" or not resolved.get("resolution", {}).get("verified"):
        raise RuntimeError("committed resolved-lock.client.json is not a verified client lock")
    return {entry["id"]: entry for entry in resolved.get("files", [])}


def apply_committed_pin(entry: dict, file_meta: dict, expected: dict, pins: dict[str, dict]) -> dict:
    pin = pins.get(entry["id"])
    if not pin:
        return expected
    if pin.get("version") != entry.get("version"):
        return expected
    if pin.get("source") != file_meta.get("source"):
        raise RuntimeError(f"{entry['id']}: committed pin source changed for same version")
    if pin.get("filename") != file_meta.get("filename"):
        raise RuntimeError(f"{entry['id']}: committed pin filename changed for same version")
    merged = dict(expected)
    for algorithm in ("sha1", "sha256", "sha512"):
        value = pin.get(algorithm)
        if not value:
            raise RuntimeError(f"{entry['id']}: committed pin missing {algorithm}")
        if merged.get(algorithm) and merged[algorithm].lower() != value.lower():
            raise RuntimeError(f"{entry['id']}: upstream {algorithm} disagrees with committed pin")
        merged[algorithm] = value
    return merged


def main() -> int:
    parser = argparse.ArgumentParser(description="Build the locked Frontier Settlement companion test pack without redistributing third-party JARs in Git.")
    parser.add_argument("--profile", choices=("client", "server"), default="client")
    parser.add_argument("--output", type=Path, default=Path("frontier-companion-instance"))
    parser.add_argument("--frontier-jar", type=Path, default=None, help="Optional built Frontier JAR to copy into mods/")
    parser.add_argument("--resolve-only", action="store_true", help="Resolve and download to verify hashes, but do not leave third-party JARs in mods/")
    args = parser.parse_args()

    lock = load_json(LOCK_PATH)
    runtime_sources = load_json(RUNTIME_SOURCES_PATH)
    pins = pinned_files()
    if lock.get("status") != "candidate_runtime_lock":
        raise RuntimeError("COMPANION_LOCK.json must remain candidate_runtime_lock until user runtime acceptance")
    target = lock["target"]
    if target.get("minecraft") != "26.2" or target.get("loader") != "neoforge":
        raise RuntimeError("unexpected companion target")

    output = args.output.resolve()
    mods = output / "mods"
    if output.exists():
        shutil.rmtree(output)
    mods.mkdir(parents=True, exist_ok=True)

    resolved = {
        "schema": 1,
        "profile": args.profile,
        "target": target,
        "lock_status": lock["status"],
        "pin_policy": "same locked version must match committed resolved-lock.client.json hashes when present",
        "files": [],
    }

    for entry in lock.get("entries", []):
        if not should_install(entry, args.profile):
            continue
        if entry.get("source") == "modrinth":
            file_meta = resolve_modrinth(entry)
        elif entry.get("source") == "curseforge":
            file_meta = resolve_curseforge(entry, runtime_sources)
        else:
            raise RuntimeError(f"{entry['id']}: unsupported source {entry.get('source')}")

        data = request_bytes(file_meta["url"])
        if not data.startswith(b"PK"):
            raise RuntimeError(f"{entry['id']}: resolved download is not a JAR/ZIP payload ({file_meta['url']})")
        expected = apply_committed_pin(entry, file_meta, file_meta.get("hashes", {}), pins)
        sha512 = verify_hash(data, expected.get("sha512"), "sha512", entry["id"])
        sha1 = verify_hash(data, expected.get("sha1"), "sha1", entry["id"])
        sha256 = verify_hash(data, expected.get("sha256"), "sha256", entry["id"])
        out_file = mods / file_meta["filename"]
        if not args.resolve_only:
            write_binary(out_file, data)
        resolved["files"].append({
            "id": entry["id"],
            "name": entry["name"],
            "side": entry["side"],
            "source": file_meta["source"],
            "version": entry["version"],
            "filename": file_meta["filename"],
            "url": file_meta["url"],
            "sha256": sha256,
            "sha512": sha512,
            "sha1": sha1,
        })
        print(f"OK {entry['id']}: {file_meta['filename']} sha256={sha256}")

    if args.frontier_jar is not None:
        frontier = args.frontier_jar.resolve()
        if not frontier.is_file():
            raise RuntimeError(f"Frontier JAR not found: {frontier}")
        target_name = f"frontier_settlement-{target['frontier_settlement']}.jar"
        if not args.resolve_only:
            shutil.copy2(frontier, mods / target_name)
        resolved["frontier"] = {
            "filename": target_name,
            "sha256": digest(frontier.read_bytes(), "sha256"),
        }

    output.mkdir(parents=True, exist_ok=True)
    (output / "resolved-lock.json").write_text(json.dumps(resolved, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Resolved {len(resolved['files'])} locked companion files for {args.profile} profile")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        raise
