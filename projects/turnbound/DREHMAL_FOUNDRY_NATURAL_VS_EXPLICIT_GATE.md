# TURNBOUND — Foundry 자연 레버 입력 vs 원작 문 함수 직접 진단 비교

2026-11-11 · branch turnbound-content-rebuild · Drehmal: APOTHEOSIS 2.2.2f (Minecraft 1.20.1 Fabric).

증거:
- [실제 막대기 클릭 후 세 레버 ON #42933135850](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42933135850): 레버 3개는 ON, 원작 점수판 항목은 각 클릭 이후 변경 관측, 정확한 문 복제 대상 블록 변경 0건.
- [원작 90블록 복제 기준 상태 #42935102184](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935102184): 소스·대상 전체 블록 상태 중 59개 서로 다름, 복제 source 공기 35개 vs 대상 공기 0개. 원본 문은 막힌 상태.
- 동일 정적 기준에서 foundry:levers/main에 score matches 1/2/3 세 조건. 실제 합산·동적 실행 경로는 미확인.

이번 실서버 진단은 *한 폐기용 공식 7+9 월드*에서 실제 플레이어 우클릭 3번의 결과를 기록한 뒤 원래 설치돼 있던 \`foundry:levers/door\` 함수를 **RCON으로 딱 한 번 직접 호출**해 문 복제 위치가 달라지는지 비교한다. clone source·destination의 90개 전체 블록 상태가 각 단계에서 서로 일치하는지도 측정한다.

여기서 직접 호출에 의한 문 변화가 관측되더라도 그것은 **원작 함수가 명시적 호출로 작동한다는 제한된 증거**일 뿐이다. 자연 업적/점수판에서 문으로 이어지는 인과, 실제 문 통행, 6웨이브 전투 미재진입, 원작 스토리·보상 분리, 4인 멀티를 증명하지 않는다.

원본 지도·게임 플레이 월드에서는 절대 실행하지 않고 실행 사본은 폐기. 기존 8단계 업적 보상 차단 9건은 적용하지 않음. 원작 코드/지도/리소스팩 배포·이식 0건. MAP SEPARATION COMPLETE: NO.

도구: tools/probe_drehmal_foundry_natural_vs_door.py
CI: .github/workflows/turnbound-drehmal-foundry-natural-vs-function.yml


## 2026-11-11 실제 공식 7+9 임시 서버 A/B 결과 — PASS

[GitHub Actions #42935729498](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935729498): 공식 SHA를 확인한 7+9 임시 사본, 원작 데이터팩·명령 블록 ON, 프로토콜 플레이어가 막대기를 들고 원래 Foundry 레버 3개를 차례로 실제 우클릭한 뒤 모두 ON 유지. 첫 세 단계에서 각각 원작 scoreboard 엔트리 2건의 변경을 관측했지만 **정확한 문 clone 대상 블록 변경 0개**, source/dest 90개 중 59개 불일치가 유지됐음.

동일 폐기 사본에서 원래 설치돼 있던 `foundry:levers/door`를 **테스트용 명령어로 딱 한 번 직접 실행**하자 대상 블록 **59개 변화**, source/dest 90개 BlockState가 모두 같아짐(불일치 0). 원작 코드를 TURNBOUND 게임에 복사·이식하지 않았다. 이는 문 함수 자체가 작동한다는 제한된 원인 대조 증거다. 원작의 자연 업적 보상 실행 여부·구체적 scoreboard 누적 조건·원작 게임 전투 재진입 여부는 아직 검사하지 않았음.

**다음 작업:** 공식 사본에서 `foundry:1/2/3` 실제 업적 상태와 `foundry:levers/main`의 matches 1·2·3 관련 scoreboard 상태를 동일한 클릭 이벤트별로 보수적으로 측정(클릭 없는 대조 시간 포함). 원작 문 함수는 보존하고, 문제가 확인된 자연 이벤트/점수판/호출 연결만 조정한다. 이후 Foundry 문 통과 및 별개 arena_start/wave_logic 원작 6웨이브 미재진입을 검사한다. MAP SEPARATION COMPLETE = NO.

## 2026-11-11 자연 레버 3회 클릭 / advancement 및 원작 점수 실서버 결과

[GitHub Actions #42940827429 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42940827429), 검증 코드 커밋 be4d9f40cc54b50f47ed30a40fb4493eb56da626.

- 정본 공식 v2.2.2f, 격리 7+9 폐기 사본, Fabric 1.20.1, 원작 데이터팩 및 명령 블록 ON. 실제 프로토콜 플레이어가 막대기를 든 채 레버 3개 OFF→ON 1회씩 자연 우클릭. 모두 ON 유지.
- foundry:1/2/3의 원작 reward 함수는 levers/1_check~3_check. 우클릭 후 저장된 advancement 완료 상태는 3개 모두 false였지만, check 함수에 advancement 명령이 존재하므로 **달성 직후 회수 여부 미확인**. 이 값만으로 자연 보상 콜백이 실행되지 않았다고 판단하지 않는다.
- 원작 levers/main 세 literal owner/objective 조건의 실서버 점수는 클릭 전 없음, 클릭마다 1→2→3으로 변했다. 각각 matches 1, matches 2, matches 3 조건이 순차 충족됐다. 각 클릭마다 scoreboard 레코드 변경 2건; 각 3초 무입력 대조 구간의 레코드 변경은 0건.
- 자연 입력 뒤 원본 90블록 문 복제 영역 source/destination 차이 59개 유지, 문 블록 ID 변화 0. 이번 CI에서는 원본 door 함수를 직접 실행하거나 advancement grant/revoke를 하지 않았다.
- 정적 함수 구성을 보면 levers/main에 원작 execute 명령 3개와 scoreboard 명령 1개가 있지만, levers/door에 대한 리터럴 직접 호출은 없다. 따라서 main 점수가 3에 도달했다는 관측을 곧 문 함수 호출 성공으로 취급하면 안 된다.
- 다음: levers/1_check~3_check의 advancement 명령 동사 확인, levers/main 조건별 run 명령의 **종류만** 파악, 원본 전체 데이터팩 내 door 함수 역방향 caller 조사. 필요한 경우에만 새로운 폐기용 사본 실서버로 인과를 분리한다.

**보존 경계:** 공식 원본 월드와 실사용 월드 변경 없음. 8단계 비무기 보상 차단 9건 미적용. 원작 문/아레나/웨이브 코드·지도 재배포·이식 없음. 문 실제 통행, arena_start/wave_logic/wave_1~6 비재진입, 클라이언트/멀티, 전체 맵 분리 **NOT VERIFIED**. MAP SEPARATION COMPLETE = NO.
