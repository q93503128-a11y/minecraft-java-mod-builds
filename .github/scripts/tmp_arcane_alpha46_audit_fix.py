#!/usr/bin/env python3
from pathlib import Path
import subprocess, sys, time
p=Path('projects/arcane-circle/tools/test_current_source.py')
s=p.read_text(encoding='utf-8')
old='\'case "prismatic_wall"\',\'14초 지속\''
new='\'case "prismatic_wall"\',\'20초 지속\''
if s.count(old)!=1:
    raise SystemExit(f'expected one old prismatic summary audit token, got {s.count(old)}')
p.write_text(s.replace(old,new,1),encoding='utf-8')
self_path=Path('.github/scripts/tmp_arcane_alpha46_audit_fix.py')
self_path.unlink()
subprocess.run(['git','config','user.name','github-actions[bot]'],check=True)
subprocess.run(['git','config','user.email','41898282+github-actions[bot]@users.noreply.github.com'],check=True)
subprocess.run(['git','add','-A','projects/arcane-circle/tools/test_current_source.py',str(self_path)],check=True)
subprocess.run(['git','diff','--cached','--check'],check=True)
subprocess.run(['git','commit','-m','test(arcane-circle): align alpha.46 wall duration audit'],check=True)
for attempt in range(1,7):
    if subprocess.run(['git','push','origin','HEAD:main']).returncode==0: sys.exit(0)
    subprocess.run(['git','fetch','origin','main'],check=True); subprocess.run(['git','rebase','origin/main'],check=True); time.sleep(attempt*2)
raise SystemExit('failed to push alpha.46 audit parity correction')
