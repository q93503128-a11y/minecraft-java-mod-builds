# Drehmal 원작 시작 지급품·원작 발견도 명령 — 별도 사본 분리 실험

## 근거
- 공식 2.2.2f 원작의 [직접 명령 식별자 검사 #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42729125584) **SUCCESS**. 원작 명령 블록 2,232개 정본을 바탕으로, 실제 목표 명령이 위치한 72개 지역의 명령 블록 1,940개 명령 해시를 재검증했다.
- 직접 `give` 37개 중 `region/r.51.-1.mca`의 두 좌표 묶음(대략 x=26457–26465, z=-260/-288)에서 **침대 3, 빵 6, 돌 곡괭이 6, 가죽 부츠 6**, 총 **21개**가 반복 지급되는 것으로 확인됨. 주변에 원작 `gamemode`, `gamerule`, `spawnpoint`, `spreadplayers`, `setblock`, `execute` 등이 함께 있어 원작 시작 장치와 관련되어 보이지만 **실제 장치 연결/용도를 런타임에서 검증하지 않았음**.
- 원작 직접 `advancement grant @a` 3개는 `discoveries:homligrave`, `primordial:enteryavhlix`, `discoveries:divineruins`. 원작 게임 진행과 연결되지만 일부 위치의 장치 의존성은 알 수 없어 **정식 제거 승인 대상이 아님**.
- 나머지 `give` 16개, `effect` 127개, `kill` 20개, `summon` 81개는 지금 단계에서 손대지 않는다. `summon` 6개는 NBT 좌표 미기록.

## 검증 방식
`tools/preview_drehmal_starter_discovery_commands.py`는 공식 직접 명령 분석 **JSON 전체 SHA-256**을 고정하고 목표 24개 각각의 명령 SHA/좌표/대상/종류를 확인한다. 공식 원본 월드와 활성 TURNBOUND 월드는 거부하며, 새 오프라인 **복사본**의 대상 4개 region 파일에 있는 해당 명령 블록의 명령 문자열만 비운다.

백업 후 별도 무결성 검사와 원본 지역 파일 SHA 복구를 진행한다. 해당 명령 블록을 삭제하거나 주변 블록/지형/건물을 수정하지 않는다. 원본 코드가 TURNBOUND 게임 로직으로 복사·이식되는 일도 없다.

이 실험은 **5종 통합 분리본에는 아직 포함되지 않은 선택적 시험**이다. 게임모드/지급 명령 블록의 조건부 체인과 퍼즐·시설 기능은 실플레이 검증 전까지 미확정. 반드시 원본 기능과 구조물 양쪽을 확인한 후 통합 여부를 결정한다.

검증: [GitHub Actions 원본 공식 월드 복사본 24개 명령 차단·복구](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-starter-discovery-preview.yml).

## 2026-11-10 공식 월드 사본 검증 결과

- [검증 CI #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42729892272) **SUCCESS**. 원본 공식 `shard_1.zip` ~ `shard_3.zip`를 검증·추출해 별도 월드 복사본 생성.
- `inspect → apply → verify → restore` 전 과정 통과. 원작 시작 지급품 21개와 발견도 업적 지급 3개, 총 **24개 명령 블록**, 지역 파일 **4개**를 검증된 해시 대상으로 처리. 파일 변경 영수증 검증 및 원본 지역 SHA로 정확한 복구. **회귀 테스트 7개 PASS**.
- **실제 Minecraft 게임에 넣어 확인한 것은 아님.** 원작 명령 블록 체인, 조건부 실행, 주변 퍼즐·장치 호환성, 실플레이/멀티플레이는 미검증. 원작 지도 파일은 GitHub/아티팩트에 업로드하지 않았으며 사용자용 설치물도 생성하지 않음.
- 정식 5종 분리 검증에 이 24개를 추가하지 않은 상태를 유지. 다음 작업에서는 장치 의존성을 먼저 실측하여 편입 여부 결정. 원작 코드 TURNBOUND 게임 로직 재사용 **없음**.

## 2026-11-10 물리 명령 블록 체인 및 복합 실험

[전체 체인 방향/블록 상태 검사 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733734444). 시작 지급 21개는 6개 체인(57블록)의 일부이고, 기존 게임모드 분리 3개를 제외해도 시작 체인의 원작 명령 **33개가 추가로 남는다**. [기존 5종+24개 가역 통합 실험 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256037)는 오프라인 파일 해시/복원만 증명한다. **기본 5종 편입 아님, 실제 원작 시작 장치 분리 완료 아님**. [위치별 상세 보고](DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md).
