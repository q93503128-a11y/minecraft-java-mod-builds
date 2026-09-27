# Campfire Sessions

Minecraft Java 26.2 / NeoForge compact music-and-rest mod.

## v0.6 test flow
1. Remove older Campfire Sessions JARs and install only `0.6.0-alpha.1`.
2. Take the Acoustic Guitar and Wooden Chair from the Campfire Sessions creative tab.
3. Right-click the guitar and spin the rhythm-game-style song rail with the mouse wheel.
4. Confirm there are **15 bundled tracks**, with real duration and BPM shown for each selection.
5. Confirm the transport controls are visually distinct: **« PREV / ▶ PLAY or ■ STOP / NEXT » / ↻ AUTO or ↻ ONE**.
6. Start a song and confirm the progress bar begins at `0:00`, then updates current/total time.
7. Scroll rapidly several notches and confirm the rail carries momentum, then eases and snaps to a centered enlarged card without a scrollbar.
8. Close the UI while still holding the guitar:
   - `N`: next track
   - `B`: previous track
   - `R`: repeat-one / auto-next toggle
9. With a track playing, switch to third-person and confirm the guitar arm rests over the body, the opposite arm reaches toward the neck, and a subtle BPM-driven strum is visible.
10. Confirm slow songs produce calmer note-particle pulses while faster songs are livelier without becoming a particle cloud.
11. Enable repeat-one and allow a track to end; it should restart instead of advancing.
12. With repeat-one off, a finished track should advance automatically.
13. Switch away from the guitar and confirm playback stops.

## Local Custom Music
1. Launch the game once so `config/campfiresessions/music/README.txt` is created.
2. Put an OGG/Vorbis file directly in `config/campfiresessions/music/`.
3. Optionally add a same-name JSON file, for example `song.ogg` + `song.json`:
   ```json
   {
     "title": "My Song",
     "artist": "Artist",
     "bpm": 120,
     "theme": "NEON"
   }
   ```
4. Restart Minecraft. The song appears after the 15 bundled tracks in the same carousel.
5. Up to 32 local songs are loaded. Missing metadata uses filename / `Local Music` / 100 BPM / `CLEAN`.
6. These files stay on the local PC and are not added to the mod JAR.

Themes: `CLEAN`, `NEON`, `OCEAN`, `DESERT`, `ROUGH`.
The key bindings are normal Minecraft key mappings and can be rebound in Controls.
