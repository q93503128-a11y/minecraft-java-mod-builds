# Campfire Sessions

Status: ALPHA.6 PLAYLIST / LOCAL MUSIC / PERFORMANCE PASS

- Mod ID: campfiresessions
- Version: 0.6.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Loader: NeoForge 26.2.0.87
- Goal: a compact Minecraft-native campfire music experience with a polished song-selection rail, believable guitar performance feedback, and a safe path for private local music.

## Alpha.6 acceptance
1. Acoustic Guitar retains the Minecraft-native Musical Instruments Pack model and does not regress first-person presentation.
2. Wooden Chair retains its Minecraft-native Voxelized Furniture model, facing, and seating behavior.
3. Bundled playlist contains 15 verified CC0 tracks spanning cozy, jazz, synth/neon, ocean/ambient, desert, rough, night, playful and fantasy moods.
4. OGG duration and BPM metadata are generated at build time and verified by CI for every bundled track.
5. Song selection remains an inertial rail/carousel: continuous wheel input adds momentum, movement eases, and the selected central card snaps into focus without a scrollbar.
6. Kenney CC0 panel/card/button art remains the actual UI surface; text and functional indicators must not hide the external artwork.
7. PREV, PLAY/STOP, NEXT and REPEAT/AUTO controls are visually distinct; elapsed/total time, progress, BPM and repeat state are visible.
8. `B`, `N`, and `R` remain rebindable Minecraft key mappings and work while the guitar is held with the screen closed.
9. Note feedback follows song BPM with calmer slow-song density and more energetic fast-song density without excessive particles.
10. Third-person guitar performance uses a dedicated NeoForge custom arm pose with one arm over the guitar body, the other toward the neck, and a small BPM-driven strum. First-person transforms are left untouched.
11. `config/campfiresessions/music/` accepts up to 32 user-supplied local OGG/Vorbis tracks plus optional same-name metadata JSON. Local audio is never committed or packaged into the mod JAR.
12. Missing or invalid local music must not prevent the 15 bundled CC0 tracks from loading.
13. Local custom tracks participate in the same playlist, repeat-one, auto-next, progress, BPM feedback, key controls, and guitar pose flow.

## Scope boundary
- Campfire Sessions does not download copyrighted commercial music or bundle user local tracks.
- Local custom playback is client-local. Multiplayer synchronized positional performances remain future work.
- Visual acceptance of the custom guitar pose, carousel spacing, and themed UI still requires an in-game client playtest after build verification.
