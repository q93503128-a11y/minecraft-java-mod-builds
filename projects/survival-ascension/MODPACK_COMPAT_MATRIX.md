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
- The Birth of Steve: project `gKOBlOap`, version `xls8dTZv`, `0.7.0+mc26.2+neoforge`, file `tbos-neoforge-26.2-0.7.0.jar`, SHA-1 `4d55c51685bff4247fa533c925f7641ce4880db3`.
- Amethyst Resonance: project `8RyryQ7j`, version `no0B3Ssy`, 1.0.0 / 26.2 NeoForge.
- Architectury API: project `lhGA9TYQ`, version `LKQeKupY`, 21.0.4 NeoForge / 26.2.
- Cloth Config API: project `9s6osm5g`, version `zErG1kOw`, 26.2.155 NeoForge.

TBS 0.7 바이너리 감사에서 추가 Modrinth 의존성은 없었고, 실제 보스 이벤트를 가진 고유 강적으로 `tbos:hour_cantor`와 `tbos:phoenix_guardian`(The Last Curator)을 확인했다. `tbos:minotaur`는 같은 기준의 boss event가 없어 0.47 강적 계약에 넣지 않는다. Fractured Archive는 별도 dimension `tbos:fractured_archive`이며 내부 biome은 `minecraft:the_void`이므로 기존 9지역 중 하나로 위장시키지 않는다.

## 현재 런타임 연동 계층

첫 콘텐츠 팩은 단순히 여러 모드를 같은 폴더에 넣는 데서 끝내지 않는다. Survival Ascension 쪽에서는 외부 구현 클래스를 직접 import하지 않고 Minecraft/NeoForge의 공용 계약과 Survival 소유 데이터 태그를 통해 기존 성장 시스템에 합류시킨다.

- 광석: `c:ores`를 `survivalascension:valuable_ores`에 포함한다. 올바르게 공용 광석 태그를 쓰는 외부 광석은 기존 광맥 벌목/추출 모드의 대상이 된다. 광석별 세밀한 XP 값이 아직 작성되지 않은 외부 블록은 기존 안전한 경도 기반 XP fallback을 사용한다.
- 벌목: 기존 `minecraft:logs` + `minecraft:leaves` 계약을 그대로 사용한다. 따라서 이 태그를 정상적으로 제공하는 외부 수종은 별도 모드 ID 분기 없이 자연 나무 연결 벌목에 들어온다.
- 농사: `CropBlock` 계열 외부 작물은 기존 성숙 판정/광역 수확에 자연 합류한다. 반대로 전용 성장 로직을 가진 비표준 작물은 씨앗/성숙 상태를 추측해서 파괴하지 않는다. 자동 재파종은 안전하게 정의된 작물만 유지한다.
- 전투: Minecraft `Enemy`와 NeoForge 공용 보스 태그를 함께 실제 전투 대상으로 취급한다. 외부 보스가 `Enemy` 구현을 쓰지 않아도 공용 보스 태그를 제공하면 파급/충격파/전투 숙련에 합류한다.
- 원거리 장비: NeoForge 공용 `c:tools/bow` / `c:tools/crossbow` 태그를 쓰는 장비는 승천 각인에 합류한다. 발사체는 발사 순간 Survival affix/Shift 정밀 상태만 자체 persistent NBT에 스냅샷하며, 외부 활/쇠뇌 구현 클래스를 직접 참조하지 않는다.
- 방패 장비: NeoForge 공용 `c:tools/shield` 태그를 쓰는 장비는 승천 각인에 합류한다. 실제 차단 성공 시에만 전투 숙련 기반 방어 파동을 만들며 외부 방패 구현 클래스를 직접 참조하지 않는다. 파동은 피해/숙련 XP 없이 주변 실제 적을 밀어내기만 한다.
- 강적: `survivalascension:expedition_major_targets` EntityType Tag를 사용한다. 현재 TBS의 Hour Cantor와 The Last Curator가 `required:false` 데이터 항목으로 들어가며 Java에는 `tbos` 레지스트리 ID나 구현 클래스를 넣지 않는다.
- 강적 처치 가중치: 일반 적의 기존 처치 크레딧 +1은 그대로 유지하고 강적이면 +3을 추가한다. 추가분은 현재 9지역의 현장 지령과 이미 진행 중인 같은 지역 원정 작전에만 적용한다. 사건 카운터에는 추가 +3을 넣지 않아 보스 한 마리가 사건을 통째로 끝내지 못한다.
- 외부 차원 경계: Fractured Archive처럼 `currentRegion == null`인 별도 차원에서는 9지역 진행을 만들지 않는다. 원정 작전의 기존 '다른 차원 이탈 시 실패' 규칙도 완화하지 않는다. 대신 강적은 전투 숙련 XP 계산에서 더 높은 체력 계수와 최대 600 XP 상한을 사용한다.
- 전투 XP는 실제 적/보스/강적 처치에만 지급한다. 가축·중립 비전투 생물을 반복 처치해 전투 숙련을 올리던 기존 우회는 제거한다.
- 원정 바이옴: 9개 `survivalascension:expedition/<region>` Biome Tag를 바닐라 fallback보다 먼저 판정한다. BOP ID는 `required:false` 데이터 항목이며 `glowing_grotto`와 `spider_nest`는 심층권에 포함한다.
- Amethyst Resonance: locked 26.2 NeoForge 1.0.0 binary audit confirmed vanilla sword/pickaxe/axe/shovel/hoe tags. 0.46 adds the previously missing shovel imprint; Resonant Pickaxe class perks and the separate persistent Resonant armor DataComponent are preserved because Survival Ascension does not replace the item or clear unrelated components. Locked SHA-1 `a3ac49a6202b7918d2ed22030df0b6e2906cdec8`.
- 외부 모드가 하나 빠져도 이 계층 자체는 로드된다. `biomesoplenty`, `tbos`, `amethyst_resonance` 구현 클래스에 대한 하드 의존은 두지 않는다.

현재는 **태그/타입 기반 통합 + 외부 장비 승천 각인 + 표준 활/쇠뇌 원거리 affix/발사체 스냅샷 + 표준 방패 성공차단 방어 파동 + BOP 원정 바이옴 브리지 + 데이터 기반 외부 강적 가중치**까지 연결되어 있다. 외부 차원이나 특정 외부 보스를 월드 승천 필수 진행으로 승격하지는 않는다.

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
- 레지스트리 ID/공용 태그/Survival 소유 optional 데이터 태그/바닐라 타입 기반의 선택적 호환으로 설계하고, 외부 모드가 빠져도 Survival Ascension 자체가 크래시하지 않게 한다.
- 외부 통나무·광석·작물은 올바른 바닐라/NeoForge 태그와 타입을 쓰면 기존 벌목·채굴·농사 시스템에 우선 자연 합류시킨다.
- 외부 적/보스는 실제 `Enemy`/공용 보스 태그/체력/감사된 강적 데이터 태그를 이용해 전투 숙련과 후반 진행에 연결한다.
- 외부 바이옴·차원은 검증 후 원정 지역과 월드 승천 단계에 연결하되, 별도 인스턴스 차원을 기존 9지역으로 위장하지 않는다.
- 특수 기능 장비는 무조건 Ascension 어픽스를 붙이지 않고, 원래 장비 정체성을 망치지 않는 항목만 허용한다.
- 월드젠 모드는 한 번에 너무 많이 겹치지 않는다. 기본 월드젠 세트와 대체 세트를 분리한다.

## 설치 UX
최종 사용자는 여러 JAR을 직접 찾지 않는다. `Survival Ascension <버전>.mrpack`을 Modrinth App에서 가져오면 Minecraft/NeoForge/외부 모드/필수 라이브러리/Survival Ascension JAR/설정이 한 인스턴스로 구성되는 형태를 목표로 한다.

`.mrpack` 안에는 ARR 외부 JAR을 재포장하지 않고 Modrinth 원본 CDN 다운로드 정보만 기록한다. Survival Ascension 자체 JAR과 우리가 만든 설정/데이터팩만 `overrides/`로 포함한다.

## 검증 상태
버전/로더/라이선스/의존성 선별과 위 태그/타입 기반 소스 연동은 자동 감사 대상으로 고정했다. 실제 한 인스턴스에서의 그래픽 클라이언트 실행, 신규 월드 생성, 각 외부 콘텐츠 진입 및 장시간 플레이 호환성은 별도 스모크가 필요하며 아직 통과했다고 주장하지 않는다.
