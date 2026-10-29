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
- GameTest task: Fabric API GameTest source set enabled; the `build` workflow task graph runs `runGameTest`
- Server smoke-test task: `runPackServer -PaugmentationPackRuntime=true`
- Client smoke-test task: `runPackClient -PaugmentationPackRuntime=true` + 실제 화면 검수

## 현재 상태

`149-AUGMENT RUNTIME ROSTER / GAMEPLAY IMPLEMENTATION COMPLETE / FINAL PLAYTEST AUDIT`

외부 모드팩 M0 부팅/의존성 기준선 위에 실제 Java runtime 구현이 완료되었다. 현재는 **149종 runtime catalog와 모든 확정 증강 효과의 런타임 연결**, 영구 플레이어 증강 상태, 세이브 전역 위협 high-water와 실제 hostile stat application, 외부 보스 공통 identity adapter, 코어 획득 경로, 기본/강화 증강 장치, 서버 권한 3후보 선택 transaction, 두 번째 선택, 3:1 코어 합성, 세이브 전역 증강 저장소가 구현되어 있다. 최종 플레이테스트 전 UI/VFX/audio polish, multiplayer 검증, 실제 게임 내 조작감 검수는 별도 완료 조건이다. 상세 감사 결과는 `FINAL_AUDIT_2026-10-29.md`를 참조한다.

Reference vertical slice는 증강 1~2개를 독립 구현하는 방식이 아니라 shared-system batch로 진행한다. stat → combat/protection/heal/death → movement/aerial → projectile shot-action/flight-segment → active/combat expansion → survival/resource → harvest/drop → consumption/fishing/breeding → save-wide threat application → external boss compatibility → reactive/status/mobility/utility bulk → advanced utility/permanent progression → special temporal/mythic active → advanced active/interact → advanced world/control → area acceleration + normalized companions → perception/revive/external-heal/enchant-refresh → inventory/storage 순으로 공통 런타임 경계를 확장했다.

현재 149종 기준 gameplay runtime 진행도:

- 완전 구현: **149 / 149**
- 부분 구현: **0 / 149**
- 완전 구현되지 않음: **0 / 149**
- gameplay runtime 자체가 아직 없는 증강: **0 / 149**

`IMPLEMENTATION_STATUS_2026-10-24.md`와 `IMPLEMENTATION_STATUS_2026-10-25.md`는 각각 작성 당시의 146종 작업 체크포인트를 보존하는 역사 기록이며 현재 로스터 상태로 사용하지 않는다. 현재 정본 로스터는 `AUGMENTS.md`와 bundled catalog다.

여기서 “완전 구현”은 canonical gameplay 효과의 runtime 구현을 뜻한다. 실제 플레이테스트, 최종 VFX/사운드/HUD/UI polish까지 끝났다는 뜻은 아니다.

### 현재 shared runtime 요약

- stat / combat / protection / healing / lethal 공통 파이프라인
- generic non-natural healing intercept + natural-heal exclusion + overflow/share recursion guard
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
- normalized companion-aware hostility predicate + entity-load tracked companion AI + allied/friendly-fire bridge
- 30-second area acceleration field + compatible growth/machine production hooks
- server-authoritative chest/danger perception snapshots + bounded local exposed-ore scan
- shared outline presentation for ore/chest/hostile/revive markers
- permanent companion death snapshot / timed marker / collision-safe revival runtime
- save-wide persistent pocket/remote storage + UUID segment/group authority + concurrent menu synchronization
- Villager final emerald-price bargaining bridge
- enchant-table fixed-XP-point/lapis refresh transaction + owner-only client control
- anvil exclusivity / prior-work / Too-Expensive override bridge
- `SPECIAL_PERCENT_HP` custom damage runtime with boss control-profile gates
- survival/resource + harvest/derived-break + food/fishing/breeding 런타임
- pickup / machine / death-XP / inventory-preservation utility 런타임
- vanilla item-use integration for ender-pearl cooldown/self-damage and portable Riptide
- save-wide threat 0~8 live hostile scaling
- external boss common identity/control-profile runtime

latest perception/companion/healing/enchant pass에서는 `광석 눈썰미`, `상자 찾기`, `위험 감지`, `소생`, `마법 새로고침`을 신규 완전 구현하고 `회복 증폭`의 외부 회복 누락을 닫았다. `광석 눈썰미`는 6/8/10블록의 노출 광석을 bounded local scan으로 찾아 청록 outline으로 표시한다. `상자 찾기`는 12/18/24블록 내 이미 load된 chunk에서 아직 loot table이 소비되지 않은 자연 loot container만 서버가 선별해 금색 outline 위치를 보낸다. `위험 감지`는 8/12/16블록 내 shared hostile predicate를 통과한 실제 적대 생명체 ID만 서버가 보내고 client entity render state에서 붉은 outline을 적용한다.

`소생`은 20블록 안 길들인/지배 영구 동료의 실제 사망 직전에 상태를 snapshot하고 20초 revive marker를 만든다. 보이는 marker를 선택해 3초 동안 계속 조준을 유지한 뒤 안전 위치에 원래 entity type/state를 복원하고 최대체력 50%로 살린다. 집중 중 시선을 돌리거나 거리/차원 조건이 깨지면 취소되며 성공한 경우에만 120초 cooldown을 소비한다. `능력 가속`, 순차 `이중 충전`, `전투 순환`과 연결하고 replay에서는 제외한다. 지배 동료는 persistent owner attachment와 점유 상태를 복구한다.

`회복 증폭`은 이제 우리 모드 내부 heal뿐 아니라 generic `LivingEntity.heal` 경계의 외부 비자연 회복을 서버에서 한 번만 증폭한다. 자연 허기회복은 별도 natural context로 제외하고, 내부 증강 heal은 managed context로 재증폭을 막는다. 실제 HP 회복량은 기존 overflow/share/healing-trigger 파이프라인에 한 번만 전달한다.

`마법 새로고침`은 마법부여대 화면에서 보유자에게만 refresh control을 노출하되 서버가 최종 검증한다. 실제 XP point 5와 청금석 1개를 소비하고 player/menu enchantment seed를 갱신한 뒤 vanilla menu가 후보 3개를 다시 생성한다.

최신 inventory/storage pass에서는 `주머니`와 `원격 창고`를 FULL runtime으로 올렸다. `주머니`는 레벨에 따라 3/6/9개의 실제 추가 슬롯을 열고 vanilla 인벤토리가 받지 못한 overflow만 저장한다. 실제 사망 시 일반 인벤토리처럼 드롭하되 vanilla `keepInventory`나 `소지품 보존`이면 유지한다. `원격 창고`는 보유자 UUID마다 영구 27칸 세그먼트를 저장하고, 공유 그룹에서는 아이템을 복사/병합하지 않은 채 구성원 세그먼트를 이어 붙여 `27×공유 보유자 수` 용량을 제공한다. 오프라인 구성원 세그먼트도 저장 상태에서 유지되고, 공유 해제 시 각 개인 27칸이 그대로 분리되며, 동시 접근자는 같은 서버 container를 사용해 변경을 공유한다. 현재 공유 그룹 관리는 서버 권한 invite/accept/leave transaction으로 동작하며 전용 시각 UX는 추후 polish 대상이다.

같은 combined source pass에서 기존 main에 먼저 들어온 `지배`의 direct `Enemy` selector 정규화가 완료되어 PARTIAL → FULL로 올라갔고, `영역 가속`도 30초/24블록/×3·×7 성장·처리 가속과 120초 cooldown/meta integration을 포함한 FULL runtime으로 확인했다. 영역 가속은 거대한 매 tick volume scan 대신 각 growth/processing consumer가 기존 처리 경계에서 활성 field multiplier를 질의한다.

이전 advanced world/control pass의 `위상 이동`, `거대화`, `금단의 마법`, advanced active/interact pass의 `되감기`, `시간 가속`, `연장전`, `방패 던지기`, `흥정`, `매달리기`, special temporal/mythic pass의 `귀환`, `시간 정지`, `천벌`, advanced utility/permanent progression의 `방어 무시`, `사냥 기록`, `나무타기`, `진주 숙련`, `휴대 수류`, `사망 지점 귀환` 등 기존 구현은 그대로 유지한다.

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

common active engine은 `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`, `방패 던지기`, `되감기`, `시간 가속`, `위상 이동`, `소생`, `영역 가속`과 `능력 가속`, `이중 충전`, `재연`, `전투 순환`, `연장전`을 처리한다. toggle routing은 `위협 억제`, `순간 채굴`, one-use routing은 `사망 지점 귀환`, no-cooldown concentration routing은 `귀환`, mythic/world routing은 `시간 정지`, `천벌`, `지배`, `거대화`를 처리한다.

현재 로스터와 최근 자동검증 상태는 아래 `현재 검증 상태` 절과 `FINAL_AUDIT_2026-10-29.md`를 따른다. 이전 full-pack 검증 checkpoint의 상세 수치와 artifact SHA-256은 역사 기록으로 그 절에 보존한다.

실제 Minecraft에서의 조작감·표시·AI·회복/마법부여 edge case, 액티브/투사체/이동/보스/threat 시스템, multiplayer correctness는 아직 플레이테스트되지 않았다. 남은 검증 범위는 `FINAL_AUDIT_2026-10-29.md`에 기록한다.

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
- 현재 확정 로스터: **149종**
  - 일반 20
  - 고급 27
  - 희귀 31
  - 영웅 41
  - 전설 20
  - 신화 10
- `GAME_DESIGN.md`의 146종 수량은 2026-10-24 설계 시점 스냅샷이다. 현재 로스터와 효과는 `AUGMENTS.md` 및 `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md`를 따른다.
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

남은 플레이테스트 조정 항목:

- 일부 구조물/외부 보스의 개별 risk/reward profile 세부 매핑
- 위협 단계 수치 테이블의 최종 플레이테스트 값
- 코어 획득 확률의 최종 플레이테스트 튜닝 값
- UI 미술 자산과 세부 연출

증강별 세부 파라미터는 `AUGMENTS.md`, `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md`와 bundled tuning JSON에 고정되어 있다. 코어 획득 확률은 구현 가능한 초기 기준값이며 실제 플레이테스트에서 조정한다.

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

149종 runtime roster 구현은 끝났다. 이후 작업은 새 증강 구현이 아니라 마지막 게임 내 검수, UI/VFX/audio polish, multiplayer 검증, 실제 플레이에서의 balance 및 feel 확인이다.

## 현재 검증 상태

최신 자동검증 gameplay-code 체크포인트는 `3ecf999d36a3c067a77d6a3ff87aa5fe7657577e`다. Build Augmentation Survival Pack #431 (run ID `40795138452`)의 실제 job log에서 `build` task graph가 GameTest를 실행했고, 74개 required tests가 모두 통과했다. build, localization audit, dedicated-server smoke, client boot smoke, 검증된 Modrinth pack 생성도 #431에서 통과했다. 현재 `main` HEAD `588943b26b024e95a3a49e6da048723a73798862` 이후 gameplay checkpoint까지의 변경은 문서 동기화다. 이번 후속 turn에서는 build/CI, GameTest, 서버/클라이언트 부팅, 수동 플레이를 다시 실행하지 않았다.

- CODE REVIEWED: YES — final audit at current main; coverage and open manual validation recorded in `FINAL_AUDIT_2026-10-29.md`
- BUILD VERIFIED: YES — Build Augmentation Survival Pack #431 at gameplay HEAD `3ecf999d36a3c067a77d6a3ff87aa5fe7657577e` (handoff record)
- JAR / PACK PRODUCED: YES — verified Modrinth pack produced by #431 (handoff record; artifact not downloaded in this audit)
- GAME TESTED: YES — 74 required GameTests passed in Build #431, run `40795138452` (historical CI result; not rerun this turn)
- LOCALIZATION AUDIT: YES — passed in #431 (handoff record)
- DEDICATED SERVER SMOKE: YES — passed in #431 (handoff record)
- CLIENT BOOT SMOKE: YES — passed in #431 (handoff record)
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Older full-pack checkpoint retained as history: Build #80 (`40153395848`), artifact `augmentation-survival-pack-smoke-80` / ID `12584170120`, 146-definition catalog, 23 pinned external artifacts, 150-file runtime classpath, server with 92 mods, client with 108 mods; ZIP SHA-256 `2dbeccbe6b4c7d697a93eba10950e8d9a40f97dfec883b46184bca6869041141`.

현재 구현 범위:

- ROSTER LOCKED: YES — 149
- GAMEPLAY RUNTIME FULLY IMPLEMENTED: **149 / 149**
- GAMEPLAY RUNTIME PARTIAL: **0 / 149**
- NOT FULLY IMPLEMENTED: **0 / 149**
- NO GAMEPLAY RUNTIME YET: **0 / 149**
- FINAL COMPATIBILITY AUDIT: YES — latest source-boundary review; live external encounter testing remains open
- INTERACTION CONTRACT V2: YES — current canonical contract
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
- DOMINATION OWNER / SAVE-WIDE CAP / ALLY / AI / HOSTILITY NORMALIZATION: IMPLEMENTED BASELINE
- AREA ACCELERATION GROWTH / MACHINE RUNTIME: IMPLEMENTED BASELINE
- PERCEPTION SCAN / OUTLINE RUNTIME: IMPLEMENTED BASELINE
- COMPANION REVIVE MARKER / SNAPSHOT RUNTIME: IMPLEMENTED BASELINE
- GENERIC NON-NATURAL HEAL AMPLIFICATION: IMPLEMENTED BASELINE
- ENCHANT-REFRESH MENU TRANSACTION: IMPLEMENTED BASELINE
- FORBIDDEN-MAGIC ANVIL RUNTIME: IMPLEMENTED BASELINE
- POCKET OVERFLOW / DEATH-PRESERVATION RUNTIME: IMPLEMENTED BASELINE
- UUID REMOTE SEGMENT / SHARED GROUP / OFFLINE STORAGE RUNTIME: IMPLEMENTED BASELINE
- FINAL DEVICE/ACTIVE/COMBAT/RESOURCE/THREAT/STORAGE MODEL-TEXTURE-VFX-SOUND-HUD: NOT COMPLETE
- NEW STORAGE BATCH LIVE-PLAYER TESTED: NO
- PRIOR COMBINED PASS LIVE-PLAYER TESTED: NO
- PERCEPTION VISUALS LIVE-PLAYER TESTED: NO
- REVIVE LIVE-PLAYER TESTED: NO
- HEALING-AMPLIFICATION LIVE-PLAYER TESTED: NO
- ENCHANT-REFRESH LIVE-PLAYER TESTED: NO
- AREA-ACCELERATION LIVE-PLAYER TESTED: NO
- DOMINATION LIVE-PLAYER TESTED: NO
- PHASE-SHIFT LIVE-PLAYER TESTED: NO
- GIGANTIFICATION LIVE-PLAYER TESTED: NO
- FORBIDDEN-MAGIC LIVE-PLAYER TESTED: NO
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
