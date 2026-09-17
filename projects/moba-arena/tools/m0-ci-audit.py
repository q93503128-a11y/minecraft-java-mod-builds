#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import sys
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

SYMBOL_RE = re.compile(
    r"(moba|team|shop|store|start|ready|skill|ability|character|select|health|bar|"
    r"minimap|map|npc|player|capab|network|packet|message|gui|screen|menu|command|"
    r"procedure|death|respawn|currency|money|gold|equipment|item)",
    re.IGNORECASE,
)


def parse_args():
    parser = argparse.ArgumentParser(description="MOBA Arena M0 CI donor audit")
    parser.add_argument("--project-root", default=str(Path(__file__).resolve().parents[1]))
    parser.add_argument("--download-dir", required=True)
    parser.add_argument("--report-dir", required=True)
    return parser.parse_args()


def forgecdn_url(file_id: int, filename: str) -> str:
    return (
        f"https://mediafilez.forgecdn.net/files/{file_id // 1000}/"
        f"{file_id % 1000:03d}/{urllib.parse.quote(filename, safe='')}"
    )


def resolve_download_url(dep):
    explicit = dep.get("downloadUrl")
    if explicit:
        return explicit, "explicit"
    return forgecdn_url(int(dep["curseforgeFileId"]), dep["expectedFilename"]), "curseforge-cdn"


def download(url: str, dest: Path):
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 moba-arena-m0-audit/1.1",
            "Accept": "*/*",
        },
    )
    with urllib.request.urlopen(req, timeout=120) as response, dest.open("wb") as out:
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            out.write(chunk)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def decode_entry(zf: zipfile.ZipFile, name: str):
    try:
        return zf.read(name).decode("utf-8", errors="replace")
    except KeyError:
        return None


def parse_mods_toml(text):
    if not text:
        return [], []
    mod_ids = sorted(set(re.findall(r'^\s*modId\s*=\s*"([^"]+)"', text, re.MULTILINE | re.IGNORECASE)))
    versions = sorted(set(re.findall(r'^\s*version\s*=\s*"([^"]+)"', text, re.MULTILINE | re.IGNORECASE)))
    return mod_ids, versions


def main():
    args = parse_args()
    project_root = Path(args.project_root).resolve()
    download_dir = Path(args.download_dir).resolve()
    report_dir = Path(args.report_dir).resolve()
    download_dir.mkdir(parents=True, exist_ok=True)
    report_dir.mkdir(parents=True, exist_ok=True)

    lock_path = project_root / "M0_RUNTIME_LOCK.json"
    lock = json.loads(lock_path.read_text(encoding="utf-8"))
    required = [d for d in lock["runtimeDependencies"] if d.get("requiredFor") == "M0"]

    records = []
    anime_symbols = []
    anime_mods_toml = None
    anime_manifest = None

    for dep in required:
        file_id = int(dep["curseforgeFileId"])
        filename = dep["expectedFilename"]
        url, url_kind = resolve_download_url(dep)
        dest = download_dir / filename
        print(f"::group::download {dep['id']}")
        print(f"source_page={dep['source']}")
        print(f"download_url={url}")
        print(f"download_url_kind={url_kind}")
        download(url, dest)
        print(f"downloaded={dest} bytes={dest.stat().st_size}")
        print("::endgroup::")

        record = {
            "id": dep["id"],
            "version": dep["version"],
            "expectedFilename": filename,
            "curseforgeProjectId": dep.get("curseforgeProjectId"),
            "curseforgeFileId": file_id,
            "downloadUrlKind": url_kind,
            "downloadUrl": url,
            "sizeBytes": dest.stat().st_size,
            "sha256": sha256(dest),
            "zipReadable": False,
            "modsTomlPresent": False,
            "manifestPresent": False,
            "modIds": [],
            "declaredVersions": [],
            "classCount": 0,
        }

        if not zipfile.is_zipfile(dest):
            raise RuntimeError(f"Downloaded file is not a readable JAR/ZIP: {dest}")

        with zipfile.ZipFile(dest) as zf:
            record["zipReadable"] = True
            mods_toml = decode_entry(zf, "META-INF/mods.toml")
            manifest = decode_entry(zf, "META-INF/MANIFEST.MF")
            mod_ids, versions = parse_mods_toml(mods_toml)
            classes = [name for name in zf.namelist() if name.endswith(".class")]
            record["modsTomlPresent"] = mods_toml is not None
            record["manifestPresent"] = manifest is not None
            record["modIds"] = mod_ids
            record["declaredVersions"] = versions
            record["classCount"] = len(classes)

            if dep["id"] == "anime_assembly":
                anime_mods_toml = mods_toml
                anime_manifest = manifest
                anime_symbols = sorted({name for name in classes if SYMBOL_RE.search(name)})

        records.append(record)
        print(
            f"AUDIT {dep['id']}: sha256={record['sha256']} bytes={record['sizeBytes']} "
            f"modIds={','.join(record['modIds']) or '-'} classes={record['classCount']}"
        )

    report = {
        "schemaVersion": 1,
        "project": "moba-arena",
        "platformExpected": lock["platform"],
        "dependencies": records,
        "animeAssemblySymbolCandidateCount": len(anime_symbols),
        "downloadAndJarAuditPassed": True,
        "runtimeBootTested": False,
        "note": "CI donor download/JAR audit only. Client gameplay and map acceptance remain untested.",
    }

    (report_dir / "M0_CI_FINGERPRINTS.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    (report_dir / "ANIME_ASSEMBLY_SYMBOL_CANDIDATES.txt").write_text(
        "# Raw class-name candidates; not a verified public API map.\n"
        + "\n".join(anime_symbols)
        + "\n",
        encoding="utf-8",
    )
    (report_dir / "ANIME_ASSEMBLY_MODS_TOML.txt").write_text(
        anime_mods_toml or "# missing\n", encoding="utf-8"
    )
    (report_dir / "ANIME_ASSEMBLY_MANIFEST.txt").write_text(
        anime_manifest or "# missing\n", encoding="utf-8"
    )

    print("\n=== M0 CI FINGERPRINT REPORT ===")
    print(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"anime_symbol_candidates={len(anime_symbols)}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"M0 CI audit failed: {exc}", file=sys.stderr)
        sys.exit(2)
