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

## 2026-11-05 원본 권능 이식 1차 코드 결과

**기준 코드 커밋:** `24ed51b` (docs 추가 이전)
**대상:** 권능 탈취 / Chrono Dawn 6종 / server-authoritative J 체인 / HUD 작은 표시.
**검증:** 26.2 compileJava, compileGametestJava, GameTest, dedicated server, client render는 각각 실행 로그 확인 전까지 완료 주장 금지. 기존 기본 GameTest 3건(지배 공격·비행 충돌·돌려보내기)은 플레이에서 정상 관측된 기능과 별도로 지속 조사.

### Chrono Dawn
| 보스 | 새 구현 | 정확성/잔여 제한 |
|---|---|---|
| Time Guardian | 안전 순간이동 후 15틱 준비 시간, 착지 지점 4블록 시간왜곡 피해 + 둔화 II + 채굴 피로 | 실제 Time Blast 투사체는 아직 추가하지 않음. 후딜레이 및 효과는 26.2 원본 메서드 기반 |
| Chronos Warden | 4블록 지면 충격파 + 100틱 Stone Stance. 시선 기준 전방·측면 피해 80% 감소, 후방은 전량 받음 | 원본은 공격 불가·이동 중지; 플레이어용으로 조작을 보존하는 적응형 시전. 피해 감소는 LivingEntity hurtServer 공통 경계에서 처리 |
| Clockwork Colossus | Chrono Dawn 설치 시 LGPL 원본 GearProjectileEntity를 런타임에서 생성해 발사 (속도 1.5, 실제 회전 렌더/충돌). 부재 시 기존 단발 공격. 선형 충격파/15% 수리 유지 | 원본 소스/텍스처 번들하지 않음. 버전별 reflective adapter 동작·동료 충돌·원본 투사체 피해 수치 인게임 검수 필요 |
| Temporal Phantom | 2초 물리 공격 회피 창, 히트당 30% 회피, 성공 후 5초 재회피 제한. 기존 순간이동·분신 유지 | 원본 Phase Shift의 확률을 반영했으나 original Warp Bolt 실투사체와 별개. clone은 해당 원본 메서드에 TODO가 남아 있으므로 프로젝트 독자 구현으로 구분 |
| Entropy Keeper | 적의 발밑에 3×3, 5초 지속 부패 지대. 10틱마다 내부 적에게 둔화 II와 독 I 갱신, 바닥 보존 | 원본 부패 지대 패턴을 충실히 반영. 원본 장비 내구도 부식 및 저체력 Entropy Burst는 권능 시퀀스에 아직 통합하지 않음 |
| Time Tyrant | 원본 3초 둔화 V/순간이동/광역 기술/체력 10% 회복에 8초 투사체 피해 70% 감소 추가 | 원본은 체력이 낮을 때 1회 회복, 플레이어용은 120초 재사용마다 회복. 이 차이를 플레이어 수치 안내에 반영 |

Source: `https://github.com/ksoichiro/ChronoDawn/tree/main/common/26.2/src/main/java/com/chronodawn/entities/bosses` 
Gear projectile: `https://github.com/ksoichiro/ChronoDawn/blob/main/common/26.2/src/main/java/com/chronodawn/entities/projectiles/GearProjectileEntity.java`
Chrono Dawn 소스 LGPL-3.0; reflection adapter가 원본 설치된 게임에서 해당 모드 엔티티를 호출할 뿐, 비공개/라이선스 불명 자산을 JAR에 직접 포함하지 않음.

### J 다중 피해 충돌 완화
- `J`는 획득·활성화된 권능만 서버가 12틱 간격으로 예약하는 기존 방식 보존.
- 공통 authority `hurt`와 Warden 음파 경로에서 대상의 `invulnerableTime`이 양수라면 타격을 폐기하지 않고 UUID/차원/소유자 정보로 지연 예약.
- 후속 서버 tick이 정상 무적시간 종료 후 `hurtServer`에 다시 넘기며, 방어구/저항/보호막을 강제 우회하지 않음. 8초 제한, 최대 512건, 사망·차원 변경·대상 해제·친화 전환 시 폐기.
- Chrono Dawn 원본 projectile은 외부 모드 자체의 hit pipeline을 사용하므로 이 공통 지연 적용의 예외. 별도 인게임 검증 필요.
- 주의: 모든 시그니처의 시각 충돌/이동 간섭을 해결한 것은 아님. 전체 26종 동시 해방, 피해 합산, 멀티플레이는 NOT PLAYTESTED.

### 새 자동 테스트
`AuthoritySignatureGameTests`: (1) 26개 권능 ID 고유성/지원/설명, (2) 모든 권능을 끄면 J가 쿨타임을 소비하지 않으며 다시 켜면 서버 권한으로 120초 쿨타임을 예약.
테스트 실행을 위해 `src/gametest/resources/fabric.mod.json`의 `fabric-gametest` entrypoint 등록이 필요하며, 2026-11-05 소스에서 등록함.
Java source 컴파일 성공과 GameTest 통과를 반드시 분리하여 보고할 것.

### 아직 남은 본편 작업
1. World Bosses 11종: 원본 소스 미공개/ARR이므로 공식 Wiki/배포 설명으로 검증한 시그니처를 독립적으로 player-use 구현. 특히 Thunder Cage, Flame Prison, Fear Pulse, Shadow King 영혼탄/소환, Dragon Mother 공중 화염, Storm Griffin 지정 지점 낙뢰, Shadow Weaver 실/분신/빛 약점.
2. Chrono Dawn 잔여 3개: Time Blast, Warp Bolt, 장비 부식/Entropy Burst. source code signature에 맞춰 추가.
3. 임시 버프 기반 fallback을 실제 source-verified 시그니처로 점진 교체하되 다른 효과는 임의 제거하지 않음.
4. J 다중 권능 순서·가독성, 외부 HUD 간섭, 26종 동시 극단 입력, 다른 플레이어/아군 보호를 인게임에서 검증.
5. 핵심 권능 발동에 고유 사운드/VFX 피드백 추가. 고정 효과를 vanilla particle 1종으로 대체하는 데서 멈추지 않는다.
6. 기존 3개 GameTest 실패는 기존 기능 제거 없이 별도로 근본 수정.

## 2026-11-05 권능 탈취 2차 구현 및 검증

### 코딩 기준
- 본 작업 시작 시 remote `main`: `54eb529f90313e80134ca7254cc208bfe5947b30`.
- 이번 작업 최종 코드/테스트 HEAD: `1bcf327703f909b0c350fa7b64c489936e254701` (문서 정리 시 main은 다른 작업으로 전진 가능).
- 범위: `projects/augmentation-survival/`. 기존 `J` 발동, `Shift+J` 토글, 저장된 권능 ID 및 120초 전역 쿨다운 유지. 다른 모드 프로젝트 수정 없음.

### Chrono Dawn 실제 26.2 소스 기반
- source: `https://github.com/ksoichiro/ChronoDawn` main `cf217e99c8959b569437eca5be93f178a978a0c6`. Runtime installed version `0.12.0`과 동일 코드인지 별도로 JAR-level 확인 필요.
- `TemporalPhantomEntity.performRangedAttack`: **Warp Bolt라는 별도 엔티티가 없음**. 실제 `TimeBlastEntity`를 소환해 속도 1.5로 발사. 원본 상태효과 Slowness II 5s / Mining Fatigue I 5s.
- `ChronoDawnAuthorityProjectileBridge.shootTimeBlast` 추가: 모드가 로드돼 있으면 original registered `com.chronodawn.entities.projectiles.TimeBlastEntity` 객체를 reflection으로 생성해 Player 소유 투사체로 발사. 서버 런타임에서만 클래스 결합, 원본 코드/리소스를 이 모드 JAR에 포함하지 않음. 미설치/호환 불가 시 기존 단발 공격+디버프 대체. `releaseTimeGuardian`과 `releaseTemporalPhantom`에 사용.
- Temporal Phantom의 `summonPhantomClones` 원본 26.2 구현은 TODO이므로, 근거 없는 자체 `CombatCloneEntity` 2체를 원본 권능으로 표시/발동하던 부분은 제거. 물리 회피 30%+뒤쪽 순간이동/원본 투사체는 유지.
- Entropy Keeper: 원본 `placeTemporalRot` 기반 3×3 5초 장판 + 원본 `performEntropyBurst` 기반 **2초 예고 뒤 반경 6 피해 10/시듦 II 10초/넉백**을 같은 권능에 추가. 블록 파괴 없음. 장비 내구도 부식은 타 플레이어 장비 파괴 리스크 때문에 아직 도입하지 않음.
- 원본 source-license LGPL-3.0: 이 모드는 실행 중 원본 설치 엔티티를 호출하기만 하며 소스/모델/음원을 복제·번들하지 않음.

### World Bosses 2.0.2 구현: 공식 공개 능력명 기준 독립 해석
- source-of-truth: CurseForge 1.3.0 공개 changelog (Thunder Cage, Flame Prison, Primordial Chains, Cataclysm Nova) 및 pinned 2.0.2 변경기록.
- 실제 World Bosses 내부 Java 메서드/정확한 피해량과 시간은 **확인할 수 없었음**. 아래 시간/피해/사거리는 우리 모드 플레이어용 밸런스이며 원본과 동일한 코드라는 주장을 하지 않음.
- `WorldBossAuthorityPatternService`: 서버에서만 약 5초간 지속하는 `THUNDER_CAGE`, `FLAME_PRISON` 및 경고 후 1.3초 뒤 폭발하는 `CATACLYSM_NOVA`의 상태·판정·연출을 소유자 UUID/차원별 관리. 전용 boss service tick을 중복 등록하지 않고 `AuthorityTheftService.tickPending` 내부에서 호출. 종료/로그아웃/사망/차원 변경 시 대기 상태 정리. Max 48 active global / 4 per caster, 48 targets per pulse 제한.
- Aureon: 초기 단일 번개 타격 + 대상 중심 반경 4의 5초 'Thunder Cage'. 1초 경고 후 14틱 간격으로 피해 4 + Slowness III/Glowing. 전용 전기 실린더 텔레그래프·낙뢰 사운드.
- Varkhul: 중심 대상에게 Infernal Chains(Slowness V/Weakness II), 5초 Flame Prison 반경 3.7. 18틱마다 연소/지속 피해 3/둔화 III, 경계에 접근한 적은 소폭 안쪽으로 끌림. 지형의 블록을 교체하거나 발화시키지 않음.
- Astrael: Primordial Chains로 반경 9 적에게 Slowness IV/Weakness II, 26틱 진행형 링 경고 뒤 고정 앵커에서 Cataclysm Nova 피해 11/넉백. 저항 II·신속 II·화염 저항 버프 유지.
- 화면에 보이는 구속 영역과 실제 판정은 반경 기준으로 연결. 현재 particle-only VFX이므로 실제 모델/텍스처·전용 애니메이션/사운드 퀄리티는 향후 playtest 후 개선 필요.
- 전체 11종 World Bosses 중 위 3종만 이번에 독자적인 지속형 원본 명칭 패턴으로 대체. 나머지 8종은 기존 범용 효과 구현 상태로 남음.

### 자동 검증 및 테스트
- Commit `1bcf327703f909b0c350fa7b64c489936e254701`, GitHub Actions run `41947456201`.
- `compileJava`: **PASS**.
- `compileGametestJava`: **PASS**.
- `runGameTest`: **157 / 160 PASS, 3 FAIL**. 새 2개 `AuthoritySignatureGameTests`: Aureon (적 피해·중립 동물 비피해), Astrael (지연 공격 결과) 모두 PASS.
- 기존 실패 3건 그대로: `domination_behavior_game_tests_domination_converts_enemy_into_persistent_combat_ally`, `mythic_live_fix_batch_hgame_tests_powered_flight_body_collision_also_damages_neutral_mobs`, `return_to_sender_game_tests_real_shield_durability_blocks_emit_cone_at_level_three_threshold`. 사용자 수동 플레이에서는 비행·지배가 동작함을 관찰했으므로 코드 고장으로 단정하지 않음.
- `BUILD VERIFIED`: NO (GameTest 실패로 전체 workflow 종료). `JAR PRODUCED`: NOT VERIFIED. `PLAYTESTED`: NO. `MULTIPLAYER TESTED`: NO. World Bosses 패턴 타격·HUD 스크린샷/Chrono original projectile 동작도 client playtest 전.

### 다음 집중 범위
1. World Bosses: Morvath Fear Pulse/Soul Drain, Sylvaron root/spore, Nekhraz sandstorm/burrow, Elyndra rift/tether, Worldbreaker Realm 4 boss의 공개 기술을 각각 **범위와 타이밍을 시각적으로 보여주는 방식**으로 리팩터링. ARR 모드의 미공개 코드 복제 금지.
2. Chrono Dawn Time Blast/gear projectile의 실제 model & collision, 동료·중립 대상 안전, 26.2 pinned version runtime check.
3. 권능 다수 누적 시 동시 타격·넉백·teleport 충돌과 512건 히트 큐 캡/8초 만료 정책, 소유자별 fairness를 profiling 및 플레이로 확인.
4. 기존 3개 GameTest는 좁은 재현 검증으로 실제 버그와 테스트 환경 차이를 분리하고, 실패를 무시해 artifact 생성하도록 우회하지 말 것.
