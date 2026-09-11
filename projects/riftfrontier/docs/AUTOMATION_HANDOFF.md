# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `d40955798e79104960c14a8dbdaa391c843eb9f6`.
- Previous handoff descendant `Build Riftfrontier` run `34565159298`, HEAD `d40955798e79104960c14a8dbdaa391c843eb9f6`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 retained boss-presentation published-generation authority fence:

- Code/test HEAD before handoff: `bad9579ed7110d6351d1c1781b53deda13b1b10a`.
- Confirmed a real production gap left after the exact actor/world/current-tick delivery fence: `ValidatedTickResult` retains a `ValidatedBossCombatSemantics` capability, but that semantic capability previously forgot which atomically published `ContentRuntimeSnapshot` generation produced it.
- `ValidatedBossCombatSemantics` now retains `CombatRuntimeCatalog.publishedGeneration()` when validated from a published snapshot. Detached fixture catalogs continue to carry no publication authority.
- Added `requirePublishedGeneration(long)` so retained semantic capability fails closed after any atomic content publication changes generation, or when the published runtime disappears.
- `BossPresentationDeliveryGuard` now compares the retained semantic capability against `ContentRuntime.current().generation()` immediately before network fan-out, in addition to the existing exact live entity/server-level/current-tick/semantic identity checks.
- Added pure-Java `PublishedBossPresentationGenerationTest`: generation 42 is retained/accepted, generation 43 and absent-runtime sentinel `-1` are rejected.
- Existing boss runtime generation/owner singleton/dimension/entity-leave/server-stop rules and player authority rules were not weakened or duplicated.
- No balance, hit geometry, damage/range/resource policy, controls, ItemStack provisioning, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/ValidatedBossCombatSemantics.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossPresentationDeliveryGuard.java`
- `src/test/java/kr/moonseungjun/riftfrontier/content/PublishedBossPresentationGenerationTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous baseline run `34565159298`: final `SUCCESS`.
- Current code/test `Build Riftfrontier` run `34568200760`, HEAD `bad9579ed7110d6351d1c1781b53deda13b1b10a`: `IN PROGRESS` at handoff write; checkout/Java setup succeeded and Gradle setup was in progress. Do not claim later gates successful until this run or a descendant finishes.
- Local Gradle: NOT RUN in this automation environment because the sandbox has no GitHub/network checkout path; executable validation is delegated to repository CI.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle and owner attribution work is DONE.
- Boss presentation semantic capability and network payload plumbing are DONE; do not expand presentation without legitimate approved input.
- Boss presentation delivery revalidates exact live server entity/world/current-tick context immediately before fan-out; preserve it.
- Retained boss presentation semantics are now published-generation-bound; never allow a pre-reload semantic capability to fan out after a newer atomic content generation becomes authoritative.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Production player combat is authenticated `ServerPlayer`-only by reachability; player sessions remain published-generation + exact actor-instance + exact server-level instance + dimension + loadout scoped and combat-eligibility gated; process-local state is cleared at server stop.
- Delayed stale player callbacks must never replace or UUID-wide-clear a successor session.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, owner-context mutation gated, entity-leave/server-stop retired, and explicitly reject `ServerPlayer` ownership.
- Shared target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of `Build Riftfrontier` run `34568200760` plus any handoff descendant CI. If failed, fix the first actual failing gate without weakening authority.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing production gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat player production reachability/generation/lifecycle/reconnect/exact-instance/loadout/target/attack-clock or boss owner/generation/server-lifetime/presentation world/tick/content-generation work.
4. Audit the next demonstrable M3 server-authority handoff where a retained result/capability can cross an authoritative state change. Prefer production reachability evidence over speculative protocol infrastructure.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
