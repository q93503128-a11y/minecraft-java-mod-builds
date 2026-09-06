#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 anchor, found {count}")
    return text.replace(old, new, 1)


# The 0.58 audit remains immutable. This wrapper translates only historical source needles whose
# authority legitimately moved or whose runtime identity advanced. It must not weaken the live audit.
release_path = ROOT / "tools/test_release_source.py"
release = read(release_path)
release = replace_once(release,
    'PREVIOUS_DOC_VERSION = "0.59.0-alpha.1"',
    'PREVIOUS_DOC_VERSION = "0.61.21-alpha.1"',
    "project doc current version")

insert_anchor = "# 0.61 also replaces the player-facing developer term \"affix\" with \"승천 옵션\".\n"
bridge = '''# 0.61.21 moved high-volume Mining extra-target execution into BulkMiningService while\n# preserving the old loaded-only/no-force-load contract. Adapt only the historical audit source\n# location and exact loaded-only needles; the current cumulative audit checks the new scheduler.\nlegacy = legacy.replace(\n    'mining57 = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")',\n    'mining57 = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java") + read("src/main/java/kr/moonseungjun/survivalascension/mining/BulkMiningService.java")'\n)\nlegacy = legacy.replace(\n    'need(mining57, ["if (!level.hasChunkAt(next)) continue;", "if (!level.hasChunkAt(target)) continue;"], "0.57 mining loaded-only")',\n    'need(mining57, ["if (!level.hasChunkAt(next) || level.getBlockEntity(next) != null) continue;", "if (!level.hasChunkAt(target) || level.getBlockEntity(target) != null) continue;"], "0.57 mining loaded-only")'\n)\n# Protocol 15 is the current wire authority; the historical 0.58 audit needle is version identity,\n# not permission to roll the live network back to protocol 9.\nlegacy = legacy.replace('PROTOCOL = "9"', 'PROTOCOL = "15"')\n\n'''
if bridge not in release:
    release = replace_once(release, insert_anchor, bridge + insert_anchor, "historical mining/protocol bridge")

# The current production radial menu deliberately names both eligible anchor container families.
# Translate the old shorthand needle to that more precise live UI text.
ui_bridge_anchor = "# Inject approved translations into nested test_current_source.py baseline.\n"
ui_bridge = '''# Current logistics UI names both container families instead of the older barrel-only shorthand.\nlegacy = legacy.replace('산업 가공소 완공 → 통 4블록 이내', '산업 가공소 완공 → 통/공용 보급고 4블록 이내')\n\n'''
if ui_bridge not in release:
    release = replace_once(release, ui_bridge_anchor, ui_bridge + ui_bridge_anchor, "production UI audit bridge")

# 0.61.20 intentionally raised the live Harvesting queue from the old 384 ceiling to 1152 so
# the current high-end hoe area is not silently truncated. The old 0.59.1 gameplay invariant is
# still exercised by the live cumulative audit; only the stale literal ceiling is advanced here.
release = replace_once(release,
    '"skillLevel >= 90 ? 4 : 0", "fieldMastery ? 8", "player.isShiftKeyDown()", "MAX_PENDING_PER_PLAYER = 384"',
    '"skillLevel >= 90 ? 4 : 0", "fieldMastery ? 8", "player.isShiftKeyDown()", "MAX_PENDING_PER_PLAYER = 1152"',
    "outer harvesting queue ceiling")

# PROJECT is a live canonical document with historical 0.59 sections below it; therefore its header
# must track the current runtime while the 0.59 section remains required as regression history.
release = replace_once(release,
    'need(project, ["Mod version: `0.59.0-alpha.1`", "## 0.59 Apex Content Escort Integration"], "historical PROJECT regression docs")',
    'need(project, ["Mod version: `0.61.21-alpha.1`", "## 0.59 Apex Content Escort Integration"], "historical PROJECT regression docs")',
    "outer PROJECT live header")
write(release_path, release)

# Keep the live long-form project identity current. Historical 0.58 sections below remain as history.
project_path = ROOT / "PROJECT.md"
project = read(project_path)
project = replace_once(project,
    '- Mod version: `0.61.20-alpha.1`',
    '- Mod version: `0.61.21-alpha.1`',
    "PROJECT version")
project = replace_once(project,
    '- Existing-world compatibility: 0.61.20 adds no SavedData ID or codec field and does not bump the network protocol. Existing skill XP, infrastructure/logistics/outpost/production data and affix CustomData remain compatible. Distinct rerolls and salvage caps are server-side service rules; the 1152 Harvesting queue cap is runtime-only. Network protocol remains 15.',
    '- Existing-world compatibility: 0.61.21 adds no SavedData ID or codec field and does not bump the network protocol. Existing skill XP, infrastructure/logistics/outpost/production data and affix CustomData remain compatible. Bulk Mining queues and queued-tool profiles are runtime-only. Network protocol remains 15.',
    "PROJECT compatibility")
section_anchor = "\n## 0.61.20 Equipment Economy & Harvest Queue Hardening / 장비 경제·수확 큐 안정화\n"
section = '''\n## 0.61.21 Bounded Bulk Mining / 대량 채굴 틱 분산\n- Pickaxe area mining, shovel earthworks, connected ore veins and Extract queue only automatic extra targets; the manually broken center block remains vanilla-authoritative.\n- Queued Mining destroys at most 12 blocks per player / 48 globally per tick and also obeys 3 ms local / 5 ms global soft server-thread budgets. Every extra still uses ServerPlayerGameMode.destroyBlock through AutomatedToolBreak.\n- The queue is capped at 512 targets, above every current live high-end area/vein ceiling, and never force-loads chunks or processes block entities.\n- Mining, Woodcutting and Harvesting queued jobs capture a damage-normalized tool component profile so changing to a different tool/affix/enchantment cancels the remaining queued work while ordinary durability loss does not.\n- No SavedData or packet change; protocol remains 15.\n'''
if "## 0.61.21 Bounded Bulk Mining" not in project:
    project = replace_once(project, section_anchor, section + section_anchor, "PROJECT 0.61.21 section")
write(project_path, project)

# Restore the curated Korean overlay for the audited TBS inventory/gameplay keys. These entries are
# Survival-owned resource overrides; keeping them exact prevents a low-quality generated translation
# from silently replacing established Korean player-facing terminology.
tbos_path = ROOT / "src/main/resources/assets/tbos/lang/ko_kr.json"
tbos = json.loads(read(tbos_path))
audited_tbos_ko = {
    "itemGroup.tbos.yesterglass": "스티브의 탄생",
    "block.tbos.archive_stone": "기록보관소 석재",
    "block.tbos.yesterglass": "예스터글라스",
    "block.tbos.cantor_gate": "기록보관소 보스 관문",
    "item.tbos.cracked_yesterglass_lens": "금 간 예스터글라스 렌즈",
    "item.tbos.archivists_journal": "기록관의 일지",
    "item.tbos.memory_plate.tooltip": "기억 등불에 사용하면 이 장면을 불러옵니다. 기억 판은 소모되지 않습니다.",
    "entity.tbos.parallax_wraith": "시차 망령",
    "entity.tbos.hour_cantor": "시간의 칸토르",
    "entity.tbos.phoenix_guardian": "최후의 큐레이터",
    "pickup.tbos.key": "기록보관소 열쇠",
    "boss.tbos.last_curator.title": "최후의 큐레이터",
}
tbos.update(audited_tbos_ko)
write(tbos_path, json.dumps(tbos, ensure_ascii=False, indent=2) + "\n")
reloaded_tbos = json.loads(read(tbos_path))
for key, value in audited_tbos_ko.items():
    if reloaded_tbos.get(key) != value:
        raise SystemExit(f"TBS Korean overlay restore failed: {key}")

print("Bridged Survival Ascension 0.61.21 historical audit, project identity and TBS Korean overlay")