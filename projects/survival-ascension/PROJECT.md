# Survival Ascension

- Mod version: `0.23.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: 기존 `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, Elite/Warband/종말 변이 persistent NBT, affix CustomData와 채굴 모드를 유지한다. 0.23은 새 플레이어별 SavedData `expedition_v1`만 추가한다.

## 핵심 방향
숙련 상승은 단순 수치 상승이 아니라 물리적 작업 규모와 행동 선택지 증가다. 플레이어가 강해질수록 적의 행동/조합, 세계 진행, 탐험 목표, 인프라와 자원 소비처도 함께 커져야 한다. 0.23의 탐험은 도감 수집이 아니라 최종 행동 체급으로 환류한다.

## 기본 숙련 VI · Lv.100
- 채굴: 11×11, 광맥/추출192, 채석장 터널7×7×10.
- 벌목: 자연 나무 연쇄384, 12/player·64/global 틱 처리.
- 농사: 11×11 성숙 작물 수확, 관개 재파종은 실제 씨앗/작물 소비.
- 전투: 파급10체/5블록/70%, 훈련장 충격파6.5블록/16체/55%/50틱.
- 건축: 선49, 벽/바닥11×11, 공방 입체7×7×7.
- 기동: 단차2, 안전낙하16, dash1.80/16틱, Stage2+중추에서 공중 돌진3회.

## 0.23 대원정
### 플레이어별 저장
- `expedition_v1`은 UUID별 `discoveredMask`와 `milestoneMask`를 저장한다.
- 발견/마일스톤은 서버 권한이며 creative/spectator는 획득하지 못한다.
- 실제 플레이어 바이옴을 20틱마다 검사한다. 한 지역은 플레이어마다 최초 1회만 발견된다.
- `/ascension stats`에서 9지역 진행률과 발견 목록을 확인한다.

### 원정권 9종
Stage 0:
- 삼림권 → 벌목 XP300
- 건조권 → 건축 XP300
- 습지권 → 농사 XP300
- 고산권 → 기동 XP350
- 대양권 → 기동 XP350

Stage 1에서 추가:
- 심층권 → 채굴 XP500
- 빙설권 → 기동 XP450
- 네더권 → 전투 XP600

Stage 2에서 추가:
- 엔드권 → 전투 XP800

월드 생성물을 새로 억지로 배치하지 않고 현재 바닐라 biome family를 원정권으로 사용한다. 따라서 기존 월드에서도 계속 진행할 수 있다.

### 플레이어별 1회 마일스톤
- Stage0 원정권 5종 중 4종: 다이아4 + 에메랄드16 + 자수정32.
- Stage1 이상, 총7종 + 심층권 + 네더권: 네더라이트 파편2 + 다이아16 + 메아리32.
- Stage2 + 9종 전부: 신화III 장비1 + 네더라이트 파편4 + 메아리64 + 드래곤의 숨결16 + XP500 + 현장 숙련.
- 마일스톤 비트는 영구 저장되어 바이옴 재진입이나 멀티플레이로 보상을 복제할 수 없다.

## 현장 숙련 · Field Mastery
조건: World Ascension Stage2 + 9개 원정권 전부 발견. 효과는 해당 숙련이 Lv.100일 때만 기본 숙련 VI 위에 추가된다.

- 채굴: 채석장 터널 `7×7×10 → 7×7×12`. pending cap은 640으로 한 작업을 담을 만큼만 확대하며 처리량은 계속 12/player·64/global 블록/tick.
- 벌목: 자연 나무 `384 → 448` 로그. 기존 잎 검증 및 12/player·64/global 틱 큐 유지.
- 농사: `11×11 → 13×13`. 0.23부터 광역 수확 전체를 12/player·64/global 틱 큐로 전환하고 pending384로 제한한다.
- 전투: 훈련장 질주 충격파 `6.5/16 → 7.5블록/20체`. 피해비율55%와 50틱 쿨은 유지한다.
- 건축: `선49 → 65`, `벽/바닥11×11 → 13×13`. 입체는7³ 유지. 실제 재료/보호 훅/global64/pending512 유지.
- 기동: Stage2+승천 중추 Lv.100 공중 돌진 `3 → 4회`. 착지 초기화와 기존 쿨 공유 유지.

현장 숙련은 Lv.100 이전 행동을 조기 해금하지 않는다. Shift 정밀 모드도 계속 우선한다.

## 월드 승천 / 종말
- 0 각성 → 위더 최초 격파 1 전설 → 엔더 드래곤 최초 격파 2 종말.
- 단계가 Elite 확률과 Warband 규모/빈도를 높인다.
- Stage2 자연 생성 대상 일부는 Withered / Phase / Plague 변이를 얻고 스포너 출신은 제외한다.
- 변이는 Elite 랭크·Warband 역할과 중첩 가능하다.

## 승천 시련
- Stage2 + 승천 중추 완공 뒤 완공 중추를 다시 선택해 연다.
- 입장: 메아리32 + 자수정64 + 드래곤의 숨결8.
- 4웨이브, 웨이브당60초, 사이5초, 교리 `쇄도/추격/봉쇄`, 웨이브별 증원 최대1회.
- 별도 HP 배율 없이 바닐라 역할 조합, Elite/Warband/변이가 정상 생성 경로에서 중첩한다.
- Evoker 직접 구성 제외, 10초 owner grace, 96블록 중복 방지, 120초 cooldown, restart orphan/stale-server cleanup 유지.

## 각성 신화 장비
- 정상 Mythic III 정확히3 affix만 각성 가능. 서버 검증 성공 후에만 재료를 소비한다.
- 각성 비용: 자수정256 + 다이아24 + 네더라이트 파편8 + 메아리64 + 드래곤의 숨결16.
- 기존3 affix 유지 + 빠진 affix1개 추가 =4 affix.
- 각성 재련: 자수정128 + 다이아16 + 파편4 + 메아리16, 4-affix 유지.
- affix는 기존 숙련 액션을 조기 해금하지 않는다.

## 안전 계약
- Shift = 광역 작업 강제 정밀 모드.
- 채굴/벌목/농사/건축의 커진 작업은 서버 틱 큐로 분산한다.
- 추가 파괴는 정상 `player.gameMode.destroyBlock` 경로를 사용한다.
- 건축/재파종은 실제 재료 소비 + 상호작용/배치 보호 훅을 유지한다.
- 스포너 기반 Elite/Warband/종말 변이 보상 농장을 차단한다.
- 시련 증원은 웨이브당 최대1회이고 타이머를 늘리지 않는다.
- 원정 보상은 플레이어별 discovery/milestone bit로 1회만 지급한다.

## 외부 소스 정책
- Gateways to Eternity(MIT): 시련 순차 웨이브/상태 정보와 modifier 개념을 고수준 적응, 기존 MIT 고지 패키징 유지.
- Apotheosis(MIT): rarity/category/affix 분리 철학 적응.
- Lootr(MIT): 0.23에서 멀티 탐험 보상을 플레이어별로 보존하는 제품 설계 원칙만 참고했으며 Lootr 코드/블록/에셋/namespace는 사용하지 않는다.
- Repurposed Structures(LGPL-3.0), Explorer's Compass/Nature's Compass(CC-BY-NC-SA)는 탐험 동기와 바닐라 월드 재활용 설계만 참고한다. 소스·에셋·worldgen은 복사하지 않는다.
