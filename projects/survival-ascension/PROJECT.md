# Survival Ascension

- Mod version: `0.26.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: 기존 `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, Elite/Warband/종말 변이 persistent NBT, affix CustomData와 채굴 모드를 유지한다. 0.26은 `expedition_v1` player entry에 optional `incident_rewards` bitmask만 추가한다.

## 핵심 방향
숙련 상승은 단순 수치 증가가 아니라 물리적 작업 규모와 행동 선택지 증가다. 플레이어가 강해질수록 적의 행동/조합, 세계 진행, 탐험 목표, 인프라와 자원 소비처도 함께 커진다. 원정은 체크리스트에 머무르지 않고 기존 벌목·건축·농사·기동·채굴·전투 시스템이 지역 상황과 희귀 사건에서 다시 조합되는 콘텐츠로 유지한다.

## 0.26 희귀 현장 사건
### 발생/수명
- 발견된 원정권 안의 생존 플레이어를 30초 간격으로 검사한다.
- 조건 충족 시 10% 확률로 지역 사건을 시작한다.
- 사건은 45~60초이며 `ServerBossEvent`로 남은 적/행동량/시간을 표시한다.
- 사건 시작점 반경48블록 또는 해당 원정권을 10초 이상 벗어나면 실패한다.
- creative/spectator는 사건 시작/진행 대상에서 제외한다.
- 승천 시련의 persisted start-ready tick 주변 5분 구간에는 사건 체크를 차단해 승천 시련과 현장 사건 전투가 중첩되지 않게 한다.
- 실패 시 지령 진행도 손실 없음. 사건 자체는 cooldown 뒤 재발생 가능하다.

### 지역별 18개 사건
각 원정권은 습격형1 + 긴급작업형1을 가진다.

Stage 0:
- 삼림 `수림 습격`: zombie/spider 6체.
- 삼림 `벌목 비상`: 실제 자연 로그 일괄 벌목24.
- 건조 `약탈대 급습`: husk/pillager 6체.
- 건조 `긴급 보급선`: 실제 scaled Construction 성공 배치24.
- 습지 `늪지 습격`: zombie/spider 6체.
- 습지 `긴급 수확`: 성숙 작물20.
- 고산 `능선 매복`: pillager/skeleton 6체.
- 고산 `능선 돌파`: 서버 검증 성공 R 돌진4.
- 대양 `익사자 습격`: drowned 6체, water spawn slot만 사용.
- 대양 `폭풍 항해`: 수영/수중/탑승 항해180.

Stage 1:
- 심층 `심층 군집`: zombie/skeleton/spider 7체.
- 심층 `붕괴 전 채굴`: pickaxe-valid block48.
- 빙설 `설원 습격`: stray/skeleton 6체.
- 빙설 `빙설 강행군`: legitimate travel180.
- 네더 `네더 급습`: wither skeleton/blaze 8체.
- 네더 `열기 속 채굴`: pickaxe-valid block48.

Stage 2:
- 엔드 `공허 습격`: endermite/shulker 7체.
- 엔드 `공허 추적`: legitimate travel180.

### 사건 생명주기/스폰 안전
- 습격은 `EntitySpawnReason.TRIGGERED`의 bounded vanilla spawn을 사용한다.
- 육상 사건은 반경7~13블록의 open spawn slot을 찾고, 대양 사건은 물이 2칸 연속 존재하는 water spawn slot만 사용한다.
- 요청 수의 2/3(최소3) 미만만 생성되면 사건을 시작하지 않고 생성분을 정리한다.
- 진행 중 사건 몹이 반경48블록 밖으로 이탈하면 중앙으로 navigation recall한다.
- 실패/로그아웃 시 남은 사건 몹을 discard하고 bossbar viewer를 제거한다.
- static incident state가 다른 integrated/dedicated server instance에서 남으면 stale-server cleanup으로 제거한다.

### 지역당 1회 사건 보상
- `incident_rewards` bitmask는 UUID별/지역별 해결 보상을 영구 기록한다.
- Stage0 사건 해결: 해당 지역 숙련 XP100 + 에메랄드4 + 자수정8.
- Stage1 사건 해결: 해당 지역 숙련 XP150 + 다이아2 + 메아리4.
- Stage2 사건 해결: 해당 지역 숙련 XP200 + 다이아4 + 메아리8.
- 현재 지역 지령이 미완료면 첫 미완료 task에 target의 20%를 bonus progress로 추가한다.
- 이 20% 보너스는 지역당 사건 최초 해결 1회만 적용되므로 사건 대기만으로 원정을 반복 우회할 수 없다.
- 사건 보너스로 지령이 완료되면 기존 `addObjectiveProgress -> region reward -> milestone` 경로를 그대로 사용한다.
- `/ascension stats`에서 사건 해결 x/9를 확인한다.

## 0.25 지역 현장 지령
### 구조
- 9개 원정권마다 2개 지령, 총 18개.
- 새 지역 최초 발견 시 플레이어별로 2개 중 하나를 server RNG로 선택하고 `expedition_v1.directives`에 영구 저장한다.
- 표준 지령은 0.24의 기존 단일 목표와 동일하다.
- 혼합 지령은 2개의 서로 다른 task를 모두 완료해야 지역 완수 처리된다.
- 지역 이탈/재진입/서버 재시작으로 지령은 reroll되지 않는다.

### task 출처 계약
- `LOGS_FELLED`: 잎 검증을 통과한 smart-tree 대량 벌목 큐에서 실제 성공한 자연 로그 destroyBlock.
- `BLOCKS_BUILT`: 실제 재료가 소비되고 mayInteract/NeoForge placement hook을 통과한 scaled Construction secondary placement.
- `CROPS_HARVESTED`: 실제 성숙 작물 파괴. 12/player·64/global 틱 큐 후속 수확도 블록 단위로 기록.
- `TRAVEL_DISTANCE`: 기존 Mobility legitimate sprint tracker. 탑승/비행/겉날개/수영/비정상 대이동 제외.
- `OCEAN_VOYAGE`: 대양권에서 수영/수중/탑승 상태의 수평 이동만 인정. 1초 displacement24블록 초과 거부.
- `BLOCKS_MINED`: 정상 pickaxe-valid block break. 광역/광맥/추출/터널 후속 destroyBlock도 블록 단위 기록.
- `HOSTILES_KILLED`: 플레이어 직접 처치 + `Enemy`만 인정.
- `DASHES_USED`: cooldown/공중 횟수/상태 검증 뒤 실제 impulse가 적용된 성공한 R만 기록.

### 0.24/0.23 migration
- `expedition_v1` ID 유지.
- 0.24에서 발견된 region에 `directives`가 없으면 index0 표준 지령을 자동 부여한다.
- 0.24 legacy region progress를 표준 지령 첫 task로 이관한다.
- completedMask/region reward/milestone 상태는 유지한다.
- 0.23 `MILESTONE_MASTER` 보유자는 9개 completed 상태를 유지해 현장 숙련을 잃지 않는다.
- 0.26의 `incident_rewards` 기본값은0이므로 기존 월드도 향후 각 지역 사건을 정상 경험할 수 있다.

## 기본 숙련 VI · Lv.100
- 채굴11×11, 광맥/추출192, 채석장7×7×10.
- 벌목384, 12/player·64/global.
- 농사11×11, 관개 재파종 실제 씨앗 소비.
- 전투 파급10체/5블록/70%, 훈련장6.5/16/55%/50틱.
- 건축 선49, 면11×11, 입체7³.
- 기동 단차2, 안전낙하16, dash1.80/16틱, Stage2+중추 공중돌진3회.

## 현장 숙련 · Field Mastery
조건: Stage2 + 9개 지역 지령 완수. 해당 숙련 Lv.100에서만 추가 적용.
- 채굴 터널7×7×12, pending640은 한 작업 수용용이며 처리량12/player·64/global 유지.
- 벌목448, 잎 검증/12·64 큐 유지.
- 농사13×13, pending384/12·64 큐 유지.
- 전투 충격파7.5블록/20체, 피해55%/50틱 유지.
- 건축 선65/면13×13, 입체7³, 재료/보호/global64/pending512 유지.
- 기동 공중돌진4회, 착지 초기화/기존 쿨 유지.

## 기존 종말 루프
- 각성0 → 최초 Wither 격파 전설1 → 최초 Ender Dragon 격파 종말2.
- Stage2 자연 생성 일부는 Withered/Phase/Plague 변이, Elite/Warband와 중첩 가능.
- Stage2 + 승천 중추 완공 후 4웨이브 승천 시련. 교리 `쇄도/추격/봉쇄`, 웨이브별 증원 최대1회, blanket HP multiplier 없음.
- 정상 Mythic III 정확히3 affix만 대량 자원으로 4-affix 각성 신화 승격 가능.

## 안전 계약
- Shift 정밀모드 우선.
- 대형 채굴/벌목/농사/건축은 bounded tick queue.
- 추가 파괴는 정상 `player.gameMode.destroyBlock`.
- 건축/재파종은 실제 재료 소비 + 보호 훅.
- 지령/사건 진행은 현재 biome family + world stage를 서버가 다시 검증한다.
- 혼합 지령은 모든 task를 충족해야 completedMask를 세운다.
- 지령 선택은 발견 시1회 persist하며 재입장 reroll 없음.
- 지역 XP/사건 보상/마일스톤은 각각 독립 영구 비트로 1회 지급.

## 외부 소스 정책
- Enhanced Celestials Tweaks(MIT): temporary event가 spawn/duration/reward/lifecycle modifier를 묶는 제품 구조만 연구. 0.26은 독립 incident catalog/스폰/보스바/persistence/reward 구현이며 소스·config·asset을 복사하지 않는다.
- Majrusz's Progressive Difficulty: Undead Army 같은 rare forced encounter pacing만 reference-only. 현재 공개 저장소 root에서 license 파일을 확인할 수 없어 소스/에셋/데이터를 사용하지 않는다.
- Gateways to Eternity(MIT): 시련 순차 웨이브/상태/변형 아이디어 적응, 기존 MIT notice 패키징 유지.
- Apotheosis(MIT), Lootr(MIT) 등 기존 허용 범위는 이전 고지 유지.
- Bountiful(GPL-3.0), FTB Quests(All Rights Reserved), Repurposed Structures/Compass 계열 등은 UX/행동 구조 reference-only로 유지한다.
