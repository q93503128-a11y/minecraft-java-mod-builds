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
TMP = ROOT / ".tmp-full-ko-nllb"
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
        headers={"User-Agent": "Frontier-Full-Korean-NLLB/1.0"},
    )
    with urllib.request.urlopen(req, timeout=60) as response:
        meta = json.load(response)
    primary = next((f for f in meta["files"] if f.get("primary")), meta["files"][0])
    sources.append({"name": primary["filename"], "url": primary["url"]})
sources = list({s["name"]: s for s in sources}.values())

# IMPORTANT: only third-party JAR translations count as upstream Korean coverage here.
# Project overlays are the output of this script and must never suppress regeneration,
# otherwise a bad generated translation can become self-perpetuating.
langs = defaultdict(lambda: {"en": {}, "ko": {}})
for index, source in enumerate(sources):
    jar = TMP / f"{index}.jar"
    req = urllib.request.Request(source["url"], headers={"User-Agent": "Frontier-Full-Korean-NLLB/1.0"})
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
                # Dungeons & Taverns 5.3.0 ships malformed ko_kr JSON.
                # Its English file is valid, so D&T is rebuilt as a complete Korean overlay.
                continue
            langs[namespace]["en"].update(data)
            if locale == "ko_kr":
                langs[namespace]["ko"].update(data)

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

# Keep Minecraft/Java formatting placeholders byte-for-byte intact. Translation is performed
# only on the surrounding natural-language fragments.
TOKEN_RE = re.compile(r"(%\d*\$?[sdif]|\{[^{}]*\}|§.|\\n|\n)")

# High-value gameplay/UI wording that should not depend on machine translation.
MANUAL_TEXT = {
    "Mob Catcher Upgrade": "몹 포획 업그레이드",
    "Captures passive mobs into backpack storage slots with sneak right-click": "웅크린 채 우클릭하면 수동적 몹을 배낭 보관 슬롯에 포획합니다.",
    "Advanced Mob Catcher Upgrade": "고급 몹 포획 업그레이드",
    "Captures passive and hostile mobs into backpack storage slots with sneak right-click": "웅크린 채 우클릭하면 수동적 몹과 적대적 몹을 배낭 보관 슬롯에 포획합니다.",
    "Mob Catcher": "몹 포획",
    "Advanced Mob Catcher": "고급 몹 포획",
    "Better Combat": "개선된 전투",
    "Xaero's Minimap": "제로의 미니맵",
    "{0} {1,choice,0#Buckets|1#Bucket|1.0<Buckets}": "{0} {1,choice,0#양동이|1#양동이|1.0<양동이}",
    "{0} {1,choice,0#Millibuckets|1#Millibucket|1.0<Millibuckets}": "{0} {1,choice,0#밀리버킷|1#밀리버킷|1.0<밀리버킷}",
    "{1,choice,1#Small|2≤Medium|4≤Large|8≤Huge} {0}": "{1,choice,1#소형|2≤중형|4≤대형|8≤초대형} {0}",
}

KEY_OVERRIDES = {
    "item.sophisticatedbackpacks.mob_catcher_upgrade": "몹 포획 업그레이드",
    "item.sophisticatedbackpacks.mob_catcher_upgrade.tooltip": "웅크린 채 우클릭하면 수동적 몹을 배낭 보관 슬롯에 포획합니다.",
    "item.sophisticatedbackpacks.advanced_mob_catcher_upgrade": "고급 몹 포획 업그레이드",
    "item.sophisticatedbackpacks.advanced_mob_catcher_upgrade.tooltip": "웅크린 채 우클릭하면 수동적 몹과 적대적 몹을 배낭 보관 슬롯에 포획합니다.",
    "sophisticatedbackpacks.configuration.mobCatcherUpgrade": "몹 포획 업그레이드",
}

todos = {}
for namespace in sorted(target_namespaces):
    english = langs[namespace]["en"]
    upstream_korean = langs[namespace]["ko"]
    # D&T's shipped Korean JSON cannot be parsed, so fully supersede it.
    todos[namespace] = dict(english) if namespace == "dnt" else {
        key: value for key, value in english.items() if key not in upstream_korean
    }
    print(f"{namespace}: overlay_keys={len(todos[namespace])}")

fragments = []
for todo in todos.values():
    for text in todo.values():
        if text in MANUAL_TEXT:
            continue
        for part in TOKEN_RE.split(text):
            if part and not TOKEN_RE.fullmatch(part) and re.search(r"[A-Za-z]", part):
                fragments.append(part)
unique = list(dict.fromkeys(fragments))
print("unique_fragments=", len(unique))

# NLLB is markedly better than the old OPUS model for short mod UI/config strings.
# The old model generated semantically unrelated garbage for many entries; those files are
# deliberately regenerated from external en_us here rather than incrementally retained.
MODEL_NAME = "facebook/nllb-200-distilled-600M"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME, src_lang="eng_Latn")
model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_NAME)
model.eval()
torch.set_num_threads(max(1, min(4, torch.get_num_threads())))
kor_bos = tokenizer.convert_tokens_to_ids("kor_Hang")
translated_fragments = {}
batch_size = 8
with torch.inference_mode():
    for start in range(0, len(unique), batch_size):
        batch = unique[start : start + batch_size]
        encoded = tokenizer(batch, return_tensors="pt", padding=True, truncation=True, max_length=384)
        output = model.generate(
            **encoded,
            forced_bos_token_id=kor_bos,
            max_new_tokens=192,
            num_beams=1,
        )
        decoded = tokenizer.batch_decode(output, skip_special_tokens=True)
        for source, translated in zip(batch, decoded):
            translated_fragments[source] = translated.strip()
        if start % 160 == 0:
            print("translated", min(start + batch_size, len(unique)), "/", len(unique))


def translate_value(text: str) -> str:
    if text in MANUAL_TEXT:
        return MANUAL_TEXT[text]
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
    if namespace == "sophisticatedbackpacks":
        translated.update({key: value for key, value in KEY_OVERRIDES.items() if key in langs[namespace]["en"]})

    if namespace == "tbos":
        destination = ROOT / "projects/survival-ascension/src/main/resources/assets/tbos/lang/ko_kr.json"
    else:
        destination = ROOT / f"projects/frontier-settlement/src/main/resources/assets/{namespace}/lang/ko_kr.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    # Replace generated overlays wholesale. Never merge the previous generated output.
    destination.write_text(json.dumps(translated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    generated[namespace] = len(translated)

missing = []
untranslated = []
empty_bad = []
placeholder_bad = []
no_korean = []
BANNED_GARBAGE = (
    "doggystyle",
    "FileReport",
    "CompositeSON",
    "♡♡♡",
    "콘스탄티노플",
    "Scenic 님",
    "process는 다음을 수행",
)
found_garbage = []

for namespace in sorted(target_namespaces):
    english = langs[namespace]["en"]
    upstream_korean = langs[namespace]["ko"]
    destination = (
        ROOT / "projects/survival-ascension/src/main/resources/assets/tbos/lang/ko_kr.json"
        if namespace == "tbos"
        else ROOT / f"projects/frontier-settlement/src/main/resources/assets/{namespace}/lang/ko_kr.json"
    )
    ours = json.loads(destination.read_text(encoding="utf-8"))
    covered = dict(upstream_korean)
    covered.update(ours)
    absent = sorted(set(english) - set(covered))
    if absent:
        missing.append(f"{namespace}:{len(absent)}")

    for key, source in todos[namespace].items():
        value = ours.get(key, "")
        source_tokens = TOKEN_RE.findall(source)
        value_tokens = TOKEN_RE.findall(value)
        if source_tokens != value_tokens:
            placeholder_bad.append(f"{namespace}:{key}: {source_tokens!r} -> {value_tokens!r}")
        literal_source = TOKEN_RE.sub("", source)
        if re.search(r"[A-Za-z]{2,}", literal_source):
            if not value.strip():
                empty_bad.append(f"{namespace}:{key}")
            if value == source and source not in MANUAL_TEXT:
                untranslated.append(f"{namespace}:{key}={value}")
            # A sentence/label that genuinely needed translation should normally contain Hangul.
            # Single-token proper names and technical identifiers are allowed to remain Latin.
            if " " in literal_source.strip() and not re.search(r"[가-힣]", value):
                no_korean.append(f"{namespace}:{key}={value}")
        if any(token in value for token in BANNED_GARBAGE):
            found_garbage.append(f"{namespace}:{key}={value}")

if missing:
    raise SystemExit("missing localization keys: " + ", ".join(missing))
if placeholder_bad:
    raise SystemExit("placeholder corruption:\n" + "\n".join(placeholder_bad[:60]))
if empty_bad:
    raise SystemExit("empty translations for non-empty English strings:\n" + "\n".join(empty_bad[:60]))
if untranslated:
    raise SystemExit("byte-identical untranslated values remain:\n" + "\n".join(untranslated[:60]))
if no_korean:
    raise SystemExit("multi-word translated values without Hangul:\n" + "\n".join(no_korean[:60]))
if found_garbage:
    raise SystemExit("known garbage from previous translator still present:\n" + "\n".join(found_garbage[:60]))

for project in ("frontier-settlement", "survival-ascension"):
    for path in (ROOT / "projects" / project / "src/main/resources/assets").glob("*/lang/ko_kr.json"):
        json.loads(path.read_text(encoding="utf-8"))

print("KOREAN COVERAGE AND QUALITY GATE PASS", generated)
print("mob_catcher=", KEY_OVERRIDES["item.sophisticatedbackpacks.mob_catcher_upgrade"])
print("advanced_mob_catcher=", KEY_OVERRIDES["item.sophisticatedbackpacks.advanced_mob_catcher_upgrade"])
