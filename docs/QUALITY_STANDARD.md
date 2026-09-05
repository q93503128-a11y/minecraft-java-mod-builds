# Minecraft Mod Quality Standard

이 문서는 이 저장소의 모든 Minecraft Java/NeoForge 모드가 공통으로 참고하는 **품질·디자인·도구 선택 표준**이다.

`docs/BUILD_STANDARD.md`가 빌드·검증·패키징의 정본이라면, 이 문서는 **어떻게 더 높은 완성도의 게임을 설계하고 구현할 것인가**의 정본이다.

> 목표는 "기능이 존재하는 모드"가 아니라, 외형·애니메이션·UI/UX·게임성·코드 구조·사운드·성능·안정성이 함께 완성된 모드다.

버전과 라이브러리 호환성은 자주 바뀐다. 아래 도구 이름과 역할은 기준점으로 사용하되, 새 작업을 시작할 때 현재 Minecraft/NeoForge 버전의 공식 문서와 실제 배포 버전을 다시 확인한다.

---

## 1. 핵심 제작 원칙

다음 순서를 기본으로 한다.

```text
요구사항 파악
→ 현재 프로젝트/기획서/기존 UI 확인
→ 우수 사례와 공식 문서 조사
→ 구조·정보계층·기술 선택
→ 목업/프로토타입
→ 구현
→ 자동 검증
→ 실제 Minecraft 플레이/스크린샷 검수
→ 성능 측정
→ 수정
→ 최종 검수
```

다음 방식은 피한다.

```text
아이디어
→ 곧바로 Java 코드
→ 임의의 사각형 UI/임의 모델
→ 기능이 돌아가면 완료
```

**코드가 동작한다는 사실과 결과물이 좋은 게임처럼 보인다는 사실은 별개다.**

---

## 2. 공용 도구 지도

| 영역 | 기본 선택 | 상황별 선택 | 주 역할 |
|---|---|---|---|
| 로더/API | NeoForge 공식 API | - | 모드 본체 |
| IDE | IntelliJ IDEA + Minecraft Development | - | Java/Minecraft 개발 |
| 3D 모델 | Blockbench | 외부 허용 모델을 베이스/레퍼런스로 사용 | 몹, 보스, 무기, 갑옷, 오브젝트 |
| 애니메이션 | GeckoLib | 다른 애니메이션 라이브러리는 프로젝트별 단일 선택 | 엔티티/아이템/갑옷 애니메이션 |
| Display Entity | Block Display/BDEngine | MCStacker | 장식, 구조물, 마법 연출, 컷신 오브젝트 |
| UI 레퍼런스 | Game UI Database + Interface In Game | 실제 게임 영상/공식 스크린샷 | 상용 게임의 해결 방식 조사 |
| UI 목업 | Penpot 또는 Figma | 이미지 편집기 | 구현 전 레이아웃 검증 |
| 일반 UI 구현 | Vanilla/NeoForge Screen | UI Lib | HUD, 상점, 캐릭터창, 퀘스트, 스킬창 |
| 고복잡도 UI | LDLib2 | - | 에디터, 그래프, 고급 데이터 바인딩 UI |
| 설정 UI | Cloth Config / EclipseUI | 프로젝트 기존 설정 프레임워크 | 설정 화면 |
| 픽셀/UI 그래픽 | Pixelorama | Aseprite, Krita | 아이콘, 프레임, 버튼, 텍스처 |
| 아이콘/그래픽 원천 | Kenney, Game-icons.net | 프로젝트별 허용 자산 | UI 베이스/레퍼런스 |
| 데이터/월드젠 | NeoForge Datagen + Misode | MCStacker | recipe, loot, tags, worldgen, NBT 실험 |
| 복잡한 몹 AI | Vanilla Goal/Brain 우선 | SmartBrainLib | 조건형 행동, 상태 기반 AI |
| 인게임 문서 | 직접 UI 또는 Modonomicon | - | 도감, 연구서, 튜토리얼 |
| 사운드 편집 | Audacity | 다른 DAW | 효과음 정리/가공 |
| 단위 테스트 | JUnit | - | 순수 Java 로직 |
| 게임 동작 테스트 | NeoForge GameTest | 수동 테스트 | 실제 월드 동작 |
| 성능 분석 | spark | Java Flight Recorder | tick/CPU 병목 분석 |

### 주요 링크

- NeoForge Docs: <https://docs.neoforged.net/>
- Blockbench: <https://www.blockbench.net/>
- GeckoLib Wiki: <https://wiki.geckolib.com/>
- Block Display / BDEngine: <https://block-display.com/>
- Misode Generators: <https://misode.github.io/>
- MCStacker: <https://mcstacker.net/>
- Game UI Database: <https://www.gameuidatabase.com/>
- Interface In Game: <https://interfaceingame.com/>
- Penpot: <https://penpot.app/>
- Figma: <https://www.figma.com/>
- UI Lib: <https://daqem.com/projects/ui-lib>
- LDLib2: <https://modrinth.com/mod/ldlib>
- Cloth Config: <https://modrinth.com/mod/cloth-config>
- SmartBrainLib: <https://modrinth.com/mod/smartbrainlib>
- Modonomicon: <https://modrinth.com/mod/modonomicon>
- spark: <https://spark.lucko.me/>
- Pixelorama: <https://orama-interactive.itch.io/pixelorama>
- Kenney Assets: <https://kenney.nl/assets>
- Quaternius: <https://quaternius.com/>
- Poly Pizza: <https://poly.pizza/>
- Game-icons.net: <https://game-icons.net/>

---

## 3. 의존성 선택 규칙

라이브러리를 많이 넣는다고 품질이 올라가지는 않는다.

새 의존성을 추가하기 전에 확인한다.

1. 직접 구현보다 품질 또는 생산성이 실제로 좋아지는가?
2. 반복 코드를 충분히 줄이는가?
3. 현재 Minecraft/NeoForge 버전을 지원하는가?
4. 최근 유지보수 상태가 괜찮은가?
5. 기능 하나 때문에 지나치게 큰 의존성을 추가하는 것은 아닌가?
6. 기존 라이브러리와 역할이 겹치지 않는가?
7. 향후 업데이트 때 프로젝트 전체를 묶어버리는 핵심 의존성이 되지 않는가?

예:

- GeckoLib와 다른 동일 목적 애니메이션 라이브러리를 이유 없이 동시에 넣지 않는다.
- 버튼 몇 개짜리 화면 때문에 LDLib2를 넣지 않는다.
- 단순 좀비형 몹 때문에 SmartBrainLib를 강제하지 않는다.
- 설정창을 직접 수천 줄 구현하지 않는다.

---

# 4. UI/UX 표준 — 최우선 규칙

## 4.1 UI를 코드부터 만들지 않는다

복잡한 UI는 다음 단계를 생략하지 않는다.

```text
화면 목적 정의
→ 실제 게임 레퍼런스 조사
→ 정보 중요도/사용 동선 정의
→ 프로젝트 디자인 시스템 적용
→ 목업
→ 구현 기술 선택
→ Minecraft 구현
→ 실제 화면 캡처
→ 레퍼런스/목업과 비교
→ 수정
```

특히 **AI가 머릿속에서 "RPG풍"을 상상해 검은 반투명 박스 + 색 테두리 + 카드 여러 개를 즉석으로 만드는 것을 기본 디자인 방식으로 사용하지 않는다.**

## 4.2 먼저 정의할 것

화면마다 다음을 적어도 머릿속이 아니라 명시적으로 정리한다.

- 플레이어가 가장 먼저 봐야 할 정보
- 가장 자주 누를 행동
- 보조 정보
- 드물게 사용하는 기능
- 전투 중인지/정지 상태인지
- 화면 체류 시간이 짧은지 긴지
- 키보드/마우스 동선
- 닫기/뒤로가기 규칙
- 실패/비활성/빈 상태

## 4.3 레퍼런스 조사

다음 소스를 우선한다.

- Game UI Database
- Interface In Game
- 실제 상용 게임 영상
- 공식 게임 스크린샷

단순 화면은 3~5개 정도의 관련 사례를 빠르게 비교할 수 있다. 핵심 RPG 화면이나 대형 개편은 가능하면 8개 이상을 보고 공통점과 차이를 정리한다.

한 게임을 그대로 복사하기보다 다음처럼 분리해 참고한다.

- 게임 A: 정보 계층
- 게임 B: 탭/내비게이션
- 게임 C: 선택/hover 피드백
- 게임 D: 애니메이션/전환
- 프로젝트 고유 디자인 언어: 색·프레임·아이콘·텍스처

## 4.4 프로젝트 Design System

프로젝트마다 최소한 다음을 통일한다.

### Color tokens

- Background
- Surface
- Elevated Surface
- Primary
- Secondary
- Accent
- Success
- Warning
- Danger
- Disabled
- Primary Text
- Secondary Text
- Border

### Spacing

XS / S / M / L / XL처럼 제한된 간격 체계를 둔다. 화면마다 임의의 7px, 11px, 13px을 남발하지 않는다.

### Typography

- 화면 제목
- 섹션 제목
- 본문
- 보조 설명
- 수치
- 경고
- tooltip

의 크기와 강조 규칙을 정한다.

### Components

가능하면 다음을 재사용 가능하게 만든다.

- 기본/보조/위험 버튼
- 탭
- 슬롯
- 카드 또는 패널
- tooltip
- scrollbar
- progress bar
- modal/popup
- selection marker

## 4.5 목업

복잡한 화면은 Penpot/Figma 등에서 구현 전에 목업한다.

최소 상태:

- Default
- Hover
- Pressed
- Selected
- Disabled
- Error
- Empty state

실데이터도 가정한다.

- 매우 긴 아이템/스킬 이름
- 긴 설명
- 큰 수치
- 한국어/영어
- 아이템 0개/1개/최대치
- 목록 스크롤 최대치

## 4.6 구현 기술 선택

### Vanilla/NeoForge Screen

다음에 우선한다.

- 단순 HUD
- 작은 팝업
- 화면 하나짜리 메뉴
- 특수 렌더링이 중요한 UI
- 외부 의존성을 늘릴 이유가 없는 화면

### UI Lib

일반적인 게임 UI가 커질 때 우선 검토한다.

적합 예:

- 스킬트리
- 퀘스트 목록
- 상점
- 캐릭터 정보
- 장비 관리
- 여러 패널/스크롤이 있는 화면

UI Lib은 컴포넌트, nine-slice, 텍스트/스크롤, 드래그·줌 가능한 스킬트리 같은 기능을 제공하지만, **좋은 디자인과 정보 구조를 대신해주는 자동 레이아웃 도구로 생각하지 않는다.**

### LDLib2

다음처럼 복잡도가 확실히 높을 때 검토한다.

- node/graph editor
- in-game editor
- 복잡한 asset browser
- 고급 데이터 바인딩
- 재사용성이 큰 modular UI

현재 프로젝트의 정확한 버전과 안정성을 먼저 확인한다. 단순 UI의 기본 선택으로 넣지 않는다.

### 설정 화면

설정은 Cloth Config/EclipseUI 등 검증된 설정 UI를 우선 검토한다. 설정 화면 자체가 핵심 콘텐츠가 아니라면 직접 구현 비용을 최소화한다.

## 4.7 UI 그래픽 규칙

가능하면 nine-slice, 전용 아이콘, 일관된 프레임 텍스처를 사용한다.

다음은 재설계 신호다.

- 이유 없는 반투명 사각형 남발
- 모든 것을 카드로 만듦
- 화면마다 다른 padding
- 지나친 glow/gradient/blur
- 낮은 contrast
- 지나치게 작은 글자
- 선택 상태가 모호함
- 클릭 가능/불가능 상태가 구분되지 않음
- 서로 다른 그림체의 아이콘 혼합
- 장식이 정보보다 눈에 띔
- 절대 좌표 수십 개를 무계획하게 하드코딩
- GUI Scale/창 크기가 바뀌면 무너짐

**기능 구현 성공은 UI 완료 판정이 아니다. 실제 스크린샷을 보고 판단한다.**

## 4.8 UI 테스트

최소한 다음을 섞어 본다.

- 여러 GUI Scale
- 작은 창 / 일반 FHD / 넓은 화면
- 16:9 / 16:10 계열
- 긴 한국어/영어 문자열
- 0개 / 1개 / 최대 데이터
- hover / selected / disabled / error
- tooltip이 화면 밖으로 나가는 상황
- 스크롤 최대치

색상 하나만으로 상태를 전달하지 않고, 가능한 경우 색 + 아이콘/텍스트/형태를 함께 사용한다.

---

# 5. 모델·애니메이션·Display Entity

## 5.1 커스텀 생물/무기/갑옷

기본 파이프라인:

```text
레퍼런스
→ Blockbench blockout
→ silhouette 확인
→ 세부 형태/텍스처
→ rig
→ animation
→ GeckoLib 또는 프로젝트 애니메이션 시스템 연동
→ 실제 게임 크기/조명/hitbox 검수
```

일반 생물은 idle/walk/run/attack/hit/death를 기준으로 하고, 보스는 intro/skill/phase transition/stagger/enrage 등 필요한 상태를 추가한다.

## 5.2 Block Display / BDEngine

적극 활용하기 좋은 대상:

- 제단
- 포탈
- 마법진
- 조각상
- 가구
- 기계
- 거대 무기
- 환경 장식
- 컷신 오브젝트
- 블록/아이템 기반 특수 연출

피하는 편이 좋은 대상:

- 관절 애니메이션이 복잡한 일반 생물
- 수십 마리가 계속 움직이는 오브젝트
- Display Entity 수가 지나치게 많은 상시 렌더링 구조

Block Display는 Blockbench의 대체품이 아니라 **다른 문제를 푸는 도구**다.

## 5.3 외부 모델/자산

처음부터 모든 것을 직접 만들 필요는 없다.

Kenney, Quaternius, Poly Pizza, Block Display 등에서 허용되는 자산을 베이스/레퍼런스로 활용할 수 있다. 단, Minecraft 스타일에 맞게 polycount, 비율, 텍스처, 애니메이션을 조정하는 편이 좋다.

개인 로컬 프로젝트는 공개 배포 프로젝트보다 활용 범위를 넓게 잡을 수 있지만, 유료 자산 우회 다운로드, DRM 우회, 명시적 접근 제한 회피는 하지 않는다.

모든 외부 자산은 가능하면 `THIRD_PARTY_ASSETS.md`에 기록한다.

```text
Asset
Author
Source
License/usage note
Modified
Used in
```

공개 배포로 전환할 경우 전체 자산의 라이선스와 제3자 IP를 다시 검수한다.

---

# 6. 코드 설계 표준

## 6.1 서버 권한

다음은 기본적으로 서버가 최종 결정한다.

- 데미지
- 아이템/재화 지급
- 스킬 성공 여부
- 퀘스트 완료
- 월드 변경
- 저장 데이터

클라이언트는 입력, 표시, 애니메이션, 시각 효과를 담당하되 중요한 게임 상태를 임의 확정하지 않는다.

## 6.2 Client / Common / Server 분리

client-only 렌더러, Screen, HUD 클래스를 common/server 경로에서 잘못 참조하지 않는다.

## 6.3 기능 중심 구조

프로젝트가 커지면 단순 `entity/`, `item/`, `screen/`, `util/`만으로 모든 코드를 분산시키지 말고 기능 단위 패키지도 검토한다.

```text
magic/
  spell/
  casting/
  networking/
  client/

quest/
  data/
  runtime/
  ui/
  networking/
```

## 6.4 Data-driven 우선

가능하면 코드에서 분리한다.

- 적 능력치
- 스킬 수치
- 아이템 수치
- loot
- recipe
- spawn
- quest data
- dialogue
- 밸런스 값

대량 JSON은 NeoForge Datagen을 기본으로 하고 Misode를 설계/실험 도구로 활용한다.

## 6.5 Mixin/내부 훅

우선순위:

```text
NeoForge 공식 API
→ Event / 공식 hook
→ 안정적인 확장점
→ 정말 필요할 때만 Mixin/내부 구현 의존
```

## 6.6 Tick 남용 금지

매 tick 전체 엔티티/블록을 스캔하기 전에 확인한다.

- Event로 가능한가?
- 상태 변화 시점에만 계산 가능한가?
- 캐시 가능한가?
- 검사 주기를 낮춰도 되는가?
- 검색 반경/대상을 줄일 수 있는가?

---

# 7. AI와 보스

## 단순 몹

Vanilla Goal/Brain을 우선한다.

## 복잡한 몹

행동 조건, 상태 전환, 근접/원거리 혼합, 복수 우선순위가 커지면 SmartBrainLib 같은 라이브러리를 검토한다.

## 보스

"체력과 공격력만 큰 일반 몹"을 보스로 만들지 않는다.

기본 구조 예:

```text
State
→ Target/환경 평가
→ 거리/HP/cooldown/phase 판단
→ 공격 선택
→ Telegraph
→ Attack
→ Recovery
→ 다음 판단
```

공격에는 가능한 범위에서 다음을 둔다.

- 사전 신호
- 외형과 일치하는 판정
- 회피 가능성
- 명확한 피격 피드백
- 후딜/회복 구간

---

# 8. 데이터·월드젠·명령 실험

복잡한 recipe/loot/worldgen/NBT를 처음부터 암기해 작성하지 않는다.

- 프로젝트 정본: NeoForge Datagen
- 설계/시각 실험: Misode
- summon/item/NBT 빠른 실험: MCStacker

월드젠은 prototype → 게임 시험 → datagen 통합 순서를 권장한다.

---

# 9. 인게임 설명/튜토리얼

복잡한 시스템이 많은 모드는 플레이어에게 외부 문서만 강요하지 않는다.

도감, 연구서, 튜토리얼, 스킬 설명이 큰 시스템이라면 직접 UI 또는 Modonomicon 같은 인게임 문서 프레임워크를 검토한다.

단순 모드에는 불필요한 의존성을 추가하지 않는다.

---

# 10. 사운드

외형과 UI가 좋아도 사운드가 빈약하면 완성도가 크게 떨어진다.

필요한 경우 하나의 행동을 다음 층으로 나눠 생각한다.

### 공격/스킬
- wind-up
- cast/swing
- hit
- environment impact

### UI
- click
- confirm
- error
- reward

### 보스
- 등장
- 공격
- phase change
- 피격
- 사망

동일 효과음을 과도하게 반복하지 않고, 필요한 경우 pitch/variant를 둔다. Audacity 등으로 trim, volume, fade, EQ, noise cleanup을 수행한다.

---

# 11. 테스트와 품질 검증

`docs/BUILD_STANDARD.md`의 빌드 계약을 따른다. 품질 관점에서는 추가로 다음을 지킨다.

```text
Compile
→ Static checks
→ Unit tests
→ GameTests
→ Minecraft 실행
→ 수동 플레이
→ 로그 확인
→ 성능 측정
```

### JUnit에 적합

- damage formula
- XP 계산
- cooldown
- 랜덤 선택/가중치
- progression
- serialization helper

### GameTest에 적합

- 블록 상호작용
- 월드 상태 변화
- 엔티티 동작
- 구조물
- 아이템 사용
- 자동화 가능한 회귀 시나리오

---

# 12. 성능

감으로 최적화하지 않는다.

```text
증상 재현
→ spark 프로파일
→ 실제 hot path 확인
→ 수정
→ 같은 조건 재측정
```

필요하면 JFR까지 사용한다.

Worst-case도 본다.

- 몹 1마리가 아니라 실제 최대 규모
- UI 아이템 3개가 아니라 최대 목록
- 스킬 1회가 아니라 여러 개 동시 사용
- Display Entity 1개가 아니라 실제 장면 전체

---

# 13. 프로젝트 시각 언어

화면과 모델을 각각 예쁘게 만드는 것보다 **모두가 하나의 게임처럼 보이게 만드는 것**이 중요하다.

프로젝트별로 가능하면 다음을 정의하고 유지한다.

- 주/보조/강조 색
- UI frame 스타일
- icon 스타일
- texture density
- 모델 비율
- animation speed/weight
- VFX 밀도
- sound character

새 화면을 만들 때 기존 화면을 무시하고 새로운 스타일을 다시 발명하지 않는다.

---

# 14. AI/에이전트가 UI 작업할 때의 강제 규칙

이 저장소를 작업하는 ChatGPT/Codex/기타 에이전트는 핵심 UI를 만들 때 다음을 따른다.

1. 현재 프로젝트의 기존 UI와 기획서를 먼저 확인한다.
2. 같은 유형의 실제 게임 UI를 여러 개 조사한다.
3. 참고한 요소를 정보 구조/내비게이션/시각 계층/상호작용으로 분해한다.
4. 화면 목적과 정보 중요도를 정의한다.
5. 복잡한 화면이면 목업을 먼저 만든다.
6. Vanilla Screen / UI Lib / LDLib2 중 복잡도에 맞춰 선택한다.
7. 실제 Minecraft에 구현한다.
8. 실제 스크린샷 또는 그래픽 실행 결과를 확인한다.
9. 목업/레퍼런스와 비교한다.
10. 못생기거나 읽기 어렵다면 기능이 정상이어도 수정한다.

다음 문구를 완료 근거로 사용하지 않는다.

- "코드상 정상입니다."
- "컴파일됩니다."
- "버튼이 모두 있습니다."

UI 완료에는 **시각 검수**가 필요하다.

---

# 15. 외부 리소스 조사 규칙

새 모델, UI, 사운드, 코드 라이브러리를 찾을 때:

1. 공식 사이트/저장소를 우선한다.
2. 현재 지원 버전을 확인한다.
3. 유지보수 상태를 확인한다.
4. 라이선스/사용 조건을 확인한다.
5. 기존 프로젝트 의존성과 중복을 확인한다.
6. 가져오기 전에 실제로 품질을 높이는지 판단한다.

단순히 "유명하다"는 이유만으로 도입하지 않는다.

---

# 16. Definition of Done — 품질 판정

기능을 완료라고 부르기 전에 가능한 범위에서 확인한다.

### 기능
- 요구사항대로 동작
- edge case 확인
- 기존 기능 회귀 없음

### 코드
- 빌드 성공
- 책임 분리
- 중복/임시 코드 최소화
- 데이터화 가능한 수치 검토

### 네트워크
- client/server 책임 명확
- 패킷 값 검증
- 동기화 문제 없음

### UI
- 실제 게임에서 확인
- GUI Scale/창 크기 변화 확인
- 긴 문자열/최대 데이터 확인
- hover/selected/disabled 확인
- overflow 없음
- 프로젝트 디자인 언어 일치

### 그래픽
- 모델 scale 정상
- hitbox와 외형 일치
- 애니메이션 전환 자연스러움
- 텍스처 스타일 일관

### 사운드
- 볼륨/반복 피로도 확인
- 시각 효과와 타이밍 일치

### 성능
- 실제 최악 조건 시험
- 병목이 있으면 profiler로 원인 확인

### 자산
- 외부 원천 기록
- 공개 배포 시 라이선스 재검수

---

# 17. 다른 채팅/에이전트에서 이 문서를 사용하는 법

새 Minecraft 작업을 시작하면 최소한 다음을 읽는다.

1. `/AGENTS.md`
2. `/docs/BUILD_STANDARD.md`
3. `/docs/QUALITY_STANDARD.md`
4. 대상 프로젝트의 `PROJECT.md`
5. 대상 프로젝트의 기획서/README/CHANGELOG

이 문서는 특정 프로젝트 하나의 취향 문서가 아니라 **공용 저장소 전체의 기본 품질 기준**이다.

프로젝트 기획서가 더 구체적인 지시를 하면 프로젝트 기획서가 우선한다. 단, 더 낮은 품질을 정당화하기 위해 이 문서를 무시하지 않는다.

---

## 한 줄 요약

**상상으로 바로 만들지 말고, 좋은 사례를 조사하고 설계한 뒤 적절한 도구로 구현하고, 반드시 실제 Minecraft 화면과 플레이에서 다시 검수한다.**
