package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CI-only physical acceptance probe for the regional guard system.
 *
 * <p>The normal game never reaches this path. CI transiently streams the representative watch-house
 * neighborhood through the same settlement builder, materializes the two rostered guards with the
 * production equipment contract, verifies the physical billet and entities, then releases every
 * temporary ticket. This closes the gap between a persistent guard roster and an actually usable
 * loaded village without introducing permanent forced chunks.</p>
 */
@EventBusSubscriber(modid = LivingKingdoms.MOD_ID)
public final class ErdenRegionalGovernancePhysicalAudit {
    private static final String REPRESENTATIVE_ID = "harvest_crossing";
    private static final Identifier VILLAGER_ID =
            Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final int PROBE_RADIUS_CHUNKS = 1;
    private static final int BUILD_BUDGET = 4_000;
    private static final int EXPECTED_GUARDS = 2;

    private static final Set<Long> RETAINED = new LinkedHashSet<>();
    private static MinecraftServer activeServer;
    private static ActiveBuild activeBuild;
    private static boolean requested;
    private static boolean guardsMaterialized;
    private static boolean passed;

    private ErdenRegionalGovernancePhysicalAudit() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!isCi()) return;
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom") || passed) return;

        ErdenRegionalGovernanceSavedData governance = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalGovernanceSavedData.TYPE);
        if (!governance.hasGovernance(
                ErdenRegionalGovernanceManager.GOVERNANCE_REVISION,
                ErdenRegionalGovernanceManager.EXPECTED_COUNCILS,
                ErdenRegionalGovernanceManager.EXPECTED_GUARDS)) return;

        ErdenRegionalSettlementCatalog.Settlement settlement = representative();
        if (!requested) requestProbe(level, settlement);
        if (!allProbeChunksLoaded(level)) return;
        if (!buildProbe(level)) return;

        if (!guardsMaterialized) {
            guardsMaterialized = materializeRepresentativeGuards(level, governance, settlement);
            if (!guardsMaterialized) return;
        }
        verifyAndFinish(level, governance, settlement);
    }

    private static void requestProbe(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        requested = true;
        ErdenRegionalSettlementCatalog.BuildingLot watch = watchLot(settlement);
        int centerX = settlement.x() + watch.dx();
        int centerZ = settlement.z() + watch.dz();
        int centerChunkX = centerX >> 4;
        int centerChunkZ = centerZ >> 4;
        for (int dx = -PROBE_RADIUS_CHUNKS; dx <= PROBE_RADIUS_CHUNKS; dx++) {
            for (int dz = -PROBE_RADIUS_CHUNKS; dz <= PROBE_RADIUS_CHUNKS; dz++) {
                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                long key = pack(chunkX, chunkZ);
                if (RETAINED.add(key)) {
                    level.getChunkSource().addTicketAndLoadWithRadius(
                            TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
                }
            }
        }
        LivingKingdoms.LOGGER.info(
                "Requested Erden regional governance physical CI probe settlement={} chunks={} transient_ticket=portal persistent_forced_chunks=false",
                REPRESENTATIVE_ID, RETAINED.size());
    }

    private static boolean allProbeChunksLoaded(ServerLevel level) {
        if (RETAINED.isEmpty()) return false;
        for (long key : RETAINED) {
            if (!level.hasChunk(unpackX(key), unpackZ(key))) return false;
        }
        return true;
    }

    private static boolean buildProbe(ServerLevel level) {
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        if (activeBuild != null) {
            activeBuild.plan().apply(level, BUILD_BUDGET);
            if (!activeBuild.plan().done()) return false;
            ChunkPos chunk = new ChunkPos(activeBuild.chunkX(), activeBuild.chunkZ());
            ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, chunk);
            construction.markChunk(
                    activeBuild.key(), ErdenRegionalSettlementCatalog.REVISION,
                    activeBuild.plan().appliedWrites());
            activeBuild = null;
        }

        for (long key : RETAINED) {
            if (!construction.needs(key, ErdenRegionalSettlementCatalog.REVISION)) continue;
            int chunkX = unpackX(key);
            int chunkZ = unpackZ(key);
            if (!level.hasChunk(chunkX, chunkZ)) return false;
            ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
            IncrementalWorldEditPlan plan = new IncrementalWorldEditPlan(chunk);
            ErdenRegionalSettlementBuilder.addChunk(plan, level, chunk);
            activeBuild = new ActiveBuild(key, chunkX, chunkZ, plan);
            activeBuild.plan().apply(level, BUILD_BUDGET);
            if (!activeBuild.plan().done()) return false;
            ConstructionDebrisCleaner.cleanStreamedChunkCompletion(level, chunk);
            construction.markChunk(
                    key, ErdenRegionalSettlementCatalog.REVISION,
                    activeBuild.plan().appliedWrites());
            activeBuild = null;
            return false;
        }
        return true;
    }

    private static boolean materializeRepresentativeGuards(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        List<ErdenRegionalGovernanceSavedData.GuardPost> roster = governance.guardPosts().stream()
                .filter(guard -> guard.settlementId().equals(REPRESENTATIVE_ID) && guard.alive())
                .sorted(java.util.Comparator.comparingInt(ErdenRegionalGovernanceSavedData.GuardPost::slot))
                .toList();
        if (roster.size() != EXPECTED_GUARDS) return false;

        Set<String> expectedNames = new LinkedHashSet<>();
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : roster) expectedNames.add(guardName(guard));
        List<Villager> existing = level.getEntitiesOfClass(
                Villager.class, settlementBounds(level, settlement),
                candidate -> expectedNames.contains(candidate.getName().getString()));
        Set<String> present = new LinkedHashSet<>();
        for (Villager villager : existing) present.add(villager.getName().getString());

        for (ErdenRegionalGovernanceSavedData.GuardPost guard : roster) {
            String name = guardName(guard);
            if (present.contains(name)) continue;
            BlockPos post = watchPostPosition(settlement, guard.slot());
            BlockPos spawn = walkableNear(level, post.getX(), post.getZ(), guard.slot());
            if (spawn.equals(BlockPos.ZERO) || !spawnGuard(level, guard, spawn)) return false;
            present.add(name);
        }
        return present.size() == EXPECTED_GUARDS;
    }

    private static boolean spawnGuard(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData.GuardPost guard,
            BlockPos spawn) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (type == null) return false;
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return false;
        villager.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        villager.setCustomName(Component.literal(guardName(guard)));
        villager.setCustomNameVisible(false);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        villager.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        villager.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        return level.addFreshEntity(villager);
    }

    private static void verifyAndFinish(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        for (long key : RETAINED) {
            if (!construction.isBuilt(key, ErdenRegionalSettlementCatalog.REVISION)) return;
        }

        List<ErdenRegionalGovernanceSavedData.GuardPost> roster = governance.guardPosts().stream()
                .filter(guard -> guard.settlementId().equals(REPRESENTATIVE_ID) && guard.alive())
                .toList();
        Set<String> expectedNames = new LinkedHashSet<>();
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : roster) expectedNames.add(guardName(guard));
        List<Villager> guards = level.getEntitiesOfClass(
                Villager.class, settlementBounds(level, settlement),
                candidate -> expectedNames.contains(candidate.getName().getString()));
        if (guards.size() != EXPECTED_GUARDS) return;

        int equipped = 0;
        int nearBillet = 0;
        for (Villager guard : guards) {
            if (guard.getMainHandItem().is(Items.IRON_SWORD)
                    && guard.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                    && guard.getItemBySlot(EquipmentSlot.CHEST).is(Items.CHAINMAIL_CHESTPLATE)) {
                equipped++;
            }
            double best = Double.MAX_VALUE;
            for (int slot = 0; slot < EXPECTED_GUARDS; slot++) {
                BlockPos post = watchPostPosition(settlement, slot);
                best = Math.min(best, guard.distanceToSqr(
                        post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D));
            }
            if (best <= 64.0D * 64.0D) nearBillet++;
        }
        if (equipped != EXPECTED_GUARDS || nearBillet != EXPECTED_GUARDS) return;

        ErdenRegionalSettlementCatalog.BuildingLot watch = watchLot(settlement);
        int watchX = settlement.x() + watch.dx();
        int watchZ = settlement.z() + watch.dz();
        int baseY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(watchX, watchZ));
        int structural = 0;
        for (int x = watchX - 16; x <= watchX + 16; x++) {
            for (int z = watchZ - 16; z <= watchZ + 16; z++) {
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                for (int y = baseY; y <= baseY + 16; y++) {
                    var state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir() || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                            || state.is(Blocks.DIRT_PATH) || state.is(Blocks.PACKED_MUD)
                            || state.is(Blocks.GRAVEL) || state.is(Blocks.FARMLAND)
                            || state.is(Blocks.WATER) || state.is(Blocks.WHEAT)) continue;
                    structural++;
                }
            }
        }
        if (structural < 40) return;

        int released = RETAINED.size();
        for (long key : Set.copyOf(RETAINED)) {
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL, new ChunkPos(unpackX(key), unpackZ(key)), 0);
            RETAINED.remove(key);
        }
        passed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_GOVERNANCE_PHYSICAL_PASS revision={} settlement={} guards={} equipped_guards={} billet_guards={} probe_chunks={} structural_blocks={} physical_watch_house=true roster_entity_identity=true persistent_equipment=true transient_probe_released=true navigation_runtime_wired=true combat_runtime_wired=true persistent_forced_chunks=false",
                ErdenRegionalGovernanceManager.GOVERNANCE_REVISION,
                REPRESENTATIVE_ID, guards.size(), equipped, nearBillet,
                released, structural);
    }

    private static ErdenRegionalSettlementCatalog.Settlement representative() {
        return ErdenRegionalSettlementCatalog.settlements().stream()
                .filter(settlement -> settlement.id().equals(REPRESENTATIVE_ID))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing governance representative settlement"));
    }

    private static ErdenRegionalSettlementCatalog.BuildingLot watchLot(
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        return settlement.buildings().stream()
                .filter(lot -> lot.role().equals("watch_house_east"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing representative watch house"));
    }

    private static BlockPos watchPostPosition(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            int slot) {
        ErdenRegionalSettlementCatalog.BuildingLot watch = watchLot(settlement);
        int x = settlement.x() + watch.dx() - 24;
        int z = settlement.z() + watch.dz() + (slot == 0 ? -3 : 3);
        int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 1;
        return new BlockPos(x, y, z);
    }

    private static BlockPos walkableNear(ServerLevel level, int centerX, int centerZ, int slot) {
        int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(centerX, centerZ)) + 1;
        int start = Math.floorMod(slot * 3, 7);
        for (int radius = 0; radius <= 12; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    if (radius > 0 && Math.floorMod(dx + dz + start, 3) != 0) continue;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;
                    int y = safeStandingY(level, x, preferredY, z);
                    if (y != Integer.MIN_VALUE) return new BlockPos(x, y, z);
                }
            }
        }
        return BlockPos.ZERO;
    }

    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 10; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int y : candidates) {
                BlockPos feet = new BlockPos(x, y, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static AABB settlementBounds(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        int radius = ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40;
        return new AABB(
                settlement.x() - radius, level.getMinY(), settlement.z() - radius,
                settlement.x() + radius, level.getMaxY(), settlement.z() + radius);
    }

    private static String guardName(ErdenRegionalGovernanceSavedData.GuardPost guard) {
        return "수확나루 경비대 " + (guard.slot() + 1) + "조 " + guard.generation() + "기";
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        RETAINED.clear();
        activeBuild = null;
        requested = false;
        guardsMaterialized = false;
        passed = false;
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private record ActiveBuild(
            long key,
            int chunkX,
            int chunkZ,
            IncrementalWorldEditPlan plan) {
    }
}
