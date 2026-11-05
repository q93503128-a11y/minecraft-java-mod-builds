# Augmentation Survival — 지배한 특수 공격 몹 개선 (2026-11-05)

## 사용자 플레이 증상
- 지배한 슬라임: 적에게 접근하지만 접촉 피해가 적용되지 않음.
- 지배한 가디언: 원래 레이저의 충전/공격이 비정상.
- 기존 일반 몹 지배와 아군 안전, `Shift+지배` 로스터·해제·저장을 보존해야 함.

## 원인과 외부 근거
- 기존 `CompanionAugmentService.applyCombatTarget`는 대상 지정 때마다 `getNavigation().moveTo(target,1.15)`를 강제했다. 이 동작은 슬라임의 고유 점프 이동 제어 및 가디언의 정지형 레이저 충전 Goal과 충돌할 수 있다. 가디언에서 이것이 **유일한 실제 원인**인지는 아직 플레이로 검증되지 않았다.
- Java 26.2에서는 `Slime`, `MagmaCube`가 `net.minecraft.world.entity.monster.cubemob`로 이동했고 `AbstractCubeMob` 부모가 `push`와 `dealDamage`를 제공한다. [NeoForged 26.2 migration primer](https://github.com/neoforged/.github/blob/main/primers/26.2/index.md).
- 바닐라 슬라임의 접촉 피해는 원래 플레이어/철 골렘 위주. 지배한 슬라임의 새 적대 타깃은 원본 접촉 피해 진입점에 도달하지 못할 수 있다. 크기 1 슬라임은 원래 접촉 무피해.

## 변경 사항
1. `augment/companion/CompanionAugmentService.java`: 기존과 동일한 목표를 틱마다 반복 `setTarget`하지 않는다. Guardian/ElderGuardian 및 `AbstractCubeMob`(Slime/MagmaCube/특수 SulfurCube)에는 일괄 `getNavigation().moveTo`를 호출하지 않아, 실제 종별 Goal·MoveControl에 공격 경로를 맡긴다. Warden 기존 anger/brain 규칙은 그대로 유지. 일반 Mob 경로 유지.
2. `mixin/DominatedSlimeContactMixin.java`: `AbstractCubeMob#push(Entity)`에 서버 전용 Mixin. 실제 Slime/MagmaCube, 지배 소유 상태, 현재 타깃 일치, 적대·주인 명령 승인, 바운딩박스 접촉이 모두 참일 때만 **바닐라의 원본 `dealDamage(LivingEntity)`** 호출. 일반 슬라임 및 SulfurCube는 변경하지 않음. 작은 슬라임(size 1)은 원래처럼 피해가 없음.
3. `src/main/resources/augmentation_survival.mixins.json`: 위 Mixin 등록.
4. `DominationBehaviorGameTests`: 중간 크기 지배 슬라임의 좀비 접촉 실제 체력 감소, 중립 소 피해 없음, 작은 슬라임 원래 무피해를 한 테스트에서 검증.

## 실제 CI 검증
- 관련 코드 commits: `bffbdc70`, `9f78c29f`, `a3c7312f`; GameTest: `666678bd`.
- 최신 CI [Build Augmentation Survival Pack #41960522979](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41960522979).
- `compileJava`: PASS.
- `runGameTest`: **162 / 165 PASS**, 추가한 지배 슬라임 회귀 테스트 PASS.
- 기존 실패 3건(이전부터): `domination_behavior_game_tests_domination_converts_enemy_into_persistent_combat_ally`; `mythic_live_fix_batch_hgame_tests_powered_flight_body_collision_also_damages_neutral_mobs`; `return_to_sender_game_tests_real_shield_durability_blocks_emit_cone_at_level_three_threshold`.
- `BUILD VERIFIED`: NO (전체 필수 GameTest 3건 실패). `JAR PRODUCED`: NOT VERIFIED for this HEAD. `PLAYTESTED`: NO. `MULTIPLAYER TESTED`: NO.

## 다음 수동 검증
- 최신 코드로 만든 새 JAR로 일반/대형 슬라임과 마그마 큐브를 지배하고, 좀비 등 적대 몹을 추적/명중하는지 검증. 주인·같은 지배 세력·중립 소 오발사 없을 것. 작은 슬라임은 바닐라처럼 무해.
- 가디언과 엘더 가디언을 물이 있는 환경에 배치하고, 약 4초/3초 레이저 충전이 끝난 뒤 지정된 적대 몹에 실제 타격되는지, 대상 전환/시야 차단/가까운 거리/지배 해제 후 원복되는지 검증.
- 다른 특수몹(블레이즈·가스트·셜커·마녀 등)은 임의로 동일 처리하지 않음. 패턴별 실제 공격 실패가 있으면 재현해 분리 수정.
- 지난 JAR/모드팩 파일에는 이번 변경이 들어 있지 않음. 새 빌드가 확보되기 전까지 변경 완료 플레이테스트라고 주장하지 않는다.

## UI 작업 계약
- Codex UI 리뉴얼은 `UI_REDESIGN_DIRECTION_2026-11-05.md`의 **시안 사용자 승인 → 구현 → GUI scale/해상도/긴 텍스트/빈 상태 자동·시각 검증** 순서로 진행. 지배 특수 몹 공격 수정과는 별개로 관리.
