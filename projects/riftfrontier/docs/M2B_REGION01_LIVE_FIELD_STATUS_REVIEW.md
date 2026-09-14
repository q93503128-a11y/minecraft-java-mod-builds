# M2-B Region 01 Live Field Status Review

This is a human field-play checklist for implementation commit `54f070f1937e861f4aabc896b8803fa81aa1aae1`.

The feature under review is a **temporary Minecraft-native actionbar projection**, not approved final Riftfrontier HUD/UI language. It owns no objective, combat, reward, extraction or persistence state.

## Build under test

Use the `riftfrontier-0.1.0-alpha.1-deliverables` artifact from `Build Riftfrontier` workflow `34849271056`.

- artifact id: `10349801150`
- expected archive digest: `sha256:19b8defdaf00f3f9b2b6c1a2fc40f18cc35ca20ce76ff67abc4897abac41d65c`

Automation for this SHA passed asset intake, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke and executable-JAR inspection. These results **do not** count as human play or multiplayer testing.

## Exact setup

1. Install the checkpoint JAR on the normal Minecraft 26.2 + NeoForge 26.2.0.38-beta Riftfrontier test profile.
2. Use a disposable fresh world or an existing disposable world whose expedition state is understood.
3. Enter Region 01 through the normal hub lodestone. Do not use commands to fake expedition state unless reproducing a failure.
4. Optionally run `/riftfrontier expedition status` before combat and record the active sequence, recovered resources and pressure.

## Main observation

During a deployed Region 01 expedition, stop interacting with salvage for several seconds and fight the patrol normally.

Expected:

- the actionbar continues to show current salvage progress and current live patrol threat count while combat is happening;
- after a tracked patrol enemy actually dies, the displayed threat count falls within approximately one second (20 server game ticks) without requiring another salvage click;
- killing a non-Riftfrontier/untracked mob must not reduce the patrol count;
- luring a still-living tracked threat away from the technical cell must not make the count drop to zero, because run ownership is not position-based;
- the actionbar must not change damage, attack timing, target selection, rewards or extraction eligibility.

## Salvage interaction consistency

1. Right-click one Region 01 salvage node.
2. Expected: the immediate actionbar update and the following periodic refresh agree on the same salvage count and patrol count.
3. Repeat until the objective reaches `3/3`.
4. Expected: no stale actionbar line reverts to an older salvage count after the periodic refresh.
5. Compare against `/riftfrontier expedition status` when useful. Any number mismatch is a failure.

## Extraction / terminal-state checks

1. Complete the three-salvage objective and extract through the field lodestone.
2. Expected: after authoritative extraction, the periodic field projection stops because there is no deployed Region 01 run.
3. Expected: the existing extraction-complete/hub feedback can remain visible normally; it must not be overwritten every second by stale field data.
4. Start another expedition and confirm the new run begins at its own authoritative salvage/threat state rather than inheriting the prior run's display.
5. Repeat an abort/death/logout test if practical. Once the run becomes terminal, stale field status must stop.

## Readability checks

Record whether the one-second cadence is useful without becoming distracting during melee/ranged combat.

Reject or request adjustment if any of these occur:

- actionbar flicker visibly interferes with other critical Minecraft feedback;
- updates are so frequent that messages cannot be read;
- updates are so slow that patrol kills feel disconnected from the displayed count;
- a stale count persists after a tracked threat dies;
- the actionbar masks boss-review information in a way that materially harms combat readability;
- English or Korean localization truncates or becomes confusing at the actual test resolution/UI scale.

Do **not** automatically tune the cadence or wording from automation alone. Record the actual observation first.

## Multiplayer check — only when actually performed

For a real multiplayer session, verify the owner player's actionbar reflects the authoritative run and no other connected player receives a false owned-run projection. Do not mark this passed from dedicated-server smoke alone.

## Result labels

Record separately:

- `CODE REVIEWED`
- `TESTED`
- `BUILD VERIFIED`
- `JAR PRODUCED`
- `PLAYTESTED`
- `MULTIPLAYER TESTED`

For the automated checkpoint itself the first four are `YES`; the last two remain `NO` until humans actually perform the corresponding sessions.
