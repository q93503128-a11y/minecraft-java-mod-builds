# 18B — IRON GOLEM VANGUARD / BREAKER PRESENTATION GATE

최종 갱신: 2026-09-14  
상태: **REFERENCE GATE PASS / CODE IMPLEMENTED / AUTOMATED GATE PENDING / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. 대표 roster 확장에서 Iron Golem의 외형/행동/VFX를 즉흥 설계하지 않기 위한 좁은 보조 gate다.

## 1. Canonical gameplay identity

현재 production data의 Iron Golem은:

- character: `turnbound_re:iron_golem`
- gameplay source: `minecraft:iron_golem`
- origin: ★5
- roles: `VANGUARD / BREAKER`
- 느린 SPD 12 / 높은 HP 240 / DEF 52 / POISE 150
- `iron_golem_iron_fist`: enemy single MELEE, HP + Poise pressure
- `iron_golem_guardian_plate`: ally single Ward
- `iron_golem_ground_slam`: enemy multi MELEE, high Poise + Intent Delay
- `iron_golem_village_judgment`: enemy single Burst, very high HP/Poise pressure

따라서 Iron Golem은 단순히 크기만 키운 vanilla mob이나 같은 팔 동작을 수치만 바꿔 재사용하는 캐릭터가 아니다. **수호 / 단일 압박 / 광역 붕괴 / Burst 처형**이 실루엣과 타이밍으로 구분되어야 한다.

## 2. External reference screening

### GOLEM-R01 — Mojang official Iron Golem identity

- source: https://www.minecraft.net/en-us/article/iron-golem
- source: https://www.minecraft.net/en-us/article/meet-iron-golem
- classification: **REFERENCE ONLY / proprietary first-party**.
- 관찰:
  - 마을 주민을 지키는 bodyguard가 핵심 fantasy다.
  - 위협을 발견하면 매우 강한 팔 공격으로 적을 공중에 띄우는, 느리지만 한 번의 충격이 큰 전투 정체성을 가진다.
  - 손상은 몸의 crack으로 드러나고, iron ingot로 수리할 수 있다.
- TURNBOUND 채택:
  - 넓은 상체와 큰 전완을 실루엣 중심으로 둔다.
  - 일반 타격도 가벼운 주먹 연타가 아니라 한 팔에 질량이 실리는 동작으로 만든다.
  - `Guardian Plate`는 공격 동작을 돌려쓰지 않고 실제로 아군 앞 공간을 막는 자세를 사용한다.
- 금지:
  - Mojang 고유 모델 geometry를 그대로 복제해 새 모델이라고 주장하지 않는다.
  - 원작 crack/repair mechanic을 새 gameplay 상태로 임의 추가하지 않는다.

### GOLEM-R02 — Fresh Animations / Iron Golem

- source: https://modrinth.com/resourcepack/fresh-animations
- relevant changelog: https://modrinth.com/resourcepack/fresh-animations/version/1.0.0
- classification: **REFERENCE ONLY / custom terms**.
- 관찰:
  - vanilla identity를 유지하면서 breathing, walk, body movement, facial/health readability를 동작으로 보강한다.
  - Iron Golem은 초기 CEM 지원 대상이며 손상 단계의 facial/health indicator도 강화한다.
- TURNBOUND 채택:
  - idle을 완전 정지시키지 않되 빠른 bounce를 금지하고 느린 weight transfer를 사용한다.
  - 큰 팔이 몸과 별개로 미세하게 늦게 움직여 무게를 느끼게 한다.
- 사용 상태: 원본 model/animation asset은 repository에 포함하지 않는다.

### GOLEM-R03 — Minecraft Dungeons golem mass language

- visual reference: Minecraft Dungeons Redstone Golem / golem-class encounters.
- first-party game overview: https://www.minecraft.net/en-us/about-dungeons
- supporting visual page: https://play.nintendo.com/activities/opinion-polls/minecraft-dungeons-mob-attack-poll/
- classification: **REFERENCE ONLY / proprietary**.
- 관찰:
  - 큰 흉곽/어깨/팔 덩어리와 작은 머리 비율이 멀리서도 heavy construct로 읽힌다.
  - 공격보다 먼저 질량과 위협 범위가 silhouette로 전달된다.
- TURNBOUND 채택:
  - stock Iron Golem보다 어깨/상체 mass를 조금 더 강하게 하고 forearm을 분절해 작은 battle-stage에서도 역할을 읽게 한다.
  - Redstone Golem의 고유 색, redstone vein, 얼굴, 모델은 복제하지 않는다.

## 3. Selected visual language

### Silhouette / idle

- 작은 머리 대비 넓은 어깨/흉곽.
- 상완보다 더 무거워 보이는 분절 전완.
- 다리는 넓게 버티고, idle은 빠른 흔들림 대신 느린 좌우 weight transfer.
- vanilla Iron Golem texture는 runtime reference로 유지하되 renderer/model geometry는 TURNBOUND 전용으로 사용한다.

### Iron Fist — OFFENSIVE

- exact action id에서만 사용.
- WINDUP / IMPACT 동안 한 팔을 크게 뒤로 싣고 반대 팔이 몸을 지탱한다.
- stage angle을 살짝 틀어 큰 forearm이 몸통에 묻히지 않게 한다.
- attack tag가 MELEE라는 이유만으로 다른 skill이 이 pose를 공유하지 않는다.

### Guardian Plate — DEFENSIVE

- ally-target exact action id에서만 사용.
- 양 전완을 아군 앞 공간을 닫듯 전방으로 모은다.
- aggressive flag를 사용하지 않는다.
- IMPACT에는 대상에 짧은 SUCCESS semantic accent와 Iron Golem repair 계열 sound를 사용한다.
- 공격 sweep sound나 공격 pose를 재사용하지 않는다.

### Ground Slam — SLAM

- exact action id + multi enemy target에서만 사용.
- 양팔을 같은 방향으로 크게 올리고 상체/무릎을 압축해 **아래로 내리치는 공격**임을 실루엣으로 읽게 한다.
- 기존 authoritative `SLAM` presentation style의 target impact accent를 재사용한다.
- 별도 damage/Intent Delay event를 만들지 않는다.

### Village Judgment — EXECUTE

- exact Burst action id에서만 사용.
- Ground Slam과 겹치지 않도록 더 세로로 길고 비대칭인 overhead execution silhouette를 사용한다.
- 기존 `HEAVY` impact accent와 authoritative target을 그대로 따른다.

### Recovery / unknown action

- RECOVERY는 neutral.
- 다른 participant가 행동 중이면 neutral.
- unknown MELEE action은 neutral.
- source entity가 `minecraft:iron_golem`과 일치하지 않으면 custom override를 적용하지 않는다.

## 4. Implemented boundary

이번 pass:

- `turnbound_re:iron_golem_visual` presentation-only entity.
- Character Detail / virtual Battle Stage shared identity.
- custom TURNBOUND Iron Golem model layer.
- Minecraft Iron Golem texture runtime reference.
- slow heavy idle + OFFENSIVE / DEFENSIVE / SLAM / EXECUTE model pose families.
- exact action/actor/target/recovery selection contract.
- Guardian Plate target에 짧은 support impact accent + repair 계열 sound.
- offensive Iron Golem actions에 Iron Golem attack 계열 sound.
- pure source/action regression tests.

이번 pass에서 하지 않음:

- gameplay `minecraft:iron_golem` 교체.
- damage / Ward / Intent Delay / target logic 변경.
- crack/repair를 새 전투 mechanic으로 추가.
- external binary model/texture import.
- screen shake/particle spam을 캐릭터 정체성으로 대체.
- screenshot 없이 production visual PASS 선언.

## 5. Screenshot gate

통합 playtest 때 확인:

1. Character Detail idle만 봐도 Zombie보다 훨씬 무거운 ★5 Vanguard silhouette가 읽히는가.
2. 넓은 어깨와 forearm이 480×270에서 뭉쳐 한 사각형으로 보이지 않는가.
3. `Iron Fist`와 `Ground Slam`이 같은 공격 pose처럼 보이지 않는가.
4. `Guardian Plate`가 공격으로 오해되지 않고 아군 보호로 읽히는가.
5. `Village Judgment`가 Burst임을 과도한 glow 없이 pose/impact timing으로 전달하는가.
6. Guardian Plate SUCCESS accent가 HP/Intent/target marker를 가리지 않는가.
7. visual target과 server-authored target이 일치하는가.

실제 screenshot 확인 전 상태는 **PLAYTESTED = NO / PRODUCTION VISUAL PASS = NO**다.
