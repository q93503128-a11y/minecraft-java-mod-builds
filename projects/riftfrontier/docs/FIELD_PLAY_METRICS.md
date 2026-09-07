# Riftfrontier — Field-Play Evidence Metrics

이 문서는 M2 Region 01 수동 field-play에서 persisted evidence trail을 비교 가능한 수치로 읽는 규칙을 잠근다.

## 목적

`/riftfrontier expedition review trail`은 원정 lifecycle의 개별 checkpoint를 보여준다. 이번 계층은 그 trail을 다시 조작하지 않고 다음 값을 순수하게 요약한다.

- 첫 salvage까지 걸린 tick
- 실제 성공 extraction의 pre-extraction까지 걸린 tick
- extraction attempt checkpoint 수
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

## extraction attempt 의미

현재 M2 adapter는 extraction 명령이 호출되면 contract 요구조건 검증 전에 `PRE_EXTRACTION` checkpoint를 남길 수 있다. 따라서 salvage가 부족해 authoritative run이 계속 `DEPLOYED`로 남는 거부된 시도도 trail에는 attempt 흔적으로 남을 수 있다.

metric 계층은 이 흔적을 실제 성공 철수 시간처럼 해석하지 않는다.

- `extractionAttempts`: persisted `PRE_EXTRACTION` checkpoint의 총 개수
- active/failed run: `preExtractionTicks=unavailable`
- successfully `EXTRACTED` run: 마지막 `PRE_EXTRACTION` checkpoint만 accepted pre-extraction으로 사용
- 성공 전의 더 이른 `PRE_EXTRACTION` checkpoint는 rejected attempt로 간주하며 성공 pacing 값을 앞당기지 않음

따라서 반복된 underfilled extraction 시도가 있어도 `preExtractionTicks`는 최초 거부 시점이 아니라 실제 성공 철수 직전 시점을 가리킨다. 이 규칙은 기존 persisted trail을 깨거나 추정값을 새 사실로 만들지 않고 읽기 단계에서 의미를 보존한다.

## unavailable 규칙

관측되지 않은 값은 `0`으로 만들지 않고 `unavailable`로 출력한다.

특히 server restart는 이전 프로세스의 terminal live-threat 수를 증명할 수 없으므로 `terminalLiveThreats=unavailable`을 유지한다. legacy run에 persisted evidence trail이 없다면 pacing/threat metric도 재구성하지 않는다.

거부된 extraction attempt 역시 authoritative extraction 상태 전이가 아니므로 active/failed run에서 `preExtractionTicks`를 만들어내지 않는다.

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
