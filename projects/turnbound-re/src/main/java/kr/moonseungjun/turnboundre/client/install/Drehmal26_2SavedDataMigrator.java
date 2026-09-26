package kr.moonseungjun.turnboundre.client.install;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Removes the obsolete 1.20.1 random-sequence cache after preserving a local backup so 26.2 can regenerate it. */
public final class Drehmal26_2SavedDataMigrator {
    public static final String MIGRATION_ID = "drehmal-2.2.2f-saved-data-to-mc-26.2-r1";
    public static final String COMPATIBILITY_MARKER_FILE = ".turnbound_re_26_2_saved_data";

    private static final String RANDOM_SEQUENCES_RELATIVE = "data/random_sequences.dat";
    private static final String BACKUP_RELATIVE = ".turnbound-re-backup/random_sequences-1.20.1.dat";

    public record MigrationReport(boolean legacyFileRemoved, Path backup) {}

    private Drehmal26_2SavedDataMigrator() {}

    public static Path compatibilityMarker(Path worldDirectory) {
        return worldDirectory.resolve(COMPATIBILITY_MARKER_FILE);
    }

    public static Path randomSequences(Path worldDirectory) {
        return worldDirectory.resolve(RANDOM_SEQUENCES_RELATIVE);
    }

    public static Path backup(Path worldDirectory) {
        return worldDirectory.resolve(BACKUP_RELATIVE);
    }

    public static boolean compatibilityMarkerMatches(Path worldDirectory) {
        if (worldDirectory == null) return false;
        Path marker = compatibilityMarker(worldDirectory);
        try {
            return Files.isRegularFile(marker)
                    && MIGRATION_ID.equals(Files.readString(marker, StandardCharsets.UTF_8).trim())
                    && !Files.isRegularFile(randomSequences(worldDirectory));
        } catch (IOException ignored) {
            return false;
        }
    }

    public static MigrationReport migrate(Path worldDirectory) throws IOException {
        if (worldDirectory == null || !Files.isDirectory(worldDirectory)) {
            throw new IOException("Drehmal world directory unavailable for saved-data migration");
        }
        if (compatibilityMarkerMatches(worldDirectory)) {
            return new MigrationReport(false, backup(worldDirectory));
        }

        Path legacy = randomSequences(worldDirectory);
        Path backup = backup(worldDirectory);
        boolean removed = false;
        if (Files.isRegularFile(legacy)) {
            if (!Files.isRegularFile(backup)) {
                Files.createDirectories(backup.getParent());
                Files.copy(legacy, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            Files.delete(legacy);
            removed = true;
        }

        Files.writeString(
                compatibilityMarker(worldDirectory),
                MIGRATION_ID + System.lineSeparator(),
                StandardCharsets.UTF_8);
        return new MigrationReport(removed, backup);
    }
}
