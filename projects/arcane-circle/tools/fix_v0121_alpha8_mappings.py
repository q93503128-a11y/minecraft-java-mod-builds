#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneGearRenderer.java"
text = path.read_text(encoding="utf-8")
text = text.replace(
    "import net.neoforged.neoforge.client.event.RegisterRenderStateModifiersEvent;",
    "import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;")
path.write_text(text, encoding="utf-8")
print("Arcane Circle alpha.8 NeoForge 26.2 mapping patch applied")
