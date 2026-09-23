package io.github.q93503128.turnbound.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Drehmal26_2CompatMigratorTest {
    @TempDir Path temp;

    @Test
    void migratesPinnedRegistrySchemaWithoutTouchingWorldChunksAndIsIdempotent() throws Exception {
        Path world = temp.resolve("world");
        Path datapacks = world.resolve("datapacks");
        Path region = world.resolve("region");
        Files.createDirectories(datapacks);
        Files.createDirectories(region);

        byte[] regionBytes = new byte[]{1, 4, 9, 16, 25};
        Files.write(region.resolve("r.0.0.mca"), regionBytes);

        Path datapack = datapacks.resolve("hi_drehmal.zip");
        createLegacyDatapack(datapack);
        String originalHash = DrehmalInstallFiles.sha256(datapack);

        Drehmal26_2CompatMigrator.Report first = Drehmal26_2CompatMigrator.migrate(world);

        assertTrue(first.changed());
        assertEquals(48, first.biomes());
        assertEquals(1, first.dimensionTypes());
        assertEquals(originalHash, first.sourceHash());
        assertNotEquals(first.sourceHash(), first.migratedHash());
        assertTrue(Drehmal26_2CompatMigrator.isCurrent(world));
        assertArrayEquals(regionBytes, Files.readAllBytes(region.resolve("r.0.0.mca")));

        Path backup = datapack.resolveSibling(datapack.getFileName() + Drehmal26_2CompatMigrator.BACKUP_SUFFIX);
        assertTrue(Files.isRegularFile(backup));
        assertEquals(originalHash, DrehmalInstallFiles.sha256(backup));

        JsonObject dimension = json(datapack, "data/minecraft/dimension_type/space_type.json");
        assertFalse(dimension.get("has_ender_dragon_fight").getAsBoolean());

        JsonObject grove = json(datapack, "data/minecraft/worldgen/biome/grove.json");
        assertTrue(grove.get("carvers").isJsonArray());
        JsonArray carvers = grove.getAsJsonArray("carvers");
        assertEquals(2, carvers.size());
        assertEquals("minecraft:cave", carvers.get(0).getAsString());
        assertEquals("minecraft:canyon", carvers.get(1).getAsString());
        assertEquals(0.9999999D, grove.get("creature_spawn_probability").getAsDouble(), 0.00000001D);
        JsonObject attributes = grove.getAsJsonObject("attributes");
        assertEquals("#350c26", attributes.get("minecraft:visual/sky_color").getAsString());
        assertEquals("#453852", attributes.get("minecraft:visual/fog_color").getAsString());
        assertTrue(attributes.has("minecraft:audio/ambient_sounds"));
        assertTrue(attributes.has("minecraft:visual/ambient_particles"));

        JsonObject basalt = json(datapack, "data/minecraft/worldgen/biome/basalt_deltas.json");
        JsonObject basaltAttributes = basalt.getAsJsonObject("attributes");
        assertFalse(basaltAttributes.has("minecraft:audio/ambient_sounds"));
        assertFalse(basaltAttributes.has("minecraft:audio/background_music"));

        JsonObject originalBasalt = json(backup, "data/minecraft/worldgen/biome/basalt_deltas.json");
        JsonObject originalMood = originalBasalt.getAsJsonObject("effects").getAsJsonObject("mood_sound");
        assertEquals("minecraft:null", originalMood.get("sound").getAsString());

        JsonObject originalGrove = json(backup, "data/minecraft/worldgen/biome/grove.json");
        assertTrue(originalGrove.get("carvers").isJsonObject());
        assertEquals(1.0D, originalGrove.get("creature_spawn_probability").getAsDouble(), 0.0D);

        String migratedHash = DrehmalInstallFiles.sha256(datapack);
        Drehmal26_2CompatMigrator.Report second = Drehmal26_2CompatMigrator.migrate(world);
        assertFalse(second.changed());
        assertEquals(migratedHash, DrehmalInstallFiles.sha256(datapack));
        assertEquals(migratedHash, second.migratedHash());
        assertArrayEquals(regionBytes, Files.readAllBytes(region.resolve("r.0.0.mca")));
    }

    @Test
    void upgradesCompatV1ArchiveAndKeepsExistingBackup() throws Exception {
        Path world = temp.resolve("v1-world");
        Path datapacks = world.resolve("datapacks");
        Files.createDirectories(datapacks);

        Path datapack = datapacks.resolve("hi_drehmal.zip");
        createCompatV1Datapack(datapack);

        Path backup = datapack.resolveSibling(datapack.getFileName() + Drehmal26_2CompatMigrator.BACKUP_SUFFIX);
        Files.writeString(backup, "existing-legacy-backup", StandardCharsets.UTF_8);
        String backupHash = DrehmalInstallFiles.sha256(backup);

        Files.writeString(
                world.resolve(Drehmal26_2CompatMigrator.MARKER_FILE),
                "compatVersion=1\ntarget=26.2\n",
                StandardCharsets.UTF_8);

        Drehmal26_2CompatMigrator.Report report = Drehmal26_2CompatMigrator.migrate(world);

        assertTrue(report.changed());
        assertTrue(Drehmal26_2CompatMigrator.isCurrent(world));
        assertEquals(backupHash, DrehmalInstallFiles.sha256(backup));

        JsonObject basalt = json(datapack, "data/minecraft/worldgen/biome/basalt_deltas.json");
        JsonObject attributes = basalt.getAsJsonObject("attributes");
        assertTrue(attributes == null || !attributes.has("minecraft:audio/ambient_sounds"));
        assertTrue(attributes == null || !attributes.has("minecraft:audio/background_music"));
        assertTrue(Files.readString(world.resolve(Drehmal26_2CompatMigrator.MARKER_FILE))
                .contains("compatVersion=2"));
    }

    private static void createLegacyDatapack(Path archive) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            write(zip, "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"fixture\"}}");
            write(zip, "data/minecraft/dimension_type/space_type.json",
                    "{\"ultrawarm\":false,\"natural\":true,\"has_skylight\":false,\"has_ceiling\":false,"
                            + "\"coordinate_scale\":1,\"ambient_light\":0.1,\"logical_height\":256,"
                            + "\"infiniburn\":\"#minecraft:infiniburn_overworld\",\"min_y\":0,\"height\":256,"
                            + "\"monster_spawn_light_level\":0,\"monster_spawn_block_light_limit\":0}");

            for (int i = 0; i < 48; i++) {
                String name = i == 0 ? "grove" : i == 1 ? "basalt_deltas" : "fixture_" + i;
                String carvers = i == 0
                        ? "{\"air\":[\"minecraft:cave\",\"minecraft:canyon\"]}"
                        : "{}";
                String probability = i == 0 ? ",\"creature_spawn_probability\":1" : "";
                String effects = i == 0
                        ? "{\"sky_color\":3476518,\"fog_color\":4536402,\"water_color\":4159204,"
                        + "\"water_fog_color\":329011,"
                        + "\"ambient_sound\":\"minecraft:ambient.cave\","
                        + "\"particle\":{\"options\":{\"type\":\"minecraft:splash\"},\"probability\":0.001}}"
                        : i == 1
                        ? "{\"sky_color\":0,\"fog_color\":0,\"water_color\":4159204,\"water_fog_color\":329011,"
                        + "\"mood_sound\":{\"sound\":\"minecraft:null\",\"tick_delay\":0,"
                        + "\"block_search_extent\":0,\"offset\":0},"
                        + "\"additions_sound\":{\"sound\":\"minecraft:null\",\"tick_chance\":0},"
                        + "\"music\":{\"sound\":\"minecraft:null\",\"min_delay\":0,\"max_delay\":0,"
                        + "\"replace_current_music\":true}}"
                        : "{\"water_color\":4159204}";
                write(zip, "data/minecraft/worldgen/biome/" + name + ".json",
                        "{\"temperature\":0.8,\"downfall\":0.4,\"has_precipitation\":true,"
                                + "\"effects\":" + effects + ",\"spawners\":{},\"spawn_costs\":{},"
                                + "\"carvers\":" + carvers + ",\"features\":[]" + probability + "}");
            }
        }
    }

    private static void createCompatV1Datapack(Path archive) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive), StandardCharsets.UTF_8)) {
            write(zip, "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"fixture\"}}");
            write(zip, "data/minecraft/dimension_type/space_type.json",
                    "{\"has_ender_dragon_fight\":false,\"ultrawarm\":false,\"natural\":true,"
                            + "\"has_skylight\":false,\"has_ceiling\":false,\"coordinate_scale\":1,"
                            + "\"ambient_light\":0.1,\"logical_height\":256,"
                            + "\"infiniburn\":\"#minecraft:infiniburn_overworld\",\"min_y\":0,\"height\":256,"
                            + "\"monster_spawn_light_level\":0,\"monster_spawn_block_light_limit\":0}");

            for (int i = 0; i < 48; i++) {
                String name = i == 0 ? "basalt_deltas" : "fixture_" + i;
                String attributes = i == 0
                        ? ",\"attributes\":{\"minecraft:audio/ambient_sounds\":{"
                        + "\"mood\":{\"sound\":\"minecraft:null\",\"tick_delay\":0,"
                        + "\"block_search_extent\":0,\"offset\":0},"
                        + "\"additions\":{\"sound\":\"minecraft:null\",\"tick_chance\":0}},"
                        + "\"minecraft:audio/background_music\":{\"default\":{"
                        + "\"sound\":\"minecraft:null\",\"min_delay\":0,\"max_delay\":0,"
                        + "\"replace_current_music\":true}}}"
                        : "";
                write(zip, "data/minecraft/worldgen/biome/" + name + ".json",
                        "{\"temperature\":0.8,\"downfall\":0.4,\"has_precipitation\":true,"
                                + "\"effects\":{\"water_color\":4159204},\"spawners\":{},\"spawn_costs\":{},"
                                + "\"carvers\":[],\"features\":[]" + attributes + "}");
            }
        }
    }

    private static void write(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static JsonObject json(Path archive, String entryName) throws Exception {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            try (InputStream stream = zip.getInputStream(entry)) {
                return JsonParser.parseReader(
                        new java.io.InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }
    }
}
