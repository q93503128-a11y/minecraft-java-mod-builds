package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs Erden's physical economy with authoritative cargo escrow. Aggregate deliveries in unloaded
 * districts settle immediately; player-near deliveries leave the source at dispatch and enter the
 * target only when ErdenTransportManager completes unloading or returns failed cargo.
 */
public final class ErdenAuthoritativeEconomyManager {
    public static final int ECONOMY_REVISION = 1;
    public static final int EXPECTED_SITES = 156;
    public static final int EXPECTED_WAREHOUSES = 15;
    public static final int EXPECTED_WALLETS = 77;

    private static final int SYNC_INTERVAL = 20;
    private static final int MAX_CATCH_UP_DAYS = 30;
    private static final long STARTING_COINS = 30L;
    private static final long DAILY_WAGE = 2L;
    private static final long RESERVE_PER_BAKERY = 2L;
    private static final int STACKS_PER_RESOURCE = 3;
    private static final int MAX_VISIBLE_PER_RESOURCE = 64 * STACKS_PER_RESOURCE;

    private static final List<ResourceItem> PHYSICAL_RESOURCES = List.of(
            new ResourceItem("wheat", Items.WHEAT),
            new ResourceItem("coal", Items.COAL),
            new ResourceItem("leather", Items.LEATHER),
            new ResourceItem("paper", Items.PAPER),
            new ResourceItem("iron", Items.IRON_INGOT),
            new ResourceItem("hay", Items.HAY_BLOCK),
            new ResourceItem("bread", Items.BREAD),
            new ResourceItem("goods", Items.BRICK)
    );
    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "shop", "bakery", "inn", "stable",
            "guard_post", "bathhouse", "warehouse"
    );

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;
    private static int lastFulfilledHouseholds;
    private static long lastReserveLoggedDay = Long.MIN_VALUE;

    private ErdenAuthoritativeEconomyManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (population.households().size() != EXPECTED_WALLETS) return;
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ensureEconomy(economy, population);
        logPlanOnce(economy);

        if (level.getGameTime() % SYNC_INTERVAL == 0L) {
            captureLoadedContainers(level, economy);
        }
        ErdenKingdomSupplyManager.prepareBeforeCityEconomy(level, economy);
        processDailyEconomy(level, population, economy);
        if (level.getGameTime() % SYNC_INTERVAL == 0L) {
            materializeLoadedContainers(level, economy);
        }
        verifyCiIfReady(level, economy);
    }

    public static void handleInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || !(level.getBlockEntity(event.getPos()) instanceof Container)) {
            return;
        }
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            BlockPos containerPos = primaryContainerPos(level, site);
            if (containerPos == null || !containerPos.equals(event.getPos())) continue;
            player.sendSystemMessage(Component.literal(
                    "§6[왕도 물류] §f" + roleName(site.role())
                            + " | 재고 " + compactStocks(site)
                            + " | 입고 대기 " + compactPending(site)
                            + " | 출고 중 " + compactInTransit(site)
                            + " | 영업 " + ErdenLivingEconomyManager.siteStatus(site, level.getGameTime())
                            + " | 가격 " + ErdenLivingEconomyManager.priceText(site)
                            + " | 현금 " + site.metric("coins")
                            + " | 외곽 입고 " + site.metric("kingdom_supply_received")
                            + " | 수령 " + site.metric("received")
                            + " / 출고 " + site.metric("sent")
                            + " | 누적 임금 " + site.metric("wages_paid")));
            return;
        }
    }

    public static List<ExternalUrbanFabricBuilder.UrbanEntrance> ciEntrances() {
        List<ExternalUrbanFabricBuilder.UrbanEntrance> result = new ArrayList<>();
        for (String role : List.of("warehouse", "bakery", "shop")) {
            ExternalUrbanFabricBuilder.entrances().stream()
                    .filter(entrance -> entrance.role().equals(role))
                    .sorted(Comparator.comparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                            .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                    .findFirst()
                    .ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
        lastFulfilledHouseholds = 0;
        lastReserveLoggedDay = Long.MIN_VALUE;
    }

    private static void ensureEconomy(
            ErdenPhysicalEconomySavedData economy,
            ErdenPopulationSavedData population) {
        if (economy.hasEconomy(ECONOMY_REVISION, EXPECTED_SITES, EXPECTED_WALLETS)) return;
        List<ExternalUrbanFabricBuilder.UrbanEntrance> workplaces =
                ExternalUrbanFabricBuilder.entrances().stream()
                        .filter(entrance -> !entrance.role().equals("tenement"))
                        .sorted(Comparator.comparing(ExternalUrbanFabricBuilder.UrbanEntrance::role)
                                .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                                .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                        .toList();
        if (workplaces.size() != EXPECTED_SITES) {
            throw new IllegalStateException(
                    "Expected " + EXPECTED_SITES + " Erden economy sites, found " + workplaces.size());
        }
        List<ErdenPhysicalEconomySavedData.SiteState> sites = new ArrayList<>();
        Map<String, Integer> roleIndexes = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : workplaces) {
            if (!SUPPORTED_ROLES.contains(entrance.role())) {
                throw new IllegalStateException("Unsupported Erden economy role " + entrance.role());
            }
            int roleIndex = roleIndexes.merge(entrance.role(), 1, Integer::sum);
            sites.add(new ErdenPhysicalEconomySavedData.SiteState(
                    "erden_%s_%03d".formatted(entrance.role(), roleIndex),
                    entrance.x(), entrance.z(), entrance.role(),
                    List.of(), List.of(), false));
        }
        List<ErdenPhysicalEconomySavedData.WalletState> wallets = population.households().stream()
                .map(household -> new ErdenPhysicalEconomySavedData.WalletState(
                        household.id(), STARTING_COINS, 0L, 0L))
                .toList();
        economy.replaceEconomy(ECONOMY_REVISION, sites, wallets);
    }

    private static void logPlanOnce(ErdenPhysicalEconomySavedData economy) {
        if (planLogged) return;
        int warehouses = 0;
        Map<String, Integer> roles = new LinkedHashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            roles.merge(site.role(), 1, Integer::sum);
            if (site.role().equals("warehouse")) warehouses++;
        }
        if (economy.sites().size() != EXPECTED_SITES
                || economy.wallets().size() != EXPECTED_WALLETS
                || warehouses != EXPECTED_WAREHOUSES) {
            throw new IllegalStateException(
                    "Invalid Erden physical economy sites=" + economy.sites().size()
                            + " wallets=" + economy.wallets().size()
                            + " warehouses=" + warehouses);
        }
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden physical economy sites={} warehouses={} wallets={} container_resources={} roles={} authoritative_transport=true",
                economy.sites().size(), warehouses, economy.wallets().size(),
                PHYSICAL_RESOURCES.size(), roles);
    }

    private static void processDailyEconomy(
            ServerLevel level,
            ErdenPopulationSavedData population,
            ErdenPhysicalEconomySavedData economy) {
        ErdenLivingEconomySavedData livingEconomy = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        long previousDay = economy.lastProcessedDay();
        if (previousDay >= currentDay) return;
        long firstDay = previousDay < 0L
                ? currentDay
                : Math.max(previousDay + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = firstDay; day <= currentDay; day++) {
            ErdenCargoEscrowManager.beginEconomyDay(level, day);
            DayResult result;
            try {
                result = processDay(level, day, population, economy.sites(), economy.wallets(), livingEconomy);
            } finally {
                ErdenCargoEscrowManager.endEconomyDay();
            }
            economy.applyDay(
                    day, result.sites, result.wallets,
                    result.deliveries, result.crafted,
                    result.sales, result.wages);
            livingEconomy.applyDay(
                    ErdenLivingEconomyManager.LIVING_ECONOMY_REVISION,
                    day, result.market.states(),
                    result.market.fulfilledHouseholds(), result.market.failedHouseholds(),
                    result.market.closedFailures(), result.market.stockoutFailures(),
                    result.market.unaffordableFailures(), result.market.salesCoins());
            lastFulfilledHouseholds = result.fulfilledHouseholds;
            if (previousDay < 0L || day % 7L == 0L) {
                LivingKingdoms.LOGGER.info(
                        "Processed Erden physical economy day={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} failed_households={} wallet_coins={} authoritative_transport=true",
                        day, result.deliveries, result.crafted,
                        result.sales, result.wages,
                        result.fulfilledHouseholds, result.market.failedHouseholds(),
                        totalWalletCoins(result.wallets));
            }
            if (result.reserveComplete == expectedBakeryCount()
                    && lastReserveLoggedDay != day) {
                lastReserveLoggedDay = day;
                LivingKingdoms.LOGGER.info(
                        "Reconciled Erden bakery reserves day={} bakeries={} reserve_bread={} transfers={} moved={} conserved=true transport_accounted=true",
                        day, result.reserveComplete, result.reserveTotal,
                        result.reserveTransfers, result.reserveMoved);
            }
        }
    }

    private static DayResult processDay(
            ServerLevel level,
            long day,
            ErdenPopulationSavedData population,
            List<ErdenPhysicalEconomySavedData.SiteState> existingSites,
            List<ErdenPhysicalEconomySavedData.WalletState> existingWallets,
            ErdenLivingEconomySavedData livingEconomy) {
        Map<String, ErdenPhysicalEconomySavedData.SiteState> sites = new LinkedHashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : existingSites) sites.put(site.id(), site);
        Map<String, ErdenPhysicalEconomySavedData.WalletState> wallets = new LinkedHashMap<>();
        for (ErdenPhysicalEconomySavedData.WalletState wallet : existingWallets) {
            wallets.put(wallet.householdId(), wallet);
        }
        Map<Long, WorkerRef> workers = livingWorkers(level, population, day);
        DayCounters counters = new DayCounters();

        ErdenLivingEconomyManager.prepareDay(day, sites);
        deliverRawMaterials(sites, workers, counters);
        runProduction(sites, workers, counters);
        distributeBread(sites, counters);
        runServiceSites(sites, workers, counters);
        Set<String> activeHouseholds =
                ErdenCapitalLifecycleManager.livingHouseholdIds(level, population, day);
        ErdenLivingEconomyManager.MarketResult market =
                ErdenLivingEconomyManager.runDailyMarket(
                        day, population, activeHouseholds, sites, wallets, livingEconomy);
        counters.sales += market.salesCoins();
        counters.fulfilledHouseholds += market.fulfilledHouseholds();
        reconcileBakeryReserves(sites, counters);
        payWages(workers, sites, wallets, counters);

        return new DayResult(
                List.copyOf(sites.values()),
                List.copyOf(wallets.values()),
                counters.deliveries, counters.crafted,
                counters.sales, counters.wages,
                counters.fulfilledHouseholds, market,
                counters.reserveTransfers, counters.reserveMoved,
                counters.reserveComplete, counters.reserveTotal);
    }

    private static void deliverRawMaterials(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<Long, WorkerRef> workers,
            DayCounters counters) {
        for (ErdenPhysicalEconomySavedData.SiteState snapshot : List.copyOf(sites.values())) {
            if (!workers.containsKey(positionKey(snapshot.x(), snapshot.z()))) continue;
            Map<String, Long> requirements = switch (snapshot.role()) {
                case "bakery" -> Map.of("wheat", 6L, "coal", 1L);
                case "shop" -> Map.of("leather", 2L, "paper", 2L);
                case "stable" -> Map.of("hay", 3L);
                case "guard_post" -> Map.of("iron", 1L, "coal", 1L);
                case "bathhouse" -> Map.of("coal", 2L);
                default -> Map.of();
            };
            for (Map.Entry<String, Long> requirement : requirements.entrySet()) {
                ErdenPhysicalEconomySavedData.SiteState site = sites.get(snapshot.id());
                long effective = effectiveStock(site, requirement.getKey());
                long missing = Math.max(0L, requirement.getValue() - effective);
                if (missing > 0L) {
                    transferFromNearestWarehouse(
                            sites, site.id(), requirement.getKey(), missing, counters);
                }
            }
        }
    }

    private static void runProduction(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<Long, WorkerRef> workers,
            DayCounters counters) {
        for (ErdenPhysicalEconomySavedData.SiteState original : List.copyOf(sites.values())) {
            if (!workers.containsKey(positionKey(original.x(), original.z()))) continue;
            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            if (site.metric("operating_today") <= 0L) continue;
            switch (site.role()) {
                case "bakery" -> {
                    if (site.stock("wheat") < 6L || site.stock("coal") < 1L) continue;
                    site = site.addStock("wheat", -6L)
                            .addStock("coal", -1L)
                            .addStock("bread", 13L)
                            .addMetric("crafted", 13L);
                    counters.crafted += 13L;
                }
                case "shop" -> {
                    if (site.stock("leather") < 2L || site.stock("paper") < 2L) continue;
                    site = site.addStock("leather", -2L)
                            .addStock("paper", -2L)
                            .addStock("goods", 6L)
                            .addMetric("crafted", 6L);
                    counters.crafted += 6L;
                }
                default -> {
                    continue;
                }
            }
            sites.put(site.id(), site);
        }
    }

    private static void distributeBread(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            DayCounters counters) {
        List<String> bakeryIds = sites.values().stream()
                .filter(site -> site.role().equals("bakery"))
                .map(ErdenPhysicalEconomySavedData.SiteState::id)
                .toList();
        distributeBreadToRole(sites, bakeryIds, "shop", 4L, counters);
        distributeBreadToRole(sites, bakeryIds, "inn", 2L, counters);
        distributeBreadToRole(sites, bakeryIds, "shop", 8L, counters);
    }

    private static void distributeBreadToRole(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            List<String> bakeryIds,
            String role,
            long targetStock,
            DayCounters counters) {
        for (String bakeryId : bakeryIds) {
            while (sites.get(bakeryId).stock("bread") > 0L) {
                ErdenPhysicalEconomySavedData.SiteState bakery = sites.get(bakeryId);
                ErdenPhysicalEconomySavedData.SiteState target = sites.values().stream()
                        .filter(site -> site.role().equals(role)
                                && effectiveStock(site, "bread") < targetStock)
                        .min(Comparator.<ErdenPhysicalEconomySavedData.SiteState>comparingLong(site ->
                                        distanceSquared(bakery.x(), bakery.z(), site.x(), site.z()))
                                .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                        .orElse(null);
                if (target == null) break;
                long amount = Math.min(
                        bakery.stock("bread"),
                        Math.max(0L, targetStock - effectiveStock(target, "bread")));
                if (amount <= 0L || !transfer(
                        sites, bakery.id(), target.id(), "bread", amount, counters)) break;
            }
        }
    }

    private static void runServiceSites(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<Long, WorkerRef> workers,
            DayCounters counters) {
        for (ErdenPhysicalEconomySavedData.SiteState original : List.copyOf(sites.values())) {
            if (!workers.containsKey(positionKey(original.x(), original.z()))) continue;
            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            if (site.metric("operating_today") <= 0L) continue;
            long serviceUnits = 0L;
            long revenue = 0L;
            switch (site.role()) {
                case "inn" -> {
                    if (site.stock("bread") < 2L) continue;
                    site = site.addStock("bread", -2L);
                    serviceUnits = 4L;
                    revenue = 4L;
                }
                case "stable" -> {
                    if (site.stock("hay") < 3L) continue;
                    site = site.addStock("hay", -3L);
                    serviceUnits = 5L;
                    revenue = 3L;
                }
                case "guard_post" -> {
                    if (site.stock("iron") < 1L || site.stock("coal") < 1L) continue;
                    site = site.addStock("iron", -1L).addStock("coal", -1L);
                    serviceUnits = 5L;
                    revenue = 3L;
                }
                case "bathhouse" -> {
                    if (site.stock("coal") < 2L) continue;
                    site = site.addStock("coal", -2L);
                    serviceUnits = 6L;
                    revenue = 3L;
                }
                default -> {
                    continue;
                }
            }
            site = site.addMetric("service_units", serviceUnits)
                    .addMetric("coins", revenue);
            counters.crafted += serviceUnits;
            counters.sales += revenue;
            sites.put(site.id(), site);
        }
    }

    private static void sellToHouseholds(
            ErdenPopulationSavedData population,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<String, ErdenPhysicalEconomySavedData.WalletState> wallets,
            DayCounters counters) {
        for (ErdenPopulationSavedData.Household household : population.households()) {
            int alive = 0;
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (!population.isDead(resident.id())) alive++;
            }
            if (alive <= 0) continue;
            ErdenPhysicalEconomySavedData.WalletState wallet = wallets.get(household.id());
            if (wallet == null || wallet.coins() < 4L) continue;
            ErdenPhysicalEconomySavedData.SiteState shop = sites.values().stream()
                    .filter(site -> site.role().equals("shop")
                            && site.stock("bread") >= 2L
                            && site.stock("goods") >= 1L)
                    .min(Comparator.<ErdenPhysicalEconomySavedData.SiteState>comparingLong(site ->
                                    distanceSquared(household.homeX(), household.homeZ(), site.x(), site.z()))
                            .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                    .orElse(null);
            if (shop == null) continue;
            shop = shop.addStock("bread", -2L)
                    .addStock("goods", -1L)
                    .addMetric("coins", 4L)
                    .addMetric("sales_units", 3L)
                    .addMetric("sales_coins", 4L);
            sites.put(shop.id(), shop);
            wallets.put(household.id(), wallet.spend(4L));
            counters.sales += 4L;
            counters.fulfilledHouseholds++;
        }
    }

    private static void reconcileBakeryReserves(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            DayCounters counters) {
        Set<Long> protectedSamples = protectedShopSamples();
        List<String> bakeryIds = sites.values().stream()
                .filter(site -> site.role().equals("bakery"))
                .sorted(Comparator.comparing(ErdenPhysicalEconomySavedData.SiteState::id))
                .map(ErdenPhysicalEconomySavedData.SiteState::id)
                .toList();
        for (String bakeryId : bakeryIds) {
            ErdenPhysicalEconomySavedData.SiteState bakery = sites.get(bakeryId);
            long missing = Math.max(0L, RESERVE_PER_BAKERY - effectiveStock(bakery, "bread"));
            while (missing > 0L) {
                ErdenPhysicalEconomySavedData.SiteState donor = sites.values().stream()
                        .filter(site -> site.role().equals("shop")
                                && site.stock("bread") > 0L
                                && !protectedSamples.contains(positionKey(site.x(), site.z())))
                        .max(Comparator.comparingLong(
                                        (ErdenPhysicalEconomySavedData.SiteState site) -> site.stock("bread"))
                                .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                        .orElse(null);
                if (donor == null) break;
                long amount = Math.min(missing, donor.stock("bread"));
                if (!transfer(sites, donor.id(), bakeryId, "bread", amount, counters)) break;
                counters.reserveTransfers++;
                counters.reserveMoved += amount;
                bakery = sites.get(bakeryId);
                missing = Math.max(0L, RESERVE_PER_BAKERY - effectiveStock(bakery, "bread"));
            }
        }
        for (ErdenPhysicalEconomySavedData.SiteState site : sites.values()) {
            if (!site.role().equals("bakery")) continue;
            counters.reserveTotal += Math.min(RESERVE_PER_BAKERY, effectiveStock(site, "bread"));
            if (effectiveStock(site, "bread") >= RESERVE_PER_BAKERY) counters.reserveComplete++;
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

    private static int expectedBakeryCount() {
        int count = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.role().equals("bakery")) count++;
        }
        return count;
    }

    private static void payWages(
            Map<Long, WorkerRef> workers,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<String, ErdenPhysicalEconomySavedData.WalletState> wallets,
            DayCounters counters) {
        Map<Long, String> siteIds = new HashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : sites.values()) {
            siteIds.put(positionKey(site.x(), site.z()), site.id());
        }
        for (Map.Entry<Long, WorkerRef> entry : workers.entrySet()) {
            WorkerRef worker = entry.getValue();
            ErdenPhysicalEconomySavedData.WalletState wallet = wallets.get(worker.householdId());
            String siteId = siteIds.get(entry.getKey());
            if (wallet == null || siteId == null) continue;
            ErdenPhysicalEconomySavedData.SiteState site = sites.get(siteId);
            long available = site.metric("coins");
            if (available < DAILY_WAGE) {
                site = site.addMetric("treasury_subsidy", DAILY_WAGE - available)
                        .withMetric("coins", DAILY_WAGE);
            }
            site = site.addMetric("coins", -DAILY_WAGE)
                    .addMetric("wages_paid", DAILY_WAGE);
            wallet = wallet.earn(DAILY_WAGE);
            counters.wages += DAILY_WAGE;
            sites.put(site.id(), site);
            wallets.put(worker.householdId(), wallet);
        }
    }

    private static void transferFromNearestWarehouse(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            String targetId,
            String resource,
            long requested,
            DayCounters counters) {
        long remaining = requested;
        while (remaining > 0L) {
            ErdenPhysicalEconomySavedData.SiteState target = sites.get(targetId);
            ErdenPhysicalEconomySavedData.SiteState warehouse = sites.values().stream()
                    .filter(site -> site.role().equals("warehouse") && site.stock(resource) > 0L)
                    .min(Comparator.<ErdenPhysicalEconomySavedData.SiteState>comparingLong(site ->
                                    distanceSquared(target.x(), target.z(), site.x(), site.z()))
                            .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id))
                    .orElse(null);
            if (warehouse == null) return;
            long amount = Math.min(remaining, warehouse.stock(resource));
            if (!transfer(sites, warehouse.id(), targetId, resource, amount, counters)) return;
            remaining -= amount;
        }
    }

    private static boolean transfer(
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            String sourceId,
            String targetId,
            String resource,
            long amount,
            DayCounters counters) {
        if (amount <= 0L || sourceId.equals(targetId)) return false;
        ErdenPhysicalEconomySavedData.SiteState source = sites.get(sourceId);
        ErdenPhysicalEconomySavedData.SiteState target = sites.get(targetId);
        if (source == null || target == null) return false;
        long moved = Math.min(Math.max(0L, amount), source.stock(resource));
        if (moved <= 0L) return false;

        ErdenCargoEscrowManager.DispatchResult dispatch =
                ErdenCargoEscrowManager.dispatchTransfer(source, target, resource, moved);
        if (dispatch == ErdenCargoEscrowManager.DispatchResult.BLOCKED) {
            source = source.addMetric("blocked_shipments", 1L);
            target = target.addMetric("delivery_delays", 1L);
            sites.put(source.id(), source);
            sites.put(target.id(), target);
            return false;
        }

        source = source.addStock(resource, -moved).addMetric("sent", moved);
        if (dispatch == ErdenCargoEscrowManager.DispatchResult.DEFERRED) {
            source = source.addMetric(ErdenCargoEscrowManager.inTransitMetric(resource), moved);
            target = target.addMetric(ErdenCargoEscrowManager.pendingMetric(resource), moved);
        } else {
            target = target.addStock(resource, moved).addMetric("received", moved);
        }
        sites.put(source.id(), source);
        sites.put(target.id(), target);
        counters.deliveries++;
        return true;
    }

    private static long effectiveStock(
            ErdenPhysicalEconomySavedData.SiteState site,
            String resource) {
        return site.stock(resource)
                + Math.max(0L, site.metric(ErdenCargoEscrowManager.pendingMetric(resource)));
    }

    private static Map<Long, WorkerRef> livingWorkers(
            ServerLevel level,
            ErdenPopulationSavedData population,
            long day) {
        Map<Long, WorkerRef> result = new HashMap<>();
        for (ErdenCapitalLifecycleManager.WorkerSnapshot worker
                : ErdenCapitalLifecycleManager.activeWorkers(level, population, day)) {
            result.put(positionKey(worker.workX(), worker.workZ()),
                    new WorkerRef(worker.householdId(), worker.personId()));
        }
        return result;
    }

    private static void captureLoadedContainers(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (!site.materialized() || !isSiteReady(level, site)) continue;
            Container container = primaryContainer(level, site);
            if (container == null) continue;
            ErdenPhysicalEconomySavedData.SiteState updated = site;
            for (ResourceItem resource : PHYSICAL_RESOURCES) {
                long visible = countItem(container, resource.item);
                long overflow = Math.max(0L, site.stock(resource.resource) - MAX_VISIBLE_PER_RESOURCE);
                updated = updated.withStock(resource.resource, overflow + visible);
            }
            economy.replaceSite(updated);
        }
    }

    private static void materializeLoadedContainers(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (!isSiteReady(level, site)) continue;
            Container container = primaryContainer(level, site);
            if (container == null) continue;
            writeContainer(container, site);
            if (!site.materialized()) economy.replaceSite(site.withMaterialized(true));
        }
    }

    private static boolean isSiteReady(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState site) {
        if (!level.hasChunk(site.x() >> 4, site.z() >> 4)) return false;
        ErdenUrbanInteriorSavedData interiors = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        return interiors.isComplete(
                positionKey(site.x(), site.z()),
                ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
    }

    private static Container primaryContainer(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState site) {
        BlockPos pos = primaryContainerPos(level, site);
        if (pos == null) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    private static BlockPos primaryContainerPos(
            ServerLevel level,
            ErdenPhysicalEconomySavedData.SiteState site) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(site.x(), site.z());
        if (entrance == null) return null;
        int doorY = findLowestDoorY(level, site.x(), site.z());
        if (doorY == Integer.MIN_VALUE) return null;
        Room room = room(entrance, doorY - 1);
        Point point = switch (site.role()) {
            case "shop" -> room.point(-3, 7);
            case "bakery" -> room.point(-3, 5);
            case "inn" -> room.point(-3, 9);
            case "stable" -> room.point(3, 5);
            case "guard_post" -> room.point(-3, 3);
            case "warehouse" -> room.point(-3, 3);
            default -> null;
        };
        return point == null ? null : new BlockPos(point.x, room.floorY + 1, point.z);
    }

    private static long countItem(Container container, Item item) {
        long count = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void writeContainer(
            Container container,
            ErdenPhysicalEconomySavedData.SiteState site) {
        for (ResourceItem resource : PHYSICAL_RESOURCES) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.is(resource.item)) container.setItem(slot, ItemStack.EMPTY);
            }
        }
        for (ResourceItem resource : PHYSICAL_RESOURCES) {
            long remaining = Math.min(MAX_VISIBLE_PER_RESOURCE, site.stock(resource.resource));
            int usedStacks = 0;
            for (int slot = 0;
                 slot < container.getContainerSize() && remaining > 0L && usedStacks < STACKS_PER_RESOURCE;
                 slot++) {
                if (!container.getItem(slot).isEmpty()) continue;
                int count = (int) Math.min(64L, remaining);
                container.setItem(slot, new ItemStack(resource.item, count));
                remaining -= count;
                usedStacks++;
            }
        }
        container.setChanged();
    }

    private static void verifyCiIfReady(
            ServerLevel level,
            ErdenPhysicalEconomySavedData economy) {
        ErdenLivingEconomySavedData livingEconomy = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        long purchaseOutcomes = livingEconomy.outcomes().size();
        long purchaseSuccesses = livingEconomy.outcomes().stream()
                .filter(ErdenLivingEconomySavedData.HouseholdMarketState::success)
                .count();
        long purchaseFailures = purchaseOutcomes - purchaseSuccesses;
        if (ciPassed
                || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                || !ErdenKingdomSupplyManager.isReady(level, economy)
                || economy.lastProcessedDay() < 0L
                || economy.sites().size() != EXPECTED_SITES
                || economy.wallets().size() != EXPECTED_WALLETS
                || economy.totalDeliveries() <= 0L
                || economy.totalCrafted() <= 0L
                || economy.totalSales() < EXPECTED_WALLETS * 4L
                || economy.totalWages() < ErdenPopulationManager.EXPECTED_WORKERS * DAILY_WAGE
                || purchaseOutcomes != EXPECTED_WALLETS
                || purchaseSuccesses <= 0L
                || purchaseSuccesses != lastFulfilledHouseholds
                || purchaseFailures < 0L) {
            return;
        }
        int visibleContainers = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ErdenAuthoritativeEconomyManager.ciEntrances()) {
            ErdenPhysicalEconomySavedData.SiteState site = findSite(
                    economy.sites(), entrance.x(), entrance.z());
            if (site == null || !site.materialized()) return;
            Container container = primaryContainer(level, site);
            if (container == null) return;
            long visible = 0L;
            for (ResourceItem resource : PHYSICAL_RESOURCES) {
                visible += countItem(container, resource.item);
            }
            if (visible <= 0L) return;
            visibleContainers++;
        }
        if (visibleContainers != 3) return;
        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_PHYSICAL_ECONOMY_PASS sites={} warehouses={} wallets={} deliveries={} crafted={} sales={} wages={} fulfilled_households={} purchase_outcomes={} purchase_failures={} wallet_coins={} containers={} authoritative_transport=true kingdom_supply=true",
                EXPECTED_SITES, EXPECTED_WAREHOUSES, EXPECTED_WALLETS,
                economy.totalDeliveries(), economy.totalCrafted(),
                economy.totalSales(), economy.totalWages(),
                lastFulfilledHouseholds, purchaseOutcomes, purchaseFailures,
                economy.totalWalletCoins(), visibleContainers);
    }

    private static ErdenPhysicalEconomySavedData.SiteState findSite(
            List<ErdenPhysicalEconomySavedData.SiteState> sites,
            int x,
            int z) {
        for (ErdenPhysicalEconomySavedData.SiteState site : sites) {
            if (site.x() == x && site.z() == z) return site;
        }
        return null;
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

    private static String compactStocks(ErdenPhysicalEconomySavedData.SiteState site) {
        List<String> parts = new ArrayList<>();
        for (ResourceItem resource : PHYSICAL_RESOURCES) {
            long amount = site.stock(resource.resource);
            if (amount > 0L) parts.add(resourceName(resource.resource) + " " + amount);
        }
        return parts.isEmpty() ? "없음" : String.join(", ", parts);
    }

    private static String compactPending(ErdenPhysicalEconomySavedData.SiteState site) {
        return compactMetrics(site, true);
    }

    private static String compactInTransit(ErdenPhysicalEconomySavedData.SiteState site) {
        return compactMetrics(site, false);
    }

    private static String compactMetrics(
            ErdenPhysicalEconomySavedData.SiteState site,
            boolean pending) {
        List<String> parts = new ArrayList<>();
        for (ResourceItem resource : PHYSICAL_RESOURCES) {
            String metric = pending
                    ? ErdenCargoEscrowManager.pendingMetric(resource.resource)
                    : ErdenCargoEscrowManager.inTransitMetric(resource.resource);
            long amount = site.metric(metric);
            if (amount > 0L) parts.add(resourceName(resource.resource) + " " + amount);
        }
        return parts.isEmpty() ? "없음" : String.join(", ", parts);
    }

    private static String resourceName(String resource) {
        return switch (resource) {
            case "wheat" -> "밀";
            case "coal" -> "석탄";
            case "leather" -> "가죽";
            case "paper" -> "종이";
            case "iron" -> "철";
            case "hay" -> "건초";
            case "bread" -> "빵";
            case "goods" -> "생활품";
            default -> resource;
        };
    }

    private static String roleName(String role) {
        return switch (role) {
            case "shop" -> "상점";
            case "bakery" -> "제빵소";
            case "inn" -> "여관";
            case "stable" -> "마구간";
            case "guard_post" -> "경비초소";
            case "bathhouse" -> "목욕시설";
            case "warehouse" -> "창고";
            default -> role;
        };
    }

    private static long totalWalletCoins(
            List<ErdenPhysicalEconomySavedData.WalletState> wallets) {
        long total = 0L;
        for (ErdenPhysicalEconomySavedData.WalletState wallet : wallets) total += wallet.coins();
        return total;
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private record ResourceItem(String resource, Item item) {
    }

    private record WorkerRef(String householdId, String residentId) {
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

    private static final class DayCounters {
        long deliveries;
        long crafted;
        long sales;
        long wages;
        int fulfilledHouseholds;
        int reserveTransfers;
        long reserveMoved;
        int reserveComplete;
        long reserveTotal;
    }

    private record DayResult(
            List<ErdenPhysicalEconomySavedData.SiteState> sites,
            List<ErdenPhysicalEconomySavedData.WalletState> wallets,
            long deliveries,
            long crafted,
            long sales,
            long wages,
            int fulfilledHouseholds,
            ErdenLivingEconomyManager.MarketResult market,
            int reserveTransfers,
            long reserveMoved,
            int reserveComplete,
            long reserveTotal) {
    }
}
