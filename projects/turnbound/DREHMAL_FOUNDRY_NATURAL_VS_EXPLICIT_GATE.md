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
