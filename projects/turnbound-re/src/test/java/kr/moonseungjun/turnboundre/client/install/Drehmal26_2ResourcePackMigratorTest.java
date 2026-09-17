package kr.moonseungjun.turnboundre.client.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Drehmal26_2ResourcePackMigratorTest {
    @TempDir Path temp;

    @Test
    void rewritesUppercaseSpawnEggIdentifierAndPreservesBackup() throws Exception {
        Path world = temp.resolve("world");
        Path pack = DrehmalInstallFiles.worldResourcePack(world);
        Files.createDirectories(pack.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(pack))) {
            output.putNextEntry(new ZipEntry("assets/minecraft/models/item/zombie_spawn_egg.json"));
            output.write("{\"model\":\"minecraft:item/spawn_egg_2D\"}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new ZipEntry("assets/drehmal/textures/keep.bin"));
            output.write(new byte[]{1, 2, 3});
            output.closeEntry();
        }

        Drehmal26_2ResourcePackMigrator.MigrationReport report = Drehmal26_2ResourcePackMigrator.migrate(world);

        assertEquals(1, report.jsonFilesChanged());
        assertTrue(Files.isRegularFile(report.backup()));
        assertTrue(Drehmal26_2ResourcePackMigrator.compatibilityMarkerMatches(world));
        assertTrue(Drehmal26_2ResourcePackMigrator.isArchiveCompatible(pack));
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            String json = new String(zip.getInputStream(zip.getEntry("assets/minecraft/models/item/zombie_spawn_egg.json")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("spawn_egg_2d"));
            assertFalse(json.contains("spawn_egg_2D"));
        }
    }

    @Test
    void secondMigrationIsIdempotent() throws Exception {
        Path world = temp.resolve("world-idempotent");
        Path pack = DrehmalInstallFiles.worldResourcePack(world);
        Files.createDirectories(pack.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(pack))) {
            output.putNextEntry(new ZipEntry("assets/minecraft/models/item/zombie_spawn_egg.json"));
            output.write("{\"model\":\"minecraft:item/spawn_egg_2D\"}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        Drehmal26_2ResourcePackMigrator.migrate(world);
        byte[] once = Files.readAllBytes(pack);

        Drehmal26_2ResourcePackMigrator.MigrationReport second = Drehmal26_2ResourcePackMigrator.migrate(world);

        assertEquals(0, second.jsonFilesChanged());
        assertTrue(java.util.Arrays.equals(once, Files.readAllBytes(pack)));
    }
}
