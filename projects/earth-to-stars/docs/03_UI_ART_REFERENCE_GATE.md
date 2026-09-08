# EARTH TO STARS — UI / Art / Audio Reference Gate

이 문서는 EARTH TO STARS의 최종 UI, 함선, 모듈, 무기, 천체, VFX, 사운드가 **AI 즉흥 SF 디자인**으로 굳어지는 것을 막는 제작 게이트다.

공용 `QUALITY_STANDARD.md`의 UI/UX·모델·자산 규칙을 이 프로젝트에 구체화한다.

---

# 1. 기본 원칙

최종 비주얼은 다음 순서 없이 구현하지 않는다.

```text
기능/정보 목적 정의
→ 실제 게임/모드/허용 자산 조사
→ 여러 사례의 해결 방식 분해
→ 사용할 디자인 언어 결정
→ asset provenance 확인
→ mockup / model study
→ Minecraft 구현
→ 실제 화면 캡처
→ reference와 비교
→ 수정
```

`컴파일됨`, `화면이 뜸`, `모델이 보임`은 visual completion이 아니다.

---

# 2. 금지 기본값

다음은 최종 디자인의 출발점으로 사용하지 않는다.

- 검은 반투명 사각형 위에 cyan border
- 모든 것을 네온으로 발광
- 기능 의미 없는 hexagon 장식
- 카드 UI 남발
- 화면마다 다른 padding/폰트 크기
- gradient/glow로만 SF 느낌을 표현
- 임의의 홀로그램 선을 배경에 채움
- vanilla button을 늘려놓고 SF라고 부름
- 같은 함선에 서로 다른 low-poly/voxel/realistic 자산을 무정리 혼합
- vanilla cube + particle만으로 대표 무기/엔진을 최종 처리

DEBUG_ONLY UI는 허용하되 production으로 승격하지 않는다.

---

# 3. Reference 역할 분리

한 작품을 통째로 복제하지 않는다.

예:

- Reference A — cockpit 정보 계층
- Reference B — radar/contact 표현
- Reference C — module management interaction
- Reference D — damage feedback
- Reference E — industrial shape language
- Reference F — sound language

그 후 프로젝트 공통 design system으로 통합한다.

---

# 4. 초기 Reference Pool

이 목록은 **연구 출발점**이며 사용 허가를 뜻하지 않는다. 자산 직접 사용 여부는 `THIRD_PARTY_ASSETS.md`가 결정한다.

## Space Engineers

연구 대상:
- modular ship readability
- weapon hardpoint placement
- cockpit information
- manual/automatic turret role
- functional industrial visual language

사용 범위:
- design reference only
- proprietary code/assets 복제 금지

## Create Cosmonautics

연구 대상:
- Minecraft 안에서 rocket/spacecraft를 읽히게 만드는 방법
- flight HUD
- orbit/travel presentation
- re-entry/space effects
- modular propulsion concept

현재 26.2 runtime dependency로 자동 채택하지 않는다.

## VS Genesis

연구 대상:
- surface/space transition
- celestial representation
- space scale compression
- ship/dimension transfer idea

## Robotica

연구 대상:
- Minecraft 26.2에서 robot/mecha/SF weapon presentation
- current-version implementation patterns

## Kenney SF Assets

연구/베이스 후보:
- Space Station Kit
- Space Kit
- Modular Space Kit
- UI Pack — Sci-Fi
- Sci-Fi Sounds / Interface Sounds

CC0 계열 자산은 라이선스 확인 후 수정 가능한 베이스로 활용할 수 있다. 그러나 그대로 섞어 넣지 않고 프로젝트 스타일과 Minecraft 스케일에 맞춰 재가공한다.

---

# 5. Ship Visual Language Gate

함선은 stage가 올라갈수록 단순히 크기만 커지지 않는다.

## 읽혀야 하는 기능

멀리서도 가능하면 다음이 구분되어야 한다.

- forward direction
- propulsion
- cockpit/bridge
- cargo/industrial mass
- weapon hardpoints
- docking/airlock

## 성장 표현

### Launch Craft
- 가벼움
- exposed utility
- 작은 추진계
- 생존에 필요한 최소 구조

### Expedition Ship
- 장거리 설비가 보임
- cargo/utility 확장
- sensor/weapon hardpoint 명확

### Mobile Base / Capital
- 대형 전력/추진 구조
- 격납/산업 기능
- 방어구조

단, “큰 사각 박스에 장식만 추가”하는 발전은 금지한다.

---

# 6. Module Visual Rules

모듈은 아이콘을 안 봐도 역할을 어느 정도 추론할 수 있어야 한다.

예:

- engine — nozzle/thrust direction
- reactor — protected core / energy routing visual
- battery — repeated cell/storage language
- cargo — access/volume
- sensor — antenna/radar/optical array
- turret — rotation base + barrel/emitter
- refinery — intake/process/output visual logic

보이는 작동부가 있다면 애니메이션으로 기능을 강화한다.

---

# 7. Weapon Presentation

무기군은 사운드/VFX/모션으로 구분되어야 한다.

## Autocannon
- 빠른 mechanical cycle
- muzzle impulse
- 짧고 명확한 hit feedback

## Coilgun / Heavy Cannon
- charge or mechanical preparation
- 큰 recoil/weight
- 강한 impact

## Laser
- energy buildup
- heat feedback
- beam visibility는 판정과 정합

## Missile
- launch impulse
- tracking feedback
- point-defense counterplay를 읽을 수 있음

무기 외형의 크기와 실제 판정/발사 위치가 최대한 일치해야 한다.

---

# 8. Cockpit / Flight HUD

HUD의 우선순위는 현재 상황에 따라 달라진다.

## Normal Flight
1. movement/orientation
2. destination/navigation
3. fuel/propellant critical state
4. critical power/life-support warning

## Combat
1. target/contact
2. aim/weapon state
3. immediate threats
4. ammo/heat
5. ship critical damage

## Landing / Re-entry
1. altitude/approach
2. vertical/relative speed
3. thermal/safety envelope
4. landing state

항상 모든 정보를 보여주지 않는다.

---

# 9. Ship Management UI

화면 목적은 “표를 많이 보여주기”가 아니다.

핵심 행동:

- module 확인
- install/remove/replace
- damaged system 확인
- power shortage 이해
- weapon control mode 변경
- cargo 확인

정보 계층:

```text
Ship overview
→ critical state
→ selected subsystem
→ detailed values
```

모든 수치를 첫 화면에 노출하지 않는다.

---

# 10. Radar / Sensor UI

센서 화면은 3D 공간의 정보를 빠르게 이해시키는 것이 목적이다.

필수 구분:

- hostile
- neutral/unknown
- mission/navigation
- salvage/resource
- missile/immediate threat

색만으로 구분하지 않는다.

shape/icon/text/behavior 중 하나 이상을 병행해 accessibility를 확보한다.

---

# 11. Alerts

중요 알림은 우선순위를 가진다.

## Critical
- hull failure
- life support
- reactor critical
- collision/immediate missile

## Warning
- low fuel
- overheating
- damaged system

## Information
- new contact
- scan complete
- docking ready

경고음이 계속 울려 플레이어가 무시하게 만드는 alarm fatigue를 피한다.

---

# 12. Planet / Space Art Gate

새 천체를 production으로 추가할 때 다음 reference board를 만든다.

- terrain
- sky
- lighting
- horizon
- structures
- color/material palette
- hazards
- ambient sound

“Moon=회색”, “Mars=빨강” 정도로 끝내지 않는다.

Minecraft에서 실제로 읽힐 수 있는 실루엣과 재질 차이를 우선한다.

---

# 13. Earth Orbit Visual Requirements

첫 우주 진입의 품질 예산은 높게 잡는다.

최소 고려:

- Earth가 방향 기준점으로 보임
- stars/black sky transition
- atmospheric limb/horizon
- sun/light transition
- debris/contact readability
- exterior craft lighting
- cockpit/interior audio contrast

우주가 단순 `void + 별 texture`처럼 보여서는 안 된다.

---

# 14. Interior Art

함선 내부는 generic corridor를 무한 반복하지 않는다.

방의 역할이 외형에 드러나야 한다.

- bridge
- engineering
- cargo
- fabrication
- airlock
- hangar

동선은 Minecraft 플레이어 크기와 카메라/FOV를 기준으로 실제 테스트한다.

장식 때문에 이동이 걸리거나 멀티 플레이어가 서로 막히지 않게 한다.

---

# 15. Texture / Material Cohesion

외부 자산을 여러 소스에서 가져오더라도 최종 결과에서는:

- texel density
- roughness/metal impression
- edge treatment
- emissive language
- palette
- scale

를 통일한다.

원본 asset pack의 그림체 차이를 그대로 노출하지 않는다.

---

# 16. Animation

움직여야 기능이 읽히는 대상에는 animation을 검토한다.

우선순위:

1. turret aim/fire/recoil
2. engine/thruster state
3. landing/docking mechanism
4. industrial machinery
5. robot/drone
6. doors/airlocks

모든 장식에 animation을 넣어 성능을 낭비하지 않는다.

---

# 17. VFX

VFX는 정보다.

최소 정합:

```text
charge/telegraph
→ actual event
→ impact/result
```

대표 효과:

- engine plume
- RCS thruster
- atmospheric heating
- shield/armor impact if applicable
- weapon hit family
- decompression
- mining beam/resource breakup

과도한 particle count로 강함을 표현하지 않는다.

---

# 18. Audio Direction

초기 reference 조사 대상:

- spacecraft machinery
- industrial servo
- radar/contact
- mechanical weapon
- energy weapon
- hull stress/impact
- emergency alarm
- airlock/docking

사운드는 자산 라이선스를 별도 확인한다.

CC0 자산을 사용해도:

- volume
- EQ
- layering
- pitch variation
- distance behavior

를 프로젝트에 맞게 가공한다.

---

# 19. Asset Provenance Gate

외부 자산마다 최소 기록:

```text
Asset / Source
Author
URL
License
Allowed use
Modification
Imported file paths
Verification date
```

라이선스 불명확하면 production에 넣지 않는다.

개인 테스트라는 이유로 공개 저장소에 불법/불명확 자산을 커밋하지 않는다.

---

# 20. Mockup Gate

핵심 UI는 구현 전에 최소 다음 상태를 검토한다.

- default
- hover/focus
- selected
- disabled
- critical warning
- empty/no target
- long text/value stress
- 16:9 common resolution
- smaller GUI scale / larger GUI scale

필요 시 Figma/Penpot 또는 실제 texture mockup을 사용한다.

---

# 21. Production Visual Acceptance

대표 화면/자산은 최소 다음을 통과해야 한다.

- 1초 안에 핵심 정보 파악 가능
- 클릭/조작 가능 상태 명확
- Minecraft 화면과 이질감 과도하지 않음
- 외부 reference를 왜 사용했는지 설명 가능
- license/provenance 정리
- actual client screenshot 검수
- 임시 cube/debug icon/debug text 제거
- multiplayer 화면에서 다른 플레이어/target과 겹침 확인
- 성능 문제 없는지 확인

이 조건을 통과하기 전 `FINAL VISUAL`로 표시하지 않는다.
