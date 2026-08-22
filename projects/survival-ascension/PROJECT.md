# Survival Ascension

- Mod version: `0.15.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Existing-world compatibility: 기존 스킬 SavedData, `infrastructure_v1`, 강화 적 NBT, affix CustomData, 채굴 모드 persistent data를 유지한다.

## 핵심 방향
숙련 상승은 단순 수치 상승이 아니라 물리적 작업 규모와 선택지 증가다. 대량 생산된 자원은 공동 인프라·재련·대형 작업 해금으로 다시 소비된다.

## 0.15 변경
### 벌목 안전화
Veinminer++ MIT의 smart-tree safety와 tick-drain 패턴을 현재 Woodcutting에 적응했다.
- 연결 로그를 최대 숙련/affix 상한만큼 먼저 탐색한다.
- 원점 또는 수집된 로그 중 하나에 `BlockTags.LEAVES`가 면접해야만 연쇄 작업을 생성한다.
- 잎이 없는 플레이어 제작 통나무 구조는 한 블록만 부순다.
- 연쇄 작업은 플레이어당 12로그/틱, 전체 64로그/틱.
- 내부 파괴 guard로 재귀 큐 생성 금지.
- 실제 파괴는 `player.gameMode.destroyBlock`으로 처리되어 도구 내구도/드랍/XP 흐름 유지.

### 건축 공방
세 번째 공동 인프라 프로젝트.
- 석재 벽돌 1024
- 철 주괴 256
- 구리 주괴 256
- 레드스톤 128
- 흑요석 64

완공 + 건축 Lv.90에서 `M → 건축 → 입체` 해금.
- 5×5×5 범위, 원점 제외 최대 124 추가 배치.
- 실제 블록 재료 소비.
- 기존 `mayInteract`, BlockSnapshot/NeoForge placement hook, 블록 생존 검사 재사용.
- 기존 최대 256 pending/player와 서버 틱 작업 큐 재사용.
- Shift는 계속 강제 단일 배치.

## 현재 주요 루프
숙련 성장 → 행동 체급 상승 → 강화 적/희귀 장비 → affix 재련/분해 → 대량 자원 생산 → 공동 인프라 완공 → 터널/재파종/입체 건축 같은 새 행동 해금.

## UI
M 메인 라디얼: `숙련 / 채굴 / 건축 / 장비 / 인프라 / 가이드 / 닫기`.
세부 기능은 MineMenu 계열 중첩 라디얼 또는 가이드 탭으로 들어간다.

## 외부 소스 정책
Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions, Apotheosis, Mekanism 등 확인된 permissive 소스는 라이선스 고지와 함께 필요한 구조를 적응한다. Create는 코드 MIT/에셋 ARR 경계를 지키며 현재 인프라 설계 참고만 한다. 제한 라이선스 프로젝트는 기능/UX만 참고하고 소스·에셋은 복사하지 않는다.
