# TURNBOUND — Foundry 원작 tick 진입점 차단의 원인 규명

기준: 2026-11-11 · branch `turnbound-content-rebuild` · 원본 Drehmal: APOTHEOSIS v2.2.2f · Minecraft 1.20.1/Fabric.
판정: **Foundry 자연 개방 실패의 직접적인 진입점 단절 원인 확인. 맵 분리 완료 아님.**

## 확인된 인과 관계

1. **원본 정적 경로:** 원작 `minecraft:tick`은 `entities:main`을 포함하며, `entities:main → players:main → players:overworld → players:overworld_p`에서 Foundry `foundry:levers/door`에 도달하는 리터럴 함수 호출 경로가 존재한다. [정적 원본 함수 5,487개 검사 #42946496758](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42946496758), **PASS**. 이는 *원본 ZIP에 경로가 존재한다*는 사실이지 7+9 사본의 틱 실행 증명은 아니다.
2. **사본 실제 구성:** `tools/prepare_drehmal_isolated_copy.py`는 `roots.apply(world)`를 적용하고 `roots.TAGS`의 모든 `values`가 비었는지 `verify(world)`에서 확인한다. 그 하위 `tools/drehmal_auto_root_preview.py`가 원작 `minecraft:load`의 `core:init`, `minecraft:tick`의 `core:main`과 **`entities:main`**을 제거한다. 7+9 사본의 원작 tick 루트가 비어 있으므로, 위의 자동 함수 경로가 실행되지 않는다. 원작 파일을 바꾼 것이 아니라 **폐기 사본에 적용된 검증 가능한 분리층**이다.
3. **문 자체 동작:** 원작 `foundry:levers/door`를 7+9 폐기 사본에서 명시적으로 실행한 선행 진단은 복제 대상의 59개 서로 다른 블록을 원본 소스와 일치시켰다. [자연/명시적 A/B #42935729498](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42935729498). 따라서 문 복제 명령 자체가 고장 난 것으로 볼 근거는 없다.
4. **자연 이벤트 성립:** 세 원본 레버 실제 우클릭으로 원작 scoreboard는 1→2→3, 세 레버가 모두 ON이다. 원본 문 함수의 점수·거리·predicate 조건 및 `players:main`에서 내려오는 상위 조건 3개는 플레이어가 문 근처일 때 모두 참이다. [업적/점수 #42940827429](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42940827429), [상위 조건 #42947160749](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42947160749), **PASS**.
5. **플레이어 디스패치도 정상:** 원본 `entities:main`의 `@a` 디스패치에는 필터가 없고, 7+9 실서버에서 플레이어 선택 조건은 콘솔·플레이어·동일인 검사 모두 참이다. [컨텍스트 수정 후 CI #42948142737](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42948142737), **PASS**. 그럼에도 자연 문 복제는 0블록 변경, 원본 소스와 목표 사이 차이 59 유지.
6. **결론:** 원작 tick 경로를 유지한 상태에서 문이 안 열리는 증상이 아니다. **사본에서 원작 자동 tick 진입점을 의도적으로 제거했으므로, 원래 tick 의존 방식으로는 문이 열릴 수 없다.** 단, 명령 블록·별도 스케줄·다른 호출 경로가 원작 함수를 재실행할 가능성까지 이 검사만으로 배제한 것은 아니다.

## 안전한 다음 단계

- 원작 `minecraft:tick`의 `core:main` 또는 `entities:main`을 **통째로 복구하지 않는다.** 원작 이야기·전투·보상·월드 변경까지 연결될 수 있기 때문이다.
- **후보:** TURNBOUND 소유의 별도 Foundry 시설 어댑터가 기존 3개 물리 레버의 실제 블록 상태, 서버 권한, 플레이어의 시설 진입 조건 및 문 블록 변경을 관리한다. 원본 함수나 명령을 복사·이식하지 않고 별도로 설계해야 한다. 실제 구현 방식·원작 동작의 재사용 범위는 `DREHMAL_DATA_PACK_SEPARATION.md`에 따른 승인 경계를 따른다.
- 구현 전에 문이 열린 뒤 상태 전이, 재접속·멀티플레이, 원작 `arena_start` / `wave_logic` / `wave_1…6`의 미재진입, 나머지 장치와의 충돌을 정리한다. 물리 건축물·퍼즐을 무작정 삭제·변경하지 않는다.
- 기존 8단계 9개 보상 분리 실험은 **미승인 그대로**. 공식 원본 지도 및 사용자 실사용 맵에 이 조사나 실험을 적용하지 않는다.

## 검증 분류

- **CODE REVIEWED:** 원본 진입 경로와 현 사본 루트 차단의 원인 대조
- **TESTED:** 7+9 폐기용 Fabric 서버에서 실제 레버 3개·조건·셀렉터 참/거짓 및 블록 차이 측정
- **NOT TESTED:** 독립 Foundry 어댑터, 실제 문 통행, 보스/6웨이브 미재진입, 렌더링 클라이언트, 4인 멀티
- **MAP SEPARATION COMPLETE:** **NO**
