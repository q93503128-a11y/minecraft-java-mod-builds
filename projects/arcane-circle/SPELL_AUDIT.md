# Arcane Circle — 109 Spell Audit Queue (alpha.57)

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

`ThirdCircleSpellService`가 3써클 10종을 전용 소유하고 NPC도 generic damage보다 먼저 같은 역할 경로를 사용한다. Fireball/Lightning은 고정 snapshot 공간을 사용하고, Fly/Haste/Dispel/Vampiric Touch/Slow/Energy Protection/Sleet/Blink가 전용 지속·해제 계약을 갖는다. alpha.57에서 Dispel은 4·5써클 유지 상태까지 해제하도록 범위를 확장했다.

## alpha.56 — 4써클 deep pass

`FourthCircleSpellService`가 4써클 10종을 전용 소유한다. Wall of Fire 실제 장벽, 5회 Ice Storm, 전투 Greater Invisibility, two-way Resilient Sphere, 동행 Dimension Door, physical-only Stoneskin, decision scramble Confusion, anti-heal Blight, 하위 이동제어 면역 Freedom, forced-flee Phantasmal Killer를 유지한다. NPC도 같은 역할 경로를 사용한다.

## alpha.57 — 5써클 deep pass

`FifthCircleSpellService`가 5써클 10종을 전용 소유한다. 플레이어와 NPC가 같은 역할 계약을 사용하며 기존 generic area/control/teleport 별칭을 전장 제어 주문으로 분리했다.

- `cone_of_cold`: 시전 snapshot 방향을 따라 실제로 넓어지는 냉기 원뿔. 맞은 적의 불을 끄고 강하게 얼리며 전방으로 압박한다.
- `wall_of_force`: 12초 실제 역장벽. 적대 생명체가 벽을 넘지 못하게 밀어내고, 벽 면을 가로지르는 적대 Arcane 주문 경로도 player/NPC 공통으로 차단한다.
- `cloudkill`: 고정 독장판이 아니라 11초 동안 시전 방향으로 천천히 이동하는 독성 전선. 체력이 낮은 적에게 피해 압박이 더 강하다.
- `telekinesis`: 즉시 한 번 밀치는 효과가 아니라 최대 5초 동안 대상을 현재 시선 앞에 붙잡고, 종료 순간 그때의 시선 방향으로 투척한다. 초대형 대상은 저항한다.
- `flame_strike`: 하나의 고정 중심에 수직 화염 기둥 피해·연소·약한 상승을 적용하고 플레이어 시전은 동일 중심에 실제 지형 파괴를 연결한다.
- `hold_monster`: NoAI를 사용하지 않는 강제 속박. 일반 대상은 15초, 초대형/보스급은 약 7초로 지속시간 저항을 하며 이동·공격·Arcane 시전을 봉쇄한다.
- `mass_cure_wounds`: 자신과 실제 아군/소유 길들인 생명체를 동시에 회복하고 짧은 재생을 부여한다.
- `passwall`: 순간이동이 아니다. 보호 블록/블록 엔티티를 피하면서 실제 벽에 임시 통로를 열고 약 12초 후 통로가 비면 저장한 원본 BlockState로 복원한다. 복원 시 새로 놓인 블록은 덮어쓰지 않는다.
- `dominate_person`: 인간형 체급의 비플레이어 적을 13초 전투 대리체로 바꾼다. 시전자를 공격하지 않고 주변 위협과 싸우며 비전투 시 따라오고 Arcane 시전은 봉쇄된다.
- `insect_plague`: 이동하는 Cloudkill과 달리 11초 고정 swarm field. 반복 피해·행동 압박과 함께 내부 적의 Arcane 집중을 간헐적으로 끊는다.
- logout/respawn/dimension/antimagic/server stop에서 5써클 유지 상태를 정리하고 Telekinesis 중력·기존 타깃·Passwall 원본 블록을 복구한다.

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
| 3 | `dispel_magic` | PASS | PASS | alpha.57 PASS · custom-state dispel through 5C |
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
| 5 | `cone_of_cold` | PASS | PASS | alpha.57 PASS · widening freeze cone |
| 5 | `wall_of_force` | PASS | PASS | alpha.57 PASS · body + Arcane trajectory barrier |
| 5 | `cloudkill` | PASS | PASS | alpha.57 PASS · drifting poison front |
| 5 | `telekinesis` | PASS | PASS | alpha.57 PASS · sustained grab + look throw |
| 5 | `flame_strike` | PASS | PASS | alpha.57 PASS · vertical fixed-center strike |
| 5 | `hold_monster` | PASS | PASS | alpha.57 PASS · boss-resisted hard control |
| 5 | `mass_cure_wounds` | PASS | PASS | alpha.57 PASS · allied multi-heal |
| 5 | `passwall` | PASS | PASS | alpha.57 PASS · real tunnel + safe restore |
| 5 | `dominate_person` | PASS | PASS | alpha.57 PASS · person-scale combat proxy |
| 5 | `insect_plague` | PASS | PASS | alpha.57 PASS · fixed swarm + cast interruption |
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

`tools/test_current_source.py`는 direct 90 + fusion 19 = 109, 효과 요약 ID 일치, 1·2·3·4·5써클 전용 권한 순서와 NPC parity, 5써클 Force Wall player/NPC 주문 차단, Passwall 원본 블록 복원, Telekinesis 중력 복원, boss-resisted Hold, combat-proxy Domination, Insect casting denial, Dispel/Antimagic/lifecycle cleanup, 그리고 alpha.49~56의 기존 계약 회귀를 실패 조건으로 둔다.
