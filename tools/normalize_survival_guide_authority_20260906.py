from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GUIDE = ROOT / "projects/survival-ascension/src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
CHECK = ROOT / "projects/survival-ascension/tools/test_current_source.py"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count == 0:
        return text
    if count != 1:
        raise SystemExit(f"{label}: expected at most one anchor, found {count}")
    return text.replace(old, new, 1)


guide = GUIDE.read_text(encoding="utf-8")

guide = replace_once(
    guide,
    '                h("전선 화물 묶음"), p("빈 상자 광산수레에서 Shift를 누른 채 물리 화물 수레를 선택하면 일반 잡자재 대신 원정1회+전초 방어1회+요새 방어1회분만 선별합니다. 식량(밀/당근/감자/비트) 60 + 철 주괴 16 + 연료(석탄 또는 숯) 3 + 아무 종류의 통나무 12 + 석재 벽돌 32이며 출발 전초 통에서 실제로 빠지고 같은 수레로 운송됩니다. Shift 없이 선택하면 기존 일반 대량화물 적재입니다."),\n',
    '                h("전선 화물 묶음"), p("빈 상자 광산수레에서 Shift를 누른 채 물리 화물 수레를 선택하면 일반 잡자재 대신 원정1회+전초 방어1회+요새 방어1회분만 선별합니다. 식량" + FreightService.FRONTLINE_FOOD\n'
    '                        + " + 철 주괴 " + FreightService.FRONTLINE_IRON + " + 연료 " + FreightService.FRONTLINE_FUEL\n'
    '                        + " + 통나무 " + FreightService.FRONTLINE_LOGS + " + 석재 벽돌 " + FreightService.FRONTLINE_STONE_BRICKS\n'
    '                        + "이며 출발 전초 통에서 실제로 빠지고 같은 수레로 운송됩니다. Shift 없이 선택하면 기존 일반 대량화물 적재입니다."),\n',
    "frontline bundle",
)

guide = replace_once(
    guide,
    '                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 실물 재고가 있어야 시작합니다. 원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3, 전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12, 요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32을 씁니다. 인벤토리나 다른 거점 재고로 대체하지 않으므로 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),\n',
    '                h("전선 현지 보급"), p("원정 작전·전초 방어·요새 방어는 현장 보급권과 별도로 출발 전초의 등록 통+연결 창고 통에 각 작전이 요구하는 실물 재고가 있어야 시작합니다. 필요한 수량은 각 작전의 서버 규칙이 직접 검증하며 인벤토리나 다른 거점 재고로 대체하지 않습니다. 먼 전초는 직접 적재하거나 물리 화물로 보급해야 합니다."),\n',
    "frontline local supply",
)
GUIDE.write_text(guide, encoding="utf-8")

check = CHECK.read_text(encoding="utf-8")
anchor = 'require("장비 분해" in guide and "남은 내구도" in guide,\n        "dynamic salvage rules are hidden from player guidance")\n'
extra = anchor + 'require("식량(밀/당근/감자/비트) 60" not in guide and "원정은 식량(밀/당근/감자/비트) 12" not in guide,\n        "guide contains duplicated hard-coded frontline supply balances")\n'
if "duplicated hard-coded frontline supply balances" not in check:
    if anchor not in check:
        raise SystemExit("test anchor missing")
    check = check.replace(anchor, extra, 1)
    CHECK.write_text(check, encoding="utf-8")

print("Normalized remaining Survival Ascension freight guide authority")
