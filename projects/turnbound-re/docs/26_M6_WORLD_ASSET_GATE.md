# 26 — M6 WORLD ASSET GATE — HISTORICAL HARNESS / REFERENCE

> **정본 변경:** production world source는 이제 `CANON.md` C-008 및 `30_EXTERNAL_WORLD_BASE.md`가 우선한다. 현재 production base는 공식 배포본을 별도 설치하는 **Drehmal: APOTHEOSIS v2.2.2f**다. 이 문서의 Wayfarer Forge Court / Riverward Foothill 및 block-by-block prototype은 mechanics/layout 비교용 historical harness이며 production visual/world source가 아니다.

이 문서는 `HUB_01 -> REGION_01` 기능 slice에서 과거 검증했던 외부 reference / palette / scale / readability gate를 보존한다.

실제 production에서는 아래의 custom authored prototype을 확장하지 않는다. 월드 외형/지형/건축은 외부 authored world 자체를 사용하고 TURNBOUND는 gameplay anchor/meaning만 얹는다.

---

## 1. 보존 목적

과거 첫 월드 slice는 다음 루프를 한 공간에서 검증하기 위해 사용했다.

`Hub 준비 -> 실제 채광/농사/낚시 -> 장비/준비물 선택 -> 보이는 Encounter -> 턴제 전투 -> 보상 -> Hub 귀환`

이 루프 계약 자체는 여전히 유효하다. 다만 공간의 외형과 건축은 이 문서의 자작 prototype이 아니라 외부 production world에 매핑한다.

검증 대상:
- Hub에서 준비 기능이 읽히는가.
- 주동선을 잃지 않는가.
- 채광/농사/낚시가 선택 가능한 분기로 연결되는가.
- 일반 Encounter와 강한 Encounter가 공간적으로 구분되는가.
- UI 메뉴를 순회하지 않아도 실제 월드 이동으로 시스템 관계를 학습하는가.

---

## 2. Historical reference set

### W-001 — Minecraft Legends: Well of Fate / village discovery
사용 분류: **reference only / proprietary**.

과거 채택 원리:
- Hub에 하나의 강한 기능 landmark.
- fast travel을 발견 가능한 월드 landmark와 연결.
- resource site는 주동선에서 존재를 짐작할 수 있는 짧은 분기.

금지:
- Well of Fate 실루엣/배치/색/기하 복제.
- Minecraft Legends 고유 asset 사용.

### W-002 — Minecraft Dungeons Camp / Blacksmith
사용 분류: **reference only / proprietary**.

과거 채택 원리:
- 월드에서 얻은 기능이 실제 camp/station으로 자리 잡는다.
- gear upgrade는 물리적 귀환 지점과 연결한다.

금지:
- Dungeons camp geometry/merchant 배치/장식/asset 복제.

### W-003 — MineColonies Medieval Oak / Medieval Spruce
- gallery: https://minecolonies.com/schematics/
- repository: https://github.com/ldtteam/minecolonies-schematics
- 검토 당시 repository license: GPL-3.0.

과거에는 material-family 일관성의 reference로만 사용했다.

현재 production policy에서는 이 gallery를 보고 TURNBOUND가 비슷한 건축을 새로 만드는 방식도 사용하지 않는다. 실제 asset을 채택하려면 별도의 직접 사용/라이선스/호환 검증을 거친다.

### Screening exclusion — Terralith
당시 repository 사용조건 검토에서 AI/generative system 관련 제한을 확인하여 reference/asset/code source에서 제외했다. 이 제외 상태는 유지한다.

---

## 3. Legacy custom prototype

과거 이름:
- `HUB_01 — Wayfarer Forge Court`
- `REGION_01 — Riverward Foothill`

과거 구현:
- `ProductionWorldSlicePlan`
- `ProductionWorldSlicePrototypeBuilder`
- `AuthoredFirstRegionBuilder`
- `/turnbound_re_world_slice build`
- `/turnbound_re_world_slice prototype`

현재 판정:
- mechanics/layout comparison harness: **유지 가능**
- production map/world visual source: **금지**
- fresh-world 자동 production 생성: **금지**

이 코드의 stone/tuff/spruce forge hall, 직접 만든 road/quarry/farm/river/patrol/rift landmark를 production 월드로 확장하지 않는다.

---

## 4. Production world replacement

현재 정본은 `30_EXTERNAL_WORLD_BASE.md`다.

- external base: **Drehmal: APOTHEOSIS v2.2.2f**
- 원본 map/resource pack은 TURNBOUND repository에 재배포하지 않는다.
- 공식 배포본을 별도로 설치한다.
- TURNBOUND는 원본 terrain/town/building을 유지하고 server-authoritative Encounter / fast travel / progression anchor만 추가한다.
- 초기 integration seed:
  - HUB_01: New Drabyel `(502, 67, 1801)`
  - REGION_01 gateway: Stasis Facility `(778, 31, 668)`

본편 공개판의 target-version migration은 아직 실검증 전이므로 production visual PASS나 playtest PASS로 취급하지 않는다.

---

## 5. 아직 필요한 실제 gate

- [ ] 공식 external world test copy를 Java 26.2 + NeoForge 환경에서 실제 load/migration.
- [ ] New Drabyel / Stasis Facility가 migration 후 시각적으로 보존되는지 확인.
- [ ] 실제 외부 지형에서 첫 patrol / elite Encounter landmark 선정.
- [ ] 외부 geography 위에 mining/farming/fishing loop 연결.
- [ ] 외부 datapack/resource-pack과 TURNBOUND server authority 충돌 검사.
- [ ] Minecraft screenshot side-by-side audit.
- [ ] GUI + external world 동시 가독성 확인.
- [ ] 이동 시간 / 채집 기대값 / 전투 접근 시간 측정.
- [ ] camera collision/readability 확인.
- [ ] multiplayer 검증.

현재 상태는 **HISTORICAL HARNESS PRESERVED / EXTERNAL PRODUCTION WORLD SELECTED / MIGRATION & PLAYTEST PENDING**이다.
