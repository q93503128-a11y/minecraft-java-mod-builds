# TURNBOUND — Foundry 자연 레버 입력 vs 원작 문 함수 직접 진단 비교

> **2026-11-11 Foundry 자동 tick 단절의 원인 확정:** 공식 7+9 사본 준비 도구가 \`minecraft:tick\`에서 \`entities:main\`을 포함한 원작 진입점 두 개를 **의도적으로 제거**한다. 원본 ZIP의 호출 경로는 존재하지만 사본에서는 자동 실행되지 않는다. 레버·점수·문 주변·상위 조건·\`@a\` 선택은 모두 정상, 문 복제 함수도 명시적 테스트에서 정상이다. 원작 전체 tick 복원 금지, 시설별 독립 어댑터 검토. [원인/증거](DREHMAL_FOUNDRY_TICK_ROOT_CAUSE.md). 맵 분리 NO.


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

## 2026-11-11 원본 5,487개 함수 대상 문 호출자 역추적

[정적 검증 #42942060986 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42942060986), 정본 SHA 검증 원본에 대한 읽기 전용 검사.

- 원작 foundry:levers/main에 세 개의 matches 1/2/3 조건이 있으나 각 조건 뒤에서 실행하는 명령은 tellraw. 문 열기 명령이나 door 직접 호출이 아니다. 세 조건은 동일한 원작 scoreboard (owner, objective) 키 1쌍에 대한 값 비교다. 실제 이전 실서버 관측 값 없음→1→2→3 및 순차 메시지 조건 충족과 부합한다.
- foundry:levers/1_check~3_check의 advancement 명령은 모두 revoke. 따라서 클릭 이후 저장된 업적 완료 false는 재발동을 위한 즉시 회수 과정일 수 있으며, 업적 콜백 미발동이라고 결론 낼 수 없다.
- foundry:levers/1~3의 scoreboard 명령은 모두 set.
- 전체 원본 mcfunction 5,487개를 조사한 리터럴 호출자 목록에서 foundry:levers/door의 유일한 caller는 players:overworld_p, foundry:levers/door1의 caller는 foundry:levers/door. 원작 문 함수 자체 직접 호출은 이전 실서버에서 정상 작동 확인.
- 아직 players:overworld_p 자연 실행/실제 door 경로 인과, 상위 스케줄·tick 분기와 동적 선택자 조건, 문 물리 통행은 확인되지 않았다. 새 정적 검사는 players:overworld_p의 door 호출 조건 및 상위 함수/함수 태그 연결을 좁혀 조사하도록 확장했다.

본 증거에 근거해 원작 levers/main을 수정하거나 door 호출을 레버 콜백에 임의 추가해서는 안 된다. 기존 원본 문·레버·퍼즐 보존, 8단계 미적용, 실제 월드 불변, MAP SEPARATION COMPLETE = NO.

## 2026-11-11 original `players:overworld_p` 문 조건 구조

[원본 5,487개 함수 및 entity 선택자 정적 검사 #42943038839 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42943038839). 원본에서 `foundry:levers/door`를 호출하는 해당 함수 내 조건은 원작 levers/main과 **동일 점수 키**에 대한 `matches 3` 그리고 `if entity @s` 선택자를 함께 요구한다. 선택자는 좌표 (-3276,159,1570)에서 12블록 이하 범위를 가리키고 별도 `predicate` 리소스 필터가 있다. 이 조건들은 한 명령의 AND 조건이며, 실제 원본 명령행은 보고서에 포함하지 않았다.

상위 리터럴 호출은 `players:main` → `players:overworld` → `players:overworld_p`. Minecraft tick/load 태그에 `overworld_p` 직접 등록은 없으며, 상위 엔트리포인트가 실제로 언제 어떤 실행자 컨텍스트로 활성화되는지는 아직 입증되지 않았다.

기존 서버는 마지막 원격 레버에서 종료하여 위치 조건은 충족되지 않았다. 다음 폐기용 서버 실험에서는 기존 3레버 자연 우클릭과 3초 무입력 대조가 끝난 후 *새롭게* 플레이어가 확인된 조건 중심 반경 12 내의 안전한 발판으로 이동한다. 문 clone 90블록 전후 차이, 레버 상태 및 위치를 측정하며 원작 door 함수 직접 호출, `advancement grant`, 원작 원문·월드 편집 및 8단계 적용은 하지 않는다. 원작 `predicate`의 실제 판정도 해당 결과가 나오기 전까지 미검증.

**MAP SEPARATION COMPLETE = NO**. 실제 통행·원작 6웨이브 비재진입·게임 START/클라이언트·멀티 승인 전.

## 2026-11-11 공식 7+9 자연 문 접근 및 원본 predicate 분리 실서버 PASS

[GitHub Actions #42944727206](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42944727206), `11e7731e73815b81d2fcfb5ced7eed89e505a350`. 기존 실제 레버 3회 자연 클릭·무입력 대조·점수 1→2→3 확인 후 문 조건 중심 (-3276,159,1570)의 반경 12블록 내 안전 위치에 프로토콜 플레이어를 둠.

- 원작 문 호출 조건에서 `predicate` 항목만 제외한 `@s` 거리/좌표 선택자는 실서버 명령 평가에서 **true**.
- 원래 `predicate`를 포함한 전체 엔티티 선택자도 **true**. 해당 원작 predicate 리소스 ID는 `players:is_not_dev`, `condition: inverted`이며 원본 데이터팩 내부에 존재.
- 점수 `matches 3`은 직전 실서버 기준 충족되어 있고 레버 모두 ON. 자연 접근 이후 90블록 문 복제 source/dest 차이는 **59→59**, destination 블록 ID 변경은 **0**.
- 새 시험에서 원작 `foundry:levers/door`나 상위 원작 함수는 직접 호출하지 않았다. `advancement grant/revoke` 사용 없음. 선택자 참/거짓은 진단용 신규 scoreboard objective에서만 읽었고 검사 뒤 제거했다.
- 따라서 이번 폐기용 실서버 조건에서는 거리, 점수, 원작 predicate 미충족을 문 미개방 원인으로 볼 수 없다. **`players:main → players:overworld → players:overworld_p`의 자연 호출 여부와 실행자/실행 위치 컨텍스트는 여전히 미확인**이다. 이전에 원작 door 함수를 직접 1회 호출하면 59블록이 변했다는 제한 증거와 구분한다.
- 다음: 원작 함수/태그에서 `players:main` 실제 tick 진입 경로와 실행자 컨텍스트를 추적하고, 필요한 경우 폐기 사본에서 실행 경로만 별도로 실증한다. 정상 원작 문 함수·레버·퍼즐을 삭제하거나 모드에 이식하지 않는다.

**검증 경계:** original world SHA 보존, 공식 7+9 폐기 사본 한정, 8단계 미적용. 실제 문 통행, 6웨이브 비재진입, GUI 클라이언트, 4인 멀티, 사용자 START와 전체 맵 분리 **NOT VERIFIED**. MAP SEPARATION COMPLETE = NO.
