# Riftfrontier — Field-Play Evidence Metrics

이 문서는 M2 Region 01 수동 field-play에서 persisted evidence trail을 비교 가능한 수치로 읽는 규칙을 잠근다.

## 목적

`/riftfrontier expedition review trail`은 원정 lifecycle의 개별 checkpoint를 보여준다. 이번 계층은 그 trail을 다시 조작하지 않고 다음 값을 순수하게 요약한다.

- 첫 salvage까지 걸린 tick
- 실제 성공 extraction의 pre-extraction까지 걸린 tick
- persisted pre-extraction checkpoint 수
- terminal까지 걸린 tick
- salvage 간 최소/최대 간격
- 관측된 live threat 최소/최대
- terminal checkpoint에서 실제로 관측된 live threat 수
- 마지막 관측 salvage
- terminal stage / end reason

인게임 명령:

```text
/riftfrontier expedition review metrics
```

## 해석 규칙

이 수치는 **전투가 재미있는지, 공정한지, 읽기 쉬운지 자동 판정하지 않는다.**

저압/고압 원정을 실제로 플레이한 뒤 같은 시나리오끼리 다음처럼 비교하기 위한 증거다.

```text
low pressure metrics
↕ compare
high pressure metrics
```

예를 들어 고압에서 `firstSalvageTicks`와 salvage interval이 크게 늘었다면 압박이 실제 진행 속도에 영향을 준 증거는 된다. 그러나 그것이 좋은 난이도 상승인지, 부당한 aggro 때문인지는 사람이 실제 화면과 플레이로 판단해야 한다.

## extraction checkpoint 의미

현재 production adapter는 extraction을 다음 순서로 처리한다.

```text
DEPLOYED run
→ non-mutating validateExtractionRequest
→ 실패: 상태/evidence/SavedData 변경 없음
→ 성공: PRE_EXTRACTION evidence 기록
→ EXTRACTION_REQUESTED
→ resolve → EXTRACTED
```

따라서 새 runtime에서 salvage가 부족한 rejected extraction command는 `PRE_EXTRACTION`을 만들지 않는다. contract gate를 통과한 실제 철수 경계만 evidence에 기록된다.

다만 이 수정 이전에 저장된 schema-3 run에는 rejected attempt가 `PRE_EXTRACTION`으로 남아 있을 수 있다. backward compatibility를 위해 metric 계층은 과거 trail을 계속 보수적으로 읽는다.

- `extractionAttempts`: persisted `PRE_EXTRACTION` checkpoint의 총 개수. 새 runtime에서는 일반적으로 accepted extraction 수와 같고, legacy trail에서는 rejected 흔적이 포함될 수 있다.
- active/failed legacy run: `preExtractionTicks=unavailable`
- successfully `EXTRACTED` legacy run: 여러 `PRE_EXTRACTION`이 있으면 마지막 checkpoint만 accepted pre-extraction으로 사용
- 성공 전의 더 이른 legacy checkpoint는 성공 pacing 값을 앞당기지 않음

즉 새 write path는 오염을 만들지 않고, metric read path는 이미 존재하는 과거 오염도 과장하지 않는다.

## unavailable 규칙

관측되지 않은 값은 `0`으로 만들지 않고 `unavailable`로 출력한다.

특히 server restart는 이전 프로세스의 terminal live-threat 수를 증명할 수 없으므로 `terminalLiveThreats=unavailable`을 유지한다. legacy run에 persisted evidence trail이 없다면 pacing/threat metric도 재구성하지 않는다.

## 데이터 경계

`FieldPlayEvidenceMetrics`는 순수 Java read-only 분석이다.

- world state 변경 없음
- expedition lifecycle 변경 없음
- 새로운 persistence field 없음
- broad entity/world scan 없음
- current balance formula로 과거 사실 재구성 없음

즉 기존 bounded `ExpeditionEvidenceCheckpoint`만 입력으로 사용한다.

## M2 수동 gate에서의 사용

각 field-play 시나리오에서 권장 순서:

```text
1. /riftfrontier expedition review checklist
2. 실제 원정 수행
3. /riftfrontier expedition review
4. /riftfrontier expedition review trail
5. /riftfrontier expedition review metrics
6. 체감 문제와 run 번호를 함께 기록
```

조정은 `관측된 체감 문제 + 해당 run의 trail/metrics`가 같이 있을 때만 한다.

자동 metric 생성 자체는 M2 field-play 완료 조건이 아니다.
