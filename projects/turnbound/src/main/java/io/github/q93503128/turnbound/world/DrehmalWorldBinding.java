package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Identifies a TURNBOUND production save without modifying the authored map.
 *
 * <p>The marker follows the validated TURNBOUND: RE pattern: an arbitrary Minecraft save is never treated as a
 * production world merely because the mod is installed. Manual binding additionally requires an operator to stand
 * near the configured New Drabyel integration seed. A future first-run installer may write the same marker only
 * after it has independently verified the pinned official world.</p>
 */
public final class DrehmalWorldBinding {
    public static final String PROFILE_MARKER_FILE = ".turnbound_world_profile";
    private static final double MANUAL_BIND_DISTANCE_SQR = 192.0D * 192.0D;
    private static final Map<MinecraftServer, Boolean> BOUND_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private DrehmalWorldBinding() {}

    public static List<String> validate() {
        List<String> errors = new ArrayList<>(DrehmalWorldProfile.validate());
        errors.addAll(DrehmalFirstRouteCatalog.validate());
        errors.addAll(DrabyelHubServiceCatalog.validate());
        return List.copyOf(errors);
    }

    /**
     * World marker I/O is intentionally cached per live server. The profile marker is immutable during normal play;
     * manual binding updates the cache in the same operation that writes it.
     */
    public static boolean isBound(MinecraftServer server) {
        if (server == null || !validate().isEmpty()) return false;
        Boolean cached = BOUND_CACHE.get(server);
        if (cached != null) return cached;

        boolean bound = readMarker(server);
        BOUND_CACHE.put(server, bound);
        return bound;
    }

    public static BlockPos hubSeed() {
        return position(required(DrehmalWorldProfile.HUB_LOCATOR));
    }

    public static BlockPos firstRegionSeed() {
        return position(required(DrehmalWorldProfile.FIRST_REGION_LOCATOR));
    }

    /**
     * Operator-confirmed binding for a separately installed official world.
     * This writes one TURNBOUND marker file and does not change terrain, entities, datapacks or resource packs.
     */
    public static void bindManual(ServerPlayer operator) {
        if (operator == null) throw new IllegalArgumentException("operator required");
        List<String> errors = validate();
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("; ", errors));
        if (!DrehmalWorldProfile.profile().dimension()
                .equals(operator.level().dimension().identifier().toString())) {
            throw new IllegalStateException("Drehmal binding must be run in " + DrehmalWorldProfile.profile().dimension());
        }

        BlockPos hub = hubSeed();
        if (operator.distanceToSqr(
                hub.getX() + 0.5D,
                hub.getY() + 0.5D,
                hub.getZ() + 0.5D) > MANUAL_BIND_DISTANCE_SQR) {
            throw new IllegalStateException(
                    "stand near the configured New Drabyel integration seed before binding this save");
        }

        MinecraftServer server = operator.level().getServer();
        if (server == null) throw new IllegalStateException("server unavailable");
        Path marker = marker(server);
        try {
            if (Files.isRegularFile(marker)) {
                String current = Files.readString(marker, StandardCharsets.UTF_8).trim();
                if (!current.isBlank() && !DrehmalWorldProfile.PROFILE_ID.equals(current)) {
                    throw new IllegalStateException("world already carries a different TURNBOUND profile marker");
                }
            }
            Files.writeString(
                    marker,
                    DrehmalWorldProfile.PROFILE_ID + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            BOUND_CACHE.put(server, true);
        } catch (IOException exception) {
            throw new IllegalStateException("could not write TURNBOUND external-world marker", exception);
        }
    }

    public static String status(MinecraftServer server) {
        List<String> errors = validate();
        if (!errors.isEmpty()) return "profile invalid: " + String.join("; ", errors);
        return isBound(server)
                ? "bound · " + DrehmalWorldProfile.PROFILE_ID
                : "not bound · waiting for verified/manual Drehmal world binding";
    }

    public static void forget(MinecraftServer server) {
        if (server != null) BOUND_CACHE.remove(server);
    }

    private static boolean readMarker(MinecraftServer server) {
        Path marker = marker(server);
        try {
            return Files.isRegularFile(marker)
                    && DrehmalWorldProfile.PROFILE_ID.equals(
                            Files.readString(marker, StandardCharsets.UTF_8).trim());
        } catch (IOException ignored) {
            return false;
        }
    }

    private static DrehmalWorldProfile.Anchor required(String locator) {
        DrehmalWorldProfile.Anchor anchor = DrehmalWorldProfile.enabled(locator);
        if (anchor == null) throw new IllegalStateException("Missing enabled external-world anchor " + locator);
        return anchor;
    }

    private static BlockPos position(DrehmalWorldProfile.Anchor anchor) {
        return new BlockPos(anchor.x(), anchor.y(), anchor.z());
    }

    private static Path marker(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(PROFILE_MARKER_FILE);
    }
}
