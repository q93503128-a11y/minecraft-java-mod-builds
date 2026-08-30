# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 캐릭터 수집형 파티 턴제 RPG.

현재 제작 버전: `0.1.0-alpha.13` — v0.4 Southgate Chapter 1 canonical realignment.

## 전투 코어
- 서버 정본 SPD 누적 Turn Gauge
- 임계값 1000 / 행동 후 1000 차감 / 초과 Gauge 보존
- 자연 연속 행동 임의 제한 없음
- 1~4 아군 / 최대 5 적
- 행동 선택 중 논리 전투 시간 정지
- Basic CD0 / Active 자기 정상 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge / Revive / Guard redirect / Reaction / Status
- 모든 스킬 `선택 → 대상 → 사용 확정`
- 실제 3D 전투원 클릭 + Tab 타겟 선택
- LMB drag orbit / Wheel zoom / RMB 취소 / Esc 설정
- 스킬 hover 상세설명
- 세계 장면 우선 Dark Glass HUD

## Chapter 1 정본 진행
캠페인 파티:
- P01 카이렌
- P03 브람
- P04 엘리시아
- F03 변경 사냥꾼

메인 퀘스트:
1. `MQ_C01_01_patrol` — ENC_M01 + ENC_M02
2. `MQ_C01_02_unstable` — E003 포함 ENC_M04
3. `MQ_C01_03_graul` — B01 들이받는 왕 그라울

M03/M05는 필드에 존재하지만 B01 해금 필수 조건은 아니다.

정본 조우:
- M01 Lv1: E001×2
- M02 Lv2: E001+E002
- M03 Lv3: E004×2
- M04 Lv4: E003+E002
- M05 Lv5: E005+E001×2

## 적/보스
- E003 불안정 폭발체: `팽창 → Armed → 다음 행동 대폭발 → 자기 전투불능`
- E004 길목 약탈자: HP 50% 이하 대상에게 비열한 찌르기 우선
- B01 Graul: HP 2800 / ATK 150 / DEF 115 / SPD 92
- B01 70%: E001+E002 소환, 부하 생존 중 DEF +15%
- B01 35%: SPD +20%, 돌파 예고 후 다음 행동 왕의 돌진
- E003 Armed / Graul 돌파 예고는 전장 Danger marker 및 상태 데이터로 표시

## Chapter 1 해금/보상
B01 전에는 캠페인 AUTO / 2.0x 잠금. 일반 필드전 도주 가능, B01 도주 불가.

B01 첫 클리어:
- XP 5,000
- Gold 12,000
- Summon Crystal 1,200
- Star Essence 60
- T2 장비 선택 상자 1
- P08 라제
- Echo Archive
- AUTO
- 2.0x

추가 튜토리얼 Crystal +1,800은 P3 영속 재화/Archive 단계에서 연결한다.

## 필드
현재 A01/A02는 Southgate Meadow를 검증하는 두 제작 셀이다. alpha.12의 A02를 Chapter 2 지역으로 취급하지 않는다. 실제 Chapter 2는 v0.4 정본의 Gloamwood다.

- PATROL → ALERT → ENGAGE 가시적 조우
- 랜덤 인카운터 없음
- 전투 종료 후 정확한 필드 위치 복귀
- 플레이어 shell은 생존 전투원이 아님
- 바닐라 체력/허기/생존 HUD 및 블록 진행 제거

## 테스트 명령어
- `/turnbound field` : Chapter 1 P2 필드 진입
- `/turnbound status` : 메인 퀘스트 UI
- `/turnbound battle` : P01~P04 4v5 전투 코어 진단. 캠페인 해금과 독립적으로 AUTO/2x 테스트 가능
- `/turnbound leave` : 현재 전투 강제 종료
- `/turnbound p0` : 결정론적 자동 전투 진단

## 정본 델타
- `DESIGN_DELTA_ALPHA13_CANON_REALIGN.md` : alpha.11~12 임시 분기를 v0.4 정본으로 재정렬

다음 P2는 Southgate 전체 공간/Radia/FT_MEADOW 정본 좌표 확장과 Chapter 2 Gloamwood 연결이다. P3에서 saveSchemaVersion 4, 보유/성장/Archive/장비/강화를 영속화한다.
