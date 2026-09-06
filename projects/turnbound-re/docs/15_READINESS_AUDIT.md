# 15 — IMPLEMENTATION READINESS AUDIT

검수 기준일: 2026-09-06

## 결론
- **Core implementation readiness: GO**
- **Vertical Slice M0~M4: GO**
- **Final production content completeness: NOT COMPLETE**
- **Final visual/UI/world presentation: GATED BY DESIGN PROCESS**

`IMPLEMENTATION READY`는 "지금부터 개발자가 핵심 전투 규칙을 임의로 발명하지 않고 M0부터 구현할 수 있다"는 뜻이다. "모든 캐릭터/지역/아이템/드롭/최종 화면까지 추가 기획 없이 완제품을 만들 수 있다"는 뜻은 아니다.

## 1. 구현자가 더 이상 결정하면 안 되는 코어
다음은 현재 정본대로 구현한다.
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
- M0~M4에서는 DEBUG_ONLY UI만 허용.

## 2. M0 시작 전 추가 기획 필요 여부
**없음.**

M0에서 필요한 플랫폼 패치 번호는 게임 기획 결정이 아니라 저장소 공용 BUILD_STANDARD와 현재 검증 조합을 확인해 선택하는 개발환경 결정이다.

## 3. M0~M4 중 플레이테스트로 튜닝 가능한 항목
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

이들은 M0 코어 개발의 blocker가 아니다. 대표 콘텐츠로 시스템을 검증한 뒤 데이터 생산 파이프라인을 이용해 확장한다.

## 5. 의도적으로 GATE인 항목
다음은 문서 부족이 아니라 잘못된 즉흥 디자인을 막기 위한 의도적 게이트다.
- production battle HUD.
- party/character/growth/inventory UI의 시각 언어.
- 캐릭터 고유 외형/모델/애니메이션.
- skill VFX.
- 마을/지역/던전의 최종 미술.
- 아이콘/폰트/프레임/색 체계.

`AGENT_RULES.md`와 `06_UI_UX_PRESENTATION.md`의 외부 레퍼런스→분석→목업→Minecraft 구현→실화면 비교 절차를 통과해야 한다.

## 6. 알려진 기술 리스크
문서로 제거할 수 없고 실제 구현/실게임으로 검증해야 한다.
- NeoForge 현재 버전의 Entity AI suppression/restore 세부 API.
- resource reload와 진행 중 battle snapshot의 경계.
- client animation queue와 server revision 동기화.
- dimension change/disconnect/death cleanup.
- 대규모 바닐라 roster 데이터 유지보수.
- 향후 multiplayer에서 latency가 반응형 입력에 미치는 영향.
- save migration.
- 최종 world/assets의 라이선스와 성능.

따라서 "문제 발생 가능성 0%"는 보장할 수 없지만, 이 리스크는 현재 M0~M4의 테스트 계획에서 조기에 발견하도록 설계되어 있다.

## 7. 다음 행동
**즉시 M0 Bootstrap & Contracts를 시작한다.**

M0에서 완성해야 할 첫 계약은 build scaffold, mod id `turnbound_re`, Codec/data registry, validation, vanilla Mob coverage 검사, pure battle test harness, debug command skeleton이다.
