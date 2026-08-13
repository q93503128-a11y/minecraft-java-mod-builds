# Senbonzakura & Ability Showcase

Minecraft Java 26.2 / NeoForge용 고연출 전투 기술 프로토타입.

## 조작

- `B` — 만해 · 천본앵경엄. `참백도 · 천본앵`을 주손/보조손에 들고 있을 때만 발동한다.
- `U` — 스킬 인벤토리 열기.
- `Shift + 1~9` — 장착 스킬 슬롯 1~9 사용.
- `Shift + 0` — 장착 스킬 슬롯 10 사용.

일반 기술은 더 이상 기술마다 별도 영구 단축키를 점유하지 않는다. 스킬 인벤토리의 전체 기술 라이브러리에서 최대 10개를 전투 슬롯에 배치해 사용한다.

## 0.1.0-alpha.9 — Skill Inventory & Fixed Combat Slots

`alpha.8`의 8개 쇼케이스 기술과 `alpha.7`의 천본앵 연출을 유지하면서 입력 구조를 확장형으로 교체했다.

- 전용 `U` 스킬 인벤토리 추가.
- 기술 라이브러리와 10칸 장착 슬롯 분리.
- 기술 카드 선택 → 슬롯 클릭으로 장착.
- 장착 슬롯 우클릭으로 해제.
- 기술 카드 더블클릭으로 첫 빈 슬롯 자동 장착.
- 같은 기술을 여러 슬롯에 중복 장착하지 않도록 자동 이동.
- 슬롯 배치는 인스턴스 `config/senbonzakura-showcase-skills.txt`에 저장.
- 최초 기본 배치: 1~8번에 현재 8개 기술, 9/0은 빈 슬롯.
- 기존 Z/V/G/R/H/J/K/O 개별 기술 단축키 제거.
- 천본앵 `B`는 참백도 전용키로 독립 유지.

## 현재 일반 기술

1. 천락 · Skyfall
2. 공간절단 · World Divide
3. 흑일 · Black Sun
4. 천검묘 · Grave of Swords
5. 역천 · Gravity Reversal
6. 시간장례 · Last Second
7. 백뢰강림 · Heaven's Judgment
8. 성창 · Stellar Lance

## UI 설계 원칙

- 바닐라 회색 인벤토리 복제가 아니라 특수 스펠북/능력 인벤토리 계열의 정보 구조를 참고한다.
- 왼쪽/상단 기술 라이브러리, 고정 상세 정보, 별도 10칸 장착 벨트를 명확히 구분한다.
- 각 기술은 텍스처 없이도 식별 가능한 전용 글리프와 강조색을 사용한다.
- 기술 수가 늘어나도 전투 입력은 10개 슬롯으로 고정한다.

## 환경

- Minecraft Java 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Gradle 9.2.1

CI에서 clean build와 JAR 내용 검사를 통과해야 배포 가능으로 본다.
