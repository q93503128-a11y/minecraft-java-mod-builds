# Arcane Circle — 109 Spell Audit Queue (alpha.56)

이 문서는 주문을 묶어서 '대충 동작'으로 보지 않고 하나씩 추적하기 위한 정본 감사 큐다.

## 검사 계약

- **S — Source contract**: 카탈로그 ID, 써클, 효과 설명이 명시적으로 존재한다.
- **R — Runtime route**: 실제 서버 실행 경로 중 적어도 하나가 해당 ID를 소유한다.
- **T — Target contract**: 자기/대상/지면/전방 판정이 주문 역할과 충돌하지 않는지 확인한다.
- **V — Visual/gameplay contract**: 보이는 위치·범위·타이밍과 실제 판정이 일치하는지 확인한다.
- **D — Deep behavior**: 지속상태, 해제, 사망/로그아웃/차원이동, NPC parity까지 수동 코드검사한다.

alpha.52에서 S/R 전 109종을 강제했고, alpha.53부터 T/V/D를 써클 순서대로 닫고 있다.

## alpha.53 — 1써클 deep pass

`FirstCircleSpellService`가 10종을 전용 소유한다. Magic Missile locked salvo, Fire Bolt 비유도 착탄, 단발 Ray of Frost, 반응형 Shield, 안전 Feather Fall, refcount 실제 Light, Grease slip field, weak-only/wake-on-hit Sleep, 물리 Thunderwave, 재생 Mage Armor를 유지한다. NPC도 같은 1써클 전용 경로를 사용한다.

## alpha.54 — 2써클 deep pass

`SecondCircleSpellService`가 10종을 전용 소유한다. Scorching Ray timed 3-hit, 안전 Misty Step, Web 지속장, direct-attack Mirror/Invisibility/Blur, 실제 Gust, 체급 제한 Hold Person, 단일 중심 Shatter, rise→safe descent Levitate를 유지한다. NPC도 같은 역할 경로를 사용한다.

## alpha.55 — 3써클 deep pass

`ThirdCircleSpellService`가 3써클 10종을 전용 소유하고 NPC도 generic damage보다 먼저 같은 역할 경로를 사용한다.

- `fireball`: 고정 snapshot 착탄점에 중심부가 더 강한 falloff 폭발 + 화상 + 같은 중심의 실제 지형 파괴.
- `lightning_bolt`: 고정 직선의 복수 대상 관통 타격 + 같은 경로의 약한 지형 파손.
- `fly`: 실제 자유 비행 권한과 NPC 공중 전투를 갖고 lifecycle에서 원래 중력/비행 권한으로 복구.
- `haste`: Arcane 시전시간 28% 단축 / 쿨다운 15% 단축.
- `dispel_magic`: 유지형 마법 상태와 해로운 상태를 해제하는 전용 경로.
- `vampiric_touch`: 실제로 잃게 한 체력+흡수량을 측정해 60% 회복.
- `slow`: 9초 persistent tempo field.
- `protection_from_energy`: Arcane/화염/투사체성 충격만 45% 줄이는 energy-only 5중 공명막.
- `sleet_storm`: 냉기·동결·암흑·미끄럼 + 내부 적대 Arcane casting denial.
- `blink`: 최대 약 20m endpoint-safe 공간 도약 + 착지 직후 위상 저항.

## alpha.56 — 4써클 deep pass

`FourthCircleSpellService`가 4써클 10종을 전용 소유한다. 플레이어와 NPC가 같은 역할 계약을 사용하며, 일반 피해감소/포션 묶음으로 뭉개던 효과를 전략 주문으로 분리했다.

- `wall_of_fire`: 12초 동안 고정된 실제 선형 장벽을 유지하고, 벽을 스치거나 통과하는 적만 반복 피해·연소시킨다.
- `ice_storm`: 오래 붙는 generic 냉기장이 아니라 고정 목표에 5회의 실제 우박 폭격이 이어진다.
- `greater_invisibility`: 공격해도 해제되지 않는 전투 은신. 적대 추적을 계속 끊고 hostile direct attack은 45% miss 판정을 받는다.
- `resilient_sphere`: **two-way isolation**. 구체 내부 대상은 외부 피해를 받지 않고, 내부에서 외부로 가하는 피해도 차단되며 Arcane 시전 역시 봉쇄된다.
- `dimension_door`: 최대 약 36m 안전 도약. 3m 이내에서 웅크린 플레이어 1명을 동행시켜 Misty Step/Blink와 역할을 분리한다.
- `stoneskin`: 범용 피해감소가 아니다. 적이 가하는 **physical-only** 비마법 공격만 50% 경감하고 Arcane·화염·환경 피해는 통과한다.
- `confusion`: 12초 동안 매초 정지/배회/오인 공격/비틀림 중 하나로 의사결정을 섞고 Arcane 시전도 간헐적으로 끊는다. NoAI로 굳히지 않는다.
- `blight`: 초기 생명 피해 후 8초 추가 drain과 **anti-heal** 80%를 건다.
- `freedom_of_movement`: 26초 동안 둔화·속박·동결·강제부양을 지속 정화하고 2~3써클 이동 제어의 시전 봉쇄를 무시한다. Antimagic/Time Stop/고써클 제어/Resilient Sphere는 그대로 우선한다.
- `phantasmal_killer`: 단순 약화가 아니라 11초 동안 대상이 시전자에게서 실제로 **forced flee**하며 주기적 정신 피해를 받는다.
- logout/respawn/dimension/antimagic/server stop에서 4써클 유지 상태를 정리하며 NPC도 같은 전용 경로를 사용한다.

## Direct spells — deep audit order

| Circle | Spell | S | R | T/V/D |
|---:|---|:---:|:---:|---|
| 1 | `magic_missile` | PASS | PASS | alpha.53 PASS · locked salvo |
| 1 | `fire_bolt` | PASS | PASS | alpha.53 PASS · non-homing impact |
| 1 | `ray_of_frost` | PASS | PASS | alpha.53 PASS · single beam |
| 1 | `shield` | PASS | PASS | alpha.53 PASS · reactive barriers |
| 1 | `feather_fall` | PASS | PASS | alpha.53 PASS · safe fall |
| 1 | `light` | PASS | PASS | alpha.53 PASS · refcount real light |
| 1 | `grease` | PASS | PASS | alpha.53 PASS · slip field |
| 1 | `sleep` | PASS | PASS | alpha.53 PASS · weak-only/wake-on-hit |
| 1 | `thunderwave` | PASS | PASS | alpha.53 PASS · cone + terrain |
| 1 | `mage_armor` | PASS | PASS | alpha.53 PASS · regenerating plates |
| 2 | `scorching_ray` | PASS | PASS | alpha.54 PASS · timed 3-hit salvo |
| 2 | `misty_step` | PASS | PASS | alpha.54 PASS · safe short teleport |
| 2 | `web` | PASS | PASS | alpha.54 PASS · persistent restraint field |
| 2 | `mirror_image` | PASS | PASS | alpha.54 PASS · direct attack only |
| 2 | `invisibility` | PASS | PASS | alpha.54 PASS · aggro break + first direct dodge |
| 2 | `gust_of_wind` | PASS | PASS | alpha.54 PASS · line force + fragile terrain |
| 2 | `hold_person` | PASS | PASS | alpha.54 PASS · restricted hard control |
| 2 | `shatter` | PASS | PASS | alpha.54 PASS · single impact center |
| 2 | `blur` | PASS | PASS | alpha.54 PASS · 35% direct attack miss |
| 2 | `levitate` | PASS | PASS | alpha.54 PASS · rise + safe descent |
| 3 | `fireball` | PASS | PASS | alpha.55 PASS · falloff blast + terrain |
| 3 | `lightning_bolt` | PASS | PASS | alpha.55 PASS · penetrating line + terrain |
| 3 | `fly` | PASS | PASS | alpha.55 PASS · lifecycle-safe real flight |
| 3 | `haste` | PASS | PASS | alpha.55 PASS · Arcane tempo accelerator |
| 3 | `dispel_magic` | PASS | PASS | alpha.55 PASS · custom-state dispel |
| 3 | `vampiric_touch` | PASS | PASS | alpha.55 PASS · actual-damage drain |
| 3 | `slow` | PASS | PASS | alpha.55 PASS · persistent tempo field |
| 3 | `protection_from_energy` | PASS | PASS | alpha.55 PASS · energy-only 5-charge ward |
| 3 | `sleet_storm` | PASS | PASS | alpha.55 PASS · cold field + casting denial |
| 3 | `blink` | PASS | PASS | alpha.55 PASS · safe long jump + phase guard |
| 4 | `wall_of_fire` | PASS | PASS | alpha.56 PASS · persistent crossing wall |
| 4 | `ice_storm` | PASS | PASS | alpha.56 PASS · five-pulse hail barrage |
| 4 | `greater_invisibility` | PASS | PASS | alpha.56 PASS · combat veil/aggro break |
| 4 | `resilient_sphere` | PASS | PASS | alpha.56 PASS · two-way isolation |
| 4 | `dimension_door` | PASS | PASS | alpha.56 PASS · safe companion transport |
| 4 | `stoneskin` | PASS | PASS | alpha.56 PASS · physical-only reduction |
| 4 | `confusion` | PASS | PASS | alpha.56 PASS · decision scramble |
| 4 | `blight` | PASS | PASS | alpha.56 PASS · anti-heal life decay |
| 4 | `freedom_of_movement` | PASS | PASS | alpha.56 PASS · maintained control immunity |
| 4 | `phantasmal_killer` | PASS | PASS | alpha.56 PASS · forced flee fear |
| 5 | `cone_of_cold` | PASS | PASS | next |
| 5 | `wall_of_force` | PASS | PASS | next |
| 5 | `cloudkill` | PASS | PASS | next |
| 5 | `telekinesis` | PASS | PASS | next |
| 5 | `flame_strike` | PASS | PASS | next |
| 5 | `hold_monster` | PASS | PASS | next |
| 5 | `mass_cure_wounds` | PASS | PASS | next |
| 5 | `passwall` | PASS | PASS | next |
| 5 | `dominate_person` | PASS | PASS | next |
| 5 | `insect_plague` | PASS | PASS | next |
| 6 | `disintegrate` | PASS | PASS | next |
| 6 | `globe_of_invulnerability` | PASS | PASS | next |
| 6 | `mass_suggestion` | PASS | PASS | next |
| 6 | `move_earth` | PASS | PASS | next |
| 6 | `sunbeam` | PASS | PASS | next |
| 6 | `true_seeing` | PASS | PASS | next |
| 6 | `freezing_sphere` | PASS | PASS | next |
| 6 | `eyebite` | PASS | PASS | next |
| 6 | `flesh_to_stone` | PASS | PASS | next |
| 6 | `circle_of_death` | PASS | PASS | next |
| 7 | `delayed_blast_fireball` | PASS | PASS | next |
| 7 | `etherealness` | PASS | PASS | next |
| 7 | `finger_of_death` | PASS | PASS | next |
| 7 | `fire_storm` | PASS | PASS | next |
| 7 | `forcecage` | PASS | PASS | next |
| 7 | `plane_shift` | PASS | PASS | next |
| 7 | `prismatic_spray` | PASS | PASS | next |
| 7 | `reverse_gravity` | PASS | PASS | next |
| 7 | `simulacrum` | PASS | PASS | alpha.52 T FIXED; V/D next |
| 7 | `teleport` | PASS | PASS | next |
| 8 | `antimagic_field` | PASS | PASS | next |
| 8 | `clone` | PASS | PASS | alpha.52 T FIXED; V/D next |
| 8 | `control_weather` | PASS | PASS | next |
| 8 | `demiplane` | PASS | PASS | next |
| 8 | `dominate_monster` | PASS | PASS | next |
| 8 | `earthquake` | PASS | PASS | next |
| 8 | `feeblemind` | PASS | PASS | next |
| 8 | `incendiary_cloud` | PASS | PASS | next |
| 8 | `maze` | PASS | PASS | next |
| 8 | `sunburst` | PASS | PASS | next |
| 9 | `meteor_swarm` | PASS | PASS | next |
| 9 | `power_word_kill` | PASS | PASS | next |
| 9 | `prismatic_wall` | PASS | PASS | next |
| 9 | `shapechange` | PASS | PASS | next |
| 9 | `time_stop` | PASS | PASS | next |
| 9 | `true_polymorph` | PASS | PASS | next |
| 9 | `weird` | PASS | PASS | next |
| 9 | `wish` | PASS | PASS | next |
| 9 | `gate` | PASS | PASS | next |
| 9 | `foresight` | PASS | PASS | next |

## Fusion spells — deep audit order

| Spell | S | R | T/V/D |
|---|:---:|:---:|---|
| `burning_hands` | PASS | PASS | next |
| `ice_knife` | PASS | PASS | next |
| `chromatic_orb` | PASS | PASS | next |
| `wind_wall` | PASS | PASS | next |
| `counterspell` | PASS | PASS | next |
| `fire_shield` | PASS | PASS | next |
| `wall_of_ice` | PASS | PASS | next |
| `chain_lightning` | PASS | PASS | next |
| `arcane_hand` | PASS | PASS | next |
| `teleportation_circle` | PASS | PASS | next |
| `steam_burst` | PASS | PASS | next |
| `frost_step` | PASS | PASS | next |
| `thunder_cage` | PASS | PASS | next |
| `solar_guard` | PASS | PASS | next |
| `void_lance` | PASS | PASS | next |
| `winter_domain` | PASS | PASS | next |
| `astral_prison` | PASS | PASS | next |
| `phoenix_requiem` | PASS | PASS | next |
| `world_sunder` | PASS | PASS | next |

## CI enforcement

`tools/test_current_source.py`는 direct 90 + fusion 19 = 109, 효과 요약 ID 일치, 1·2·3·4써클 전용 권한 순서와 NPC parity, 4써클 two-way isolation/physical-only Stoneskin/decision scramble/anti-heal/forced flee/Freedom 우선순위 및 lifecycle cleanup, 그리고 alpha.49~55의 기존 계약 회귀를 실패 조건으로 둔다.
