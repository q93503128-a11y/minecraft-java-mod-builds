# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 캐릭터 수집형 파티 턴제 RPG.

현재 제작 버전: `0.1.0-alpha.15` — automatic starter village + one-field vertical slice + battle UI reset.

## alpha.15 플레이 방식
명령어 없이 새 오버월드에 들어가면 약 2초 뒤 TURNBOUND가 자동 시작된다. 기본 Superflat도 별도 preset 없이 현재 지표 높이를 읽어 맵을 맞춘다.

현재 의도적으로 작은 범위만 제작한다.
- 평화로운 시작 마을 `남문 마을`
- 마을 남문에 바로 연결된 64×64 첫 필드 셀
- 필드에만 보이는 M01 / M02 적 파티
- 마을 안에는 전투 적 없음
- NPC `남문 정찰관`과 계전석
- 두 조우 클리어 후 이 vertical slice 완료

`/turnbound field`, `/turnbound status`, `/turnbound battle`은 개발/회귀 테스트용 fallback으로만 남아 있다.

## 전투 UX
- 카메라 피벗은 전체 인원 평균이 아니라 `아군 중심 ↔ 적군 중심`의 정확한 중점
- 실제 Minecraft post-collision camera projection으로 3D 캐릭터 클릭 판정
- 적 HP는 화면 위쪽 별도 벽이 아니라 실제 3D 적 옆의 작은 world-space HUD
- 선택 적: 빨간 `▼`, 선택 아군: 하늘색 `▼`, 현재 행동자: 금색 `◆`
- 하단 아군 4명 상태 + 상단 얇은 timeline + 우하단 contextual skill dock
- 별도의 `사용` 버튼 없음
- 스킬 첫 클릭 = 선택, 같은 스킬 빠르게 두 번 = 현재 대상이 유효하면 즉시 사용
- 대상 첫 클릭 = 선택, 같은 대상 두 번 = 즉시 사용
- Enter는 키보드 확정 fallback
- RMB 선택 취소
- 스킬 hover 상세 설명 유지
- AUTO/배속/도주는 스킬 dock과 겹치지 않는 별도 하단 control strip

## UI reference policy
alpha.15의 프레임/밀도/툴팁 계층은 Minecraft 모드 UI를 우선 참고한다.
- BetterQuesting (MIT): compact nested frame, quest 정보 계층
- REI: dense framed control / tooltip hierarchy
- 사용자 제공 reference-game screenshots: world-first spatial hierarchy와 target arrow 감각만 참고

외부 픽셀/코드는 alpha.15 JAR에 복사하지 않았다. 세부 출처 정책은 `EXTERNAL_ASSETS.md`.

## 전투 코어
- 서버 정본 SPD Turn Gauge / threshold 1000 / overflow 보존
- 1~4 아군 / 최대 5 적
- 행동 선택 중 logical time 정지
- Basic CD0 / Active owner-action cooldown
- Damage / Heal / Barrier / Gauge / Revive / Guard redirect / Reaction / Status
- 일반 필드전 100% 도주, 보스/이벤트는 encounter data로 금지 가능
- player entity는 생존 캐릭터가 아닌 이동/카메라/session shell

## 다음 제작
이 vertical slice에서 UI·카메라·클릭·자동 시작을 실플레이로 확정한 뒤, 같은 구조를 반복해서 `마을/안전 거점 → 필드 → 마을/거점 → 다음 필드`로 확장한다. PvP/팀전은 미래 확장 후보이며 현재 P2 범위에는 넣지 않는다.
