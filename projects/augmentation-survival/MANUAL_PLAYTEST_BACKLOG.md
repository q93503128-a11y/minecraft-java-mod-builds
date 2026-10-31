# Augmentation Survival — Manual Playtest Backlog

This file tracks only player-facing behavior that is still unresolved or has changed since the latest player-confirmed playtest. Do not reopen confirmed items unless related code changes again.

## Active retest after Bundle H

### Bundle H — mythic / Auto Refill

- [ ] `area_acceleration` / **영역 가속 — 전설 Lv.1 / Lv.2** — 반경 24블록 안에 같은 광량·수분 조건의 밀 약 10칸 이상을 준비한다. 새 규칙은 **60초 지속 / Lv.1 15배 / Lv.2 30배**다. Lv.2 정지 밭의 각 위치는 한 번의 사용 동안 약 25회 이상의 추가 랜덤 틱 기회를 받도록 서버 샘플러가 구성된다. Lv.1도 이전 3배가 아니라 15배로 상향되었다. 지속시간/범위 HUD는 왼쪽에 떠야 하며 바닐라 우측 상단 효과와 겹치면 안 된다.
- [ ] `gigantification` / **거대화 — 신화** — 사용 직후 두 칸 충전 표시가 아니라 **실제 단일 60초 쿨다운**이 표시되어야 한다. `이중 충전`을 보유해도 두 충전으로 바뀌면 안 된다. 거대화 중 일반 휩쓸기와 Better Combat 다중 타격 범위가 커진 몸집에 맞춰 실제로 넓어지는지 확인한다.
- [ ] `domination` / **지배 — 신화** — 재사용 대기시간이 없으므로 충전 표시가 없어야 한다. 자기 지배 개체를 바라보고 지배를 다시 사용하면 그 개체만 해제되어야 한다. 평소 지배몹은 주인 주변으로 돌아와야 하며, 동굴이나 먼 곳의 적을 무한 추격하면 실패다. 전투 추적은 주인 기준 약 24블록, 직접 명령 대상은 약 32블록을 넘으면 포기하고, 같은 차원에서 40블록 이상 떨어진 지배몹은 주인 근처 안전 위치로 귀환해야 한다.
- [ ] `flight` / **비행 — 신화 토글** — 비행 중 화면/FOV 떨림이 이전보다 안정됐는지 실제 체감으로 확인한다. 비행 자세는 속도에 따라 수영 자세처럼 출렁이지 않고, 정면을 향한 엎드린 비행 자세로 일관되어야 한다. 소·돼지 등 비적대 생물도 고속 몸통 충돌 피해를 받아야 하며 플레이어와 아군은 충돌 피해 대상이 아니어야 한다. 카메라 떨림은 자동 테스트만으로 완전 해결을 선언하지 않는다.
- [ ] `time_stop` / **시간 정지 — 신화** — 실제 정지 반경과 같은 구체가 회색/백색 재 입자로 보이고 내부도 옅은 회색 입자로 채워져 정지 영역이 시각적으로 구분되어야 한다. 표시 범위와 실제 적/투사체 정지 범위가 어긋나면 실패다.
- [ ] `divine_punishment` / **천벌 — 신화** — 대상 위치에 바닐라 번개가 짧은 간격으로 **3번** 연속 내리쳐야 한다. 체력 비율 피해와 Lv.2 기절은 한 번만 적용되어야 하며, 연출용 2·3번째 번개 때문에 추가 피해나 화재가 발생하면 안 된다.
- [ ] `auto_refill` / **자동 보충** — 영상에서 재현된 경우를 그대로 다시 확인한다. 인벤토리에 같은 아이템이 있어도 **의도적으로 빈 핫바 칸으로 선택을 옮기기만 했을 때는 절대 자동 보충되면 안 된다.** 반대로 같은 선택 슬롯에 있던 스택 가능한 아이템의 마지막 1개를 실제 사용해 `1 -> 0`이 되면 기존처럼 인벤토리의 동일 아이템/컴포넌트 스택으로 즉시 보충되어야 한다.
- [ ] `time_acceleration` / **시간 가속 — 전설 Lv.1** — 기존 설계대로 3초 집중 중 움직이거나 아이템을 사용하면 집중이 끊긴다. 카드 설명에도 이 조건이 표시된다. 집중 성공 후 20초 동안 이동속도 +60%, 공격속도 +60%, 실제 채굴속도 +60%, 아이템 사용 및 자신이 쏜 투사체 속도 +40%가 적용되는지 확인한다.

### 이전 Bundle에서 아직 수동 확인이 남은 항목

- [ ] `return_to_sender` / **돌려보내기 — 희귀 Lv.3 권장** — Survival에서 실제 방패로 공격을 3회 막는다. 3번째 방어 후 기존 7블록 / 전체 45° 전방 충격파가 나와야 하며, 카드 설명에는 내부 구현 규칙인 `내구도`가 노출되면 안 된다. Lv.1 / Lv.2 / Lv.3의 실제 발동 횟수는 5 / 4 / 3회, 반환 비율은 60% / 80% / 100%다.
- [ ] `invincibility` / **무적 — 전설 Lv.1** — 10초마다 자동 발동하는 3초 무적 동안 금색 반투명 보호막이 머리 위가 아니라 실제 아바타 몸통을 중심으로 감싸는지 3인칭에서 확인한다. Bundle F에서 render-space Y 방향을 바로잡아 중심을 `-0.20`으로 옮겼다.
- [ ] `phase_shift` / **위상 이동 — 전설** — 들어간 상태에서 고체 블록 내부 `IN_WALL` 질식 피해를 받지 않아야 한다. 일반 공격/환경 피해까지 무적이 되면 안 된다.
- [ ] `rewind` / **되감기 — 전설** + 위상 이동 — 위상 이동으로 지형 내부에 있던 시점의 기록을 만든 뒤 밖으로 나와 되감기를 사용한다. 고체 블록 안으로 복원되지 않고 근처 안전 공간으로 보정되어야 한다.
- [ ] `revive` / **소생 — 전설 Lv.1** — 길들인 늑대 같은 길들인 영구 동료와 `지배` 영구 동료는 현재 런타임 대상이다. 20블록 안에서 실제 사망 -> 20초 표식 -> 3초 계속 조준 -> 최대체력 50% 부활, 성공 쿨 120초를 확인한다. 아군 플레이어 소생은 별도 멀티플레이 death/respawn authority pass로 구현한다.
- [ ] `water_run` / **수상 보행** — stand, walk and sprint across real water surfaces. The surface should feel like ground from above, including flowing-water height. Crouching must let the player sink. A fall below 5 blocks should land without fall damage; a 5-block-or-higher dive must pass through. While already underwater, swimming upward must cross the surface normally instead of trapping the player below it. Lava must remain unaffected.
- [ ] `portable_riptide` / **휴대 수류** — Survival, dry land, Riptide trident in the main hand, water bucket in the offhand. Repeated dry Riptide uses must keep the bucket intact and must show the same visible body-spin animation as ordinary wet Riptide.

## Waiting for reproduction evidence

- None for Bundle H. The previously requested Auto Refill recording was reviewed and provided direct reproduction evidence.

## Investigation closed

- Monster-room chests currently use an 8% normal-core roll per chest. Four chests have only about a 28.4% chance of at least one core, so one normal core from four chests is not evidence of a loot bug.

## Confirmed / do not reopen without a new symptom

- `last_stand` + `delayed_damage` — no new failure was reported after the delayed-debt clearing/protection repair.
- Previously confirmed lower-tier items remain closed unless code affecting them changes again.

## Deferred / separate implementation work

- `revive` allied-player branch — design decision: a death marker remains valid for 20 seconds. If the target player has already respawned when an allied caster completes the 3-second revive, that live player is returned to the recorded death position with 50% maximum health. Inventory, XP, and already-created death drops are not rolled back. If the target has not respawned yet, the marker remains until expiry and can be used after respawn. Exact party/shared-owner ally eligibility should also allow allied permanent tamed/dominated companions. This must be implemented and multiplayer-tested in a dedicated death/respawn authority pass; do not duplicate a `ServerPlayer` entity.
- `aerial_combo` — later change requested: its air jump should use 1.5x jump strength. This remains deferred and is not part of Bundle H.

## Verification boundary

Bundle H final runtime checkpoint `664e5d70ac4ebaf74794e7a654aa63e562630a2b` passed **140/140 required GameTests**, clean build, dedicated-server boot smoke, client boot smoke, localization checks, verified MRPack generation and production-JAR verification in workflow **#541**.

- Production JAR SHA-256: `b8a4411de70f5b0734734a367bdbb9caa454d133af877793ba14707b5144cb1c`
- MRPack SHA-256: `806e079c5b9e60576f9ba0ff4b8d1d977945668138c6252ec021415baec411f4`

These checks do not replace the manual visual/feel retests above. Bundle H post-fix playtesting and multiplayer testing remain **NOT RUN**.

Report failures as `augment O/X - symptom` with the simplest reproducible situation possible.
