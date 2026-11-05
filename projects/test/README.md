# Yin-Yang Taiji experiment

Press **G** in a loaded world to cast. The animation is server-authorized and synchronized to players in the same dimension. It is a presentation experiment: the traveling taiji collides visually with blocks, and does not deal damage or change world state.

Player arms are animated with Player Animation Library (PAL) 1.2.8 for Minecraft 26.3. Install PAL alongside Fabric API. The custom left/right hand anchors drive the two energy forms so they follow the animated rig in first and third person. The world-space taiji uses a thick custom mesh with an S-curve split and contrasting inset dots.

See [`PROJECT.md`](PROJECT.md) for implementation and validation status. Build and runtime evidence is recorded in `BUILD_AND_RUNTIME_REPORT.md` after verification.
