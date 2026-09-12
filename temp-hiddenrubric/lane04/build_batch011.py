import json
from pathlib import Path

TARGET='a5bee9ea-656a-4ab4-9185-d1976e279902'
TURN=0
SESSION='d7870119-edee-4f4e-9eab-c2f846da5027'
SRC=Path('temp-hiddenrubric/lane04/decision-evaluator-v3/candidates.jsonl')
OUT=Path('temp-hiddenrubric/lane04/batch011')
OUT.mkdir(parents=True,exist_ok=True)

found=None
for line in SRC.read_text(encoding='utf-8').splitlines():
    if not line.strip():
        continue
    x=json.loads(line)
    if x.get('sourceRowId')==TARGET and x.get('turnIndex')==TURN:
        found=x
        break
assert found is not None
assert found['sourceEvaluationSessionId']==SESSION
source=found['sourceText']
assert 'Make a template response for this job posted on upwork' in source
assert 'convince the client' in source
assert source.count('client')==1

ko='''업워크에 올라온 이 일자리 공고에 대한 답변 템플릿을 만들어줘. 너무 길게 하지 말고 2~3문단만 쓰고, 쉬운 영어로 작성해서 클라이언트를 설득할 수 있게 해줘:

"AI 도구 활용에 능숙한 고객 서비스 업무용 가상 비서

일상 업무, 특히 이메일 고객 서비스에서 AI 도구를 능숙하게 활용할 수 있고 매우 체계적인 가상 비서를 찾고 있습니다. 이상적인 지원자는 AI 기술을 이용해 의사소통을 간소화하고, 고객 문의를 효율적으로 관리하며, 응답 시간을 개선할 수 있어야 합니다. 기술을 활용해 생산성을 높이고 훌륭한 고객 지원을 제공하는 데 열정이 있다면 여러분의 지원을 기다립니다!"'''

def span(hay,needle):
    s=hay.index(needle)
    return {'start':s,'end':s+len(needle),'text':needle}

src_span=span(source,'client')
ko_span=span(ko,'클라이언트')
assert (src_span['start'],src_span['end'])==(141,147)
assert source[src_span['start']:src_span['end']]=='client'
assert ko[ko_span['start']:ko_span['end']]=='클라이언트'

row={
  'schemaVersion':'router-stage05-parallel-v1-lane04-requester-evaluator-v1',
  'lane':'lane04-requester-evaluator',
  'sourceDataset':'lmarena-ai/arena-human-preference-140k',
  'sourceRevision':'a9cb587ee0906192dc1fc5e51778282f36c6bf35',
  'sourceArtifact':'pinned-parquet-decision-evaluator-v3:public-run-34687688354',
  'sourceEvaluationSessionId':SESSION,
  'sourceRowId':TARGET,
  'familyIndex':None,
  'kind':'user',
  'turnIndex':TURN,
  'sourceLanguage':'en',
  'targetLanguage':'ko',
  'source_en':source,
  'localized_ko':ko,
  'assistantOutputsIncluded':False,
  'sourceComplete':True,
  'translationReviewed':True,
  'semanticReviewed':True,
  'existingStage05DuplicateChecked':True,
  'privacyFlags':[],
  'ownership':{'stateOps':False,'preference':False,'crossDomain':False},
  'actors':{
    'requester':{'present':False,'sourceSpans':[],'koreanSpans':[],'relationEvidence':[]},
    'evaluator':{'present':True,'sourceSpans':[src_span],'koreanSpans':[ko_span],'relationEvidence':['submit_to_actor']}
  },
  'reviewStatus':'manual-semantic-reviewed-staging',
  'trainingEligible':False,
  'humanGold':False,
  'factualGold':False,
  'domainExpertGold':False
}

reviews=[
 {'sourceRowId':TARGET,'turnIndex':0,'accepted':True,'role':'evaluator','actorText':['client'],'relationEvidence':'submit_to_actor','reason':'requested_upwork_application_response_is_explicitly_written_to_convince_the_job_client','notes':'The requested artifact is the Upwork application response itself. The client is the decision actor selecting a provider, not merely a generic audience.'},
 {'sourceRowId':'84e6fb61-cc3d-46c1-b350-ce15536f47b2','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['panel'],'reason':'lexical_panel_false_positive'},
 {'sourceRowId':'bf900ee9-28c5-4481-9020-5bff571133cf','turnIndex':1,'accepted':False,'role':'evaluator','actorText':['directors'],'reason':'persuasion_advice_not_artifact_submitted_for_review'},
 {'sourceRowId':'957335f1-56ab-4a6c-b0f6-23e2709ca3b6','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['management'],'reason':'assistant_persona_and_lexical_false_positive'},
 {'sourceRowId':'da848dae-5f18-4d34-8c5c-4c892420e6ed','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['clients'],'reason':'customer_recipient_decision_without_explicit_output_review','notes':'Renewal responses persuade clients, but the source does not explicitly establish review/evaluation of the requested output; kept out for precision.'},
 {'sourceRowId':'9e88b8ad-fa51-4276-9571-53ddde8e86f5','turnIndex':1,'accepted':False,'role':'evaluator','actorText':['teacher marker'],'reason':'supplied_assignment_context_not_current_requested_output'}
]

report={
 'schemaVersion':'router-stage05-parallel-v1-lane04-report-v1',
 'lane':'lane04-requester-evaluator','date':'2026-09-12',
 'source':{'dataset':'lmarena-ai/arena-human-preference-140k','revision':'a9cb587ee0906192dc1fc5e51778282f36c6bf35','rawRows':135634,'humanUserTurnsOnly':True,'assistantOutputsUsed':False},
 'batch':{'acceptedRows':1,'requesterPositiveRows':0,'requesterExactSpans':0,'evaluatorPositiveRows':1,'evaluatorExactSpans':1,'rawHeuristicCandidateAnnotationsReviewed':6,'rawHeuristicCandidateAnnotationsAccepted':1,'falsePositiveOrHigherLaneAnnotationsRemoved':5,'manualRescueActorAnnotations':1},
 'ownership':{'stateOpsExcluded':True,'preferenceExcluded':True,'crossDomainExcluded':True},
 'duplicateCheck':{'baseHead':'9538b47a6c261ade711727bcdb6f721d7ab60341','githubCodeSearchHitCount':0,'selectedRowsUnique':True,'sourceKeys':[TARGET+':0']},
 'validator':{'status':'PASS'},
 'notes':['The requested Upwork application response is explicitly written to convince the client who decides whether to engage the applicant.','Five nearby decision/persuasion candidates remain explicit rejections rather than relaxed into evaluator gold.','No assistant/model output was used as source data.']
}

# Fail-closed validation equivalent to the Lane04 per-batch invariants used by the repo.
assert row['schemaVersion']=='router-stage05-parallel-v1-lane04-requester-evaluator-v1'
assert row['lane']=='lane04-requester-evaluator'
assert row['sourceDataset']=='lmarena-ai/arena-human-preference-140k'
assert row['sourceRevision']=='a9cb587ee0906192dc1fc5e51778282f36c6bf35'
assert row['kind']=='user' and row['assistantOutputsIncluded'] is False
assert row['sourceComplete'] and row['translationReviewed'] and row['semanticReviewed'] and row['existingStage05DuplicateChecked']
assert all(row['ownership'][k] is False for k in ('stateOps','preference','crossDomain'))
assert row['actors']['evaluator']['present'] is True
assert row['actors']['evaluator']['relationEvidence']==['submit_to_actor']
assert len(row['actors']['evaluator']['sourceSpans'])==len(row['actors']['evaluator']['koreanSpans'])==1
for hay,s in ((source,src_span),(ko,ko_span)):
    assert hay[s['start']:s['end']]==s['text']

staging=OUT/'staging-batch011-evaluator-expansion.jsonl'
review=OUT/'review-batch011-evaluator-expansion.jsonl'
rep=OUT/'router-stage05-parallel-v1-lane04-requester-evaluator-batch011-evaluator-expansion-2026-09-12.json'
staging.write_text(json.dumps(row,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
review.write_text(''.join(json.dumps(r,ensure_ascii=False,separators=(',',':'))+'\n' for r in reviews),encoding='utf-8')
rep.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(OUT/'VALIDATION.txt').write_text(json.dumps({'status':'PASS','rows':1,'requesterPositiveRows':0,'requesterExactSpans':0,'evaluatorPositiveRows':1,'evaluatorExactSpans':1,'falsePositiveOrHigherLaneAnnotationsRemoved':5},ensure_ascii=False)+'\n',encoding='utf-8')
print((OUT/'VALIDATION.txt').read_text())
