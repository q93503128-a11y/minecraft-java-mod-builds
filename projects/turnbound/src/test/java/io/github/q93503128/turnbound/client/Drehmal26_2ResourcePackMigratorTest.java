package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Drehmal26_2ResourcePackMigratorTest {
    @TempDir Path temp;

    @Test
    void migratesUppercaseSpawnEggModelPathsWithoutChangingBinaryAssetsAndIsIdempotent() throws Exception {
        Path world = temp.resolve("world");
        Files.createDirectories(world);
        Path pack = world.resolve("resources.zip");

        byte[] texture = new byte[]{3, 1, 4, 1, 5, 9};
        createLegacyPack(pack, true, texture);
        String originalHash = DrehmalInstallFiles.sha256(pack);

        Drehmal26_2ResourcePackMigrator.Report first = Drehmal26_2ResourcePackMigrator.migrate(world);

        assertTrue(first.changed());
        assertEquals(2, first.referencesRewritten());
        assertTrue(first.modelRenamed());
        assertEquals(originalHash, first.sourceHash());
        assertNotEquals(first.sourceHash(), first.migratedHash());
        assertTrue(Drehmal26_2ResourcePackMigrator.isCurrent(world));

        Path backup = pack.resolveSibling(pack.getFileName() + Drehmal26_2ResourcePackMigrator.BACKUP_SUFFIX);
        assertEquals(originalHash, DrehmalInstallFiles.sha256(backup));

        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertNull(zip.getEntry(Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_MODEL));
            assertTrue(zip.getEntry(Drehmal26_2ResourcePackMigrator.MODERN_SPAWN_EGG_MODEL) != null);
            assertEquals(
                    Drehmal26_2ResourcePackMigrator.MODERN_SPAWN_EGG_PARENT,
                    parent(zip, "assets/minecraft/models/item/bat_spawn_egg.json"));
            assertEquals(
                    Drehmal26_2ResourcePackMigrator.MODERN_SPAWN_EGG_PARENT,
                    parent(zip, "assets/minecraft/models/item/zombie_spawn_egg.json"));
            assertArrayEquals(texture, bytes(zip, "assets/minecraft/textures/item/example.png"));
        }

        try (ZipFile backupZip = new ZipFile(backup.toFile())) {
            assertTrue(backupZip.getEntry(Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_MODEL) != null);
            assertEquals(
                    Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_PARENT,
                    parent(backupZip, "assets/minecraft/models/item/bat_spawn_egg.json"));
        }

        String migratedHash = DrehmalInstallFiles.sha256(pack);
        Drehmal26_2ResourcePackMigrator.Report second = Drehmal26_2ResourcePackMigrator.migrate(world);
        assertFalse(second.changed());
        assertEquals(migratedHash, DrehmalInstallFiles.sha256(pack));

        // A stale marker must never hide a restored/old resource archive.
        Files.copy(backup, pack, StandardCopyOption.REPLACE_EXISTING);
        assertFalse(Drehmal26_2ResourcePackMigrator.isCurrent(world));
        Drehmal26_2ResourcePackMigrator.Report repaired = Drehmal26_2ResourcePackMigrator.migrate(world);
        assertTrue(repaired.changed());
        assertTrue(Drehmal26_2ResourcePackMigrator.isCurrent(world));
    }

    @Test
    void fallsBackToVanillaGeneratedParentWhenLegacyCustomModelIsAbsent() throws Exception {
        Path world = temp.resolve("fallback-world");
        Files.createDirectories(world);
        Path pack = world.resolve("resources.zip");
        createLegacyPack(pack, false, new byte[]{7, 7, 7});

        Drehmal26_2ResourcePackMigrator.Report report = Drehmal26_2ResourcePackMigrator.migrate(world);

        assertTrue(report.changed());
        assertFalse(report.modelRenamed());
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertEquals(
                    "minecraft:item/generated",
                    parent(zip, "assets/minecraft/models/item/bat_spawn_egg.json"));
        }
    }

    private static void createLegacyPack(Path pack, boolean includeCustomModel, byte[] texture) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(pack), StandardCharsets.UTF_8)) {
            write(zip, "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"fixture\"}}");
            if (includeCustomModel) {
                write(zip, Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_MODEL,
                        "{\"parent\":\"minecraft:item/generated\"}");
            }
            write(zip, "assets/minecraft/models/item/bat_spawn_egg.json",
                    "{\"parent\":\"" + Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_PARENT
                            + "\",\"textures\":{\"layer0\":\"minecraft:item/bat_spawn_egg\"}}");
            write(zip, "assets/minecraft/models/item/zombie_spawn_egg.json",
                    "{\"parent\":\"" + Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_PARENT
                            + "\",\"textures\":{\"layer0\":\"minecraft:item/zombie_spawn_egg\"}}");
            zip.putNextEntry(new ZipEntry("assets/minecraft/textures/item/example.png"));
            zip.write(texture);
            zip.closeEntry();
        }
    }

    private static void write(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String parent(ZipFile zip, String entryName) throws Exception {
        String json = new String(bytes(zip, entryName), StandardCharsets.UTF_8);
        String key = "\"parent\":\"";
        int start = json.indexOf(key);
        int from = start + key.length();
        int end = json.indexOf('"', from);
        return json.substring(from, end);
    }

    private static byte[] bytes(ZipFile zip, String entryName) throws Exception {
        ZipEntry entry = zip.getEntry(entryName);
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }
}
