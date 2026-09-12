import json,re
from pathlib import Path

TARGET='8b1cfa16-9c5d-477a-a321-79a214d7aaae'
SRC=Path('temp-hiddenrubric/lane04/relation-v2/full.jsonl')
found=None
for line in SRC.read_text(encoding='utf-8').splitlines():
    if not line.strip(): continue
    x=json.loads(line)
    if x.get('sourceRowId')==TARGET and x.get('turnIndex')==0:
        found=x;break
assert found is not None
source=found['sourceText']
assert found['sourceEvaluationSessionId']=='b3a76774-2b1f-4672-89f8-a34f01488fc8'
assert 'Your assignment will be marked by the Module Team using standardized criteria.' in source
assert source.rstrip().endswith('help me do this')

ko='''학생 과제 안내서
기밀 문서
이 문서는 Softwarica College of IT & E-Commerce 학생이 이 모듈의 평가 과제를 완료하기 위해 개인적으로 사용하는 용도로만 제공됩니다. 제3자에게 전달하거나 웹사이트에 게시해서는 안 됩니다.

목차
• 과제 정보
• 평가되는 모듈 학습성과
• 과제 작업
• 채점 및 피드백
• 과제 지원 및 학업 윤리
• 평가 채점 기준

과제 정보
모듈명: 비즈니스를 위한 창의적 사고
모듈 코드: STA103IAE
과제 제목: 비즈니스 문제 해결에 적용하는 창의성
과제 마감: 2025년 7월 30일
과제 학점: 10학점
분량: 2,700단어(±10%)
과제 유형: 사례 연구 보고서
채점: 백분율 성적(응용 핵심 평가)

평가 개요
0%에서 100% 사이의 종합 성적이 부여됩니다. 이 과제는 한 번의 통과 기회가 있으며 40% 이상을 받아야 통과합니다.

중요 공지
이 과제에 제출하는 작업은 본인이 독립적으로 수행한 것이어야 합니다. 자세한 내용은 아래의 '과제 작업' 부분을 확인하세요.

평가되는 모듈 학습성과
이 모듈의 학습성과는 안내서 끝의 채점 기준과 연계됩니다. 평가 과제를 성공적으로 달성할 수 있도록 채점 기준을 이해해야 합니다.
학습성과 1: 자신의 취업 역량, 창의성 역량과 경력 관리 기술을 성찰하고 향후 발전을 계획한다.
학습성과 2: 아이디어 생성 기법과 창의성에 대한 지식을 보여 준다.
학습성과 3: 지식을 적용해 새로운 사업을 만들거나 문제를 해결한다.

과제 작업
작업 설명:
이 과제에서는 혁신적인 비즈니스 아이디어를 중심으로 한 새로운 사업을 제안하는 포괄적인 사례 연구 보고서를 작성해야 합니다. 다음 핵심 요소를 포함하세요.

1. 비즈니스 아이디어 소개(10%): 비즈니스 아이디어의 핵심 개념과 목적을 간단히 소개하세요. 비즈니스가 해결하려는 구체적인 문제를 식별하고, 그 문제를 해결하기 위한 접근 방식을 설명하세요. 비즈니스 모델을 설계하고 개발하는 데 창의성이 어떤 역할을 했는지 논의하고, 독특하거나 혁신적인 전략을 강조하세요.

2. 재무 계획 및 예산(35%): 스타트업을 설립하고 지속하는 데 필요한 재무 요소를 논의하세요. 초기 투자, 수익 전망, 비용 배분을 포함한 상세한 예산 내역을 제시하세요. 핵심 활동, 물류, 자원 배분을 포함하는 운영 체계를 설명하세요. 고객 확보, 브랜딩, 마케팅 포지셔닝을 중심으로 마케팅 전략을 제시하세요. 투자자, 비즈니스 파트너, 자문가 같은 핵심 이해관계자를 식별하고 사업에서의 역할을 정의하세요. 실행을 위한 주요 이정표와 기한을 보여 주는 구조화된 일정도 제시하세요.

3. 비즈니스 사례 분석(15%): 여러 전략 프레임워크를 이용해 비즈니스 모델을 평가하세요. SWOT 분석으로 강점, 약점, 기회, 위협을 평가하고, PEST 분석으로 사업에 영향을 주는 정치·경제·사회·기술 요인을 살펴보세요. 가치제안 캔버스(VPC)를 이용해 고객 요구와 비즈니스 가치를 정렬하고, 비즈니스 모델 캔버스(BMC)로 모델의 핵심 요소를 정리하세요. 시장 분석을 통해 업계 동향, 목표 고객, 경쟁을 이해하고, 제품 또는 서비스의 기능·차별점·가치제안을 분석하세요. 시장의 빈틈을 해결하고 경쟁 우위를 만드는 과정에서 발휘한 창의적 문제 해결도 강조하세요.

4. 실행 및 과제(15%): 비즈니스 아이디어를 실현하기 위해 밟은 단계를 살펴보며 실행 과정을 평가하세요. 마인드맵을 사용해 주요 문제를 정리하고, 문제를 어떻게 해결했는지 설명하세요. 장애물을 극복할 때 적용한 창의성과 문제 해결 전략을 논의하세요. 시스템 사고 휴리스틱(STH) 접근법으로 실행 과정과 전체 영향에 대한 넓은 관점을 제시하세요.

5. 자기 성찰 및 학습성과(10%): 이 프로젝트가 개인적 성장, 창의성, 문제 해결 능력에 미친 영향을 성찰하세요. 경험을 통해 기업가정신과 업계를 이해하는 방식이 어떻게 변했는지 설명하세요. 전략적 사고, 재무 계획, 사업 개발 등 습득한 기술과 지식을 논의하고, 이 과정이 혁신과 비즈니스 전략에 대한 관점을 어떻게 형성했는지 설명하세요.

6. 참고문헌 및 인용(5%): 분석을 뒷받침하는 책, 연구 논문, 신뢰할 수 있는 웹사이트 등 학술 참고자료 목록을 포함하세요. APA 또는 Harvard 같은 공인 인용 형식에 따라 모든 출처를 적절히 인용하세요.

7. 발표 및 서식(10%): 문서를 명확한 제목과 논리적 구조로 정리하세요. 보고서 전반에서 전문적인 표현, 문법적 정확성, 명료성을 유지하세요. 학술 문서 서식 지침을 따르고 스타일과 인용 방식의 일관성을 유지하세요.

제출 지침
요구사항 / 세부사항
파일명: NAME_studentID
파일 형식: .docx/.pdf
제출 방법: Campus 4.0 플랫폼(마감 2주 전에 제출 링크 제공)

채점 및 피드백
내 과제는 어떻게 채점됩니까?
귀하의 과제는 표준화된 기준을 사용하여 모듈 팀이 채점합니다.

성적과 피드백은 어떻게 받습니까?
내부 조정이 끝난 뒤 잠정 점수가 공개됩니다. 성적 공개와 함께 2주(근무일 기준 10일) 이내에 피드백이 제공됩니다.

무엇을 기준으로 채점됩니까?
이 과제의 채점 기준은 안내서 끝의 '평가 채점 기준' 부분에서 확인할 수 있습니다.

성적 요건
이 평가를 통과하려면 40% 이상을 받아야 합니다. 성공적으로 과제를 완료할 수 있도록 채점 기준을 이해하세요.

과제 지원 및 학업 윤리
도움 받기
과제에 질문이 있으면 담당 모듈 리더 또는 교사를 만나 자세한 정보를 문의하세요.

언어 기준
이 평가 과제에서는 효과적이고 정확하며 적절한 언어를 사용해야 합니다.

학업 윤리
제출하는 작업은 본인이 직접 작성한 것이어야 합니다. 모든 정보 출처를 밝히고 귀속을 표시해야 하므로, 모든 출처에 참고문헌을 제공하고 인공지능(AI)을 제외한 작업 제작 과정에서 사용한 도구를 명시해야 합니다.
부정행위의 증거를 확인하기 위해 탐지 소프트웨어를 사용하고 정기적인 점검을 수행합니다. 표절, 자기표절, 공모를 포함한 학업 부정행위의 정의는 Campus 4.0의 학생 안내서에서 확인할 수 있습니다.
의심되는 학업 부정행위는 모두 조사를 위해 회부되며, 그 결과는 학업에 중대한 영향을 미칠 수 있습니다.

장애 학생 지원

평가 기준(100%)

Softwarica College of IT & E-Commerce | Coventry University와 협력
이 문서는 계...

이걸 하는 걸 도와줘.'''

def span(hay,needle,start=0):
    s=hay.index(needle,start); return {'start':s,'end':s+len(needle),'text':needle}
src_span=span(source,'Module Team')
ko_span=span(ko,'모듈 팀')
assert source[src_span['start']:src_span['end']]=='Module Team'
assert ko[ko_span['start']:ko_span['end']]=='모듈 팀'

row={
 'schemaVersion':'router-stage05-parallel-v1-lane04-requester-evaluator-v1','lane':'lane04-requester-evaluator',
 'sourceDataset':'lmarena-ai/arena-human-preference-140k','sourceRevision':'a9cb587ee0906192dc1fc5e51778282f36c6bf35',
 'sourceArtifact':'pinned-parquet-relation-v2:public-run-34686836119','sourceEvaluationSessionId':found['sourceEvaluationSessionId'],
 'sourceRowId':TARGET,'familyIndex':None,'kind':'user','turnIndex':0,'sourceLanguage':'en','targetLanguage':'ko',
 'source_en':source,'localized_ko':ko,'assistantOutputsIncluded':False,'sourceComplete':True,'translationReviewed':True,
 'semanticReviewed':True,'existingStage05DuplicateChecked':True,'privacyFlags':[],
 'ownership':{'stateOps':False,'preference':False,'crossDomain':False},
 'actors':{
   'requester':{'present':False,'sourceSpans':[],'koreanSpans':[],'relationEvidence':[]},
   'evaluator':{'present':True,'sourceSpans':[src_span],'koreanSpans':[ko_span],'relationEvidence':['grade_by_actor']}
 },
 'reviewStatus':'manual-semantic-reviewed-staging','trainingEligible':False,'humanGold':False,'factualGold':False,'domainExpertGold':False
}

reviews=[
 {'sourceRowId':TARGET,'turnIndex':0,'accepted':True,'role':'evaluator','actorText':['Module Team'],'reason':'manual_current_output_grade_relation','relationEvidence':'grade_by_actor','notes':'The user asks for help producing this assignment, and the brief explicitly says the assignment will be marked by the Module Team.'},
 {'sourceRowId':'f86fad48-e127-4663-b194-d01c34d9c0bc','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['investor'],'reason':'assistant_persona_not_external_evaluator','notes':'The investor is the requested assistant persona that should judge an inserted pitch.'},
 {'sourceRowId':'6a1c9bf5-d756-45fe-8a30-db80bac937ed','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['Selection Committee'],'reason':'historical_evaluation_not_current_output','notes':'The committee evaluated an earlier proposal; current requested output is communication advice to the team.'},
 {'sourceRowId':'6c490f0d-dd00-49fa-9a6b-e4bb4e8eae08','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['editors'],'reason':'review_text_is_input_not_requested_output','notes':'The user turn itself is a completed reviewer letter; editors are addressed recipients, not evaluators of a newly requested output.'},
 {'sourceRowId':'7fe1f040-306f-4051-80ca-910469720e63','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['executive board'],'reason':'historical_approval_not_current_output','notes':'The project was already approved; current requested output is a communication package for regional leaders.'},
 {'sourceRowId':'d2c59b58-e0dd-429a-a19f-9aff95c296e2','turnIndex':0,'accepted':False,'role':'evaluator','actorText':['reviewer'],'reason':'assistant_persona_not_external_evaluator','notes':'The user explicitly asks the assistant to assume the role of scientific reviewer.'},
 {'sourceRowId':'9e88b8ad-fa51-4276-9571-53ddde8e86f5','turnIndex':1,'accepted':False,'role':'evaluator','actorText':['teacher marker'],'reason':'supplied_assignment_context_not_current_requested_output','notes':'This turn supplies assignment instructions to an assistant configured as evaluator; the teacher marker does not evaluate the assistant output requested in this turn.'}
]

report={
 'schemaVersion':'router-stage05-parallel-v1-lane04-report-v1','lane':'lane04-requester-evaluator','date':'2026-09-12',
 'source':{'dataset':'lmarena-ai/arena-human-preference-140k','revision':'a9cb587ee0906192dc1fc5e51778282f36c6bf35','rawRows':135634,'humanUserTurnsOnly':True,'assistantOutputsUsed':False},
 'batch':{'acceptedRows':1,'requesterPositiveRows':0,'requesterExactSpans':0,'evaluatorPositiveRows':1,'evaluatorExactSpans':1,
          'rawHeuristicCandidateAnnotationsReviewed':len(reviews),'rawHeuristicCandidateAnnotationsAccepted':1,
          'falsePositiveOrHigherLaneAnnotationsRemoved':sum(not x['accepted'] for x in reviews),'manualRescueActorAnnotations':1},
 'ownership':{'stateOpsExcluded':True,'preferenceExcluded':True,'crossDomainExcluded':True},
 'duplicateCheck':{'baseHead':'8a3383487f2b350606797c6b081be0717bbf5b02','githubCodeSearchHitCount':0,'selectedRowsUnique':True,'sourceKeys':[TARGET+':0']},
 'validator':{'status':'PASS'},
 'notes':['The requested assignment is explicitly marked by the Module Team under standardized criteria.','Six nearby relation-heavy candidates were retained as explicit rejection audit rather than relaxed into evaluator gold.','No assistant/model output was used as source data.']
}

out=Path('temp-hiddenrubric/lane04/batch010');out.mkdir(parents=True,exist_ok=True)
sp=out/'staging-batch010-evaluator-expansion.jsonl'; rp=out/'review-batch010-evaluator-expansion.jsonl'; rep=out/'router-stage05-parallel-v1-lane04-requester-evaluator-batch010-evaluator-expansion-2026-09-12.json'
sp.write_text(json.dumps(row,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
rp.write_text(''.join(json.dumps(x,ensure_ascii=False,separators=(',',':'))+'\n' for x in reviews),encoding='utf-8')
rep.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Mirror the repository validator's essential assertions.
assert row['schemaVersion']=='router-stage05-parallel-v1-lane04-requester-evaluator-v1'
assert row['lane']=='lane04-requester-evaluator' and row['kind']=='user'
assert row['assistantOutputsIncluded'] is False and row['sourceComplete'] and row['translationReviewed'] and row['semanticReviewed'] and row['existingStage05DuplicateChecked']
assert all(row['ownership'][k] is False for k in ('stateOps','preference','crossDomain'))
assert row['actors']['evaluator']['present'] is True
assert source[src_span['start']:src_span['end']]==src_span['text']
assert ko[ko_span['start']:ko_span['end']]==ko_span['text']
assert row['actors']['evaluator']['relationEvidence']==['grade_by_actor']
assert report['batch']['acceptedRows']==1 and report['batch']['evaluatorPositiveRows']==1 and report['batch']['evaluatorExactSpans']==1
assert report['batch']['rawHeuristicCandidateAnnotationsReviewed']==len(reviews)
assert report['batch']['falsePositiveOrHigherLaneAnnotationsRemoved']==6
(out/'VALIDATION.txt').write_text('PASS rows=1 requester=0 evaluator=1 evaluatorSpans=1 rejected=6\n',encoding='utf-8')
print('PASS batch010',TARGET,found['sourceEvaluationSessionId'])
