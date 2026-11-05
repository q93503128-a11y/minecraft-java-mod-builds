# 《이능 직업 서바이벌》 — 멀티 위치 표시·동적 조명·레시피 검색 및 외부 모드 범위 정책

> 2026-11-05 공개 배포 페이지/제작사 설명 대조. **RESEARCH ONLY — NOT INSTALLED / NOT CO-LOAD TESTED.** [프로젝트 정본](./PROJECT.md), [게임 기획](./GAME_DESIGN.md), [외부 몹·보스/던전 후보](./EXTERNAL_MOD_ECOSYSTEM_RESEARCH_2026-11-05.md)와 함께 읽는다. Minecraft **1.21.1 NeoForge는 유력 후보지만 아직 미확정**. 직업 7·8의 상세 기획 보류도 유지.

## 2026-11-05 JEI 표시 선택 확인 및 전체 성장 통합

- [공통 생존·경험치/레벨·보상·난이도 통합](./CORE_GAMEPLAY_GROWTH_REWARDS_DIFFICULTY_V1.md)과 함께 외부 QOL을 유지하되 **별도 RPG 성장축은 금지**.
- **사용자 확인:** JEI를 같은 모드팩에서 쓰더라도 필요 없는 플레이어는 **`Ctrl+O`로 JEI 목록/검색 UI를 숨기면 충분**하므로 JEI 제거/전체 서버 강제 비활성화 요구 없음. 숨기기와 JEI jar 언로드/제작법 조회 자체 사용 불가 상태를 혼동하지 않음.
- **바닐라 책**은 JEI와 별개. 아이콘 버튼 숨김은 Crafting Tweaks + Balm의 개별 선택 옵션이며 모든 플레이어에게 강제할지 미확정. 우리 전용 제작/매개체/제단은 JEI 레시피 화면과 연계 검토.
- 멀티 위치 바와 바닐라 XP/이 게임 RPG 레벨 HUD는 서로 가리지 않도록 게임 클라이언트에서 검증한다.

## 1. 사용자 최신 요구사항

**진행에 추가할 편의성 후보**
1. 멀티플레이어의 **방향/위치를 경험치 바 주변에 표시**해 다른 플레이어를 찾기 편하게 한다.
2. **손에 횃불을 들었을 때 이동 중에도 주변이 밝아지도록** 동적 조명을 제공한다.
3. 모드 조합법 및 아이템 사용처를 폭넓게 찾아보는 **레시피 검색 UI**를 추가한다. 이때 **바닐라 레시피북이 별도 중복 패널로 남는 것이 불편하면 버튼을 숨기는 선택**을 제공한다.
4. 외부 모드는 **이 게임을 보조하는 QOL/몬스터/보스/던전/작은 장비 강화** 중심. **별도의 RPG 레벨/직업/스탯/스킬 트리·강화 계열·재화·퀘스트/세계관/전투 규칙을 강제로 추가해 우리 게임 주력 성장축과 경쟁하는 모드**는 기본 선택에서 제외한다. 새로운 장비 소폭 강화/인챈트 편의는 조건부 허용하지만 **독립 성장축이면 제외**.

## 2. 1.21.1 NeoForge에서 실제 출시 파일을 찾은 모드

| 목적 | 1차 후보 | 버전·구현과 배치 | 판정 |
|---|---|---|---|
| **멀티 위치 — 경험치 바 자체** | [Locator Bar (Reworked)](https://modrinth.com/mod/reworked-locator-bar) [**1.1.0+1.21.1-neoforge**](https://modrinth.com/mod/reworked-locator-bar/version/1.1.0%2B1.21.1-neoforge) | 1.21.1 NeoForge **실제 릴리스 확인**. 기본은 상단 바 구성도 가능하나 **설정에서 classic style + elements on XP bar** 옵션 제공. 멀티 위치 표시/waypoint manager. 서버 설치 시 서버 설정이 클라이언트 표시 정책에 영향 | **1순위 시험 후보** |
| **멀티 위치 — XP 바 위** | [PlayerLocatorPlus NeoForged](https://www.curseforge.com/minecraft/mc-mods/playerlocatorplus-neoforged) | 1.21.1 NeoForge 전용 배포 확인. 다른 차원은 동기화 목록에서 제외/정리; 팀 색상/거리 페이드·높이 표시·웅크리기 등 숨김 규칙. 제작자 [GitHub 설명](https://github.com/kanoyo-git/PlayerLocatorPlus-Neoforged)에 **서버와 클라이언트 모두 설치**, 바닐라 XP/점프 UI 우선 등 | **대안**, Locator Bar와 **둘을 함께 사용하지 말 것** |
| **횃불·불붙은 몹의 동적 조명** | [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) [**4.8.9+1.21.1**](https://modrinth.com/mod/lambdynamiclights/version/4.8.9%2B1.21.1) | **Fabric/NeoForge**, 클라이언트 전용 파일 확인. 1.21.1용 End-of-Life 업데이트라는 제작자 설명; 손에 든 횃불/광원 아이템 등 이동 조명. 장면 가독성 향상, 렌더 부하/다른 셰이더/렌더러 조합 실측 | **1순위 시험 후보** |
| **동적 조명 대안** | [RyoamicLights](https://modrinth.com/mod/ryoamiclights) | **1.21.1 NeoForge** 지원, LambDynamicLights 기반 비공식 포트; 클라이언트 전용, **ObsidianUI 필요**, 활성 설정/Embeddium 등 검토 | LambDynamicLights와 **동시 설치 금지** |
| **전체 모드 제작/사용처 검색** | [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) | **1.21.1 NeoForge** 실제 배포 파일 [JEI 릴리스 목록](https://www.curseforge.com/minecraft/mc-mods/jei/files/all). 아이템 `R` 제작법, `U` 사용처, `@mod` 검색·목록 토글 `Ctrl+O`. 새로운 직업/스탯/아이템을 생성하지 않음. 사용자 지정 제작대는 **모드가 JEI 호환 통합 제공 시** 목록에 표시 | **1순위 시험 후보** |
| **바닐라 제작책 버튼 감추기 + 제작창 QOL** | [Crafting Tweaks](https://modrinth.com/mod/crafting-tweaks) [**21.1.10+neoforge-1.21.1**](https://modrinth.com/mod/crafting-tweaks/version/21.1.10%2Bneoforge-1.21.1) | 1.21.1 NeoForge 실제 파일 확인. **Balm 의존**. 재료 정렬/회전/제작칸 정리. 제작사 [공식 설정](https://mods.twelveiterations.com/minecraft/crafting-tweaks/config)에 **`hideVanillaCraftingGuide`** 값 명시. JEI/바닐라 레시피 중복 UI 해결 | **책 버튼 숨김을 원하면 추천** |

**모드가 정상 설치/로딩되는지, 위치 HUD/XP/UI/셰이더·인벤토리 UI 동시 호환성, server-client 역할, 메모리/프레임 비용, 정확한 dependency 버전**은 모두 실제 modpack 설치 후 확인해야 한다. 특히 **동적 조명은 일반적으로 클라이언트 표시 향상이지 실제 서버 블록 광원/몬스터 스폰 억제 기능을 약속하지 않는다**. 건축 중 횃불을 설치하지 않았는데 조명으로 몹 스폰이 막힌다고 오해하지 않도록 한다.

## 3. 바닐라 레시피북 vs JEI 질문의 정확한 답

- **바닐라 레시피북은 모드가 추가한 일반적인 Crafting/Smelting 등 레시피 중 등록·해금된 것을 표시할 수 있다.** 하지만 플레이어가 재료/특정 조건을 발견하기 전에는 표시되지 않을 수 있고, 모든 특수 제작대·의식/제단·장비 업그레이드/레시피 타입을 보편적으로 검색하는 '모드팩 전체 제작법 탐색기'는 아니다.
- **JEI는 별도 아이템/레시피 브라우저**로 `R`/`U`를 눌러 제작법/재료 사용처 검색, 바닐라·일반 모드 레시피를 더 쉽게 확인하게 한다. 다만 **모드 특수 제작/제물/제단**은 별도 JEI 통합을 구현하거나 기본 지원해야 나타난다. **우리 모드가 특수 제작대를 만든다면 JEI 레시피 표시/도움말 API도 기획에 넣어야 한다**.
- **JEI 설치만으로 바닐라 레시피북을 자동으로 완전히 끈다고 가정하지 않는다.** 게임의 바닐라 책 아이콘은 눌러서 닫을 수 있다([Minecraft Wiki Recipe book](https://minecraft.wiki/w/Recipe_book)).
- **버튼 자체 숨김:** Crafting Tweaks의 제작사 공식 [Configuration](https://mods.twelveiterations.com/minecraft/crafting-tweaks/config)에 `hideVanillaCraftingGuide` 기본값 false가 있고 true로 바꾸면 버튼을 숨긴다고 명시. 실제 프로젝트의 해당 버전에서 인벤토리/제작대/화로 모두 원하는 방식으로 숨겨지는지 확인하고, `Balm` 의존성도 함께 고려.
- **설정 예시:** `config/craftingtweaks-common.toml`의 client 항목 `hideVanillaCraftingGuide = true` (설치한 버전이 자동 생성한 실제 구성 파일의 값·섹션 우선). 같은 페이지에 가이드 오프셋과 `mode` (Crafting Tweaks 버튼/핫키 조절) 값도 존재하므로 **`mode=DISABLED`가 바닐라 레시피북 자체를 끄는 설정이라고 오해하지 않도록**.
- 레시피북 **표시 버튼을 감추는 것**과 원래 레시피 데이터/제작 가능 여부를 **삭제하는 것**은 다르다. JEI가 보인다고 실제 재료·보상 요구나 기술 사용 권한을 풀어주지 않음.

## 3.1 경험치 바와 RPG 레벨 최신 확정

- [무제한 레벨 및 XP 분리 수치 비교](./RPG_XP_LEVEL_SKILLPOINTS_BALANCE_V1.md): **바닐라 XP 바/레벨은 바닐라 인챈트·수선 경험치를 그대로 표시**, 우리 이능 RPG 레벨은 별도 서버 성장 상태. **같은 XP 바를 RPG 레벨로 덮어쓰지 않는다**.
- 같은 모드팩의 JEI는 개인별 `Ctrl+O`로 아이템 목록 UI 숨김 가능하고, 멀티 Locator Bar는 **바닐라 XP HUD와 겹칠 때 우선순위/가독성**을 실제 화면에서 확인한다. RPG 레벨 표시의 정확한 위치는 미정.

## 4. 멀티 플레이어 위치 바의 게임 시스템/UI 규칙

- **1.21.6 이후 Java는 바닐라 Locator Bar 기능이 있지만 현재 유력 후보 1.21.1에는 기본으로 없다.** 1.21.1용 별도 모드를 검토하는 이유. [Minecraft Wiki: Locator Bar](https://minecraft.wiki/w/Locator_Bar).
- **경험치 바 충돌 주의**: 이 게임은 **바닐라 XP + 별도 공통 RPG 레벨·스킬 포인트** 성장 시스템이 있다. HUD를 새로 디자인할 때 **플레이어 위치 바가 XP 수치/이동/비행/스킬 쿨다운·체력 표시와 겹치지 않아야** 한다. XP bar '위' 대안과 '안' 클래식 옵션을 실제 FOV/GUI scale에서 비교. 게임이 자체 XP bar 전체를 임의로 제거하지 않음.
- 멀티 탐색 재미·협동 목적으로 사용하되 **PvP 은신/상대 위치 강제 공개 문제**를 고려. 서버 측 거리·차원·팀·웅크리기/은신/투명효과의 위치 숨김 규칙이 가능한지 확인. **PlayerLocatorPlus NeoForged**는 같은 차원/웅크리기 숨김을 명시하나 **Locator Bar (Reworked)**에서도 원하는 보안/은신 정책을 실제 검증해야 하며 **서로 같다고 단정할 수 없음**.
- 동적 조명은 클라이언트 편의 기능, 위치 바는 멀티 권한/개인정보·서버 동기화 기능. 두 기능이 모두 'QOL'이라도 **설치 위치/서버 필수성**은 서로 다르다.

## 5. 외부 모드 선별 계약 — 이 게임보다 개성이 강하면 보류

**허용 우선(하위 시스템)**
- 다른 플레이어 위치 안내, 모드/바닐라 제작법 검색, 동적 조명, 성능·버그 수정, 지도/인벤토리 편의(선택), 짧은 애니메이션·접근성 설정.
- 몬스터/보스/던전·구조물/탐험 보상·스폰/XP 보정. 단 콘텐츠가 **직업 능력 활용처**가 되고 월드/보스 기본 기믹을 보존해야 함.
- **소소한 갑옷/인챈트 강화**는 새로운 전용 레벨/등급·보석/재화·스킬 트리·전용 장비 직업 체계 없이 일반 아이템 업그레이드 범위로 끝날 경우 **조건부 허용**. 해당 모드의 조합법·UI·성장 중첩을 따로 검토.

**제외 기본(독립 RPG 성장축)**
- 외부 자체 **직업 선택/스탯 레벨/XP 시스템**, 별도 스킬 포인트/스킬 트리, 완전한 신규 마법 체계/직업, 필수 퀘스트·진영·평판·시즌 패스, 독립 통화/강화 장비 단계, 영구 능력치 성장/대규모 자체 보스 레벨링 시스템.
- **Iron's Spells 'n Spellbooks**, **Apotheosis 전체 성장 시스템**, **Epic Fight**(전투 표현·피해 시스템 대폭 변경), **Ars Nouveau / 대형 RPG/스킬트리 모드** 등은 **검토 없이 기본 채택하지 않는다**. 이들이 나쁜 모드라는 평가가 아니라 **이 게임 고유 직업/전투/성장 주도권과 충돌할 가능성이 높다**는 프로젝트 적합성 판단. 단순 무기 손맛 개선이 필요한 경우도 실제 클래스 전투 판정이 정상이어야 함.
- **중복 기능 금지**: 동적 조명 1개, 위치 HUD 1개, 아이템/레시피 탐색기 1개, 몹 난이도 조절 중복 최소화. 편의 모드가 UI 상호 충돌하거나 필수 Java/loader 버전 변경을 요구하면 다른 대안으로 교체.
- **총 설치 모드 수가 높다는 이유로 품질이 좋아지지는 않는다.** 사용 이유·클래스 연계·플레이 불편·성능·유지보수·라이선스·멀티 허용을 기준으로 채택.

## 6. 추천 조합 및 다음 단계

**우선 후보**: `Locator Bar (Reworked)` **또는** `PlayerLocatorPlus NeoForged`, `LambDynamicLights`, `JEI`. **선택 추가**: `Crafting Tweaks + Balm`을 레시피북 버튼 숨김 및 제작창 정렬 기능 목적에만 사용. 두 위치 바/동적 조명 모드를 동시에 넣지 않음.

**빠른 클라이언트/서버 QA**:
1. 1.21.1 NeoForge 환경에서 각 파일·버전/종속성 맞는지 검증. **로더/MC 버전 선택 전 실제 jar 다운로드/게임 로딩은 보류**.
2. 2인 이상 멀티에서 위치 표시 방향·거리·높이/은신·차원 전환·XP 획득·점프·비행과의 겹침·서버 설치 강제 여부 확인.
3. 횃불/영혼 횃불/랜턴 등 손에 드는 조명과 셰이더/리소스팩/다른 광원·낮은 FPS 상태를 확인. 동적 조명은 서버 스폰 억제·실제 블록 light level을 변경하지 않는다는 경계.
4. 모드 아이템을 JEI의 R/U로 실제 조회, **우리 직업 매개체/계약 제단/인형 제작대**의 추가 등록이 필요할 경우 전용 recipe categories/도움말 표시 구현. 바닐라 레시피북 버튼을 숨긴 화면과 숨기지 않은 화면의 GUI 충돌 확인.
5. 직업 선택/XP/자원/장비 성장/외부 모드 몬스터와 QOL 조합을 실제 플레이하면서, **추가 레벨·재화·스탯 시스템이 불쑥 나타나지 않는지** 점검.

**현재 상태:** 정확한 프로젝트별 버전/loader/다운로드 파일 선택은 미확정. 후보 공개 지원·설정을 확인한 단계이고 **실제 설치/성능/멀티 테스트는 미실행**.

**STATUS: RESEARCHED / CANDIDATE ONLY / NO JAR INSTALLED / NO COLOAD OR CLIENT/MULTIPLAYER TEST.**
