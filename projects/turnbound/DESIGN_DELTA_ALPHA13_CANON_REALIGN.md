# TURNBOUND alpha.13 — v0.4 정본 재정렬

alpha.11~alpha.12에서 P2 플레이 루프를 빠르게 세우는 과정에서 Southgate 일부 적/조우/진행을 임시 설계로 확장한 부분이 있었다. alpha.13부터 `01_세부기획서_v0.4`, `02_수치규칙위키_v0.4`, `03_캐릭터설계위키_v0.4`를 다시 우선 정본으로 고정한다.

## 되돌린 임시 분기
- E003 `갈고리 추적자` → 정본 `불안정 폭발체`
- E004 `철갑 파수병` → 정본 `길목 약탈자`
- B01 임시 스탯/스킬 → 정본 Graul Lv6/2800 HP 패턴
- 캠페인 테스트 파티 P01/P02/P03/P04 → Prologue 정본 P01/P03/P04/F03
- Southgate ENC_M01~M05 편성 → 수치 위키 #125 정본 편성
- 5개 일반 조우 전부 강제 후 B01 → MQ_C01_01/MQ_C01_02 단계식 해금
- alpha.12 `South Road A02`를 Chapter 2처럼 취급하던 설명 → Southgate Meadow 제작용 2번째 셀로 재분류. 실제 Chapter 2는 Gloamwood.

## Chapter 1 진행 정본
1. ENC_M01 + ENC_M02 승리 → MQ_C01_01 완료 / Meadow 경로·맵 진행 해금
2. E003가 포함된 ENC_M04 승리 → MQ_C01_02 완료 / B01 길 해금
3. B01 Graul 승리 → MQ_C01_03 완료
4. B01 첫 클리어 해금: Echo Archive, AUTO, 2.0x, P08 Raze

ENC_M03/M05는 필드에서 유지되지만 B01 해금의 필수 조건으로 강제하지 않는다.

## P2 개발 셀
현재 A01/A02는 최종 1024×1024 월드 전체가 아니라 Southgate Meadow 동선/조우를 검증하는 제작 셀이다. A02는 Gloamwood가 아니며 Chapter 2를 대신하지 않는다. 다음 실제 지역 제작은 정본 좌표/역할의 Gloamwood로 연결한다.

## 전투 정본 보강
- E003는 `팽창 → Armed → 다음 정규 행동 대폭발 → 자기 전투불능` 흐름을 사용한다.
- Armed는 HUD/타임라인에서 Danger 상태로 노출한다.
- E004는 HP 50% 이하 대상을 우선하는 `비열한 찌르기`를 사용한다.
- Graul은 70%에서 E001+E002를 소환하고, 부하 생존 중 DEF +15%를 얻는다.
- Graul은 35%에서 SPD +20%, `돌파 예고 → 다음 행동 왕의 돌진`을 사용한다.
- 보스전은 도주 불가, 일반 필드전은 도주 가능.
- AUTO/2.0x는 캠페인에서 B01 첫 승리 전 잠금. `/turnbound battle` 진단 전투는 회귀검사를 위해 사용 가능.

## 보상 정본
- B01: XP 5,000 / Gold 12,000 / Summon Crystal 1,200 / Star Essence 60 / T2 선택 상자 1 / P08 지급 / Echo Archive 해금.
- B01 직후 튜토리얼 보상 Crystal +1,800은 P3 영속 재화/Archive 구현에서 동일 정본으로 연결한다.

## 다음 순서
alpha.13 정렬이 안정화되면 P2에서 Southgate 전체 공간과 Radia/FT_MEADOW를 정본 좌표 체계로 확장한 뒤, Chapter 2 Gloamwood를 시작한다. 그 후 P3 영속 진행/보유/성장/Archive/장비로 넘어간다.
