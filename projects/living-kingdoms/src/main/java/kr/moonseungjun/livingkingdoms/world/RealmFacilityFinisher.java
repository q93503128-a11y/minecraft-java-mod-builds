package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Applies support-sensitive and overlap-sensitive finishing details once after a capital build. */
public final class RealmFacilityFinisher {
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final Set<MinecraftServer> FINISHED_SERVERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private RealmFacilityFinisher() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        synchronized (FINISHED_SERVERS) {
            if (FINISHED_SERVERS.contains(server)) return;
        }
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;
        RealmSiteLayoutSavedData.RealmSite erden = RealmSitePlanner.site(realm, "erden_kingdom");
        if (erden == null || !erden.built()) return;

        ErdenCapitalIntegrityFinalizer.ensure(realm, "erden_kingdom", erden);
        ensureCriticalFacilities(realm, erden);
        ConstructionDebrisCleaner.schedule(realm, "erden_kingdom", erden);
        synchronized (FINISHED_SERVERS) {
            FINISHED_SERVERS.add(server);
        }
    }

    public static void ensureCriticalFacilities(ServerLevel realm,
                                                RealmSiteLayoutSavedData.RealmSite erden) {
        int cx = erden.centerX();
        int cz = erden.centerZ();
        int y = Math.max(68, Math.min(112, erden.baseY()));

        // Farms, cottages and decorative trees are authored after the canal in the old plan and can
        // refill parts of its trench. Re-cut the waterway once, then restore intentional crossings.
        int canalBaseX = cx - 164;
        for (int z = cz - 142; z <= cz + 142; z++) {
            int bend = (int) Math.round(Math.sin(z * 0.045) * 6.0);
            for (int dx = -8; dx <= 8; dx++) {
                int x = canalBaseX + bend + dx;
                set(realm, x, y - 5, z, Math.abs(dx) > 5 ? Blocks.CLAY : Blocks.GRAVEL);
                for (int py = y - 4; py <= y - 1; py++) set(realm, x, py, z, Blocks.WATER);
                for (int py = y; py <= y + 6; py++) set(realm, x, py, z, Blocks.AIR);
            }
        }

        restoreBridge(realm, cx, y, cz);
        restoreDock(realm, cx, y, cz);
        restoreResidencePier(realm, cx, y, cz);

        BlockPos lantern = new BlockPos(cx + 32, y + 3, cz - 67);
        set(realm, lantern.below(2), Blocks.SPRUCE_FENCE);
        set(realm, lantern.below(), Blocks.SPRUCE_FENCE);
        set(realm, lantern, Blocks.LANTERN);

        ErdenFunctionalFacilityFinalizer.ensure(realm, erden);
    }

    private static void restoreBridge(ServerLevel realm, int cx, int y, int cz) {
        for (int x = cx - 172; x <= cx - 118; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) set(realm, x, y + 1, z, Blocks.STONE_BRICKS);
            set(realm, x, y + 2, cz - 4, Blocks.STONE_BRICK_WALL);
            set(realm, x, y + 2, cz + 4, Blocks.STONE_BRICK_WALL);
        }
    }

    private static void restoreDock(ServerLevel realm, int cx, int y, int cz) {
        int x = cx - 166;
        int z = cz + 67;
        for (int dx = -4; dx <= 18; dx++) {
            for (int dz = -4; dz <= 4; dz++) set(realm, x + dx, y, z + dz, Blocks.SPRUCE_PLANKS);
            if (Math.floorMod(dx, 5) == 0) {
                for (int py = y - 3; py <= y; py++) {
                    set(realm, x + dx, py, z - 4, Blocks.SPRUCE_LOG);
                    set(realm, x + dx, py, z + 4, Blocks.SPRUCE_LOG);
                }
            }
        }
        set(realm, x + 8, y + 1, z, Blocks.BARREL);
    }

    private static void restoreResidencePier(ServerLevel realm, int cx, int y, int cz) {
        int x = cx - 172;
        int z = cz + 116;
        for (int dz = 0; dz <= 22; dz++) {
            for (int dx = -2; dx <= 2; dx++) set(realm, x + dx, y, z + dz, Blocks.SPRUCE_PLANKS);
            if (dz % 5 == 0) {
                for (int py = y - 3; py <= y; py++) {
                    set(realm, x - 2, py, z + dz, Blocks.SPRUCE_LOG);
                    set(realm, x + 2, py, z + dz, Blocks.SPRUCE_LOG);
                }
            }
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        set(level, new BlockPos(x, y, z), block);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) return;
        if (level.getBlockState(pos).getBlock() == block) return;
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }
}
