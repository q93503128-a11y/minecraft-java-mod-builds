# Changelog

## Current 0.12.1 alpha line
- 과거 카드형 마도서와 네모 주문 핫바 presentation을 폐기했다.
- 공통 마법진 장식층과 써클별 범용 VisualIdentity 체계를 제거했다.
- `SpellCinematicDirector`가 주문의 실제 공간 사건을 기준으로 연출한다.
- 복장과 시전 동작을 `ArcaneRegaliaRenderer` / `ArcaneCastingPerformance`로 분리했다.
- 구형 presentation 클래스, 과거 전용 CI, 버전별 apply/fix/migration 도구를 active tree에서 제거했다.
- 1~9써클, 직접 주문 90개, 융합 주문 19개, ready-hold/release, 서버 권위 판정, `presentationImpactDelay`, `syncAtomicRobe` 계약은 유지한다.

과거 세부 변경 이력은 Git history가 보존한다.
