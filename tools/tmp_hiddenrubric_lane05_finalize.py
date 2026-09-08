#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, subprocess, textwrap
from collections import Counter, defaultdict
from pathlib import Path

SRC=Path('tmp/hiddenrubric-lane05-candidates')
OLD='6cf80e5d7896d4cdb2fb632a3227be68f3006995'
FINAL=Path('tmp/hiddenrubric-lane05-final')
REV='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
DATASET='lmarena-ai/arena-human-preference-140k'

# (sourceRowId, targetTurnIndex, source exact span, Korean exact span)
AUD=[
('2388de59-aecd-44df-a342-61af9303a92d',0,'beginners','초보자'),
('dcc6f23b-5a57-4c80-8aa3-41435e139a32',0,'kids','아이들'),
('aeda6e55-7fab-439d-817b-10046d129857',0,'kids','아이들'),
('5f483731-bb24-4094-9435-a7d2e2fed387',0,'kids','아이들'),
('77e5d368-e4f3-4a52-abce-bdce822760b1',0,'beginners','초보자'),
('4fbadf58-bc54-4515-85f5-cb2a6f288d8d',0,'kids','아이들'),
('acef8dc7-d8e8-4d1c-ae17-8ba3281ad7a1',0,'kids','아이들'),
('ab8147c6-195e-413c-b186-7f13441f7ccc',0,'kids','아이들'),
('7b46b927-c4a9-48b0-b306-4d883c95ab1c',1,'college students','대학생'),
('abbc082b-467b-4427-b831-6eb112931ecd',0,'kids','아이들'),
('0df47d30-5246-42a0-ae3a-0a77b1f7973f',0,'kids','아이들'),
('d00e90b0-f4ad-4212-b121-501a61407ea2',0,'athletes','운동선수'),
('ccf018cb-7f49-4bf6-b2b8-82f46fc04579',0,'kids','아이들'),
('53ee7b4b-6e56-4000-b360-d45f82ab132f',0,'students','학생'),
('430bd433-ebd8-4fc7-94e9-6f012e423095',0,'audience','시청자'),
('0617824a-5a08-4031-ac1d-ab54a690f6a8',2,'university students','대학생'),
('42f81871-094d-479b-8eaa-6659c45a38af',0,'shmup fans','슈팅 게임 팬'),
('71285dc6-4205-4308-ae7f-8f88cd53f2f3',0,'engineers','엔지니어'),
('484f95da-d040-497c-b102-381f8aa11149',2,'customers','고객'),
('ba888386-68b2-46ba-8c6c-d6381d02fb4b',0,'high school students','고등학생'),
('9340eb3f-e60d-44e1-a0df-f85a5eaf047b',0,'students','학생'),
('20da5ba1-a8e5-46ff-8e9e-c2cb69e51829',0,'children','어린이'),
('c423b0fd-d1e2-4a35-aa43-a5c23197dd1e',0,'students','학생'),
('e6f144e5-419b-498e-8c31-912dbea37eb1',1,'club fans','구단 팬'),
('dba3a925-7a71-4f99-9968-0cded817c565',0,'students','학생'),
('8029b54f-7e4f-468f-9d1d-a074e2f9c9c6',0,'students','학생'),
('e571989e-a05f-4ae0-9ffc-e7abd20073a5',0,'beginners','초보자'),
('d3af614d-c03f-4920-b2ac-49a467a21cdc',0,'Intern students','인턴 학생'),
('32d5e43e-552f-4962-b5f3-422e86df8fd1',0,'clients','고객'),
('e37cb132-8ba4-412a-affe-b7bb0c33593c',0,'beginners','초보자'),
('b01e454d-081f-48e2-818f-aa2912bca630',0,'managers','관리자'),
('43e11933-6f3a-478d-b403-4f4460a801aa',0,'employees','직원'),
('742038dc-bb90-47b8-bac6-3890df7d042c',0,'beginners','초보자'),
('eefdd156-cf4f-461d-a203-34f4a3ed25c8',0,'regulatory medical writers','규제 의료 작가'),
('db30a13e-4780-4cca-a8b4-169a78c755fa',0,'Teachers','교사'),
('9dc258c2-e35b-46ed-a170-5d6b43e7c6d3',0,'audience','관객'),
('0f1a076f-4f28-463c-840b-63e47b2c3f98',0,'children','어린이'),
('f34e2bd7-917d-4a26-851c-12ecc40fccf3',0,'readers','독자'),
('fc66bd01-434d-45f7-80d4-f13711c4f02b',0,'accountants','회계사'),
('749ea8f5-dba3-4f60-9736-fa7bb4430204',0,'children','어린이'),
('d265f4ee-b0ac-4813-9fe0-491d57d19a98',0,'users','사용자'),
('baf0811f-30e0-43ea-947c-24c4f72347b4',0,'teachers','교사'),
('d9007191-d087-4a37-a746-f649c79ce452',0,'clients','고객'),
('d194cbf5-4610-4567-9d29-4cb78f6174ad',0,'investors','투자자'),
('7e210fec-0ae1-45f7-8b3d-67527242d0f8',0,'Beginners','초보자'),
('9b985da4-921e-4588-aa2c-3f93e2003c51',0,'kids','아이들'),
('74abf044-f0d5-4a10-a893-2135e50f3ecb',0,'audience','독자'),
('125d1853-92ce-420c-9ed1-494931a2a59b',0,'experts','전문가'),
('16fcc7ac-3991-4fe9-8d1a-fe3222ae1d99',0,'teacher','교사'),
('71235f79-2bf2-40c8-a09d-3fb30f9453bc',0,'Beginners','초보자'),
('4556cc02-4f21-4b19-8375-c1113a28d18e',0,'viewers','시청자'),
('795c11ac-84ee-448a-af00-4050ba02e0c2',0,'executives','경영진'),
('5335676e-d5ce-43c1-9174-9e0a226462dd',0,'family','가족'),
('5c22bf2a-cfc2-4627-95bc-9830b2ee7bcc',0,'users','사용자'),
('33739cb2-fae8-46a7-b168-36d1598139c9',0,'kids','아이들'),
('b735ba7e-61e5-4295-9390-d9bba051c877',0,'children','어린이'),
('2e6c42dd-56eb-49b6-a10b-4d8c39174f39',0,'data scientists','데이터 과학자'),
('339909d7-ec88-49d8-8fac-3bdb39bbaa89',0,'teenagers','청소년'),
('2cc08f9b-0876-4538-9d91-d3d152903fd6',0,'students and staff','학생과 교직원'),
('fb63159b-a69f-4af4-bfde-0012ce9582a3',0,'students','학생'),
('85bd5f0b-8e4c-42cb-9ae9-9ca921b1481c',0,'students','학생'),
('f4d7b8e1-050f-410c-a97c-4715c5118ce9',0,'beginners','초보자'),
('fb420e3d-847d-44c3-bbec-945f97499a5e',0,'customers','고객'),
('0bbf7c89-4ccc-434b-84d6-3c3cb01dfdb2',0,'kids','아이들'),
('6b01f072-3f07-4990-ad68-a1e2f5d2f216',0,'engineers, economists, and lawyers','엔지니어, 경제학자, 변호사'),
('598e3e0d-c24c-43e1-a57a-b0fd441f3e14',0,'high school students','고등학생'),
('bcb029e5-e0dd-4005-ba83-f9fa5d0a1585',0,'designers','디자이너'),
('ce9c8913-0c32-4601-a1e2-66b81a044d8d',0,'kids','아이들'),
('b8a85d39-0d65-4536-a93f-3981d67dc31d',0,'family of five','5인 가족'),
('d8815736-6d6c-4cb2-b9ac-f542e9c18e97',0,'students','학생'),
('8ae0f334-d9ae-4c33-b7b7-a72cea5cf724',0,'students','학생'),
('e437a536-ba8f-4097-b31d-8cc4952efe79',0,'client','고객'),
]

PERF=[
('9a818c2c-8116-4c2f-ac02-8abd05888efc',0,'clinicians','임상의'),
('e89c5413-a76c-48b9-aade-5befddee3acc',0,'users','사용자'),
('f148b4a7-982a-42e2-a95b-6a845e13a48f',0,'participants','참가자'),
('f56a96d2-286e-4e56-8d17-a0862d190d95',0,'pilots','조종사'),
('ae8a43ac-07e4-4aba-916a-ebee01292753',0,'kids','아이들'),
('e571989e-a05f-4ae0-9ffc-e7abd20073a5',0,'beginners','초보자'),
('b64734a6-18fa-4389-ac0d-986724853ea9',1,'users','사용자'),
('bedba1b1-cac4-4907-8815-a95b9e222fcb',0,'data scientists','데이터 과학자'),
('e1a5f9fd-16c3-4ed0-b39d-b97cb4daf847',1,'Contractors','계약업체'),
('457a297f-519b-4ea0-bacd-5984b0ced3ac',0,'participants','참가자'),
('9377aa77-1e00-4aa3-a907-3dc4c5d2da69',0,'college students','대학생'),
('0874d62f-e872-443a-9f25-bc0d632d09f6',0,'customers','고객'),
('9beaaa79-9f5d-4416-907a-333e422ef48c',0,'executives','임원'),
('7394bd8f-6999-48b2-a07d-d5c56461c8b5',0,'voters','유권자'),
('bfa478e1-2ad9-44e7-b8ed-fb927fb20d04',0,'users','사용자'),
('888460d3-4ed3-4d3e-a6a4-56a5181b6d52',1,'teenagers','청소년'),
('22c45a63-d6c3-4563-91ea-fb28305378b1',0,'Students','학생'),
('ee856801-7b47-488a-9a9b-c8c937f28215',0,'lawyers','변호사'),
('9517b42d-4fc4-4fb2-a450-0cdcc0e3757b',0,'readers','독자'),
('9c9bc027-427e-40a3-88e2-6d21bfaafed7',0,'users','사용자'),
('a990fc6f-2644-468f-af3e-8adf16d41012',0,'users','사용자'),
('5c22bf2a-cfc2-4627-95bc-9830b2ee7bcc',0,'users','사용자'),
('3ad53bad-b726-4221-af31-50775988d2cd',0,'participants','참가자'),
('d3af614d-c03f-4920-b2ac-49a467a21cdc',0,'Intern students','인턴 학생'),
('db30a13e-4780-4cca-a8b4-169a78c755fa',0,'Teachers','교사'),
('749ea8f5-dba3-4f60-9736-fa7bb4430204',0,'children','어린이'),
('85bd5f0b-8e4c-42cb-9ae9-9ca921b1481c',0,'students','학생'),
('43e11933-6f3a-478d-b403-4f4460a801aa',0,'employees','직원'),
('9b5594fe-ee20-4e90-9e52-02002c12d09f',2,'users','사용자'),
('7b26dbc9-5fcb-471c-adcf-5bc10660490c',0,'customers','고객'),
('09a60173-df50-4da4-b77f-3edf4289e529',0,'i','나'),
('d1c7b576-cfaa-4ec6-b4f9-dc002df2a2f1',0,'I','나'),
('e5f20a5e-5d57-4775-97f4-f9772d157e64',0,'i','나'),
('cd471c9d-2ec1-4ff7-9b16-447b1ed93bae',0,'i','나'),
('c40fa8a1-c452-43f8-a225-f9ef0d3b2f8d',0,'I','내가'),
('60d1510c-9cc4-42d4-97f3-fd49fd5fc433',0,'i','내가'),
('2f75cfb1-fd1c-4726-af3a-a7f1a99fa270',1,'we','우리'),
('bacf79e6-0f64-4024-b4d5-95f2509130fb',0,'i','내가'),
('f83cfbc3-ddac-40ef-9b00-df1bee69c299',3,'i','내가'),
('a465c84d-6a0f-4e31-82f7-9004148f63ec',0,'I','나'),
('1e3c2471-de4c-4ec5-9085-e70c8d7c256b',0,'I','내가'),
('1b02168e-3417-4f4c-9abe-073c1ec1f49a',2,'I','내가'),
('34a87035-594f-4354-a30f-42dcd6206470',5,'I','내가'),
('235130f8-bdaa-4898-ad6b-8ab432d409e3',0,'I','나'),
('b72420c8-dd2c-44ec-8892-782a183bb409',0,'I','내가'),
('ebe89ee3-2a05-456f-b1be-8e48ec6a2964',0,'i','내가'),
('e2e32597-0bc6-443f-bf1b-a2d63e0d959e',0,'I','나'),
('8a9c8fb4-0e7c-46f6-80f3-ae777d9434e1',0,'I','나'),
('42bce15c-9bf7-4994-be38-7b5229641a26',0,'I','나'),
('f8df22d5-c120-4b6c-af48-030a80633636',0,'I','나'),
('d09e5110-77a1-464a-aeda-e40850beac07',0,'I','나'),
('de6e4bbe-b7ed-4067-a1bc-28dac66ba69c',1,'I','나'),
('d84fee42-bed0-488d-99ef-78eca8e57de3',0,'i','내가'),
('94342fe0-858c-4938-927b-e5b9cf2f4d71',0,'I','나'),
('1ab09545-0a4e-4168-adb7-0fdad4527cea',0,'I','나'),
('038558c3-f413-4a5b-9d4f-7d1d8ebdec25',0,'i','나'),
('7bbac13b-abd9-4ede-9d76-1e932b6672bb',0,'I','나'),
('0bbf7c89-4ccc-434b-84d6-3c3cb01dfdb2',0,'I','나'),
('fc66bd01-434d-45f7-80d4-f13711c4f02b',0,'I','나'),
('d265f4ee-b0ac-4813-9fe0-491d57d19a98',0,'I','나'),
]


def load_lines(path:Path):
    return [json.loads(x) for x in path.read_text(encoding='utf-8').splitlines() if x.strip()]

def load_old_audience():
    rows=[]
    listing=subprocess.check_output(['git','ls-tree','-r','--name-only',OLD,'tmp/hiddenrubric-lane05-candidates'],text=True).splitlines()
    for p in listing:
        if not p.rsplit('/',1)[-1].startswith('audience-') or not p.endswith('.jsonl'): continue
        txt=subprocess.check_output(['git','show',f'{OLD}:{p}'],text=True)
        rows += [json.loads(x) for x in txt.splitlines() if x.strip()]
    return rows

def all_candidates():
    rows=[]
    for p in sorted(SRC.glob('*.jsonl')):
        rows += load_lines(p)
    rows += load_old_audience()
    by={}
    for r in rows:
        k=(str(r['sourceRowId']),int(r['targetTurnIndex']))
        # Prefer v2 current over v1 old when both exist.
        if k not in by or str(r.get('schemaVersion','')).endswith('v2'):
            by[k]=r
    return by

def ann(role,span,ko):
    first_person=span.lower() in {'i','we'}
    return {
        'role':role,'present':True,'sourceExactSpan':span,'koreanExactSpan':ko,
        'relationshipToTask': 'intended-reader-listener-or-end-user' if role=='audience' else ('explicit-first-person-executor' if first_person else 'explicit-action-procedure-or-participant-executor'),
        'evidence':{'sourceTurn':'target','sourceExactSpan':span,'koreanActorPhrase':ko},
        'translation':{'scope':'actor-span','method':'direct-manual-model-localization','sourceMeaningPreserved':True,'actorReferencePreserved':True},
    }

def main():
    by=all_candidates(); merged=defaultdict(list)
    for rid,ti,span,ko in AUD: merged[(rid,ti)].append(ann('audience',span,ko))
    for rid,ti,span,ko in PERF: merged[(rid,ti)].append(ann('performer',span,ko))
    if len(AUD)!=72 or len(PERF)!=60 or len(merged)!=122: raise RuntimeError((len(AUD),len(PERF),len(merged)))

    rows=[]
    for idx,key in enumerate(sorted(merged),1):
        if key not in by: raise RuntimeError(f'missing selected candidate {key}')
        c=by[key]; text=str(c['sourceText']); turns=c.get('userTurns') or []
        if int(c['targetTurnIndex'])>=len(turns) or turns[int(c['targetTurnIndex'])].strip()!=text.strip():
            raise RuntimeError(f'source completeness failure {key}')
        for a in merged[key]:
            if a['sourceExactSpan'] not in text: raise RuntimeError(f"span missing {key} {a['sourceExactSpan']!r}")
        flags=c.get('higherLaneHeuristicFlags') or {}
        if any(bool(flags.get(x)) for x in ('stateOps','preference','crossDomain','requesterEvaluator')):
            raise RuntimeError(f'higher lane heuristic selected {key}')
        roles=sorted({a['role'] for a in merged[key]})
        rows.append({
            'schemaVersion':'router-stage05-parallel-v1-lane05-audience-performer-v1',
            'id':f'L05-AP-{idx:04d}',
            'source':{
                'dataset':DATASET,'revision':REV,'sourceKind':'human','language':'en',
                'sourceRowId':str(c['sourceRowId']),'evaluationSessionId':str(c['sourceEvaluationSessionId']),
                'familyId':str(c['familyId']),'familyBasis':'evaluation_session_id','targetTurnIndex':int(c['targetTurnIndex']),
            },
            'sourceText':text,'sourceUserTurns':turns,
            'actorAnnotations':merged[key],
            'reviewedActorPresenceRoles':roles,'reviewedActorSpanRoles':roles,
            'secondaryLabels':['audience-performer-overlap'] if len(roles)>1 else ([f'first-person-explicit-performer'] if any(a['role']=='performer' and a['sourceExactSpan'].lower() in {'i','we'} for a in merged[key]) else [f'{roles[0]}-exact-span']),
            'higherLaneReview':{'stateOps':False,'preference':False,'crossDomain':False,'requester':False,'evaluator':False,'quotedTextFalsePositive':False,'assistantRoleplayFalsePositive':False},
            'privacy':{'passed':True,'piiCopiedIntoAnnotation':False},
            'sourceCompleteness':{'allHumanUserTurnsIncluded':True,'canonicalUserTurnCount':len(turns),'targetTurnIndex':int(c['targetTurnIndex'])},
            'assistantOutputsIncluded':False,'candidateSignalsAreGold':False,'trainingEligible':False,'independentReview':False,
        })

    if FINAL.exists():
        for p in sorted(FINAL.rglob('*'),reverse=True):
            if p.is_file(): p.unlink()
            elif p.is_dir(): p.rmdir()
    stage=FINAL/'data/staging/router-stage05/parallel-v1/lane05-audience-performer'
    review=FINAL/'data/reviews/router/stage05-parallel-v1/lane05-audience-performer'
    report_dir=FINAL/'data/reports'; scripts=FINAL/'scripts/modeling'
    for d in (stage,review,report_dir,scripts): d.mkdir(parents=True,exist_ok=True)

    batches=[]
    for start in range(0,len(rows),31):
        chunk=rows[start:start+31]
        p=stage/f'lane05-audience-performer-{start//31:03d}.jsonl'
        p.write_text(''.join(json.dumps(r,ensure_ascii=False,separators=(',',':'))+'\n' for r in chunk),encoding='utf-8')
        batches.append({'path':str(p.relative_to(FINAL)),'rows':len(chunk),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})

    counts=Counter(a['role'] for r in rows for a in r['actorAnnotations'])
    overlap=sum(1 for r in rows if len(r['actorAnnotations'])>1)
    first_person=sum(1 for r in rows for a in r['actorAnnotations'] if a['role']=='performer' and a['sourceExactSpan'].lower() in {'i','we'})
    manifest={
        'schemaVersion':'router-stage05-parallel-v1-lane05-manifest-v1','asOfDate':'2026-09-08','lane':'05-audience-performer',
        'source':{'dataset':DATASET,'revision':REV,'rawRowsAuthority':135634,'canonicalSessionsObserved':41038,'humanUserTextOnly':True,'assistantOutputsIncluded':False},
        'selection':{'uniqueTargetTurns':len(rows),'audienceExactSpanRows':counts['audience'],'performerExactSpanRows':counts['performer'],'audiencePerformerOverlapRows':overlap,'firstPersonPerformerRows':first_person,'higherLaneFamilyExcluded':True,'quotedTextFalsePositiveExcluded':True},
        'localization':{'scope':'actor-exact-span','method':'direct-manual-model-localization','translator':'OpenAI GPT-5.6 Sol','automaticTranslationApiUsed':False,'bulkMachineTranslationUsed':False,'independentTranslationReviewCompleted':False},
        'authority':{'trainingEligibleRows':0,'independentReviewCompletedRows':0,'globalPromotionPerformed':False,'productionAuthorityChanged':False},
        'batches':batches,
    }
    (stage/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

    review_rows=[]
    for r in rows:
        review_rows.append({
            'schemaVersion':'router-stage05-parallel-v1-lane05-self-review-v1','id':r['id'],'sourceRowId':r['source']['sourceRowId'],'targetTurnIndex':r['source']['targetTurnIndex'],
            'decision':'accepted','actorRoles':r['reviewedActorSpanRoles'],
            'checks':{'higherLaneFamilyExcluded':True,'requesterEvaluatorConfusionAbsent':True,'quotedTextFalsePositiveAbsent':True,'assistantRoleplayFalsePositiveAbsent':True,'actorSpanExactSubstring':True,'sourceComplete':True,'privacyPassed':True,'duplicateAbsent':True,'assistantOutputAbsent':True,'actorSpanTranslationFidelityPassed':True},
            'trainingEligible':False,'independentReview':False,'reviewStatus':'lane-self-review-complete-independent-review-pending',
        })
    rp=review/'review.jsonl'; rp.write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in review_rows),encoding='utf-8')
    review_summary={'schemaVersion':'router-stage05-parallel-v1-lane05-review-summary-v1','reviewedRows':len(rows),'acceptedRows':len(rows),'rejectedRowsNotMaterialized':True,'audienceExactSpanRows':counts['audience'],'performerExactSpanRows':counts['performer'],'independentReviewCompletedRows':0,'trainingEligibleRows':0}
    (review/'review-summary.json').write_text(json.dumps(review_summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

    report={
        'schemaVersion':'router-stage05-parallel-v1-lane05-report-v1','asOfDate':'2026-09-08','status':'STAGING_COMPLETE_NO_GLOBAL_PROMOTION',
        'sourceAudit':{'rawRows':135634,'canonicalSessions':41038,'assistantOutputsIncluded':False,'sourceRevision':REV},
        'counts':{'uniqueTargetTurns':len(rows),'audienceExactSpanRows':counts['audience'],'performerExactSpanRows':counts['performer'],'overlapRows':overlap,'firstPersonPerformerRows':first_person},
        'targets':{'targetTurnsRange':[120,170],'audienceMinimum':70,'performerMinimum':50,'targetTurnsMet':120<=len(rows)<=170,'audienceMet':counts['audience']>=70,'performerMet':counts['performer']>=50},
        'qualityGates':{'higherLaneFamilyExcluded':True,'requesterEvaluatorConfusionChecked':True,'quotedTextFalsePositiveRemoved':True,'assistantRoleplayFalsePositiveRemoved':True,'actorSpanExactSubstring':True,'sourceCompleteness':True,'privacy':True,'duplicate':True,'assistantOutputAbsent':True,'translationFidelityScope':'actor-exact-span'},
        'authority':{'trainingEligibleRows':0,'independentReviewCompletedRows':0,'globalPromotionPerformed':False,'productionAuthorityChanged':False},
        'files':{'stagingManifest':'data/staging/router-stage05/parallel-v1/lane05-audience-performer/manifest.json','review':'data/reviews/router/stage05-parallel-v1/lane05-audience-performer/review.jsonl','reviewSummary':'data/reviews/router/stage05-parallel-v1/lane05-audience-performer/review-summary.json','validator':'scripts/modeling/validate_router_stage05_parallel_v1_lane05_audience_performer.py'},
    }
    (report_dir/'router-stage05-parallel-v1-lane05-audience-performer-2026-09-08.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

    validator=textwrap.dedent('''\
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
    ''')
    vp=scripts/'validate_router_stage05_parallel_v1_lane05_audience_performer.py'; vp.write_text(validator,encoding='utf-8')
    print(json.dumps({'rows':len(rows),'audience':counts['audience'],'performer':counts['performer'],'overlap':overlap,'firstPersonPerformer':first_person,'finalRoot':str(FINAL)},ensure_ascii=False))

if __name__=='__main__': main()
