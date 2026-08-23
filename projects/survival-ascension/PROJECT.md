# Survival Ascension

- Mod version: `0.27.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: 기존 `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, Elite/Warband/종말 변이 persistent NBT, affix CustomData와 채굴 모드를 유지한다. 0.27은 기존 `infrastructure_v1`에 새 프로젝트 ID의 funding key만 추가하므로 과거 프로젝트 진행을 건드리지 않으며, 정점 도감/승리는 새 독립 SavedData `apex_hunt_v1`에 저장한다.

## 핵심 방향
숙련 상승은 단순 수치 증가가 아니라 물리적 작업 규모와 행동 선택지 증가다. 플레이어가 강해질수록 적의 행동/조합, 세계 진행, 탐험 목표, 인프라와 자원 소비처도 함께 커진다. 0.27의 정점 강적은 HP만 큰 몹이 아니라 위치 변경과 전조 대응을 요구하는 행동 패턴을 가진다.

## 0.27 정점 추적소 / Apex Hunts
### 인프라 게이트
- `APEX_TRACKING_POST / 정점 추적소`는 월드 승천 Stage1부터 공동 건설 가능하다.
- 건설비: 철512 · 금256 · 자수정256 · 메아리32 · 네더의 별1.
- `infrastructure_v1`의 기존 string funding key 구조를 그대로 사용한다. 새 enum/project ID 추가 외 스키마 변경 없음.
- 완공 뒤 이미 완수한 원정권 안에서 `M → 인프라 → 정점 추적소`를 다시 선택하면 그 지역 정점 사냥을 시작한다.
- 반복 추적비: 메아리8 · 자수정32 · 금32.

### 9개 지역 정점
- 삼림 `수림 파쇄자`: ravager + vindicator/zombie 호위. `CHARGE` — 1초 전조 뒤 고속 돌진, 근접 충돌 시 강한 밀치기.
- 건조 `황야 지휘관`: husk + skeleton/pillager. `REINFORCE` — HP70%/35%에서 각각 bounded 증원2.
- 습지 `늪지 역병핵`: zombie + cave spider/witch. `PLAGUE` — 근접 독기장, 플레이어 중독 + 보스 소량 회복.
- 고산 `능선 사냥꾼`: stray + stray/skeleton. `SKIRMISH` — 측면 이동과 거리 교정으로 정적 근접 추격을 방해.
- 대양 `심해 압제자`: elder guardian + guardian. `PULL` — 중거리 플레이어를 다시 보스 사거리로 끌어당김.
- 심층 `심층 추적자`: spider + cave spider/skeleton. `LEAP` — 중거리 장거리 도약.
- 빙설 `빙설 감시자`: stray + stray/zombie. `FROST` — 근거리 강한 단기 Slowness 냉기장.
- 네더 `네더 약탈자`: wither skeleton + blaze/wither skeleton. `WITHER` — 근거리 Wither 파동과 위치 밀어내기.
- 엔드 `공허 전조자`: enderman + shulker/enderman. `VOID` — 근거리 Levitation 파동 + 공격적 추격.
- 보스 체력/방어/공격 추가량은 archetype별 개별 값이며 blanket HP multiplier를 쓰지 않는다.

### 사냥 생명주기 / 안전
- survival player만 시작 가능, Stage>=1, 정점 추적소 완공, 현재 지역 지령 완수가 모두 필요하다.
- 사냥 제한시간90초 (`HUNT_TIMEOUT_TICKS=1800`).
- 소유자가 사망/관전/차원 이탈/현재 원정권 이탈/중심64블록 이탈 상태를 10초 지속하면 실패한다.
- 같은 레벨 정점 사냥 중심 간 최소96블록을 요구한다.
- 호위가 중심48블록 밖으로 나가면 navigation recall한다.
- 정점/호위는 `EntitySpawnReason.TRIGGERED`로 생성되고 owner/type persistent tag를 붙인다.
- 실패/성공/로그아웃 때 추적 몹과 bossbar를 정리한다. 서버 재시작 뒤 ACTIVE에 없는 tagged orphan이 join하면 취소한다.
- 시작 시 현장 사건 ready tick을 사냥 종료 가능 시간 뒤로 밀어 사건이 같은 플레이어에게 중첩되지 않게 한다.
- 현장 사건 진행 중에는 사냥을 시작할 수 없다.
- 정점 사냥 진행 중에는 승천 중추에서 승천 시련을 시작할 수 없다.
- 최근/진행 가능한 승천 시련 ready window가 남아 있는 동안 정점 사냥 시작도 막는다.

### 보상 / 장기 자원 싱크
- Stage1 승리: 승천 II affix 장비1 · 다이아2 · 메아리4 · XP120.
- Stage2 승리: 기본 승천 II, 20% 확률 신화 III · 다이아3 · 메아리6 · 네더라이트 파편1 · XP180.
- 반경48 내 생존 협동 플레이어는 owner loot 복제 없이 XP50.
- 반복 추적비가 메아리8이므로 일반 반복 성공도 메아리 순소비가 남는다.
- 승천 시련은 Stage2에서 신화 III를 100% 보장하므로 정점 사냥이 deterministic 최종 전리품 루프를 대체하지 않는다.
- `apex_hunt_v1`: UUID별 `defeated` 9-bit mask · `victories` · `mastery_claimed`.
- 9종 최초 격파 완주: 신화 III1 · 네더라이트 파편4 · 메아리32 · 드래곤숨결16 · XP500, 1회.
- `/ascension stats`: 정점 최초 격파 x/9 + 총 승리 + 9종 완주 보상 상태.

## 0.26 희귀 현장 사건
### 발생/수명
- 발견된 원정권 안의 생존 플레이어를 30초 간격으로 검사한다.
- 조건 충족 시 10% 확률로 지역 사건을 시작한다.
- 사건은 45~60초이며 `ServerBossEvent`로 남은 적/행동량/시간을 표시한다.
- 사건 시작점 반경48블록 또는 해당 원정권을 10초 이상 벗어나면 실패한다.
- creative/spectator는 사건 시작/진행 대상에서 제외한다.
- 승천 시련의 persisted start-ready tick 주변 구간에는 사건 체크를 차단해 승천 시련과 현장 사건 전투가 중첩되지 않게 한다.
- 실패 시 지령 진행도 손실 없음. 사건 자체는 cooldown 뒤 재발생 가능하다.

### 지역별 18개 사건
각 원정권은 습격형1 + 긴급작업형1을 가진다.

Stage 0:
- 삼림 `수림 습격`: zombie/spider 6체 / `벌목 비상`: 실제 자연 로그 일괄 벌목24.
- 건조 `약탈대 급습`: husk/pillager 6체 / `긴급 보급선`: 실제 scaled Construction 성공 배치24.
- 습지 `늪지 습격`: zombie/spider 6체 / `긴급 수확`: 성숙 작물20.
- 고산 `능선 매복`: pillager/skeleton 6체 / `능선 돌파`: 서버 검증 성공 R 돌진4.
- 대양 `익사자 습격`: drowned 6체 / `폭풍 항해`: 수영/수중/탑승 항해180.

Stage 1:
- 심층 `심층 군집`: zombie/skeleton/spider 7체 / `붕괴 전 채굴`: pickaxe-valid block48.
- 빙설 `설원 습격`: stray/skeleton 6체 / `빙설 강행군`: legitimate travel180.
- 네더 `네더 급습`: wither skeleton/blaze 8체 / `열기 속 채굴`: pickaxe-valid block48.

Stage 2:
- 엔드 `공허 습격`: endermite/shulker 7체 / `공허 추적`: legitimate travel180.

### 사건 보상/안전
- 습격은 bounded `EntitySpawnReason.TRIGGERED`, 생성 요청의 2/3 미만이면 취소/정리, 대양은 water spawn slot만 사용.
- 실패/로그아웃/stale server에서 추적 몹과 bossbar 정리.
- `incident_rewards` bitmask는 UUID별/지역별 최초 해결 보상을 영구 기록한다.
- Stage0 XP100+에메랄드4+자수정8, Stage1 XP150+다이아2+메아리4, Stage2 XP200+다이아4+메아리8.
- 미완료 지령의 첫 task에 최대 target20% 보너스, 지역당 최초 사건 보상 1회만 적용.

## 0.25 지역 현장 지령
- 9개 원정권마다 표준/혼합 2개, 총18개 지령. 최초 발견 때 플레이어별 랜덤 선택 후 `expedition_v1.directives`에 persist하며 재입장 reroll 없음.
- 혼합 지령은 모든 task를 완료해야 region complete.
- 실제 smart-tree 로그, material/protection-backed 건축, 성숙 작물, legitimate travel, ocean voyage, pickaxe destroy, hostile kill, 성공 R dash만 기록.
- 0.24 legacy progress/complete/reward/milestone migration 유지. 0.23 master milestone 보유자는 Field Mastery 유지.

## 기본 숙련 VI · Lv.100
- 채굴11×11, 광맥/추출192, 채석장7×7×10.
- 벌목384, 12/player·64/global.
- 농사11×11, 관개 재파종 실제 씨앗 소비.
- 전투 파급10체/5블록/70%, 훈련장6.5/16/55%/50틱.
- 건축 선49, 면11×11, 입체7³.
- 기동 단차2, 안전낙하16, dash1.80/16틱, Stage2+중추 공중돌진3회.

## 현장 숙련 · Field Mastery
조건: Stage2 + 9개 지역 지령 완수. 해당 숙련 Lv.100에서만 추가 적용.
- 채굴 터널7×7×12, pending640/처리12 player·64 global.
- 벌목448, 잎 검증/12·64 큐.
- 농사13×13, pending384/12·64 큐.
- 전투 충격파7.5/20, 피해55%/50틱 유지.
- 건축 선65/면13×13, 입체7³, 재료/보호/global64/pending512.
- 기동 공중돌진4회, 착지 초기화/기존 쿨 유지.

## 기존 종말 루프
- 각성0 → 최초 Wither 전설1 → 최초 Ender Dragon 종말2.
- Stage2 자연 생성 일부 Withered/Phase/Plague 변이, Elite/Warband 중첩 가능.
- Stage2 + 승천 중추 완공 후 4웨이브 승천 시련. 교리 `쇄도/추격/봉쇄`, 웨이브별 증원 최대1회, blanket HP multiplier 없음.
- 정상 Mythic III 정확히3 affix만 대량 자원으로 4-affix 각성 신화 승격 가능.

## 안전 계약
- Shift 정밀모드 우선.
- 대형 채굴/벌목/농사/건축 bounded tick queue.
- 추가 파괴 정상 `player.gameMode.destroyBlock`.
- 건축/재파종 실제 재료 소비 + 보호 훅.
- 지령/사건/정점 사냥은 현재 world stage/region/server state를 다시 검증한다.
- 지역 XP/사건 보상/마일스톤/정점 9종 완주는 각자의 persistent 상태로 중복 지급 방지.

## 외부 소스 정책
- Apotheosis: 공식 `Shadows-of-Fire/Apotheosis` GitHub `26.1` branch의 code LICENSE는 MIT. 기존 affix 코드 패턴 고지 유지. CurseForge 배포 페이지/자산 권리는 별도 취급하고 Apotheosis asset/data를 복사하지 않는다.
- Silent Gear(MIT): 장기 장비 성장에서 자원이 계속 의미를 갖게 하는 제품 철학만 0.27 설계 참고. 소스/파트/재료/blueprint/data/assets 미복사.
- Enhanced Celestials Tweaks(MIT): temporary event 제품 구조 reference-only.
- Majrusz's Progressive Difficulty: rare encounter pacing reference-only, 명확한 재사용 라이선스가 확인되지 않은 코드는 미사용.
- Gateways to Eternity(MIT), Skill Proficiencies(MIT), Veinminer++(MIT), MineMenu(MIT), Building Gadgets 2(MIT), Mob Champions(MIT), Mekanism(MIT), Warband(MIT) 기존 허용 패턴/notice 유지.
- Bountiful(GPL-3.0), FTB Quests(ARR), Repurposed Structures/Compass 계열 등은 UX/행동 구조 reference-only.
