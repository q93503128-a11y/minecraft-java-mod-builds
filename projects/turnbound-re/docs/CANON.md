# CANON — TURNBOUND: RE

이 문서는 현재 핵심 결정의 최상위 정본이다. 다른 문서가 충돌하면 CANON이 우선한다.

## C-001 프로젝트 독립성
TURNBOUND: RE는 구 TURNBOUND의 후속 코드베이스가 아니다. `projects/turnbound/`의 규칙, 캐릭터, UI, 수치, 월드, 코드 구조는 자동 승계하지 않는다.

## C-002 파티
활성 파티는 기본 4명이다. 전투 중 교대 시스템은 초기 코어 범위가 아니다.

## C-003 별과 레벨
- 태생 별: ★1~★5.
- 승급 최대: ★6.
- 현재 별별 레벨 상한: ★1 20 / ★2 30 / ★3 40 / ★4 50 / ★5 60 / ★6 70.
- `originStar`는 영구 불변, `currentStar`만 승급으로 증가한다.
- 저태생에게 숨겨진 성장률 보정을 주지 않는다.

## C-004 저태생의 가치
저태생은 고태생보다 성장폭이 더 커서 따라잡는 구조가 아니다. 대신 낮은 squad cost, 특정 역할, 빠른 육성, 조합 시너지, 특수 유틸리티로 사용 이유를 만든다. 고태생은 높은 기본 성능/키트 완성도/전문화로 키우는 보람을 보존한다.

## C-005 바닐라 로스터
전투 캐릭터로 의미 있는 모든 바닐라 `Mob` 계열 EntityType은 명시적으로 CharacterDefinition 또는 exclusion 규칙에 매핑한다. 돼지·소 같은 수동적 생물도 캐릭터화 대상이다. 미분류 eligible mob이 있으면 콘텐츠 검증 실패다.

## C-006 조우
일반 자연 스폰 몹의 접촉/피격이 무조건 턴제 전투를 여는 구조는 사용하지 않는다. 월드에 보이는 Encounter를 통해 전투를 시작하며, 전투 시간 증가를 보상하기 위해 Encounter 단위 보상 밀도를 Minecraft 기본 전투보다 높인다.

## C-007 전투 정체성
전투 핵심은 `Intent + Affinity + Poise + EXPOSED`다. 적의 다음 행동을 읽고, 약점/역할에 맞는 공격으로 Poise를 깎아 큰 행동을 끊거나 약화시키고, EXPOSED의 제한된 공격 창에서 파티가 연계한다.

## C-008 월드
고정/저작형 RPG 월드를 기본으로 한다. production 월드의 외형/지형/건축은 AI 즉흥 제작으로 채우지 않고 **실제 외부 authored world를 직접 base로 사용**한다.

현재 선택된 production base는 `Drehmal: APOTHEOSIS v2.2.2f` 공식 월드다.
- 원본 월드 파일은 TURNBOUND 저장소에 재배포하지 않는다. 사용자는 공식 배포본을 별도로 설치한다.
- TURNBOUND는 원본 지형/도시/건축을 다시 만들지 않고 server anchor, Encounter, progression, fast travel 의미만 얹는다.
- 현재 공개 본편은 Minecraft 1.20.1 기반이므로 Java 26.2에서 실제 호환 검증 전에는 `PLAYTESTED`로 취급하지 않는다.
- 호환 문제가 생겨도 임의 자작 월드로 자동 대체하지 않는다. 외부 base의 호환 adapter/업데이트 또는 다른 사용 가능한 외부 authored world를 먼저 검토한다.
- 기존 `FunctionalWorldSliceBuilder`, `ProductionWorldSlicePrototypeBuilder`, `AuthoredFirstRegionBuilder` 계열은 mechanics/layout 검증용 harness이며 production visual/world source가 아니다.

Minecraft의 광질·제작·낚시·농사·탐험은 외부 월드 위에서도 장식이 아니라 성장 루프와 연결한다.

## C-009 서버 권한
전투 상태, RNG, 대미지, 대상 검증, 보상은 서버가 권위자다. 클라이언트는 명령 의도와 표시만 담당한다.

## C-010 시각 디자인
최종 UI/외형/VFX/맵/건축에 임시 AI 디자인이나 AI가 외부 reference를 보고 다시 만든 디자인을 production으로 넣지 않는다.
- 직접 사용 가능한 외부 asset/base가 있으면 실제 파일/런타임 자산을 사용한다.
- proprietary/reference-only 자산을 눈으로 보고 비슷하게 재구성하지 않는다.
- 사용할 수 있는 외부 asset이 없으면 임시 자작 visual로 빈칸을 채우지 않고 해당 visual을 미완 상태로 둔다.
- 외부 자산은 URL/버전 또는 commit/라이선스/사용 분류를 `THIRD_PARTY_ASSETS.md`에 기록한다.

## C-011 데이터 중심
캐릭터, 행동, 상태, 조우, 보상, 지역은 가능한 한 데이터 정의로 분리한다. 핵심 규칙만 코드에 둔다.

## C-012 초기 개발
싱글플레이 실검증을 우선하되 네트워크/권한 경계를 처음부터 서버 권한으로 설계하여 추후 멀티에서 전투 규칙을 다시 쓰지 않게 한다.
