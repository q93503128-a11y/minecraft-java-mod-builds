# R01 Asset Intake — Phase-B Pass 5 Motion Evidence

> Date: 2026-09-17  
> Scope: unresolved teammate revive/help-up and Trail Stag mount/dismount motion sources  
> Authority: `R01_ASSET_INTAKE.md` remains the intake manifest; this file records new evidence only.  
> Result: **NO FALSE CLOSURE — both motion gaps remain `NEEDS_EXTERNAL_CLIP`**

This pass deliberately searched only the two R01 motion gaps that remained unresolved after Pass 4. The goal was not to collect more generic animation libraries. The goal was to find an exact, legally usable, retargetable motion source that could survive Minecraft-scale review.

---

# 1. Quaternius Universal Animation Library recheck

Creator-uploaded OpenGameArt snapshots remain strong public-safe animation sources:

- Universal Animation Library — 120+ animation family, Standard download, universal humanoid rig, CC0;
- Universal Animation Library 2 — 130+ animation family, universal humanoid rig, CC0.

Sources:

- https://opengameart.org/content/universal-animation-library
- https://opengameart.org/content/universal-animation-library-2

The public descriptions confirm broad locomotion/combat/swimming/climbing/work coverage, but this pass did **not** find authoritative public evidence for exact clips named or clearly described as:

```text
teammate revive
help up another character
horse / stag mount
horse / stag dismount
```

Therefore:

```text
UAL revive/help-up = NOT ACCEPTED
UAL mount/dismount = NOT ACCEPTED
```

UAL remains the preferred public-safe rig/motion base for composing or retargeting a future accepted solution.

---

# 2. Mixamo license boundary and fallback value

Adobe's current public Mixamo FAQ states that Mixamo is available free with an Adobe ID and that characters/animations may be used royalty-free in personal, commercial and non-profit projects, including video games.

Adobe's licensing FAQ/community guidance separately makes the important redistribution boundary explicit: a game/product may incorporate Mixamo content, but raw character/animation files may not be redistributed as an asset package or free raw-file distribution.

Sources:

- https://helpx.adobe.com/creative-cloud/faq/mixamo-faq.html
- https://community.adobe.com/questions-696/mixamo-faq-licensing-royalties-ownership-eula-and-tos-589400

Project classification if an exact Mixamo clip is later selected:

```text
private playable build: potentially usable
public GitHub raw FBX/animation bytes: NO
intake state: READY_LOCAL_ONLY only after exact clip is verified/downloaded under the current Adobe terms
```

A third-party indexed Mixamo catalog exposes relevant motion ideas such as:

- `Standing Up From A Kneeling Position`;
- `Patting On The Back`;
- `Helping Someone Into A Vehicle`;
- various carrying/injured/get-up motions.

However this is only catalog evidence. None of those names alone proves a good two-character revive/help-up pair, and the exact clip must be inspected directly in Mixamo before adoption.

Mixamo therefore becomes a **credible local-only fallback source**, not an accepted R01 binding yet.

---

# 3. Horse-Riding-Simulator source check

Repository checked:

`Lakshman-YT/Horse-Riding-Simulator`

The public README describes:

- animation-driven mounting/dismounting;
- modular rider/horse architecture;
- animations created in Blender;
- personal/commercial use allowed;
- redistribution prohibited.

Repository:

- https://github.com/Lakshman-YT/Horse-Riding-Simulator

Important intake result:

The current public repository contains only the README and images. It does **not** expose the actual animation/project source files needed for direct intake.

Therefore:

```text
motion-design reference: useful
exact adoptable clip from current public repository: unavailable
status: REFERENCE_ONLY
```

Do not promote this to `READY_LOCAL_ONLY` unless the actual source package is legitimately obtained from the creator under the stated terms.

---

# 4. Paid/proprietary horse-animation fallback

A current marketplace package, `Horse&Man V01`, explicitly advertises horse + rider animations including Mounting and Dismount.

This proves that a paired mount/dismount solution is commercially available, but it is not a free/public-safe source and has not been purchased or inspected by this project.

Status:

```text
possible last-resort private/local purchase candidate
NOT ADOPTED
NOT PUBLIC-REPO SAFE BY DEFAULT
```

The project should continue searching free/legally clean sources or compose a project-owned clip from a public-safe humanoid rig before spending money solely to close this gap.

---

# 5. Motion-authoring implication

For Minecraft implementation, mount/dismount must be treated as a **paired presentation + authoritative seat-state transition**, not as free root-motion authority.

Target production structure:

```text
player approaches authored mount side / attachment point
→ server validates mount interaction and reserves mount
→ accepted rider clip begins
→ at one authored sync frame, server attaches rider to the mount seat
→ clip finishes into mounted locomotion pose
```

Dismount performs the inverse with a validated safe landing position.

The external clip may provide the body motion/root trajectory, but server position, collision and seat ownership remain authoritative. This matches the project's existing motion rule for dash/dodge/work/mount actions.

A future custom composition is acceptable only if it is built from an external/public-safe motion base and then deliberately authored/retargeted for the project; `no accepted clip found` is not permission to use an instant teleport or vanilla sitting pose as finished presentation.

---

# 6. Pass-5 decisions

## Revive / help-up

```text
status: NEEDS_EXTERNAL_CLIP
```

Reason:

- no exact CC0 paired revive/help-up clip was proven in this pass;
- Mixamo is a viable local-only pool but exact motion must be previewed and selected directly;
- a single generic stand-up clip is insufficient because the helper's contact/gesture must also read correctly.

Preferred next search/order:

1. direct Mixamo preview search for two-character/helping/injured/stand-up motions;
2. direct UAL/UAL2 viewer inspection for kneel/reach/get-up components that can be composed on the same universal rig;
3. if no clean existing pair exists, author a short paired project clip using the accepted CC0 universal humanoid rig + external motion bases rather than shipping a placeholder.

## Trail Stag mount / dismount

```text
status: NEEDS_EXTERNAL_CLIP
```

Reason:

- no exact free/public-safe humanoid mount/dismount clip was proven;
- the found open repository demonstrates the architecture but does not publish the animation bytes;
- a paid proprietary pair exists but is not adopted;
- arbitrary rider snapping is below the project quality bar.

Preferred next search/order:

1. direct Mixamo/other legal motion libraries for exact mounting/dismounting body clips;
2. inspect ActionForge/UAL-compatible clips and public-safe quadruped/rider sources;
3. if necessary, compose an authored CC0-based pair around the final Trail Stag saddle/seat geometry after the mount model is accepted.

---

# 7. What this pass does NOT claim

```text
REVIVE CLIP ACCEPTED: NO
MOUNT CLIP ACCEPTED: NO
DISMOUNT CLIP ACCEPTED: NO
SOURCE ARCHIVES ACQUIRED: NO
PROJECT-LOCAL SHA-256: NO
BLOCKBENCH / RETARGET TESTED: NO
MINECRAFT VISUAL ACCEPTANCE: NO
```

This pass improves the legal/source decision tree and eliminates one weak repository candidate from direct intake. It intentionally does not change `R01 ASSET READY = NO`.