from __future__ import annotations

from pathlib import Path
import re
import subprocess
import sys

ROOT = Path.cwd()
WORKSPACE = ROOT.parent.parent
BASE = WORKSPACE / ".github/scripts/arcane-circle/apply_v0121_alpha17_spell_presentation.py"


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def run_base() -> None:
    subprocess.run([sys.executable, str(BASE)], cwd=ROOT, check=True)


def fix_profile_enums() -> None:
    rel = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java"
    text = read(rel)
    # The profile table intentionally reads like a design sheet, but Java still needs the nested
    # enum owner on each constant. Qualify only the two enum columns of authored put(...) rows.
    sigils = {
        "FRONT_COMPACT", "FRONT_LANCE", "GROUND_SEAL", "TARGET_SEAL", "BODY_HALO",
        "FEET_RUNE", "SKY_RITUAL", "QUAD_ARRAY", "WALL_MATRIX", "PORTAL_GATE"
    }
    motions = {
        "SNAP", "DART", "BOLT", "HEAVY_ORB", "MISSILE_SWARM", "LANCE", "BEAM",
        "WAVE", "FIELD", "SKY_DROP", "STORM", "PORTAL", "PRISON", "WALL",
        "TARGET_BURST", "AURA"
    }

    pattern = re.compile(r'put\("([^"]+)", ([A-Z_]+), ([A-Z_]+),')

    def repl(match: re.Match[str]) -> str:
        spell, sigil, motion = match.groups()
        if sigil not in sigils or motion not in motions:
            raise SystemExit(f"unknown authored presentation enum: {spell}: {sigil}/{motion}")
        return f'put("{spell}", SigilStyle.{sigil}, MotionStyle.{motion},'

    text, count = pattern.subn(repl, text)
    if count < 20:
        raise SystemExit(f"expected authored profile rows, qualified only {count}")
    write(rel, text)


def synchronize_visible_impacts() -> None:
    rel = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java"
    text = read(rel)
    text = text.replace(
        " * Separates visual travel from authoritative combat. Projectiles resolve immediately so\n"
        " * networking/render duration can never add a hidden one-second damage delay.\n",
        " * Keeps authoritative combat synchronized with authored presentation. Instant spells still\n"
        " * resolve immediately unless their profile has an explicit visible wind-up; projectile and\n"
        " * sky-drop damage lands on the same server tick as the visible impact, never afterward.\n",
        1,
    )

    projectile = '''        if (mode == SpellArchetype.Mode.INSTANT) {
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
        }
        if (mode == SpellArchetype.Mode.PROJECTILE) {
            int projectileImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
                    SpellCastingService.kineticDistance(player, cast.range()));
            if (projectileImpactDelay <= 1) {
                boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(), cast.range(), cast.power());
                SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
                return executed;
            }
            enqueue(player, new PendingCast(cast, snapshot, clock(player) + projectileImpactDelay,
                    0, 1, cast.power(), false));
            return true;
        }
'''
    replacement = '''        int presentationImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
                SpellCastingService.kineticDistance(player, cast.range()));

        if (mode == SpellArchetype.Mode.INSTANT) {
            if (presentationImpactDelay > 1) {
                enqueue(player, new PendingCast(cast, snapshot, clock(player) + presentationImpactDelay,
                        0, 1, cast.power(), false));
                return true;
            }
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
        }
        if (mode == SpellArchetype.Mode.PROJECTILE) {
            if (presentationImpactDelay <= 1) {
                boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(), cast.range(), cast.power());
                SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
                return executed;
            }
            enqueue(player, new PendingCast(cast, snapshot, clock(player) + presentationImpactDelay,
                    0, 1, cast.power(), false));
            return true;
        }
'''
    if projectile not in text:
        raise SystemExit("SpellKineticsService: alpha.17 projectile block not found")
    text = text.replace(projectile, replacement, 1)

    first_pulse = '''        // The first pulse resolves on the cast tick; only subsequent pulses are queued.
        boolean first = SpellCastingService.executeResolved(player, cast.spell().id(),
                cast.range(), pulsePower);
        int remaining = totalPulses - 1;
        if (remaining <= 0) {
            SpellCastingService.finishKineticCast(player, cast, snapshot, first);
            return first;
        }

        enqueue(player, new PendingCast(cast, snapshot, clock(player) + interval, interval,
                remaining, pulsePower, first));
        return true;
'''
    delayed_first = '''        // Ordinary channels/fields still start on the cast tick. Authored sky rituals and other
        // explicitly telegraphed spells instead begin their first authoritative pulse exactly when
        // the visible payload reaches the impact point; later pulses retain the original cadence.
        if (presentationImpactDelay > 1) {
            enqueue(player, new PendingCast(cast, snapshot, clock(player) + presentationImpactDelay,
                    interval, totalPulses, pulsePower, false));
            return true;
        }

        boolean first = SpellCastingService.executeResolved(player, cast.spell().id(),
                cast.range(), pulsePower);
        int remaining = totalPulses - 1;
        if (remaining <= 0) {
            SpellCastingService.finishKineticCast(player, cast, snapshot, first);
            return first;
        }

        enqueue(player, new PendingCast(cast, snapshot, clock(player) + interval, interval,
                remaining, pulsePower, first));
        return true;
'''
    if first_pulse not in text:
        raise SystemExit("SpellKineticsService: first pulse block not found")
    text = text.replace(first_pulse, delayed_first, 1)
    write(rel, text)


def main() -> None:
    run_base()
    fix_profile_enums()
    synchronize_visible_impacts()
    print("Arcane Circle alpha.17 spell-presentation migration v2: PASS")


if __name__ == "__main__":
    main()
