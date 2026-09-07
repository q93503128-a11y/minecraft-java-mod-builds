# Region 01 Persisted Field Evidence Trail

Status: **VERIFIED BASELINE / MANUAL FIELD PLAY STILL REQUIRED**

This document records the bounded automatic evidence trail used by the M2-B Region 01 field-play gate. It extends `FIELD_PLAY_REVIEW.md`; it does not replace the manual combat/presentation review required by `QUALITY_STANDARD.md` and `ROADMAP.md`.

## Why this exists

`/riftfrontier expedition review` gives a correct read-only snapshot when a human asks for one, but a manual reviewer can forget to capture an edge and a terminal snapshot cannot reconstruct every live observation that existed immediately before cleanup.

Production expeditions therefore keep a small immutable `field_evidence` list inside their authoritative `ExpeditionRun`. It records observed lifecycle edges without deciding whether combat feels good.

This is evidence, not balancing logic. No reward, pressure, combat, extraction, ownership or failure decision is derived from the trail.

## Persisted checkpoint shape

Each `ExpeditionEvidenceCheckpoint` contains:

- stable stage;
- authoritative game time;
- recovered Region 01 salvage count;
- live run-owned threat count when the current process can prove it;
- hub salvage at that edge;
- expedition supply at that edge;
- Region 01 pressure at that edge.

Stable stages are:

```text
DEPLOYED
SALVAGE_RECOVERED
PRE_EXTRACTION
EXTRACTED
FAILED
```

Normal player-originated terminal paths record the live threat count before encounter cleanup. Restart reconciliation deliberately records `liveThreats=-1` because the new server process cannot truthfully reconstruct the previous process-local direct threat tracker.

## Automatic capture points

The production gameplay adapter records checkpoints at:

```text
successful deployment
→ every successful salvage recovery
→ accepted extraction immediately before authoritative request transition
→ successful extraction after authoritative settlement
→ player abort/death/logout failure before encounter cleanup
→ server-restart reconciliation with threat count explicitly unavailable
```

Extraction evidence has an additional atomicity contract:

```text
DEPLOYED
→ validateExtractionRequest (read-only)
   ├─ rejected: no state transition, no PRE_EXTRACTION, no SavedData write
   └─ accepted: append PRE_EXTRACTION while still DEPLOYED
                → EXTRACTION_REQUESTED
                → resolve extraction
```

This ordering matters because `PRE_EXTRACTION` means an accepted extraction boundary, not merely that the player typed an extraction command. Saves produced before this contract was hardened may contain rejected-attempt checkpoints; `FIELD_PLAY_METRICS.md` keeps conservative legacy read semantics for them.

The existing owner UUID, end reason, content fingerprint and persisted `start_context` remain the authoritative identity/tuning evidence for the same run.

## Bounded and non-blocking rule

The trail has a hard maximum of 16 checkpoints per expedition.

Diagnostic evidence is not allowed to become a gameplay failure mode:

- while the trail is full, additional non-terminal observations are ignored rather than rejecting gameplay;
- a terminal `EXTRACTED` or `FAILED` checkpoint is always retained;
- if necessary, terminal evidence evicts the oldest non-terminal observation;
- no checkpoint may be appended after a terminal checkpoint;
- checkpoint game time must remain monotonic.

This policy prevents diagnostic observation from soft-locking an expedition merely because its bounded trail filled up.

## Persistence compatibility

`field_evidence` is an optional `ExpeditionRunCodec` field with an empty-list default. Existing schema-3 saves written before this feature remain readable and are explicitly distinguishable by an empty trail.

No persistence schema bump is used for this backward-compatible optional diagnostic extension. The existing explicit migration chain remains unchanged.

## Required regression boundary

The required native `extraction_evidence_atomicity` GameTest locks the rejected-request contract at the authoritative world boundary:

- an underfilled Region 01 run remains `DEPLOYED`;
- its evidence count does not change;
- it gains no `PRE_EXTRACTION` checkpoint;
- validation does not advance `worldRevision`;
- after the required salvage is recovered, normal request/resolve still reaches `EXTRACTED`;
- the test terminally closes its own fixture so it cannot leak a non-terminal run into other required GameTests.

## Review commands

`/riftfrontier expedition review` remains the concise current/latest-run snapshot.

`/riftfrontier expedition review trail` prints the persisted checkpoints for the caller's latest owner-bound run in order. A legacy run with no stored trail reports `evidence=legacy-unavailable` instead of inventing observations.

`/riftfrontier expedition review metrics` summarizes those persisted observations without modifying the run.

These commands are read-only. They do not mutate expedition, encounter, economy or content state.

## What automation still cannot prove

A green evidence trail cannot prove:

- good spawn spacing;
- readable hunter/scout/elite roles in motion;
- fair aggro and pacing;
- fair salvage-hazard timing;
- whether fast extraction versus patrol-clear bonus is an interesting choice;
- whether death/logout/abort/restart re-entry messaging feels clear;
- production visual, animation, sound or telegraph quality.

Those remain manual Minecraft client judgments.

## Exact next point

Do not tune Region 01 numbers merely because the evidence is now structurally cleaner. Run actual low-pressure and elevated-pressure Region 01 expeditions in the Minecraft client, then use `/riftfrontier expedition review`, `review trail`, and `review metrics` to preserve the observed run context.

Exercise normal extraction, abort, death, logout and restart/re-entry. Change spawn spacing, pressure scaling, hazard timing or patrol-clear reward only when a concrete gameplay symptom is paired with persisted evidence for that run.

After this manual field-play gate is genuinely closed, prepare the M3 combat/elite/boss reference dossier before final creature art, animation, telegraph or production combat presentation work.
