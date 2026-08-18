from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java')
s = path.read_text(encoding='utf-8')
for old in (
    'import kr.moonseungjun.livingkingdoms.world.ErdenUrbanAuthoredInteriorPreserver;\n',
    '        ErdenUrbanAuthoredInteriorPreserver.captureBeforeConversion(event);\n',
    '        ErdenUrbanAuthoredInteriorPreserver.restoreAfterConversion(event);\n',
):
    if old not in s:
        raise SystemExit(f'Obsolete authored-interior preserver hook not found: {old!r}')
    s = s.replace(old, '', 1)
path.write_text(s, encoding='utf-8')
print('Removed obsolete ErdenUrbanAuthoredInteriorPreserver runtime hooks')
