# M3 Region 01 Boss Material Review

This procedure reviews the current **field-review material candidate only**. It does not approve final Region 01 art, does not satisfy `Region01BossMaterialPreparation`, and does not authorize production encounter publication.

## Candidate under review

- runtime review texture: `riftfrontier:textures/entity/region_01/boss_dark_rock_candidate.png`
- repository path: `src/main/resources/assets/riftfrontier/textures/entity/region_01/boss_dark_rock_candidate.png`
- field-review renderer only: `Region01BossFieldReviewRenderPreview`
- accepted geometry remains the sanitized Quaternius Dragon Evolved resource recorded in `THIRD_PARTY_ASSETS.md`
- the original Dragon Evolved `Atlas` remains forbidden as a final-art shortcut

The candidate is intentionally separate from the production material gate. It exists so a human can judge UV fit, silhouette readability and attack-motion readability on the actual Dragon rig in Minecraft.

## Build checkpoint before human review

The repaired candidate resource at commit `027f51a2d6fc935dd0c91c6b29e28cfccffa0823` passed `Build Riftfrontier` workflow `34832719483`.

Deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10342792054`
- artifact digest: `sha256:c680228f75a68799ffd965b1512471a216bb0fa3f3ae962ee34337c6ead6399f`

This establishes automated build/JAR validity only. It is not a visual approval.

## Human test procedure

Use a disposable client world with the Riftfrontier test JAR.

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
```

Observe the Dragon from front-quarter, side and rear-quarter views while it idles, performs `Punch`, performs `Headbutt`, takes damage and enters death motion. Then run:

```text
/riftfrontier boss fieldtest phase2
```

Observe the same material during the arena-pressure candidate motion and its one-shot radial-pressure cue.

## Accept-for-further-authoring criteria

Record `ACCEPT FOR MATERIAL AUTHORING` only when all of the following are true in actual Minecraft rendering:

1. UV seams or stretches do not dominate the body, wings, neck, tail or face.
2. Head, wing, limb and body masses remain separable at normal combat distance.
3. The material does not erase `Punch`, `Headbutt`, HitReact or Death deformation readability.
4. The arena-pressure silhouette compression/burst remains readable rather than turning into an indistinct dark blob.
5. The candidate is not confused with Minecraft stone, the stripped source Atlas, or an already-finalized production texture.
6. The candidate remains readable under ordinary bright and dim Minecraft lighting without requiring fullbright/emissive tricks.

Acceptance means only that this direction is worth turning into a reviewed production material resource. It does **not** mean the current PNG is final.

## Reject criteria

Record `REJECT` if any of the following are visible:

- obvious UV tearing, flipped islands or severe texture swimming;
- loss of face/wing/limb silhouette at practical combat distance;
- muddy value grouping that hides telegraph motion;
- a look that reads as generic Minecraft stone, source-Atlas restoration, or unrelated visual language;
- material contrast that suggests a larger/smaller gameplay hit volume than the actual boss body;
- unreadable motion in dim Region 01 conditions.

If rejected, preserve the geometry/rig and source-animation decisions. Replace only the material direction that failed review; do not restart model search or weaken the production publication gates.

## Verification vocabulary

Until a human completes this procedure:

- `CODE REVIEWED`: may be YES after code review
- `TESTED`: may be YES after automated tests
- `BUILD VERIFIED`: may be YES after successful clean workflow
- `JAR PRODUCED`: may be YES after executable-JAR artifact verification
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `MATERIAL VISUALLY ACCEPTED`: NO

Automated Xvfb/client smoke cannot promote the last three states.
