# Augmentation Survival — Augment Interaction Contract V2

Date: 2026-10-24
Status: CANONICAL DESIGN CONTRACT — 146 roster lock

이 문서는 `AUGMENTS.md`의 확정 증강 전체가 동시에 존재할 때 적용할 계산 순서, 출처 판정, 재귀 차단, 외부 모드 호환, 서버 권한 규칙을 하나로 합친 정본이다.

이 문서가 이전 `AUGMENT_PROJECTILE_SEGMENT_OVERRIDE_2026-10-24.md`의 내용을 흡수한다. 충돌 시 이 V2가 우선한다. 개별 증강의 이름/등급/최대 레벨/기본 수치는 `AUGMENTS.md`가 우선한다.

## 1. 절대 원칙

- 강한 조합은 허용한다. 강하다는 이유만으로 시너지를 끊지 않는다.
- 같은 효과가 자기 자신을 다시 만들어 무한/기하급수 재귀가 되는 경우만 차단한다.
- 피해, 회복, 드롭, 채굴, 능력 성공, 쿨다운, 증강 상태, 보상, 저장 상태는 서버가 최종 결정한다.
- 클라이언트는 입력 요청과 표시를 담당하고 결과를 결정하지 않는다.
- 바닐라/외부 모드의 정상 행동을 최대한 보존한 뒤 증강 레이어를 얹는다.
- 외부 모드 호환은 ID 분기 난립보다 tag/profile/adapter 기반으로 관리한다.

## 2. 스탯과 피해 계산

### 양의 퍼센트 스탯

같은 축의 증강 보너스는 합연산 후 한 번 적용한다.

예:
- 공격속도: `속공 + 연속 처치 + 광폭화 + 시간 가속`
- 이동속도: `빠른 발 + 숨 고르기 + 야행성 + 햇빛 체질 + 다급한 발걸음 + 연속 처치 + 관성 전환 + 시간 가속`
- 채굴속도: `광부의 손 + 야행성 + 연속 채굴 + 시간 가속`

`속도 전환`은 위에서 계산된 **양의 보너스 이동속도**만 읽는다. 기본 이동속도와 단순 질주는 입력값에 포함하지 않는다.

### 피해 감소

증강 피해 감소끼리는 곱연산한다.

기본 피해 순서:
1. 무기/공격의 기준 원피해
2. 공격자 측 일반/조건부 증강 피해 보너스
3. 대상 갑옷/방어 강도/Protection
4. `방어 무시` 같은 방어 수정
5. 일반 증강 피해 감소
6. 조건부/속성 피해 감소
7. `약점 표식` 같은 최종 받는 피해 증가
8. 이후 피해 지연/보호막/흡수/HP 파이프라인

`천벌` 같은 명시적 최대체력 비율 특수 피해는 일반 공격 피해증폭 레이어를 사용하지 않는다.

## 3. 전투 provenance

최소 출처 분류:

- `ROOT_MELEE`: 플레이어가 직접 실행한 근접 적중
- `ROOT_PROJECTILE`: 플레이어가 직접 발사한 원본 물리 투사체 적중
- `DERIVED_PROJECTILE`: `분산 사격` 등 증강이 만든 보조 물리 투사체
- `DERIVED_CHAIN`: `연쇄 화살`, `벼락` 등 연쇄 피해
- `DERIVED_AOE`: `연쇄 폭발`, `생명 파동`, `보호막 폭발` 등 자동 광역 피해
- `ECHO`: `메아리 타격` 추가 타격
- `PATH_ECHO`: `되밟기` 잔상 피해
- `ACTIVE_SPECIAL`: 일반 액티브의 특수 피해
- `REPLAY`: `재연`이 만든 능력 재발동
- `AUTHORITY`: `권능 탈취` 해방에서 나온 피해
- `COMPANION`: `분신`, `지배` 등 동료 피해
- `REFLECTED`: `돌려보내기`로 소유권이 변경된 투사체
- `SPECIAL_PERCENT_HP`: 최대체력 비율 특수 피해

용어:
- **ROOT 직접 적중/직접 처치** = `ROOT_MELEE` 또는 `ROOT_PROJECTILE`.
- **플레이어 귀속 처치** = 플레이어 ROOT 또는 플레이어가 소유한 파생/액티브 피해로 죽였으나 `COMPANION`은 아닌 처치.

`벼락`, `메아리 타격`, `약점 표식`, `전투 순환`, `공중 연계`, `되밟기`처럼 직접 행동을 요구하는 효과는 명시된 ROOT 이벤트만 카운트한다.

`분신`/`지배` 처치는 직접 처치가 아니며 코어/진행 보상, `전투 순환`, `사냥 기록`의 직접 처치 판정을 만들지 않는다.

## 4. 물리 투사체 그래프

### 4.1 발사 행동과 분산 사격

- 한 번의 발사 입력을 하나의 `shot action`으로 식별한다.
- `분산 사격`은 shot action당 보조 투사체를 한 번만 만든다.
- 보조 투사체는 `분산 사격`을 다시 생성하지 않는다.
- Crossbow Multishot도 하나의 shot action이다. 기존 Multishot 화살 각각에서 분산을 재생성하지 않는다.
- `화살 아끼기` 탄약 미소모 판정도 root shot action당 한 번이다.
- 각 보조 투사체는 독립된 물리 투사체로서 관통/연쇄/도탄/추적 상태를 가진다.

### 4.2 비행 구간

물리 투사체는 `flight segment`를 가진다.

- 발사 직후 segment 0 시작.
- `도탄`으로 고체 블록에 반사될 때 기존 segment 종료, 새 segment 시작.
- 같은 segment 안에서는 같은 엔티티를 최대 1회만 직접 적중한다.
- 새 segment에서는 이전 segment에서 맞힌 엔티티도 다시 직접 적중 가능하다.
- 같은 물리 투사체라는 identity는 도탄 후에도 유지한다.

### 4.3 관통

- 바닐라 Piercing과 `관통 화살`은 하나의 관통 예산으로 합친다.
- 관통 예산은 flight segment마다 새로 시작한다.
- 따라서 `적 관통 → 벽 도탄 → 같은 적 재관통`을 허용한다.

### 4.4 도탄

- 최대 도탄 1 / 2 / 3회.
- 도탄당 이후 직접 피해는 원래 투사체 기준 +20% / +35% / +50%씩 누적 합산한다.
- Lv.3에서 세 번 도탄한 뒤 이후 직접 적중은 원래 기준 +150%.
- 도탄 횟수가 유한하므로 동일 투사체의 영구 루프는 불가능하다.

### 4.5 추적 화살

- 기본 후보 획득은 전방 30° / 12블록 범위를 기준으로 한다.
- 방향 보정은 tick당 최대 2° / 3° / 4°.
- 도탄 뒤에도 추적 상태를 유지/재획득할 수 있다.
- 직전 segment에서 맞힌 대상도 새 segment에서는 다시 추적 후보가 된다.
- 직전 목표가 살아 있고 유효하면 재획득 우선 대상이 될 수 있다.

### 4.6 연쇄 화살

- **물리 투사체 identity 하나당 전체 수명에서 연쇄 시퀀스 최대 1회**.
- 관통/도탄으로 segment가 바뀌어도 연쇄 사용 여부는 초기화하지 않는다.
- 연쇄 피해는 물리 투사체가 아니므로 관통/도탄/추적/분산을 다시 실행하지 않는다.

### 4.7 ROOT 카운터와 도탄

원본 화살의 도탄 후 재적중도 `ROOT_PROJECTILE` provenance를 유지한다. 따라서 같은 원본 화살이 새 segment에서 같은 적을 다시 맞히면 ROOT 직접 적중을 요구하는 효과의 유효 적중으로 계산할 수 있다.

`분산 사격` 보조탄은 `DERIVED_PROJECTILE`이므로 관통/도탄/추적/연쇄는 이어받지만 `벼락`, `약점 표식` 같은 ROOT 카운터를 올리지 않는다.

## 5. 근접/이동 전투 시너지

### 공중 연계 × 두 번 점프

- 지상/수영/등반 접촉 시 기본 공중 점프 상태를 초기화한다.
- `두 번 점프`는 지상 이탈 후 1회의 기본 공중 점프 권한을 준다.
- `공중 연계`는 공중 ROOT 근접 적중마다 공중 점프 권한 1회를 즉시 다시 사용할 수 있게 한다.
- 내부 쿨다운은 없다. 같은 적을 계속 맞혀도 재충전된다.
- 따라서 타격을 계속 성공시키면 착지 없이 무한 공중전을 할 수 있으며 이는 의도된 시너지다.

### 탄성 저장 × 관성 전환

- 4블록 이상 **실제 낙하 거리**가 조건 입력이다. 낙하 피해 감소 여부와 무관하다.
- `탄성 저장`은 낙하 거리를 3초 보관하고 다음 지상 점프에 30% / 60% / 100% 추진력으로 소비한다.
- `관성 전환`은 같은 착지에서 이동속도 버프를 동시에 받을 수 있다.
- 임시 발판으로 낙하 피해가 0이어도 실제 낙하 거리가 조건을 만족하면 둘 다 발동 가능하다.

### 되밟기

- 최근 2초 플레이어 위치를 유한 길이 ring buffer로 보관한다.
- 완충 ROOT 근접 적중 때 경로를 역방향으로 재생한다.
- 한 activation에서 같은 대상은 1회만 `PATH_ECHO` 피해를 받는다.
- `PATH_ECHO`는 ROOT 카운터/되밟기/메아리/벼락을 재귀 생성하지 않는다.

### 벽치기

- ROOT 직접 공격이 만든 knockback event에 소유자와 기준 피해를 기록한다.
- 해당 knockback의 짧은 유효창 안에 대상이 고체 지형에 충돌하면 30% / 60% / 100% 추가 피해를 1회 준다.
- `거대화`의 가하는 넉백 +100%와 정상 연계한다.
- 환경 이동/다른 개체의 넉백은 해당 플레이어의 벽치기를 발동하지 않는다.
- 보스는 실제로 이동이 허용된 경우에만 충돌 판정을 받으며 profile에서 제한할 수 있다.

### 작아지기 × 거대화

두 변신은 동시에 크기 배율을 곱하지 않는다.

- `거대화`가 활성화되면 `작아지기` 상태를 일시 중지한다.
- 거대화 중에는 새 작아지기를 시작하지 않는다.
- 거대화 종료 뒤 웅크리기 조건을 다시 평가하여 작아지기를 재개할 수 있다.
- 둘 다 종료/해제 때 안전 공간 검사를 하고 질식/벽 끼임을 만들지 않는다.

## 6. 회복 파이프라인

기본 순서:
1. 1차 회복 원인 생성.
2. 받는 대상의 `회복 증폭` 적용.
3. 실제 HP 회복량 확정.
4. 1차 회복 초과분을 `넘치는 회복`이 흡수 체력으로 변환.
5. `회복 나눔`은 실제 HP 회복량만 공유.
6. 공유 회복은 `회복 나눔`을 다시 만들지 않고 `넘치는 회복`용 초과 회복도 만들지 않는다.

`흡혈`은 자연회복이 아니므로 회복 증폭/넘치는 회복/회복 나눔과 정상 연계한다.

### 물약 확산

- 지속형 이로운 물약: 원 지속시간의 25% / 40% / 55% 공유.
- 즉시형: 원 효과량의 25% / 40% / 55% 공유.
- 공유 복사본은 물약 확산을 다시 발동하지 않는다.
- 공유 즉시치유는 받는 대상의 회복 증폭을 적용할 수 있다.

### 생명 파동

- 자연회복이 아닌 **1차 실제 회복 사건**에서만 공격 파동을 만든다.
- `회복 나눔`으로 생성된 공유 회복은 생명 파동을 다시 만들지 않는다.

## 7. 보호막 / 흡수 / 피해 지연 / 무적

`무적`은 가장 앞에서 피해 이벤트 자체를 거부한다.

- 무적으로 취소된 공격은 `피해 지연`, `피해 적응`, `다급한 발걸음` 등을 새로 만들지 않는다.
- 과거 생성된 지연 피해 installment가 무적 시간에 도착하면 해당 installment도 0이 될 수 있다.

무적이 아닌 정상 순서:
1. 갑옷/방어 강도/Protection
2. 일반 증강 피해 감소
3. 조건부 피해 감소
4. `피해 지연` 예약 비율 분리
5. 별도 `보호막` pool
6. 바닐라/증강 absorption
7. 실제 HP
8. 치명 판정

예약된 지연 피해는 방어 계산과 피해 지연을 다시 적용하지 않는다.

### 보호막 폭발

보호막/흡수는 source pool identity를 보존한다.

- 같은 pool은 완전 소진 또는 만료 시 최대 1회만 폭발한다.
- 중간에 값을 보충했다고 같은 pool의 폭발 횟수가 리셋되지 않는다.
- 새로 생성된 별도 pool만 새로운 1회의 폭발 권리를 가진다.
- vanilla absorption과 다른 모드의 알 수 없는 absorption은 기본적으로 증강 생성 pool로 취급하지 않는다.

## 8. 치명 피해와 실제 사망

순서:
1. `한 번 버티기`
2. 주손/보조손 Totem
3. `비상 토템` 인벤토리 Totem
4. `재림`
5. 실제 사망

`재림`은 실제 사망이 아니다. 드롭/XP 손실/사망 지점/소생 표식을 만들지 않는다.

실제 사망에서만:
- `소지품 보존`
- `경험 보존`
- 마지막 사망 지점 기록
- `소생` 표식 생성
을 처리한다.

## 9. 상태효과

적에게서 온 제거 가능한 해로운 효과:
1. 무적이면 적용 자체를 거부하며 반사도 없음.
2. 적용 가능하면 `상태 반사`는 원래 incoming 지속시간을 기준으로 복사본 생성.
3. 자기 자신에게는 `강인한 정신` 지속시간 감소 적용.
4. 이후 `정화` 가능.

반사 복사본은 다시 상태 반사를 발동하지 않는다.

## 10. 액티브 / 쿨다운 / 충전 / 재연

### 기본 쿨다운

- authored base cooldown에서 `능력 가속` 25%를 적용해 유효 재충전 시간을 만든다.
- `이중 충전`은 eligible 비신화 액티브에 최대 2충전을 제공한다.
- 두 충전은 **순차 재충전**한다.
- `전투 순환`은 현재 충전 타이머를 줄이고, 초과 감소량은 다음 빈 충전으로 이월한다.

기본 제외:
- 쿨다운 없음
- 토글만 존재
- 사망당 1회 자원형
- 신화 액티브

### 연장전

- ROOT 직접 처치 때 현재 실행 중인 eligible 비신화 지속형 액티브를 연장한다.
- 한 번의 수동 사용 instance당 기본 지속시간의 최대 50%까지 연장한다.

### 재연

능력마다 data-driven `ReplayPolicy`를 가진다.

필수 필드 예:
- `eligible`
- `powerScale`
- `durationScale`
- `rangeScale`
- `targetPolicy`
- `stackMode`

규칙:
- **MANUAL** 비신화 전투 액티브 사용만 1.5초 뒤 재연을 예약한다.
- 재연은 충전/쿨다운을 소비하지 않는다.
- 재연 invocation은 provenance `REPLAY`이며 또 다른 재연을 예약하지 않는다.
- 순간이동/부활/장기 채널/단순 50% 스케일이 성립하지 않는 능력은 제외한다.
- `이중 충전`의 각 수동 사용은 각각 한 번의 재연을 예약할 수 있다.
- 동일 지속형 능력이 원본과 재연으로 겹칠 때의 합산/갱신/별도 instance 여부는 능력별 `stackMode`가 결정한다.

## 11. 자원 / 채굴 / 농사 / 풍요

드롭 순서:
`기본 바닐라/모드 드롭 → Fortune/Looting → 호환 추가/복제 증강 → 최종 eligible stack → 풍요 ×2`

- `풍요`는 최종 일반 스택 수량에 적용한다.
- 코어/고유 보스 보상/진행 아이템/고유 NBT 장비/비스택 장비 제외.
- 자동 채굴/수확된 블록은 호환 가능한 드롭/Fortune/내구도 규칙을 이어받는다.
- 자동 처리 블록은 또 다른 범위/연쇄 selector를 시작하지 않는다.
- `연속 채굴`은 player root mining action만 카운트한다.
- `순간 채굴`은 직접 조준한 root block의 파괴시간만 1 tick으로 바꾼다.
- `굴파기`는 일반 굴착 tag, `나무 베기`는 연결 원목을 독점적으로 담당한다.

Farmer's Delight 등 외부 작물은 age 기반/우클릭 수확/다중 블록/재파종 재료 adapter를 구분한다.

## 12. 동료 / 아군 / 지배

공통 분류:
- allied player
- tamed permanent combat companion
- `지배` permanent companion
- `분신` temporary companion

기본:
- 치유 파동/회복 나눔/물약 확산/약점 표식의 아군 범위에 길들인·지배 동료 포함.
- 분신은 치유/버프를 받을 수 있으나 보상/진행 귀속 없음.
- 소생은 길들인/지배 영구 동료만 가능, 분신 제외.
- 지배 동료가 소생되면 소유권 유지.
- 지배 즉시 hostile target filter에서 제외.

외부 특수 AI는 강제 개조하지 않고 profile/adapter/exclusion을 사용한다.

## 13. 보스 / 적 제어 profile

데이터 기반 필드:
- `scaleGroup`
- `allowPull`
- `pullMultiplier`
- `allowPositionSwap`
- `allowTimeStop`
- `allowStun`
- `allowPercentHpDamage`
- `allowWallImpact`
- `allowDominate` (boss 기본 false)
- `customAdapter`

현재 디자인상 pull/swap/time-stop/stun/%HP는 기본 허용하고, 실제 scripted boss가 깨지는 경우만 개체별 제한한다.

`천벌`은 HP를 직접 set하지 않고 정상 damage/hurt 경로를 사용하며, 일반 공격 피해증폭과 생명 연결 공유를 받지 않는다. 보스 무적/phase 차단은 존중한다.

`시간 정지` 동안 대상의 스턴/상태 타이머도 정지한다.

## 14. 권능 탈취

- 지원 보스 최초 직접 처치 시 그 보스의 **플레이어용 전투 권능 패키지 전체**를 영구 해금한다.
- 원본 보스 AI/phase/world script를 런타임 복제하지 않는다.
- `boss profile → authored player authority package`로 명시 매핑한다.
- 권능 전용키 한 번으로 현재까지 해금한 모든 package를 하나의 서버 스케줄 시퀀스로 실행한다.
- 전체 쿨다운 120초.
- package 수가 많아져도 같은 tick에 전부 생성하지 않고 bounded schedule로 분산한다.
- `AUTHORITY` 피해는 플레이어 귀속이지만 ROOT 직접 적중/직접 처치는 아니다.
- 신화이므로 능력 가속/이중 충전/재연 대상이 아니다.

## 15. 두 번째 선택과 증강 획득 transaction

`두 번째 선택` 보유자는 코어 1개에서 서로 다른 후보 2개를 고른다.

서버 transaction 규칙:
1. 코어와 후보 pool 유효성을 서버에서 생성/잠금.
2. 첫 선택을 임시 보관하고 같은 후보 재선택 금지.
3. 두 번째 선택까지 유효하면 두 획득을 한 transaction으로 commit.
4. disconnect/취소/서버 검증 실패에서는 코어 소비와 부분 획득이 남지 않게 rollback.
5. 후보가 2개 미만이면 기존 후보 부족 규칙을 따른다.

## 16. 위협 단계와 적 스케일

- 세이브 전체가 위협 0~8 하나를 공유한다.
- 월드 위협 점수는 해당 세이브에서 달성한 플레이어 power score 최고 기록.
- 단계는 자동 하락하지 않는다.
- 일반/엘리트는 단계 상승/로드 시 갱신하되 매 tick 재계산하지 않는다.
- 보스는 encounter 시작 시 위협 단계를 스냅샷한다.
- 적 공격력보다 HP를 더 크게 스케일한다.
- 스케일 자체가 코어 드롭률을 자동 증가시키지 않는다.

수치 테이블은 `ENEMY_POWER_SCALING_DESIGN.md`의 플레이테스트 초기값을 사용하되 최종 밸런스 값으로 간주하지 않는다.

## 17. Better Combat

Better Combat 사용 시 하나의 서버 권한 ROOT melee hit를 정규화한다.

- 공격속도 관련 증강은 하나의 attack-speed 축을 수정한다.
- 휩쓸기 강화는 실제 target selection/hitbox 결과를 기준으로 확장한다.
- 벼락/메아리/약점표식/공중연계/되밟기/직접처치 판정은 대상당 정확히 정의된 ROOT 이벤트만 받는다.
- 방패 던지기 무기 스냅샷은 우연한 현재 콤보 프레임 배율이 아니라 표준 완충 공격 기준을 사용한다.

실제 공존은 구현 후 플레이테스트한다.

## 18. 외부 모드 호환

- Terralith / Repurposed Structures / Dungeons and Taverns: 기존 loot/worldgen 보존 + core pool data overlay.
- Friends&Foes / Illager Invasion: hostile/elite scaling profile과 loot adapter.
- Naturalist: 일반 평화 동물을 코어 파밍 대상으로 만들지 않는다.
- Farmer's Delight Refabricated: crop/food/recipe/processing adapter.
- Chrono Dawn: 공식 보스 처치 API가 유효하면 우선 사용하고 reward/control/authority profile을 연결.
- World Bosses: namespace/entity adapter를 사용하며 원본 JAR 수정/재배포 없음.
- MCEndgame: boss/progression adapter, 반복 보상률 별도 관리, 공식 팩에서는 analytics 비활성화 우선.
- GeckoLib / Player Animation Library: 연출/애니메이션 역할이며 gameplay authority는 서버 로직이 가진다.

## 19. 저장 / 네트워크 경계

### 플레이어 상태

서버 영구 상태에 최소:
- augment level map
- active slot layout
- cooldown/charge state
- 영구 counter (`사냥 기록` entity type 등)
- stolen authority boss IDs
- death point/resource state
- state schema version

를 저장한다.

### 월드/공유 상태

플레이어 attachment에 넣지 않고 별도 월드/서버 영구 데이터로 관리:
- 월드 위협 high-water mark/stage
- 원격 창고 UUID/shared-group storage
- 공간 연결 portal network

### 네트워크

serverbound는 **요청**만 보낸다. 서버가 재검증한다.

예:
- active use request
- active slot edit
- authority release request
- target confirm
- card/device selection confirm

clientbound:
- initial state snapshot
- small state delta
- cooldown/HUD/threat state
- preview/VFX result

큰 플레이어 상태를 매 작은 변경마다 전체 재동기화하지 않는다.

## 20. 성능 / 무한루프 방지 예산

- 매 tick 전체 엔티티/전체 플레이어/전체 인벤토리 전수 검색 금지.
- 반경 탐색은 이벤트 발생 시점 또는 낮은 주기의 bounded query로 제한.
- 되밟기 경로는 플레이어당 최근 2초의 고정 길이 buffer.
- projectile segment hit-set은 투사체 수명 동안만 유지하고 entity 제거와 함께 폐기.
- 권능 탈취는 bounded scheduler 사용.
- 연쇄/분산/도탄은 저작된 최대 횟수 이상의 child action을 생성하지 않는다.
- 보호막 pool, delayed damage, cooldown queue는 source ID와 만료시각으로 정리한다.
- 위협 스케일은 event/load 시 갱신하며 tick 단위 재계산 금지.

## 21. 구현 전 남은 수치 파라미터

아래는 **아키텍처 차단 요소가 아니라 콘텐츠 파라미터**다. 구현 구조는 먼저 만들 수 있다.

1. `탄도 장막` 정확한 발동 주기 / 범위 / 회당 투사체 수
2. `임시 발판` 지속시간 / 쿨다운
3. `생명 연결` 자동 연결 조건 / 대상 수 / 범위 / 지속 / 공유 비율
4. `작아지기` 웅크리기 유지시간
5. `벽치기` 충돌 유효 시간 / 충돌 판정 최소 기준
6. 코어 획득처별 정확한 등급 확률
7. 위협 단계 수치 테이블 최종 튜닝

이 값은 하드코딩 분기보다 data/config parameter로 넣어 플레이테스트에서 조정 가능하게 한다.

## 22. 검증 상태

- ROSTER TARGET: 146
- DESIGN CONTRACT V2: FINALIZED
- DUPLICATE REQUIRING REMOVAL: 0
- RUNTIME IMPLEMENTED: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
- BUILD/CI: NOT RUN — docs-only
