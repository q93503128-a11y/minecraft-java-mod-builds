#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SOURCE_REPLACEMENTS = [
    ("Math.min(13, base + bonus)", "Math.min(21, base + bonus)"),
    ("new MaterialCost(Items.AMETHYST_SHARD, 48", "new MaterialCost(Items.AMETHYST_SHARD, 12"),
    ("new MaterialCost(Items.AMETHYST_SHARD, 96", "new MaterialCost(Items.AMETHYST_SHARD, 24"),
    ("재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터", "재료 소비: 같은 차원에서 현재 로딩된 등록 창고 전체를 공용 재고로 사용하고"),
    ("4블록 내 기본 통 앵커", "산업 가공소 완공 → 통 4블록 이내"),
    ('new LocalRequirement("식량", 32', 'new LocalRequirement("식량(밀/당근/감자/비트)", 12'),
    ('new LocalRequirement("철 주괴", 8', 'new LocalRequirement("철 주괴", 3'),
    ('new LocalRequirement("연료", 8', 'new LocalRequirement("연료(석탄 또는 숯)", 3'),
    ('new LocalRequirement("식량", 48', 'new LocalRequirement("식량(밀/당근/감자/비트)", 16'),
    ('new LocalRequirement("철 주괴", 16', 'new LocalRequirement("철 주괴", 5'),
    ('new LocalRequirement("통나무", 32', 'new LocalRequirement("아무 종류의 통나무", 12'),
    ('new LocalRequirement("식량", 96', 'new LocalRequirement("식량(밀/당근/감자/비트)", 32'),
    ('new LocalRequirement("철 주괴", 32', 'new LocalRequirement("철 주괴", 8'),
    ('new LocalRequirement("석재 벽돌", 128', 'new LocalRequirement("석재 벽돌", 32'),
    ("전초재고(식량48/철16/통나무32)", "전초 재고(식량 16 · 철 주괴 5 · 아무 종류의 통나무 12)"),
    ("전초재고(식량96/철32/석재벽돌128)", "전초 재고(식량 32 · 철 주괴 8 · 석재 벽돌 32)"),
    ("전초재고(식량32/철8/연료8)", "전초 재고(식량 12 · 철 주괴 3 · 연료: 석탄 또는 숯 3)"),
    ("화물 → 전초 현지재고 → 방어/원정", "등록 물류 통은 같은 차원 로딩 중이면 원격 사용"),
    ('new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)', 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 384)'),
    ('new Requirement(Items.COBBLESTONE, "조약돌", 1536)', 'new Requirement(Items.COBBLESTONE, "조약돌", 256)'),
    ('new Requirement(Items.GRAVEL, "자갈", 1536)', 'new Requirement(Items.GRAVEL, "자갈", 256)'),
    ("한도3→토목6→중추9", "산업 가공소 완공 → 통 4블록 이내"),
    ("FRONTLINE_FOOD = 176", "FRONTLINE_FOOD = 60"),
    ("FRONTLINE_IRON = 56", "FRONTLINE_IRON = 16"),
    ("FRONTLINE_FUEL = 8", "FRONTLINE_FUEL = 3"),
    ("FRONTLINE_LOGS = 32", "FRONTLINE_LOGS = 12"),
    ("FRONTLINE_STONE_BRICKS = 128", "FRONTLINE_STONE_BRICKS = 32"),
    ("식량176+철56+석탄/목탄8+통나무32+석재벽돌128", "식량(밀/당근/감자/비트) 60 + 철 주괴 16 + 연료(석탄 또는 숯) 3 + 아무 종류의 통나무 12 + 석재 벽돌 32"),
    ("원정은 식량32+철8+석탄/목탄8", "원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3"),
    ("전초 방어는 식량48+철16+통나무32", "전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12"),
    ("요새 방어는 식량96+철32+석재벽돌128", "요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32"),
    ("원정은 식량32+철8+석탄/목탄8", "원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3"),
    ("원정은 식량32+철8+석탄/목탄8", "원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3"),
]


def inject_source_wrapper():
    path = ROOT / "tools/test_release_source.py"
    text = path.read_text(encoding="utf-8")
    sentinel = "# 0.61 solo/QOL nested-baseline translation v2"
    if sentinel in text:
        return
    marker = 'namespace = {"__file__": str(legacy_path), "__name__": "__main__"}'
    if marker not in text:
        raise SystemExit("source wrapper namespace marker missing")
    lines = [sentinel, "# Adapt direct 0.58 needles without editing the historical audit file.", "for _old, _new in ["]
    for old, new in SOURCE_REPLACEMENTS:
        lines.append(f"    ({old!r}, {new!r}),")
    lines += ["]:", "    legacy = legacy.replace(_old, _new)", "", "# Inject approved translations into nested test_current_source.py baseline.", "_baseline_lines = []", "for _old, _new in ["]
    for old, new in SOURCE_REPLACEMENTS:
        lines.append(f"    ({old!r}, {new!r}),")
    lines += ["]:", "    _baseline_lines.append(f\"baseline = baseline.replace({_old!r}, {_new!r})\")", "_baseline_anchor = 'baseline = baseline.replace(BASELINE_VERSION, REQUIRED_VERSION)'", "legacy = legacy.replace(_baseline_anchor, _baseline_anchor + '\\n' + '\\n'.join(_baseline_lines), 1)", ""]
    text = text.replace(marker, "\n".join(lines) + "\n" + marker, 1)
    path.write_text(text, encoding="utf-8")


def inject_content_wrapper():
    path = ROOT / "tools/test_release_content_pack.py"
    text = path.read_text(encoding="utf-8")
    sentinel = "# 0.61 solo/QOL nested content translation v2"
    if sentinel in text:
        return
    marker = 'namespace = {"__file__": str(legacy_path), "__name__": "__main__"}'
    if marker not in text:
        raise SystemExit("content wrapper namespace marker missing")
    lines = [sentinel, "# Adapt direct 0.58 pack needles without editing the historical audit file.", "for _old, _new in ["]
    for old, new in SOURCE_REPLACEMENTS:
        lines.append(f"    ({old!r}, {new!r}),")
    lines += ["]:", "    legacy = legacy.replace(_old, _new)", "", "# Inject approved translations into nested test_content_pack_source.py baseline.", "_content_lines = []", "for _old, _new in ["]
    for old, new in SOURCE_REPLACEMENTS:
        lines.append(f"    ({old!r}, {new!r}),")
    lines += ["]:", "    _content_lines.append(f\"baseline = baseline.replace({_old!r}, {_new!r})\")", "_content_anchor = 'baseline = baseline.replace(BASELINE_LOCK_VERSION, REQUIRED_LOCK_VERSION)'", "legacy = legacy.replace(_content_anchor, _content_anchor + '\\n' + '\\n'.join(_content_lines), 1)", ""]
    text = text.replace(marker, "\n".join(lines) + "\n" + marker, 1)
    path.write_text(text, encoding="utf-8")


inject_source_wrapper()
inject_content_wrapper()
print("0.61 nested audit expectation adapters installed")
