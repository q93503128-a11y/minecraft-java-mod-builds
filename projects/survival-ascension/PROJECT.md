# Survival Ascension

- Mod version: `0.25.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: 기존 `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, Elite/Warband/종말 변이 persistent NBT, affix CustomData와 채굴 모드를 유지한다. 0.25는 `expedition_v1` player entry에 optional `directives` map을 추가하고 기존 0.24 single-objective progress를 표준 지령으로 migration한다.

## 핵심 방향
숙련 상승은 단순 수치 증가가 아니라 물리적 작업 규모와 행동 선택지 증가다. 플레이어가 강해질수록 적의 행동/조합, 세계 진행, 탐험 목표, 인프라와 자원 소비처도 함께 커진다. 원정 역시 체크리스트가 아니라 이미 배운 벌목·건축·농사·기동·채굴·전투를 지역 상황에 맞게 다시 조합하는 콘텐츠로 유지한다.

## 0.25 지역 현장 지령
### 구조
- 9개 원정권마다 2개 지령, 총 18개.
- 새 지역 최초 발견 시 플레이어별로 2개 중 하나를 server RNG로 선택하고 `expedition_v1.directives`에 영구 저장한다.
- 표준 지령은 0.24의 기존 단일 목표와 동일하다.
- 혼합 지령은 2개의 서로 다른 task를 모두 완료해야 지역 완수 처리된다.
- 지역 이탈/재진입/서버 재시작으로 지령은 reroll되지 않는다.
- `/ascension stats`에서 발견/완수와 현재 지령 이름, task별 현재값/목표값을 확인한다.

### 18개 지령
Stage 0:
- 삼림 `거목 정리`: 자연 로그96.
- 삼림 `수림 개척`: 자연 로그64 + 정상 도보 이동240.
- 건조 `전초 건설`: 대량 건축128.
- 건조 `사막 보급로`: 대량 건축96 + 정상 도보 이동240.
- 습지 `습지 수확`: 성숙 작물96.
- 습지 `습지 정비`: 성숙 작물64 + hostile8.
- 고산 `능선 횡단`: 정상 도보 이동600.
- 고산 `능선 돌파`: 정상 도보 이동360 + 성공한 R 돌진12.
- 대양 `해양 항로`: 수영/수중/탑승 항해800.
- 대양 `심해 순찰`: 항해500 + hostile8.

Stage 1:
- 심층 `심층 채굴`: pickaxe-valid block192.
- 심층 `심층 개척`: pickaxe-valid block128 + hostile10.
- 빙설 `설원 횡단`: 정상 도보 이동600.
- 빙설 `빙설 돌파`: 정상 도보 이동360 + 성공한 R 돌진12.
- 네더 `네더 토벌`: hostile24.
- 네더 `네더 보급전`: hostile16 + pickaxe-valid block96.

Stage 2:
- 엔드 `엔드 토벌`: hostile32.
- 엔드 `공허 전진`: hostile20 + 정상 도보 이동360.

## task 출처 계약
- `LOGS_FELLED`: Veinminer++식 잎 검증을 통과한 smart-tree 대량 벌목 큐에서 실제 성공한 자연 로그 destroyBlock.
- `BLOCKS_BUILT`: 실제 재료가 소비되고 mayInteract/NeoForge placement hook을 통과한 scaled Construction secondary placement.
- `CROPS_HARVESTED`: 실제 성숙 작물 파괴. 12/player·64/global 틱 큐 후속 수확도 블록 단위로 기록.
- `TRAVEL_DISTANCE`: 기존 Mobility legitimate sprint tracker. 탑승/비행/겉날개/수영/비정상 대이동 제외.
- `OCEAN_VOYAGE`: 대양권에서 수영/수중/탑승 상태의 수평 이동만 인정. 1초 displacement 24블록 초과 거부.
- `BLOCKS_MINED`: 정상 pickaxe-valid block break. 광역/광맥/추출/터널 후속 destroyBlock도 블록 단위로 기록.
- `HOSTILES_KILLED`: 플레이어 직접 처치 + `Enemy`만 인정.
- `DASHES_USED`: 서버가 cooldown/공중 횟수/상태 검증을 끝내 실제 impulse를 적용한 성공한 R 사용만 기록.

## 0.24/0.23 migration
- `expedition_v1` ID 유지.
- 0.24에서 발견된 region에 `directives` 값이 없으면 index0 표준 지령을 자동 부여한다.
- 0.24 legacy region progress를 표준 지령 첫 task progress로 이관한다.
- 0.24 completedMask는 그대로 보존하고 해당 표준 지령 task를 완료 상태로 채운다.
- `region_rewards`가 없던 0.23 저장은 당시 discoveredMask를 이미 XP 지급된 상태로 migration하므로 숙련 XP 중복 지급 없음.
- 0.23/0.24 `MILESTONE_MASTER` 보유자는 9개 completed 상태로 유지해 현장 숙련을 잃지 않는다.
- 기존 milestoneMask가 자원/Mythic 보상 재지급을 차단한다.

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
- 지령 진행은 현재 biome family + world stage를 서버가 다시 검증한다.
- 혼합 지령은 모든 task를 충족해야 completedMask를 세운다.
- 지령 선택은 발견 시 1회 persist하며 재입장 reroll 없음.
- 지역 XP/마일스톤은 영구 비트로 1회 지급.

## 외부 소스 정책
- Gateways to Eternity(MIT): 시련의 순차 웨이브/상태/변형 아이디어 적응, 기존 MIT notice 패키징 유지.
- Apotheosis(MIT): rarity/category/affix 분리 철학 적응.
- Lootr(MIT): 플레이어별 탐험 보상 공정성 설계만 참고.
- Bountiful(GPL-3.0): 가변 bounty/목표/보상 계약 철학만 reference-only. 소스/에셋/데이터/namespace 미사용.
- FTB Quests(All Rights Reserved): 한 퀘스트의 multi-task, persistent progress, stage reward 구조만 reference-only. 소스/에셋/UI/quest data/namespace 미사용.
- Repurposed Structures/Compass 계열 등 제한 라이선스 프로젝트는 탐험 동기/UX만 참고하고 구현·에셋을 복사하지 않는다.
