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
    """Recover one damaged character only when the complete zlib stream validates.

    GitHub content payloads are split only to stay below connector limits. A successful
    repair must decode as Base85, pass the zlib checksum, and decode as UTF-8. This means
    a merely plausible Base85 group is never accepted.
    """
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

print("Arcane v0.8 migration payload decoded:",
      ", ".join(f"part{i}={len(part)}" for i, part in enumerate(parts)),
      f"repair={repair}")
exec(compile(source, __file__ + "::<expanded>", "exec"), {"__name__": "__main__", "__file__": __file__})
