# 2026-11-14 — 매개체 제작법 실제 로드 실패 수정

**문제:** 게임 `latest.log`에 `Pattern references symbol '.' but it's not defined in the key`가 기록됐다. 기존 `tools/generate_focus_recipes.py`는 정본 UI 카탈로그의 `.` 빈 칸 표기를 Minecraft 서버용 `minecraft:crafting_shaped` 레시피에 그대로 출력했다. 1.21.1에서는 빈 칸이 **공백**이어야 한다.

**동시에 발견한 문제:** 대체 가능한 재료를 `{"items":["minecraft:..."]}`로 출력하던 것도 1.21.1 `Ingredient` 규격과 맞지 않는다. `[{"item":"minecraft:..."}, {"item":"minecraft:..."}]`로 수정했다. 계약술사 불가해의 불사의 토템 / 바다의 심장 대체안을 유지한다.

**변경 범위:** `tools/generate_focus_recipes.py`, `tools/verify_focus_recipes.py` 및 본 문서. 정본 `data/focus_recipes.json`의 30개 배치와 원재료/소모 수량, 매개체 아이템 ID·효과·서버 장착/슬롯, UI 레시피 뷰어는 수정하지 않았다.

**정적 검증 강화:** 배포용 생성 JSON은 카탈로그 점을 공백으로 변환한 패턴과 일치해야 하고, 개별 재료는 단일 `{"item":...}` 객체 또는 대체 재료 객체 배열이어야 한다. JSON 원천/런타임 표시 간 변환을 검산한다.

**검증 경계:** 정적 검증 및 Gradle 빌드 성공만으로 실제 인게임 제작대 레시피 로딩 성공을 보장하지 않는다. 마인크래프트 클라이언트의 데이터팩/레시피 로드 로그 또는 전용 서버 로드 확인이 아직 필요하다. **CLIENT TESTED: NOT TESTED · PLAYTESTED: NOT TESTED · MULTIPLAYER TESTED: NOT TESTED.**

참고: NeoForge 1.21.1 Ingredients 문서: https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/ingredients/
