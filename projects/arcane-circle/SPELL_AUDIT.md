# Arcane Circle — 109 Spell Audit Queue (alpha.54)

이 문서는 주문을 묶어서 '대충 동작'으로 보지 않고 하나씩 추적하기 위한 정본 감사 큐다.

## 검사 계약

- **S — Source contract**: 카탈로그 ID, 써클, 효과 설명이 명시적으로 존재한다.
- **R — Runtime route**: 실제 서버 실행 경로 중 적어도 하나가 해당 ID를 소유한다.
- **T — Target contract**: 자기/대상/지면/전방 판정이 주문 역할과 충돌하지 않는지 확인한다.
- **V — Visual/gameplay contract**: 보이는 위치·범위·타이밍과 실제 판정이 일치하는지 확인한다.
- **D — Deep behavior**: 지속상태, 해제, 사망/로그아웃/차원이동, NPC parity까지 수동 코드검사한다.

alpha.52에서 S/R 전 109종을 강제하고 복제계 타깃 오류를 고쳤다. alpha.53부터 T/V/D를 실제 주문 순서대로 닫는다.

## alpha.53 — 1써클 deep pass

1써클 10종은 전용 `FirstCircleSpellService`를 통해 오래된 generic effect보다 먼저 실행된다.

- `magic_missile`: 시전 순간 포착한 생명체를 유지하는 3발 합산 salvo. 일반 비유도 탄환과 분리.
- `fire_bolt`: 보이는 착탄점에 맞는 비유도 화염탄. 피해 후 화상.
- `ray_of_frost`: generic CHANNEL에서 제거. 단발 beam + 동결/둔화.
- `shield`: 기존 2장 반응 방벽 런타임을 정본으로 유지.
- `feather_fall`: 시전 즉시 누적 fall distance를 지우고 6초 안정 낙하.
- `light`: 실제 LightBlock 5점을 따라다니게 유지. 중첩 광원은 refcount로 공유.
- `grease`: 8초 유지형 slip field.
- `sleep`: weak-only 체급 제한 + 최대 7초 수면 + wake-on-hit + 기존 noAI 상태 복원.
- `thunderwave`: 고정한 전방 부채꼴 판정과 넉백, 같은 snapshot의 취약 지형 파손.
- `mage_armor`: 4장 재생 플레이트의 소모/재충전 피해 분산.
- NPC 마법사도 1써클 10종을 generic direct damage로 처리하지 않는다.

## alpha.54 — 2써클 deep pass

2써클 10종은 전용 `SecondCircleSpellService`가 1써클 다음 우선순위로 소유한다. 플레이어와 NPC가 같은 역할 계약을 사용한다.

- `scorching_ray`: 0.5초 간격 3회 실제 타격으로 vanilla hurt interval을 피하며 3광선 의미를 살림.
- `misty_step`: 가까운 안전 착지점을 서버에서 탐색하는 실제 단거리 이동. NPC도 전투 재배치에 사용.
- `web`: 11초 유지 이동 억제 영역.
- `mirror_image`: 환영 3체는 **hostile direct attack**만 대신 받고 낙하·화염·익사 같은 환경 피해는 막지 않음.
- `invisibility`: 주변 적대 추적을 끊고 첫 hostile direct attack 1회를 흘린 뒤 해제.
- `gust_of_wind`: 전방 직선 실제 넉백 + 거미줄/불/횃불 등 바람에 취약한 오브젝트 제거.
- `hold_person`: 일반 체급에만 9초 속박. 대형/보스급은 2써클로 봉쇄하지 못하며 Arcane 시전도 함께 끊김.
- `shatter`: 피해와 취약 블록 파괴가 동일한 고정 목표 중심을 공유.
- `blur`: 모든 피해 감쇄를 폐기하고 18초 동안 hostile direct attack만 **35% miss** 판정.
- `levitate`: 실제 상승 구간 뒤 느린 하강, 종료 후 안전 낙하 보호.
- logout/respawn/dimension/antimagic/server stop에서 상태를 정리하며 NPC도 동일 전용 경로를 사용한다.

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
| 3 | `fireball` | PASS | PASS | next |
| 3 | `lightning_bolt` | PASS | PASS | next |
| 3 | `fly` | PASS | PASS | next |
| 3 | `haste` | PASS | PASS | next |
| 3 | `dispel_magic` | PASS | PASS | next |
| 3 | `vampiric_touch` | PASS | PASS | next |
| 3 | `slow` | PASS | PASS | next |
| 3 | `protection_from_energy` | PASS | PASS | next |
| 3 | `sleet_storm` | PASS | PASS | next |
| 3 | `blink` | PASS | PASS | next |
| 4 | `wall_of_fire` | PASS | PASS | next |
| 4 | `ice_storm` | PASS | PASS | next |
| 4 | `greater_invisibility` | PASS | PASS | next |
| 4 | `resilient_sphere` | PASS | PASS | next |
| 4 | `dimension_door` | PASS | PASS | next |
| 4 | `stoneskin` | PASS | PASS | next |
| 4 | `confusion` | PASS | PASS | next |
| 4 | `blight` | PASS | PASS | next |
| 4 | `freedom_of_movement` | PASS | PASS | next |
| 4 | `phantasmal_killer` | PASS | PASS | next |
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

`tools/test_current_source.py`는 109종 카탈로그/효과 요약/실행 경로 일치, 1·2써클 전용 권한 순서, NPC parity, direct-attack illusion 계약, lifecycle/antimagic cleanup, 그리고 alpha.49~53의 기존 고써클·1써클 계약 회귀를 실패 조건으로 둔다.
