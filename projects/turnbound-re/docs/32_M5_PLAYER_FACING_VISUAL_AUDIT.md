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
| Battle action identity | server snapshot action name/slot/energy + tooltip | VISUAL GATE PENDING | 충분한 의미를 주는 실제 외부 action-icon family 또는 Minecraft-runtime semantic icon이 필요한지 screenshot 기준 결정. AI 자작 icon 금지 |
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
| Expedition Journal encounter rows | Kenney frame + authored text facts | VISUAL GATE PENDING | text-only density가 충분한지 screenshot으로 판단. 필요 시 실제 외부/Mojang visual source를 먼저 고정 |
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

## 4. 금지 회귀

다음은 이후 작업에서도 금지한다.

- 빈 frame을 최종 icon처럼 사용.
- `◇`, `◆`, 단순 색 사각형 등을 실제 item/action artwork의 대체물로 확정.
- `[1]`, `[2]` 같은 AI 자작 키캡 그림/PNG를 production source로 사용.
- 외부 icon pack을 눈으로 본 뒤 비슷한 TURNBOUND icon을 새로 그림.
- 임시 자작 RPG panel을 Kenney family 사이에 섞음.
- Mojang runtime item을 visual identity로 사용한다는 이유로 vanilla gameplay component/effect를 TURNBOUND 시스템에 몰래 상속.

상태 표시용 문자나 텍스트는 정보 전달 보조로 사용할 수 있지만, **필요한 artwork가 존재해야 하는 자리를 영구적으로 대신할 수 없다.**

## 5. 다음 visual gate

다음 우선순위:
1. Battle action identity가 text+tooltip만으로 실제 480x270 / 일반 GUI Scale에서 충분히 읽히는지 screenshot audit.
2. 부족하면 먼저 **하나의 실제 외부 action-icon family** 또는 의미가 명확한 Mojang runtime semantic item mapping을 source/license와 함께 고정.
3. Expedition Journal encounter row가 텍스트-only 카드처럼 느껴지면 같은 방식으로 외부/Mojang visual source gate를 먼저 통과.
4. Drehmal 26.2 migration 후 실제 world에서 anchor/marker/UI의 시야 충돌 검사.

외부 source가 확정되지 않은 상태에서 1~3을 AI 자작 icon/illustration으로 임시 완성하지 않는다.

## 6. 검증 상태

- SOURCE / LICENSE REVIEW: DONE for the assets listed as SOURCE CLEAN.
- CODE REVIEWED: YES for the changes recorded in this audit.
- BUILD VERIFIED after this audit batch: NO.
- CI RUN for this audit batch: NO (`[skip ci]`).
- SCREENSHOT AUDIT: NOT RUN.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.

따라서 M5 production visual PASS는 아직 선언하지 않는다.
