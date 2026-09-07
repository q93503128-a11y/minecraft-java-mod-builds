package kr.moonseungjun.riftfrontier.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered migration pipeline for authoritative Riftfrontier persisted state.
 * Migrations are explicit one-version steps so skipped or ambiguous upgrades fail loudly.
 */
public final class PersistenceMigrationRegistry {
    @FunctionalInterface
    public interface Migration {
        Map<String, Object> migrate(Map<String, Object> input);
    }

    private final Map<Integer, Migration> migrations = new HashMap<>();

    public PersistenceMigrationRegistry register(int fromVersion, Migration migration) {
        if (fromVersion < 0) throw new IllegalArgumentException("fromVersion must be >= 0");
        if (fromVersion >= PersistenceSchema.CURRENT) {
            throw new IllegalArgumentException("Migration source " + fromVersion + " must be older than current schema " + PersistenceSchema.CURRENT);
        }
        Objects.requireNonNull(migration, "migration");
        if (migrations.putIfAbsent(fromVersion, migration) != null) {
            throw new IllegalStateException("Duplicate persistence migration from schema " + fromVersion);
        }
        return this;
    }

    public Map<String, Object> migrate(int sourceVersion, Map<String, Object> input) {
        if (sourceVersion < 0) throw new IllegalArgumentException("sourceVersion must be >= 0");
        if (sourceVersion > PersistenceSchema.CURRENT) {
            throw new IllegalArgumentException(
                "Persistence schema " + sourceVersion + " is newer than supported schema " + PersistenceSchema.CURRENT
            );
        }

        Map<String, Object> state = new LinkedHashMap<>(Objects.requireNonNull(input, "input"));
        int version = sourceVersion;
        while (version < PersistenceSchema.CURRENT) {
            Migration migration = migrations.get(version);
            if (migration == null) {
                throw new IllegalStateException("Missing persistence migration from schema " + version + " to " + (version + 1));
            }
            state = new LinkedHashMap<>(Objects.requireNonNull(migration.migrate(Map.copyOf(state)), "migration result"));
            version++;
            state.put(PersistenceSchema.VERSION_KEY, version);
        }
        PersistenceSchema.requireSupported(version);
        return Map.copyOf(state);
    }

    /**
     * Schema 0 was the pre-alpha unversioned root. Schema 1 introduced revision/content breadcrumbs.
     * Schema 2 introduced persisted expedition runs. Schema 3 adds the first authoritative hub economy
     * and region-response state used by the M2 vertical slice.
     */
    public static PersistenceMigrationRegistry defaults() {
        return new PersistenceMigrationRegistry()
            .register(0, input -> new LinkedHashMap<>(input))
            .register(1, input -> {
                Map<String, Object> migrated = new LinkedHashMap<>(input);
                migrated.putIfAbsent("expeditions", new ArrayList<>());
                return migrated;
            })
            .register(2, input -> {
                Map<String, Object> migrated = new LinkedHashMap<>(input);
                migrated.putIfAbsent("secured_region_01_salvage", 0);
                migrated.putIfAbsent("expedition_supply", 2);
                migrated.putIfAbsent("region_01_pressure", 0);
                return migrated;
            });
    }
}
