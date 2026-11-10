# TURNBOUND — Drehmal 8+9 통합 오프라인 가역 분리 실험

> 2026-11-10 · TURNBOUND (TURNBOUND: RE 아님) · branch: turnbound-content-rebuild · Minecraft 1.20.1 Fabric · 공식 Drehmal: APOTHEOSIS 2.2.2f.

## 목적과 적용 금지선

7단계 SHA 검증 사본에서만 8단계 비무기 업적 보상 함수 연결 9개와 9단계 자동 복귀 트리거 연결 1개를 **서로 독립적으로** 시험한다. 둘은 기존에는 서로 다른 7단계 사본에서만 적용·복구됐다. 새 결합 도구는 두 순서의 결과가 같은지와 원상복구를 검증하기 위한 **오프라인 실험**이다. 원본 지도, 실제 사용자 월드, 원작 함수 소스 및 게임 자산을 재배포하지 않는다.

**중요:** 8단계 9건은 실사용 삭제 승인 **0건**이며, 특히 kill_with_bowblade는 bowbladEffect 점수판 의존성이 직접 확인됐다. 이 9건을 게임에 적용해도 된다는 뜻이 아니다. 9단계 primordial/root의 players:sethost 연결 1개만 별도 2인 실서버 인과 A/B 검증 완료. 기존 문서의 제8·9 개별 실험 결과와 실사용 범위를 절대 혼동하지 않는다.

## 보존·실패 차단 계약

1. 공식 원본 검증 후 1~7단계를 적용한 **비활성·폐기 가능 사본**만 허용. 활성 마커, 이전 8/9 receipt 또는 백업, 통합 receipt/백업, 이전 SHA와 다른 원본 ZIP은 거부한다.
2. 8단계는 고정 SHA의 181 callback 감사에서 선택된 원래 9개 JSON의 rewards.function 연결만, 9단계는 primordial/root의 players:sethost 연결만 차단한다. 업적의 다른 필드·기준·보상, 모든 원본 mcfunction 및 다른 ZIP member의 원시 내용 유지.
3. 8 → 9와 9 → 8 적용 후 **완전 동일한 최종 ZIP 바이트**를 요구한다. 원본 ZIP의 member 목록·내용을 전수 비교해 정확히 10개만 바뀌었는지 확인한다.
4. 수정 전 7단계 팩을 별도 백업하고 복구 시 전체 SHA 및 7단계 검증을 재검사한다. 변조된 백업·receipt·변환 내용은 자동 복구를 시도하기보다 거부한다.
5. 적용·복구를 실제 START 승인 또는 원작 시설 물리 조작·퍼즐 플레이 검증으로 간주하지 않는다. 원작 기능이 아직 남아 있으므로 MAP SEPARATION COMPLETE = NO.

## 실험용 명령

SHA 검증한 7단계 임시 사본에만 사용한다. source review는 기존 SHA 고정 callback-schedule-risk-review.json이다.

    python3 tools/preview_drehmal_eight_nine_combined.py inspect --world-copy "/path/to/seven-stage-copy" --source-review build/callback-review/callback-schedule-risk-review.json --output build/combined-inspect.json
    python3 tools/preview_drehmal_eight_nine_combined.py apply --world-copy "/path/to/seven-stage-copy" --source-review build/callback-review/callback-schedule-risk-review.json --output build/combined-applied.json
    python3 tools/preview_drehmal_eight_nine_combined.py verify --world-copy "/path/to/seven-stage-copy" --output build/combined-verified.json
    python3 tools/preview_drehmal_eight_nine_combined.py restore --world-copy "/path/to/seven-stage-copy" --output build/combined-restored.json

회귀 테스트: tools/test_preview_drehmal_eight_nine_combined.py.
CI: .github/workflows/turnbound-drehmal-eight-nine-combined.yml.

## 후속 순서

1. 이 오프라인 통합 실험의 회귀/실제 원본 사본 해시·복구 검증.
2. 보존할 Av'Sal, Foundry, Godmachine, Terminus, 문·포탈·퍼즐의 **실제 물리 상호작용**을 실서버/가능한 실제 그래픽 클라이언트에서 조사. 단순 advancement grant는 불충분.
3. 원작 게임 상태·서사·전투·보상으로 새어 나가는 경로만 근거별로 가역 분리. 8단계 실험의 9개를 성급하게 승격하지 않는다.
4. 게임 START/체크포인트 및 실제 4인 멀티/기본 플레이 흐름 검증 후에만 실사용 맵 승인.

**아직 미검증:** 물리 시설 상호작용, 렌더링 클라이언트 플레이, 4인 멀티, 게임 START, 8단계 실사용 안전성, 전체 맵 분리.
