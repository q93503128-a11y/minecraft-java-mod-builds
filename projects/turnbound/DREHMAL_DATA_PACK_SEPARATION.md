# Drehmal 원작 시스템 분리 — 현재 검증 범위

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
