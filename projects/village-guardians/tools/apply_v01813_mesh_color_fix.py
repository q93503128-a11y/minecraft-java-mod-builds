#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MESH = ROOT / "src/main/java/kr/moonseungjun/villageguardians/VillageSkillMeshLibrary.java"


def once(old: str, new: str) -> None:
    text = MESH.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match, got {count}: {old[:180]!r}")
    MESH.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    once(
'''                    (color & 0xFFFFFF00) | (int) (205 * fade));''',
'''                    withAlpha(color, (int) (205 * fade)));''')
    once(
'''                    rgba((color >> 24) & 255, (color >> 16) & 255, (color >> 8) & 255, (int) (120 * fade)));''',
'''                    withAlpha(color, (int) (120 * fade)));''')
    once(
'''                    (color & 0xFFFFFF00) | Math.max(20, (int) (90 * (1.0 - progress))));''',
'''                    withAlpha(color, Math.max(20, (int) (90 * (1.0 - progress)))));''')
    once(
'''    private static int rgba(int r, int g, int b, int a) {
        return (clampInt(a) << 24) | (clampInt(r) << 16) | (clampInt(g) << 8) | clampInt(b);
    }''',
'''    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (clampInt(alpha) << 24);
    }

    private static int rgba(int r, int g, int b, int a) {
        return (clampInt(a) << 24) | (clampInt(r) << 16) | (clampInt(g) << 8) | clampInt(b);
    }''')
    print("[PASS] defense mesh alpha fades preserve packed ARGB color channels")


if __name__ == "__main__":
    main()
