# TURNBOUND — Drehmal 공식 맵 시험 위치 충돌·공통 순간이동 분리 결과

> **2026-11-10 원작 자동 위치 복귀 경로 선별 차단 검증:** [primordial/root → players:sethost 실서버 원인 확인 및 제9 가역 분리 정본](DREHMAL_PRIMORDIAL_ENTRY_REWARD_NINTH.md). [원작 데이터팩 ON/OFF #42814595461 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42814595461) 이후 [원작 보상 함수 연결 **한 개만** 제거한 2인 실서버 비교 #42817029748 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42817029748)에서 원작 데이터팩은 두 경우 모두 **활성**, 두 서버 모두 **reload** 조건인 상태로, `advancements:primordial/root`의 보상 `players:sethost`를 해제한 쪽의 수평 이동이 두 플레이어 모두 **0블록**으로 사라졌다(유지 사본 공중 210.345·바닥 213.214). [제9단계 독립 7단계 사본 적용/검증/정확 복구 #42818384756 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42818384756). **원작 함수/다른 원본 ZIP member는 그대로**, 원본 SHA 유지. 단, **기존 8단계와 9단계는 아직 결합된 결과가 아니고**, 실제 시설 조작·시작 좌표·멀티플레이·전체 맵 분리는 **미검증**. 다음도 캐릭터/전투 콘텐츠가 아니라 맵 분리·시설 보존이 우선.


> 기준: 2026-11-10 · `turnbound-content-rebuild` · 공식 Drehmal: APOTHEOSIS v2.2.2f · Minecraft Java 1.20.1 / Fabric · **MAP SEPARATION COMPLETE = NO**.

## 목적과 맵 보존 경계

이전 CI의 `(26458.5,223,-285.5)` 이동 후 수평 210.345블록 복귀가 **잘못 잡힌 공중 위치 때문인지**, 또는 **다른 공통 서버/월드 이동 처리 때문인지** 구별한다. 이전처럼 **SHA 고정된 공식 원본은 수정하지 않고, 별도 7단계 가역 분리 사본만 검사**한다. 지도 바이너리·원작 명령·플레이어 NBT·서버 내부 로그를 GitHub에 배포하지 않는다.

## 1. 공식 원본 대 7단계 사본 정적 지형 실측

- [초기 형상 검사 #42807662367 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42807662367): 최초에는 `minecraft:light`를 비공기 장애물로 분류했으나, 이는 **통과 가능한 조명 블록**이다. 최초 분류의 점유 판정을 안전한 충돌 판정으로 활용하지 않는다.
- [정정된 정적 검사 #42808235715 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808235715) · [파생 결과 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808235715/artifacts/13760251862): 원본 `level.dat` 및 `hi_drehmal.zip` SHA-256 확인, 별도 복사본의 7단계 영수증 확인, 관측 위치 세 곳의 블록 ID와 범주가 원본과 완전히 일치.

| CI 전용 위치 | 발 아래 | 발 위치 | 머리 위치 | 정적 판정 |
| --- | --- | --- | --- | --- |
| `(26458.5,223,-285.5)` | 공기 | 공기 | 공기 | **바닥 없는 공중 위치** |
| 기존 복귀 지점 `(26520.5,177,-84.5)` | `light_gray_concrete` | 공기 | `light` | 보수적 바닥·통과 가능한 두 블록 공간 |
| 원래 봇 접속 지점 `(26519.5,177,-82.5)` | `light_gray_concrete` | 공기 | `light` | 보수적 바닥·통과 가능한 두 블록 공간 |

원래 잘못 지정한 공중 CI 좌표의 반경 5·높이 ±12 범위에서 **4개**의 근처 바닥 시험 후보가 발견됐다. 최초 봇 접속 지점 주변 반경 8·높이 ±5에는 **101개**의 보수적 후보가 발견됐다. 이 숫자는 **현 검사기의 whitelist를 만족하는 순수 정적 위치 후보 수**이지, 실제 이동·시설 기능·게임 플레이 경로의 안전성 증명은 아니다.

정적 검사기는 계단/반블록 등 부분 충돌 블록을 안전으로 간주하지 않는다. `minecraft:light`는 비충돌 공간, 바닥은 확인된 바닐라 완전 블록만 허용한다.

## 2. 공식 복사 맵의 실제 서버에서 공중 대 바닥 비교

- [공중/바닥 실제 2인 서버 검사 #42808944435 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808944435) · [파생 좌표 아티팩트](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808944435/artifacts/13760972549).
- 첫 CI 검사는 테스트용 모의 `footprint` 자료에서 필드 누락으로 단위 시험이 실패하여 서버를 부팅하지 않았고([#42808791834](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808791834)), **테스트 데이터만 보완한 재실행이 성공**했다.
- 1.20.1 실제 Fabric 전용 서버. 검증된 공식 맵 7단계 **폐기용 단일 사본**, 원작 START 명령 57개는 공란 상태, command block 서버 기능 OFF, 실제 Minecraft 프로토콜 플레이어 2명. 각각 동일한 위치 패킷 구현으로 관측. TURNBOUND 월드 마커 없음.
- 두 실험 플레이어에게 **같은 서버에서** 각각 다음 위치를 RCON 이동했다.

| 구분 | 실험 이동 위치(절대 게임 START 아님) | 바닥 정적 검사 | 최종 수평 이동 | 최종 서버 위치 |
| --- | --- | --- | ---: | --- |
| Alice | `(26458.5,223,-285.5)` | 공기 (바닥 없음) | **210.345블록** | `(26520.5,177,-84.5)` |
| Bob | `(26458.5,219,-288.5)` | `purple_concrete` 완전 블록 + 발/머리 공기 | **213.214블록** | `(26520.5,177,-84.5)` |

두 플레이어 모두 실험 TP 후 **0~1.004초**에는 위치 유지, **1.610초 관측 시점**에는 똑같은 위치로 복귀했다. 둘 다 오버월드 유지, 연결 유지, 정상 서버 종료, 원본 해시 보존. 이 시험은 **실제 게임 내 UI·문·퍼즐 조작, 4인 인간 멀티플레이가 아니다**.

**판정: 바닥이 없는 특정 좌표라는 이유만으로 이 이동을 설명할 수 없다.** 인접하고 검증된 완전 블록 바닥 위에서도 똑같은 이동이 있었고, 두 플레이어의 최종 위치가 정확히 일치한다. 따라서 플레이어별 물리 낙하 문제 하나로 확정하거나, 문제 해결을 위해 원작 명령 블록/시설을 무차별 삭제하면 안 된다. 다만 여전히 원작 데이터팩, 다른 이동 트리거, Minecraft 서버의 텔레포트/위치 보정 중 어느 것이 발생시켰는지는 미확정이다.

## 3. 코드 및 검증 결과

- `tools/inspect_drehmal_teleport_footprints.py`: 1.20.1 섹션 팔레트에서 발 아래·발·머리·상부 블록과 색상 콘크리트/조명 블록을 **read-only**로 분류. 공식 원본과 7단계 사본을 비교하고 주변 CI 후보만 보고. 대응 단위 테스트 5개.
- `tools/probe_drehmal_grounded_control.py`: 7단계 receipt/원본 SHA/두 지점 지형 사전 검증 → 실제 Fabric 서버의 2인 관측 → 원본 무결성 후검증. 대응 단위 테스트 4개.
- 대응 GitHub CI `.github/workflows/turnbound-drehmal-teleport-geometry.yml` 및 `.github/workflows/turnbound-drehmal-grounded-runtime.yml`. 정식 모드에 임시 적·좌표·원작 함수를 추가하지 않았다.
- **VERIFIED**: 소스 해시, 원본과 7단계 복사본의 지정 블록 형상 동일, command block OFF 서버 부팅/2인 실측/공통 이동, 주어진 57개 START 명령의 차단 유지.
- **NOT VERIFIED**: 이동을 명령한 실제 함수·시스템, 원작 전체 게임성 차단/시설 플레이 가능성, 실제 게임 START·체크포인트, 렌더링 클라이언트/4인 플레이, 최종 맵 분리.

## 4. 다음 작업 우선순위

1. 다른 시설에 영향을 주지 않는 **독립된 공식 7단계 사본 2개로 데이터팩의 원작 동적 이동 트리거 영향 유무를 분리**한다. datapack 전체 제거/비활성은 진단용 사본에서만 수행하고, 정식 맵을 그대로 훼손한 뒤 진행하지 않는다. 데이터팩 비활성 사본에서 월드 로딩/차원/구조물이 달라질 수 있으므로 조건이 바뀌면 비교 무효로 기록한다.
2. 동일 실험에서 각 연결/서버 틱 경과·플레이어 NBT/Dimension·서버 자체 teleport correction 전후를 관측해 **정확한 공통 이동 소유자**를 찾아낸다. 업적 초기 레시피 1,159건을 원작 스크립트 실행의 증거로 오인하지 않는다.
3. 실제 Av'Sal/Foundry/Godmachine/Terminus의 물리 시설 및 월드 동선/퍼즐 보존을 **선택적으로 검증**한 뒤, 원작 스토리·전투·보상 제거 범위를 기능별로 결정한다. 그래픽 클라이언트 검증 및 사용자 승인 전 어떤 CI 좌표도 게임 START로 승격하지 않는다.

**최종 상태: MAP SEPARATION COMPLETE = NO / Original World Untouched / No Player-Facing Content Changed.**
