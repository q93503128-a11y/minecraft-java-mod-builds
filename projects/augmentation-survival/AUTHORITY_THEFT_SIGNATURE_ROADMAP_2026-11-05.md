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

## 실제 원본 패턴 추가 대조 (2026-11-05, source-backed)

### 시각 HUD 위치 및 노출 우선순위
- 현 `CombatHudClient.extractSkills`: 일반 스킬 모드에서 스킬바 `(sw/2 - 89, sh - 67)`, J 카운트 `(sw/2 + 97, sh - 67)`; 두 UI 경계 사이 약 8 GUI 픽셀. 스킬 모드 해제 시 J는 `(sw/2+97, sh-49)`. 기존 스킬 슬롯을 직접 덮지는 않으나 게임 월드 일부를 가리고, 외부 HUD와 충돌할 가능성은 별도 시각 검수 필요.
- UI 정제 목표: 상시 큰 텍스트보다 쿨타임 중 경과 초, 준비 시 작은 J 상태 표시. 낮은 GUI scale / 좁은 화면 / 다른 HUD 공존 확인.

### Chrono Dawn 26.2 원본 보스 코드와 대조
외부 원본 코드: `https://github.com/ksoichiro/ChronoDawn/tree/main/common/26.2/src/main/java/com/chronodawn/entities/bosses` (조회 당시 main `cf217e99c8959b569437eca5be93f178a978a0c6`). 체크포인트. 정확히 현재 모드팩에 pinned된 빌드와 소스가 같은지도 검증해야 함.

| 보스 | 원본 26.2 구현 확인 | 현재 권능 재현 부족/필수 수정 |
| --- | --- | --- |
| Time Guardian | 2페이즈 안전 순간이동, 15틱 후딜, 4초 계열 시간 왜곡 광역 피해+Slowness II+Mining Fatigue, 원거리 Time Blast | 단순 순간이동/둔화 묶음이 아닌 실제 경고·위치 안전 검사·타격 시점·시각화를 재현 |
| Chronos Warden | 지면 강타, 2페이즈 5초 Stone Stance, 전후방에 따른 피해 감소 | 기존 5초 Resistance IV + 충격파는 원본의 방향 방어/자세를 보존하지 않음. 차등 방어와 짧은 준비 모션 필요 |
| Clockwork Colossus | Gear Shot 실제 투사체(8초 주기), 과충전, 40% HP에서 1회 15% 수리, 선형 Ground Slam | 현 신속/회복/선형 공격 패키지에서 핵심 Gear Shot 누락. 플레이어 소유 투사체 판정·모델/텍스처를 먼저 검토 |
| Temporal Phantom | 30% 물리 회피(2초), Warp Bolt / Time Blast, 후방 Blink Strike; 소환 분신은 메서드 내부 TODO 표기 존재 | 현재 자체 제작한 분신을 원본에서 확정된 기술로 오인하지 말 것. 회피·탄환·안전 순간이동의 실제 동작 우선 |
| Entropy Keeper | 근접 장비 부식(내구도), 3×3 부패 지대(Slowness II/Poison I), 엔트로피 폭발(6블록·Wither II) | 현 즉시 광역 독/시듦과 달리 **지속 영역** 및 부식 표현이 핵심. 타 플레이어 장비 손상은 PvP/권한 확인 선행 |
| Time Tyrant | 원본 Time Stop은 실제 시간 정지 필드가 아니라 Slowness V 3초; 순간이동+가속, AoE, 저체력 10% 회복, 투사체 70% 피해 감소 | 기존 '둔화'는 원본과 일부 일치. 이름만 보고 실제 필드 정지를 강제하지 말 것. 회복·시간 둔화 타이밍/연출 일치가 우선 |

### World Bosses 기술명 근거 및 라이선스
- 공식 CurseForge/변경기록: `https://www.curseforge.com/minecraft/mc-mods/world-bosses/files/8288335` — 1.3.0 기술 강화: Astrael `Primordial Chains`, `Cataclysm Nova`; Varkhul `Infernal Chains`, `Flame Prison`; Aureon `Thunder Cage`; Morvath `Fear Pulse`, `Soul Drain`; Sylvaron nature/root/spore; Nekhraz sandstorm/burrow; Elyndra void/rift/tether.
- 2.0.2 배포 노트: `https://www.curseforge.com/minecraft/mc-mods/world-bosses/files/9258424`. 원본 소스 공개 저장소는 검증하지 못했고 배포 라이선스는 All Rights Reserved. 공개 설명을 참고하여 독립 구현하고 JAR 디컴파일 결과를 허가 없이 복제하지 않는다.
- 공식 위키만 담긴 `Moekki/World-Bosses` 저장소는 현재 소스코드 저장소로 확인되지 않음. 보스별 실제 판정/연출이 불명확하면 `source verified`로 표시하지 말 것.

### 게임성 개선 필수 조건
1. 24/26개 능력을 단순 즉시 피해/파티클/버프 묶음으로 두지 말 것. 실제 보스별 시그니처 식별→서버 공격 경계→전용 모델/VFX/사운드→타격 감각 순서로 개편.
2. 현재 `PACKAGE_SPACING_TICKS=12`는 같은 틱 피해 중첩을 줄이는 완화책일 뿐이며, 획득량이 많을수록 `J` 해방이 지나치게 길어질 수 있음. 스킬별 액션 타입과 hit-window를 재설계하면서 텔레포트·넉백·피해 중첩을 실측.
3. 체력 기반 대체 버프는 원본 시그니처가 검증되지 않을 때만, 해당 UI에 대체형이라고 정확히 표시. 보스 분류와 코어 확률/보상을 혼합하지 않음.
4. `J` 개별 토글·서버 쿨다운·기존 획득 목록은 보존. 실제 테스트 전 BUILD/PLAYTESTED로 표시하지 말 것.
5. 플레이어 피드백: 비행 피해와 지배는 수동 플레이에서 작동 관측. 실패한 GameTest는 테스트 조건/전투 경계 차이를 독립 조사하고 기능 실패라고 단정하지 않음. 방패 반사는 사용자가 추가 확인 예정.
