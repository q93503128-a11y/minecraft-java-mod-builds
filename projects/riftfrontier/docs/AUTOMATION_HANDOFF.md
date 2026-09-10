# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `91acd193dfc13444dbf61440ce99c86766500b91`.
- Implementation/test HEAD completed and fully validated in this batch: `c89eec410711d312c862804ec2e64c3f1ce56f8b`.
- Production Region 01 still has no approved final boss material treatment and no legitimate production numeric `attack_pattern` / final `boss_profile`.
- Production boss semantic creation is UUID-qualified. UUID-less `BossPresentationSemanticState` compatibility helpers are fixture/test convenience only; the actor-identity subthread is closed unless a real production consumer appears.

## Completed in this batch

M3 player-combat composition kernel:

- Audited the handoff-requested boss semantic creation/clear path first. Production server binding uses `(entityId, UUID)` and the wire payload carries UUID, so no compatibility-only API was removed merely for uniformity.
- Moved to the next objective M3 gap required by the roadmap: player `Weapon Family + module composition` semantics.
- Added data-driven `weapon_family` and `weapon_module` core definitions instead of weapon-specific Java subclass proliferation.
- `weapon_family` composes authored `attack_pattern` moves, combat-role semantics, and declared module sockets. It does not create a second combat clock.
- `weapon_module` composes compatible weapon-family ids, one socket, and behaviour-change semantics. Exact stat deltas are intentionally outside this gate.
- Extended strict JSON decoding and graph validation. Families must have resolvable moves/roles/sockets; modules must resolve families, use sockets actually declared by every compatible family, and change at least one behaviour.
- Added positive and fail-closed JUnit coverage for decoding, missing move references, unresolved family references, incompatible sockets, and empty semantic components.
- Added `docs/M3_PLAYER_COMBAT_KERNEL.md` defining the production gate. Fixture ids/timings are explicitly non-production and must not be promoted.
- No final weapon family, damage, range, cooldown, hitbox, art, VFX, sound, or field-balance value was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/content/CoreDefinition.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentDocumentCodec.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentValidator.java`
- `src/test/java/kr/moonseungjun/riftfrontier/content/CombatDefinitionTest.java`
- `docs/M3_PLAYER_COMBAT_KERNEL.md`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- First `Build Riftfrontier` run `34439674920` for interim HEAD `9b0f989c888896a7268da4f8469f9c2087a4c9a7`: FAILED at `Tests and clean build` before later gates. Downloaded workflow logs identified the first actual compiler error: a lambda parameter named `definition` shadowed the outer `definition` variable inside `ContentValidator.validate()`.
- Fixed only that Java name collision; no feature/test/content requirement was removed or weakened.
- `Build Riftfrontier` run `34439967460` for implementation/test HEAD `c89eec410711d312c862804ec2e64c3f1ce56f8b`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload, and logs/report upload.
- Local clone/build: NOT RUN successfully in this automation environment; GitHub Actions is the validation source.
- Production weapon families/modules and Minecraft player weapon execution: NOT AUTHORED / NOT TESTED.
- Player damage/range/cadence/hitboxes, animation/VFX/sound, final models/textures/icons/UI, and human field-play: NOT AUTHORED / NOT TESTED.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source Dragon `Atlas` remains prohibited.
- Production Region 01 numeric boss `attack_pattern`/final `boss_profile`, actual boss encounter attachment, final dimensions/hitbox/scale, VFX/sound, and spawned/deformed Dragon graphical capture: NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` window review, exact reload/geometry provenance, reviewed-animation preparation, boss semantic schema/validator, material/publication provenance, server-semantic ↔ reviewed-animation join, server-semantic presentation-resolver provenance closure, validated boss runtime, network ordering, UUID-safe lifecycle pruning, and UUID-qualified cache/render lookup are DONE.
- Do not restore numeric-id-only boss presentation lookup/render APIs.
- Do not modify UUID-less fixture compatibility helpers unless an actual production consumer is found.
- The `weapon_family` / `weapon_module` schema, strict decoder, move/family/socket graph validation, and their regression tests are DONE. Do not replace them with duplicated weapon subclasses or a second attack timing system.
- `AttackPattern` remains the authoritative telegraph/ACTIVE/recovery cadence primitive for authored weapon moves.
- Never promote fixture weapon ids/timings to production, infer combat balance from third-party animations, or invent boss/player art or balance before their evidence gates.

## Exact next start point

1. Re-check current remote `main`, canonical docs, and this handoff. Do not redo boss provenance/order/lifecycle/actor-identity work or the player-combat schema kernel.
2. Re-check whether approved final Region 01 boss material or legitimate production boss `attack_pattern` / `boss_profile` has appeared. If so, use the already-completed boss gates instead of inventing replacements.
3. If those remain absent, continue M3 player combat by producing a bounded `M3_PLAYER_WEAPON_REFERENCE_DOSSIER`: compare strong first/third-person action-combat weapon-family references and extract only structural lessons about commitment, reach/mobility, counterplay, recovery, and module-driven behaviour changes.
4. From that dossier, lock exactly two genuinely different production weapon-family role contracts and one module-composition line before authoring any production `attack_pattern`, `weapon_family`, or `weapon_module` JSON.
5. Do not assign final damage/range/cooldown/hitbox values or final art at that stage. Those remain field-play / presentation evidence gates. After semantic roles are defensible, connect the authored definitions to server-authoritative Minecraft player combat runtime and only then proceed toward executable moves and field-play tuning.
