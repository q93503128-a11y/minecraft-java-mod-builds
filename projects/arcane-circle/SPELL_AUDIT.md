# Arcane Circle — 109 Spell Audit Queue (alpha.61)

S/R은 109종 전부 명시적 source/runtime route를 요구한다. T/V/D는 타깃·보이는 범위/타이밍·지속/해제/NPC parity를 함께 검사한다.

## alpha.61 — 9써클 deep pass

`NinthCircleSpellService`가 9써클 10종의 최종 전용 권한층이다. 플레이어 Shapechange/Foresight, Time Stop/Wish, True Polymorph의 강한 기존 의미는 각각 ArcaneBuffRuntime, ArcaneFieldService, HighUtilitySpellService에 위임해 퇴행시키지 않는다.

- `meteor_swarm`: seeded cratering meteor sequence. 보이는 seeded 연속 착탄과 동일 지점에 피해·화상·충격을 적용하며 플레이어 운석은 실제 크레이터를 남긴다.
- `power_word_kill`: 릴리즈 순간 고정한 단일 생명체만 처형 역치를 검사하며 소실 시 다른 적으로 재조준하지 않는다.
- `prismatic_wall`: seven-layer physical prism wall. 20초 물리 경계 + 7개 독립 층 효과 + 적대 Arcane 궤적 차단.
- `shapechange`: 기존 90초 초월 육체/50% 일반 피해 감쇠를 보존하며 NPC도 같은 전투 체급을 갖는다.
- `time_stop`: 기존 실제 시간 정지를 보존하고 NPC도 AI·이동체·Arcane 시전을 정지한 뒤 원상 복구한다.
- `true_polymorph`: 플레이어 기존 실제 body-swap을 보존한다. NPC→Mob도 실제 임시 몸체를 만들며 NPC→Player는 엔티티 타입 교체 대신 전투·시전 억제 role parity다.
- `weird`: 15초 유지형 집단 공포. 주기 정신 피해·암흑·시전 방해, Mob 실제 도주와 이전 타깃 복구.
- `wish`: 실제 마력 완전 복구 + Wish 외 쿨타임 초기화 + 회복/정화를 보존한다. NPC는 플레이어 마력 저장소가 없으므로 완전 회복·정화 role parity다.
- `gate`: two-way world gate. 30초간 두 안전 지점을 잇고 양방향 생명체 통과와 재진입 방지를 제공한다.
- `foresight`: 기존 120초 예지, 2초마다 완전 회피 + 사이 피해 25% 감쇠를 보존하며 NPC도 동일 규칙을 사용한다.

Dispel/Antimagic은 기존 HighControl cleanup 경로에서 NinthCircle cleanup까지 연결되며 logout/respawn/dimension/server stop에서도 원상 복구한다.

## Direct spells — deep audit order

| Circle | Spell | S | R | T/V/D |
|---:|---|:---:|:---:|---|
| 1 | `magic_missile` | PASS | PASS | alpha.53 PASS |
| 1 | `fire_bolt` | PASS | PASS | alpha.53 PASS |
| 1 | `ray_of_frost` | PASS | PASS | alpha.53 PASS |
| 1 | `shield` | PASS | PASS | alpha.53 PASS |
| 1 | `feather_fall` | PASS | PASS | alpha.53 PASS |
| 1 | `light` | PASS | PASS | alpha.53 PASS |
| 1 | `grease` | PASS | PASS | alpha.53 PASS |
| 1 | `sleep` | PASS | PASS | alpha.53 PASS |
| 1 | `thunderwave` | PASS | PASS | alpha.53 PASS |
| 1 | `mage_armor` | PASS | PASS | alpha.53 PASS |
| 2 | `scorching_ray` | PASS | PASS | alpha.54 PASS |
| 2 | `misty_step` | PASS | PASS | alpha.54 PASS |
| 2 | `web` | PASS | PASS | alpha.54 PASS |
| 2 | `mirror_image` | PASS | PASS | alpha.54 PASS |
| 2 | `invisibility` | PASS | PASS | alpha.54 PASS |
| 2 | `gust_of_wind` | PASS | PASS | alpha.54 PASS |
| 2 | `hold_person` | PASS | PASS | alpha.54 PASS |
| 2 | `shatter` | PASS | PASS | alpha.54 PASS |
| 2 | `blur` | PASS | PASS | alpha.54 PASS |
| 2 | `levitate` | PASS | PASS | alpha.54 PASS |
| 3 | `fireball` | PASS | PASS | alpha.55 PASS |
| 3 | `lightning_bolt` | PASS | PASS | alpha.55 PASS |
| 3 | `fly` | PASS | PASS | alpha.55 PASS |
| 3 | `haste` | PASS | PASS | alpha.55 PASS |
| 3 | `dispel_magic` | PASS | PASS | alpha.55 PASS |
| 3 | `vampiric_touch` | PASS | PASS | alpha.55 PASS |
| 3 | `slow` | PASS | PASS | alpha.55 PASS |
| 3 | `protection_from_energy` | PASS | PASS | alpha.55 PASS |
| 3 | `sleet_storm` | PASS | PASS | alpha.55 PASS |
| 3 | `blink` | PASS | PASS | alpha.55 PASS |
| 4 | `wall_of_fire` | PASS | PASS | alpha.56 PASS |
| 4 | `ice_storm` | PASS | PASS | alpha.56 PASS |
| 4 | `greater_invisibility` | PASS | PASS | alpha.56 PASS |
| 4 | `resilient_sphere` | PASS | PASS | alpha.56 PASS |
| 4 | `dimension_door` | PASS | PASS | alpha.56 PASS |
| 4 | `stoneskin` | PASS | PASS | alpha.56 PASS |
| 4 | `confusion` | PASS | PASS | alpha.56 PASS |
| 4 | `blight` | PASS | PASS | alpha.56 PASS |
| 4 | `freedom_of_movement` | PASS | PASS | alpha.56 PASS |
| 4 | `phantasmal_killer` | PASS | PASS | alpha.56 PASS |
| 5 | `cone_of_cold` | PASS | PASS | alpha.57 PASS |
| 5 | `wall_of_force` | PASS | PASS | alpha.57 PASS |
| 5 | `cloudkill` | PASS | PASS | alpha.57 PASS |
| 5 | `telekinesis` | PASS | PASS | alpha.57 PASS |
| 5 | `flame_strike` | PASS | PASS | alpha.57 PASS |
| 5 | `hold_monster` | PASS | PASS | alpha.57 PASS |
| 5 | `mass_cure_wounds` | PASS | PASS | alpha.57 PASS |
| 5 | `passwall` | PASS | PASS | alpha.57 PASS |
| 5 | `dominate_person` | PASS | PASS | alpha.57 PASS |
| 5 | `insect_plague` | PASS | PASS | alpha.57 PASS |
| 6 | `disintegrate` | PASS | PASS | alpha.58 PASS |
| 6 | `globe_of_invulnerability` | PASS | PASS | alpha.58 PASS |
| 6 | `mass_suggestion` | PASS | PASS | alpha.58 PASS |
| 6 | `move_earth` | PASS | PASS | alpha.58 PASS |
| 6 | `sunbeam` | PASS | PASS | alpha.58 PASS |
| 6 | `true_seeing` | PASS | PASS | alpha.58 PASS |
| 6 | `freezing_sphere` | PASS | PASS | alpha.58 PASS |
| 6 | `eyebite` | PASS | PASS | alpha.58 PASS |
| 6 | `flesh_to_stone` | PASS | PASS | alpha.58 PASS |
| 6 | `circle_of_death` | PASS | PASS | alpha.58 PASS |
| 7 | `delayed_blast_fireball` | PASS | PASS | alpha.59 PASS |
| 7 | `etherealness` | PASS | PASS | alpha.59 PASS |
| 7 | `finger_of_death` | PASS | PASS | alpha.59 PASS |
| 7 | `fire_storm` | PASS | PASS | alpha.59 PASS |
| 7 | `forcecage` | PASS | PASS | alpha.59 PASS |
| 7 | `plane_shift` | PASS | PASS | alpha.59 PASS |
| 7 | `prismatic_spray` | PASS | PASS | alpha.59 PASS |
| 7 | `reverse_gravity` | PASS | PASS | alpha.59 PASS |
| 7 | `simulacrum` | PASS | PASS | alpha.59 PASS |
| 7 | `teleport` | PASS | PASS | alpha.59 PASS |
| 8 | `antimagic_field` | PASS | PASS | alpha.60 PASS |
| 8 | `clone` | PASS | PASS | alpha.60 PASS |
| 8 | `control_weather` | PASS | PASS | alpha.60 PASS |
| 8 | `demiplane` | PASS | PASS | alpha.60 PASS |
| 8 | `dominate_monster` | PASS | PASS | alpha.60 PASS |
| 8 | `earthquake` | PASS | PASS | alpha.60 PASS |
| 8 | `feeblemind` | PASS | PASS | alpha.60 PASS |
| 8 | `incendiary_cloud` | PASS | PASS | alpha.60 PASS |
| 8 | `maze` | PASS | PASS | alpha.60 PASS |
| 8 | `sunburst` | PASS | PASS | alpha.60 PASS |
| 9 | `meteor_swarm` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `power_word_kill` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `prismatic_wall` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `shapechange` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `time_stop` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `true_polymorph` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `weird` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `wish` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `gate` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `foresight` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |

## Fusion spells — audit order

| Spell | S | R | T/V/D |
|---|:---:|:---:|---|
| `burning_hands` | PASS | PASS | preserved · dedicated fusion runtime |
| `ice_knife` | PASS | PASS | preserved · dedicated fusion runtime |
| `chromatic_orb` | PASS | PASS | preserved · dedicated fusion runtime |
| `wind_wall` | PASS | PASS | preserved · dedicated fusion runtime |
| `counterspell` | PASS | PASS | preserved · dedicated fusion runtime |
| `fire_shield` | PASS | PASS | preserved · dedicated fusion runtime |
| `wall_of_ice` | PASS | PASS | preserved · dedicated fusion runtime |
| `chain_lightning` | PASS | PASS | preserved · dedicated fusion runtime |
| `arcane_hand` | PASS | PASS | preserved · dedicated fusion runtime |
| `teleportation_circle` | PASS | PASS | preserved · dedicated fusion runtime |
| `steam_burst` | PASS | PASS | preserved · dedicated fusion runtime |
| `frost_step` | PASS | PASS | preserved · dedicated fusion runtime |
| `thunder_cage` | PASS | PASS | preserved · dedicated fusion runtime |
| `solar_guard` | PASS | PASS | preserved · dedicated fusion runtime |
| `void_lance` | PASS | PASS | preserved · dedicated fusion runtime |
| `winter_domain` | PASS | PASS | preserved · dedicated fusion runtime |
| `astral_prison` | PASS | PASS | preserved · dedicated fusion runtime |
| `phoenix_requiem` | PASS | PASS | preserved · dedicated fusion runtime |
| `world_sunder` | PASS | PASS | preserved · dedicated fusion runtime |

## CI enforcement

`tools/test_current_source.py`는 90 direct + 19 fusion, 1~9써클 전용 route, alpha.53~60 회귀 anchor, alpha.61 9C authority/NPC parity, Wish mana/cooldown 복구, seeded Meteor, physical Prismatic Wall, Weird/Gate 유지 상태, Dispel/Antimagic/lifecycle을 실패 조건으로 둔다. `tools/verify_jar.py`는 동일 metadata와 NinthCircle class를 패키지에서 다시 검증한다.
