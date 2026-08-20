# Arcane Circle — 109 Spell Audit Queue (alpha.53)

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
- `feather_fall`: 시전 즉시 누적 fall distance를 지우고 6초 안정 낙하. respawn/dimension lifecycle에서 잔존 상태 제거.
- `light`: 실제 LightBlock 5점을 따라다니게 유지. 멀티플레이 중첩 광원은 **refcount**로 공유해 한 플레이어의 종료가 다른 플레이어 광원을 지우지 않음.
- `grease`: 8초 유지형 slip field. 짧은 반복 둔화와 횡미끄러짐을 서버가 갱신.
- `sleep`: 보스급까지 무조건 정지하던 기존 alias를 폐기. **weak-only** 체급 제한 + 최대 7초 수면 + **wake-on-hit** + 기존 noAI 상태 복원.
- `thunderwave`: 고정한 전방 부채꼴 판정과 넉백, 그리고 같은 snapshot을 쓰는 취약 지형 파손.
- `mage_armor`: 4장 재생 플레이트의 소모/재충전 피해 분산을 정본으로 유지.
- NPC 마법사도 이 10종을 generic direct damage로 처리하지 않고 전용 1써클 경로를 사용한다.

## Direct spells — deep audit order

| Circle | Spell | S | R | T/V/D |
|---:|---|:---:|:---:|---|
| 1 | `magic_missile` | PASS | PASS | alpha.53 PASS · locked 3-dart salvo |
| 1 | `fire_bolt` | PASS | PASS | alpha.53 PASS · non-homing visible impact |
| 1 | `ray_of_frost` | PASS | PASS | alpha.53 PASS · single beam, no channel alias |
| 1 | `shield` | PASS | PASS | alpha.53 PASS · 2 reactive barriers |
| 1 | `feather_fall` | PASS | PASS | alpha.53 PASS · fall reset + lifecycle clear |
| 1 | `light` | PASS | PASS | alpha.53 PASS · real light + overlap refcount |
| 1 | `grease` | PASS | PASS | alpha.53 PASS · persistent slip pulses |
| 1 | `sleep` | PASS | PASS | alpha.53 PASS · weak-only + wake-on-hit + AI restore |
| 1 | `thunderwave` | PASS | PASS | alpha.53 PASS · cone + locked physical aftermath |
| 1 | `mage_armor` | PASS | PASS | alpha.53 PASS · 4 regenerating plates |
| 2 | `scorching_ray` | PASS | PASS | next |
| 2 | `misty_step` | PASS | PASS | next |
| 2 | `web` | PASS | PASS | next |
| 2 | `mirror_image` | PASS | PASS | next |
| 2 | `invisibility` | PASS | PASS | next |
| 2 | `gust_of_wind` | PASS | PASS | next |
| 2 | `hold_person` | PASS | PASS | next |
| 2 | `shatter` | PASS | PASS | next |
| 2 | `blur` | PASS | PASS | next |
| 2 | `levitate` | PASS | PASS | next |
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

`tools/test_current_source.py`가 다음을 실패 조건으로 둔다.

- direct 90 + fusion 19 = 정확히 109종
- 카탈로그 ID 집합과 `SpellEffectSummary`의 명시적 case 집합이 정확히 동일
- 109개 각 ID가 실제 서버 런타임 소유 파일들 중 하나에 존재
- alpha.53 1써클 10종 모두 전용 서버 권한 경로를 먼저 사용
- `ray_of_frost`가 CHANNEL cadence로 회귀하지 않음
- `sleep`의 weak-only / wake-on-hit / AI restore 계약 유지
- `light`의 실제 LightBlock 공유 refcount 유지
- NPC 1써클 전용 경로 유지
- 사망/로그아웃/차원이동에서 1써클 지속 상태가 잔존하지 않음
- alpha.49~52 Plane Shift/Demiplane/Simulacrum/고써클 제어/Globe/효과 도감/복제 타깃 계약 회귀 금지
