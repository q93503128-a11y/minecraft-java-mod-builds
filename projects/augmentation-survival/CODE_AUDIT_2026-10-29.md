# Augmentation Survival 코드 감사 기록 — 2026-10-29

## 범위와 완료 상태

이번 작업은 `origin/main`의 `47f1319253ef4512c333c33762e820b307eff638`에서 시작했다. 저장소 상태 확인 시 branch는 `main`이었고, 같은 시점의 `HEAD`와 `origin/main`은 일치했다.

프로젝트 내부 코드/설정 inventory는 447개 파일이다. 이 중 Java 364개, JSON 75개, Gradle 2개, properties 2개, 재식재 function 1개, Python 도구 1개, Gradle wrapper 2개다. 상세 경로별 판정은 [`CODE_AUDIT_2026-10-29_INVENTORY.csv`](CODE_AUDIT_2026-10-29_INVENTORY.csv)에 기록했다.

**전체 전수 감사는 완료되지 않았다.** inventory의 현재 상태는 447개 중 249개 판정(15개 결함 수정, 4개 추가 검증 필요), 198개 미검토다. 따라서 `FULL SOURCE AUDIT COMPLETE: NO`다. Mixin 137개는 모두 파일 전체 검토 상태지만 client, augment 서비스, GameTest, 리소스 등에 미검토 파일이 남아 있다. 미확인 파일을 검토 완료로 계산하지 않았다.

## 확인한 결함과 수정

### P1 — 투사체의 증강 비행 상태가 저장되지 않음

활공/관통/유도/연쇄 동작은 mixin 인스턴스 필드에 원본 소유자, vanilla pierce 기준값, 증강 레벨, 반사 출처, 튕김 횟수, 연쇄 사용 여부, 유도 대상과 현재 구간의 적중 UUID를 보관했지만 엔티티 저장 데이터에 쓰지 않았다. 청크 unload 또는 서버 재시작 뒤에는 이미 합산된 pierce 수치를 원본 수치로 오인해 증강 pierce가 중복 적용될 수 있고, 물리 투사체 수명에 묶인 연쇄/구간 제한도 초기화될 수 있었다.

`AbstractArrow` save/read 경계에서 해당 상태를 함께 저장/복구하도록 했다. 동적 엔티티 ID 대신 재접속·재로드에 안정적인 UUID로 구간 적중과 유도 대상을 기록한다. 입력값은 레벨, UUID 개수, 배율 범위로 제한했다. 저장 후 복원 및 pierce 중복 적용 방지 GameTest를 추가했다.

### P2 — 보스 호환성 제어값이 실제 당기기/교환 경로에서 무시됨

프로필 parser가 `allow_pull`, `pull_multiplier`, `allow_position_swap`을 읽었지만 활성 능력 실행 경로가 이를 사용하지 않았다. `ActivePullEntityMixin`이 `executePull`을 HEAD에서 대체하므로 기본 서비스 수정만으로는 해결되지 않는다.

서비스와 실제 mixin 경로에 pull 허용/배율 정책을 연결하고, swap 대상에도 프로필 허용 여부를 적용했다. 기본값은 기존 0.30 보스 배율과 일치하며, 프로필 재정의가 이제 반영된다. 가짜 차단 프로필과 배율의 fallback을 확인하는 GameTest를 추가했다.

### P2 — 지배 동료 명령 대상 UUID가 차원 경계 없이 사용됨

소유자가 공격한 대상 UUID를 소유자별로 저장했지만 차원 정보가 없었다. 다른 차원에 남아 있는 동료도 해당 UUID를 자신의 차원에서 조회했고, 우연히 일치하는 상태를 명령 대상으로 취급할 수 있었다.

명령 대상에 dimension key를 함께 저장하고, 동료의 공격 허용과 대상 조회 때 차원을 함께 확인한다. 서로 다른 차원의 중립 대상과 동료를 이용한 GameTest를 추가했다.

### P2 — 만료 offer가 map에서 제거되지 않음

offer 만료는 API 호출 때만 확인되어 만료 뒤 재방문하지 않은 플레이어의 항목이 서버 세션 동안 남았다. 동일 JVM에서 integrated server를 바꿀 때도 이전 pending 상태가 남을 여지가 있었다.

서버 tick마다 만료 항목을 정리하고, 서버 종료 때 map을 비운다. 플레이어별 만료 후 새 token으로 offer를 다시 만들 수 있는 GameTest를 추가했다.

### P2 — 재식재 fallback 소유자 컨텍스트가 종료되지 않을 수 있음

재식재 예약 직전에 위치별 owner map을 기록하지만, 예약 작업이 취소되거나 해당 위치가 더 이상 비어 있지 않으면 owner inventory 소비 경계에 도달하지 않아 기록이 남을 수 있었다. 위치가 재사용되면 오래된 소유자의 씨앗을 소비할 가능성도 있었다.

서버 tick 기반 10초 만료와 주기 sweep을 추가했다. 만료된 컨텍스트가 나중에 같은 위치의 씨앗을 소비하지 않는 GameTest를 추가했다.

### P2 — client card presentation mixin 미등록

`AugmentDeviceCardPresentationMixin` 소스와 대상 method는 존재했지만 mixin client list에 없어 packaged client에서 실행되지 않았다. client mixin 설정에 등록하고 설정 회귀 GameTest를 추가했다. 실제 화면 검수는 빌드/클라이언트 실행 차단으로 수행하지 못했다.

### P2 — powered-flight avatar pose가 다른 no-gravity 상태와 혼동됨

렌더 mixin은 player의 `isNoGravity()`만으로 powered-flight pose를 선택했다. 다른 효과나 모드가 no-gravity를 부여해도 비행 증강이 없는 로컬 플레이어가 같은 pose를 받을 수 있었다. 로컬 아바타에 한해 서버 동기화된 flight augmentation level 조건을 추가했다. 화면 검수와 원격 플레이어의 pose 동기화 확인은 수행하지 못했다.

## 의심 항목 및 근거

- `PlayerReturnToSenderMixin`은 mixin config에 등록되지 않았고 `Player.tryDeflect`에 주입한다. 26.2 서버 클래스 검사에서 해당 method는 없고 `deflectProjectile(Entity)`가 확인됐다. 현재 실행 대상이 아니므로 등록하지 않았다. 과거 구현 잔재인지 제거할지 판단하려면 별도 이력 확인이 필요하다.
- `EnchantmentResonanceMixin`의 owner-context ThreadLocal은 HEAD/RETURN 경계에 의존한다. 일반 경로의 push/pop은 보이나 예외 경로에서 정리되는지 이번 검증에서는 확인하지 못해 런타임 검증 필요로 남겼다.
- 저장/네트워크/정적 UUID map 전수 검토는 완료되지 않았다. 이번에는 투사체 상태, offer, domination, 재식재 fallback 등 선택 경로만 수정했다.
- entity compatibility profile은 코드와 정책 resource 일부를 검토했지만 외부 선택 모드들의 실제 런타임 classloading/게임 동작은 확인하지 못했다.
- 프로젝트 기준의 고품질 UI 확인에는 실제 Minecraft 화면 검수가 필요하다. 현재 client boot/screenshot은 실행하지 못했다.
- `LifeLink` 만료의 overworld gameTime 비교는 26.1 공식 변경 노트의 `time query gametime` 설명을 대조했다. `/time` world clock과 총 게임 tick은 별도이므로 이를 결함으로 판정하거나 수정하지 않았다. [Minecraft Java Edition 26.1 — Changes to time](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1)

## 검증 상태

- Java 확인: 임시 설치한 Temurin 25.0.5+8
- Gradle 확인: wrapper Gradle 9.5.1
- `git diff --check`: PASS
- `clean build`: BLOCKED. Java 25 환경에서 Gradle 설정 단계가 Fabric Loom `1.17.20` POM을 `maven.fabricmc.net`에서 받지 못했고 `Unsupported or unrecognized SSL message`로 끝났다. 컴파일 단계까지 도달하지 못했다.
- 이번 변경 GameTest: NOT RUN — 로컬 빌드 의존성 해석 차단. 새 GameTest 중 여러 개는 실제 damage/save/menu 동작을 거치지만, active kill 이벤트는 invoker를 직접 호출하고 compatibility policy는 helper만 검증한다. 이에 해당 인벤토리 행은 runtime 검증 필요로 남겼다.
- Historical authoritative Build Augmentation Survival Pack #431 (`40795138452`, gameplay HEAD `3ecf999d36a3c067a77d6a3ff87aa5fe7657577e`): 74 GameTests PASS, build/localization audit/dedicated-server smoke/client boot smoke/verified Modrinth pack PASS. 이는 이번 변경의 검증으로 계산하지 않는다.
- Datagen: NOT RUN — 프로젝트에 datagen task 미구성
- 전용 서버 smoke: NOT RUN
- client boot/screen review: NOT RUN
- 최종 JAR 검사/SHA-256/artifact: NOT RUN — 빌드가 실행되지 않음
- 과거 workflow #431의 74 GameTests 통과 기록은 이번 변경 검증으로 계산하지 않았다.
- 성능: 구조적 검토 일부만 수행; profiler 측정 NOT RUN

## 최종 hard gate

```text
SOURCE FILES FOUND: 447 (Java: 364)
SOURCE FILES REVIEWED: 249
SOURCE FILES UNREAD: 198
FULL SOURCE AUDIT COMPLETE: NO
CODE REVIEWED: NO (전체 소스 전수 검토 미완료)
TESTED: NO (이번 변경의 GameTest 미실행; historical build 431의 74 GameTests PASS와 구분)
BUILD VERIFIED: NO
PACK PRODUCED: NO
DEDICATED SERVER SMOKE: NO
CLIENT BOOT TESTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

커밋 SHA, 이번 변경을 포함한 workflow run ID/결과, 최종 `origin/main` 일치 여부와 로컬 상위 폴더 rename 결과는 배포/검증 후 이 기록에 보충한다.
