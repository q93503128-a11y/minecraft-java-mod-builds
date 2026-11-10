# TURNBOUND — Drehmal 원작 자동 입장 위치 이동: 보상 연결 한 개 분리

> 2026-11-10 · `turnbound-content-rebuild` / `projects/turnbound/` / Minecraft 1.20.1 Fabric / 공식 Drehmal: APOTHEOSIS v2.2.2f.
>
> **결론: 원작 자동 위치 복귀의 실행 연결 1개를 격리하여 실서버 A/B 검증 성공. 정확한 원본 복구 성공.** 그러나 **MAP SEPARATION COMPLETE: NO**. 시설/스토리/보상 전체 시스템 분리·실사용 월드 승인 아님.

## 1. 인과 증거 — 어떤 분리만으로 위치 이동이 사라졌는가

기존 [공중/바닥 실서버 #42808944435 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42808944435)에서는 Y223 공중 또는 같은 지형 내 Y219 완전 블록 바닥 위 두 플레이어가 공통 좌표로 강제 이동했다. 다음 [데이터팩 ON/OFF 비교 #42814595461 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42814595461)에서는 두 7단계 원본 맵 사본 중 하나의 **원작 `file/hi_drehmal.zip`만 비활성화**했다. 원작 데이터팩 유지 사본은 두 명 모두 이동, 비활성화 사본은 두 명 모두 이동량 0이었다.

- 원작 데이터팩의 활성 상태에 의존하는 현상임을 검증했다.
- 그러나 데이터팩 전체를 꺼서 실제 TURNBOUND 맵을 완성했다는 주장은 **금지**한다. 원작 문·퍼즐·전송·시설의 기능까지 잃을 수 있다.

[원본 데이터팩 정적 경로 분석 #42817029475 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42817029475)에서는 해당 고정 SHA의 단일 `hi_drehmal.zip` 안에서 **함수 5,487개, 업적 501개, 직접 텔레포트 관련 명령을 포함한 함수 534개**를 조사했다. 한 실행 후보:

- **업적 ID:** `advancements:primordial/root`
- **진입 조건:** `minecraft:location`
- **업적 보상 함수 ID:** `players:sethost`
- 해당 보상 함수에는 이동 명령 포함. 테스트 대상은 **업적의 `rewards.function` 연결 단 한 개**이며 함수 본문/업적 기준/다른 reward 내용은 유지한다.
- 정적 분석만으로 이 기능이 실제로 실행됐다고 주장하지 않는다. 별도의 실서버 검증으로 판정했다.

[단일 보상 연결 A/B 실서버 #42817029748 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42817029748):

| 폐기 가능한 공식 7단계 사본 | 원작 데이터팩 | 서버 `reload` | 공중 지점 수평 이동 | 원작 바닥 지점 수평 이동 |
| --- | --- | --- | ---: | ---: |
| `rewards.function` 연결 유지 | 활성 | 실행 | 210.345블록 | 213.214블록 |
| 정확히 `advancements:primordial/root`의 `players:sethost` 보상 연결만 제거 | **활성 유지** | **같이 실행** | **0블록** | **0블록** |

두 시험 모두 독립 원본 사본을 7단계 분리까지 진행한 다음 동일한 서버 구성, 명령 블록 OFF, 가상 프로토콜 플레이어 2명, 강제 이동·시간 관측 조건으로 검증했다. **두 경우 모두 RCON `reload`가 수행되어 리로드 자체의 차이를 통제**했다. 원본 공식 월드와 원작 함수 내용, 다른 ZIP member의 원문은 변경하지 않았다. 따라서 *이 실험 조건*에서 이 업적의 보상 함수 연결이 강제 이동에 필요함을 입증했다. 다른 경로에서 `players:sethost`를 직접 호출할 가능성까지 모두 제거한 것은 아니다.

## 2. 가역 분리 도구 — 공식 복사본에서 적용·검증·복구

[오프라인 정확 복구 #42818384756 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42818384756).

- `tools/preview_drehmal_primordial_root_ninth.py`는 **검증된 7단계 복사본에만** 9번째 독립 실험을 적용한다.
- 원작 `advancements:primordial/root`의 `rewards.function` 호출 연결 **1개**만 ZIP 안에서 차단. 업적 기준·다른 보상·모든 mcfunction·기타 원본 ZIP member의 **내용은 그대로 유지**한다.
- 변경 전 ZIP을 복사본에 별도로 백업, 적용 후 백업 SHA·ZIP 변환 결과·영수증·원래 57개 START 명령 블록 분리 상태를 검증한다.
- 복구 후 **검증된 이전 7단계 ZIP과 정확히 바이트 일치**하고, 자체 영수증·백업을 제거한 상태를 확인했다.
- 다른 기능을 작동시켰거나 사용자 월드에 설치한 것이 아니며, **원본 소스 바이너리·ZIP·원작 함수 내용은 GitHub에 커밋하거나 검증 아티팩트로 업로드하지 않았다**.
- [단위 테스트/보존·복원 테스트](tools/test_preview_drehmal_primordial_root_ninth.py)와 GitHub Actions 오프라인 실제 원본 복사본 테스트 모두 성공.

### 오프라인 사본 전용 명령

아래 명령은 준비된 **검증된 7단계 실험용 Drehmal 복사본**에만 실행하며 실제 사용 중인 마크 세이브나 공식 원본에는 실행하지 않는다. 동일한 활성 월드, 이미 8단계 overlay가 있는 월드, 정본 SHA가 바뀐 월드는 도구가 거부한다.

```bash
python3 tools/preview_drehmal_primordial_root_ninth.py inspect --world-copy "/path/to/seven-stage-copy" --output build/ninth-inspect.json
python3 tools/preview_drehmal_primordial_root_ninth.py apply   --world-copy "/path/to/seven-stage-copy" --output build/ninth-apply.json
python3 tools/preview_drehmal_primordial_root_ninth.py verify  --world-copy "/path/to/seven-stage-copy" --output build/ninth-verify.json
python3 tools/preview_drehmal_primordial_root_ninth.py restore --world-copy "/path/to/seven-stage-copy" --output build/ninth-restore.json
```

## 3. 이전 8단계와 새로운 9단계의 관계

**8단계와 9단계가 누적 설치된 것이 아니다.** 각각 검증된 7단계 사본에서 출발한 별도 가역 실험이다.

- **8단계:** 원작 비무기 업적 보상 링크 9개를 선택 차단하는 실험.
- **9단계:** 원작 `advancements:primordial/root` → `players:sethost` 이동 호출 링크 1개를 선택 차단하는 실험.
- 8+9단계를 합친 동일 복사본, 실사용 월드 적용, 공식 클라이언트의 문·포탈·퍼즐·진행 보존은 **미검증**. 도구는 의도적으로 8단계 overlay가 있는 사본에 9번째 실험을 덧씌우지 않도록 거부한다.

## 4. 남은 맵 분리 우선순위

1. **8+9 선별 차단 충돌·기능 범위 검증**: 별도 실험으로 존재하는 두 overlay를 안전하게 결합하고, 원작 `rewards.function` 9+1개 조정 외 다른 데이터팩 내용이 바뀌지 않는지 확인. 결합 시에도 START·Terminus·퍼즐 기능의 손실 여부를 따로 검사한다.
2. **실제 지도 시설 테스트**: Av'Sal 터미널 / Foundry 레버 / Godmachine 버튼 / Terminus의 물리 상호작용, 입·출구/문/포탈, 원래 위치 이동 및 시설에 필요한 원작 스크립트 의존성 분리. 업적을 강제 `grant`한 테스트는 시설 사용의 대체물이 아니다.
3. **그 외 원작 시스템 분리**: 남은 보상 콜백, 스케줄·점수판·기타 직접 명령 블록의 원작 스토리/전투·보상 경로를 조건별로 분류해 필요한 경로만 복사본에서 가역 차단한다. 무조건 원작 파일 전체 삭제 금지.
4. **TURNBOUND START·멀티플레이 검증**: 게임 시작점과 패배 복귀 위치를 사용자/정본 승인 기반으로 고정한 뒤 실제 클라이언트에서 시작→탐험→시설 조작→체크포인트→재접속·멀티를 실측한다.

**검증 상태**: ORIGINAL SOURCE SHA VERIFIED / CODE REVIEWED / TWO-PROTOCOL-CLIENT DEDICATED SERVER DIFFERENTIAL PASS / ONE-ENTRY OFFLINE APPLY+VERIFY+RESTORE PASS. **미검증**: 렌더링 클라이언트 플레이, 실사용 START, 실제 4인 멀티플레이, 원작 시설 및 퍼즐 기능, 8+9 누적 월드, 게임 완성도.
