# TURNBOUND — Foundry 삼중 레버 조건과 90블록 clone 원본/대상 실측

2026-11-11 · branch turnbound-content-rebuild · official Drehmal APOTHEOSIS v2.2.2f / Minecraft Java 1.20.1 Fabric.

원작의 foundry:levers/door 는 source (-3274,134,1557)부터 +5×9×2 블록을 dest (-3278,156,1576)으로 복제한다. 기존 공식 7+9 임시 서버에서 레버 3개를 모두 켜고, 막대기를 든 사용 패킷으로 점수판 변경을 관측했어도 문 복제 대상 블록 ID 변경은 0건이었다(최신 #42933135850 PASS). 원인 판별 없이 플레이어 월드의 업적이나 문을 임의 차단·수정하지 않는다.

이번 읽기 전용 감사는 **원본 월드 SHA 확인 후 90개 source/dest의 블록 ID와 전체 BlockState를 1:1 비교**한다. original foundry:levers/main / door / door1 / 1_check~3_check / 1~3 함수의 실제 조건 명령 형식, 정수 matches 조건, 정적 callee를 *원문 없이* 반환한다. 원본 코드·월드/NBT 재배포 불가. 실행 조건의 존재와 실제 실행은 구분한다.

다음에는 원본 상태가 복제 전부터 동일한지 / 문이 원래 열린 상태인지 / 삼중 조건이 참인지 보고 좁은 실서버 인과 검증을 설계한다. 현재 문 통행, 원작 6웨이브 전투 분리, 게임 START/4인 플레이는 완료 아님. MAP SEPARATION COMPLETE = NO.


## 2026-11-11 공식 원본 SHA 기반 원본/대상 상태 실측

[GitHub Actions #42935102184 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935102184)에서 `foundry:levers/door` clone(source -3274,134,1557 → dest -3278,156,1576; 크기 5×9×2)의 **90개 전체 BlockState 중 59개 다름** 확인. source 공기 35칸, dest 공기 0칸. 원본 문은 닫혀 있고 실제 clone이 정상 작동하면 블록 변경이 발생해야 한다는 제한된 판정 가능. `foundry:levers/main`에 score matches 1,2,3 세 조건을 식별했으며 **어떤 실제 scoreboard owner/objective에 적용되는지는 아직 이 검사에서 공개하거나 동적으로 증명하지 않았다**. [후속 문 함수 직접 호출 실서버 비교 #42935729498](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935729498)에서 직접 진단 실행 후 source/dest 90개 전체 상태가 동일해졌음. 문 통과/원작 6웨이브 억제/맵 분리 전체는 미완료.
