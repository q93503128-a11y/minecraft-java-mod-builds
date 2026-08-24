# Survival Ascension 콘텐츠 모드팩 호환 매트릭스

기준: Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

목표는 플레이어가 모드를 하나씩 찾는 구조가 아니라 **Survival Ascension 모드팩 하나를 가져와 설치**하는 것이다. 외부 모드는 원본 JAR을 그대로 사용하고, Survival Ascension은 숙련·월드 승천·장비·원정·물류 진행을 연결한다. 라이선스가 명시적으로 허용하지 않는 코드/리소스는 Survival Ascension JAR 안으로 복사하지 않는다.

## 1차 실제 팩 후보

| 모드 | 26.2 NeoForge 확인 | 역할 | 라이선스/포장 | 상태 |
| --- | --- | --- | --- | --- |
| Biomes O' Plenty | 확인 | 오버월드·네더·엔드 50+ 바이옴 | ARR, 원본 Modrinth 파일 참조 | 1차 포함 |
| GlitchCore | 확인 | Biomes O' Plenty 필수 라이브러리 | ARR, 원본 Modrinth 파일 참조 | 1차 포함 |
| The Birth of Steve | 확인 | 로그라이크 던전·조우·보스·장비 | ARR, 원본 Modrinth 파일 참조 | 1차 포함 |
| Amethyst Resonance | 확인 | 기능성 자수정 장비·Deep Dark 상호작용 | ARR, 프로젝트가 모드팩 사용 허용·원본 파일 참조 | 1차 포함 |
| Architectury API | 확인 | Amethyst Resonance 필수 라이브러리 | LGPL-3.0, 원본 파일 참조 | 1차 포함 |
| Cloth Config API | 확인 | Amethyst Resonance 필수 라이브러리 | LGPL-3.0, 원본 파일 참조 | 1차 포함 |

### 고정한 1차 Modrinth 버전
- Biomes O' Plenty: project `HXF82T3G`, version `kYz8T08F`, 26.2.0.0.26 NeoForge.
- GlitchCore: project `s3dmwKy5`, version `POAebwFo`, 26.2.0.0.0 NeoForge.
- The Birth of Steve: project `gKOBlOap`, version `12SBAmcX`, 0.5.0 / 26.2 NeoForge.
- Amethyst Resonance: project `8RyryQ7j`, version `no0B3Ssy`, 1.0.0 / 26.2 NeoForge.
- Architectury API: project `lhGA9TYQ`, version `LKQeKupY`, 21.0.4 NeoForge / 26.2.
- Cloth Config API: project `9s6osm5g`, version `zErG1kOw`, 26.2.155 NeoForge.

## 2차 후보

### Twilight Forest: Re26
- 프로젝트가 26.2 NeoForge를 지원하고, CurseForge에는 `MC 26.2 - 4.8.4204` 릴리스가 확인된다.
- 신규 차원, 다수 바이옴, 던전, 순차 보스, 장비를 한 번에 공급하므로 Survival Ascension 종말 단계 이후 콘텐츠와 가장 잘 맞는다.
- 코드 LGPL-2.1, 시각 리소스 CC BY-NC-SA 4.0 등 구성요소별 조건이 나뉜다.
- **실제 26.2 클라이언트/월드젠 스모크 후 기본 팩 승격**. 처음부터 넣어 크래시 원인을 복잡하게 만들지 않는다.

### Biomes of Overworld
- 26.2 NeoForge 지원, 23 바이옴·몹·광물·방어구·구조물·보스 제공.
- Biomes O' Plenty와 역할이 겹치므로 둘을 기본으로 동시에 넣지 않는다.
- BOP 대체 월드젠 프로필 후보.

### Pumpkillager's Quest / Exotelcraft
- 둘 다 26.2 계열 사용 가능.
- 전자는 소규모 조우/보스, 후자는 차원·몹·장비·월드 추가량이 크다.
- 게임 톤과 충돌 여부를 먼저 본 뒤 선택적으로 추가한다.

## Survival Ascension 연동 원칙
- `ModList`/레지스트리 ID/태그 기반의 선택적 호환으로 설계하고, 외부 모드가 빠져도 Survival Ascension 자체가 크래시하지 않게 한다.
- 외부 통나무·광석·작물은 올바른 바닐라/NeoForge 태그를 쓰면 기존 벌목·채굴·농사 시스템에 우선 자연 합류시킨다.
- 외부 적/보스는 실제 `Enemy`/체력/보스 성격을 이용해 전투 숙련과 후반 진행에 연결한다.
- 외부 바이옴·차원은 검증 후 원정 지역과 월드 승천 단계에 연결한다.
- 특수 기능 장비는 무조건 Ascension 어픽스를 붙이지 않고, 원래 장비 정체성을 망치지 않는 항목만 허용한다.
- 월드젠 모드는 한 번에 너무 많이 겹치지 않는다. 기본 월드젠 세트와 대체 세트를 분리한다.

## 설치 UX
최종 사용자는 여러 JAR을 직접 찾지 않는다. `Survival Ascension <버전>.mrpack`을 Modrinth App에서 가져오면 Minecraft/NeoForge/외부 모드/필수 라이브러리/Survival Ascension JAR/설정이 한 인스턴스로 구성되는 형태를 목표로 한다.

`.mrpack` 안에는 ARR 외부 JAR을 재포장하지 않고 Modrinth 원본 CDN 다운로드 정보만 기록한다. Survival Ascension 자체 JAR과 우리가 만든 설정/데이터팩만 `overrides/`로 포함한다.

## 검증 상태
이 문서는 **버전/로더/라이선스/의존성 선별까지 완료한 호환 매트릭스**다. 실제 한 인스턴스에서의 클라이언트 실행, 신규 월드 생성, 각 외부 콘텐츠 진입 및 장시간 플레이 호환성은 별도 스모크가 필요하며 아직 통과했다고 주장하지 않는다.
