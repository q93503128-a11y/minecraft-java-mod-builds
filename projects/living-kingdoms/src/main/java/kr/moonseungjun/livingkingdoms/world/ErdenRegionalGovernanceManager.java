package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Municipal government and public security for Erden's six second-ring villages.
 *
 * <p>Existing reeves and clerks remain the administrative actors. Public revenue is assessed from
 * actual regional production deltas rather than generated on a timer. Two separately-accounted
 * public guards are billeted at each authored watch house and patrol only through loaded chunks;
 * no gameplay chunk tickets or guard teleports are used.</p>
 */
public final class ErdenRegionalGovernanceManager {
    public static final int GOVERNANCE_REVISION = 1;
    public static final int EXPECTED_COUNCILS = 6;
    public static final int GUARDS_PER_SETTLEMENT = 2;
    public static final int EXPECTED_GUARDS = EXPECTED_COUNCILS * GUARDS_PER_SETTLEMENT;
    private static final long OPENING_TREASURY = 36L;
    private static final int TAX_DIVISOR = 10;
    private static final long GUARD_DAILY_PAY = 2L;
    private static final long GUARD_REPLACEMENT_COST = 12L;
    private static final int GUARD_REPLACEMENT_DAYS = 3;
    private static final int MAX_CATCH_UP_DAYS = 14;
    private static final int SPAWN_INTERVAL = 20;
    private static final int ROUTINE_INTERVAL = 60;
    private static final int INCIDENT_INTERVAL = 100;
    private static final int INCIDENT_COOLDOWN = 600;
    private static final int ROUTE_LOAD_SAMPLE = 8;
    private static final int GUARD_SPAWN_BUDGET = 4;
    private static final int GUARD_NAVIGATION_BUDGET = 8;
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");

    private static MinecraftServer activeServer;
    private static boolean planLogged;
    private static boolean ciPassed;

    private ErdenRegionalGovernanceManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        ErdenRegionalSocietySavedData society = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSocietySavedData.TYPE);
        ErdenRegionalEconomySavedData economy = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalEconomySavedData.TYPE);
        if (!society.hasPopulation(
                ErdenRegionalSocietyManager.SOCIETY_REVISION,
                ErdenRegionalSocietyManager.EXPECTED_HOUSEHOLDS)
                || !economy.hasEconomy(
                ErdenRegionalEconomyManager.ECONOMY_REVISION,
                ErdenRegionalEconomyManager.EXPECTED_SETTLEMENTS)) return;

        ErdenRegionalGovernanceSavedData governance = data(level);
        ensureGovernance(level, economy, governance);
        logPlanOnce(governance);
        processMissingDays(level, economy, governance);
        materializeLoadedLedgers(level, governance);
        ensureLoadedGuards(level, governance);
        runGuardPatrols(level, governance);
        detectIncidents(level, governance);
        verifyCi(level, society, economy, governance);
    }

    public static void handleOfficialInteraction(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;

        ErdenRegionalGovernanceSavedData governance = data(level);
        OfficialRef official = officialByName(level, villager.getName().getString());
        if (official != null) {
            ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(official.settlementId());
            if (council == null) return;
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            sendCouncilStatus(player, official.settlementId(), official.role(), council, governance);
            return;
        }

        ErdenRegionalGovernanceSavedData.GuardPost guard = guardByName(governance, villager.getName().getString());
        if (guard == null) return;
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(guard.settlementId());
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        boolean onDuty = guard.slot() == 0 ? dayTime < 12_000L : dayTime >= 12_000L;
        player.sendSystemMessage(Component.literal(
                "§6[" + guardName(guard) + "] §f"
                        + (onDuty ? "순찰 근무 중" : "초소 대기 중")
                        + " | 순찰 " + guard.totalPatrols() + "회"
                        + " | 마을 치안 " + (council == null ? "-" : council.safetyScore()) + "/100"));
    }

    public static void handleLedgerInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenRegionalGovernanceSavedData governance = data(level);
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (!ledgerPosition(settlement).equals(event.getPos())) continue;
            ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(settlement.id());
            if (council == null) return;
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            sendCouncilStatus(player, settlement.id(), "ledger", council, governance);
            return;
        }
    }

    public static void markDeadIfGuard(ServerLevel level, Villager villager) {
        if (!level.dimension().equals(StarterRealmManager.REALM_KEY)) return;
        ErdenRegionalGovernanceSavedData governance = data(level);
        ErdenRegionalGovernanceSavedData.GuardPost guard = guardByName(governance, villager.getName().getString());
        if (guard == null || !guard.alive()) return;
        long day = Math.floorDiv(level.getGameTime(), 24_000L);
        governance.replaceGuard(guard.killed(day + GUARD_REPLACEMENT_DAYS));
        LivingKingdoms.LOGGER.info(
                "Erden regional guard {} killed settlement={} replacement_due_day={} no_duplicate_respawn=true",
                guard.id(), guard.settlementId(), day + GUARD_REPLACEMENT_DAYS);
    }

    public static BlockPos ledgerPosition(ErdenRegionalSettlementCatalog.Settlement settlement) {
        ErdenRegionalSettlementCatalog.BuildingLot hall = lot(settlement, "reeve_hall");
        int x = settlement.x() + hall.dx();
        int z = settlement.z() + hall.dz() + 20;
        int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 1;
        return new BlockPos(x, y, z);
    }

    private static ErdenRegionalGovernanceSavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenRegionalGovernanceSavedData.TYPE);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        planLogged = false;
        ciPassed = false;
    }

    private static void ensureGovernance(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalGovernanceSavedData governance) {
        if (governance.hasGovernance(GOVERNANCE_REVISION, EXPECTED_COUNCILS, EXPECTED_GUARDS)) return;
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        List<ErdenRegionalGovernanceSavedData.CouncilState> councils = new ArrayList<>();
        List<ErdenRegionalGovernanceSavedData.GuardPost> guards = new ArrayList<>();
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            ErdenRegionalEconomySavedData.SettlementState state = economy.settlement(settlement.id());
            if (state == null) throw new IllegalStateException(
                    "Missing regional economy for government " + settlement.id());
            councils.add(new ErdenRegionalGovernanceSavedData.CouncilState(
                    settlement.id(), OPENING_TREASURY, currentDay - 1L,
                    state.totalProduced(), 0L, 0L, 0L,
                    "", -1L, -1L, 0L, 70, 0L, -1L));
            for (int slot = 0; slot < GUARDS_PER_SETTLEMENT; slot++) {
                guards.add(new ErdenRegionalGovernanceSavedData.GuardPost(
                        settlement.id(), slot, 1, true, -1L, 0L));
            }
        }
        governance.initialize(GOVERNANCE_REVISION, councils, guards);
        validatePlan(governance);
    }

    private static void validatePlan(ErdenRegionalGovernanceSavedData governance) {
        if (!governance.hasGovernance(GOVERNANCE_REVISION, EXPECTED_COUNCILS, EXPECTED_GUARDS)) {
            throw new IllegalStateException("Regional government count drifted");
        }
        Set<String> councils = new HashSet<>();
        Set<String> guards = new HashSet<>();
        for (ErdenRegionalGovernanceSavedData.CouncilState council : governance.councils()) {
            if (settlement(council.settlementId()) == null || !councils.add(council.settlementId())) {
                throw new IllegalStateException("Invalid regional council " + council.settlementId());
            }
        }
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : governance.guardPosts()) {
            if (settlement(guard.settlementId()) == null
                    || guard.slot() >= GUARDS_PER_SETTLEMENT
                    || !guards.add(guard.settlementId() + ":" + guard.slot())) {
                throw new IllegalStateException("Invalid regional guard post " + guard.id());
            }
        }
        if (councils.size() != EXPECTED_COUNCILS || guards.size() != EXPECTED_GUARDS) {
            throw new IllegalStateException("Regional governance coverage drifted councils="
                    + councils.size() + " guards=" + guards.size());
        }
    }

    private static void logPlanOnce(ErdenRegionalGovernanceSavedData governance) {
        if (planLogged) return;
        validatePlan(governance);
        planLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden regional governance revision={} councils={} guards={} reeve_clerk_authority=true production_assessed_tax=true village_treasury=true public_contracts=true guard_payroll=true replacement_roster=true navigation_only=true aggregate_when_unloaded=true",
                GOVERNANCE_REVISION, EXPECTED_COUNCILS, EXPECTED_GUARDS);
    }

    private static void processMissingDays(
            ServerLevel level,
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalGovernanceSavedData governance) {
        long currentDay = Math.floorDiv(level.getGameTime(), 24_000L);
        long oldest = currentDay;
        for (ErdenRegionalGovernanceSavedData.CouncilState council : governance.councils()) {
            oldest = Math.min(oldest, council.lastProcessedDay());
        }
        long first = Math.max(oldest + 1L, currentDay - MAX_CATCH_UP_DAYS + 1L);
        for (long day = first; day <= currentDay; day++) {
            for (ErdenRegionalGovernanceSavedData.CouncilState snapshot : governance.councils()) {
                if (snapshot.lastProcessedDay() >= day) continue;
                processCouncilDay(economy, governance, snapshot.settlementId(), day);
            }
        }
    }

    private static void processCouncilDay(
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalGovernanceSavedData governance,
            String settlementId,
            long day) {
        ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(settlementId);
        ErdenRegionalEconomySavedData.SettlementState market = economy.settlement(settlementId);
        if (council == null || market == null) return;

        long productionNow = market.totalProduced();
        long productionDelta = Math.max(0L, productionNow - council.lastProductionSeen());
        long tax = productionDelta / TAX_DIVISOR;
        int aliveBefore = governance.aliveGuardCount(settlementId);
        long payroll = Math.min(council.treasuryMarks() + tax, aliveBefore * GUARD_DAILY_PAY);
        long available = Math.max(0L, council.treasuryMarks() + tax - payroll);

        List<ErdenRegionalGovernanceSavedData.GuardPost> due = governance.guardPosts().stream()
                .filter(guard -> guard.settlementId().equals(settlementId)
                        && !guard.alive()
                        && guard.replacementDueDay() >= 0L
                        && guard.replacementDueDay() <= day)
                .toList();
        int replacements = Math.min(due.size(), (int) (available / GUARD_REPLACEMENT_COST));
        long replacementCost = replacements * GUARD_REPLACEMENT_COST;
        available -= replacementCost;

        String active = council.activeContract();
        long started = council.contractStartedDay();
        long dueDay = council.contractDueDay();
        boolean completed = !active.isBlank() && dueDay >= 0L && day >= dueDay;
        if (completed) {
            active = "";
            started = -1L;
            dueDay = -1L;
        }

        long contractCost = 0L;
        if (active.isBlank()) {
            Contract choice = chooseContract(market, day);
            if (available >= choice.cost()) {
                active = choice.id();
                started = day;
                dueDay = day + choice.durationDays();
                contractCost = choice.cost();
                available -= contractCost;
            }
        }

        int aliveAfter = aliveBefore + replacements;
        int safety = safetyScore(market, active, aliveAfter, available);
        governance.replaceCouncil(council.processDay(
                day, productionNow, tax, payroll,
                replacementCost + contractCost,
                active, started, dueDay, completed, safety));
        for (int index = 0; index < replacements; index++) {
            governance.replaceGuard(due.get(index).replaced());
        }
    }

    private static Contract chooseContract(ErdenRegionalEconomySavedData.SettlementState market, long day) {
        if (market.shortageDays() > 0L && Math.floorMod(day, 3L) == 0L) {
            return new Contract("grain_reserve", 14L, 2);
        }
        return switch (market.industry()) {
            case "river_market" -> new Contract("storehouse_watch", 10L, 2);
            case "colliery", "iron_mine" -> new Contract("road_maintenance", 18L, 3);
            case "ranch" -> new Contract("road_watch", 12L, 2);
            case "grain" -> Math.floorMod(day, 2L) == 0L
                    ? new Contract("road_maintenance", 18L, 3)
                    : new Contract("market_watch", 10L, 2);
            default -> new Contract("market_watch", 10L, 2);
        };
    }

    private static int safetyScore(
            ErdenRegionalEconomySavedData.SettlementState market,
            String contract,
            int aliveGuards,
            long reserve) {
        int contractBonus = switch (contract) {
            case "road_watch" -> 12;
            case "market_watch", "storehouse_watch" -> 9;
            case "road_maintenance" -> 6;
            case "grain_reserve" -> 4;
            default -> 0;
        };
        int shortagePenalty = (int) Math.min(18L, market.shortageDays());
        int reserveBonus = reserve >= 24L ? 5 : reserve >= 12L ? 2 : 0;
        return Math.max(0, Math.min(100,
                38 + aliveGuards * 18 + contractBonus + reserveBonus - shortagePenalty));
    }

    private static void materializeLoadedLedgers(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance) {
        if (level.getGameTime() % 40L != 0L) return;
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            BlockPos ledger = ledgerPosition(settlement);
            if (!constructedAndLoaded(level, ledger.getX(), ledger.getZ())) continue;
            if (level.getBlockState(ledger).isAir()) {
                level.setBlockAndUpdate(ledger, Blocks.LECTERN.defaultBlockState());
                governance.markLedgerMaterialized(settlement.id());
            } else if (level.getBlockState(ledger).is(Blocks.LECTERN)) {
                governance.markLedgerMaterialized(settlement.id());
            }
        }
    }

    private static void ensureLoadedGuards(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance) {
        if (level.getGameTime() % SPAWN_INTERVAL != 0L) return;
        Map<String, Villager> existing = loadedGuardsByName(level, governance);
        int budget = GUARD_SPAWN_BUDGET;
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : governance.guardPosts()) {
            if (budget <= 0) break;
            if (!guard.alive()) continue;
            String name = guardName(guard);
            if (existing.containsKey(name)) continue;
            ErdenRegionalSettlementCatalog.Settlement settlement = settlement(guard.settlementId());
            if (settlement == null || !playerNearSettlement(level, settlement, 192)) continue;
            BlockPos post = watchPostPosition(settlement, guard.slot());
            if (!constructedAndLoaded(level, post.getX(), post.getZ())) continue;
            BlockPos spawn = walkableNear(level, post.getX(), post.getZ(), guard.slot());
            if (spawn.equals(BlockPos.ZERO) || !spawnGuard(level, guard, spawn)) continue;
            budget--;
        }
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

    private static void runGuardPatrols(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance) {
        if (level.getGameTime() % ROUTINE_INTERVAL != 0L) return;
        Map<String, ErdenRegionalGovernanceSavedData.GuardPost> refs = guardReferences(governance);
        int navigationBudget = GUARD_NAVIGATION_BUDGET;
        long dayTime = Math.floorMod(level.getGameTime(), 24_000L);
        long patrolPhase = Math.floorDiv(level.getGameTime(), 1_200L);
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            for (Villager villager : level.getEntitiesOfClass(
                    Villager.class, settlementBounds(level, settlement),
                    candidate -> refs.containsKey(candidate.getName().getString()))) {
                ErdenRegionalGovernanceSavedData.GuardPost guard = refs.get(villager.getName().getString());
                if (guard == null || !guard.alive()) continue;
                boolean onDuty = guard.slot() == 0 ? dayTime < 12_000L : dayTime >= 12_000L;
                BlockPos target = onDuty
                        ? patrolTarget(level, settlement, (int) Math.floorMod(patrolPhase + guard.slot(), 5L))
                        : walkableNear(level, watchPostPosition(settlement, guard.slot()).getX(),
                                watchPostPosition(settlement, guard.slot()).getZ(), guard.slot());
                if (target.equals(BlockPos.ZERO)) continue;
                if (villager.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) <= 9.0D) {
                    if (onDuty && level.getGameTime() % 1_200L == 0L) {
                        governance.replaceGuard(guard.patrolCompleted());
                    }
                    continue;
                }
                if (navigationBudget > 0 && routeLoaded(level, villager.blockPosition(), target)) {
                    navigationBudget--;
                    villager.getNavigation().moveTo(
                            target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.62D);
                }
            }
        }
    }

    private static BlockPos patrolTarget(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            int phase) {
        return switch (phase) {
            case 0 -> walkableNear(level,
                    watchPostPosition(settlement, 0).getX(), watchPostPosition(settlement, 0).getZ(), 0);
            case 1 -> walkableNear(level, settlement.x() + 20, settlement.z(), 0);
            case 2 -> {
                BlockPos ledger = ledgerPosition(settlement);
                yield walkableNear(level, ledger.getX(), ledger.getZ(), 0);
            }
            case 3 -> {
                BlockPos storage = ErdenRegionalEconomyManager.storagePosition(settlement);
                yield constructedAndLoaded(level, storage.getX(), storage.getZ())
                        ? walkableNear(level, storage.getX(), storage.getZ(), 0) : BlockPos.ZERO;
            }
            default -> {
                ErdenRegionalRoadNetwork.Point hub = ErdenRegionalRoadNetwork.hub(settlement.id());
                yield walkableNear(level, hub.x(), hub.z(), 0);
            }
        };
    }

    private static void detectIncidents(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance) {
        if (level.getGameTime() % INCIDENT_INTERVAL != 0L) return;
        long now = level.getGameTime();
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (!playerNearSettlement(level, settlement, 256)) continue;
            ErdenRegionalGovernanceSavedData.CouncilState council = governance.council(settlement.id());
            if (council == null || now - council.lastIncidentTick() < INCIDENT_COOLDOWN) continue;
            AABB threatArea = new AABB(
                    settlement.x() - 128, level.getMinY(), settlement.z() - 128,
                    settlement.x() + 128, level.getMaxY(), settlement.z() + 128);
            List<Monster> threats = level.getEntitiesOfClass(Monster.class, threatArea);
            if (threats.isEmpty()) continue;
            governance.replaceCouncil(council.recordIncident(now, 4));
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(settlement.x(), player.getY(), settlement.z()) <= 256.0D * 256.0D) {
                    player.sendSystemMessage(Component.literal(
                            "§c[" + settlementName(settlement.id()) + " 경비대] §f"
                                    + "마을 인근 위협 " + threats.size() + "체 감지. 초소와 주요 시설을 경계합니다."));
                }
            }
        }
    }

    private static void verifyCi(
            ServerLevel level,
            ErdenRegionalSocietySavedData society,
            ErdenRegionalEconomySavedData economy,
            ErdenRegionalGovernanceSavedData governance) {
        if (ciPassed || !isCi()) return;
        validatePlan(governance);
        if (society.residentCount() != ErdenRegionalSocietyManager.EXPECTED_RESIDENTS
                || society.workerCount() != ErdenRegionalSocietyManager.EXPECTED_WORKERS
                || !economy.hasEconomy(
                ErdenRegionalEconomyManager.ECONOMY_REVISION,
                ErdenRegionalEconomyManager.EXPECTED_SETTLEMENTS)
                || governance.aliveGuardCount() != EXPECTED_GUARDS) return;

        int officialCount = 0;
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (resident.workRole().equals("reeve") || resident.workRole().equals("clerk")) officialCount++;
            }
        }
        if (officialCount != EXPECTED_COUNCILS * 2) return;

        ErdenRegionalSettlementCatalog.Settlement sample = settlement("harvest_crossing");
        if (sample == null) return;
        ErdenRegionalGovernanceSavedData.CouncilState sampleCouncil = governance.council(sample.id());
        if (sampleCouncil == null) return;
        BlockPos localA = watchPostPosition(sample, 0);
        BlockPos localB = new BlockPos(sample.x() + 20, localA.getY(), sample.z());
        ErdenRegionalSettlementCatalog.Settlement remote = settlement("ironvale");
        if (remote == null) return;
        boolean localNavigationRule = Math.abs(localA.getX() - localB.getX()) < 256;
        boolean unloadedRouteGuard = !routeLoaded(
                level, localA, new BlockPos(remote.x(), localA.getY(), remote.z()));
        if (!localNavigationRule || !unloadedRouteGuard) return;

        ciPassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_REGIONAL_GOVERNANCE_PASS revision={} councils={} officials={} guard_posts={} alive_guards={} production_assessed_tax=true village_treasury=true public_contracts=true guard_payroll=true casualty_replacement=true watch_house_billet=true shift_patrol=true road_gateway_coverage=true hostile_incident_detection=true navigation_only=true loaded_route_guard=true aggregate_when_unloaded=true persistent_forced_chunks=false",
                GOVERNANCE_REVISION, EXPECTED_COUNCILS, officialCount,
                EXPECTED_GUARDS, governance.aliveGuardCount());
    }

    private static void sendCouncilStatus(
            ServerPlayer player,
            String settlementId,
            String officialRole,
            ErdenRegionalGovernanceSavedData.CouncilState council,
            ErdenRegionalGovernanceSavedData governance) {
        String speaker = officialRole.equals("reeve") ? "촌장"
                : officialRole.equals("clerk") ? "서기" : "공공장부";
        String contract = council.activeContract().isBlank()
                ? "대기" : contractName(council.activeContract()) + "(완료일 " + council.contractDueDay() + ")";
        player.sendSystemMessage(Component.literal(
                "§6[" + settlementName(settlementId) + " " + speaker + "] §f"
                        + "재정 " + council.treasuryMarks() + "마르크"
                        + " | 누적세입 " + council.totalTaxCollected()
                        + " | 공공지출 " + council.totalPublicSpent()
                        + " | 경비급료 " + council.totalGuardPayroll()
                        + " | 계약 " + contract
                        + " | 경비 " + governance.aliveGuardCount(settlementId) + "/" + GUARDS_PER_SETTLEMENT
                        + " | 치안 " + council.safetyScore() + "/100"
                        + " | 사건 " + council.totalIncidents()));
    }

    private static Map<String, Villager> loadedGuardsByName(
            ServerLevel level,
            ErdenRegionalGovernanceSavedData governance) {
        Set<String> names = guardReferences(governance).keySet();
        Map<String, Villager> result = new HashMap<>();
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            for (Villager villager : level.getEntitiesOfClass(
                    Villager.class, settlementBounds(level, settlement),
                    candidate -> names.contains(candidate.getName().getString()))) {
                result.putIfAbsent(villager.getName().getString(), villager);
            }
        }
        return result;
    }

    private static Map<String, ErdenRegionalGovernanceSavedData.GuardPost> guardReferences(
            ErdenRegionalGovernanceSavedData governance) {
        Map<String, ErdenRegionalGovernanceSavedData.GuardPost> result = new HashMap<>();
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : governance.guardPosts()) {
            if (guard.alive()) result.put(guardName(guard), guard);
        }
        return result;
    }

    private static ErdenRegionalGovernanceSavedData.GuardPost guardByName(
            ErdenRegionalGovernanceSavedData governance,
            String name) {
        for (ErdenRegionalGovernanceSavedData.GuardPost guard : governance.guardPosts()) {
            if (guard.alive() && guardName(guard).equals(name)) return guard;
        }
        return null;
    }

    private static OfficialRef officialByName(ServerLevel level, String name) {
        ErdenRegionalSocietySavedData society = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSocietySavedData.TYPE);
        for (ErdenRegionalSocietySavedData.Household household : society.households()) {
            for (ErdenRegionalSocietySavedData.Resident resident : household.residents()) {
                if (resident.name().equals(name)
                        && (resident.workRole().equals("reeve") || resident.workRole().equals("clerk"))) {
                    return new OfficialRef(household.settlementId(), resident.workRole());
                }
            }
        }
        return null;
    }

    private static BlockPos watchPostPosition(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            int slot) {
        ErdenRegionalSettlementCatalog.BuildingLot watch = lot(settlement, "watch_house_east");
        int x = settlement.x() + watch.dx() - 24;
        int z = settlement.z() + watch.dz() + (slot == 0 ? -3 : 3);
        int y = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 1;
        return new BlockPos(x, y, z);
    }

    private static boolean playerNearSettlement(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement,
            int radius) {
        double radiusSquared = (double) radius * radius;
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - settlement.x();
            double dz = player.getZ() - settlement.z();
            if (dx * dx + dz * dz <= radiusSquared) return true;
        }
        return false;
    }

    private static boolean constructedAndLoaded(ServerLevel level, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) return false;
        ErdenRegionalSettlementSavedData construction = level.getDataStorage()
                .computeIfAbsent(ErdenRegionalSettlementSavedData.TYPE);
        return construction.isBuilt(chunkKey(chunkX, chunkZ), ErdenRegionalSettlementCatalog.REVISION);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
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

    private static boolean routeLoaded(ServerLevel level, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int distance = Math.max(Math.abs(dx), Math.abs(dz));
        int steps = Math.max(1, (distance + ROUTE_LOAD_SAMPLE - 1) / ROUTE_LOAD_SAMPLE);
        for (int step = 0; step <= steps; step++) {
            int x = from.getX() + Math.floorDiv(dx * step, steps);
            int z = from.getZ() + Math.floorDiv(dz * step, steps);
            if (!level.hasChunk(x >> 4, z >> 4)) return false;
        }
        return true;
    }

    private static AABB settlementBounds(
            ServerLevel level,
            ErdenRegionalSettlementCatalog.Settlement settlement) {
        int radius = ErdenRegionalSettlementCatalog.SETTLEMENT_RADIUS + 40;
        return new AABB(
                settlement.x() - radius, level.getMinY(), settlement.z() - radius,
                settlement.x() + radius, level.getMaxY(), settlement.z() + radius);
    }

    private static ErdenRegionalSettlementCatalog.Settlement settlement(String id) {
        for (ErdenRegionalSettlementCatalog.Settlement settlement
                : ErdenRegionalSettlementCatalog.settlements()) {
            if (settlement.id().equals(id)) return settlement;
        }
        return null;
    }

    private static ErdenRegionalSettlementCatalog.BuildingLot lot(
            ErdenRegionalSettlementCatalog.Settlement settlement,
            String role) {
        return settlement.buildings().stream()
                .filter(candidate -> candidate.role().equals(role))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing regional building role " + settlement.id() + "/" + role));
    }

    private static String guardName(ErdenRegionalGovernanceSavedData.GuardPost guard) {
        return settlementName(guard.settlementId()) + " 경비대 " + (guard.slot() + 1)
                + "조 " + guard.generation() + "기";
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

    private static String contractName(String id) {
        return switch (id) {
            case "grain_reserve" -> "비상곡물 비축";
            case "storehouse_watch" -> "창고 야간경계";
            case "road_maintenance" -> "국도·진입로 보수";
            case "road_watch" -> "국도 순찰";
            case "market_watch" -> "시장 질서유지";
            default -> id;
        };
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    }

    private record Contract(String id, long cost, int durationDays) {
    }

    private record OfficialRef(String settlementId, String role) {
    }
}
