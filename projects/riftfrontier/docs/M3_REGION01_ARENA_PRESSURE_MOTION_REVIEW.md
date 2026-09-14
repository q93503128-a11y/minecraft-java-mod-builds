# M3 — Region 01 Arena-Pressure Motion Candidate Review

This is a human field-review checklist for the temporary authored arena-pressure silhouette candidate. It is **not** production animation approval and does not close the production 9/9 semantic-animation gate.

## Candidate contract

`Region01BossArenaPressureFieldMotionCandidate` is Riftfrontier-authored presentation code. It does not rename or recycle any Dragon Evolved source clip. The accepted CC0 Dragon Evolved geometry/rig remains the visible body, while neutral reviewed `Flying_Idle` supplies only the underlying deformation. The candidate applies whole-body scale around the already-positioned actor origin from the server-synced `patternId`, `attackPhase`, and `phaseProgress`:

- TELEGRAPH: horizontal `1.00 -> 0.94`, vertical `1.00 -> 1.04` (planted inward load);
- ACTIVE: horizontal `1.12 -> 1.08`, vertical `0.94 -> 0.97` (single outward snap aligned to authoritative ACTIVE entry);
- RECOVERY: horizontal `1.08 -> 1.00`, vertical `0.97 -> 1.00` (settle only).

The candidate owns no clock, damage, hit geometry, impulse, movement, targeting, health, death, or phase state. It must remain field-review-only until a human judges the motion readable and appropriate. Production binding remains incomplete regardless of automated test/build success.

## Build / launch

Use the JAR produced by the successful `Build Riftfrontier` workflow for the checkpoint commit under review.

Run:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase2
```

Stay at a three-quarter view where both body width and height are easy to compare.

## Required observations

1. Wait for `region_01_arena_pressure` TELEGRAPH. The body should remain planted rather than lunging or travelling forward. It should visibly gather inward while rising slightly.
2. At authoritative ACTIVE entry, the body should snap wider/lower once, at the same moment as the existing radial impulse and diagnostic explosion cue.
3. During the rest of ACTIVE, the silhouette should ease slightly toward recovery rather than emitting repeated visible pulses.
4. During RECOVERY, it should settle continuously back to neutral scale without a second outward snap.
5. Repeat from several camera angles. Reject the candidate if the anisotropic scale reads as rubbery, comic, visually broken, or makes attack range appear materially different from the authoritative radial hit geometry.
6. Confirm committed strike still uses reviewed `Punch` and line displacement still uses reviewed `Headbutt`; the candidate must never appear on those patterns.
7. Damage the boss during arena pressure. Existing reaction/death priority must not create duplicate meshes or corrupt the pose stack. Death must still override the candidate.
8. Confirm the actor does not physically translate, home, change collision, alter damage timing, or create any second impulse from this presentation-only cue.

## Accept / reject evidence

Capture one clip containing a complete TELEGRAPH -> ACTIVE -> RECOVERY cycle plus the radial impulse. Record one of:

- `ACCEPT FOR SOURCE-MOTION AUTHORING`: planted radial pressure role is clearly readable and the silhouette timing is useful as a target for a real authored rig animation; or
- `REJECT`: include the camera angle and the exact readability/deformation defect.

Do **not** record `PLAYTESTED` unless a human actually performs this procedure in a normal client. Automated tests, GameTest, dedicated-server smoke and Xvfb client smoke do not count. A real two-client/server session is required for `MULTIPLAYER TESTED`.
