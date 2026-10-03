package dev.moonseungjun.openworldrpg.camp;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerActionRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectUltimateChargeRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerActionRuntimeState;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.profession.R01CraftingRecipe;
import dev.moonseungjun.openworldrpg.profession.R01CraftingService;
import dev.moonseungjun.openworldrpg.profession.R01SmithingRecipe;
import dev.moonseungjun.openworldrpg.recovery.RecoveryBeltReloadService;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class R01CampService {
    private static final String KIT_MATERIAL_RECEIPT_PREFIX =
            "openworld_rpg:camp_material/";
    private static final String DEPLOY_ACTION_ID =
            "openworld_rpg:action/camp_deploy";
    private static final Map<UUID, PendingDeployment> PENDING_DEPLOYMENTS =
            new ConcurrentHashMap<>();

    private R01CampService() {
    }

    public static R01CampPlayerState playerState(ServerPlayer player) {
        return player.getAttachedOrSet(
                R01CampPlayerAttachments.STATE,
                R01CampPlayerState.initial()
        );
    }

    public static R01CampWorldState worldState(MinecraftServer server) {
        return server.overworld().getAttachedOrSet(
                R01CampWorldAttachments.STATE,
                R01CampWorldState.initial()
        );
    }

    public static R01CampPlayerState recordMaterialEncounter(
            ServerPlayer player,
            String materialId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(materialId, "materialId");
        if (!R01GatheringRules.HARDWOOD.equals(materialId)
                && !R01CampRules.TOUGH_HIDE.equals(materialId)) {
            return playerState(player);
        }
        return replacePlayer(
                player,
                playerState(player).recordMaterialEncounter(materialId)
        );
    }

    public static void reconcile(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        reconcileRecipeDiscovery(player);
        R01CampPlayerState.PendingKitCraft pending =
                playerState(player).pendingKitCraft().orElse(null);
        if (pending != null) {
            executePendingKitCraft(player, pending);
        } else {
            PlayerInventoryService.clearCompletedDeliveryIdsWithPrefix(
                    player,
                    KIT_MATERIAL_RECEIPT_PREFIX
            );
        }
    }

    public static KitCraftResult craftKitAtHoltForge(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        reconcile(player);
        R01CampPlayerState current = playerState(player);
        if (current.kitUnlocked()) {
            return result(KitCraftStatus.ALREADY_UNLOCKED);
        }
        if (!current.recipeKnown()) {
            return result(KitCraftStatus.RECIPE_LOCKED);
        }
        if (current.pendingKitCraft().isPresent()) {
            return new KitCraftResult(
                    KitCraftStatus.RECOVERY_WAITING,
                    current.pendingKitCraft().map(
                            R01CampPlayerState.PendingKitCraft::transactionId
                    )
            );
        }
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                R01SmithingRecipe.FORGE_SERVICE_ID
        ).isEmpty()) {
            return result(KitCraftStatus.SERVICE_NOT_PRODUCTION);
        }
        if (!PlayerInventoryService.canConsumeMaterials(
                player, R01CampRules.KIT_MATERIAL_COSTS, true
        )) {
            return result(KitCraftStatus.INSUFFICIENT_MATERIALS);
        }
        if (PlayerCurrencyService.state(player).gold() < R01CampRules.KIT_GOLD_COST) {
            return result(KitCraftStatus.INSUFFICIENT_GOLD);
        }

        var begin = current.beginKitCraft(player.getUUID().toString());
        replacePlayer(player, begin.state());
        return executePendingKitCraft(player, begin.pending());
    }

    public static DeploymentStartResult beginDeployment(
            ServerPlayer player,
            R01CampPlacementAuthority.Candidate candidate,
            R01CampPlacementAuthority.PlacementProbe probe
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(probe, "probe");
        if (!playerState(player).kitUnlocked()) {
            return DeploymentStartResult.rejected(DeploymentStatus.KIT_NOT_UNLOCKED);
        }
        if (!player.isAlive()) {
            return DeploymentStartResult.rejected(DeploymentStatus.INVALID_STATE);
        }
        if (!Level.OVERWORLD.equals(player.level().dimension())) {
            return DeploymentStartResult.rejected(DeploymentStatus.WRONG_DIMENSION);
        }

        UUID playerId = player.getUUID();
        if (PENDING_DEPLOYMENTS.containsKey(playerId)) {
            return DeploymentStartResult.rejected(DeploymentStatus.ALREADY_DEPLOYING);
        }
        long nowTick = player.level().getGameTime();
        if (CombatStateServices.states().getOrCreate(playerId, nowTick).isCombatActive(nowTick)) {
            return DeploymentStartResult.rejected(DeploymentStatus.IN_COMBAT);
        }

        MinecraftServer server = requireServer(player);
        R01CampWorldState shared = worldState(server);
        var decision = R01CampPlacementAuthority.evaluate(
                playerId.toString(), candidate, probe, shared
        );
        if (!decision.allowed()) {
            return new DeploymentStartResult(
                    DeploymentStatus.INVALID_PLACEMENT,
                    Optional.empty(),
                    Optional.of(decision.status())
            );
        }

        var begin = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        DEPLOY_ACTION_ID,
                        R01CampRules.DEPLOY_TICKS,
                        R01CampRules.DEPLOY_TICKS,
                        1.0
                )
        );
        if (!begin.accepted()) {
            return DeploymentStartResult.rejected(DeploymentStatus.BUSY);
        }

        long oldGeneration = shared.camp(playerId.toString())
                .map(R01CampWorldState.ActiveCamp::generation)
                .orElse(0L);
        PendingDeployment pending = new PendingDeployment(
                candidate, oldGeneration, nowTick, begin.endTick()
        );
        PENDING_DEPLOYMENTS.put(playerId, pending);
        return new DeploymentStartResult(
                DeploymentStatus.STARTED,
                Optional.of(pending),
                Optional.empty()
        );
    }

    public static DeploymentCommitResult commitDeployment(
            ServerPlayer player,
            R01CampPlacementAuthority.PlacementProbe refreshedProbe
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(refreshedProbe, "refreshedProbe");
        UUID playerId = player.getUUID();
        PendingDeployment pending = PENDING_DEPLOYMENTS.get(playerId);
        if (pending == null) {
            return DeploymentCommitResult.rejected(
                    DeploymentStatus.NO_PENDING_DEPLOYMENT
            );
        }

        long nowTick = player.level().getGameTime();
        if (nowTick < pending.completeAtTick()) {
            return DeploymentCommitResult.rejected(DeploymentStatus.NOT_READY);
        }
        if (!pending.candidate().equals(refreshedProbe.candidate())) {
            cancelDeployment(player);
            return DeploymentCommitResult.rejected(DeploymentStatus.STALE_PROBE);
        }
        if (!player.isAlive()
                || !Level.OVERWORLD.equals(player.level().dimension())) {
            cancelDeployment(player);
            return DeploymentCommitResult.rejected(DeploymentStatus.INVALID_STATE);
        }
        if (CombatStateServices.states().getOrCreate(playerId, nowTick).isCombatActive(nowTick)) {
            cancelDeployment(player);
            return DeploymentCommitResult.rejected(DeploymentStatus.IN_COMBAT);
        }

        MinecraftServer server = requireServer(player);
        R01CampWorldState current = worldState(server);
        long oldGeneration = current.camp(playerId.toString())
                .map(R01CampWorldState.ActiveCamp::generation)
                .orElse(0L);
        if (oldGeneration != pending.expectedOldGeneration()) {
            cancelDeployment(player);
            return DeploymentCommitResult.rejected(DeploymentStatus.STALE_WORLD_STATE);
        }

        var decision = R01CampPlacementAuthority.evaluate(
                playerId.toString(),
                pending.candidate(),
                refreshedProbe,
                current
        );
        if (!decision.allowed()) {
            cancelDeployment(player);
            return new DeploymentCommitResult(
                    DeploymentStatus.INVALID_PLACEMENT,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(decision.status())
            );
        }

        R01CampWorldState.DeployResult deployed = current.deploy(
                playerId.toString(), pending.candidate()
        );
        replaceWorld(server, deployed.state());
        PENDING_DEPLOYMENTS.remove(playerId, pending);
        ProjectPlayerActionRuntime.cancelAction(player, DEPLOY_ACTION_ID);
        return new DeploymentCommitResult(
                DeploymentStatus.DEPLOYED,
                deployed.previous(),
                Optional.of(deployed.current()),
                Optional.empty()
        );
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            PendingDeployment pending = PENDING_DEPLOYMENTS.get(playerId);
            if (pending == null) {
                continue;
            }
            long nowTick = player.level().getGameTime();
            if (nowTick >= pending.completeAtTick()) {
                continue;
            }

            boolean invalid = !player.isAlive()
                    || !Level.OVERWORLD.equals(player.level().dimension())
                    || CombatStateServices.states()
                            .getOrCreate(playerId, nowTick)
                            .isCombatActive(nowTick);
            if (!invalid) {
                PlayerActionRuntimeState.Snapshot snapshot =
                        ProjectPlayerActionRuntime.snapshot(playerId, nowTick);
                invalid = !snapshot.active()
                        || snapshot.kind() != PlayerActionRuntimeState.WindowKind.ACTION
                        || !DEPLOY_ACTION_ID.equals(snapshot.actionId());
            }
            if (invalid) {
                cancelDeployment(player);
            }
        }
    }

    public static RestResult restAfterValidatedInteraction(
            ServerPlayer player,
            UUID campOwnerId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(campOwnerId, "campOwnerId");
        MinecraftServer server = requireServer(player);
        if (!Level.OVERWORLD.equals(player.level().dimension())
                || worldState(server).camp(campOwnerId.toString()).isEmpty()) {
            return new RestResult(RestStatus.CAMP_NOT_AVAILABLE, 0);
        }

        long nowTick = player.level().getGameTime();
        if (CombatStateServices.states().getOrCreate(player.getUUID(), nowTick)
                .isCombatActive(nowTick)) {
            return new RestResult(RestStatus.IN_COMBAT, 0);
        }
        if (!ProjectPlayerActionRuntime.canStartAction(player)) {
            return new RestResult(RestStatus.BUSY, 0);
        }

        player.setHealth(player.getMaxHealth());
        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        combat.restoreMana(combat.maxMana(), nowTick);
        combat.restoreStamina(combat.maxStamina(), nowTick);
        ProjectUltimateChargeRuntime.resetForRest(player);
        var reload = RecoveryBeltReloadService.reloadAtRest(player);
        CombatStateServices.persistRuntime(player);
        return new RestResult(RestStatus.RESTED, reload.loadedCount());
    }

    /**
     * Camp-authorized cooking after a physical interaction adapter has proved the player is using
     * this active camp. Shared camp services do not transfer ownership.
     */
    public static R01CraftingService.CraftResult cookAfterValidatedInteraction(
            ServerPlayer player,
            UUID campOwnerId,
            R01CraftingRecipe recipe,
            int quantity
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(campOwnerId, "campOwnerId");
        Objects.requireNonNull(recipe, "recipe");

        MinecraftServer server = requireServer(player);
        if (!Level.OVERWORLD.equals(player.level().dimension())
                || worldState(server).camp(campOwnerId.toString()).isEmpty()) {
            return new R01CraftingService.CraftResult(
                    R01CraftingService.CraftStatus.INVALID_CONTEXT,
                    0,
                    Optional.empty()
            );
        }
        long nowTick = player.level().getGameTime();
        if (CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .isCombatActive(nowTick)) {
            return new R01CraftingService.CraftResult(
                    R01CraftingService.CraftStatus.INVALID_CONTEXT,
                    0,
                    Optional.empty()
            );
        }
        return R01CraftingService.craftCampCooking(
                player,
                recipe,
                quantity
        );
    }

    public static void reset(UUID playerId) {
        PENDING_DEPLOYMENTS.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static void reconcileRecipeDiscovery(ServerPlayer player) {
        R01CampPlayerState next = playerState(player);
        boolean hardwoodSeen = R01GatheringService.state(player)
                .discoveryFlags()
                .contains(R01GatheringRules.discoveryFlag(R01GatheringRules.HARDWOOD))
                || ownsMaterial(player, R01GatheringRules.HARDWOOD);
        if (hardwoodSeen) {
            next = next.recordMaterialEncounter(R01GatheringRules.HARDWOOD);
        }
        if (ownsMaterial(player, R01CampRules.TOUGH_HIDE)) {
            next = next.recordMaterialEncounter(R01CampRules.TOUGH_HIDE);
        }
        replacePlayer(player, next);
    }

    private static boolean ownsMaterial(ServerPlayer player, String materialId) {
        PlayerInventoryState inventory = PlayerInventoryService.state(player);
        return inventory.materialPouch().getOrDefault(materialId, 0)
                + inventory.materialVault().getOrDefault(materialId, 0) > 0;
    }

    private static KitCraftResult executePendingKitCraft(
            ServerPlayer player,
            R01CampPlayerState.PendingKitCraft pending
    ) {
        String transactionId = pending.transactionId();
        String goldId = transactionId + "/gold";
        String materialId = KIT_MATERIAL_RECEIPT_PREFIX
                + transactionId.substring(transactionId.indexOf(':') + 1);

        boolean goldDone = PlayerCurrencyService.state(player).hasAppliedDebit(goldId);
        boolean materialsDone = PlayerInventoryService.state(player)
                .deliveryCompleted(materialId);

        if (!goldDone && !materialsDone) {
            if (PlayerCurrencyService.state(player).gold() < R01CampRules.KIT_GOLD_COST) {
                return new KitCraftResult(
                        KitCraftStatus.INSUFFICIENT_GOLD,
                        Optional.of(transactionId)
                );
            }
            if (!PlayerInventoryService.canConsumeMaterials(
                    player, R01CampRules.KIT_MATERIAL_COSTS, true
            )) {
                return new KitCraftResult(
                        KitCraftStatus.INSUFFICIENT_MATERIALS,
                        Optional.of(transactionId)
                );
            }
        }

        if (!goldDone && !PlayerCurrencyService.debitOnce(
                player, goldId, R01CampRules.KIT_GOLD_COST
        ).success()) {
            return new KitCraftResult(
                    KitCraftStatus.RECOVERY_WAITING,
                    Optional.of(transactionId)
            );
        }

        if (!PlayerInventoryService.consumeMaterialsOnce(
                player,
                materialId,
                R01CampRules.KIT_MATERIAL_COSTS,
                true
        ).consumed()) {
            return new KitCraftResult(
                    KitCraftStatus.RECOVERY_WAITING,
                    Optional.of(transactionId)
            );
        }

        replacePlayer(
                player,
                playerState(player).completeKitCraft(transactionId)
        );
        PlayerInventoryService.forgetCompletedDeliveryReceipt(player, materialId);
        return new KitCraftResult(
                KitCraftStatus.UNLOCKED,
                Optional.of(transactionId)
        );
    }

    private static void cancelDeployment(ServerPlayer player) {
        PENDING_DEPLOYMENTS.remove(player.getUUID());
        ProjectPlayerActionRuntime.cancelAction(player, DEPLOY_ACTION_ID);
    }

    private static R01CampPlayerState replacePlayer(
            ServerPlayer player,
            R01CampPlayerState next
    ) {
        R01CampPlayerState current = playerState(player);
        if (!current.equals(next)) {
            player.setAttached(R01CampPlayerAttachments.STATE, next);
        }
        return next;
    }

    private static R01CampWorldState replaceWorld(
            MinecraftServer server,
            R01CampWorldState next
    ) {
        ServerLevel overworld = server.overworld();
        R01CampWorldState current = worldState(server);
        if (!current.equals(next)) {
            overworld.setAttached(R01CampWorldAttachments.STATE, next);
        }
        return next;
    }

    private static MinecraftServer requireServer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            throw new IllegalStateException("R01 Camp requires a live server.");
        }
        return server;
    }

    private static KitCraftResult result(KitCraftStatus status) {
        return new KitCraftResult(status, Optional.empty());
    }

    public enum KitCraftStatus {
        UNLOCKED,
        ALREADY_UNLOCKED,
        RECIPE_LOCKED,
        SERVICE_NOT_PRODUCTION,
        INSUFFICIENT_MATERIALS,
        INSUFFICIENT_GOLD,
        RECOVERY_WAITING
    }

    public record KitCraftResult(
            KitCraftStatus status,
            Optional<String> transactionId
    ) {
        public KitCraftResult {
            Objects.requireNonNull(status, "status");
            transactionId = Objects.requireNonNull(transactionId, "transactionId");
        }
    }

    public enum DeploymentStatus {
        STARTED,
        DEPLOYED,
        KIT_NOT_UNLOCKED,
        INVALID_STATE,
        WRONG_DIMENSION,
        ALREADY_DEPLOYING,
        IN_COMBAT,
        BUSY,
        INVALID_PLACEMENT,
        NO_PENDING_DEPLOYMENT,
        NOT_READY,
        STALE_PROBE,
        STALE_WORLD_STATE
    }

    public record DeploymentStartResult(
            DeploymentStatus status,
            Optional<PendingDeployment> pending,
            Optional<R01CampPlacementAuthority.Status> placementStatus
    ) {
        public DeploymentStartResult {
            Objects.requireNonNull(status, "status");
            pending = Objects.requireNonNull(pending, "pending");
            placementStatus = Objects.requireNonNull(placementStatus, "placementStatus");
            if ((status == DeploymentStatus.STARTED) != pending.isPresent()) {
                throw new IllegalArgumentException("Camp deployment start state mismatch.");
            }
        }

        static DeploymentStartResult rejected(DeploymentStatus status) {
            return new DeploymentStartResult(
                    status, Optional.empty(), Optional.empty()
            );
        }
    }

    public record DeploymentCommitResult(
            DeploymentStatus status,
            Optional<R01CampWorldState.ActiveCamp> previous,
            Optional<R01CampWorldState.ActiveCamp> current,
            Optional<R01CampPlacementAuthority.Status> placementStatus
    ) {
        public DeploymentCommitResult {
            Objects.requireNonNull(status, "status");
            previous = Objects.requireNonNull(previous, "previous");
            current = Objects.requireNonNull(current, "current");
            placementStatus = Objects.requireNonNull(placementStatus, "placementStatus");
            if ((status == DeploymentStatus.DEPLOYED) != current.isPresent()) {
                throw new IllegalArgumentException("Camp deployment commit state mismatch.");
            }
        }

        static DeploymentCommitResult rejected(DeploymentStatus status) {
            return new DeploymentCommitResult(
                    status, Optional.empty(), Optional.empty(), Optional.empty()
            );
        }
    }

    public record PendingDeployment(
            R01CampPlacementAuthority.Candidate candidate,
            long expectedOldGeneration,
            long startedAtTick,
            long completeAtTick
    ) {
        public PendingDeployment {
            Objects.requireNonNull(candidate, "candidate");
            if (expectedOldGeneration < 0L || startedAtTick < 0L
                    || completeAtTick != startedAtTick + R01CampRules.DEPLOY_TICKS) {
                throw new IllegalArgumentException("Invalid Camp deployment transaction.");
            }
        }
    }

    public enum RestStatus {
        RESTED,
        CAMP_NOT_AVAILABLE,
        IN_COMBAT,
        BUSY
    }

    public record RestResult(RestStatus status, int beltSlotsReloaded) {
        public RestResult {
            Objects.requireNonNull(status, "status");
            if (beltSlotsReloaded < 0
                    || (status != RestStatus.RESTED && beltSlotsReloaded != 0)) {
                throw new IllegalArgumentException("Invalid Camp rest result.");
            }
        }
    }
}
