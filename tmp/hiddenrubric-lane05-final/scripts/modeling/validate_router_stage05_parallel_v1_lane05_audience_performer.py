#!/usr/bin/env python3
from __future__ import annotations
import json
from collections import Counter
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
STAGE=ROOT/'data/staging/router-stage05/parallel-v1/lane05-audience-performer'
REVIEW=ROOT/'data/reviews/router/stage05-parallel-v1/lane05-audience-performer/review.jsonl'
REPORT=ROOT/'data/reports/router-stage05-parallel-v1-lane05-audience-performer-2026-09-08.json'
EXPECTED_REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
def req(x,m):
    if not x: raise RuntimeError(m)
def jl(path):
    return [json.loads(x) for x in path.read_text(encoding='utf-8').splitlines() if x.strip()]
def main():
    man=json.loads((STAGE/'manifest.json').read_text(encoding='utf-8'))
    rows=[]
    for b in man['batches']:
        p=ROOT/b['path']; req(p.is_file(),f'missing batch {p}'); rows+=jl(p)
    req(len(rows)==122,'unique target-turn row count drift')
    seen=set(); fam=set(); c=Counter(); overlap=0
    for r in rows:
        src=r['source']; key=(src['sourceRowId'],src['targetTurnIndex'])
        req(key not in seen,f'duplicate target {key}'); seen.add(key); fam.add(src['familyId'])
        req(src['revision']==EXPECTED_REV and src['sourceKind']=='human' and src['language']=='en','source provenance drift')
        req(r['assistantOutputsIncluded'] is False,'assistant output leakage')
        req(r['trainingEligible'] is False and r['independentReview'] is False,'authority leak')
        req(r['sourceCompleteness']['allHumanUserTurnsIncluded'] is True,'source incomplete')
        ti=src['targetTurnIndex']; turns=r['sourceUserTurns']; req(ti < len(turns) and turns[ti].strip()==r['sourceText'].strip(),'target/source mismatch')
        req(r['privacy']['passed'] is True and r['privacy']['piiCopiedIntoAnnotation'] is False,'privacy failure')
        h=r['higherLaneReview']; req(not any(h[k] for k in ('stateOps','preference','crossDomain','requester','evaluator','quotedTextFalsePositive','assistantRoleplayFalsePositive')),'higher-lane/false-positive leak')
        roles=set()
        for a in r['actorAnnotations']:
            role=a['role']; span=a['sourceExactSpan']; ko=a['koreanExactSpan']
            req(role in {'audience','performer'},'wrong actor role')
            req(a['present'] is True and span and span in r['sourceText'],'actor exact span not verbatim')
            req(ko and a['evidence']['koreanActorPhrase']==ko,'Korean exact span grounding failure')
            req(a['translation']['scope']=='actor-span' and a['translation']['sourceMeaningPreserved'] is True and a['translation']['actorReferencePreserved'] is True,'translation fidelity flag failure')
            roles.add(role); c[role]+=1
        req(roles==set(r['reviewedActorSpanRoles'])==set(r['reviewedActorPresenceRoles']),'actor mask drift')
        if len(roles)>1: overlap+=1
    req(len(fam)==122,'more than one target selected from a family')
    req(c['audience']==72 and c['performer']==60 and overlap==10,f'actor counts drift {c} overlap={overlap}')
    review=jl(REVIEW); req(len(review)==122,'review row count drift')
    for x in review:
        req(x['decision']=='accepted' and x['trainingEligible'] is False and x['independentReview'] is False,'review authority drift')
        req(all(x['checks'].values()),f"review gate failure {x['id']}")
    rep=json.loads(REPORT.read_text(encoding='utf-8')); req(rep['targets']['targetTurnsMet'] and rep['targets']['audienceMet'] and rep['targets']['performerMet'],'report target gate failed')
    req(rep['authority']['globalPromotionPerformed'] is False and rep['authority']['productionAuthorityChanged'] is False,'global promotion detected')
    out={'schemaVersion':'router-stage05-parallel-v1-lane05-validation-v1','status':'PASS','uniqueTargetTurns':len(rows),'uniqueFamilies':len(fam),'audienceExactSpanRows':c['audience'],'performerExactSpanRows':c['performer'],'audiencePerformerOverlapRows':overlap,'trainingEligibleRows':0,'independentReviewCompletedRows':0,'globalPromotionPerformed':False,'assistantOutputsIncluded':False}
    print(json.dumps(out,ensure_ascii=False,indent=2,sort_keys=True))
if __name__=='__main__': main()
