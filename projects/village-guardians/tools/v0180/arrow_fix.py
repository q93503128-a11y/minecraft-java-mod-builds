#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one marker, found {count}")
    write(path, text.replace(old, new, 1))


ability = JAVA / "VillageRoleAbilitySystem.java"

replace_once(
    ability,
    '''    private static final Map<UUID, SkillScale> RAPID_SCALE = new HashMap<>();
    private static final Map<UUID, Integer> RAPID_DRAW_TICKS = new HashMap<>();
''',
    '''    private static final Map<UUID, SkillScale> RAPID_SCALE = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> RAPID_ARROWS = new HashMap<>();
    private static final Map<UUID, Integer> RAPID_DRAW_TICKS = new HashMap<>();
''',
    "rapid arrow state map",
)
replace_once(
    ability,
    '''        RAPID_SCALE.clear();
        RAPID_DRAW_TICKS.clear();
''',
    '''        RAPID_SCALE.clear();
        RAPID_ARROWS.clear();
        RAPID_DRAW_TICKS.clear();
''',
    "rapid arrow reset",
)
replace_once(
    ability,
    '''        RAPID_SCALE.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
        RAPID_DRAW_TICKS.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
''',
    '''        RAPID_SCALE.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
        RAPID_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        RAPID_DRAW_TICKS.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
''',
    "rapid arrow expiry",
)

# Tracking arrows retain power in RICOCHET_ARROWS and apply it on impact.
replace_once(
    ability,
    '''            arrow.setBaseDamage(arrow.getBaseDamage() * scale.power());
            RICOCHET_ARROWS.put(arrow.getUUID(),
''',
    '''            RICOCHET_ARROWS.put(arrow.getUUID(),
''',
    "remove unavailable tracking base-damage getter",
)

# Main rapid arrow and every generated side arrow use the same impact-time
# multiplier, preserving vanilla bow/crossbow/enchantment damage.
replace_once(
    ability,
    '''        SkillScale scale = rapidScale == null ? SkillScale.DEFAULT : rapidScale;
        arrow.setBaseDamage(arrow.getBaseDamage() * scale.power());
        spawningGeneratedArrow = true;
''',
    '''        SkillScale scale = rapidScale == null ? SkillScale.DEFAULT : rapidScale;
        RAPID_ARROWS.put(arrow.getUUID(),
                new EmpoweredArrowState(now + 240L, scale.power(), scale.specialRank()));
        spawningGeneratedArrow = true;
''',
    "main rapid arrow impact state",
)
replace_once(
    ability,
    '''            spawnSideArrow(level, player, arrow, -8.0, 1.0f);
            spawnSideArrow(level, player, arrow, 8.0, 1.0f);
            if (scale.specialRank() >= 4) {
                spawnSideArrow(level, player, arrow, -16.0, 0.82f);
                spawnSideArrow(level, player, arrow, 16.0, 0.82f);
''',
    '''            spawnSideArrow(level, player, arrow, -8.0, scale.power());
            spawnSideArrow(level, player, arrow, 8.0, scale.power());
            if (scale.specialRank() >= 4) {
                spawnSideArrow(level, player, arrow, -16.0, scale.power() * 0.82f);
                spawnSideArrow(level, player, arrow, 16.0, scale.power() * 0.82f);
''',
    "side arrow stored multiplier",
)
replace_once(
    ability,
    '''        arrow.setBaseDamage(Math.max(2.0, source.getBaseDamage() * 0.82) * power);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
''',
    '''        arrow.setBaseDamage(2.0);
        RAPID_ARROWS.put(arrow.getUUID(), new EmpoweredArrowState(
                level.getGameTime() + 160L, power, 0));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
''',
    "side arrow impact state",
)

# Apply saved power exactly once when the arrow actually deals damage.
replace_once(
    ability,
    '''            VillageRole role = activeRole(attacker);
            TimedScale rally = RALLY_SCALE.get(attacker.getUUID());
''',
    '''            VillageRole role = activeRole(attacker);
            if (event.getSource().getDirectEntity() instanceof AbstractArrow directArrow) {
                EmpoweredArrowState rapid = RAPID_ARROWS.remove(directArrow.getUUID());
                if (rapid != null) event.setAmount(event.getAmount() * rapid.power());
            }
            TimedScale rally = RALLY_SCALE.get(attacker.getUUID());
''',
    "rapid impact power",
)
replace_once(
    ability,
    '''                EmpoweredArrowState ricochet = RICOCHET_ARROWS.remove(directArrow.getUUID());
                TRACKING_ARROWS.remove(directArrow.getUUID());
                int ricochetRank = ricochet == null ? 0 : ricochet.specialRank();
''',
    '''                EmpoweredArrowState ricochet = RICOCHET_ARROWS.remove(directArrow.getUUID());
                TRACKING_ARROWS.remove(directArrow.getUUID());
                if (ricochet != null) event.setAmount(event.getAmount() * ricochet.power());
                int ricochetRank = ricochet == null ? 0 : ricochet.specialRank();
''',
    "ricochet impact power",
)

# Update current regression contract for impact-time scaling.
test = ROOT / "tools/test_v0180_content_scaling.py"
text = read(test)
text = text.replace(
    '"spawnSideArrow(level, player, arrow, -8.0, 1.0f)" in ability\n        and "event.getAmount() * 0.72f * ricochetPower" not in ability,',
    '"RAPID_ARROWS" in ability\n        and "event.setAmount(event.getAmount() * rapid.power())" in ability\n        and "event.setAmount(event.getAmount() * ricochet.power())" in ability\n        and "getBaseDamage()" not in ability\n        and "event.getAmount() * 0.72f * ricochetPower" not in ability,')
write(test, text)

print("Applied v0.18.0 impact-time arrow power scaling")
