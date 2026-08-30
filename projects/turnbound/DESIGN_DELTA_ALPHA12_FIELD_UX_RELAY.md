# TURNBOUND Field UX / Relay Delta — alpha.12

상태: alpha.11의 `5개 조우 → B01 → Chapter 1 clear`를 실제 필드 UX와 지역 이동으로 연결하는 P2 후속.

## 1. 목표
alpha.11까지는 진행/보상이 서버 채팅 중심이었다. alpha.12에서는 같은 서버 정본을 유지하면서 플레이어가 실제 RPG처럼
`NPC 확인 → 필드 조우 → 전투 → 결과/보상 → 다음 목표 → 봉쇄 해제 → 다음 셀 진출 → Relay 활성화/이동`
흐름을 화면과 월드에서 읽을 수 있게 만든다.

## 2. Quest UI
`남문 정찰관` 우클릭 또는 `/turnbound status`로 임무 패널을 연다.

패널 표시:
- Chapter 1 현재 목표
- ENC_M01~M05 클리어 여부
- B01 잠금/해금/클리어 여부
- 누적 XP / Gold
- 진행 단계별 정찰관 대사

불투명 풀스크린 메뉴가 아니라 필드가 그대로 보이는 dark-glass side panel을 사용한다.

## 3. Reward Result UI
전투 승리 후 필드로 복귀할 때 별도 결과 패널을 연다.
- 조우 이름
- 최초 클리어 XP
- 최초 클리어 Gold
- 중복 클리어 시 보상 없음 명시
- Chapter clear 여부
- 다음 목표

보상 계산/중복 방지는 기존 서버 진행 정본에서 처리하며 UI가 임의 계산하지 않는다.

## 4. Relay / Fast Travel
A01의 Relay 잔해 위치에 `남문 초원 계전석` 상호작용 오브젝트를 배치한다.
직접 조사한 계전석만 활성화된다.

목적지 규칙:
- A01 Relay: Chapter 시작부터 물리적으로 접근/활성화 가능
- A02 Relay: Chapter 1 clear 전 활성화 불가
- 활성화하지 않은 Relay는 목록에 보여도 이동 버튼 비활성
- 이동은 서버가 목적지 활성 상태를 재검증

## 5. South Road A02
Chapter 1 clear 후 A01 남쪽 봉쇄를 실제로 해제하고 인접 64×64 셀 A02를 연다.

좌표:
- A01: X -32..31 / Z 128..191
- A02: X -32..31 / Z 192..255
- base Y 64

A02 현재 목적:
- Chapter clear 이후 실제 다음 공간으로 걸어갈 수 있는지 검증
- 길/암벽/소규모 거점/Relay라는 지역 언어 확립
- A02 Relay까지 직접 도달한 뒤 두 거점 Fast Travel 검증

A02의 정식 적/NPC/후속 퀘스트는 다음 P2 콘텐츠 묶음에서 추가한다.

## 6. 네트워크 정본
Field UI는 client 추정이 아니라 server snapshot을 사용한다.
- active field session
- progress / boss unlock / chapter clear
- XP / Gold
- encounter checklist
- dialogue / objective
- reward receipt
- activated travel destinations

클라이언트는 이 snapshot을 표시만 하며 Fast Travel 요청은 서버에서 다시 검증한다.

## 7. 합격 기준
- NPC 우클릭으로 Quest 패널 정상 표시
- `/turnbound status`가 같은 패널 사용
- 기존 전투 진입/복귀/직접 3D 타겟/카메라 회귀 없음
- 승리 후 보상 패널 정상 표시
- 5/5 후 B01 출현 유지
- B01 격파 후 A02 봉쇄 해제
- A02 진입 가능
- A01/A02 Relay 활성화 순서 강제
- 미활성 Relay 이동 거부
- 두 Relay 활성화 후 양방향 이동
- Java 25 clean test/build + NeoForge server smoke
