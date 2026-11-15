# 기생술사 — 2026-11-15 구현·검증 체크포인트

> 최신 GitHub `main` + 실제 코드가 정본이며, 이 기록은 2026-11-15 기준 확인 상태다. 기획을 새로 설계하지 않는다.

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

