#!/usr/bin/env python3
import json
import re
import urllib.request
import zipfile
from collections import defaultdict
from pathlib import Path

import torch
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

ROOT = Path(__file__).resolve().parents[3]
TMP = ROOT / ".tmp-full-ko-v2"
TMP.mkdir(exist_ok=True)

frontier = json.loads(
    (ROOT / "projects/frontier-settlement/companion-testpack/resolved-lock.client.json").read_text(encoding="utf-8")
)
sources = [{"name": f["filename"], "url": f["url"]} for f in frontier["files"]]
survival = json.loads(
    (ROOT / "projects/survival-ascension/modpack/content-lock.json").read_text(encoding="utf-8")
)
for mod in survival["mods"]:
    req = urllib.request.Request(
        f"https://api.modrinth.com/v2/version/{mod['version_id']}",
        headers={"User-Agent": "Frontier-Full-Korean-V2/1.0"},
    )
    with urllib.request.urlopen(req, timeout=60) as response:
        meta = json.load(response)
    primary = next((f for f in meta["files"] if f.get("primary")), meta["files"][0])
    sources.append({"name": primary["filename"], "url": primary["url"]})
sources = list({s["name"]: s for s in sources}.values())

langs = defaultdict(lambda: {"en": {}, "ko": {}})
for index, source in enumerate(sources):
    jar = TMP / f"{index}.jar"
    req = urllib.request.Request(source["url"], headers={"User-Agent": "Frontier-Full-Korean-V2/1.0"})
    with urllib.request.urlopen(req, timeout=90) as response:
        jar.write_bytes(response.read())
    with zipfile.ZipFile(jar) as zf:
        for name in zf.namelist():
            match = re.fullmatch(r"assets/([^/]+)/lang/(en_us|ko_kr)\.json", name)
            if not match:
                continue
            namespace, locale = match.groups()
            try:
                data = json.loads(zf.read(name).decode("utf-8"))
            except Exception:
                # Dungeons & Taverns 5.3.0 has malformed ko_kr JSON. A valid full overlay is generated below.
                continue
            langs[namespace]["en"].update(data)
            if locale == "ko_kr":
                langs[namespace]["ko"].update(data)

for project in ("frontier-settlement", "survival-ascension"):
    assets = ROOT / f"projects/{project}/src/main/resources/assets"
    if not assets.exists():
        continue
    for path in assets.glob("*/lang/ko_kr.json"):
        try:
            langs[path.parent.parent.name]["ko"].update(json.loads(path.read_text(encoding="utf-8")))
        except Exception:
            pass

target_namespaces = {
    "bettercombat",
    "dnt",
    "jade",
    "sophisticatedbackpacks",
    "sophisticatedcore",
    "tbos",
    "weaponsexpanded",
    "xaerobetterpvp",
    "xaerominimap",
}

TOKEN_RE = re.compile(r"(%\d*\$?[sdif]|\{[^{}]*\}|§.|\\n|\n)")
manual = {
    "Mob Catcher Upgrade": "몹 포획 기능",
    "Captures passive mobs into backpack storage slots with sneak right-click": "웅크린 채 우클릭하면 수동적 몹을 배낭 보관 슬롯에 포획합니다.",
    "Advanced Mob Catcher Upgrade": "고급 몹 포획 기능",
    "Captures passive and hostile mobs into backpack storage slots with sneak right-click": "웅크린 채 우클릭하면 수동적 몹과 적대적 몹을 배낭 보관 슬롯에 포획합니다.",
    "Mob Catcher": "몹 포획",
    "Advanced Mob Catcher": "고급 몹 포획",
    "Better Combat": "개선된 전투",
    "Xaero's Minimap": "제로의 미니맵",
    "{0} {1,choice,0#Buckets|1#Bucket|1.0<Buckets}": "{0} {1,choice,0#양동이|1#양동이|1.0<양동이}",
    "{0} {1,choice,0#Millibuckets|1#Millibucket|1.0<Millibuckets}": "{0} {1,choice,0#밀리버킷|1#밀리버킷|1.0<밀리버킷}",
    "{1,choice,1#Small|2≤Medium|4≤Large|8≤Huge} {0}": "{1,choice,1#소형|2≤중형|4≤대형|8≤초대형} {0}",
}

todos = {}
for namespace in sorted(target_namespaces):
    english = langs[namespace]["en"]
    korean = langs[namespace]["ko"]
    todos[namespace] = dict(english) if namespace == "dnt" else {k: v for k, v in english.items() if k not in korean}
    print(f"{namespace}: overlay_keys={len(todos[namespace])}")

fragments = []
for todo in todos.values():
    for text in todo.values():
        if text in manual:
            continue
        for part in TOKEN_RE.split(text):
            if part and not TOKEN_RE.fullmatch(part) and re.search(r"[A-Za-z]", part):
                fragments.append(part)
unique = list(dict.fromkeys(fragments))
print("unique_fragments=", len(unique))

tokenizer = AutoTokenizer.from_pretrained("Helsinki-NLP/opus-mt-tc-big-en-ko")
model = AutoModelForSeq2SeqLM.from_pretrained("Helsinki-NLP/opus-mt-tc-big-en-ko")
model.eval()
torch.set_num_threads(max(1, min(4, torch.get_num_threads())))
translated_fragments = {}
batch_size = 16
with torch.inference_mode():
    for start in range(0, len(unique), batch_size):
        batch = unique[start : start + batch_size]
        encoded = tokenizer(batch, return_tensors="pt", padding=True, truncation=True, max_length=512)
        output = model.generate(**encoded, max_new_tokens=512, num_beams=2)
        decoded = tokenizer.batch_decode(output, skip_special_tokens=True)
        for source, translated in zip(batch, decoded):
            translated_fragments[source] = translated
        if start % 160 == 0:
            print("translated", min(start + batch_size, len(unique)), "/", len(unique))


def translate_value(text: str) -> str:
    if text in manual:
        return manual[text]
    parts = TOKEN_RE.split(text)
    return "".join(
        part
        if not part or TOKEN_RE.fullmatch(part) or not re.search(r"[A-Za-z]", part)
        else translated_fragments.get(part, part)
        for part in parts
    )


generated = {}
for namespace, todo in todos.items():
    translated = {key: translate_value(value) for key, value in todo.items()}
    if namespace == "tbos":
        destination = ROOT / "projects/survival-ascension/src/main/resources/assets/tbos/lang/ko_kr.json"
    else:
        destination = ROOT / f"projects/frontier-settlement/src/main/resources/assets/{namespace}/lang/ko_kr.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    base = {}
    if destination.exists():
        try:
            base = json.loads(destination.read_text(encoding="utf-8"))
        except Exception:
            base = {}
    base.update(translated)
    if namespace == "sophisticatedbackpacks":
        base.update(
            {
                "item.sophisticatedbackpacks.mob_catcher_upgrade": "몹 포획 기능",
                "item.sophisticatedbackpacks.mob_catcher_upgrade.tooltip": "웅크린 채 우클릭하면 수동적 몹을 배낭 보관 슬롯에 포획합니다.",
                "item.sophisticatedbackpacks.advanced_mob_catcher_upgrade": "고급 몹 포획 기능",
                "item.sophisticatedbackpacks.advanced_mob_catcher_upgrade.tooltip": "웅크린 채 우클릭하면 수동적 몹과 적대적 몹을 배낭 보관 슬롯에 포획합니다.",
            }
        )
    destination.write_text(json.dumps(base, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    generated[namespace] = len(translated)

missing = []
untranslated = []
for namespace in sorted(target_namespaces):
    english = langs[namespace]["en"]
    if namespace == "tbos":
        destination = ROOT / "projects/survival-ascension/src/main/resources/assets/tbos/lang/ko_kr.json"
    else:
        destination = ROOT / f"projects/frontier-settlement/src/main/resources/assets/{namespace}/lang/ko_kr.json"
    ours = json.loads(destination.read_text(encoding="utf-8")) if destination.exists() else {}
    covered = dict(langs[namespace]["ko"])
    covered.update(ours)
    absent = sorted(set(english) - set(covered))
    if absent:
        missing.append(f"{namespace}:{len(absent)}")
    for key in todos[namespace]:
        value = ours.get(key, "")
        source = english[key]
        literal = TOKEN_RE.sub("", source)
        if value == source and source not in manual and re.search(r"[A-Za-z]{2,}", literal):
            untranslated.append(f"{namespace}:{key}={value}")

if missing:
    raise SystemExit("missing localization keys: " + ", ".join(missing))
if untranslated:
    raise SystemExit("byte-identical untranslated values remain:\n" + "\n".join(untranslated[:60]))

for project in ("frontier-settlement", "survival-ascension"):
    for path in (ROOT / "projects" / project / "src/main/resources/assets").glob("*/lang/ko_kr.json"):
        json.loads(path.read_text(encoding="utf-8"))

print("KOREAN COVERAGE PASS", generated)
