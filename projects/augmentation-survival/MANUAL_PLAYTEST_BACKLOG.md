# Augmentation Survival — Manual Playtest Backlog

This file tracks only player-facing behavior that is still unresolved or has changed since the latest player-confirmed playtest. Do not reopen confirmed items unless related code changes again.

## Active retest after Bundle I

### Bundle I — mythic / combat presentation

- [ ] `second_choice` / **두 번째 선택 — 신화** — 일반적으로 코어 하나에서 서로 다른 증강 2개를 고른다. 미보유 후보가 단 하나만 남은 상황에서는 그 1개만 제시되고 **한 번 선택하면 정상 종료**되어야 한다. 존재하지 않는 두 번째 선택을 요구하면 실패다.
- [ ] `gigantification` / **거대화 — 신화** — 카드 세부 문구가 `크기·최대/현재 체력·휩쓸기 범위 1.75배 / 2.5배`로 묶여 읽혀야 한다. 사용 직후 두 칸 충전 표시가 아니라 실제 단일 60초 쿨다운이 표시되고, 실제 휩쓸기/Better Combat 범위도 커진 몸집에 맞춰 넓어져야 한다.
- [ ] `time_stop` / **시간 정지 — 신화** — 32블록 구체 안에서 **시전자만 정상적으로 움직이고**, 적·중립·아군 생물·보스·투사체·드롭 아이템·탈것·점화 TNT·낙하 블록 같은 동적 엔티티는 멈춰야 한다. 구체 밖 개체는 정상 동작해야 한다. 전역 서버 시간과 구체 밖 블록 틱은 정지 대상이 아니다. 화면에는 이전보다 훨씬 강한 저채도/냉색 장면 처리와 회색 구체 경계가 보여야 한다.
- [ ] `flight` / **비행 — 신화 토글** — Bundle H의 새 FOV 스무딩/고정 자세를 제거하고 **Bundle H 이전 비행 연출**로 복귀했다. 기존 속도 기반 FOV, 속도 임계값 글라이딩 자세, 제한된 몸통 뱅킹이 돌아왔는지 확인한다. 단, 소·돼지 등 비아군 중립 생물에 대한 고속 몸통 충돌 피해는 유지되어야 한다.
- [ ] `retrace` / **되밟기 — 전설** — 최소 2초 정도 이동 경로를 만든 뒤 완충 근접 공격으로 발동한다. 잔상이 실제 지난 경로를 역으로 따라가야 하며, 그 경로에 걸친 적이 레벨별 50% / 75% / 100% 추가 피해를 받아야 한다. 현재 경로 기록은 플레이어 몸통 중심과 적 히트박스 중심을 같은 좌표계로 판정한다.
- [ ] `lightning` / **벼락 — 전설** — 발동 횟수를 채웠는데 주변 연쇄 대상이 없으면 발동 횟수가 사라지지 않고 **대기 상태로 유지**되어야 한다. 이후 주변에 두 번째 적을 두고 다음 직접 공격을 하면 즉시 연쇄 피해가 나가며, 대상에 실제 바닐라 번개 연출이 보여야 한다. 연출 번개 자체가 별도 피해/화재를 추가하면 실패다.
- [ ] `divine_punishment` / **천벌 — 신화** — 대상 위치에 0 / +4 / +8틱으로 바닐라 번개가 3번 치고, 주변에 붉은/암적색 갈라지는 번개형 입자와 충격 연출이 추가되어 일반 `벼락`보다 확실히 강해 보여야 한다. %체력 피해와 Lv.2 기절은 한 번만 적용되고 연출 번개는 추가 피해/화재를 내면 안 된다.
- [ ] **보스 판정** — 위더와 엔더 드래곤은 기존처럼 보스, 워든도 이제 Augmentation Survival 전투/제어 규칙에서 보스로 취급된다. 워든은 지배할 수 없어야 하며 천벌은 일반몹 60% / 90%가 아니라 보스 10% / 25% 분기를 사용해야 한다. 보스 판정이 곧 보스 코어 보상을 뜻하지는 않는다.

### Bundle H에서 아직 수동 확인이 남은 항목

- [ ] `area_acceleration` / **영역 가속 — 전설 Lv.1 / Lv.2** — 반경 24블록 안에 같은 광량·수분 조건의 밀 약 10칸 이상을 준비한다. **60초 지속 / Lv.1 15배 / Lv.2 30배**가 실제 성장과 생산에 반영되는지 확인한다. 지속시간/범위 HUD는 왼쪽에 떠야 한다.
- [ ] `domination` / **지배 — 신화** — 쿨다운/충전 표시가 없어야 한다. 자기 지배 개체를 바라보고 다시 사용하면 그 개체만 해제되어야 한다. 평소 주인 주변으로 복귀하며 동굴이나 먼 곳의 적을 무한 추격하면 안 된다.
- [ ] `auto_refill` / **자동 보충** — 의도적으로 빈 핫바 칸으로 선택을 옮기기만 했을 때는 자동 보충되면 안 된다. 같은 선택 슬롯의 스택 가능한 아이템 마지막 1개를 실제 사용해 `1 -> 0`이 된 경우에만 동일 아이템/컴포넌트 스택으로 보충되어야 한다.
- [ ] `time_acceleration` / **시간 가속 — 전설 Lv.1** — 3초 집중 중 움직이거나 아이템을 사용하면 집중이 끊긴다. 성공 후 20초 동안 이동/공격/채굴 +60%, 아이템 사용/자신의 투사체 +40%를 확인한다.

### 이전 Bundle에서 아직 수동 확인이 남은 항목

- [ ] `return_to_sender` / **돌려보내기 — 희귀 Lv.3 권장** — Survival에서 실제 방패로 공격을 3회 막는다. 3번째 방어 후 7블록 / 전체 45° 전방 충격파가 나와야 한다. Lv.1 / Lv.2 / Lv.3 실제 발동 횟수 5 / 4 / 3회, 반환 비율 60% / 80% / 100%.
- [ ] `invincibility` / **무적 — 전설 Lv.1** — 10초마다 자동 발동하는 3초 무적 동안 금색 반투명 보호막이 실제 아바타 몸통을 중심으로 감싸는지 3인칭에서 확인한다.
- [ ] `phase_shift` / **위상 이동 — 전설** — 고체 블록 내부에서 `IN_WALL` 질식 피해를 받지 않아야 하며 일반 공격/환경 피해까지 무적이 되면 안 된다.
- [ ] `rewind` / **되감기 — 전설** + 위상 이동 — 지형 내부 시점을 기록한 뒤 되감기해도 고체 블록 안으로 복원되지 않고 근처 안전 공간으로 보정되어야 한다.
- [ ] `revive` / **소생 — 전설 Lv.1** — 길들인 영구 동료와 지배 영구 동료: 20블록 안 실제 사망 -> 20초 표식 -> 3초 계속 조준 -> 최대체력 50% 부활 -> 성공 쿨 120초.
- [ ] `water_run` / **수상 보행** — real water surface에서 서기/걷기/달리기, 웅크려 잠수, 5블록 미만 착지 무낙뎀, 5블록 이상 다이브 관통, 수중에서 위로 수면 통과, 용암 비적용을 확인한다.
- [ ] `portable_riptide` / **휴대 수류** — 마른 땅에서 주손 Riptide 삼지창 + 보조손 물 양동이. 반복 사용에도 양동이는 보존되고 일반 수중 Riptide와 같은 몸통 회전 연출이 보여야 한다.

## Waiting for reproduction evidence

- None for Bundle I.

## Investigation closed

- Monster-room chests currently use an 8% normal-core roll per chest. Four chests have only about a 28.4% chance of at least one core, so one normal core from four chests is not evidence of a loot bug.

## Confirmed / do not reopen without a new symptom

- `last_stand` + `delayed_damage` — no new failure was reported after the delayed-debt clearing/protection repair.
- Previously confirmed lower-tier items remain closed unless code affecting them changes again.

## Deferred / separate implementation work

- `revive` allied-player branch — a dedicated multiplayer death/respawn-authority pass remains required. Do not duplicate a `ServerPlayer` entity.
- `aerial_combo` — requested air-jump strength 1.5x remains deferred and was not mixed into Bundle I.

## Verification boundary

Bundle I final verified checkpoint `b30d0a7e4a2bbeb4d5038bbe6861b18f1d80d6c0` passed **144/144 required GameTests**, clean build, dedicated-server boot smoke, client boot smoke, localization checks, verified MRPack generation and production-JAR verification in workflow **#544**.

- Production JAR SHA-256: `23fd6820f05f48405fab863ba50eafb876f0e78192d38daa6748e8cd7ed42dc1`
- MRPack SHA-256: `ef83d91403d48f8cc25e251d707e1c0a22697d9b0eccbb09ac6b5d0ba694848d`

These checks do not replace the manual visual/feel retests above. Bundle I post-fix playtesting and multiplayer testing remain **NOT RUN**.

Report failures as `augment O/X - symptom` with the simplest reproducible situation possible.
