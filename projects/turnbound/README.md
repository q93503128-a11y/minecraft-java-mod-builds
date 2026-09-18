# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 캐릭터 수집형 파티 턴제 RPG.

현재 제작 버전: `0.1.0-alpha.17` — 기존 전투/성장/저장 시스템을 보존하면서 물리 월드 레이어를 Drehmal: APOTHEOSIS v2.2.2f 기반으로 교체하는 대개편 브랜치다.

## 현재 월드 방식

기존 Aster March의 자체 제작 도로/지형/도시/전투 셀은 더 이상 production 월드가 아니다. TURNBOUND는 별도 설치한 Drehmal 월드에 semantic gameplay anchor를 바인딩하는 구조로 전환 중이다.

현재 첫 개편 체크포인트:
- profile: `turnbound:drehmal_apotheosis_2_2_2f`
- New Drabyel binding seed: `502 67 1801`
- Stasis Facility region seed: `778 31 668`
- 원본 Drehmal world/resource pack은 저장소에 포함하지 않음
- marker가 없는 임의 월드에서는 Aster March terrain builder/spawn sanitizer를 실행하지 않음
- 기존 Aster March minimap production registration 제거
- 기존 전투/캐릭터/성장/보상/save/WAL 코드는 유지

수동 통합 검증 명령:
- `/turnbound world status`
- `/turnbound world bind_drehmal` — 별도 설치한 Drehmal Overworld의 New Drabyel seed 근처에서 OP 권한으로 실행

현재는 **world-binding architecture checkpoint**이며 완성된 외부 월드 vertical slice라고 주장하지 않는다. integration seed는 아직 자동 텔레포트에 사용하지 않으며, 설치 자동화·26.2 migration·terrain-safe arrival/encounter/battle anchor·카메라 개편은 후속 단위다. Build TURNBOUND #754에서 test/build/server smoke/JAR 검증까지 통과했다. 자세한 경계는 `WORLD_OVERHAUL_DREHMAL.md`.

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
alpha.15에서 확정한 프레임/밀도/툴팁 계층을 alpha.17에서도 계승한다.
- BetterQuesting (MIT): compact nested frame, quest 정보 계층
- REI: dense framed control / tooltip hierarchy
- 사용자 제공 reference-game screenshots: world-first spatial hierarchy와 target arrow 감각만 참고

외부 픽셀/코드는 TURNBOUND JAR에 복사하지 않는다. 세부 출처 정책은 `EXTERNAL_ASSETS.md`.

## 전투 코어
- 서버 정본 SPD Turn Gauge / threshold 1000 / overflow 보존
- 1~4 아군 / 최대 5 적
- 행동 선택 중 logical time 정지
- Basic CD0 / Active owner-action cooldown
- Damage / Heal / Barrier / Gauge / Revive / Guard redirect / Reaction / Status
- 일반 필드전 100% 도주, 보스/이벤트는 encounter data로 금지 가능
- player entity는 생존 캐릭터가 아닌 이동/카메라/session shell

## 검증
alpha.17 작업 브랜치는 Java 25 clean test/build, NeoForge dedicated-server boot smoke, JAR metadata/class/v0.4 resource 검증을 CI에서 수행한다. `mod_version`, 런타임 로드 로그, NeoForge metadata, Manifest/JAR 이름이 서로 다르면 검증 실패로 처리한다.

## 다음 제작

TURNBOUND: RE에서 검증된 Drehmal 다운로드/hash/26.2 migration 경로를 TURNBOUND에 맞게 이식한 뒤, 실제 migrated world를 기준으로 첫 거점·탐험 동선·조우 anchor를 검증한다. 그 다음 `FieldSessionManager`의 자체 지형 생성 의존성을 제거하고 terrain-aware battlefield/camera로 첫 external-world vertical slice를 완성한다.
