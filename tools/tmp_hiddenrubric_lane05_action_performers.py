#!/usr/bin/env python3
from __future__ import annotations
import json,re
from pathlib import Path
import pyarrow.parquet as pq
from huggingface_hub import hf_hub_download
REPO='lmarena-ai/arena-human-preference-140k'; REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
SHARDS=[f'data/train-{i:05d}-of-00007.parquet' for i in range(7)]
OUT=Path('tmp/hiddenrubric-lane05-action-performers')
ROLES=['students','participants','employees','workers','staff','technicians','operators','contractors','engineers','developers','teachers','trainees','volunteers','nurses','doctors','clinicians','researchers','users','customers','applicants','candidates','drivers','pilots','athletes','parents','children','kids','team members','our team','my team']
ALT='|'.join(sorted(map(re.escape,ROLES),key=len,reverse=True))
ACT=r'(?:complete|submit|present|perform|conduct|practice|write|create|build|prepare|record|collect|measure|inspect|review|test|install|configure|deploy|use|run|execute|implement|follow|answer|solve|discuss|work|participate|bring|deliver|give|take|fill|upload|download|report|check|manage|operate|repair|train|teach|demonstrate|evaluate|improve|share|choose|select|make|do|read|watch|listen|speak)'
PATS=[re.compile(rf'\b(?P<a>{ALT})\b\s+(?:will|must|should|need to|are required to|are expected to|have to)\s+{ACT}\b',re.I),re.compile(rf'\b(?:each|every)\s+(?P<a>{ALT})\b\s+(?:will|must|should|needs to|has to)\s+{ACT}\b',re.I)]
REQ=re.compile(r'\b(?:write|create|design|develop|prepare|draft|generate|make|give|provide|plan|lesson|activity|exercise|assignment|workshop|training|procedure|workflow|instructions|schedule|session|game|quiz|course|program|programme)\b',re.I)
STATE=re.compile(r'\b(?:instead|rather than|change|replace|revise|update|previous|earlier|above|from now on|going forward|another one|try again|you(?:\'re| are) wrong)\b',re.I)
PREF=re.compile(r'\b(?:i prefer|we prefer|i like|we like|my favorite|our favorite|i would rather|we would rather)\b',re.I)
REQE=re.compile(r'\b(?:my (?:boss|client|manager|professor|teacher|supervisor) (?:asked|wants|needs)|reviewer|evaluator|assessor|judge|grading committee|selection committee|hiring committee|evaluating committee)\b',re.I)
PII=re.compile(r'\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b',re.I)
def text_of(m):
 if not isinstance(m,dict): return ''
 if isinstance(m.get('text'),str): return m['text']
 c=m.get('content')
 if isinstance(c,str): return c
 if isinstance(c,list): return '\n'.join(x if isinstance(x,str) else x.get('text','') if isinstance(x,dict) else '' for x in c).strip()
 return ''
def uts(r):
 o=[]
 for e in r.get('full_conversation') or []:
  if not isinstance(e,dict): continue
  u=e.get('user')
  if isinstance(u,dict):
   t=text_of(u).strip()
   if t:o.append(t)
  elif e.get('role')=='user':
   t=text_of(e).strip()
   if t:o.append(t)
 return o
def main():
 old=set()
 for p in Path('tmp/hiddenrubric-lane05-final/data/staging/router-stage05/parallel-v1/lane05-audience-performer').glob('lane05-audience-performer-*.jsonl'):
  for l in p.read_text(encoding='utf-8').splitlines():
   if l.strip():old.add(json.loads(l)['source']['familyId'])
 sessions={}
 for sh in SHARDS:
  path=hf_hub_download(REPO,filename=sh,repo_type='dataset',revision=REV); pf=pq.ParquetFile(path)
  cols=[c for c in ['id','evaluation_session_id','evaluation_order','language','is_code','full_conversation'] if c in pf.schema_arrow.names]
  for b in pf.iter_batches(batch_size=512,columns=cols):
   for r in b.to_pylist():
    if r.get('language')!='en' or r.get('is_code'):continue
    t=uts(r)
    if not t:continue
    sid=str(r.get('evaluation_session_id') or r.get('id') or ''); k=(int(r.get('evaluation_order') or 0),len(t))
    if sid not in sessions or k>sessions[sid][0]:sessions[sid]=(k,r,t)
 out=[]
 for sid,(_,r,ts) in sessions.items():
  fam='session:'+sid
  if fam in old or len(ts)!=1:continue
  text=ts[0]
  if not 35<=len(text)<=1600 or PII.search(text) or STATE.search(text) or PREF.search(text) or REQE.search(text) or not REQ.search(text):continue
  for i,p in enumerate(PATS,1):
   m=p.search(text)
   if m:
    out.append({'sourceRowId':str(r.get('id') or ''),'targetTurnIndex':0,'sessionId':sid,'familyId':fam,'sourceText':text,'userTurns':ts,'sourceExactSpan':m.group('a'),'patternId':f'action-{i}','trainingEligible':False});break
 out.sort(key=lambda x:(len(x['sourceText']),x['sourceRowId']))
 OUT.mkdir(parents=True,exist_ok=True)
 for p in OUT.glob('*.jsonl'):p.unlink()
 for i in range(0,len(out),5):(OUT/f'performer-{i//5:03d}.jsonl').write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in out[i:i+5]),encoding='utf-8')
 print(json.dumps({'actionPerformerCandidates':len(out),'files':(len(out)+4)//5}))
if __name__=='__main__':main()
