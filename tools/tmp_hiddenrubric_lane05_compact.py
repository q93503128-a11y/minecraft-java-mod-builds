#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path
SRC=Path('tmp/hiddenrubric-lane05-candidates')
OUT=Path('tmp/hiddenrubric-lane05-compact')
OUT.mkdir(parents=True,exist_ok=True)
for p in OUT.glob('*.jsonl'): p.unlink()
for role in ('audience','performer'):
    rows=[]
    for p in sorted(SRC.glob(f'{role}-*.jsonl')):
        for line in p.read_text(encoding='utf-8').splitlines():
            if not line.strip(): continue
            r=json.loads(line)
            rows.append({
                'sourceRowId':r['sourceRowId'],
                'sessionId':r['sourceEvaluationSessionId'],
                'familyId':r['familyId'],
                'targetTurnIndex':r['targetTurnIndex'],
                'turnCount':len(r.get('userTurns') or []),
                'sourceText':r['sourceText'],
                'actors':[a for a in r.get('heuristicActors',[]) if a.get('role') in ('audience','performer')],
                'domainSignals':(r.get('higherLaneHeuristicFlags') or {}).get('domainSignals',[]),
            })
    for i in range(0,len(rows),5):
        q=OUT/f'{role}-{i//5:03d}.jsonl'
        q.write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in rows[i:i+5]),encoding='utf-8')
print(json.dumps({'audience':sum(1 for p in SRC.glob('audience-*.jsonl') for l in p.read_text(encoding='utf-8').splitlines() if l.strip()),'performer':sum(1 for p in SRC.glob('performer-*.jsonl') for l in p.read_text(encoding='utf-8').splitlines() if l.strip())}))
