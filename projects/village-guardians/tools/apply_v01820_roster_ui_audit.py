#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
ui = JAVA / "VillageUiService.java"
text = ui.read_text(encoding="utf-8")
old = '''            case BARRACKS -> "최대 내구도 " + (620 + safe * 130) + " · 훈련 XP " + (30 + safe * 18)
                    + " · 용병 정원 " + (1 + safe / 2);'''
new = '''            case BARRACKS -> "최대 내구도 " + (620 + safe * 130) + " · 훈련 XP " + (30 + safe * 18)
                    + " · 용병 정원 " + (1 + safe / 2 + VillageDefenseResearchSystem.mercenaryCapacityBonus());'''
if old not in text:
    raise SystemExit("barracks management capacity anchor missing")
ui.write_text(text.replace(old, new, 1), encoding="utf-8")

contract = ROOT / "tools/test_v01820_roster_ui_consistency.py"
contract.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

def main():
    ui = read("VillageUiService.java")
    merc = read("VillageMercenarySystem.java")
    research = read("VillageDefenseResearchSystem.java")
    assert "public static int capacity()" in merc
    assert "VillageDefenseResearchSystem.mercenaryCapacityBonus()" in merc
    assert '" · 용병 정원 " + (1 + safe / 2 + VillageDefenseResearchSystem.mercenaryCapacityBonus())' in ui
    assert "mercenaryCapacityAt" in research
    assert '"open_mercenary_roster", "용병 명부 · " + VillageMercenarySystem.rosterCount() + " / "' in ui
    print("[PASS] barracks management and roster screens show research-aware mercenary capacity")

if __name__ == "__main__":
    main()
''', encoding="utf-8")
print("[PASS] v0.18.20 roster UI consistency patch staged")
