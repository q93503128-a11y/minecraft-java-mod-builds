from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java')
s = path.read_text(encoding='utf-8')
removed = 0
for old in (
    'import kr.moonseungjun.livingkingdoms.world.ErdenUrbanAuthoredInteriorPreserver;\n',
    '        ErdenUrbanAuthoredInteriorPreserver.captureBeforeConversion(event);\n',
    '        ErdenUrbanAuthoredInteriorPreserver.restoreAfterConversion(event);\n',
):
    if old in s:
        s = s.replace(old, '', 1)
        removed += 1
if 'ErdenUrbanAuthoredInteriorPreserver' in s:
    raise SystemExit('Obsolete authored-interior preserver reference remains in LivingKingdoms.java')
path.write_text(s, encoding='utf-8')
print(f'Removed obsolete ErdenUrbanAuthoredInteriorPreserver runtime hooks count={removed}')
