package dev.moonseungjun.openworldrpg.gathering;

import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.progression.r01.R01MainQuestService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RiverbankRemediesService;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerState;
import dev.moonseungjun.openworldrpg.progression.r01.R01PlayerStateService;
import dev.moonseungjun.openworldrpg.profession.R01SmithingService;
import dev.moonseungjun.openworldrpg.progression.r01.R01WorldActionService;
import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only R01 personal resource-node transaction boundary.
 *
 * <p>Spatial adapters prove the physical node and interaction context before calling this service.
 * This layer owns personal cooldown, tool/mastery validation, yield, lossless pouch delivery and
 * reconnect repair without inventing any Azari coordinate.</p>
 */
public final class R01GatheringService {
    private R01GatheringService() {
    }

    public static R01GatheringState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01GatheringAttachments.GATHERING,
                R01GatheringState.initial()
        );
    }

    /**
     * Live-world entry point. Candidate/client-verified anchors fail closed until promoted.
     */
    public static HarvestResult tryHarvestBoundNode(
            ServerPlayer player,
            String nodeId,
            R01GatheringAuthority.GatherContext context
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(context, "context");

        var binding = R01GatheringSpatialRegistry.productionNode(nodeId);
        if (binding.isEmpty()) {
            return new HarvestResult(HarvestStatus.NODE_NOT_PRODUCTION, 0);
        }
        return tryHarvest(
                player,
                nodeId,
                binding.orElseThrow().resourceId(),
                context
        );
    }

    public static HarvestResult tryHarvest(
            ServerPlayer player,
            String nodeId,
            String resourceId,
            R01GatheringAuthority.GatherContext context
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(context, "context");

        var definition = R01GatheringRules.resource(resourceId);
        if (definition.isEmpty()) {
            return new HarvestResult(HarvestStatus.INVALID_RESOURCE, 0);
        }
        var resource = definition.orElseThrow();
        R01GatheringState current = state(player);
        long activeTicks = PlayerActiveWorldTimeService.state(player).activeTicks();

        int baseRoll = player.getRandom().nextInt(resource.yieldRangeSize());
        int bonusRoll = player.getRandom().nextInt(100);
        R01GatheringAuthority.GatherDecision decision =
                R01GatheringAuthority.evaluate(
                        current,
                        nodeId,
                        resourceId,
                        activeTicks,
                        context,
                        baseRoll,
                        bonusRoll
                );
        if (!decision.allowed()) {
            return new HarvestResult(mapStatus(decision.status()), 0);
        }

        if (!PlayerInventoryService.canAcceptMaterialInPouch(
                player,
                resourceId,
                decision.quantity()
        )) {
            return new HarvestResult(HarvestStatus.MATERIAL_POUCH_FULL, 0);
        }

        R01GatheringState.BeginHarvestResult begin =
                current.beginHarvest(
                        nodeId,
                        resourceId,
                        decision.quantity(),
                        activeTicks
                );
        replace(player, begin.state());

        boolean finalized = resolvePending(player, begin.pending());
        return new HarvestResult(
                finalized
                        ? HarvestStatus.HARVESTED
                        : HarvestStatus.PENDING_DELIVERY,
                decision.quantity()
        );
    }

    /**
     * Replays durable harvest plans left between node-consume and reward-finalization.
     */
    public static void reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ArrayList<R01GatheringState.PendingHarvest> pending =
                new ArrayList<>(state(player).pendingHarvests().values());
        for (R01GatheringState.PendingHarvest harvest : pending) {
            resolvePending(player, harvest);
        }

        // Any receipt left after a fully finalized plan is stale crash-window protection only.
        PlayerInventoryService.clearCompletedDeliveryIdsWithPrefix(
                player,
                R01GatheringRules.DELIVERY_RECEIPT_PREFIX
        );
    }

    public static R01GatheringState awardMasteryBonusOnce(
            ServerPlayer player,
            R01GatheringRules.GatheringDiscipline discipline,
            String awardFlag,
            int xp
    ) {
        Objects.requireNonNull(player, "player");
        return replace(
                player,
                state(player).awardMasteryXpOnce(
                        discipline,
                        awardFlag,
                        xp
                )
        );
    }

    private static boolean resolvePending(
            ServerPlayer player,
            R01GatheringState.PendingHarvest pending
    ) {
        PlayerInventoryState.MaterialDeliveryResult delivery =
                PlayerInventoryService.deliverMaterialToPouchOnce(
                        player,
                        pending.transactionId(),
                        pending.resourceId(),
                        pending.quantity()
                );
        if (delivery.status()
                == PlayerInventoryState.MaterialDeliveryStatus.CAPACITY_BLOCKED) {
            return false;
        }

        if (R01GatheringRules.VERDANT_CRYSTAL.equals(
                pending.resourceId()
        )) {
            R01SmithingService.recordVerdantCrystalEncounter(player);
        }

        if (R01GatheringRules.HEALING_HERB.equals(
                pending.resourceId()
        )) {
            R01RiverbankRemediesService.recordHealingHerbHarvest(
                    player,
                    pending.transactionId(),
                    pending.quantity()
            );
        }

        R01GatheringRules.ResourceDefinition definition =
                R01GatheringRules.requireResource(pending.resourceId());
        if (definition.dustQuestCredit()
                && R01MainQuestService.canCreditQuarryRoadAction(
                        R01PlayerStateService.state(player),
                        R01PlayerState.QuarryRoadAction.R01_GATHERING_NODE
                )) {
            R01WorldActionService.recordValidGather(player, pending.resourceId());
        }

        replace(player, state(player).finalizeHarvest(pending.transactionId()));
        PlayerInventoryService.forgetCompletedDeliveryReceipt(
                player,
                pending.transactionId()
        );
        return true;
    }

    private static HarvestStatus mapStatus(R01GatheringAuthority.Status status) {
        return switch (status) {
            case ALLOWED -> throw new IllegalArgumentException(
                    "Allowed gathering decision must not be mapped as a rejection."
            );
            case INVALID_RESOURCE -> HarvestStatus.INVALID_RESOURCE;
            case ACTION_BLOCKED -> HarvestStatus.ACTION_BLOCKED;
            case NODE_COOLDOWN -> HarvestStatus.NODE_COOLDOWN;
            case TOOL_TIER_TOO_LOW -> HarvestStatus.TOOL_TIER_TOO_LOW;
        };
    }

    private static R01GatheringState replace(
            ServerPlayer player,
            R01GatheringState next
    ) {
        R01GatheringState current = state(player);
        if (current.equals(next)) {
            return current;
        }
        player.setAttached(R01GatheringAttachments.GATHERING, next);
        return next;
    }

    public enum HarvestStatus {
        HARVESTED,
        NODE_NOT_PRODUCTION,
        INVALID_RESOURCE,
        ACTION_BLOCKED,
        NODE_COOLDOWN,
        TOOL_TIER_TOO_LOW,
        MATERIAL_POUCH_FULL,
        PENDING_DELIVERY
    }

    public record HarvestResult(
            HarvestStatus status,
            int quantity
    ) {
        public HarvestResult {
            Objects.requireNonNull(status, "status");
            if (quantity < 0) {
                throw new IllegalArgumentException("quantity must be non-negative.");
            }
            if (status != HarvestStatus.HARVESTED
                    && status != HarvestStatus.PENDING_DELIVERY
                    && quantity != 0) {
                throw new IllegalArgumentException(
                        "Rejected harvest cannot report material quantity."
                );
            }
        }
    }
}
