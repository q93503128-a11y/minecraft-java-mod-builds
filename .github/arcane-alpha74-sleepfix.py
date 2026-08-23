from pathlib import Path
p=Path('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.java')
s=p.read_text(encoding='utf-8')
old='''        SLEEP.put(target.getUUID(), state);\n        enforceSleep(target);'''
new='''        SLEEP.put(target.getUUID(), state);\n        enforceSleep(target);\n        if (previous != null && !previous.ownerId.equals(ownerId))\n            cancelSleepReleaseIfIdle(previous.level, previous.ownerId);'''
if s.count(old)!=1: raise SystemExit(f'expected one sleep replacement, found {s.count(old)}')
p.write_text(s.replace(old,new,1),encoding='utf-8')

t=Path('projects/arcane-circle/tools/test_current_source.py')
s=t.read_text(encoding='utf-8')
old="     'cancelSleepReleaseIfIdle', 'resolveNpcWard(target, event)')"
new="     'cancelSleepReleaseIfIdle', 'previous != null && !previous.ownerId.equals(ownerId)', 'resolveNpcWard(target, event)')"
if s.count(old)!=1: raise SystemExit('source verifier anchor mismatch')
t.write_text(s.replace(old,new,1),encoding='utf-8')
