package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalResourceRepairDiscoveryTest {
    @TempDir Path temp;

    @Test
    void findsManualDrehmalSaveWithoutBindingArbitraryWorlds() throws Exception {
        Path saves=temp.resolve("saves");
        Path drehmal=saves.resolve("Drehmal");
        Path ordinary=saves.resolve("Ordinary");
        Files.createDirectories(drehmal.resolve("datapacks"));
        Files.createDirectories(ordinary);
        Files.write(drehmal.resolve("level.dat"),new byte[]{1});
        Files.write(drehmal.resolve("resources.zip"),new byte[]{1});
        Files.write(drehmal.resolve("datapacks/hi_drehmal.zip"),new byte[]{1});
        Files.write(ordinary.resolve("level.dat"),new byte[]{1});
        Files.write(ordinary.resolve("resources.zip"),new byte[]{1});

        assertTrue(DrehmalResourceRepairDiscovery.looksLikeDrehmal(drehmal));
        assertFalse(DrehmalResourceRepairDiscovery.looksLikeDrehmal(ordinary));
        assertEquals(java.util.List.of(drehmal),DrehmalResourceRepairDiscovery.find(temp));
    }

    @Test
    void legacySpawnEggSignatureRepairsOldInstanceEvenWithoutTurnboundMarker() throws Exception {
        Path world=temp.resolve("legacy-instance");
        Files.createDirectories(world);
        Files.write(world.resolve("level.dat"),new byte[]{1});
        try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(world.resolve("resources.zip")), StandardCharsets.UTF_8)){
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write("{\"pack\":{\"pack_format\":15,\"description\":\"legacy\"}}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("assets/minecraft/models/item/cat_spawn_egg.json"));
            zip.write(("{\"parent\":\""+Drehmal26_2ResourcePackMigrator.LEGACY_SPAWN_EGG_PARENT+"\"}")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertTrue(DrehmalResourceRepairDiscovery.looksLikeDrehmal(world));
    }

    @Test
    void boundProfileIsAlsoStrongRepairEvidence() throws Exception {
        Path world=temp.resolve("world");
        Files.createDirectories(world);
        Files.write(world.resolve("level.dat"),new byte[]{1});
        Files.write(world.resolve("resources.zip"),new byte[]{1});
        Files.writeString(world.resolve(".turnbound_world_profile"),"turnbound:drehmal_apotheosis_2_2_2f");
        assertTrue(DrehmalResourceRepairDiscovery.looksLikeDrehmal(world));
    }
}
