package dev.moonseungjun.openworldrpg.mount;

import java.util.Objects;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * Server seam for authored no-mount volumes. Until a region provider registers, ordinary world
 * space is legal; once registered, the provider is authoritative for every summon candidate.
 */
public final class TrailStagMountVolumeAuthority {
    private static volatile BiPredicate<ServerLevel, AABB> allowed = (level, box) -> true;
    private static volatile boolean registered;

    private TrailStagMountVolumeAuthority() {
    }

    public static synchronized void register(BiPredicate<ServerLevel, AABB> provider) {
        Objects.requireNonNull(provider, "provider");
        if (registered) {
            throw new IllegalStateException("Trail Stag mount-volume authority is already registered.");
        }
        allowed = provider;
        registered = true;
    }

    public static boolean isAllowed(ServerLevel level, AABB box) {
        return allowed.test(Objects.requireNonNull(level, "level"), Objects.requireNonNull(box, "box"));
    }
}
