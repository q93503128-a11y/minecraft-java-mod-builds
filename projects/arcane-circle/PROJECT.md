# Arcane Circle: Ninefold Arcana — Project Contract

- Mod ID / namespace: `arcanecircle`
- Version source: `gradle.properties -> mod_version`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / Gradle 9.2.1
- Direct spells 90 / Fusion spells 19 / Circles 1~9
- Canonical CI: `.github/workflows/build-arcane-circle.yml`
- Source audit: `tools/test_current_source.py`
- JAR audit: `tools/verify_jar.py`

게임 데이터·마력·숙련·시전·네트워크·판정은 서버 권위다. 0초 시전도 ready-hold 후 release 발동을 유지한다. 현재 presentation 정본은 `GrimoireScreen`, `ArcaneHud`, `ArcaneSigilDirector`, `SpellCinematicDirector`, `ArcaneRegaliaRenderer`, `ArcaneCastingPerformance`이며 구형 presentation 클래스와 버전별 migration/apply/fix 도구는 active tree에 두지 않는다.

alpha.28부터 모든 주문은 `ArcaneSigilDirector`의 주문별 술식 마법진과 `SpellCinematicDirector`의 물리 현상을 연속된 한 연출로 사용한다. 지팡이 시전시간 배율은 직접 주문과 융합 주문의 시전 계산 및 하한에 실제로 참여한다.
