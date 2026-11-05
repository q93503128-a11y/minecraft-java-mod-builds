package io.github.q93503128.turnbound.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Finds only saves with strong local evidence of being Drehmal for pre-resource-load compatibility repair.
 * This does not bind a world to TURNBOUND and never scans arbitrary resource packs outside saves.
 */
final class DrehmalResourceRepairDiscovery {
    private static final String PROFILE_MARKER = ".turnbound_world_profile";
    private static final String DREHMAL_DATAPACK = "datapacks/hi_drehmal.zip";

    private DrehmalResourceRepairDiscovery() {}

    static List<Path> find(Path gameDir) {
        if (gameDir == null) return List.of();
        Path saves = gameDir.resolve("saves");
        if (!Files.isDirectory(saves)) return List.of();
        try (var stream = Files.list(saves)) {
            return stream.filter(Files::isDirectory)
                    .filter(DrehmalResourceRepairDiscovery::looksLikeDrehmal)
                    .sorted()
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    static boolean looksLikeDrehmal(Path world) {
        if (world == null
                || !Files.isRegularFile(world.resolve("level.dat"))
                || !Files.isRegularFile(world.resolve("resources.zip"))) {
            return false;
        }
        return Files.isRegularFile(world.resolve(PROFILE_MARKER))
                || Files.isRegularFile(world.resolve(DREHMAL_DATAPACK))
                || Drehmal26_2ResourcePackMigrator.hasLegacySignature(world);
    }
}
