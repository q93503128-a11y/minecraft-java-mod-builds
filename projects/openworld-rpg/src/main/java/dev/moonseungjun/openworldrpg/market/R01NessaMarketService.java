package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
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
        reconcileInterruptedPurchases(player);
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

        R01NessaMarketState.PurchasePlan plan = state(player).purchasePlan(
                player.getUUID().toString(),
                expectedCycleIndex,
                slotIndex
        ).orElseThrow(() -> new IllegalStateException(
                "Validated Nessa slot did not produce a purchase plan."
        ));

        var materialization =
                R01NessaEquipmentMaterialization.resolve(
                        plan.item()
                );
        if (materialization.status()
                == R01NessaEquipmentMaterialization.ResolutionStatus
                        .RUNTIME_AFFIX_BLOCKED) {
            return new PurchaseResult(
                    PurchaseStatus.AFFIX_RUNTIME_INCOMPLETE,
                    Optional.empty()
            );
        }
        ProjectInventoryItem item =
                materialization.item().orElseThrow();

        var currency = PlayerCurrencyService.state(player);
        boolean alreadyDebited =
                currency.hasAppliedDebit(plan.transactionId());
        if (!alreadyDebited) {
            if (currency.gold() < plan.item().priceGold()) {
                return new PurchaseResult(
                        PurchaseStatus.INSUFFICIENT_GOLD,
                        Optional.empty()
                );
            }
            if (!PlayerInventoryService.canAcceptBackpack(
                    player,
                    item
            )) {
                return new PurchaseResult(
                        PurchaseStatus.INVENTORY_FULL,
                        Optional.empty()
                );
            }
        }

        var debit = PlayerCurrencyService.debitOnce(
                player,
                plan.transactionId(),
                plan.item().priceGold()
        );
        if (!debit.success()) {
            return new PurchaseResult(
                    PurchaseStatus.INSUFFICIENT_GOLD,
                    Optional.empty()
            );
        }

        PlayerInventoryState.BackpackDeliveryResult delivery =
                PlayerInventoryService.deliverBackpackOnce(
                        player,
                        plan.transactionId(),
                        item
                );

        PurchaseStatus successStatus = PurchaseStatus.PURCHASED;
        if (!delivery.delivered()) {
            /*
             * A newly accepted purchase preflights Backpack capacity on the same server thread, so
             * this branch is only an interruption/corruption recovery guard. Gold has already been
             * durably receipted; never lose the purchased item merely because the Backpack changed
             * before an interrupted transaction was resumed.
             */
            PlayerInventoryState.DeliveryResult recovered =
                    PlayerInventoryService.deliverImportantOnce(
                            player,
                            plan.transactionId(),
                            item
                    );
            successStatus = switch (recovered.status()) {
                case PENDING, STILL_PENDING ->
                        PurchaseStatus.PURCHASED_RECOVERY_PENDING;
                case DELIVERED, ALREADY_COMPLETED ->
                        PurchaseStatus.PURCHASED_RECOVERED;
            };
        }

        commitDeliveredPurchase(player, plan);
        return new PurchaseResult(
                successStatus,
                Optional.of(plan)
        );
    }

    /**
     * Repairs the only legal interruption window: Gold was receipted before item delivery/SOLD.
     *
     * <p>Normal purchases require Backpack space and never route directly to storage. Recovery is
     * deliberately stronger: after payment already exists, Backpack -> Personal Storage -> Pending
     * Reward Claim is allowed so a reconnect cannot destroy a paid deterministic stock item.</p>
     */
    public static void reconcileInterruptedPurchases(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        R01NessaMarketState current = state(player);
        R01NessaMarketState.Cycle cycle =
                current.currentCycle().orElse(null);
        if (cycle == null) {
            return;
        }

        for (int slotIndex = 1;
             slotIndex <= R01NessaMarketRules.STOCK_SLOTS;
             slotIndex++) {
            if (cycle.soldSlots().contains(slotIndex)) {
                continue;
            }
            R01NessaMarketState.PurchasePlan plan =
                    current.purchasePlan(
                            player.getUUID().toString(),
                            cycle.cycleIndex(),
                            slotIndex
                    ).orElse(null);
            if (plan == null
                    || !PlayerCurrencyService.state(player)
                            .hasAppliedDebit(plan.transactionId())) {
                continue;
            }

            var materialization =
                    R01NessaEquipmentMaterialization.resolve(
                            plan.item()
                    );
            if (materialization.status()
                    != R01NessaEquipmentMaterialization.ResolutionStatus
                            .READY) {
                continue;
            }

            ProjectInventoryItem item =
                    materialization.item().orElseThrow();
            if (!PlayerInventoryService.state(player)
                    .deliveryCompleted(plan.transactionId())) {
                PlayerInventoryService.deliverImportantOnce(
                        player,
                        plan.transactionId(),
                        item
                );
            }
            commitDeliveredPurchase(player, plan);
            current = state(player);
            cycle = current.currentCycle().orElseThrow();
        }
    }

    /** Commits SOLD only after the exact purchase item has a durable inventory receipt. */
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
        PURCHASED,
        PURCHASED_RECOVERED,
        PURCHASED_RECOVERY_PENDING,
        SERVICE_NOT_PRODUCTION,
        SHRINE_NOT_ACTIVATED,
        STALE_CYCLE,
        SOLD,
        AFFIX_RUNTIME_INCOMPLETE,
        INSUFFICIENT_GOLD,
        INVENTORY_FULL;

        public boolean successful() {
            return this == PURCHASED
                    || this == PURCHASED_RECOVERED
                    || this == PURCHASED_RECOVERY_PENDING;
        }
    }

    public record PurchaseResult(
            PurchaseStatus status,
            Optional<R01NessaMarketState.PurchasePlan> plan
    ) {
        public PurchaseResult {
            Objects.requireNonNull(status, "status");
            plan = Objects.requireNonNull(plan, "plan");
            if (status.successful() != plan.isPresent()) {
                throw new IllegalArgumentException(
                        "Nessa purchase status/plan mismatch."
                );
            }
        }
    }
}
