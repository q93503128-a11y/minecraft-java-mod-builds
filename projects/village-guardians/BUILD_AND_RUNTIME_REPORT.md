# Build and Runtime Report

- Project: Village Guardians — 마을지키기
- Mod ID: `villageguardians`
- Current source version: `0.18.22-alpha.1`
- Minecraft: `26.2`
- NeoForge build dependency: `26.2.0.37-beta`
- Java target: `25`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.143`
- Target JAR: `villageguardians-0.18.22-alpha.1.jar`
- Final acceptance Actions run: `32338345420`
- Final acceptance head: `f27a50b1c7fd1632fb4443b908c8e83d73846885`
- Final JAR SHA-256: `cd7712bacac503748ff0a03e3f6a76f5ae90df6d9322fea674e156225e67f990`
- Final JAR size: `958353` bytes

## 0.18.22 UI 롤백 완결 · 후속 시스템 보존 감사

- 사용자 화면 평가에서 기각된 0.18.16 중앙 집중형 Defense HUD를 실제 정본에서 제거했다. `VillageMainHudOverlay`, `VillageHudSystem`, `VillageSkillHudOverlay`, `VillageStructureHud`는 승인된 pre-defense-pass 동작을 복원했고, Command Center/Town Hall은 최신 기능을 유지한 채 기존 로컬 팔레트로 복귀했다.
- `VillageDefenseHudFrame.java`, `VillageDefenseUiTheme.java`, HUD 전용 `VillageRaidSystem.RaidHudSnapshot`, 기각 UI를 다시 요구하던 `test_v01816_mobile_defense_ui.py`를 제거했다.
- 반대로 `VillageDefenseEffectSystem`의 turret placement/deploy/repair/upgrade, breach alarm, 0.18.21 front warning/arrival과 포탑·용병·보스 절차 메시 Presentation은 그대로 유지했다. 즉 UI 외형 롤백이 월드 전투 Presentation 롤백으로 번지지 않았다.
- 0.18.18 장기 강화·전투 소모품·성벽 전투 갤러리·Lv.60 용병, 0.18.19 Lv.10 연구·포탑 내구/사거리·용병 Presentation, 0.18.20 4병과 명부/레거시 이관, 0.18.21 authoritative raid lifecycle/directional signal 회귀를 전부 재검사했다.
- 통합 중 `test_runtime_safety.py`가 기각 UI의 `guiHeight()-112`와 `abilityCard`를 영구 요구하던 오류를 발견해 승인 UI의 `guiHeight()-98` 저프로필 안전 계약으로 수정했다. 신규 0.18.22 테스트의 잘못된 소모품 클래스명도 실제 소유자 `VillageConsumableSystem`으로 교정했다.
- 검증된 런타임 롤백 source commit은 `2c4c97ff7335e16d063bc7e2f1202f5322c5bafc`다. integration run `32338131966`에서 canonical regressions, Java 25 clean NeoForge build, runtime JAR verifier가 모두 PASS했다.
- 최종 acceptance Actions run `32338345420`은 deterministic contracts, Java 25 setup, clean build, runtime JAR verifier, artifact upload, acceptance metadata 기록을 모두 PASS했다. acceptance head는 `f27a50b1c7fd1632fb4443b908c8e83d73846885`다.
- Actions artifact `9395435283`을 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`의 0.18.22/NeoForge/Minecraft 범위, manifest, `VillageMainHudOverlay`, `VillageSkillHudOverlay`, `VillageStructureHud`, `VillageConsumableSystem`, `VillageDefenseResearchSystem`, `VillageMercenarySystem`, `VillageDefenseEffectSystem` 포함과 rejected frame/theme class 부재를 독립 재검증했다. Artifact digest는 `sha256:a0044850a650cf315ff96b0bd5c298d32fb2267c9a243af3933dabebbc299c2d`다.
- 최종 JAR SHA-256 `cd7712bacac503748ff0a03e3f6a76f5ae90df6d9322fea674e156225e67f990`, 크기 `958353` bytes.

## 0.18.21 습격 정본 판정·수명주기·다전선 Presentation 감사

- `VillageRaidSystem.isRaidEnemy`에서 커스텀 이름의 `웨이브`/보스 표시명 fallback을 제거했다. 실제 습격 적은 `ACTIVE_ENEMIES`, `ACTIVE_ARCHETYPES`, `ACTIVE_WAVES`, 내부 `villageguardians_raid_enemy` tag 중 하나로만 판정된다.
- `releaseEnemy`가 `VillageAttackPlanSystem`, `VillageEnemyEliteSystem`, `VillageSiegeBossSystem`, `VillageBossAspectSystem`의 해당 UUID 상태를 즉시 제거한다. 사망/누락된 적의 전선·정예 지연 시전·보스 시전이 다음 웨이브까지 남지 않는다.
- `clearState`는 승리와 게임오버에서 전선, 정예 교리, grapple/firebrand/plague cast, 보스 doctrine/phase-two/breach/ritual/duel cast를 즉시 비운다. AttackPlan cleanup은 현재 phase를 `lastPhase`에 저장해 게임오버 NIGHT를 새 밤 진입으로 잘못 감지하지 않는다.
- `VillageDefenseEffectSystem`과 `VillageSkillMeshLibrary`에 `raid_front_warning` / `raid_front_arrival`을 추가했다. 야간 전 위험 전선과 실제 웨이브 사용 전선을 회전 링·chevron·수직 신호로 표시하며 주공/별동대 강도를 구분한다. 기존 smoke/flame은 보조 분위기 피드백만 담당한다.
- 더 이상 서버 tick에서 호출되지 않던 `VillageDefenseSystem`의 `TOWER_TICKS`와 `fireBallista/fireFlame/fireFrost/fireArcane` 고정 성루 전투 구현을 완전히 제거했다. 컴파일된 최종 class에서도 해당 심볼이 없음을 독립 확인했다.
- 통합 게이트 과정에서 `test_v0189_siege_phase2.py`, `test_v01818_growth_consumables.py` 등 과거 회귀가 최신 버전 문자열을 고정해 새 릴리스를 거짓 실패시키는 문제를 확인했다. 구 버전 테스트들의 `mod_version=0.18.x-alpha.1` 리터럴 고정을 기능 계약 검사로 이관했고, 최신 `test_v01821_raid_lifecycle_presentation.py`만 현재 정확한 버전을 확인한다.
- 검증된 런타임 소스 commit은 `04a9c9c2d9b0996f781f334475399291de49ac65`다. 통합 게이트에서 canonical regressions, Java 25 clean NeoForge build, runtime JAR verifier를 모두 통과했다.
- 최종 acceptance Actions run `32332392760`은 deterministic contracts, Java 25 setup, NeoForge clean build, runtime JAR verifier, artifact upload, acceptance metadata 기록을 모두 PASS했다. acceptance head는 `cb5268807406a982bc39e038cdbf5b59526e0f8c`다.
- Actions artifact `9393481896`를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`의 mod version/NeoForge/Minecraft 범위, manifest, `VillageRaidSystem`, `VillageAttackPlanSystem`, `VillageEnemyEliteSystem`, `VillageSiegeBossSystem`, `VillageDefenseSystem`, `VillageDefenseEffectSystem`, `VillageSkillMeshLibrary` 포함을 독립 재검증했다. Artifact digest는 `sha256:4fb0f10652991ddccb846609c8e842e9025eba9560b17a5b3a2c4fd544ee219a`다.
- 최종 JAR SHA-256 `f39d02ef247387a669991cc6cc4dfb2c67a6f6949134fd4c1db0f720cfa5ab7e`, 크기 `971022` bytes.

## 0.18.20 용병 명부·병영 경제·레거시 이관 감사

- `VillageMercenarySystem`의 SavedData `CLASSES` 명부를 용병 정원의 단일 정본으로 승격했다. 고용 시 현재 전장 AABB의 Iron Golem 수를 세던 경로를 제거해 언로드/원거리 이탈로 정원을 우회할 수 없게 했다.
- 병영 강화가 병과 고용비에 `+25/level`을 더하던 역성장 수식을 제거하고, 병과별 기본 비용을 유지한 채 병영 단계가 오를수록 완만한 할인만 적용되게 했다.
- `VillageDefenseSystem`의 별도 generic `마을 용병` 생성 소유권을 폐기하고 생산 고용을 `VillageMercenarySystem`으로 통합했다. 기존 호환 facade도 방벽 수호병 고용으로 위임한다.
- 기존 세이브에 남은 이름 `마을 용병` Iron Golem은 entity join 시 `adoptLegacy`를 통해 방벽 수호병 Lv.1로 1회 이관되며 병과/레벨/훈련 SavedData, 허용 엔티티 마킹, 패시브와 이름을 즉시 동기화한다.
- 병영 UI는 전용 `mercenary_roster` 화면으로 전환했다. 네 병과별 실제 현재 고용비와 설명을 보여주고, 로드된 개별 용병은 UUID 기반 `retire_mercenary` 작업으로 낮 정비 시간에만 퇴역시킨다. 퇴역은 Presentation actor와 허용 엔티티 마킹·SavedData를 함께 정리하며 환불은 없다.
- 수동 후속 감사에서 병영 관리 화면의 정원 표시가 연구 보너스를 빠뜨리는 불일치를 발견해 `VillageDefenseResearchSystem.mercenaryCapacityBonus()`를 포함하도록 수정했고 별도 `test_v01820_roster_ui_consistency.py` 계약을 추가했다.
- 통합 과정에서 역사 버전 문자열을 의도적으로 고정한 0.17.x 테스트까지 전부 실행하는 게이트 오류와 `Mob`→`IronGolem` 정적 타입 오류를 실제 CI가 잡았다. 역사 테스트는 변조하지 않고 현행 canonical 회귀 세트로 복구했으며 legacy migration은 pattern binding으로 타입 정합을 맞췄다.
- 검증된 런타임 소스는 `b2304912534dc70afae6a67f96874e212c36b2d3`, 후속 UI 정합 수정은 `4466b1d46146175a2df5e06eb34f68e645037f95`에 반영됐다.
- 최종 acceptance Actions run `32330475890`은 canonical deterministic contracts, Java 25 setup, NeoForge clean build, runtime JAR verifier, artifact upload, acceptance metadata 기록을 모두 PASS했다. acceptance head는 `00a9ca1f4350d6dd431a4bd7ed2d6be817c50d7d`다.
- Actions artifact `9392861932`를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageMercenarySystem`, `VillageMercenarySystem$RosterEntry`, `VillageMercenaryPresentationSystem`, `VillageDefenseSystem`, `VillageUiService`, `VillageActionDescriptions`, `VillageActionDetailScreen` 포함을 독립 재검증했다. Artifact digest는 `sha256:de51d284c73a9c82a99704e0678b29f8b0fb372dbc9417efc8e56ef65ca49067`이다.
- 최종 JAR SHA-256 `8349e946556c23d45694274397989ff1212fb2998c204eab0967a0a6ff04d55c`, 크기 `976814` bytes.

## 0.18.19 후반 연구·용병 정체성·세이브 연속성 감사

- `VillageDefenseResearchSystem`의 기존 `mercenary/tower/logistics` 저장 ID를 유지한 채 각 연구 상한을 Lv.5 → Lv.10으로 확장했다. Lv.1~5의 기존 핵심 배율은 보존하고 Lv.6~10은 완만한 mastery curve를 사용한다.
- 용병 교리는 최대 연구에서 피해 x1.85, 치유 보정, 처치당 훈련 진척 최대 x3, 정원 보너스 최대 +5를 제공한다. 수동 감사에서 초기 수식이 기존 연구 Lv.2~4 정원을 줄이는 회귀를 발견해 기존 0/1/2/3/3/3 진행을 정확히 보존하도록 수정했다.
- `VillageMercenaryPresentationSystem`을 추가해 Iron Golem 기반 실제 판정/저장은 유지하면서 4개 병과에 owner-follow 절차 메시 실루엣을 부여했다. Lv.20/40/60에서 별도 시각 tier가 열린다.
- transient mercenary reset이 presentation map만 지워 중복 actor를 만들 수 있는 수명주기 문제를 수동 감사에서 발견했다. 서버 초기화에서만 map을 초기화하고 일반 transient reset에서는 유지하도록 교정했다.
- 포탑 연구가 실전 피해뿐 아니라 사거리와 최대 내구도에도 연결된다. 기존 0.18.18 풀피 포탑은 `v01819_turret_durability_migrated` 1회 마커를 사용해 새 최대 내구도로 이행하며, 이후 포탑 공학 강화는 각 포탑의 현재 HP 비율을 보존한다.
- 전리품 군수학은 기존 장비 드랍/판매 보너스 외에 전투 소모품 최대 25% 할인과 현장 포탑 수리 효율을 제공한다.
- 두 개 오래된 회귀검사가 포탑 연구의 옛 `+10%/level` 소스 문자열 자체를 고정하고 있어 새 mastery curve를 잘못 실패시켰다. 중복 연구 발사 제거/실제 배치 포탑 소유권이라는 행동 계약은 유지하고 수식 구현은 연구 시스템이 소유하도록 이관했다.
- 통합 게이트에서 canonical deterministic regressions, Java 25 clean NeoForge build, runtime JAR verifier를 통과한 뒤 source commit `78624b2d...`가 반영되었고, post-audit 게이트도 동일 검증을 통과한 뒤 `1bf96aa6...`가 반영됐다.
- 최종 acceptance Actions run `32328485511`: deterministic contracts PASS, Java 25 setup PASS, NeoForge clean build PASS, runtime JAR verifier PASS, artifact upload PASS, acceptance metadata 기록 PASS.
- 최종 artifact를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageDefenseResearchSystem`, `VillageMercenaryPresentationSystem`, `VillageMercenarySystem`, `VillagePlacedTurretSystem`, `VillageSkillEffectEntity`, `VillageSkillMeshLibrary` 포함을 재검증했다.
- 최종 JAR SHA-256 `092566a1f57194df43a556b709af4d97615d596565f56035679f3c29f01b37e2`, 크기 `974113` bytes.

## 0.18.18 장기 강화·전투 소모품·성벽 전투 감사

- 유료 일반 식량을 일일 배급 식량으로 통합해 중복 역할을 제거하고 붕대/정화제/자극제/수호 비약/비전 촉진제/응급 포탑 수리 키트의 전투 소모품 축을 추가했다. 소모품은 서버가 기록한 custom data identity만 인정해 모루 이름 위조가 작동하지 않는다.
- 장비 강화 상한을 최대 +30까지 확장하고 무기 계열별 개별 상한 및 +10 이후 완만한 diminishing-return 구간을 적용했다.
- 기술·마법 연구소의 위력/지속 성장과 과도한 병영 쿨다운 중첩을 정리했다.
- 성벽에는 몬스터 통로가 되지 않는 높이의 사격구와 바깥으로 돌출된 수비 보행로/투하 구간을 추가해 닫힌 성벽 양면 전투를 개선했다.
- 용병 최대 레벨을 60으로 확장하고 원거리 사거리·치유 범위·직업 패시브를 장기 성장 곡선에 연결했다. 수동 감사에서 reload sanitize가 구식 Lv.5 상한으로 잘라버리는 문제를 발견해 `MAX_LEVEL` 기준으로 수정했다.
- Actions run `32224131813`: canonical regressions, Java 25 clean build, JAR verifier, artifact upload 모두 PASS. 최종 0.18.18 JAR SHA-256은 `42475cd9155e7c335a5f1f259740933e8ddc7f09bee97a7736a74fc6d8f8a9d6`, 크기 `967335` bytes.

## 0.18.16 Defense HUD·Presentation 감사

- `VillageDefenseHudFrame`을 추가해 기존 styled status string 대신 21개 고정 필드로 메인 HUD 데이터를 전달한다. server state가 단일 정본이며 클라이언트는 표시만 담당한다.
- `VillageRaidSystem.RaidHudSnapshot`이 현재 wave/max wave, active enemy count, next-wave seconds, trait와 북/서/동/후방 압박 수를 실제 `ACTIVE_ENEMIES`에서 계산한다.
- `VillageMainHudOverlay`를 responsive command ribbon + weakest-defense integrity card + four-front pressure pips 구조로 재작성했다.
- `VillageSkillHudOverlay`는 두 개 ability card와 READY/cooldown state, active-effect chip을 사용하고 바닐라 hotbar 위 안전 여백을 확대했다.
- `VillageStructureHud`의 순환식 BossBar를 제거하고 실제 시설 피해 직후만 보이는 emergency alert로 바꿨다.
- `VillageDefenseUiTheme`을 추가해 command center와 town hall을 같은 저채도 surface/semantic accent 체계로 통일했다.
- 포탑 배치/설치/수리/강화와 실제 성벽 돌파에 `VillageDefenseEffectSystem` → `VillageSkillEffectEntity` → `VillageSkillMeshLibrary` synchronized procedural mesh feedback을 연결했다.
- 첫 apply run에서 오래된 runtime test의 `guiHeight()-98` 고정 좌표 계약이 새 ability-card HUD를 실패시켰고, 실제 안전성이 더 높은 `-112` baseline + card renderer 계약으로 이관했다.
- apply/회귀 run `32094444642`에서 core runtime/UI/interaction 계약과 0.18.8~0.18.16 회귀가 모두 PASS했다.
- 최종 Actions run `32094533863`: Java 25 setup PASS, deterministic contracts PASS, NeoForge clean build PASS, runtime JAR verify PASS, artifact upload PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageDefenseHudFrame`, `VillageDefenseUiTheme`, `VillageMainHudOverlay`, `VillageSkillHudOverlay`, `VillageStructureHud`, `VillageDefenseEffectSystem`, `VillageRaidSystem$RaidHudSnapshot`, `VillageSkillMeshLibrary`, `VillageCommandCenterScreen`, `VillageTownHallGridScreen` 포함을 재검증했다.
- 최종 JAR SHA-256 `62c1e69fba813828a7b59761bb3320daaca8097e7ce5bcff47b092cc8c918b2b`, 크기 `955045` bytes.

## 0.18.15 보스 Presentation·Cast 정합 감사

- `VillageBossEffectSystem`을 추가해 persistent boss presence, phase-two layer, breach/ritual/duel/bloodbound/storm cast telegraph를 공용 synchronized procedural-mesh 경로로 통합했다.
- `VillageSiegeBossSystem`은 `BREACH_CASTS`, `RITUAL_CASTS`, `DUEL_CASTS`를 통해 경고 시점의 Segment/좌표/대상 UUID를 실제 발동까지 유지한다.
- 보스 교리는 날짜 단독이 아니라 day + actual wave + archetype으로 혼성화했고, 3교리 modulo에서 `wave*3`가 무효임을 발견해 `wave*2`로 수정했다. 별도 regression에서 연속 3웨이브가 세 슬롯을 모두 방문함을 계산 검증한다.
- `VillageBossAspectSystem`의 Bloodbound도 fixed center cast state를 사용하고 Stormcaller fixed dodge point는 mesh ground warning으로 교체했다.
- persistent boss actor는 boss 사망 시 owner-follow 수명 규칙으로 자동 정리되며 phase two overlay는 기존 Aspect id를 extra로 전달해 색 정체성을 보존한다.
- 결투 target presentation API는 `LivingEntity`를 사용해 ServerPlayer와 타입 정합을 맞췄다.
- Actions run `32089737176`: 18개 deterministic/pre-build contracts PASS, Java 25 + NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, mod metadata/manifest, `VillageBossEffectSystem`, `VillageSiegeBossSystem`, `VillageBossAspectSystem`, `VillageSkillEffectEntity`, `VillageSkillMeshLibrary`, 그리고 `BreachCast/RitualCast/DuelCast` inner class 포함을 다시 검증했다.
- 최종 JAR SHA-256 `6a37f758510ba5ee20ec20d607dd9696a60e15f160961587a43b4042632497e4`, 크기 `940741` bytes.

## 0.18.14 지속형 Presentation·복구 감사

- `VillageTurretPresentationSystem`을 추가해 실제 포탑 SavedData와 runtime-only mesh actor를 분리했다. actor 누락은 1초 주기로 재생성되며 철거/잔해/재초기화에서는 정리된다.
- 활성 포탑 상부는 invisible `BARRIER` collision shell, 실제 보이는 기계는 10종 전용 procedural mesh가 담당한다. 포탑 목표 방향, 레벨, 교란 상태가 시각적으로 반영된다.
- `VillageEnemyEffectSystem`과 elite owner-follow actor를 추가해 다섯 정예 교리의 지속 식별 silhouette를 만들었다.
- Grappler는 18틱 Bézier traversal, Firebrand는 18틱 fixed-point projectile, Plague Weaver는 20틱 fixed danger-zone cast로 바뀌어 텔레그래프와 판정 좌표가 일치한다.
- 실패한 밤/새 게임 복구에서 persistence state를 `VillagePlacedTurretSystem.reloadAfterPersistenceChange`로 runtime state·collision shell·mesh actor에 즉시 재투영하고 `VillageSiegeSegmentSystem.restoreAllVisuals`로 성벽 projection도 즉시 동기화한다.
- 신규 presentation parser에서 Java 정규식 escape 오타를 1차 clean build가 발견했다. 수정 후 전체 17개 pre-build contract를 다시 통과하고 2차 Java 25 clean build가 성공했다.
- Actions run `32088820912`: deterministic contracts PASS, Java 25/NeoForge clean build PASS, runtime JAR verification PASS, artifact upload PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, mod metadata/manifest, `VillageTurretPresentationSystem`, `VillageEnemyEffectSystem`, `VillageEnemyEliteSystem` 상태기계 inner class, `VillagePlacedTurretSystem`, `VillageSkillEffectEntity`, `VillageSkillMeshLibrary$TurretPresentation`, `VillageCouncilState` 포함을 재검증했다.
- 최종 JAR SHA-256 `9aef5f25c469d5023306dfd22345af4e9f0e4aaf4b72d3666ac33bb6861b9357`, 크기 `931551` bytes.

## 0.18.13 공성 통합·수동 감사

- `VillageRaidSystem`에 UUID별 `ACTIVE_WAVES`를 추가하고 entity join 전에 archetype/wave 메타데이터를 등록한다. 보스·정예 전선은 더 이상 커스텀 표시 이름에 의존하지 않는다.
- 일반 플레이어 우선 추격과 적 병과 범위 능력에서 downed 플레이어를 제외했다.
- 시설 및 측·후방 Segment 공격 주기를 공격자 UUID로 stagger하여 동일 30틱 순간에 모든 적의 구조물 피해가 몰리지 않게 했다.
- `VillageTowerResearchBonusSystem`을 제거하고 실제 배치 포탑 본체의 연구 배율만 유지했다. 구형 고정 성루 전문화는 실전 포탑 화력/교란의 소유자가 아니다.
- 탑 사냥꾼은 48블록 내 가장 가까운 실제 배치 포탑을 전용 목표로 추적하고, 같은 근접 전선의 포탑을 7초 교란한다. 포탑 피해 해결 레이어는 경로 AI를 덮어쓰지 않는다.
- 10종 포탑 및 4종 용병 연출을 `VillageDefenseEffectSystem` → `VillageSkillEffectEntity` → `VillageSkillMeshLibrary` 동기화 procedural mesh 경로로 통합했다.
- 광역 투석포는 12틱 snapshot-position 곡사 포격이며 직접 사격용 LOS 필터의 유일한 의도적 예외다. 착탄 전 궤적과 착탄 지점이 보이고, 실제 피해는 도착 시점 주변 적에게 적용된다.
- mesh ARGB 페이드가 색상 채널을 훼손하던 문제를 `withAlpha`로 교정했다.
- `tools/test_enemy_content.py`, `tools/test_runtime_safety.py` 등 오래된 core 계약을 현행 배치 포탑 소유권으로 이관했다. 완전한 pre-build 16개 테스트 세트와 0.18.8~0.18.13 회귀가 PASS했다.
- Actions run `32087562708`에서 Java 25 설정 PASS, deterministic contracts PASS, NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS, acceptance metadata 기록 PASS.
- Actions artifact를 별도 다운로드해 ZIP/JAR CRC, `neoforge.mods.toml`, manifest, `VillageDefenseEffectSystem`, `VillagePlacedTurretSystem`, `VillageRaidSystem`, `VillageEnemyArchetypeSystem`, `VillageMercenarySystem`, `VillageSkillMeshLibrary` 클래스 포함을 다시 검증했다.
- 최종 JAR SHA-256 `c6d96eff852929f90fa11e888a6ebbc714252f0f47914f34e4e8852a539d2f2a`, 크기 `915862` bytes.

## 0.18.12 수동 품질 감사

- 자동 테스트에 의존하지 않고 포탑, 용병, 정예, 공성 보스, 보스 변이의 실제 호출 흐름과 상태 경계를 수동으로 추적했다.
- 포탑 LOS가 자기 3블록 실루엣에 막힐 수 있는 시작점 문제를 수정하고 목표 방향으로 캡 바깥 muzzle을 계산한다. 대공 우선순위와 연쇄 전격의 경로 연출도 실제 판정 순서와 맞췄다.
- 용병 배치는 표시 이름 파싱을 제거하고 `VillageMercenarySystem.classOf`의 SavedData 기반 병과 상태를 정본으로 사용한다. 원거리/치유 병과의 바닐라 근접 경로도 랠리 복귀와 충돌하지 않도록 억제한다.
- 파성 거신의 북문 no-op을 제거하고 2페이즈 파쇄 주기를 45→30틱으로 실제 단축했다. 사령 결속자와 검은 결투원수에도 발동 전 시각 경고를 추가했다.
- 정예 화염/역병/갈고리 행동은 즉발 판정 전에 텔레그래프가 나오며, 암살자와 돌파 기병은 가장 가까운 유효 플레이어를 선택한다.
- 보스 변이는 다운된 플레이어를 제외한다. 뇌광은 경고한 월드 좌표를 `STORM_WARNINGS`에 저장하고 같은 지점에서만 피해를 판정하여 실제 회피 가능한 공격이 됐다.
- `tools/test_v01812_quality_audit.py`와 기존 0.18.8~0.18.11 계약을 모두 실행했다.
- 최종 Actions run `32083991529`: Java 25 설정 PASS, deterministic contracts PASS, NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS.
- 실제 Actions artifact를 다시 내려받아 JAR CRC/압축 무결성, `neoforge.mods.toml`, manifest, 수정 핵심 클래스 포함을 재검증했다.
- 최종 JAR SHA-256 `2597579386b3a77c9a2423d70f560c03b710ee3ce91d6aeecf7662c7bd50cacd`, 크기 `908845` bytes.

## 0.18.11 용병·포탑 안정화

- 공용 `VillageDefenseLineOfSight` 블록 충돌 raycast를 추가해 포탑과 궁수 용병의 벽 관통 획득/피해를 차단했다.
- 포탑 사냥꾼은 36블록 탐색 반경과 약 7.5블록 실제 타격 반경을 분리했다. 공병 6블록, 보스 8블록 압박도 `distanceToSqr` 기준을 명시했다.
- 철갑 관통포는 중장갑 공성 병과에 최대 `1.55x`, 저항 목표에 별도 보정을 적용해 이름뿐이던 역할을 실제 전투 특성으로 만들었다.
- 포탑은 설치 검증부터 활성 외형, 잔해, 철거까지 일관된 3블록 높이 footprint를 사용한다.
- 용병 경험치는 적 사망 위치 48블록 내 전체 공유 방식에서 실제 killer mercenary 1기 귀속 방식으로 교체했다.
- 수호/공격/궁수/의무 4병과 모두 별도 전장 행동을 갖고, 궁수·의무의 근접 AI 이탈을 차단했다. 성벽 랠리 경로 실패 시 내부 거점 fallback을 적용한다.
- 신규 `tools/test_v01811_defense_polish.py`와 기존 0.18.8/0.18.9/0.18.10 계약을 포함한 acceptance를 수행했다.
- 1차 Java 컴파일에서 Minecraft 26.2의 구리 블록 컬렉션 타입 차이를 발견했고 단일 concrete block으로 교정한 뒤 2차 clean build가 성공했다.
- 최종 Actions run `31583212244`: Java 25 설정 PASS, 전체 deterministic contracts PASS, NeoForge clean build PASS, JAR verifier PASS, artifact upload PASS.
- 최종 JAR SHA-256 `fc0b411c5c4400a44434f7d7205f1de94ceacce484c8917f0f64425a42def4b7`, 크기 `906456` bytes.

## 기준점과 회귀 기준

이번 작업은 사용자 실제 테스트 기준 JAR `villageguardians-0.18.8-alpha.1`과 당시 GitHub `main`을 다시 확인한 뒤 진행했다.

- 0.18.8 기준 JAR SHA-256: `688a63f2e46f2d52738955eadf566e178bf160fde2435612915605fa2f7cdd78`
- 0.18.8 마지막 확인 성공 Actions run: `31551899143`
- 0.18.8의 초반 솔로 난이도 완화 유지
- 플레이어 추가 1명마다 적 수 약 `+30%` 유지
- 전원 부활 대기 중 시설 피해 30% 보호는 제거된 상태 유지
- 기존 상점 카테고리, 잡템 판매 보호, 중요 행동 확인창, 다음 웨이브 도감, UI Safe Area, 구조물 파괴 드롭 억제 계약 유지

## 0.18.9 방어전 시스템 2차 확장

### 성벽 Segment / 국소 돌파

성벽 전체를 하나의 HP로 처리하지 않고 다음 7개 방어 구역으로 분리했다.

1. 북서 성벽
2. 북문
3. 북동 성벽
4. 서쪽 방벽
5. 동쪽 방벽
6. 후방 서측 방벽
7. 후방 동측 방벽

북문은 기존 `Building.WALLS` 저장 구조를 어댑터로 사용해 이전 세이브와 호환한다. 나머지 구역은 별도 현재 HP, 최대 HP, 방어 등급, 강화 단계, 손상 상태를 저장한다.

HP 비율에 따라 정상 → 균열 → 대파 → 돌파 상태로 월드 외형이 변하며 0이 되면 마지막 공격 축을 중심으로 폭 5블록의 국소 돌파구만 생성한다. `destroyBlock`이나 블록 드롭을 사용하지 않고 직접 블록 상태를 갱신한다. 수리 시 동일 위치를 재구축한다.

### 공격 방향과 전장 상황

날짜에 따라 전선 구조가 단계적으로 확장된다.

- 1~4일: 북문 정면 전용
- 5~7일: 북서/북동 소규모 별동대
- 8~11일: 서/동 측면 공세
- 12~15일: 정면 + 양측 복합 전선
- 16일 이후: 정찰에 공개된 후방 서측/후방 동측 별동대까지 포함 가능

측·후방 스폰은 자연 지형에 묻히지 않도록 목표 Y 기준 ±24블록 범위에서 `단단한 바닥 + 발/머리 2블록 공기` 위치를 찾는다. 돌파 전에는 대응 Segment를 공격하고, 돌파 후에는 해당 구멍 안쪽 접근점을 새 경로 목표로 사용한다.

전장 상황은 맑은 전장, 공성 북소리, 검은 안개, 불탄 진입로를 deterministic하게 선택하고 실제 강화/기동/화염 저항/구조물 압박 효과와 연결했다.

다음 밤 정찰에는 주공, 별동대, 예상 총병력, 병과, 공성 병과, 정예, 보스 전투 구조, 전장 상황을 표시하며 야간 시작 직전 위험 방향에 연기/화염 신호를 제공한다.

### 신규 정예 교리 5종

- 갈고리병: 측면 성벽 접근 후 내부 침투
- 화염 투척병: 근거리 범위 화상·마법 피해
- 침투 암살자: 플레이어 직접 추격
- 역병술사: 독·약화 범위 방해
- 돌파 기병: 고속 돌격·순간 강화

기존 적 병과를 색상/스탯만 바꾼 것이 아니라 각 교리가 실제 이동·목표·공격 방식을 변경한다.

### 신규 보스 전투 구조 3종

- 파성 거신: Segment를 직접 파쇄하고 HP 50% 이하에서 파쇄 주기 강화
- 사령 결속자: 주변 침공 병력에 반복 회복·보호막·저항 의식
- 검은 결투원수: 시설보다 살아 있는 플레이어를 우선 추격하는 결투형 지휘관

기존 보스 종류/변이 시스템 위에 별도 전투 구조로 적용된다.

### 직접 배치 포탑 10계열

기존 고정 성루는 요새 건축/관측 구조물로 남기고 실전 화력은 플레이어가 직접 배치하는 포탑이 담당한다.

- 중쇠뇌
- 연사 포탑
- 철갑 관통포
- 화염 투사기
- 서리 억제기
- 연쇄 전격탑
- 광역 투석포
- 마법 억제탑
- 대공 발사대
- 지원 봉화

설치 과정은 계열 선택 → 바닥 우클릭 유효성 미리보기 → 같은 위치 재클릭 확정이다.

서버 설치 검증은 방어구역, 주 통행로, 북문 전면 도배, 건물 출입구/운영 공간, 포탑간 최소 8블록, 단단한 바닥, 3블록 높이 공간, 전체 설치 한도를 검사한다.

각 포탑은 영구 ID, 위치, 레벨, 현재/최대 HP, 공격력, 공격 주기, 사거리, 계열 역할, 가동 상태를 가진다. 포탑 사냥꾼/폭파병/보스가 포탑을 공격하며 HP 0은 아이템 드롭 없는 잔해 상태가 된다. 회관에서 개별 수리/강화/철거와 손상 포탑 일괄 수리를 제공한다.

기존 `VillageTowerResearchBonusSystem` 생명주기 훅은 안전 계약 때문에 유지하되 고정 좌표 성루 보너스 사격 대신 직접 배치 포탑 연구 보너스로 재구현했다. `VillageDefenseSystem.tick`의 고정 성루 실전 사격은 production 전투 루프에서 제거했다.

### 용병 배치 3거점

전투 전 병과별로 성문 전방, 성 내부, 성벽 중 허용된 거점을 선택한다. 전투 중에는 RTS식 세밀 조작 없이 병과별 AI가 자동 전투하고, 역할별 leash 범위를 지나치게 벗어나면 지정 전선으로 복귀한다.

### 장비/무기/세트

기존 장비 등급·강화의 범용 성능을 주력으로 유지한 채 다음 무기 계열 차이를 실제 피해 경로에 연결했다.

- 장검
- 대형 도끼
- 장창/투척창
- 전투 망치
- 장궁
- 석궁

2/3세트는 다음 두 계열을 추가했다.

- 성벽 수호자: 범용 피해 증가 + 받는 피해 감소
- 밤사냥꾼: 원거리 운용 강화 + 범용 생존 보정

세트는 장비 상태에서 매번 계산하므로 해제/재장착 시 중복 누적되지 않는다. 동일 계산 결과를 장비 툴팁과 인벤토리 세트 상태 UI가 표시한다.

### UI 회귀 방지

- 포탑 신규 배치 10종 화면과 설치 포탑 관리 화면을 분리했다.
- 인벤토리 보조 패널은 실제 좌/우 안전공간을 기준으로 폭을 정한다.
- 좁은 GUI에서는 96px compact 패널로 줄이고 버튼을 제거한다.
- 읽을 수 있는 측면 공간이 없으면 바닐라 인벤토리와 겹치지 않도록 패널을 숨기며 장비 툴팁 정보는 유지한다.
- 테스트 모델에서 GUI 폭 `256`, `320`, `360`, `426`, `854`, `1280`, `1920`을 검사했다.
- `open_tower_control`, `tower_status`, `tower_open:*`, `tower_branch:*`, `tower_upgrade:*` 등 구식 고정 성루 action은 production에서 새 공성 방어 지휘 화면으로 강제 리다이렉트한다.

## 재도전 / 새 게임 상태 복원

0.18.9의 Segment/포탑 SavedData가 패배한 밤 이후 누적되는 문제를 방지하기 위해 낮→밤 전환 시 별도 공성 스냅샷을 저장한다.

- `segment_hp_*`
- `segment_breach_*`
- `turret_*`

같은 날 재도전은 야간 시작 스냅샷을 복원한다. 처음부터 재시작은 0.18.9 공성 SavedData를 초기화한다. 이후 `VillageWorldSystem.forceRebuild`가 기본 요새를 먼저 재건한 뒤 복원된 Segment 손상 외형과 포탑 상태를 다시 월드에 투영한다.

## 0.18.10 추적 도탄 수정

0.18.9의 공성 확장 상태를 유지한 채 궁수 `추적 도탄`의 실제 동작과 보상 귀속을 재설계했다.

- 최초 타깃 선정은 정규화된 전방 조준 원뿔을 사용한다.
- 발사 직후 실제 화살 벡터와 목표 벡터를 보간해 순간 90도 급회전을 줄였다.
- 이동 예측 유도는 현재 속도와 대상 속도를 사용하며 특수 숙련에 따라 회전 보간 강도가 증가한다.
- 기존 표적 사망/소멸 시 비행 방향 전방 52블록 내 새 적을 재포착한다.
- 재포착 후보는 화살 위치에서 대상 몸통까지 실제 블록 충돌이 없는 경우만 허용한다.
- 첫 적중 후 주변 일괄 피해를 제거하고 이전 적 → 가장 가까운 LOS 적 순서의 nearest-neighbour 연쇄로 변경했다.
- 도탄 대상 UUID를 방문 집합으로 관리해 같은 연쇄에서 중복 타격하지 않는다.
- 도탄은 2틱 간격으로 순차 발생하며 기본 4회, 특수 숙련으로 최대 8회다.
- 연쇄 피해는 이전의 선형 감소 대신 단계당 0.86배 감쇠로 변경해 음수/과도한 후반 감쇠를 방지한다.
- 2차 도탄은 플레이어 공격 소스로 귀속해 처치 XP·주화·처치 기반 성장과 연결한다.
- `PRE_SCALED_RICOCHET_DAMAGE` 가드로 이미 계산된 1차 화살 장비/직업/유물 배율의 이중 적용과 일반 전투기술 도탄 재귀를 차단한다.
- 사용자 설명도 재포착·순차 도탄 동작과 일치하도록 갱신했다.

전용 `test_v01810_ranger_ricochet.py`는 조준 원뿔, 부드러운 유도, 재포착, 블록 LOS, 중복 방지, 4~8회 도탄 상한, 피해 감쇠, 플레이어 귀속, 이중 배율/재귀 차단을 검증한다.

## 검증 결과

| 단계 | 상태 | 비고 |
|---|---|---|
| 0.18.8 기준 JAR 검사 | PASS | 버전/Manifest/NeoForge/MC 범위/SHA-256 확인 |
| 기존 RPG/영역/성장/요새/런타임 안전 계약 | PASS | 최종 run에서 재실행 |
| 0.18.10 추적 도탄 계약 | PASS | 조준·유도·재포착·LOS·순차 도탄·보상 귀속·이중배율 차단 |
| 0.18.8 위험/상점/UI 회귀 계약 | PASS | 초반 난이도, +30% 멀티, 사망 리스크, 생산 라우팅 유지 |
| 7개 Segment 계약 | PASS | 독립 HP, 5블록 국소 돌파, no-drop 복구 |
| 경로/다전선 변수 검사 | PASS | 날짜 단계, 측·후방 전선, safe terrain-height spawn, breach route |
| 1/2/3/4인 스케일 | PASS | 기준 10명 모델에서 10/13/16/19 |
| 직접 배치 포탑 | PASS | 10계열, 위치검증, HP/파괴/수리/강화/철거 |
| 재도전/새게임 스냅샷 | PASS | 저장/복원/초기화 + forceRebuild 재투영 계약 |
| 용병 배치 | PASS | 3거점 + 병과 제한 + 자동 전투 leash |
| 정예/보스 전투 구조 | PASS | 정예 5교리 + 보스 3구조 |
| 장비 실성능/설명 일치 | PASS | 동일 runtime multiplier와 tooltip/set UI 연결 |
| 세트 해제/재장착 | PASS | 매번 장착 상태 계산, 중복 누적 없음 |
| 인벤토리 Safe Area | PASS | 256~1920 모델, compact/hide 정책 검증 |
| 구조물/성벽 파괴 드롭 회귀 | PASS | no-drop 블록 투영 + 기존 debris guard 유지 |
| Java 25 NeoForge clean build | PASS | Actions run `31564184543` |
| JAR verifier | PASS | `tools/verify_jar.py` |
| Actions artifact upload | PASS | artifact `villageguardians-0.18.10-alpha.1` |
| 다운로드 후 ZIP/JAR 재검사 | PASS | `unzip -t`, TOML/Manifest 버전, artifact SHA 대조 |
| Windows + Modrinth 실제 플레이 | NOT RUN HERE | 이 실행 환경에는 Windows Modrinth 클라이언트가 없어 사용자 인스턴스에서 인게임 연출/AI 최종 체감 확인 필요 |

## 빌드 중 발견 및 수정한 문제

1. 기존 런타임 안전 계약이 `VillageTowerResearchBonusSystem.tick` 생명주기 훅을 요구함 → 훅은 유지하고 직접 배치 포탑 연구 보너스로 재구현.
2. Minecraft 26.2의 `Blocks.LIGHTNING_ROD`가 단일 `Block`이 아닌 구리 weathering collection으로 변경되어 컴파일 오류 발생 → 연쇄 전격탑 외형을 26.2 안전 단일 블록 `Blocks.END_ROD`로 변경.
3. 좁은 GUI에서 신규 세트 패널이 바닐라 인벤토리와 겹칠 가능성 발견 → 실측 side-space 기반 compact/hide Safe Area 로직으로 교체.
4. 측/후방 스폰 원점이 요새 평탄화 반경 밖이라 자연 지형에 묻힐 가능성 발견 → ±24Y safe spawn 탐색 추가.
5. 실패 재도전에서 신규 Segment/포탑 파손 상태가 누적될 가능성 발견 → 야간 시작 공성 스냅샷 및 forceRebuild 재투영 추가.
6. 오래된 고정 성루 UI action이 남아 있을 수 있음 → 새 공성 지휘 UI로 production redirect 추가.
7. 추적 도탄이 실제 순차 도탄이 아니라 첫 적중 시 주변 적에게 즉시 피해를 뿌리던 구조 확인 → nearest-neighbour 시간차 도탄으로 교체.
8. 추적 대상 사망 시 유도가 즉시 종료되고 매 틱 방향을 강제 스냅하던 구조 확인 → 재포착 + 예측 보간 유도로 교체.
9. 2차 도탄을 플레이어 귀속으로 단순 전환하면 RPG 공격 배율이 두 번 적용될 위험 확인 → pre-scaled damage guard 추가.
10. 비행 중 재포착이 벽 너머 적을 선택할 가능성 확인 → 화살 기준 block LOS 검사 추가.

## 최종 산출물

GitHub Actions run `31564184543`은 Java 25 / NeoForge 26.2 환경에서 deterministic contract tests → clean build → JAR verifier → artifact upload까지 모두 성공했다.

다운로드한 최종 JAR 내부는 다음을 다시 확인했다.

```text
META-INF/neoforge.mods.toml: version="0.18.10-alpha.1"
META-INF/MANIFEST.MF: Specification-Version: 0.18.10-alpha.1
META-INF/MANIFEST.MF: Implementation-Version: 0.18.10-alpha.1
JAR compressed-data test: no errors
size: 902784 bytes
SHA-256: 6e24aa279b5f2fb29c91224ed404a8b084acfa843796a68c5099fd46f0e45209
```

## 남은 실제 플레이 테스트 포인트

코드/CI/JAR 수준 검증은 완료됐다. Windows + Modrinth App에서 다음을 우선 확인한다.

- 1~4일 솔로 정면전 체감 난이도
- 5~7일 측면 별동대가 정찰 정보/월드 신호와 같은 방향에서 등장하는지
- 16일 이후 후방 별동대가 사전 예고 없이 생성되지 않는지
- Segment HP 감소 → 균열 → 5블록 국소 돌파 → 적 진입 → 수리 복원 흐름
- 포탑 미리보기 유효/무효 위치, 파괴 잔해, 회관 개별/일괄 수리
- 패배 후 같은 날 재도전에서 야간 시작 전 Segment/포탑 상태가 정확히 복원되는지
- 처음부터 재시작에서 공성 SavedData가 초기화되는지
- GUI 배율 변경 시 인벤토리 세트 패널과 회관/포탑 화면의 실제 글꼴 렌더링
- 정예 5종과 보스 3구조의 체감 가독성/난이도
- 추적 도탄이 실제 전방 타깃을 자연스럽게 추적하고, 표적 사망 후 새 적을 재포착하며, 벽을 통과하지 않고 순차 도탄하는지

이 단계에서 발견되는 문제는 다음 alpha에서 실제 플레이 피드백 기준으로 보정한다.
