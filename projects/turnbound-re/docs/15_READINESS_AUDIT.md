# 15 — IMPLEMENTATION READINESS AUDIT

검수 기준일: 2026-09-07

## 결론
- **Core implementation readiness: GO**
- **M0~M4 automated implementation gates: PASS**
- **M2 manual client gate: PENDING — final combined playtest에서 수행**
- **M4 runtime world save/reconnect manual check: PENDING — final combined playtest에서 수행**
- **M5 production visual implementation: GATED — research/design/mockup 선행 필수**
- **Final production content completeness: NOT COMPLETE**

`M0~M4 automated PASS`는 현재 representative vertical slice의 핵심 계약과 자동 회귀검증이 닫혔다는 뜻이다. 모든 캐릭터/지역/아이템/최종 UI/최종 밸런스까지 완제품이라는 뜻은 아니다.

## 1. 구현자가 더 이상 결정하면 안 되는 코어
다음은 현재 정본대로 유지한다.
- 프로젝트 독립성 및 구 TURNBOUND ZERO AUTHORITY.
- 4인 활성 파티.
- originStar ★1~★5 / currentStar 최대 ★6.
- 별별 레벨 상한 20/30/40/50/60/70.
- 저태생 숨은 성장 보정 금지.
- server-authoritative battle.
- deterministic seed/revision/event log.
- SPD initiative.
- Enemy Intent.
- 6 affinity tag.
- Poise → EXPOSED → RECOVER/POISE_GUARD 흐름.
- Energy/Basic/Skill/Guard/Burst.
- damage/heal 기본 공식.
- visible Encounter.
- 바닐라 Mob 전수 매핑/명시 제외 validation.
- 데이터 중심 Character/Action/Status/Encounter/Reward 정의.
- M4까지 DEBUG_ONLY UI만 허용.
- authored Encounter metadata가 Encounter reward의 유일한 정본 source.
- 진행 중 battle은 definition + reward table snapshot을 유지하여 `/reload`가 이미 열린 전투를 변형하지 않음.
- VICTORY→REWARD settlement는 공용 lifecycle에서 one-shot persistence로 처리.

## 2. M0~M4 현재 검증 상태
마지막 검증 기준:
- commit `9544e30487bf9d9025f5e5d258fca67784a92f28`
- `Build turnbound-re` run `34075982400`
- result **SUCCESS**
- Java 25 / NeoForge 26.2.0.38-beta / Gradle 9.2.1
- dependency resolution + `clean build` + 전체 JUnit + production JAR verify 통과.
- JAR SHA-256 `5133b0f38f618402b9e2abe49021d13e0b489a1ecbafed581c95a4a06c7da505`.

M4 E2E 자동 검증은 production authored Encounter를 직접 사용해 battle open→actual data action→victory→reward→one-shot claim→PlayerProgress→SavedData Codec round-trip→unlock/level cap/ascend/party/Squad Cost→reload-equivalent round-trip을 검증한다.

persistence callback 실패 시 reward claim은 소모되지 않고 재시도 가능하다.

## 3. 플레이테스트로 튜닝 가능한 항목
이 값들은 코드를 막지 않으며 데이터로 조정한다.
- affinity 배율.
- EXPOSED damage multiplier.
- Poise damage/Poise max.
- Energy gain/cost.
- crit/variance.
- squad capacity 초기 목표 12.
- encounter 시간/보상량.
- 캐릭터 base/growth/ascension 수치.

튜닝 변경이 핵심 루프 자체를 없애지 않는 한 CANON 변경은 아니다.

## 4. 아직 CONTENT COMPLETE가 아닌 항목
최종 게임 전체를 추가 기획 없이 만들려면 아래 대량 콘텐츠 정본이 앞으로 채워져야 한다.
- 모든 eligible 바닐라 캐릭터 각각의 최종 originStar/role/stat/growth/affinity.
- 각 캐릭터의 최종 Basic/Skill/Burst/Passive 전체 정의.
- 모든 적 AI/Intent script와 보스 phase table.
- 전체 Encounter 배치/구성/난이도/보상표.
- XP 및 승급 필요량의 전체 수치표.
- 장비 슬롯/아이템/옵션/강화 비용 전체 카탈로그.
- 제작/광질/농사/낚시의 전체 recipe/drop/value table.
- 전체 지역 topology, 퀘스트, 진행 gate, fast travel 목록.
- 최종 획득처와 Character Shard 분배.
- 튜토리얼/스토리/텍스트 콘텐츠.

이들은 M5 시각 게이트 준비의 blocker가 아니다. 대표 콘텐츠로 시스템을 검증한 뒤 데이터 생산 파이프라인을 이용해 확장한다.

## 5. M5 의도적 Visual Gate
다음은 문서 부족이 아니라 잘못된 즉흥 디자인을 막기 위한 의도적 게이트다.
- production battle HUD.
- party/character/growth/inventory UI의 시각 언어.
- 캐릭터 고유 외형/모델/애니메이션.
- skill VFX.
- 마을/지역/던전의 최종 미술.
- 아이콘/폰트/프레임/색 체계.

production UI를 만들기 전에 반드시:
1. 실제 우수 턴제 RPG UI 다수 조사.
2. 실제 Minecraft UI/모드 구현 사례 조사.
3. 여러 reference를 기능/정보계층/연출 원리 단위로 비교.
4. `08_REFERENCE_CATALOG.md` 보강.
5. 화면별 information hierarchy.
6. design tokens.
7. mockup.
8. 그 뒤 Minecraft 구현.
9. 실화면 screenshot comparison 반복.

`AGENT_RULES.md`, `06_UI_UX_PRESENTATION.md`, 공용 `QUALITY_STANDARD.md`가 이 절차의 authority다.

## 6. 알려진 기술 리스크 / 수동 검증 부채
자동 gate와 별개로 실제 구현/실게임에서 확인해야 한다.
- 실제 client 20회 encounter 반복 시 Entity AI suppression/restore와 orphan battle 0 확인.
- 실제 world save/reload 또는 재접속 시 progression persistence 확인.
- client animation queue와 server revision 동기화.
- dimension change/disconnect/death cleanup 체감 검증.
- 대규모 바닐라 roster 데이터 유지보수.
- 향후 multiplayer에서 latency가 반응형 입력에 미치는 영향.
- save migration.
- 최종 world/assets의 라이선스와 성능.

사용자 방침상 앞의 client/runtime 수동 검증은 중간 JAR마다 요구하지 않고 전체적으로 한 번에 볼 만한 완성본에서 수행한다.

## 7. 다음 행동
**M5 Production UI/Presentation Gate의 연구/설계 단계로 이동한다.**

지금 할 일은 UI Java 구현이 아니다. 실제 턴제 RPG UI와 Minecraft 구현 사례 조사→reference catalog→information hierarchy→design tokens→mockup 순으로 선행 정본을 만든다. 이 gate가 닫힌 뒤에만 production HUD/menu 구현을 시작한다.
