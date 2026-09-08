#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, re
from collections import Counter
from pathlib import Path
import pyarrow.parquet as pq
from huggingface_hub import hf_hub_download

REPO='lmarena-ai/arena-human-preference-140k'
REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
SHARDS=[f'data/train-{i:05d}-of-00007.parquet' for i in range(7)]
EXPECTED=135634
OUT=Path('tmp/hiddenrubric-lane05-candidates')

PII=[re.compile(r'\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b',re.I),re.compile(r'(?<!\d)(?:\+?\d[\d .()\-]{7,}\d)(?!\d)'),re.compile(r'\b(?:sk-[A-Za-z0-9_-]{16,}|api[_-]?key\s*[:=]\s*[\'\"]?[A-Za-z0-9_-]{16,})',re.I)]
STATE=re.compile(r'\b(?:actually|instead|rather than|change|replace|revise|update|scratch that|forget that|previous|earlier|above|keep the|preserve|retain|don\'t change|do not change|no longer)\b',re.I)
PREF=re.compile(r'\b(?:i prefer|we prefer|i like|we like|i love|we love|my favorite|our favorite|preference is)\b',re.I)
REQUESTER=re.compile(r'\b(?:i am|i\'m|we are|we\'re)\s+(?:a|an|the)\s+[a-z][a-z -]{1,40}\b|\bas\s+(?:a|an|the)\s+[a-z][a-z -]{1,40},',re.I)
EVALUATOR=re.compile(r'\b(?:reviewer|evaluator|assessor|judge|grading committee|selection committee|hiring committee)\b',re.I)
DOMAIN_PATTERNS={
'legal':re.compile(r'\b(?:legal|law|lawyer|contract|compliance|court|regulation)\b',re.I),
'finance':re.compile(r'\b(?:finance|financial|accounting|tax|investment|portfolio|budget)\b',re.I),
'health':re.compile(r'\b(?:medical|health|clinical|doctor|nurse|patient|hospital|surgery|therapy)\b',re.I),
'software':re.compile(r'\b(?:software|developer|programming|code|api|database|cloud|devops|javascript|python)\b',re.I),
'marketing':re.compile(r'\b(?:marketing|seo|advertising|campaign|sales|crm|social media)\b',re.I),
'education':re.compile(r'\b(?:education|teacher|student|classroom|lesson|school|curriculum)\b',re.I),
'science':re.compile(r'\b(?:physics|chemistry|biology|astronomy|scientific|research experiment)\b',re.I),
}
TERMS=['preschool children','preschoolers','kindergarten students','kindergarteners','elementary school students','primary school students','middle school students','high school students','college students','university students','graduate students','young children','children','kids','teenagers','students','teachers','educators','parents','families','customers','clients','patients','nurses','doctors','physicians','investors','executives','board members','managers','employees','staff members','staff','software developers','developers','engineers','designers','researchers','scientists','general public','non-technical readers','nontechnical readers','technical readers','beginners','experts','readers','viewers','listeners','audience','end users','users','website visitors','subscribers','donors','voters','tourists','travelers','job candidates','applicants','recruiters','stakeholders','shareholders','small business owners','business owners','entrepreneurs','homeowners','tenants','residents','volunteers','participants','attendees','workshop participants','trainees','new hires','team members','sales representatives','customer service representatives','technicians','operators','administrators','analysts','drivers','mechanics','cashiers','caregivers','coaches','athletes']
ALT='|'.join(sorted((re.escape(x) for x in TERMS),key=len,reverse=True))
AUD=[
re.compile(rf'\b(?:for|aimed at|targeted at|targeting|intended for|written for|designed for|suitable for)\s+(?:the\s+|my\s+|our\s+|a\s+|an\s+)?(?P<a>{ALT})\b',re.I),
re.compile(rf'\b(?:explain|describe|present|communicate|teach|introduce|summarize)\b.{{0,70}}?\bto\s+(?:the\s+|my\s+|our\s+)?(?P<a>{ALT})\b',re.I|re.S),
re.compile(rf'\b(?:email|letter|message|speech|presentation|guide|article|post|lesson|tutorial|report|summary|copy|content|instructions)\b.{{0,60}}?\b(?:to|for)\s+(?:the\s+|my\s+|our\s+)?(?P<a>{ALT})\b',re.I|re.S),
]
ACTION=r'perform|implement|run|execute|carry out|conduct|use|operate|complete|follow|administer|teach|deliver|install|configure|deploy|test|fill out|enter|collect|record|upload|download|build|create|manage|handle|process|monitor|inspect|measure|assemble|practice|submit|apply|review|approve|maintain|troubleshoot|respond|facilitate|moderate|schedule|document|prepare|drive|repair|check|assess|calculate|write|record|report|serve|provide|take|make|set up|open|close'
PERF=[
re.compile(rf'\b(?P<a>{ALT})\b\s+(?:will|would|should|must|need to|needs to|are going to|can|have to|has to)\s+(?:\w+\s+){{0,4}}?(?:{ACTION})\b',re.I),
re.compile(rf'\b(?:steps|instructions|procedure|workflow|checklist|process|activity|protocol|plan)\b.{{0,80}}?\bfor\s+(?P<a>{ALT})\b\s+to\s+(?:\w+\s+){{0,4}}?(?:{ACTION})\b',re.I|re.S),
re.compile(rf'\b(?:used|performed|implemented|executed|run|operated|completed|followed|administered|conducted|handled|managed)\s+by\s+(?:the\s+|my\s+|our\s+)?(?P<a>{ALT})\b',re.I),
re.compile(rf'\b(?P<a>{ALT})\b\s+(?:are|is)\s+(?:responsible for|expected to|required to)\s+(?:\w+\s+){{0,4}}?(?:{ACTION})\b',re.I),
]

def text_of(m):
    if not isinstance(m,dict): return ''
    if isinstance(m.get('text'),str): return m['text']
    c=m.get('content')
    if isinstance(c,str): return c
    if isinstance(c,list):
        return '\n'.join(x if isinstance(x,str) else x.get('text','') if isinstance(x,dict) and isinstance(x.get('text'),str) else '' for x in c).strip()
    return ''

def user_turns(row):
    out=[]
    for e in row.get('full_conversation') or []:
        if not isinstance(e,dict): continue
        u=e.get('user')
        if isinstance(u,dict):
            t=text_of(u).strip()
            if t: out.append(t)
        elif str(e.get('role') or '')=='user':
            t=text_of(e).strip()
            if t: out.append(t)
    return out

def matches(text, pats, role):
    out=[]; seen=set()
    for i,p in enumerate(pats,1):
        for m in p.finditer(text):
            s,e=m.span('a'); exact=text[s:e]; k=exact.lower()
            if k in seen: continue
            seen.add(k); out.append({'role':role,'sourceExactSpan':exact,'patternId':f'{role}-{i}'})
    return out

def rowkey(r,turns):
    try: ts=int(r.get('timestamp').timestamp()*1e9) if hasattr(r.get('timestamp'),'timestamp') else int(r.get('timestamp') or 0)
    except Exception: ts=0
    return (int(r.get('evaluation_order') or 0),len(turns),ts)

def iter_rows(paths):
    cols=['id','evaluation_session_id','evaluation_order','language','is_code','timestamp','full_conversation']
    for path in paths:
        pf=pq.ParquetFile(path); use=[c for c in cols if c in set(pf.schema_arrow.names)]
        for b in pf.iter_batches(batch_size=512,columns=use): yield from b.to_pylist()

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    paths=[hf_hub_download(REPO,filename=s,repo_type='dataset',revision=REV) for s in SHARDS]
    sessions={}; raw=0
    for r in iter_rows(paths):
        raw+=1
        if r.get('language')!='en' or bool(r.get('is_code')): continue
        ts=user_turns(r)
        if not ts: continue
        sid=str(r.get('evaluation_session_id') or r.get('id') or '')
        k=rowkey(r,ts)
        if sid not in sessions or k>sessions[sid][0]: sessions[sid]=(k,r,ts)
    if raw!=EXPECTED: raise RuntimeError(f'raw rows {raw} != {EXPECTED}')
    pools={'audience':[],'performer':[]}; rejection=Counter(); normseen=set()
    for sid,(_,r,turns) in sessions.items():
        if len(turns)!=1: rejection['multiTurn']+=1; continue
        t=turns[0].strip()
        if not (24<=len(t)<=3500): rejection['length']+=1; continue
        if any(p.search(t) for p in PII): rejection['privacy']+=1; continue
        domains=[k for k,p in DOMAIN_PATTERNS.items() if p.search(t)]
        h={'stateOps':bool(STATE.search(t)),'preference':bool(PREF.search(t)),'crossDomain':len(domains)>=2,'requesterEvaluator':bool(REQUESTER.search(t) or EVALUATOR.search(t)),'domainSignals':domains}
        if any(h[k] for k in ('stateOps','preference','crossDomain','requesterEvaluator')): rejection['higherLaneHeuristic']+=1; continue
        am=matches(t,AUD,'audience'); pm=matches(t,PERF,'performer')
        if not am and not pm: continue
        nh=hashlib.sha256(re.sub(r'\s+',' ',t).strip().lower().encode()).hexdigest()
        if nh in normseen: rejection['duplicate']+=1; continue
        normseen.add(nh)
        base={'schemaVersion':'router-stage05-parallel-v1-lane05-candidate-v1','sourceDataset':REPO,'sourceRevision':REV,'sourceRowId':str(r.get('id') or ''),'sourceEvaluationSessionId':sid,'evaluationOrder':int(r.get('evaluation_order') or 0),'familyId':f'session:{sid}','familyBasis':'evaluation_session_id','userTurns':turns,'targetTurnIndex':0,'sourceText':t,'privacyFlags':[],'higherLaneHeuristicFlags':h,'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'trainingEligible':False}
        if am:
            x=dict(base); x['heuristicActors']=am+pm; pools['audience'].append(x)
        if pm:
            x=dict(base); x['heuristicActors']=am+pm; pools['performer'].append(x)
    manifest={'schemaVersion':'router-stage05-parallel-v1-lane05-candidate-manifest-v1','sourceDataset':REPO,'sourceRevision':REV,'rawRowsRead':raw,'canonicalSessions':len(sessions),'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'trainingEligible':False,'rejections':dict(rejection),'pools':{}}
    for role,items in pools.items():
        items.sort(key=lambda x:(len(x['sourceText']),x['sourceEvaluationSessionId']))
        items=items[:240]
        files=[]
        for start in range(0,len(items),15):
            p=OUT/f'{role}-{start//15:03d}.jsonl'; rows=items[start:start+15]
            p.write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in rows),encoding='utf-8')
            files.append({'path':str(p),'rows':len(rows),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
        manifest['pools'][role]={'rows':len(items),'files':files}
    mp=OUT/'candidate-manifest.json'; mp.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(manifest,ensure_ascii=False,indent=2))
if __name__=='__main__': main()
