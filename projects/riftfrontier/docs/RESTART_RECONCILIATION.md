# Riftfrontier — Restart Reconciliation Contract

이 문서는 M2 Region 01의 active expedition과 process-local encounter ownership이 서버 재시작 경계를 만났을 때의 권위 규칙을 고정한다.

## 검증 기준

기준 코드 커밋: `eef82853220ba36aa1d6d2096293541fc5c92c41`

GitHub Actions `Build Riftfrontier` run: `34109970161`

검증 결과:

- clean / unit test / build: PASS
- required native GameTest gate: PASS
- dedicated server smoke: PASS
- Xvfb client smoke: PASS
- executable JAR inspection: PASS
- report / deliverable upload: PASS

검증 JAR SHA-256:

`f42cff32667fa5aab72fb31d196a3d03aff2c265746de8041eed4d169716d490`

## 문제 정의

Region 01 technical encounter는 같은 server process 안에서 `runSequence → Mob handles`를 직접 추적한다. 이 방식은 장거리 lure와 terminal cleanup에는 안전하지만 JVM/server restart를 넘을 수 없다.

반면 `ExpeditionRun`은 `RiftfrontierWorldData` SavedData에 저장되며, technical proxy entity도 Minecraft entity persistence에 의해 저장될 수 있다. 따라서 재시작 뒤에는 다음 두 상태가 서로 다른 수명주기를 갖는다.

```text
SavedData ExpeditionRun        durable
RUN_THREATS direct handles     process-local
Tagged technical proxy entity  world-persisted, chunk-load dependent
```

process-local ownership이 사라진 뒤 남아 있는 entity를 추정 복구하여 정상 원정을 계속하면 보상, patrol-clear, cleanup 권위를 증명할 수 없다.

## authoritative policy

M2에서는 불확실한 복구보다 보수적 실패를 선택한다.

```text
server starts
→ authoritative world SavedData load
→ persisted non-terminal expedition 존재 여부 확인
→ 존재하면 ExpeditionLifecycle.fail(...)
→ run = FAILED 저장
→ preparation supply는 환불하지 않음
→ 새 원정 시작을 다시 허용
```

재시작 전에 이미 지불한 원정 준비 비용은 실제 세계 사실이므로 rollback하지 않는다.

`PREPARING`, `DEPLOYED`, `EXTRACTION_REQUESTED`처럼 terminal이 아닌 저장 상태를 임의로 성공/복구 상태로 추정하지 않는다.

## persisted proxy cleanup

Region 01 technical proxy는 생성 시 다음 stable entity tag를 가진다.

```text
riftfrontier.region01.run.<sequence>
riftfrontier.region01.role.<role>
```

같은 server process에서 정상 생성된 proxy는 `RUN_THREATS`에 해당 run sequence가 존재하므로 유효하다.

재시작 이후 저장된 proxy가 청크와 함께 다시 로드되면 process-local tracker는 존재하지 않는다. `EntityJoinLevelEvent`에서 run tag를 읽고 tracker가 없는 tagged proxy를 즉시 stale technical state로 제거한다.

이 정책은 다음을 금지한다.

- 서버 시작 시 전체 월드 entity scan
- 매 tick 전체 loaded entity scan
- 위치 반경 검색으로 run ownership 추정
- 이름/엔티티 타입만으로 Riftfrontier proxy 추정

cleanup은 **entity가 실제로 로드되는 event**에만 반응한다.

## same-process ownership rule

현재 process 안에서는 direct handle이 authoritative encounter ownership이다.

- live tracked proxy가 technical cell 밖으로 이동해도 patrol-clear를 막는다.
- terminal extraction/failure/abort에서 tracked proxy를 위치와 무관하게 discard한다.
- run tag는 restart orphan 판별용 durable breadcrumb이며, 같은-process combat 판정에서 spatial scan을 대체하는 두 번째 registry가 아니다.

## verification contract

required native GameTest `restart_reconciliation`은 최소 다음을 검증한다.

1. authoritative preparation supply를 소비한다.
2. persisted expedition을 생성하고 `DEPLOYED`로 만든다.
3. restart reconciliation을 호출한다.
4. 같은 sequence가 `FAILED`가 되는지 확인한다.
5. authoritative non-terminal expedition이 0개인지 확인한다.
6. 이미 소비한 preparation supply가 환불되지 않는지 확인한다.
7. process-local tracker가 없는 stable run-tag technical proxy를 실제 GameTest `ServerLevel`에 넣는다.
8. entity-load event 경계에서 해당 proxy가 거부 또는 discard되는지 확인한다.

CI의 GameTest gate는 단순 exit code가 아니라 non-zero test execution marker와 required-tests-passed marker를 모두 요구한다.

## 아직 자동 검증만으로 완료라고 부르지 않는 것

이 문서가 닫는 것은 **서버 권위 상태와 technical proxy의 restart consistency**다.

다음은 실제 client field-play로 검수해야 한다.

- 재시작 뒤 플레이어가 technical field 위치에서 다시 접속할 때의 UX
- logout/death/restart 후 재진입 동선
- 실패 메시지와 다음 원정 준비 흐름의 이해 가능성
- 실제 combat pacing / spawn spacing / aggro
- salvage hazard 체감
- 빠른 extraction과 patrol suppression 보상의 선택 압력

필요하면 field-play 결과로 안전한 재진입/거점 복귀 adapter를 추가하되, 실제 이벤트 의미를 감추는 무조건 순간이동으로 때우지 않는다.

## 다음 경계

restart reconciliation을 다시 설계하지 않는다.

다음 작업은 실제 Minecraft client에서 반복 원정을 플레이하며 M2 vertical slice의 체감 품질과 edge UX를 검수하고, 그 결과를 근거로 수치·동선·피드백을 조정하는 것이다. 이후 M3 production combat/elite/boss presentation 전에 별도 reference dossier를 작성한다.
