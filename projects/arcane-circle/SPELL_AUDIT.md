# Arcane Circle — 109 Spell Audit Queue (alpha.52)

이 문서는 주문을 묶어서 '대충 동작'으로 보지 않고 하나씩 추적하기 위한 정본 감사 큐다.

## 검사 계약

- **S — Source contract**: 카탈로그 ID, 써클, 효과 설명이 명시적으로 존재한다.
- **R — Runtime route**: 실제 서버 실행 경로 중 적어도 하나가 해당 ID를 소유한다.
- **T — Target contract**: 자기/대상/지면/전방 판정이 주문 역할과 충돌하지 않는지 확인한다.
- **V — Visual/gameplay contract**: 보이는 위치·범위·타이밍과 실제 판정이 일치하는지 확인한다.
- **D — Deep behavior**: 지속상태, 해제, 사망/로그아웃/차원이동, NPC parity까지 수동 코드검사한다.

alpha.52 1차 패스는 **S/R 전 109종 전수 강제**와 복제계 T 오류 수정을 완료한다. V/D는 아래 순서대로 계속 닫는다.

## 이번 패스에서 즉시 수정한 결함

- `clone`: BODY/0m 자기 주문처럼 노출되던 계약을 **조준 생명체 / 32m**로 교정. 실제 독립 복제체 런타임과 일치시킴.
- `simulacrum`: BODY/0m 때문에 조준 원본을 얻지 못하던 계약을 **조준 생명체 / 28m**로 교정. 명령 가능한 반실체 복제체 런타임과 일치시킴.
- 주문 상세 효과를 일반 화면에서 분리하고 **효과 도감**에서만 기계적 설명을 집중 표시.
- 마도회 메인 페이지는 소속명·설명·강점·약점·본거지를 큰 정보 영역으로 승격.

## Direct spells — deep audit order

| Circle | Spell | S | R | T/V/D |
|---:|---|:---:|:---:|---|
| 1 | `magic_missile` | PASS | PASS | next |
| 1 | `fire_bolt` | PASS | PASS | next |
| 1 | `ray_of_frost` | PASS | PASS | next |
| 1 | `shield` | PASS | PASS | next |
| 1 | `feather_fall` | PASS | PASS | next |
| 1 | `light` | PASS | PASS | next |
| 1 | `grease` | PASS | PASS | next |
| 1 | `sleep` | PASS | PASS | next |
| 1 | `thunderwave` | PASS | PASS | next |
| 1 | `mage_armor` | PASS | PASS | next |
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
- `clone`/`simulacrum`의 유효 타깃 계약이 TARGET + 양의 사거리
- 효과 도감 UI와 메인/효과 분리 구조 존재
- alpha.49~51의 Plane Shift/Demiplane/Simulacrum/고써클 제어/Globe 전용 런타임 회귀 금지
