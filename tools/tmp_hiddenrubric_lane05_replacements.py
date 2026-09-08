#!/usr/bin/env python3
from __future__ import annotations
import json, subprocess
from pathlib import Path

CUR=Path('tmp/hiddenrubric-lane05-candidates')
FINAL=Path('tmp/hiddenrubric-lane05-final/data/staging/router-stage05/parallel-v1/lane05-audience-performer')
OUT=Path('tmp/hiddenrubric-lane05-replacements')
OLD='6cf80e5d7896d4cdb2fb632a3227be68f3006995'

selected=set()
for p in sorted(FINAL.glob('lane05-audience-performer-*.jsonl')):
    for line in p.read_text(encoding='utf-8').splitlines():
        if not line.strip(): continue
        r=json.loads(line); selected.add((str(r['source']['sourceRowId']),int(r['source']['targetTurnIndex'])))

def load_current():
    out=[]
    for p in sorted(CUR.glob('*.jsonl')):
        for line in p.read_text(encoding='utf-8').splitlines():
            if line.strip(): out.append(json.loads(line))
    return out

def load_old_aud():
    out=[]
    names=subprocess.check_output(['git','ls-tree','-r','--name-only',OLD,'tmp/hiddenrubric-lane05-candidates'],text=True).splitlines()
    for name in names:
        base=name.rsplit('/',1)[-1]
        if not (base.startswith('audience-') and base.endswith('.jsonl')): continue
        txt=subprocess.check_output(['git','show',f'{OLD}:{name}'],text=True)
        for line in txt.splitlines():
            if line.strip(): out.append(json.loads(line))
    return out

by={}
for r in load_old_aud()+load_current():
    key=(str(r['sourceRowId']),int(r['targetTurnIndex']))
    if key in selected: continue
    rec=by.setdefault(key,{
        'sourceRowId':key[0], 'targetTurnIndex':key[1],
        'sessionId':str(r['sourceEvaluationSessionId']), 'familyId':str(r['familyId']),
        'sourceText':r['sourceText'], 'turnCount':len(r.get('userTurns') or []),
        'userTurns':r.get('userTurns') or [], 'roles':{},
        'higherLaneHeuristicFlags':r.get('higherLaneHeuristicFlags') or {},
    })
    for a in r.get('heuristicActors') or []:
        role=a.get('role')
        if role in {'audience','performer'}:
            rec['roles'].setdefault(role,[]).append({'span':a.get('sourceExactSpan'),'patternId':a.get('patternId')})

rows=list(by.values())
# Prefer concise, single-turn material. Dedupe actor spans per role.
for r in rows:
    for role, arr in list(r['roles'].items()):
        seen=set(); clean=[]
        for a in arr:
            k=(a.get('span'),a.get('patternId'))
            if k not in seen: seen.add(k); clean.append(a)
        r['roles'][role]=clean
rows.sort(key=lambda r:(r['turnCount']!=1,len(r['sourceText']),r['sourceRowId'],r['targetTurnIndex']))

OUT.mkdir(parents=True,exist_ok=True)
for p in OUT.glob('*.jsonl'): p.unlink()
for role in ('audience','performer'):
    rr=[r for r in rows if role in r['roles']]
    for i in range(0,len(rr),5):
        p=OUT/f'{role}-{i//5:03d}.jsonl'
        p.write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in rr[i:i+5]),encoding='utf-8')
    print(role,len(rr))
