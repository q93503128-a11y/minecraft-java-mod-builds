package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.time.PlayerActiveWorldTimeService;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.server.level.ServerPlayer;

public final class R01NessaMarketService {
    private R01NessaMarketService() {
    }

    public static R01NessaMarketState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01NessaMarketAttachments.MARKET,
                R01NessaMarketState.initial()
        );
    }

    public static ViewResult view(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                R01NessaMarketRules.MARKET_SERVICE_ID
        ).isEmpty()) {
            return new ViewResult(ViewStatus.SERVICE_NOT_PRODUCTION, Optional.empty());
        }

        var time = PlayerActiveWorldTimeService.state(player);
        OptionalLong epoch = time.epoch(
                PlayerActiveWorldTimeService.ALDERFORD_SHRINE_EPOCH
        );
        if (epoch.isEmpty()) {
            return new ViewResult(ViewStatus.SHRINE_NOT_ACTIVATED, Optional.empty());
        }

        long cycleIndex = R01NessaMarketRules.cycleIndex(
                time.activeTicks(),
                epoch.getAsLong()
        );
        R01NessaMarketState next = state(player).ensureCycle(
                Objects.requireNonNull(player.level().getServer())
                        .overworld()
                        .getSeed(),
                player.getUUID().toString(),
                cycleIndex
        );
        replace(player, next);
        return new ViewResult(
                ViewStatus.READY,
                next.currentCycle()
        );
    }

    public static PurchaseResult requestPurchase(
            ServerPlayer player,
            long expectedCycleIndex,
            int slotIndex
    ) {
        ViewResult view = view(player);
        if (view.status() != ViewStatus.READY) {
            return new PurchaseResult(
                    view.status() == ViewStatus.SERVICE_NOT_PRODUCTION
                            ? PurchaseStatus.SERVICE_NOT_PRODUCTION
                            : PurchaseStatus.SHRINE_NOT_ACTIVATED,
                    Optional.empty()
            );
        }

        R01NessaMarketState.Cycle cycle =
                view.cycle().orElseThrow();
        if (cycle.cycleIndex() != expectedCycleIndex) {
            return new PurchaseResult(
                    PurchaseStatus.STALE_CYCLE,
                    Optional.empty()
            );
        }
        if (cycle.soldSlots().contains(slotIndex)) {
            return new PurchaseResult(
                    PurchaseStatus.SOLD,
                    Optional.empty()
            );
        }

        var materialization =
                R01NessaEquipmentMaterialization.resolve(
                        cycle.item(slotIndex)
                );
        if (materialization.status()
                == R01NessaEquipmentMaterialization.ResolutionStatus
                        .RUNTIME_AFFIX_BLOCKED) {
            return new PurchaseResult(
                    PurchaseStatus.AFFIX_RUNTIME_INCOMPLETE,
                    Optional.empty()
            );
        }

        return new PurchaseResult(
                PurchaseStatus.READY_FOR_ITEM_MATERIALIZATION,
                state(player).purchasePlan(
                        player.getUUID().toString(),
                        expectedCycleIndex,
                        slotIndex
                )
        );
    }

    /**
     * Called only after the future shared equipment materializer has durably completed Gold debit
     * and inventory delivery for the exact transaction.
     */
    public static void commitDeliveredPurchase(
            ServerPlayer player,
            R01NessaMarketState.PurchasePlan plan
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(plan, "plan");
        replace(
                player,
                state(player).markSold(
                        player.getUUID().toString(),
                        plan.cycleIndex(),
                        plan.slotIndex(),
                        plan.transactionId()
                )
        );
    }

    private static void replace(
            ServerPlayer player,
            R01NessaMarketState next
    ) {
        R01NessaMarketState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(R01NessaMarketAttachments.MARKET, next);
        }
    }

    public enum ViewStatus {
        READY,
        SERVICE_NOT_PRODUCTION,
        SHRINE_NOT_ACTIVATED
    }

    public record ViewResult(
            ViewStatus status,
            Optional<R01NessaMarketState.Cycle> cycle
    ) {
        public ViewResult {
            Objects.requireNonNull(status, "status");
            cycle = Objects.requireNonNull(cycle, "cycle");
            if ((status == ViewStatus.READY) != cycle.isPresent()) {
                throw new IllegalArgumentException("Nessa view status/cycle mismatch.");
            }
        }
    }

    public enum PurchaseStatus {
        READY_FOR_ITEM_MATERIALIZATION,
        SERVICE_NOT_PRODUCTION,
        SHRINE_NOT_ACTIVATED,
        STALE_CYCLE,
        SOLD,
        AFFIX_RUNTIME_INCOMPLETE
    }

    public record PurchaseResult(
            PurchaseStatus status,
            Optional<R01NessaMarketState.PurchasePlan> plan
    ) {
        public PurchaseResult {
            Objects.requireNonNull(status, "status");
            plan = Objects.requireNonNull(plan, "plan");
            if ((status == PurchaseStatus.READY_FOR_ITEM_MATERIALIZATION)
                    != plan.isPresent()) {
                throw new IllegalArgumentException(
                        "Nessa purchase status/plan mismatch."
                );
            }
        }
    }
}
