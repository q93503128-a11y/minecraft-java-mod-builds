# THIRD PARTY / REFERENCE REGISTER

외부 코드·맵·모델·텍스처·UI 키트·사운드·폰트를 실제 프로젝트에 넣기 전 반드시 이 문서를 갱신한다.

| ID | 종류 | 출처 | 라이선스 | 현재 사용 | 허용 범위 |
|---|---|---|---|---|---|
| REF-CODE-001 | 구조 참고 | Stephen-Seo/TurnBasedMinecraftMod, `neoforge` branch | MIT | 코드 복사 없음 | Battle/Combatant/manager/network 분리 및 Minecraft 턴제 구현 사례 조사만 |
| REF-UI-001 | UI production skin | Kenney, `UI Pack - Pixel Adventure` 2.0 — https://kenney.nl/assets/ui-pack-pixel-adventure | CC0 1.0 | **사용 중** | Large tiles / Thin outline의 `tile_0002`, `0008`, `0009`, `0020`, `0021`, `0022`를 title + semantic frame으로 사용. meter는 같은 pack의 neutral/red/blue/gold palette를 5px strip으로 축약한 수정본. GUI 확장은 9-slice metadata 사용 |
| REF-UI-002 | UI 자산 후보 / 비교 | tiopalada, `Tiny RPG - Dragon Regalia GUI` — https://tiopalada.itch.io/tiny-rpg-dragon-regalia-gui | CC0 1.0 | 파일 반입 전 | 9-slice frame, rest/hover/click/disabled 상태, target cursor, meter 구조 참고 및 보조 후보. 원본의 강한 JRPG 색/장식은 TURNBOUND: RE 전체 skin으로 그대로 혼합하지 않음 |
| REF-UI-003 | UI 자산 후보 / 입력 glyph | Kenney `Input Prompts Pixel 16×` — Kenney Game Assets preview/catalog | CC0 1.0 | 파일 반입 전 | 키보드/패드 입력 glyph 후보. 실제 파일 반입 전 개별 pack의 공식 배포 페이지와 CC0 표시를 다시 고정 확인 |
| REF-MODEL-001 | humanoid articulation reference | SL0ANE/Loy-s-Goodies, `models/generic-model/characters/230507_alex.bbmodel`, commit `afbb7695b09de0ed8ee3aa97732ff7c3d367520c` — https://github.com/SL0ANE/Loy-s-Goodies | CC0 1.0 | **legacy reference only / production 미사용** | 과거 Zombie 분절 관절 참고에 사용했지만 원본 BBModel을 직접 쓰지 않고 geometry를 재구성했으므로 새 external-only 규칙에서 해당 재구성 모델은 폐기 |
| EXT-MODEL-002 | runtime production base | Mojang Minecraft entity model layers + entity textures, Minecraft Java 26.2 runtime | Mojang first-party proprietary runtime content | **직접 사용 중** | `ModelLayers.CREEPER`, `SPIDER`, `BLAZE`, `WITCH`, `IRON_GOLEM`, `ZOMBIE`와 대응 `minecraft:textures/entity/...`를 게임 runtime에서 직접 사용. TURNBOUND는 replacement geometry/UV/texture를 새로 디자인하지 않고 authoritative action에 필요한 기존 관절 pose/state 연결만 추가 |
| CAND-MODEL-003 | Creeper direct replacement candidate | Moth's Creeper Redone — https://modrinth.com/resourcepack/moths-creeper-redone | MIT | **파일 반입 전** | custom Creeper model/texture 직접 사용 후보. 공개 호환/의존성이 26.2 production 조건을 충족하는지 검증 전에는 반입하지 않음. 눈으로 보고 재구성 금지 |
| CAND-MODEL-004 | Spider direct replacement candidate | Scary Spider — https://modrinth.com/resourcepack/scary-spider / version `p54uoJxL` | MIT | **파일 반입 전** | 실제 model pack 직접 사용 후보. 26.2 호환/변환 검증 전에는 production에 반입하지 않음. 눈으로 보고 재구성 금지 |
| CAND-ANIM-001 | Spider animation/base candidate | Wall Climbers 1.2 — https://modrinth.com/resourcepack/wall-climbers/version/1.2 | MIT + 프로젝트 사용조건 확인 | **파일 반입 전** | 26.2 지원. Spider/Cave Spider의 벽/천장 leg presentation external animation/base 후보. 실제 파일과 고지 조건을 고정한 뒤에만 반입 |
| REF-MODEL-005 | Spider reference only | Fresh Animations: Spiders — https://modrinth.com/resourcepack/fresh-animations-spiders | ARR / custom terms | **reference only** | 원본 `.jem`/`.jpm`/texture/animation 파일 미반입. 공개 설명/이미지를 보고 비슷한 geometry를 수동 재구성하는 것도 금지 |

## M5 현재 선택

- **Primary UI skin:** `REF-UI-001` Kenney UI Pack - Pixel Adventure.
- 이유: Minecraft와 충돌이 적은 픽셀 해상도, 500+ 분리 sprite, thin/thick outline, panel/button/bar 계열을 한 family에서 공급하며 CC0라 수정/재배포 제약이 가장 낮다.
- `REF-UI-002`는 상태별 frame/9-slice/target cursor 구조가 좋지만 화풍 혼합 위험 때문에 보조 후보로 제한한다.
- 검증 원본 ZIP SHA-256: `6ebf462e7f209f5f348419b09be6601a559ef1e1d6b595f0e9f8aa4c00a84048`.
- **대표 roster의 vanilla-source 캐릭터 production base:** 새 creature geometry를 만들지 않고 `EXT-MODEL-002` Mojang runtime model/texture를 직접 사용한다.
- TURNBOUND가 추가하는 것은 server-authored action을 구분하기 위한 기존 bone/part pose, stage motion, projectile/impact timing뿐이다.
- 별도 외부 custom model을 채택하려면 실제 사용 가능한 파일과 라이선스, 26.2 호환을 먼저 고정하고 그 파일 자체를 사용한다.

## 제거된 legacy visual

- 과거 `starter_zombie.png`와 reference 기반 분절 Zombie geometry는 external-only 규칙 이전 산출물이다.
- 과거 texture SHA-256 기록: `52822eabfae98c0dbacc1299173b80b2aae4c372e964c568a4139613cf4b1b7b`.
- 현재 production renderer는 Mojang runtime Zombie model + `minecraft:textures/entity/zombie/zombie.png`를 직접 사용한다.
- 해당 legacy texture는 production resources에서 제거한다.
- Blaze의 TURNBOUND 전용 core/rod geometry, Witch의 별도 hat/satchel geometry, Iron Golem의 widened chest/segmented limb geometry도 동일 이유로 제거한다.

## 규칙
- `현재 사용=없음`, `파일 반입 전`, `reference only`는 실제 외부 파일이 production에 들어오지 않았다는 뜻이다.
- 코드/리소스를 가져오면 원본 URL, commit/tag 또는 배포 버전, 파일 경로, 라이선스, 수정 내용을 기록한다.
- 라이선스 파일/고지 의무가 있으면 배포 형태와 무관하게 보존한다.
- 출처가 불명확한 리소스는 production에 넣지 않는다.
- 개인 테스트 목적이라도 정본 저장소에는 출처 불명 파일을 넣지 않는다.
- 여러 UI pack을 한 화면에 무분별하게 혼합하지 않는다.
- reference-only 자산을 보고 새 모델/텍스처/UI를 수동으로 닮게 만드는 것은 직접 사용으로 취급하지 않으며 production에 넣지 않는다.
