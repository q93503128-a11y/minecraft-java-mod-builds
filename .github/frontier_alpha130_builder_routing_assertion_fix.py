from pathlib import Path

TEST = Path('projects/frontier-settlement/tools/test_current_source.py')
text = TEST.read_text(encoding='utf-8')
old = '''require("builderStrandedOnArtificialElevation" in construction and "nearestNaturalGroundBelow" in construction
        and "artificialRise < 3" in construction,
        "disconnected elevated builder recovery missing")
'''
new = '''require("builderStrandedOnArtificialElevation" in construction and "builderOnArtificialElevation" in construction
        and "nearestNaturalGroundBelow" in construction and "return artificialRise >= 3;" in construction,
        "disconnected elevated builder recovery missing")
'''
if text.count(old) != 1:
    raise SystemExit(f'elevated recovery assertion: expected one match, got {text.count(old)}')
TEST.write_text(text.replace(old, new, 1), encoding='utf-8')
print('Alpha.130 elevated-recovery assertion updated')
