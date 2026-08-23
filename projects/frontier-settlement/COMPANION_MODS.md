# Frontier Settlement — External Content / Companion Stack (Minecraft 26.2 / NeoForge)

이 문서는 `ORIGINAL_DESIGN_v0.2.md`와 이전 마을 키우기 기획에서 정한 외부 모드 활용 방향을 실제 개발 규칙으로 고정한다.

## 핵심 결론

Frontier Settlement는 모든 Minecraft 콘텐츠를 직접 다시 만드는 프로젝트가 아니다.

우리가 직접 소유하는 핵심은:

- 공동 마을 성장;
- 주민 건설/생산;
- 실제 자원/창고;
- 도로/전초기지/물류;
- 영지 성장과 외부 콘텐츠를 연결하는 progression.

반대로 아래 영역은 검증된 외부 모드를 적극 사용해 콘텐츠 폭을 빠르게 확보한다.

- 바이옴/월드 생성;
- 던전/구조물/탐험 장소;
- 몬스터/야생 생물/적 변형;
- 무기/장비 폭;
- 전투 체감;
- 탐험 수납/정보/지도;
- 멀티 탐험 편의.

**Frontier 단독 실행은 호환성과 개발 편의를 위한 fallback이다. 실제 완성 플레이 경험의 콘텐츠 밀도는 이 companion/content stack과 함께 평가한다.**

외부 모드를 `옆에 깔아도 되는 추천 목록` 정도로 취급하지 않는다. Frontier의 탐험/전투/희귀 자원/영지 성장 입력으로 실제 연결한다.

## 외부 콘텐츠를 가져오는 3가지 방식

### A. 그대로 설치해서 콘텐츠 공급자로 사용

가장 빠른 방식. 외부 모드가 이미 잘하는 기능을 다시 만들지 않는다.

예:
- Terralith 바이옴/지형;
- Dungeons and Taverns / Repurposed Structures 구조물;
- Better Combat 전투;
- Weapons Expanded 무기;
- 몬스터 모드의 생물/적;
- Lootr의 멀티 탐험 보상 처리.

Frontier는 이 콘텐츠를 감지하거나 태그/구조물/아이템 범주를 통해 progression에 연결한다.

### B. 공개 라이선스 코드/데이터/리소스를 실제 Frontier 구현에 재사용

MIT/LGPL/CC 등 라이선스가 허용하는 범위에서 실제 코드/데이터/구조를 가져와 수정할 수 있다.

- 반드시 원저작자/저장소/라이선스/가져온 파일 또는 아이디어 범위를 기록한다.
- GPL 코드 직접 복사는 Frontier 전체 라이선스에 영향을 줄 수 있으므로 기본적으로 `참고/의존성`으로 두고, 명시적으로 라이선스 전환을 결정하기 전에는 Frontier MIT 코드에 섞지 않는다.
- MIT 코드/데이터는 저작권 고지와 라이선스 조건을 유지하며 재사용할 수 있다.
- LGPL 코드는 경계와 배포 형태를 검토한 뒤 사용한다.

### C. 공개돼 있지만 재배포가 제한된 모드는 구조만 참고하거나 의존성으로 사용

ARR/커스텀 라이선스/NC-ND 등은 공개 GitHub 저장소에 자산을 복사하지 않는다.

- JAR/데이터팩을 공식 배포처에서 설치;
- 공개 API/데이터팩/태그 연동;
- 동작/정보 구조를 참고해 Frontier에 독립 구현;
- 허가 없는 텍스처/모델/사운드/구조물 파일 재배포 금지.

개인 플레이 목적이어도 이 Frontier 저장소 자체가 공개되므로 이 경계는 유지한다.

## 26.2 기본 콘텐츠 스택

| 역할 | 모드 | 26.2 기준 | 라이선스/취급 | Frontier 사용 |
| --- | --- | --- | --- | --- |
| 월드/바이옴 | Terralith | 2.6.4 NeoForge | Stardust Labs License / 공식 JAR 사용 | 지형·전초 후보 다양화 |
| 구조물/던전 | Dungeons and Taverns | 5.3.0 Data Pack / 26.2 | ARR / 공식 팩 사용 | 탐험 목표·희귀 보상 |
| 구조물 확장 | Repurposed Structures | 7.7.5+26.2-neoforge | LGPL-3.0 | 구조물 밀도·다양성 확장, DnT와 밀도 회귀검사 |
| 전투 | Better Combat | 3.2.2+26.2-neoforge | ARR / 공식 JAR 사용 | 플레이어 전투 체감, 무기 자동 호환 |
| 무기 폭 | Weapons Expanded | 26.2 NeoForge 계열 | MIT | 무기/전투 콘텐츠 즉시 확장, Better Combat 호환 검사 |
| 탐험 수납 | Sophisticated Backpacks | 26.2-3.25.x | ARR / 공식 JAR 사용 | 원거리 개척 왕복 피로 감소 |
| 필수 라이브러리 | Sophisticated Core | 26.2 호환 | 공식 JAR | Sophisticated 계열 의존성 |
| 멀티 던전 보상 | Lootr | 26.2 NeoForge | MIT | 협동 플레이에서 구조물 상자 개인별 보상 |
| 최소 상태 표시 | Jade | 26.2.2+neoforge | CC-BY-NC-SA-4.0 | Frontier 건물/주민 상태 provider |
| 위치/지도 | Xaero's Minimap | 26.4.2 NeoForge 26.2 | ARR / 공식 JAR 사용 | 본진·전초·도로 위치 확인 |

## 26.2 추가 콘텐츠 후보

### 몬스터/야생 콘텐츠

- **Variants & Ventures 1.0.26+mc26.2 NeoForge**
  - 바닐라 감각을 크게 깨지 않고 몹 변형을 늘림.
  - CC BY-NC-ND 4.0이므로 수정본/자산 흡수보다 공식 모드 그대로 사용.

- **Alex's Mobs Continued 2.x+26.2 NeoForge**
  - 약 100종 이상 생물/몬스터 계열을 한 번에 확보할 수 있는 큰 콘텐츠 공급원.
  - GPL-3.0 계열이며 CodxLib 의존성이 있음.
  - 현재 포트가 비교적 새로우므로 기본 스택 승격 전 크래시/밸런스/스폰 밀도 테스트 필수.

### 현재 제외/보류

- **Create**: 공식 배포가 현재 26.2까지 올라오지 않았으므로 이번 스택에서는 제외. 코드 일부는 MIT지만 assets는 ARR이라 자산 복사 금지.
- **Farmer's Delight**: MIT이고 좋은 후보지만 현재 공식 배포가 1.21.1 계열이라 26.2 기본 스택에서는 보류.
- **MineColonies**: 건설/주민/청사진/UI의 핵심 레퍼런스. 현재 26.2 플레이 의존성으로 쓰기보다 설계/코드 구조 조사 대상으로 유지. GitHub 코드는 GPL-3.0이므로 Frontier MIT 코드에 직접 섞지 않는다.
- **Supplementaries**: 매우 좋은 Vanilla+ 콘텐츠지만 커스텀 라이선스의 공개 재배포 제한이 있으므로 공식 모드 의존성/개인 로컬 실험 외에는 자산을 Frontier 저장소에 복사하지 않는다.
- **Waystones 계열**: 초반 도로/물류 가치를 무너뜨리므로 제외.

## Frontier와 실제로 연결할 것

외부 모드는 단순히 월드에 존재하는 것으로 끝내지 않는다.

### 바이옴/전초

- Terralith 바이옴을 vanilla/tag fallback과 함께 읽어 산악/숲/평야/수변 전초 판정을 개선한다.
- 특정 모드 바이옴 ID를 하드 의존하지 않고 tag/config mapping을 사용한다.

### 던전/구조물

- Dungeons and Taverns / Repurposed Structures 발견 또는 희귀 전리품 획득을 후반 영지 성장의 입력으로 사용할 수 있게 한다.
- 구조물을 Frontier가 복제하지 않는다.
- 구조물 밀도가 너무 높아 마을/도로 공간을 잡아먹지 않는지 새 월드에서 검사한다.

### 몬스터/전투/무기

- Better Combat을 기본 전투 체감으로 사용한다.
- Weapons Expanded 같은 26.2 무기 모드를 추가해 무기 종류를 Frontier가 직접 수십 개 만들 필요를 없앤다.
- 외부 무기는 item/tag 기반으로 경비/후반 제작/희귀 보상과 연결 가능하게 한다.
- 몬스터 모드가 있으면 군사 전초기지/경비 가치가 실제로 올라가도록 하되 상시 웨이브 게임으로 만들지 않는다.

### 멀티 탐험

- Lootr를 기본 후보로 넣어 두 명 이상이 같은 던전을 탐험해도 한 명이 상자를 먼저 열었다고 다른 사람이 보상을 잃지 않게 한다.

### 정보/지도

- Jade provider로 Frontier 건물/주민/전초 상태를 별도 대형 UI 없이 노출한다.
- Xaero는 공식 지원 범위 안에서 위치 확인용으로 사용하고, API가 안정적일 때만 자동 waypoint를 검토한다.

## 콘텐츠 도입 우선순위

1. **Repurposed Structures + Lootr + Weapons Expanded**를 26.2 테스트 스택에 추가해 탐험/보상/무기 폭을 즉시 늘린다.
2. 기존 Terralith + Dungeons and Taverns + Better Combat + Sophisticated + Jade + Xaero와 함께 새 월드 호환 검사한다.
3. 구조물 과밀, 스폰 과밀, 전투 밸런스, 키 충돌, 저장소 태그 오인을 검사한다.
4. 안정적이면 Variants & Ventures를 추가한다.
5. Alex's Mobs Continued는 별도 안정성 패스로 검증 후 기본 스택 승격 여부를 결정한다.
6. Frontier progression이 외부 구조물/무기/희귀 아이템을 실제 목표로 사용하게 선택적 integration을 구현한다.

## 설치 원칙

- 월드 생성 계열(Terralith, Dungeons and Taverns, Repurposed Structures)은 **새 월드 만들기 전에** 설치한다.
- 클라이언트+서버 모드는 멀티 참가자도 동일한 호환 버전을 사용한다.
- 공식 Modrinth/CurseForge 파일을 사용하고 출처 불명 재배포 JAR을 사용하지 않는다.
- `COMPANION_MODS.md`와 실제 테스트 인스턴스의 버전이 다르면 테스트 결과를 정본으로 인정하지 않는다.

## 금지

- Frontier가 던전/몹/무기 수십 종을 다시 만들어 외부 모드 활용의 장점을 없애는 것.
- 외부 ARR/ND 자산을 공개 Frontier 저장소에 복사하는 것.
- 라이선스 확인 없이 `GitHub에 공개되어 있다`는 이유만으로 코드/텍스처/모델을 복사하는 것.
- Create/Farmer's Delight처럼 26.2 공식 호환이 확인되지 않은 모드를 기본 테스트 스택에 억지로 넣는 것.
- 외부 모드가 없는 경우 Frontier가 부팅 자체를 못 하는 하드 의존성을 무분별하게 늘리는 것.

외부 콘텐츠는 **Frontier의 폭을 빠르게 늘리는 핵심 개발 수단**이다. Frontier는 이 콘텐츠를 마을 성장에 연결하는 접착제 역할에 집중한다.
