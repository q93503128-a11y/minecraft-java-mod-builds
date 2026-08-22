# Survival Ascension

- Mod version: `0.6.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge 26.2.0.38-beta`
- Existing-world compatibility: 기존 `mining_progress_v1` SavedData와 공용 스킬 XP 맵을 그대로 유지한다.

## 정체성

바닐라 서바이벌에서 숙련이 오를수록 수치뿐 아니라 한 번의 행동이 처리하는 물리적 범위와 영향력이 커지는 성장 모드다.

## 현재 활성 성장

- 채굴: Lv.10/30/60/90 = 3×3/5×5/7×7/9×9 굴착, 광맥 최대 24/64/128.
- 벌목: 연결 통나무 최대 16/48/128/256.
- 농사: 성숙 작물 3×3/5×5/7×7/9×9 광역 수확.
- 전투: Lv.100 약 1.8× 피해, Lv.30/60/90 근접 파급 2/4/8체.
- 건축·기동: 다음 gameplay 확장 구간.

## M 통합 메뉴

K 직접 숙련키를 제거하고 M을 통합 메뉴키로 사용한다. M을 누르면 MineMenu(MIT)의 current radial interaction/presentation을 기반으로 한 6방향 메뉴가 뜬다.

- 숙련: 현재 레벨/XP/효과
- 가이드: 각 계통의 성장 방법
- 해금표: Lv.10/30/60/90 변화
- 통계: 현재 캐릭터 진행도
- 조작: M/Shift 등 핵심 조작
- 닫기

숙련/가이드 화면은 Skill Proficiencies(MIT)의 native Minecraft screen information architecture를 기반으로 한다.

## 외부 코드 정책

Skill Proficiencies, Veinminer++, MineMenu처럼 MIT로 명시된 코드는 고지를 보존하고 필요한 부분을 포팅한다. Project MMO 2.0 같은 제한/ARR 소스는 기능·UI 참고만 한다.
