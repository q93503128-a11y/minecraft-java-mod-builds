#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old in text:
        target.write_text(text.replace(old, new), encoding="utf-8")


# Java does not allow a multi-character char literal.
patch(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    ".replace('|', ' · ')",
    '.replace("|", " · ")',
)

# NeoForge 26.2 uses the same SPEED holder name as the existing project.
patch(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
    "MobEffects.MOVEMENT_SPEED",
    "MobEffects.SPEED",
)

# Avoid depending on authlib GameProfile accessor naming.
patch(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterData.java",
    "player.getGameProfile().name()",
    "player.getScoreboardName()",
)

# Server-side overlay messages are routed through the existing notice service.
patch(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterService.java",
    "player.sendOverlayMessage(Component.literal(message));",
    "ArcaneNoticeService.push(player, Component.literal(message), 45);",
)

# In 26.2 the command-tag collection uses its explicit mapping name.
for path in (
    "src/main/java/kr/moonseungjun/arcanecircle/magic/RpgScaleService.java",
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterService.java",
):
    patch(path, ".getTags()", ".getCommandTags()")

# The old shared-spawn accessor was removed in 1.21.9+. Encounter coordinates are
# intentionally stable world coordinates, so the origin is a deterministic anchor.
patch(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterService.java",
    "level.getServer().overworld().getSharedSpawnPos()",
    "BlockPos.ZERO",
)
patch(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterService.java",
    "level.getSharedSpawnPos()",
    "BlockPos.ZERO",
)

print("Arcane Circle alpha.10 NeoForge 26.2 compatibility fixes applied")
