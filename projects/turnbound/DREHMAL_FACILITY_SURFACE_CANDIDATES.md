# TURNBOUND — Drehmal 시설 물리 상호작용 좌표 후보 조사

> **2026-11-10 공식 7+9 사본 실서버 물리 조작 검증 PASS:** [실제 Foundry 레버 우클릭 CI #42824376001](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42824376001)에서 원작 데이터팩 ON·명령 블록 ON인 폐기용 사본에 1인 프로토콜 클라이언트를 접속해 레버 **3개 모두 각각 첫 클릭에서 powered 상태 전환·두 번째 클릭에서 원복**을 관측했다. 원본 위치 확인은 [시설 구역 스캔 #42823505301](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42823505301). **문 실제 개방 / 원작 Foundry 6웨이브 전투 / 클라이언트 시각적 작동 / 4인 멀티는 미검증**. 8단계 미승인 차단 9건은 이 실험에 포함하지 않았다.


2026-11-10, branch turnbound-content-rebuild, Minecraft 1.20.1 Fabric / Drehmal 2.2.2f.

## 범위

통합 8+9는 오프라인 정확 복원까지 검증됐지만 **8단계 보상 연결 9건의 플레이용 비활성화는 승인되지 않았다**. 다음 실사용 후보 검증은 먼저 **7+9**를 유지하여 불필요한 8단계 위험을 배제한다.

원작 위키의 위치 정보(신뢰성은 2.2.2f 실제 맵에서 재확인 필요)를 참고하여 찾을 후보:
- Foundry 레버 1: -3130,148,1490; 레버 2: -3035,121,1186; 레버 3: -3394,145,1082. 참고: https://wiki.drehmal.cyou/World/Points_of_Interest/Foundry/
- Av'Sal Repository 홀로도어 부근: -251,75,1646. 참고: https://wiki.drehmal.cyou/Story_and_Features/Holotexts/Early-Game_Holotexts/Av%27Sal/avsal_door/
- God Machine 홀로텍스트 부근: 3023,83,-569. 참고: https://wiki.drehmal.cyou/Story_and_Features/Holotexts/Post-Yav/Azimuth_Labs/gm_apotheosis/
- Capital Valley Terminus Tower: 559,129,1177. 참고: https://wiki.drehmal.cyou/Story_and_Features/Terminus_Network/

읽기 전용 도구 tools/preview_drehmal_facility_surface_inventory.py는 공식 맵 원본과 검증된 7+9 비활성 사본을 비교하고, 고정 반경에서 바닐라 lever/button/door/redstone 후보 좌표와 개수만 출력한다. 함수 본문, 원작 명령·NBT·지도·리소스팩은 출력하지 않는다.

**위키 좌표는 실제 클릭할 블록이 아니며**, 가까운 레버나 버튼을 발견해도 물리 조작·문 열림·퍼즐 정상 작동을 검증한 것은 아니다. 후보를 찾은 후에만 실제 플레이어 사용 패킷, 주변 구조 상태의 전후 비교, 서버의 원작 보상 재진입 여부를 A/B 테스트한다. 원본 사용자 월드 변경·원작 코드 이식·미승인 게임 시작점 지정 금지.

검증: .github/workflows/turnbound-drehmal-facility-surface-inventory.yml.
완료 게이트: 시설별 실제 클릭·레드스톤/문/포탈 결과 + 원작 스토리/보상 재진입 여부 확인. 현재 MAP SEPARATION COMPLETE = NO.
