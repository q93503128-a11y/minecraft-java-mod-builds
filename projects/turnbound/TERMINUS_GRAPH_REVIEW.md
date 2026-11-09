# TURNBOUND — Terminus dependency review

Based on the verified official v2.2.2f audit from GitHub Actions run 42568764777. This is analysis of function identifiers, edges and command-block references; no source/world bytes were modified in this stage.

- 82 unique referenced function IDs, 83 command-block matched invocations. This is incomplete physical coverage (at least 737 region scan warnings).
- 29 direct command-referenced tower-arrival IDs and 32 tower-departure IDs.
- All 32 original tower-departure functions directly call players:gamemode/set_survival, which conflicts with TURNBOUND's no-survival contract.
- All 32 original tower-arrival functions directly call players:gamemode/set_adventure and the original unlock chain. The unlock chain reaches terminus:count_towers and some original gift/progression functions.
- Structure/scene/shuttle command blocks must be reviewed individually; namespace names alone do not establish safe removal.
- The reviewed graph does not contain exact block coordinates, original commands or redstone invocation conditions. Do not infer actual destination positions or declare teleport functionality safe.

Decision: retain existing tower structures but implement server-authoritative TURNBOUND travel, location discovery and unlock state separately. Do not directly invoke the original movement, story or gift functions. The next task is to capture a position-aware metadata-only inventory of those direct command-block invocations, then implement copy-only reversible changes after explicit classification.

The dedicated Terminus graph workflow reads and verifies the previous audit artifact without redownloading the ~4 GB official world. Neither the analysis nor its artifacts redistribute map bytes.

## Actual reviewed call graph — verified checkpoint

[Terminus graph review #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42573686150) **SUCCESS**, using only the pinned previous official map-audit artifact. Full map not downloaded again and source world not modified.

- **82 distinct function IDs / 83 matched commands** from 2,232 scanned command blocks.
- **29 tower-arrival calls**, **33 tower-departure calls** (32 distinct departure functions; one used twice).
- **8 structure-scene calls**, **12 original player/drop/dialogue calls**, **1 other Terminus function call**. These are counts of regex-matched invocations, not whole-world device counts.
- Three referenced IDs have **no matching function body in the parsed graph**: `abilities:lowparticlemode`, `shuttle:reset`, `shuttle:start`. No automatic deletion or replacement is authorized for unresolved calls.
- The fixed source graph proves that all 29 directly referenced tower-arrival functions reach original adventure-mode switching, tower unlock and reward paths. All 32 distinct directly referenced tower-departure functions directly reach vanilla survival mode switching.
- The output is review metadata and **does not determine physical warp positions**. Coordinates, invocation conditions and missing region/command block coverage remain unverified.

The next needed evidence is [physical command-block inventory](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-trigger-locations.yml), which inspects the verified official original world and emits only the redacted function-reference, dimension, coordinate and command hash metadata. Its output is not yet a permission to modify any scene mechanism.

## Architecture boundary for replacement

For each confirmed original tower, maintain a data-driven TURNBOUND record with exact dimension, entry/exit coordinates, unlock state owned by the server, and a verified interaction surface. Discover/unlock state is independent of Drehmal scoreboard, original gifts, survival mode and original item drops. No gameplay action may be enabled merely from the presence of a function ID. Decisions about world access, initial towers and costs remain subject to existing TURNBOUND design.

## Selective copied-world suppression proof

## Terminus 62개 원작 워프 장치 분리 실검사 (2026-11-09)

- **실제 검증:** [TURNBOUND verified Terminus command isolation preview #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42582286295) **SUCCESS**, [해시 증거 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42582286295/artifacts/13658410066).
- 공식 원본 v2.2.2f ZIP 3개 SHA 검증 후 러너의 임시 경로에 전개 → **별도 월드 사본 생성** → 원본 위치/명령어 SHA-256으로 모든 대상 확인 → 해당 명령 블록의 `Command` 문자열만 비움 → **정확한 지역 파일 복구**. 원본 지역 파일은 수정하지 않음.
- 79개 함수 호출 명령 블록 중 **Terminus 62개**만 대상으로, **30개** Overworld `.mca` 지역 파일을 수정·복원. 구성은 탑 진입 **29개**, 출발 **33개**. 다른 직접 함수 호출 장치 **17개**는 명령을 변경하지 않음.
- 수정 대상 이외의 NBT 문자열/블록은 변경하지 않음(청크의 구조를 검사하고, 비대상 청크 압축 데이터와 NBT 바이트를 보존); 필요한 Anvil 섹터 위치 정보는 새 파일에 재계산됨.
- 적용 전후 각 지역 파일 SHA를 기록, 백업 SHA 검사 및 사본 복원 결과 **30/30개 지역 SHA 일치**, 상태 `EXACT_RESTORATION`, 대상 수 62. 합성 NBT 유닛 테스트 **4개 PASS**.
- `tools/patch_drehmal_terminus_commands.py`는 원본 `level.dat` 및 보고서 SHA-256이 정확히 일치하는 **비활성화된 오프라인 사본**에서만 작동. `inspect`/ `apply`/ `restore` 경로를 제공함. 활성화된 TURNBOUND 월드에 직접 적용 불가.
- 이는 **원작 Terminus 명령 블록의 선택적 차단+복구 안전성 검증**이지 새 빠른 이동 기능 완성, 맵 정상 플레이, 모든 원작 스크립트 비활성화, 멀티플레이 검증이 아니다.
- 남아 있는 원작 실행: 다른 명령 블록 17개, 자동 루트 패치와는 별도인 예약 함수·업적·서사 트리거·장치, 기존 737개 지역 스캔 경고. Terminus 건물·버튼 배경의 실제 상호작용 검증 전까지 정식 모드팩으로 전환하지 않는다.

### 후속 구현 경계

1. 원작 워프 함수의 **실제 안전한 도착 위치**와 이동 전후 조건을 월드·원본 함수에서 추출한다. 명령 블록의 좌표를 도착 위치로 오인하지 않는다.
2. TURNBOUND 서버의 검증 가능한 출발/도착/발견·해금 데이터로 **원작 생존/보상/스토리/scoreboard 호출 없이** 이동 구조를 작성한다. 비용·해금 조건은 승인된 게임 기획으로 결정하고 임의로 추가하지 않는다.
3. 자동 시작·tick 차단과 62개 명령 블록 차단을 함께 통합하는 전용 설치/복사본 절차를 작성한다. 남은 원작 장치의 재진입 경로를 조사한다.
4. 임시 월드에서 실제 게임 입장, 구조물/문/탑 기구, 서버 권한, 멀티클라이언트를 검증한 뒤 플레이 가능한 빌드로 판정한다.

