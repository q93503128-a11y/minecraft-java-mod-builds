# 32 — M5 PLAYER-FACING VISUAL SOURCE AUDIT

Date: 2026-09-15

## 1. 목적

이 문서는 TURNBOUND: RE의 현재 M5/M6 player-facing 화면에서 **AI 자작/임시 시각물**, 출처 없는 placeholder, 외부 자산을 보고 다시 만든 유사 디자인이 production 경로에 남아 있는지 검사한 결과를 기록한다.

이 감사의 PASS는 `화면이 예쁘다` 또는 `실플레이 최종 PASS`를 뜻하지 않는다.

구분:
- **SOURCE CLEAN**: 실제 외부/Mojang runtime source를 사용하며 AI 자작 visual placeholder가 없음.
- **VISUAL GATE PENDING**: source는 정리됐지만 screenshot/GUI scale/실플레이 품질 검증 또는 추가 외부 asset이 필요함.
- **BLOCKED**: 필요한 실제 외부 source가 아직 없어 임시 자작물로 메우지 않고 중단함.

## 2. 감사 결과

| Surface | current visual source | 판정 | 남은 일 |
|---|---|---|---|
| Shared panel/frame/title/meter | Kenney `UI Pack - Pixel Adventure` 2.0 / CC0 | SOURCE CLEAN | screenshot / GUI scale visual audit |
| Battle Command frame | Kenney UI frame family | SOURCE CLEAN | screenshot audit |
| Battle Command number prompts | Kenney `Input Prompts Pixel` 1.0, original `tile_0051`~`tile_0056` bytes renamed to `key_1`~`key_6` | SOURCE CLEAN | 실화면 크기/가독성 확인 |
| Battle Command input | top-row `1`~`6` screen-local shortcuts, glyph와 실제 입력 일치 | CODE REVIEWED | runtime keyboard test |
| Battle action identity | Mojang runtime semantic `ItemStack` + server snapshot action name/slot/energy/tooltip | SOURCE CLEAN / VISUAL GATE PENDING | 480x270 포함 screenshot에서 item silhouette와 text가 실제로 충분히 구분되는지 확인 |
| Target chooser buttons | Kenney frame + Kenney number prompt + authoritative participant name | SOURCE CLEAN / VISUAL GATE PENDING | 실제 전투 밀도에서 가독성 확인 |
| Live-world target marker | Minecraft nametag text state, server-published eligible/selected marker facts | SOURCE CLEAN | world clutter/readability playtest |
| Virtual participant target marker | Kenney semantic frame on logical stage slot | SOURCE CLEAN | stage readability playtest |
| Battle participant model/texture | Mojang runtime entity model/texture | SOURCE CLEAN | pose/centering/animation quality gate |
| Battle travel/action accents | Mojang runtime ItemStack + Mojang sound + Kenney frame | SOURCE CLEAN | timing/VFX quality gate |
| Equipment rows | Mojang runtime Shield / Copper Sword / Golden Apple + Kenney frame | SOURCE CLEAN | screenshot audit |
| Battle Result Coin | Mojang `gold_nugget` runtime item | SOURCE CLEAN | reveal timing/readability |
| Battle Result Essence | Mojang `experience_bottle` runtime item | SOURCE CLEAN | reveal timing/readability |
| Battle Result Character Shard | Mojang `amethyst_shard` runtime item | SOURCE CLEAN | reveal timing/readability |
| Encounter preparation | Mojang Iron Ingot / Golden Carrot / Cooked Cod / Cooked Salmon runtime item | SOURCE CLEAN | screenshot audit |
| Expedition Journal encounter rows | Kenney frame + server-authored Mojang runtime enemy entity lineup + translated encounter facts | SOURCE CLEAN / VISUAL GATE PENDING | 26×26 compact lineup의 실제 GUI scale 가독성·모델 겹침 screenshot 확인 |
| Production world | Drehmal: APOTHEOSIS v2.2.2f external world | SOURCE LOCKED | Java 26.2 migration/load + actual-world visual gate |

## 3. 이번 감사에서 제거한 placeholder

### Battle Result

이전에는 reward row 왼쪽의 Kenney success frame 안이 비어 있어 사실상 icon placeholder였다.

현재:
- Coin -> `minecraft:gold_nugget`
- Essence -> `minecraft:experience_bottle`
- Character Shard -> `minecraft:amethyst_shard`

TURNBOUND 통화 PNG는 만들지 않는다.

### Encounter preparation

이전에는 준비물 앞의 `◇` 문자가 item icon 역할을 대신했다.

현재:
- Iron Reinforcement -> `minecraft:iron_ingot`
- Golden Provision -> `minecraft:golden_carrot`
- Cooked Cod Ration -> `minecraft:cooked_cod`
- Cooked Salmon Ration -> `minecraft:cooked_salmon`

실제 Mojang runtime ItemStack을 렌더링하며 unknown id는 자작 placeholder로 대체하지 않는다.

### Battle Command input prompts

숫자 단축키를 단순 텍스트 `[1]`, `1.` 같은 자작 표기로 만들지 않았다.

Kenney `Input Prompts Pixel` 1.0의 실제 16x16 원본 sprite를 반입했다.
- `tile_0051` -> `input/key_1.png`
- `tile_0052` -> `input/key_2.png`
- `tile_0053` -> `input/key_3.png`
- `tile_0054` -> `input/key_4.png`
- `tile_0055` -> `input/key_5.png`
- `tile_0056` -> `input/key_6.png`

원본 sprite bytes는 수정하지 않고 파일명만 production 의미에 맞게 rename했다.
license는 `third_party/licenses/Kenney_Input_Prompts_Pixel_CC0.txt`에 보존한다.

화면 표시와 실제 동작을 일치시키기 위해 Battle Command screen 안에서:
- action picker의 현재 1~6번째 항목을 top-row `1`~`6`으로 선택,
- target picker의 **현재 페이지** 1~6번째 eligible target을 top-row `1`~`6`으로 선택,
- mouse selection은 그대로 유지한다.

이 shortcut은 GUI-local input이며 별도 global TURNBOUND combat key mapping을 등록하지 않는다.
서버가 publish한 action/eligible target을 선택하는 UI shortcut일 뿐 command legality/damage/reward 권한을 클라이언트로 이전하지 않는다.

### Battle action identity

이전에는 action identity가 action name / slot / energy / tooltip 텍스트에 거의 전적으로 의존했다.

현재는 `vertical_actions.json`의 8캐릭터 32개 active action을 `docs/33_M5_ACTION_VISUAL_ASSET_GATE.md`의 정본 mapping에 따라 실제 Mojang runtime item으로 연결한다.

- action picker에서는 각 action button 위 compact identity icon으로 표시,
- action timeline에서는 현재 실행 중 action의 compact identity icon으로 표시,
- unknown action은 generic sword/star/magic icon을 만들지 않고 icon 없이 fail-closed,
- gameplay 수치/타깃/상태/판정은 그대로 server-authoritative data가 정본이다.

TURNBOUND 전용 skill icon PNG는 추가하지 않았다.

### Expedition Journal encounter identity

이전 Journal encounter row는 Kenney frame 안에 이름 / 위험도 / 적 수만 표시되어 전투 구성이 텍스트에만 의존했다.

현재:
- 서버 `EncounterView`가 해당 encounter의 validated character definition에서 실제 `sourceEntity` 목록을 함께 publish한다.
- 클라이언트가 encounter id를 보고 적 종류를 추측하거나 하드코딩하지 않는다.
- row 왼쪽에는 `CharacterEntityPreview`를 통해 Mojang runtime `EntityType` 모델을 최대 4기까지 실제 lineup으로 표시한다.
- 26×26 소형 슬롯은 별도 `EntityPreviewLayout.fitCompact` 경로로 체형에 맞춰 스케일한다.
- source entity가 잘못되었거나 LivingEntity가 아니면 임의 몹 실루엣/icon으로 대체하지 않고 해당 preview만 fail-closed한다.
- 내부 encounter id가 `debug_*`여도 플레이어-facing 이름은 `encounter.turnbound_re.*.name` 번역 key를 사용한다. 현재 `폐허 길목 순찰`, `균열 선봉대`처럼 세계 내 이름으로 노출한다.

TURNBOUND 전용 encounter illustration/icon은 추가하지 않았다.

## 4. 금지 회귀

다음은 이후 작업에서도 금지한다.

- 빈 frame을 최종 icon처럼 사용.
- `◇`, `◆`, 단순 색 사각형 등을 실제 item/action artwork의 대체물로 확정.
- `[1]`, `[2]` 같은 AI 자작 키캡 그림/PNG를 production source로 사용.
- 외부 icon pack을 눈으로 본 뒤 비슷한 TURNBOUND icon을 새로 그림.
- 임시 자작 RPG panel을 Kenney family 사이에 섞음.
- Mojang runtime item을 visual identity로 사용한다는 이유로 vanilla gameplay component/effect를 TURNBOUND 시스템에 몰래 상속.
- 모르는 action에 generic sword/star/magic icon을 자동 fallback으로 붙임.
- encounter id를 보고 클라이언트에서 적 visual을 임의 추측하거나 generic skull/sword icon을 붙임.

상태 표시용 문자나 텍스트는 정보 전달 보조로 사용할 수 있지만, **필요한 artwork가 존재해야 하는 자리를 영구적으로 대신할 수 없다.**

## 5. 다음 visual gate

다음 우선순위:
1. 현재 Mojang runtime action mapping이 실제 480x270 / 일반 GUI Scale에서 action 간 silhouette를 충분히 구분하는지 screenshot audit.
2. Expedition Journal의 26×26 실제 몹 lineup이 Zombie/Skeleton/Spider와 Creeper/Blaze/Witch/Enderman을 충분히 구분하는지 screenshot audit.
3. 특정 action/entity의 의미가 현재 runtime visual로 충분히 전달되지 않으면 억지 mapping/크기 조정으로 버티지 말고 실제 외부 asset gate를 다시 연다.
4. Drehmal 26.2 migration 후 실제 world에서 anchor/marker/UI의 시야 충돌 검사.

외부 source가 확정되지 않은 상태에서 1~3을 AI 자작 icon/illustration으로 임시 완성하지 않는다.

## 6. 검증 상태

- SOURCE / LICENSE REVIEW: DONE for the assets listed as SOURCE CLEAN.
- CODE REVIEWED: YES for the changes recorded in this audit.
- RELATED TEST CONTRACTS UPDATED: YES, but not executed in this batch.
- BUILD VERIFIED after this audit batch: NO.
- CI RUN for this audit batch: NO (`[skip ci]`).
- SCREENSHOT AUDIT: NOT RUN.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.

따라서 M5 production visual PASS는 아직 선언하지 않는다.
