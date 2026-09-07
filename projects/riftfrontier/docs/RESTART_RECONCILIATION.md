# Riftfrontier — Restart Reconciliation Contract

이 문서는 M2 Region 01의 persisted expedition과 process-local encounter ownership이 서버 재시작 경계를 만났을 때의 권위 규칙을 고정한다.

## 최신 검증 기준

기준 코드 커밋: `19df3e82339f7b1c5d18413e73e47d01ab91fa68`

GitHub Actions `Build Riftfrontier` run: `34146353605`

검증 결과:

- clean / unit test / build: PASS
- required native GameTest gate: PASS (`4 tests`, all required passed)
- dedicated server smoke: PASS
- Xvfb client smoke: PASS
- executable JAR inspection: PASS
- report / deliverable upload: PASS

검증 JAR SHA-256:

`168a5cf55df824065efdcf89ec9110fff183400aafc78a07ccb6fcfae4ccc77d`

## 문제 정의

Region 01 technical encounter는 같은 server process 안에서 `runSequence → Mob handles`를 직접 추적한다. 이 방식은 장거리 lure와 terminal cleanup에는 안전하지만 JVM/server restart를 넘을 수 없다.

반면 `ExpeditionRun`은 `RiftfrontierWorldData` SavedData에 저장되며, technical proxy entity도 Minecraft entity persistence에 의해 저장될 수 있다. 따라서 재시작 뒤에는 다음 상태가 서로 다른 수명주기를 갖는다.

```text
SavedData ExpeditionRun        durable
RUN_THREATS direct handles     process-local
Tagged technical proxy entity  world-persisted, chunk-load dependent
```

process-local ownership이 사라진 뒤 남아 있는 entity를 추정 복구하여 정상 원정을 계속하면 보상, patrol-clear, cleanup 권위를 증명할 수 없다.

또한 정상 M2 runtime은 세계 전체에서 non-terminal expedition을 최대 1개만 허용하지만, restart recovery는 과거 버그·migration·부분 저장·비정상 fixture 때문에 persisted list 자체가 이 최신 불변식을 이미 만족한다고 가정해서는 안 된다. 이전 구현은 가장 최신 non-terminal run 하나만 실패시켰기 때문에 저장 데이터에 둘 이상 남아 있으면 오래된 active run이 재시작 이후에도 살아남을 수 있었다.

## authoritative policy

M2에서는 불확실한 복구보다 보수적 실패를 선택한다.

```text
server starts
→ authoritative world SavedData load
→ persisted expedition list에서 모든 non-terminal run 수집
→ sequence 오름차순으로 결정적 reconciliation
→ 각각 FAILED / endReason=SERVER_RESTART 저장
→ 각각 terminal FAILED evidence checkpoint 저장
→ preparation supply는 환불하지 않음
→ reconciliation 종료 후 persisted non-terminal run = 0
→ 새 원정 시작을 다시 허용
```

대상은 `PREPARING`, `DEPLOYED`, `EXTRACTION_REQUESTED`를 포함한 모든 non-terminal 상태다. 이미 terminal인 `EXTRACTED`, `FAILED` 이력은 건드리지 않는다.

재시작 전에 이미 지불한 원정 준비 비용은 실제 세계 사실이므로 rollback하지 않는다.

### cardinality hardening

정상 gameplay path에서 active expedition이 하나뿐이라는 사실과, persistence recovery가 오염된 historical state를 견딜 수 있어야 한다는 요구는 별개다.

따라서 server-start reconciliation은 `latest active` 한 건을 찾는 대신 persisted expedition list 전체에서 non-terminal run을 찾는다. 이 작업은 서버 시작 때 한 번, 저장된 원정 이력 크기에 대해서만 bounded하게 실행된다.

금지:

- per-tick reconciliation scan
- 전체 entity/world scan
- 위치 반경 검색으로 run ownership 추정
- 가장 최신 run 하나만 닫고 나머지를 정상이라고 가정

### content-independent failure edge

Restart cleanup은 현재 content snapshot에 남아 있는 region/contract 정의가 존재해야만 실행되는 gameplay action이 아니다. historical run의 content fingerprint가 오래되었거나 현재 pack에서 정의가 제거·교체됐더라도 durable non-terminal 사실은 반드시 닫을 수 있어야 한다.

따라서 restart reconciler는 현재 content registry를 다시 검증하는 `ExpeditionLifecycle.fail(...)`에 의존하지 않고, persisted `ExpeditionRun` 자체의 합법적인 terminal transition인 `run.fail(..., SERVER_RESTART)`를 사용한다.

이 경계가 보장하는 것:

- stale content reference가 server-start cleanup을 deadlock시키지 않는다.
- restart 실패 이유는 content validation 실패로 왜곡되지 않는다.
- owner UUID, start context, recovered resources, 기존 field evidence는 immutable run transition을 통해 보존된다.

## restart evidence policy

재시작 시 이전 JVM의 direct Mob handles는 존재하지 않으므로 live threat 수를 복원했다고 꾸미지 않는다.

각 reconciled run에는 `FAILED` terminal evidence checkpoint를 남기며 `liveThreats=-1`은 `unavailable`을 의미한다. salvage/supply/pressure는 server-start 시점 authoritative world state를 기록한다.

이 terminal evidence는 diagnostic 정보이며 gameplay 성공/보상 판정을 대체하지 않는다.

## persisted proxy cleanup

Region 01 technical proxy는 생성 시 다음 stable entity tag를 가진다.

```text
riftfrontier.region01.run.<sequence>
riftfrontier.region01.role.<role>
```

같은 server process에서 정상 생성된 proxy는 `RUN_THREATS`에 해당 run sequence가 존재하므로 유효하다.

재시작 이후 저장된 proxy가 청크와 함께 다시 로드되면 process-local tracker는 존재하지 않는다. `EntityJoinLevelEvent`에서 run tag를 읽고 tracker가 없는 tagged proxy를 stale technical state로 제거한다.

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

자동 검증은 서로 다른 경계를 담당한다.

### unit regression

`ExpeditionRestartReconcilerTest`는 mixed persisted history에서 다음을 검증한다.

1. `PREPARING`, `DEPLOYED`, `EXTRACTION_REQUESTED`가 모두 reconciliation 대상으로 선택된다.
2. `EXTRACTED`, `FAILED`는 선택되지 않는다.
3. 입력 순서와 관계없이 sequence 오름차순의 결정적 순서를 사용한다.
4. terminal history만 있는 세계에서는 대상이 0개다.

### required native GameTest

기존 `restart_reconciliation` GameTest는 실제 SavedData/ServerLevel 경계에서 최소 다음을 검증한다.

1. authoritative preparation supply를 소비한다.
2. persisted expedition을 생성하고 `DEPLOYED`로 만든다.
3. restart reconciliation을 호출한다.
4. 같은 sequence가 `FAILED`가 되는지 확인한다.
5. authoritative non-terminal expedition이 0개인지 확인한다.
6. 이미 소비한 preparation supply가 환불되지 않는지 확인한다.
7. process-local tracker가 없는 stable run-tag technical proxy를 실제 GameTest `ServerLevel`에 넣는다.
8. entity-load event 경계에서 해당 proxy가 거부 또는 discard되는지 확인한다.

최신 CI에서는 총 4개의 required GameTest가 실제 실행됐고 모두 통과했다. CI gate는 단순 exit code가 아니라 non-zero test execution marker와 required-tests-passed marker를 모두 요구한다.

### runtime/build verification

최신 검증 run `34146353605`에서 다음 단계가 모두 성공했다.

- Java 25 / Gradle 9.2.1 toolchain
- `clean test build`
- native GameTest gate
- dedicated server ready-state smoke
- Xvfb client initialization smoke
- executable JAR unzip/inventory 검사
- source `.java` leak 부재 검사
- deliverable/report/log artifact upload

CI head는 `19df3e82339f7b1c5d18413e73e47d01ab91fa68`이며 executable JAR 안에 `ExpeditionRestartReconciler.class`가 포함된 것도 JAR inspection으로 확인됐다.

## 아직 자동 검증만으로 완료라고 부르지 않는 것

이 문서가 닫는 것은 **서버 권위 상태와 technical proxy의 restart consistency 및 corrupted-cardinality recovery**다.

다음은 실제 client field-play로 검수해야 한다.

- 재시작 뒤 플레이어가 technical field 위치에서 다시 접속할 때의 UX
- logout/death/restart 후 재진입 동선과 메시지 이해 가능성
- 실제 combat pacing / spawn spacing / aggro
- salvage hazard 체감
- 빠른 extraction과 patrol suppression 보상의 선택 압력

field-play 결과 없이 수치나 연출을 임의로 최종 확정하지 않는다.

## 다음 경계

restart reconciliation/cardinality를 추가 근거 없이 다시 설계하지 않는다.

다음 작업은 실제 Minecraft client에서 저압·고압 Region 01을 반복 플레이하며 persisted review/evidence trail과 함께 M2 vertical slice의 체감 품질을 검수하는 것이다. 구체적인 관측 문제와 해당 run 증거가 함께 있는 항목만 수치·동선·피드백을 조정한다. 이 수동 gate를 닫은 뒤 M3 production combat/elite/boss presentation 전에 별도 reference dossier를 작성한다.
