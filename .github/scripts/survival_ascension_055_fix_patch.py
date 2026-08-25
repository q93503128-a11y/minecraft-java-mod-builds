from pathlib import Path

path = Path(__file__).with_name("survival_ascension_055_patch.py")
lines = path.read_text(encoding="utf-8").splitlines(keepends=True)

out = []
java_zone = False
jar_verify_zone = False
for line in lines:
    if line.startswith("# Dedicated spear affix category"):
        java_zone = True
    if line.startswith("# User-facing equipment flows"):
        java_zone = False
    if line.startswith('jar_verify = "tools/verify_jar.py"'):
        jar_verify_zone = True

    if java_zone or jar_verify_zone:
        line = line.replace("dedent('''\\", "'''\\")
        line = line.replace("'''), '''\\", "''', '''\\")
        line = line.replace("'''))", "''')")

    out.append(line)

text = "".join(out)
if "replace_once(affix, dedent(" in text or "replace_once(combat, dedent(" in text:
    raise SystemExit("Java dedent anchor repair incomplete")

old_changelog = "- Drive line is hostile-only, zero-damage/zero-XP, Shift-suppressible and knockback-resistance-aware;"
new_changelog = "- Drive line is hostile-only, 0피해/0XP (zero-damage/zero-XP), Shift-suppressible and knockback-resistance-aware;"
if text.count(old_changelog) != 1:
    raise SystemExit(f"expected one 0.55 CHANGELOG wording anchor, got {text.count(old_changelog)}")
text = text.replace(old_changelog, new_changelog, 1)

compile(text, str(path), "exec")
path.write_text(text, encoding="utf-8")
print("0.55 patch delimiters/docs repaired and syntax-checked")
