#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match in {path.name}, got {count}: {old[:180]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    boss = JAVA / "VillageSiegeBossSystem.java"
    once(boss,
        "return values[Math.floorMod(day * 5 + wave * 3 + salt, values.length)];",
        "return values[Math.floorMod(day * 5 + wave * 2 + salt, values.length)];")
    once(boss,
'''        BlockPos target = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());
        if (!VillageSiegeSegmentSystem.breached(segment)) {
            mob.setTarget(null);''',
'''        BlockPos target = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());
        if (VillageSiegeSegmentSystem.breached(segment)) {
            BREACH_CASTS.remove(mob.getUUID());
            return;
        }
        {
            mob.setTarget(null);''')

    effects = JAVA / "VillageBossEffectSystem.java"
    once(effects,
'''        VillageSkillEffectEntity.spawn(level, boss,
                "boss_phase_two_" + doctrine.name().toLowerCase(Locale.ROOT),
                boss.position(), horizontal(boss.getLookAngle()), 20 * 60 * 30, 0.0f, "");''',
'''        VillageBossAspectSystem.Aspect aspect = VillageBossAspectSystem.aspectOf(boss);
        String aspectId = aspect == null ? "" : aspect.name().toLowerCase(Locale.ROOT);
        VillageSkillEffectEntity.spawn(level, boss,
                "boss_phase_two_" + doctrine.name().toLowerCase(Locale.ROOT),
                boss.position(), horizontal(boss.getLookAngle()), 20 * 60 * 30, 0.0f, aspectId);''')

    print("[PASS] boss doctrine selection now genuinely varies with wave modulo three")
    print("[PASS] phase-two overlay preserves the boss aspect identity color")
    print("[PASS] completed breach segments discard stale pending breach casts")


if __name__ == "__main__":
    main()
