# 권능 탈취·지배 실구현 점검 및 남은 원본 패턴 이식 — 2026-11-05

## 기준
Minecraft 26.2 / Fabric, `projects/augmentation-survival/`, 확인 시작 main `32e1691`.
2026-11-05 추가 코드: 바닐라 대표 권능, 획득 권능별 영구 On/Off, 서버 기준 120초 쿨타임 HUD, Shift+J, 지배 원격 로스터 관리.
현재 GameTest 156개 중 3개 실패(지배 동료 피해, 비행 충돌, 방패 반사). 전체 빌드/JAR 및 실제 인게임 UX 검증 완료가 아님.

## 대상 선정
- 권능 해방 목록은 현재 whitelist 24에서 위더/엔더드래곤/워든/엘더가디언/Chrono Dawn 6/World Bosses 11/MCEndgame 5로 확장.
- 엘더 가디언은 원래 core reward 및 일반 보스 분류에서 특수 비보스로 처리하므로 그 분류를 변경하지 않고 권능 탈취에만 포함.
- `EntityCompatibilityProfileService`가 boss=true로 인식하는 그 밖의 엔티티는 `boss_entity/<registry-id>`로 획득. 원본 패턴을 알 수 없는 대상은 실제 처치 시 체력/공격력 스냅샷으로 6초 전투 강화만 제공하며 UI에 대체형임을 설명. 존재하지 않는 시그니처를 꾸며내지 않음.
- 공식 모드팩은 MCEndgame을 제거했음(2026-11-04 commit `7e194d2`). 해당 5종 adapter는 외부 모드 설치 시에만 사용 가능.
- 동일 보스의 재처치 시 권능 중복 없음. 기존 saved IDs 유지.

## 현재 구현 시그니처
- 위더: 체력 절반 이하의 투사체 무시에서 착안. 플레이어에게 8초간 서버 피해 경계 기반 투사체 무효화 + SOUL 연출. 시듦/피해 묶음은 제거.
- 워든: 원본 음파 패턴을 모델로 34틱 충전, 음파 DamageType, 벽 무시 목표 지정, 음파 파티클·사운드·넉백. 15블록 범위에서 대상 추적. 실제 인게임 판정 검증 필요.
- 엘더 가디언: 원본 Mining Fatigue III 지역 저주. 적대몹에는 행동이 실제로 느려지도록 Slowness/Weakness를 병행한 8초 축약 CC. 바닐라 플레이어 대상 5분 저주를 그대로 복사한 것은 아님.
- 엔더드래곤 및 외부 보스: 기존 코드 유지. 일부는 원본 시그니처가 아니라 범용 수치 피해/상태이상 조합이므로 **본격 이식 완료라고 부를 수 없음.**

## 해방 조작 및 세이브
- J: 켜진 권능들만 `stolenAuthorityBossIds`의 획득 순서대로 12틱 간격으로 큐잉. 소유자 사망·차원 이동 시 남은 큐 취소. 같은 틱 피해 겹침을 줄이는 첫 조치, 실제 시뮬레이션 검증 전.
- Shift+J: 서버 저장 목록 표시 및 각 행 우측 클릭으로 J 포함/제외. 기본 켬, 사용 안 함 상태는 `AugmentationPlayerState.persistentInts`의 `authority_disabled/<id>` 1에 저장. 기존 스키마 유지.
- HUD: J 쿨타임을 서버 snapshot(접속/탈취/해방/토글/목록 조회)에서 받아 클라이언트에 실시간 초 단위 환산 표시. 유일한 서버 권한은 `AuthorityTheftService.tryRelease`.
- 명중/상태반응/속도/분신 시퀀스는 이식된 개별 패턴에 따라 달라진다.

## 지배 관리
- 기존 상태는 동료 조준 후 지배 능력 재사용해 해제 가능했으나 Shift+스킬 키와 로스터 화면은 없었음.
- 새 구조: skill mode 활성화 + Shift + 해당 지배 슬롯(1~0) => 서버 관리 요청. UI에서 owner UUID 스냅샷을 조회하고 해제.
- 신규 지배/로드/부활 때 save-wide 로스터 기록. 기존 save에는 `companions_by_owner` optional 필드가 없으므로 로드된 옛 지배 몹부터 지연 인덱싱. 기존 `occupied_by_owner` 점유량 유지.
- 로드된 개체는 즉시 해제, 언로드 개체는 서버에 pending release를 남겨 이후 로드 시 owner attachment를 정리. 미로드 기존 몹은 old save의 metadata 부재로 목록에 표시되지 않을 수 있음. 이 경우 목록/점유 수 mismatch가 있을 수 있으므로 legacy migration playtest 필요.
- 실제 서버 동작·멀티플레이 검증 전, full verified가 아님.

## 반드시 해야 할 검증
1. Warden 비가시 대상 공격·음파 피해 원본 태그·충전 시 딜레이·보스 친화 대상 예외.
2. Wither shield: 화살/삼지창 무효, 근접/폭발 피해 허용, 다른 방어 스킬과 충돌 없음.
3. Elder Guardian: Mining Fatigue 이외의 Slowness/Weakness가 mob AI에 체감 가능한지.
4. J On/Off 영구 저장 후 재접속, 권능 0개/1개/전부, 쿨타임 도중 이탈, 월드 재시작, 서버/클라이언트 불일치 거부.
5. 지배 몹 3/5/10, 사망/소생, 원거리 청크 언로드→해제 예약→리로드, 재접속, 다른 플레이어로 위조 해제 시도, 기존 데이터 마이그레이션.
6. 기존 실패 GameTest 3개를 각각 원인 분석 후 근본 수정. 컴파일 통과만으로 게임 완성 판정 금지.
7. Chrono Dawn 6종 원본 소스(https://github.com/ksoichiro/ChronoDawn)와 설계 비교 및 signature-by-signature 이식.
8. World Bosses 11종은 공개 설명·허가 범위 확인 후 별도 이식. 원본 저작물 무단 복사 금지. MCEndgame은 현재 공식 모드팩 제외 상태라 conditional adapter만 유지.
9. 1~7번 전체 UI는 별도 `UI_REDESIGN_DIRECTION_2026-11-05.md` 기준에 따라 Minecraft 실제 화면으로 검수. 이번 권능/지배 조회 화면 추가가 전체 GUI 리뉴얼 완료를 의미하지 않음.
