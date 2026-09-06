# AGENTS — 반드시 먼저 읽기

TURNBOUND: RE에서 코드/콘텐츠/디자인을 수정하는 모든 사람과 에이전트의 진입점이다.

## HARD STOP
작업 전 반드시 아래를 읽는다.
1. `AGENT_RULES.md`
2. `docs/CANON.md`
3. 현재 작업 분야의 세부 문서
4. 저장소 공용 `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`

## 절대 규칙
- `projects/turnbound/`의 구 TURNBOUND는 **ZERO AUTHORITY**다. 기억이나 과거 구현을 자동 승계하지 않는다.
- 문서에 없는 핵심 게임 규칙을 구현자가 편의상 발명하지 않는다. 필요한 경우 데이터값으로 격리하거나 CANON 변경을 명시한다.
- 최종 UI/캐릭터 외형/VFX/맵/건축/아이콘을 AI의 취향만으로 디자인하지 않는다.
- 시각 작업은 반드시 실제 레퍼런스 증거와 목업을 남긴 뒤 구현한다.
- 테스트용 UI는 `DEBUG_ONLY`라고 명확히 표시하고 최종 디자인으로 승격시키지 않는다.
- 외부 코드/리소스는 출처와 라이선스를 기록한다.

작업 결과가 이 파일과 충돌하면 작업 결과가 잘못된 것이다.
