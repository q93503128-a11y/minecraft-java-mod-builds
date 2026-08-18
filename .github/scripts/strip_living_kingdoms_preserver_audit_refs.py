from pathlib import Path

residence = Path('.github/workflows/audit-living-kingdoms-residence-modes.yml')
s = residence.read_text(encoding='utf-8')

for old in (
    "          grep -F 'ErdenUrbanResidenceResolver.isResidenceReady(level, entrance)' \"$root/ErdenUrbanAuthoredInteriorPreserver.java\"\n",
    "          grep -F 'LK_ERDEN_AUTHORED_INTERIOR_RESTORE_SAMPLE_PASS' \"$root/ErdenUrbanAuthoredInteriorPreserver.java\"\n",
    "              && grep -Fq 'LK_ERDEN_AUTHORED_INTERIOR_RESTORE_SAMPLE_PASS' logs/residence-server.log \\\n",
    "          grep -F 'LK_ERDEN_AUTHORED_INTERIOR_RESTORE_SAMPLE_PASS' logs/residence-server.log | grep -F 'role_aware_navigation=true' | grep -F 'rollback_count=0'\n",
):
    s = s.replace(old, '')

old_block = '''          if grep -Fq 'upper.isUpperFloorComplete' "$root/ErdenUrbanAuthoredInteriorPreserver.java"; then
            echo 'Interior preservation still depends on legacy synthetic upper completion.' >&2
            exit 1
          fi
'''
s = s.replace(old_block, '')

s = s.replace(
    "|LK_ERDEN_AUTHORED_INTERIOR_RESTORE_SAMPLE_PASS",
    "")
s = s.replace(
    " loaded_restore_sample=true loaded_restore_rollbacks=0",
    " legacy_preserver=false loaded_restore_rollbacks=0")

if 'ErdenUrbanAuthoredInteriorPreserver.java' in s:
    raise SystemExit('Residence audit still references deleted preserver source')
if "grep -F 'LK_ERDEN_AUTHORED_INTERIOR_RESTORE_SAMPLE_PASS'" in s:
    raise SystemExit('Residence audit still requires deleted preserver runtime marker')
residence.write_text(s, encoding='utf-8')

build = Path('.github/workflows/build-living-kingdoms.yml')
b = build.read_text(encoding='utf-8')
needle = "          if grep -Eq 'woodland_mansion|deepslate_mega_base|(^|/)(tools|\\.github)/|\\.java$' logs/jar-entries.txt; then\n            exit 1\n          fi\n"
replacement = "          if grep -Fxq 'kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredInteriorPreserver.class' logs/jar-entries.txt; then\n            echo 'Obsolete ErdenUrbanAuthoredInteriorPreserver was packaged.' >&2\n            exit 1\n          fi\n" + needle
if replacement not in b:
    if needle not in b:
        raise SystemExit('Canonical JAR negative guard insertion point not found')
    b = b.replace(needle, replacement, 1)
build.write_text(b, encoding='utf-8')

print('Removed legacy preserver assumptions from Living Kingdoms audits')
