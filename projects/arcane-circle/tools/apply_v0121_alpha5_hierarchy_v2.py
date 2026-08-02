#!/usr/bin/env python3
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
original = TOOLS / "apply_v0121_alpha5_hierarchy.py"
source = original.read_text(encoding="utf-8")
source = source.replace(
    "SELF = Path(__file__).resolve()",
    'SELF = ROOT / "tools/apply_v0121_alpha5_hierarchy.py"',
    1,
)
old = '''def patch_damage_attribution() -> None:
    magic_root = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic"
    pattern = re.compile(
        r"\\b([A-Za-z_][A-Za-z0-9_]*)\\.hurtServer\\(level,\\s*level\\.damageSources\\(\\)\\.magic\\(\\),\\s*([^;]+)\\);"
    )
    replaced = 0
    for path in magic_root.glob("*.java"):
        if path.name == "ArcaneDamage.java":
            continue
        text = path.read_text(encoding="utf-8")
        updated, count = pattern.subn(r"ArcaneDamage.hurt(level, player, \\1, \\2);", text)
        if count:
            path.write_text(updated, encoding="utf-8")
            replaced += count
    if replaced < 9:
        raise RuntimeError(f"expected at least 9 attributed magic damage replacements, got {replaced}")
    offenders = []
    for path in magic_root.glob("*.java"):
        if path.name == "ArcaneDamage.java":
            continue
        text = path.read_text(encoding="utf-8")
        if "damageSources().magic()" in text:
            offenders.append(path.name)
    if offenders:
        raise RuntimeError(f"unattributed spell damage remains: {offenders}")
'''
new = '''def patch_damage_attribution() -> None:
    magic_root = ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic"
    direct_pattern = re.compile(
        r"\\b([A-Za-z_][A-Za-z0-9_]*)\\.hurtServer\\(level,\\s*level\\.damageSources\\(\\)\\.magic\\(\\),\\s*([^;]+)\\);"
    )
    attributed_pattern = re.compile(
        r"\\b([A-Za-z_][A-Za-z0-9_]*)\\.hurtServer\\(level,\\s*level\\.damageSources\\(\\)\\.playerAttack\\(player\\),\\s*([^;]+)\\);"
    )
    replaced = 0
    for path in magic_root.glob("*.java"):
        if path.name == "ArcaneDamage.java":
            continue
        text = path.read_text(encoding="utf-8")
        updated, direct_count = direct_pattern.subn(r"ArcaneDamage.hurt(level, player, \\1, \\2);", text)
        # Remaining forms include multiline lambdas and helper returns. Bind their source first,
        # then collapse ordinary statements through ArcaneDamage when the syntax is simple.
        source_count = updated.count("level.damageSources().magic()")
        updated = updated.replace("level.damageSources().magic()", "level.damageSources().playerAttack(player)")
        updated, attributed_count = attributed_pattern.subn(r"ArcaneDamage.hurt(level, player, \\1, \\2);", updated)
        if direct_count or source_count or attributed_count:
            path.write_text(updated, encoding="utf-8")
            replaced += direct_count + source_count
    if replaced < 12:
        raise RuntimeError(f"expected broad spell damage attribution, got {replaced} replacements")
    offenders = []
    for path in magic_root.glob("*.java"):
        if path.name == "ArcaneDamage.java":
            continue
        text = path.read_text(encoding="utf-8")
        if "damageSources().magic()" in text:
            offenders.append(path.name)
    if offenders:
        raise RuntimeError(f"unattributed spell damage remains: {offenders}")
'''
if source.count(old) != 1:
    raise RuntimeError("alpha.5 damage patch anchor mismatch")
source = source.replace(old, new, 1)
exec(compile(source, str(original), "exec"))
