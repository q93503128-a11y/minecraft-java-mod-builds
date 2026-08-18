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


def all_existing(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    turret = JAVA / "VillagePlacedTurretSystem.java"
    all_existing(turret, "    private static int disableCursor;\n", "")
    all_existing(turret, "        disableCursor = 0;\n", "")
    all_existing(turret, "            disableCursor = 0;\n", "")

    once(turret,
'''        List<Mob> candidates = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null)
                .stream().filter(mob -> VillageDefenseLineOfSight.hasLine(level, turretMuzzle(state, mob), mob)).toList();
        if (candidates.isEmpty()) return;''',
'''        List<Mob> nearby = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null);
        List<Mob> candidates = state.type() == TurretType.BOMBARD
                ? nearby
                : nearby.stream().filter(mob -> VillageDefenseLineOfSight.hasLine(level, turretMuzzle(state, mob), mob)).toList();
        if (candidates.isEmpty()) return;''')

    once(turret,
'''            if (type == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER && distanceSquared <= 36.0 * 36.0) {
                mob.getNavigation().moveTo(state.pos().getX() + 0.5, state.pos().getY(), state.pos().getZ() + 0.5, 1.08);
                if (distanceSquared <= 7.5 * 7.5) {
                    damage = Math.max(damage, 18 + VillageCouncilState.currentDay());
                }''',
'''            if (type == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER && distanceSquared <= 36.0 * 36.0) {
                // Navigation ownership lives in VillageRaidSystem; this layer only resolves physical turret contact damage.
                if (distanceSquared <= 7.5 * 7.5) {
                    damage = Math.max(damage, 18 + VillageCouncilState.currentDay());
                }''')

    start = turret.read_text(encoding="utf-8").find("    public static synchronized int disableRandomActiveTurret(int ticks) {")
    end = turret.read_text(encoding="utf-8").find("    public static synchronized boolean isDisabled", start)
    if start < 0 or end < 0:
        raise SystemExit("legacy random turret disruption method not found")
    text = turret.read_text(encoding="utf-8")
    replacement = '''    public static synchronized TurretState nearestActiveTurret(Vec3 origin, double range) {
        if (origin == null || range <= 0.0) return null;
        double squared = range * range;
        return TURRETS.values().stream()
                .filter(TurretState::active)
                .filter(state -> Vec3.atCenterOf(state.pos()).distanceToSqr(origin) <= squared)
                .min(Comparator.comparingDouble(state -> Vec3.atCenterOf(state.pos()).distanceToSqr(origin)))
                .orElse(null);
    }

    public static synchronized int disableNearestActiveTurret(Vec3 origin, double range, int ticks) {
        TurretState selected = nearestActiveTurret(origin, range);
        if (selected == null) return -1;
        DISABLED_TICKS.put(selected.id(), Math.max(DISABLED_TICKS.getOrDefault(selected.id(), 0), Math.max(1, ticks)));
        return selected.id();
    }

'''
    turret.write_text(text[:start] + replacement + text[end:], encoding="utf-8")

    enemy = JAVA / "VillageEnemyArchetypeSystem.java"
    once(enemy,
'''                int disabledId = VillagePlacedTurretSystem.disableRandomActiveTurret(20 * 7);''',
'''                int disabledId = VillagePlacedTurretSystem.disableNearestActiveTurret(mob.position(), 48.0, 20 * 7);''')

    raid = JAVA / "VillageRaidSystem.java"
    once(raid,
'''            ServerPlayer nearbyPlayer = gatePassable
                    && !VillageEnemyArchetypeSystem.ignoresNearbyPlayersUntilInside(archetype)
                    ? nearestPriorityPlayer(server, mob)
                    : null;''',
'''            if (archetype == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER) {
                VillagePlacedTurretSystem.TurretState turret =
                        VillagePlacedTurretSystem.nearestActiveTurret(mob.position(), 48.0);
                if (turret != null) {
                    Vec3 turretCenter = Vec3.atCenterOf(turret.pos());
                    mob.setTarget(null);
                    mob.getLookControl().setLookAt(turretCenter.x, turretCenter.y + 1.0, turretCenter.z);
                    mob.getNavigation().moveTo(turretCenter.x, turretCenter.y, turretCenter.z, 1.14);
                    continue;
                }
            }

            ServerPlayer nearbyPlayer = gatePassable
                    && !VillageEnemyArchetypeSystem.ignoresNearbyPlayersUntilInside(archetype)
                    ? nearestPriorityPlayer(server, mob)
                    : null;''')

    print("[PASS] tower hunters now own one coherent nearest-placed-turret objective")
    print("[PASS] bombard targeting may arc over fortress cover while direct-fire turrets retain LOS")


if __name__ == "__main__":
    main()
