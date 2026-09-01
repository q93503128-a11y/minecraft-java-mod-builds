# TITANBREAK alpha.48 — B07 Storm Leviathan

B07 follows the content-bible Storm Leviathan specification: a T4 aerial roaming boss with 17,000 visible HP, TR55, four destructible wing membranes, six electric sacs, a head sensor, and a deep storm organ.

Gameplay structure:
- P1 high-altitude orbit and dive pressure.
- P2 begins after major wing damage or 72% HP and establishes a storm field.
- P3 begins after four electric sacs are destroyed or 38% HP and descends into a lower-altitude electric-field fight.
- Destroyed wing membranes reduce dive speed and control.
- Destroyed electric sacs reduce field radius and attack frequency.
- Destroying the head sensor adds prediction error to guided attacks.
- The storm organ is only targetable in P3 after at least four electric sacs are destroyed; destroying it ends the fight.

Core attacks implemented: dive strike, chain lightning, wind pressure, delayed electric-orb zones, guided lightning zones, and storm-field pulses. Delayed zones expose server-side electric telegraphs before damage.

Progression/reward contract:
- Requires the Chronophage first kill for natural encounter progression.
- Drops `leviathan_storm_organ` x1, `capacitor_stack` x4–6, `radiation_core` x1.
- First kill records `storm_leviathan`, grants 780 Research Data and 6 bonus Adaptation Points.
- The existing `propulsion_legs` recipe consumes `leviathan_storm_organ`, so B07 naturally gates the high-output propulsion route without adding a duplicate unlock system.

Presentation uses a dedicated 130–160 block-class flying geometry, multipart visibility changes, phase field bones, and GeckoLib animation. The current texture source remains project-owned and is tracked in `ASSET_REGISTRY.md`.
