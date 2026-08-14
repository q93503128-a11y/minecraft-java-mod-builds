from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATH = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenCapitalLifecycleManager.java"
text = PATH.read_text(encoding="utf-8")

old = '''            if (year - line.lastBirthYear() < BIRTH_SPACING_YEARS) continue;'''
new = '''            if (line.lastBirthYear() != Integer.MIN_VALUE
                    && year - line.lastBirthYear() < BIRTH_SPACING_YEARS) continue;'''

if new not in text:
    if old not in text:
        raise SystemExit("capital lifecycle birth-spacing anchor missing")
    text = text.replace(old, new, 1)

if "line.lastBirthYear() != Integer.MIN_VALUE" not in text:
    raise SystemExit("capital lifecycle first-birth sentinel guard missing")

PATH.write_text(text, encoding="utf-8")
print("Fixed first-birth spacing sentinel overflow without changing later birth spacing.")
