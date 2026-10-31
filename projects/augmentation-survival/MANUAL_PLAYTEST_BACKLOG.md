# Augmentation Survival — Manual Playtest Backlog

This file tracks only player-facing behavior that is still unresolved or has changed since the latest player-confirmed playtest. Do not reopen confirmed items unless related code changes again.

## Active retest after Bundle F

- [ ] `return_to_sender` / **돌려보내기 — 희귀 Lv.3 권장** — Survival에서 실제 방패로 공격을 3회 막는다. 3번째 방어 후 기존 7블록 / 전체 45° 전방 충격파가 나와야 하며, 카드 설명에는 내부 구현 규칙인 `내구도`가 노출되면 안 된다. Lv.1 / Lv.2 / Lv.3의 실제 발동 횟수는 5 / 4 / 3회, 반환 비율은 60% / 80% / 100%다.
- [ ] `area_acceleration` / **영역 가속 — 전설 Lv.2** — 한 포기 대신 같은 광량·수분 조건의 밀/당근 등 여러 칸을 준비해 일부는 반경 24블록 x7 영역 안, 일부는 충분히 먼 대조군에 둔다. 30초 동안 영역 안 작물군이 대조군보다 뚜렷하게 더 많이 성장하는지 본다. 한 포기 0성장만으로 실패 판정하지 않는다. 지속시간/범위 HUD는 왼쪽에 떠야 하며 바닐라 우측 상단 효과와 겹치면 안 된다.
- [ ] `time_acceleration` / **시간 가속 — 전설 Lv.1** — 3초 집중 후 20초 지속. 지속 중 이동속도 +60%, 공격속도 +60%, 최종 채굴속도 +60%가 실제 플레이에서 적용되는지 확인한다. 아이템 사용속도와 자신이 쏜 투사체 속도는 정본대로 +40%다. 지속 HUD는 왼쪽에 떠야 한다.
- [ ] `invincibility` / **무적 — 전설 Lv.1** — 10초마다 자동 발동하는 3초 무적 동안 금색 반투명 보호막이 머리 위가 아니라 실제 아바타 몸통을 중심으로 감싸는지 3인칭에서 확인한다. Bundle F에서 render-space Y 방향을 바로잡아 중심을 `-0.20`으로 옮겼다.
- [ ] `phase_shift` / **위상 이동 — 전설** — 들어간 상태에서 고체 블록 내부 `IN_WALL` 질식 피해를 받지 않아야 한다. 일반 공격/환경 피해까지 무적이 되면 안 된다.
- [ ] `rewind` / **되감기 — 전설** + 위상 이동 — 위상 이동으로 지형 내부에 있던 시점의 기록을 만든 뒤 밖으로 나와 되감기를 사용한다. 고체 블록 안으로 복원되지 않고 근처 안전 공간으로 보정되어야 한다.
- [ ] `revive` / **소생 — 전설 Lv.1** — 현재 구현 확인 범위는 길들인/지배 영구 동료다. 20블록 안에서 실제 사망 -> 20초 표식 -> 3초 계속 조준 -> 최대체력 50% 부활, 성공 쿨 120초를 확인한다. 정본에는 아군 플레이어도 포함되어 있으나 현재 런타임은 플레이어 사망을 제외하므로 이는 별도 구현 결함으로 추적한다.
- [ ] `water_run` / **수상 보행** — stand, walk and sprint across real water surfaces. The surface should feel like ground from above, including flowing-water height. Crouching must let the player sink. A fall below 5 blocks should land without fall damage; a 5-block-or-higher dive must pass through. While already underwater, swimming upward must cross the surface normally instead of trapping the player below it. Lava must remain unaffected.
- [ ] `portable_riptide` / **휴대 수류** — Survival, dry land, Riptide trident in the main hand, water bucket in the offhand. Repeated dry Riptide uses must keep the bucket intact and must show the same visible body-spin animation as ordinary wet Riptide.

## Waiting for reproduction evidence

- Auto Refill / unexpected hotbar insertion — do not change again until the promised video is reviewed. The current Auto Refill path only restores the selected empty slot with the exact prior item/component stack, but the reported live symptom needs video-level reproduction before attributing it to vanilla insertion or the augment.

## Investigation closed without a balance change

- Area Acceleration's x3/x7 contract accelerates random-tick opportunities, not guaranteed visible crop stages. A single wheat plant can therefore remain unchanged during one 30-second activation even while the field is active. Use a patch-vs-control test before changing the multiplier or duration.
- Monster-room chests currently use an 8% normal-core roll per chest. Four chests have only about a 28.4% chance of at least one core, so one normal core from four chests is not evidence of a loot bug.

## Confirmed / do not reopen without a new symptom

- `last_stand` + `delayed_damage` — no new failure was reported after the delayed-debt clearing/protection repair.
- Previously confirmed lower-tier items remain closed unless code affecting them changes again.

## Deferred / separate implementation work

- `revive` allied-player branch — canon includes allied players, but the current server runtime explicitly excludes `ServerPlayer` deaths. Implement only in a dedicated multiplayer death/respawn authority pass.
- `aerial_combo` — later change requested: its air jump should use 1.5x jump strength. This remains deferred and is not part of Bundle F.

## Verification boundary

Bundle F runtime checkpoint `b4709126c02c86a5b40aad285cc54f34c46c656d` passed **132/132 required GameTests**, clean build, dedicated-server boot smoke, client boot smoke, localization checks, verified MRPack generation and production-JAR verification in workflow #523. The Time Acceleration regression now activates the ability, waits through its real focus, then checks the sustained state and live movement/attack/final mining/item-use/projectile behavior instead of merely reading tuning constants.

These checks do not replace the manual visual/feel retests above, and multiplayer behavior after Bundle F has not been manually tested.

Report failures as `augment O/X - symptom` with the simplest reproducible situation possible.
