package io.github.q93503128.turnbound;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

/**
 * Original saves are read-only to TURNBOUND unless the *copied* world has an exact opt-in marker.
 * No prefix, world-name or random-save heuristics are accepted.
 */
public final class TurnboundWorldGate {
    public static final String MARKER_FILENAME = "turnbound-world.marker";
    public static final String MARKER_CONTENT = "TURNBOUND_DREHMAL_WORLD_V1";
    private static final Set<MinecraftServer> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<>());

    private TurnboundWorldGate() {}

    public static void open(MinecraftServer server) {
        Path marker = server.getSavePath(WorldSavePath.ROOT).resolve(MARKER_FILENAME);
        try {
            if (Files.isRegularFile(marker)
                    && Files.readString(marker, StandardCharsets.UTF_8).trim().equals(MARKER_CONTENT)) {
                ACTIVE.add(server);
            }
        } catch (IOException exception) {
            // Fail closed: a missing or unreadable marker must never affect an unrelated save.
            ACTIVE.remove(server);
        }
    }

    public static void close(MinecraftServer server) {
        ACTIVE.remove(server);
    }

    public static boolean active(MinecraftServer server) {
        return server != null && ACTIVE.contains(server);
    }
}
