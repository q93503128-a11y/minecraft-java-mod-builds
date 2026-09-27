# Campfire Sessions

Minecraft Java 26.2 / NeoForge music-and-rest prototype.

## v0.5 test flow
1. Remove older Campfire Sessions JARs and install only `0.5.0-alpha.1`.
2. Take the Acoustic Guitar and Wooden Chair from the Campfire Sessions creative tab.
3. Right-click the guitar and scroll the rhythm-game-style selection rail.
4. Confirm there are **10 tracks** and each selection shows its real duration and BPM.
5. Confirm the bottom controls are distinct: **PREV / PLAY-STOP / NEXT / REPEAT ONE**.
6. Start a song and confirm the progress bar and elapsed/total time update.
7. Close the UI while still holding the guitar:
   - `N`: next track
   - `B`: previous track
   - `R`: repeat-one toggle
8. With a track playing, switch to third-person and confirm the player takes a two-handed guitar-holding pose.
9. Confirm note particles pulse faster/slower according to each track BPM.
10. Enable repeat-one and allow a track to end; it should restart instead of advancing.
11. With repeat-one off, a finished track should advance automatically.
12. Switch away from the guitar and confirm playback stops.

The key bindings are normal Minecraft key mappings and can be rebound in Controls.
