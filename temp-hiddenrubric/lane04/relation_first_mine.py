import hashlib, json, re
from pathlib import Path
import pyarrow.parquet as pq
from huggingface_hub import hf_hub_download

REPO='lmarena-ai/arena-human-preference-140k'
REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
SHARDS=[f'data/train-{i:05d}-of-00007.parquet' for i in range(7)]
EXPECTED=135634

# Precision-first actor classes. Evaluation-native roles rank above generic recipients.
EVAL_NATIVE=[
 'hiring manager','peer reviewer','peer reviewers','scientific reviewer','reviewer','reviewers',
 'thesis committee','dissertation committee','selection committee','review committee','evaluation committee','evaluating committee',
 'admissions committee','admissions officer','admissions team','interview panel','grading committee','assessment panel',
 'module team','teacher marker','marker','examiner','examiners','professor','professors','teacher','instructor',
 'supervisor','advisor','adviser','editor','journal editor','associate editor','judge','judges','jury','panel','panelists',
 'grant committee','funding committee','grant reviewer','grant reviewers','auditor','auditors','assessor','assessors','evaluator','evaluators','grader','graders',
 'board of directors','executive board','senior management','senior leadership','executive leadership','leadership','management','executives','executive',
 'project sponsor','sponsor','sponsors','approver','approvers','team lead','manager','boss','recruiter','hr manager','hr team','human resources'
]
GENERIC=['client','clients','customer','customers','stakeholder','stakeholders','investor','investors','director','president','principal']
ACTORS=sorted(set(EVAL_NATIVE+GENERIC),key=len,reverse=True)
ACT='|'.join(re.escape(x) for x in ACTORS)
NATIVE=set(x.lower() for x in EVAL_NATIVE)

OUTPUT=r'(?:report|paper|manuscript|essay|assignment|thesis|dissertation|proposal|application|resume|cv|portfolio|presentation|slides?|document|draft|submission|deliverable|memo|email|letter|plan|analysis|case study|abstract|statement|pitch|spiel|response|answer|project|work)'
REL=r'(?:review|approval|feedback|evaluation|assessment|grading|marking|score|scoring|judging|decision)'
ACTION=r'(?:review(?:s|ed|ing)?|grade(?:s|d|ing)?|mark(?:s|ed|ing)?|evaluat(?:e|es|ed|ing)|assess(?:es|ed|ing)?|approv(?:e|es|ed|ing)|reject(?:s|ed|ing)?|score(?:s|d|ing)?|judg(?:e|es|ed|ing)|provide(?:s|d)? feedback|give(?:s|n)? feedback)'
REQUEST=re.compile(r'\b(?:write|draft|prepare|create|transform|revise|edit|improve|polish|rewrite|develop|generate|make|help me|compose|format|finalize|refine|review|check|critique|assess|evaluate)\b',re.I)
OUTPUT_RE=re.compile(rf'\b{OUTPUT}\b',re.I)

PATTERNS=[
 ('output_for_actor_relation',12,re.compile(rf'\b(?P<output>{OUTPUT})\b.{{0,180}}?\b(?:for|to)\s+(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\s+(?:for\s+)?(?P<rel>{REL})\b',re.I|re.S)),
 ('actor_will_action_output',12,re.compile(rf'\b(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\b.{{0,35}}?\b(?:will|would|must|shall|can|is going to|are going to|needs? to|has to|have to)\s+(?P<rel>{ACTION})\b.{{0,160}}?\b(?P<output>{OUTPUT})\b',re.I|re.S)),
 ('output_action_by_actor',12,re.compile(rf'\b(?P<output>{OUTPUT})\b.{{0,140}}?\b(?P<rel>{ACTION})\b.{{0,45}}?\bby\s+(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\b',re.I|re.S)),
 ('submit_output_to_actor_for_relation',13,re.compile(rf'\b(?:submit|send|present|share|deliver|provide)(?:ted|ting|s)?\b.{{0,100}}?\b(?P<output>{OUTPUT})\b.{{0,100}}?\bto\s+(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\b.{{0,80}}?\bfor\s+(?P<rel>{REL})\b',re.I|re.S)),
 ('submit_to_native_actor',8,re.compile(rf'\b(?:submit|send|present|share|deliver|provide)(?:ted|ting|s)?\b.{{0,120}}?\b(?P<output>{OUTPUT})\b.{{0,100}}?\bto\s+(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\b',re.I|re.S)),
 ('actor_action_this_output',11,re.compile(rf'\b(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\b.{{0,70}}?\b(?P<rel>{ACTION})\b.{{0,120}}?\b(?:this|my|our|the)\s+(?P<output>{OUTPUT})\b',re.I|re.S)),
 ('used_by_actor_to_evaluate',13,re.compile(rf'\b(?P<output>{OUTPUT}|finished work|final work|finished product|final product)\b.{{0,100}}?\b(?:used|seen|read|considered)\s+by\s+(?P<actor>(?:(?:my|our|the|a|an|their|his|her|potential)\s+)?(?:{ACT}))\b.{{0,100}}?\bto\s+(?P<rel>evaluate|assess|review|approve|judge|score)\b',re.I|re.S)),
 ('seeking_actor_approval_for_output',12,re.compile(rf'\b(?P<output>{OUTPUT})\b.{{0,140}}?\b(?:seeking|seek|get|obtain|need(?:ing)?)\s+(?:the\s+)?(?P<rel>approval|feedback|review|evaluation)\s+(?:from|of)\s+(?P<actor>(?:(?:my|our|the|a|an|their|his|her)\s+)?(?:{ACT}))\b',re.I|re.S)),
]

PII=[re.compile(r'\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b',re.I),re.compile(r'(?<!\d)(?:\+?\d[\d .()\-]{7,}\d)(?!\d)'),re.compile(r'\b(?:sk-[A-Za-z0-9_-]{16,}|api[_-]?key\s*[:=])',re.I)]
PREF=re.compile(r'\b(?:i prefer|i would prefer|we prefer|my preference|our preference|my favorite|my favourite|i hate|i dislike|i love|i like|we like|we love|prefer(?:red)? to)\b',re.I)
STATE=re.compile(r'\b(?:actually|instead|rather than|change|replace|revise|update|scratch that|forget (?:that|the)|previous|earlier|above|keep (?:the|it)|preserve|retain|don.t change|do not change|remove|omit|delete|no longer|make it|rewrite|same as before|again but)\b',re.I)
DOMAINS={
'legal':re.compile(r'\b(?:legal|law|lawyer|attorney|contract|compliance|court|regulation|lawsuit|patent)\b',re.I),
'finance':re.compile(r'\b(?:finance|financial|accounting|tax|investment|portfolio|budget|loan|mortgage|stock)\b',re.I),
'health':re.compile(r'\b(?:medical|health|clinical|doctor|nurse|patient|hospital|surgery|therapy|disease|medicine)\b',re.I),
'software':re.compile(r'\b(?:software|developer|programming|code|api|database|cloud|devops|javascript|python|network|cyber|security)\b',re.I),
'business':re.compile(r'\b(?:business|marketing|sales|customer|client|startup|revenue|brand|seo|company|entrepreneur|product)\b',re.I),
'education':re.compile(r'\b(?:education|teacher|student|classroom|lesson|school|curriculum|university|assignment|exam|thesis)\b',re.I),
'science':re.compile(r'\b(?:physics|chemistry|biology|astronomy|scientific|research|experiment|journal|mathematics|math)\b',re.I),
'creative':re.compile(r'\b(?:design|graphic|photo|video|music|art|story|novel|film|logo|content creator)\b',re.I)}
JOBISH=re.compile(r'\b(?:resume|curriculum vitae|job description|responsibilities|work experience|employment history)\b',re.I)
HYPOTHETICAL=re.compile(r'\b(?:imagine|hypothetical|roleplay|role-play|pretend|assume yourself as|act as|you are a|you are an)\b',re.I)


def content_text(msg):
    if not isinstance(msg,dict): return ''
    if isinstance(msg.get('text'),str): return msg['text']
    c=msg.get('content')
    if isinstance(c,str): return c
    if isinstance(c,list):
        out=[]
        for item in c:
            if isinstance(item,str): out.append(item)
            elif isinstance(item,dict) and isinstance(item.get('text'),str): out.append(item['text'])
        return '\n'.join(out)
    return ''

def user_turns(row):
    out=[]
    for e in row.get('full_conversation') or []:
        if not isinstance(e,dict): continue
        u=e.get('user')
        if isinstance(u,dict):
            t=content_text(u).strip()
            if t: out.append(t)
        elif str(e.get('role') or '')=='user':
            t=content_text(e).strip()
            if t: out.append(t)
    return out

def row_key(row,ts):
    stamp=row.get('timestamp')
    try: sk=int(stamp.timestamp()*1e9) if hasattr(stamp,'timestamp') else int(stamp or 0)
    except Exception: sk=0
    return (int(row.get('evaluation_order') or 0),len(ts),sk)

def cross_domain(text):
    ds={k for k,p in DOMAINS.items() if p.search(text)}
    if len(ds)>=4: return True
    return any(a in ds and b in ds for a,b in [('legal','software'),('legal','health'),('finance','health'),('finance','software'),('legal','finance')])

def family_flags(ts):
    return {'preference':any(PREF.search(t) for t in ts),'stateOps':any(i>0 and STATE.search(t) for i,t in enumerate(ts)),'crossDomain':any(cross_domain(t) for t in ts)}

def relation(raw,ptype):
    x=raw.lower()
    if 'approval' in x or 'approv' in x: return 'approval_from_actor'
    if 'feedback' in x: return 'feedback_from_actor'
    if ptype.startswith('submit'): return 'submit_to_actor'
    if 'reject' in x: return 'reject_by_actor'
    if 'grad' in x or 'mark' in x: return 'grade_by_actor'
    if any(z in x for z in ['evaluat','assess','score','judg']): return 'evaluate_by_actor'
    return 'review_by_actor'

def evidence(text,s,e):
    lo=max(text.rfind('\n',0,s),text.rfind('.',0,s),text.rfind('?',0,s),text.rfind('!',0,s))
    lo=0 if lo<0 else lo+1
    rs=[x for x in [text.find('\n',e),text.find('.',e),text.find('?',e),text.find('!',e)] if x>=0]
    hi=min(rs)+1 if rs else min(len(text),e+450)
    return re.sub(r'\s+',' ',text[lo:hi]).strip()[:900]

cols=['id','evaluation_session_id','evaluation_order','language','is_code','timestamp','full_conversation']
sessions={}; raw=0
for shard in SHARDS:
    path=hf_hub_download(REPO,filename=shard,repo_type='dataset',revision=REV)
    pf=pq.ParquetFile(path); use=[c for c in cols if c in set(pf.schema_arrow.names)]
    for batch in pf.iter_batches(batch_size=512,columns=use):
        for row in batch.to_pylist():
            raw+=1
            if row.get('language')!='en' or bool(row.get('is_code')): continue
            ts=user_turns(row)
            if not ts: continue
            sid=str(row.get('evaluation_session_id') or row.get('id') or '')
            key=row_key(row,ts)
            if sid not in sessions or key>sessions[sid][0]: sessions[sid]=(key,row,ts)
assert raw==EXPECTED,(raw,EXPECTED)

candidates=[]; seen=set(); rejects={'higherLaneFamily':0,'privacy':0,'length':0,'duplicateText':0,'noOutputCue':0}
for sid,(_,row,ts) in sessions.items():
    flags=family_flags(ts)
    if any(flags.values()): rejects['higherLaneFamily']+=1; continue
    for ti,text in enumerate(ts):
        if not (24<=len(text)<=7000): rejects['length']+=1; continue
        if any(p.search(text) for p in PII): rejects['privacy']+=1; continue
        if not OUTPUT_RE.search(text): rejects['noOutputCue']+=1; continue
        n=hashlib.sha256(re.sub(r'\s+',' ',text).strip().lower().encode()).hexdigest()
        if n in seen: rejects['duplicateText']+=1; continue
        ms=[]
        for ptype,base,pat in PATTERNS:
            for m in pat.finditer(text):
                actor=m.group('actor').strip(); actor_core=re.sub(r'^(?:my|our|the|a|an|their|his|her|potential)\s+','',actor,flags=re.I).lower()
                rawrel=m.group('rel'); score=base
                if actor_core in NATIVE: score+=3
                if REQUEST.search(text): score+=2
                if ti==0: score+=1
                if len(text)<=1800: score+=1
                if JOBISH.search(text): score-=4
                if HYPOTHETICAL.search(text): score-=4
                # generic recipients need explicit review/evaluation relation; plain submit gets heavily penalized
                if actor_core in {x.lower() for x in GENERIC} and ptype=='submit_to_native_actor': score-=6
                s,e=m.span('actor')
                ms.append({'pattern':ptype,'score':score,'actor':actor,'sourceStart':s,'sourceEnd':e,'relationEvidence':relation(rawrel,ptype),'evidence':evidence(text,m.start(),m.end())})
        if not ms: continue
        seen.add(n); ms.sort(key=lambda x:(-x['score'],x['sourceStart']))
        best=ms[0]
        candidates.append({'sourceDataset':REPO,'sourceRevision':REV,'sourceRowId':str(row.get('id') or ''),'sourceEvaluationSessionId':sid,'evaluationOrder':int(row.get('evaluation_order') or 0),'turnIndex':ti,'familyTurnCount':len(ts),'sourceLength':len(text),'sourceText':text,'actor':best['actor'],'sourceActorSpan':{'start':best['sourceStart'],'end':best['sourceEnd'],'text':text[best['sourceStart']:best['sourceEnd']]},'relationEvidence':best['relationEvidence'],'pattern':best['pattern'],'score':best['score'],'evidence':best['evidence'],'allMatches':ms[:8],'assistantOutputsIncluded':False,'trainingEligible':False})

candidates.sort(key=lambda x:(-x['score'],x['sourceLength'],x['sourceEvaluationSessionId'],x['turnIndex']))
candidates=candidates[:400]
out=Path('temp-hiddenrubric/lane04'); out.mkdir(parents=True,exist_ok=True)
with (out/'relation-evaluator-full.jsonl').open('w',encoding='utf-8') as f, (out/'relation-evaluator-compact.jsonl').open('w',encoding='utf-8') as c:
    for x in candidates:
        f.write(json.dumps(x,ensure_ascii=False)+'\n')
        c.write(json.dumps({k:x[k] for k in ['sourceRowId','sourceEvaluationSessionId','turnIndex','familyTurnCount','sourceLength','actor','sourceActorSpan','relationEvidence','pattern','score','evidence']},ensure_ascii=False)+'\n')
manifest={'sourceDataset':REPO,'sourceRevision':REV,'rawRows':raw,'sessionFamilies':len(sessions),'relationFirstCandidates':len(candidates),'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'rejections':rejects}
(out/'relation-evaluator-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(manifest,ensure_ascii=False))
