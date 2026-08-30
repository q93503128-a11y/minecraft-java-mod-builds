#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def write(rel, text):
    (ROOT / rel).write_text(text, encoding='utf-8')

def swap(rel, pairs):
    text = read(rel)
    for old, new in pairs:
        if old not in text:
            raise SystemExit(f'{rel}: missing token {old!r}')
        text = text.replace(old, new, 1)
    write(rel, text)

# Repair the solo migration's generated status line before compilation.
p = 'src/main/java/kr/moonseungjun/survivalascension/production/FieldDepotService.java'
text = read(p)
bad = '                + " §7· 같은 차원 로딩 창고는 거리 제한 없음));'
good = '                + " §7· 같은 차원 로딩 창고는 거리 제한 없음"));'
if bad not in text:
    raise SystemExit('FieldDepotService generated status-string hotfix token missing')
write(p, text.replace(bad, good, 1))

# The frontline manifest is exactly one new solo expedition + one normal defense + one bastion defense.
swap('src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java', [
    ('// 0.48 local-operation costs combined exactly once each:\n    // expedition food32/iron8/fuel8 + defense food48/iron16/logs32\n    // + bastion food96/iron32/stone bricks128.\n    public static final int FRONTLINE_FOOD = 176;\n    public static final int FRONTLINE_IRON = 56;\n    public static final int FRONTLINE_FUEL = 8;\n    public static final int FRONTLINE_LOGS = 32;\n    public static final int FRONTLINE_STONE_BRICKS = 128;',
     '// Solo local-operation costs combined exactly once each:\n    // expedition food12/iron3/fuel3 + defense food16/iron5/logs12\n    // + bastion food32/iron8/stone bricks32.\n    public static final int FRONTLINE_FOOD = 60;\n    public static final int FRONTLINE_IRON = 16;\n    public static final int FRONTLINE_FUEL = 3;\n    public static final int FRONTLINE_LOGS = 12;\n    public static final int FRONTLINE_STONE_BRICKS = 32;'),
    ('"§7/§f" + FRONTLINE_FOOD + " · 철 §e"', '"§7/§f" + FRONTLINE_FOOD + " · 철 주괴 §e"'),
    ('"§7/§f" + FRONTLINE_IRON + " · 연료 §e"', '"§7/§f" + FRONTLINE_IRON + " · 연료(석탄/숯) §e"'),
    ('"§7/§f" + FRONTLINE_FUEL + " · 통나무 §e"', '"§7/§f" + FRONTLINE_FUEL + " · 아무 종류의 통나무 §e"'),
    ('"§7/§f" + FRONTLINE_LOGS + " · 석재벽돌 §e"', '"§7/§f" + FRONTLINE_LOGS + " · 석재 벽돌 §e"'),
])

# Guide must disclose the real current costs and exact item names, not historical shorthand.
swap('src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java', [
    ('식량176+철56+석탄/목탄8+통나무32+석재벽돌128',
     '식량(밀/당근/감자/비트) 60 + 철 주괴 16 + 연료(석탄 또는 숯) 3 + 아무 종류의 통나무 12 + 석재 벽돌 32'),
    ('원정은 식량32+철8+석탄/목탄8, 전초 방어는 식량48+철16+통나무32, 요새 방어는 식량96+철32+석재벽돌128',
     '원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3, 전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12, 요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32'),
])

# Exact-name wording for the operational status text that still used historical abbreviations.
p = 'src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java'
text = read(p)
text = text.replace('식량32+철8+연료8 / 방어=식량48+철16+통나무32 / 요새=식량96+철32+석재벽돌128',
                    '식량(밀/당근/감자/비트)12+철 주괴3+연료(석탄/숯)3 / 방어=식량16+철 주괴5+아무 종류의 통나무12 / 요새=식량32+철 주괴8+석재 벽돌32')
write(p, text)

# Acceptance: old solo-prohibitive frontline bundle and ambiguous guide names must be gone.
freight = read('src/main/java/kr/moonseungjun/survivalascension/production/FreightService.java')
guide = read('src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java')
for token in ('FRONTLINE_FOOD = 60', 'FRONTLINE_IRON = 16', 'FRONTLINE_FUEL = 3', 'FRONTLINE_LOGS = 12', 'FRONTLINE_STONE_BRICKS = 32', '철 주괴', '석재 벽돌'):
    if token not in freight:
        raise SystemExit(f'freight acceptance missing: {token}')
for token in ('원정은 식량(밀/당근/감자/비트) 12', '철 주괴 3', '연료(석탄 또는 숯) 3', '아무 종류의 통나무 12', '석재 벽돌 32'):
    if token not in guide:
        raise SystemExit(f'guide acceptance missing: {token}')
for old in ('식량176+철56', '원정은 식량32+철8', '요새 방어는 식량96+철32+석재벽돌128'):
    if old in guide:
        raise SystemExit(f'old guide cost remains: {old}')

print('Survival Ascension final freight/guide QOL migration applied')
