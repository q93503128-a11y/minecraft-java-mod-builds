package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void boundProfileIsAlsoStrongRepairEvidence() throws Exception {
        Path world=temp.resolve("world");
        Files.createDirectories(world);
        Files.write(world.resolve("level.dat"),new byte[]{1});
        Files.write(world.resolve("resources.zip"),new byte[]{1});
        Files.writeString(world.resolve(".turnbound_world_profile"),"turnbound:drehmal_apotheosis_2_2_2f");
        assertTrue(DrehmalResourceRepairDiscovery.looksLikeDrehmal(world));
    }
}
