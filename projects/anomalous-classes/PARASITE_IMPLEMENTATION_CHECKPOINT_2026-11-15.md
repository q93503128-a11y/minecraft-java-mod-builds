# 기생술사 — 2026-11-15 구현·검증 체크포인트

## 2026-11-16 — 기생술사 54종 보존·외부 3계열 특성 묶음 구현·개발 우선순위 전환

- 코드: [`f9a9897c6818a1c535f0988a16db633246e170f7`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/f9a9897c6818a1c535f0988a16db633246e170f7).
- **BUILD VERIFIED/JAR PRODUCED:** [GitHub Actions #43828544203](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43828544203) SUCCESS. JAR `anomalous-classes-0.1.0-alpha.1.jar`, **32,075,489 bytes/189 classes**, [검증된 JAR + SHA256](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43828544203/artifacts/14207094308). `verifyParasiteFoundation`, 타 직업 regression, JAR verifier PASS.
- **기존 54개 기생 프로필·기생 권한/BD/숙련/세이브·6개 클래스 설계 보존.** 외부 20개 모두를 원본 전용 특수공격까지 구현한 것으로 간주하지 않는다.
- V+5 `mowziesmobs:grottol` 재빠른 질주: 원본 숙주 실물에 대해 지면·전방 충돌·수중 조건을 검사해 실제 이동 임펄스를 주며 손상 없는 반짝임/소리로 보조. **18초 쿨다운**, 공격 공통 후딜 0. 그로톨 본체는 숙련0부터, 그로톨 숙련100 이후 다른 숙주에서도 가능.
- V+6 `mowziesmobs:bluff` 지면 충격: 숙주 눈으로 9블록 내 **실제 지면 위 표적**, 벽 가림·친선/팀·PvP 권한 확인 후 5HP 공격·수직 밀림·바닥 블록 입자/충격음. **23초 쿨다운, 3초 공통 공격 후딜**. 블러프 본체는 숙련0부터, 숙련100 후 다른 숙주에 계승. 블록 파괴/소환체 생성 없음.
- `mutantmonsters:spider_pig`: 내장 환경형 **벽 타기 + 점프 0.55**를 숙련0에 제공, 해당 종 숙련100에 다른 실숙주에도 자동 계승; 기존 거미/슬라임과 중복 이동력 급증 방지 규칙 보존.
- **출처/표현 구분:** 그로톨은 빠른 도주, 블러프는 지면 상승 공격, 스파이더 피그는 벽 타기/도약이 원본 모드 행동의 근거. 이번 구현은 해당 행동을 참고한 **게임 자체의 컨트롤 어댑터**이며 **외부 모드 내부 애니메이션/AI 메서드를 직접 호출하거나 완전 재현한 것이 아님**. 외부 라이선스 자산 재배포 없음.
- deterministic 검사: 스파이더 피그의 native vs mastery99/100 환경형 능력, 그로톨 지면/수중/충돌 차단, 2종 지원 ID와 핫키, 개별·공통 쿨다운 전이, 기존 54종·세이브·기본 전투/숙주 판정 검사 PASS.
- **CLIENT RUNTIME TESTED: NO / PLAYTESTED: NO / MULTIPLAYER TESTED: NO.** 외부 모드 + 최신 JAR 동시 기생 시연, 클라 렌더/동작 타격감, 특수 본체 애니메이션, 소유권·네트워크 및 저장/재접속 세션은 현재 빌드만으로 검증되지 않음.
- **개발 우선순위 조정:** 기생술사는 계속 종별 기술을 무제한 추가하는 대신 **현재 단계의 기능 개발을 여기서 멈추고, 실제 플레이 QA를 위한 체크리스트로 미완료 항목을 이관**. 다음 구현은 기존 확정 기획의 **결계술사**: 방어/압축/격리 독립 3결계가 실제 전장에 함께 존재하는 플레이 사이클을 한 작업 단위로 구축. 현행 사용자가 확정한 입력 `R 미리보기→R 설치 / Z 유형 / X 크기 / G 최근 성공 즉시 시전` 유지. 여섯 직업을 두루 완성한 뒤 전체 멀티/클라 통합 QA로 복귀. 이 선언은 **기생술사 품질 완료/실제 플레이 통과**를 의미하지 않는다.


---

> 아래 항목들은 이전 단계의 기록이다. 신규 외부 3계열·다음 직업 개발 우선순위는 위 최신 체크포인트 우선.


## 2026-11-16 — 기생술사 실숙주 상호작용·대형 근접 타격·탑승체 순간이동

- **구현 소스:** [`f3098fc173f9690ea2b65f7f5c5c5df5a93115f3`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/f3098fc173f9690ea2b65f7f5c5c5df5a93115f3), standalone verifier 접근 제한 수정 [`e909e5726a4c10fa7c72c602c81066d1ea1a1139`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/e909e5726a4c10fa7c72c602c81066d1ea1a1139). 기존 바닐라 **34종 + 외부 20종 = 총54 숙주** 및 타 직업/데이터/외부 모드 고정 manifest 유지.
- **BUILD VERIFIED / JAR PRODUCED:** [Build anomalous-classes #43824126337](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43824126337) **SUCCESS**, `anomalous-classes-0.1.0-alpha.1.jar` **32,070,688 bytes / 188 classes**. [JAR+SHA256와 검증 기록](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43824126337/artifacts/14204724896). `verifyParasiteFoundation` PASS: 54 hosts, original-host interaction reach, giant melee cone, mounted teleport projection, cooldown, input, mastery and NBT. 기타 직업/스킬트리/UI layout/JAR 검사 포함 기존 build PASS.
- **바닐라 생존 조작을 버리지 않고 숙주 눈 기준 재검증:** 신규 `ParasiteHostReachService`가 기생 중 실제 player 블록/엔티티 도달 거리 속성에 **일시적인 +2.75 블록 network reach buffer**를 주고, 채굴 시작/블록 우클릭·엔티티 우클릭·블록 파괴 완료 시 **서버에서 원본 숙주 눈높이·시선·시야 가림·실제 사거리**를 독립 확인한다. 원본 플레이어 인벤토리/도구/아이템·제작/상자·일반 채굴 처리와 소유권/도전과제 경로를 보존. 호스트 없는 플레이어나 클래스 변경/사망/로그아웃/자발적 이탈 시 reach modifier 삭제; 기존 속성 모디파이어는 삭제하지 않는다. 기존 상호작용의 새로운 서버 권한 우회 금지.
- **큰 몸체 근접 판정:** `ParasiteHostCombatService`는 플레이어 기본 사거리와 원본 Mob 폭을 이용해 최대 **5.5블록** 내에서 근접 가능 범위를 제한적으로 넓힌다. 기존 숙주 눈높이 레이캐스트를 1순위로, 그 결과가 없을 때만 대형 몸체의 **좁은 전방 몸통 공격 범위**를 검사한다. 벽 뒤 대상·후방 대상은 원칙적으로 제외하고, 공격/드롭/인챈트/무기 내구도/기여도는 기존 서버 `owner.attack` 경로 사용. 공격의 최종 feel, 원본 애니메이션·히트박스는 미검증.
- **엔더맨 수동 순간이동·자동 회피에서 탑승자 보호:** 이동 후보가 원본 Mob의 충돌 없는 발판/로드된 청크/월드 보더를 충족하는지뿐 아니라 실제 플레이어가 숙주 승차 위치에 들어갈 **실제 몸체 충돌·머리 공간**도 확인. `host.teleportTo` 성공 후 실제 owner 승차 상태 복원 시도, `host.positionRider(owner)`, `owner.connection.resetPosition()`, `ParasiteService.syncView(owner)`를 사용해 원본 몸+탑승자 클라이언트 상태를 갱신. 착지/승차 복구가 실패하면 기술은 실패로 처리. 쿨다운은 이전 도입한 성공 결과 기반 `castIf` 계약을 유지.
- **회귀 검증:** 상호작용 cap, 정면/후방·횡방향 시야 검사, 대형 공격 보너스 상한과 뒤쪽/작은 몹 제외, 호스트 원위치·승차점 이동량 일치 등 순수 규칙 fixture PASS. 기존 54종, 외부 20 exact class, BD/마스터리, 생체 데이터/저장·전투 쿨다운 테스트 유지.
- **실패 후 수정 사실:** 최초 빌드 [#43823909989](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43823909989)는 별도 verifier에서 새로운 policy 함수에 접근할 수 없어 실패. 해당 함수만 public으로 바꾼 뒤 #43824126337 재검증 성공, 기존 테스트·콘텐츠 축소 없음.
- **실행하지 않은 것:** **CLIENT RUNTIME / ACTUAL PLAYTEST / MULTIPLAYER TEST = NO.** 서버 패킷과 클라이언트 블록 outline의 도달 거리 표시 일치, 손실/연속 입력, 승차체 순간이동 시 멀티 좌표 동기화·크기별 좌석/천장 충돌, 블록 파괴 후 리소스 드롭, 상자/문/우클릭 모드 호환, 대형 몹 애니메이션과 손맛은 실게임 검수가 필요. [직전 검증된 외부 13 JAR 동시 서버 부팅 #43741441267](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267)은 별도 이전 단계이며 **이 변경 커밋의 외부 모드 플레이테스트가 아님**.


---

> 아래 항목들은 이전 검증 시점의 기록이다. 상호작용·대형 근접 공격·승차체 순간이동 현황은 위 최신 체크포인트 우선.


## 2026-11-16 — 숙주 직접 조작 안정화: 입력·시선·AI·무효화 복구

- **구현 코드:** [`b217a05fb73e04ba834ac216b0d2f94d4d42817d`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/b217a05fb73e04ba834ac216b0d2f94d4d42817d). 기존 숙주 바닐라34/외부20 **54개 프로필 보존**. 기존 외부 모드 9+6, 저장 NBT 키, 종족 직접 능력 슬롯, 다른 직업 변경 없음.
- **BUILD VERIFIED / JAR PRODUCED:** [GitHub Actions Build #43744893340](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43744893340) **SUCCESS**. `anomalous-classes-0.1.0-alpha.1.jar` **32,064,956 bytes, 187 classes**. [JAR+SHA256 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43744893340/artifacts/14173509953). `verifyParasiteFoundation`, `verifyPuppeteerFoundation`, `verifyGraphSkillTreeRuntime`, UI/layout/resource verification and JAR integrity checks PASS.
- **Stationary aim bug:** previously host yaw/head/pitch rotation was updated only while horizontal input had movement. While stopped, the player could turn their camera and the authoritative original host would remain facing its prior direction. Moved `setYRot`, `setYHeadRot`, `setYBodyRot`, `setXRot` into a fresh-look-input path **independent of planar movement**; no empty-key 0° forced turn after packet expiration. Client stays the input/view capture point, server the authoritative world entity.
- **Shared deterministic motion math:** new `ParasiteMotionRules.java` validates finite camera yaw/pitch, keymask, maximum packet staleness (10 server ticks and monotonic age), normalizes diagonals, prevents backward/forward co-press from triggering sprint, normalizes yaw and bounds pitch-controlled water ascent/descent. `ParasiteService.movement` now also verifies the current claimed original host before accepting rotation/keys. Server still derives all speed, damage, target and world effects; no client coordinates.
- **External Mob direct control:** while actively riding the claimed original Mob, if third-party AI unexpectedly becomes enabled between ticks, stop navigation and restore NoAI. This prevents concurrent AI pathfinding and player movement competing for a single host; it does not claim native scripted attacks are reproduced.
- **Stale saved host recovery:** if the exact registered external EntityType still exists but Java class fingerprint changed on entity join, detach the present player from that invalid original host, stop that host's per-host ability effects, clear in-flight input/possession, restore original NoAI state, release only the current matching claim and resync human health/camera. Existing unloaded-host claim preservation for valid entities remains.
- **Deterministic regression verifier:** invalid keymasks, NaN/infinity and pitch range; 10-tick freshness boundary and clock rollback; 0-movement fresh look; 0° forward and diagonal normalization; reverse+sprint rejection; ±90° underwater pitch and bounded swim speed; wrapped yaw; 54 profiles, 20 external fingerprints and previous saved masteries/cooldowns all pass. This fixture is **CODE/RULES TESTED**, not an in-game mouse/collision test.
- **Earlier external pack simultaneous server test remains:** [#43741441267](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267) PASS 20/20 classes, 13 server JARs. The present commit did **not** rerun a dedicated external pack with player-connected possession.
- **CLIENT RUNTIME TESTED: NO / ACTUAL PLAYTESTED: NO / MULTIPLAYER TESTED: NO.** Real client camera rotation when stationary, walk/jump/collision per original body, dismount resistance, external AI resumption, backup host in unloaded chunks, mining from original host eye, performance/animation still require actual gameplay QA before declaring the experience complete.


---

> 아래 내용은 이전 시점의 구현·검증 스냅샷이다. 현재 조작 안정화 상태는 위 최신 체크포인트를 우선한다.


## 2026-11-16 — 기생술사 계승 전투 안전성 + 파괴수 포효 + 거북 방어

- **소스 변경:** [`c122b90808c3b02523ab6ca7cb101cf48e506e21`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/c122b90808c3b02523ab6ca7cb101cf48e506e21). 바닐라 34 + 외부 20 = **54개 원본 숙주 프로필 유지**. 타 직업/기존 저장 키/고정 외부 모드팩 9+6 구성 불변.
- **BUILD VERIFIED / JAR PRODUCED:** [Build anomalous-classes #43743671415](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43743671415) **SUCCESS**. `anomalous-classes-0.1.0-alpha.1.jar`: **32,063,691 bytes, 186 classes**, [JAR·SHA256 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43743671415/artifacts/14172778208). `verifyParasiteFoundation`, `verifyPuppeteerFoundation`, `verifyGraphSkillTreeRuntime`, 전체 기존 project 검사, JAR 구조 검사 PASS.
- **원인 수정 — 전투 쿨다운:** 기존 가디언 광선/엔더맨 순간이동은 실제 피해·위치 변경을 먼저 실행하고 이후 `ParasiteTraitSavedData.cast`로 공통 후딜/개별 cooldown을 확인했다. 이전 공격의 공통 회복 중 다음 기술이 **쿨다운 소비 없이 효과만 일으킬 수 있는 실행 순서 결함**을 수정했다. `ParasiteTraitSavedData.castIf`가 먼저 권한/개별·공통 쿨다운을 확인하고, 같은 서버 스레드에서 효과를 시도하며, **유효한 효과가 성공했을 때만** 영구 cooldown을 기록한다. 오류 또는 목표/착지 실패는 cooldown 미소모. transient 재진입 플래그로 피격 콜백 중 기술 재실행 차단. 기존 `cast` API와 NBT 스키마 유지.
- **고유 직접 발동 추가:** 파괴수(`minecraft:ravager`)가 선천으로 쓰거나 파괴수 종 숙련100 달성 후 다른 몸에서도 V 모드 **숫자 4(코드 슬롯3)**로 사용. 5블록 이내 벽으로 가려지지 않은 적에게 기본 피해 6 후보와 밀쳐내기, 40초 개별 쿨다운/3초 공격형 공통 후딜(후보). 유효 피격 대상이 없다면 시전하지 않음. 플레이어·다른 직업의 지배체·기생 소유물·길들인 동물 등 기존 friendly-fire 권한 적용. 첫 기본 서버 효과음·파티클 적용, **원본 Ravager 포효 애니메이션의 최종 시각 재현은 미완료**.
- **상시 생존 특성:** 거북(`minecraft:turtle`)이 현재 숙주이거나 해당 종 숙련100이면 현재 숙주에 일시적 밀침 저항 속성 `+0.35` 후보. 기술 모드 V, 숫자키, 추가 자원과 무관하게 상시 적용. 숙주 이탈 시 transient modifier와 HP modifier 복구; 다른 종의 base knockback resistance를 삭제하지 않음. 숙주 스탯 주기 갱신 또는 신규 기생 시 적용.
- **외부 호환 실패 경계:** 외부 모드 업데이트로 기존 `EntityType` ID가 유지되더라도 실제 Java 클래스가 지정된 `expectedClass`와 다르면 서버 `onEntityJoin`에서 기존 숙주의 원래 AI를 복원하고 그 원본 claim을 해제해, 무한 NoAI/frozen body 상태가 남지 않도록 수정.
- **검증된 회귀:** 시전 중 실제 효과 콜백 전에 공통 후딜을 검사하는지, 거절된 광선/순간이동이 콜백을 실행하지 않는지, 무효 대상·실패 시전이 cooldown을 소비하지 않는지, 같은 동기 콜백에서 재진입이 차단되는지, 거북 native/숙련99·100, 54종/외부20종 기존 프로필·지배/인형술사 회귀 테스트.
- **별도 실제 서버 동시 부팅 정본:** [이전 외부 통합 #43741441267](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267)에서 이미 20/20 외부 원본 클래스 및 13 JAR 동시 부팅 PASS. 이번 코드 커밋은 **일반 game build만 재검증했으며 변경된 포효/거북·updated host reload를 실제 월드에서 직접 실행한 테스트는 아님**.
- **CLIENT RUNTIME TESTED: NO / PLAYTESTED: NO / MULTIPLAYER TESTED: NO.** 파괴수 포효 적 대상, 벽 시야, sound/VFX sync, turtle knockback actual physics, camera/mount/dismount, mod class change on real persisted host는 실게임 검증이 필요. 기능 카탈로그 수를 완성도 지표로 오인하지 말 것.


---

> 이하 과거 54종 도입·외부 20종 연동 기록은 이전 시점의 검증 상태다. 위의 전투 수정 및 새 지원 능력 기록을 먼저 참고한다.


> 최신 GitHub `main` + 실제 코드가 정본이며, 이 기록은 2026-11-15 기준 확인 상태다. 기획을 새로 설계하지 않는다.

## 2026-11-16 — 실외부 모드 20종 기생 어댑터 검증 및 54종 숙주 통합

- **게임 코드:** [`b45e05dd2d006e0543211a6410bf329b622b8d10`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/b45e05dd2d006e0543211a6410bf329b622b8d10) + 첫 컴파일 오류를 바로잡은 [`001b52750e110b10f25556624ed21643b655a83a`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/001b52750e110b10f25556624ed21643b655a83a). [통합 감사 워크플로 `1d384ee`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/1d384eeb320a9d473a9ad6fee2b1eb0d4c22aead).
- **BUILD VERIFIED / JAR PRODUCED:** [Game build #43741286060](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741286060) SUCCESS, `anomalous-classes-0.1.0-alpha.1.jar` 32,061,417 bytes / 186 classes. [JAR+SHA256 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741286060/artifacts/14172467653). 관련 `verifyParasiteFoundation`, 다른 직업·스킬트리·JAR 검증 PASS.
- **DEDICATED SERVER CO-LOAD / EXTERNAL CLASS REGISTRY VERIFIED:** [External pack integration #43741441267](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267) SUCCESS, [감사 JSON/원본 서버 로그 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267/artifacts/14172409041). NeoForge 21.1.244 / Java 21, 외부 서버 필수 JAR 12개 + 우리 모드 JAR 1개 동시 로딩·월드 시작·정상 종료.
- 네 모드의 실제 런타임 레지스트리에 외부 엔티티 유형 **256개**, 그중 `Mob` 인스턴스 **149개** 등록 확인: Born in Chaos **98/84**, Cataclysm **103/39**, Mowzie's Mobs **41/18**, Mutant Monsters **14/8**(표기 유형/Mob). 인스턴스 생성 검사 오류 **0건**.
- **선정한 20개 호스트 모두 실제 EntityType ID와 Java 클래스 fingerprint 일치(20/20)**. 새 구성 총 **바닐라 34 + 외부 20 = 54종**. 출처별 외부 프로필 수: Born **10**, Cataclysm **7**, Mowzie **2**, Mutant **1**. 네임스페이스와 클래스가 예상과 다르면 서버가 해당 기생 시도를 거부한다. 다른 몹·보스는 등록만 됐다고 자동 기생 불가.
- 외부 몹은 원본 `Mob` 몸 1기를 사용해 서버 권위의 침투·기본 이동/근접 공격·BD·숙련·사망/이탈 로직을 공유한다. **특수 AI, 고유 투사체/보스 phase, 애니메이션 재현 및 능력 완성까지 보장하지 않는다.** 외부 `deepling` 계열의 내장 수중 적응·`baby_spider`의 벽 타기를 공통 환경형 속성으로 등록해 해당 종 숙련100 이후 타종에 영구 계승, 바닐라 거미·가디언·슬라임 등 기존 원리는 유지.
- 별도 클래스 실행/JAR이 없는 일반 서버에서도 외부 모드 의존성이 강제되지 않는다. 외부 몹이 실제로 없으면 해당 `EntityType`을 만날 수 없으며, 코드에는 제3자 클래스를 직접 import하지 않고 문자열 ID+실제 클래스 비교로 검증한다. 정확한 고정 버전은 [`pack-source.json`](./modpack/pack-source.json).
- 명시적 기생 제외 보스: `cataclysm:ignis`, `the_harbinger`, `netherite_monstrosity`, `mowziesmobs:ferrous_wroughtnaut`, `frostmaw`, `mutantmonsters:mutant_zombie`, `mutant_creeper`, `born_in_chaos_v1:supreme_bonescaller`. 관련 원본 phase·AI·보상 어댑터 제작 전에는 허용하지 않는다.
- 첫 CI [#43741135487](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741135487)은 이동 메서드의 중복 `species` 지역변수 선언 때문에 컴파일 실패. 정확한 중복 선언 제거 뒤 게임 build 및 외부 동시 로딩을 모두 다시 검증해 통과.
- **실제 CLIENT RUNTIME / PLAYTESTED / MULTIPLAYER TESTED = NO.** 개별 신규 20종의 탑승, 원본 AI 해제·복귀, 타격/무기/애니메이션, 이동·카메라 손맛, 보스 전투와 드롭/서버 지연은 미검증. 외부팩 통합 서버 부팅과 20/20 엔티티 실등록 확인은 **실게임 기생 조작 성공을 뜻하지 않는다**.
- [외부 20종 전용 선정·검증·잔여 QA 기록](./modpack/EXTERNAL_HOST_RUNTIME_BINDING_2026-11-16.md) 참고.


---

> 이하 34종 표기는 2026-11-16 중간 스냅샷이다. 현재 지원 범위는 위의 54종 프로필이 우선한다.

## 2026-11-16 — 34-species data-driven original-host catalog

- Source [`d52996e432ab39dbd95ba3a566ff3fd98bc41207`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/d52996e432ab39dbd95ba3a566ff3fd98bc41207) (`main`).
- [**Build anomalous-classes #43735461247**](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43735461247) SUCCESS. **BUILD VERIFIED / JAR PRODUCED** — `anomalous-classes-0.1.0-alpha.1.jar` **32,056,584 bytes / 185 classes**. [Deliverables with JAR and checksum](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43735461247/artifacts/14170476905), [logs](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43735461247/artifacts/14169624215).
- Replaced source-only `Map.of(...)` in `ParasiteRules` with bundled strict, validated [JSON host profile catalog](./src/main/resources/data/anomalousclasses/parasite/hosts.json). Entries declare `id`, `requiredBd`, `intrusionTicks`, `speedFactor`, `adapter` and `innateJump`. Exact registered mob ID (not wildcard or tag) is the eligibility key. Existing original 10 specs remain unchanged. **24 additional vanilla ground species; 34 total explicitly declared host profiles.**
- Common original-mob logic reuses single UUID ownership, NoAI/restoration, host-eye camera/attack/LOS, permanent BD/mastery, save data, lockout, multiplayer ownership checks and movement. No copy-spawning or fake substitute mobs.
- Added native and mastery100 transferred environmental jump profiles (rabbit, goat, horse, donkey, mule, frog), conserves stronger native slime impulse, and treats cave spider as native spider wall-climber with mastery100 transfer. No ability equipment slots or new currency.
- Existing tamed `TamableAnimal` protection extended to **tamed `AbstractHorse`**. Hosts already being ridden or themselves riding another entity remain barred, as before. Villagers, absent scripts, tamed animals and externally owned bodies remain barred.
- The **24 new entries are generic walking/jumping/basic melee** support, NOT their entire native AI, special abilities, riding inventory, boss phases or combat spell mechanics. In particular the ravager roar/stun, piglin ranged actions, fox target behavior and underwater turtle/frog locomotion are not yet fully adapted. Certain mobs/animations may fail to feel good as player-controlled hosts until actual playtests.
- Non-vanilla entities are **not registered** in this catalog yet. Modpack selections stay fixed at 9 gameplay/QoL mods +6 libraries and their external-pack server smoke was previously run separately. Schema is namespace-aware; adding a known third-party EntityType still requires verified compatibility, valid custom AI/animations, hitboxes and boss-state transitions. The combined class mod + 15 dependencies and actual external boss/host interaction **NOT TESTED**.
- Deterministic verifier passed for all 34 explicit profiles, 24 additions, unsupported ranged/flying/external boss exclusions, 99↔100 jump inheritance, preexisting guardian/enderman/slime/creeper ability gates, save data and cooldown restore. Existing class management / graph skill tree / puppeteer checks passed, and JAR verification passed in the same workflow.
- **CLIENT RUNTIME / ACTUAL POSSESSION OF ALL 34 / MOUNT & MOVEMENT PHYSICS / VISUAL QA / EXTERNAL CO-LOAD / MULTIPLAYER = NOT TESTED.** BUILD VERIFIED does not claim in-game quality. Keep expanding body-type-specific adapters and testing before treating all native species skills as complete.


---

> 아래는 이전 단계의 10종 지원 상태를 기록한 것이다. 최신 지원 범위는 위 34종 카탈로그가 우선한다.

## 2026-11-16 current integrated gameplay: guardian + enderman + camera controls

- **Source commit:** [`b976a5be0db80680f57683745a2f05ac3617a0c7`](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/b976a5be0db80680f57683745a2f05ac3617a0c7).
- [**Build anomalous-classes #43729597678**](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43729597678) **SUCCESS / BUILD VERIFIED / JAR PRODUCED**. JAR `anomalous-classes-0.1.0-alpha.1.jar` **32,053,032 bytes / 185 classes**. [Deliverables](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43729597678/artifacts/14167608277).
- Original vanilla host adapters now **10 supported species**: pig, chicken, sheep, cow, zombie, spider, slime, creeper, **guardian**, **enderman**. Warden and external/modded boss adapters are still unsupported.
- **Guardian:** trial BD requirement **600**, base hold intrusion **160 ticks**. Guardian host has innate water adaptation and aim-directed 14-block instant focus beam (base 7 damage, normal server combat ownership, block LOS and owned/team/PvP protection); at guardian mastery100, other hosts gain water adaptation and the beam. Swimming uses host rotation pitch, water-only ascent and ordinary water oxygen refill for both the host and its passenger. **Slot 2** (`V` + `2`) triggers guardian beam, separate 480-tick cooldown and 60-tick offensive shared recovery. Invalid beam targets do not consume cooldown. This is a candidate balance and basic audiovisual effect, not validated polished final beam animation.
- **Enderman:** trial BD requirement **900**, base hold intrusion **160 ticks**. Original host can use its short controlled teleport without prior mastery; at enderman mastery100, other hosts inherit active teleport and automatic projectile-only evasions. **Slot 3** (`V` + `3`) requests a forward up-to-8-block blink, only to loaded, grounded, collision-free, nonfluid positions within world borders; otherwise no cooldown spent. Separate 560-tick cooldown, 0 offensive shared recovery. When a real projectile hits an inhabited host, the server applies a **35% conditional attempt** with projectile-UUID dedup and a 12-tick dodge guard, then cancels the hit **only on a safe successful relocation**. Ordinary melee, magic and waves are not covered. These numerical constraints remain playtest candidates, not exact reproductions of native Minecraft Enderman odds.
- **Camera/input correction:** Minecraft mouse rotates the active camera entity. After initial view transfer, the active host's mouse yaw/pitch are copied to LocalPlayer before sending authoritative movement/look input; previously copying player yaw into the host every tick could undo mouse movement. The original player's server-authoritative inventory, combat and data remain unchanged.
- **Persistent trait behavior:** V controls direct slots only. Slot1 creeper, slot2 guardian, slot3 enderman; slots4–10 have no implemented active. Environmental guardian adaptation and conditional Enderman projectile evasion are independent of the V toggle. All three abilities share existing per-player persistent cooldown IDs and global offensive recovery. Creature mastery remains the source of permanent acquisition; no new currency, equip restriction or skill tree replacement.
- **Verification:** `verifyParasiteFoundation` PASS (guardian/enderman BD, slime/guardian/enderman 99↔100 mastery policies, guardian mastery NBT reload, 3 actives, individual/global cooldown and saved cooldown reload). `verifyPuppeteerFoundation`, `verifyGraphSkillTreeRuntime`, existing class/UI layout checks and packaged JAR PASS.
- **Explicitly NOT TESTED:** actual Minecraft client, mouse rotation after camera takeover, Guardian underwater pitch/mob physics, blast+beam native visuals and hit feeling, Enderman passenger teleport with loaded chunk boundaries, projectiles from mods, vanilla block mining and PvP, multiplayer latency. Do not equate build success with these outcomes.
- Next implementation/QA: stabilize real-client camera/attack/interaction behavior and in-game species ability animations; then expand higher-tier species and earned-ability UI without adding unnecessary menus.


---

> 아래는 2026-11-15 단계별 구현 당시의 기록이다. 현재 상태는 위 2026-11-16 통합 체크포인트가 우선한다.

## 기준

- Repository: `q93503128-a11y/minecraft-java-mod-builds`, branch `main`
- Project: `projects/anomalous-classes/`
- Build target: Minecraft 1.21.1 / NeoForge 21.1.244 / Java 21
- Final gameplay commit: `3b8cd95f8c99ef5c218c9a27dfce3344f991bbe7`
- Build run: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43722781049
- Deliverables artifact: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43722781049/artifacts/14165620402
- **BUILD VERIFIED / JAR PRODUCED**, Gradle + existing class checks PASS; JAR 32,034,081 bytes / 180 classes.
- Actual CLIENT RUNTIME / PLAYTEST / MULTIPLAYER: **NOT TESTED**. Headless CI is not gameplay approval.

## 서버 연결 현황

- 실제 원본 숙주 UUID 단독 점유, 인간형 약점, BD 최소치/시야 유지 침투, 종 숙련, 숙주 사망 후 인간형 배출·재기생 제한, 전직 복구.
- 기생 가능 초안: 돼지·닭·양 BD0, 소20, 좀비100, 거미160, **크리퍼240**. 외부 모드 몹·가디언·워든은 아직 조작 어댑터 미지원.
- R 유지 침투/대상 전환, 안전 자발적 이탈 후보 처리; Z 침투 고정, X 생체 수복, C 본능 각성.
- 이동/점프/달리기 기반 실제 숙주 이동, 돼지 짧은 돌진, 거미 상시 벽 타기(거미 숙련100 이후 타 종으로 자동 전승).
- BD는 숙주 활동, 탐험 최초 생물군계, 실제 적대 전투 기여, 철·다이아 채굴/작물 수확/심층 채굴 최초 성취에 의해 수급. 반복 최초 보상은 저장 데이터로 차단.
- 재접속 직후 숙주 청크 미로드 상태라 해서 단독 점유 기록을 곧바로 버리지 않음. 웅크리기 즉시 이탈 우회 방어.

## 이번 계승 직접 기술 입력 계약

- V 토글 **계승 기술 모드 ON/OFF**. ON이면 물리 숫자키 1~9/0이 계승 기술 1~10, OFF는 바닐라 핫바 조작. UI 중에는 숫자키 차단 없음.
- 숫자키가 발동하는 효과와 쿨다운은 **서버가 현재 직업/점유 숙주/종 숙련/재사용 대기/아군/PvP 설정** 검증. 임의 클라이언트 발동값은 신뢰하지 않음.
- 사용 가능한 직접 기술 **슬롯1=크리퍼 생체 폭발**만 구현. 크리퍼 숙주에서는 선천 기술, 다른 숙주에서 사용하려면 **크리퍼 종 숙련도100**. 기존 종 숙련 NBT가 영구 해금의 정본이며 별도 도감 화폐 없음.
- 크리퍼 숙주의 폭발은 실제 숙주 사망·기존 인간형 배출/사망 후 재기생 제한을 적용. 다른 숙주의 계승 폭발은 지형 파괴 없는 근접 광역 피해+숙주 최대 HP 약12% 반동. 시전 후 시각/음향 효과 포함(시각 품질 미검수).
- 후보 수치: 크리퍼 BD 요구 240, 침투 120tick, 폭발 반경 3.5, 종 기술 30초 재사용 대기+공용 공격형 기술 3초 회복. 초안이며 사용자 확정 밸런스라고 취급하지 않음.
- 기술/공용 재사용 대기와 잠금 상태는 UUID+안정적 기술 ID에 연결되어 SavedData 저장/로드. 기술 슬롯은 소유 능력 제한이 아니며, 현재 나머지 슬롯은 미구현으로 안내됨.
- **실제 조작 키 설정 명칭이 정신 지배자 V 기능과 공유되는 점**, 물리 숫자키 별도 재바인딩 UX, 11개 이상 직접 기술의 페이지/편집 경로, HUD는 후속 클라이언트 UX 작업 범위.

## 검증·주의점

- CI #43722644221 첫 시도: NeoForge `SoundEvents.GENERIC_EXPLODE` Holder 참조를 SoundEvent로 직접 전달해 컴파일 실패.
- 수정 `3b8cd95`: `SoundEvents.GENERIC_EXPLODE.value()` 사용. 후속 CI #43722781049 **SUCCESS**, 기생술사 `ParasiteFoundationVerifier`에서 BD·소유권·생존 성취·Z/X/C·계승 쿨다운 및 NBT 왕복 PASS. 인형술사/공용 그래프 검증도 포함.
- Mixin 런타임 숫자키 차단, 멀티플레이 패킷 경합, 실제 카메라/숙주 히트박스, 폭발 시 플레이어·몹 피해/드롭, 숙주 사망 장소, 거미 벽 타기 체감, 모드 키 충돌은 **실제 Minecraft 미검증**. JAR 빌드 통과만으로 게임 완료라 하지 않음.
- 기생 술사 고유 조작의 본질적인 남은 일: 진짜 숙주 시점/원본 플레이어 모델 중첩 문제, 바닐라 생존 상호작용/숙주 고유 이동·전투 완성, 직업 성장 숙련 성취 다양화, 신규 종 어댑터, 고품질 HUD/애니메이션/VFX/사운드 검수.
- 인형술사와 정신 지배자 코드 및 기존 6직업 공통 스킬트리/직업변경 시스템을 유지한다.

## 간단한 실플레이 경로 (사용자가 직접 확인할 때)

1. 해당 JAR을 정확한 Minecraft 1.21.1 / NeoForge 환경에 설치, 월드 진입 후 기생술사 직업 선택.
2. `/summon minecraft:pig ~2 ~ ~` 후 R을 유지해 실제 원본 돼지 숙주 기생. 방향키/점프/달리기 입력으로 조작.
3. 숙주 상태에서 V를 켜고 숫자 1, 이후 V OFF 숫자1로 기존 핫바가 정상 동작하는지 확인. 크리퍼를 숙주로 얻기 전에는 숫자1 폭발이 시전되면 안 됨.
4. 자연스러운 BD를 240 이상 축적한 뒤 `/summon minecraft:creeper ~2 ~ ~`로 크리퍼 기생 시험. 크리퍼 숙주에서 V→1 폭발→사망·인간형 배출·재기생 잠금 확인.
5. 실제 유효 활동으로 크리퍼 종 숙련100 뒤 다른 종 숙주에서 V→1을 누르면 숙주 HP 반동을 받는 지형 비파괴 폭발이 발동해야 함.
6. 월드 저장/재접속, 서버 PvP=false/true, 다른 플레이어 숙주 소유권을 별도로 검수. **위 순서는 테스트 계획이며 실제 시행 결과가 아님.**

## 2026-11-15 후속 구현 — 원본 숙주 시점·서버 근접 공격

- Commit `29eacf9025584d4b8f8f79c9aed203acc497cc5f`; `main` 통합.
- [CI #43725526476](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43725526476) **SUCCESS / BUILD VERIFIED / JAR PRODUCED**, JAR 32,044,062 bytes/184 classes, [JAR artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43725526476/artifacts/14165683534).
- `ParasiteHostViewPayload` broadcasts the authentic host entity ID+UUID to its player only; `ParasiteViewClient` only changes Minecraft camera after client confirms matching mounted entity. View is restored when the server ends possession or when local world is unloaded. Local player's redundant mounted renderer is suppressed; other clients' rendering not yet adapted.
- `ParasiteHostAttackPayload` carries **no target ID and no damage value**. `ParasiteHostCombatService` raycasts living entity/blocks from the original host's eye, enforces valid allied/PvP/ownership target, player attack strength/cooldown and throttled intents, and then calls vanilla `ServerPlayer.attack` for item attribution. Server cancels an alternative vanilla attacker packet while a host is controlled.
- Intrusion raycast now starts from current host eye when already inhabited; host rotation includes player pitch. Host release restores old-dimension Mob if already loaded and releases claim safely when dimension changes; it never forcibly loads remote chunks.
- Passenger incoming damage redirects to real host without charging the player a second hit if the host is temporarily immune.
- **NOT PLAYTESTED**: camera mouse-look/interpolation and first/third-person collision, world chunk/portal transfer, player-body visibility for remote multiplayer clients, vanilla mining/block range alignment, physical swings and attack feel, PvP modes, real entity drops and TPS profiling.
- The UI/model/animation polish remains under Codex per [UI ownership](./ui/WORK_OWNERSHIP_AND_UI_HANDOFF_2026-11-13.md); gameplay authority and preservation remain in the server modules.


## 2026-11-15 subsequent gameplay checkpoint — host look + slime

- Main game commit: `01d50f7d62eae97d01199e0f8dc5ad7533611a3a`.
- [Build CI #43727647235](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43727647235) SUCCESS; **BUILD VERIFIED / JAR PRODUCED** (JAR 32,044,952 bytes / 184 classes); [latest JAR package](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43727647235/artifacts/14166767379).
- Client look requests now carry yaw/pitch; server validates and stores direction for original host's LOS-targeted intrusion and direct melee. Vanilla block mining retains the left-click path when crosshair hits a block; host attack intents also work on non-block hits even when the passenger's vanilla raycast disagrees.
- Added slime original host support with trial BD180 and 110-tick concentration. Elastic native jump and cross-species inherited jump/fall softening at slime mastery100 are no-cooldown environmental traits. Slime passives and gates covered in server-contract fixture.
- **Runtime Minecraft QA / real client camera rotation / multiplayer not executed**. Main client and host camera interplay, entity-hit/block-hit discrimination and actual jumper physics require in-game verification; don't claim these are verified from CI alone.
