# TURNBOUND: RE — 2026-09-17 integration recovery

## Observed in real client

- Minecraft 26.2 loads the migrated Drehmal save and TURNBOUND management screens.
- Party / growth / equipment surfaces are functional, but the growth and equipment detail panes overflow into their action buttons at the tested window/GUI scale.
- Equipment is a shared selectable system with one slot per character in the current vertical slice; the UI must state this directly.
- Expedition rows currently behave like a read-only journal even though players reasonably read them as actionable routes.
- The authored external-world encounter candidates were still disabled, so no visible TURNBOUND enemy group appeared on the route.
- The official Drehmal 2.2.2f resource pack still contains 1.20.1-era spawn-egg model identifiers using `spawn_egg_2D`; Minecraft 26.2 rejects uppercase path characters and renders affected content as missing textures.
- The migrated world also carries a legacy `data/random_sequences.dat` shape that Minecraft 26.2 rejects because it lacks the new `salt` field.

## Recovery contract

1. Patch the locally downloaded Drehmal resource pack in-place after preserving an original backup. Do not redistribute Drehmal assets.
2. Preserve then remove the obsolete random-sequence cache so 26.2 can regenerate it.
3. Keep the world-first encounter contract: Expedition Journal may track an authored route, but it must not launch combat from a menu.
4. Enable only the Hunter's Crypt patrol as a real-client placement gate; keep the elite ruins disabled until the migrated-world location is inspected.
5. Spawn a visible, non-combat Minecraft entity group at enabled encounter anchors while retaining server-authoritative interaction/confirmation.
6. Add a persistent Guide entry to the M menu and use Minecraft's native TutorialToast for first-session onboarding. The guide surface must use the adopted Kenney-backed UI language.
7. Fix detail-pane overflow before adding more character-management features.

## Validation gate

The first real-client pass after this recovery is expected to confirm: no spawn-egg missing-model flood, no legacy random-sequence parse error, no growth/equipment text-over-button overlap, Expedition route tracking feedback, visible patrol actors at the tracked world anchor, and successful encounter preview/start by interacting with one of those visible actors.
