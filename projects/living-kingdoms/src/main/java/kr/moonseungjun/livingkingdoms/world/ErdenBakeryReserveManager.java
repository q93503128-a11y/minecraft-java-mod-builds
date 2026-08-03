package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconciles unsold shop bread into a two-loaf opening reserve for every bakery. The operation
 * conserves stock: bread is moved out of shop inventories, not created. Loaded containers are
 * updated immediately so the saved economy and the visible world cannot diverge on the next sync.
 */
public final class ErdenBakeryReserveManager {
    public static final long RESERVE_PER_BAKERY = 2L;

    private static MinecraftServer activeServer;
    private static long lastLoggedDay = Long.MIN_VALUE;

    private ErdenBakeryReserveManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            activeServer = server;
            lastLoggedDay = Long.MIN_VALUE;
        }
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        if (economy.lastProcessedDay() < 0L
                || economy.sites().size() != ErdenAuthoritativeEconomyManager.EXPECTED_SITES) {
            return;
        }

        Set<Long> protectedSamples = protectedShopSamples();
        List<ErdenPhysicalEconomySavedData.SiteState> bakeries = economy.sites().stream()
                .filter(site -> site.role().equals("bakery"))
                .sorted(Comparator.comparing(ErdenPhysicalEconomySavedData.SiteState::id))
                .toList();
        int transfers = 0;
        long movedTotal = 0L;

        for (ErdenPhysicalEconomySavedData.SiteState bakerySnapshot : bakeries) {
            ErdenPhysicalEconomySavedData.SiteState bakery = findSite(economy, bakerySnapshot.id());
            if (bakery == null) continue;
            long missing = Math.max(0L, RESERVE_PER_BAKERY - bakery.stock("bread"));
            while (missing > 0L) {
                ErdenPhysicalEconomySavedData.SiteState donor = economy.sites().stream()
                        .filter(site -> site.role().equals("shop")
                                && site.stock("bread") > 0L
                                && !protectedSamples.contains(positionKey(site.x(), site.z())))
                        .max(Comparator.comparingLong(
                                        (ErdenPhysicalEconomySavedData.SiteState site) -> site.stock("bread"))
                                .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                        .orElse(null);
                if (donor == null) break;
                long amount = Math.min(missing, donor.stock("bread"));
                ErdenPhysicalEconomySavedData.SiteState updatedDonor = donor
                        .addStock("bread", -amount)
                        .addMetric("reserve_sent", amount);
                bakery = bakery
                        .addStock("bread", amount)
                        .addMetric("reserve_received", amount);
                economy.replaceSite(updatedDonor);
                economy.replaceSite(bakery);
                syncBreadContainer(level, updatedDonor);
                syncBreadContainer(level, bakery);
                missing -= amount;
                movedTotal += amount;
                transfers++;
            }
        }

        long reserveTotal = 0L;
        int complete = 0;
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (!site.role().equals("bakery")) continue;
            reserveTotal += Math.min(RESERVE_PER_BAKERY, site.stock("bread"));
            if (site.stock("bread") >= RESERVE_PER_BAKERY) complete++;
        }
        if (complete == bakeries.size()
                && lastLoggedDay != economy.lastProcessedDay()) {
            lastLoggedDay = economy.lastProcessedDay();
            LivingKingdoms.LOGGER.info(
                    "Reconciled Erden bakery reserves day={} bakeries={} reserve_bread={} transfers={} moved={} conserved=true",
                    economy.lastProcessedDay(), complete, reserveTotal, transfers, movedTotal);
        }
    }

    private static Set<Long> protectedShopSamples() {
        Set<Long> result = new HashSet<>();
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ErdenAuthoritativeEconomyManager.ciEntrances()) {
            if (entrance.role().equals("shop")) {
                result.add(positionKey(entrance.x(), entrance.z()));
            }
        }
        return result;
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            ErdenPhysicalEconomySavedData economy,
            String id) {
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (site.id().equals(id)) return site;
        }
        return null;
    }

    private static void syncBreadContainer(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState site) {
        BlockPos pos = primaryContainerPos(level, site);
        if (pos == null) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) return;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(Items.BREAD)) {
                container.setItem(slot, ItemStack.EMPTY);
            }
        }
        long remaining = Math.min(192L, site.stock("bread"));
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0L; slot++) {
            if (!container.getItem(slot).isEmpty()) continue;
            int count = (int) Math.min(64L, remaining);
            container.setItem(slot, new ItemStack(Items.BREAD, count));
            remaining -= count;
        }
        container.setChanged();
    }

    private static BlockPos primaryContainerPos(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState site) {
        if (!level.hasChunk(site.x() >> 4, site.z() >> 4)) return null;
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(site.x(), site.z());
        if (entrance == null) return null;
        ErdenUrbanInteriorSavedData interiors = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        if (!interiors.isComplete(
                positionKey(site.x(), site.z()),
                ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) return null;
        int doorY = findLowestDoorY(level, site.x(), site.z());
        if (doorY == Integer.MIN_VALUE) return null;
        Room room = room(entrance, doorY - 1);
        Point point = switch (site.role()) {
            case "shop" -> room.point(-3, 7);
            case "bakery" -> room.point(-3, 5);
            default -> null;
        };
        return point == null ? null : new BlockPos(point.x, room.floorY + 1, point.z);
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
    }

    private static int findLowestDoorY(ServerLevel level, int x, int z) {
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 8);
        int maximum = Math.min(level.getMaxY() - 1, designed + 64);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static Room room(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int floorY) {
        int deltaX = entrance.roadX() - entrance.x();
        int deltaZ = entrance.roadZ() - entrance.z();
        int inwardX;
        int inwardZ;
        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            inwardX = deltaX >= 0 ? -1 : 1;
            inwardZ = 0;
        } else {
            inwardX = 0;
            inwardZ = deltaZ >= 0 ? -1 : 1;
        }
        return new Room(
                floorY, entrance.x(), entrance.z(),
                inwardX, inwardZ, -inwardZ, inwardX);
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record Point(int x, int z) {
    }

    private record Room(
            int floorY,
            int originX,
            int originZ,
            int inwardX,
            int inwardZ,
            int rightX,
            int rightZ) {
        Point point(int lateral, int depth) {
            return new Point(
                    originX + inwardX * depth + rightX * lateral,
                    originZ + inwardZ * depth + rightZ * lateral);
        }
    }
}
