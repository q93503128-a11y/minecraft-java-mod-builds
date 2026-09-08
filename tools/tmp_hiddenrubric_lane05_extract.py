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

PII=[
    re.compile(r'\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b',re.I),
    re.compile(r'(?<!\d)(?:\+?\d[\d .()\-]{7,}\d)(?!\d)'),
    re.compile(r'\b(?:sk-[A-Za-z0-9_-]{16,}|api[_-]?key\s*[:=]\s*[\'\"]?[A-Za-z0-9_-]{16,})',re.I),
]
STATE=re.compile(r'\b(?:actually|instead|rather than|change|replace|revise|update|scratch that|forget that|previous|earlier|above|keep the|preserve|retain|don\'t change|do not change|no longer|from now on|going forward|ignore (?:that|the)|remove (?:that|the)|delete (?:that|the))\b',re.I)
PREF=re.compile(r'\b(?:i prefer|we prefer|i like|we like|i love|we love|my favorite|our favorite|preference is|i would rather|we would rather)\b',re.I)
REQUESTER=re.compile(r'\b(?:i am|i\'m|we are|we\'re)\s+(?:a|an|the)\s+[a-z][a-z -]{1,45}\b|\bas\s+(?:a|an|the)\s+[a-z][a-z -]{1,45},',re.I)
EVALUATOR=re.compile(r'\b(?:reviewer|evaluator|assessor|judge|grading committee|selection committee|hiring committee|admissions committee)\b',re.I)
DOMAIN_PATTERNS={
'legal':re.compile(r'\b(?:legal|law|lawyer|contract|compliance|court|regulation)\b',re.I),
'finance':re.compile(r'\b(?:finance|financial|accounting|tax|investment|portfolio|budget)\b',re.I),
'health':re.compile(r'\b(?:medical|health|clinical|doctor|nurse|patient|hospital|surgery|therapy)\b',re.I),
'software':re.compile(r'\b(?:software|developer|programming|code|api|database|cloud|devops|javascript|python)\b',re.I),
'marketing':re.compile(r'\b(?:marketing|seo|advertising|campaign|sales|crm|social media)\b',re.I),
'education':re.compile(r'\b(?:education|teacher|student|classroom|lesson|school|curriculum)\b',re.I),
'science':re.compile(r'\b(?:physics|chemistry|biology|astronomy|scientific|research experiment)\b',re.I),
}
TERMS=[
'preschool children','preschoolers','kindergarten students','kindergarteners','elementary school students','primary school students','middle school students','high school students','college students','university students','graduate students','young children','children','kids','teenagers','students','teachers','educators','parents','families','customers','clients','patients','nurses','doctors','physicians','investors','executives','board members','managers','employees','staff members','staff','software developers','developers','engineers','designers','researchers','scientists','general public','non-technical readers','nontechnical readers','technical readers','beginners','experts','readers','viewers','listeners','audience','end users','users','website visitors','subscribers','donors','voters','tourists','travelers','job candidates','applicants','recruiters','stakeholders','shareholders','small business owners','business owners','entrepreneurs','homeowners','tenants','residents','volunteers','participants','attendees','workshop participants','trainees','new hires','team members','sales representatives','customer service representatives','technicians','operators','administrators','analysts','drivers','mechanics','cashiers','caregivers','coaches','athletes','clinicians','pharmacists','therapists','surgeons','dentists','accountants','lawyers','attorneys','paralegals','consultants','auditors','inspectors','supervisors','project managers','product managers','data analysts','data scientists','security analysts','system administrators','network administrators','receptionists','servers','bartenders','cooks','chefs','warehouse workers','factory workers','construction workers','contractors','installers','electricians','plumbers','pilots','crew members','police officers','firefighters','paramedics','dispatchers','librarians','counselors','social workers','trainers','instructors','moderators','editors','writers','authors','photographers','videographers','presenters','speakers','interviewers','interviewees','survey respondents','reviewers','testers','qa engineers','support agents','agents','our team','my team','our staff','our developers','our engineers','our employees','our nurses','our teachers','our managers','our analysts','our technicians'
]
ALT='|'.join(sorted((re.escape(x) for x in TERMS),key=len,reverse=True))

AUD=[
    re.compile(rf'\b(?:explain|describe|present|communicate|teach|introduce|summarize)\b.{{0,90}}?\bto\s+(?:the\s+|my\s+|our\s+)?(?P<a>{ALT})\b',re.I|re.S),
    re.compile(rf'\b(?:email|letter|message|speech|presentation|guide|article|post|lesson|tutorial|report|summary|copy|content|instructions|plan|game|song|story|course|worksheet|quiz|handout|brochure|script|manual|training|tips|rules|activity|video|newsletter|proposal)\b.{{0,90}}?\b(?:to|for)\s+(?:the\s+|my\s+|our\s+|a\s+|an\s+)?(?P<a>{ALT})\b',re.I|re.S),
    re.compile(rf'\b(?:write|create|make|generate|design|prepare|draft|develop|produce|adapt|rewrite|simplify)\b.{{0,110}}?\bfor\s+(?:the\s+|my\s+|our\s+|a\s+|an\s+)?(?P<a>{ALT})\b',re.I|re.S),
    re.compile(rf'\b(?:aimed at|targeted at|targeting|intended for|written for|designed for|tailored to|geared toward|geared towards)\s+(?:the\s+|my\s+|our\s+|a\s+|an\s+)?(?P<a>{ALT})\b',re.I),
]

MODAL=r'(?:will|would|should|must|need to|needs to|have to|has to|are going to|is going to|are expected to|is expected to|are required to|is required to|can|may)'
PERF=[
    re.compile(rf'\b(?P<a>{ALT})\b\s+{MODAL}\s+[^\n.!?;:]{{1,120}}',re.I),
    re.compile(rf'\b(?:steps|instructions|procedure|workflow|checklist|process|activity|protocol|plan|guide|manual)\b.{{0,100}}?\bfor\s+(?:the\s+|my\s+|our\s+)?(?P<a>{ALT})\b\s+to\s+[^\n.!?;:]{{1,100}}',re.I|re.S),
    re.compile(rf'\b(?:used|performed|implemented|executed|run|operated|completed|followed|administered|conducted|handled|managed|reviewed|approved|tested|installed|configured|deployed|maintained|prepared|submitted|recorded|collected|measured|inspected|monitored)\s+by\s+(?:the\s+|my\s+|our\s+)?(?P<a>{ALT})\b',re.I),
    re.compile(rf'\b(?P<a>{ALT})\b\s+(?:are|is)\s+(?:responsible for|in charge of|tasked with)\s+[^\n.!?;:]{{1,120}}',re.I),
    re.compile(rf'\b(?P<a>{ALT})\b\s+(?:perform|performs|performed|implement|implements|implemented|execute|executes|executed|run|runs|conduct|conducts|conducted|administer|administers|administered|install|installs|installed|configure|configures|configured|deploy|deploys|deployed|test|tests|tested|complete|completes|completed|follow|follows|followed|operate|operates|operated|collect|collects|collected|record|records|recorded|submit|submits|submitted|review|reviews|reviewed|approve|approves|approved|inspect|inspects|inspected|measure|measures|measured|monitor|monitors|monitored|maintain|maintains|maintained|repair|repairs|repaired|prepare|prepares|prepared|document|documents|documented|assess|assesses|assessed|calculate|calculates|calculated|teach|teaches|taught|deliver|delivers|delivered|facilitate|facilitates|facilitated|moderate|moderates|moderated|drive|drives|drove|diagnose|diagnoses|diagnosed|treat|treats|treated|prescribe|prescribes|prescribed|sterilize|sterilizes|sterilized|verify|verifies|verified|validate|validates|validated|publish|publishes|published|present|presents|presented|interview|interviews|interviewed|grade|grades|graded|train|trains|trained|supervise|supervises|supervised|assemble|assembles|assembled|pack|packs|packed|ship|ships|shipped|clean|cleans|cleaned|scan|scans|scanned|sign|signs|signed|enter|enters|entered|upload|uploads|uploaded|download|downloads|downloaded|backup|backs up|restore|restores|restored|migrate|migrates|migrated|edit|edits|edited)\b',re.I),
]
FIRST_PERSON_PERF=[
    re.compile(r'\b(?P<a>I)\b\s+(?:will|must|need to|have to|am going to|plan to|intend to)\s+(?:perform|implement|execute|run|conduct|administer|install|configure|deploy|test|complete|follow|operate|collect|record|submit|review|inspect|measure|monitor|maintain|repair|prepare|document|assess|calculate|teach|deliver|drive|diagnose|treat|verify|validate|publish|present|train|assemble|pack|ship|clean|scan|enter|upload|download|backup|restore|migrate|edit)\b',re.I),
    re.compile(r'\b(?P<a>we)\b\s+(?:will|must|need to|have to|are going to|plan to|intend to)\s+(?:perform|implement|execute|run|conduct|administer|install|configure|deploy|test|complete|follow|operate|collect|record|submit|review|inspect|measure|monitor|maintain|repair|prepare|document|assess|calculate|teach|deliver|drive|diagnose|treat|verify|validate|publish|present|train|assemble|pack|ship|clean|scan|enter|upload|download|backup|restore|migrate|edit)\b',re.I),
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

def matches(text,pats,role,prefix):
    out=[]; seen=set()
    for i,p in enumerate(pats,1):
        for m in p.finditer(text):
            s,e=m.span('a'); exact=text[s:e]; k=(role,exact.lower())
            if k in seen: continue
            seen.add(k); out.append({'role':role,'sourceExactSpan':exact,'patternId':f'{prefix}-{i}'})
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

def norm_hash(text):
    return hashlib.sha256(re.sub(r'\s+',' ',text).strip().lower().encode()).hexdigest()

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for p in OUT.glob('*.jsonl'): p.unlink()
    paths=[hf_hub_download(REPO,filename=s,repo_type='dataset',revision=REV) for s in SHARDS]
    sessions={}; raw=0
    for r in iter_rows(paths):
        raw+=1
        if r.get('language')!='en' or bool(r.get('is_code')): continue
        turns=user_turns(r)
        if not turns: continue
        sid=str(r.get('evaluation_session_id') or r.get('id') or '')
        k=rowkey(r,turns)
        if sid not in sessions or k>sessions[sid][0]: sessions[sid]=(k,r,turns)
    if raw!=EXPECTED: raise RuntimeError(f'raw rows {raw} != {EXPECTED}')

    pools={'audience':[],'performer':[]}; rejection=Counter(); seen={'audience':set(),'performer':set()}
    for sid,(_,r,turns) in sessions.items():
        joined='\n'.join(turns)
        if any(p.search(joined) for p in PII): rejection['privacyFamily']+=1; continue
        domains=[k for k,p in DOMAIN_PATTERNS.items() if p.search(joined)]
        family_flags={
            'stateOps':bool(STATE.search(joined)),
            'preference':bool(PREF.search(joined)),
            'crossDomain':len(domains)>=2,
            'requesterEvaluator':bool(REQUESTER.search(joined) or EVALUATOR.search(joined)),
            'domainSignals':domains,
        }
        if any(family_flags[k] for k in ('stateOps','preference','crossDomain','requesterEvaluator')):
            rejection['higherLaneFamily']+=1; continue
        for ti,t0 in enumerate(turns):
            t=t0.strip()
            if not (24<=len(t)<=3500): rejection['lengthTurn']+=1; continue
            am=matches(t,AUD,'audience','audience')
            pm=matches(t,PERF,'performer','performer')+matches(t,FIRST_PERSON_PERF,'performer','performer-first-person')
            if not am and not pm: continue
            base={
                'schemaVersion':'router-stage05-parallel-v1-lane05-candidate-v2',
                'sourceDataset':REPO,'sourceRevision':REV,
                'sourceRowId':str(r.get('id') or ''),
                'sourceEvaluationSessionId':sid,
                'evaluationOrder':int(r.get('evaluation_order') or 0),
                'familyId':f'session:{sid}','familyBasis':'evaluation_session_id',
                'userTurns':turns,'targetTurnIndex':ti,'sourceText':t,
                'privacyFlags':[],'higherLaneHeuristicFlags':family_flags,
                'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'trainingEligible':False,
            }
            if am:
                nh=norm_hash(t)
                if nh not in seen['audience']:
                    seen['audience'].add(nh); x=dict(base); x['heuristicActors']=am+pm; pools['audience'].append(x)
            if pm:
                nh=norm_hash(t)
                if nh not in seen['performer']:
                    seen['performer'].add(nh); x=dict(base); x['heuristicActors']=pm+am; pools['performer'].append(x)

    manifest={
        'schemaVersion':'router-stage05-parallel-v1-lane05-candidate-manifest-v2',
        'sourceDataset':REPO,'sourceRevision':REV,'rawRowsRead':raw,'canonicalSessions':len(sessions),
        'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'trainingEligible':False,
        'familyOwnershipPolicy':'exclude family when StateOps, preference, crossDomain, or requester/evaluator heuristic fires anywhere in canonical user history',
        'rejections':dict(rejection),'pools':{}
    }
    for role,items in pools.items():
        def score(x):
            pats=[a['patternId'] for a in x['heuristicActors'] if a['role']==role]
            first=any(p.startswith('performer-first-person') for p in pats)
            return (first,len(x['sourceText']),x['sourceEvaluationSessionId'],x['targetTurnIndex'])
        items.sort(key=score)
        items=items[:300]
        files=[]
        for start in range(0,len(items),12):
            p=OUT/f'{role}-{start//12:03d}.jsonl'; rows=items[start:start+12]
            p.write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in rows),encoding='utf-8')
            files.append({'path':str(p),'rows':len(rows),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
        manifest['pools'][role]={'rows':len(items),'files':files}
    mp=OUT/'candidate-manifest.json'; mp.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(manifest,ensure_ascii=False,indent=2))
if __name__=='__main__': main()
