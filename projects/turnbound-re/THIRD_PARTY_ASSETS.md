# THIRD PARTY / REFERENCE REGISTER

외부 코드·맵·모델·텍스처·UI 키트·사운드·폰트를 실제 프로젝트에 넣기 전 반드시 이 문서를 갱신한다.

| ID | 종류 | 출처 | 라이선스 / 사용조건 | 현재 사용 | 허용 범위 |
|---|---|---|---|---|---|
| EXT-CODE-001 | turn-based Minecraft integration | Stephen-Seo/TurnBasedMinecraftMod, `neoforge`, commit `4d685cb187f91b2573a469d09fc47df270b90a4e`, `common/AttackEventHandler.java` — https://github.com/Stephen-Seo/TurnBasedMinecraftMod | MIT | **일부 패턴 직접 adaptation 사용 중** | `BattleWorldEventHooks`의 source/target 양방향 vanilla damage interception 및 player attack 선제 차단 경계에 적용. 외부 battle rule/RNG/UI/config는 복사하지 않음. 라이선스는 `third_party/licenses/Stephen-Seo_TurnBasedMinecraftMod_MIT.txt` 보존 |
| EXT-CODE-002 | battle camera smoothing | Cukkoo12/free-camera, `master`, commit `9dc299c70e19cfbd2973a469d09fc47df270b90a4e`, NeoForge 26.2 `CinematicRotationSmoother.java` + `CinematicMotionProfile.java` — https://github.com/Cukkoo12/free-camera | MIT | **adapted source 사용 중** | upstream critically-damped exponential yaw/pitch integration + CINEMATIC rotation frequency `7.0` 사용. 라이선스는 `third_party/licenses/Cukkoo12_free-camera_MIT.txt` 보존 |
| EXT-WORLD-001 | production external authored world | Drehmal Team, `Drehmal: APOTHEOSIS v2.2.2f` — https://www.drehmal.net/downloads / https://github.com/Drehmal-Team/map/releases/tag/v2.2.2f | 공식 무료 다운로드/싱글·멀티·서버 설치 안내 확인. TURNBOUND 저장소 재배포 허가는 확인되지 않았으므로 **원본 world/resource-pack vendoring/재배포 금지**. 사용자가 공식 배포본을 별도 설치 | **production base 채택 / 외부 설치 방식** | 원본 terrain/town/building 직접 사용. `external_world_profiles.json`이 TURNBOUND semantic anchor 좌표/상태를 data-driven으로 보유하고 `DrehmalExternalWorldBinding`은 enabled anchor만 server metadata/Interaction entity로 연결. 현재 New Drabyel + Stasis fast-travel만 enabled. 광산/농장/낚시/일반전/엘리트 후보는 26.2 migration 전까지 disabled |
| REF-UI-001 | UI production skin | Kenney, `UI Pack - Pixel Adventure` 2.0 — https://kenney.nl/assets/ui-pack-pixel-adventure | CC0 1.0 | **사용 중** | Large tiles / Thin outline 계열을 title + semantic frame으로 사용. GUI 확장은 9-slice metadata 사용 |
| REF-UI-002 | UI 자산 후보 / 비교 | tiopalada, `Tiny RPG - Dragon Regalia GUI` — https://tiopalada.itch.io/tiny-rpg-dragon-regalia-gui | CC0 1.0 | 파일 반입 전 | state frame/9-slice/target cursor 보조 후보. 화풍 혼합 금지 |
| EXT-UI-003 | battle input glyph production family | Kenney, `Input Prompts Pixel` 1.0 — https://kenney.nl/assets/input-prompts-pixel | CC0 1.0 | **사용 중** | 원본 16×16 keyboard number glyph `tile_0051`~`tile_0056`을 수정 없이 각각 `input/key_1.png`~`key_6.png`로 rename하여 반입. Battle Command의 1~6 실제 screen-local shortcut과 동일 glyph를 표시. 원본 bytes 확인 경로는 public mirror `Tiddybub/2d-assets`의 `ui/input-prompts-pixel/`, mirror `SOURCE.md`가 공식 Kenney source/CC0를 명시. license는 `third_party/licenses/Kenney_Input_Prompts_Pixel_CC0.txt` 보존 |
| REF-MODEL-001 | humanoid articulation reference | SL0ANE/Loy-s-Goodies, `230507_alex.bbmodel`, commit `afbb7695b09de0ed8ee3aa97732ff7c3d367520c` | CC0 1.0 | **legacy reference only / production 미사용** | 과거 reference-recreated Zombie geometry는 external-only 규칙에서 폐기 |
| EXT-MODEL-002 | runtime production base | Mojang Minecraft entity model layers + entity textures, Java 26.2 runtime | Mojang first-party proprietary runtime content | **직접 사용 중** | Creeper/Spider/Blaze/Witch/Iron Golem/Zombie 대응 runtime model/texture 직접 사용. TURNBOUND는 replacement geometry/UV/texture를 새로 디자인하지 않음 |
| EXT-ITEM-003 | representative equipment visual base | Mojang Minecraft Java 26.2 runtime item models/textures: `minecraft:shield`, `minecraft:copper_sword`, `minecraft:golden_apple` | Mojang first-party proprietary runtime content | **production visual source 채택 / runtime 직접 참조** | `iron_bulwark`→Shield, `copper_edge`→Copper Sword, `golden_heart`→Golden Apple. TURNBOUND namespace로 PNG를 복사하지 않고 vanilla `ItemStack` renderer를 사용. vanilla item gameplay component/effect는 TURNBOUND 장비 수치에 상속하지 않음 |
| CAND-ITEM-004 | future custom equipment icon family | Kettoman, `Pixel Art Icons - RPG Essentials (16x16)` — https://kettoman.itch.io/pixel-art-icons-rpg-essentials-16x16 | CC0, author page에 no generative AI 표기 | **후보 / 미반입** | 64개 16×16 weapons/food/materials/potions. 현재 대표 3종은 Mojang runtime item으로 충분하므로 production에는 미사용 |
| CAND-ITEM-005 | future custom equipment icon family | Shade, `Free 16x16 Assorted RPG Icons` — https://merchant-shade.itch.io/16x16-mixed-rpg-icons | CC0 1.0 Universal, author page에 no generative AI 표기 | **후보 / 미반입** | weapons/armours/consumables/chests 등. 향후 vanilla runtime item으로 역할 표현이 부족할 때 동일 family에서 직접 sprite를 채택하는 후보 |
| EXT-ITEM-006 | reward currency visual base | Mojang Minecraft Java 26.2 runtime item models/textures: `minecraft:gold_nugget`, `minecraft:experience_bottle`, `minecraft:amethyst_shard` | Mojang first-party proprietary runtime content | **production reward visual source 채택 / runtime 직접 참조** | Battle Result의 `Coin`→Gold Nugget, `Essence`→Experience Bottle, `Character Shard`→Amethyst Shard. 보상 값/경제 규칙은 변경하지 않고 시각 identity만 제공. TURNBOUND 전용 통화 PNG를 새로 만들지 않음 |
| CAND-MODEL-003 | Creeper replacement candidate | Moth's Creeper Redone | MIT | 파일 반입 전 | 26.2 호환/의존성 검증 뒤 실제 asset 직접 사용 후보 |
| CAND-MODEL-004 | Spider replacement candidate | Scary Spider | MIT | 파일 반입 전 | 실제 asset 직접 사용 후보. 눈으로 보고 재구성 금지 |
| CAND-ANIM-001 | Spider animation/base candidate | Wall Climbers 1.2 | MIT + 프로젝트 사용조건 | 파일 반입 전 | 26.2 지원. 실제 파일/고지조건 고정 후 사용 가능 |
| REF-MODEL-005 | Spider reference only | Fresh Animations: Spiders | ARR / custom terms | reference only | 파일 미반입. 보고 비슷한 geometry를 수동 재구성하는 것도 금지 |

## 현재 production 선택

- **World:** `EXT-WORLD-001` Drehmal: APOTHEOSIS v2.2.2f. 공식 배포본을 별도 설치하며 TURNBOUND repo에는 원본 맵을 넣지 않는다.
- **Primary UI skin:** `REF-UI-001` Kenney UI Pack - Pixel Adventure.
- **Battle input glyphs:** `EXT-UI-003` Kenney Input Prompts Pixel 1.0. 현재 1~6 number-key glyph를 실제 1~6 screen-local input과 연결한다.
- **Vanilla-source roster base:** `EXT-MODEL-002` Mojang runtime model/texture.
- **Representative equipment visuals:** `EXT-ITEM-003` Mojang runtime item models. 현재 mapping은 Shield / Copper Sword / Golden Apple이며 별도 TURNBOUND 아이콘을 만들지 않는다.
- **Battle reward visuals:** `EXT-ITEM-006` Mojang runtime item models. Coin / Essence / Character Shard는 Gold Nugget / Experience Bottle / Amethyst Shard로 읽히며 별도 TURNBOUND 통화 아이콘을 만들지 않는다.
- **Turn-based Minecraft adapter:** `EXT-CODE-001`의 실제 MIT integration pattern adaptation.
- **Battle camera smoothing:** `EXT-CODE-002`의 실제 MIT 26.2 source adaptation.

## Input-prompt visual boundary

- `input/key_1.png`~`key_6.png`는 TURNBOUND가 그린 키 아이콘이 아니라 Kenney `Input Prompts Pixel` 원본 sprite bytes다.
- 실제 키 입력과 화면 glyph를 반드시 일치시킨다. 현재 action picker와 target-page picker에서 top-row `1`~`6`을 screen-local shortcut으로 처리한다.
- 숫자 glyph는 gameplay action identity를 대신하는 자체 스킬 아이콘이 아니다. 액션 의미는 서버 snapshot의 action name/slot/tooltip이 계속 정본이다.
- 새 키/게임패드 glyph가 필요하면 같은 Kenney family의 실제 원본 sprite를 추가하고 source tile과 local rename을 이 문서에 기록한다.
- glyph가 없다는 이유로 TURNBOUND 전용 키캡 PNG를 새로 그리지 않는다.

## Equipment visual boundary

- gameplay equipment id와 visual item id는 분리한다.
- 현재 대표 mapping은 `docs/31_M6_EQUIPMENT_VISUAL_ASSET_GATE.md`가 정본이다.
- runtime item model/texture는 Minecraft가 이미 보유한 asset을 직접 렌더링하며 TURNBOUND repo에 복사하지 않는다.
- `minecraft:golden_apple`을 Golden Heart의 visual identity로 사용해도 vanilla food/effect를 장비 시스템에 복사하지 않는다.
- 향후 custom equipment가 필요하면 `CAND-ITEM-004/005` 같은 실제 허용 asset family에서 직접 파일을 채택하고, sprite 위치/원본 버전/수정 내역을 먼저 이 문서에 고정한다.
- 외부 pack을 참고만 한 뒤 TURNBOUND 전용 16×16 sprite를 새로 그리는 방식은 금지한다.

## Reward visual boundary

- Battle Result의 보상 경제 값은 서버 snapshot이 정본이며 runtime item은 presentation-only다.
- `Coin`은 `minecraft:gold_nugget`, `Essence`는 `minecraft:experience_bottle`, `Character Shard`는 `minecraft:amethyst_shard`의 Mojang runtime model/texture를 직접 렌더링한다.
- 보상 icon을 위해 TURNBOUND namespace에 자체 coin/crystal/shard PNG를 만들지 않는다.
- vanilla item 자체를 지급하거나 해당 vanilla gameplay 효과를 보상 재화에 상속하지 않는다.
- 잘못되거나 존재하지 않는 runtime item id는 임시 자작 icon으로 대체하지 않고 시각 요소만 fail-closed한다.

## World external-base boundary

- external-world coordinates are data, not Java layout constants.
- profile id `turnbound_re:drehmal_apotheosis_2_2_2f`가 locator + coordinates + `enabled` + source note를 보유한다.
- registry가 anchor kind, locator cross-reference, dimension을 검증한다.
- `DrehmalExternalWorldBinding`은 enabled profile anchor만 연결하며 원본 terrain/building/resource-pack을 복사하거나 수정하지 않는다.
- New Drabyel / Stasis Facility 두 fast-travel seed만 현재 enabled다.
- Drabyel farmhouse, Primal Caverns, Solvei stream, Hunter's Crypt, Ruins of Ihted 후보는 profile에 기록돼 있지만 **disabled**이며 26.2 migration 후 실제 화면을 보기 전에는 작동시키지 않는다.
- `FunctionalWorldSliceBuilder`, `ProductionWorldSlicePrototypeBuilder`, `AuthoredFirstRegionBuilder`는 mechanics/layout harness일 뿐 production visual source가 아니다.
- 현재 APOTHEOSIS full map은 1.20.1-era 공개본이다. Java 26.2 + NeoForge migration 성공을 아직 주장하지 않는다.
- 외부 월드가 호환되지 않더라도 AI 자작 맵으로 자동 대체하지 않는다.

## 전투/카메라 external-code boundary

- TURNBOUND의 `Intent + Affinity + Poise + EXPOSED`, deterministic RNG, server-authoritative command/target/reward 규칙은 `CANON.md`와 `02_COMBAT_SYSTEM.md`가 정본이다.
- 외부 턴제 코드는 Minecraft vanilla combat interception/integration adapter에 사용하며 TURNBOUND 전투 규칙을 대체하지 않는다.
- 일반 자연몹 공격으로 battle을 자동 생성하는 외부 gameplay 흐름은 채택하지 않는다.
- battle camera는 external Free Camera의 실제 smoothing math를 사용하며 근거 없는 임의 cinematic profile을 추가하지 않는다.

## 규칙

- `파일 반입 전`, `reference only`, `disabled`는 production에서 실제로 사용되지 않는다는 뜻이다.
- 코드/리소스/맵을 실제 사용하면 원본 URL, commit/tag 또는 버전, 라이선스/사용조건, 수정/연결 내용을 기록한다.
- 출처 불명 리소스는 production repo에 넣지 않는다.
- reference-only 자산을 보고 새 모델/텍스처/UI/맵을 수동으로 닮게 만드는 것은 금지한다.
