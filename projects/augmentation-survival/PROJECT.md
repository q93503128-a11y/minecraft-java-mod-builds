# Augmentation Survival (working title)

> 정식 게임명은 아직 정하지 않았다. `Augmentation Survival`과 slug `augmentation-survival`은 저장소 내 작업 식별을 위한 임시 명칭이다.

- Slug: `augmentation-survival`
- Mod ID: `augmentation_survival` (working technical id)
- Namespace: `augmentation_survival`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `Fabric`
- Loader version: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Gradle: `9.5.1`
- Build plugin: `Fabric Loom 1.17.20`
- Final JAR: `augmentation-survival-0.1.0-alpha.1.jar` (development target; not release-ready)
- Existing-world compatibility: 가능한 한 유지하되 Terralith는 새 월드 기준. 월드젠 외부 모드 교체/제거는 별도 migration 계약 없이는 지원하지 않는다.
- Required dependency: Fabric API
- Official pack candidate mods: `MODPACK_BASELINE.md` 참조
- Forbidden bundled dependencies: 재배포 권한이 불명확한 모드/자산을 저장소 JAR에 직접 번들하지 않는다. 외부 모드는 원본 배포처를 참조하는 모드팩 의존성으로 관리한다.
- Datagen task: 아직 미구성
- GameTest task: 아직 미구성
- Server smoke-test task: `runPackServer -PaugmentationPackRuntime=true`
- Client smoke-test task: `runPackClient -PaugmentationPackRuntime=true` + 실제 화면 검수

## 현재 상태

`M1C CORE ECONOMY + DEVICE + SHARED GAMEPLAY RUNTIMES / SAVE-WIDE THREAT + EXTERNAL BOSS COMPATIBILITY / 119-OF-146 GAMEPLAY RUNTIME VERIFIED`

외부 모드팩 M0 부팅/의존성 기준선 위에 실제 Java runtime 구현을 진행 중이다. 현재는 **146종 runtime catalog, 영구 플레이어 증강 상태, 세이브 전역 위협 high-water와 실제 hostile stat application, 외부 보스 공통 identity adapter, 코어 획득 경로, 기본/강화 증강 장치, 3후보 선택 transaction, 두 번째 선택, 3:1 코어 합성**까지 기능 기준선이 구현되어 있다.

Reference vertical slice는 증강 1~2개를 독립 구현하는 방식이 아니라 shared-system batch로 진행한다. stat → combat/protection/heal/death → movement/aerial → projectile shot-action/flight-segment → active/combat expansion → survival/resource → harvest/drop → consumption/fishing/breeding → save-wide threat application → external boss compatibility → reactive/status/mobility/utility bulk → advanced utility/permanent progression → special temporal/mythic active → advanced active/interact → advanced world/control 순으로 공통 런타임 경계를 확장했다.

현재 146종 기준 gameplay runtime 진행도:

- 완전 구현: **119 / 146**
- 부분 구현: **5 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`, `지배`
- 완전 구현되지 않음: **27 / 146**
- gameplay runtime 자체가 아직 없음: **22 / 146**
- 최신 batch 신규 완전 구현: **3종** — `위상 이동`, `거대화`, `금단의 마법`
- 최신 batch 신규 부분 구현: **1종** — `지배`
- 이전 source-consumer audit에서 기존 완전 구현으로 재분류: **1종** — `피해 적응`

여기서 “완전 구현”은 canonical gameplay 효과의 runtime 구현을 뜻한다. 실제 플레이테스트, 최종 VFX/사운드/HUD/UI polish까지 끝났다는 뜻은 아니다.

### 현재 shared runtime 요약

- stat / combat / protection / healing / lethal 공통 파이프라인
- reactive/status/protection/path-echo/invincibility 공통 런타임
- armor-effectiveness / permanent hunt-record combat bridge
- movement / aerial / flowing-water / mount / tree-climb / mantle 공통 런타임
- projectile shot-action / flight-segment + focused-aim snapshot 런타임
- 10-slot server-authoritative active engine + toggle/one-use/special/world-control routing
- non-mythic active cooldown / haste / sequential double-charge / combat-cycle runtime
- rolling player-history rewind + sustained-active instance extension runtime
- concentration channel + hostile control/stun + projectile/status-timer freeze runtime
- temporal client prediction for item-use duration + owned-projectile launch-speed scaling
- phase-state S2C prediction + server/client collision parity runtime
- player scale / max-health / reach / attack-damage transient transform runtime
- persistent dominated-entity owner state + save-wide domination occupancy runtime
- entity-load tracked dominated-companion AI + allied/friendly-fire bridge
- Villager final emerald-price bargaining bridge
- anvil exclusivity / prior-work / Too-Expensive override bridge
- `SPECIAL_PERCENT_HP` custom damage runtime with boss control-profile gates
- survival/resource + harvest/derived-break + food/fishing/breeding 런타임
- pickup / machine / death-XP / inventory-preservation utility 런타임
- vanilla item-use integration for ender-pearl cooldown/self-damage and portable Riptide
- save-wide threat 0~8 live hostile scaling
- external boss common identity/control-profile runtime

latest advanced world/control pass에서는 `위상 이동`, `거대화`, `금단의 마법`을 완전 구현하고 `지배`의 영구 동료 baseline을 부분 구현했다. `위상 이동`은 5초 동안 수평/상향 고체 충돌을 무시하되 하향 충돌은 유지하고, 공격·채굴·설치·아이템/엔티티/블록 상호작용을 잠근다. 서버 상태를 클라이언트로 동기화해 로컬 이동 예측도 같은 충돌 규칙을 사용하며, 종료 시 안전 위치를 보정한다. 전설 비신화 액티브이므로 `능력 가속`, 순차 `이중 충전`, `전투 순환`, eligible `연장전`에 정식 편입하고 `재연`에서는 제외했다.

`거대화`는 20초 동안 크기, 최대/현재 체력, 근접 사거리, 공격 피해를 canonical 값으로 변경하고 근접 넉백을 2배로 한다. Lv.2는 반경 8블록 일반 비보스 적에게 2초 공포 이동을 적용한다. 체력 비율은 진입/종료 시 보존하고 종료 충돌도 안전 보정한다. 신화이므로 일반 액티브 cooldown meta는 적용하지 않는다.

`금단의 마법`은 모루의 기본 아이템/인챈트 적용 가능성은 유지하면서 상호 배타 인챈트 제한만 완화하고, Too Expensive 제한과 prior-work 누적 비용을 제거한다. 실제 수리/인챈트/이름 변경 기본 비용은 유지한다.

`지배`는 반경 24블록의 보이는 비보스 적을 영구 아군화하는 owner attachment, 3/5/10 세이브 전역 점유 cap, 오프라인 owner 상태에서도 죽음 시 슬롯 회수, owner/동료 아군 판정, friendly-fire 차단, hostile 자동 전투 AI, `시간 정지`/`천벌` 제외를 구현했다. 다만 기존 일부 projectile/AoE/pull 코드가 여전히 직접 `instanceof Enemy`를 사용하므로 지배 동료가 후보/카운트 슬롯에 들어가는 경계가 남아 있다. script-owned/special entity 예외도 하나의 공통 predicate로 아직 전수 정규화되지 않아 **PARTIAL**로 유지한다.

이전 advanced active/interact pass의 `되감기`, `시간 가속`, `연장전`, `방패 던지기`, `흥정`, `매달리기`, special temporal/mythic pass의 `귀환`, `시간 정지`, `천벌`, advanced utility/permanent progression의 `방어 무시`, `사냥 기록`, `나무타기`, `진주 숙련`, `휴대 수류`, `사망 지점 귀환` 등 기존 구현은 그대로 유지한다.

### Save-wide threat runtime

기존 high-water 점수를 실제 적 스탯에 연결한다. stage 상승 시 loaded scalable entity를 한 번만 reconcile하고, 새로 load/spawn되는 적은 current stage를 받는다. ordinary hostile은 canonical normal curve, Warden/Elder Guardian은 65% extra elite curve를 사용한다. boss는 35% extra HP / 30% extra damage midpoint curve와 encounter-stage snapshot을 사용한다. HP 비율은 max-health 변경 시 보존하고 normal/elite에는 persistent entity threat attachment를 남기지 않는다. hostile damage는 server pre-mitigation `DamageSource` 경계에서 한 번만 배율되며 `DELAYED_DAMAGE`에는 재적용하지 않는다.

위협 분류는 코어 보상용 boss assignment와 분리한다. boss identity가 core reward를 자동 생성하지 않으며, difficulty/control/loot-exclusion identity와 reward probability는 별도 계약이다.

### External boss compatibility runtime

`entity_compatibility_profiles.json`을 공통 identity/control-policy 정본으로 사용한다.

현재 exact boss identity:

- `minecraft:wither`
- `minecraft:ender_dragon`
- Chrono Dawn 6종: `time_guardian`, `chronos_warden`, `clockwork_colossus`, `entropy_keeper`, `temporal_phantom`, `time_tyrant`

Pinned World Bosses 2.0.2는 서버 registry에서 token selector를 exact ID로 resolve하며 최신 full-pack checkpoint에서도 11/11이 bind된다. MCEndgame는 entity type 자체가 boss 여부를 결정하지 않으므로 pinned runtime의 `MobEntityDungeonBossAccessor#mcendgame$isDungeonBoss` dynamic adapter를 사용한다.

동일 identity source는 threat boss curve/snapshot, `우두머리 사냥`, `전투 순환`, `연장전`, normal-mob 전용 loot exclusion, boss pull scalar, `위협 억제`, generic boss-origin status handling, `시간 정지`, `천벌`, `지배` target exclusion에 재사용된다.

Compatibility contract는 pull/swap/time-stop/stun/percent-HP/wall-impact/dominate 제어 필드를 제공한다. 실제 구현된 능력만 enforcement된 것으로 간주하며 미래 능력은 profile 필드가 있다는 이유만으로 완료 처리하지 않는다.

common active engine은 `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`, `방패 던지기`, `되감기`, `시간 가속`, `위상 이동`과 `능력 가속`, `이중 충전`, `재연`, `전투 순환`, `연장전`을 처리한다. toggle routing은 `위협 억제`, `순간 채굴`, one-use routing은 `사망 지점 귀환`, no-cooldown concentration routing은 `귀환`, mythic/world routing은 `시간 정지`, `천벌`, `지배`, `거대화`를 처리한다.

최신 검증 gameplay-code checkpoint `3ebeb3000a618fec998c8bd2cad4a1c05fbbcdd7`는 Build Augmentation Survival Pack #72 (`40145505210`)에서 build, 23개 pinned pack dependency inspection, 150-file runtime classpath verification, dedicated-server smoke, client boot smoke를 모두 통과했다. 서버는 92 mods, 클라이언트는 108 mods로 full-pack 부팅했다. 검증 artifact는 `augmentation-survival-pack-smoke-72` / ID `12580571756`, ZIP SHA-256 `fe3e2ed9e9120d7cf2bfc2b822f0d2ddaac0d1b9b04f905d4aae34daad164b58`이다.

새 advanced world/control runtime의 실제 조작감·예측·AI·모루 edge case, 이전 액티브/투사체/이동/보스/threat 시스템과 multiplayer correctness는 아직 실제 플레이로 검증하지 않았다. 세부 구현 현황은 `IMPLEMENTATION_STATUS_2026-10-24.md`를 따른다.

## 프로젝트 정체성

Minecraft 장기 서바이벌을 유지하면서 다양한 증강을 계속 획득해 플레이어가 매우 강해지는 파워 판타지 모드다.

전투 전용 로그라이크가 아니다. 전투뿐 아니라 채광, 이동, 탐험, 생존, 농사, 낚시, 건축·자원 수집 등 Minecraft의 여러 활동이 증강으로 크게 변해야 한다.

핵심 방향:

- 증강은 미세 보너스가 아니라 플레이 방식을 크게 바꾸는 중심 성장 시스템이다.
- 장기 월드에서 한 번의 운 때문에 빌드가 망가지지 않게 한다.
- 높은 등급 증강도 엔더 드래곤 처치 이후에만 열지 않는다.
- 특정 장소 하나가 특정 코어 등급 하나만 독점하지 않는다. 장소마다 여러 인접 등급의 코어가 서로 다른 확률로 나온다.
- 외부 모드는 증강을 사용할 세계와 콘텐츠를 확장해야 하며, 별도 성장 시스템으로 증강과 경쟁하지 않는다.
- 강한 증강 조합은 의도적으로 허용하고 무한/기하급수 재귀만 차단한다.
- 적은 플레이어 성장을 1:1 상쇄하지 않고 세이브 전체 위협 단계로 부분 추격한다.
- 코어는 제작/탐험/전투/낚시/보스/합성으로 분산 획득하며 한 경로가 전체 경제를 독점하지 않는다.

## 정본 우선순위

프로젝트 내부 기본 우선순위:

1. `AUGMENTS.md`
2. `AUGMENT_PROPOSAL_HISTORY.md`
3. `GAME_DESIGN.md`
4. `PROJECT.md`
5. `MODPACK_BASELINE.md`
6. 실제 source/resource
7. 최신 playtest feedback
8. 과거 대화

세부 구현 문서인 `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md`, `CORE_ACQUISITION_DESIGN_2026-10-24.md`, `AUGMENT_FINAL_COMPATIBILITY_AUDIT_2026-10-24.md`, `DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md`, `IMPLEMENTATION_STATUS_2026-10-24.md`는 위 정본을 구체화한다. 충돌 시 위 기본 우선순위를 따른다.

저장소 공용 `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`는 상위 공통 작업 계약으로 항상 함께 적용한다.

## 현재 확정된 핵심 규칙

- 등급: `일반 → 고급 → 희귀 → 영웅 → 전설 → 신화`
- 현재 확정 로스터: **146종**
  - 일반 20
  - 고급 27
  - 희귀 30
  - 영웅 40
  - 전설 20
  - 신화 9
- 각 증강의 최대 레벨과 현재 효과는 `AUGMENTS.md`에 고정한다.
- 등급별 증강 코어가 존재한다.
- 증강 장치에 코어를 넣으면 같은 등급의 유효한 후보 3개를 제시한다.
- 기본 선택은 1개, 신화 `두 번째 선택` 보유 시 서로 다른 2개를 한 transaction으로 획득한다.
- 이미 최대 레벨인 증강은 후보 풀에서 제거한다.
- 특정 등급 전체를 최대치까지 획득했다면 그 등급 뽑기는 불가능하며 코어도 소비하지 않는다.
- 동일 등급 코어 3개를 바로 위 등급 코어 1개로 합성할 수 있다.
- 코어 합성은 강화 증강 장치에서 제공한다.
- 일반 코어는 레드스톤/자수정/구리 레시피로 확정 제작 가능하다.
- 자연 구조물은 기존 loot를 유지하면서 위험도별 추가 core roll을 가진다.
- 일반 적대몹은 매우 낮은 확률의 저등급 core roll만 가지며 Looting/풍요 등으로 코어를 복제하지 않는다.
- 정식 보스는 코어 보상을 보장하고 플레이어별 최초 처치에는 추가 코어 1개를 지급한다.
- 신화 직접 획득은 pinnacle 콘텐츠에서만 낮은 확률로 허용하며 전설 3:1 합성으로도 확정 접근 가능하다.
- 총 증강 보유 개수 제한은 없다.
- 액티브는 일반 10칸 빠른 슬롯을 사용하고 `권능 탈취`는 별도 전용키 하나를 사용한다.
- 월드 위협 단계는 지역/플레이어별이 아니라 **세이브 전체 공통 0~8**이다.

아직 고정하지 않은 것:

- 일부 구조물/외부 보스의 개별 risk/reward profile 세부 매핑
- 일부 증강의 세부 파라미터 (`탄도 장막`, `임시 발판`, `생명 연결`, `작아지기`, `벽치기`)
- `물약 확산`의 정확한 주변 범위
- `손재주`의 비전투 수집 whitelist
- `탄성 저장`의 낙하거리→추진력 환산 기준
- 위협 단계 수치 테이블의 최종 플레이테스트 값
- 코어 획득 확률의 최종 플레이테스트 튜닝 값

현재 코어 획득 확률은 구현 가능한 초기 기준값으로 고정하고 실제 플레이테스트에서 조정한다.

## 외부 모드 연동 계약

- 외부 모드 JAR 자체를 수정하는 방식보다 우리 모드의 loot/event/data/profile/adapter 레이어를 우선한다.
- 외부 구조물 상자에는 원본 loot를 보존하면서 증강 코어 pool을 추가한다.
- 외부 몹은 loot table 또는 서버 권한 이벤트를 통해 코어 드랍을 추가한다.
- `장소 = 단일 등급`이 아니라 `장소 = 여러 등급의 확률 분포`로 설계한다.
- 호환 데이터는 가능한 한 data-driven으로 분리한다.
- Better Combat은 하나의 서버 ROOT melee hit로 정규화한다.
- Chrono Dawn / World Bosses / MCEndgame 보스는 control/reward/authority/threat profile을 분리하되 identity resolver는 공유할 수 있다.
- boss identity는 boss core reward를 자동으로 의미하지 않는다.
- Farmer's Delight는 작물/음식/생산 adapter로 연결한다.

## 구현 시작 순서

세부 계획은 `DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md`를 따른다.

1. Runtime/state foundation
2. Core item / acquisition profile / loot-event reward layer
3. Core / device / 3-choice transaction / functional UI
4. Reference vertical slice: 스탯·회복·사망·이동·투사체·액티브·자원 최소 한 경로
5. 일반·고급 bulk implementation
6. 희귀·영웅 복잡 파이프라인
7. 전설·신화 및 공유 저장/권능
8. save-wide threat scaling gameplay application
9. 외부 모드 adapter 확장
10. HUD/VFX/animation/sound polish
11. gameplay/performance/multiplayer validation

현재 1~4 기능 기준선, 8번 save-wide threat gameplay application baseline, 9번의 핵심 external boss identity adapter baseline과 **119종 gameplay runtime**까지 구현되어 있다. 앞으로도 한 증강씩 독립 하드코딩하지 않고 현재 공통 기반으로 구현 가능한 쉬운/중간 난도 증강을 먼저 전수 분류하고, 같은 runtime boundary에 맞는 것은 한 번에 전부 연결하는 대형 묶음으로 진행한다.

다음 pass는 먼저 **`지배`의 shared hostile-selector normalization**을 마쳐 PARTIAL을 FULL로 올릴 수 있는지 검증한다. 그 다음 남은 **22종 no-runtime**을 실제 source consumer와 다시 대조해 scanner/inventory/enchantment/companion/world 계열 중 가장 밀도가 높은 공통 subsystem을 선택한다. 세부 의미가 아직 고정되지 않은 증강은 임의 수치나 동작을 만들어 넣지 않는다.

## 현재 검증 상태

검증된 최신 gameplay-code 체크포인트:

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `3ebeb3000a618fec998c8bd2cad4a1c05fbbcdd7`
- JAR PRODUCED: YES — Actions run `40145505210`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENTITY COMPATIBILITY CONTRACT INITIALIZATION: PASS — 8 exact / 11 token / 1 dynamic
- WORLD BOSSES LIVE REGISTRY PROFILE BINDING: PASS — 11 / 11
- MCENDGAME DYNAMIC BOSS ADAPTER BINDING: PASS — 1 / 1
- CHRONO DAWN EXACT BOSS PROFILE DEFINITIONS: PASS — 6
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- ADVANCED WORLD / PHASE / GIANT / DOMINATION / FORBIDDEN-MAGIC CONTRACT: PASS
- GitHub Actions: `40145505210` / Build Augmentation Survival Pack #72
- Verification artifact: `augmentation-survival-pack-smoke-72` / ID `12580571756`
- Artifact ZIP SHA-256: `fe3e2ed9e9120d7cf2bfc2b822f0d2ddaac0d1b9b04f905d4aae34daad164b58`

현재 구현 범위:

- ROSTER LOCKED: YES — 146
- GAMEPLAY RUNTIME FULLY IMPLEMENTED: **119 / 146**
- GAMEPLAY RUNTIME PARTIAL: **5 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`, `지배`
- NOT FULLY IMPLEMENTED: **27 / 146**
- NO GAMEPLAY RUNTIME YET: **22 / 146**
- CURRENT BATCH NEW FULL AUGMENTS: **3** — `위상 이동`, `거대화`, `금단의 마법`
- CURRENT BATCH PARTIAL AUGMENTS: **1** — `지배`
- FINAL COMPATIBILITY AUDIT: YES
- INTERACTION CONTRACT V2: YES
- CORE ACQUISITION LOOP: IMPLEMENTED BASELINE
- PERSISTENT PLAYER AUGMENT STATE: IMPLEMENTED
- SAVE-WIDE THREAT STATE / HOSTILE SCALING / BOSS SNAPSHOT: IMPLEMENTED BASELINE / LIVE TEST PENDING
- EXTERNAL BOSS IDENTITY ADAPTER: IMPLEMENTED BASELINE / FULL-PACK BINDING VERIFIED
- CORE / DEVICE / 3-CHOICE TRANSACTION: IMPLEMENTED BASELINE
- SECOND CHOICE ATOMIC 2-PICK: IMPLEMENTED
- 3:1 CORE UPGRADE ONE/ALL: IMPLEMENTED
- COMMON ACTIVE-ABILITY ENGINE + ADVANCED WORLD ROUTING: IMPLEMENTED BASELINE
- PHASE SERVER/CLIENT COLLISION PREDICTION: IMPLEMENTED BASELINE
- GIANT ATTRIBUTE / FEAR RUNTIME: IMPLEMENTED BASELINE
- DOMINATION OWNER / SAVE-WIDE CAP / ALLY / AI RUNTIME: PARTIAL BASELINE
- FORBIDDEN-MAGIC ANVIL RUNTIME: IMPLEMENTED BASELINE
- FINAL DEVICE/ACTIVE/COMBAT/RESOURCE/THREAT MODEL-TEXTURE-VFX-SOUND-HUD: NOT COMPLETE
- NEW BATCH LIVE-PLAYER TESTED: NO
- PHASE-SHIFT LIVE-PLAYER TESTED: NO
- GIGANTIFICATION LIVE-PLAYER TESTED: NO
- FORBIDDEN-MAGIC LIVE-PLAYER TESTED: NO
- DOMINATION LIVE-PLAYER TESTED: NO
- EXTERNAL BOSS LIVE ENCOUNTER TESTED: NO
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- BOSS GAMEPLAY TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
