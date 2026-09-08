#!/usr/bin/env python3
from __future__ import annotations
import json,re
from pathlib import Path
SRC=Path('tmp/hiddenrubric-lane05-replacements'); OUT=Path('tmp/hiddenrubric-lane05-strong-replacements')
OUT.mkdir(parents=True,exist_ok=True)
for p in OUT.glob('*.jsonl'): p.unlink()
seen=set(); rows=[]
for p in sorted(SRC.glob('performer-*.jsonl')):
  for line in p.read_text(encoding='utf-8').splitlines():
    if not line.strip(): continue
    r=json.loads(line); k=(r['sourceRowId'],r['targetTurnIndex'])
    if k in seen: continue
    seen.add(k)
    t=r['sourceText']; pats=[x.get('patternId','') for x in r.get('roles',{}).get('performer',[])]
    strong=(any(x.startswith(('performer-2','performer-3','performer-4','performer-5')) for x in pats) or
      bool(re.search(r"\b(?:I|i|we|We)\s+(?:need to|have to|has to|will|must|am going to|are going to|plan to|intend to)\b",t)))
    if not strong: continue
    if r.get('turnCount')!=1: continue
    if len(t)>1200: continue
    rows.append(r)
rows.sort(key=lambda r:(len(r['sourceText']),r['sourceRowId']))
for i in range(0,len(rows),5):
  (OUT/f'performer-{i//5:03d}.jsonl').write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in rows[i:i+5]),encoding='utf-8')
print('strong performer',len(rows))
