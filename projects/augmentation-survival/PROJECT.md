# Augmentation Survival (working title)

> 정식 게임명은 아직 정하지 않았다. `Augmentation Survival`과 slug `augmentation-survival`은 저장소 내 작업 식별을 위한 임시 명칭이다.

- Slug: `augmentation-survival`
- Mod ID: `augmentation_survival` (working technical id)
- Namespace: `augmentation_survival`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `Fabric`
- Loader version: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Gradle: `9.5.1` wrapper baseline
- Build plugin: `Fabric Loom 1.17.20`
- Final JAR: `augmentation-survival-0.1.0-alpha.1.jar` (bootstrap target; not release-ready)
- Existing-world compatibility: 가능한 한 유지하되 Terralith는 새 월드 기준. 월드젠 외부 모드 교체/제거는 별도 migration 계약 없이는 지원하지 않는다.
- Required dependencies: Fabric API
- Official pack candidate mods: `MODPACK_BASELINE.md` 참조
- Forbidden bundled dependencies: 재배포 권한이 불명확한 모드/자산을 저장소 JAR에 직접 번들하지 않는다. 외부 모드는 원본 배포처를 참조하는 모드팩 의존성으로 관리한다.
- Datagen task: bootstrap 단계 미구성
- GameTest task: bootstrap 단계 미구성
- Server smoke-test task: `runPackServer -PaugmentationPackRuntime=true`
- Client smoke-test task: `runPackClient -PaugmentationPackRuntime=true` (수동/화면 검수 필요)

## 현재 상태

`M0 PACK BOOTSTRAP / EXTERNAL COMPATIBILITY VERIFICATION`

기획 정본은 유지하면서 실제 Minecraft 26.2 Fabric 개발 골격과 외부 모드팩 dependency probe를 추가했다. 외부 모드가 한 런타임에서 실제로 동시 로딩되는지는 GitHub CI dedicated-server smoke test 결과로 판정한다.

## 프로젝트 정체성

Minecraft 장기 서바이벌을 유지하면서 다양한 증강을 계속 획득해 플레이어가 매우 강해지는 파워 판타지 모드다.

전투 전용 로그라이크가 아니다. 전투뿐 아니라 채광, 이동, 탐험, 생존, 농사, 낚시, 건축·자원 수집 등 Minecraft의 여러 활동이 증강으로 크게 변해야 한다.

핵심 방향:

- 증강은 미세 보너스가 아니라 플레이 방식을 크게 바꾸는 중심 성장 시스템이다.
- 장기 월드에서 한 번의 운 때문에 빌드가 망가지지 않게 한다.
- 높은 등급 증강도 엔더 드래곤 처치 이후에만 열지 않는다.
- 특정 장소 하나가 특정 코어 등급 하나만 독점하지 않는다. 장소마다 여러 인접 등급의 코어가 서로 다른 확률로 나온다.
- 외부 모드는 증강을 사용할 세계와 콘텐츠를 확장해야 하며, 별도 성장 시스템으로 증강과 경쟁하지 않는다.

## 정본 문서

1. `PROJECT.md`
2. `GAME_DESIGN.md`
3. `MODPACK_BASELINE.md`
4. 저장소 공용 `AGENTS.md`
5. 저장소 공용 `docs/BUILD_STANDARD.md`
6. 저장소 공용 `docs/QUALITY_STANDARD.md`

실제 source/resource와 최신 검증 결과가 과거 대화보다 우선한다.

## 현재 확정된 핵심 규칙

- 등급: `일반 → 고급 → 희귀 → 영웅 → 전설 → 신화`
- 등급별 증강 코어가 존재한다.
- 증강 장치에 코어를 넣으면 같은 등급의 유효한 증강 후보 3개가 나오고 하나를 선택한다.
- 증강은 1회 획득형과 레벨형을 모두 허용한다.
- 이미 최대 레벨인 증강은 다시 후보에 나오지 않는다.
- 특정 등급의 모든 증강을 최대치까지 획득했다면 그 등급 뽑기는 불가능하며 코어도 소비하지 않는다.
- 동일 등급 코어 3개를 바로 위 등급 코어 1개로 합성할 수 있다.
- 코어 합성 기능은 강화 증강 장치에서 열린다.
- 정확한 드랍 확률, 증강별 최대 레벨, 등급별 개수와 수치 밸런스는 아직 확정하지 않는다.

## 외부 모드 연동 계약

- 외부 모드 JAR 자체를 수정하는 방식보다 우리 모드의 loot/event/data 호환 레이어를 우선한다.
- 외부 구조물 상자에는 원본 loot를 보존하면서 증강 코어 풀을 추가한다.
- 외부 몹은 해당 모드의 loot table 또는 서버 권한 이벤트를 통해 코어 드랍을 추가한다.
- 외부 바이옴은 biome tag/worldgen hook를 통해 코어 관련 구조물·자원·이벤트를 배치할 수 있다.
- `장소 = 단일 등급`이 아니라 `장소 = 여러 등급의 확률 분포`로 설계한다.
- 호환 데이터는 가능한 한 data-driven으로 분리한다.

## 다음 검증 게이트

1. pinned 외부 모드/라이브러리 전부 dependency resolution
2. 각 JAR의 `fabric.mod.json` 실제 depends 감사
3. loot table / structure / biome 데이터 엔트리 인벤토리
4. dedicated server 동시 로딩
5. 구조물 중첩 밀도와 월드젠 충돌 확인
6. Better Combat과 향후 공격 증강 판정 호환 테스트
7. 실제 클라이언트 화면 플레이테스트

빌드 성공과 실제 플레이 성공은 구분한다.
