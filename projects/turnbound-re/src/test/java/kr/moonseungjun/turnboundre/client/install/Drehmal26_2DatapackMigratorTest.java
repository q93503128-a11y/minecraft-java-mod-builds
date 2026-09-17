package kr.moonseungjun.turnboundre.client.install;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Drehmal26_2DatapackMigratorTest {
    @TempDir
    Path temp;

    @Test
    void migratesObservedDrehmalRegistryBreaksAndKeepsOriginalBackup() throws Exception {
        Path world = temp.resolve("world");
        Path datapacks = world.resolve("datapacks");
        Files.createDirectories(datapacks);
        Path pack = datapacks.resolve("hi_drehmal.zip");

        byte[] grove = """
                {
                  "carvers":{"air":["minecraft:cave","minecraft:cave_extra_underground","minecraft:canyon"]},
                  "creature_spawn_probability":1.0,
                  "downfall":0.8,
                  "effects":{"water_color":4159204},
                  "features":[],
                  "has_precipitation":true,
                  "spawn_costs":{},
                  "spawners":{},
                  "temperature":-0.2
                }
                """.getBytes(StandardCharsets.UTF_8);
        byte[] emptyCarvers = """
                {
                  "carvers":{},
                  "downfall":0.5,
                  "effects":{"water_color":4159204},
                  "features":[],
                  "has_precipitation":false,
                  "spawn_costs":{},
                  "spawners":{},
                  "temperature":0.5
                }
                """.getBytes(StandardCharsets.UTF_8);
        byte[] spaceType = """
                {
                  "ultrawarm":false,
                  "natural":true,
                  "piglin_safe":false,
                  "respawn_anchor_works":false,
                  "bed_works":true,
                  "has_raids":true,
                  "has_skylight":false,
                  "has_ceiling":false,
                  "coordinate_scale":1,
                  "ambient_light":0.1,
                  "logical_height":256,
                  "effects":"minecraft:the_end",
                  "infiniburn":"#minecraft:infiniburn_overworld",
                  "min_y":0,
                  "height":256,
                  "monster_spawn_light_level":0,
                  "monster_spawn_block_light_limit":0
                }
                """.getBytes(StandardCharsets.UTF_8);

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(pack))) {
            put(output, "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"fixture\"}}");
            put(output, "data/minecraft/worldgen/biome/grove.json", grove);
            put(output, "data/minecraft/worldgen/biome/the_void.json", emptyCarvers);
            put(output, "data/minecraft/dimension_type/space_type.json", spaceType);
            put(output, "data/minecraft/functions/keep.mcfunction", "say untouched");
        }
        byte[] originalPack = Files.readAllBytes(pack);

        Drehmal26_2DatapackMigrator.MigrationReport report = Drehmal26_2DatapackMigrator.migrate(world);

        assertEquals(2, report.biomeFilesChanged());
        assertEquals(1, report.dimensionTypeFilesChanged());
        assertTrue(Drehmal26_2DatapackMigrator.compatibilityMarkerMatches(world));
        assertArrayEquals(originalPack, Files.readAllBytes(Drehmal26_2DatapackMigrator.backup(world)));

        try (ZipFile migrated = new ZipFile(pack.toFile())) {
            JsonObject migratedGrove = readJson(migrated, "data/minecraft/worldgen/biome/grove.json");
            JsonArray carvers = migratedGrove.getAsJsonArray("carvers");
            assertEquals(3, carvers.size());
            assertEquals("minecraft:cave", carvers.get(0).getAsString());
            assertEquals(0.9999999D, migratedGrove.get("creature_spawn_probability").getAsDouble(), 0.00000001D);

            JsonObject migratedVoid = readJson(migrated, "data/minecraft/worldgen/biome/the_void.json");
            assertTrue(migratedVoid.getAsJsonArray("carvers").isEmpty());

            JsonObject migratedSpace = readJson(migrated, "data/minecraft/dimension_type/space_type.json");
            assertFalse(migratedSpace.get("has_ender_dragon_fight").getAsBoolean());
            assertEquals("end", migratedSpace.get("skybox").getAsString());

            String untouched = new String(
                    migrated.getInputStream(migrated.getEntry("data/minecraft/functions/keep.mcfunction")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertEquals("say untouched", untouched);
        }

        String firstMigratedHash = DrehmalInstallFiles.sha256(pack);
        Drehmal26_2DatapackMigrator.MigrationReport second = Drehmal26_2DatapackMigrator.migrate(world);
        assertEquals(0, second.totalFilesChanged());
        assertEquals(firstMigratedHash, DrehmalInstallFiles.sha256(pack));
    }

    @Test
    void pureTransformsMatchObserved26_2CodecRequirements() {
        JsonObject biome = JsonParser.parseString("""
                {
                  "carvers":{"air":["minecraft:cave"],"liquid":["example:liquid"]},
                  "creature_spawn_probability":1.0
                }
                """).getAsJsonObject();
        JsonObject migratedBiome = Drehmal26_2DatapackMigrator.migrateBiomeObject(biome);
        assertTrue(migratedBiome.get("carvers").isJsonArray());
        assertEquals(2, migratedBiome.getAsJsonArray("carvers").size());
        assertEquals(0.9999999D, migratedBiome.get("creature_spawn_probability").getAsDouble(), 0.00000001D);

        JsonObject dimension = JsonParser.parseString("""
                {"has_skylight":false,"has_ceiling":false,"effects":"minecraft:the_end"}
                """).getAsJsonObject();
        JsonObject migratedDimension = Drehmal26_2DatapackMigrator.migrateDimensionTypeObject(
                "data/minecraft/dimension_type/space_type.json",
                dimension);
        assertFalse(migratedDimension.get("has_ender_dragon_fight").getAsBoolean());
        assertEquals("end", migratedDimension.get("skybox").getAsString());
    }

    private static JsonObject readJson(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        assertTrue(entry != null, "missing " + name);
        String json = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static void put(ZipOutputStream output, String name, String text) throws Exception {
        put(output, name, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void put(ZipOutputStream output, String name, byte[] bytes) throws Exception {
        output.putNextEntry(new ZipEntry(name));
        output.write(bytes);
        output.closeEntry();
    }
}
