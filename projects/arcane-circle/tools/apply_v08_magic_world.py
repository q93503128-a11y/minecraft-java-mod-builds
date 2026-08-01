#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import base64
import re
import zlib

PART_COUNT = 6
B85_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!#$%&()*+-;<=>?@^_`{|}~"
parts_dir = Path(__file__).with_name("v08_payload")
parts = [(parts_dir / f"part{i}.txt").read_text(encoding="utf-8").strip() for i in range(PART_COUNT)]
encoded = "".join(parts)


def unpack(candidate: str) -> str:
    compressed = base64.b85decode(candidate.encode("ascii"))
    return zlib.decompress(compressed).decode("utf-8")


def recover_single_character(candidate: str, failure: Exception) -> tuple[str, str]:
    """Recover one damaged character only when the complete zlib stream validates."""
    match = re.search(r"byte (\d+)", str(failure))
    center = int(match.group(1)) if match else max(0, len(candidate) // 2)
    left = max(0, center - 8)
    right = min(len(candidate), center + 13)

    attempts: list[tuple[str, str]] = []
    for pos in range(left, right):
        attempts.append((f"delete@{pos}:{candidate[pos]!r}", candidate[:pos] + candidate[pos + 1:]))
        for char in B85_ALPHABET:
            if char != candidate[pos]:
                attempts.append((f"replace@{pos}:{candidate[pos]!r}->{char!r}", candidate[:pos] + char + candidate[pos + 1:]))
    for pos in range(left, right + 1):
        for char in B85_ALPHABET:
            attempts.append((f"insert@{pos}:{char!r}", candidate[:pos] + char + candidate[pos:]))

    for description, repaired in attempts:
        try:
            source = unpack(repaired)
            compile(source, __file__ + "::<recovered-payload>", "exec")
            return source, description
        except (ValueError, zlib.error, UnicodeDecodeError, SyntaxError):
            continue

    boundaries = []
    running = 0
    for index, part in enumerate(parts):
        boundaries.append(f"part{index}=[{running},{running + len(part)}) len={len(part)}")
        running += len(part)
    context = candidate[max(0, center - 15):min(len(candidate), center + 20)]
    raise RuntimeError(
        "Arcane v0.8 payload could not be checksum-recovered. "
        f"original={failure!r}; failure_context={context!r}; " + "; ".join(boundaries)
    ) from failure


try:
    source = unpack(encoded)
    repair = "none"
except (ValueError, zlib.error, UnicodeDecodeError) as failure:
    source, repair = recover_single_character(encoded, failure)

# Adapt migration anchors from the v0.6 preparation snapshot to the actual v0.7
# release without changing any v0.8 replacement text.
source = source.replace("ninefold-arcana-6", "ninefold-arcana-7")
source = source.replace(
    '        g.text(font, Component.literal("지팡이 " + ArcaneClientState.text("staff", "맨손")), x, y + 24, 0xFFFFD98A);',
    '        g.text(font, Component.literal(compactName(ArcaneClientState.text("staff", "맨손"), 12)),\n'
    '                x, y + 1, 0xFFFFD489);'
)

print("Arcane v0.8 migration payload decoded:",
      ", ".join(f"part{i}={len(part)}" for i, part in enumerate(parts)),
      f"repair={repair}")
exec(compile(source, __file__ + "::<expanded>", "exec"), {"__name__": "__main__", "__file__": __file__})

project_root = Path(__file__).resolve().parents[1]

# Opening a player profile also opens/validates their persistent Arcana wallet.
main_java = project_root / "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"
main_source = main_java.read_text(encoding="utf-8")
if "import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;" not in main_source:
    main_source = main_source.replace(
        "import kr.moonseungjun.arcanecircle.world.MagicWorldService;",
        "import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;\n"
        "import kr.moonseungjun.arcanecircle.world.MagicWorldService;"
    )
if "ArcaneEconomyService.balance(player);" not in main_source:
    main_source = main_source.replace(
        "        boolean firstAwakening = data.ensureProfile(player);",
        "        boolean firstAwakening = data.ensureProfile(player);\n"
        "        ArcaneEconomyService.balance(player);"
    )
main_java.write_text(main_source, encoding="utf-8")

# Name the already-functional effective-range visual multiplier explicitly. The
# ratio controls the radius of every family-specific and spell-signature glyph.
sigil_java = project_root / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellSigilService.java"
sigil_source = sigil_java.read_text(encoding="utf-8")
sigil_source = sigil_source.replace(
    "double ratio = spell.range() <= 0.0 ? 1.0 : Math.max(0.75, Math.min(3.2, range / spell.range()));",
    "double rangeRatio = spell.range() <= 0.0 ? 1.0 : Math.max(0.75, Math.min(3.2, range / spell.range()));"
)
sigil_source = sigil_source.replace("Math.sqrt(ratio) * familyScale", "Math.sqrt(rangeRatio) * familyScale")
sigil_java.write_text(sigil_source, encoding="utf-8")

print("Arcane v0.8 lifecycle linked and effective-range sigil scaling normalized")
