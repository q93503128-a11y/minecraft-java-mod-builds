# Drehmal 원작 시스템 분리 — 현재 검증 범위

> **최신 실측 검증(2026-11-09):** 공식 v2.2.2f 3개 ZIP SHA-256 검증 및 원본 데이터팩/명령 블록 분석 성공. 공식 월드를 **별도 복사**하여 `minecraft:load`/`minecraft:tick`의 원작 3개 자동 실행 진입점을 해시 고정 방식으로 차단한 뒤 **데이터팩 바이트까지 동일하게 복원**하는 오프라인 검증 PASS. [원본 분석 #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777) · [실제 복사본 가역 패치 #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42572004391). 다만 **개별 명령 블록·예약 함수·원작 업적/보상은 아직 활동 가능**하며, 구조물/Terminus/실게임·멀티플레이 검증 전이므로 **최종 맵 분리 완료는 아님**. 아래 이전 '원본 미확인' 문장은 시점별 과거 기록임.

## 작업 권한 경계 (사용자 명시)
- **원작 코드 조사와 원작 게임 시스템 분리는 별개다.** 원작 지형·건축물을 TURNBOUND 전용 맵 복사본에 보존하는 것은 승인된 방향이고, 분석 과정에서 원작 코드/동작을 참고하는 것은 가능하다.
- 실제 TURNBOUND 기능에 원작 데이터팩 함수, 명령어, Terminus 이동, NPC/전투/보상 논리를 복사·이식·재사용·변형하는 일은 사용자에게 후보를 제시하고 **명시적으로 승인받기 전까지 금지**한다.
- 재사용 후보는 **검토 대상**으로만 남기고, 승인받기 전 기존 승인 기획(특히 역참)을 임의로 변경하지 않는다.
- 기존 복사본 전용 차단/복구 툴은 원작 코드를 TURNBOUND에 이식하는 것이 아니라 원작 시스템을 분리하는 안전성 시험이다. 월드 전체 분리 완료로 간주하지 않는다.

## 정본
- 대상: 공식 Drehmal: APOTHEOSIS **2.2.2f**, Minecraft 1.20.1/Fabric.
- 공식 map release: https://github.com/Drehmal-Team/map/releases/tag/v2.2.2f
- ZIP 파일: shard_1.zip (1,484,178,230 B), shard_2.zip (1,641,289,974 B), shard_3.zip (861,863,079 B). 리소스팩 resources.zip (190,255,507 B).
- 현 GitHub 작업 정본: TURNBOUND \`turnbound-content-rebuild\` / \`projects/turnbound/\`. 다른 프로젝트 TURNBOUND: RE 또는 과거 알파 콘텐츠와 섞지 않음.

## 현 작업 상태
**원본 v2.2.2f 월드 바이너리의 내부 데이터팩은 아직 열지 못했음.** 런타임에 4 GB가 넘는 공식 월드 압축파일이 제공되지 않았으며, 현재 \`q93503128-a11y/minecraft-java-mod-builds\`에는 해당 원본 월드가 없음.

- \`tools/drehmal_pack_boundary.py\`: 실제 추출한 월드의 함수, \`minecraft:load\`/\`minecraft:tick\` 참조, 연결된 함수 그래프, 보상/진척도/이동/월드 변경 명령을 **읽기 전용**으로 조사.
- \`audit\`는 필수 \`level.dat\` 및 \`datapacks/\`를 요구하며 저장 데이터 수정 없음.
- \`stage\`는 원본을 보존하고 별도 위치에 **전체 월드 복사본**을 생성. 기존 대상 경로 덮어쓰기와 소스 내부 복사 금지. 이 단계에서도 원본 함수나 맵 장치는 건드리지 않음.
- 데이터팩 분석 결과에 "REVIEW_REQUIRED"라고 표기하며, *시각/퍼즐/포탈에 필요한 명령을 증거 없이 자동으로 제거하지 않음*.
- `tools/scan_drehmal_command_blocks.py`는 지역 파일(`region/*.mca`, `entities/*.mca`)의 NBT에서 **명령 블록/명령 블록 광차**를 읽기 전용으로 검사하고 명령·좌표를 보고한다. 외부 `.mcc` 청크, 지원하지 않는 압축, 손상된 청크는 경고로 남기므로 완전한 검사로 오인하지 않는다. 실제 2.2.2f 월드에는 아직 실행하지 않았음.

## 재현
\`\`\`bash
# 공식 맵 조각을 정상 추출·조립한 후:
python3 tools/drehmal_pack_boundary.py audit \
  --world "/path/to/official-Drehmal" \
  --output "/path/to/reports/drehmal-2.2.2f-functions.json"

# 디스크 여유(원본 이상의 용량)를 확인하고 별도 보존 사본 생성:
python3 tools/drehmal_pack_boundary.py stage \
  --source "/path/to/official-Drehmal" \
  --destination "/path/to/turnbound-drehmal-copy"
\`\`\`

## 분리 우선순위
1. 동일 월드에서 \`load\`, \`tick\`, 스케줄 및 외부 명령 블록 경로의 **실제 함수 ID**를 추출한다.
2. 원본 서사/전투/보상 계통과 **지도 구조 유지**에 필요한 문·포탈·워프·전용 텍스처/자막 계통을 호출 관계별로 구분한다.
3. 참조되는 기능을 삭제하여 지도에 깨진 오브젝트가 생기지 않게 한다. 필요한 것은 TURNBOUND 기능으로 교체하면서 최종 원작 상호작용을 분리한다.
4. 검토된 파일의 정확한 경로·SHA를 고정한 변경 목록으로 **복사본에만** 반영한다.
5. 실제 1.20.1/Fabric 클라이언트에서 서사/퀘스트/보상 재진입 여부, 문·포탈·Terminus, 월드 렌더링 및 멀티플레이를 확인한다.

## 전투 엔진 이식
- \`GenericBattleResolver\`는 서버 측 공통 전투 로직이 사용할 **로더 비종속 액션 실행기**다. 1000 고정소수점 게이지, 행동/타깃 검증, 피해·회복·실드·게이지·부활의 공통 하위 기능을 구현하고, 구현되지 않은 복잡 효과는 행동 전에 거절한다.
- 실제 필드 몬스터, 캐릭터 수집/저장, 공동 전투, 패시브, 카메라/UI, 스킬 전체·보상 처리 등은 **미구현**. 이 실행기는 아직 월드 엔티티를 자동 조우/전투로 전환하지 않는다. 캐릭터 기획·보스·스킬 수치를 임의로 채워 넣지 않는다.
- 이 단계의 자동 테스트는 순수 전투 액션의 거절/성공/상태 불변성만 검증한다.

## 수동 판단이 필요한 부분
원본의 시각 효과나 특정 도어/워프가 데이터팩에서 제공되는지는 실제 함수·NBT·클라이언트 검사 뒤 판정해야 한다. 현재는 원본 지도 삭제/수정/원본 코드 비활성화 **0건**이다.

## 공식 v2.2.2f 파일 무결성 자료
출처: GitHub \`Drehmal-Team/map\` 공식 Release Asset API 및 \`Drehmal-Team/installer/main/manifest.json\` (2026-11-09 조회).

| 원본 다운로드 | bytes | 공식 SHA-256 |
|---|---:|---|
| shard_1.zip | 1484178230 | 378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358 |
| shard_2.zip | 1641289974 | 2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331 |
| shard_3.zip | 861863079 | 8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67 |

원본 설치 프로그램 manifest 기준 추출된 월드 예상 크기는 5,139,218,299 bytes. Resource pack \`resources.zip\`은 190,255,507 bytes로 별도 보존해야 한다. 원본/리소스팩을 저장소에 직접 포함하지 않는다. 오프라인 월드 검사 전에 공식 조각 해시를 검증할 것.

## 검증 결과 및 남은 게임 내부 작업
- GitHub [Build TURNBOUND #1070](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42566636024) **SUCCESS** at \`3b65df702332741e264eaac160c01607be5b1ced\`.
- \`core-logic\`: Python 합성 맵 데이터팩 및 .mca 명령 블록 테스트 PASS; JDK 17 단독 범용 전투 코드 컴파일·행동 검증 PASS.
- \`fabric-build\`: Fabric 1.20.1 Gradle 테스트/빌드 및 생성 JAR 구조검증 PASS.
- 기존 CI 두 회의 504 오류는 Gradle distribution 다운로드 단계의 외부 오류로, 해당 커밋의 코드 컴파일 결과가 아님. 이후 정상 빌드에서 통과.
- **NOT TESTED**: 실제 Drehmal 2.2.2f 원본 데이터팩 분석, 원작 기능 분리 적용, 복사본 월드 진입·문/포탈/Terminus 확인, 실게임 전투, 멀티플레이.
- Fabric 클라이언트 그래픽 부팅은 이전 [smoke](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42564964435)에서 확인. 최신 JAR에서 실제 월드 HUD를 보았다는 의미가 아님.

## 원본 파일 자동 분석 작업(One-time CI)
- \`tools/download_audit_official_drehmal.py\`가 공식 v2.2.2f 릴리스 3개 ZIP을 **순서대로** 다운로드하고 공식 SHA-256/크기를 검사한다. 제작진 공식 설치 프로그램의 각 ZIP을 **같은 world root로 풀어 조립**하는 방식과 일치하도록 하되, 중복 파일 내용이 다르면 안전하게 중지한다.
- 다운로드/압축 해제 대상은 GitHub Actions 러너의 임시 경로이고, 압축 파일은 각 조각 추출 후 삭제한다. 사용자의 월드, Git 저장소, 기존 저장 데이터에 어떤 쓰기도 하지 않는다.
- 공식 파일에서 데이터팩 함수 graph와 region NBT를 검사하고 **원작 함수/명령 본문을 제외한 참조/통계 보고서만** 공개 가능한 CI artifact에 담는다.
- 일회성 GitHub workflow: \`.github/workflows/turnbound-drehmal-map-audit.yml\`. URL: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-map-audit.yml
- 대형 원본 다운로드 및 분석은 GitHub 네트워크/디스크 자원에 의존한다. 실패 시 원본 비활성화나 월드 통합이 완료됐다고 간주할 수 없다.
- 이 분석이 성공하더라도 실제 원작 스토리·전투·보상 함수는 **분석만 했을 뿐 꺼지지 않는다**. 서브시스템별 대상 식별과 복사본 적용/플레이 검증이 별도로 필요하다.

## 2026-11-09 실제 원본 데이터팩 감사 결과와 첫 가역 분리본

공식 [read-only 분석 실행](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777) SUCCESS, 원본 SHA-256 3개 일치. 이 실행은 기능을 제거하거나 플레이하지 않았다.

- 원본 추출 월드의 데이터팩: `bern.zip`, `emis.zip`, `hi_drehmal.zip`, `mael.zip`.
- **18,771개** 함수, **2개** 함수 태그, **2,232개** 명령 블록/명령 블록 광차 명령이 발견됨. 지역 파일 **2,461개** 탐색 중 경고 **737개**(지역 헤더 오류 등); 따라서 NBT 스캔은 불완전.
- 서버 시작 자동 태그: `#minecraft:load` → `core:init`; 매 틱 태그 `#minecraft:tick` → `core:main`, `entities:main`. 태그는 모두 `hi_drehmal.zip`에 있음.
- 이 진입점을 통한 간접 연결: 약 **16,517개** 함수 (정적 문자열 호출 기준; 명령 블록/scoreboard/schedule 등 직접 호출은 제외).
- 원본 지도 구성에서 TERMINUS 텔레포트 관련 함수 호출을 명령 블록에서 직접 찾았음. **따라서 시작·매 틱 루트 3개만 비활성화해도 원작 기능이 완전히 사라지지 않으며**, 일부 맵 기계 장치는 더 이상 작동하지 않을 수 있음.
- 분석기의 `teleport` 범주 통계는 `execute` 명령도 포함하므로 **실제 텔레포트 함수 수를 뜻하지 않음**. 이름/정적 호출은 후보 분류용이지 자동 삭제 승인 근거가 아님.
- 원본 발표의 샘플 `root_tags` SHA 및 `level.dat` SHA를 고정한 복사본 전용 도구 `tools/drehmal_auto_root_preview.py` 추가. `turnbound-stage.json`이 있고 활성화 마커가 없는 **오프라인 사본만** 받음. `inspect`는 읽기 전용, `apply`는 오직 `load.json`과 `tick.json`의 values를 비우고 다른 ZIP 내부 파일 바이트 보존 확인, `restore`는 백업 SHA에 따라 원래 ZIP로 복구. World level, NBT, command blocks, advancement, scheduler, functions 원본 내용은 건드리지 않음.
- **이는 테스트용 AUTO-ROOT 비활성화 분리본이지 완전한 terrain-only 변환이 아님.** 해당 사본을 켜서 게임을 진행하거나 배포할 단계가 아니다.
- 일회성 원본 복사본 실검사: [reversible root isolation acceptance](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-root-preview.yml). 공식 ZIP 다운로드→해시 검사→분리 사본→변경→복구를 러너에서 진행하고 **보고서 메타데이터만** 업로드함.

## 가역 분리 실검사 증거 (공식 월드 전체 파일)

- [Workflow `TURNBOUND Drehmal reversible root isolation acceptance #2`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42572004391): **SUCCESS**.
- 공식 월드에서 생성한 `hi_drehmal.zip` SHA-256: `f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74`.
- 사본의 `load.json` + `tick.json` 자동 진입점만 차단한 ZIP SHA-256: `344f82d835fb63e8d96d39e1aef62f0325977dc0cd87263bbef679d3ef03d8e0`.
- 복구 후 ZIP SHA-256: `f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74` **원본과 일치**.
- 확인된 함수: `core:init`(load), `core:main` + `entities:main`(tick). 다른 ZIP member의 내용 바이트는 변경하지 않음.
- 공식 `level.dat` SHA-256: `44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1`. `turnbound-world.marker`는 생성하지 않음.
- [Fabric Build #1077](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42572004087) **SUCCESS**. CI 다운로드 오류(HTTP 504) 후 재시도 로직을 수정하여 검증 완료. Fabric 코드/전투 기반은 변경하지 않았음.
- CI 테스트가 증명하는 것은 **오프라인 분리/복구 안전성**이며, 원작 맵 문·포탈·Terminus 작동이나 게임 플레이 가능성을 증명하지 않는다. 원본 재배포 권한도 부여하지 않는다.
- **다음 실작업:** 명령 블록에서 직접 호출하는 Terminus 이동 함수(공식 보고서 기준 63개 참조)와 각 특수 구조물 트리거를 위치·의존성별로 검토해 원작 게임 서사/보상만 차단하고 TURNBOUND 전용 이동·시설 동작으로 교체. 오래된 서바이벌·아이템 실행 루틴은 무조건 '재사용'하지 않음.

## Terminus command graph checkpoint

[Reviewed and verified metadata](TERMINUS_GRAPH_REVIEW.md) reports 82 referenced functions / 83 matched invocations. Original Terminus arrivals/departures cannot be directly reused because their call graph intersects vanilla gamemode, unlock and gift functions. A metadata-only physical trigger scan is now separately defined; original world bytes are excluded from its artifact. No new live command-block deactivation is claimed.

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


## 2026-11-09 잔여 경로 분석 검증

- [GitHub Actions: TURNBOUND Drehmal residual original entrypoints #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591270837): **SUCCESS**, 공식 이전 그래프와 좌표 보고서 SHA-256 재검증.
- 원작 함수 18,771개, load/tick 정적 도달 16,517개, 모든 명령 블록 함수의 정적 도달 314개(이 중 173개는 자동 루트에 포함되지 않음).
- 앞서 검증한 62개 Terminus 차단을 가정해도 원작 명령 블록 직접 함수 호출 17개/함수 참조 21건 → 정적 연결 가능 함수 225개. 이 수는 실제 실행 횟수나 게임 내 활성 시스템 수가 아니다.
- 기타 확인된 2,153개 명령 블록의 명령 종류, 업적·예약 함수 실행은 현재 추가 감사 대상. 함수 호출 명령으로 식별되지 않았다는 이유만으로 안전하다고 볼 수 없다.
- [원작 비함수 실행 경로 감사](DREHMAL_AUXILIARY_EXECUTION_AUDIT.md)는 공식 소스 월드의 명령어 종류·예약 함수·업적 콜백을 본문 없이 추출한다. 결과 검증 전까지 조치 승인/자동 제거 대상이 아니다.

## 2026-11-09 원작 실행 경로 감사 및 복사본 통합 분리 검증

- [원작 명령·예약·업적 추가 감사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591982023) **SUCCESS**. 명령 블록 2,232개(지역 경고 737개), 업적 501개, 업적 보상 필드가 있는 기록 292개(함수 콜백 220개, 경험치 보상만 72개), 예약 함수 호출을 포함한 함수 672개(명시된 schedule 호출 854건, 동일 함수 직접 재예약 27개)를 확인함. 이는 정적 명령·JSON 분석이며 실제 실행 여부 확인은 아님.
- 명령 블록 대표 유효 명령 종류: `playsound` 530개, `particle` 415개, `setblock` 271개, `clone` 198개, `summon` 83개, `function` 78개, `give` 37개, `gamemode` 29개. 구조물·연출과 원작 플레이어/보상 상태가 섞여 있으므로 명령 종류만으로 자동 삭제하지 않음.
- 원작 `data/weapons/advancements/*.json` 39개는 모두 `inventory_changed` 조건과 무기 보상 함수/제작법 보상을 가지며, 원작 함수 호출 그래프에서 해당 보상 함수 39개는 `called_by`가 비어 있음. 이 결과는 다른 종류의 원작 스코어/업적 트리거 가능성을 완전히 배제하지 못함.
- [공식 맵 복사본 **3종 통합 분리·복구 검증**](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42603106525) **SUCCESS**: 원작 무기 업적 39개의 `rewards`만 비움(달성 criteria 등은 유지) → `load/tick` 자동 진입점 3개 차단 → Terminus 명령 블록 62개 차단(30개 region) → 반대 순서로 정확히 복구. 원본 월드와 기존 TURNBOUND 게임 코드 무변경; 원작 데이터팩 SHA 원상복귀 `f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74`. 복사본 변경 검증이지 활성 월드/실게임 플레이 검증이 아님.
- **남은 주요 경계**: 다른 업적 462개(무기 업적 이외), 다른 함수 보상 콜백 181개, 72개 경험치 보상 기록, 672개 예약 함수 포함 함수, 원작 명령 블록의 직접 명령 및 특수 공간 장치, 원작 스케줄 저장 상태, 기존 지역 파일 경고. 실제 호출·장치 의존성 판정 후 *선별* 차단 필요.
- **권한 원칙**: Drehmal 코드를 TURNBOUND 시스템으로 복사·변형·재사용하지 않음. 쓸 만한 코드는 사용자 승인을 받을 후보로만 보고. 역참 구현, 전투/성장 기획 확장, 실제 플레이 준비 완료 선언은 이번 작업에서 하지 않음.


## 2026-11-09 원작 보상 콜백·예약 함수 위험 분류

[검사 및 데이터 요약](DREHMAL_CALLBACK_SCHEDULE_TRIAGE.md). 공식 데이터팩 그래프/명령어·업적·예약 함수 감사의 SHA-256을 고정해서 **181개 원작 무기 외 보상 함수**와 **672개 예약 실행 함수를 분석했다.** 구조물/퍼즐 65건, 월드 상태/이동 77건, 스토리/업적 21건, 플레이어 상태/보상 11건, 엔티티/전투 5건, 정의가 발견되지 않은 콜백 2건으로 *검토 범주*만 구분했다. 예약 함수 정적 그래프에서 순환 컴포넌트 29개, 자기 예약 27건을 확인했다. 이는 실제 서버 실행을 증명하지 않는다. 업적 달성 조건과 문/포탈/장치 연계가 확인되기 전에는 **181개 일괄 제거 금지**. Drehmal 코드 TURNBOUND 재사용 0건.

## 2026-11-09 원작 예약 이벤트 실측 / 네 단계 분리 사본 실검사

- [공식 원본 저장 상태 검사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42607952302) **SUCCESS**: 1.20.1 원본 `level.dat`의 `Data.ScheduledEvents`에 **원작 선물 처리 함수 `terminus:gifts/respawn_gift` 예약 1건** (게임 시간 `1135083463`, 예정 틱 `1135083594`, 131틱 차이). 정적 함수 그래프상 `core:init`과 본 함수 자체에서 예약/호출될 수 있고, 원작 선물·나침반 계열에 연결. 해당 **저장된 예약 큐는 load/tick 차단만으로 비워지지 않는다**.
- 같은 원본 월드의 저장된 점수판 목표 **539개** 및 command storage 파일 **12개** 확인. 실제 플레이어 점수/UUID/저장 내용은 공개하지 않음. 원본 장치 상태를 포함할 수 있어 삭제 대상 확정이 아님.
- [원작 4종 분리·복구 검증](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42608494226) **SUCCESS**: 공식 ZIP 3개 SHA 검증 후 월드 복사본에서 원작 무기 업적 보상 39개, 자동 시작/틱 3개, 원작 Terminus 명령 62개(30 region), 저장 보상 예약 1개를 같은 사본에 순차 적용하고 역순으로 전부 복원.
- 최종 원본 `level.dat` SHA `44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1` 일치, 원본 `hi_drehmal.zip` SHA `f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74` 일치, Terminus 지역 30개 각 원본 SHA 일치. 합성 NBT 테스트 **7개 PASS**.
- **OFFLINE ONLY, PLAYTESTED NO**: 월드 자체를 TURNBOUND 게임으로 활성화한 결과가 아니다. 남은 원작 장치·업적/예약 함수·스코어보드·커맨드 스토리지·명령 블록 / 지역 스캔 경고를 분리해야 하며, 구조물 보존 검증이 필요하다.
- 개발 범위 외 원작 코드 재사용, 역참 시스템 변경, 원본 맵 수정은 전혀 하지 않음.

## 4종 통합 분리 월드 복사본 — 독립 재검증 기록

- [TURNBOUND retained offline separated Drehmal copy #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42616136730) **SUCCESS**. 합성 회귀 테스트 5개 PASS, 공식 v2.2.2f 3샤드 SHA 확인. `inspect` 원본 상태 PASS → `prepare` 원작 4개 경계 차단 → **차단 상태 유지** → 독립 `verify` 결과 동일성 확인 → `restore` 원본 파일 상태로 정확히 복원.
- 차단 범위: 무기 업적 보상 39, 자동 load/tick 진입점 3, Terminus 명령 블록 62(지역 파일 30), 공식 월드 저장 예약 보상 1. 변경한 세계 파일은 GitHub에 업로드하지 않고 검증용 임시 러너에만 생성됨.
- 명령 블록·점수판·기타 업적·원작 전투/스토리 실행 경로가 **아직 남아 있으므로 전체 분리 완료 아님**. 실제 월드에서 문/포탈/퍼즐/이동 동작도 검증되지 않음.
- [검증 메타데이터](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42616136730)에서 전후 해시와 격리 경계 상태 확인 가능. 원작 코드 TURNBOUND 이식 0건.
