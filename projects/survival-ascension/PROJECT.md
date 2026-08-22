# Survival Ascension

- Mod version: `0.8.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge 26.2.0.38-beta`
- Existing-world compatibility: 기존 `mining_progress_v1` SavedData와 공용 스킬 XP 맵을 그대로 유지한다.

## 정체성
바닐라 서바이벌에서 숙련이 오를수록 수치뿐 아니라 한 번의 행동이 처리하는 물리적 범위와 행동 선택지 자체가 커지는 성장 모드다.

## 현재 활성 성장
- 채굴: Lv.10/30/60/90 = 3×3/5×5/7×7/9×9 굴착, 광맥 최대 24/64/128.
- 벌목: 연결 통나무 최대 16/48/128/256.
- 농사: 성숙 작물 3×3/5×5/7×7/9×9 광역 수확.
- 전투: Lv.100 약 1.8× 피해, Lv.30/60/90 근접 파급 2/4/8체.
- 건축: 선 5/9/17/33, 벽·바닥 3×3/5×5/9×9. 실제 재료와 보호 이벤트를 사용.
- 기동: 지상 질주 XP, Lv.10 단차/안전낙하, Lv.30 R 지상돌진, Lv.60 공중돌진 1회, Lv.90 극한돌진.

## M 통합 메뉴
M을 통합 메뉴키로 사용한다. MineMenu(MIT)의 radial interaction/presentation을 기반으로 한 7방향 메뉴다.
- 숙련
- 건축
- 가이드
- 해금표
- 통계
- 조작
- 닫기

건축은 두 번째 라디얼에서 단일/선/벽/바닥을 고른다. 숙련/가이드 화면은 Skill Proficiencies(MIT)의 native Minecraft screen information architecture를 기반으로 한다.

## 조작
- `M`: 통합 라디얼 메뉴
- `R`: 기동 액션 / 돌진
- `Shift`: 광역·대량 작업의 정밀 단일 모드

## 외부 코드 정책
Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2처럼 MIT로 명시된 코드는 고지를 보존하고 필요한 부분만 포팅/적응한다. Project MMO 2.0, ParCool 및 제한/카피레프트/커스텀 라이선스 소스는 의도적으로 의무를 수용하지 않는 한 기능·UX 참고만 하고 코드는 독립 구현한다.
