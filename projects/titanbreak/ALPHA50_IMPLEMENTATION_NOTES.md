# TITANBREAK alpha.50 implementation notes

## B09 Null Seraph
- Added the canonical T5 Null Seraph boss at 14,000 visible HP and TR70.
- Multipart targets: four suppression wings, two Null cores, and the head resonator.
- P1 selects and temporarily seals the player's highest-priority active augmentation systems.
- P2 adds sanity pressure and stronger analysis/auto-aim jamming.
- P3 collapses the suppression field and shifts the boss into fast direct lance assaults.
- Player targeting weights augmentation tier plus power/neural burden, so dense high-output builds draw priority.
- Temporary suppression never deletes or unequips an augmentation. Reflex Drive, arm abilities, Phase Step, mobility jump, Overdrive, and Combat Autopilot requests respect the runtime seal and recover automatically after expiry.
- Drops follow the content bible: Null Suppression Core x1, Resonant Neural Ganglion (M-NEU-02) x3, Calculation Core (M-COM-03) x4.
- Encounter progression chains from the first Ash Titan kill.
- Added dedicated GeckoLib geometry, animation, renderer state, texture, localization, and B09 CI.
