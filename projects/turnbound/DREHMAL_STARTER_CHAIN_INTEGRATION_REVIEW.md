# Drehmal 24개 시작·발견도 명령 물리 체인 및 6종 오프라인 통합 판정

## 실제 검증

- 대상: 공식 Drehmal APOTHEOSIS 2.2.2f, Minecraft 1.20.1, SHA 확인된 비활성 독립 월드 복사본.
- [공식 물리 체인 전체 감사 #2 — PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733734444): 명령 블록의 1.20.1 palette LongArray, 좌표, NBT 명령 해시, facing, conditional, auto, powered, 주변 반경 2블록과 방향성 체인 실측. 24/24개 원본과 일치.
- [기존 5종 + 24개 여섯 경계 통합 실험 — PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256037): inspect, prepare, verify, restore, 중복 지역 파일의 단계별 SHA와 완전 복구 검증.
- [기존 5종 회귀 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733255944), [기존 4종 회귀 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256158), [24개 독립 시험 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256005).
- 검증 대상 24개는 9개 방향성 연결 컴포넌트, 이 중 22개가 다른 명령 블록과 체인으로 연결되어 있다. 신호 관련 인접 블록은 10/24개 위치에서 관측. 조사된 체인 내 conditional 블록 0개, 무한/초과 추적 0건.

## 시작 아이템 지급 21개 = 원작 6개 시작 체인 중 일부

| x,z (상단 y=214부터) | 전체 체인 | 이번 차단 give | 원작의 나머지 동작 유형 |
|---|---:|---:|---|
| 26458,-288 | 10 | 3 | spawnpoint, execute advancement, execute scoreboard, setblock 2, effect, tp |
| 26461,-288 | 10 | 3 | 위와 동일 |
| 26464,-288 | 10 | 3 | 위와 동일 |
| 26457,-260 | 9 | 4 | execute advancement, execute scoreboard, gamemode, gamerule, spreadplayers |
| 26461,-260 | 9 | 4 | 위와 동일 |
| 26465,-260 | 9 | 4 | 위와 동일 |

6개 체인의 합계는 57블록이며 선택한 give는 21개다. 나머지 원작 명령 36개 중 gamemode 3개는 기존 5종에 포함되어 있다. 따라서 **기존 5종 + 24개 실험에서도 시작 체인의 원작 명령 33개가 차단되지 않고 남는다.**

체인의 대상 블록은 조건부 실행이 아니지만, 이것만으로 나머지 블록의 안전한 비실행을 증명할 수 없다. 특히 월드 구조를 바꾸는 setblock, 이동/부활 위치를 제어하는 tp/spreadplayers/spawnpoint, gamerule이 남아 있어 이 24개만을 원작 시작 기능 제거로 선언할 수 없다. 반대로 33개 일괄 삭제도 구조물 보존을 검증하기 전에는 허용되지 않는다.

## 발견도 지급 3개

- discoveries:homligrave — (2291,31,-2959), 자동 반복 명령 블록, 직접 연결 체인 없음.
- primordial:enteryavhlix — (26907,57,-219), 자동 반복 명령 블록, 직접 연결 체인 없음.
- discoveries:divineruins — (27221,182,-169), 2개 블록의 체인에 위치하며 다른 명령은 gamemode(기존 5종 차단 대상). 가까운 별도 spawnpoint 명령도 확인됨.

두 개가 직접 연결되지 않았다는 사실은 감지 조건·원작 스토리·시설 의존성까지 없다는 증거가 아니다.

## 통합 범위와 안전 경계

- 기존 prepare_drehmal_isolated_copy.py에 추가한 --include-starter-experiment는 명시적 오프라인 실험 옵션이다. --include-gamemode와 SHA 검증된 --direct-report가 함께 있을 때만 24개를 다섯 번째 경계 뒤에 적용한다.
- 기존 4종 기본과 5종 옵션은 그대로 유지한다. 통합 실험은 SHA 중첩과 정확 복구를 증명하지만, 원작 시설·버튼·문·입구·포탈의 기능 및 실제 TURNBOUND 입장/멀티플레이 안전성을 증명하지 않는다.
- 원작 명령 본문·함수/JSON 로직을 TURNBOUND에 이식하거나 재사용하지 않았다. 원본과 변경 지도 파일을 저장소·아티팩트에 게시하지 않았다.

## 바로 다음 작업

1. 원작 시작 장치의 6개 루트/트리거를 실제 서버·클라이언트의 비활성 복사본에서 재현한다. 남은 33개 명령 중 원작 보상/스폰/이동과 구조물·장치 유지 기능을 구분한다.
2. TURNBOUND가 소유하는 시작 위치·리스폰·게임모드 관리 경로를 먼저 검증한 후, 맵 보존 증거가 있는 기능만 추가 가역 경계에서 차단한다. 원작 코드를 복사해 대체하지 않는다.
3. 차단 후 기존 지형·문·포탈·시작 시설과 단독/멀티플레이의 플레이 흐름을 확인한다. 원작 예약 실행, 다른 업적·명령·점수판/스토리지도 별도 선별 검토한다.

**검증:** CODE REVIEWED, ORIGINAL WORLD OFFLINE TESTED, EXACT RESTORE VERIFIED.
**미검증:** PLAYTESTED, MULTIPLAYER TESTED, STRUCTURE FUNCTIONALITY TESTED, MAP SEPARATION COMPLETE.

## 2026-11-10 — 7번째 시작 장치 잔여 명령 분리 실험

- [GitHub Actions 공식 7단계 분리 검사 #1 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42735713657). 공식 v2.2.2f 별도 비활성 사본에서만 실행.
- 앞서 검증한 5종과 지급/발견도 24개 처리 이후에 남은 시작 명령 33개를 독립적인 7번째 실험 경계로 추가했다: tp 3, spreadplayers 3, spawnpoint 3, setblock 6, effect 3, gamerule 3, execute-scoreboard 6, execute-advancement 6.
- 모든 대상은 region/r.51.-1.mca의 확인된 명령 블록 33개로, 공식 직접 명령 보고서 SHA 및 개별 위치/원본 명령 SHA를 기준으로 패치했다.
- 결과: 기존 24개와 5종 중 게임모드 3개, 신규 33개를 합쳐 **시작 장치 6개 체인 57개 블록의 Command 필드가 모두 빈 문자열**임을 실제 지역 재스캔으로 검증했다.
- 원본 지역 백업·영수증을 변경 전에 보존하고, 7단계 전후 SHA를 검증했다. 7번째 차단을 해제해 앞선 6단계 분리본과의 SHA를 확인하고, 6단계 자체도 모두 원본 해시로 복구했다. 합성 단위 테스트 3개 PASS.
- 기존 4종 기본 및 5종 옵션이나 24개 선택형 실험 동작을 바꾸지 않았다. 도구: tools/verify_drehmal_start_chain_isolation.py. 이 검증 도구는 사본에 6단계가 완료된 때에만 사용한다.
- 원작 명령 블록의 지형/레드스톤/시설은 삭제하지 않았지만 원래 setblock 명령에 의한 실행 시점의 시설 변화는 차단된다. 따라서 **시작 장치, 문, 포탈, 리스폰, 레드스톤의 실제 기능 보존은 미검증**이다. TURNBOUND 시작 위치·리스폰의 서버 소유 시스템도 아직 구현되지 않았다.
- 원작 명령/함수 코드 재사용 0건. 공식 지도 원본과 변경 사본은 GitHub 또는 artifact에 게시하지 않았다.

**다음:** 원작 시설과 시작 트리거의 전후 실제 클라이언트/서버 동작을 확인하고, 승인된 TURNBOUND 시작 위치·리스폰 정책이 정해질 때 서버 소유로 구현한다. 다른 수많은 원작 실행 경로는 별도 선별 분리가 필요하므로 전체 맵 분리 완료는 아님.

## 2026-11-10 Fabric 1.20.1 실서버 장면 비교 (6단계 vs 7단계)

- [GitHub Actions #42738739482 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42738739482). SHA 고정된 공식 원작에서 **6단계 및 7단계 비활성 월드 사본을 각각 새로 생성**하여 동일 Fabric 1.20.1 전용 서버에서 순차 실행하고 서버 RCON으로 시작 영역 강제 로드 및 서버 틱을 확인했다. 원본 지도와 실제 사용자 월드는 단 한 번도 서버에 올리지 않았다.
- **6단계:** 전용 서버 부팅/강제 로드/정상 종료 PASS, 관측 169틱, 원작 시작 명령 57개 중 비어 있는 명령 24개. **7단계:** 동일 항목 PASS, 관측 165틱, 57개 중 57개 비어 있음.
- 각 사본에서 동일한 시작 체인 6곳의 원래 공기 블록에 **임시 레드스톤 블록 6개를 실제 명령으로 설치**했고, 그 외 주변 **1,440개 지형/장치 블록 상태의 전후 변화는 두 서버 모두 0개**. 최초 구조 블록 상태 SHA-256은 6·7단계 간 일치. 검사한 대상 명령 블록 57개는 양쪽 런타임 후에도 모두 존재하고, 지역 스캔 경고는 0개.
- 이 결과는 정해진 구간/시간의 정적 지형 변화가 없었다는 뜻이지, 원작 입장 체인의 실제 사용 또는 시설 기능 보존 성공이 아니다. 이 테스트에는 **실제 플레이어가 0명**이었으므로 @p/@a 등 플레이어 대상 실행 조건을 재현하지 못한다. 실제 레버/문/포탈 사용, 플레이어 조작/리스폰, 기능 UI, 2인 멀티 및 TURNBOUND 게임 활성화는 **NOT TESTED**.
- 서버가 실제로 수정한 임시 사본은 각각 폐기했으며, 월드 파일·원작 NBT 명령문·서버 원본 로그/UUID·실행 가능한 맵을 아티팩트에 게시하지 않았다. 오직 SHA와 실행 수치, 허용된 블록 유형 차이 등 파생 메타데이터만 게시했다.
- 신규 재현 절차: .github/workflows/turnbound-drehmal-start-scene-runtime.yml (수동 workflow_dispatch 지원), tools/probe_drehmal_start_runtime.py 및 대응 회귀 테스트. 프로젝트/게임 코드/원작 시스템/승인 기획은 수정하지 않았다.

**판정:** DEDICATED SERVER SCENE SMOKE VERIFIED. STRUCTURAL FUNCTIONALITY / PLAYER JOIN / RESPAWN / MULTIPLAYER / GAMEPLAY **NOT TESTED**. 실제 플레이 월드로 승격하지 않는다.

**다음 실작업:** 두 사본 각각에서 실제 플레이어가 오리지널 입장 기구를 작동시키는 실측 경로 마련, server-authoritative TURNBOUND 시작 위치/리스폰의 승인된 위치 확인, 원작 기타 장치·업적·점수판 재진입 차단. 실행 조건/플레이어 효과를 검증하지 않은 원작 코드 재사용 금지.
