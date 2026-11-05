# 《이능 직업 서바이벌》 외부 모드 후보 조사 — 생존 난이도·몬스터/보스·월드·호환성

> 조사일 **2026-11-05** / **RESEARCHED FROM PUBLIC PROJECT/FILE LISTINGS, NOT INSTALLED OR CO-LOADED**. 정본은 [PROJECT](./PROJECT.md), [GAME_DESIGN](./GAME_DESIGN.md), [기획 통합 감사](./DESIGN_CONSOLIDATION_AUDIT_2026-11-05.md). **모드 버전과 로더는 아직 선정하지 않았음**. 외부 페이지의 지원 범위가 넓더라도 **각 Minecraft 버전/로더 조합의 실 다운로드 파일·종속 라이브러리와 게임 내 동작은 별도 검증**한다. **중력술사·공명술사 상세 설계는 사용자 지시로 보류**, 8직업 목록에서 삭제하지 않는다.

## 요약 권고 (추천이지 사용자 최종 승인 아님)

- 현재 확인된 '특색 있는 강한 보스 + 야외 적 + 던전 + 유연한 스폰/보상 조절' 조합에서는 **Minecraft 1.21.1 + NeoForge**를 **유력한 첫 후보**로 둔다. **1.20.1 + Forge**는 기존 대형 몹/동굴 생태계 대안, **1.21.1 + Fabric**은 Fabric 원본 보스/구조물 생태계 대안, **26.2**는 일부 모드의 실제 지원이 있으나 대표 보스 라이브러리 선택 폭이 다름. 최종 버전/로더 결정 전 모드를 설치·구현 확정하지 않는다.
- **1차 우선 비교군:** `L_Ender's Cataclysm` + `Mowzie's Mobs`(고품질 보스), `Born in Chaos`(다양한 일반/중간 적), `YUNG's Better Dungeons`(던전), `In Control!`(적 스폰/드롭/경험치 제어).
- **확장 비교:** `The Graveyard`(야외/던전 언데드), `Mutant Monsters`(익숙한 적 강화), `Alex's Mobs Continued`(다양한 숙주·지배 후보), `Improved Mobs` **또는** `Silent's Power Scale`(난이도 조절 방법 중 1개 먼저 테스트). 선택 초기부터 모두 다 넣지 않는다.
- **일단 보류:** `Epic Fight`(전투 전반을 변경해 지배/기생/군체/결계 판정과 충돌 위험), `Iron's Spells 'n Spellbooks`(자체 100개 이상 주문/성장이 우리 6직업 정체성을 압도할 수 있음), `Apotheosis` 전체 성장 시스템(우리 레벨/장비와 중첩 위험), `Better Combat`(가능성 있지만 실제 전투 판정 호환 테스트 후), `Alex's Caves`(1.20.1 계열 버전/로더 목적이 결정된 경우에만 비교).
- **라이선스:** 직접 jar를 **일반적인 모드 로딩 의존성으로 쓰는 것**과 타인의 몹 모델/코드를 **이 프로젝트 공개 리포에 복제/수정·배포하는 것**은 구분. Cataclysm CC-BY-NC-ND / Mowzie 전용 라이선스 / Born in Chaos ARR 등은 특히 **직접 자산 재사용 불허를 기본값**으로 해석하고 실제 허가/조건을 확인한다.

## A. 보스·몬스터·생물 후보

| 후보 (공식 프로젝트) | 확인한 Minecraft/loader 근거 | 우리 게임의 역할 | 중요 위험 / 판정 | 라이선스 페이지 표시 |
|---|---|---|---|---|
| [**L_Ender's Cataclysm**](https://www.curseforge.com/minecraft/mc-mods/lendercataclysm) | **1.21.1 NeoForge** 배포 파일 확인; 1.20.1 Forge 존재 | **중후반 고난도 던전/보스/장비**, 능력 활용의 큰 목표 | 고유 보스 phase·script/광역 공격/타깃 강제. 지배·기생·정지 CC 성공·실패·해제/보스 원상 복귀 GATE. **설치가 곧 지배 호환 의미 X** | Modrinth **CC-BY-NC-ND-4.0**; CurseForge custom |
| [**Mowzie's Mobs**](https://modrinth.com/mod/mowzies-mobs) | **1.21.1 NeoForge 1.8.2** CurseForge 실제 파일; 1.20.1 Forge 1.8.2 | 보스 개성/전투 텔레그래프/애니메이션 품질 레퍼런스. Frostmaw·Ferrous Wroughtnaut 등 | Wroughtnaut 특정 취약점/보스 고유 공격과 우리 군체 피해/결계·지배 CC가 충돌할 수 있음. 고유 animation/AI 보존 전제 | **Mowzies Mobs License** |
| [**Born in Chaos**](https://modrinth.com/mod/borninchaos) | 프로젝트 페이지 **1.21.1/1.20.1, Forge/NeoForge**, GeckoLib 요구 | 야간·동굴 일반/중형 적 확대: 기존 좀비/스켈레톤보다 공격/특수행동 다양함 | 스폰 과밀/탐험 피로, 다수 효과/소환수; 일부 특수 공격 모드 API 검증. 개별 버전/로더 jar 확인 필요 | **ARR** |
| [**Mutant Monsters**](https://modrinth.com/mod/mutant-monsters) | **1.21.1 NeoForge** 파일 직접 확인, Fabric/Forge/NeoForge 다중 버전 | 바닐라 익숙한 몬스터의 강력 변종, 초중반~중반 미니보스 | 공격 범위와 지배·기생 숙주 허용/기술 발동/실제 드롭을 검증. 초반 대량 스폰 금지 | **AGPL-3.0-only** |
| [**The Graveyard**](https://modrinth.com/mod/the-graveyard-fabric) | 1.21.1 Fabric/NeoForge, 26.2/26.3 포함 여러 버전 | 폐허·묘지·지하묘지 탐험, 언데드와 Corrupted Champion 보스 | Born in Chaos와 야간/언데드 스폰·구조물 중복. 다수 사용 시 스폰 희소도 조정 | **GPL-3.0-only** |
| [**Alex's Mobs Continued**](https://modrinth.com/mod/alexs-mobs-continued) | **1.21.1 NeoForge 개별 파일 확인**, Fabric/Forge와 26.2 다중 버전, **CodxLib 요구( Fabric은 Fabric API도)** | 다양한 동물/몬스터를 지배·기생 숙주/생물 다양성으로 제공, 탐험/수중 행동 후보 | **공식 원본이 아닌 커뮤니티 후속 포트**. 항목별 권한, mount/render/특수 AI·NPC 관계가 작동하는지 회귀 필요. **동일 목적의 다른 Alex's Mobs 비공식 포트와 중복 설치 금지** | **GPL-3.0-only** |
| [**Bosses of Mass Destruction**](https://modrinth.com/mod/bosses-of-mass-destruction) | **1.21.1 Fabric 원본**, [비공식 Forge/NeoForge port](https://modrinth.com/mod/bosses-of-mass-destruction-forge) 존재 | 네 개의 고유 보스/Night Lich·Obsidilith·Nether Gauntlet·Void Blossom, 추가 엔드/네더 목표 | Fabric 원본과 Forge/NeoForge 포트는 **별개 배포물/종속성**, 상호 호환성·라이선스·보스 상태 원상 복귀 재검증. 차원 콘텐츠 늘어남 | **LGPL-3.0-only** |
| [**Alex's Caves**](https://modrinth.com/mod/alexs-caves) | **1.20.1** Forge 중심, 프로젝트 페이지 NeoForge 표기는 파일별 확인 필수; **Citadel 요구** | 여섯 희귀 동굴 바이옴/몬스터·광물·탐험 체험, 1.20.1 대안의 강점 | 목표가 1.21.1이면 바로 채택 불가. 독자 기능/자원 루프와 과도한 신규 시스템 중첩 주의 | **LGPL-3.0-only** |

**검증 근거**
- Cataclysm: https://www.curseforge.com/minecraft/mc-mods/lendercataclysm/files/all?page=1&version=1.21.1 / https://modrinth.com/mod/l_enders-cataclysm
- Mowzie 1.21.1 NeoForge 파일: https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs/files/all?page=1&version=1.21.1
- Born in Chaos: https://modrinth.com/mod/borninchaos (페이지에 GeckoLib 의존·몬스터별 상세 설명 포함)
- Mutant 1.21.1 NeoForge: https://modrinth.com/mod/mutant-monsters/version/v21.1.1-1.21.1-NeoForge
- The Graveyard: https://modrinth.com/mod/the-graveyard-fabric
- Alex Continued: https://modrinth.com/mod/alexs-mobs-continued/versions
- Bosses of Mass Destruction(원본/비공식 포트): 위 링크
- Alex Caves: https://modrinth.com/mod/alexs-caves

## B. 던전/월드 구조물 — '강한 적을 찾을 이유'

| 모드 | 확인 지원·주요 기능 | 채택 판단 |
|---|---|---|
| [**YUNG's Better Dungeons**](https://modrinth.com/mod/yungs-better-dungeons) | **1.21.1 NeoForge/Fabric 실제 파일**, 1.20.1 지원; 원래 던전 리디자인, Catacombs/Fortresses of the Undead/Spider Caves. **YUNG's API 요구** | **유력 기본 던전**. 기존 바닐라 채굴/탐험과 자연스럽게 연결, 다른 월드젠/대형 던전과 밀도 중복·보상 상충 점검 |
| [**When Dungeons Arise**](https://modrinth.com/mod/when-dungeons-arise) | 1.21.1/1.20.x, Fabric/Forge/NeoForge 프로젝트 지원. **30종 이상** 대형 건축물/던전; Fabric 배포의 Cloth Config/Fabric API 주의 | **선택형 확장**. 대형 구조물이 너무 많으면 원래 마크 지형/탐험 리듬이 파괴될 수 있으므로 구조물 빈도/전리품 제한 |
| [**The Lost Castle**](https://modrinth.com/mod/the-lost-castle) | 1.21.1 등 Fabric/Forge/NeoForge 지원 표기, 일종의 대형 일회성 성채 탐험 목표 | 구조물 한 종류라 목표/탐험 동기를 더 선명하게 만드는 확장. 파일별 loader/버전 확인 후 적용 |
| **Cataclysm 자체 던전/Graveyard 구조물** | 원래 보스 모드가 별도 구조물을 자체 생성 | 구조물 모드 추가 이전부터 월드 생성량·희귀도·멀티 탐험 거리를 합산해 계획 |

근거: https://modrinth.com/mod/yungs-better-dungeons , https://modrinth.com/mod/when-dungeons-arise , https://modrinth.com/mod/the-lost-castle

## C. 난이도/밸런스 제어 — 중복 없이 한 세트로

| 모드 | 정확한 역할 | 1.21.1 NeoForge 상황 | 판단 |
|---|---|---|---|
| [**In Control!**](https://modrinth.com/mod/in-control) | 서버 설정으로 **몬스터 스폰, 특성 일부, 전리품, 경험치**를 규칙 기반으로 조정. config/incontrol 의 spawn/loot/experience 등 | 프로젝트 페이지에 1.21–1.21.1 NeoForge 존재, 현재 26.2/26.3도 지원 | **가장 우선**, 직업 레벨·등급·보스/차원에 맞춘 스폰/보상 연결에 유리. 모드가 모든 보스의 고유 phase/공격 자체를 밸런싱하진 않음 |
| [**Improved Mobs**](https://modrinth.com/mod/improved-mobs) | 몹의 아이템 활용·길찾기·일부 블록 파괴/전투 AI 강화, 난이도 동적 관리 | **1.21.1 NeoForge 실제 파일** 및 설정/업데이트 확인 | **조건부**: 건축 파괴/동굴 안전성·소환물·AI 타깃을 바꾸므로 기본 활성은 신중. 필요 기능 일부만 켜서 시험 |
| [**Silent's Power Scale**](https://modrinth.com/mod/silents-power-scale) | 1.21.x NeoForge에서 **적/플레이어 속성·체력 등 난이도 수치 보정** | 1.21~1.21.1 NeoForge 확인 | **선택 1안**: 우리 자체 레벨별 스탯 보정과 **곱/중복**되지 않게 속성 책임 분담이 필요. 처음부터 이 모드+다른 성장 모드를 중첩 X |
| [**Mob Champions**](https://modrinth.com/mod/mobchampions) | 바닐라/기존 몬스터의 일부를 엘리트 버전·장비·강화·보상으로 등장 | **1.21.1 NeoForge 실제 파일**, MIT 표기 | **선택 2안**: 희귀 강적에 선택을 더하는 방식으로 적합, 그러나 유효 드롭·기존 방어 보스/선행 룹과 중복 검증 |
| [**Champions-Unofficial**](https://modrinth.com/mod/champions-unofficial) | 엘리트 접두효과/희귀도/전리품 | 1.21.1/26.2 여러 loader 포트, **limited testing이라고 제작자가 명시** | 정식 우선 후보 아님, Mob Champions와 목적 중복·재현 테스트 없으면 추가 X |
| [**Majrusz's Progressive Difficulty**](https://modrinth.com/mod/majruszs-progressive-difficulty) | 게임 진행에 따라 Normal→Expert→Master 등 단계적인 난이도 | **원본은 1.20.1까지**. 1.21.1은 **별도 비공식 Continued 포트**가 있으므로 원본과 혼동 금지 | 1.20.1 분기에서만 유력; 1.21.1 채택 시 비공식 포트의 실제 파일·버그·라이선스 별도 확인 |
| [**Scaling Health**](https://modrinth.com/mod/scaling-health) | 플레이어/몹 체력·난이도 점증 | **1.20.1 Forge까지**, 제작자도 1.21+에서 Silent's Power Scale 권장 | **1.21.1 제외**. 1.20.1용이라도 전용 XP/기본 HP와 중복 효과 우려 |
| [**KubeJS**](https://modrinth.com/mod/kubejs) + [**LootJS**](https://modrinth.com/mod/lootjs) | 레시피·태그·서버 데이터/아이템/전리품 스크립트 변경 | 1.21.1 NeoForge/Fabric/Forge 릴리스/프로젝트 지원 | 전리품·제작 규칙을 데이터로 빠르게 시험할 **개발·모드팩 구성 도구 후보**. 이 모드의 게임 핵심 판정·서버 저장을 전부 JS 스크립트에 맡기지 않음 |

근거: https://modrinth.com/mod/in-control , https://modrinth.com/mod/improved-mobs/version/1.21.1-1.16.0-neoforge , https://modrinth.com/mod/silents-power-scale , https://modrinth.com/mod/mobchampions/version/1.21.1-1.0.2.2-NEOFORGE , https://modrinth.com/mod/majruszs-progressive-difficulty , https://modrinth.com/mod/scaling-health , https://modrinth.com/mod/kubejs , https://modrinth.com/mod/lootjs

## D. 버전·모드 로더 비교 (프로젝트 결정 전)

| 후보 구성 | 입증된 장점 | 남는 핵심 비용/리스크 | 1차 판단 |
|---|---|---|---|
| **1.21.1 NeoForge** | Cataclysm 1.21.1 NeoForge(개별 파일), Mowzie's 1.8.2 NeoForge(개별 파일), Born in Chaos, Mutant Monsters, Graveyard, YUNG's Better Dungeons, In Control!, Silent's Power Scale 등 | 여러 고급 보스의 각기 다른 AI/단계별 피해/렌더·지배/숙주/결계/군체 피격 어댑터 구현 비용 | **가장 유력한 본편 콘텐츠 후보** |
| **1.21.1 Fabric** | Bosses of Mass Destruction Fabric 원본, The Graveyard, Mutant Monsters, YUNG's, In Control!, Alex Continued | **Cataclysm/Mowzie's/Born in Chaos의 확인된 공식 Fabric 릴리스 없음**. 보스 품질/양을 다시 선정 | **멀티 편의 등 별도 장점이 큰 경우 대안** |
| **1.20.1 Forge** | Cataclysm/Mowzie's/Born in Chaos 및 Alex's Caves 원본, 오래된 몹/구조물/모드팩 선택 폭 | 이전 API/Java·유지보수/최근 기능 지원·새 게임 버전 기획의 비용 | **동굴/기존 생태계 우선이면 대안** |
| **26.2 NeoForge/Fabric** | Alex's Mobs Continued, Mutant Monsters, Graveyard, In Control!, Better Combat 등 **실제 최신 버전 파일 확인** | Cataclysm/Mowzie's 등 핵심 보스는 확인된 최신 릴리스 1.21.1까지, Bosses of Mass Destruction 원본 1.21.1까지. 모드 생태계가 완전히 같은 것이 아님 | **후속 평가**, 최신 버전 자체가 핵심 목표가 되지 않음 |

**주의:** `프로젝트 페이지가 Fabric Forge NeoForge 모두 표기`하는 것은 **모든 MC 버전에서 모든 loader 파일이 존재한다는 뜻이 아니다**. 실제 선택 전 추천 목록의 각 jar 버전/필요 라이브러리/로딩 충돌을 확인한다. 특히 `Alex's Mobs Continued`는 **CodxLib**, `Born in Chaos`는 **GeckoLib**, `YUNG's Better Dungeons`는 **YUNG's API**, Bosses of Mass Destruction 포트는 별도 API/GeckoLib/Cloth Config 등 요구. **정확한 해당 빌드의 의존성은 설치 직전 다시 확인**.

## E. 여섯 직업과 외부 몹의 상호작용 가능성 테스트

| 직업 | 해당 후보가 보강하는 핵심 재미 | 모드 설치만으로 해결되지 않는 기술 위험 / 검증 항목 |
|---|---|---|
| **정신 지배자** | 일반 잡몹→특수 패턴 몹→강력한 보스 지배 도전, 강적 활용 | 고유 target goal·보스 arena/phase/attack script/주민 거래/특수 드롭을 **지배 후 원상 보존**할 수 있는가? **보스/몬스터 종류만으로 절대 지배 불가 금지**, 높은 난도·유지비로 조정하되 기술 미지원은 검증해 명시 |
| **인형술사** | 다수 적/좁은 던전/보스 등 지형별 인형 선택·재료 획득 | 몬스터가 인형에 실제 어그로를 걸고 때리는가, 인형·팀원·다른 소환물 간 피격·주문 오폭 방지, 협소 구조물 pathfinding |
| **기생술사** | 넓은 야생 생물/특수 이동/강한 몹을 숙주로 체험, 종별 숙련·BD 목표 | **특수 애니메이션/고유 공격/외부 AI**, 탑승/변신 시 플레이어 카메라·인벤토리·지형/저장과 실제 연동 가능한가? 보스/특수 종류는 정확한 종별 어댑터 없으면 '모두 완벽히 조종 가능' 선언 금지 |
| **결계술사** | 적의 광역/돌진/원거리 공격에 방어·압축·격리 결계로 대응 | 고유 공격의 실제 충돌 범위, 보스 단계별 이벤트·지속/다단 피해/면역화 방지, 큰 보스 히트박스가 결계를 통과하는지 |
| **계약술사** | 어려운 던전/보스 탐험→제물·관계 심화로 새로운 권능/등급/탐험 보상 연결 | 제단 16종과 외부 구조물의 과밀/중복 스폰, 외부 드롭으로 제물 경제 붕괴/반복 파밍, 해당 신성/마법/실체 권능과 몬스터 공격 충돌 |
| **군체술사** | 소형 실제 생물 다수가 원격 공격·상시 엄호·운반 비행, 광역 적/투사체와 전투 | 적의 target acquisition이 군체를 실제 공격하는가? 화살이 **보이는 군단**과 실제 교차해 손실되는가? 파티 화살/기생·지배 동료와 오폭, 비행군 피해·안전착륙, 0.2블록 모델·LOD/대군단 TPS |

- **'외부 모드가 설치된다'**는 첫 단계. **'보스가 지배/기생/CC 가능하다'**는 **별도의 기능 통합 구현 및 런타임 테스트를 거쳐야 하는 결과**다.
- 보스의 **기믹을 무효화하는 무제한 지배/무효 피해**도, '보스니까 절대 면역'도 지양. 사용자 승인 난도/정체성에 맞는 **유지비·성공 판정/제어권/상태 복원/고유 스크립트와의 충돌**을 해결해야 함.
- 특히 `Mowzie's Mobs`의 **Ferrous Wroughtnaut**는 특정 약점 외 피해가 성립하지 않는 보스. 군체/결계/정신 정지로 피해 규칙을 실수로 우회하거나 영구 봉쇄할 우려. `Born in Chaos`는 몬스터 소환·장갑·광역·상태 효과 타입이 다양함. 따라서 본격 편입 전 대표 개체·패턴별 시나리오로 나눠 확인.
- 스폰/난도는 **처음 1일/밤/동굴 입문이 사망 반복이 되지 않게**. 강적은 지형·구조물/탐험·정해진 성장 맥락과 연결하고 '스폰율 3배, 적 HP 10배'식 무차별 난도 상승 금지.

## F. 제안하는 실제 난이도/콘텐츠 배치

**초기(바닐라 생존 + 직업 첫 스킬):** 기본 적/구조물의 출현은 유지하되, 이능을 사용해 얻는 실질 선택이 있어야 한다. 초반 1일차부터 Born in Chaos/Mutant/Graveyard 모든 적을 몰아 스폰하지 말 것. 선택 1개 지역/이벤트에서 강화 적 조우.

**중반(능력 성장/장비 변조·변칙 단계 시험):** YUNG 던전·Mutant/Graveyard/선별된 Born in Chaos를 통해 여러 전투 유형(다수·원거리·방어·기동·소환)을 경험. 실질 XP/아이템/제물·인형 소재/BD 성장을 연결; 적 주먹질만 세지지 않게 한다.

**후반(본격 특수전/불가해 장비와 장기 성장):** Cataclysm/Mowzie 고급 보스와 장기 모드 보스 조우. 보스 단계/멀티 협동/정확한 난도·성장 보상 조절. 강력 보스의 지배·기생 기획은 '불가능' 금지가 아니라 **극도로 높은 요구치 + 기믹/상태 보존 + 실제 제어 어댑터** 문제로 처리.

**무제한 레벨 이후:** 클래스 능력/개체 스탯의 질적 성장과 몬스터 출현 조건/전투 챌린지의 선택을 늘리되, 월드 생성 보스/기본 스폰이 레벨에 비례하여 무한 능력치로 폭주하거나 동접 20인이 동일 지역의 스폰/서버 TPS를 무한 증가시키지 않게 함.

**아직 결정하지 않은 것:** 실제 레벨 컷/보스 출현 지역, 필수/선택 구조물, 차원/네더/엔드 필수 여부, XP 계산/최초 마나, 드롭 표/데이터 JSON, 세계관·주요 게임 제목. 이번 조사 결과로 임의 확정 금지.

## G. 현실적인 3단계 의존성 도입 시험 (이 게임 구현 전에 모드 선정 단계)

1. **A — 조합 후보 로딩 검증**: **1.21.1 NeoForge**에서 **In Control! + YUNG's Better Dungeons + Mowzie's Mobs + Cataclysm** 정도를 우선 최소 구성으로 시험. 필수 라이브러리만 정확히 포함. 동일 Minecraft 버전/loader, 종속성 해결, 전용 서버 부팅/클라 부팅, 구조물 생성과 보스 스폰이 되는지 확인. **아직 실제로 실행하지 않음**.
2. **B — 몬스터/난도 확장 통합**: Born in Chaos·The Graveyard·Mutant Monsters·Alex's Mobs Continued를 **하나씩 추가**하고 충돌/스폰 밀도/메모리·TPS·실제 루프를 비교. In Control!로 스폰·드롭·XP 조정. 대형 생물/동일 목적 비공식 포트의 중복 주의.
3. **C — 특수 클래스/외부 보스 기능 검증**: 해당 몬스터 한 마리를 대상으로 AI 지배/기생/결계·군체 화살 등 **고위험 수직 슬라이스 기술 실험**. 이 단계는 A/B가 스펙상 설득력 있고 **6직업 공통 게임 기획/성장·경제 정본 통합 뒤** 수행. Epic Fight/Better Combat/Improved Mobs/Power Scale 같은 광범위 전투 변경은 **핵심 클래스 전투가 검증된 뒤에만 추가** 여부 결정.
4. **마지막:** 플레이테스트에 따라 특정 모드 제거/교체도 허용. **게임성·기능 호환·빌드 품질이 우선**이고 '모드를 많이 설치했다'를 진행률/완성도로 쓰지 않는다.

## H. 다음 실질 작업

- **현재 완료 = 최신 프로젝트/파일 페이지와 공식 제작자 설명을 기반으로 후보·버전·로더/라이선스·고위험 상호작용의 1차 비교**, 설치/동시 로딩/성능·실제 AI·지배·기생·비행·서버 실험 **미수행**.
- **다음은 기획 정본 통합**: 선택된 외부 후보의 강적·보스/드롭/월드 생성과 6직업의 XP·스킬포인트/장비·제물/숙주/군체·기본 생존 루프를 함께 정리. **최종 modpack**은 그 기획 검수와 loader 선택을 통과한 후 확정.
- 작성일 이후 외부 파일이 변경될 수 있으므로 **버전/의존성/라이선스는 실제 다운로드 시 재검증**하고 원본 URL/검증 일자를 관리한다.

**STATUS: MOD CANDIDATES RESEARCHED / NO MODS INSTALLED / MODPACK NOT PRODUCED / MC+LOADER NOT LOCKED / NO BUILD / NO JAR / NO PLAYTEST / NO MULTIPLAYER TEST.**
