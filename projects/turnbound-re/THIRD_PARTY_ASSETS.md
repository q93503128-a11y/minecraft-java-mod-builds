# THIRD PARTY / REFERENCE REGISTER

외부 코드·맵·모델·텍스처·UI 키트·사운드·폰트를 실제 프로젝트에 넣기 전 반드시 이 문서를 갱신한다.

| ID | 종류 | 출처 | 라이선스 | 현재 사용 | 허용 범위 |
|---|---|---|---|---|---|
| REF-CODE-001 | 구조 참고 | Stephen-Seo/TurnBasedMinecraftMod, `neoforge` branch | MIT | 코드 복사 없음 | Battle/Combatant/manager/network 분리 및 Minecraft 턴제 구현 사례 조사만 |
| REF-UI-001 | UI production skin | Kenney, `UI Pack - Pixel Adventure` 2.0 — https://kenney.nl/assets/ui-pack-pixel-adventure | CC0 1.0 | **사용 중** | Large tiles / Thin outline의 `tile_0002`, `0008`, `0009`, `0020`, `0021`, `0022`를 title + semantic frame으로 사용. meter는 같은 pack의 neutral/red/blue/gold palette를 5px strip으로 축약한 수정본. GUI 확장은 9-slice metadata 사용 |
| REF-UI-002 | UI 자산 후보 / 비교 | tiopalada, `Tiny RPG - Dragon Regalia GUI` — https://tiopalada.itch.io/tiny-rpg-dragon-regalia-gui | CC0 1.0 | 파일 반입 전 | 9-slice frame, rest/hover/click/disabled 상태, target cursor, meter 구조 참고 및 보조 후보. 원본의 강한 JRPG 색/장식은 TURNBOUND: RE 전체 skin으로 그대로 혼합하지 않음 |
| REF-UI-003 | UI 자산 후보 / 입력 glyph | Kenney `Input Prompts Pixel 16×` — Kenney Game Assets preview/catalog | CC0 1.0 | 파일 반입 전 | 키보드/패드 입력 glyph 후보. 실제 파일 반입 전 개별 pack의 공식 배포 페이지와 CC0 표시를 다시 고정 확인 |
| REF-MODEL-001 | humanoid articulation reference | SL0ANE/Loy-s-Goodies, `models/generic-model/characters/230507_alex.bbmodel`, commit `afbb7695b09de0ed8ee3aa97732ff7c3d367520c` — https://github.com/SL0ANE/Loy-s-Goodies | CC0 1.0 | **legacy reference only / 교체 감사 필요** | 과거 Zombie 분절 관절 참고에 사용했으나 원본 BBModel을 직접 쓰지 않고 geometry를 재구성했으므로 새 external-only 규칙에서는 production-final 근거가 아님 |
| EXT-MODEL-002 | runtime production base | Mojang Minecraft Creeper / Spider model layers + entity textures, Minecraft Java 26.2 runtime | Mojang first-party proprietary runtime content | **직접 사용 중** | `ModelLayers.CREEPER`, `ModelLayers.SPIDER`와 `minecraft:textures/entity/...`를 게임 runtime에서 직접 사용. TURNBOUND가 replacement geometry/UV/texture를 새로 디자인하지 않고 action pose/state 연결만 추가 |
| CAND-MODEL-003 | Creeper direct replacement candidate | Moth's Creeper Redone — https://modrinth.com/resourcepack/moths-creeper-redone | MIT | **파일 반입 전** | custom Creeper model/texture를 실제 asset로 직접 사용하는 후보. 현재 공개 호환은 1.21.1이며 OptiFine 또는 EMF+ETF 필요. 26.2 직접 호환/변환 검증 전에는 production에 반입하지 않음. 눈으로 보고 재구성 금지 |
| CAND-MODEL-004 | Spider direct replacement candidate | Scary Spider — https://modrinth.com/resourcepack/scary-spider / version `p54uoJxL` | MIT | **파일 반입 전** | 실제 model pack 직접 사용 후보. `Scary Spider 1.20.2+.zip`은 1.20.2–1.21.8 대상. 26.2 호환/변환 검증 전에는 production에 반입하지 않음. 눈으로 보고 재구성 금지 |
| CAND-ANIM-001 | Spider animation/base candidate | Wall Climbers 1.2 — https://modrinth.com/resourcepack/wall-climbers/version/1.2 | MIT + 프로젝트 사용조건 확인 | **파일 반입 전** | 26.2 지원. Spider/Cave Spider의 벽/천장 leg presentation 및 다른 CEM pack과 병합 가능한 external animation/base 후보. 실제 파일과 고지 조건을 고정한 뒤에만 반입 |
| REF-MODEL-005 | Spider reference only | Fresh Animations: Spiders — https://modrinth.com/resourcepack/fresh-animations-spiders | ARR / custom terms | **reference only** | 원본 `.jem`/`.jpm`/texture/animation 파일 미반입. 새 규칙상 공개 설명/이미지를 보고 비슷한 geometry를 수동 재구성하는 것도 금지 |

## M5 현재 선택

- **Primary UI skin:** `REF-UI-001` Kenney UI Pack - Pixel Adventure.
- 이유: Minecraft와 충돌이 적은 픽셀 해상도, 500+ 분리 sprite, thin/thick outline, panel/button/bar 계열을 한 family에서 공급하며 CC0라 수정/재배포 제약이 가장 낮다.
- `REF-UI-002`는 상태별 frame/9-slice/target cursor의 구조가 매우 좋지만, 원본의 분홍/주황/청색 JRPG visual identity를 그대로 섞으면 화면별 언어가 갈라질 위험이 있어 보조 후보로 제한한다.
- 검증 원본 ZIP SHA-256: `6ebf462e7f209f5f348419b09be6601a559ef1e1d6b595f0e9f8aa4c00a84048`. 현재 M5 frame/title/meter는 `turnbound_re` namespace production resource를 사용하며 vanilla advancement/boss-bar bridge는 제거했다.
- **Creeper / Spider 현재 production base:** 새 geometry를 만들지 않고 `EXT-MODEL-002` Mojang runtime model/texture를 직접 사용한다.
- `CAND-MODEL-003`, `CAND-MODEL-004`, `CAND-ANIM-001`은 실제 파일을 반입/검증하기 전까지 후보일 뿐이며, 해당 디자인을 눈으로 보고 TURNBOUND 자체 geometry로 다시 만드는 것은 금지한다.

## Starter character visual asset

- `starter_zombie.png`는 이 프로젝트 작업에서 직접 제공된 128×128 texture 원본이다.
- SHA-256: `52822eabfae98c0dbacc1299173b80b2aae4c372e964c568a4139613cf4b1b7b`.
- 외부 `REF-MODEL-001`의 texture를 사용하지 않는다.
- 단, Zombie geometry가 `REF-MODEL-001`을 참고해 수동 재구성된 부분은 새 external-only 규칙에 따라 별도 교체 감사 대상이다.

## 규칙
- `현재 사용=없음`, `파일 반입 전`, `reference only`는 실제 외부 파일이 저장소에 들어오지 않았다는 뜻이다.
- 코드/리소스를 가져오면 원본 URL, commit/tag 또는 배포 버전, 파일 경로, 라이선스, 수정 내용을 기록한다.
- 라이선스 파일/고지 의무가 있으면 배포 형태와 무관하게 보존한다.
- 출처가 불명확한 리소스는 production에 넣지 않는다.
- 개인 테스트 목적이라도 정본 저장소에는 출처 불명 파일을 넣지 않는다.
- 여러 UI pack을 한 화면에 무분별하게 혼합하지 않는다. 하나의 primary sprite family를 정하고 부족한 기능만 같은 family 또는 명시적으로 호환되는 외부 자산으로 보완한다.
- reference-only 자산을 보고 새 모델/텍스처/UI를 수동으로 닮게 만드는 것은 직접 사용으로 취급하지 않으며 production에 넣지 않는다.
