from pathlib import Path


def extract_applicator() -> None:
    source = Path('.github/workflows/sa054-apply.yml').read_text(encoding='utf-8')
    start_marker = "          python3 - <<'PY'\n"
    end_marker = "\n          PY\n"
    start = source.index(start_marker) + len(start_marker)
    end = source.index(end_marker, start)
    code = '\n'.join(
        line[10:] if line.startswith('          ') else line
        for line in source[start:end].splitlines()
    ) + '\n'
    compile(code, '/tmp/sa054_apply.py', 'exec')
    Path('/tmp/sa054_apply.py').write_text(code, encoding='utf-8')
    namespace = {'__name__': '__main__', '__file__': '/tmp/sa054_apply.py'}
    exec(compile(code, '/tmp/sa054_apply.py', 'exec'), namespace)


def repair_release_verifier() -> None:
    path = Path('projects/survival-ascension/tools/verify_release_jar.py')
    text = path.read_text(encoding='utf-8')
    start = text.index('    for token in [b"TOOLS_MACE"')
    end = text.index('\nprint("frontline_freight_manifest_runtime=present")', start)
    lines = [
        'with zipfile.ZipFile(jar) as zf:',
        '    affix054 = zf.read("kr/moonseungjun/survivalascension/equipment/AscensionAffixes.class")',
        '    combat054 = zf.read("kr/moonseungjun/survivalascension/combat/CombatProgression.class")',
        '    main054 = zf.read("kr/moonseungjun/survivalascension/SurvivalAscension.class")',
        '    for token in [b"TOOLS_MACE", b"maceImpactRadiusBonus", b"maceImpactTargetBonus", b"maceImpactKnockbackBonus", b"maceImpactLiftBonus"]:',
        '        if token not in affix054:',
        '            raise SystemExit(f"0.54 compiled mace-affix token missing: {token!r}")',
        '    for token in [b"IS_MACE_SMASH", b"tryMaceImpact", b"KNOCKBACK_RESISTANCE", b"setDeltaMovement"]:',
        '        if token not in combat054:',
        '            raise SystemExit(f"0.54 compiled mace-impact token missing: {token!r}")',
        '    if b"mace outer impact rings" not in main054:',
        '        raise SystemExit("0.54 runtime banner missing mace outer impact rings")',
        '',
    ]
    block = '\n'.join(lines) + '\n'
    repaired = text[:start] + block + text[end:]
    compile(repaired, str(path), 'exec')
    path.write_text(repaired, encoding='utf-8')


if __name__ == '__main__':
    extract_applicator()
    repair_release_verifier()
