# 07 — TECHNICAL ARCHITECTURE

## 1. 패키지 경계
```text
turnboundre/
  api/
  battle/
    BattleManager
    BattleInstance
    BattleState
    BattleAction
    BattleEvent
    BattleResolver
    InitiativeService
    TargetingService
    DamageService
    StatusService
    RewardService
  character/
  data/
  encounter/
  network/
  persistence/
  debug/
  client/ui/
  client/render/
  client/camera/
```
클라이언트 package를 common/server 코드에서 참조하지 않는다.

## 2. BattleManager
- active battle registry.
- participant→battle lookup.
- lifecycle creation/cleanup.
- dimension/disconnect/death 예외 처리.
- 동일 entity의 복수 battle 참가 금지.

## 3. BattleInstance
순수 상태 컨테이너에 가깝게 유지한다.
- UUID battleId.
- long seed.
- long revision.
- phase/state.
- cycle.
- participant states.
- intent states.
- event log ring buffer + optional full debug export.

## 4. Server authoritative command
클라이언트 패킷 예:
```text
BattleCommandC2S {
  battleId,
  expectedRevision,
  actorId,
  actionId,
  targetIds[]
}
```
서버는 검증 후 action을 resolve하고 revision을 증가시킨다. stale revision은 거부한다.

## 5. 서버→클라이언트
- battle start: full snapshot.
- 명령 해결: event batch + resulting revision.
- 필요 시 resync: full snapshot.
- battle end: result/reward/cleanup.
클라이언트 animation queue는 이벤트를 소비하지만 authoritative state를 변경하지 않는다.

## 6. Deterministic RNG
Battle seed와 명시적인 RNG stream을 사용한다. `Random`을 서비스 곳곳에서 새로 만들지 않는다. RNG가 필요한 event 순서가 테스트에 고정되어야 한다.

## 7. World isolation
battle participant는 battle 중:
- vanilla AI goal 실행/공격을 일시 억제.
- vanilla damage는 **피격자와 causing/source entity 양쪽**에서 battle ownership을 검사하여 차단한다.
- battle-owned player의 `AttackEntityEvent`를 선제 차단하여 외부 world entity를 vanilla melee로 공격하지 못하게 한다.
- battle-owned entity가 소유한 새 vanilla `Projectile`은 `EntityJoinLevelEvent`에서 world spawn을 차단한다.
- 외부 projectile이 battle-owned entity에 명중할 때는 `ProjectileImpactEvent` 자체를 취소하여 potion/status 등 damage 외 side effect도 우회하지 못하게 한다.
- 외부 entity 피해/knockback을 차단한다.
- despawn/removal은 battle cleanup 경계를 거친다.
- battle 종료 시 정책이 즉시 원상복구된다.

이 Minecraft combat interception 경계는 Stephen-Seo/TurnBasedMinecraftMod `AttackEventHandler`의 MIT source-side interception pattern을 실제 adaptation한다. 그러나 TURNBOUND는 일반 자연 공격으로 battle을 자동 생성하지 않으며 `CANON.md`의 visible authored Encounter 진입 규칙을 유지한다.

별도 dimension으로 순간이동하는 것은 첫 구현의 필수조건이 아니다.

## 8. Data loading
NeoForge data/resource reload 경계에서 definitions를 immutable registry snapshot으로 만든다. reload 시 모든 참조를 검증하고 오류가 있으면 어떤 `id.field`가 잘못됐는지 보고한다.

## 9. 순수 전투 엔진
Damage/Poise/turn/state transition은 가능한 한 Minecraft Entity 없이 unit test 가능한 POJO/record 입력으로 작성한다. Minecraft adapter는 entity snapshot을 core model로 변환하고 결과 event를 월드에 적용한다.

외부 턴제 구현의 integration code는 재사용할 수 있지만 `Intent + Affinity + Poise + EXPOSED`, deterministic RNG, action legality, server authority 등 게임 규칙 자체의 정본은 TURNBOUND 문서/데이터/코어다. 외부 규칙을 편의상 섞어 정본을 변경하지 않는다.

## 10. Persistence
저장 루트에는 `schemaVersion`을 둔다.
- 캐릭터 보유/레벨/별/재화.
- party formation.
- world/story personal flags.
- discovery/codex.
진행 중 battle의 완전한 영구저장은 M0 필수가 아니다. 서버 재시작 시 안전 cleanup + encounter reset을 우선한다.

## 11. Client battle camera
- battle camera는 client-only presentation이며 server battle state에 권한을 갖지 않는다.
- active `BattleClientState` snapshot이 있을 때만 개입한다.
- 현재 production base는 Cukkoo12/free-camera NeoForge 26.2의 MIT `CinematicRotationSmoother` + `CinematicMotionProfile.CINEMATIC` rotation frequency `7.0`을 vendor/adapt한 구현이다.
- battle이 끝나면 smoother state를 clear하고 vanilla camera angle을 그대로 통과시킨다.
- 외부 근거 없이 TURNBOUND 전용 camera shot/FOV/profile을 임의 설계하지 않는다. 후속 follow/orbit/collision이 필요하면 같은 external camera source의 실제 rig/resolver를 우선 검토한다.
- Free Camera 전체 mod를 runtime dependency로 요구하지 않고 필요한 source subset만 attribution/license와 함께 보존한다.

## 12. Debug 도구
최소 명령:
- `/tbre debug battle start <encounter>`
- `/tbre debug battle dump`
- `/tbre debug battle end`
- `/tbre debug give-character <id> <level> <star>`
- `/tbre debug currency ...`
- `/tbre validate-data`
실제 Brigadier 문법은 구현 시 표준에 맞추되 기능은 제공한다.

## 13. 성능 목표
턴제 전투는 tick마다 모든 participant를 전수 계산하지 않는다. 상태 변화 중심(event-driven)으로 처리하고 UI 동기화는 변화 시점에만 보낸다. profiling 전 추측 최적화 대신 battle count/packet size/tick cost를 측정한다.
