from pathlib import Path
import subprocess
import time

p=Path('projects/arcane-circle/tools/test_current_source.py')
s=p.read_text(encoding='utf-8')

old='''              'followsCaster','if("time_stop".equals(spell.id()))return false;',\n              'if("antimagic_field".equals(spell.id()))return true;','findPlayer(UUID id)',\n'''
new='''              'followsCaster','if("time_stop".equals(spell.id()))return false;',\n              'if("antimagic_field".equals(spell.id())||"control_weather".equals(spell.id()))return true;','findLiving(UUID id)',\n'''
if s.count(old)!=1:
    raise SystemExit(f'alpha44 tracker token block: expected 1, found {s.count(old)}')
s=s.replace(old,new,1)

old='''for attached_id in ['shield','mage_armor','haste','greater_invisibility','true_seeing','solar_guard','shapechange','foresight','fly']:\n    assert f'"{attached_id}"' in tracker, attached_id\n'''
new='''for attached_id in ['shield','mage_armor','haste','greater_invisibility','true_seeing','solar_guard','shapechange','foresight','fly']:\n    authored_body=f'put("{attached_id}", SigilStyle.BODY_HALO'\n    authored_feet=f'put("{attached_id}", SigilStyle.FEET_RUNE'\n    assert authored_body in presentation or authored_feet in presentation, attached_id\n'''
if s.count(old)!=1:
    raise SystemExit(f'alpha44 attached-id block: expected 1, found {s.count(old)}')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')

self_path=Path('.github/scripts/fix-arcane-audit45.py')
self_path.unlink()
subprocess.run(['git','config','user.name','github-actions[bot]'],check=True)
subprocess.run(['git','config','user.email','41898282+github-actions[bot]@users.noreply.github.com'],check=True)
subprocess.run(['git','add','projects/arcane-circle/tools/test_current_source.py',str(self_path)],check=True)
subprocess.run(['git','diff','--cached','--check'],check=True)
subprocess.run(['git','commit','-m','test(arcane): align alpha.45 lifecycle audit with generic attachment'],check=True)
for attempt in range(1,7):
    if subprocess.run(['git','push','origin','HEAD:main']).returncode==0:
        break
    subprocess.run(['git','pull','--rebase','origin','main'],check=True)
    time.sleep(attempt*2)
else:
    raise SystemExit('failed to push alpha.45 audit correction')
