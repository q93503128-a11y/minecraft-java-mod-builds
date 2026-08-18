#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match in {path.name}, got {count}: {old[:220]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    turret = JAVA / "VillagePlacedTurretSystem.java"
    once(turret,
'''    public static synchronized int count() { return TURRETS.size(); }''',
'''    /** Re-project an externally restored/reset siege snapshot into runtime state and world visuals. */
    public static synchronized void reloadAfterPersistenceChange(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        for (TurretState state : new ArrayList<>(TURRETS.values())) clearVisual(level, state);
        TURRETS.clear();
        PENDING.clear();
        DISABLED_TICKS.clear();
        PENDING_BOMBARDS.clear();
        combatTicks = 0;
        VillageSiegePersistence.stringsWithPrefix(PREFIX).forEach((key, value) -> {
            try {
                int id = Integer.parseInt(key.substring(PREFIX.length()));
                TurretState state = decode(id, value);
                if (state != null) TURRETS.put(id, state);
            } catch (NumberFormatException ignored) { }
        });
        VillageTurretPresentationSystem.initialize(level, states());
        rebuildVisuals(level);
    }

    public static synchronized int count() { return TURRETS.size(); }''')

    council = JAVA / "VillageCouncilState.java"
    once(council,
'''            VillageSiegePersistence.resetForNewGame();
        } else {
            VillageSiegePersistence.restoreNightSnapshot();
        }
        persist();''',
'''            VillageSiegePersistence.resetForNewGame();
        } else {
            VillageSiegePersistence.restoreNightSnapshot();
        }
        // SavedData restoration is not enough: rebuild runtime turret state, collision shells, mesh actors and wall projection now.
        VillagePlacedTurretSystem.reloadAfterPersistenceChange(server);
        VillageSiegeSegmentSystem.restoreAllVisuals(server.overworld());
        persist();''')

    print("[PASS] failed-night/new-game siege persistence now reprojects immediately into runtime and world visuals")


if __name__ == "__main__":
    main()
