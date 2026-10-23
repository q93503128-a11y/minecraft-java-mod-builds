# Augmentation Survival (working title)

> 정식 게임명은 아직 정하지 않았다. 이 문서의 `Augmentation Survival`과 slug `augmentation-survival`은 저장소 내 작업 식별을 위한 임시 명칭이다.

- Slug: `augmentation-survival`
- Mod ID: `TBD`
- Namespace: `TBD`
- Mod version: `TBD`
- Minecraft: `TBD` (새 프로젝트 착수 시 현재 생태계 기준으로 결정)
- Java: `TBD`
- Loader: `TBD` (Fabric / NeoForge 등을 외부 모드 생태계와 요구사항 기준으로 비교 후 결정)
- Loader version: `TBD`
- Gradle: `TBD`
- Build plugin: `TBD`
- Final JAR: `TBD`
- Existing-world compatibility: 가능하면 기존 월드 유지가 기본 목표, 구현 착수 전 세부 계약 확정
- Required dependencies: `TBD`
- Optional external mods: 바이옴·구조물·몹·전투·QoL·성능 모드를 엄선한 소형 모드팩 후보. 최종 목록은 호환성·라이선스·로더 조사 후 확정
- Forbidden bundled dependencies: 재배포 권한이 불명확한 모드/자산, 핵심 증강 시스템을 대체하거나 무력화하는 중복 시스템
- Datagen task: `TBD`
- GameTest task: `TBD`
- Server smoke-test task: `TBD`
- Client smoke-test task: `TBD`

## 현재 상태

`DESIGN CANON BUILDING / NO SOURCE BOOTSTRAP YET`

현재는 구현 전에 핵심 성장 루프와 증강 획득 구조를 확정하는 단계다. 로더와 Minecraft 버전은 아직 확정하지 않는다. 외부 모드 후보와 실제 지원 생태계를 확인한 뒤 선택한다.

## 프로젝트 정체성

이 프로젝트는 Minecraft의 장기 서바이벌을 유지하면서, 다양한 증강을 계속 획득해 플레이어가 매우 강해지는 파워 판타지를 추가하는 모드다.

전투 전용 로그라이크가 아니다. 전투뿐 아니라 채광, 이동, 탐험, 생존, 농사, 낚시, 건축·자원 수집 등 Minecraft의 여러 활동이 증강으로 크게 변해야 한다.

핵심 방향은 다음과 같다.

- 증강은 아껴 쓰는 소수의 미세 보너스가 아니라 플레이 체감을 크게 바꾸는 핵심 성장 시스템이다.
- 장기 월드에서 한 번의 운 때문에 빌드가 망가지지 않게 한다.
- 랜덤성은 증강 선택의 재미를 만들되, 이미 완성한 증강이 반복해서 꽝으로 나오지 않게 한다.
- 높은 등급 증강도 엔더 드래곤 처치 이후에만 열리지 않는다. 엔더 드래곤과 싸울 때 이미 고등급 증강을 활용할 수 있어야 한다.
- 특정 장소 하나가 특정 코어 등급 하나만 독점하지 않는다. 장소마다 여러 인접 등급의 코어가 서로 다른 확률로 나온다.
- 외부 모드를 사용하더라도 증강 시스템이 게임의 중심이어야 한다.

## 정본 문서

1. `PROJECT.md` — 프로젝트 식별, 기술 상태, 확정된 최상위 계약
2. `GAME_DESIGN.md` — 현재 증강 시스템 및 획득 구조의 상세 기획 정본
3. 저장소 공용 `AGENTS.md`
4. 저장소 공용 `docs/BUILD_STANDARD.md`
5. 저장소 공용 `docs/QUALITY_STANDARD.md`

구현이 시작되면 실제 source/resource와 최신 플레이테스트 결과가 과거 대화보다 우선한다.

## 현재 확정된 핵심 규칙

- 현재 등급 체계: `일반 → 고급 → 희귀 → 영웅 → 전설 → 신화`
- 등급별 증강 코어가 존재한다.
- 증강 장치에 코어를 넣으면 같은 등급의 증강 후보 3개가 나오고 하나를 선택한다.
- 증강은 `1회 획득형`과 `레벨형`을 모두 허용한다.
- 이미 최대 레벨인 증강은 다시 후보에 나오지 않는다.
- 특정 등급의 모든 증강을 최대치까지 획득했다면 그 등급 뽑기는 불가능하며 코어도 소비하지 않는다.
- 동일 등급 코어 3개를 바로 위 등급 코어 1개로 합성할 수 있다.
- 코어 합성 기능은 기본 증강 장치가 아니라 강화 증강 장치에서 열린다.
- 정확한 드랍 확률, 증강별 최대 레벨, 등급별 개수와 수치 밸런스는 아직 확정하지 않는다.

## 구현 시작 전 남은 결정

- 정식 게임명 / mod id / namespace
- 목표 Minecraft 버전
- Fabric / NeoForge 등 로더 비교와 최종 선택
- 공식 소형 모드팩에 포함할 외부 모드 후보와 라이선스·호환성 감사
- 등급별 코어의 바닐라 및 외부 콘텐츠 획득 분포와 실제 확률
- 첫 구현용 대표 증강 세트
- 증강 저장·동기화·멀티플레이 권한 구조
