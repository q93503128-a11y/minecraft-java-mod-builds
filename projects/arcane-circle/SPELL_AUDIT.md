# Arcane Circle — 109 Spell Audit Queue (alpha.62)

S/R은 109종 전부 명시적 source/runtime route를 요구한다. T/V/D는 타깃·보이는 범위/타이밍·지속/해제/NPC parity를 함께 검사한다.

## alpha.62 — UI readability + 1~9써클 presentation prestige pass

alpha.62는 기존 1~9써클 게임플레이 권한을 축소하지 않고, **읽히는 UI / 써클 체급 / 연출 점유 공간 / 역할 중복 제거**를 한 번에 정리한다.

- `ReadableGrimoireScreen`: 주문/마도회 본문과 행동 칼럼을 물리적으로 분리한다. 긴 설명은 wrap/fit을 거치고, 1~5 장착 슬롯은 주문명·써클·학파·실제 남은 쿨타임이 보이는 카드다.
- `CircleScaleEnvelope`: 1~6써클의 추가 presentation-only 스케일 문법. 1~2C는 hand-scale immediate, 3~4C는 combat-space, 5~6C는 multi-plane grand magic이다. 이 레이어는 피해/판정/지형 범위를 변경하지 않는다.
- `HighCirclePrestigeOverlay`: 7C는 fortress/planar authority, 8C는 regional/reality authority, 9C는 world-law catastrophe로 공간 점유 방식 자체를 분리한다. 단일 대상 법칙술은 무작정 거대화하지 않고 압축된 다중 평면/법칙 봉인 밀도로 위계를 표현한다.
- `fire_storm`: 7C 상공 6개 낙하 포트와 각 지상 낙하지점이 연결되는 화염 지배 장면.
- `reverse_gravity`: 7C 지면에서 상공까지 이어지는 중력 대성당형 유지 구조.
- `plane_shift`: 7개 평면이 겹치는 전이 구조와 목적지 게이트를 연결한다.
- `forcecage`: 대상 주위 7각 물리 성채형 시각 경계. 기존 실제 이동 경계 의미는 보존한다.
- `earthquake` / `control_weather` / `sunburst`: 8C 지역 단위 지면·상공 장면으로 확대한다.
- `meteor_swarm`: 15발 seeded 전장 파쇄 이후 별도 16번째 Crown Meteor가 지연 낙하한다. Crown은 일반 운석보다 명확히 큰 시각 몸체와 별도 광역 생명체 충격·소멸 분지를 가진다.
- `time_stop` / `wish` / `gate` / `world_sunder` / `prismatic_wall`: 9C 전용 세계법칙급 공간 구조로 확대한다.
- `DeathDoctrineService`: 죽음계 역할을 고정한다. 6C `circle_of_death`=광역 생명 침식(처형 없음), 7C `finger_of_death`=단일 영혼 파열(즉사 역치 없음), 9C `power_word_kill`=유일한 법칙 처형 + 역치 외 대상에도 9C 생명 붕괴 피해.

### alpha.61 9써클 authority 보존

`NinthCircleSpellService`가 9써클 10종의 전용 권한층인 구조는 유지한다. 플레이어 Shapechange/Foresight, Time Stop/Wish, True Polymorph의 강한 기존 의미는 각각 ArcaneBuffRuntime, ArcaneFieldService, HighUtilitySpellService에 위임해 퇴행시키지 않는다. alpha.62의 DeathDoctrine/Crown Meteor는 겹치던 역할을 더 높은 우선순위에서 명시적으로 분리하는 추가 권한층이다.

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
| 6 | `circle_of_death` | PASS | PASS | alpha.62 PASS · broad life erosion / no execution |
| 7 | `delayed_blast_fireball` | PASS | PASS | alpha.59 PASS |
| 7 | `etherealness` | PASS | PASS | alpha.59 PASS |
| 7 | `finger_of_death` | PASS | PASS | alpha.62 PASS · locked soul rupture / no threshold execution |
| 7 | `fire_storm` | PASS | PASS | alpha.62 PASS · six-pillar dominion presentation |
| 7 | `forcecage` | PASS | PASS | alpha.62 PASS · physical citadel presentation |
| 7 | `plane_shift` | PASS | PASS | alpha.62 PASS · seven-plane transit presentation |
| 7 | `prismatic_spray` | PASS | PASS | alpha.62 PASS · seven-ray prestige fan |
| 7 | `reverse_gravity` | PASS | PASS | alpha.62 PASS · gravity cathedral presentation |
| 7 | `simulacrum` | PASS | PASS | alpha.59 PASS |
| 7 | `teleport` | PASS | PASS | alpha.59 PASS |
| 8 | `antimagic_field` | PASS | PASS | alpha.60 PASS |
| 8 | `clone` | PASS | PASS | alpha.60 PASS |
| 8 | `control_weather` | PASS | PASS | alpha.62 PASS · regional sky authority |
| 8 | `demiplane` | PASS | PASS | alpha.60 PASS |
| 8 | `dominate_monster` | PASS | PASS | alpha.60 PASS |
| 8 | `earthquake` | PASS | PASS | alpha.62 PASS · regional fault presentation |
| 8 | `feeblemind` | PASS | PASS | alpha.60 PASS |
| 8 | `incendiary_cloud` | PASS | PASS | alpha.60 PASS |
| 8 | `maze` | PASS | PASS | alpha.60 PASS |
| 8 | `sunburst` | PASS | PASS | alpha.62 PASS · solar judgment presentation |
| 9 | `meteor_swarm` | PASS | PASS | alpha.62 PASS · 15 barrage + Crown Meteor catastrophe |
| 9 | `power_word_kill` | PASS | PASS | alpha.62 PASS · exclusive 9C execution law |
| 9 | `prismatic_wall` | PASS | PASS | alpha.62 PASS · world-scale seven-layer wall |
| 9 | `shapechange` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `time_stop` | PASS | PASS | alpha.62 PASS · world-law temporal presentation |
| 9 | `true_polymorph` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `weird` | PASS | PASS | alpha.61 PASS · dedicated 9C authority/NPC parity |
| 9 | `wish` | PASS | PASS | alpha.62 PASS · stacked reality rewrite presentation |
| 9 | `gate` | PASS | PASS | alpha.62 PASS · giant two-endpoint gate presentation |
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

`tools/test_current_source.py`는 90 direct + 19 fusion, 1~9써클 전용 route, alpha.53~60 회귀 anchor, alpha.61 9C 보존 authority, alpha.62 DeathDoctrine/Crown Meteor/readable grimoire/1~6 scale envelope/7~9 prestige hierarchy, NPC parity, Dispel/Antimagic/lifecycle을 실패 조건으로 둔다. `tools/verify_jar.py`는 동일 metadata와 신규 UI·presentation·death/cataclysm class가 실제 JAR에 포함됐는지 다시 검증한다.
