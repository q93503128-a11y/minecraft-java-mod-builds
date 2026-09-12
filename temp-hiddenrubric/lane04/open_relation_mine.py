import hashlib, json, re
from pathlib import Path
import pyarrow.parquet as pq
from huggingface_hub import hf_hub_download

REPO='lmarena-ai/arena-human-preference-140k'
REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
SHARDS=[f'data/train-{i:05d}-of-00007.parquet' for i in range(7)]
EXPECTED=135634

# Discover explicit evaluation relations without requiring a predefined actor lexicon.
REL_PATTERNS=[
 ('for_relation_by',14,re.compile(r'\bfor\s+(?:final\s+)?(?P<rel>review|approval|feedback|evaluation|assessment|grading|marking|scoring)\s+(?:by|from)\b',re.I)),
 ('passive_by',14,re.compile(r'\b(?P<rel>reviewed|graded|marked|evaluated|assessed|approved|rejected|scored|judged|vetted)\s+by\b',re.I)),
 ('feedback_from',13,re.compile(r'\b(?P<rel>feedback|comments?|critique|approval|sign[- ]?off|review)\s+from\b',re.I)),
 ('future_actor_action',12,re.compile(r'\b(?:will|would|shall|must|needs?\s+to|has\s+to|have\s+to|is\s+going\s+to|are\s+going\s+to)\s+(?P<rel>review|grade|mark|evaluate|assess|approve|reject|score|judge|vet|sign\s+off)\b',re.I)),
 ('actor_action_object',10,re.compile(r'\b(?P<rel>review(?:s|ed|ing)?|grade(?:s|d|ing)?|mark(?:s|ed|ing)?|evaluat(?:e|es|ed|ing)|assess(?:es|ed|ing)?|approv(?:e|es|ed|ing)|reject(?:s|ed|ing)?|score(?:s|d|ing)?|judg(?:e|es|ed|ing)|vet(?:s|ted|ting)?|sign(?:s|ed|ing)?\s+off)\b',re.I)),
 ('submit_to',8,re.compile(r'\b(?P<rel>submit(?:ted|ting|s)?|present(?:ed|ing|s)?|send(?:ing|s|sent)?|share(?:d|ing|s)?|deliver(?:ed|ing|s)?)\b.{0,180}\bto\b',re.I|re.S)),
]

OUTPUT_RE=re.compile(r'\b(?:report|paper|manuscript|essay|assignment|thesis|dissertation|proposal|application|resume|cv|portfolio|presentation|slides?|document|draft|submission|deliverable|memo|email|letter|plan|analysis|case study|abstract|statement|pitch|spiel|response|answer|project|work|design|prototype|recommendation|business case|brief|form|request)\b',re.I)
REQUEST_RE=re.compile(r'\b(?:write|draft|prepare|create|transform|revise|edit|improve|polish|rewrite|develop|generate|make|help me|compose|format|finalize|refine|review|check|critique|assess|evaluate|summarize|summarise|proofread)\b',re.I)
FUTURE_RE=re.compile(r'\b(?:will|would|shall|must|needs? to|has to|have to|going to|before (?:i|we) (?:submit|send|present|share)|once (?:i|we) (?:submit|send|present|share))\b',re.I)
PAST_RE=re.compile(r'\b(?:last year|yesterday|previously|already|was approved|were approved|had approved|gave me feedback|gave feedback|received feedback|got feedback|commented on)\b',re.I)
ASSISTANT_PERSONA_RE=re.compile(r'\b(?:you are|act as|assume yourself|pretend you are|your role is|as an expert reviewer|as a reviewer|you will review)\b',re.I)
JOBISH_RE=re.compile(r'\b(?:job description|responsibilities|work experience|employment history|key responsibilities|resume bullet)\b',re.I)

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

def content_text(msg):
    if not isinstance(msg,dict): return ''
    if isinstance(msg.get('text'),str): return msg['text']
    c=msg.get('content')
    if isinstance(c,str): return c
    if isinstance(c,list):
        return '\n'.join(i if isinstance(i,str) else i.get('text','') for i in c if isinstance(i,str) or isinstance(i,dict))
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

def sentence_window(text,s,e):
    lo=max(text.rfind('\n',0,s),text.rfind('.',0,s),text.rfind('?',0,s),text.rfind('!',0,s),text.rfind(';',0,s))
    lo=0 if lo<0 else lo+1
    rights=[x for x in [text.find('\n',e),text.find('.',e),text.find('?',e),text.find('!',e),text.find(';',e)] if x>=0]
    hi=min(rights)+1 if rights else min(len(text),e+550)
    sent=re.sub(r'\s+',' ',text[lo:hi]).strip()
    # Add neighboring context for actor recovery.
    ctx_lo=max(0,lo-220); ctx_hi=min(len(text),hi+220)
    ctx=re.sub(r'\s+',' ',text[ctx_lo:ctx_hi]).strip()
    return sent[:1100],ctx[:1600]

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
            k=row_key(row,ts)
            if sid not in sessions or k>sessions[sid][0]: sessions[sid]=(k,row,ts)
assert raw==EXPECTED,(raw,EXPECTED)

rows=[]; seen=set(); rej={'higherLaneFamily':0,'privacy':0,'length':0,'duplicateText':0,'noRelation':0}
for sid,(_,row,ts) in sessions.items():
    flags=family_flags(ts)
    if any(flags.values()): rej['higherLaneFamily']+=1; continue
    for ti,text in enumerate(ts):
        if not (24<=len(text)<=20000): rej['length']+=1; continue
        if any(p.search(text) for p in PII): rej['privacy']+=1; continue
        norm=re.sub(r'\s+',' ',text).strip().lower(); h=hashlib.sha256(norm.encode()).hexdigest()
        if h in seen: rej['duplicateText']+=1; continue
        matches=[]
        for ptype,base,pat in REL_PATTERNS:
            for m in pat.finditer(text):
                sent,ctx=sentence_window(text,m.start(),m.end())
                score=base
                if OUTPUT_RE.search(sent): score+=4
                elif OUTPUT_RE.search(ctx): score+=2
                if REQUEST_RE.search(text): score+=2
                if FUTURE_RE.search(sent): score+=3
                if PAST_RE.search(sent): score-=5
                if ASSISTANT_PERSONA_RE.search(ctx): score-=6
                if JOBISH_RE.search(ctx): score-=4
                if ti==0: score+=1
                if len(text)<=2500: score+=1
                matches.append({'pattern':ptype,'relationRaw':m.groupdict().get('rel') or ptype,'score':score,'evidence':sent,'context':ctx,'matchStart':m.start(),'matchEnd':m.end()})
        if not matches:
            rej['noRelation']+=1; continue
        seen.add(h); matches.sort(key=lambda x:(-x['score'],x['matchStart']))
        b=matches[0]
        rows.append({'sourceDataset':REPO,'sourceRevision':REV,'sourceRowId':str(row.get('id') or ''),'sourceEvaluationSessionId':sid,'evaluationOrder':int(row.get('evaluation_order') or 0),'turnIndex':ti,'familyTurnCount':len(ts),'sourceLength':len(text),'sourceText':text,'score':b['score'],'pattern':b['pattern'],'relationRaw':b['relationRaw'],'evidence':b['evidence'],'context':b['context'],'allMatches':matches[:8],'assistantOutputsIncluded':False,'trainingEligible':False})

rows.sort(key=lambda x:(-x['score'],x['sourceLength'],x['sourceEvaluationSessionId'],x['turnIndex']))
rows=rows[:600]
out=Path('temp-hiddenrubric/lane04'); out.mkdir(parents=True,exist_ok=True)
with (out/'open-relation-full.jsonl').open('w',encoding='utf-8') as f,(out/'open-relation-compact.jsonl').open('w',encoding='utf-8') as c:
    for x in rows:
        f.write(json.dumps(x,ensure_ascii=False)+'\n')
        c.write(json.dumps({k:x[k] for k in ['sourceRowId','sourceEvaluationSessionId','turnIndex','familyTurnCount','sourceLength','score','pattern','relationRaw','evidence','context']},ensure_ascii=False)+'\n')
manifest={'sourceDataset':REPO,'sourceRevision':REV,'rawRows':raw,'sessionFamilies':len(sessions),'openRelationCandidates':len(rows),'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'rejections':rej}
(out/'open-relation-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(manifest,ensure_ascii=False))
