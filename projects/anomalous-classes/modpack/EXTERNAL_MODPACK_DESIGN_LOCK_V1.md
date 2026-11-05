# 《이능 직업 서바이벌》 외부 모드팩 선정 정본 v1 — 1.21.1 NeoForge

> **Decision: DESIGN LOCKED / RUNTIME NOT VERIFIED. 작성·공개 배포 자료 확인일: 2026-11-05.** 사용자의 요청대로 핵심 게임을 해치는 독립 RPG 성장·과도한 월드젠·전투 엔진 교체/중복 QOL을 제외하고 **실제로 첫 통합 시험에 올릴 모드 목록**을 고정한다. 이는 **실제 호환성 성공, JAR 패키징, 서버 멀티 검증을 뜻하지 않는다**. 실제 `mods/` 디렉터리에 넣고 클라이언트·전용 서버·새 월드를 함께 실행한 뒤에야 **RUNTIME LOCK**으로 승격할 수 있다. **이번에는 어떤 JAR도 다운로드·설치·배포·코드 변경하지 않음.**
>
> 정본 연결: [PROJECT](../PROJECT.md) / [GAME DESIGN](../GAME_DESIGN.md) / [이전 모드 조사](../EXTERNAL_MOD_ECOSYSTEM_RESEARCH_2026-11-05.md) / [QOL·별도 RPG 성장 금지](../QOL_MODS_AND_EXTERNAL_SCOPE_POLICY_2026-11-05.md) / [Lv300 종결급 목표](../LEVEL_300_ENDGAME_POWER_TARGET_V3.md).

## 0. 선택한 기반

| 항목 | 결정 |
|---|---|
| **Minecraft Java** | **1.21.1** (모드팩 **설계 고정**) |
| **Loader** | **NeoForge 21.1.x** (로더 계열 설계 고정; **세부 패치 버전은 의존 파일 설치/실로딩 직전에 확정**) |
| **Java** | **21** 계열 |
| 게임 진행 | 바닐라 채굴·제작·건축·음식·인챈트/XP 보존. **우리 이능 자체 RPG XP/레벨·패시브/자동 성장**, 레벨 300은 사실상 종결급, 300 이상 계속 성장 |
| **금지 정책** | 다른 RPG 모드의 추가 직업/레벨·스킬 트리·독립 고급 재화, 전투 시스템 전면 교체, 스폰·HP 전역 무조건 증가, 중복 레시피 UI/동적 조명/위치 UI |
| **아직 실패 가능** | mod loading/version/transitive dependency 충돌·worldgen 충돌/성능/보스 커스텀 AI가 지배·기생·군체·결계에 반응하는지. **실행하지 않았으므로 성공 주장 금지** |

**왜 NeoForge 1.21.1인가?** 실제 `L_Ender's Cataclysm 3.33`, `Mowzie's Mobs 1.8.2`, `Born in Chaos 1.7.6`이 이 버전에서 직접 릴리스되었고, YUNG's/Mutant/GeckoLib/In Control/JEI/동적 조명·Locator도 존재한다. Fabric 1.21.1에서는 이 핵심 3개 공식 조합을 그대로 가져올 수 없다. 현재 다른 프로젝트의 Fabric/26.2 설정을 자동 상속하지 않음. *NeoForge 21.1.x 안의 하나의 최종 패치 선택은 별도 작은 단계로 남긴다.*

## 1. **설계 확정: 첫 통합 시험에 반드시 포함할 기능 모드 9개**

`설계 확정`은 **모드팩 1차 검증 대상으로 반드시 함께 고려할 고정 목록**이다. 첫 실제 게임 로딩이 실패하거나 정체성/성능이 심각하게 깨지면 **원인 기록·대체 후 설계 잠금 개정**할 수 있다. 원인을 읽지 않은 채 모드를 모두 강제 유지하지 않는다.

| 구분 | 선정 모드 | 1.21.1 NeoForge 확인 빌드/릴리스 | 채택 이유 | 실제 위험과 필수 설정 |
|---|---|---|---|---|
| **보스/던전 A** | **L_Ender's Cataclysm** | **3.33**, 1.21.1 NeoForge [공식 CurseForge](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm) | 후반 다단계 보스·네더/엔드/오버월드 구조물·강력한 장비, Lv300 기준 실보스 타깃 | HP 비례 공격/적당한 피격 상한·원래 보스 페이즈/구조물, 드롭 장비가 클래스 매개체를 압도하는지. **Curios + Lionfish** 필수 |
| **보스/전투 B** | **Mowzie's Mobs** | **1.8.2** [1.21.1 NeoForge 실파일](https://modrinth.com/mod/mowzies-mobs/version/1.8.2) | 독특한 AI·애니메이션과 약점 공략 보스, 강력한 이능에 대한 다른 패턴 시험 | Wroughtnaut 무적·약점 타이밍, Frostmaw 빙결·대상 AI/지배 복원; 고유 무기/권능 중복. **GeckoLib** 필수 |
| **일반/중형 적** | **Born in Chaos** | **1.7.6** [실파일](https://modrinth.com/mod/borninchaos/version/1.7.6) | 바닐라 밤/동굴에 특수 대응을 요구하는 다양한 적 추가 | **초반 스폰 과밀 방지**. 고유 구조물·무기·낙하/폭발·소환 연쇄/AI, 인형·지배체 어그로와 겹침; **GeckoLib** 필수. **Better Combat은 선택 의존성일 뿐 설치하지 않음** |
| **초중반 강적** | **Mutant Monsters** | **v21.1.1-1.21.1-NeoForge** [실파일](https://modrinth.com/mod/mutant-monsters/version/v21.1.1-1.21.1-NeoForge) | 바닐라 익숙한 몹의 특수 전투 패턴, 명확한 강화 몹·장비 보상 | **Chemical X로 만들어진 변종 및 미니언**의 보상 무한 파밍·원래 월드 스폰 밀도 검수. **Puzzles Lib** 필수 |
| **던전** | **YUNG's Better Dungeons** | **1.21.1-NeoForge-5.1.2** [실파일](https://modrinth.com/mod/yungs-better-dungeons/version/1.21.1-NeoForge-5.1.2) | 바닐라 던전 개편, 지하묘지·언데드 요새·거미 동굴; 던전 3중 중복 추가 대신 **한 기본 던전 모드** 채택 | Cataclysm/Born 자체 구조물·계약술사 제단 배치와 빈도/지도/상자 중복. 작은 네더 던전 등 옵션은 원래 설정 확인 후 조정. **YUNG's API** 필수 |
| **스폰/전리품** | **In Control!** | **1.21-10.3.1**, MC 1.21.1 NeoForge [공식 파일 목록](https://www.curseforge.com/minecraft/mc-mods/in-control/files/all?page=1&version=1.21.1) | 외부 강적의 자연 스폰/조건·원래 드롭/바닐라 XP를 한 군데서 조절해 초반 생존 보존 | **우리 자체 RPG XP 이벤트와 이중 지급 X**. 모드 고유 보스 스폰/이벤트·구조물 내부 전용 스폰을 전부 해결한다고 가정 X. 10.3.1은 **Beta 분류**로 실제 실행 게이트가 필요 |
| **멀티 방향 HUD** | **Locator Bar (Reworked)** | **1.2.2+1.21.1-neoforge** [실파일](https://modrinth.com/mod/reworked-locator-bar/version/1.2.2%2B1.21.1-neoforge) | **Classic style를 XP 바 근처**로 설정, 멀티 플레이어 방향 표식. 웅크리기/일부 머리 장비로 숨김 규칙 | 클라/서버 모드 정책·차원·거리·은신/팀·원래 바닐라 XP/별도 RPG HUD 겹침. **PlayerLocatorPlus 동시 설치 금지** |
| **손 횃불 조명** | **LambDynamicLights** | **4.8.9+1.21.1** [실파일](https://modrinth.com/mod/lambdynamiclights/version/4.8.9%2B1.21.1) | 클라이언트에서 들고 있는 횃불 등의 실제 이동 조명 표현 | 서버 블록 light/spawn 억제 기능 **아님**. FPS/셰이더/렌더 호환 검사, 같은 목적 RyoamicLights 중복 금지 |
| **제작법 검색** | **Just Enough Items (JEI)** | **19.70.0.469**, MC 1.21.1 NeoForge [공식 파일 목록](https://www.curseforge.com/minecraft/mc-mods/jei/files/all) | 바닐라/모드 레시피·사용처 검색, **개인별 Ctrl+O로 JEI 목록 숨김**(JEI 자체 제거 아님) | **MezzConfig 최신 요구**. 사용자 만든 인형제작대/제단/매개체의 특수 레시피는 별도 JEI 카테고리/설명 연계 필요. 19.70.0.469는 Beta 라벨, 실제 게임 확인 필요 |

**설계상 기능 모드 9개** + 아래에 적은 **독립 필수 라이브러리 6종 = 15개 주요 파일 그룹(조건에 따라 전이 의존성 추가)**. 모드 이름이 유명하다는 이유만으로 뒤늦게 무엇이든 추가하지 않는다.

### JEI와 레시피북 (확정 요구)

- JEI는 **모두 같은 모드팩을 받아도 각자의 Ctrl+O로 목록 표시 여부 선택**. 비활성화된 것은 패널이지 실제 모드 로딩/서버 레시피가 아님.
- 바닐라 레시피북 자체는 유지. **Crafting Tweaks가 없으므로 책 아이콘을 강제로 숨기지 않는다**. 이전의 '책 아이콘 숨김' 제안은 사용자가 별도 요청하지 않으면 필수 기능 아님.
- MC1.21.1 JEI는 **클라만 설치해도 조회 가능하지만 일부 레시피 자동 재료 배치 등 기능 제한**; 이 게임이 커스텀 레시피 동기화 요구가 있으면 JEI 서버 설치를 추가 검수. 서버에 모든 QOL 모드를 아무 생각 없이 강제 설치 금지.

## 2. 필수 의존성 라이브러리 6종 — **빼먹으면 안 되는 것**

| 모드 | 필수 라이브러리 | 1.21.1 NeoForge 확인 파일/릴리스 | 근거 |
|---|---|---|---|
| Cataclysm | **Curios API** | **9.5.1+1.21.1** [실파일](https://modrinth.com/mod/curios/version/9.5.1%2B1.21.1) | [Cataclysm 제작자 CurseForge 필수 의존성](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm/relations/dependencies) |
| Cataclysm | **Lionfish API** | **3.1** [실파일](https://modrinth.com/mod/lionfish-api/version/3.1) | Cataclysm 제작자 필수 의존성. **자바 21 / NeoForge 1.21.1 맞춤** |
| Mowzie's + Born in Chaos | **GeckoLib** | **4.9.3** [실파일](https://modrinth.com/mod/geckolib/version/Grwn5rUB) | 두 모드의 제작자 필수 의존성. 동일 버전 GeckoLib **하나만** 설치 |
| Mutant Monsters | **Puzzles Lib** | **21.1.56** [실파일](https://modrinth.com/mod/puzzles-lib/version/21.1.56) | [Mutant Monsters 공식 프로젝트](https://modrinth.com/mod/mutant-monsters), NeoForge 1.21.1 양측 필요 |
| YUNG's Better Dungeons | **YUNG's API** | **1.21.1-NeoForge-5.1.9** [실파일](https://modrinth.com/mod/yungs-api/version/1.21.1-NeoForge-5.1.9) | [YUNG 제작자 프로젝트/릴리스](https://modrinth.com/mod/yungs-better-dungeons) |
| JEI 최신 | **MezzConfig** | **0.6.10** [NeoForge 1.21.1 standalone 파일](https://www.curseforge.com/minecraft/mc-mods/mezzconfig/files/9374030) | [JEI 공식 CurseForge Dependencies](https://www.curseforge.com/minecraft/mc-mods/jei/relations/dependencies). **2026 가을 JEI 최신에는 신규 필수 라이브러리**. 기존 계획에서 누락되어 수정 |

- **Curios**는 Cataclysm 필수 의존성으로 설치하되, **우리 게임의 직업 전용 매개체 1슬롯 규칙을 Curios의 임의 추가 장신구 슬롯으로 대체하지 않음**. Cataclysm 장신구의 독립 추가 버프가 우리 매개체/자동 스탯을 압도하지 않게 검사.
- **MezzConfigGUI**는 JEI 필수가 아닌 **선택형**이므로 기본 추가 안 함.
- **CodxLib**는 아래 *후보 보류* 상태의 Alex's Mobs/Caves Continued용이다. 두 모드를 본편에서 제외한 동안 기본 모드팩에 **CodxLib를 넣지 않는다**.
- 15개라는 계산은 **모드 이름 그룹**의 수이며 실제 launcher가 표시하는 jar 숫자, 포함 라이브러리, 런타임 종속성/재배포 허가, hash는 **실제 릴리스 파일의 `neoforge.mods.toml` 및 launcher 의존성 탐색에서 최종 검증**해야 한다.

## 3. **보류·제외 확정** — 범위·밸런스·호환 위험으로 첫 모드팩에서 뺀 모드

| 상태 | 모드 | 제거/보류 근거, 다시 넣을 조건 |
|---|---|---|
| **첫 모드팩 제외(추후 검토)** | **Alex's Mobs Continued** | 1.21.1 NeoForge **실제 출시 존재** ([2.2.2 예](https://modrinth.com/mod/alexs-mobs-continued/version/ThZbQpga), CodxLib≥1.6.0). **모드 호환 불가여서 제외한 게 아님**. 다양하고 많은 생물·모델·도감/독특한 이동·기술의 **기생/지배 대상별 구현·QA 부담이 큼**. 본편 첫 모드팩에서는 제외하고 6직업/타 고유 보스 통합이 안전하게 성립한 이후 **소수 종 고유 행동/지배·기생/성능 검증을 통과할 때** 확장. 2.2.2 개발자 릴리스에 GeckoLib 모드의 Rainbow Jelly 렌더 연동 수정 등 복합 렌더 영향이 기록돼 있어 재시험 필요 |
| **첫 모드팩 제외** | **Alex's Caves Continued** | **중요한 조사 갱신: 원본 Alex's Caves는 1.20.1 전용이었지만 [Continued 공식 포트 프로젝트](https://github.com/Codx-org/AlexsCavesContinued/blob/main/modrinth-description.md)는 NeoForge 1.21.1을 지원**. 즉 **버전 미지원으로 제외하는 건 틀림**. 그러나 6개 희귀 동굴 바이옴/장비/모험 루프·추가 세계생태계가 본 게임의 **새로운 병렬 콘텐츠 진행/월드 복잡도**를 과도하게 늘리므로 초기 제외 |
| **중복 월드젠 제외** | **The Graveyard**, **When Dungeons Arise**, **The Lost Castle** | Born in Chaos/기본 바닐라/YUNG·Cataclysm 구조물+계약술사 16제단 후보와 구조물·보상 밀도/테마 중복. **The Graveyard가 버전 불가라서 제거하는 게 아님**. 지역 탐험을 실제로 해 본 후 던전 밀도가 *부족*한 경우만 하나씩 재평가 |
| **전투규칙 충돌로 제외** | **Epic Fight**, **Better Combat** | 플레이어 공격 처리·모션·피격 구조와 지배 AI/기생 신체/다수 인형·군체/결계가 충돌할 위험. Born in Chaos가 Better Combat을 **선택 의존성**으로 소개하는 것이 '필수'는 아님. 본편은 먼저 바닐라 공격·자체 이능을 보존 |
| **독립 RPG 시스템으로 제외** | **Iron's Spells 'n Spellbooks**, **Ars Nouveau**, **Apotheosis의 독립 성장/던전/전리품 축**, Project MMO류 | 사용자 요구 **별도 주문·직업·레벨/스킬 트리/재화·반복 강화 축 금지**에 충돌. 일부 자체 갑옷 개선 기능만 쓰기 위해 전체 별도 성장 모드를 추가하지 않음 |
| **난도 중첩·기본 생존 파괴 우려 제외** | **Improved Mobs**, **Silent's Power Scale**, **Mob Champions/Champions 포트**, **Scaling Health/Progressive Difficulty 계열** | AI가 건축물/길찾기/기본 월드 안전을 바꾸거나 외부 HP/데미지 스케일러·엘리트 어픽스가 **우리 Lv300 성장/외부 모드 보스 원래 패턴**과 이중 중첩될 수 있음. In Control! 및 우리 게임 XP/보스 데이터 조절만 먼저 사용 |
| **UI 중복 제외** | **PlayerLocatorPlus NeoForged**, **RyoamicLights** | 선택된 Locator Bar / LambDynamicLights와 역할 중복. 플레이어 위치/조명 **각 1개**만. 초기 모드 중 하나가 실제 런타임 실패하면 해당 대안으로 바꾸는 것은 가능 |
| **선택 QOL 보류** | **Crafting Tweaks + Balm** | 바닐라 레시피북 버튼 강제 숨김이 필요하지 않고 JEI는 개별 목록 숨김 가능. 추가 버튼/단축키·Balm 의존성은 처음엔 불필요. 개인 요청/실게임 편의가 확인되면 **클라이언트 개별 설치 가능한 제작 편의**로 재평가 |
| **성능 모드 추가 보류** | Embeddium·FerriteCore·ModernFix·Entity Culling 등 | 실제 렌더/틱 부하·충돌을 재지 않고 무조건 추가하면 새로운 버전/렌더·Mowzie/GeckoLib/동적 조명 호환 문제가 늘어남. **실플레이 프로파일에서 원인이 확인된 영역에 맞춰** 검증 후 1개씩 채택 |

`조건부 후보 보류`는 **본편 최초 설계 잠금에는 포함하지 않는다**. 새로운 컨텐츠 욕심으로 사용자의 **6직업·기존 Minecraft 서바이벌 중심**을 희석하지 않는다.

## 4. 실제 몬스터/보스 스펙 — 출처와 수치 신뢰도를 분리

**단위 HP = 체력 1, 하트 2HP**. 아래 Cataclysm 수치는 **제작자 프로젝트 설명에 공개된 기본 프로필**, Mowzie 수치는 **커뮤니티 가이드/위키 프로필**이며 **1.21.1 선택 JAR의 실제 config/클래스 데이터와 대조한 실측치가 아니다**. 버전·server config, 장비/방어, 보스 기믹/피해상한, 난도 설정에 따라 바뀔 수 있다. 외부 모드 엔티티 상태를 먼저 읽지 않고 **Lv300 밸런스를 이 숫자 하나에만 맞추지 않는다**.

| 외부 모드 | 적 이름 | 공개 기본 HP | 갑옷/공격·기믹 | 출처 신뢰도 및 핵심 영향 |
|---|---|---:|---|---|
| Cataclysm | **Ender Golem** | **150** | 방어 12; 근접/지진/룬 10~16HP 정도(공격 종류별) | [제작자 설명](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm). 중간 강적 시험 |
| Cataclysm | **Ender Guardian** | **300** | 방어 20, 근접 16, 끌어당김/상향타 **대상 최대HP의 5~10%** 추가, 절반 체력에서 헬멧 파괴/방어 감소 | [제작자 설명](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm). 종결 캐릭터의 체력이 높아도 방어/제어 검사 필요 |
| Cataclysm | **Netherite Monstrosity** | **500** | 방어 10, 근접 약 **21 + 대상 HP의 8%**, 용암 충전 중 회복, 33% 미만 분노 | 제작자 설명. 단순 TTK 더미가 아님 |
| Cataclysm | **Ignis** | **450** | 방어 10, 기본 베기 **14 + 대상 최대HP의 5%**, 방패 강타 **21 + 대상 최대HP의 15%** 등, 방어 약화·여러 페이즈 | 제작자 설명. **높은 플레이어 HP를 자동으로 통과하는 비례 공격** 때문에 본 게임 Lv300 HP만 상향해선 종결감 X |
| Cataclysm | **The Harbinger** | **390** | 방어 12, 미사일 8, 레이저·충돌시 **대상 HP의 5~6%** 항목, 절반 HP 아래 투사체 면역/특수 대응 | 제작자 설명. 군체 투사체/기생·지배/결계와 충돌 가능한 고유 보스 |
| Mowzie's Mobs | **Frostmaw** | **250** | 기본 할퀴기 약 10HP, 얼음/빙결; 체력 60% 이하 강력한 **Slam 약 40HP**, 원래 화살 면역 등의 특수 규칙 | [2026 커뮤니티 가이드](https://www.minecraft-guides.com/mod/mowzies-mobs/wiki/frostmaw/), [제작자 모드 페이지](https://modrinth.com/mod/mowzies-mobs), **현행 JAR 기본 수치 실측 아님** |
| Mowzie's Mobs | **Ferrous Wroughtnaut** | **40** | 일반 시기 거의 모든 피해 차단, 특정 후방 취약시간(대략 3초)만 유효, 기본 휘두르기 약30HP, 쓰러뜨리면 특수 도끼 | [2026 커뮤니티 가이드](https://www.minecraft-guides.com/mod/mowzies-mobs/wiki/ferrous-wroughtnaut/). **HP40을 '쉬운 보스'로 잘못 분류 금지** |

**스펙 발견 → 게임 기획에 주는 의미**
1. Cataclysm **최대HP 비례 피해**는 무제한 레벨 기본 HP만 키워 무적이 되는 설계를 구조적으로 막는다. 직업별 방어/회피/숙주 특성/지배체 분산/인형 엄호/결계/군체 소모가 실제 살아 있어야 한다.
2. Wroughtnaut **40HP**는 Ignis **450HP**보다 쉬운 보스라는 뜻이 아니다. **취약창/무적/위상·패턴**을 보유하므로 기본 HP×DPS = 보스 난이도 정의는 실패. 기생·지배/군체 공격으로 약점을 무시해 즉사시키는 편법도 방지하되 **기술 자체를 전면 면역 처리 X**.
3. Mowzie 보스의 **빙결/넉백/보스 체력 재설정·원래 드롭 조건**과 Cataclysm 보스의 **돌진/넉백/화염/쉴드·레이저**는 강력한 이능에게 실제 전투 동작을 요구. 외부 boss HP를 10배로 늘리는 무식한 레벨 스케일링 대신 기존 기믹을 살린다.
4. **Born in Chaos / Mutant** 개별 1.21.1 몬스터의 HP/공격/스폰 표는 **이번 단계에서 특정 실 JAR의 정확한 데이터로 추출·검증하지 못함**. 기본 스폰 과밀은 사용자 불편을 만들므로 **정확한 entityTypeId·spawnWeight·biome/structure·HP·공격은 설치 후 해당 파일/등록 속성·config·/attribute**로 표본 추출. 숫자 임의 제작 금지.
5. 외부 몹의 **드롭·무기/방어구**는 독립 캐릭터 레벨이 아니므로 무조건 폐기하지 않는다. 하지만 클래스 매개체/계약·인형·숙주 고유 전력과의 `+스탯` 중복/최상위 장비 빠른 파밍을 검사. 게임 전용 클래스 성장 시스템을 대체하는 효율이면 드롭·제작 수급/상호 작용을 데이터로 재조정한다.

## 5. 외부 모드 때문에 게임을 망치지 않는 **실제 통과 게이트**

| 게이트 | 체크할 실제 상황 | 실패 시 올바른 수리/제거 결정 |
|---|---|---|
| **L1 의존성/버전** | **21.1.x NeoForge + Java21**, 위 9개 기능 + 필수6개 모드에 정확히 MC1.21.1 NeoForge 파일 사용, 클라/전용서버가 실행되는가 | 누락 Curios/Lionfish/Gecko/Puzzles/YUNG API/MezzConfig·재귀 dependency 해결. **다른 loader의 1.21.x jar 사용 금지**. 원인 읽기 전 modpack 성공 주장 X |
| **L2 월드/구조물** | 새 월드에서 Cataclysm·YUNG·Born 고유 구조물이 생성되는가, 계약술사 예정 16제단/마을/바닐라 구조물이 탐색 가능한가 | 스폰/구조물 희귀도·태그/지형/loot 테이블 데이터 조정; 빽빽한 대형 던전·모든 바이옴 야간 위협 배제. 스폰률 0을 말없이 만들어 핵심 콘텐츠 삭제 금지 |
| **L3 시작 생존 보호** | 첫 밤/초기 채굴/초급 스킬·기초 마리오네트/첫 계약/기생 숙주가 가능한가; Born+Mutant가 몰려와 플레이가 불가능하지 않은가 | In Control! 등으로 **자연 출현 장소·밀도 분배**; 도전형 강적은 희귀/구조물에 집중, 바닐라 몹 의미도 보존 |
| **L4 보스 원형** | Mowzie Wroughtnaut 약점·Frostmaw 빙결/페이즈, Cataclysm Ignis 페이즈·최대HP 비례 피해/방어·Harbinger 원래 스크립트 정상 | 단순 Boss AI 교체/전면 면역 금지. **강력 정신 지배·기생도 성공 조건/유지비/보스 상태·AI 복원 어댑터와 연동**. 실체 손실/보상 중복 등 버그 시 패턴별 어댑터 |
| **L5 직업 상호작용** | 6직업의 지배·인형 소환/보상 귀속·기생 숙주 고유 스킬·결계 영억 충돌·계약 권능/가호·군체 적중/투사체 엄호/비행의 **실제 외부 엔티티 판정** | 실제 API·entityTypeId별 지원/명확한 제한/고유 기술 적응; 불특정 외부 boss에 **모든 기술 자동 작동 성공**이라고 주장 금지. 미지원이면 명확 표시·다른 지원 대안/보호 제공 |
| **L6 성장 독립성·보상** | 바닐라 XP/인챈트 보존, 별도 우리 RPG XP만 서버에서 관리, 외부 mod 추가 레벨/재화/스킬 트리 없는가. Chemical X/소환 NPC를 반복 처치해 RPG XP 대량 파밍하지 않는가 | 변이체/소환체의 XP 귀속·자연 적/유효 전투 보상 구별, 전리품/제작·아군 판정 조정, 무제한 레벨과 자동 파워의 과도한 곱승 방지 |
| **L7 UI/QOL** | Locator Classic XP 표시 vs 바닐라 XP·RPG HUD 충돌, crouch invisibility rules; JEI Ctrl+O 숨김/레시피 R/U, 손 횃불 동적 광원 | 다른 위치/광원/JEI 중복 패키지 X. 클라 개인 설정 보존. JEI 19.70 최신 **MezzConfig** 포함. 렌더·쉐이더 충돌 시 겹치는 후보 제거 |
| **L8 성능/멀티** | 서버 2/8/20인·인형 여러 기/지배체/군체 대군단/던전 생성/외부 몹/부서진 블록/보스·VFX 실제 MSPT/클라 FPS/패킷/재접속 저장 | 실제 profiler로 병목 확인·최대 활성 AI/군체 렌더·패킷 빈도/월드젠 분산 최적화. **JAR 존재·빌드 성공만으로 플레이·멀티 성공 아님** |
| **L9 배포권한/저작권** | 원본 다운로드 URL/기본 라이선스/정확한 파일 해시/사용 조건을 기록, 직접 jar를 repo로 재배포할 권한 있는가 | 기본은 **모드팩 매니페스트/다운로드 레퍼런스**와 자체 모드 JAR만 배포. Cataclysm의 별도 라이선스·Born ARR 등은 모델·코드/자산 복제·재배포 자동 허가로 간주 X |

**기술적 중요 경계:** 동일한 `1.21.1 NeoForge` 파일이 존재한다는 사실은 **함께 부팅된다**는 실증이 아니며, 둘이 로딩된다는 사실도 **특정 보스가 직업 능력의 숙주/지배체가 된다**는 보증이 아니다. 이 문서의 채택 결정은 **설계 수준 최종 선정**으로만 주장한다.

## 6. 다음으로 필요한 실제 작업(정본 개발 순서)

1. 이 문서의 기본 **9기능+6라이브러리 설계 잠금**과 `1.21.1 NeoForge` 계열을 PROJECT에 반영, 기존 '미선정'은 날짜가 지난 당시 기록으로 구분. **정확한 NeoForge patch, 정확한 각 jar 해시, 1.21.1 버전 배포 권한** 확인.
2. **실제 15그룹 파일을 같은 Modrinth/CurseForge 런처 인스턴스에 설치해 로드 테스트**. Java21 클라→동일한 콘텐츠 모드 전용 서버→새 월드에서 대표 몬스터/구조물 스폰/loot·손 횃불/JEI/Locator→내구·기본 공격·월드젠 프로파일. **현재 이 단계는 NOT TESTED**.
3. 실제 파일에서 **보스/일반/변이 EntityType 목록 + 본래 HP/공격/방어/기믹/드롭·스폰/차원** 표를 뽑아 6직업 능력과 매칭. 공식 웹의 2026 공개 수치는 초깃값/reference이지 runtime measured가 아니다.
4. 6직업 Lv300의 유효 DPS(5초 burst/30초 지속)/유효 생존/소환체 수·손실/제단·BD·비행/압축 결계의 실전 전력 수치 모델을 작성. **Lv300 종결급을 먼저 만들고 1~300 역성장**. 자동 성장 일괄 `÷3` 폐기 유지.
5. 실제 인게임 밸런스·모드 충돌 결과에 따른 **원인 있는 1개씩 변경**만 적용. 선정 원칙은 지키되 증거 없이 무조건 '모두 호환성 통과'라고 쓰지 않는다.

### 조사 출처 인덱스 (공식 우선)

- Cataclysm [릴리스/제작자 보스·스펙 설명](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm), [필수 관계](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm/relations/dependencies); 3.33 NeoForge/Curios·Lionfish.
- Mowzie [1.8.2 NeoForge](https://modrinth.com/mod/mowzies-mobs/version/1.8.2)와 [Frostmaw](https://www.minecraft-guides.com/mod/mowzies-mobs/wiki/frostmaw/), [Wroughtnaut](https://www.minecraft-guides.com/mod/mowzies-mobs/wiki/ferrous-wroughtnaut/) 커뮤니티 스펙(인게임 검증 필요).
- Born [1.7.6](https://modrinth.com/mod/borninchaos/version/1.7.6), [필수 GeckoLib/선택 Better Combat](https://www.curseforge.com/minecraft/mc-mods/born-in-chaos/relations/dependencies).
- Mutant [1.21.1](https://modrinth.com/mod/mutant-monsters/version/v21.1.1-1.21.1-NeoForge), [Puzzles Lib 필수](https://modrinth.com/mod/mutant-monsters), [설정·Chemical X](https://mcmodwiki.com/mutant-monsters/1.21.1).
- YUNG's Better Dungeons [1.21.1](https://modrinth.com/mod/yungs-better-dungeons/version/1.21.1-NeoForge-5.1.2), [YUNG's API](https://modrinth.com/mod/yungs-api/version/1.21.1-NeoForge-5.1.9).
- In Control! [원본 동작 설명](https://modrinth.com/mod/in-control), [1.21.1 NeoForge 릴리스](https://www.curseforge.com/minecraft/mc-mods/in-control/files/all?page=1&version=1.21.1).
- Locator Bar [XP/classic/플레이어 숨김](https://modrinth.com/mod/reworked-locator-bar), [1.2.2 NeoForge 릴리스](https://modrinth.com/mod/reworked-locator-bar/version/1.2.2%2B1.21.1-neoforge); Lamb [4.8.9 동적 조명](https://modrinth.com/mod/lambdynamiclights/version/4.8.9%2B1.21.1).
- JEI [제작자 1.21.1 클라 단독 허용 FAQ](https://modrinth.com/mod/jei), [JEI CurseForge 필수 MezzConfig](https://www.curseforge.com/minecraft/mc-mods/jei/relations/dependencies), [MezzConfig 0.6.10 NeoForge](https://www.curseforge.com/minecraft/mc-mods/mezzconfig/files/9374030).
- 참고/범위 재평가: [Alex's Caves Continued 1.21.1 공식 포트 설명](https://github.com/Codx-org/AlexsCavesContinued/blob/main/modrinth-description.md), [Alex's Mobs Continued NeoForge 2.2.2](https://modrinth.com/mod/alexs-mobs-continued/version/ThZbQpga).

**정확한 검증 상태: RESEARCHED / DESIGN LOCKED / RELEASES AND DEPENDENCIES PUBLICLY VERIFIED / NO MOD FILE INTEGRITY HASHES VERIFIED / COLOAD NOT TESTED / BOSS NUMBERS NOT RUNTIME MEASURED / CLASS COMPATIBILITY NOT TESTED / NO JAR OR MODPACK BUILT / NO PLAYTEST OR MULTIPLAYER.**
