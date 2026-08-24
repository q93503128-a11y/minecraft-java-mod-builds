#!/usr/bin/env python3
import hashlib, json, os, re, subprocess, sys, urllib.request, zipfile
from pathlib import Path

VERSION_ID = "xls8dTZv"
API = f"https://api.modrinth.com/v2/version/{VERSION_ID}"
OUT = Path(os.environ.get("RUNNER_TEMP", "/tmp")) / "sa047-tbs-audit"
OUT.mkdir(parents=True, exist_ok=True)

def fetch_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "SurvivalAscension-compat-audit/0.47"})
    with urllib.request.urlopen(req, timeout=45) as r:
        return json.load(r)

def download(url, path):
    req = urllib.request.Request(url, headers={"User-Agent": "SurvivalAscension-compat-audit/0.47"})
    with urllib.request.urlopen(req, timeout=90) as r, path.open("wb") as f:
        while True:
            b = r.read(1024 * 1024)
            if not b: break
            f.write(b)

def digest(path, algo):
    h = hashlib.new(algo)
    with path.open("rb") as f:
        for b in iter(lambda: f.read(1024 * 1024), b""):
            h.update(b)
    return h.hexdigest()

def javap(jar, cls):
    p = subprocess.run(["javap", "-classpath", str(jar), "-c", "-p", "-verbose", cls], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return p.stdout

meta = fetch_json(API)
if meta.get("version_number") != "0.7.0":
    raise SystemExit(f"unexpected version_number={meta.get('version_number')}")
if "26.2" not in (meta.get("game_versions") or []):
    raise SystemExit("26.2 missing from game_versions")
if "neoforge" not in (meta.get("loaders") or []):
    raise SystemExit("neoforge missing from loaders")

files = meta.get("files") or []
candidates = [f for f in files if f.get("filename", "").endswith(".jar") and "neoforge" in f.get("filename", "").lower() and "26.2" in f.get("filename", "")]
if not candidates:
    candidates = [f for f in files if f.get("filename", "").endswith(".jar") and "neoforge" in f.get("filename", "").lower()]
if not candidates:
    raise SystemExit("no NeoForge jar in Modrinth metadata")
chosen = next((f for f in candidates if f.get("primary")), candidates[0])
jar = OUT / chosen["filename"]
download(chosen["url"], jar)
sha1 = digest(jar, "sha1")
sha512 = digest(jar, "sha512")
if chosen.get("hashes", {}).get("sha1") and sha1 != chosen["hashes"]["sha1"]:
    raise SystemExit("sha1 mismatch")
if chosen.get("hashes", {}).get("sha512") and sha512 != chosen["hashes"]["sha512"]:
    raise SystemExit("sha512 mismatch")

with zipfile.ZipFile(jar) as z:
    names = z.namelist()
    lang_name = next((n for n in names if n.endswith("/lang/en_us.json") and "tbos" in n.lower()), None)
    entity_keys = []
    if lang_name:
        lang = json.loads(z.read(lang_name).decode("utf-8"))
        for k, v in sorted(lang.items()):
            if k.startswith("entity."):
                entity_keys.append((k, v))
    mod_meta_names = [n for n in names if n in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml") or n.endswith("neoforge.mods.toml")]
    mod_meta = ""
    for n in mod_meta_names:
        mod_meta += f"\n--- {n} ---\n" + z.read(n).decode("utf-8", errors="replace")

registry_cls = "com.nightbeam.tbos.registry.ModEntities"
registry_javap = javap(jar, registry_cls)
(OUT / "ModEntities.javap.txt").write_text(registry_javap, encoding="utf-8")

class_names = [n[:-6].replace("/", ".") for n in names if n.endswith(".class")]
focus_classes = [c for c in class_names if re.search(r"(Curator|Cantor|Minotaur|Phoenix|Guardian|Boss)", c, re.I)]
class_info = []
for cls in focus_classes:
    text = javap(jar, cls)
    if not text.strip(): continue
    (OUT / (cls.replace(".", "_") + ".javap.txt")).write_text(text, encoding="utf-8")
    header = next((line.strip() for line in text.splitlines() if re.search(r"\b(class|interface)\b", line) and "Compiled from" not in line), "")
    bossbar = "ServerBossEvent" in text or "BossEvent" in text
    health_literals = sorted(set(re.findall(r"(?:Double|Float)\s+([0-9]+(?:\.[0-9]+)?)", text)))[:20]
    class_info.append({"class": cls, "header": header, "boss_event_ref": bossbar, "numeric_literals": health_literals})

# Preserve only interoperability evidence, not binary/code payloads, in the report.
report = {
    "version_id": VERSION_ID,
    "version_number": meta.get("version_number"),
    "name": meta.get("name"),
    "game_versions": meta.get("game_versions"),
    "loaders": meta.get("loaders"),
    "dependencies": meta.get("dependencies"),
    "file": {
        "filename": chosen.get("filename"),
        "url": chosen.get("url"),
        "size": chosen.get("size"),
        "primary": chosen.get("primary"),
        "sha1": sha1,
        "sha512": sha512,
    },
    "entity_translation_keys": entity_keys,
    "focused_classes": class_info,
    "registry_string_candidates": sorted(set(re.findall(r"[A-Za-z0-9_./:-]{3,}", registry_javap)))[:2000],
    "mod_metadata_excerpt": mod_meta[:12000],
}
(OUT / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

print("SA047_TBS_BINARY_AUDIT")
print(f"version={report['version_number']} id={VERSION_ID}")
print(f"file={chosen.get('filename')} size={chosen.get('size')}")
print(f"sha1={sha1}")
print(f"sha512={sha512}")
print("dependencies=" + json.dumps(meta.get("dependencies"), ensure_ascii=False))
print("entity_keys=")
for k, v in entity_keys:
    print(f"  {k} = {v}")
print("focused_classes=")
for x in class_info:
    print(f"  {x['class']} | boss_event={x['boss_event_ref']} | {x['header']}")
print("registry_focus=")
for line in registry_javap.splitlines():
    if re.search(r"curator|cantor|minotaur|phoenix|guardian|register|entity", line, re.I):
        print("  " + line.strip())
