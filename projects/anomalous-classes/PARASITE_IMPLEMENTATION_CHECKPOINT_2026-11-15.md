# 기생술사 — 2026-11-15 구현·검증 체크포인트

> 최신 GitHub `main` + 실제 코드가 정본이며, 이 기록은 2026-11-15 기준 확인 상태다. 기획을 새로 설계하지 않는다.

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
