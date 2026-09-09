# Changelog

## 0.1.0-alpha.1

- Created a Minecraft 26.2 Fabric project skeleton.
- Added server-authoritative per-player fishing sessions.
- Added cast -> water validation -> bite -> hold/release reel -> catch/line-break state flow.
- Added a serverbound held-reel input payload; the client sends input state while the server owns tension/progress/result.
- Separated hook flight time from bite wait time so long casts do not shorten the bite delay.
- Added a tiny weighted fish catalog for technical testing.
- Added pure reel-math unit tests.
- Kept Essential optional and outside gameplay authority.

Not yet claimed: polished HUD, hooked-fish model, selling, persistent collection/economy, real multiplayer playtest or client visual playtest.
