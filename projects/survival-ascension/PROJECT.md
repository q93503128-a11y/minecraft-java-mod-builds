# Survival Ascension

- Mod version: `0.24.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: 기존 `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, Elite/Warband/종말 변이 persistent NBT, affix CustomData와 채굴 모드를 유지한다. 0.24는 `expedition_v1` player entry에 optional completion/progress/reward 필드만 추가한다.

## 핵심 방향
숙련 상승은 단순 수치 상승이 아니라 물리적 작업 규모와 행동 선택지 증가다. 플레이어가 강해질수록 적의 행동/조합, 세계 진행, 탐험 목표, 인프라와 자원 소비처도 함께 커져야 한다. 0.24부터 탐험은 바이옴에 발만 찍는 도감이 아니라 기존 숙련 행동을 해당 지역에서 실제 수행하는 현장 콘텐츠다.

## 기본 숙련 VI · Lv.100
- 채굴: 11×11, 광맥/추출192, 채석장 터널7×7×10.
- 벌목: 자연 나무 연쇄384, 12/player·64/global 틱 처리.
- 농사: 11×11 성숙 작물 수확, 관개 재파종은 실제 씨앗/작물 소비.
- 전투: 파급10체/5블록/70%, 훈련장 충격파6.5블록/16체/55%/50틱.
- 건축: 선49, 벽/바닥11×11, 공방 입체7×7×7.
- 기동: 단차2, 안전낙하16, dash1.80/16틱, Stage2+중추에서 공중 돌진3회.

## 0.24 대원정 현장 목표
### 발견과 완수 분리
- 서버가 20틱마다 실제 플레이어 바이옴을 확인해 원정권을 `발견`한다.
- 발견만으로 지역 보상/마일스톤/현장 숙련을 완료하지 않는다.
- 발견한 원정권 안에서 해당 현장 행동을 누적해 목표량을 채워야 `완수`한다.
- creative/spectator는 발견/진행/보상 대상에서 제외한다.
- `/ascension stats`에서 발견 x/9, 완수 x/9, 진행 중 각 원정의 목표 수치를 확인한다.

### Stage 0
- 삼림권: Veinminer++식 잎 검증을 통과한 자연 나무 bulk-fell queue에서 실제 파괴된 로그 96.
- 건조권: Construction의 material-backed/protected bulk queue에서 실제 성공한 secondary placement 128.
- 습지권: 실제 성숙 작물 파괴 96. 틱 큐가 추가로 수확한 블록도 정상 break event 기준으로 각각 기록한다.
- 고산권: 기존 Mobility가 XP로 인정하는 정상 도보 질주 거리 600.
- 대양권: 수영/수중/탑승 상태의 실제 수평 이동 800. 1초 이동량 24블록 초과는 텔레포트성 이동으로 거부하며 일반 Mobility 기록 경로에서는 대양 진행을 차단한다.

### Stage 1
- 심층권: 심층 biome family 안에서 정상 pickaxe-valid block 파괴 192. 광역/광맥/추출/터널의 정상 destroyBlock 후속 이벤트도 실제 블록 단위로 기록된다.
- 빙설권: 정상 도보 질주 거리 600. 탑승/비행/겉날개/수영/비정상 대이동 제외.
- 네더권: 네더 원정 biome 안에서 플레이어가 직접 처치한 hostile `Enemy` 24.

### Stage 2
- 엔드권: 엔드 원정 biome 안에서 플레이어가 직접 처치한 hostile `Enemy` 32.

### 진행/보상 안전
- `expedition_v1`은 UUID별 `discoveredMask`, `completedMask`, objective progress map, `regionRewardMask`, milestoneMask를 저장한다.
- 지역 숙련 XP는 새 0.24 플레이어에게 현장 목표 완수 시 1회만 지급한다.
- 0.23 저장 데이터에는 `region_rewards` 필드가 없으므로 로드시 기존 discoveredMask를 이미 지역 XP가 지급된 것으로 migration한다. 따라서 옛 발견을 0.24에서 완수해도 숙련 XP를 중복 지급하지 않는다.
- 0.23에서 `MILESTONE_MASTER`를 이미 받은 플레이어는 9개 completed로 migration해 기존 현장 숙련을 잃지 않는다.
- 기존 milestoneMask를 보존하므로 과거 자원/Mythic 마일스톤도 재지급되지 않는다.

### 플레이어별 1회 마일스톤
- Stage0 목표 5종 중 4종 완수: 다이아4 + 에메랄드16 + 자수정32.
- Stage1 이상, 총7종 완수 + 심층권 + 네더권 필수: 네더라이트 파편2 + 다이아16 + 메아리32.
- Stage2 + 9종 전부 완수: 신화III 장비1 + 네더라이트 파편4 + 메아리64 + 드래곤의 숨결16 + XP500 + 현장 숙련.

## 현장 숙련 · Field Mastery
조건: World Ascension Stage2 + 9개 원정권 현장 목표 전부 완수. 효과는 해당 숙련이 Lv.100일 때만 기본 숙련 VI 위에 추가된다.

- 채굴: 채석장 터널 `7×7×10 → 7×7×12`. pending cap640은 한 작업을 담기 위한 용량일 뿐 처리량은 계속 12/player·64/global block/tick.
- 벌목: 자연 나무 `384 → 448` 로그. 잎 검증 및 12/player·64/global 틱 큐 유지.
- 농사: `11×11 → 13×13`. 광역 수확 전체 12/player·64/global 틱 큐, pending384.
- 전투: 훈련장 질주 충격파 `6.5/16 → 7.5블록/20체`. 피해비율55%와 50틱 쿨 유지.
- 건축: `선49 → 65`, `벽/바닥11×11 → 13×13`. 입체7³ 유지. 실제 재료/보호 훅/global64/pending512 유지.
- 기동: Stage2+승천 중추 Lv.100 공중 돌진 `3 → 4회`. 착지 초기화와 기존 쿨 공유 유지.

현장 숙련은 Lv.100 이전 행동을 조기 해금하지 않는다. Shift 정밀 모드도 계속 우선한다.

## 기존 종말 루프
- World Stage: 각성0 → 최초 Wither 격파 전설1 → 최초 Ender Dragon 격파 종말2.
- Stage2 자연 생성 대상 일부는 Withered / Phase / Plague 변이를 얻으며 Elite/Warband 역할과 중첩 가능하다.
- Stage2 + 승천 중추 완공 후 중추 재선택으로 승천 시련을 연다.
- 승천 시련은 4웨이브/60초, 교리 `쇄도/추격/봉쇄`, 웨이브별 증원 최대1회, 별도 blanket HP multiplier 없음.
- 정상 Mythic III 정확히3 affix만 각성 가능하며, 검증 뒤 대량 자원을 소비해 4-affix 각성 신화로 만든다.

## 안전 계약
- Shift = 광역 작업 강제 정밀 모드.
- 채굴/벌목/농사/건축의 커진 작업은 서버 틱 큐로 분산한다.
- 추가 파괴는 정상 `player.gameMode.destroyBlock` 경로를 사용한다.
- 건축/재파종은 실제 재료 소비 + 상호작용/배치 보호 훅을 유지한다.
- 스포너 기반 Elite/Warband/종말 변이 보상 농장을 차단한다.
- 원정 목표는 플레이어의 현재 region + world stage를 서버에서 다시 검증한다.
- 대양은 수영/수중/탑승 이동만 전용 tracker로 인정하며 일반 육상 기동 카운트와 분리한다.
- 원정 완료/지역 XP/마일스톤 보상은 각각 영구 비트로 1회만 지급한다.

## 외부 소스 정책
- Gateways to Eternity(MIT): 순차 웨이브/상태 정보와 modifier 아이디어 적응, MIT 고지 패키징 유지.
- Apotheosis(MIT): rarity/category/affix 분리 철학 적응.
- Lootr(MIT): 플레이어별 탐험 보상 공정성 설계만 참고, 코드/블록/에셋/namespace 미사용.
- Bountiful(LGPL-3.0): 지역/상황에 맞는 objective→reward 계약 구조를 행동 설계로만 참고. 소스/에셋/데이터 미사용.
- FTB Quests(All Rights Reserved): discovery/task/completion과 플레이어별 누적 진행/단계 보상 분리만 UX·설계 참고. 소스/에셋/quest data/namespace 미사용.
- Repurposed Structures 및 Compass 계열은 탐험 동기/바닐라 월드 재활용 참고용이며 소스·에셋·worldgen은 복사하지 않는다.
