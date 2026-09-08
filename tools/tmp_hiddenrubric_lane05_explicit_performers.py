#!/usr/bin/env python3
from __future__ import annotations
import json,re
from pathlib import Path
import pyarrow.parquet as pq
from huggingface_hub import hf_hub_download

REPO='lmarena-ai/arena-human-preference-140k'; REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
SHARDS=[f'data/train-{i:05d}-of-00007.parquet' for i in range(7)]
OUT=Path('tmp/hiddenrubric-lane05-explicit-performers')
ROLES=[
'employees','workers','staff members','staff','technicians','operators','contractors','installers','engineers','developers','software developers','teachers','instructors','students','trainees','participants','volunteers','nurses','doctors','physicians','clinicians','therapists','pharmacists','surgeons','dentists','caregivers','researchers','scientists','analysts','data analysts','data scientists','accountants','lawyers','attorneys','paralegals','auditors','inspectors','managers','supervisors','project managers','product managers','administrators','system administrators','network administrators','drivers','pilots','crew members','mechanics','electricians','plumbers','construction workers','warehouse workers','factory workers','sales representatives','support agents','customer service representatives','customers','users','end users','applicants','job candidates','recruiters','interviewers','authors','writers','editors','presenters','speakers','coaches','athletes','parents','children','kids','our team','my team','our staff','our employees','our developers','our engineers','our technicians','our nurses','our teachers'
]
ALT='|'.join(sorted(map(re.escape,ROLES),key=len,reverse=True))
ACTION=r'(?:complete|submit|follow|perform|conduct|administer|install|configure|deploy|test|operate|collect|record|review|inspect|measure|monitor|maintain|repair|prepare|document|assess|calculate|teach|deliver|facilitate|drive|diagnose|treat|prescribe|verify|validate|publish|present|train|supervise|assemble|pack|ship|clean|scan|sign|enter|upload|download|backup|restore|migrate|edit|approve|report|check|create|write|fill out|use|run|execute|implement|manage|handle)'
PATS=[
 re.compile(rf'\b(?:checklist|procedure|instructions|steps|workflow|protocol|SOP|guide|manual|plan)\b.{{0,100}}?\bfor\s+(?:the\s+|our\s+|my\s+)?(?P<a>{ALT})\b\s+to\s+{ACTION}\b',re.I|re.S),
 re.compile(rf'\b(?:what|how)\s+(?:exactly\s+)?(?:should|must|does|do|can)\s+(?:the\s+|our\s+|my\s+)?(?P<a>{ALT})\b\s+(?:do|{ACTION})\b',re.I),
 re.compile(rf'\b(?P<a>{ALT})\b\s+(?:must|should|need to|needs to|are required to|is required to|are expected to|is expected to|are responsible for|is responsible for)\s+{ACTION}\b',re.I),
 re.compile(rf'\b(?:create|write|draft|design|develop|prepare|give|provide)\b.{{0,60}}?\b(?:checklist|procedure|instructions|steps|workflow|protocol|SOP|guide|manual|plan)\b.{{0,100}}?\b(?:for|used by|followed by)\s+(?:the\s+|our\s+|my\s+)?(?P<a>{ALT})\b',re.I|re.S),
]
PII=[re.compile(r'\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b',re.I),re.compile(r'\b(?:sk-[A-Za-z0-9_-]{16,}|api[_-]?key\s*[:=]\s*[A-Za-z0-9_-]{16,})',re.I)]
# Conservative higher-lane signals; final manual review remains authoritative.
STATE=re.compile(r'\b(?:instead|rather than|change|replace|revise|update|scratch that|forget that|previous|earlier|above|keep the|preserve|retain|no longer|from now on|going forward|ignore that|remove that|delete that|another one|try again|you(?:\'re| are) wrong)\b',re.I)
PREF=re.compile(r'\b(?:i prefer|we prefer|i like|we like|my favorite|our favorite|i would rather|we would rather)\b',re.I)
REQEVAL=re.compile(r'\b(?:my (?:boss|client|manager|professor|teacher|supervisor) (?:asked|wants|needs)|reviewer|evaluator|assessor|judge|grading committee|selection committee|hiring committee|evaluating committee)\b',re.I)

def text_of(m):
 if not isinstance(m,dict): return ''
 if isinstance(m.get('text'),str): return m['text']
 c=m.get('content')
 if isinstance(c,str): return c
 if isinstance(c,list): return '\n'.join(x if isinstance(x,str) else x.get('text','') if isinstance(x,dict) else '' for x in c).strip()
 return ''
def turns(row):
 out=[]
 for e in row.get('full_conversation') or []:
  if not isinstance(e,dict): continue
  u=e.get('user')
  if isinstance(u,dict):
   t=text_of(u).strip()
   if t: out.append(t)
  elif e.get('role')=='user':
   t=text_of(e).strip()
   if t: out.append(t)
 return out

def main():
 old=set()
 for p in Path('tmp/hiddenrubric-lane05-final/data/staging/router-stage05/parallel-v1/lane05-audience-performer').glob('lane05-audience-performer-*.jsonl'):
  for line in p.read_text(encoding='utf-8').splitlines():
   if line.strip():
    r=json.loads(line); old.add(str(r['source']['familyId']))
 paths=[hf_hub_download(REPO,filename=s,repo_type='dataset',revision=REV) for s in SHARDS]
 cols=['id','evaluation_session_id','evaluation_order','language','is_code','full_conversation']
 sessions={}
 for path in paths:
  pf=pq.ParquetFile(path); use=[c for c in cols if c in pf.schema_arrow.names]
  for b in pf.iter_batches(batch_size=512,columns=use):
   for r in b.to_pylist():
    if r.get('language')!='en' or r.get('is_code'): continue
    ts=turns(r)
    if not ts: continue
    sid=str(r.get('evaluation_session_id') or r.get('id') or '')
    # Prefer largest evaluation order / fullest history as canonical.
    key=(int(r.get('evaluation_order') or 0),len(ts))
    if sid not in sessions or key>sessions[sid][0]: sessions[sid]=(key,r,ts)
 rows=[]; seen=set()
 for sid,(_,r,ts) in sessions.items():
  fam='session:'+sid
  if fam in old or len(ts)!=1: continue
  text=ts[0]
  if not 30<=len(text)<=1800 or any(p.search(text) for p in PII): continue
  if STATE.search(text) or PREF.search(text) or REQEVAL.search(text): continue
  for i,p in enumerate(PATS,1):
   for m in p.finditer(text):
    span=m.group('a'); k=(str(r.get('id') or ''),span.lower())
    if k in seen: continue
    seen.add(k)
    rows.append({'sourceRowId':str(r.get('id') or ''),'targetTurnIndex':0,'sessionId':sid,'familyId':fam,'sourceText':text,'userTurns':ts,'sourceExactSpan':span,'patternId':f'explicit-{i}','trainingEligible':False})
    break
   else: continue
   break
 rows.sort(key=lambda x:(len(x['sourceText']),x['sourceRowId']))
 OUT.mkdir(parents=True,exist_ok=True)
 for p in OUT.glob('*.jsonl'): p.unlink()
 for i in range(0,len(rows),5):
  (OUT/f'performer-{i//5:03d}.jsonl').write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in rows[i:i+5]),encoding='utf-8')
 print(json.dumps({'explicitPerformerCandidates':len(rows),'files':(len(rows)+4)//5}))
if __name__=='__main__': main()
