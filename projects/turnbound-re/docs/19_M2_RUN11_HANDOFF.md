# TURNBOUND: RE — M2 Run11 Handoff

Date: 2026-09-07 KST
Status: **M2 IN PROGRESS**
Design gate: **UNCHANGED / GATED** for final UI, character appearance, VFX, and world design.

## Verified code baseline

- Verified code commit: `ba052ea7c49634a5c7edb8dd19f061716ea1c3ca`
- Version: `0.1.0-alpha.1`
- GitHub Actions workflow: `Build turnbound-re`
- Final verification run: `34066477016` — **PASS**
- Java: 25.0.4.1
- Gradle: 9.2.1
- NeoForge: `26.2.0.38-beta`
- `dependencies clean build --stacktrace`: **PASS**
- JUnit: **PASS**
- production JAR ZIP/metadata/class/assets/data/source-dev-path/duplicate-entry verify: **PASS**
- JAR SHA-256: `09a3a26cee07f9ba711811aa83947ebcd0c2265f9120a6b126e00280e3f397dd`
- deliverable artifact ID: `9999098612`
- deliverable ZIP SHA-256: `f1ad2f589653a03f0450aaba4fec5d8620a0f0934a1b173ab892b3d2c98fad61`
- GameTest: NOT RUN
- dedicated server smoke: NOT RUN
- client smoke: NOT RUN

A first network-contract CI run exposed a reversed `EntityParticipantBinding` constructor order in the new test fixture. It was corrected without weakening tests; the final run above is the authoritative PASS.

## Completed in Run11

### M2 network wire contracts

Added NeoForge 26.2 play-phase payload registration:

- C2S `battle_command`
- S2C `battle_snapshot`
- S2C `battle_events`

The command payload carries:

- `battleId`
- `expectedRevision`
- `actorId`
- `actionId`
- stable `commandId`
- `targetIds`

Snapshot carries authoritative battle revision/state/cycle/current actor and participant HP/Poise/Energy/Guard/EXPOSED/POISE_GUARD/alive state. Event payload carries event batches and resulting revision. Common/server code imports no client rendering/UI class.

### Server-authoritative C2S command path

`BattleNetworkGateway` now validates before mutation:

1. battle exists
2. sender entity is bound to requested participant
3. actor ownership matches
4. `expectedRevision` equals authoritative revision
5. actor is the current command-window actor
6. persistent strict command gate exists
7. action is resolvable without fabricating data
8. existing M1 `BattleCommandService` performs target/action/replay validation and core submission

Accepted universal `basic` / `guard` commands now mutate the real `BattleInstance` only through the existing M1 strict command service. The server returns the newly emitted event slice plus an authoritative full snapshot. Rejections are mutation-free and return an authoritative snapshot for resync.

### Persistent replay protection

`BattleManager` can now register network-enabled battles with a persistent `BattleCommandService`. This preserves consumed command IDs across packets. Cleanup removes the command service together with battle/entity bindings.

Tests prove:

- command wire round-trip keeps command identity and targets
- foreign sender rejection is mutation-free
- stale revision rejection is mutation-free
- bound/current player `basic` command increments revision and Energy through the strict service
- replaying the same command ID is rejected without a second mutation
- unknown data-defined actions are rejected without inventing definitions
- snapshot/event payloads are generated from authoritative state

## Deliberately not fabricated

Skill/Burst definitions and eligibility are **not hardcoded in the network adapter**. Current network mutation support is limited to universal `basic` and `guard`. Skill/Burst must be resolved from the canonical data/definition registry with ownership/cooldown/status/target policy before being accepted.

No final HUD, character art/model, VFX, or world visual work was produced. The project design gate remains intact.

## Exact next work from latest main

Continue M2 in backlog order:

1. Add canonical data-action resolver for Skill/Burst using `DefinitionRegistry`/loaded action data and produce the corresponding `ActionUsePolicy`; keep every unresolved/invalid action mutation-free.
2. Add DEBUG_ONLY network inspection consumer/state view for snapshot + event batches; do not create production UI.
3. Add/strengthen disconnect, entity-removal, dimension-change, and terminal battle cleanup guards; verify command service + entity bindings are released together.
4. Build a real DEBUG_ONLY encounter path using the network-enabled `BattleManager.register(..., participants)` overload.
5. Run available runtime/static checks, then actual client/debug encounter repetition target 20x with orphan battle count = 0 before declaring M2 PASS.

Important registration rule: any encounter expected to accept C2S player commands must use the network-enabled BattleManager registration overload that supplies the canonical participant list, otherwise the gateway intentionally returns `STRICT_GATE_NOT_REGISTERED` rather than bypassing M1 validation.
