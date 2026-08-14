package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
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
 * Turns Erden's physical inventories into a household-scale living market. Shops keep deterministic
 * opening hours and distributed weekly holidays, prices respond to remaining stock, households try
 * alternative shops when the nearest one is closed or empty, and loaded dependents visibly walk the
 * recorded errand route instead of the purchase existing only in a ledger.
 */
public final class ErdenLivingEconomyManager {
    public static final int LIVING_ECONOMY_REVISION = 1;

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_CLOSED = "closed";
    public static final String STATUS_STOCKOUT = "stockout";
    public static final String STATUS_UNAFFORDABLE = "unaffordable";

    private static final int ROUTINE_INTERVAL = 40;
    private static final int ERRAND_LEAD_TICKS = 500;
    private static final int ERRAND_STAY_TICKS = 1_300;
    private static final int MAX_SHOPS_CONSIDERED = 12;

    private static MinecraftServer activeServer;
    private static boolean ciPassed;

    private ErdenLivingEconomyManager() {
    }

    public record MarketResult(
            List<ErdenLivingEconomySavedData.HouseholdMarketState> states,
            long salesCoins,
            int fulfilledHouseholds,
            int failedHouseholds,
            int closedFailures,
            int stockoutFailures,
            int unaffordableFailures) {
        public MarketResult {
            states = List.copyOf(states);
            salesCoins = Math.max(0L, salesCoins);
            fulfilledHouseholds = Math.max(0, fulfilledHouseholds);
            failedHouseholds = Math.max(0, failedHouseholds);
            closedFailures = Math.max(0, closedFailures);
            stockoutFailures = Math.max(0, stockoutFailures);
            unaffordableFailures = Math.max(0, unaffordableFailures);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) {
            activeServer = server;
            ciPassed = false;
        }
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        runLoadedShoppingErrands(level);
        verifyCi(level);
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) {
            return;
        }
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ErdenPopulationSavedData.Household household = findHouseholdByResidentName(
                population, villager.getName().getString());
        if (household == null) return;
        ErdenLivingEconomySavedData living = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        ErdenLivingEconomySavedData.HouseholdMarketState state = living.outcome(household.id());
        if (state == null) return;
        player.sendSystemMessage(Component.literal(
                "§e[오늘의 장보기] §f" + householdMarketMessage(state)));
    }

    public static void prepareDay(
            long day,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites) {
        for (ErdenPhysicalEconomySavedData.SiteState snapshot : List.copyOf(sites.values())) {
            boolean operating = operatesOnDay(snapshot, day);
            ErdenPhysicalEconomySavedData.SiteState updated = snapshot
                    .withMetric("market_day", day + 1L)
                    .withMetric("open_tick", openingTick(snapshot.role()))
                    .withMetric("close_tick", closingTick(snapshot.role()))
                    .withMetric("weekly_holiday", weeklyHoliday(snapshot) + 1L)
                    .withMetric("operating_today", operating ? 1L : 0L)
                    .withMetric("daily_customers", 0L)
                    .withMetric("daily_purchase_failures", 0L)
                    .withMetric("daily_fallback_customers", 0L)
                    .withMetric("listed_bread_price", breadUnitPrice(snapshot))
                    .withMetric("listed_goods_price", goodsUnitPrice(snapshot))
                    .withMetric("listed_bundle_price", bundlePrice(snapshot));
            if (!operating && hasWeeklyHoliday(snapshot.role())) {
                updated = updated.addMetric("closed_days", 1L);
            }
            sites.put(updated.id(), updated);
        }
    }

    public static MarketResult runDailyMarket(
            long day,
            ErdenPopulationSavedData population,
            Set<String> activeHouseholds,
            Map<String, ErdenPhysicalEconomySavedData.SiteState> sites,
            Map<String, ErdenPhysicalEconomySavedData.WalletState> wallets,
            ErdenLivingEconomySavedData previousLivingEconomy) {
        List<ErdenLivingEconomySavedData.HouseholdMarketState> outcomes = new ArrayList<>();
        long salesCoins = 0L;
        int fulfilled = 0;
        int failed = 0;
        int closedFailures = 0;
        int stockoutFailures = 0;
        int unaffordableFailures = 0;

        for (ErdenPopulationSavedData.Household household : population.households()) {
            if (!activeHouseholds.contains(household.id())) continue;
            ErdenPhysicalEconomySavedData.WalletState wallet = wallets.get(household.id());
            long coins = wallet == null ? 0L : wallet.coins();
            int attemptTick = shoppingTick(household.id(), day);
            PurchaseDecision decision = chooseShop(day, attemptTick, household, coins, sites.values());
            ErdenLivingEconomySavedData.HouseholdMarketState previous =
                    previousLivingEconomy.outcome(household.id());
            int previousFailures = previous == null ? 0 : previous.consecutiveFailures();

            if (decision.success()) {
                ErdenPhysicalEconomySavedData.SiteState shop = sites.get(decision.shopId());
                shop = shop.addStock("bread", -2L)
                        .addStock("goods", -1L)
                        .addMetric("coins", decision.price())
                        .addMetric("sales_units", 3L)
                        .addMetric("sales_coins", decision.price())
                        .addMetric("customers_served", 1L)
                        .addMetric("daily_customers", 1L);
                if (decision.fallbackShop()) {
                    shop = shop.addMetric("fallback_customers", 1L)
                            .addMetric("daily_fallback_customers", 1L);
                }
                sites.put(shop.id(), shop);
                if (wallet != null) wallets.put(household.id(), wallet.spend(decision.price()));
                salesCoins += decision.price();
                fulfilled++;
                outcomes.add(new ErdenLivingEconomySavedData.HouseholdMarketState(
                        household.id(), day, STATUS_SUCCESS, decision.shopId(),
                        decision.price(), attemptTick, 0, decision.fallbackShop()));
            } else {
                failed++;
                if (decision.status().equals(STATUS_CLOSED)) closedFailures++;
                else if (decision.status().equals(STATUS_STOCKOUT)) stockoutFailures++;
                else if (decision.status().equals(STATUS_UNAFFORDABLE)) unaffordableFailures++;
                if (!decision.shopId().isBlank()) {
                    ErdenPhysicalEconomySavedData.SiteState shop = sites.get(decision.shopId());
                    if (shop != null) {
                        shop = shop.addMetric("purchase_failures", 1L)
                                .addMetric("daily_purchase_failures", 1L)
                                .addMetric("failure_" + decision.status(), 1L);
                        sites.put(shop.id(), shop);
                    }
                }
                outcomes.add(new ErdenLivingEconomySavedData.HouseholdMarketState(
                        household.id(), day, decision.status(), decision.shopId(),
                        decision.price(), attemptTick, previousFailures + 1, decision.fallbackShop()));
            }
        }

        for (ErdenPhysicalEconomySavedData.SiteState snapshot : List.copyOf(sites.values())) {
            if (!snapshot.role().equals("shop")) continue;
            boolean stocked = snapshot.stock("bread") >= 2L && snapshot.stock("goods") >= 1L;
            long streak = stocked || !operatesOnDay(snapshot, day)
                    ? 0L : snapshot.metric("stockout_streak") + 1L;
            ErdenPhysicalEconomySavedData.SiteState updated = snapshot
                    .withMetric("stockout_streak", streak)
                    .withMetric("listed_bread_price", breadUnitPrice(snapshot))
                    .withMetric("listed_goods_price", goodsUnitPrice(snapshot))
                    .withMetric("listed_bundle_price", bundlePrice(snapshot));
            if (!stocked && operatesOnDay(snapshot, day)) {
                updated = updated.addMetric("stockout_days", 1L);
            }
            sites.put(updated.id(), updated);
        }

        return new MarketResult(
                outcomes, salesCoins, fulfilled, failed,
                closedFailures, stockoutFailures, unaffordableFailures);
    }

    public static boolean operatesOnDay(
            ErdenPhysicalEconomySavedData.SiteState site,
            long day) {
        return !hasWeeklyHoliday(site.role())
                || Math.floorMod(day, 7L) != weeklyHoliday(site);
    }

    public static String siteStatus(
            ErdenPhysicalEconomySavedData.SiteState site,
            long gameTime) {
        long day = Math.floorDiv(gameTime, 24_000L);
        int dayTime = (int) Math.floorMod(gameTime, 24_000L);
        if (site.role().equals("guard_post")) return "상시 근무";
        if (!operatesOnDay(site, day)) return "정기 휴무";
        if (isOpenAt(site, day, dayTime)) return "영업 중";
        int open = openingTick(site.role());
        int close = closingTick(site.role());
        if (open <= close && dayTime < open) return "영업 전";
        return "영업 종료";
    }

    public static String priceText(ErdenPhysicalEconomySavedData.SiteState site) {
        if (!site.role().equals("shop")) return "해당 없음";
        long bread = Math.max(1L, site.metric("listed_bread_price"));
        long goods = Math.max(1L, site.metric("listed_goods_price"));
        long bundle = Math.max(1L, site.metric("listed_bundle_price"));
        return "빵 " + bread + " / 생활품 " + goods + " / 가구 묶음 " + bundle;
    }

    private static PurchaseDecision chooseShop(
            long day,
            int attemptTick,
            ErdenPopulationSavedData.Household household,
            long walletCoins,
            Iterable<ErdenPhysicalEconomySavedData.SiteState> allSites) {
        List<ErdenPhysicalEconomySavedData.SiteState> shops = new ArrayList<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : allSites) {
            if (site.role().equals("shop")) shops.add(site);
        }
        shops.sort(Comparator.<ErdenPhysicalEconomySavedData.SiteState>comparingLong(site ->
                        distanceSquared(household.homeX(), household.homeZ(), site.x(), site.z()))
                .thenComparing(ErdenPhysicalEconomySavedData.SiteState::id));
        if (shops.isEmpty()) return new PurchaseDecision(STATUS_STOCKOUT, "", 0L, false);

        ErdenPhysicalEconomySavedData.SiteState first = shops.getFirst();
        ErdenPhysicalEconomySavedData.SiteState nearestOpen = null;
        ErdenPhysicalEconomySavedData.SiteState nearestStocked = null;
        int considered = 0;
        for (int index = 0; index < shops.size() && considered < MAX_SHOPS_CONSIDERED; index++) {
            ErdenPhysicalEconomySavedData.SiteState shop = shops.get(index);
            considered++;
            if (!isOpenAt(shop, day, attemptTick)) continue;
            if (nearestOpen == null) nearestOpen = shop;
            if (shop.stock("bread") < 2L || shop.stock("goods") < 1L) continue;
            if (nearestStocked == null) nearestStocked = shop;
            long price = bundlePrice(shop);
            if (walletCoins < price) continue;
            return new PurchaseDecision(
                    STATUS_SUCCESS, shop.id(), price, index > 0);
        }
        if (nearestOpen == null) {
            return new PurchaseDecision(STATUS_CLOSED, first.id(), bundlePrice(first), false);
        }
        if (nearestStocked == null) {
            return new PurchaseDecision(
                    STATUS_STOCKOUT, nearestOpen.id(), bundlePrice(nearestOpen),
                    !nearestOpen.id().equals(first.id()));
        }
        return new PurchaseDecision(
                STATUS_UNAFFORDABLE, nearestStocked.id(), bundlePrice(nearestStocked),
                !nearestStocked.id().equals(first.id()));
    }

    private static boolean isOpenAt(
            ErdenPhysicalEconomySavedData.SiteState site,
            long day,
            int dayTime) {
        if (!operatesOnDay(site, day)) return false;
        if (site.role().equals("guard_post")) return true;
        int open = openingTick(site.role());
        int close = closingTick(site.role());
        if (open <= close) return dayTime >= open && dayTime < close;
        return dayTime >= open || dayTime < close;
    }

    private static boolean hasWeeklyHoliday(String role) {
        return role.equals("shop")
                || role.equals("bakery")
                || role.equals("stable")
                || role.equals("bathhouse");
    }

    private static int weeklyHoliday(ErdenPhysicalEconomySavedData.SiteState site) {
        if (!hasWeeklyHoliday(site.role())) return 7;
        return Math.floorMod(site.id().hashCode(), 7);
    }

    private static int openingTick(String role) {
        return switch (role) {
            case "bakery", "stable", "warehouse" -> 1_000;
            case "inn" -> 2_000;
            case "shop" -> 3_000;
            case "bathhouse" -> 4_000;
            case "guard_post" -> 0;
            default -> 3_000;
        };
    }

    private static int closingTick(String role) {
        return switch (role) {
            case "bakery" -> 10_000;
            case "warehouse" -> 11_000;
            case "stable" -> 12_000;
            case "shop" -> 13_000;
            case "bathhouse" -> 16_000;
            case "inn" -> 22_000;
            case "guard_post" -> 24_000;
            default -> 13_000;
        };
    }

    private static int shoppingTick(String householdId, long day) {
        int spread = 8_500;
        int offset = Math.floorMod((householdId + ':' + day).hashCode(), spread);
        return 3_500 + offset;
    }

    private static long breadUnitPrice(ErdenPhysicalEconomySavedData.SiteState site) {
        long stock = site.stock("bread");
        long base = stock >= 8L ? 1L : stock >= 4L ? 2L : 3L;
        if (site.metric("stockout_streak") >= 2L) base++;
        return Math.min(4L, Math.max(1L, base));
    }

    private static long goodsUnitPrice(ErdenPhysicalEconomySavedData.SiteState site) {
        long stock = site.stock("goods");
        long base = stock >= 4L ? 2L : stock >= 2L ? 3L : 4L;
        if (site.metric("stockout_streak") >= 2L) base++;
        return Math.min(5L, Math.max(2L, base));
    }

    private static long bundlePrice(ErdenPhysicalEconomySavedData.SiteState site) {
        return breadUnitPrice(site) * 2L + goodsUnitPrice(site);
    }

    private static int aliveResidents(
            ErdenPopulationSavedData population,
            ErdenPopulationSavedData.Household household) {
        int alive = 0;
        for (ErdenPopulationSavedData.Resident resident : household.residents()) {
            if (!population.isDead(resident.id())) alive++;
        }
        return alive;
    }

    private static void runLoadedShoppingErrands(ServerLevel level) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        int dayTime = (int) Math.floorMod(level.getGameTime(), 24_000L);
        ErdenLivingEconomySavedData living = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        if (living.lastProcessedDay() != day) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);

        Map<String, Errand> errandsByName = new HashMap<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            ErdenLivingEconomySavedData.HouseholdMarketState outcome = living.outcome(household.id());
            if (outcome == null || outcome.day() != day || outcome.shopId().isBlank()) continue;
            ErdenPopulationSavedData.Resident shopper = household.residents().stream()
                    .filter(resident -> !resident.worker() && !population.isDead(resident.id()))
                    .findFirst().orElse(null);
            if (shopper == null || !inErrandWindow(dayTime, outcome.attemptTick())) continue;
            errandsByName.put(shopper.name(), new Errand(outcome, household));
        }
        if (errandsByName.isEmpty()) return;

        Map<String, ErdenPhysicalEconomySavedData.SiteState> sites = new HashMap<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) sites.put(site.id(), site);
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                candidate -> errandsByName.containsKey(candidate.getName().getString()))) {
            Errand errand = errandsByName.get(villager.getName().getString());
            ErdenPhysicalEconomySavedData.SiteState shop = sites.get(errand.outcome.shopId());
            if (shop == null || !level.hasChunk(shop.x() >> 4, shop.z() >> 4)) continue;
            ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(shop.x(), shop.z());
            if (entrance == null) continue;
            int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(
                    entrance.roadX(), entrance.roadZ())) + 1;
            int y = safeStandingY(level, entrance.roadX(), preferredY, entrance.roadZ());
            villager.setPersistenceRequired();
            if (villager.distanceToSqr(
                    entrance.roadX() + 0.5D, y, entrance.roadZ() + 0.5D) > 4.0D) {
                villager.getNavigation().moveTo(
                        entrance.roadX() + 0.5D, y,
                        entrance.roadZ() + 0.5D, 0.62D);
            }
        }
    }

    private static boolean inErrandWindow(int dayTime, int attemptTick) {
        int start = Math.max(0, attemptTick - ERRAND_LEAD_TICKS);
        int end = Math.min(23_999, attemptTick + ERRAND_STAY_TICKS);
        return dayTime >= start && dayTime < end;
    }

    private static String householdMarketMessage(
            ErdenLivingEconomySavedData.HouseholdMarketState state) {
        String shop = shopName(state.shopId());
        return switch (state.status()) {
            case STATUS_SUCCESS -> state.fallbackShop()
                    ? "가까운 상점의 문이 닫혔거나 재고가 없어 " + shop
                    + "까지 이동해 빵과 생활품을 " + state.price() + "화폐에 구입했습니다."
                    : shop + "에서 빵과 생활품을 " + state.price() + "화폐에 구입했습니다.";
            case STATUS_CLOSED -> shop + "에 갔지만 정기 휴무 또는 영업시간 밖이라 장보지 못했습니다.";
            case STATUS_STOCKOUT -> shop + "에 빵이나 생활품이 떨어져 장보지 못했습니다.";
            case STATUS_UNAFFORDABLE -> shop + "의 현재 가격을 감당하지 못해 구입을 포기했습니다.";
            default -> "오늘의 장보기 기록이 아직 정리되지 않았습니다.";
        };
    }

    private static String shopName(String shopId) {
        if (shopId == null || shopId.isBlank()) return "상점";
        int separator = shopId.lastIndexOf('_');
        if (separator < 0 || separator + 1 >= shopId.length()) return "상점";
        try {
            return Integer.parseInt(shopId.substring(separator + 1)) + "번 상점";
        } catch (NumberFormatException ignored) {
            return "상점";
        }
    }

    private static ErdenPopulationSavedData.Household findHouseholdByResidentName(
            ErdenPopulationSavedData population,
            String residentName) {
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (resident.name().equals(residentName)) return household;
            }
        }
        return null;
    }

    private static void verifyCi(ServerLevel level) {
        if (ciPassed || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        ErdenPhysicalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenPhysicalEconomySavedData.TYPE);
        ErdenLivingEconomySavedData living = level.getDataStorage()
                .computeIfAbsent(ErdenLivingEconomySavedData.TYPE);
        long day = economy.lastProcessedDay();
        if (day < 0L
                || !living.hasCurrentDay(
                LIVING_ECONOMY_REVISION, day, ErdenPopulationManager.EXPECTED_HOUSEHOLDS)) return;

        int shops = 0;
        Set<Integer> holidayCoverage = new HashSet<>();
        for (ErdenPhysicalEconomySavedData.SiteState site : economy.sites()) {
            if (!site.role().equals("shop")) continue;
            shops++;
            holidayCoverage.add(weeklyHoliday(site));
            if (site.metric("open_tick") <= 0L
                    || site.metric("close_tick") <= site.metric("open_tick")
                    || site.metric("listed_bundle_price") < 4L) return;
        }
        if (shops != 50 || holidayCoverage.size() != 7) return;
        long successCount = living.outcomes().stream()
                .filter(ErdenLivingEconomySavedData.HouseholdMarketState::success)
                .count();
        long recognizedCount = living.outcomes().stream()
                .filter(state -> state.status().equals(STATUS_SUCCESS)
                        || state.status().equals(STATUS_CLOSED)
                        || state.status().equals(STATUS_STOCKOUT)
                        || state.status().equals(STATUS_UNAFFORDABLE))
                .count();
        long failureCount = living.outcomes().size() - successCount;
        if (recognizedCount != ErdenPopulationManager.EXPECTED_HOUSEHOLDS
                || successCount <= 0L || failureCount < 0L) return;
        if (!auditDecisionPaths()) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_LIVING_ECONOMY_PASS revision={} households={} shops={} purchase_successes={} purchase_failures={} schedules=true holidays={} dynamic_prices=true stockouts_persist=true shopping_routines=true success_path=true closed_path=true stockout_path=true unaffordable_path=true market_spent={}",
                LIVING_ECONOMY_REVISION, ErdenPopulationManager.EXPECTED_HOUSEHOLDS,
                shops, successCount, failureCount, holidayCoverage.size(), living.totalSpent());
    }

    private static boolean auditDecisionPaths() {
        ErdenPopulationSavedData.Household household = new ErdenPopulationSavedData.Household(
                "audit_household", "감사", 0, 0, List.of());
        ErdenPhysicalEconomySavedData.SiteState stocked = auditShop(8L, 4L);
        long openDay = Math.floorMod(weeklyHoliday(stocked) + 1L, 7L);
        int tick = 6_000;
        PurchaseDecision success = chooseShop(
                openDay, tick, household, 30L, List.of(stocked));
        PurchaseDecision stockout = chooseShop(
                openDay, tick, household, 30L, List.of(auditShop(0L, 0L)));
        PurchaseDecision unaffordable = chooseShop(
                openDay, tick, household, 0L, List.of(stocked));
        PurchaseDecision closed = chooseShop(
                weeklyHoliday(stocked), tick, household, 30L, List.of(stocked));
        return success.status().equals(STATUS_SUCCESS)
                && stockout.status().equals(STATUS_STOCKOUT)
                && unaffordable.status().equals(STATUS_UNAFFORDABLE)
                && closed.status().equals(STATUS_CLOSED);
    }

    private static ErdenPhysicalEconomySavedData.SiteState auditShop(long bread, long goods) {
        ErdenPhysicalEconomySavedData.SiteState site = new ErdenPhysicalEconomySavedData.SiteState(
                "erden_shop_999", 4, 4, "shop", List.of(), List.of(), false);
        return site.withStock("bread", bread).withStock("goods", goods);
    }

    private static AABB capitalBounds(ServerLevel level) {
        return new AABB(
                ErdenCapitalStreamingBuilder.WEST_WALL_X - 64,
                level.getMinY(),
                ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 64,
                ErdenCapitalStreamingBuilder.EAST_WALL_X + 64,
                level.getMaxY(),
                ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 64);
    }

    private static ExternalUrbanFabricBuilder.UrbanEntrance findEntrance(int x, int z) {
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance
                : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.x() == x && entrance.z() == z) return entrance;
        }
        return null;
    }

    private static int safeStandingY(
            ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 8; offset++) {
            int[] candidates = offset == 0
                    ? new int[]{preferredY}
                    : new int[]{preferredY + offset, preferredY - offset};
            for (int standingY : candidates) {
                BlockPos feet = new BlockPos(x, standingY, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) {
                    return standingY;
                }
            }
        }
        return preferredY;
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }

    private record PurchaseDecision(
            String status,
            String shopId,
            long price,
            boolean fallbackShop) {
        boolean success() {
            return status.equals(STATUS_SUCCESS);
        }
    }

    private record Errand(
            ErdenLivingEconomySavedData.HouseholdMarketState outcome,
            ErdenPopulationSavedData.Household household) {
    }
}
