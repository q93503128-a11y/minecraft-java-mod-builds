package kr.moonseungjun.turnboundre.client.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Drehmal26_2SavedDataMigratorTest {
    @TempDir Path temp;

    @Test
    void backsUpAndRemovesLegacyRandomSequenceCache() throws Exception {
        Path world = temp.resolve("world");
        Path legacy = Drehmal26_2SavedDataMigrator.randomSequences(world);
        Files.createDirectories(legacy.getParent());
        byte[] original = "old-random-sequences".getBytes(StandardCharsets.UTF_8);
        Files.write(legacy, original);

        Drehmal26_2SavedDataMigrator.MigrationReport report = Drehmal26_2SavedDataMigrator.migrate(world);

        assertTrue(report.legacyFileRemoved());
        assertFalse(Files.exists(legacy));
        assertTrue(Files.isRegularFile(report.backup()));
        assertArrayEquals(original, Files.readAllBytes(report.backup()));
        assertTrue(Drehmal26_2SavedDataMigrator.compatibilityMarkerMatches(world));
    }

    @Test
    void migrationIsSafeWhenLegacyCacheIsAlreadyAbsent() throws Exception {
        Path world = temp.resolve("world-empty");
        Files.createDirectories(world);

        Drehmal26_2SavedDataMigrator.MigrationReport first = Drehmal26_2SavedDataMigrator.migrate(world);
        Drehmal26_2SavedDataMigrator.MigrationReport second = Drehmal26_2SavedDataMigrator.migrate(world);

        assertFalse(first.legacyFileRemoved());
        assertFalse(second.legacyFileRemoved());
        assertTrue(Drehmal26_2SavedDataMigrator.compatibilityMarkerMatches(world));
    }
}
