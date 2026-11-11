# TURNBOUND — Rebuild project contract

> **2026-11-11 Foundry 독립 어댑터 실서버 검증 성공:** [독립 시설 검증 #42952475774](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42952475774) **PASS**. 원본 `minecraft:tick` 자동 함수 2개를 차단한 7+9 폐기 사본에서, TURNBOUND 자체 서버 어댑터를 활성화하자 원본 레버 3개 실제 우클릭 후 문 근처 접근에 따라 **정확히 59개 대상 블록이 변경**, 소스와 대상 90개 전체 블록 상태가 일치함(차이 **59→0**). 원작 문 함수 호출 0. [미활성 대조 #42952475741](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42952475741) **PASS**: 마커 없는 사본은 차이 59 그대로. [Fabric 빌드 #42952475746](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42952475746) **PASS**. **문 통행·원작 웨이브 비재진입·실제 화면·4인 멀티·전체 맵 분리 미검증.** [상세](DREHMAL_FOUNDRY_INDEPENDENT_GATE_ADAPTER.md). MAP SEPARATION COMPLETE = NO.


> **2026-11-11 Foundry 자동 tick 단절의 원인 확정:** 공식 7+9 사본 준비 도구가 `minecraft:tick`에서 `entities:main`을 포함한 원작 진입점 두 개를 **의도적으로 제거**한다. 원본 ZIP의 호출 경로는 존재하지만 사본에서는 자동 실행되지 않는다. 레버·점수·문 주변·상위 조건·`@a` 선택은 모두 정상, 문 복제 함수도 명시적 테스트에서 정상이다. 원작 전체 tick 복원 금지, 시설별 독립 어댑터 검토. [원인/증거](DREHMAL_FOUNDRY_TICK_ROOT_CAUSE.md). 맵 분리 NO.


> **2026-11-11 Foundry 전체 엔티티 조건 자연 검증 #42944727206 PASS:** 원작 점수 3/레버 ON, 반경 12 내 플레이어 및 원작 `players:is_not_dev` predicate **모두 충족**, 그런데 자연 문 clone 0변경(소스/대상 59블록 차이 유지). 원작 문 직접 호출 0. 다음은 `players:main` 계열 실제 실행 및 실행 컨텍스트를 검증해야 함. [상세](DREHMAL_FOUNDRY_NATURAL_VS_EXPLICIT_GATE.md). MAP SEPARATION COMPLETE = NO.


> **2026-11-11 Foundry door 호출자 정적 #42942060986 PASS:** [원본 5,487개 함수 추적](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42942060986): 원래 levers/main의 matches 1/2/3은 모두 tellraw, 조건은 동일 점수 키. levers/1_check~3_check의 업적 동사는 revoke. door 함수의 발견된 원본 리터럴 호출자는 players:overworld_p 하나. 다음은 해당 상위 함수 실행 경로와 조건 분리. 시설/원작 함수 변경 없음, 전체 맵 분리 NO. [상세](DREHMAL_FOUNDRY_NATURAL_VS_EXPLICIT_GATE.md).


> **2026-11-11 Foundry 자연 업적/score 실서버 #42940827429 PASS:** [증거](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42940827429). 세 레버 우클릭에 따라 levers/main 관련 원작 scoreboard 값 없음→1→2→3, matches 1/2/3 순차 충족, 무입력 대조 구간 변경 0. advancement 저장 완료 상태 false는 check 함수 내부 advancement 명령에 의한 회수 가능성 때문에 자연 보상 미발동 증거가 아님. 자연 입력 뒤 문 90블록의 source/target 차이 59개 유지. main 함수 내 door 리터럴 직접 호출 없음. [상세](DREHMAL_FOUNDRY_NATURAL_VS_EXPLICIT_GATE.md). 수정·배포/실사용 승인 없음. MAP SEPARATION COMPLETE = NO.


> **2026-11-11 Foundry 원인 분리 최신 정본:** [원본 90블록 source/destination 조건·상태 CI #42935102184 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935102184): 복제 전 90블록 중 **59개 전체 상태 불일치**, 원본 source 공기 35칸·닫힌 destination 공기 0칸. `foundry:levers/main`은 점수 matches 1/2/3 조건 3개. [자연 레버 3개 우클릭 vs 원작 문 함수 1회 직접 호출 실서버 #42935729498 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935729498): 원작 데이터팩·명령 블록 ON, 7+9 일회성 사본에서 실제 막대기 우클릭 후 레버 3개 ON·원작 점수판 항목 각 2건 변화, 문 대상 **0블록 변경**, source와 대상 59개 차이 유지. 같은 사본에서 진단용으로 `function foundry:levers/door`를 1회 직접 실행하자 대상 **59블록 변경**, source/dest **90블록 전체 상태 완전 일치**. 즉 문 복제 함수는 작동하며 **자연 레버 이벤트부터 문 함수 실행 사이 경로가 미충족/미실행인 문제**로 범위가 좁혀졌다. 정확한 조건/원인은 아직 미확정. 문 통행·원작 전투 비재진입·실사용 START·멀티/그래픽 클라이언트/맵 분리 완료는 전부 NO.

> **2026-11-11 Foundry 실제 시설 실서버 최신 정본:** [3개 레버 동시 ON #42929635789 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42929635789) → [원작 문 clone/source + 웨이브 호출 조사 #42931261435 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42931261435) → [정확한 clone 대상 라이브 검사 #42931701776 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42931701776) → [업적 predicate 검사 #42932597961 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42932597961) → [실제 손 아이템 우클릭/점수판/문 #42933135850 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42933135850). 원작 데이터팩·명령 블록 ON인 7+9 폐기 사본에서 레버 3개 모두 OFF→ON. 막대기 장착 클릭마다 **점수판 기록 변경 2건** 관측(다른 배경 원인 배제 못함). 그러나 실제 foundry:levers/door clone 대상 (-3278..-3274, Y156..164, Z1576..1577)의 블록 ID 변경 **0건**. 원작 함수 실행·정확한 문 개방/통행·6웨이브 미재진입 등은 **미검증**이며, 원본 clone 전후 소스·대상 비교와 levers/main 삼중 조건 검사 우선. 공식 맵·실사용 START 승인 = NO.


> **2026-11-10 Drehmal 실제 시설 사용 최신 정본:** [7+9 공식 맵 사본 실제 레버 조작 #42824376001 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42824376001). 7단계와 `primordial/root` 강제 복귀 보상 연결 차단(9단계)을 적용한 공식 임시 사본에서 **원작 데이터팩·명령 블록 활성**, Fabric 1.20.1 프로토콜 클라이언트 실제 우클릭으로 Foundry 레버 3개 모두 첫 클릭 상태 전환 / 두 번째 클릭 복원 관측. [시설 좌표/물리 후보 원본·사본 비교 #42823505301 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42823505301). 8단계의 9개 실사용 차단은 미승인 상태로 이 시험에 **포함하지 않았다**. 문 개방·퍼즐·원작 전투 비재진입·실제 그래픽 클라이언트·멀티 미확인. **전체 맵 분리와 실사용 START 승인 아님.**


> **2026-11-10 8+9 통합 실험 최신 상태:** [8+9 통합 정본](DREHMAL_EIGHT_NINE_COMBINED_PREVIEW.md) · [공식 원본 사본 CI #42822471453 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42822471453). 독립 8단계(9건)와 9단계(1건)를 동일 7단계 사본에 결합, 양방향 순서 결과 일치 및 원본 정확 복원 검증. **이는 오프라인 사본 무결성 시험이지 8단계의 실사용 안전성이나 문·퍼즐·포탈 작동 승인 아님. MAP SEPARATION COMPLETE: NO.** 다음은 원작 시설 실제 물리 조작과 원작 재진입 조건 선별 판정.


> **2026-11-10 원작 자동 위치 복귀 경로 선별 차단 검증:** [primordial/root → players:sethost 실서버 원인 확인 및 제9 가역 분리 정본](DREHMAL_PRIMORDIAL_ENTRY_REWARD_NINTH.md). [원작 데이터팩 ON/OFF #42814595461 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42814595461) 이후 [원작 보상 함수 연결 **한 개만** 제거한 2인 실서버 비교 #42817029748 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42817029748)에서 원작 데이터팩은 두 경우 모두 **활성**, 두 서버 모두 **reload** 조건인 상태로, `advancements:primordial/root`의 보상 `players:sethost`를 해제한 쪽의 수평 이동이 두 플레이어 모두 **0블록**으로 사라졌다(유지 사본 공중 210.345·바닥 213.214). [제9단계 독립 7단계 사본 적용/검증/정확 복구 #42818384756 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42818384756). **원작 함수/다른 원본 ZIP member는 그대로**, 원본 SHA 유지. 단, **기존 8단계와 9단계는 아직 결합된 결과가 아니고**, 실제 시설 조작·시작 좌표·멀티플레이·전체 맵 분리는 **미검증**. 다음도 캐릭터/전투 콘텐츠가 아니라 맵 분리·시설 보존이 우선.


> **2026-11-10 공식 Drehmal 실제 서버 공중/바닥 대조 검증:** [지형·착지/공통 복귀 최신 정본](DREHMAL_TELEPORT_FOOTPRINT_SERVER_COMPARISON.md). 원본/7단계 복사본의 실제 블록 팔레트를 비교하여 기존 Y223 목표가 **공중**임을 확인. 그 근처 **Y219의 purple_concrete 바닥 + 발/머리 공기**인 별도 시험 위치를 확보하고, [CI #42808944435 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808944435)에서 2인 동시 실측한 결과 **공중·바닥 위치 모두 약 1~1.6초 후 정확히 같은 (26520.5,177,-84.5) 좌표로 복귀**했다(각 수평 210.345/213.214블록). 명령 블록 OFF, 57 START 명령 차단, TURNBOUND 활성 마커 없음. 따라서 **공중 위치만을 원인으로 간주하지 않으며**, 다음 대상은 원작 남은 동적 데이터팩/서버 공통 이동 경로의 분리다. **맵 분리 완료·게임 START 승인·시설 기능 검증은 여전히 NO.**


> **최신 Drehmal 맵 분리 검증(2026-11-10):** [원작 시작 위치 비정상 복귀 시계열·대조 정본](DREHMAL_ENTRY_POSITION_DISPLACEMENT_TRIAGE.md). 공식 2.2.2f의 독립 7단계 사본에서 RCON 위치 이동 후 수평 **210.345블록** 복귀는 command block ON/OFF 모두 동일([#42795308304](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42795308304)); 합성 flat world에서는 재현 안 됨([#42796956789](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42796956789)); 공식 지도에서 vanilla-like position packet을 보내도 동일([#42798205522](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42798205522)). 따라서 원작 명령 블록이나 봇 ACK만 원인으로 단정 금지. **실제 CI 지점 발/머리 충돌과 서버 이동·업적 루트 추가 검사가 다음 우선순위.** 프로덕션 전투/캐릭터 구현을 우선하지 않고, 맵 분리 완료/정식 START 승인도 선언하지 않는다.


> **2026-11-10 실제 Drehmal 시설 콜백 검증 최신 결과:** [Actions #42788411203 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42788411203)에서 7·8단계 임시 공식 맵의 가상 플레이어 2명씩으로 Foundry/Godmachine/Av'Sal 원작 **업적 콜백 강제 호출**이 동일한 점수판 상태 변경을 남기는지 관측했다. 실제 시설 조작은 미검증. 또한 시험용 START 인접 위치에서 X+62,Y-46,Z+201 이동이 양쪽 모두 발생, 원인은 불명확. **게임 시작 좌표를 승인하거나 맵 분리 완료로 선언하지 않는다.** [현행 분리 보고서](DREHMAL_FACILITY_CALLBACK_RUNTIME.md). 새 캐릭터/전투 콘텐츠 개발보다 맵 분리 검증이 우선이다.


> **최우선 작업: Drehmal 맵 분리(2026-11-10 현재 미완료).** [실사용 승인 게이트](DREHMAL_MAP_SEPARATION_ACCEPTANCE.md)를 우선 확인한다. 공식 2.2.2f 맵을 7·8단계로 분리한 **각각의 폐기용 사본**에서 실제 Minecraft Fabric 서버에 가상 플레이어 2명 접속, START 57개 차단 명령 유지, 시작 영역 1,440블록 무변경 및 **한 개 원작 보상 콜백의 태그 효과 차단**을 확인했다([Actions #42772502908 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42772502908)). **이것은 원작 전체 기능 분리·문/포탈/퍼즐 보존·정식 START 선정·실게임 플레이 완료가 아니다.** 다음 우선순위는 원작 시설 실동작 및 남은 원작 서사·보상 분리이며 **캐릭터·몬스터·전투 UI 신규 콘텐츠 제작은 그 이후**다. CI 임시 위치를 게임 시작 좌표로 사용하지 않는다.


> **Current game-design decision index**: [GAME_DESIGN_CANON_INDEX.md](GAME_DESIGN_CANON_INDEX.md) is the concise 2026-11-09 status map for user-approved gameplay rules, pending decisions and superseded older proposals. This file remains the technical project contract; earlier appended historical sections may conflict with later approved decisions. Do not treat a stale 'TBD' in an older section as active canon.

> **현행 기술 정본 (2026-11-09, 이전 기록보다 우선)**: 아래 과거 26.2 / NeoForge / Java 25 안내는 역사적 기록이며 현재 개발/설치 기준이 아니다. 현행 게임은 **Minecraft 1.20.1, Fabric 0.16.14, Fabric API 0.92.6+1.20.1, Java 17, Fabric Loom 1.9.2, Gradle 8.12**로 빌드한다. Drehmal: APOTHEOSIS v2.2.2f의 별도 복사 월드를 이용하는 턴제 RPG다. 모든 생존·채굴·건축은 비활성화하는 방향이며, 정식 월드/전투 연동은 아직 미완이다. [전투 세션 브리지](BATTLE_SESSION_FABRIC_BRIDGE.md) · [Drehmal 분리 현황](DREHMAL_DATA_PACK_SEPARATION.md).

> **Drehmal 원작 코드 활용의 승인 경계 (2026-11-09, 사용자 명시):** 원작 맵 지형/건축물을 보존하며 원작 시스템을 분리하는 것은 현재 작업 범위다. 그러나 `Drehmal`의 원본 함수·명령·JSON 동작·Terminus/기타 시스템을 TURNBOUND 게임 로직으로 **복사, 재사용, 이식, 각색하거나 기존 기획을 바꾸는 일은 별도 사용자 승인 전 금지**한다. 쓸 만한 기술을 발견하면 파일 ID, 실제 역할, 장점/의존성/라이선스와 기존 TURNBOUND 시스템 중복 여부를 **재사용 후보(미승인)**로만 보고한다. TURNBOUND 역참은 원래 승인된 기획이며 Terminus로 대체하지 않는다. 개별 원작 코드를 채택하지 않고, 검증된 복사본에서 원작 게임 기능을 끄는 분리 작업은 위 범위 안에서 계속할 수 있다. **맵 전체 분리는 아직 미완료**다.

> **통합 맵 분리 체크포인트 (2026-11-09):** [공식 복사본에서 무기 업적 보상 39개 + 자동 실행 진입점 3개 + Terminus 명령 블록 62개 동시 차단·완전 복구 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42603106525). 원작 [비함수 명령·예약·업적 감사 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591982023). **최종 맵 분리 미완료, 실플레이 미실시, 원작 코드 재사용 0건.** [상세 기록](DREHMAL_DATA_PACK_SEPARATION.md).

> **원작 업적·예약 함수 분류 최신 검증 (2026-11-09):** [#1 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42605435141). 무기 외 업적 콜백 181건, 예약 함수를 포함한 함수 672개 및 정적 순환 29개 분류. 상당수가 기존 맵 구조물과 연관될 수 있어 **일괄 비활성화 금지**. 원작 코드 재사용 0건, 맵/전투 코드 변경 0건. [자세한 검토표](DREHMAL_CALLBACK_SCHEDULE_TRIAGE.md).

> **원작 저장 예약 이벤트·4종 가역 분리 검증 (2026-11-09):** [실제 저장 이벤트 감사 #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42607952302) **SUCCESS** — `level.dat`의 원작 보상 이벤트 `terminus:gifts/respawn_gift` **1건**이 저장 시점 131틱 뒤 실행되도록 예약되어 있었음. 저장된 scoreboard objective 539개, `command_storage_*.dat` 파일 12개도 읽기 전용 확인. [4종 통합 복사본 검증 #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42608494226) **SUCCESS** — 예약 이벤트 1건 + 무기 업적 보상 39개 + 자동 진입점 3개 + 원작 Terminus 명령 블록 62개를 같은 별도 월드 사본에서 차단하고 원본 `level.dat` / 데이터팩 / 지역 파일로 정확히 복구. **맵 전체 분리 및 실플레이 완료는 아님, 원작 코드 이식 없음.** [상세 내용](DREHMAL_SAVED_REWARD_PREVIEW.md).

> **4종 분리 상태의 별도 복사본 독립 재검증 (2026-11-09):** [공식 월드 CI #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42616136730) **SUCCESS**. `inspect → prepare → verify`로 원본 월드 복사본을 네 가지 원작 기능 차단 상태로 유지한 후 별도 재검증, `restore`로 전체 원본 해시 복구. 원작 무기 보상 39개/자동 진입점 3개/Terminus 62개(30 region)/저장 예약 보상 1건. **원본/복사본 월드를 GitHub에 업로드하지 않음. 러너 임시 복사본은 검증 후 복구·폐기. 이것은 게임 실행 가능 판정 또는 전체 원작 게임 분리 완료가 아니다.** [세부 도구](DREHMAL_ISOLATED_COPY_PREPARATION.md).

> **원작 게임모드 강제 전환 차단 + 5종 통합 분리 상태 (2026-11-10):** [공식 월드 5종 검증 #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42725309538) **SUCCESS**, [기존 4종 호환 검증](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42725309921) **SUCCESS**. `gamemode` 직접 실행 명령 블록 **29개(오버월드 26, space 차원 3, 7 region)**를 새 다섯 번째 분리 경계로 추가. 기존 원작 `load/tick` 진입 3, 무기 업적 보상 39, Terminus 명령 62, 저장 예약 선물 1과 함께 공식 월드의 **별도 오프라인 사본**에 적용·독립 재검증·정확한 복원을 확인. **전체 맵 분리/시설/실플레이/멀티 검증 아직 미완료. 원작 코드 재사용 없음, 월드 파일 배포 없음.** [기술 상세](DREHMAL_GAMEMODE_COMMAND_SEPARATION.md).

> **2026-11-10 원작 비무기 업적 콜백 181건 교차 의존성 감사:** [보정된 공식 CI #42743577751](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42743577751) **SUCCESS**, 회귀 6개 PASS. 검증된 기존 4종 메타데이터의 SHA를 대조해 원작 함수 호출·예약 실행·명령 블록 리터럴 호출·점수판/스토리지 표본 참조를 통합. 전체 181건 중 예약 실행 경로 45, world_edit 42, teleport 정적 분류 138, 위치/차원 감지 53, 점수판 표본 일치 53, 저장소 표본 일치 4, 정의되지 않은 함수 경로 5건. **173건은 구조/상태 의존 우선 보존으로, 기존 8단계 9건 중 나머지 8건도 오직 실험 단계**로 분류. 특히 기존 9건 중 `kill_with_bowblade`에서 `bowbladEffect` 점수판 참조 표본이 발견되어 우선 보존·추가 조사로 상향. 해당 분류는 제거 허가가 아니고 실제 월드·TURNBOUND 코드 변경 0건. [세부 보고](DREHMAL_CALLBACK_MECHANISM_CROSS_REVIEW.md).

> **2026-11-10 업적 보상 연결 9건: 8번째 오프라인 가역 실험 검증:** [공식 Drehmal 2.2.2f 분리·복원 CI #42741395799](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42741395799) **SUCCESS**. 무기 외 업적 콜백 181건 중 기존 정적 조사에서 다른 함수/월드 변경/예약 실행 연관이 관측되지 않은 비위치 이벤트 9건만 별도 일시 분리. 업적 달성 조건과 나머지 ZIP 구성원 내용을 100% 보존했으며, 8→7→6단계 순서로 기존 영수증·해시 확인 및 원본 완전 복구 PASS(회귀 테스트 3건). **위험 0이라는 뜻이 아님**: 해당 9개도 실제 효과·시설 의존성은 미검증. 나머지 콜백 172건은 일절 건드리지 않음. 이 실험은 기존 4/5종 정상 분리에 편입하지 않았고 **실제 플레이/배포/원작 코드 재사용/원본 월드 수정 없음**. [8단계 상세 기록](DREHMAL_NONWEAPON_CALLBACK_EIGHTH_PREVIEW.md).

> **2026-11-10 필드 적 감지→접촉→전투 패배 체크포인트 복귀 기술 검증:** [Fabric Build #42767437269 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42767437269), [접촉→전투 #42768225763 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42768225763), [접촉→전투 패배→복귀 통합 #42768714927 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42768714927). 서버가 등록한 대표 적 1마리에 대해 시야 감지·경고 대기·추격·실제 접촉 선점, 파티 세션 생성·서버 패배 판정·원위치가 아닌 안전 체크포인트 복귀를 **폐기용 평지 월드의 가상 프로토콜 플레이어 2명으로** 검증했다. 경고 즉시 전투 금지, 다른 플레이어 무단 참전 금지, 패배 뒤 아이템 보존 검증. **실제 적/로스터·드롭·승리 보상·정식 UI/애니메이션·스폰 정책·Drehmal 퍼즐·4인 실플레이는 미구현/미검증.** 정식 JAR에는 CI-only 테스트 적·캐릭터 및 콘솔 명령을 넣지 않았고 원작 자산도 배포하지 않았다. [상세](FIELD_ENCOUNTER_SERVER_BOUNDARY.md).

> **2026-11-10 TURNBOUND 프로토콜 플레이어 입장/사망 회귀검증:** [실제 Fabric 2인 접속·재접속·2회 서버 구동 #42753458094 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42753458094), [Fabric Build/JAR #42753458067 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42753458067). 폐기용 새 평지 월드에서 *가상 Minecraft 1.20.1 프로토콜 클라이언트* 2명 동시 접속, 명시적 CI 시작점 이동, 다른 플레이어 위치 독립, 동일 UUID 재접속, 서버 재시작 시 개인 위치/체크포인트 기록 유지 확인. `/kill`은 플레이어를 **죽이지 않고 기존 위치에 유지**, HP 양수와 테스트 다이아몬드 보존을 확인했다. **`/kill`에 의한 체크포인트 텔레포트는 발생하지 않았고, 실제 파티 패배 복귀/리스폰/원작 Drehmal 시설 및 4인 실플레이는 미검증**. 임시 사망 Mixin 실험은 되돌려진 채 해당 CI가 통과함. [기술·회귀 기록](TURNBOUND_ENTRY_CHECKPOINT_SERVER.md).

> **TURNBOUND 서버 진입·개인 체크포인트 코드 구현 (2026-11-10):** [Fabric Build #42748867041 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42748867041), [폐기용 빈 월드 Dedicated Server #42749364808 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42749364808). 원작/다른 월드 비활성 게이트 유지, 명시적 미승인 초기 위치 설정 시 자동 이동 없음, UUID별 checkpoint `PersistentState`, 서버 측 체크포인트 등록·패배 복귀 API, 렌더 없이 안전 좌표 검사 및 치명타 보호 경로. **확정 좌표 0건·실제 플레이어 입장/리스폰/파티 패배 연결/시설/멀티 테스트 0건.** 원작 맵 수정·코드 재사용 0건. [기술 문서](TURNBOUND_ENTRY_CHECKPOINT_SERVER.md).

> **2026-11-10 Bowblade 원작 점수판 정적 진입 경로 분리 검증:** [공식 #42747001893 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42747001893). 원작 명령/함수 그래프와 직전 18,771개 SHA 검증 결과를 재활용해 `bowbladEffect` 참조 함수 5개를 역추적: **4개는 `core:init`/`entities:main` 원작 load/tick 자동 루트만**, **1개는 `kill_with_bowblade` 업적 보상만** 알려진 리터럴 진입점. 이 정적 그래프의 해당 5개에 도달하는 별도 명령 블록/다른 업적 진입 경로는 관측되지 않았다. 기존 8단계 **비활성 사본 실험**에서 이 알려진 진입점 5개는 자동 태그 3개 및 9개 업적 실험 차단의 대상이 된다. Avsal/Foundry/Godmachine 시설 콜백 3개는 각각 별도 업적 보상 연결이 유지된다. 회귀 4개 및 공식 원본 재검증 PASS. **동적/저장된 실행, 실제 시설 기능, 플레이어/멀티는 미검증**이고 실제 게임용 분리 승인 0건 유지. [상세](DREHMAL_BOWBLADE_ENTRYPOINT_BOUNDARY_REVIEW.md).

> **2026-11-10 원본 함수 18,771개 해시 대조 + 콜백 12개 실제 명령 종류 조사:** [공식 #42745349874 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42745349874). 원본 공식 v2.2.2f 전체 mcfunction의 SHA를 확인하고 기존 9개 실험 콜백 + 중요 시설 3개 콜백의 명령 종류를 직접 분석했다. bowbladEffect 점수판 참조 함수 **5개**(기록된 참조 역할: 쓰기 3, 조건 참조 6, 목적 추가 1). **avsal 터미널 정적 6개 함수에 블록 변경 4, teleport 1, 예약 실행 2**가 있어 시설 보존 우선. **foundry 레버 정적 3개 함수는 직접 tp/teleport 0이나 scoreboard 2**; 원래 그래프 분류의 teleport 범주와 실제 명령의 차이 기록. 8번째 실험 9개는 여러 효과/팀/태그/능력치/점수판 변경을 포함하므로 실사용 일괄 차단은 **미승인**. 원본 파일 수정/원작 코드 이식 0, 실제 장치 작동/플레이 미검증. [명령 효과 보고](DREHMAL_ORIGINAL_CALLBACK_COMMAND_EFFECTS.md).

> **2026-11-10 실제 Fabric 1.20.1 전용 서버 6/7단계 비교:** [전용 서버 장면 검사 #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42738739482) **SUCCESS**. 공식 원작에서 각각 새로 분리한 **비활성 임시 월드 사본** 두 개를 Fabric 서버로 순차 실행하고 시작 영역을 강제 로드했다. 6단계는 169틱/원작 시작 체인 차단 24/57, 7단계는 165틱/57/57. 양쪽 모두 **6곳에 임시 레드스톤 설치**, 1,440개 구조 블록 상태의 전후 검사에서 인위적 6블록 외 변화 **0건**, 시작 시 블록 상태 SHA 완전 일치, 정상 서버 종료. **실제 플레이어 입장·원작 장치 상호작용·문·포탈·리스폰·멀티 검증 아님**(플레이어 0명); 기능 보존/전체 맵 분리/게임 플레이 가능 판정 금지. 독립 사본만 서버에 로드했으며 TURNBOUND 활성 마커를 만들거나 원작 코드·월드를 배포하지 않음. [자세한 기록](DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md).

> **2026-11-10 원작 시작 장치 57개 명령 일괄 차단 가역 실험:** [7번째 분리 실험 CI #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42735713657) **SUCCESS**. 기존 5종 + 24개를 적용한 별도 비활성 복사본에서 원작 시작 체인의 남은 직접 명령 **33개**를 SHA·좌표·명령 유형으로 검증하고 비운 뒤, 시작 장치 **6개 체인 57개 명령이 모두 빈 문자열**임을 원본 지역 파일 재스캔으로 확인. 7번째 경계 및 선행 6개 경계를 역순 복원하고 원본 해시 검증 PASS. **지도 전체 분리, 실제 시설·시작 위치·리스폰·멀티플레이 기능 테스트를 의미하지 않으며 실제 게임용 월드에는 미적용.** 원작 코드 이식·재배포 없음. [상세 기술 기록](DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md).

> **원작 시작 장치 체인 실측·오프라인 6종 검증 (2026-11-10):** [물리 체인 감사 #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733734444) **PASS** — 24개 대상 중 22개가 체인에 연결되며, 시작 아이템 21개는 6개 시작 체인(총 57개 블록)의 일부다. [5종+24개 해시·복원 통합 검사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256037) **PASS**지만 원작 시작 체인 명령 33개가 남는다. **실제 시설·게임플레이는 미검증, 정식 5종 분리에는 24개를 아직 포함하지 않음.** 추가 24개는 명시적인 비활성 복사본 실험 옵션만 허용. 원작 코드 재사용·배포 없음. [체인별 상세 보고](DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md).

> **원작 시작 아이템·발견도 지급 24개 복사본 분리 실험 (2026-11-10):** [직접 명령 검사 #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42729125584) **SUCCESS** — 72개 대상 지역의 명령 블록 1,940개를 원본 명령 해시로 재검증, 직접 명령 268개 분류, 좌표 없는 summon 6개 보존 검토. [별도 사본 분리·정확 복원 CI](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42729892272) **SUCCESS** — 원작 시작 아이템 지급 21개와 발견도 업적 지급 3개를 **4개 지역 파일에서** 차단·독립 재검증·원본 해시 복원. **기존 5종 통합 분리본에 아직 편입하지 않았으며, 문/레드스톤 체인/퍼즐/실제 게임·멀티 플레이 호환성 미검증. 원작 코드 이식 0건.** [세부 사항](DREHMAL_STARTER_DISCOVERY_EXPERIMENT.md).

## Current build contract (supersedes historical implementation checkpoints below)
- Slug: `projects/turnbound/`; Mod ID / namespace: `turnbound`.
- Mod version: `0.1.0-alpha.17` (현재 마이그레이션 빌드의 식별자이며 플레이 가능 릴리스가 아님).
- Minecraft: `1.20.1`.
- Java: `17`.
- Loader: `Fabric` / `0.16.14`.
- Gradle: `8.12` (GitHub Actions CI).
- Build plugin: Fabric Loom `1.9.2`.
- Final JAR: `build/libs/turnbound-0.1.0-alpha.17.jar`.
- Existing-world compatibility: old TURNBOUND/NeoForge saves not migrated, direct world alteration prohibited. Only a **separate Drehmal world copy** may be marked for TURNBOUND after datapack analysis.
- Required dependencies: Fabric API `0.92.6+1.20.1`; loader and Minecraft above.
- Optional external mods: original Drehmal client graphics/visual dependencies may be required after screen review; GeckoLib/renderer not yet part of the active Fabric runtime.
- Forbidden bundled dependencies: third-party Drehmal map/resource pack bytes without distribution consent; historical NeoForge/old gameplay content.
- Datagen task: not configured; GameTest task: not configured.
- Server smoke-test task: not configured for current Fabric branch.
- Client smoke-test task: Fabric `turnbound-client-boot-smoke.yml` (boot window only).
- Latest residual original gameplay analysis (2026-11-09): [metadata-only callgraph #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591270837) **PASS**. Following verified offline patch targets, 17 direct function-calling blocks / 21 call references still connect to **225 possible static functions**; another 2,153 scanned blocks need direct command categorization. No original code adopted, no full separation. [Residual review](DREHMAL_REMAINING_ENTRYPOINTS.md).
- Newest Terminus copied-world checkpoint: [62 original tower commands isolated and exactly restored on official world copy](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42582286295), **PASS** (30 overworld region files). **NO in-world travel test**, 17 other command-trigger references retained, no full map conversion. [Technical guide](DREHMAL_DATA_PACK_SEPARATION.md).
- Latest Terminus dependency review: [graph and replacement boundary](TERMINUS_GRAPH_REVIEW.md), [verified GitHub run](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42573686150). 82 referenced functions reviewed; physical command-block positions and full map disconnection still NOT VERIFIED.
- Latest map compatibility checkpoint (2026-11-09): official Drehmal v2.2.2f read-only datapack/region audit **SUCCESS**, [reversible copied-world auto-root isolation #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42572004391) **SUCCESS** (3 automatic function entrypoints disabled and restored with original ZIP SHA match). This is **not** full story/command-block deactivation and **not** PLAYTESTED.
- Latest Fabric build: [TURNBOUND #1077](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42572004087) **SUCCESS**; the historical #1072 checkpoint below remains an earlier reference.
- Build verification: [TURNBOUND #1072](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568147041) **SUCCESS** at `ff035f9ae6282e359e530f3c28bbc37ba5df9c0a` (portable combat/session tests + Fabric Gradle/JUnit + JAR inspection).
- Gameplay PLAYTESTED: NO; MULTIPLAYER TESTED: NO; real Drehmal datapack separated: NO; game-ready party/UI/loot: NO.

---

## Historical notes (NeoForge 26.2 content-rebuild period; superseded where they conflict)


## Identity
- Project: `projects/turnbound/` (NOT `projects/turnbound-re/`).
- Mod ID / namespace: `turnbound`; Java package `io.github.q93503128.turnbound`.
- Minecraft Java 26.2; NeoForge 26.2.0.62; Java 25; Gradle 9.2.1.
- This is the *existing TURNBOUND technical project with a new player-facing game*, not a migration from TURNBOUND: RE.

## Current explicit design decisions (2026-11-05)
- **All previously authored player characters (P01–P08, F01–F04), character kits, quests, story, enemy/boss content, item lists, map/NPC placements and Drehmal-centered gameplay are retired.** No old content has approval as the new baseline.
- The new goal is a playable exploration-oriented turn-based RPG in Minecraft, integrating **exploration/story + tactical turn battles + collection/progression**. Neither narrative rush nor prolonged repetitive grinding should dominate.
- New characters, equipment, quests, regions, story and combat content are co-designed with the user. The assistant must not independently choose identities, abilities, rarity assignments or quests as settled canon.
- Codex is primarily assigned visual asset production, UI/UX implementation, animation, VFX, audio, world building and actual Minecraft screen verification. Gameplay logic is designed together and implemented/reviewed separately.
- Do not start creating story, roster, NPCs or equipment while the content-free technical foundation is not fully ready.

## Engineering foundation to preserve (subject to actual code/CI verification)
- Fixed-point 1000-Gauge turn scheduler and player-decision pause.
- Generic combat damage/healing/target/effect/status/cooldown mechanics; cleanly separate remaining hardcoded legacy character rules.
- Server-authoritative battle session ownership, network validation, shared-party/reconnect and idempotent reward settlement.
- Versioned campaign persistence and explicit legacy-save safety; old saves must never be silently rewritten into an unrelated new roster.
- Data-driven authoring, safe empty-content behavior and runtime/renderer checks before new content is approved.
- Existing gameplay details and numeric balances are **not** binding merely because corresponding Java or test fixtures exist.

## Frozen reference and workbranch
- Last verified legacy commit: `1877b9ef59c86d8e8271e43e0b1f6b8657dc174f` on `turnbound-alpha17-full-v04`. Treat this as the recoverable legacy implementation, not a live design source.
- Content-rebuild branch: `turnbound-content-rebuild`.
- Recovery reference includes legacy source, artwork, existing saves' schema and old design docs in Git history. Do not force-push or delete the reference branch.
- The working dependency/removal audit is `CONTENT_REBUILD_AUDIT_2026-11-05.md`.
- `MULTIPLAYER_DESIGN_v1.md` is historical technical guidance requiring revalidation. `EXTERNAL_ASSETS.md` and `AUDIO_ASSETS.md` remain as prior-asset provenance; they are not new design authority.

## Current implementation checkpoint
- Legacy world/quest/gameplay lifecycle, Drehmal auto-installer, old HUD/camera boot path and initial content activation have been detached from the active client/server entrypoints.
- All 27 old authored `data/turnbound` JSON files have been moved from `src/main/resources` to `src/test/resources`; historical tests can use them, **released JARs cannot**.
- The production catalog `data/turnbound/rebuild/foundation_v1.json` deliberately contains zero characters, enemies, quests, equipment, encounters and regions. Runtime initialization validates this.
- All 223 former `assets/turnbound` files (models, textures, animations, audio and legacy UI) have moved to test resources. The rebuild JAR excludes them. Registrations for retired actor entities, mounts, held items and old sounds were removed from the active mod entrypoint; associated legacy renderer subscribers and presentation isolation subscriber have been detached. The legacy JourneyMap marker plugin and old automatic audio playback tick were also disconnected.
- Legacy Java classes **still compile into the JAR**. Their dependency-safe physical removal remains unfinished; do not claim complete source cleanup.
- Current blank foundation is not a playable RPG. Minecraft client gameplay and multiplayer were not verified for this rebuild.

## Validation
- Build TURNBOUND #1049 (rollback verified): Gradle build/tests PASS; dedicated-server smoke PASS; JAR inspection PASS; Modrinth pack validation PASS. This validates the restored runtime code plus production-data/asset removal.
- CI run: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41980378906
- JAR from this run is a foundation artifact, **not a released playable game**. Never claim it was client playtested.
- The one-click rebuild pack no longer downloads or repairs Drehmal, and should be installed separately from the old production instance.

## Next executable cleanup gates
1. Remove remaining authored-content Java classes and stale dependency chains, preserving the generic battle, network and persistence engines.
2. Verify no unwanted static/event registry callbacks reactivate old content; retain required third-party asset license records.
3. Confirm new-game empty state and block accidental use of old player save state; prove no content/world mutation at client and server bootstrap.
4. Validate meaningful code units with relevant tests, one integrated build per checkpoint, dedicated-server smoke, and eventual client/multiplayer playtest.
5. After foundation verification, jointly agree on new game systems, region flow, characters, equipment and quests.

## Quality
- Gameplay completeness over feature count. Systems must tie into a meaningful loop.
- Commercial references for important UI and animations, with licensing documented.
- No no-op compatibility hacks, deletion of still-relevant tests just to pass CI, fake APIs, or hidden save loss.
- Distinguish CODE REVIEWED, TESTED, BUILD VERIFIED, PLAYTESTED, MULTIPLAYER TESTED honestly.

## Additional cleanup checkpoint (2026-11-05)
- Detached the last identified legacy world-level auto-subscriber `FieldEncounterPresentationBridge` to prevent obsolete encounter-marker polling and actor replacement.
- Removed old summon source portraits and presentation images (18 PNGs, approx. 37.4 MB) and their unused generation script from the active workbranch. Original bytes remain recoverable from Git history; asset provenance was moved to `legacy/ASSET_PROVENANCE_summon.txt`.
- Historical NeoForge/Minecraft Java classes are still retained in the main compile source set until their shared runtime dependencies can be properly extracted. Do NOT move these classes into the ordinary JUnit `test` source set: Build #1048 failed because that source set lacked the transformed game API. The attempted move was completely rolled back in #1049.

## Empty rebuild campaign boundary — 2026-11-05
- A fresh empty rebuild runtime is now explicitly opt-in through `CampaignProgressStore.startEmptyRebuildGame`; it never replaces an already loaded player runtime. The old `ensureNewGame` and legacy v1–v6 reader remain historical paths for existing tests and recovery, not rebuild game entrypoints.
- The new save codec uses schema **7** with `saveKind=turnbound-empty-foundation`, rejects authored content and reads no legacy save as a rebuild. Storage is reserved under the distinct NeoForge `rebuild_campaign_profile` attachment; the legacy `campaign_profile` remains untouched.
- Until server login/logout and migration policy are separately implemented and playtested, the new rebuild attachment has **no automatic read/write lifecycle**. No current work claims end-to-end persistence in a launched client.
- New rebuild profiles contain no characters, party, rewards or currencies. Legacy gacha calls are rejected for those runtime instances; legacy engine/test fixtures remain available for historical regression tests.

## Legacy encounter boundary — 2026-11-05
- Explicit empty rebuild runtime refuses retired solo/training/encounter battle creation before UI/world transitions. Shared battle creation refuses if any participant has an empty rebuild profile. The historical combat engine and its existing tests remain untouched for future generic extraction.
- The current battle engine still has dozens of hardcoded old ability/character/boss rules. This guard is safety isolation, **not** proof that the generic engine has been extracted or that a playable new battle exists.

## Protected server session and arithmetic extraction — 2026-11-05
- The only active server gameplay hooks are now the rebuild profile login, 100-tick dirty autosave, logout, and server-stop lifecycle; no world builder or old quest/encounter entrypoint is re-enabled.
- The login admission gate refuses any existing `campaign_profile` attachment or legacy JSON/.bak file. A conflicting legacy + rebuild save also fails closed; no automatic old save migration, quarantine, reset or overwrite is attempted. New profiles are allocated only with no old data and persist to `rebuild_campaign_profile` (NeoForge attachment `copyOnDeath`).
- Schema-7 malformed rebuild saves cause disconnect rather than destructive reset. Client restart/respawn, actual multiplayer, and old-world visual safety remain NOT PLAYTESTED. Legacy worlds must not be used for rebuild playtests.
- Damage arithmetic is factored into content-independent `CombatMath` and the old `BattleEngine.calculateDamage` delegates to it for compatibility. The remaining legacy character/boss rules in BattleEngine are still present and must be addressed as an entire combat-behavior extraction, not by deleting tests.
- Old `CampaignPersistence` is preserved for archival tests but not registered for this rebuild. A future authored-content schema needs a separate reviewed storage migration from the strict empty schema 7.

## Verified implementation checkpoint — Build TURNBOUND #1059 (2026-11-05)
- Source HEAD `f19710f2469519c6c53a01b36c31aedd365e3cf6` passed Gradle test/build, dedicated NeoForge server smoke, JAR contents and Modrinth one-click pack verification: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41987155431
- The previously disconnected legacy world/bootstrap, authored catalogs and art remain inactive. The rebuild now automatically manages its **own separate empty-profile attachment** on server login, dirty autosave, logout and server stop, while refusing legacy/conflicted data; NeoForge `copyOnDeath` and identity-bound owner transfer cover the intended respawn path.
- The original battle resolver now delegates generic target selection (`BattleTargetResolver`) and damage arithmetic (`CombatMath`); legacy boss/character-specific logic remains in historical classes and is not approved new-game design.
- CODE REVIEWED: yes. TESTED (JUnit): yes. BUILD VERIFIED: yes. JAR PRODUCED: yes. DEDICATED SERVER SMOKE: yes. CLIENT PLAYTESTED: **no**. MULTIPLAYER TESTED: **no**.
- **Next code-removal unit:** analyze and break source/test references to old world builders, quest/NPC classes and character-specific battle handlers before physical Java removal. Do not move Minecraft classes into the ordinary JUnit test source set (Build #1048 failure). Preserve old-save snapshots, historical tests, and generic networking/settlement semantics. Client login/restart/death and multi-owner server sessions require real playtests in a separate NEW world before release.
- Do not co-design new roster/map/story or commission Codex assets until this boundary is confirmed through client verification and the remaining legacy code separation has been deliberately reviewed.

## Blank-production gate — 2026-11-05 / Build TURNBOUND #1062
- Verification source commit: `1a901f9237a84828f113b3ba0a89d7bdec42b0b0` with [Build TURNBOUND #1062](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42014459244) PASS (Gradle tests/build, NeoForge dedicated-server smoke, JAR inspection, MRPACK inspection).
- `tools/verify_rebuild_boundary.py` now checks the empty authored-content production manifest and detects any auto-registered legacy Java subscribers. It also verifies the runtime JAR has no old authored asset/data files or retired Drehmal installer/world-repair class bytecode.
- The retired world downloader, world-loading overlay and resource/compatibility repair implementation are excluded from **the shipped JAR** through Gradle packaging. These historical Java source files remain compiled for existing tests; do not mistake packaging exclusion for complete physical code removal.
- Content-first milestone: **BLANK BUILD/SERVER BASELINE VERIFIED**. This is suitable for agreeing on the new player-facing game's direction, but NOT certified as PLAYTESTED, client-load-safe or multiplayer-safe.
- Remaining technical cleanup: break historical compile/test references to old world, NPC/quest UI and character-specific battle Java before physically retiring source files. Do not move NeoForge Java classes into the standard JUnit test sourceSet; the failed #1048 already demonstrated an invalid classpath.
- Next real-world acceptance: install the one-click artifact in a completely separate Minecraft 26.2 test instance, create a fresh world, observe no legacy onboarding/download or terrain writes, log in/reconnect/respawn and confirm the empty rebuild attachment safely persists. Test multiple connected players separately. NEVER use the old world/save as the first playtest.

## Java source cleanup checkpoint — 2026-11-05
- removed 49 disconnected, untested-and-unreferenced retired Java classes, including the dormant Drehmal auto-installer and obsolete user-facing field/facility/summon UI, without removing any historical JUnit test source. Source recovery is guaranteed by the frozen legacy branch.
- Exact retired source names are tracked under `tools/retired_legacy_java_classes.txt`, with both Java-source and packaged-bytecode CI regression checks.
- Remaining old Java modules have compile-time dependencies on preserved combat/session/world protocols or historical tests, and must be isolated deliberately before deletion.
- CI success on this checkpoint verifies a blank-data/dedicated-server foundation, NOT Minecraft client launch, player-login persistence, or multiplayer gameplay. The production-ready-for-co-design decision must distinguish this from client-playtest readiness.

- Build #1064 revealed a compile-time reference from retained unused `BattlePlayerRenderPolicy` to retired `SummonCameraController`. Both were disconnected from the live and test roots. Removed `BattlePlayerRenderPolicy` too rather than retaining a broken reference or replacing behavior with a stub. Total isolated Java files removed: **49**.

## Blank-world manual acceptance gate — 2026-11-05
- Build TURNBOUND #1065 PASS after physical removal of 49 isolated old Java classes, retaining all existing tests and generic core systems. Source SHA: `3fc091ca58b2f6d8e2bc0c50333379e69e2ae20e`.
- Fresh Minecraft client acceptance procedure and evidence checklist: `REBUILD_FRESH_WORLD_ACCEPTANCE.md`. Actual fresh-world login, restart, death and multiplayer remain **NOT TESTED**, regardless of any headless client-boot result.

## Verified client window and remaining gameplay gates — 2026-11-05
- [Client Boot Smoke #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42018332635) PASS: headless Linux client created a Minecraft window after the TURNBOUND mod registered. Client-only JourneyMap from the modpack was staged for the dev-run environment. This is CLIENT BOOT SMOKE VERIFIED, not PLAYTESTED.
- **Blank technical base now verified through:** Build TURNBOUND #1065 (JUnit/build/dedicated server/JAR/mrpack), source and runtime legacy-data guard, physically retired 49 unreferenced legacy Java classes, and headless client window creation.
- **Still gated before full ready-for-play claim:** real client fresh-world entry, logout/rejoin, restart, /kill & respawn and actual two-person multiplayer; source-reference cleanup for the remaining old world and character-specific BattleEngine branches. Do not claim legacy code fully removed.

## Approved party size and gacha direction — 2026-11-06
- **Confirmed:** Player-avatar-centric hybrid survival/turn combat. Starting play is player-driven, with companion acquisition via **gacha**, not mandatory difficult exploration or recruitment quests.
- **Solo active-party capacity: 1 player + up to 4 companions = 5 combat participants.** The four slots EXCLUDE the player.
- The prior 'very difficult/rare companion recruitment' proposal is superseded.
- Gacha draw acquisition sources, currencies, odds, rarities, duplicate handling, guarantees, character roles and co-op limits are undecided. No paid draws were requested.
- This is design documentation only, **not an implemented five-participant party or working gacha**. Existing historical battle capacity and the empty save schema require later implementation and testing.
- The details and open choices are in NEW_GAME_CONTENT_DIRECTION_DRAFT.md.

## Updated confirmed solo party and starter choice — 2026-11-06
**OVERRIDES the earlier 2026-11-06 player-plus-four-companion entry above.**
- User shifted combat back to the original party-only format: **up to 4 gacha character actors**, **player avatar excluded from turn combat**. Player remains responsible for vanilla-world exploration, mining, building, crafting and survival.
- **First-start choice: exactly one of three normal native ★3 characters.** They are normal characters, NOT exclusive starter classes, not stat-boosted, and not permanently inaccessible to gacha. The two unselected choices remain available from regular character acquisition.
- Characters' names, role kits, new star rarity ceiling, gacha probabilities, starter UI and saved first-choice/one-time claim mechanics remain unimplemented and require design/verification. No old retired characters automatically reused.
- HISTORICAL evidence (legacy `turnbound-alpha17-full-v04`): `PROJECT.md`, `02_BALANCE_RULES_v1.md`, `03_CHARACTER_DESIGN_v1.md` defined **native ★1–★5**. The proposed ★6 promotion/Awakening relationship remained unresolved, and old `CanonicalData.levelCap` supporting an integer 6 is **not proof of approved ★6 native rarity**.
- This is only a planning/documentation checkpoint; do not claim any starter selection or populated four-character roster currently functions in the released empty foundation.
- Refer to `NEW_GAME_CONTENT_DIRECTION_DRAFT.md` latest selected combat direction section for the current design; superseded historical sections remain for traceability, not authority.

## Approved native rarity range — 2026-11-06
- **Native / born rarity is ★1, ★2, ★3, ★4 or ★5 only.** There is no native ★6 gacha rank.
- Native rarity and any future upgrade/promotion/awakening tiers are distinct concepts. A ★6 promotion, sixth display star or Awakening path is **not approved** by today's native rarity decision; decide separately if ever needed.
- The player avatar is outside turn-based combat. The solo combat party holds up to **four playable characters**, obtained by gacha except for the first choice.
- On first start, choose **one of three ordinary ★3 characters**. No special starter-only power, extra rarity or exclusivity; all three are ordinary characters available through the normal pool.
- No rarity probabilities, duplicate conversion, star growth curves, new character identities or starter-specific role kits have been approved.
- These decisions are currently **DESIGN ONLY**. The blank production catalog remains empty and the game does not yet include the new roster or selection UI.
- **Next recommended design unit:** field-to-turn encounter entry/exit, avatar vulnerability/protection while directing the character party, party defeat and survival consequences, including multiplayer/exploit boundaries. Then first starter trio roles and the gacha economy. No code change required for native rarity documentation.

## Confirmed encounter presentation — overworld field battle (2026-11-06)
- **CONFIRMED: Option A**. Encounters transition into turn-based battles **in the same actual vanilla-generated overworld terrain where the enemy was met**, with authored TURNBOUND character/enemy models and a dedicated battle camera/UI. This is NOT a separate instanced arena or teleport to a dedicated battle dimension.
- Up to **four playable gacha/starter characters** fight; the player's Minecraft avatar is an overworld explorer/director, **not a turn-based combatant**.
- Preserve the surrounding survival world and other players' simulation. A battle session must not pause the entire world. Camera placement, slopes/cliffs, obstacles, entity positioning and post-battle state restoration require testing on actual terrain.
- The exact **encounter trigger** (attack, enemy aggression, interaction or proximity), participant/world-external damage handling, avatar vulnerability/immobility, retreat, enemy interference and multiplayer participation rules are **not yet approved**.
- This is a design choice only. The current content-empty build has **not implemented or playtested** this new field encounter flow. Legacy world-combat source must not be treated as approved behavior without review.

- Next co-design decision: specify the **encounter trigger** without making ordinary Minecraft movement or resource gathering produce unavoidable repetitive battles. Then specify avatar protection/retreat and multiplayer authority before implementing a complete field battle.

## Confirmed R_PG-style visual contact and low-friction traversal — 2026-11-06

### User-confirmed encounter trigger (CANON DIRECTION)
- Reuse the older TURNBOUND **R_PG-style overworld enemy reaction**, not the recently offered attack/interact/proximity-selection alternatives.
- A visible hostile field monster **notices the player**, displays a readable **`!` alert**, briefly reacts, and **chases**. **Physical contact triggers a 4-character-party turn-based battle on the current terrain**. No separate preparation menu and no special battle-interact key.
- Escape should remain meaningful: a chasing monster disengages if the player gets far enough / breaks pursuit conditions and returns to its patrol or territory.
- **Critical user concern:** if the overworld becomes thick with mobs, ordinary travel must not force constant, frustrating turn battles. Traversal comfort is a design acceptance requirement, not an optional polish item.

### Anti-frustration measures — recommended for review, NOT numeric canon
1. **Spawn policy by area:** Roads, starting points, settlements, bases and ordinary routes should have safe travel/breathing stretches; dangerous ruins, dungeons and optional hostile territories can have more pressure. Do not populate every section of every road with a forced battle.
2. **Fewer representative field groups:** One visible field creature/group can represent a multi-enemy turn encounter, so battle complexity does not require equivalent counts of chasing field entities. Avoid overlapping aggro groups that chain player into several battles.
3. **Avoidance readability:** Enemies should be visible before contact when plausible. Alert is a brief warning, not instant teleport into combat; line-of-sight and bounded chasing make circumventing monsters legitimate.
4. **Retreat and re-aggro:** Chases end when leaving territory or losing pursuit. Returning creatures have a brief re-alert grace; avoid permanent endless chase, including across base/settlement boundaries.
5. **After-battle breathing room:** A defeated field group must not instantly respawn/retrigger on the same spot. Other nearby enemies should not ambush a player in the immediate transition back from the battle UI. Exact respawn/cooldown behavior must be validated for fairness and farming balance.
6. **Ambient versus hostile wildlife:** Although user wants all vanilla living mobs replaced, **not every authored animal should auto-aggro**. Distinguish peaceful/neutral wildlife from aggressive opponents so gathering and traveling remain possible.
7. **Multiplayer and authority:** One player/group cannot claim multiple overlapping battle sessions; server adjudicates contacts, parties, return-to-field positions and collision with spectators. The rest of the server world must not pause.

### Historical implementation reference (NOT approved new numeric balance)
- Legacy branch `turnbound-alpha17-full-v04`: `world/DrehmalFieldEncounterPolicy.java` records the older R_PG reference sequence: see in field → alert beat → chase/contact → battle, with no pre-battle menu.
- `world/FieldEncounterRules.java` used PATROL/ALERT/RETURN and, historically, 10-block alert, 15-block disengage, 18-block home leash, 2.6-block contact and 60-tick return grace. Those are historical code constants **not automatically the new game settings**.
- The old `ENCOUNTER_ENEMY_PLACEMENT_v1.md` explicitly forbade monster corridors, repeated roadblocks, invisible battle triggers and rapid nearby respawns, and permitted field representatives to stand for a multi-opponent battle. **Do not transfer old Drehmal-specific map, mobs, routes or coordinates.**

### Still open before coding
- Where/how new hostile ecology spawns in the vanilla-generated overworld, including light levels and player-built safe areas; tuning maximum simultaneous nearby hostile groups and aggro frequency.
- What counts as escape when climbing, swimming, flying, mining or crossing a region boundary.
- Field battle protection, third-party interference, post-combat positioning and how a player avatar (not a combat actor) is represented during battle.
- Whether several visible monsters join a single battle when clustered and what happens when multiple players contact a group.
- Exact detection, chase, respawn and grace numbers are **UNDECIDED**. Validate against real survival traversal rather than copying legacy 20–35-second common respawns.
- This is a **documented design decision only**; no hostile monster spawning or field contact battle has been re-enabled in the blank-build runtime.

- Gate for the first playable slice: a player must be able to travel normal routes without being repeatedly forced into consecutive turn battles; playtest for both avoiding enemies and intentionally engaging them. This is an experience requirement, not a promise that any actual density tuning has been tested.

## Monster ecology/spawning planning checkpoint — 2026-11-06
- User agreed to the next design step: preserve ordinary survival traversal with mostly peaceful/neutral custom wildlife, place sparse avoidable hostile field groups, and increase intentional combat pressure in caves/ruins/dungeons. All vanilla living creatures are intended to be replaced eventually.
- Design source: `OVERWORLD_ECOLOGY_SPAWNING_DRAFT.md`. Contains proposed ecology-zone roles, explicit anti-spam combat acceptance, vanilla-material replacement matrix, NeoForge spawn-gate approach and small vertical-slice milestones.
- Spawn density numbers/weights, actual new entity kinds, loot tables and dimensions remain **NOT APPROVED/NOT IMPLEMENTED**. No Java source/resources were altered, and no build/CI was necessary for this docs-only checkpoint.

## Confirmed peaceful-wildlife response and dungeon direction — 2026-11-06
- **Peaceful authored wildlife becomes a turn-based opponent if the player deliberately attacks it**, while normal unprovoked survival travel leaves it peaceful. Routine nonhostile breeding/feeding/resource interactions must not accidentally force combat.
- **Dungeons and ruins are planned as discoverable authored structures in ordinary vanilla-generated survival terrain**. They should be varied, contain meaningful encounters/rewards, preserve user builds and support same-world turn battle cameras and multiplayer authority.
- R_PG visual ! reaction, pursuit/contact remains the normal *naturally hostile* enemy trigger; deliberate hits on passive fauna cause the field battle without requiring a nonsensical chase before contact.
- `OVERWORLD_ECOLOGY_SPAWNING_DRAFT.md` records ecosystem, dungeon generation options, compatibility considerations and remaining design questions. This is a documentation decision, **not a new entity/dungeon implementation or playtest**.

## Confirmed preservation of TURNBOUND turn-combat system — 2026-11-06
**User decision: preserve the established TURNBOUND battle system; do not redesign the entire combat engine for the new survival-world game.**

Retain the proven generic combat contract from legacy TURNBOUND as a system, not old character content:
- **Four playable character actors maximum**, player avatar not a combat actor (latest party choice).
- **Deterministic fixed-point 1000 Turn Gauge scheduler.** When Gauge reaches 1000, action is available; each normal action subtracts 1000; overflow survives. SPEED/SPD influences future Gauge fill and thus action frequency, not merely initial turn order. Preserve natural successive turns when legitimately earned.
- **Battle logic pauses during manual player action/target selection**. Runtime actors, turn-order preview, and AUTO must use the same authoritative scheduler, not separate approximate systems.
- Keep **Basic (CD 0), active skill cooldowns, passive/reaction and other general effect mechanisms**, target validation, buffs/debuffs, healing, status/Gauge manipulation, and server-authoritative damage/settlement, as long as the generic implementation is still appropriate. Keep player decisions meaningful and avoid reaction recursion.
- Preserve the player-facing convenience design of **AUTO and 1x/2x presentation speed** without altering the actual underlying action economics. Actual initial AUTO policies and exact controls may need UI reimplementation/revalidation after the blank-content reset.
- Retain old generic battle tests and network/save safety boundaries; do not restore old cast-specific skill definitions, boss hardcoding, retired story/quests, old landscapes, retired 3D assets, or previous numerical balance by default.
- The user requested preservation of the **battle system**, not automatic reapproval of prior native character kits, duplicate progression bonuses, equipment stats, gacha probabilities, enemy lineup, playtime targets or world geometry.
- Implementation boundary: the current shipped rebuild is still an **empty playable-content foundation**. Most generic combat types survive but the live authoring/battle entrypoint is intentionally disabled and historical character-specific branches are present in source. Preserving a design is **NOT** proof of a working new-game field battle; an appropriate integration, test pass and actual Minecraft playtest remain required.
- Historical supporting reference: `turnbound-alpha17-full-v04:projects/turnbound/02_BALANCE_RULES_v1.md` sections 1–2, 21. Old 1000-Gauge and fixed-point `TurnScheduler`, plus action cooldowns and AUTO/1x/2x semantics, are reference behavior; old numerical tuning not new canon.
- Follow-up co-design: clarify how combat rewards/party progression integrate with vanilla survival and how a player avatar's world-state is protected during field battle. Do not move straight to building a detailed first region or entire roster before core integration decisions.


## Growth pillars retained for the new survival game — 2026-11-06
**User decision:** Keep **all four existing TURNBOUND growth pillars** in the redesigned Minecraft survival + party-turn-RPG: (1) character XP/levels, (2) gear/equipment, (3) awakening, (4) duplicate character rewards. Do NOT discard one for convenience or blindly reinvent the system.

### Preserved system shape (legacy baseline to carry forward)
- **Character levels:** retain battle/contract XP progression. Historical baseline: base Lv1–60, independent duplicate bonus +0–+10, effective maximum Lv70. The precise numeric curve and pacing are balance review items for the new survival loop.
- **Equipment:** retain the three ordinary character equipment slots: weapon, armor and accessory. Retain the concept of character-specific signature gear as a distinct optional slot/mechanic; it is not permission to automatically restore old signature items or character kits. Historical enhancement target: +0–+10 with no random failures or destruction. Specific stat values, Gold costs and recipes require adaptation to survival.
- **Awakening:** retain the once-per-character mechanical evolution concept; make it a meaningful kit option rather than only a flat stat multiplier. The old activation path depending on mandatory main-story or character-story quests is INCOMPATIBLE with the now-confirmed job-board/nonlinear game direction and must be redesigned (e.g. gameplay accomplishments/optional challenge), not silently copied.
- **Duplicate gacha characters:** preserve meaningful choice between **one additional bonus level** (up to the cap) **OR native-rarity Star Essence** per duplicate; not both. At the +level cap, Essence-only is the prior baseline. Preserve unclaimed selection state safely across save/reconnect. Whether old Essence conversion shop/costs are retained is a separate economy review; no mandatory duplicate copies to unlock a unit's core skills.
- **Native rarity remains ★1–★5 only.** A possible ★6 promotion/awakening relationship is separate and still undecided.

### Survival integration requirements (proposals to design, not approved numbers)
- Mine/forage/craft resources should help produce, enhance or trade useful **party** equipment. The Minecraft player avatar remains an explorer/crafter, not a combat actor, so do not accidentally transfer vanilla weapon damage into the turn battle as avatar damage.
- Turn battles and optional contracts award character XP and useful equipment resources; ruins/dungeons/bosses can provide signature/awakening-related objectives and rarer upgrade materials.
- Vanilla character experience, raw ores, survival resources and RPG progression must not spawn several duplicated, unnecessary currencies or repetitive processing steps.
- Existing fixed-point Gauge / party-only four-character combat is retained. A complete new-game save system for characters, equipment, duplicate pending choices and awakening will need an explicit migration from the current intentionally empty rebuild profile.

### Boundaries / verification
- The user confirmed retention of the **systems**. Old character names, kits, specific gear items, story unlocks, rarities per character, numerical XP/stat balance, drop rates, previous map and quests remain retired unless explicitly newly approved.
- Source references: frozen TURNBOUND `turnbound-alpha17-full-v04`, `02_BALANCE_RULES_v1.md` sections 9–11 and 16–17 plus prior `PROJECT.md` duplicate-choice policy. These are historical detailed baselines, not active new content.
- This commit updates **planning documents only**; there is no newly implemented party progression, awakening UI, equipment integration, or duplicate selection, and no build or playtest performed for this doc-only change.

### Next design dependency
- Before first-region production: decide the **resource/reward bridge** from ordinary survival mining, crafting, hunting, ruins, dungeons and NPC contracts into the four retained growth systems, ensuring neither survival nor tactical combat is made optional/pointless.

## Updated star-based level caps and forging direction — 2026-11-06

### User decisions: DO NOT silently revert
- **Current star rank sets the normal character level cap:** ★1 Lv10, ★2 Lv20, ★3 Lv30, ★4 Lv40, ★5 Lv50, ★6 Lv60. Formula: `baseLevelCap(currentStars) = 10 * clamp(currentStars, 1, 6)`.
- **Native rarity is ★1–★5 only** (gacha identity); the sixth star is an achievable *growth/promotion* rank, NOT a native ★6 gacha rarity. This resolves the earlier uncertainty about whether a ★6 growth rank is in scope, but does **not** determine promotion costs, eligibility, number of ascensions, star-stat modifiers or resource recipes.
- Prior legacy v1 active growth applied a flat base Lv60 and effective Lv70 when duplicate +level was counted; `CanonicalData.levelCap(stars)` still contained 10/20/30/40/50/60 as a largely unused legacy helper. **The user's latest decision supersedes that flat-cap interpretation** for the new game. Do not treat the presence of the old helper as proof that the new rank-based cap is already in live runtime.
- Keep previously approved duplicate acquisition bonus level OR Star Essence option as a growth pillar; **the stacking and UI semantics of duplicate bonus +0–+10 versus the new star-specific base-level limits remain to be reconciled before code and balancing**. Do not silently force a level-70 base cap, auto-award +levels or let duplicate bonuses bypass a promotion gate without an explicit rule.

### User-requested equipment direction
- **Change the primary normal gear acquisition loop from direct finished-equipment drops to material-based crafting at a forge/blacksmith.**
- Players obtain ore/metal and other useful materials through vanilla survival mining and exploration; authored TURNBOUND creatures drop distinctive components, while dungeon/elite/boss rewards offer rare special materials or recipe discoveries.
- **Better ingredients yield better or meaningfully more specialized equipment**, but the effect should be inspectable (visible recipe/preview), balanced and relevant to actual four-character turn combat rather than a random hidden power roll.
- Preserve existing character equipment identity (weapon/armor/accessory ordinary slots, optionally character-specific signature gear, and the already selected enhancement system). Do not automatically copy retired equipment identities, recipes, numerical stats or old direct-drop economy.
- Exact forge station type, whether it is NPC-operated/buildable, crafting formulas, ore-compatibility, item tiers, traits, required materials and upgrade costs are **proposed, not yet approved**.
- Full equipment design proposal and integration constraints: `SURVIVAL_FORGE_AND_STAR_GROWTH_DRAFT.md`.
- **This update is documentation only:** NO equipment recipes, forge UI, new materials, star-cap runtime, promotion or test/JAR is implemented or verified.

### Next design dependency
Decide forge workflow and how the basic survival materials / monster trophies produce weapon, armor and accessory options without excessive crafting steps; then reconcile level caps with duplicate bonuses and star promotion. Avoid switching to first-region production before these relationships are coherent.


## User-confirmed forge separation from vanilla Smithing Table — 2026-11-06
- **TURNBOUND gear forging is an independently registered custom forge/blacksmith workstation and recipe interface, separate from Minecraft's vanilla Smithing Table.** No repurposing or replacement of vanilla Smithing Table recipes, templates, armor-trim/smithing functions or player-survival inventory item behavior.
- The TURNBOUND forge creates/enhances **party character equipment** from survival ores and custom monster materials. Vanilla crafting/smithing remains useful for the Minecraft explorer's ordinary survival tools and armor.
- Allowing an NPC blacksmith in towns to use the same shared forge recipes is an architectural **proposal**; the exact station location, GUI, extra modules and whether the player can craft/place a forge remain unfinalized in detail. Keep the player's ability to access normal Minecraft crafting independent of TURNBOUND's equipment loops.
- Existing ★1–★6 rank level caps and duplicate +level cap-interaction are separate decisions. This separation does not resolve those mechanics.
- Documentation checkpoint only: there is no actual custom forge block, recipe serializer, Forge GUI or registered structure yet.


## Confirmed duplicate-level option A — 2026-11-06
- Latest user choice: a character's current star rank limits only base XP level: star 1=Lv10, 2=Lv20, 3=Lv30, 4=Lv40, 5=Lv50, 6=Lv60. Native rarity remains 1–5 stars; rank 6 is progression-only.
- Existing duplicate reward: choose either +1 bonus level or native-rarity Star Essence per copy. Bonus levels cap at +10 and accumulate separately from base XP.
- Effective combat level = base XP level + duplicate bonus level, even if that exceeds the current-star base XP cap. Example: star 1, base Lv10 + bonus 3 gives effective Lv13; star 6, base Lv60 + bonus 10 gives effective Lv70.
- Bonus levels do not independently promote star rank, increase the XP cap or substitute for star-promotion requirements. Promotion prerequisites, costs, exact stat curve and final UI display remain undecided.
- This supersedes earlier statements marking bonus-level/cap interaction unresolved, but does not approve any new runtime feature or assert testing. Documentation only.

## User-approved sequential star promotion — 2026-11-06
- **Promotion is available when a character reaches the CURRENT STAR RANK'S BASE XP LEVEL CAP** and the player spends the required **promotion materials obtained through monster encounters and dungeons**. This is an AND condition, not either/or.
- Rank grows **one star at a time** (★1→★2→★3→★4→★5→★6). All native ★1–★5 characters are eligible to grow eventually to ★6. No native ★6 draws.
- Each current star rank has an independent normal base XP ceiling: ★1 Lv10, ★2 Lv20, ★3 Lv30, ★4 Lv40, ★5 Lv50, ★6 Lv60. Example: ★3 Lv30 (+5 duplicate levels) may ascend to ★4 after paying the appropriate materials; its normal base XP ceiling then becomes Lv40.
- **Duplicate bonus levels do NOT satisfy the base XP cap requirement:** ★3 Lv25(+5) has effective combat level 30 but still cannot promote until its actual base XP level reaches 30.
- Promotion raises `currentStars` and the **base XP cap** but must not change immutable `nativeStars`, remove already-earned +bonus levels, or automatically restore old character assets/old balance.
- **No specific costs are approved yet**: promotion-material names, quantities, the split between ordinary monsters, elites and dungeons, any optional Gold component, future star stat changes, level/XP carry behavior and relation to once-only Awakening need separate design.
- Promotion should create meaningful mining/monster/dungeon gameplay without overwhelming inventory with one new currency per star or requiring repetitive mandatory dungeon grinding. Proposed UX/recipe implementation is not an approved final design.
- **DESIGN ONLY:** no actual rank-promotion screen, save schema or verified playable progression exists in the current empty-content build; no code/build/CI work was requested here.

## Confirmed summon guarantee and reward sources — 2026-11-06
- **Every 10-pull batch guarantees at least one native ★4-or-★5 character.** The batch may contain more high-rarity results naturally. A ★5 counts toward this guaranteed ★4+ slot; do not silently promise an additional ★4 on top of a ★5.
- **Every 50th individual draw is guaranteed native ★5**: 50, 100, 150, 200, ...; this is a **fixed milestone** guarantee, not a conventional pity counter that resets after obtaining ★5.
- **An early ★5 never resets or advances the fixed 50-draw schedule**. Example: ★5 on draw 27 still guarantees draw 50; ★5 on draw 49 still guarantees draw 50. Within a 10-pull, a scheduled guaranteed ★5 also satisfies the ★4+ batch guarantee.
- Both single and batch pulls contribute their actual individual draws to the same cumulative milestone count. The batch-specific ★4+ guarantee is explicitly associated with the **10-pull action**; no separate ten-consecutive-single-pulls ★4+ promise has been authorized.
- Gacha's possible native ranks remain **★1–★5**; rank ★6 exists only through character promotion. Initial starter choice is one of three ordinary native ★3 characters in the normal obtainable roster.
- **Summon currency comes principally from NPC/job-board contracts and dungeons.** Elite/boss first-clears and substantial discoveries are viable proposed secondary sources; exact payout amounts and whether repeat-clears award summon currency remain undecided.
- Prior frozen legacy 65-soft/80-hard pity and any ★5-on-early-draw reset logic **DO NOT apply** to this new approved scheme. Do not copy old numerical summon odds or 300-crystal costs without a separate explicit balance decision.
- Retain earlier duplicate character handling: reward choice of a +1 duplicate bonus level (up to +10) OR native-star essence, unless later explicitly changed.
- Still unapproved: native-rarity probability weights, exact summons cost, currency/item names, how/where the UI opens, cross-pool/banner account rules if multiple pools ever exist, any additional ★4 guarantee on singles, and precise repeatable reward anti-grind policy.
- Save the per-player cumulative draw count and enforce reward settlement server-side when implementing. Never award guaranteed draws twice on interrupted transactions. These are design/implementation criteria, **not evidence that summons are live**.
- This is **documentation only**, not new summon code, resource drops, tests, server runtime, JAR or playtest. Full draft: `SUMMON_GACHA_AND_REWARD_ECONOMY_DRAFT.md`.


## Confirmed native draw probabilities and flexible initial roster size — 2026-11-06
**Latest user decision, overriding earlier unresolved odds and provisional market comparisons:**

| Native rarity | Base chance per non-guaranteed draw |
| --- | ---: |
| ★1 | 15% |
| ★2 | 25% |
| ★3 | 40% |
| ★4 | 15% |
| ★5 | 5% |
| **Total** | **100%** |

- These are the **base-rarity odds before applying guarantees**. The 10-pull guarantees at least one ★4+ in that batch; cumulative individual draws #50, #100, #150, ... guarantee a ★5 regardless of earlier lucky ★5 outcomes. The exact server draw/guarantee algorithm must preserve the promised minimum and not downgrade a naturally or milestone rolled ★5.
- The initial **overall character roster size and the number per rarity are intentionally NOT fixed**. Character identities, designs, role balance, assets, skills, and available pool counts should evolve with content production and playtest. Do not manufacture filler units merely to fill an artificial quota.
- The first-start selection remains exactly one from **three ordinary ★3 candidates**, all available from the normal summon pool. This fixed starter-choice count does NOT set the total number of ★3 characters.
- Low rarity must have purposeful roles and an attainable promotion path to growth rank ★6; native ★5 should offer appealing identities without making low-rarity strategic choices irrelevant. Exact individual kits and raw stat/power budgets remain to be designed.
- Main summon resources principally come from optional contracts and dungeons. **Exact summon cost and currency pacing are STILL undecided.**
- This is a **planning-only decision**. GachaService is not connected to the rebuilt empty-runtime playable content; no code/build or gameplay tests were run.


## Field battle failure, possible flee, contracts and dungeons — 2026-11-06
### User-confirmed gameplay outcomes
- **Party defeat:** return the Minecraft explorer avatar to the most recent applicable checkpoint. There is **no defeat penalty**: no inventory loss, dropped tools, Gold/gacha-gem deduction, XP/character level loss or equipment destruction. Defeat is a battle outcome, **not a vanilla player-death/drop event**.
- A failed battle grants no victory loot, contract kill credit, dungeon-clear status or one-time clear bonus. This distinction is not a punishment or preexisting resource deduction; it simply does not award a victory.
- Checkpoint activation, initial fallback, boss-specific checkpoint placement, recovery of the defeated party's HP and the non-teleporting behavior in multiplayer require design before implementation. No loss of progress or overworld building is intended.
- **Contracts:** the player wants contract difficulty to influence **Gold and gem payouts**. Gems are principally for character summoning, and Gold is principally for other RPG services/equipment/transactions, but the exact currency uses and award rules remain **flexible, not fully fixed**. No amount or difficulty rating has been chosen.
- **Dungeons:** discovery should be intrinsically rewarding, with worthwhile loot; they are one of the major gem-earning content sources. Dungeon layout, duration, repeatability, difficulty, resource drops and payout numbers have not been fixed.

### Recommendations under discussion — NOT yet user-approved details
- **Probabilistic flee:** the user raised escape chance because free, guaranteed fleeing would trivialize world navigation. Recommend a chance-based escape attempt: on success, exit at the encounter location; on failure, the attempt costs an action/opportunity and the enemy can pressure the party. Do not set a numeric percentage until tested. Whether bosses disallow fleeing or modify odds remains open.
- **Retry-loop protection:** a small escape probability alone cannot prevent unlimited escape attempts, so failed attempts should create actual time/turn pressure; escaping should not award clear rewards or allow a bypass of required boss/encounter victories.
- **Combined NPC + quest-board workflow:** local boards list repeatable regular jobs, while villagers/other named NPCs can offer memorable or specialist tasks. Both use the same underlying server-owned contract records, journal and payouts to avoid duplicated systems. This is preferred over making the player browse several disconnected interfaces.
- **Reward directions:** first dungeon clears should feel valuable; later repeats can still provide materials and useful progress, but do not lock all gems behind one endlessly repeated easy dungeon. Exact first-clear and repeat payouts remain to be designed, not an approved economy.
- **Protect the overworld avatar during battle:** since the Minecraft world does not globally pause and the player is directing four characters rather than fighting as an avatar, define the avatar's physical vulnerability to mobs, hazards, other players, world changes, disconnect and death; server authority must prevent exploits and double settlements.
- **Checkpoint exploit consideration:** loss-free defeat returning to a checkpoint might be used as free fast travel. Assess its impact in real play; do not introduce an unrequested item or currency death penalty as a workaround.

### Existing technical reference and scope
- On current source, `BattleSessionManager.command` treats `FLEE` as an unconditional session end when `fleeAllowed()`, not a probabilistic retreat action. `BattleOutcome` currently has `RUNNING`, `ALLY_VICTORY`, `ENEMY_VICTORY`; fleeing is not a modeled outcome. Adding chance/failure costs requires real gameplay changes, not merely a value tweak.
- Old `RegionalContractService` demonstrates NPC-delivered repeatable region tasks with Gold/party XP, but it is legacy authored gameplay and is NOT active new-game content. Its architectural ideas may be reused after review, without importing its old map, NPCs, payout values or forced story requirements.
- This checkpoint records **content direction only**. No battle defeat checkpoint teleport, flee probability, new NPC or noticeboard, dungeon spawn/rewards, or gem settlement is implemented/tested in the rebuilt empty content.

### Next co-design unit
- Pick a **contract acquisition UX** (proposed combined noticeboard + NPC), then detail dungeon exploration decisions and checkpoint placements before setting reward amounts or summon-currency pacing.


## Confirmed unified contract acquisition — 2026-11-06
- **Both village noticeboards and resident NPCs issue contracts.** Noticeboards are the principal access point for routine/open contracts; residents/NPCs can offer personal, place-specific or special contracts.
- They share **one contract system**: common objective tracking, acceptance/completion state, player journal and authoritative reward settlement. Do not create parallel unrelated quest economies or duplicate credit just because two sources expose a contract.
- Main story is not required; contracts remain optional activities in the Minecraft survival + turn-battle loop.
- Reward direction already chosen: difficulty-sensitive Gold and summon gems; exact payout amounts, difficulty brackets and available job count remain open. Gold primarily serves other progression/services, gems primarily serve character summons, without permanently freezing every possible use.
- **Unresolved content design:** job categories and objectives, contract generation/availability, delivery and hand-in rules, limits on concurrent/recurring jobs, cancellation, dungeon integration, NPC interaction details and any exceptional reward cases. Do not manufacture a fixed initial contract count.
- **Design documentation only**: NPC/board contract UI, world registrations, new rewards and multiplayer state are not implemented or playtested.

## Graded stochastic contracts and dungeon quest discovery — 2026-11-06

### User-chosen direction and user proposals
- **Replace reward stereotypes by objective type with an independent quest grade/difficulty axis.** Gold and summoning gems are awarded according to contract difficulty/grade and actual challenge; killing, collection, exploration and dungeon tasks are not permanently Gold-only or gem-only categories. Exact grade labels, bracket counts, drop tables and pay amounts are not fixed.
- **Ordinary contracts** may originate from either village noticeboards **or suitable villager NPCs**; **special contracts** originate only from NPCs or special world interactions/events, not the ordinary noticeboard listing.
- **Both ordinary and special contracts should have probabilistic occurrence/availability.** Villagers that offer a contract should vary across world-generated villages; no universal permanently fixed quest-giver list, no requirement that every villager offers work.
- User prefers **not** to create a separate category of identically repeatable contracts. Distinguish each completed/consumed individual contract from future newly generated contracts. Infinite copies of a single trivial request should not become the primary gems economy.
- **Dungeons are worthwhile independently of quests**, and they are a primary source of summoning gems. Whether a dungeon-related quest points toward its location from a resident or begins via an NPC/event at the entrance is a **question still under co-design**, not a resolved user requirement.
- The player's previous acceptance of unified quest tracking and NPC/noticeboard acquisition still applies; the newer special-contract exception supersedes earlier blanket statements implying every quest may appear on either surface.

### Design recommendations (NOT user-approved binding specifics)
- Represent grade independently from mission kind; calculate grade using actual threat, travel/risk, dungeon difficulty and required effort. Higher grade should improve the expected total value of Gold and gems, not necessarily require identical rewards for every mission. Do not substitute a fixed drop value based solely on objective type.
- For grade naming, E/D/C/B/A/S is only an illustrative UI scheme; **no letters or tier counts are approved**.
- Create ordinary contracts as **persisted instances** with objective, region, giver/source, grade, designated targets, completion and claim state. Generate new instances from region-appropriate event/templates when world conditions allow; never re-roll an existing offer simply by clicking an NPC repeatedly, rejoining or breaking/replacing a board.
- A special contract may appear with a low but nonzero chance after an eligible world occurrence (expedition clue, rare encounter, dungeon lead, NPC incident), rather than on every random villager conversation. Keep sufficient authored variety and meaningful stages so mere random parameter swaps do not masquerade as content.
- Avoid a dedicated daily/weekly contract reset or requiring the exact same completed job over and over. **Economic caveat:** if one-off jobs are finite, a primary summoning currency runs out. Continually generated *new distinct contract instances* are a candidate solution, but their quality and supply need playtesting. No specific respawn timer, generation rate, cap or guaranteed minimum is approved.
- **Dungeon-related lead distribution:** recommended majority mode is a villager/contract informant describing a rough region, landmarks or route to a **real existing world-generated dungeon**, allowing exploration and discovery. A rarer entrance-site clue or distressed NPC can initiate a **special event/quest** at the dungeon itself. Avoid one generic NPC standing in front of every dungeon. A dungeon remains enterable and rewarding if found without a quest. These are suggestions, **not yet agreed user decisions**.
- Quest-to-dungeon pointers MUST resolve to a real generated structure rather than pointing to a nonexistent or already exhausted dungeon; per-player multiplayer clear semantics and worldgen chunk availability need implementation design.
- Reward stability: no gem duplication from multiple quest-giver surfaces, overlapping objectives, board refreshing, villager multiplication, reconnection or repeatedly completing the same persisted instance. Per-player completion and multiplayer target ownership require explicit rules.
- Design the objective verbs and actual player choices before finalizing numerical gem/hour expectations or assigning fixed counts to quest types, NPCs or dungeons.

### Supporting game comparisons, not implementation copies
- *Monster Hunter: World* investigation quests can be generated from monster traces/activities, relate to a field area and provide better rewards with difficulty. Japanese/Korean game tutorial transcription: https://mhw.poedb.tw/kor/panel_tutorial ; community investigation overview: https://monsterhunterwiki.org/wiki/MHWorld/Quests .
- *The Witcher 3* links noticeboard contracts to clients and discoveries; notices can direct players to locations. Reference catalog: https://witcher-games.fandom.com/wiki/Notice_board and https://witcher-games.fandom.com/wiki/The_Witcher_3_contracts .
- These are **design references**, not approvals to copy private code/content or assets.

### Implementation state and next gate
This is a **design document change only**. No procedural NPC offers, board generation, special events, quest grade/reward engine, dungeon lead system, new state persistence or gem settlement exists in the new-game empty runtime yet.
Before implementing, co-design **the contract lifecycle**, **frequency/eligibility by region** and **dungeon discovery** through actual in-game choices; keep all numerical payouts and probabilities open.


## Dungeon exploration design reference checkpoint — 2026-11-06
- Deeper dungeon design research and **unapproved proposed gameplay structure** recorded in `DUNGEON_GAMEPLAY_STRUCTURE_REFERENCE_AND_OPTIONS_DRAFT.md`.
- External inspirations: YUNG's Better Dungeons (recognizable layered themed structures), When Dungeons Arise (discoverable spectacle with authored modular pieces), Vault Hunters (varied dungeon objectives); do not copy code/assets or claim current TURNBOUND 26.2 compatibility.
- **No new dungeon mechanic has yet been user-approved by this research alone.** Next co-design pivot: whether every dungeon needs a boss or only the major ones; preserve dungeon discoverability without mandatory quests, meaningful gems and reward ownership, loss-free defeat, and same-world party turn battles.
- Design/documentation work only, no Java changes or tests/build/playtest.

## Confirmed frequent procedural dungeons, optional full clears and three-defeat instance lockout — 2026-11-06

### User-approved gameplay direction
- **Dungeon presence:** the normal generated survival world should contain **many discoverable dungeon sites**. Prioritize world coverage and rewarding exploration rather than locking in a large number of distinct handcrafted themes. The actual structure frequency, spacing/biome weights and world count remain tuning decisions, not approved numbers.
- **Procedural variants within a recognizable theme:** allow dungeon instances sharing a concept/visual identity to have meaningfully different spatial layouts, room composition, enemy/elite placements, objective path lengths, difficulty, encounter mix and loot choices. Reuse high-quality authored modules assembled with controlled generation; avoid random shape soup, unreachable rewards or generic repetitive hallways.
- **Combat is not required in every room.** There is a readable main route plus optional branches; the player may choose to defeat extra enemies, explore more chambers and **clear all available encounters/loot for maximum rewards**. Some guards or objectives may still be mandatory for a specific main-clear reward.
- **Boss mix:** small ruins/exploration sites need not have a boss; bigger/significant dungeons should feature a climactic boss and richer rewards, without fixing a taxonomy or exact number of rooms/structures.
- **Within a specific physical generated dungeon, three party-battle defeats trigger a ban on entering THAT dungeon.** Other instances, including the same theme elsewhere in the world, remain accessible. Do not impose the lock across an entire dungeon type or across the player's world.
- Regular defeat still returns the Minecraft avatar to a valid checkpoint with **no inventory, character XP, Gold, gem or equipment loss**; this dungeon-specific access loss is the intended non-monetary consequence. A defeat is not a victorious clear and gives no unearned battle rewards.
- Dungeon entry does **not** require an NPC/contract. Dungeons are worthwhile on discovery, support gem earning, and should offer useful side-room rewards besides their major clear rewards.
- The player explicitly accepted the previously suggested exploration/branching/reward framework and the small-site boss exemption / major-dungeon boss direction. Other unapproved numeric content, spawn tables and named dungeon identities remain open.

### Recommended implementation and safety constraints (not extra rules the user explicitly finalized)
- Assign each generated physical dungeon a persistent **instance UUID or stable world/dimension + generated-structure identifier**, independent of the visual theme. Keep a **per-player, per-instance defeat count** (0–3+) and an admission flag; persist across relogs and server restarts so reentering cannot reset the count. Avoid counting every character faint as a party defeat; count only a final lost battle in that structure.
- Minecraft is a destructible shared world, so an intended 'entry ban' needs **server-authoritative region/admission enforcement**, including digging, flying, teleporting, alternate entrances and reconnect positions; a mere locked entrance door is not sufficient. Avoid permanently preventing players from modifying other parts of the world.
- On the **third defeat**, select a safe checkpoint *outside the now-forbidden instance*; do not return the player to an interior checkpoint and trap them. Previously approved loss-free checkpoint behavior remains, and no additional death penalty should be invented.
- **Duration/unlock semantics are unresolved:** the user required denial after 3 defeats but did not explicitly select permanent lockout versus a future way to regain access. Do not quietly assume a timed cooldown, paid reset, or permanent account-level loss. Clarify at a later design decision before implementing.
- Other players' own defeat counters should remain independent by default; actual multiplayer instance ownership, looting, bosses and dungeon mutation require explicit server-world policies.
- Distinguish **dungeon entry** from **combat entry/exit**. Leaving voluntarily should not automatically count as a defeat, and intentionally retreating through a normal exit should not spend a defeat unless the user later approves it. Fleeing inside a battle remains a separate pending design.
- Worldgen should use authored reusable rooms/templates and weighted variation with graph/path validity checks. Java Jigsaw/template-pool worldgen is a plausible basis. Real-world generated chunks must remain stable for a given seed and saved terrain; do not regenerate on login or player deaths. Worldgen must avoid overwriting saved builds/chunks and significant terrain/cave damage.
- Balance high structure availability against exploration surprise, visual clutter, server chunk generation cost and loot inflation: no arbitrary claim that a numerical spacing/density is already tuned.
- Track major first-clear and side loot independently, so optional combat rewards can be collected once as designed while duplicated gems on relog, chest replacement, or team contention are prevented. Exact repeat/refill rules and per-player/per-party first-clear status are unsettled.
- Room/battle-geometry validators should check reachability, lack of accidental seals, exit routes, no liquid/lava softlocks, camera headroom and four-actor party placement on actual terrain.
- References for technical concept only (not audited NeoForge 26.2 APIs):
  https://minecraft.wiki/w/Jigsaw_structure
  https://minecraft.wiki/w/Template_pool
  https://minecraft.wiki/w/Tutorial:Custom_structures

### Still open (do not assume final design)
- Whether 3-loss dungeon exclusion is permanent or recoverable; what can reset it, if anything.
- How generated instance bounds and multiple entrances are represented and enforced, how checkpoints interact with nearby structures, and how multiplayer physical loot works.
- Exact distribution, density, biome-specific themes, difficulty parameters and whether depth/biome determines enemy strength; no fixed dungeon count or genre-specific labels.
- Rules for side-room loot, optional rare battles, clearing all rooms, previously looted sites and re-encounter spawning.
- Exact dungeon list, bosses, rewards, gems, durations, structural art, loot, NPC leads and crafting-material identities.

### Status
**DOCUMENTED ONLY.** No worldgen/room generator, lockout persistence, access gate, checkpoint behavior, loot attribution or actual playable dungeon has been implemented or tested as a consequence of this decision.


## Confirmed three-defeat, 30-online-minute individual dungeon lockout — 2026-11-06

### User decisions superseding old open lockout duration
- A character party's **third full battle defeat in one specific generated dungeon instance** imposes a **30-minute admission lockout for that exact dungeon and player only**. This is **not a permanent ban**, and it does not affect other dungeon instances even when sharing the same theme or structure template.
- Time is measured in **cumulative time connected to the Minecraft server/world**, **not wall-clock time**. Offline time does **not** reduce the remaining lock. The remaining duration is retained across player logout/login and server restarts.
- After accumulating **30 minutes of connected server play**, the dungeon becomes accessible again and its per-player defeat count resets to **0/3**. Repeated future defeats can trigger a new independent 30-online-minute lock.
- The triggering third defeat follows existing user-confirmed loss-free defeat behavior: teleport to a valid checkpoint **outside that prohibited dungeon**, retain inventory, party equipment, XP, Gold and gems; no unearned victory or dungeon-clear rewards. Other players' own admission and defeat counts remain unaffected.
- Count only **actual party defeat outcomes** during that dungeon's battles. One fainted party member, a successful escape, voluntarily leaving a dungeon, or a successful battle does not increase defeat count by itself.
- A lock is an **entry/admission constraint** and should be enforced server-side over actual structure bounds/entrances, not only by locking the visible entrance door; otherwise mining, flying, teleportation or log-in-position exploits could bypass it. Avoid restricting unrelated overworld construction/traversal.
- Persist keyed **player identity + physical dungeon instance identifier + loss count + remaining online lock time**; time should decrement once per eligible online server tick (or equivalent trusted server-side elapsed session accounting) without client-provided timing. Count/entry state changes must be saved durably and handled correctly across world unload/relog/disconnect.
- **No separate real-time expiry, gem-paid reset, daily reset or permanent access denial** is authorized. A remaining 18-minute example after playing 12 minutes and disconnecting follows the same counter when reconnecting.
- New initial design specifics (checkpoints, dungeon structure bounds, UI, single-player pause semantics, actual time source, multiplayer interactions and playtest balancing) require implementation review. Online clock includes connection time; no AFK penalty or AFK-exclusion rule is approved.
- This is **design-only**; no running Java timer, lockout gate, persistence migration, structure registry, GUI, automated test, build or live Minecraft playtest has been performed.


## Dungeon room fog-of-war, compulsory entered-room combat and layered rewards — 2026-11-06

### User-approved direction
- Keep each generated dungeon's **unexplored room contents undisclosed**. Before entering a room, players should not receive exact lists of enemies, rewards or full room types through a map/menu. Discover room content on entry. The dungeon's broad danger/grade may remain visible for fair risk assessment, but the exact encounter is a surprise.
- **Entering a combat room initiates a battle; the player cannot simply cross it as if the combat did not exist.** That local forced encounter is distinct from the otherwise optional choice to enter that room or explore another branch. Not every room must contain combat.
- **Offer layered rewards**: individual victorious encounters, battle-room completion, treasure/reward chambers, major dungeon objective clear, and an extra full-clear reward for thoroughly clearing the dungeon. The user suggested these as reward mechanisms to design further; exact content, stacking rule and award quantities are NOT approved.
- Preserve prior user choice: players can clear the main route only or enter extra rooms and finish every battle for additional rewards; small ruins can lack a boss while major dungeons contain bosses; generated world instances vary theme modules, room graph and difficulty.
- Preserve prior 3-party-defeat limit **per player + specific physical dungeon**, followed by **30 minutes of cumulative connected playtime** lockout at that same dungeon only (not real-world wall-clock or permanent ban), and loss-free checkpoint return.

### Recommended interpretations and safety rules — subject to subsequent approval
- **Entry trigger:** do not auto-start a battle merely because a procedural room loads or a chunk appears. Trigger once when the actual exploring player intentionally crosses a recognized combat-room threshold / engages the combat-room representative, without allowing the player to run straight through uncleared guarded objectives.
- **Escape not silently abolished:** the user proposed probabilistic flee earlier, but final flee odds and whether dungeon rooms allow flee remain UNRESOLVED. If flee is permitted, a successful attempt exits combat without battle/room/full-clear rewards and does not count as a defeat; an uncleared combat room stays uncleared. If fleeing is prohibited in a room, communicate that limitation before the decision is committed. Do not treat 'combat automatically starts on entry' as blanket approval to ban every flee action.
- **Fair discovery:** hide room specifics, not broad dungeon threat. Give atmosphere/indirect danger cues where appropriate without a menu revealing exact rewards. Avoid deceptive rooms that kill a low-level party instantly with no reasonable option to avoid the dungeon itself.
- **Full-clear criterion:** recommend all reachable generated combat-room victories + main dungeon objective, rather than demanding impossible-to-find secret caches, irrelevant safe rooms or constantly respawning field mobs. Count completion at a stable generated-instance ID with stable per-room IDs. Rooms with no battle may offer exploration loot and should not block full-clear unless they carry a clear main objective.
- **Layered payout coherence:** an enemy defeat and battle-room clear can be distinct milestone events, but do not mechanically award the *same* loot twice. Gem and rare-material sources must be rationed across basic/side/full-clear rewards with server-idempotent per-player or shared-loot semantics to prevent overpayment, relog farming or PvE multiplayer disputes.
- **Traversal bypass:** Minecraft players can mine/place blocks, fly or teleport. Do not convert the whole dungeon into indestructible no-build zones by default. Server-owned guarded objectives/loot can be claimed only after encounter victory, regardless of reaching a physical reward room by an alternate path.
- **Seeded generated truth:** room type, placement, reward identity and combat selection must be generated/stored persistently per dungeon instance and not rerolled on reopen, relog, interaction spam or retry. In multiplayer the physical world/layout is shared; whether cleared-room states and major rewards are personal or shared needs further design.
- **Anti-chain-combat UX:** battle must not trigger just from standing still after battle resolution; mark room completed and provide safe breathing room. Thresholds and door behavior must support player movement and camera visibility on real world geometry. Exact interactions/visuals await actual Minecraft playtest.
- **Dungeon loss rule:** only final party defeats add 1 to that individual's 3-loss counter; successful combat, voluntarily leaving and successful escape do not. Rewards already legitimately claimed should remain after later defeats; no inventory/currency loss.

### Next design issues still open
- Definition of a valid 'room entry' in an editable survival world and the interaction between fight triggering, escape and physical exiting.
- Which specific room categories to implement; randomization weights, number of encounters, main-goal variability and how often a dungeon can be all-cleared.
- Whether major treasure chambers are reachable without fighting, whether full-clear reward can be re-earned, first-clear versus replay benefits and whether older dungeon instances replenish.
- Exact Gold/gem/material allocation across ordinary kills, room rewards and main/full-clear rewards, and quest overlap — only after content pacing is designed.
- Multiplay looting policy, per-player versus party clear credit, instance locking & checkpoint resolution.

### Status
Documentation only; no combat-room trigger, loot-room actor, procedural reveal masking, reward settlement, full-clear state, Java tests, new JAR or Minecraft playtest was run here.


## Confirmed Minecraft free movement and combat-session room exit gate — 2026-11-06

### Player-confirmed exploration/combat boundary
- **Dungeon traversal remains normal first-person/third-person Minecraft 3D world movement** on actual generated terrain. The player walks, sprints, jumps and explores rooms/corridors directly. Dungeon traversal is **NOT a menu-choice/node-by-node roguelike room selector** and is NOT an instanced separate-dimension dungeon.
- Room contents remain hidden before crossing a room entrance. A room may contain treasure, exploration, environmental interaction or enemies; **not every room forces combat**.
- **Entering a combat room initiates a turn-based party battle; after encounter start the participant cannot physically leave that room until the battle is resolved.** They cannot just sprint/dig/fly/teleport out while combat is unresolved or bypass the encounter without its intended outcome.
- The intended room exit resolutions are **enemy defeat (victory)** or **successful flee (escape)**; a party wipe resolves as a **defeat** and returns the user to the appropriate checkpoint (with the established per-dungeon loss count). A failed probabilistic flee attempt does **not** finish combat or allow leaving.
- **Winning** permits normal 3D exploration from the resolved room and awards earned encounter/room rewards. **Escaping** permits leaving the combat room without those victory/clear rewards and leaves the room uncleared. An immediate reentry must not create an uncontrollable infinite encounter loop; exact escape repositioning/retrigger grace is to be designed.
- The **choice to investigate/enter a branch** is the user's while exploring physical Minecraft space. Once a *combat* room is entered, its encounter is obligatory until victory/escape/defeat, which is separate from requiring combat in every room.
- Preserve same-world local battle presentation with 4 collectible characters fighting; player Minecraft avatar only controls party and is NOT a fifth combatant. Other players and world keep running, and protection from external actors/damage while in the battle UI remains a required technical design issue.

### Implementation boundary
- Recommended execution: server-owned participant battle-session state that denies in-battle exploration actions/position escape and changes 3D controls to turn-battle camera/UI only during combat; it must resolve end states and restore safe Minecraft-world control exactly once.
- Minecraft terrain remains editable in normal dungeon exploration; do not globally forbid mining/building in an entire dungeon merely to stop **in-battle** exits. Define server-side encounter boundaries/eligibility so alternate paths cannot evade an already-active encounter.
- **User has not selected numerical flee success chance, boss flee policy, combat-room trigger shape, default reposition after flee, avatar invulnerability implementation or multiplayer session joining.** Do not silently invent those specifics.
- Previously approved individual per-physical-dungeon **3 defeat → 30 connected-playtime-minute lockout** with loss-free checkpoint return remains in force.
- No new room trigger, UI, collision gate, combat control enforcement, code, build or playtest has been performed. This is a planning/contract change only.


## Confirmed combat-space clearance and all-enemies dungeon full clear — 2026-11-06

### User decisions
- **Dungeon battle chambers must be spacious enough for the actual turn-based battle camera and full character/enemy lineup**. Allocate horizontal room for the up-to-four-character party and varied enemies, vertical camera headroom, consistent sight lines, animations and VFX. Avoid camera clipping into walls/ceilings or actors, and ensure large enemies/bosses have enough authored clearance. Corridors and noncombat exploration spaces may be narrower; these are ordinary Minecraft 3D walkable spaces, not menu choices.
- Layout generation must validate combat chambers for actual terrain actor placement, camera distance/FOV/orbits where used, occlusion, headroom, hazards, visibility and access. Do not hardcode a room dimension before representative models and camera composition are playtested; if an autogenerated room fails a check, select/assemble a compliant room instead.
- **Dungeon all-clear = defeat every dungeon-authored enemy / enemy encounter in that physical dungeon for the active clear cycle**, including optional branches and hidden combat rooms. Bosses are included if present. Treasure collection, ordinary noncombat rooms and optional nonenemy puzzles are NOT extra requirements. Do not require unrelated roaming overworld spawns, endlessly respawning mobs or an unbounded enemy source to reach 'all clear'.
- Combat-room victories must persist in the appropriate dungeon completion state, so players may clear multiple rooms over an exploration attempt and earn the full-clear award only after the last eligible enemy is defeated. Fleeing a combat room does not count as defeating its enemies; prior user-approved separate per-instance 3-defeat, 30-online-minute lockout rule remains.
- **Historical uncertainty superseded by the latest 2026-11-06 replay decision appended below:** repeat clears are allowed after one hour, with no reduced payout.

### Historical replay recommendation (superseded by current full-reward replay)
- **Allow replaying already-cleared physical dungeon instances**, because many-world-site procedural dungeons should remain worthwhile to revisit. Keep the original terrain, saved player changes, generated room structure and dungeon identity stable; only designed hostile encounters/treasure states become eligible again for a distinct subsequent clear cycle.
- **SUPERSEDED:** earlier first-only bonus / reduced repeat payout proposal was rejected in favor of equal rewards across repeats after cooldown. Ordinary combat drops/materials may remain useful; do not let repeated easy dungeon gem rewards dominate discovering new sites, completing graded contracts and exploring harder dungeons. Exact gem, Gold, material and gear quantities are unapproved.
- For replay, design explicit server-owned activation, per-player clear-cycle tracking, reward idempotency and shared-world multiplayer behavior. NPCs, monsters, room doors, treasure and world block changes cannot be independently reset for each player without a deliberate multiplayer policy.
- The existing **3 failures in one specific dungeon → 30 online-minute ban for that individual** remains independent of reward replay. Do not silently reset the loss count on a successful clear or allow repeated entry to evade it.
- Repeated runs should not overwrite player-made changes or unconditionally rebuild an already generated structure; the dungeon layout remains a persistent part of Minecraft world.

### Status
Planning/doc change only; no camera room geometry, all-clear tracking, replay/reset logic, reward economy, builds or real playtests have been implemented/verified.


## Latest dungeon replay decision — 2026-11-06
- **APPROVED:** no enemy/battle/loot refill until a dungeon run has been cleared. That physical dungeon may be replayed after a **one-hour cooldown following a clear**, with the **same normal combat/treasure/main-clear/full-clear reward rules and payouts**, not reduced repeat rates.
- An all-clear still means defeating all dungeon enemy encounters in that run. Keep its distinction from the ordinary main-clear trigger pending detailed design. The one-hour cooldown's wall-clock vs online-only basis has **not** been chosen (unlike the separately specified 30-online-minute personal 3-defeat lockout).
- Source of detailed contract and open implementation questions: `DUNGEON_GAMEPLAY_STRUCTURE_REFERENCE_AND_OPTIONS_DRAFT.md`. No implementation or testing performed.

## Non-commercial, generous repeatable gem farming — 2026-11-06

### User-confirmed intent
- TURNBOUND is principally being made for private/non-commercial enjoyment and likely will not be distributed. The player explicitly **wants long dedicated dungeon farming sessions to reward players with substantial summon gems**; do **not** add restrictive anti-grind economics, hidden repeated-clear reductions, daily gem earning caps or forced diminishing returns simply to control player engagement.
- Continue the approved policy: **same reward eligibility and value for every eligible replay**, with the existing one-hour cooldown following dungeon completion and no encounter/treasure refill before completion. The cooldown itself was user-approved and is **not** repealed by this decision.
- The exact rate and quantity of gems are not set. Balance for satisfying voluntary effort, challenge and content variety rather than monetization or fear that a committed player may summon many characters.
- Do not silently decide whether the one-hour replay cooldown advances offline; the user said to proceed to the next topic without choosing online-only vs real-world clock. The already-approved 30-online-minute lockout after three losses is a separate timer.

### Next design proposal (NOT yet an approved rule)
- **Ordinary dungeon clear** could mean completing a structure-specific main objective (boss, central relic, main treasure or similar). **All clear** is independently user-confirmed as defeating **every dungeon-authored enemy**, including optional rooms. If the main objective is met first, players may remain in the current cycle to finish side encounters and claim an all-clear bonus before a voluntary replay/reset.
- A post-clear cooldown should not prematurely erase uncleared optional enemies or prevent collecting legitimately available full-clear rewards. The exact lifecycle, reward-claim timing, reset activation and multiplayer sharing require a separate user decision; do not implement this proposal as settled.
- The user already confirmed full-clear enemy defeat; avoid reclassifying unopened noncombat treasure rooms as mandatory full-clear targets.

Status: documentation only. No economy code, cooldown clock implementation, dungeon objective code, tests, build, JAR or gameplay validation performed.


## Latest dungeon exit-clear and result UI protection — 2026-11-08
- **Confirmed:** entering/finding the escape chamber, killing its boss, or discovering its exit does not clear the dungeon; **only using its exit object and actually leaving** marks an ordinary clear, pays completion reward and starts the one-hour dungeon replay cooldown.
- Before choosing to leave, the player may backtrack to clear every finite authored dungeon enemy, then use the exit to receive the ordinary clear plus **all-clear bonus**. Combat, room and treasure rewards remain separate.
- **Confirmed:** enemies must not approach/attack/re-aggro a player while reward or dungeon-clear result UI is open; battle-result UI should receive matching protection. Preserve multiplayer world activity for others and design safe transitions rather than a global world freeze.
- Detailed canon and open implementation issues: `DUNGEON_GAMEPLAY_STRUCTURE_REFERENCE_AND_OPTIONS_DRAFT.md`. Documentation only, not implemented/tested.

## Dungeon exit UI and multiplayer ownership proposal — 2026-11-08
- Current approved explicit exit/interact, all-enemy all-clear at exit, personally scoped 3-defeat 30-online-minute lockout, and non-decaying repeat rewards after one-hour cooldown remain authoritative.
- **Unapproved co-design options only:** exit UI interaction sequence, safety implementation and same-world multiplayer ownership. Favor discussing shared physical dungeon-run state with per-player rewards/defeat tracking versus independently simulated rooms in the same physical world before implementation.
- Reference: `DUNGEON_EXIT_UI_MULTIPLAYER_OPTIONS_DRAFT.md`. No gameplay code or test/build changes.

## Confirmed cooperative dungeon run and joint exit — 2026-11-09
- **User-confirmed cooperative mode:** same generated dungeon's authored enemy/room progress and full-clear completion are SHARED across active teammates. Teammates may split up and fight different rooms concurrently.
- **Combat/room/dungeon clear/all-clear rewards are full and equal for each eligible teammate, no reward splitting or missing rewards when somebody else fights**. Exactly-once personal server settlement required; team member eligibility rules for join-in-progress remain to design.
- **Joint exit:** the whole active dungeon party must gather physically in the escape room, then leave together through the exit interaction. An individual cannot unilaterally finish or pull absent teammates out. Specific who-clicks/confirmation interactions still OPEN.
- **Per-player only:** dungeon defeats, 3-loss 30-connected-minute entry ban and owned inventory/resources remain personal. A locked-out teammate, disconnect, staggered rejoin and exit vote/roster management cannot trap the other party indefinitely; exact policy OPEN.
- Preserve reward/result UI safety, normal Minecraft walking/building, on-site battles, one-hour full-value repeat cycle and all-enemies all-clear at actual exit.
- Multiplay same-room skill-control and chest/mined loot ownership are separate OPEN implementation/design decisions, not settled by equal currency/encounter rewards.
- Detailed authority: `DUNGEON_EXIT_UI_MULTIPLAYER_OPTIONS_DRAFT.md`. Documentation only; no multiplayer gameplay tested or built.


## Approved co-op disconnect and third-defeat spectator continuation — 2026-11-09
- **Game disconnect:** offline teammate no longer blocks team exit; remaining connected explorers continue the same physical dungeon run, and a lone survivor proceeds as solo. No room reset and no forced early clear. Offline member's later rewards/reconnect policy remains undecided.
- **Third personal defeat:** retain the per-player 30-online-minute admission ban plus loss-free checkpoint, but the defeated member becomes a **non-interactive spectator of surviving teammates** (remote camera, no physical dungeon access). The spectator remains linked to the team run and **receives the same clear/full-clear reward if surviving explorers exit successfully**.
- The exit-room physical gathering rule applies only to **connected active explorers**, not spectators or disconnected members. Spectators cannot trigger the exit or teleport into the blocked dungeon.
- Equal co-op rewards and shared encounter/room progress remain authoritative; protect per-player idempotent settlement. Spectator post-defeat event credit, reconnection, total wipe, split-target spectator camera and same-room combat control still require follow-up design.
- Canon: `DUNGEON_EXIT_UI_MULTIPLAYER_OPTIONS_DRAFT.md`. Design only, no game implementation or multiplayer playtest.


## Dungeon co-op combat inherits the overworld rules — 2026-11-09
- **Confirmed:** when allies enter the same dungeon battle room, use ordinary overworld/field co-op turn-based battle controls and combat outcomes. No separate dungeon-specific multiplayer battle mode, party size scheme, turn rules or new character-control decision.
- Dungeon layer contributes only the room entrance/exit gate, camera-safe combat space, generated enemy placement, shared dungeon progress and loot, spectator/admission handling and group completion.
- Generic field co-op actor/party ownership details are not assumed implemented or decided. Resolve them in the shared combat architecture once, not independently per dungeon. Source: `DUNGEON_EXIT_UI_MULTIPLAYER_OPTIONS_DRAFT.md`. Docs only; no client or multiplayer testing.
