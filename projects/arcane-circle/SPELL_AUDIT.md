# Arcane Circle — 109 Spell Audit Queue (alpha.60)

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

`ThirdCircleSpellService`가 3써클 10종을 전용 소유하고 NPC도 generic damage보다 먼저 같은 역할 경로를 사용한다. Fireball/Lightning은 고정 snapshot 공간을 사용하고, Fly/Haste/Dispel/Vampiric Touch/Slow/Energy Protection/Sleet/Blink가 전용 지속·해제 계약을 갖는다. alpha.60에서 Dispel은 4·5·6·7·8써클 유지 상태와 Etherealness/Simulacrum 같은 별도 고써클 권한까지 해제하도록 범위를 확장했다.

## alpha.56 — 4써클 deep pass

`FourthCircleSpellService`가 4써클 10종을 전용 소유한다. Wall of Fire 실제 장벽, 5회 Ice Storm, 전투 Greater Invisibility, two-way Resilient Sphere, 동행 Dimension Door, physical-only Stoneskin, decision scramble Confusion, anti-heal Blight, 하위 이동제어 면역 Freedom, forced-flee Phantasmal Killer를 유지한다. NPC도 같은 역할 경로를 사용한다.

## alpha.57 — 5써클 deep pass

`FifthCircleSpellService`가 5써클 10종을 전용 소유한다. widening Cone of Cold, 생명체와 Arcane 궤적을 모두 막는 Wall of Force, 이동 Cloudkill, sustained Telekinesis, vertical Flame Strike, boss-resisted Hold Monster, allied Mass Cure, 실제 벽을 열고 복원하는 Passwall, person-scale combat Domination, casting-break Insect Plague를 유지한다. Force Wall은 양쪽 면에서 같은 충돌 규칙을 사용한다.

## alpha.58 — 6써클 deep pass

`SixthCircleSpellService`가 6써클 10종의 전용 권한층을 추가한다. 이미 의미가 강한 player Globe/Mass Suggestion/True Seeing/Flesh to Stone은 기존 전용 런타임을 그대로 재사용하고, 나머지 공격 주문을 고정 snapshot 기반 전용 판정으로 분리했다. NPC는 더 이상 6써클을 generic damage로 처리하지 않는다.

- `disintegrate`: 가느다란 고정 분해광선으로 복수 생명체를 관통하고 플레이어 시전은 **같은 광선 경로의 실제 물질 파괴**를 수행한다.
- `globe_of_invulnerability`: 플레이어의 기존 26초 1~5써클 경계 소거 계약을 보존한다. NPC 6써클 마법사도 같은 반경·지속시간으로 **외부에서 안으로 들어오는 적대 1~5써클 주문을 실제 차단**한다. 6써클 이상과 물리 공격은 통과한다.
- `mass_suggestion`: 플레이어의 기존 behavioral retreat를 보존한다. NPC 시전도 8초 동안 여러 대상이 공격을 끊고 실제로 약 22m 후퇴하며 Arcane 시전이 억제되고 종료 시 기존 타깃을 복구한다.
- `move_earth`: 고정 목표 지면의 대형 범위에 거리 감쇠 피해를 주고 적을 바깥·위로 밀어 올린다. 플레이어 시전은 같은 중심/반경의 실제 지형 변형을 연결한다.
- `sunbeam`: 넓은 고정 직선의 복수 대상을 관통해 피해·화상·장기 실명·발광을 준다.
- `true_seeing`: 플레이어는 기존 60초 반복 reveal을 유지한다. NPC도 60초 동안 주변 적대 생명체의 투명화를 반복 제거하고 발광시킨다.
- `freezing_sphere`: 하나의 고정 착탄점에 거리 감쇠 냉기 폭발을 적용하고 화염을 끄며 초강력 동결·둔화를 준다.
- `eyebite`: 단일 정신 피해 뒤 18초 공포·쇠약·암흑·행동 저하를 건다. 비플레이어 대상은 효과가 끝날 때까지 타깃을 잃고 시전자에게서 계속 강제 도주하며 Arcane 시전도 중단된다. 종료·해제 시 이전 전투 타깃을 복구한다.
- `flesh_to_stone`: 플레이어 대상은 기존 유지형 석화 제어를 재사용하고, NPC 경로도 약 18초 이동·공격·Arcane 시전을 봉쇄하는 지속 석화와 석질 저항을 적용한다.
- `circle_of_death`: 단순 평면 광역기가 아니라 넓은 생명 파동이다. 일반 대상은 피해를 받고, 낮은 체력의 **보통 체급 적은 강한 처형 압박**을 받으며 대형/보스급은 처형 조건에서 제외된다.
- Dispel/Antimagic/logout/respawn/dimension/server stop에서 6써클 유지 상태를 정리하며 NPC suggestion/Eyebite fear/petrification의 기존 전투 타깃도 복구한다.

## alpha.59 — 7써클 deep pass

`SeventhCircleSpellService`가 7써클 10종을 6써클 다음의 전용 권한층으로 소유하고, player/NPC 모두 generic fallback보다 먼저 같은 주문 역할을 해석한다. 기존에 이미 의미가 강했던 Etherealness/Forcecage/Plane Shift/Simulacrum은 약화해서 재구현하지 않고 각각 `HighUtilitySpellService`, `HighControlSpellService`, `PlanarSpellService`, `SimulacrumService`로 위임한다.

- `delayed_blast_fireball`: release 순간의 고정 snapshot 지면을 유지하고, authored presentation의 지연 충격 시점에 실제 폭발이 일어난다. 거리 감쇠 피해·화상·넉백과 플레이어 시전의 실제 지형 파괴를 같은 중심/범위에 묶었다.
- `etherealness`: **preserved ethereal phase**. 기존 noPhysics·noGravity·투명화·자유비행과 일반 피해 88% 감쇠, 종료 후 이전 이동/충돌 상태 복원·안전 낙하 계약을 그대로 사용한다. NPC도 일반 피해 88% 감쇠와 위상 상태를 유지한 뒤 원래 상태를 복원한다.
- `finger_of_death`: release 때 고정한 단일 생명체만 사멸선 대상으로 사용한다. 큰 단일 피해·위더·쇠약과 약한 보통 체급 처형 압박을 주며, 대상이 사라졌다고 다른 적에게 재조준하지 않는다.
- `fire_storm`: 고정 중심 둘레 **6개 화염 기둥**과 실제 판정을 같은 육각 패턴에 배치한다. 중첩 다단히트 대신 가장 가까운 기둥 기준 피해를 적용하고 플레이어 시전은 각 낙하지점 지형도 파괴한다.
- `forcecage`: **preserved physical forcecage**. 기존 약 20초 공간 경계·반경 3.1m 계약을 유지한다. 대상은 감옥 밖으로 나갈 수 없지만 감옥 안에서 이동·공격·Arcane 시전까지 마비되지는 않는다. NPC 시전도 같은 공간 봉쇄 역할을 갖는다.
- `plane_shift`: 플레이어는 **preserved cross-dimension plane shift**. 시선 높이에 따라 End/Nether/Overworld 실제 차원을 전환하고, Nether↔Overworld 8배 좌표 관계와 안전 착지·웅크린 동행자 이동을 유지한다. NPC 전투 AI에는 대상을 때리는 generic damage 대신 **NPC planar disengage role**을 부여해 약 28m 안전 공간 이탈로 처리하며 플레이어의 차원 이동 의미를 퇴행시키지 않는다.
- `prismatic_spray`: 하나의 넓은 generic cone이 아니라 **seven independent prism rays**. 고정 발사 방향에서 7개의 좁은 광선이 각각 독립 선 판정을 갖고 화염·극저온·전격·위더·암흑·구속·생명 절단의 서로 다른 후유증을 부여한다. 한 대상에 여러 광선이 겹쳐 과증폭되는 것은 차단한다.
- `reverse_gravity`: **maintained reverse gravity**. 고정 지면에 약 8초간 실제 중력 역전장을 유지하고 새로 들어온 적도 계속 상승시킨다. 원래 noGravity 상태를 저장해 종료·Dispel·Antimagic·lifecycle clear 때 복원하고 Slow Falling을 부여해 해제 즉시 추락사하는 회귀를 막는다.
- `simulacrum`: **preserved commandable simulacrum**. 조준 생명체와 같은 실제 복제체 1체, 최대 체력 50%, 전투력 약 72%, FOLLOW/GUARD/ASSAULT와 웅크린 G키 명령 계약을 그대로 유지한다. NPC도 실제 복제 엔티티를 만들고 시전자 전투 목표를 지원한다.
- `teleport`: release 때 고정한 목적지에서 안전한 발판·2블록 공간을 검색하고 가장 가까운 유효 착지점으로 이동한다. 발동 후 시선을 돌려 목적지가 바뀌거나 막힌 블록/공중에 강제로 박히지 않는다.
- Dispel/Antimagic/logout/respawn/dimension/server stop에서 7써클 전용 상태를 정리하고, player Etherealness/Simulacrum 같은 별도 권한도 함께 해제한다.

## alpha.60 — 8써클 deep pass

`EighthCircleSpellService`가 8써클 10종을 7써클 다음의 전용 권한층으로 소유한다. 플레이어에서 이미 의미가 강한 Antimagic Field/Clone/Control Weather/Demiplane/Dominate Monster/Feeblemind/Maze는 재구현하지 않고 기존 전용 서비스에 위임하며, Earthquake/Incendiary Cloud/Sunburst는 generic 범위 피해에서 실제 유지형·전용 판정으로 승격했다. NPC 역시 8써클 전용 경로를 타며 역할상 불가능한 플레이어 전용 의미는 거짓 parity 대신 명시된 전투 역할로 처리한다.

- `antimagic_field`: **preserved antimagic field**. 플레이어의 16초 실제 반마법장과 범위 내 Arcane 시전·유지 상태 지속 소거를 그대로 사용한다. NPC도 이동하는 반마법장을 유지해 안에 들어온 플레이어/NPC의 Arcane 시전과 유지 상태를 계속 끊는다.
- `clone`: **preserved living clone**. 플레이어의 실제 대상 생명체 복제본·장비/전투 능력 복제 계약을 그대로 사용한다. NPC도 실제 Mob 복제체를 생성해 시전자의 현재 전투 목표를 지원하며 generic 버프나 일회성 부활 효과로 대체하지 않는다.
- `control_weather`: 기존 45초 실제 폭우·뇌우와 G키 12연속 낙뢰 권한을 보존한다. NPC도 45초 동안 실제 뇌우를 만들고 주변 적대 대상에 주기적인 낙뢰를 호출한다.
- `demiplane`: 플레이어의 **persistent demiplane**을 보존해 개인 주머니 공간과 귀환점·동행자·방 내부 블록/물품이 유지된다. NPC는 플레이어용 영속 방을 가짜로 만들지 않고 8초간 전장에서 빠져나오는 **NPC pocket-sanctuary role**을 사용한 뒤 기존 전투 상태를 복구한다.
- `dominate_monster`: 기존 강력한 몬스터 전투 지배를 그대로 사용한다. NPC가 Mob을 지배하면 대상의 전투 목표를 시전자 편으로 재편하고, 플레이어를 대상으로 할 때는 가짜 진영 변경 대신 Arcane 시전 억제·전투 약화를 거는 casting/combat compulsion 역할을 수행한다.
- `earthquake`: **maintained earthquake**. release 순간 고정된 지점을 중심으로 약 9초간 반복 지진을 유지해 넓은 범위 적에게 주기 피해·수직/외곽 충격을 준다. 플레이어 시전은 최초 충격과 같은 중심/반경의 실제 지형 파괴까지 연결한다.
- `feeblemind`: 기존 약 35초 주문 회로 붕괴 의미를 보존한다. 대상은 이동 자체가 멈추지는 않지만 극심한 전투 약화와 Arcane 시전 봉쇄를 받으며 NPC 경로도 같은 지속 역할을 갖는다.
- `incendiary_cloud`: **drifting incendiary cloud**. release 순간 진행 방향을 고정한 약 12초 소이 구름이 천천히 이동하며 내부 적에게 반복 피해와 화상을 준다. 단발 generic 폭발로 끝나지 않는다.
- `maze`: 플레이어의 **preserved maze exile**을 그대로 유지해 대상 Mob을 약 18초 실제 전투에서 추방했다가 복귀시킨다. NPC 시전은 차원 이동을 거짓 구현하지 않고 대상의 충돌·공격·이동·Arcane 상호작용을 끊는 **NPC combat-exile role**로 18초간 전장에서 격리한 뒤 원래 상태를 복구한다.
- `sunburst`: **dedicated sunburst**. 고정된 중심의 대형 태양광 폭발이 넓은 범위에 거리 감쇠 생명 피해를 주고 화상·장기 실명·발광을 남긴다.
- Dispel/Antimagic/logout/respawn/dimension/server stop에서 8써클 전용 유지 상태를 정리하고, 유지형 8C의 월드 연출 시간도 서버 권한 지속시간과 맞춘다.

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
| 3 | `dispel_magic` | PASS | PASS | alpha.60 PASS · custom-state dispel through 8C |
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
| 6 | `disintegrate` | PASS | PASS | alpha.58 PASS · material-breaking narrow ray |
| 6 | `globe_of_invulnerability` | PASS | PASS | alpha.58 PASS · player/NPC 1~5C boundary denial |
| 6 | `mass_suggestion` | PASS | PASS | alpha.58 PASS · behavioral multi-retreat |
| 6 | `move_earth` | PASS | PASS | alpha.58 PASS · physical terrain upheaval |
| 6 | `sunbeam` | PASS | PASS | alpha.58 PASS · piercing radiant line |
| 6 | `true_seeing` | PASS | PASS | alpha.58 PASS · persistent invisibility reveal |
| 6 | `freezing_sphere` | PASS | PASS | alpha.58 PASS · fixed cryogenic blast |
| 6 | `eyebite` | PASS | PASS | alpha.58 PASS · maintained fear + weakness |
| 6 | `flesh_to_stone` | PASS | PASS | alpha.58 PASS · casting-block petrification |
| 6 | `circle_of_death` | PASS | PASS | alpha.58 PASS · weak ordinary execution pressure |
| 7 | `delayed_blast_fireball` | PASS | PASS | alpha.59 PASS · locked delayed detonation |
| 7 | `etherealness` | PASS | PASS | alpha.59 PASS · preserved ethereal phase |
| 7 | `finger_of_death` | PASS | PASS | alpha.59 PASS · locked death ray |
| 7 | `fire_storm` | PASS | PASS | alpha.59 PASS · six-pillar fire storm |
| 7 | `forcecage` | PASS | PASS | alpha.59 PASS · preserved physical forcecage |
| 7 | `plane_shift` | PASS | PASS | alpha.59 PASS · preserved cross-dimension plane shift |
| 7 | `prismatic_spray` | PASS | PASS | alpha.59 PASS · seven independent prism rays |
| 7 | `reverse_gravity` | PASS | PASS | alpha.59 PASS · maintained reverse gravity |
| 7 | `simulacrum` | PASS | PASS | alpha.59 PASS · preserved commandable simulacrum |
| 7 | `teleport` | PASS | PASS | alpha.59 PASS · locked safe teleport |
| 8 | `antimagic_field` | PASS | PASS | alpha.60 PASS · preserved antimagic field |
| 8 | `clone` | PASS | PASS | alpha.60 PASS · preserved living clone |
| 8 | `control_weather` | PASS | PASS | alpha.60 PASS · preserved real weather authority |
| 8 | `demiplane` | PASS | PASS | alpha.60 PASS · persistent demiplane / NPC pocket-sanctuary role |
| 8 | `dominate_monster` | PASS | PASS | alpha.60 PASS · preserved combat domination |
| 8 | `earthquake` | PASS | PASS | alpha.60 PASS · maintained earthquake + terrain |
| 8 | `feeblemind` | PASS | PASS | alpha.60 PASS · preserved spellbreaking feeblemind |
| 8 | `incendiary_cloud` | PASS | PASS | alpha.60 PASS · drifting incendiary cloud |
| 8 | `maze` | PASS | PASS | alpha.60 PASS · preserved maze exile / NPC combat-exile role |
| 8 | `sunburst` | PASS | PASS | alpha.60 PASS · dedicated sunburst |
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

`tools/test_current_source.py`는 direct 90 + fusion 19 = 109, 효과 요약 ID 일치, 1~8써클 전용 권한 순서와 NPC parity, player/NPC Globe 1~5써클 경계 차단, Mass Suggestion behavioral retreat, material Disintegrate, physical Move Earth, persistent True Seeing, maintained Eyebite fear, casting-block Petrification, weak-ordinary Circle of Death, alpha.59의 locked delayed detonation / preserved ethereal phase / six-pillar fire storm / preserved physical forcecage / preserved cross-dimension plane shift / seven independent prism rays / maintained reverse gravity / preserved commandable simulacrum / locked safe teleport / NPC planar disengage role, alpha.60의 preserved antimagic field / preserved living clone / real Control Weather / persistent demiplane / preserved Dominate Monster / maintained earthquake / preserved Feeblemind / drifting incendiary cloud / preserved maze exile / dedicated sunburst / NPC pocket-sanctuary role / NPC combat-exile role, Dispel/Antimagic/lifecycle cleanup, 그리고 alpha.49~59의 기존 계약 회귀를 실패 조건으로 둔다.
