import json,re
from pathlib import Path

src=Path('temp-hiddenrubric/lane04/open-relation-compact.jsonl')
rows=[json.loads(x) for x in src.read_text(encoding='utf-8').splitlines() if x.strip()]

native=re.compile(r'\b(?:hiring manager|human resources manager|hr manager|recruiter|teacher|professor|supervisor|advisor|adviser|mentor|committee|reviewer|reviewers|editor|examiner|marker|grader|judge|judges|jury|panel|board|executive|leadership|management|investor|investors|stakeholder|stakeholders|sponsor|approver|auditor|assessor|evaluator|director|principal)\b',re.I)
strong_rel=re.compile(r'\b(?:for (?:final )?(?:review|approval|feedback|evaluation|assessment|grading|marking) (?:by|from)|(?:reviewed|graded|marked|evaluated|assessed|approved|rejected|scored|judged) by|(?:will|would|must|shall|needs? to|has to|have to) (?:review|grade|mark|evaluate|assess|approve|reject|score|judge)|feedback from|approval from|review from)\b',re.I)
output=re.compile(r'\b(?:report|paper|manuscript|essay|assignment|thesis|dissertation|proposal|application|resume|cv|portfolio|presentation|slide|slides|document|draft|submission|deliverable|memo|email|letter|plan|analysis|case study|abstract|statement|pitch|project|work|design|prototype|recommendation|business case|brief|form|request)\b',re.I)
current=re.compile(r'\b(?:write|draft|prepare|create|transform|revise|edit|improve|polish|rewrite|develop|generate|make|help me|compose|format|finalize|refine|proofread|submit|present|apply)\b',re.I)
assistant_persona=re.compile(r'\b(?:you will|you would|your role|act as|assume yourself|pretend you are|as an? (?:expert )?(?:reviewer|teacher|professor|judge|grader|evaluator))\b',re.I)
historical=re.compile(r'\b(?:was approved|were approved|has been approved|had been approved|received feedback|got feedback|gave me feedback|previously reviewed|was reviewed|were reviewed|last year|yesterday)\b',re.I)
generic=re.compile(r'\b(?:client|customer|user|recipient)\b',re.I)

out=[]
for r in rows:
    ev=r.get('evidence',''); ctx=r.get('context',''); text=ev+' '+ctx
    score=int(r.get('score',0))
    if native.search(ev): score+=8
    elif native.search(ctx): score+=4
    else: continue
    if strong_rel.search(text): score+=7
    if output.search(ev): score+=4
    elif output.search(ctx): score+=2
    if current.search(ctx): score+=3
    if re.search(r'\b(?:my|our)\s+(?:report|paper|manuscript|essay|assignment|thesis|dissertation|proposal|application|resume|cv|portfolio|presentation|document|draft|submission|work|project)\b',ctx,re.I): score+=4
    if assistant_persona.search(ctx): score-=10
    if historical.search(text): score-=7
    if generic.search(ev) and not strong_rel.search(ev): score-=5
    rr=dict(r); rr['postfilterScore']=score
    out.append(rr)
out.sort(key=lambda x:(-x['postfilterScore'],x.get('sourceLength',0),x['sourceRowId']))
Path('temp-hiddenrubric/lane04/open-relation-native-shortlist.jsonl').write_text(''.join(json.dumps(x,ensure_ascii=False)+'\n' for x in out[:180]),encoding='utf-8')
Path('temp-hiddenrubric/lane04/open-relation-native-shortlist-manifest.json').write_text(json.dumps({'inputRows':len(rows),'nativeShortlistRows':min(len(out),180),'totalNativeMatches':len(out)},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print({'inputRows':len(rows),'nativeShortlistRows':min(len(out),180),'totalNativeMatches':len(out)})
