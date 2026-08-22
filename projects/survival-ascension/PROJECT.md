# Survival Ascension

- Slug: `survival-ascension`
- Mod ID: `survivalascension`
- Namespace: `survivalascension`
- Mod version: `0.3.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `net.neoforged.moddev 2.0.143`
- Final JAR: `survivalascension-0.3.0-alpha.1.jar`
- Existing-world compatibility: 기존 26.2 월드에 추가 가능. 기존 `mining_progress_v1` SavedData를 유지하며 0.1/0.2 채굴 성장 데이터와 공용 스킬 맵을 그대로 이어간다.
- Required dependencies: Minecraft 26.2, NeoForge 26.2.0.38-beta 이상
- Optional external mods: 없음
- Datagen task: `NOT IMPLEMENTED`
- GameTest task: `NOT IMPLEMENTED`
- Server/client smoke-test: canonical CI에서는 아직 `NOT RUN`

## 정체성

Survival Ascension은 바닐라 서바이벌의 성장 체급을 크게 확장한다. 수치만 조금 커지는 성장보다 숙련이 쌓일수록 한 번에 처리하는 작업 단위 자체가 커지는 것을 핵심으로 한다.

## 현재 활성 숙련

### 채굴
- 곡괭이 전용 속도 성장
- Lv.10 3×3 / Lv.30 5×5 / Lv.60 7×7
- 웅크리기 1×1 정밀 모드

### 벌목
- 도끼 + 통나무 행동 기반 XP
- Lv.10/30/60/90에 연결 통나무 16/48/128/256개
- 웅크리기 단일 통나무 모드

### 농사
- 완전히 익은 작물·네더와트, 멜론, 호박만 XP
- 괭이 사용 시 농사 숙련에 따라 수확속도 증가
- Lv.10 3×3 / Lv.30 5×5 / Lv.60 7×7 / Lv.90 9×9 광역 수확
- 손 수확은 바닐라 크기를 유지하고 웅크리면 항상 정밀 모드

## 공용 성장/UI

- 채굴·벌목·농사·전투·건축·기동 6개 슬롯을 하나의 XP 맵에 저장
- K키 숙련 화면에서 전체 슬롯, 레벨, 숙련 등급, 활성 능력과 XP 진행도를 확인
- 숙련 등급 I~V를 공용 기반으로 두어 이후 도구 티어/인챈트/콘텐츠 해금이 같은 규칙을 사용하도록 확장
- 광역 파괴·수확은 `ServerPlayerGameMode.destroyBlock`을 사용해 정상 드랍·내구도·이벤트 경로를 통과

## 외부 코드 정책

Skill Proficiencies의 MIT 허용 범위는 고지를 보존하고 필요한 구조를 포팅한다. Project MMO 2.0과 같은 제한/ARR 소스는 기능 및 UI 구조 참고만 하고 코드·리소스·에셋은 복제하지 않는다.
