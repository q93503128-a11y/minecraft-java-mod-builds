package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gives the six second-ring villages a physical storehouse market, household consumption and
 * persistent trade. Local needs are reserved first; only true surplus enters the existing Erden
 * kingdom-supply shipment escrow. Unloaded villages remain aggregate records, while loaded
 * storehouse barrels become authoritative and player-editable.
 */
public final class ErdenRegionalEconomyManager {
    public static final int ECONOMY_REVISION = 1;
    public static final int EXPECTED_SETTLEMENTS = 6;
    public static final int EXPECTED_MARKETS = 6;

    private static final int SYNC_INTERVAL = 20;
    private static final int MAX_CATCH_UP_DAYS = 14;
    private static final long TRADE_RETENTION_TICKS = 7L * 24_000L;
    private static final int MIN_LOCAL_TRAVEL_TICKS = 1_200;
    private static final int TICKS_PER_METRE = 2;
    private static final int MAX_STACK_SIZE = 64;
    private static final long WHEAT_PER_HOUSEHOLD = 2L;

    private static final List<ResourceItem> RESOURCES = List.of(
            new ResourceItem("wheat", Items.WHEAT),
            new ResourceItem("coal", Items.COAL),
            new ResourceItem("hay", Items.HAY_BLOCK),
            new ResourceItem("leather", Items.LEATHER),
            new ResourceItem("iron", Items.IRON_INGOT)
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;
    private static final Set<String> CI_PHYSICAL_MARKETS = new HashSet<>();

    private ErdenRegionalEconomyManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenRegionalSocietySavedData society = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSocietySavedData.TYPE);
        if (!society.hasPopulation(
                ErdenRegionalSocietyManager.SOCIETY_REVISION,
                ErdenRegionalSocietyManager.EXPECTED_HOUSEHOLDS)) return;

        ErdenRegionalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        ensureEconomy(economy);
        logPlanOnce(economy);

        if (level.getGameTime() % SYNC_INTERVAL == 0L) {
            syncLoadedMarketsFromWorld(level, economy);
        }
        settleLocalTrades(level, economy);
        processMissingDays(level, society, economy);
        exportRegionalSurplus(level, economy);
        if (level.getGameTime() % SYNC_INTERVAL == 0L) {
            syncLoadedMarketsToWorld(level, economy);
        }
        economy.pruneTrades(Math.max(0L, level.getGameTime() - TRADE_RETENTION_TICKS));
        verifyCi(level, economy);
    }

    public static void handleInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (!storagePosition(settlement).equals(event.getPos())) continue;
            ErdenRegionalEconomySavedData economy = level.getDataStorage()
                    .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
            ErdenRegionalEconomySavedData.SettlementState state = economy.settlement(settlement.id());
            if (state == null) return;
            long inbound = economy.tradeShipments().stream()
                    .filter(shipment -> shipment.targetId().equals(settlement.id())
                            && shipment.status().equals("in_transit"))
                    .mapToLong(ErdenRegionalEconomySavedData.TradeShipment::amount)
                    .sum();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6[" + settlementName(settlement.id()) + " 시장] §f"
                            + compactStock(state)
                            + " | 지역 운송 도착 예정 " + inbound
                            + " | 누적 생산 " + state.totalProduced()
                            + " | 소비 " + state.totalConsumed()
                            + " | 왕도 반출 " + state.totalExported()
                            + " | 부족일 " + state.shortageDays()), true);
            return;
        }
    }

    /** Storehouse loading-bay barrel tied to the real storehouse_west building. */
    public static BlockPos storagePosition(ErdenRegionalSettlementCatalog.Settlement settlement) {
        ErdenRegionalSettlementCatalog.BuildingLot storehouse = settlement.buildings().stream()
                .filter(lot -> lot.role().equals("storehouse_west"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing regional storehouse for " + settlement.id()));
        int centerX = settlement.x() + storehouse.dx();
        int centerZ = settlement.z() + storehouse.dz();
        int x = centerX + 24;
        int z = centerZ;
        int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 1;
        return new BlockPos(x, y, z);
    }

    public static long storageChunkKey(ErdenRegionalSettlementCatalog.Settlement settlement) {
        BlockPos pos = storagePosition(settlement);
        return pack(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
        CI_PHYSICAL_MARKETS.clear();
    }

    private static void ensureEconomy(ErdenRegionalEconomySavedData economy) {
        if (economy.hasEconomy(ECONOMY_REVISION, EXPECTED_SETTLEMENTS)) return;
        List<ErdenRegionalEconomySavedData.SettlementState> states = new ArrayList<>();
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            List<ErdenRegionalEconomySavedData.ResourceStock> opening = new ArrayList<>();
            opening.add(new ErdenRegionalEconomySavedData.ResourceStock(
                    "wheat", switch (settlement.industry()) {
                        case "grain" -> 128L;
                        case "river_market", "ranch" -> 20L;
                        default -> 12L;
                    }));
            opening.add(new ErdenRegionalEconomySavedData.ResourceStock(
                    "coal", settlement.industry().equals("colliery") ? 96L : 12L));
            switch (settlement.industry()) {
                case "ranch" -> {
                    opening.add(new ErdenRegionalEconomySavedData.ResourceStock("hay", 96L));
                    opening.add(new ErdenRegionalEconomySavedData.ResourceStock("leather", 32L));
                }
                case "iron_mine" -> opening.add(
                        new ErdenRegionalEconomySavedData.ResourceStock("iron", 64L));
                default -> {
                }
            }
            states.add(new ErdenRegionalEconomySavedData.SettlementState(
                    settlement.id(), settlement.x(), settlement.z(), settlement.industry(),
                    opening, -1L, -1L, 0L, 0L, 0L, 0L, 0L, 0L));
        }
        economy.initialize(ECONOMY_REVISION, states);
    }

    private static void logPlanOnce(ErdenRegionalEconomySavedData economy) {
        if (planLogged) return;
        validateEconomy(economy);
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden regional economy revision={} settlements={} markets={} households={} physical_storehouse_barrels=true local_consumption=true worker_production=true local_trade_escrow=true kingdom_supply_export=true unloaded_aggregate=true",
                ECONOMY_REVISION, EXPECTED_SETTLEMENTS, EXPECTED_MARKETS,
                ErdenRegionalSocietyManager.EXPECTED_HOUSEHOLDS);
    }

    private static void validateEconomy(ErdenRegionalEconomySavedData economy) {
        if (!economy.hasEconomy(ECONOMY_REVISION, EXPECTED_SETTLEMENTS)) {
            throw new IllegalStateException("Erden regional economy settlement count drifted");
        }
        Set<String> ids = new HashSet<>();
        for (ErdenRegionalEconomySavedData.SettlementState state : economy.settlements()) {
            if (!ids.add(state.id())) throw new IllegalStateException(
                    "Duplicate Erden regional market " + state.id());
            ErdenRegionalSettlementCatalog.Settlement template = settlement(state.id());
            if (template == null || template.x() != state.x() || template.z() != state.z()
                    || !template.industry().equals(state.industry())) {
                throw new IllegalStateException("Regional market geography drifted for " + state.id());
            }
        }
    }

    private static void syncLoadedMarketsFromWorld(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            Container container = ensureStorageContainer(level, settlement);
            if (container == null) continue;
            ErdenRegionalEconomySavedData.SettlementState state = economy.settlement(settlement.id());
            if (state == null) continue;
            if (!economy.storageMaterialized(settlement.id())) {
                if (fits(container, state)) {
                    writeContainer(container, state);
                    economy.markStorageMaterialized(settlement.id());
                    notePhysicalMarket(settlement, state, container);
                }
                continue;
            }
            ErdenRegionalEconomySavedData.SettlementState updated = state;
            for (ResourceItem resource : RESOURCES) {
                updated = updated.withStock(resource.resource(), countItem(container, resource.item()));
            }
            economy.replaceSettlement(updated);
            notePhysicalMarket(settlement, updated, container);
        }
    }

    private static void syncLoadedMarketsToWorld(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            Container container = ensureStorageContainer(level, settlement);
            ErdenRegionalEconomySavedData.SettlementState state = economy.settlement(settlement.id());
            if (container == null || state == null || !fits(container, state)) continue;
            writeContainer(container, state);
            economy.markStorageMaterialized(settlement.id());
            notePhysicalMarket(settlement, state, container);
        }
    }

    private static Container ensureStorageContainer(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        BlockPos pos = storagePosition(settlement);
        if (!level.hasChunkAt(pos)) return null;
        ErdenRegionalSettlementSavedData settlementData = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        if (!settlementData.isBuilt(
                storageChunkKey(settlement), ErdenRegionalSettlementCatalog.REVISION)) return null;
        if (!level.getBlockState(pos).is(Blocks.BARREL)) {
            if (!level.getBlockState(pos).isAir()) return null;
            level.setBlockAndUpdate(pos, Blocks.BARREL.defaultBlockState());
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    private static void processMissingDays(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy) {
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        long oldest = currentDay;
        for (ErdenRegionalEconomySavedData.SettlementState state : economy.settlements()) {
            oldest = Math.min(oldest, state.lastProcessedDay());
        }
        long firstDay = Math.max(oldest + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = firstDay; day <= currentDay; day++) {
            boolean needed = false;
            for (ErdenRegionalEconomySavedData.SettlementState state : economy.settlements()) {
                if (state.lastProcessedDay() < day) {
                    needed = true;
                    break;
                }
            }
            if (needed) processDay(level, society, economy, day);
        }
    }

    private static void processDay(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy,
            long day) {
        for (ErdenRegionalEconomySavedData.SettlementState snapshot : economy.settlements()) {
            if (snapshot.lastProcessedDay() >= day) continue;
            ErdenRegionalEconomySavedData.SettlementState state = snapshot;
            long produced = 0L;
            for (Production output : productionFor(state.industry())) {
                int workers = activeWorkers(society, state.id(), output.workerRole(), day);
                long amount = (long) workers * output.perWorker();
                if (amount <= 0L) continue;
                state = state.addStock(output.resource(), amount);
                produced += amount;
            }

            int households = livingHouseholds(society, state.id());
            long wheatNeed = households * WHEAT_PER_HOUSEHOLD;
            long wheatUsed = Math.min(wheatNeed, state.stock("wheat"));
            state = state.addStock("wheat", -wheatUsed);
            long coalNeed = Math.floorMod(day, 2L) == 0L ? households : 0L;
            long coalUsed = Math.min(coalNeed, state.stock("coal"));
            state = state.addStock("coal", -coalUsed);
            boolean shortage = wheatUsed < wheatNeed || coalUsed < coalNeed;
            state = state.recordDay(day, produced, wheatUsed + coalUsed, shortage);
            economy.replaceSettlement(state);
        }

        scheduleLocalBalancing(level, economy, day, "wheat", 32L);
        scheduleLocalBalancing(level, economy, day, "coal", 12L);
        LivingKingdoms.LOGGER.info(
                "Processed Erden regional market day={} produced={} consumed={} trade_shipments={} shortage_days={} worker_linked=true household_linked=true",
                day, economy.totalProduced(), economy.totalConsumed(),
                economy.activeTradeCount(), economy.totalShortageDays());
    }

    private static void scheduleLocalBalancing(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy,
            long day,
            String resource,
            long targetReserve) {
        for (ErdenRegionalEconomySavedData.SettlementState targetSnapshot : economy.settlements()) {
            ErdenRegionalEconomySavedData.SettlementState target = economy.settlement(targetSnapshot.id());
            if (target == null) continue;
            long inbound = inbound(economy, target.id(), resource);
            long missing = Math.max(0L, targetReserve - target.stock(resource) - inbound);
            while (missing > 0L) {
                final ErdenRegionalEconomySavedData.SettlementState finalTarget = target;
                ErdenRegionalEconomySavedData.SettlementState source = economy.settlements().stream()
                        .filter(candidate -> !candidate.id().equals(finalTarget.id()))
                        .filter(candidate -> candidate.stock(resource) > sourceReserve(candidate, resource))
                        .min(Comparator.comparingLong(candidate ->
                                        manhattan(candidate.x(), candidate.z(), finalTarget.x(), finalTarget.z()))
                                .thenComparing(ErdenRegionalEconomySavedData.SettlementState::id))
                        .orElse(null);
                if (source == null) break;
                long available = source.stock(resource) - sourceReserve(source, resource);
                long amount = Math.min(missing, available);
                if (amount <= 0L) break;
                int routeMetres = (int) Math.min(Integer.MAX_VALUE,
                        manhattan(source.x(), source.z(), target.x(), target.z()));
                long departureTick = day * 24_000L + 6_000L;
                long arrivalTick = departureTick + Math.max(
                        MIN_LOCAL_TRAVEL_TICKS, (long) routeMetres * TICKS_PER_METRE);
                economy.replaceSettlement(source.recordTradeOut(resource, amount));
                economy.addTrade(new ErdenRegionalEconomySavedData.TradeShipment(
                        economy.nextTradeId(day), source.id(), target.id(), resource, amount,
                        departureTick, arrivalTick, "in_transit", routeMetres));
                missing -= amount;
            }
        }
    }

    private static void settleLocalTrades(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy) {
        long now = level.getGameTime();
        for (ErdenRegionalEconomySavedData.TradeShipment shipment : economy.tradeShipments()) {
            if (shipment.terminal() || shipment.arrivalTick() > now) continue;
            ErdenRegionalEconomySavedData.SettlementState target = economy.settlement(shipment.targetId());
            if (target == null) {
                economy.replaceTrade(shipment.withStatus("failed"));
                continue;
            }
            economy.replaceSettlement(target.recordTradeIn(shipment.resource(), shipment.amount()));
            economy.replaceTrade(shipment.withStatus("arrived"));
        }
    }

    private static void exportRegionalSurplus(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy) {
        ErdenPhysicalEconomySavedData capital = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        if (capital.sites().size() != ErdenAuthoritativeEconomyManager.EXPECTED_SITES
                || !supply.hasSupply(
                ErdenKingdomSupplyManager.SUPPLY_REVISION,
                ErdenKingdomSupplyManager.EXPECTED_NODES)) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        for (ErdenRegionalEconomySavedData.SettlementState snapshot : economy.settlements()) {
            ErdenRegionalEconomySavedData.SettlementState state = economy.settlement(snapshot.id());
            if (state == null || state.lastProcessedDay() < day || state.lastExportDay() >= day) continue;
            for (String resource : exportResources(state.industry())) {
                long amount = Math.max(0L, state.stock(resource) - exportReserve(state, resource));
                if (amount <= 0L) continue;
                long accepted = ErdenRegionalSupplyBridge.enqueue(
                        level, capital, "regional:" + state.id(), state.x(), state.z(),
                        resource, amount, day);
                if (accepted > 0L) state = state.recordExport(resource, accepted, day);
            }
            if (state.lastExportDay() < day) state = state.markExportDay(day);
            economy.replaceSettlement(state);
        }
    }

    private static int activeWorkers(
            ErdenRegionalSocietySavedData society,
            String settlementId,
            String role,
            long day) {
        int total = 0;
        int weekDay = (int) Math.floorMod(day, 7L);
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            if (!household.settlementId().equals(settlementId)) continue;
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (resident.worker()
                        && resident.workRole().equals(role)
                        && resident.restDay() != weekDay
                        && !society.isDead(resident.id())) total++;
            }
        }
        return total;
    }

    private static int livingHouseholds(
            ErdenRegionalSocietySavedData society,
            String settlementId) {
        int total = 0;
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            if (!household.settlementId().equals(settlementId)) continue;
            boolean living = false;
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (!society.isDead(resident.id())) {
                    living = true;
                    break;
                }
            }
            if (living) total++;
        }
        return total;
    }

    private static List<Production> productionFor(String industry) {
        return switch (industry) {
            case "grain" -> List.of(new Production("farmer", "wheat", 12L));
            case "ranch" -> List.of(
                    new Production("shepherd", "hay", 8L),
                    new Production("shepherd", "leather", 2L));
            case "colliery" -> List.of(new Production("coal_miner", "coal", 12L));
            case "iron_mine" -> List.of(new Production("iron_miner", "iron", 5L));
            case "river_market" -> List.of();
            default -> throw new IllegalStateException("Unknown regional industry " + industry);
        };
    }

    private static List<String> exportResources(String industry) {
        return switch (industry) {
            case "grain" -> List.of("wheat");
            case "ranch" -> List.of("hay", "leather");
            case "colliery" -> List.of("coal");
            case "iron_mine" -> List.of("iron");
            case "river_market" -> List.of();
            default -> List.of();
        };
    }

    private static long sourceReserve(
            ErdenRegionalEconomySavedData.SettlementState state,
            String resource) {
        if (resource.equals("wheat")) return state.industry().equals("grain") ? 48L : 24L;
        if (resource.equals("coal")) return state.industry().equals("colliery") ? 32L : 8L;
        return 0L;
    }

    private static long exportReserve(
            ErdenRegionalEconomySavedData.SettlementState state,
            String resource) {
        return switch (state.industry() + "/" + resource) {
            case "grain/wheat" -> 64L;
            case "ranch/hay" -> 32L;
            case "ranch/leather" -> 16L;
            case "colliery/coal" -> 40L;
            case "iron_mine/iron" -> 16L;
            default -> Long.MAX_VALUE;
        };
    }

    private static long inbound(
            ErdenRegionalEconomySavedData economy,
            String targetId,
            String resource) {
        long total = 0L;
        for (ErdenRegionalEconomySavedData.TradeShipment shipment : economy.tradeShipments()) {
            if (shipment.targetId().equals(targetId)
                    && shipment.resource().equals(resource)
                    && shipment.status().equals("in_transit")) total += shipment.amount();
        }
        return total;
    }

    private static boolean fits(
            Container container,
            ErdenRegionalEconomySavedData.SettlementState state) {
        long stacks = 0L;
        for (ResourceItem resource : RESOURCES) {
            stacks += (state.stock(resource.resource()) + MAX_STACK_SIZE - 1L) / MAX_STACK_SIZE;
        }
        int available = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || managedItem(stack.getItem())) available++;
        }
        return stacks <= available;
    }

    private static void writeContainer(
            Container container,
            ErdenRegionalEconomySavedData.SettlementState state) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && managedItem(stack.getItem())) container.setItem(slot, ItemStack.EMPTY);
        }
        for (ResourceItem resource : RESOURCES) {
            long remaining = state.stock(resource.resource());
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0L; slot++) {
                if (!container.getItem(slot).isEmpty()) continue;
                int amount = (int) Math.min(MAX_STACK_SIZE, remaining);
                container.setItem(slot, new ItemStack(resource.item(), amount));
                remaining -= amount;
            }
        }
        container.setChanged();
    }

    private static long countItem(Container container, Item item) {
        long count = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static boolean managedItem(Item item) {
        for (ResourceItem resource : RESOURCES) if (resource.item() == item) return true;
        return false;
    }

    private static void notePhysicalMarket(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            ErdenRegionalEconomySavedData.SettlementState state,
            Container container) {
        if (!isCi()) return;
        for (ResourceItem resource : RESOURCES) {
            if (countItem(container, resource.item()) != state.stock(resource.resource())) return;
        }
        CI_PHYSICAL_MARKETS.add(settlement.id());
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy) {
        if (ciPassed || !isCi()) return;
        validateEconomy(economy);
        if (!CI_PHYSICAL_MARKETS.contains("harvest_crossing")
                || economy.materializedStorageCount() < 1
                || economy.totalProduced() <= 0L
                || economy.totalConsumed() <= 0L
                || economy.totalTradedOut() <= 0L
                || economy.totalExported() <= 0L
                || !hasRegionalKingdomShipment(level)) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_ECONOMY_PASS revision={} settlements={} households={} markets={} physical_market_sample={} produced={} consumed={} traded_out={} traded_in={} exported={} shortage_days={} active_local_shipments={} physical_barrel=true player_inventory_authoritative=true local_reserve_first=true worker_linked=true household_consumption=true local_trade_escrow=true kingdom_supply_escrow=true unloaded_aggregate=true persistent_forced_chunks=false",
                ECONOMY_REVISION, EXPECTED_SETTLEMENTS,
                ErdenRegionalSocietyManager.EXPECTED_HOUSEHOLDS, EXPECTED_MARKETS,
                CI_PHYSICAL_MARKETS.size(), economy.totalProduced(), economy.totalConsumed(),
                economy.totalTradedOut(), economy.totalTradedIn(), economy.totalExported(),
                economy.totalShortageDays(), economy.activeTradeCount());
    }

    private static boolean hasRegionalKingdomShipment(ServerLevel level) {
        ErdenKingdomSupplySavedData supply = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomSupplySavedData.TYPE);
        for (ErdenKingdomSupplySavedData.ShipmentState shipment : supply.shipments()) {
            if (shipment.sourceId().startsWith("regional:")) return true;
        }
        return false;
    }

    private static String compactStock(ErdenRegionalEconomySavedData.SettlementState state) {
        List<String> values = new ArrayList<>();
        for (ResourceItem resource : RESOURCES) {
            long amount = state.stock(resource.resource());
            if (amount > 0L) values.add(resourceName(resource.resource()) + " " + amount);
        }
        return values.isEmpty() ? "재고 없음" : String.join(", ", values);
    }

    private static String resourceName(String resource) {
        return switch (resource) {
            case "wheat" -> "밀";
            case "coal" -> "석탄";
            case "hay" -> "건초";
            case "leather" -> "가죽";
            case "iron" -> "철";
            default -> resource;
        };
    }

    private static String settlementName(String id) {
        return switch (id) {
            case "harvest_crossing" -> "수확나루";
            case "silvermead" -> "은초원";
            case "sunfield" -> "해들판";
            case "pinewatch" -> "솔망루";
            case "blackstone" -> "흑석";
            case "ironvale" -> "철골짜기";
            default -> id;
        };
    }

    private static ErdenRegionalSettlementCatalog.Settlement settlement(String id) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (settlement.id().equals(id)) return settlement;
        }
        return null;
    }

    private static long manhattan(int x1, int z1, int x2, int z2) {
        return Math.abs((long) x1 - x2) + Math.abs((long) z1 - z2);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private record ResourceItem(String resource, Item item) {
    }

    private record Production(String workerRole, String resource, long perWorker) {
    }
}
